package ir.exam.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import ir.exam.app.BuildConfig
import ir.exam.app.core.network.SupabaseRouter

/**
 * کلاینت مرکزی Supabase؛ Auth، Postgrest و Storage باید همگی نصب شوند.
 *
 * V335.20 — نشانی کلاینت از `SupabaseRouter` می‌آید: اگر دسترسی مستقیم به دامنهٔ پروژه در شبکه‌ای
 * (مثل دادهٔ همراه اول) بسته باشد، برنامه از «مسیر جایگزین» روی دامنهٔ خودمان وصل می‌شود. با تغییر
 * شبکه یا تغییر مسیر، `reset()` صدا زده می‌شود و کلاینت با نشانی تازه دوباره ساخته می‌شود.
 */
object SupabaseProvider {
    @Volatile
    private var appContext: android.content.Context? = null

    @Volatile
    private var testOverride: SupabaseClient? = null

    /** V335.20 — کلاینت ساخته‌شده برای نشانی فعلی (با تغییر مسیر پاک می‌شود). */
    @Volatile
    private var cachedClient: SupabaseClient? = null

    private val buildLock = Any()

    internal fun overrideForTesting(client: SupabaseClient?) {
        testOverride = client
    }

    /**
     * V75.7 — باید از کلاس Application صدا زده شود (ExamApplication.onCreate)،
     * پیش از هر دسترسی به client؛ در غیر این صورت نشست نمی‌تواند رمزنگاری شود.
     */
    @Synchronized
    fun attach(context: android.content.Context) {
        appContext = context.applicationContext
    }

    /** V335.20 — دور انداختن کلاینت فعلی تا با مسیر تازهٔ شبکه دوباره ساخته شود. */
    fun reset() {
        synchronized(buildLock) { cachedClient = null }
    }

    private fun buildClient(): SupabaseClient {
        require(BuildConfig.SUPABASE_URL.isNotBlank()) { "SUPABASE_URL در تنظیمات build وارد نشده است" }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) { "SUPABASE_ANON_KEY در تنظیمات build وارد نشده است" }
        val baseUrl = SupabaseRouter.currentBase().ifBlank { BuildConfig.SUPABASE_URL.trim().trimEnd('/') }

        return createSupabaseClient(
            supabaseUrl = baseUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            useHTTPS = !BuildConfig.SUPABASE_URL.startsWith("http://", ignoreCase = true)
            // V335.27 — کلاینت OkHttp مشترک با نشانی تثبیت‌شده (RelayDns) تا در شبکه‌ای که DNS
            // نام دامنه را پیدا نمی‌کند، اتصال از راه نشانی درست همان سرور انجام شود.
            httpEngine = OkHttp.create { preconfigured = ir.exam.app.core.network.RelayHttp.client }
            install(Auth) {
                autoLoadFromStorage = true
                autoSaveToStorage = true
                alwaysAutoRefresh = true
                // V75.7 — نشست در حافظهٔ رمزنگاری‌شده (Keystore) ذخیره می‌شود،
                // نه در فایل XML سادهٔ SharedPreferences.
                val context = appContext
                    ?: error("SupabaseProvider.attach(context) در ExamApplication صدا زده نشده است")
                sessionManager = EncryptedSessionManager(context)
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }

    val client: SupabaseClient
        get() = testOverride ?: cachedClient ?: synchronized(buildLock) {
            cachedClient ?: buildClient().also { cachedClient = it }
        }
}
