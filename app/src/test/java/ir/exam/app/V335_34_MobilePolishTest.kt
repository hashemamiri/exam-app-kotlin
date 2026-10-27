package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source contracts; layout is additionally checked in Chromium mobile viewports. */
class V335_34_MobilePolishTest {
    private fun source(path: String): String {
        var root: File? = File("").absoluteFile
        while (root != null && !File(root, "settings.gradle.kts").exists()) root = root.parentFile
        return File(root ?: error("Repository root not found"), path).readText()
    }

    @Test fun diagnosticsHaveNoVisibleEntry() {
        val signIn = source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt")
        val settings = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertTrue("showNetDiag" !in signIn)
        assertTrue("NetworkDiagnosticsCard()" !in signIn)
        assertTrue("NetworkDiagnosticsCard()" !in settings)
        assertTrue("networkOpen" !in settings)
        val router = source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt")
        assertTrue("suspend fun recover(" in router)
    }

    @Test fun nativeCardTimingAndReducedMotion() {
        val css = source("site/src/site.css")
        val mobile = source("site/src/mobile.js")
        assertTrue("animation-duration:480ms" in css)
        assertTrue("animation-timing-function:cubic-bezier(.4,0,.2,1)" in css)
        assertTrue("@keyframes m-native-card-in" in css)
        assertTrue("animation-fill-mode:backwards" in css)
        assertTrue("@media (prefers-reduced-motion:reduce)" in css)
        assertTrue("Math.min(index, 12) * 55" in mobile)
        assertTrue("Math.min(ri, 12) * 55" in mobile)
    }

    @Test fun examSheetHasViewportLimitAndSingleScrollOwner() {
        val css = source("site/src/site.css")
        val builder = source("site/src/builder.js")
        assertTrue("class: 'modal-bg b-exam-settings-bg'" in builder)
        assertTrue("class: 'modal wide b-exam-settings'" in builder)
        assertTrue("height:100vh;height:100dvh" in css)
        assertTrue("max-height:100vh;max-height:100dvh;overflow-y:auto" in css)
        assertTrue(".b-exam-settings .grid3{grid-template-columns:minmax(0,1fr)}" in css)
    }
}
