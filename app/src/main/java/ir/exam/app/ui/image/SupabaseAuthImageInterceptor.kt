package ir.exam.app.ui.image

import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
// V75.8.1 — در این کتابخانه auth یک extension روی SupabaseClient است و بدون این
// import کامپایل شکست می‌خورد (خطای Unresolved reference 'auth').
import io.github.jan.supabase.auth.auth
import ir.exam.app.BuildConfig
import ir.exam.app.data.remote.SupabaseProvider

/**
 * V75.8 — بارگذاریِ تصاویرِ خصوصیِ Supabase با توکنِ نشست کاربر.
 *
 * بعد از خصوصی‌شدنِ باکت exam-images، نشانیٔ عمومیِ ذخیره‌شده در دیتابیس دیگر
 * بدون احراز هویت باز نمی‌شود. این Interceptor برای هر درخواستی که به مسیر
 * storage پروژهٔ خودمان می‌رود، دو هدر Authorization و apikey را می‌افزاید؛
 * بنابراین RLSِ Storage تصمیم می‌گیرد چه کسی چه تصویری را ببیند.
 *
 * ویژگی‌های مهمِ طراحی:
 * - نشانی تغییر نمی‌کند ⇒ کشِ دیسکیِ Coil همچنان کار می‌کند و تصویر در حالت
 *   آفلاین (مثلاً هنگام آزمون) دوباره دانلود نمی‌شود.
 * - برای نشانی‌های بیرونی (تصویرِ خارجیِ معلم) هیچ هدری فرستاده نمی‌شود؛
 *   توکنِ کاربر هرگز به سایتِ دیگر نمی‌رود.
 */
class SupabaseAuthImageInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val target = request.data as? String
        if (target == null || !isOwnStorageUrl(target)) return chain.proceed(request)

        // V75.8.1 — نوعِ صریح و استفاده از currentSessionOrNull (عضوِ خودِ Auth)
        // تا هیچ ابهامی در استنتاجِ نوع پیش نیاید.
        // try/catch به‌جای runCatching: فراخوانی ممکن است suspend باشد و
        // runCatching پارامترش suspend نیست (خطای کامپایل در آن حالت قطعی است).
        val token: String? = try {
            SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
        } catch (_: Throwable) {
            null
        }
        if (token.isNullOrBlank()) return chain.proceed(request)

        val authorized: ImageRequest = request.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .build()
        return chain.proceed(authorized)
    }

    private fun isOwnStorageUrl(value: String): Boolean {
        val base = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (base.isBlank()) return false
        return value.startsWith("$base/storage/v1/object/")
    }
}
