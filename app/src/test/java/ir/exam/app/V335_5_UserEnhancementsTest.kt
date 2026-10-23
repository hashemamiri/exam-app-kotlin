package ir.exam.app

import ir.exam.app.ui.builder.OcrQuestionParser
import ir.exam.app.ui.builder.QuestionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.4 (1.02.89) — آزمون‌های پوشش ۶ قابلیت جدید درخواستی کاربر:
 * ۱. فیلدهای پویای چندگانه برای پاسخ‌های قابل‌قبول در سؤالات پاسخ کوتاه (با دکمهٔ +)
 * ۲. حذف یادداشت مجوز قلم‌های فارسی از صفحه تنظیمات پروفایل
 * ۳. انتقال دکمهٔ تغییر تم روشن/تیره به هدر تقویم کنار اعلان‌ها
 * ۴. ابزار تصویر در منوی + گزینه‌های سؤالات جورکردنی وب
 * ۵. نمایش کلمهٔ «بارم» به‌جای «∑» روی کارت‌های آزمون
 * ۶. حذف OCR از استودیوی تصویر و قرار دادن ابزار اختصاصی OCR کنار وایت‌برد برای استخراج، ویرایش و افزودن تست‌ها
 */
class V335_5_UserEnhancementsTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(relPath: String): String = File(root(), relPath).readText(Charsets.UTF_8)

    @Test
    fun feature1_fillBlankMultiInputEditorInstalled() {
        val builderScreen = read("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("FillBlankAcceptableAnswersEditor در اندروید پیاده‌سازی شده است", "FillBlankAcceptableAnswersEditor(question, viewModel)" in builderScreen)
        assertTrue("فیلدها با علامت | ترکیب می‌شوند", "joinToString(\"|\")" in builderScreen)
        assertTrue("دکمهٔ افزودن فیلد پاسخ دیگر", "افزودن پاسخ دیگر (+)" in builderScreen)

        val builderJs = read("site/src/builder.js")
        assertTrue("کلاس b-fill-answers در وب پیاده‌سازی شده است", "b-fill-answers" in builderJs)
        assertTrue("ترکیب پاسخ‌ها با | در وب", "answers.join('|')" in builderJs)
        assertTrue("دکمهٔ افزودن پاسخ دیگر در وب", "➕ افزودن پاسخ دیگر" in builderJs)
    }

    @Test
    fun feature2_fontLicenseNoteRemovedFromProfileSettings() {
        val profileScreen = read("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertFalse("یادداشت مجوز سه قلم فارسی حذف شده است", "سه قلم فارسی همراه برنامه و با مجوز OFL ذخیره شده‌اند." in profileScreen)
    }

    @Test
    fun feature3_themeToggleMovedToCalendarHeaderNextToNotifications() {
        val menuScreen = read("app/src/main/java/ir/exam/app/ui/app/Design69MainMenuScreen.kt")
        val appScreen = read("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt")

        // در کارت پروفایل منوی کشویی دیگر دکمهٔ تم وجود ندارد
        assertFalse("دکمه تغییر تم از کارت پروفایل منو حذف شده است", "Icons.ChevronLeft" in menuScreen && "if (darkTheme) \"☀️\" else \"🌙\"" in menuScreen)

        // در هدر تقویم و کنار زنگولهٔ اعلان‌ها قرار دارد
        assertTrue("کلید تغییر تم در actions کنار زنگولهٔ اعلان‌ها است", "toggleTheme" in appScreen && "NotificationBell" in appScreen)
        assertTrue("نمایش آیکن ماه یا خورشید در نوار بالا", "if (darkActive) \"☀️\" else \"🌙\"" in appScreen)
    }

    @Test
    fun feature4_matchingQuestionOptionsHaveImageToolInWebBuilder() {
        val builderJs = read("site/src/builder.js")
        assertTrue("تصویر مورد راست در منوی درج", "image: pickLImg, hasImage: !!lImg" in builderJs)
        assertTrue("تصویر مورد چپ در منوی درج", "image: pickRImg, hasImage: !!rImg" in builderJs)
        assertTrue("بندانگشتی تصویر مورد راست", "تصویر مورد راست — برای حذف کلیک کنید" in builderJs)
        assertTrue("بندانگشتی تصویر مورد چپ", "تصویر مورد چپ — برای حذف کلیک کنید" in builderJs)
    }

    @Test
    fun feature5_examCardsShowBaremInsteadOfSigma() {
        val appJs = read("site/src/app.js")
        assertTrue("روی کارت آزمون کلمهٔ بارم درج شده است", "'بارم ' + fa(fmtScore(x.total_score))" in appJs)
        assertFalse("علامت سیگما روی کارت آزمون نیست", "'∑ ' + fa(fmtScore(x.total_score))" in appJs)
    }

    @Test
    fun feature6_ocrRemovedFromStudioAndStandaloneNextToWhiteboard() {
        val studio = read("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt")
        assertFalse("OCR از تب استودیو حذف شده است", "🔎 استخراج متن (OCR فارسی)" in studio)
        assertFalse("عنوان تب استودیو OCR ندارد", "✨ بهبود و OCR" in studio)
        assertTrue("عنوان تب بهبود در استودیو باقی مانده است", "✨ بهبود" in studio)

        val mediaEditor = read("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        assertTrue("پارامتر onOpenOcr کنار تختهٔ سفید وجود دارد", "onOpenOcr: (() -> Unit)? = null" in mediaEditor)
        assertTrue("آیکن اسکنر اسناد برای OCR", "Icons.Outlined.DocumentScanner" in mediaEditor)

        val builderScreen = read("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        assertTrue("اجرای OCR روی تصویر", "ExamImageOcr.recognize(context, bound)" in builderScreen)
        assertTrue("پنجرهٔ بررسی و ویرایش OCR", "OcrQuestionReviewDialog" in builderScreen)
        assertTrue("افزودن سؤالات OCR به آزمون", "viewModel.addOcrQuestions(parsed, question.id)" in builderScreen)
    }

    @Test
    fun feature6_ocrQuestionParserParsesMultipleChoiceAndEssayTests() {
        val rawInput = """
            ۱- کدام گزینه پایتخت ایران است؟
            الف) اصفهان
            ب) شیراز
            ج) تهران
            د) تبریز

            ۲. کدام رابطه درست است؟
            ۱) ۲ + ۲ = ۴
            ۲) ۱ + ۱ = ۳
            ۳) ۳ + ۳ = ۷
            ۴) ۵ + ۵ = ۱۱

            سؤال ۳: قانون اول نیوتون را توضیح دهید.
        """.trimIndent()

        val parsed = OcrQuestionParser.parse(rawInput)
        assertEquals("باید ۳ سؤال تفکیک شود", 3, parsed.size)

        // سؤال اول: چهارگزینه‌ای با الف، ب، ج، د
        val q1 = parsed[0]
        assertEquals(QuestionType.MULTIPLE_CHOICE, q1.type)
        assertTrue("صورت سؤال ۱", q1.text.contains("کدام گزینه پایتخت ایران است؟"))
        assertEquals(4, q1.options.size)
        assertEquals("اصفهان", q1.options[0])
        assertEquals("شیراز", q1.options[1])
        assertEquals("تهران", q1.options[2])
        assertEquals("تبریز", q1.options[3])

        // سؤال دوم: چهارگزینه‌ای با ۱، ۲، ۳، ۴
        val q2 = parsed[1]
        assertEquals(QuestionType.MULTIPLE_CHOICE, q2.type)
        assertTrue("صورت سؤال ۲", q2.text.contains("کدام رابطه درست است؟"))
        assertEquals(4, q2.options.size)
        assertEquals("۲ + ۲ = ۴", q2.options[0])
        assertEquals("۱ + ۱ = ۳", q2.options[1])
        assertEquals("۳ + ۳ = ۷", q2.options[2])
        assertEquals("۵ + ۵ = ۱۱", q2.options[3])

        // سؤال سوم: تشریحی بدون گزینه
        val q3 = parsed[2]
        assertEquals(QuestionType.ESSAY, q3.type)
        assertTrue("صورت سؤال ۳", q3.text.contains("قانون اول نیوتون"))
    }
}
