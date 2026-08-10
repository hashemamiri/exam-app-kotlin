package ir.exam.app.data.repository

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import ir.exam.app.data.dto.ProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseAuthRepository : AuthRepository {
    private val auth get() = SupabaseProvider.client.auth
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    override val currentUser: Flow<AppUser?> = _currentUser.asStateFlow()

    private suspend fun currentProfile(): AppUser {
        val sessionUser = auth.currentUserOrNull() ?: error("نشست ورود پیدا نشد")
        val profile = SupabaseProvider.client.from("profiles").select {
            filter { eq("id", sessionUser.id) }
        }.decodeSingle<ProfileDto>()
        val role = if (profile.role.lowercase() == "teacher") UserRole.TEACHER else UserRole.STUDENT
        return AppUser(profile.id, profile.display_name ?: profile.full_name, sessionUser.email, role)
    }

    override suspend fun signInWithPassword(email: String, password: String): Result<AppUser> = runCatching {
        auth.signInWith(Email) { this.email = email.trim(); this.password = password }
        currentProfile().also { _currentUser.value = it }
    }

    override suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        auth.signInWith(OTP) { this.email = email.trim(); createUser = true }
    }

    override suspend fun verifyOtp(email: String, code: String): Result<AppUser> = runCatching {
        auth.verifyEmailOtp(OtpType.Email.EMAIL, email.trim(), code.trim())
        currentProfile().also { _currentUser.value = it }
    }

    override suspend fun signOut(): Result<Unit> = runCatching { auth.signOut(); _currentUser.value = null }
}
