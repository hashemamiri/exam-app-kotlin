package ir.exam.app.data.remote

import android.content.Context
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
 */
class EncryptedSessionManager(context: Context) : SessionManager {
    private val storage = SecureSessionStorage(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun saveSession(session: UserSession) {
        storage.write(json.encodeToString(UserSession.serializer(), session))
    }

    override suspend fun loadSession(): UserSession? =
        storage.read()?.let { text ->
            runCatching { json.decodeFromString(UserSession.serializer(), text) }.getOrNull()
        }

    override suspend fun deleteSession() {
        storage.clear()
    }
}
