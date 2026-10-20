package ir.exam.app

import android.app.Application
import ir.exam.app.data.remote.SupabaseProvider

/**
 * V75.7 — نقطهٔ آغاز برنامه: پیش از هر چیز، زمینهٔ برنامه را در اختیار
 * SupabaseProvider می‌گذارد تا نشستِ ورود در حافظهٔ رمزنگاری‌شده نگه‌داری شود.
 */
class ExamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseProvider.attach(this)
        ir.exam.app.core.cache.ExamListCache.attach(this) // V213 — فهرست آزمون‌ها روی دیسک
        // V321 — عکس فوری کارنامه/آمار برای باز شدن آفلاین (فقط عنوان/نام/درصد، بدون سؤال و پاسخ)
        ir.exam.app.core.cache.ReportsSnapshot.attachDir(java.io.File(filesDir, "list_cache"))
    }
}
