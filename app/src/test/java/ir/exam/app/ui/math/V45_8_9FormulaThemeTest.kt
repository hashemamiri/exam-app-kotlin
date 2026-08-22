package ir.exam.app.ui.math

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V45_8_9FormulaThemeTest {
    private fun projectRoot(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/assets/math_editor_standalone.html").isFile
    }

    @Test
    fun `static light host theme is absent while native dark theme remains`() {
        val asset = File(
            projectRoot(),
            "app/src/main/assets/math_editor_standalone.html"
        ).readText()

        assertFalse(
            "static hostThemeOverride makes the Android formula page white",
            "<style id=\"hostThemeOverride\">" in asset
        )
        assertTrue("native dark background was removed", "--bg1:#0f0c29" in asset)
        assertTrue("native light-on-dark text was removed", "--text:#fff" in asset)
        assertTrue(
            "dynamic host theme API must remain available for explicit hosts",
            "window.__mathHostTheme" in asset && "var __HOST_THEME_CSS" in asset
        )
    }

    @Test
    fun `android strips future host theme injection before loading completes`() {
        val source = File(
            projectRoot(),
            "app/src/main/java/ir/exam/app/ui/math/MathEditorWebViewDialog.kt"
        ).readText()

        val started = source.indexOf("override fun onPageStarted")
        val load = source.indexOf("wb.loadUrl(ASSET_URL)")
        assertTrue("onPageStarted hook is missing or too late", started >= 0 && started < load)
        assertTrue(
            "early theme-strip script is not evaluated from onPageStarted",
            "evaluateJavascript(EARLY_THEME_STRIP_JS, null)" in source
        )
        assertTrue("dark WebView fallback is missing", "background:var(--bg1)" in source)
        assertFalse("legacy forced grid layout must not return", "__mbForceLayout" in source)
    }
}
