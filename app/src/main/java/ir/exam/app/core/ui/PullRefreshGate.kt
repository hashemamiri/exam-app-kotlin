package ir.exam.app.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * V229 — دروازهٔ «کشیدن برای بازخوانی» (مشترک بین ViewModelها، جدا از Android تا در تست واحد اجرا شود).
 *
 * قرارداد V227: (۱) تک‌پروازی — تا بازخوانی قبلی تمام نشده، درخواست جدید نادیده گرفته می‌شود؛
 * (۲) پرچم refreshing بلافاصله true می‌شود (نشانگر Material3 فقط وقتی به آستانه می‌رود که isRefreshing بالا رود)؛
 * (۳) حداقل [minVisibleMs] نمایش داده می‌شود تا نشانگر «پرش» نکند؛ (۴) خطای بارگذاری هرگز پرچم را روشن نمی‌گذارد.
 */
class PullRefreshGate(
    private val scope: CoroutineScope,
    private val minVisibleMs: Long = 500L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var job: Job? = null

    val isActive: Boolean get() = job?.isActive == true

    /** true اگر بازخوانی شروع شد؛ false اگر یکی در جریان بود. */
    fun run(setRefreshing: (Boolean) -> Unit, load: suspend () -> Unit): Boolean {
        if (job?.isActive == true) return false
        job = scope.launch {
            setRefreshing(true)
            val started = now()
            try {
                runCatching { load() }
                val rest = minVisibleMs - (now() - started)
                if (rest > 0) delay(rest)
            } finally {
                setRefreshing(false)
            }
        }
        return true
    }
}
