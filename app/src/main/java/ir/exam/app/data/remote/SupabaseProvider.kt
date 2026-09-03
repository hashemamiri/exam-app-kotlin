package ir.exam.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import ir.exam.app.BuildConfig

/** کلاینت مرکزی Supabase؛ Auth، Postgrest و Storage باید همگی نصب شوند. */
object SupabaseProvider {
    @Volatile
    private var appContext: android.content.Context? = null

    /**
     * V75.7 — باید از کلاس Application صدا زده شود (ExamApplication.onCreate)،
     * پیش از هر دسترسی به client؛ در غیر این صورت نشست نمی‌تواند رمزنگاری شود.
     */
    @Synchronized
    fun attach(context: android.content.Context) {
        appContext = context.applicationContext
    }

    val client: SupabaseClient by lazy {
        require(BuildConfig.SUPABASE_URL.isNotBlank()) { "SUPABASE_URL در تنظیمات build وارد نشده است" }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) { "SUPABASE_ANON_KEY در تنظیمات build وارد نشده است" }

        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            httpEngine = OkHttp.create()
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
}
