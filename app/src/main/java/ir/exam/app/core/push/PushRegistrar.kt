package ir.exam.app.core.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import ir.exam.app.BuildConfig
import ir.exam.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.coroutines.resume

/**
 * V232 — ثبت/حذف توکن FCM روی سرور (RPC native_push_register_v1 / native_push_unregister_v1).
 * فقط وقتی اپ با google-services.json ساخته شده (BuildConfig.PUSH_ENABLED) کاری می‌کند.
 * ترجیح کاربر («اعلان‌ها» در تنظیمات) محلی است؛ خاموش = توکن از سرور حذف می‌شود.
 */
object PushRegistrar {
    private const val PREFS = "native_push"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_TOKEN = "last_token"
    private const val KEY_LAST_USER = "last_user"

    val available: Boolean get() = BuildConfig.PUSH_ENABLED

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    /* ===== V326 — دسته‌بندی اعلان و بازهٔ سکوت =====
       پیش‌تر یک کلید کلی بود؛ معلم نمی‌توانست اعلان نمره را نگه دارد و پیام تقویم را خاموش کند.
       کلیدها در همان SharedPreferences محلی‌اند و در PushMessagingService پیش از نمایش اعمال می‌شوند. */
    val CATEGORIES = listOf("grades", "exam", "calendar", "requests")

    fun isCategoryEnabled(context: Context, category: String): Boolean {
        if (category !in CATEGORIES) return true
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("cat_" + category, true)
    }

    fun setCategoryEnabled(context: Context, category: String, enabled: Boolean) {
        if (category !in CATEGORIES) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("cat_" + category, enabled).apply()
    }

    /** بازهٔ سکوت به دقیقه از نیمه‌شب (مثلاً ۲۳:۰۰ تا ۷:۰۰ → 1380 تا 420). صفر/صفر = خاموش. */
    fun quietRange(context: Context): Pair<Int, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (p.getInt("quiet_from", 0) to p.getInt("quiet_to", 0))
    }

    fun setQuietRange(context: Context, fromMin: Int, toMin: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("quiet_from", fromMin.coerceIn(0, 1439))
            .putInt("quiet_to", toMin.coerceIn(0, 1439)).apply()
    }

    /** آیا الان در بازهٔ سکوت هستیم؟ (بازه می‌تواند از نیمه‌شب بگذرد) */
    fun inQuietHours(context: Context, nowMinuteOfDay: Int = java.util.Calendar.getInstance()
            .let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }): Boolean {
        val (from, to) = quietRange(context)
        if (from == to) return false
        return if (from < to) nowMinuteOfDay in from until to else (nowMinuteOfDay >= from || nowMinuteOfDay < to)
    }

    /** تصمیم نمایش: کلید کلی + دستهٔ پیام + بازهٔ سکوت. */
    fun shouldShow(context: Context, page: String?): Boolean {
        if (!isEnabled(context)) return false
        val category = categoryOf(page)
        if (!isCategoryEnabled(context, category)) return false
        return !inQuietHours(context)
    }

    /** صفحهٔ مقصد پیام سرور → دستهٔ تنظیمات */
    fun categoryOf(page: String?): String = when (page) {
        "grades" -> "grades"
        "exam" -> "exam"
        "calendar" -> "calendar"
        "requests", "grading", "students" -> "requests"
        else -> "exam"
    }

    suspend fun setEnabled(context: Context, enabled: Boolean, userId: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) sync(context, userId, force = true) else unregister(context)
    }

    /** پس از ورود/بازیابی نشست: اگر توکن یا کاربر عوض شده، دوباره ثبت می‌شود. */
    suspend fun sync(context: Context, userId: String?, force: Boolean = false) {
        if (!available || userId.isNullOrBlank() || !isEnabled(context)) return
        val token = currentToken() ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!force && prefs.getString(KEY_LAST_TOKEN, null) == token && prefs.getString(KEY_LAST_USER, null) == userId) return
        register(context, token, userId)
    }

    /** از PushMessagingService.onNewToken */
    suspend fun onNewToken(context: Context, token: String) {
        if (!available || !isEnabled(context)) return
        val userId = runCatching { SupabaseProvider.client.auth.currentUserOrNull()?.id }.getOrNull() ?: return
        register(context, token, userId)
    }

    private suspend fun register(context: Context, token: String, userId: String) {
        runCatching {
            withContext(Dispatchers.IO) {
                SupabaseProvider.client.postgrest.rpc(
                    "native_push_register_v1",
                    buildJsonObject { put("p_token", JsonPrimitive(token)); put("p_platform", JsonPrimitive("android")) }
                ).decodeAs<JsonObject>()
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_LAST_TOKEN, token).putString(KEY_LAST_USER, userId).apply()
        }.onFailure { Log.w("PushRegistrar", "register failed: ${it.message}") }
    }

    private suspend fun unregister(context: Context) {
        if (!available) return
        val token = currentToken() ?: return
        runCatching {
            withContext(Dispatchers.IO) {
                SupabaseProvider.client.postgrest.rpc(
                    "native_push_unregister_v1",
                    buildJsonObject { put("p_token", JsonPrimitive(token)) }
                ).decodeAs<JsonObject>()
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LAST_TOKEN).remove(KEY_LAST_USER).apply()
        }.onFailure { Log.w("PushRegistrar", "unregister failed: ${it.message}") }
    }

    private suspend fun currentToken(): String? = runCatching {
        suspendCancellableCoroutine<String?> { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
    }.getOrNull()

    /** V326 — حذف کامل توکن دستگاه از سرور و پاک‌کردن وضعیت محلی (برای «حذف داده‌های محلی»). */
    suspend fun unregisterAll(context: Context) {
        runCatching { unregister(context) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_LAST_TOKEN).remove(KEY_LAST_USER).apply()
    }
}
