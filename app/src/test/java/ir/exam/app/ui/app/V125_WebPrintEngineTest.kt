package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V125 — استخراج بخش پیش‌نمایش/چاپ و موتورهای وابستهٔ «آزمون‌ساز v20» (وب) و جایگذاری در برنامه:
 * سندِ اصلیِ رندرر اکنون میزبانِ نازکِ موتورِ وب است (پوشهٔ print/web)، رندررِ قبلی کنار گذاشته شده،
 * و لایهٔ میزبان (webhost.js) همان API قبلیِ برنامه (setExamData/printStudent/printTeacher/
 * ExamPrintRenderer) را روی موتورِ وب نگه می‌دارد.
 */
class V125_WebPrintEngineTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val printDir by lazy { File(root(), "app/src/main/assets/print") }
    private val host by lazy { File(printDir, "exam_print_renderer.html").readText() }
    private val webhost by lazy { File(printDir, "web/webhost.js").readText() }
    private val webhostCss by lazy { File(printDir, "web/webhost.css").readText() }
    private val engine by lazy { File(printDir, "web/pgs_engine.js").readText() + File(printDir, "web/mainscript.js").readText() }
    private val dialog by lazy { File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText() }

    @Test
    fun `renderer entry is a thin host over the web engine`() {
        assertTrue("رندرر باید سبک بماند", host.length < 8_000)
        listOf(
            "window.__appHost = true;",
            "src=\"web/host_dom.js\"", "src=\"web/geo_fig.js\"", "src=\"web/graph_fig.js\"", "src=\"web/table_fig.js\"",
            "src=\"web/anatomy_fig.js\"", "src=\"web/periodic_fig.js\"", "src=\"web/science_fig.js\"",
            "src=\"web/math_host.js\"", "src=\"web/mainscript.js\"", "src=\"web/ui_v2_runtime.js\"",
            "src=\"web/pgs_engine.js\"", "src=\"web/webhost.js\"",
            "href=\"web/pgs_style.css\"", "href=\"web/webhost.css\"",
        ).forEach { assertTrue("نشانگر میزبان نیست: $it", it in host) }
        assertFalse("URL خارجی در میزبان", Regex("https?://").containsMatchIn(host))
    }

    @Test
    fun `web engine files are bundled verbatim and offline`() {
        listOf(
            "main.css", "pgs_style.css", "vazirmatn_embed.css", "geo_fig.js", "graph_fig.js", "table_fig.js",
            "anatomy_atlas_data.js", "science_atlas_data.js", "math_host.js", "mainscript.js", "pgs_engine.js",
        ).forEach { assertTrue("فایل موتور وب نیست: $it", File(printDir, "web/$it").isFile) }
        listOf("function renderPreview()", "function paginate()", "function buildHeader()", "function rebuildPrintRoot()", "classList.add('pgs-fallback')")
            .forEach { assertTrue("موتور PGS ناقص است: $it", it in engine) }
        assertFalse("اسکریپت Cloudflare در موتور وب", "cdn-cgi" in engine)
    }

    @Test
    fun `atlas data points to the app figure_atlas files instead of embedded base64`() {
        val anatomy = File(printDir, "web/anatomy_atlas_data.js").readText()
        val science = File(printDir, "web/science_atlas_data.js").readText()
        assertTrue("window.ATLAS={" in anatomy && "'/figure_atlas/anatomy/atlas-" in anatomy)
        assertTrue("window.SCIENCE_ATLAS={" in science && "'/figure_atlas/science/" in science)
        assertFalse("تصویر base64 در اطلس آناتومی", "data:image" in anatomy)
        assertFalse("تصویر base64 در اطلس علوم", "data:image" in science)
        assertTrue(anatomy.length < 20_000 && science.length < 20_000)
        val assets = File(root(), "app/src/main/assets")
        Regex("'(/figure_atlas/[^']+)'").findAll(anatomy + science).forEach { m ->
            assertTrue("فایل اطلس نیست: ${'$'}{m.groupValues[1]}", File(assets, m.groupValues[1].removePrefix("/")).isFile)
        }
        // WebView چاپ همین مسیر را از assets سرو می‌کند
        assertTrue("path.startsWith(\"/figure_atlas/\") -> path.removePrefix(\"/\")" in dialog)
    }

    @Test
    fun `webhost keeps the app renderer API and print handshake`() {
        listOf(
            "window.setExamData = setExamData;",
            "window.printStudent = function",
            "window.printTeacher = function",
            "window.ExamPrintRenderer = {",
            "function requestPrint(mode, opts)",
            "callBridge('print', mode)",
            "callBridge('previewClosed')",
            "callBridge('editFigureTool', String(qid), index)",
            "window.openPreviewWindow()",
            "function toWebQuestion(src, index)",
            // پاسخِ beforeprintِ خودِ WebView نباید صفحه‌بندی را در میانهٔ چاپ دوباره اجرا کند
            "ev.__appHost = true; window.dispatchEvent(ev)",
            "e.stopImmediatePropagation(); document.body.classList.add('pgs-fallback')",
        ).forEach { assertTrue("لایهٔ میزبان ناقص است: $it", it in webhost) }
        listOf("localStorage", "innerHTML", "<iframe").forEach { assertFalse("ساختار ممنوع در webhost.js: $it", it in webhost) }
        assertTrue("فونت‌های برنامه به موتور وب وصل نیست", "/fonts/" in webhostCss)
        assertTrue("در چاپ، بیننده پنهان نمی‌شود", "@media print" in webhostCss)
    }

    @Test
    fun `legacy renderer is deleted and no native header remains`() {
        assertFalse("رندرر قبلی باید حذف شده باشد (V126)", File(printDir, "exam_print_renderer_legacy.html").exists())
        assertFalse("PrintPreviewHeader" in dialog)
        assertFalse("WEB_ENGINE_PREVIEW" in dialog)
        assertTrue("settings.setSupportZoom(false)" in dialog)
        // V127 — پنل‌های fixed موتور وب (📐/دیالوگ چاپ) به viewport واقعی دستگاه نیاز دارند
        assertTrue("settings.loadWithOverviewMode = false" in dialog)
        // V127 — بازه/تعداد نسخه از دیالوگ چاپ وب؛ امضای دبیر/مدیر حذف
        assertTrue("function applyRangeAndCopies(opts)" in webhost)
        // V128 — قالب‌بندیِ متنِ انتخاب‌شده در لایهٔ میزبان؛ snapshot بازه‌ها را برمی‌گرداند (applyFigLayouts آن‌ها را می‌خواند)
        assertTrue("function ensureFmtBar()" in webhost && "spans: (q.__spans || []).map(" in webhost)
        assertTrue("#pgsViewer #previewArea .question-sep-drag{display:flex !important" in webhostCss)
        // V129 — دلیمترهای عینِ ویرایشگر، انتخابِ پابرجا، لمسِ اول انتخاب/دوم ویرایش، اندازهٔ ۱..۱۰۰
        assertTrue("function installDelimOverride()" in webhost && "function restoreSelection()" in webhost && "(n >= 1 && n <= 100)" in webhost)
        assertTrue("#pgsViewer #previewArea .interactive-figure.selected .fig-resize-handle{display:block !important;}" in webhostCss && ".mdelim-x{display:flex !important" in webhostCss)
        // V127.1 — پنل تنظیمات صفحه بدون vh (در WebView یک‌سطری باز می‌شد)
        assertTrue("top:var(--host-top,60px) !important;bottom:0 !important" in webhostCss && "max-height:none !important" in webhostCss)
        assertTrue("requestPrint(mode, {rangeKind: rangeKind, rangeText: rangeText, current: current, copies: copies})" in webhost)
        val models = File(root(), "app/src/main/java/ir/exam/app/domain/model/OfficialPrintModels.kt").readText()
        assertFalse("امضای دبیر/مدیر باید حذف شده باشد", "نام و امضای دبیر:" in models)
        assertTrue("internal const val MAIN_PAGE_URL = \"https://exam-print.local/print/exam_print_renderer.html\"" in dialog)
    }
}
