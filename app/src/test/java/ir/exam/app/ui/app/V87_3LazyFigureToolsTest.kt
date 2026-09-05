package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V87.3 — شش ماژولِ ابزارِ درج تا نخستین نیاز پارس نمی‌شوند، و نوارِ HTML
 * دیگر پیش از رسیدنِ CSS یک لحظه هم دیده نمی‌شود.
 */
class V87_3LazyFigureToolsTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    private val deferred = listOf(
        "extracted-geo-fig-js", "extracted-graph-fig-js", "extracted-table-fig-js",
        "extracted-anatomy-fig-js", "extracted-periodic-fig-js", "extracted-science-fig-js"
    )

    @Test
    fun `the legacy toolbar is hidden by the tag itself not by a stylesheet far below`() {
        // قانونِ CSS حدودِ ۸۳۷KB پایین‌تر از خودِ نوار بود، پس نوار یک لحظه دیده می‌شد
        assertTrue("id=\"qmfLegacyToolbar\" style=\"display:none\"" in asset)
        val tag = asset.indexOf("id=\"qmfLegacyToolbar\"")
        val rule = asset.indexOf("#qmfLegacyToolbar{display:none !important}")
        assertTrue("قانونِ CSS همچنان پایین‌تر است", rule > tag)
    }

    @Test
    fun `every figure module is deferred`() {
        deferred.forEach {
            assertTrue("$it معوق نشده", "<script id=\"$it\" type=\"qmf/deferred\">" in asset)
        }
    }

    @Test
    fun `but none of them was deleted because print still draws with them`() {
        deferred.forEach { assertTrue("id=\"$it\"" in asset) }
        listOf(
            "GeoFig.make(", "GraphFig.svg(", "TableFig.html(",
            "AnatomyFig.svg(", "PeriodicFig.html(", "ScienceFig.svg("
        ).forEach { assertTrue("رندرِ چاپ به $it نیاز دارد", it in asset) }
    }

    @Test
    fun `rendering a figure activates the modules first`() {
        val at = asset.indexOf("function renderFigToken")
        assertTrue(at > 0)
        val head = asset.substring(at, at + 400)
        assertTrue("renderFigToken باید پیش از رسم فعال کند", "__qmfEnsureFigTools()" in head)
    }

    @Test
    fun `the loader only wakes up when a figure is actually involved`() {
        assertTrue("window.__qmfEnsureFigTools = activate" in asset)
        assertTrue("text.indexOf('%%FIG:') === -1" in asset)
        assertTrue(".q-tool-btn, .interactive-figure, .qmf-fig" in asset)
    }

    @Test
    fun `about a third of the javascript leaves the load path`() {
        val re = Regex("""<script[^>]*id="([^"]+)"([^>]*)>([\s\S]*?)</script>""")
        var eager = 0
        var lazy = 0
        re.findAll(asset).forEach { m ->
            val isDeferred = "type=\"qmf/deferred\"" in m.groupValues[2]
            if (isDeferred) lazy += m.groupValues[3].length else eager += m.groupValues[3].length
        }
        assertTrue("باید بیش از ۳۵۰KB معوق شود ولی $lazy است", lazy > 350_000)
        assertTrue("پارسِ فوری باید کمتر از قبل باشد: $eager", eager < 550_000)
    }

    @Test
    fun `the formula editor is not deferred because the page needs it at once`() {
        // ۳۹ ارجاع به QMF و ۲۸ به qMathSync از بیرونِ بلوک
        assertTrue("id=\"extracted-math-host-script\"" in asset)
        assertTrue(
            "ویرایشگر فرمول نباید معوق شود",
            "id=\"extracted-math-host-script\" type=" !in asset
        )
    }

    @Test
    fun `the deferred blocks are inert to the browser`() {
        // type نامعتبر یعنی مرورگر نه پارس می‌کند نه اجرا.
        // فقط تگِ واقعی شمرده می‌شود؛ ذکرِ همین رشته داخلِ کامنتِ بارگذار نه.
        assertEquals(6, Regex("<script id=\"[^\"]+\" type=\"qmf/deferred\">").findAll(asset).count())
    }
}
