package ir.exam.app.data.remote

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import ir.exam.app.core.security.SecureSessionStorage
import kotlinx.serialization.json.Json

/**
 * V75.7 — نشستِ Supabase را به‌جای SharedPreferences ساده، در حافظهٔ رمزنگاری‌شده
 * با کلیدِ Android Keystore نگه می‌دارد (بند ۳.۵ گزارش امنیتی).
 *
 * پیش از این، پلاگین Auth نشست را با SettingsSessionManager در یک فایل XML
 * ذخیره می‌کرد؛ هر دسترسیِ فایلی (دستگاه روت‌شده، بدافزار، پشتیبان غیررسمی)
 * access token و refresh token را به‌صورت متن ساده لو می‌داد.
 *
 * V335.47 — اگر خواندنِ رمزنگاری‌شده به‌دلیل گذرا شکست بخورد، Supabase ناچار نبود نشست را
 * از دست بدهد: `readWithRetry` چند بار تلاش می‌کند و داده را پاک نمی‌کند. همچنین اگر
 * رمزگشایی شد ولی JSON با نسخهٔ فعلی کتابخانه نخواند (تغییر نسخه بین دو آپدیت)، یک
 * خواندنِ سهل‌گیر انجام می‌شود تا کاربر مجبور به ورود دوباره نشود.
 */
class EncryptedSessionManager(context: Context) : SessionManager {
    private val storage = SecureSessionStorage(context.applicationContext)
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** V335.47 — خواندن دوم با نرمی بیشتر: فیلد اضافه/نوع ساده‌شده مانع بازیابی نشست نشود. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val lenient = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun saveSession(session: UserSession) {
        storage.write(json.encodeToString(UserSession.serializer(), session))
    }

    override suspend fun loadSession(): UserSession? {
        val text = storage.readWithRetry() ?: return null
        runCatching { json.decodeFromString(UserSession.serializer(), text) }
            .onSuccess { return it }
            .onFailure { error ->
                Log.w(TAG, "خواندن نشست با تنظیمات سخت‌گیرانه نشد (" + (error.message ?: "").take(80) + ")؛ تلاش سهل‌گیر")
            }
        return runCatching { lenient.decodeFromString(UserSession.serializer(), text) }
            .onSuccess { Log.w(TAG, "نشست با خواندن سهل‌گیر بازیابی شد") }
            .onFailure { error ->
                Log.e(TAG, "نشست ذخیره‌شده خوانده نشد؛ داده برای نسخهٔ بعدی دست‌نخورده ماند (" +
                    (error.javaClass.simpleName ?: "") + ")")
            }
            .getOrNull()
    }

    override suspend fun deleteSession() {
        storage.clear()
    }

    private companion object {
        const val TAG = "SessionDiag"
    }
}
