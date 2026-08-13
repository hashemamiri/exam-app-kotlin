package ir.exam.app.core.security

import android.content.Context

/** وضعیت قفل محلی هر حساب؛ احراز هویت فقط به BiometricPrompt رسمی Android واگذار می‌شود. */
class AppLockManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "native_app_lock",
        Context.MODE_PRIVATE
    )

    fun enabled(userId: String): Boolean {
        preferences.edit()
            .remove("salt_$userId")
            .remove("hash_$userId")
            .remove("device_$userId")
            .apply()
        return preferences.getBoolean("enabled_$userId", false)
    }

    fun setEnabled(userId: String, enabled: Boolean) {
        preferences.edit()
            .putBoolean("enabled_$userId", enabled)
            // پاک‌سازی امن بقایای PIN نسخه‌های قبلی؛ PIN دیگر مسیر احراز هویت نیست.
            .remove("salt_$userId")
            .remove("hash_$userId")
            .remove("device_$userId")
            .apply()
    }
}
