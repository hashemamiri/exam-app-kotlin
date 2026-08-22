package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V45.1 — رفع «دانلود بروزرسانی انجام نمی‌شود»:
 *
 * ۱) پنجره «بروزرسانی جدید» هنگام ورود دیگر با لمس «دریافت نسخه» بی‌صدا بسته نمی‌شود؛
 *    در حالت دانلود نوار پیشرفت و متن «در انتظار اتصال اینترنت…» را نشان می‌دهد.
 * ۲) خطای واقعی دانلود در همان پنجره نمایش داده می‌شود و «تلاش دوباره» دارد.
 * ۳) پس از کامل‌شدن دانلود، نصب‌کننده خودکار (با مسیر مجوز نصب) باز می‌شود.
 * ۴) DownloadManager بی‌صدا در PAUSED نمی‌ماند؛ توقف ۱۲۰ ثانیه‌ای لغو و اعلام می‌شود.
 * ۵) در نبود موفقیت دانلودکننده سیستم، «دریافت با مرورگر» به‌عنوان مسیر جایگزین هست.
 */
class V45_1UpdateDownloadFixTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val about by lazy { source("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt") }
    private val viewModel by lazy { source("app/src/main/java/ir/exam/app/ui/update/UpdateViewModel.kt") }
    private val apkManager by lazy {
        source("app/src/main/java/ir/exam/app/core/update/ApkUpdateManager.kt")
    }

    // ============================================================
    // ۱) پنجره ورود: دیالوگ هنگام دانلود باز می‌ماند و پیشرفت نشان می‌دهد
    // ============================================================

    @Test
    fun `login update prompt stays open while downloading and shows progress`() {
        assertTrue("UpdatePromptDialog(" in appShell)
        assertTrue("downloading = updateState.downloading" in appShell)
        assertTrue("LinearProgressIndicator(progress = { it }" in appShell)
        assertTrue("progressText" in appShell)
        // رفتار معیوب قدیمی (بستن فوری دیالوگ هنگام شروع دانلود) حذف شده است:
        assertFalse("""onClick = {
                        updatePromptDismissed = true
                        updateViewModel.downloadAndInstall()
                    }""" in appShell)
    }

    @Test
    fun `waiting for network text is shown in prompt and about screens`() {
        assertTrue("در انتظار اتصال اینترنت…" in viewModel)
        assertTrue("در حال دریافت نسخه" in appShell)
        assertTrue("پنهان‌کردن" in appShell)
    }

    // ============================================================
    // ۲) خطای واقعی + تلاش دوباره
    // ============================================================

    @Test
    fun `download error is visible in login prompt with retry action`() {
        assertTrue("error = updateState.error" in appShell)
        assertTrue("error != null -> Button(onClick = onDownload)" in appShell)
        assertTrue("تلاش دوباره" in appShell)
    }

    @Test
    fun `about screen keeps error visible and adds browser fallback`() {
        assertTrue("state.error?.let { error ->" in about)
        assertTrue("دریافت با مرورگر" in about)
        assertTrue("Intent.ACTION_VIEW" in about)
        assertTrue("Intent.ACTION_VIEW" in appShell)
    }

    // ============================================================
    // ۳) نصب خودکار پس از کامل‌شدن دانلود (مسیر پنجره ورود)
    // ============================================================

    @Test
    fun `auto install launches after download completes in login flow`() {
        assertTrue("LaunchedEffect(updateState.autoInstallPending, updateState.downloadedApkPath)" in appShell)
        assertTrue("updateViewModel.markAutoInstallHandled()" in appShell)
        assertTrue("requestInstaller(path)" in appShell)
        assertTrue("updateInstallLauncher" in appShell)
        assertTrue("canRequestPackageInstalls()" in appShell)
        assertTrue("unknownSourcesSettingsIntent()" in appShell)
    }

    // ============================================================
    // ۴) تشخیص توقف بی‌صدای DownloadManager
    // ============================================================

    @Test
    fun `silent paused download is detected and cancelled with clear message`() {
        assertTrue("DOWNLOAD_STALL_TIMEOUT_MS" in apkManager)
        assertTrue("120_000L" in apkManager)
        assertTrue("دانلود بروزرسانی متوقف شده است" in apkManager)
        assertTrue("NETWORK_PAUSE_REASONS" in apkManager)
        assertTrue("PAUSED_WAITING_FOR_NETWORK" in apkManager)
        assertTrue("PAUSED_WAITING_FOR_WIFI" in apkManager)
    }

    @Test
    fun `waiting for network is propagated to state and shown as text`() {
        assertTrue("waitingForNetwork" in apkManager)
        assertTrue("waitingForNetwork: Boolean = false" in viewModel)
        assertTrue("waitingForNetwork = progress.waitingForNetwork" in viewModel)
        assertTrue("val progressText" in viewModel)
    }
}
