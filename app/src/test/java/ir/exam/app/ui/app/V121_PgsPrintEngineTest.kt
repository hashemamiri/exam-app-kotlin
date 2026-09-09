package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V121 — موتور پیش‌نمایش/چاپِ PGS (آزمون‌ساز v20) در رندرر بومی:
 * تنظیمات صفحه (کاغذ/جهت/حاشیه/کادر/شمارهٔ صفحه/تکرار سرستون/فونت)،
 * تقسیمِ محتواییِ سطرِ سؤال (q-cont) پیش از برشِ تصویری، و صفاتِ چاپِ هم‌اندازه با برگه.
 */
class V121_PgsPrintEngineTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val renderer by lazy { File(root(), "app/src/main/assets/print/exam_print_renderer_legacy.html").readText() }
    private val dialog by lazy { File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText() }
    private val store by lazy { File(root(), "app/src/main/java/ir/exam/app/data/local/PrintPageSetupStore.kt").readText() }
    private val payload by lazy { File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt").readText() }

    @Test
    fun `renderer exposes PGS page setup and splits rows by content before pixel cut`() {
        listOf(
            "function setPageSetup(value)", "function getPageSetup()", "function readSetup(value)",
            "var PAPERS = {a4:[210,297],a5:[148,210]", "--pg-w", "--pg-h", "--pg-mt", "--pg-font",
            "@media print{@page{size:' + d.W + 'mm ' + d.H + 'mm;margin:0}}",
            "function splitRowByPieces(row, src, alone)", "function moveLastPiece(main, target)",
            "' q-cont'", "if (split && split.whole) {", "tableShell(src, pageSetup.repeatHeader)",
            "page.className = 'a4-page' + (pageSetup.border ? '' : ' no-border')", "no.className = 'page-no'",
            "setPageSetup:setPageSetup,getPageSetup:getPageSetup"
        ).forEach { assertTrue("در رندرر نیست: $it", it in renderer) }
        assertTrue("خطوطِ پاسخ باید تک‌تک به سطرِ ادامه بروند", "space2.insertBefore(lines[lines.length - 1], space2.firstChild)" in renderer)
        assertTrue("فاصلهٔ جداکننده باید بینِ دو برگه تقسیم شود", "if (room >= 8 && room < h) {" in renderer)
        assertFalse("iframe در رندرر ممنوع است", "<iframe" in renderer.lowercase())
    }

    @Test
    fun `page setup is stored on device, injected into payload and applied to Android print attributes`() {
        listOf("class PrintPageSetupStore", "data class PrintPageSetup", "fun toJson(): String", "fun printAttributes(): PrintAttributes",
            "PrintAttributes.MediaSize.ISO_A5", "asLandscape()", "PrintAttributes.Margins.NO_MARGINS").forEach { assertTrue(it, it in store) }
        assertTrue("pageSetupJson: String? = null" in payload)
        assertTrue("if (pageSetupJson != null) put(\"pageSetup\", pageSetupJson)" in payload)
        assertTrue("ir.exam.app.data.local.PrintPageSetupStore(context).read().toJson()" in dialog)
        assertTrue("PrintPageSetupStore(ctx).read().printAttributes()" in dialog)
        assertTrue("PrintPageSetupStore(appContext).read().printAttributes()" in dialog)
        assertFalse("صفاتِ چاپِ پیش‌فرض (A4 ثابت) برگشته است", "PrintAttributes.Builder().build())" in dialog)
        listOf("private fun PrintPageSetupDialog(", "onPageSetup = { pageSetupOpen = true }", "Text(\"تنظیمات صفحه\"",
            "window.ExamPrintRenderer.setPageSetup(\$json)", "SetupSwitch(\"تکرارِ سرستونِ جدول در هر برگه\"").forEach { assertTrue(it, it in dialog) }
    }
}
