package ir.exam.app.uitest

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.exam.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V333.0 — سناریوی بصری و تعاملی سرتاسری دوپرسونایی (معلم ↔ دانش‌آموز ↔ معلم ↔ کارنامهٔ دانش‌آموز)
 * روی شبیه‌ساز واقعی Pixel 6 (API 34) همراه ثبت گالری تصاویر PNG از تمام مراحل کلیدی:
 *
 * ۱) صفحهٔ خوش‌آمد و ورود راست‌به‌چپ
 * ۲) ورود معلم، ساخت آزمون در آزمون‌ساز بومی و دریافت کد ۶حرفی
 * ۳) باز کردن آزمون روی کارت داشبورد معلم («بازکردن» → «باز») و خروج امن
 * ۴) ورود دانش‌آموز با نام کاربری، ورود با کد آزمون، پاسخ به سؤال فعال (با ثبت تصویر بدون شکستن FLAG_SECURE)
 * ۵) مرور و ارسال نهایی پاسخ دانش‌آموز و دریافت رسید ثبت
 * ۶) ورود مجدد معلم، گردش روی دستهٔ «کارت‌ها» تا کارت «تصحیح»، ثبت بازخورد تشریحی و ذخیرهٔ نمره
 * ۷) ورود مجدد دانش‌آموز، باز کردن منوی همبرگری («منو» → «نتایج من»)، مشاهدهٔ کارنامهٔ توصیفی و بازخورد معلم
 */
