package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V279 — فیلتر کارنامهٔ اپ: ردیف‌های انتخاب + پنجرهٔ انتخاب به‌جای چیپ‌ها. */
class V279_ReportsPickerTest {
    private fun src(p: String) = File(p).readText()

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
