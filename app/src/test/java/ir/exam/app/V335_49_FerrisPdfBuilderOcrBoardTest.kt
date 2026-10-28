package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.49 — پنج گزارش میدانی کاربر:
 * ۱) چرخ‌وفلک کارت‌های اپ در سایت گوشی نبود (سایت «دِک» کارت‌ها را داشت).
 * ۲) خروجی PDF کارنامه روی گوشی بد/نامفهوم بود (جدول ۵۶۰px از کاغذ بیرون می‌زد).
 * ۳) پنجرهٔ آزمون‌ساز در صفحهٔ گوشی از عرض درست جا نمی‌گرفت.
 * ۴) متن OCR باید فقط در متن همان سؤال بنشیند (نه ساخت سؤال‌های تازه).
 * ۵) بعضی ابزارهای وایت‌برد اپ درست کار نمی‌کردند (پاک‌کن/متن ابزار هندسی را جابه‌جا می‌کردند،
 *    در صفحهٔ کوتاه بوم به صفر می‌رسید و درج تصویرِ ناموفق بی‌صدا بود).
 */
class V335_49_FerrisPdfBuilderOcrBoardTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun read(rel: String): String = File(root(), rel).readText(Charsets.UTF_8)

    private val css = read("site/src/site.css")
    private val mobile = read("site/src/mobile.js")

    @Test
    fun ferrisWheelMatchesTheNativeCardsScreen() {
        assertTrue("مدار بیضوی ۱۱۲px", "-Math.sin(a) * RX" in mobile && "RX = 112" in mobile)
        assertTrue("عمق عمودی ۴۴", "RY = 44" in mobile)
        assertTrue("مقیاس ۰٫۷۸ + ۰٫۲۲cos", "0.78 + 0.22 * cs" in mobile)
        assertTrue("شفافیت ۰٫۳۵ + ۰٫۶۵", "0.35 + 0.65" in mobile)
        assertTrue("کشش ۱۹۰dp برای هر کارت", "DRAG_PX = 190" in mobile)
        assertTrue("فنر اپ: damping ۰٫۹۲ و stiffness ۱۸۰", "C = 2 * 0.92 * Math.sqrt(K)" in mobile && "K = 180" in mobile)
        assertTrue("پرتاب مانند اپ", "vel * 0.14" in mobile)
        assertTrue("کلاس‌های چرخ‌وفلک در CSS", ".m-wheel-stage{" in css && ".m-wheel-card{" in css)
        assertTrue("کاهش حرکت", "@media (prefers-reduced-motion:reduce){.m-wheel-card{transition:none!important}}" in css)
        assertFalse("دِک قدیمی حذف شد", "m-deck-stage" in mobile)
    }

    @Test
    fun printPdfFitsThePageOnPhones() {
        val printBlock = css.substring(css.indexOf("@media print{\n  @page{size:A4 portrait"))
        assertTrue("اندازهٔ کاغذ A4 و حاشیهٔ ۱۰mm", "@page{size:A4 portrait;margin:10mm}" in css)
        assertTrue("لغو min-width جدول در چاپ", "min-width:0!important" in printBlock)
        assertTrue("لغو اسکرول افقی در چاپ", "overflow:visible!important" in printBlock)
        assertTrue("چیدمان ثابت ستون‌ها", "table-layout:fixed" in printBlock)
        assertTrue("شکست صفحه بین ردیف‌ها", "break-inside:avoid" in printBlock)
        assertTrue("سربرگ جدول در هر صفحه", "display:table-header-group" in printBlock)
    }

    @Test
    fun builderSettingsSheetFitsThePhoneWidth() {
        assertTrue("فیلد قالب یک ردیف کامل", ".b-tpl>.field{flex:1 1 100%;min-width:0}" in css)
        assertTrue("دکمه‌های قالب بدون شکستن", ".b-tpl>.btn{flex:1 1 40%;min-width:0;white-space:nowrap" in css)
        assertTrue("ردیف‌های مودال گوشی", ".m-mode .modal .row{flex-wrap:wrap}" in css)
        assertTrue("قاعدهٔ ۴۲۰px", "@media (max-width:420px){" in css)
    }

    @Test
    fun ocrTextStaysInsideTheSameQuestion() {
        val builder = read("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val vm = read("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("افزودن به همان سؤال", "viewModel.appendOcrTextToQuestion(question.id, editedText)" in builder)
        assertFalse("ساخت سؤال تازه حذف شد", "addOcrQuestions" in builder)
        assertFalse("تفکیک‌کننده حذف شد", "OcrQuestionParser" in builder)
        assertTrue("پیام درست", "متن اسکن‌شده به همین سؤال افزوده شد." in builder)
        assertTrue("ViewModel", "fun appendOcrTextToQuestion(" in vm)
    }

    @Test
    fun whiteboardToolsBehave() {
        val board = read("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("پاک‌کن ابزار هندسی را جابه‌جا نمی‌کند", "val instActive = inst.kind != BoardInstrument.NONE && tool.usesInstrument" in board)
        assertTrue("چسبیدن فقط برای ابزارهای خطی", "if (instActive && tool.snapsToInstrument)" in board)
        assertTrue("بوم حداقل ۱۸۰dp", "heightIn(min = 180.dp)" in board)
        assertTrue("نوارهای کنترل اسکرول‌پذیر", "LocalConfiguration.current.screenHeightDp * 0.50f" in board)
        assertTrue("درج تصویر ناموفق پیام می‌دهد", "این تصویر روی تخته درج نشد" in board)
    }
}
