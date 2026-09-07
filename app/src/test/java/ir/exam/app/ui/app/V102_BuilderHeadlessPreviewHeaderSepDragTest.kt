package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V102 — سه رفع‌خطا بر روی APKِ V101:
 *
 * (A) چاپ از آزمون‌ساز (مداد → چاپ): پنجرهٔ بدون‌هدر دیگر باز نمی‌شود و
 *     کارت‌های ویرایش پشتِ پنلِ چاپ نمی‌مانند؛ چاپ دقیقاً همان «چاپِ
 *     مستقیمِ بدون‌صفحه» (HeadlessExamPrinter) است که بخشِ چاپ دارد.
 *     مسیرِ چشم (پیش‌نمایش) دست‌نخورده می‌ماند.
 *
 * (B) پنجرهٔ پیش‌نمایش هدر دارد: عنوانِ برگه + دکمهٔ بستنِ Compose که
 *     مستقیم requestDismiss() می‌زند (با ذخیرهٔ چیدمان اشیاء).
 *
 * (C) خطِ جداکنندهٔ کادرِ متنِ سؤال در پنجرهٔ پیش‌نمایش وقتی برگه
 *     مقیاسِ کوچک (شیءِ بزرگ) دارد قابل جابجایی است:
 *       - ناحیهٔ لمس با مقیاس بزرگ می‌شود (حداقل ۳۴px روی صفحه)
 *       - خط در پنجره همیشه دیده می‌شود (لمس hover ندارد)
 *       - دلتای کشیدن بر مقیاس تقسیم می‌شود (حرکتِ ۱:۱ با انگشت)
 *     فقط با انتخابِ صریحِ کاربر حرکت می‌کند (بدونِ تنظیمِ خودکار).
 */
