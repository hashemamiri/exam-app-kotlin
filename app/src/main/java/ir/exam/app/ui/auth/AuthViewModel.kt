package ir.exam.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setEmail(value: String) { _state.update { it.copy(email = value.trim(), error = null) } }
    fun setPassword(value: String) { _state.update { it.copy(password = value, error = null) } }
    fun setOtp(value: String) { _state.update { it.copy(otp = value.filter(Char::isDigit).take(6), error = null) } }

    fun sendOtp() = request {
        repository.sendOtp(state.value.email).getOrThrow()
        _state.update { it.copy(otpSent = true) }
    }

    fun verifyOtp() = request {
        val user = repository.verifyOtp(state.value.email, state.value.otp).getOrThrow()
        _state.update { it.copy(user = user) }
    }

    fun signIn() = request {
        val user = repository.signInWithPassword(state.value.email, state.value.password).getOrThrow()
        _state.update { it.copy(user = user) }
    }

    private fun request(action: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            action()
        } catch (error: Throwable) {
            _state.update { it.copy(error = safeAuthError(error.message)) }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
}


/** جزئیات شبکه و Header نباید هرگز در رابط کاربری نمایش داده شوند. */
private fun safeAuthError(raw: String?): String {
    val text = raw.orEmpty().lowercase()
    return when {
        "otp_disabled" in text -> "ورود با کد یک‌بارمصرف برای این کاربر فعال نیست. ابتدا وجود کاربر و تنظیمات Email/OTP در Supabase را بررسی کنید."
        "invalid" in text && "token" in text -> "کد واردشده نادرست است یا اعتبار آن تمام شده است."
        "email not confirmed" in text -> "ایمیل این حساب هنوز تأیید نشده است."
        "invalid login credentials" in text -> "ایمیل یا رمز عبور نادرست است."
        else -> "ورود انجام نشد. اتصال اینترنت و تنظیمات Supabase را بررسی کنید."
    }
}
