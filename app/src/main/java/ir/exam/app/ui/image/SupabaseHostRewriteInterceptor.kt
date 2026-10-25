package ir.exam.app.ui.image

import coil.intercept.Interceptor
import coil.request.ImageResult

/**
 * V335.20 — پیش از خواندن تصویر، اگر نشانی از خودِ پروژهٔ Supabase باشد و در این شبکه دسترسی مستقیم
 * به آن بسته باشد، همان تصویر از «مسیر جایگزین» (دامنهٔ خودمان) خوانده می‌شود. بدون این لایه،
 * تصویرهای سؤال در شبکه‌هایی مثل دادهٔ همراه اول هیچ‌وقت بارگذاری نمی‌شدند.
 *
 * این Interceptor باید پیش از [SupabaseAuthImageInterceptor] نصب شود تا توکن نشست روی نشانی
 * نهایی (همان دامنهٔ خودمان) فرستاده شود، نه دامنهٔ بستهٔ Supabase.
 */
class SupabaseHostRewriteInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data is String) {
            val rewritten = ir.exam.app.core.network.SupabaseRouter.rewrite(data)
            if (rewritten != data) return chain.proceed(request.newBuilder().data(rewritten).build())
        }
        return chain.proceed(request)
    }
}
