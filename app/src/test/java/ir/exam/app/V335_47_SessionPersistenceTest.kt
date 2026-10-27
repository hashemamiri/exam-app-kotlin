package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.47 — نگهبان «ماندگاری نشست ورود».
 *
 * شکایت میدانی: «اپ و سایت بعد از آپدیت/رفرش/باز و بستن، لاگین نمی‌مانند».
 *
 * سایت (اینترنت ضعیف): تازه‌سازی با تایم‌اوت ۸ ثانیه‌ای شکست می‌خورد، درخواست با توکن
 * منقضی می‌رفت، ۴۰۱ می‌گرفت و بوت نشست را پاک می‌کرد ⇒ خروج بی‌دلیل.
 * اپ (بعد از آپدیت): خواندن Keystore هر استثنایی را «خرابی» می‌دید و نشستِ سالم را پاک می‌کرد.
 *
 * این تست همان رفتارهای تازه را قفل می‌کند.
 */
class V335_47_SessionPersistenceTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun src(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val site = src("site/src/app.js")
    private val storage = src("app/src/main/java/ir/exam/app/core/security/SecureSessionStorage.kt")
    private val manager = src("app/src/main/java/ir/exam/app/data/remote/EncryptedSessionManager.kt")

    @Test
    fun `site retries a 401 once after forcing a refresh`() {
        assertTrue("پرچم تکرار", "__authRetry" in site)
        assertTrue("تازه‌سازی اجباری", "function refreshSession(force)" in site)
        assertTrue("تکرار همان درخواست", "return http(path, retryOpts)" in site)
    }

    @Test
    fun `site never logs out on network trouble`() {
        assertTrue("عقب‌نشینی شبکه", "NET_REFRESH_BACKOFF_MS" in site)
        assertTrue("ثبت خطای شبکه", "refresh-network" in site)
        assertFalse("بوت دیگر پاک نمی‌کند", "if (/نشست|JWT|401/.test(errMsg(e))) saveSession(null)" in site)
    }

    @Test
    fun `site adopts a session refreshed by another tab`() {
        assertTrue("پذیرش از تب دیگر", "adopt-other-tab" in site)
        assertTrue("همگام‌سازی storage", "storage-sync" in site)
        assertTrue("دفترچهٔ تشخیص", "sessionDiag(" in site)
    }

    @Test
    fun `app read retries and keeps the blob on transient errors`() {
        assertTrue("خواندن با تلاش دوباره", "readWithRetry" in storage)
        assertTrue("ثبت آخرین خطا", "lastError" in storage)
        assertTrue("پیام نگه‌داشتن داده", "داده دست‌نخورده ماند" in storage)
        assertTrue("خرابی ساختاری", "corrupt-payload" in storage)
    }

    @Test
    fun `app decodes leniently after a library change`() {
        assertTrue("خواندن سهل‌گیر", "coerceInputValues = true" in manager)
        assertTrue("برچسب تشخیص", "SessionDiag" in manager)
        assertTrue("نگه‌داشتن داده برای نسخهٔ بعد", "دست‌نخورده ماند" in manager)
    }
}
