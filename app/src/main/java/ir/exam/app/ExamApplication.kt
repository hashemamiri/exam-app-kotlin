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
    }
}
