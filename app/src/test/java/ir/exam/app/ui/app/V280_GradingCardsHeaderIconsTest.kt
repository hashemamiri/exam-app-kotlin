package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V280 — توضیح جلوی هدر تصحیح/مانده/پاسخ در یک ردیف؛ کارت‌های آزمون بدون دکمه تا لمس؛ آیکن‌های متمایز کارت‌های معلم. */
class V280_GradingCardsHeaderIconsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `grading hint sits in header row and cards expand on tap`() {
        val app = src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")
        assertTrue("gradingHint = if (page == MainPage.GRADING)" in app)
        assertTrue("if (page == MainPage.GRADING && gradingHint != null) Text(gradingHint, style = MaterialTheme.typography.bodySmall" in app)
        val g = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue("var expandedCardId by remember { mutableStateOf<String?>(null) }" in g)
        assertTrue("clickable { expandedCardId = if (expanded) null else item.id }" in g)
        assertTrue("if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {" in g)
        assertFalse("وضعیت تصحیح آزمون‌ها" in g)
    }

    @Test
    fun `teacher management cards have distinct icons`() {
        val c = src("app/src/main/java/ir/exam/app/ui/app/TeacherManagementCardsScreen.kt")
        for (ic in listOf("ReportCard", "QuestionBank", "Pending", "Answers", "Requests")) assertTrue(ic, "Design69Icons.$ic," in c)
        val teacher = c.substringBefore("Design69Icons.SchoolAdd")
        assertTrue(teacher.split("Design69Icons.Reports,").size == 2) // فقط «آمار»
        assertTrue(teacher.split("Design69Icons.Grading,").size == 2) // فقط «تصحیح»
        val i = src("app/src/main/java/ir/exam/app/ui/app/Design69Icons.kt")
        for (ic in listOf("ReportCard", "QuestionBank", "Pending", "Answers", "Requests")) assertTrue(ic, "val $ic: ImageVector by lazy" in i)
    }
}
