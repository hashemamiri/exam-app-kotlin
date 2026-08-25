package ir.exam.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import ir.exam.app.BuildConfig

/** کلاینت مرکزی Supabase؛ Auth، Postgrest و Storage باید همگی نصب شوند. */
object SupabaseProvider {
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
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
            // V60.0 — ثبت‌نام/ورود Native گوگل؛ اگر GOOGLE_WEB_CLIENT_ID خالی
            // باشد پلاگین نصب می‌شود ولی startFlow به fallback (بی‌اثر) می‌رود.
            install(ComposeAuth) {
                if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                    googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
                }
            }
        }
    }
}
