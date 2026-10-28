package ir.exam.app

import java.io.File
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
        val siteCss = read("site/src/site.css")
        assertTrue("کلاس b-fill-answers در وب پیاده‌سازی شده است", "b-fill-answers" in builderJs)
        assertTrue("ردیف پاسخ‌ها با استایل استاندارد field مثل فیلد درس پیچیده شده است", "b-fill-row" in builderJs && "el('div', {class: 'field'}, [ai])" in builderJs)
        assertTrue("استایل b-fill-answers .field در CSS سایت تعریف شده است", ".b-fill-answers .field" in siteCss)
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
        // V335.49 — متن OCR فقط در متن همان سؤال می‌نشیند (سؤال تازه ساخته نمی‌شود).
        assertTrue("افزودن متن OCR به همان سؤال", "viewModel.appendOcrTextToQuestion(question.id, editedText)" in builderScreen)
        assertFalse("تفکیک OCR به سؤال‌های تازه حذف شده است", "addOcrQuestions" in builderScreen)
    }

    @Test
    fun feature6_ocrTextLandsInTheSameQuestionOnly() {
        /* V335.49 — «هر چه اسکن می‌شود فقط در متن همان سؤال»: تفکیک‌کنندهٔ چندسؤالی حذف شد و
           افزودن متن به همان سؤال در ViewModel انجام می‌شود. */
        assertFalse(
            "فایل تفکیک‌کنندهٔ OCR حذف شده است",
            java.io.File(root(), "app/src/main/java/ir/exam/app/ui/builder/OcrQuestionParser.kt").exists()
        )
        val vm = read("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("تابع افزودن به همان سؤال", "fun appendOcrTextToQuestion(questionId: String, raw: String): Boolean" in vm)
        assertTrue("ادغام با متن فعلی", "cur.text.trimEnd() + "\\n" + clean" in vm)
        assertTrue("بدون ساخت سؤال تازه", "st.questions + " !in vm.substring(vm.indexOf("fun appendOcrTextToQuestion"), vm.indexOf("fun addFromBank")))
        val board = read("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("شکست خاموش درج تصویر رفع شد", "این تصویر روی تخته درج نشد" in board)
        assertTrue("پاک‌کن ابزار هندسی را جابه‌جا نمی‌کند", "tool.usesInstrument" in board)
        assertTrue("فقط ابزارهای خطی به لبه می‌چسبند", "tool.snapsToInstrument" in board)
        assertTrue("بوم در صفحهٔ کوتاه جمع نمی‌شود", "heightIn(min = 180.dp)" in board)
        assertTrue("نوارهای کنترل اسکرول‌پذیر شدند", "LocalConfiguration.current.screenHeightDp * 0.50f" in board)
    }
}