class V102_BuilderHeadlessPreviewHeaderSepDragTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val center by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val html by lazy { source("app/src/main/assets/print/exam_print.html") }

    // ---------- (A) چاپِ بدون‌صفحه از آزمون‌ساز ----------

    @Test
    fun `builder creates the shared headless printer`() {
        assertTrue("headlessPrinter تعریف نشده", builder.contains("val headlessPrinter = remember(context) {"))
        assertTrue("ساختِ HeadlessExamPrinter گم شد", builder.contains("ir.exam.app.ui.printing.HeadlessExamPrinter(context.applicationContext)"))
    }

    @Test
    fun `builder student and teacher print go through the headless printer`() {
        val studentIdx = builder.indexOf("headlessPrinter.print(")
        assertTrue("فراخوانِ چاپ از منوی بیلدر گم شد", studentIdx >= 0)
        assertTrue("مُد student گم شد", builder.substring(studentIdx).contains("\"student\""))
        val teacherIdx = builder.indexOf("headlessPrinter.print(", studentIdx + 1)
        assertTrue("فراخوانِ چاپِ دوام (teacher) گم شد", teacherIdx >= 0)
        assertTrue("مُد teacher گم شد", builder.substring(teacherIdx).contains("\"teacher\""))
    }

    @Test
    fun `builder print reports status via snackbar and clears on finish`() {
        assertTrue("onStatus گم شد", builder.contains("onStatus = { msg -> printStatus = msg }"))
        assertTrue("onFinished گم شد", builder.contains("onFinished = { printStatus = null }"))
        assertTrue("نمایشِ وضعیت در snackbar گم شد", builder.contains("printStatus?.let { noticeSnackbar.showSnackbar(it) }"))
        assertTrue("پیامِ آماده‌سازی گم شد", builder.contains("printStatus = \"در حال آماده‌سازی چاپ...\""))
    }

    @Test
    fun `builder print no longer opens the headerless window`() {
        assertFalse("بیلدر هنوز printInitialMode=\"student\" می‌گذارد", builder.contains("printInitialMode = \"student\""))
        assertFalse("بیلدر هنوز printInitialMode=\"teacher\" می‌گذارد", builder.contains("printInitialMode = \"teacher\""))
    }

    @Test
    fun `builder eye preview path stays unchanged`() {
        assertTrue("مسیرِ چشم (پیش‌نمایش) حذف شده!", builder.contains("printInitialPreview = true"))
    }

    @Test
    fun `center print is still headless (V101 regression)`() {
        assertTrue("چاپِ بخشِ چاپ دیگر بدون‌صفحه نیست!", center.contains("headlessPrinter.print("))
        assertTrue("HeadlessExamPrinter از دیالوگ حذف شده!", dialog.contains("internal class HeadlessExamPrinter(context: Context)"))
    }

    // ---------- (B) هدرِ پنجرهٔ پیش‌نمایش ----------

    @Test
    fun `preview window has a compose header with title and close`() {
        assertTrue("عنوانِ برگه در هدر گم شد", dialog.contains("printable?.documentTitle ?: \"پیش‌نمایش برگه\""))
        assertTrue("آیکنِ بستن گم شد", dialog.contains("Icons.Filled.Close"))
        assertTrue("contentDescription بستن گم شد", dialog.contains("contentDescription = \"بستن\""))
        assertTrue("بستن از requestDismiss باید برود", dialog.contains("IconButton(onClick = { requestDismiss() }) {"))
        assertTrue("importِ Icons گم شد", dialog.contains("import androidx.compose.material.icons.Icons"))
    }

    @Test
    fun `header renders above the webview`() {
        val headerIdx = dialog.indexOf("printable?.documentTitle ?: \"پیش‌نمایش برگه\"")
        val boxIdx = dialog.indexOf("Box(Modifier.fillMaxSize().weight(1f)) {")
        assertTrue("هدر پیدا نشد", headerIdx >= 0)
        assertTrue("کادرِ WebView پیدا نشد", boxIdx >= 0)
        assertTrue("هدر باید پیش از WebView رندر شود", headerIdx < boxIdx)
    }

    // ---------- (C) خطِ جداکننده در پیش‌نمایشِ مقیاس‌شده ----------

    @Test
    fun `sep touch target grows with preview scale`() {
        assertTrue("هدفِ لمسِ مقیاس‌آگاه گم شد", html.contains("height:max(14px, calc(34px / var(--qmf-pv-k, 1)))"))
        assertTrue("نوارِ خط باید به لبهٔ پایین چسبیده بماند", html.contains("height:3px;border-radius:999px;bottom:5px;"))
    }

    @Test
    fun `sep line is always visible inside the preview window (no hover on touch)`() {
        assertTrue("خطِ همیشه‌دیده در پنجره گم شد", html.contains("#previewWinOverlay .question-sep-drag::before"))
    }

    @Test
    fun `fit scale exposes the effective scale as a css var`() {
        val mainIdx = html.indexOf("pc.style.setProperty('--qmf-pv-k', String(kz))")
        assertTrue("متغیرِ مقیاس در مسیرِ اصلی گم شد", mainIdx >= 0)
        assertTrue("بازنشانی در مسیرِ بدونِ مقیاس گم شد", html.contains("pc.style.setProperty('--qmf-pv-k', '1')"))
        assertTrue("بازنشانی در بستنِ پنجره گم شد", html.contains("pc.style.removeProperty('--qmf-pv-k')"))
    }

    @Test
    fun `sep drag delta is divided by the current scale (one to one with finger)`() {
        assertTrue("خواندنِ مقیاسِ فعلی گم شد", html.contains("function curScale() {"))
        assertTrue("مطابقتِ scale از transform گم شد", html.contains("/scale\\(([0-9.]+)\\)/"))
        assertTrue("ذخیرهٔ مقیاس در شروعِ کشیدن گم شد", html.contains("k: curScale()"))
        assertTrue("تقسیمِ دلتا بر مقیاس گم شد", html.contains("(e.clientY - sepDrag.sy) / (sepDrag.k || 1)"))
    }

    @Test
    fun `sep drag stays manual (no auto layout adjustments introduced)`() {
        // محدودسازیِ 0..600 باید بماند؛ هیچ کلیدِ «تنظیم خودکار ارتفاع» نباید آمده باشد
        assertTrue("clampِ 600px گم شد", html.contains("Math.min(600, Math.round(val || 0))"))
        assertFalse("تنظیمِ خودکارِ ارتفاعی اضافه شده!", html.contains("autoFitQuestionBox"))
    }
}
