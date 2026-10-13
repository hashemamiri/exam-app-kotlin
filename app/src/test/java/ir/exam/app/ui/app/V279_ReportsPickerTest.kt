package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V279 — فیلتر کارنامهٔ اپ: ردیف‌های انتخاب + پنجرهٔ انتخاب به‌جای چیپ‌ها. */
class V279_ReportsPickerTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `app reports filter uses PickerRow and PickerDialog`() {
        val s = src("app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt")
        assertTrue("private fun PickerRow(" in s && "private fun PickerDialog(" in s && "data class PickItem(" in s)
        assertTrue("\"نمایش کارنامه‌ها\"" in s && "\"همه\"" in s && "\"هیچ‌کدام\"" in s && "\"تأیید\"" in s)
        assertFalse("FilterStepCard" in s)
        assertFalse("FilterChip" in s)
        val vm = src("app/src/main/java/ir/exam/app/ui/reports/ReportsViewModel.kt")
        assertTrue("fun setStudents(ids: Set<String>)" in vm && "fun setExams(ids: Set<String>)" in vm)
    }
}

/** V279.1 — صفحهٔ تصحیح/مانده/پاسخ عنوان داخلی ندارد؛ هدر برنامه کافی است. */
class V279_1_GradingSingleHeaderTest {
    private fun root(): java.io.File = listOf(java.io.File("."), java.io.File("..")).first { java.io.File(it, "site/src/extras.js").isFile }

    @Test
    fun `grading list has no duplicate heading`() {
        val s = java.io.File(root(), "app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt").readText()
        assertFalse("مانده — پاسخ‌های در انتظار تصحیح" in s)
        assertFalse("Text(heading, style = MaterialTheme.typography.headlineSmall)" in s)
        assertFalse("Text(hint, style = MaterialTheme.typography.bodySmall" in s) // V280: توضیح به هدر رفت
    }
}
