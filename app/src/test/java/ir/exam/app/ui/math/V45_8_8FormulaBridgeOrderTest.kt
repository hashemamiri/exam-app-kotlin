package ir.exam.app.ui.math

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V45_8_8FormulaBridgeOrderTest {
    private fun dialogSource(): String {
        val root = listOf(File("."), File("..")).first {
            File(it, "app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt").isFile
        }
        return File(
            root,
            "app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt"
        ).readText()
    }

    @Test
    fun `viewport fallback remains valid concatenated javascript`() {
        val source = dialogSource()
        assertFalse("dangling quote after a JavaScript comment", "*/' +" in source)
        assertTrue("fallback CSS terminator missing", "s.textContent = css;" in source)
    }

    @Test
    fun `internal close cannot settle bridge before apply result`() {
        val source = dialogSource()
        val apply = source.substringAfter("window.mfApply = function(){")
            .substringBefore("var ic = window.closeMath;")
        assertTrue("apply guard is not enabled", "window.__mbApplyInFlight = true;" in apply)
        assertTrue("apply result is not delivered", "AndroidMathBridge.onApplyResult(v);" in apply)
        assertTrue("apply guard is not released in finally", "finally {\n              window.__mbApplyInFlight = false;" in apply)

        val close = source.substringAfter("window.closeMath = function(){")
            .substringBefore("window.__mbAndroidInstalled = true")
        val guard = close.indexOf("if (window.__mbApplyInFlight) return;")
        val innerClose = close.indexOf("ic.apply(window, arguments)")
        val closedCallback = close.indexOf("AndroidMathBridge.onClosed();")
        assertTrue("apply guard must run before the asset close", guard >= 0 && guard < innerClose)
        assertTrue("apply guard must run before Android onClosed", guard < closedCallback)
    }
}
