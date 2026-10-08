package ir.exam.app.core.cache

import ir.exam.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import java.util.concurrent.ConcurrentHashMap

/**
 * V212 — حافظهٔ موقتِ درون‌برنامه‌ای (فقط RAM) برای فهرست‌هایی که با هر بار ورود به صفحه
 * دوباره از سرور خوانده می‌شدند (داشبورد، تصحیح، آمار، کیف پول، نتایج، کلاس‌ها).
 *
 * الگو: صفحه اول دادهٔ قبلی را فوراً نشان می‌دهد (بدون چرخ انتظار) و هم‌زمان نسخهٔ تازه
 * را از سرور می‌گیرد و جایگزین می‌کند (stale-while-revalidate). کلیدها با شناسهٔ کاربر
 * جاری پیشونددار می‌شوند و در خروج از حساب پاک می‌شوند تا دادهٔ یک کاربر به کاربر بعدی
 * نشت نکند. هیچ‌چیز روی دیسک نوشته نمی‌شود.
 */
object SessionCache {
    private val store = ConcurrentHashMap<String, Any>()

    private fun uid(): String = runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.id }.getOrNull().orEmpty()

    private fun fullKey(key: String): String = uid() + "|" + key

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? = store[fullKey(key)] as? T

    fun put(key: String, value: Any) { store[fullKey(key)] = value }

    fun remove(key: String) { store.remove(fullKey(key)) }

    /** خروج از حساب / تعویض کاربر: همهٔ داده‌های موقت پاک می‌شوند. */
    fun clear() = store.clear()
}
