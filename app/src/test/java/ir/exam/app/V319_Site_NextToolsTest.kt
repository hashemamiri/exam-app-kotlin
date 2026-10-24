package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V319.1 — هم‌ترازی وب (سایت) با اندروید برای ۳۰ ابزار بستهٔ سوم آموزشی. */
class V319_Site_NextToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private val ids = listOf(
        "axunitconv", "axperimeter", "axrounding",
        "axcircleparts", "axcylinder", "axproportion", "axlinear", "axbarchart", "axparabola",
        "axinthales", "axpendulum", "axincline", "axcircuit", "axoptics", "axheatcurve",
        "axdna", "axtimeline", "axmapgeo", "axsentence", "axvectors", "axmatrix",
        "axintegral", "axflowchart", "axstack", "axsort", "axregression"
    )
    private val legacyRemovedIds = listOf("axpictograph", "axdivision", "axshapeid", "axcell")

    @Test
    fun webEngineRendersFieldsIconsAndDropdownsForEveryNextTool() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        for (k in ids) {
            assertTrue("Missing picker entry $k", g.contains("{ id: '$k',"))
            assertTrue("Missing fields for $k", g.contains(" $k: ["))
            assertTrue("Missing icon for $k", g.contains("      $k: '<"))
            assertTrue("Missing type in engine for $k", g.contains("if (t === '$k')"))
        }
        for (k in legacyRemovedIds) {
            assertTrue("Missing legacy type in engine for $k", g.contains("if (t === '$k')"))
        }
    }

    @Test
    fun siteDropDownsCoverNextEnumParameters() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        for (key in listOf("'axunitconv:X.kind'", "'axperimeter:X.shape'", "'axpictograph:X.mode'",
            "'axrounding:X.mode'", "'axshapeid:X.kind'", "'axcircleparts:X.show'", "'axcylinder:X.kind'",
            "'axbarchart:X.show'", "'axcircuit:X.mode'", "'axoptics:X.kind'", "'axdna:X.mode'",
            "'axcell:X.kind'", "'axmapgeo:X.kind'", "'axsentence:X.mode'", "'axvectors:X.mode'",
            "'axintegral:X.f'", "'axstack:X.mode'", "'axstack:X.op'")) {
            assertTrue("Missing dropdown $key", g.contains(key))
        }
    }

    @Test
    fun cycleAndStepParametersAreWiredInWebEngine() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        assertTrue("unit conv cell editor", g.contains("state.t === 'axunitconv'"))
        assertTrue("numstep table", g.contains("var NUMSTEP = {"))
        assertTrue("sort step", g.contains("axsort: ['step',"))
        assertTrue("rounding step", g.contains("axrounding: ['n',"))
        assertTrue("parabola c step", g.contains("axparabola: ['c',"))
    }
}
