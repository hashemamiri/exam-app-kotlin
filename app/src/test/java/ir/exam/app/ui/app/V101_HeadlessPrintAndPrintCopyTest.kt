package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V101 — دو تغییرِ بخشِ چاپِ آزمون:
 *
 * (A) چاپِ «مستقیمِ بدون‌صفحه» (Headless):
 *     دکمهٔ پرینتر دیگر هیچ پنجره‌ای باز نمی‌کند؛ چاپ با WebViewِ بدون‌صفحه
 *     (الگوی رسمیِ AOSP/PrintHtmlOffScreen) انجام می‌شود:
 *       - کارخانهٔ مشترکِ createExamPrintWebView (هم پنجرهٔ پیش‌نمایش، هم
 *         پرینترِ بدون‌صفحه از همان تنظیمات/تزریق/برج استفاده می‌کنند)
 *       - HeadlessExamPrinter: WebView بدون افزودن به سلسله‌مراتب +
 *         web.createPrintDocumentAdapter(jobName) + printManager.print
 *       - OneShotPrintAdapter: انتقالِ رویدادها + آزادسازی در onFinish
 *       - timeoutِ 180 ثانیه‌ای برای انصراف/اتصالِ معلق (API چاپ
 *         callbackِ job ندارد)
 *       - آزادسازی: stopLoading → about:blank → destroy (یک‌بار)
 *
 * (B) مداد روی کارتِ آزمونِ آنلاین:
 *     «نسخهٔ چاپی» ویرایش‌پذیر از آزمونِ سرور می‌سازد که **فقط در بخشِ
 *     چاپ** ذخیره می‌شود (sourceExamId) و در آزمون‌سازِ بومی باز می‌شود.
 *     ذخیرهٔ مجدد همان نسخه را به‌روز می‌کند (localPrintExamId در draft →
 *     examId در ViewModel) — هیچ تغییری به آزمونِ سرور نمی‌رود.
 */
class V101_HeadlessPrintAndPrintCopyTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }
    private val center by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val store by lazy { source("app/src/main/java/ir/exam/app/data/local/PrintExamStore.kt") }
    private val draft by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val examApp by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }

    // ---------- (A) پرینترِ بدون‌صفحه: وجود ----------

    @Test
    fun `headless printer class exists with the official AOSP print path`() {
        assertTrue("HeadlessExamPrinter تعریف نشده", dialog.contains("internal class HeadlessExamPrinter(context: Context)"))
        assertTrue("OneShotPrintAdapter تعریف نشده", dialog.contains("private class OneShotPrintAdapter("))
        assertTrue("createPrintDocumentAdapter(jobName) گم شد", dialog.contains("web.createPrintDocumentAdapter(jobName)"))
        assertTrue("printManager.print گم شد", dialog.contains("printManager.print(jobName, OneShotPrintAdapter(base) { finish() }"))
        assertTrue("timeoutِ 180 ثانیه‌ای گم شد", dialog.contains("180_000L"))
        assertTrue("cancelِ timeout گم شد", dialog.contains("handler.removeCallbacks(timeoutRunnable)"))
    }

    @Test
    fun `headless printer releases the webview exactly once in the safe order`() {
        assertTrue("finish guard گم شد", dialog.contains("if (finished) return"))
        // رتبه‌بندی: stopLoading پیش از about:blank و destroy
        val stopIdx = dialog.indexOf("view.stopLoading()")
        val blankIdx = dialog.indexOf("view.loadUrl(\"about:blank\")")
        val destroyIdx = dialog.indexOf("view.destroy()")
        assertTrue("stopLoading گم شد", stopIdx >= 0)
        assertTrue("about:blank گم شد", blankIdx >= 0)
        assertTrue("destroy گم شد", destroyIdx >= 0)
        assertTrue("ترتیبِ آزادسازی غلط است (stopLoading→about:blank→destroy)", stopIdx < blankIdx && blankIdx < destroyIdx)
        // چاپِ دوباره، WebView قبلی را آزاد می‌کند
        assertTrue("releaseWebView در شروعِ print گم شد", dialog.contains("fun print(") && dialog.indexOf("releaseWebView()") < dialog.indexOf("val web = WebView(appContext)"))
    }

    @Test
    fun `shared factory createExamPrintWebView is used by both the dialog and the printer`() {
        assertTrue("factory گم شد", dialog.contains("fun createExamPrintWebView("))
        assertTrue("factory باید context بگیرد", dialog.contains("context: Context"))
        // دو مصرف‌کننده: پنجرهٔ دیالوگ + پرینتر
        val factoryCalls = Regex("createExamPrintWebView\\(").findAll(dialog).count()
        assertTrue("انتظار: دست‌کم ۲ فراخوانیِ factory (تعریف + مصرف)؛ پیدا شد: $factoryCalls", factoryCalls >= 3)
        // پرینتر از همان factory استفاده می‌کند (همان bridge/injection/intercept)
        assertTrue("callِ پرینتر گم شد", dialog.contains("printMode = mode,"))
        assertTrue("onPrint → startPrintJob گم شد", dialog.contains("onPrint = { m -> startPrintJob(web, m) }"))
    }

    @Test
    fun `factory keeps the V87_8 injection contract`() {
        assertTrue("bridge گم شد", dialog.contains("ExamPrintBridge("))
        assertTrue("setExamData گم شد", dialog.contains("window.setExamData(\$payload)"))
        assertTrue("qmf-print-mode گم شد", dialog.contains("qmf-print-mode"))
        assertTrue("printStudent(); گم شد", dialog.contains("printStudent();"))
        assertTrue("printTeacher(); گم شد", dialog.contains("printTeacher();"))
    }

    // ---------- (A) مرکزِ چاپ: بدون پنجره ----------

    @Test
    fun `print center starts headless print and no window is shown`() {
        assertTrue("ساختِ پرینتر گم شد", center.contains("HeadlessExamPrinter(context.applicationContext)"))
        assertTrue("فراخوانیِ پرینتر گم شد", center.contains("headlessPrinter.print("))
        assertTrue("پایان → بسته‌شدنِ وضعیت گم شد", center.contains("onFinished = { htmlPrintOpen = false; printStatus = null }"))
        assertFalse("پنجرهٔ چاپ در مرکز هنوز فراخوانی می‌شود!", center.contains("ExamHtmlPrintDialog("))
        assertFalse("printModeFor باید حذف شده باشد", center.contains("printModeFor"))
        assertFalse("importِ پنجره باید از مرکز رفته باشد", center.contains("import ir.exam.app.ui.printing.ExamHtmlPrintDialog"))
    }

    @Test
    fun `print center keeps the V99_1 direct-print entry points`() {
        assertTrue("شروعِ چاپِ دانش‌آموز گم شد", center.contains("startPrint(target, \"student\")"))
        assertTrue("شروعِ چاپِ پاسخ‌نامه گم شد", center.contains("startPrint(target, \"teacher\")"))
        assertTrue("مسیرِ سرور (portability) گم شد", center.contains("portability.printableExam("))
        assertTrue("مسیرِ محلی (PrintableFromDrafts) گم شد", center.contains("ir.exam.app.domain.model.PrintableFromDrafts.build("))
        assertTrue("وضعیتِ چاپ گم شد", center.contains("در حال آماده‌سازی چاپ..."))
    }

    // ---------- (B) مداد: نسخهٔ چاپی از آزمونِ آنلاین ----------

    @Test
    fun `server exam card has a pencil that opens a local print copy`() {
        assertTrue("openPrintCopy گم شد", center.contains("fun openPrintCopy(exam: ir.exam.app.data.dto.ExamDashboardDto)"))
        assertTrue("آیکنِ مداد گم شد", center.contains("IconButton(onClick = { openPrintCopy(exam) }, enabled = !copyLoading)"))
        assertTrue("آیکنِ Edit گم شد", center.contains("Icons.Outlined.Edit"))
        assertTrue("labelِ مداد گم شد", center.contains("contentDescription = \"ویرایش نسخهٔ چاپی\""))
        assertTrue("بازکردن در آزمون‌ساز گم شد", center.contains("onOpenLocalPrintExam(rec.id)"))
    }

    @Test
    fun `print copy is saved only in the print section with a source link`() {
        assertTrue("کپیِ تکراری باز می‌شود (dedupe)", center.contains("firstOrNull { it.sourceExamId == exam.id }"))
        assertTrue("لینک به آزمونِ سرور گم شد", center.contains("sourceExamId = exam.id"))
        assertTrue("بارگذاریِ کامل از سرور گم شد", center.contains("SupabaseExamBuilderRepository("))
        assertTrue("ذخیره در Store گم شد", center.contains("printExamStore.save(rec)"))
        assertTrue("fieldِ sourceExamId در Store گم شد", store.contains("val sourceExamId: String? = null"))
    }

    @Test
    fun `re-saving the print copy updates it in place with the local-only id chain`() {
        assertTrue("localPrintExamId در draft گم شد", draft.contains("val localPrintExamId: String? = null"))
        assertTrue("ExamApp localId را در draft می‌گذارد", examApp.contains("localPrintExamId = localId"))
        assertTrue("ViewModel examId را از draft می‌گیرد", builderVm.contains("examId = imported.localPrintExamId"))
        // ویرایشِ نسخهٔ چاپی نباید به سرور برسد: examId فقط از draft می‌آید
        val vmSnippet = builderVm.substringAfter("fun importDraft").substringBefore("fun saveLocalPrint")
        assertTrue("examId از imported.localPrintExamId می‌آید", vmSnippet.contains("examId = imported.localPrintExamId"))
    }
}