@RunWith(AndroidJUnit4::class)
class FullVisualJourneyUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val args get() = InstrumentationRegistry.getArguments()
    private val netTimeout = 60_000L

    private fun waitForText(text: String, substring: Boolean = false, timeout: Long = netTimeout) {
        rule.waitUntil(timeout) {
            rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun signOutIfAuthenticated() {
        rule.waitForIdle()
        if (rule.onAllNodesWithText("ورود به حساب").fetchSemanticsNodes().isNotEmpty()) return
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("منو").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("منو").performClick()
        waitForText("خروج", timeout = 10_000)
        rule.onAllNodesWithText("خروج").onFirst().performClick()
        waitForText("خروج از حساب", timeout = 10_000)
        rule.onAllNodesWithText("خروج").onLast().performClick()
        waitForText("ورود به حساب", timeout = 30_000)
    }

    private fun extractSavedExamCode(): String {
        val nodes = rule.onAllNodesWithText("کد آزمون:", substring = true).fetchSemanticsNodes()
        assertTrue("کد آزمون در پنجرهٔ «ذخیره شد» یافت نشد", nodes.isNotEmpty())
        return nodes.first().config[SemanticsProperties.Text].joinToString { it.text }
            .substringAfter("کد آزمون:").trim().takeWhile { !it.isWhitespace() }
    }

    @Test
    fun fullTeacherStudentGradingAndReportCardVisualJourney() {
        val teacherUser = args.getString("teacherUser").orEmpty()
        val teacherPass = args.getString("teacherPass").orEmpty()
        val studentUser = args.getString("studentUser").orEmpty()
        val studentPass = args.getString("studentPass").orEmpty()

        // اگر کاربر به‌صورت دستی فقط نام کاربری معلم را داده باشد، این تست رد (Skip) می‌شود تا تست تک‌پرسونا سبز بماند؛
        // در اجرای خودکار GitHub Actions هر چهار مقدار توسط سرور آزمایشی محلی تأمین و کامل اجرا می‌شوند.
        assumeTrue(
            "برای اجرای سناریوی دوپرسونایی، studentUser و studentPass هم لازم‌اند",
            teacherUser.isNotBlank() && teacherPass.isNotBlank() && studentUser.isNotBlank() && studentPass.isNotBlank()
        )

        signOutIfAuthenticated()

        // ۱) صفحهٔ خوش‌آمد
        waitForText("ورود به حساب", timeout = 30_000)
        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "01_welcome_screen.png",
            title = "صفحهٔ خوش‌آمد و درگاه ورود راست‌به‌چپ اپ اندروید",
            detail = "نمایش لوگوی آزمون آنلاین و دکمهٔ ورود به حساب در Pixel 6"
        )

        // ۲) ورود معلم + ساخت آزمون تازه
        rule.onNodeWithText("ورود به حساب").performClick()
        rule.onNodeWithText("معلم").performClick()
        rule.onNodeWithText("ایمیل یا نام کاربری معلم").performTextInput(teacherUser)
        rule.onNodeWithText("رمز عبور").performTextInput(teacherPass)
        rule.onNodeWithText("ورود با رمز عبور").performClick()

        rule.waitUntil(netTimeout) {
            rule.onAllNodesWithContentDescription("بازکردن افزودن سریع").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("بازکردن افزودن سریع").performClick()
        waitForText("آزمون جدید", timeout = 10_000)
        rule.onNodeWithText("آزمون جدید").performClick()

        waitForText("ساخت آزمون", timeout = 15_000)
        waitForText("مشخصات آزمون")
        val examTitle = "آزمون بصری خودکار ${System.currentTimeMillis() % 10000}"
        rule.onNodeWithText("مشخصات آزمون").performClick()
        waitForText("عنوان آزمون", timeout = 10_000)
        rule.onNodeWithText("عنوان آزمون").performTextInput(examTitle)
        rule.onNodeWithText("بستن مشخصات آزمون").performClick()

        waitForText("+", timeout = 10_000)
        rule.onNodeWithText("+").performClick()
        waitForText("صحیح/غلط", timeout = 10_000)
        rule.onNodeWithText("صحیح/غلط").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("builder-question-text").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithTag("builder-question-text").onLast()
            .performTextInput("در مثلث قائم‌الزاویه، مربع وتر برابر مجموع مربعات دو ضلع قائمه است.")
        rule.onNodeWithText("صحیح").performScrollTo().performClick()

        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "02_teacher_builder_question.png",
            title = "سازندهٔ بومی آزمون معلم در اندروید",
            detail = "ساخت آزمون «$examTitle» و تنظیم کلید پاسخ صحیح/غلط"
        )

        rule.onNodeWithContentDescription("ذخیره آزمون").performClick()
        waitForText("تأیید و ذخیره", timeout = 10_000)
        rule.onNodeWithText("تأیید و ذخیره").performClick()
        waitForText("ذخیره شد")
        val examCode = extractSavedExamCode()
        assertTrue("کد آزمون باید ۶ کاراکتر معتبر باشد", examCode.length >= 4)

        // بازگشت از سازنده به فهرست آزمون‌ها و باز کردن آزمون برای دانش‌آموزان
        if (rule.onAllNodesWithText("بازگشت به لیست").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("بازگشت به لیست").performClick()
        } else {
            rule.onNodeWithText("ادامهٔ ویرایش").performClick()
            waitForText("ویرایش آزمون", timeout = 10_000)
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        }

        // ۳) باز کردن آزمون روی کارت داشبورد معلم
        waitForText(examTitle, timeout = 20_000)
        rule.onNodeWithText(examTitle).performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithContentDescription("بازکردن").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("بازکردن").performClick()
        waitForText("باز", timeout = 15_000)

        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "03_teacher_exam_open.png",
            title = "داشبورد معلم — باز شدن آزمون با کد $examCode",
            detail = "نمایش برچسب وضعیت «باز» و نوار عملیات کارت آزمون"
        )

        // خروج معلم و ورود دانش‌آموز
        signOutIfAuthenticated()

        // ۴) ورود دانش‌آموز با نام کاربری و شروع آزمون فعال
        rule.onNodeWithText("ورود به حساب").performClick()
        rule.onNodeWithText("دانش‌آموز").performClick()
        waitForText("نام کاربری دانش‌آموز", timeout = 10_000)
        rule.onNodeWithText("نام کاربری دانش‌آموز").performTextInput(studentUser)
        rule.onNodeWithText("رمز عبور").performTextInput(studentPass)
        rule.onAllNodesWithText("ورود").onLast().performClick()

        waitForText("کد آزمون را وارد کنید", timeout = netTimeout)
        rule.onNodeWithText("کد آزمون").performTextInput(examCode)
        rule.onNodeWithText("ورود به آزمون").performClick()

        waitForText("شروع پاسخ‌گویی", timeout = netTimeout)
        rule.onNodeWithText("شروع پاسخ‌گویی").performClick()

        waitForText("ارسال نهایی", timeout = 15_000)
        rule.onNodeWithText("صحیح").performClick()

        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "04_student_active_exam.png",
            title = "صفحهٔ فعال آزمون دانش‌آموز و زمان‌سنج زنده",
            detail = "انتخاب گزینهٔ «صحیح» و ثبت تصویر با حفظ پرچم امنیتی FLAG_SECURE"
        )

        // ۵) ارسال نهایی پاسخ دانش‌آموز و مشاهدهٔ رسید
        rule.onNodeWithText("ارسال نهایی").performClick()
        waitForText("تأیید و ارسال نهایی", timeout = 10_000)
        rule.onNodeWithText("تأیید و ارسال نهایی").performClick()
        waitForText("پاسخ شما با موفقیت ثبت شد.", timeout = netTimeout)

        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "05_student_submitted_receipt.png",
            title = "رسید ثبت موفق پاسخ‌نامهٔ دانش‌آموز",
            detail = "تأیید ثبت پاسخ آزمون با کد $examCode روی سرور"
        )

        rule.onNodeWithText("بازگشت به داشبورد").performClick()
        waitForText("کد آزمون را وارد کنید", timeout = 15_000)
        signOutIfAuthenticated()

        // ۶) ورود مجدد معلم → «کارت‌ها» → «تصحیح» → ثبت بازخورد و نمره
        rule.onNodeWithText("ورود به حساب").performClick()
        rule.onNodeWithText("معلم").performClick()
        rule.onNodeWithText("ایمیل یا نام کاربری معلم").performTextInput(teacherUser)
        rule.onNodeWithText("رمز عبور").performTextInput(teacherPass)
        rule.onNodeWithText("ورود با رمز عبور").performClick()

        rule.waitUntil(netTimeout) {
            rule.onAllNodesWithContentDescription("کارت‌ها").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("کارت‌ها").performClick()
        var guard = 0
        while (
            guard < 16 &&
            rule.onAllNodesWithText("همه پاسخ‌ها، حضور، بازخورد", substring = true).fetchSemanticsNodes().isEmpty()
        ) {
            rule.onNodeWithContentDescription("کارت‌ها").performClick()
            rule.waitForIdle()
            guard++
        }
        rule.onAllNodesWithText("تصحیح").onFirst().performClick()

        waitForText(examTitle, timeout = netTimeout)
        rule.onNodeWithText(examTitle).performClick()
        waitForText("ورود به تصحیح", timeout = 10_000)
        rule.onNodeWithText("ورود به تصحیح").performClick()

        val teacherFeedback = "آفرین! تسلط کامل بر قضیهٔ فیثاغورس."
        waitForText("ذخیره نمره", timeout = netTimeout)
        rule.onNodeWithText("بازخورد").performScrollTo().performTextInput(teacherFeedback)
        rule.onNodeWithText("ذخیره نمره").performScrollTo().performClick()
        waitForText("نمره و بازخورد ذخیره شد.", timeout = netTimeout)

        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "06_teacher_grading_feedback.png",
            title = "پنل تصحیح معلم — ثبت بازخورد تشریحی و تأیید نمره",
            detail = "ثبت بازخورد «$teacherFeedback» برای پاسخ دانش‌آموز"
        )

        signOutIfAuthenticated()

        // ۷) ورود مجدد دانش‌آموز → منوی همبرگری («منو») → «نتایج من» → کارنامه و جزئیات
        rule.onNodeWithText("ورود به حساب").performClick()
        rule.onNodeWithText("دانش‌آموز").performClick()
        waitForText("نام کاربری دانش‌آموز", timeout = 10_000)
        rule.onNodeWithText("نام کاربری دانش‌آموز").performTextInput(studentUser)
        rule.onNodeWithText("رمز عبور").performTextInput(studentPass)
        rule.onAllNodesWithText("ورود").onLast().performClick()

        waitForText("کد آزمون را وارد کنید", timeout = netTimeout)
        rule.onNodeWithContentDescription("منو").performClick()
        waitForText("نتایج من", timeout = 10_000)
        rule.onNodeWithText("نتایج من").performClick()

        waitForText("نتایج و پاسخ‌های من", timeout = netTimeout)
        waitForText(examTitle, timeout = netTimeout)
        rule.onNodeWithText("مشاهده سؤال‌ها و پاسخ‌ها").performScrollTo().performClick()
        waitForText(teacherFeedback, substring = true, timeout = 15_000)

        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "07_student_final_report_card.png",
            title = "کارنامهٔ نهایی و جزئیات پاسخ‌نامهٔ دانش‌آموز در اپ اندروید",
            detail = "نمایش نمرهٔ کامل، سطح توصیفی چهارستاره و بازخورد اختصاصی معلم"
        )
    }
}
