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
    fun `restore failure keeps user away from false logged-out screen`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = AuthViewModel(
            FakeAuthRepository(Result.failure(IllegalStateException("backend unavailable")))
        )

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRestoringSession)
        assertNull(viewModel.state.value.user)
        assertNotNull(viewModel.state.value.restoreError)
    }
}

private class FakeAuthRepository(
    private val restoreResult: Result<AppUser?>
) : AuthRepository {
    private val userState = MutableStateFlow<AppUser?>(null)
    override val currentUser: Flow<AppUser?> = userState

    override suspend fun restoreSession(): Result<AppUser?> = restoreResult

    override suspend fun signInWithPassword(email: String, password: String): Result<AppUser> =
        Result.failure(UnsupportedOperationException())

    override suspend fun sendOtp(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun verifyOtp(email: String, code: String): Result<AppUser> =
        Result.failure(UnsupportedOperationException())

    override suspend fun signOut(): Result<Unit> = Result.success(Unit)
}
