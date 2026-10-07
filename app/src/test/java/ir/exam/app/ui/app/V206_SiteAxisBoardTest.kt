package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V206 — سایت: ابزار «محور» (حالت محور در موتور نمودار مشترک) و «تختهٔ سفید» در نوار ابزار سؤال. */
class V206_SiteAxisBoardTest {
    private fun source(path: String): String {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, path).isFile) dir = dir.parentFile
        return File(dir ?: File(""), path).readText()
    }

    @Test
    fun `graph engine has an axis mode mirroring AXIS_FIGURES`() {
        val g = source("app/src/main/assets/print/web/graph_fig.js")
        assertTrue("function open(spec, el, mode)" in g && "var axis = mode === 'axis' || !!(spec && isAxisType(spec.t));" in g)
        assertTrue("function fillShapes(axis)" in g && "(axis ? AXIS_TYPES : TYPES).forEach" in g)
        for (id in listOf("axnum", "axxy", "axq1", "axgrid", "axpol", "ax3d")) assertTrue(id, "{ id: '$id'," in g)
        assertTrue("function defAxis(t)" in g && "if (isAxisType(id)) {" in g)
    }

    @Test
    fun `site toolbar has axis and whiteboard buttons like the app`() {
        val b = source("site/src/builder.js")
        assertTrue("class: 'tool-btn is-axis', title: 'محور'" in b && "insertFigure('axis', ta, q)" in b)
        assertTrue("axis: w.GraphFig" in b && "api.open(null, null, kind === 'axis' ? 'axis' : sciDom);" in b)
        assertTrue("class: 'tool-btn is-board'" in b && "window.SiteStudent.openWhiteboard(q, function (pages) {" in b)
        val s = source("site/src/student.js")
        assertTrue("function openWhiteboard(q, onDone, opts)" in s && "openWhiteboard: openWhiteboard," in s)
        assertTrue("var prev = opts ? (opts.prev || []) :" in s)
    }
}
