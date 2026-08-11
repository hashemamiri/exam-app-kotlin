package ir.exam.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
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
        }
    }
}
