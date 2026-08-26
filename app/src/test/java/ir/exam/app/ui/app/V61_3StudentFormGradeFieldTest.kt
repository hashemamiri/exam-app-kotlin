package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.3 — فرم‌های دانش‌آموز و پایه/رشته:
 * - در پنجرهٔ ایجاد دانش‌آموز (گروهی)، زیر کادرهای رمز/رمز فعلی دکمه‌ها
 *   وسط‌چین و به ترتیب: چشم، پسر، دختر، تاس.
 * - انتخاب «سایر» در پایه/رشته دیگر فیلد جداگانه باز نمی‌کند: همان فیلد به
 *   حالت تایپ مستقیم می‌رود و مقدار در همان فیلد نمایش داده می‌شود.
 */
class V61_3StudentFormGradeFieldTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val grade by lazy { source("app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt") }

    @Test
    fun `bulk create row is centered eye boy girl dice`() {
        val bulk = school.substringAfter("private fun BulkStudentDialog(")
            .substringBefore("internal fun studentClipboardText")
        val row = bulk.substringAfter("// V61.0 — ترتیب درخواستی وسط‌چین: چشم، پسر، دختر، تاس.")
        assertTrue("Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)" in row)
        val eye = row.indexOf("PasswordVisibilityButton(")
        val boy = row.indexOf("Text(\"پسر\")")
        val girl = row.indexOf("Text(\"دختر\")")
        val dice = row.indexOf("Text(\"🎲\")")
        assertTrue(eye in 0 until boy && boy < girl && girl < dice)
        // چشم هر دو کادر رمز و رمز فعلی را همزمان نشان می‌دهد
        assertTrue(bulk.split("passwordTransformation(row.passwordVisible)").size - 1 == 2)
    }

    @Test
    fun `custom grade or field types directly in the same field`() {
        // حالت سایر: همان فیلد جای خود را به OutlinedTextField می‌دهد (فیلد دوم حذف شد)
        assertTrue("if (customMode) {" in grade)
        val custom = grade.substringAfter("// V61.0 —").substringBefore("} else {")
        assertTrue("OutlinedTextField(" in custom)
        assertTrue("label = { Text(customLabel) }" in custom)
        // آیکن بازکردن دوبارهٔ چرخ داخل همان فیلد
        assertTrue("contentDescription = \"بازکردن انتخاب‌گر \$label\"" in grade)
    }
}
