package ir.exam.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.ui.image.PrivateImageLoader

/**
 * V75.7 — نقطهٔ آغاز برنامه: پیش از هر چیز، زمینهٔ برنامه را در اختیار
 * SupabaseProvider می‌گذارد تا نشستِ ورود در حافظهٔ رمزنگاری‌شده نگه‌داری شود.
 * V335.2 — پیاده‌سازی ImageLoaderFactory به‌جای LocalImageLoader منسوخ در Coil.
 */
class ExamApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        SupabaseProvider.attach(this)
        ir.exam.app.core.cache.ExamListCache.attach(this) // V213 — فهرست آزمون‌ها روی دیسک
        // V321 — عکس فوری کارنامه/آمار برای باز شدن آفلاین (فقط عنوان/نام/درصد، بدون سؤال و پاسخ)
        ir.exam.app.core.cache.ReportsSnapshot.attachDir(java.io.File(filesDir, "list_cache"))
    }

    override fun newImageLoader(): ImageLoader = PrivateImageLoader.create(this)
}
