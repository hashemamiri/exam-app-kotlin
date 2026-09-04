package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V80.0 — چرا کلیک روی آیکن فرمول هیچ پنجره‌ای باز نمی‌کرد.
 *
 * V79.0 ویرایشگر فرمول را از doc.write به بارگذاریِ هم‌مبدأ با src برد؛ آن بخش
 * درست بود. اما `onPageFinished` در WebView برای **هر فریم** صدا زده می‌شود،
 * نه فقط فریمِ اصلی. پس به‌محض اینکه iframe ویرایشگر لود می‌شد، این متد دوباره
 * شلیک می‌کرد و در حالت «آزمون‌ساز چاپی» (printable == null) دوباره
 * `setExamData({reset:true})` را می‌فرستاد که می‌کند:
 *     questions = []; qIdCounter = 0; renderAll();
 * یعنی سؤالِ کاربر پاک و کلِ صفحه باز-رندر می‌شد و کادری که ویرایشگر به آن
 * وصل شده بود از DOM جدا می‌ماند — ویرایشگر عملاً باز نمی‌شد.
 *
 * رفع در دو لایه: (۱) گاردِ فریمِ اصلی در Kotlin، (۲) گاردِ «کارِ در جریان»
 * در خودِ setExamData تا هیچ ریستِ سرگردانی نتواند کار کاربر را پاک کند.
 */
class V80_0FormulaFrameResetGuardTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val assetText by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `onPageFinished only reacts to the main document`() {
        assertTrue("ثابت نشانی سند اصلی نیست", "internal const val MAIN_PAGE_URL" in dialog)
        assertTrue("گارد فریم اصلی نیست", "if (url != MAIN_PAGE_URL) return" in dialog)
        // همان ثابت باید برای بارگذاری هم استفاده شود تا هرگز از هم جدا نیفتند
        assertTrue("loadUrl(MAIN_PAGE_URL)" in dialog)
        assertFalse(
            "نشانی نباید دوباره hardcode شود",
            "loadUrl(\"https://exam-print.local/print/exam_print.html\")" in dialog
        )
    }

    @Test
    fun `the guard sits before the reset injection`() {
        val guard = dialog.indexOf("if (url != MAIN_PAGE_URL) return")
        val reset = dialog.indexOf("ExamHtmlPrintPayloadBuilder.build(printable)")
        assertTrue("گارد پیدا نشد", guard > 0)
        assertTrue("تزریق پیدا نشد", reset > 0)
        assertTrue("گارد باید پیش از تزریق باشد", guard < reset)
    }

    @Test
    fun `setExamData refuses a stray reset while work is in progress`() {
        assertTrue("data.reset && !data.force" in assetText)
        assertTrue("questions.length > 0" in assetText)
        assertTrue("qmfAnyToolOpen" in assetText)
    }

    @Test
    fun `a real exam import is never blocked by the guard`() {
        // ورودِ آزمونِ واقعی reset:false دارد، پس هرگز به گارد نمی‌خورد
        val payload = File(
            root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt"
        ).readText()
        assertTrue("put(\"reset\", true)" in payload)
        assertTrue("put(\"reset\", false)" in payload)
        // و مسیر بازیابیِ پیش‌نویس V78.2 دست‌نخورده است
        assertTrue("__qmfHasLocalDraft" in dialog)
        assertTrue("ExamDraftMirror.load" in dialog)
    }

    @Test
    fun `the V79 same-origin formula load is still in place`() {
        assertTrue("f.src = MATH_EDITOR_URL" in assetText)
        assertTrue("MATH_EDITOR_URL = \"/print/math_editor.html\"" in assetText)
        assertFalse("doc.write(" in assetText)
        assertTrue(File(root(), "app/src/main/assets/print/math_editor.html").isFile)
    }

    @Test
    fun `all eight tools go through the native bridge`() {
        // V82.0 — فرمول هم به پل بومی پیوست؛ استثنا حذف شد.
        assertFalse("activeExactTool !== 'formula'" in assetText)
        assertTrue("ExamPrintNative.openFigureTool" in assetText)
        assertTrue("is-fx formula-btn" in assetText)
    }
}
