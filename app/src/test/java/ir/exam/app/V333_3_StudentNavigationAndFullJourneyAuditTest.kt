package ir.exam.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V333.3 — قفل رگرسیون برای یافته‌های اجرای زندهٔ V333.2 روی Pixel 6 (API 34):
 * ۱) در ExamApp دانش‌آموز پس از ورود ابتدا در «تقویم» قرار می‌گیرد؛ در FullVisualJourneyUiTest
 *    به‌جای انتظار مستقیم برای «کد آزمون را وارد کنید» بلافاصله پس از ورود، در مرحلهٔ ۴ از
 *    «منو ← آزمون ← پیوستن به آزمون ← کد آزمون ← جست‌وجوی آزمون» و در مرحلهٔ ۷ از «منو ← نتایج من» استفاده می‌شود.
 * ۲) در StudentExamScreen کل کارت گزینه‌های چندگزینه‌ای قابل‌لمس (`.clickable`) است.
 * ۳) در مرحلهٔ ۷ ابتدا بارگذاری «میانگین کل» تأیید و فهرست LazyColumn تا «مشاهده سؤال‌ها و پاسخ‌ها» اسکرول می‌شود
 *    و سپس «تصحیح‌شده» و بازخورد معلم بررسی می‌گردند.
 */
class V333_3_StudentNavigationAndFullJourneyAuditTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".").canonicalFile
        return if (File(cwd, "scripts/mock_supabase_server.py").exists()) cwd else cwd.parentFile
    }

    @Test
    fun studentPostLoginRoutesUseMenuInsteadOfAssumingStudentHomeImmediately() {
        val root = repoRoot()
        val examApp = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        val journey = File(root, "app/src/androidTest/java/ir/exam/app/uitest/FullVisualJourneyUiTest.kt").readText()

        // ۱) منبع حقیقت در ExamApp: صفحهٔ پیش‌فرض غیرمدیر «MainPage.CALENDAR» و پنجرهٔ «پیوستن به آزمون» با «جست‌وجوی آزمون» است
        assertTrue(
            "ExamApp must define student join dialog with title «پیوستن به آزمون» and icon «جست‌وجوی آزمون»",
            examApp.contains("title = { Text(\"پیوستن به آزمون\") }") &&
                examApp.contains("contentDescription = \"جست‌وجوی آزمون\"")
        )

        // ۲) در FullVisualJourneyUiTest بلافاصله پس از کلیک دکمهٔ «ورود» دانش‌آموز دیگر نباید waitForText("کد آزمون را وارد کنید") بیاید
        assertFalse(
            "FullVisualJourneyUiTest must not wait for StudentHomeScreen immediately after student login when default page is Calendar",
            Regex("""clickDialogOrLastText\(rule,\s*"ورود"\)\s+waitForText\("کد آزمون را وارد کنید"""").containsMatchIn(journey)
        )

        // ۳) مرحلهٔ ۴ باید دقیقاً مسیر منو ← آزمون ← پیوستن به آزمون ← کد آزمون ← جست‌وجوی آزمون را طی کند
        assertTrue(
            "Step 4 must open menu, tap «آزمون», wait for «پیوستن به آزمون», enter examCode, and tap «جست‌وجوی آزمون»",
            journey.contains("waitForText(\"پیوستن به آزمون\"") &&
                journey.contains("rule.onNodeWithContentDescription(\"جست‌وجوی آزمون\").performClick()")
        )

        // ۴) فقط یک بار و منحصراً پس از «بازگشت به داشبورد» (جایی که page == MainPage.HOME است) باید «کد آزمون را وارد کنید» بررسی شود
        val occurrences = Regex("""waitForText\("کد آزمون را وارد کنید"""").findAll(journey).count()
        assertEquals(
            "«کد آزمون را وارد کنید» should only be checked right after «بازگشت به داشبورد» in Step 5",
            1,
            occurrences
        )
    }

    @Test
    fun multipleChoiceOptionCardIsFullSurfaceClickableAndResultsScrollBeforeoffscreenAnswerCard() {
        val root = repoRoot()
        val studentExam = File(root, "app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt").readText()
        val journey = File(root, "app/src/androidTest/java/ir/exam/app/uitest/FullVisualJourneyUiTest.kt").readText()

        assertTrue(
            "Non-primary multiple-choice Card in StudentExamScreen must be clickable across the entire card surface",
            studentExam.contains(".clickable { onAnswer(ChoiceAnswer(question.id, originalIndex)) }")
        )

        val scrollIdx = journey.indexOf("scrollUntilTextVisible(rule, \"مشاهده سؤال‌ها و پاسخ‌ها\")")
        val gradedIdx = journey.indexOf("waitForText(\"تصحیح‌شده\"")
        assertTrue("Step 7 must scroll LazyColumn to «مشاهده سؤال‌ها و پاسخ‌ها» before asserting «تصحیح‌شده»", scrollIdx in 1..<gradedIdx)
    }
}
