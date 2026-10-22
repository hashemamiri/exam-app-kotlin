package ir.exam.app.uitest

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V276 / V333.1 — تست رابط واقعی ساخت و ذخیرهٔ دوبارهٔ آزمون توسط معلم روی شبیه‌ساز Pixel 6 (API 34):
 * ورود با رمز → داک «+» → «آزمون جدید» → عنوان → «+» → «صحیح/غلط» → متن و کلید →
 * ذخیرهٔ اول (یادآوری «آزمون فعلاً بسته است» + کد ۶حرفی) → «ادامهٔ ویرایش» → ذخیرهٔ دوم با همان کد.
 */
@RunWith(AndroidJUnit4::class)
class TeacherExamFlowUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val args get() = InstrumentationRegistry.getArguments()
    private val netTimeout = 60_000L

    private fun waitForText(text: String, substring: Boolean = false, timeout: Long = netTimeout) {
        rule.waitUntil(timeout) {
            rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensureSignedOut() {
        UiScreenshotHelper.hideKeyboard(rule)
        rule.waitForIdle()
        if (rule.onAllNodesWithText("ورود به حساب").fetchSemanticsNodes().isNotEmpty()) return
        rule.waitUntil(15_000) {
            rule.onAllNodesWithContentDescription("منو").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithContentDescription("بازکردن منو").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodesWithContentDescription("منو").fetchSemanticsNodes().isNotEmpty()) {
            rule.onAllNodesWithContentDescription("منو").onFirst().performClick()
        } else {
            rule.onNodeWithContentDescription("بازکردن منو").performClick()
        }
        waitForText("خروج", timeout = 10_000)
        rule.onAllNodesWithText("خروج").onFirst().performClick()
        waitForText("خروج از حساب", timeout = 10_000)
        rule.onAllNodesWithText("خروج").onLast().performClick()
        waitForText("ورود به حساب", timeout = 30_000)
    }

    private fun clickSaveAndConfirm() {
        UiScreenshotHelper.hideKeyboard(rule)
        rule.onNodeWithContentDescription("ذخیره آزمون").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("تأیید و ذخیره").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("ذخیره شد").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodesWithText("تأیید و ذخیره").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("تأیید و ذخیره").performClick()
        }
        waitForText("ذخیره شد", timeout = netTimeout)
    }

    @Test
    fun createSaveAndResaveExamKeepsSameCode() = UiScreenshotHelper.runWithFailureCapture(rule, "teacher_resave_same_code") {
        val user = args.getString("teacherUser").orEmpty()
        val pass = args.getString("teacherPass").orEmpty()
        assumeTrue("teacherUser/teacherPass داده نشده — از اجرای خودکار CI یا پارامتر Instrumentation استفاده کنید", user.isNotBlank() && pass.isNotBlank())

        ensureSignedOut()

        // ورود
        waitForText("ورود به حساب", timeout = 30_000)
        rule.onNodeWithText("ورود به حساب").performClick()
        rule.onNodeWithText("معلم").performClick()
        rule.onNodeWithText("ایمیل یا نام کاربری معلم").performTextInput(user)
        rule.onNodeWithText("رمز عبور").performTextInput(pass)
        UiScreenshotHelper.hideKeyboard(rule)
        rule.onNodeWithText("ورود با رمز عبور").performClick()

        // داشبورد معلم: دکمهٔ «+» داک پایین
        rule.waitUntil(netTimeout) { rule.onAllNodesWithContentDescription("بازکردن افزودن سریع").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("بازکردن افزودن سریع").performClick()
        waitForText("آزمون جدید", timeout = 10_000)
        rule.onNodeWithText("آزمون جدید").performClick()

        // سازندهٔ آزمون
        waitForText("ساخت آزمون", timeout = 15_000)
        waitForText("مشخصات آزمون")
        val title = "UI-TEST ${System.currentTimeMillis() / 1000}"
        rule.onNodeWithText("مشخصات آزمون").performClick()
        waitForText("عنوان آزمون", timeout = 10_000)
        rule.onNodeWithText("عنوان آزمون").performTextInput(title)
        UiScreenshotHelper.hideKeyboard(rule)
        rule.onNodeWithText("بستن مشخصات آزمون").performClick()

        waitForText("+", timeout = 10_000)
        rule.onNodeWithText("+").performClick()
        waitForText("صحیح/غلط", timeout = 10_000)
        rule.onNodeWithText("صحیح/غلط").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("builder-question-text").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithTag("builder-question-text").onLast().performTextInput("آیا زمین گرد است؟")
        UiScreenshotHelper.hideKeyboard(rule)
        rule.onNodeWithText("صحیح").performScrollTo().performClick()

        // ذخیرهٔ اول
        clickSaveAndConfirm()
        rule.onNodeWithText("آزمون فعلاً بسته است", substring = true).assertIsDisplayed()
        val code1 = savedCodeText()
        assertTrue("کد آزمون خالی است", code1.isNotBlank())

        // ادامهٔ ویرایش → ذخیرهٔ دوباره باید همان آزمون را با همان کد به‌روز کند
        rule.onNodeWithText("ادامهٔ ویرایش").performClick()
        waitForText("ویرایش آزمون", timeout = 10_000)
        clickSaveAndConfirm()
        val code2 = savedCodeText()
        assertEquals("ذخیرهٔ دوم آزمون تازه ساخت (کد تغییر کرد)", code1, code2)
        rule.onAllNodesWithText("آزمون فعلاً بسته است", substring = true).fetchSemanticsNodes().let {
            assertTrue("یادآوری «بسته است» فقط برای آزمون تازه باید بیاید", it.isEmpty())
        }
        UiScreenshotHelper.captureStep(
            rule = rule,
            filename = "00_teacher_resave_same_code.png",
            title = "ذخیرهٔ مجدد آزمون با حفظ کد یکتا ($code2)",
            detail = "تأیید یکسان‌ماندن کد آزمون در ذخیرهٔ اول و دوم بدون ساخت آزمون تکراری"
        )
    }

    private fun savedCodeText(): String {
        val nodes = rule.onAllNodesWithText("کد آزمون:", substring = true).fetchSemanticsNodes()
        assertTrue("خط کد آزمون در دیالوگ «ذخیره شد» پیدا نشد", nodes.isNotEmpty())
        return nodes.first().config[SemanticsProperties.Text].joinToString { it.text }
            .substringAfter("کد آزمون:").trim().takeWhile { !it.isWhitespace() }
    }
}
