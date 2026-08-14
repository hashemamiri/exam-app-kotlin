package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V21StudentBuilderPolishTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").isFile
    }

    @Test
    fun `student list toolbar order and animated search are exact`() {
        val school = File(root(), "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val students = school.substringAfter("private fun StudentsContent(")
            .substringBefore("private fun StudentCard(")
        assertFalse("new single-account button remains", "حساب جدید" in students)
        val excel = students.indexOf("Text(\"Excel\")")
        val bulk = students.indexOf("contentDescription = \"افزودن گروهی دانش‌آموز\"")
        val search = students.indexOf("Icons.Outlined.Search")
        assertTrue(excel >= 0 && bulk > excel && search > bulk)
        assertTrue("AnimatedVisibility" in students)
        assertTrue("Icons.Outlined.Close" in students)
        assertTrue("searchOpen = false" in students)
        assertTrue("horizontalArrangement = Arrangement.Center" in students)
    }

    @Test
    fun `bulk controls are at top without old title`() {
        val school = File(root(), "app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt").readText()
        val bulk = school.substringAfter("private fun BulkStudentDialog(")
            .substringBefore("private fun studentWorkbook")
        assertFalse("table/title button remains", "Text(\"▦\"" in bulk)
        assertFalse("old bulk title remains", "افزودن گروهی دانش‌آموز" in bulk)
        val plus = bulk.indexOf("contentDescription = \"ردیف جدید\"")
        val create = bulk.indexOf("Text(\"ایجاد\")")
        val cancel = bulk.indexOf("contentDescription = \"انصراف\"")
        val cards = bulk.indexOf("rememberLazyListState()")
        // زیر دکمه‌ها فقط لیست شمارهٔ کارت‌ها می‌آید؛ کلاس‌ها حذف شده‌اند.
        assertTrue(plus >= 0 && create > plus && cancel > create && cards > cancel)
        assertFalse("classes still shown", "classes.forEach" in bulk)
        assertTrue("Color(0xFF25A86B)" in bulk)
        assertTrue("Color(0xFFE5484D)" in bulk)
    }

    @Test
    fun `builder waits for layout then aligns question below header`() {
        val builder = File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        assertTrue("suspend fun scrollQuestionToHeader" in builder)
        assertTrue(builder.split("withFrameNanos").size - 1 >= 2)
        assertTrue("animateScrollToItem(questionPrefaceCount + questionIndex, 0)" in builder)
        assertTrue("scope.launch { scrollQuestionToHeader(index) }" in builder)
        assertTrue("Alignment.CenterStart" in builder && "Alignment.CenterEnd" in builder)
    }

    @Test
    fun `formula predictive scroll happens earlier`() {
        val formula = File(root(), "app/src/main/java/ir/exam/app/ui/math/NativeFormulaView.kt").readText()
        assertTrue("viewport.width * .14f" in formula)
        assertTrue("viewport.width * .62f" in formula)
        assertTrue("viewport.height * .12f" in formula)
        assertTrue("viewport.height * .68f" in formula)
    }
}
