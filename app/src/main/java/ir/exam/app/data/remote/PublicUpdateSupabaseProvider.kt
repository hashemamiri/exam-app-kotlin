package ir.exam.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import ir.exam.app.BuildConfig
import ir.exam.app.core.network.SupabaseRouter

/**
 * کلاینت فقط‌خواندنی عمومی برای app_version.
 * Auth روی آن نصب نمی‌شود تا access token منقضی کاربر به درخواست بررسی نسخه تزریق نشود.
 *
 * V335.20 — بررسی نسخه هم از همان مسیر فعال (`SupabaseRouter`) می‌رود تا در شبکه‌هایی که دامنهٔ
 * مستقیم بسته است، پیام «بروزرسانی» هم بدون خطا کار کند.
 */
object PublicUpdateSupabaseProvider {
    val client: SupabaseClient by lazy {
        require(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL در تنظیمات build وارد نشده است"
        }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY در تنظیمات build وارد نشده است"
        }
        val baseUrl = SupabaseRouter.currentBase().ifBlank { BuildConfig.SUPABASE_URL.trim().trimEnd('/') }

        createSupabaseClient(
            supabaseUrl = baseUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            useHTTPS = !BuildConfig.SUPABASE_URL.startsWith("http://", ignoreCase = true)
            httpEngine = OkHttp.create()
            install(Postgrest)
        }
    }
}
