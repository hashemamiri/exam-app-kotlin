package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.2 — پس‌زمینهٔ یخی و اسپینر اختصاصی صفحهٔ «در حال بازیابی نشست ورود».
 * V62.4 — به درخواست کاربر اسپینر از حالت نئونی خارج شد (هاله‌ها و هستهٔ
 * نبض‌دار حذف؛ NeonIceSpinner → IceSpinner) و بزرگ‌تر شد (۹۶dp).
 */
class V62_2NeonSessionLoadingTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val components by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthIceComponents.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }

    @Test
    fun `session loading uses the ice backdrop like the login screen`() {
        assertTrue("fun IceSessionLoading(message: String)" in components)
        val loading = components.substringAfter("fun IceSessionLoading(")
            .substringBefore("fun IceAppBackdrop(")
        assertTrue("IceBackdrop(Modifier.fillMaxSize())" in loading)
        assertTrue("IceSpinner()" in loading)
        // متن با رنگ یخی و همان پیام قبلی از ExamApp پاس می‌شود
        assertTrue("color = IceInk" in loading)
        assertTrue("IceSessionLoading(message = \"در حال بازیابی نشست ورود...\")" in appShell)
    }

    @Test
    fun `the spinner is the larger non-neon dual arc design`() {
        assertTrue("internal fun IceSpinner(" in components)
        val spinner = components.substringAfter("internal fun IceSpinner(")
            .substringBefore("fun IceSessionLoading(")
        // گرادیان sweep + چرخش دو کمان ناهم‌جهت، بزرگ‌تر از قبل
        assertTrue("Brush.sweepGradient(" in spinner)
        assertTrue("rotate(angle)" in spinner)
        // V64.6 — حلقهٔ سفید باید یک دور کاملِ هم‌درز داشته باشد تا restart نپرد.
        assertTrue("val innerAngle by transition.animateFloat" in spinner)
        assertTrue("rotate(-innerAngle + 160f)" in spinner)
        assertFalse("angle * 1.4f" in spinner)
        assertTrue("modifier.size(96.dp)" in spinner)
        // V62.4 — بدون هالهٔ نئونی و هستهٔ نبض‌دار
        assertFalse("glow" in spinner)
        assertFalse("pulse" in spinner)
        // چرخ سادهٔ متریال از صفحهٔ انتظار حذف شد
        val sessionLoading = appShell.substringAfter("private fun SessionLoadingScreen()")
            .substringBefore("private fun SessionRestoreErrorScreen(")
        assertFalse("CircularProgressIndicator" in sessionLoading)
    }
}
