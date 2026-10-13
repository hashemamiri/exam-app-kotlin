package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V277 — کارنامه: فیلتر پلکانی مدرسه→کلاس→دانش‌آموز→آزمون + کارت تمیز با آیکن کارنامه؛ تصحیح سؤال‌محور مرتب. */
class V277_ReportsFilterAndGradingLayoutTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `reports grades page has stepwise filter and single report card`() {
        val scr = src("app/src/main/java/ir/exam/app/ui/reports/ReportsScreen.kt")
        assertFalse("دو هدر", "Text(if (gradesOnly) \"کارنامه و لیست نمرات\"" in scr)
        /* V279: ردیف‌های انتخاب (PickerRow) جای کارت‌های چیپی */
        assertTrue("PickerRow(\"\${PersianDigits.convert(n++)}. مدرسه\"" in scr)
        assertTrue("PickerRow(\"\${PersianDigits.convert(n++)}. کلاس\"" in scr)
        assertTrue(". دانش‌آموزان\"" in scr && ". آزمون‌ها\"" in scr && "Text(\"نمایش کارنامه‌ها\")" in scr)
        assertTrue("Icon(Icons.Outlined.Description, contentDescription = \"کارنامهٔ \${row.studentName}\"" in scr)
        assertTrue("private fun StudentReportCardDialog(" in scr)
        assertFalse("ریز نمرات روی کارت", "selectedExams.joinToString(\"   \")" in scr)
        val vm = src("app/src/main/java/ir/exam/app/ui/reports/ReportsViewModel.kt")
        assertTrue("native_teacher_schools_v61" in vm && "native_teacher_school_classes_v61" in vm)
        assertTrue("fun toggleStudent(" in vm && "fun setAllStudents(" in vm && "fun setAllExams(" in vm && "fun showReports()" in vm)
        assertTrue("val maxScores: Map<String, Double> = emptyMap()" in src("app/src/main/java/ir/exam/app/domain/model/GradingModels.kt"))
    }

    @Test
    fun `question centric grading layout`() {
        val g = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue("FilledTonalIconButton(onClick = { viewModel.moveQuestion(-1) }, enabled = index > 0)" in g)
        assertTrue("Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = \"سؤال قبل\"" in g)
        assertTrue("Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = \"سؤال بعد\"" in g)
        assertTrue("Text(\"ثبت نمره‌های این سؤال برای همه\")" in g)
        assertFalse("Text(\"ثبت اتمیک همه نمره‌ها\")" in g)
        assertTrue("modifier = Modifier.width(112.dp)" in g)
    }
}
