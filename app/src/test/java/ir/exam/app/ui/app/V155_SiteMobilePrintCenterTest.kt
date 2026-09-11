package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V155 — بدون زوم/کش‌آمدن در گوشی؛ «چاپ آزمون» مثل ExamPrintCenterScreen؛ کارت‌های پاستلی سؤال با ویرایشگر بازشونده مثل QuestionEditor. */
class V155_SiteMobilePrintCenterTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `viewport disables zoom and print center mirrors app`() {
        assertTrue("maximum-scale=1.0, user-scalable=no" in source("site/src/template.html"))
        val m = source("site/src/mobile.js")
        assertTrue("function printCenter(c)" in m && "text: 'آزمون جدید'" in m && "text: 'آزمون‌های آنلاین'" in m && "sourceExamId: x.id" in m)
        assertTrue("'هنوز آزمون چاپی‌ای نیست. «آزمون جدید» بزنید یا از «آزمون‌های آنلاین» نسخهٔ چاپی بسازید.'" in m && "'در حال آماده‌سازی نسخهٔ چاپی...'" in m)
        assertTrue("function placeEditor()" in m && "on.after(edRef)" in m)
        val css = source("site/src/site.css")
        assertTrue("overscroll-behavior:none;touch-action:pan-x pan-y" in css && ".m-mode input,.m-mode select,.m-mode textarea{font-size:16px}" in css && ".m-builder .b-editor.m-open{" in css)
    }
}
