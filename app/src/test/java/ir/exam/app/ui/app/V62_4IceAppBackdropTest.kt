package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.4 — سه درخواست کاربر:
 * ۱) کل برنامه پس‌زمینهٔ یخی «بدون موج» بگیرد؛ ورود/بازیابی نشست/قفل برنامه
 *    با موج بمانند (IceBackdrop پارامتر waves گرفت + IceAppBackdrop عمومی
 *    با گارد تم تیره).
 * ۲) صفحهٔ قفل برنامه پس‌زمینهٔ یخی با موج بگیرد و پنجرهٔ قفل امن دستگاه
 *    «خودکار» باز شود (LaunchedEffect)؛ دکمهٔ تأیید دستی هم بماند.
 * ۳) اسپینر بازیابی نشست از حالت نئونی خارج و بزرگ‌تر شود (در تست V62_2).
 */
class V62_4IceAppBackdropTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val components by lazy { source("app/src/main/java/ir/exam/app/ui/auth/AuthIceComponents.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val lockUi by lazy { source("app/src/main/java/ir/exam/app/ui/security/AppLockUi.kt") }
    private val overlay by lazy { source("app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt") }
    private val signIn by lazy { source("app/src/main/java/ir/exam/app/ui/auth/SignInScreen.kt") }

    @Test
    fun `the whole app gets the waveless ice backdrop`() {
        // IceBackdrop حالا موج را با پارامتر کنترل می‌کند؛ بدون موج انیمیشن هم ندارد
        assertTrue("internal fun IceBackdrop(modifier: Modifier = Modifier, waves: Boolean = true)" in components)
        assertTrue("if (!waves) return@Canvas" in components)
        // نسخهٔ عمومی با گارد تم تیره (در تاریک همان پس‌زمینهٔ تم)
        assertTrue("fun IceAppBackdrop(modifier: Modifier = Modifier, waves: Boolean = false)" in components)
        assertTrue("scheme.background.luminance() < .42f" in components)
        // پوستهٔ اصلی، منوی همبرگری و پنجرهٔ + بدون موج
        assertTrue("IceAppBackdrop(Modifier.fillMaxSize(), waves = false)" in appShell)
        assertTrue("IceAppBackdrop(Modifier.fillMaxSize(), waves = false)" in overlay)
        // لایه‌های رویی شفاف شدند تا پس‌زمینه دیده شود
        assertTrue("containerColor = androidx.compose.ui.graphics.Color.Transparent" in appShell)
        assertTrue("TopAppBarDefaults.topAppBarColors(" in appShell)
        // صفحهٔ ورود موج‌دار ماند (IceBackdrop پیش‌فرض waves=true)
        assertTrue("IceBackdrop(Modifier.fillMaxSize())" in signIn)
    }

    @Test
    fun `the app lock screen is icy with waves and auto-prompts the device credential`() {
        val gate = lockUi.substringAfter("fun AppLockGate(")
            .substringBefore("fun AppLockSettings(")
        // پس‌زمینهٔ یخی با موج مثل ورود/بازیابی نشست
        assertTrue("IceAppBackdrop(Modifier.fillMaxSize(), waves = true)" in gate)
        // باز شدن خودکار پنجرهٔ قفل بدون کلیک؛ فقط وقتی قفل است
        assertTrue("LaunchedEffect(locked, prompt)" in gate)
        assertTrue("if (locked && prompt != null)" in gate)
        assertTrue("prompt.authenticate(systemPromptInfo())" in gate)
        // دکمهٔ دستی همچنان هست (تلاش دوباره پس از لغو)
        assertTrue("Text(\"تأیید با قفل امن دستگاه\")" in gate)
        // پنجرهٔ خودکار فقط در AppLockGate است، نه در تنظیمات سوییچ
        val settings = lockUi.substringAfter("fun AppLockSettings(")
        assertFalse("LaunchedEffect(locked" in settings)
    }
}
