package ir.exam.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val isLoading: Boolean = false,
    val user: AppUser? = null,
    val error: String? = null
)

/** تمام state ورود در ViewModel است؛ Composable هیچ ارتباط شبکه‌ای انجام نمی‌دهد. */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    fun setEmail(value: String) { _state.value = _state.value.copy(email = value, error = null) }
    fun setPassword(value: String) { _state.value = _state.value.copy(password = value, error = null) }
    fun setOtp(value: String) { _state.value = _state.value.copy(otp = value.filter(Char::isDigit).take(6), error = null) }
    fun sendOtp() = launchRequest { repository.sendOtp(state.value.email).onSuccess { _state.value = state.value.copy(otpSent = true) } }
    fun verifyOtp() = launchRequest { repository.verifyOtp(state.value.email, state.value.otp).onSuccess { _state.value = state.value.copy(user = it) } }
    fun signIn() = launchRequest { repository.signInWithPassword(state.value.email, state.value.password).onSuccess { _state.value = state.value.copy(user = it) } }
    private fun launchRequest(block: suspend () -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        try { block() } catch (e: Exception) { _state.value = _state.value.copy(error = e.message ?: "عملیات ناموفق بود") }
        _state.value = _state.value.copy(isLoading = false)
    }
}
