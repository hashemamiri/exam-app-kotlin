package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** پیاده‌سازی Native ورود با ایمیل/رمز و OTP. نقش در فاز پروفایل از جدول users خوانده می‌شود. */
class SupabaseAuthRepository : AuthRepository {
    private val auth get() = SupabaseProvider.client.auth
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    override val currentUser: Flow<AppUser?> = _currentUser.asStateFlow()

    override suspend fun signInWithPassword(email: String, password: String): Result<AppUser> = runCatching {
        auth.signInWith(Email) { this.email = email.trim(); this.password = password }
        val user = auth.currentUserOrNull() ?: error("ورود انجام نشد")
        AppUser(id = user.id, name = user.email ?: "کاربر", email = user.email, role = UserRole.TEACHER)
            .also { _currentUser.value = it }
    }

    override suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        auth.signInWith(OTP) { this.email = email.trim(); createUser = false }
    }

    override suspend fun verifyOtp(email: String, code: String): Result<AppUser> = runCatching {
        auth.verifyEmailOtp(type = io.github.jan.supabase.auth.OtpType.Email.EMAIL, email = email.trim(), token = code.trim())
        val user = auth.currentUserOrNull() ?: error("کد تأیید نشد")
        AppUser(id = user.id, name = user.email ?: "کاربر", email = user.email, role = UserRole.TEACHER)
            .also { _currentUser.value = it }
    }

    override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut(); _currentUser.value = null }
}
