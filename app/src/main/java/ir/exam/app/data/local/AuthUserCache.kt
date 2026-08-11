package ir.exam.app.data.local

import android.content.Context
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole

/**
 * فقط نمای عمومی پروفایل را نگه می‌دارد؛ access token و refresh token هرگز اینجا ذخیره نمی‌شوند.
 * خود Supabase Auth نشست و tokenها را با storage داخلی پلاگین مدیریت می‌کند.
 */
class AuthUserCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun read(expectedUserId: String): AppUser? {
        val id = preferences.getString(KEY_ID, null) ?: return null
        if (id != expectedUserId) return null

        val role = preferences.getString(KEY_ROLE, null)
            ?.let { value -> runCatching { UserRole.valueOf(value) }.getOrNull() }
            ?: return null
        val name = preferences.getString(KEY_NAME, null)?.takeIf(String::isNotBlank)
            ?: return null

        return AppUser(
            id = id,
            name = name,
            email = preferences.getString(KEY_EMAIL, null),
            role = role,
            avatarUrl = preferences.getString(KEY_AVATAR_URL, null)
        )
    }

    fun write(user: AppUser) {
        preferences.edit()
            .putString(KEY_ID, user.id)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_ROLE, user.role.name)
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .commit()
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "persisted_auth_user"
        const val KEY_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
        const val KEY_AVATAR_URL = "avatar_url"
    }
}
