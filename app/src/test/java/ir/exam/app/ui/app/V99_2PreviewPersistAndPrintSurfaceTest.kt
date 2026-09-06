package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V99.2 — دو ایراد بعد از V99.1:
 * ۱) بعد از بازگشت از پنجرهٔ چاپ، یک صفحهٔ سفید دیده می‌شد:
 *    در حالتِ چاپِ مستقیم، کارت‌های بومی پنهان و پنجرهٔ پیش‌نمایشِ HTML
 *    هم باز نبود و همهٔ عناصرِ صفحه در screen مخفی‌اند ⇒ WebView کاملاً
 *    خالی. حالا با class `qmf-print-mode` برگهٔ A4 در جایِ اصلی‌اش
 *    (خارجِ overlay؛ برای چاپ هم درجا) دیده می‌شود.
 * ۲) تغییراتِ کاربر در پنجرهٔ پیش‌نمایش (موقعیتِ اشیاء/شناور/slot و
 *    فاصلهٔ جداکننده) با هر بارِ باز شدنِ پنجره ریست می‌شد، چون فقط در
 *    وضعیتِ صفحه بود و تزریقِ setExamData آن را با دادهٔ بومی (که چیدمان
 *    نداشت) عوض می‌کرد. حالا چیدمان اسنپ‌شات گرفته، به وضعیتِ بومیِ
 *    بیلدر برمی‌گردد و در بارِ بعدی (پیش‌نمایش و چاپ) بازمی‌گردد.
 */
class V99_2PreviewPersistAndPrintSurfaceTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val payload by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintPayload.kt").readText()
    }
    private val draft by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/QuestionDraft.kt").readText()
    }
    private val printable by lazy {
        File(root(), "app/src/main/java/ir/exam/app/domain/model/PrintableFromDrafts.kt").readText()
    }
    private val viewModel by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt").readText()
    }
    private val builder by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
    }

    // ---------- ۱) چاپِ مستقیم: صفحهٔ سفید نباشد ----------

    // ---------- ۱ب) هشدارهای بی‌ضررِ کنسول نباید خطا نمایش دهند ----------

    @Test
    fun `benign console warnings do not show the red error banner`() {
        // V99.2d — WebView «Ignored attempt to cancel a: touchmove ...» را با
        // سطحِ ERROR می‌فرستد؛ دیالوگ آن را می‌بلعد (handler های pinch-zoom).
        assertTrue("if (text.contains(\"Ignored attempt to cancel\")) return true" in dialog)
    }

    @Test
    fun `direct print shows the a4 sheet in place instead of a white page`() {
        // کلاسِ حالتِ چاپ: ابزارها/کارت‌های HTML پنهان، برگه در جایِ اصلی:
        assertTrue("body.qmf-print-mode" in asset)
        assertTrue("body.qmf-print-mode #printContent.live-preview{" in asset)
        // V99.2e — روی موبایل برگه دیگر تمام‌صفحهٔ سفید نمی‌شود:
        // عرضِ محدود + زمینهٔ خاکستریِ روشن + حاشیه/سایهٔ قوی.
        assertTrue("width:min(210mm, 92vw) !important" in asset)
        assertTrue("body.qmf-print-mode{background:#cbd5e1 !important}" in asset)
        assertTrue("body.qmf-print-mode .toolbar" in asset)
        // دیالوگ در حالتِ چاپِ مستقیم کلاس را اضافه می‌کند:
        assertTrue("document.body.classList.add('qmf-print-mode')" in dialog)
        // و برگه خارجِ overlay می‌ماند (چاپ از جایِ اصلی؛ overlay که
        // هنگامِ چاپ پنهان می‌شود باز نمی‌شود):
        assertTrue("if (initialPreview) {" in dialog)
    }

    // ---------- ۲) چیدمانِ پیش‌نمایش ریست نشود ----------

    @Test
    fun `the page snapshots figure layouts for the native host`() {
        assertTrue("window.__qmfFigLayoutsSnapshot = function ()" in asset)
        assertTrue("entry = { figLayouts: q.figLayouts };" in asset)
        assertTrue("entry.sepExtraPx = q.sepExtraPx;" in asset)
    }

    @Test
    fun `injected questions carry their saved figure layouts`() {
        // normQ چیدمانِ ارسالیِ بومی را به سؤالِ صفحه برمی‌گرداند:
        assertTrue("n.figLayouts = {};" in asset)
        assertTrue("if (q.figLayoutsJson) {" in asset)
        assertTrue("if (Number(q.sepExtraPx) > 0) n.sepExtraPx = Number(q.sepExtraPx);" in asset)
    }

    @Test
    fun `the round trip reaches the native builder state`() {
        // مدل‌ها فیلد را دارند:
        assertTrue("val figLayoutsJson: String = \"\"" in draft)
        assertTrue("val sepExtraPx: Int = 0" in draft)
        assertTrue("figLayoutsJson = question.figLayoutsJson," in printable)
        // پلِ payload فیلد را می‌فرستد:
        assertTrue("put(\"figLayoutsJson\", q.figLayoutsJson)" in payload)
        assertTrue("put(\"sepExtraPx\", q.sepExtraPx)" in payload)
        // دیالوگ اسنپ‌شات را می‌گیرد و به میزبان می‌دهد:
        assertTrue("onFigLayouts: ((String) -> Unit)? = null" in dialog)
        assertTrue("fun fetchFigLayoutsSnapshot()" in dialog)
        assertTrue("__qmfFigLayoutsSnapshot" in dialog)
        // هم هنگامِ بستنِ پیش‌نمایش و هم هنگامِ بستنِ پنجره:
        assertTrue(
            dialog.indexOf("onPreviewClosed = {") > 0
                && "fetchFigLayoutsSnapshot()" in dialog
        )
        // بیلدر بومی آن را به وضعیت می‌نویسد:
        assertTrue("fun applyFigLayouts(snapshotJson: String)" in viewModel)
        assertTrue("onFigLayouts = { viewModel.applyFigLayouts(it) }" in builder)
        // intOrNull در این نسخهٔ kotlinx خاصیتِ extension است: هم import
        // لازم است و هم با پرانتز صدا زده نمی‌شود (V99.2b/c).
        assertTrue("import kotlinx.serialization.json.intOrNull" in viewModel)
        assertTrue("?.jsonPrimitive?.intOrNull ?: 0" in viewModel)
    }
}
