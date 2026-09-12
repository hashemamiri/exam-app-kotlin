package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V180 — سایت: هر چاپ (آنلاین/چاپی/محلی) مثل PrintCostConfirmDialog اپ اول پنجرهٔ هزینه را نشان می‌دهد و پنجره بالای موتور چاپ است. */
class V180_SitePrintCostDialogTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: File("")
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `print bridge always asks for cost like the app`() {
        val a = source("site/src/app.js")
        assertFalse("if (!printCtx.examId) { doNative(); return; }" in a)
        assertTrue("var examRef = printCtx.examId || 'local';" in a)
        assertTrue("api.chargePrint(examRef, n, mode)" in a)
        assertTrue("'مبلغ قابل کسر از کیف پول: '" in a && "'پرداخت و چاپ'" in a)
        assertTrue("class: 'modal-bg' + (document.querySelector('.engine-bg') ? ' over-engine' : '')" in a)
        assertTrue(".modal-bg.over-engine{z-index:65}" in source("site/src/site.css"))
        val b = source("site/src/builder.js")
        assertTrue("examId: state.mode === 'online' ? state.examId : (state.printId || 'local')" in b)
        assertTrue("{title: full.title, examId: r.id}" in b)
        /* همان مقادیر اپ */
        val k = source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt")
        assertTrue("const val PRINT_COST_PER_QUESTION_TOMAN = 1000L" in k && "Text(\"پرداخت و چاپ\")" in k)
    }
}
