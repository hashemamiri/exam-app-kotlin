package ir.exam.app.domain.repository

import ir.exam.app.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/** قرارداد ورود؛ UI به Supabase وابسته نیست و فقط با این قرارداد کار می‌کند. */
interface AuthRepository {
    val currentUser: Flow<AppUser?>
    suspend fun restoreSession(): Result<AppUser?>
    suspend fun signInWithPassword(identifier: String, password: String): Result<AppUser>
    suspend fun sendLoginOtp(email: String): Result<Unit>
    suspend fun verifyLoginOtp(email: String, code: String): Result<AppUser>
    suspend fun sendTeacherRegistrationOtp(email: String, fullName: String): Result<Unit>
    suspend fun verifyTeacherRegistrationOtp(email: String, code: String): Result<Unit>
    suspend fun completeTeacherRegistration(
        fullName: String,
        username: String,
        password: String
    ): Result<AppUser>
    suspend fun sendRecoveryOtp(email: String): Result<Unit>
    suspend fun verifyRecoveryOtp(email: String, code: String): Result<String?>
    suspend fun changePassword(newPassword: String): Result<AppUser>
    suspend fun refreshCurrentUser(): Result<AppUser>
    suspend fun signOut(): Result<Unit>
}
