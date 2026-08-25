package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V58.0 (پچ ۱ از ۳) — بازطراحی صفحهٔ آزمون دانش‌آموز + تایمر:
 * ۱) رفع همپوشانی نوار بالای پنجرهٔ زوم با جدول تناوبی چرخیده (Column؛ چرخش
 *    در BoxWithConstraints زیر نوار).
 * ۲) حذف هدر/نام آزمون؛ شمارهٔ سؤال‌ها در یک سطر اسکرول‌شونده با آیکن
 *    قبلی/بعدی در دو سر؛ دکمه‌های قبلی/بعدی پایین حذف؛ نوار پایین =
 *    خروج | زمان‌سنج وسط | ارسال نهایی.
 * ۳) تایمر فقط با «شروع پاسخ‌گویی» شروع می‌شود.
 * ۴) ویرایش وسط آزمون توسط معلم → پنجرهٔ موارد + مکث تایمر تا بستن.
 * ۵) پیام «به بانک سؤال اضافه شد» (Snackbar در سازنده).
 * ۶) زمان‌سنج رنگی سبز→نارنجی→قرمز.
 * ۷) نگه‌داشتن ۲ ثانیه‌ای شمارهٔ سؤال = علامت مرور؛ دکمهٔ متنی حذف شد.
 * ۸) کادرهای نامگذاری اطلس در پنل معلم نمایش داده نمی‌شوند.
 */
class V58_0StudentExamUxTimerTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }
    private val studentVm by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt") }
    private val zoomDialog by lazy { source("app/src/main/java/ir/exam/app/ui/figure/ZoomableFigureDialog.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val builderScreen by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val editorAsset by lazy { source("app/src/main/assets/question_editor/question_editor.html") }

    @Test
    fun `zoom dialog toolbar no longer overlaps the rotated periodic table`() {
        // نوار بالا جدا در Column و محتوا در BoxWithConstraints با weight زیر آن
        val afterBar = zoomDialog.substringAfter("TextButton(onClick = onDismiss) { Text(\"بستن ✕\") }")
        assertTrue("BoxWithConstraints(Modifier.fillMaxWidth().weight(1f))" in afterBar)
        // چرخش داخل محدودهٔ زیر نوار انجام می‌شود
        assertTrue("requiredSize(width = maxH, height = maxW)" in afterBar)
    }

    @Test
    fun `exam screen has scrollable question strip and clean bottom bar`() {
        // هدر و عنوان آزمون از «صفحهٔ آزمون» حذف شد (پیش‌نمایش شروع همچنان دارد)
        val content = student.substringAfter("fun StudentExamContent(")
        assertFalse("Text(exam.title, style = MaterialTheme.typography.titleLarge)" in content)
        // سطر اسکرول‌شونده با آیکن قبلی/بعدی دو سر
        assertTrue("horizontalScroll(rememberScrollState())" in student)
        // V58.0.2 — نسخهٔ AutoMirrored در RTL برعکس رندر می‌شد؛ غیرآینه‌ای شد.
        assertTrue("Icons.Outlined.KeyboardArrowRight" in student)
        assertTrue("Icons.Outlined.KeyboardArrowLeft" in student)
        assertFalse("Icons.AutoMirrored.Outlined.KeyboardArrowRight" in student)
        // دکمه‌های متنی قبلی/بعدی پایین حذف شدند
        assertFalse("{ Text(\"قبلی\") }" in student)
        assertFalse("{ Text(\"بعدی\") }" in student)
        // نوار پایین: خروج + زمان‌سنج + ارسال نهایی
        assertTrue("OutlinedButton(onClick = { showExit = true }) { Text(\"خروج\") }" in student)
        assertTrue("ExamCountdownText(" in student)
        // نگه‌داشتن ۲ ثانیه‌ای برای علامت مرور؛ دکمهٔ متنی حذف
        assertTrue("onLongClick = { onToggleFlag(q.id) }" in student)
        assertFalse("Text(\"برداشتن علامت\")" in student)
        assertFalse("OutlinedButton(onClick={onToggleFlag(question.id)})" in student)
    }

    @Test
    fun `timer starts only with the start button and pauses on teacher edits`() {
        // openExam دیگر تایمر را شروع نمی‌کند؛ startExam می‌کند
        val openExam = studentVm.substringAfter("private suspend fun openExam(")
            .substringBefore("private fun observeDrafts(")
        assertFalse("startTimer()" in openExam)
        val startExam = studentVm.substringAfter("fun startExam()").substringBefore("fun dismissExamChanges")
        assertTrue("started = true" in startExam)
        assertTrue("startTimer()" in startExam)
        assertTrue("watchExamChanges()" in startExam)
        // مکث تایمر هنگام پنجرهٔ تغییرات + جبران زمان مکث
        assertTrue("if (state.value.timerPaused)" in studentVm)
        assertTrue("deadline + pausedTotalMs - System.currentTimeMillis()" in studentVm)
        // دیالوگ تغییرات معلم در UI
        assertTrue("آزمون توسط معلم ویرایش شد" in student)
        assertTrue("onDismissExamChanges" in student)
        assertTrue("fun diffExams(old: Exam, new: Exam)" in studentVm)
    }

    @Test
    fun `countdown text turns from green to red near the end`() {
        assertTrue("fun ExamCountdownText(" in student)
        assertTrue("Color(0xFF2E7D32)" in student) // سبز
        assertTrue("Color(0xFFF57C00)" in student) // نارنجی
        assertTrue("Color(0xFFD32F2F)" in student) // قرمز
        assertTrue("remainingSeconds <= 300L || fraction <= .15f" in student)
    }

    @Test
    fun `bank save shows a transient confirmation message`() {
        assertTrue("notice = \"به بانک سؤال اضافه شد\"" in builderVm)
        assertTrue("fun clearNotice()" in builderVm)
        assertTrue("noticeSnackbar.showSnackbar(message)" in builderScreen)
    }

    @Test
    fun `teacher editor hides atlas naming boxes`() {
        assertTrue(".qmf-surface.input .an-af{display:none !important;}" in editorAsset)
        assertTrue("showAtlasBlanks = false" in builderScreen)
    }
}
