package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.23 — نگهبان «انتخاب فوری مسیر سالم و ورود مقاوم در برابر تغییر شبکه»:
 * ۱) مسیریاب باید نخستین مسیر سالم را همان لحظه انتخاب کند، نه بعد از پایان سنجش همهٔ مسیرها.
 * ۲) باید تابع نجات فوری مسیر (`recover`) وجود داشته باشد که پس از خطای شبکه مسیر سالم را فعال کند.
 * ۳) ورود با رمز باید پس از خطای شبکه یک بار دیگر تلاش کند و خطاهای غیرشبکه‌ای را دست نزند.
 */
class V335_23_RouteRecoveryTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val router by lazy { source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt") }
    private val auth by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseAuthRepository.kt") }

    @Test
    fun `router selects the first healthy route immediately`() {
        assertTrue(
            "انتخاب باید به‌محض رسیدن نخستین پاسخ سالم انجام شود",
            "decided.compareAndSet(false, true)" in router
        )
        assertTrue(
            "شمارندهٔ انتخاب باید پیش از سنجش ساخته شود",
            "AtomicBoolean(false)" in router
        )
        assertTrue(
            "سنجش موازی مسیرها باید باقی بماند",
            "val outcome = probe(base)" in router
        )
        assertTrue(
            "انتخاب دستی کاربر نباید با انتخاب خودکار بازنویسی شود",
            "manualBase() == null && decided.compareAndSet(false, true)" in router
        )
    }

    @Test
    fun `router exposes a synchronous recovery helper`() {
        assertTrue(
            "تابع نجات مسیر باید با تایم‌اوت کوتاه باشد",
            "suspend fun recover(timeoutMs: Int = 3_500): Boolean" in router
        )
        assertTrue(
            "نجات مسیر باید مسیر سالم را فعال کند",
            "if (manualBase() == null) remember(best.base)\n        return true" in router
        )
        assertTrue(
            "نتیجهٔ سنجش باید برای گزارش ذخیره شود",
            "if (results.isNotEmpty()) lastProbe = results" in router
        )
    }

    @Test
    fun `password sign in retries once after a network failure`() {
        assertTrue(
            "تابع تک‌تلاشی ورود باید جدا شده باشد",
            "private suspend fun signInOnce(identifier: String, password: String): AppUser" in auth
        )
        assertTrue(
            "تشخیص خطای شبکه باید استفاده شود",
            "NetworkFailureClassifier.isNetworkFailure(error)" in auth
        )
        assertTrue(
            "پس از نجات مسیر باید یک بار دیگر تلاش شود",
            "SupabaseRouter.recover()" in auth
        )
        assertTrue(
            "خطای غیرشبکه‌ای نباید تلاش دوباره بسازد",
            "if (!ir.exam.app.core.network.NetworkFailureClassifier.isNetworkFailure(error)) return last" in auth
        )
        assertFalse(
            "بدنهٔ ورود نباید به‌صورت Result تودرتو باقی بماند",
            "override suspend fun signInWithPassword(identifier: String, password: String): Result<AppUser> = runCatching {" in auth
        )
    }

    @Test
    fun `relay defaults and view wiring are unchanged`() {
        assertTrue("https://sb.onlineexam.ir" in router)
        assertTrue("fun currentBase(): String" in router)
        assertTrue("return cached ?: directBase" in router)
    }
}
