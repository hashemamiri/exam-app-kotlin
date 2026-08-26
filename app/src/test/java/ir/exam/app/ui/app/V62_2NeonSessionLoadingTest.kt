package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.2 — درخواست کاربر (با اسکرین‌شات):
 * ۱) «پس‌زمینهٔ کادر در حال بازیابی نشست ورود همانند صفحهٔ لاگین شود» —
 *    IceSessionLoading همان IceBackdrop (گرادیان + هاله + موج) را می‌کشد.
 * ۲) «نوار دایره‌ای... را نئونی و زیبا کن و طرحش را عوض کن» —
 *    NeonIceSpinner: دو کمان چرخان ناهم‌جهت با گرادیان sweep و هالهٔ
 *    نئونی چندلایه + هستهٔ سفید نبض‌دار؛ جایگزین CircularProgressIndicator.
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
            .substringBefore("پس‌زمینهٔ یخی ماژول")
        assertTrue("IceBackdrop(Modifier.fillMaxSize())" in loading)
        assertTrue("NeonIceSpinner()" in loading)
        // متن با رنگ یخی و همان پیام قبلی از ExamApp پاس می‌شود
        assertTrue("color = IceInk" in loading)
        assertTrue("IceSessionLoading(message = \"در حال بازیابی نشست ورود...\")" in appShell)
    }

    @Test
    fun `the spinner is a layered neon design not the plain material ring`() {
        assertTrue("internal fun NeonIceSpinner(" in components)
        val spinner = components.substringAfter("internal fun NeonIceSpinner(")
            .substringBefore("fun IceSessionLoading(")
        // گرادیان sweep + چرخش دو کمان ناهم‌جهت
        assertTrue("Brush.sweepGradient(" in spinner)
        assertTrue("rotate(angle)" in spinner)
        assertTrue("rotate(-angle * 1.4f)" in spinner)
        // هالهٔ نئونی چندلایه (پهن کم‌آلفا روی باریک پررنگ) و هستهٔ نبض‌دار
        assertTrue("width = stroke + glow" in spinner)
        assertTrue("RepeatMode.Reverse" in components)
        assertTrue("radius = 9.dp.toPx() * pulse" in spinner)
        // چرخ سادهٔ متریال از صفحهٔ انتظار حذف شد
        val sessionLoading = appShell.substringAfter("private fun SessionLoadingScreen()")
            .substringBefore("private fun SessionRestoreErrorScreen(")
        assertFalse("CircularProgressIndicator" in sessionLoading)
    }
}
