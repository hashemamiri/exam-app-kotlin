package ir.exam.app.ui.image

import android.content.Context
import coil.ImageLoader

/**
 * V75.8 — بارگذار تصویرِ برنامه: همان بارگذار استانداردِ Coil به‌اضافهٔ یک
 * Interceptor که تصاویرِ خصوصیِ Supabase را با توکنِ نشست می‌خواند.
 * این بارگذار در ریشهٔ برنامه (ExamApp) یک‌بار نصب می‌شود و همهٔ AsyncImageها
 * به‌طور خودکار از آن استفاده می‌کنند.
 */
object PrivateImageLoader {
    fun create(context: Context): ImageLoader = ImageLoader.Builder(context)
        // V118 — data-URLهای استودیوی تصویر (Coil 2 خودش پشتیبانی نمی‌کند)
        .components { add(SupabaseAuthImageInterceptor()); add(DataUrlFetcher.Factory()); add(DataUrlFetcher.UriFactory()) }
        .respectCacheHeaders(false)
        .build()
}
