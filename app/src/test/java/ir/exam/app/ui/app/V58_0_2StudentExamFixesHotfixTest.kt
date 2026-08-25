package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * هات‌فیکس V58.0.2 — ۹ گزارش دستگاه پس از V58.x:
 * ۱) هدر «خانه دانش‌آموز» و منوی همبرگری در حین آزمون حذف
 *    (studentExamActive از StudentHomeScreen به پوستهٔ ExamApp).
 * ۲) «خروج» با تأیید از «صفحهٔ آزمون» خارج می‌شود (exitExamScreen)، نه بستن
 *    برنامه؛ پاسخ‌ها در draft می‌مانند و زمان سرور ادامه دارد.
 * ۳) آیکن‌های قبلی/بعدی: AutoMirrored در RTL برعکس رندر می‌شد → غیرآینه‌ای.
 * ۴) دیالوگ کاذب «معلم ویرایش کرد»: deadline در هر refresh با ساعت محلی
 *    دوباره ساخته می‌شود و چند ثانیه جابه‌جاست؛ فقط تغییر بیش از ۲ دقیقه
 *    گزارش می‌شود.
 * ۵) سؤال تکراری بانک: به‌جای خطای قرمز، پیام گذرای Snackbar.
 * ۶) «برخی آزمون‌ها گزارش ندارند»: گزارش پایه در startExam و submit هم
 *    upsert می‌شود (پیش‌تر فقط رویداد امنیتی می‌فرستاد) + متن راهنمای دیالوگ.
 * ۷) long-press علامت مرور کار نمی‌کرد: FilterChip لمس را می‌بلعید؛ چیپ
 *    دست‌ساز Surface با combinedClickable مستقیم.
 * ۸) جای خالی «…………» از نمایش سؤال حذف شد؛ تصویر مثل پنل معلم + فقط
 *    کادرهای تایپ دانش‌آموز (blanks فقط وقتی onBlankAnswer != null).
 * ۹) نمودار پاسخ: اگر متن سؤال توکن نمودار (k='g') دارد، بدون نیاز به چیپ
 *    معلم هم برای دانش‌آموز فعال است.
 */
class V58_0_2StudentExamFixesHotfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }
    private val studentVm by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt") }
    private val home by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentHomeScreen.kt") }
    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val atlasView by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasFigureView.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val gradingScreen by lazy { source("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt") }

    @Test
    fun `shell header and hamburger hide during an active exam`() {
        assertTrue("onExamActiveChanged: (Boolean) -> Unit = {}" in home)
        assertTrue("val examActive = state.exam != null && !state.showPreview && !state.finished" in home)
        assertTrue("var studentExamActive by rememberSaveable(user.id)" in app)
        assertTrue("if (!menuOpen && !(user.role == UserRole.STUDENT && studentExamActive))" in app)
        assertTrue("onExamActiveChanged = { studentExamActive = it }" in app)
    }

    @Test
    fun `exit button leaves the exam screen instead of killing the app`() {
        assertTrue("fun exitExamScreen()" in studentVm)
        assertTrue("onExitExam: () -> Unit = {}" in student)
        assertTrue("onClick={showExit=false;onExitExam()}" in student)
        assertFalse("activity?.finish()" in student)
        assertTrue("onExitExam = viewModel::exitExamScreen" in home)
    }

    @Test
    fun `previous next icons are not auto mirrored anymore`() {
        assertTrue("Icons.Outlined.KeyboardArrowRight" in student)
        assertTrue("Icons.Outlined.KeyboardArrowLeft" in student)
        assertFalse("Icons.AutoMirrored.Outlined.KeyboardArrowRight" in student)
        assertFalse("Icons.AutoMirrored.Outlined.KeyboardArrowLeft" in student)
    }

    @Test
    fun `teacher edit dialog no longer fires from clock drift`() {
        assertTrue("kotlin.math.abs(newDeadline - oldDeadline) > 120_000L" in studentVm)
        assertFalse("if (old.deadlineEpochMs != new.deadlineEpochMs) notes" in studentVm)
    }

    @Test
    fun `duplicate bank question shows a transient notice not a red error`() {
        assertTrue("این سؤال از قبل در بانک سؤال موجود است" in builderVm)
        assertTrue("if (\"از قبل در بانک\" in message)" in builderVm)
    }

    @Test
    fun `monitor report has a baseline for every started exam`() {
        val startExam = studentVm.substringAfter("fun startExam()").substringBefore("fun dismissExamChanges")
        assertTrue("reportMonitor" in startExam)
        val submit = studentVm.substringAfter("fun submit()")
        assertTrue("reportMonitor" in submit)
        assertTrue("گزارش از زمان شرکت دانش‌آموز با نسخهٔ جدید" in gradingScreen)
    }

    @Test
    fun `long press review flag works with a hand made chip`() {
        // چیپ سطر شماره‌ها دیگر FilterChip نیست تا combinedClickable لمس را بگیرد.
        val strip = student.substringAfter("horizontalScroll(rememberScrollState())")
            .substringBefore("IconButton(onClick = onNext")
        assertFalse("FilterChip(" in strip)
        assertTrue("combinedClickable(" in strip)
        assertTrue("onLongClick = { onToggleFlag(q.id) }" in strip)
    }

    @Test
    fun `atlas image renders like the teacher panel with only typing boxes`() {
        assertTrue("onBlankAnswer != null &&" in atlasView)
        // جای خالی نمایشی فقط در مسیر کادر تایپ‌نشدنی (برچسب معلم‌داده) مانده است.
        assertTrue("blankAnswers: Map<Int, String>? = null" in atlasView)
    }

    @Test
    fun `question graphs unlock the student answer graph automatically`() {
        assertTrue("val questionHasGraph = remember(question.id, question.text)" in student)
        assertTrue("it.spec.kind == \"g\"" in student)
        assertTrue("if (presentation.allowAnswerGraph || questionHasGraph)" in student)
    }
}
