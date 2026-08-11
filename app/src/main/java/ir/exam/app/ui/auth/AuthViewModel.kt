package ir.exam.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val isLoading: Boolean = false,
    val isRestoringSession: Boolean = true,
    val user: AppUser? = null,
    val error: String? = null,
    val restoreError: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private var restoreJob: Job? = null

    init {
        restoreSession()
    }

    fun retrySessionRestore() = restoreSession()

    private fun restoreSession() {
        if (restoreJob?.isActive == true) return
        restoreJob = viewModelScope.launch {
            _state.update {
                it.copy(isRestoringSession = true, restoreError = null, error = null)
            }
            repository.restoreSession()
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            isRestoringSession = false,
                            user = user,
                            restoreError = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isRestoringSession = false,
                            restoreError = safeAuthError(error.message)
                        )
                    }
                }
        }
    }

    fun setEmail(value: String) { _state.update { it.copy(email = value.trim(), error = null) } }
    fun setPassword(value: String) { _state.update { it.copy(password = value, error = null) } }
    fun setOtp(value: String) { _state.update { it.copy(otp = value.filter(Char::isDigit).take(8), error = null) } }

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

    fun signOut() = request {
        repository.signOut().getOrThrow()
        _state.value = AuthUiState(isRestoringSession = false)
    }

    private fun request(action: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null, restoreError = null) }
        try {
            action()
        } catch (error: Throwable) {
            _state.update { it.copy(error = safeAuthError(error.message)) }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
}


/** پیام امن تشخیصی: فقط code و متن کوتاه، هرگز Header یا Token نمایش داده نمی‌شود. */
private fun safeAuthError(raw: String?): String {
    val clean = raw.orEmpty()
        .substringBefore("URL:")
        .substringBefore("Headers:")
        .replace(Regex("(?i)authorization[^,\n]*"), "")
        .replace(Regex("(?i)apikey[^,\n]*"), "")
        .trim()
        .take(260)
    val text = clean.lowercase()
    return when {
        "otp_disabled" in text -> "کد تشخیصی: otp_disabled — ثبت‌نام یا ورود OTP در تنظیمات Email Supabase غیرفعال است."
        "signup" in text && "disabled" in text -> "کد تشخیصی: signup_disabled — ثبت‌نام ایمیلی در Supabase غیرفعال است."
        "invalid" in text && "token" in text -> "کد تشخیصی: invalid_token — کد نادرست یا منقضی است."
        "email not confirmed" in text -> "کد تشخیصی: email_not_confirmed — ایمیل کاربر تأیید نشده است."
        "invalid login credentials" in text -> "کد تشخیصی: invalid_credentials — ایمیل یا رمز عبور نادرست است."
        clean.isNotBlank() -> "کد تشخیصی Supabase: $clean"
        else -> "کد تشخیصی: unknown_auth_error"
    }
}
