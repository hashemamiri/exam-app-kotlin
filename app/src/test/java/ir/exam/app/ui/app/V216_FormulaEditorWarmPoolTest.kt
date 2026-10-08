package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V216 — ویرایشگر فرمول «گرم»: WebView از قبل ساخته/بارگذاری‌شده در اپ و iframe پنهان آماده در سایت. */
class V216_FormulaEditorWarmPoolTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun appReusesPreparedWebView() {
        val host = src("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt")
        assertTrue("object FormulaEditorPool" in host)
        assertTrue("fun prepare(context: Context)" in host)
        assertTrue("fun acquire(context: Context): WebView" in host)
        assertTrue("fun recycle(view: WebView)" in host)
        assertTrue("val view = FormulaEditorPool.acquire(context)" in host)
        assertTrue("FormulaEditorPool.recycle(view)" in host)
        // پس از بستن، همان نمونه در پس‌زمینه بازنشانی می‌شود و نمونهٔ پارک‌شده تایمر مصرف نمی‌کند
        assertTrue("view.loadUrl(EDITOR_URL)\n        parked = view" in host)
        assertTrue("if (parked === view) view.onPause()" in host)
        // پل JS فقط به جلسهٔ فعال می‌رسد
        assertTrue("onText = { text -> session?.onText?.invoke(text) }" in host)
        assertFalse("view.destroy()\n                    }\n                )" in host)
        val main = src("app/src/main/java/ir/exam/app/MainActivity.kt")
        assertTrue("FormulaEditorPool.prepare(this)" in main)
        assertTrue("FormulaEditorPool.release()" in main)
        assertTrue("override fun onTrimMemory(level: Int)" in main)
    }

    @Test
    fun editorOpensOnFirstTickAndSiteKeepsWarmFrame() {
        val html = src("app/src/main/assets/formula_editor/formula.html")
        assertTrue("var opener = setInterval(function () { tick(); }, 120);" in html)
        assertTrue("        tick();\n      }\n    };" in html)
        val app = src("site/src/app.js")
        assertTrue("function prewarmFormula()" in app)
        assertTrue("function makeFormulaOverlay()" in app)
        assertTrue("var w = warmFormula && !warmFormula.failed ? warmFormula : makeFormulaOverlay();" in app)
        assertTrue("c.resolve(c.text); prewarmFormula();" in app)
        assertTrue("loadEngines().then(function () { prewarmFormula();" in app)
        assertTrue("user.role === 'student') return;" in app)
    }
}
