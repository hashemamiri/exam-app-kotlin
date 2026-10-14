package ir.exam.app.ui.app

import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.NativeMathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V288 — کتابخانهٔ دانشگاهی (۲۴ زبانه) + رفع «گاهی ویرایشگر فرمول لود نمی‌شود» در اپ و سایت. */
class V288_UniversityLibraryAndLoadTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "site/src/extras.js").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun universityGroupHas24Pads() {
        val f = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("MB_GROUPS.push({ key: 'uni', label: '🎓 دانشگاه و تحصیلات تکمیلی'" in f)
        val ids = listOf("uni-real", "uni-complex", "uni-ode", "uni-pde", "uni-linalg", "uni-abstract", "uni-topo", "uni-numeric",
            "uni-prob", "uni-optim", "uni-qm", "uni-em", "uni-mech", "uni-statmech", "uni-solid", "uni-pchem",
            "uni-engmech", "uni-ee", "uni-control", "uni-cs", "uni-econ", "uni-bio", "uni-earth", "uni-chemeng")
        ids.forEach { assertTrue(it, "{ id: \"$it\", label:" in f) }
        assertTrue("b.setAttribute('data-uni-chip', '1');" in f)
        assertTrue("\"شرودینگر وابسته به زمان\"" in f && "\"بلک-شولز\"" in f && "\"کوکرافت-گالت\"" in f)
    }

    @Test
    fun nativeParserKnowsUniversitySymbols() {
        fun flat(n: MathNode): String = when (n) {
            is MathNode.Symbol -> n.value
            is MathNode.Sequence -> n.children.joinToString("") { flat(it) }
            else -> "?"
        }
        assertEquals("⊗", flat(NativeMathParser.parse("\\otimes")))
        assertEquals("†", flat(NativeMathParser.parse("\\dagger")))
        assertEquals("‖", flat(NativeMathParser.parse("\\|")))
        assertEquals("sup", flat(NativeMathParser.parse("\\sup")))
    }

    @Test
    fun appFormulaHostRecoversFromDeadOrReloadingWebView() {
        val h = src("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt")
        assertTrue("fun isReady(view: WebView): Boolean = ready[view] == true" in h)
        assertTrue("override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail?): Boolean {" in h)
        assertTrue("fun replace(view: WebView, context: Context): WebView {" in h)
        assertTrue("if (!FormulaEditorPool.isReady(view)) {" in h)
        assertTrue("recreate(\"NO_JS_RESPONSE\")" in h)
        assertTrue("attempts < 200 -> view.postDelayed({ tryBeginRef?.invoke() }, 150)" in h)
        assertTrue("ready[view] = true // V288" in h)
    }

    @Test
    fun siteEngineLoadHasTimeoutsRetryAndKeepsOldEngines() {
        val app = src("site/src/app.js")
        assertTrue("var enginesAttempt = 0;" in app)
        assertTrue("if (enginesAttempt === 1) {" in app)
        assertTrue("}, 120000);" in app)
        assertTrue("if (w.failed || waited > 120000 || (tLoaded && Date.now() - tLoaded > 20000))" in app)
        assertTrue("class: 'engine-wait'" in app)
        assertTrue(".engine-wait{" in src("site/src/site.css"))
        val b = src("site/build_site.py")
        assertTrue("engines_keep.txt" in b)
        assertTrue("keep = keep[:3]" in b)
    }
}
