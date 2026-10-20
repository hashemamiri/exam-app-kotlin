package ir.exam.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V318.0 — هم‌ترازی وب (سایت) با اندروید برای ۴۰ ابزار جامع آموزشی. */
class V318_Site_AdvancedToolsTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private val advancedIds = listOf(
            "axrod", "axtower", "axmulwheel", "axmultgrid", "axnet",
            "axcubes", "axcalendar", "axlever", "axcycle", "axwordtrain",
            "axtiles", "axkenken", "axdicefold", "axtransform", "axcaliper",
            "axsigntable", "axtrigcircle", "axmapping", "axprobtree", "axconic",
            "axorbital", "axpunnett", "axecg", "axprosody", "axtruthtable",
            "axledger", "axstaff", "axkmap", "axgate", "axdfa",
            "axbst", "axbode", "axbeam", "axmohr", "axpv",
            "axpert", "axbenzene", "axnormal", "axsupply", "axgame"
    )

    @Test
    fun webEngineRendersFieldsIconsAndDropdownsForAllAdvancedTools() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        for (k in advancedIds) {
            assertTrue("Missing picker entry $k", g.contains("{ id: '$k',"))
            assertTrue("Missing fields for $k", g.contains(" $k: ["))
            assertTrue("Missing icon for $k", g.contains("      $k: '<"))
            assertTrue("Missing type in engine for $k", g.contains("if (t === '$k')"))
        }
        assertTrue(g.contains("var CYCLE = {"))
        assertTrue(g.contains("var NUMSTEP = {"))
        assertTrue(g.contains("function pfxArrow("))
    }

    @Test
    fun siteDropDownsCoverAdvancedEnumParameters() {
        val g = File(root(), "app/src/main/assets/print/web/graph_fig.js").readText()
        for (key in listOf("'axnet:X.shape'", "'axlever:X.kind'", "'axcycle:X.kind'", "'axtransform:X.op'",
            "'axconic:X.kind'", "'axorbital:X.mode'", "'axpunnett:X.kind'", "'axecg:X.kind'",
            "'axprosody:X.mode'", "'axtruthtable:X.kind'", "'axstaff:X.kind'", "'axkmap:X.vars'",
            "'axgate:X.kind'", "'axbst:X.mode'", "'axbode:X.kind'", "'axbeam:X.load'", "'axpv:X.kind'",
            "'axbenzene:X.kind'", "'axsupply:X.kind'", "'axgame:X.kind'", "'axcaliper:X.kind'", "'axkenken:X.size'")) {
            assertTrue("Missing dropdown $key", g.contains(key))
        }
    }
}
