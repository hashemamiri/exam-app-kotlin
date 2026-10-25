package ir.exam.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.24 — نگهبان «تاب‌آوری مسیر جایگزین در برابر خطای گذرای DNS و شبکهٔ موبایل»:
 * ۱) فهرست مسیرهای پیش‌فرض باید بیش از یک نام داشته باشد (زیردامنهٔ تازه ممکن است موقتاً
 *    در کش منفی DNS نباشد).
 * ۲) سنجش هر مسیر باید خطای «نام دامنه پیدا نشد» را یک‌بار دیگر بیازماید.
 * ۳) ورود باید تا سه تلاش، با فاصلهٔ کوتاه و سنجش دوبارهٔ مسیر، تکرار شود.
 */
class V335_24_RelayDnsResilienceTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val auth by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt") }

    @Test
    fun `default relay list carries a second freshly named route`() {
        assertTrue("فهرست مسیرهای پیش‌فرض باید تعریف شود", "val DEFAULT_RELAYS: List<String> = listOf(" in router)
        assertTrue("مسیر اصلی باید همان sb.onlineexam.ir بماند", "const val DEFAULT_RELAY = \"https://sb.onlineexam.ir\"" in router)
        assertTrue("نام تازهٔ دوم باید در فهرست باشد", "\"https://relay.onlineexam.ir\"" in router)
        assertTrue("در نبود تنظیم زمان ساخت باید فهرست پیش‌فرض برگردد", "return configured.ifEmpty { DEFAULT_RELAYS }" in router)
    }

    @Test
    fun `probe retries once on a transient dns failure`() {
        assertTrue("تابع سنجش باید تلاش را بشمارد", "fun probe(base: String, timeoutMs: Int = 6_000): Probe = probe(base, timeoutMs, 0)" in router)
        assertTrue("تلاش دوباره فقط برای خطای نام دامنه و یک‌بار", "if (e is UnknownHostException && attempt < 1)" in router)
        assertTrue("تلاش دوباره باید بازگشتی باشد", "return probe(base, timeoutMs, attempt + 1)" in router)
    }

    @Test
    fun `sign in repeats up to three attempts only for network failures`() {
        assertTrue("دو تلاش دوباره باید وجود داشته باشد", "repeat(2) { attempt ->" in auth)
        assertTrue("فاصلهٔ کوتاه بین تلاش‌ها", "delay(600L * (attempt + 1))" in auth)
        assertTrue("خطای غیرشبکه‌ای نباید تلاش دوباره بسازد", "if (!ir.exam.app.core.network.NetworkFailureClassifier.isNetworkFailure(error)) return last" in auth)
        assertTrue("نتیجهٔ آخر باید برگردد", "return last" in auth)
    }

    @Test
    fun `route selection and relay report stay intact`() {
        assertTrue("انتخاب فوری مسیر سالم", "decided.compareAndSet(false, true)" in router)
        assertTrue("نجات مسیر در دسترس است", "suspend fun recover(timeoutMs: Int = 3_500): Boolean" in router)
        assertTrue("نشانی مستقیم حفظ شود", "val directBase: String get() = BuildConfig.SUPABASE_URL" in router)
    }
}
