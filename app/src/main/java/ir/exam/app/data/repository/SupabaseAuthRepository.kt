package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import ir.exam.app.data.dto.ProfileDto
import ir.exam.app.data.local.AuthUserCache
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

class SupabaseAuthRepository(context: Context) : AuthRepository {
    private val auth get() = SupabaseProvider.client.auth
    private val userCache = AuthUserCache(context)
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    override val currentUser: Flow<AppUser?> = _currentUser.asStateFlow()

    /**
     * باید پیش از currentUserOrNull صبر کنیم تا Supabase نشست ذخیره‌شده را از storage بخواند.
     * در نبود اینترنت، نمای cached پروفایل فقط وقتی استفاده می‌شود که id آن با نشست Supabase برابر باشد.
     */
    override suspend fun restoreSession(): Result<AppUser?> = runCatching {
        auth.awaitInitialization()
        val sessionUser = auth.currentUserOrNull()
        if (sessionUser == null) {
            userCache.clear()
            _currentUser.value = null
            return@runCatching null
        }

        val cachedUser = userCache.read(sessionUser.id)
        val refreshedProfile = withTimeoutOrNull(PROFILE_REFRESH_TIMEOUT_MS) {
            try {
                // refreshCurrentSession نشست منقضی را پیش از اولین درخواست Postgrest تازه می‌کند.
                auth.refreshCurrentSession()
                Result.success(currentProfile())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }
        val restoredUser = refreshedProfile?.getOrNull()
            ?: cachedUser
            ?: refreshedProfile?.getOrThrow()
            ?: currentProfile()

        persistUser(restoredUser)
    }

    private suspend fun currentProfile(): AppUser {
        val sessionUser = auth.currentUserOrNull()
            ?: error("نشست ورود پیدا نشد. دوباره وارد شوید.")
        val fallbackName = sessionUser.email?.substringBefore('@').orEmpty().ifBlank { "کاربر" }

        val profile = SupabaseProvider.client.from("profiles").select {
            filter { eq("id", sessionUser.id) }
        }.decodeList<ProfileDto>().firstOrNull() ?: run {
            // فقط نبود واقعی ردیف باعث ساخت پروفایل می‌شود؛ خطای شبکه دیگر با «نبود ردیف» اشتباه نمی‌شود.
            val newProfile = ProfileDto(
                id = sessionUser.id,
                full_name = fallbackName,
                role = "student",
                display_name = fallbackName
            )
            SupabaseProvider.client.from("profiles").upsert(newProfile)
            newProfile
        }

        val role = if (profile.role.lowercase() == "teacher") {
            UserRole.TEACHER
        } else {
            UserRole.STUDENT
        }
        return AppUser(
            id = profile.id,
            name = profile.display_name ?: profile.full_name,
            email = sessionUser.email,
            role = role
        )
    }

    override suspend fun signInWithPassword(email: String, password: String): Result<AppUser> = runCatching {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        persistUser(currentProfile())
    }

    override suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        auth.signInWith(OTP) {
            this.email = email.trim()
            createUser = true
        }
    }

    override suspend fun verifyOtp(email: String, code: String): Result<AppUser> = runCatching {
        auth.verifyEmailOtp(OtpType.Email.EMAIL, email.trim(), code.trim())
        persistUser(currentProfile())
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
        userCache.clear()
        _currentUser.value = null
    }

    private fun persistUser(user: AppUser): AppUser {
        userCache.write(user)
        _currentUser.value = user
        return user
    }

    private companion object {
        const val PROFILE_REFRESH_TIMEOUT_MS = 5_000L
    }
}
