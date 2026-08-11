package ir.exam.app.domain.repository

import ir.exam.app.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/** قرارداد ورود؛ UI به Supabase وابسته نیست و فقط با این قرارداد کار می‌کند. */
interface AuthRepository {
    val currentUser: Flow<AppUser?>
    suspend fun restoreSession(): Result<AppUser?>
    suspend fun signInWithPassword(email: String, password: String): Result<AppUser>
    suspend fun sendOtp(email: String): Result<Unit>
    suspend fun verifyOtp(email: String, code: String): Result<AppUser>
    suspend fun refreshCurrentUser(): Result<AppUser>
    suspend fun signOut(): Result<Unit>
}
