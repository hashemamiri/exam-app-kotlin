package ir.exam.app.ui.auth

import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.domain.repository.AuthRepository
import ir.exam.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `saved session restores user before login screen`() = runTest(mainDispatcherRule.dispatcher) {
        val user = AppUser(
            id = "user-1",
            name = "کاربر آزمایشی",
            email = "user@example.test",
            role = UserRole.TEACHER
        )
        val viewModel = AuthViewModel(FakeAuthRepository(Result.success(user)))

        assertTrue(viewModel.state.value.isRestoringSession)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRestoringSession)
        assertEquals(user, viewModel.state.value.user)
        assertNull(viewModel.state.value.restoreError)
    }

    @Test
    fun `missing saved session opens normal login`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository(Result.success(null)))

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRestoringSession)
        assertNull(viewModel.state.value.user)
        assertNull(viewModel.state.value.restoreError)
    }

    @Test
    fun `interrupted verified teacher signup resumes setup instead of student dashboard`() = runTest(mainDispatcherRule.dispatcher) {
        val pending = AppUser(
            id = "pending-1",
            name = "معلم نیمه‌تمام",
            email = "pending@example.test",
            role = UserRole.STUDENT,
            username = "pending_teacher",
            requiresTeacherSetup = true
        )
        val viewModel = AuthViewModel(FakeAuthRepository(Result.success(pending)))
        advanceUntilIdle()

        assertNull(viewModel.state.value.user)
        assertEquals(AuthScreen.TEACHER_REGISTER_SETUP, viewModel.state.value.screen)
        assertEquals("pending_teacher", viewModel.state.value.username)
    }

    @Test
    fun `restore failure keeps user away from false logged-out screen`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AuthViewModel(
            FakeAuthRepository(Result.failure(IllegalStateException("backend unavailable")))
        )

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRestoringSession)
        assertNull(viewModel.state.value.user)
        assertNotNull(viewModel.state.value.restoreError)
    }

    @Test
    fun `explicit sign out clears restored user`() = runTest(mainDispatcherRule.dispatcher) {
        val user = AppUser("u1", "معلم", "t@example.test", UserRole.TEACHER)
        val viewModel = AuthViewModel(FakeAuthRepository(Result.success(user)))
        advanceUntilIdle()
        assertEquals(user, viewModel.state.value.user)

        viewModel.signOut()
        advanceUntilIdle()

        assertNull(viewModel.state.value.user)
        assertFalse(viewModel.state.value.isRestoringSession)
    }

    @Test
    fun `teacher registration requires otp then profile and password completion`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = CriticalAuthRepository()
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()
        viewModel.showTeacherRegistration()
        viewModel.setFullName("معلم تازه")
        viewModel.setEmail("teacher@example.test")
        viewModel.sendTeacherRegistrationOtp()
        advanceUntilIdle()
        assertEquals(AuthScreen.TEACHER_REGISTER_OTP, viewModel.state.value.screen)

        viewModel.setOtp("۱۲۳۴۵۶")
        viewModel.verifyTeacherRegistrationOtp()
        advanceUntilIdle()
        assertEquals(AuthScreen.TEACHER_REGISTER_SETUP, viewModel.state.value.screen)

        viewModel.setUsername("teacher_new")
        viewModel.setNewPassword("safe-pass-123")
        viewModel.setConfirmPassword("safe-pass-123")
        viewModel.completeTeacherRegistration()
        advanceUntilIdle()

        assertEquals(UserRole.TEACHER, viewModel.state.value.user?.role)
        assertEquals("teacher_new", repository.completedUsername)
    }

    @Test
    fun `recovery does not enter dashboard before new password is saved`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = CriticalAuthRepository()
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()
        viewModel.showRecovery()
        viewModel.setEmail("teacher@example.test")
        viewModel.sendRecoveryOtp()
        advanceUntilIdle()
        viewModel.setOtp("123456")
        viewModel.verifyRecoveryOtp()
        advanceUntilIdle()

        assertEquals(AuthScreen.RECOVERY_PASSWORD, viewModel.state.value.screen)
        assertEquals("teacher_name", viewModel.state.value.recoveredUsername)
        assertNull(viewModel.state.value.user)

        viewModel.setNewPassword("another-safe-pass")
        viewModel.setConfirmPassword("another-safe-pass")
        viewModel.saveRecoveredPassword()
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.user)
    }
}

private class FakeAuthRepository(
    private val restoreResult: Result<AppUser?>
) : AuthRepository {
    private val userState = MutableStateFlow<AppUser?>(null)
    override val currentUser: Flow<AppUser?> = userState

    override suspend fun restoreSession(): Result<AppUser?> = restoreResult

    override suspend fun signInWithPassword(identifier: String, password: String): Result<AppUser> =
        Result.failure(UnsupportedOperationException())

    override suspend fun sendLoginOtp(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun verifyLoginOtp(email: String, code: String): Result<AppUser> =
        Result.failure(UnsupportedOperationException())

    override suspend fun sendTeacherRegistrationOtp(email: String, fullName: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun verifyTeacherRegistrationOtp(email: String, code: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun completeTeacherRegistration(
        fullName: String,
        username: String,
        password: String
    ): Result<AppUser> = Result.failure(UnsupportedOperationException())

    override suspend fun sendRecoveryOtp(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun verifyRecoveryOtp(email: String, code: String): Result<String?> =
        Result.failure(UnsupportedOperationException())

    override suspend fun changePassword(newPassword: String): Result<AppUser> =
        Result.failure(UnsupportedOperationException())

    override suspend fun refreshCurrentUser(): Result<AppUser> =
        restoreResult.mapCatching { it ?: error("کاربر موجود نیست") }

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
}

private class CriticalAuthRepository : AuthRepository {
    private val teacher = AppUser("teacher-1", "معلم تازه", "teacher@example.test", UserRole.TEACHER)
    private val userState = MutableStateFlow<AppUser?>(null)
    var completedUsername: String? = null
    override val currentUser: Flow<AppUser?> = userState

    override suspend fun restoreSession(): Result<AppUser?> = Result.success(null)
    override suspend fun signInWithPassword(identifier: String, password: String) = Result.success(teacher)
    override suspend fun sendLoginOtp(email: String) = Result.success(Unit)
    override suspend fun verifyLoginOtp(email: String, code: String) = Result.success(teacher)
    override suspend fun sendTeacherRegistrationOtp(email: String, fullName: String) = Result.success(Unit)
    override suspend fun verifyTeacherRegistrationOtp(email: String, code: String) = Result.success(Unit)
    override suspend fun completeTeacherRegistration(
        fullName: String,
        username: String,
        password: String
    ): Result<AppUser> {
        completedUsername = username
        return Result.success(teacher)
    }
    override suspend fun sendRecoveryOtp(email: String) = Result.success(Unit)
    override suspend fun verifyRecoveryOtp(email: String, code: String) = Result.success("teacher_name")
    override suspend fun changePassword(newPassword: String) = Result.success(teacher)
    override suspend fun refreshCurrentUser() = Result.success(teacher)
    override suspend fun signOut() = Result.success(Unit)
}
