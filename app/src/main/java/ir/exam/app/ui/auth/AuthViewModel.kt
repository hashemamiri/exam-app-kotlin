package ir.exam.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RegistrationKind { TEACHER, MANAGER }

enum class AuthScreen {
    SIGN_IN,
    REGISTRATION_ROLE,
    LOGIN_OTP,
    TEACHER_REGISTER,
    TEACHER_REGISTER_OTP,
    TEACHER_REGISTER_SETUP,
    MANAGER_REGISTER,
    MANAGER_REGISTER_OTP,
    MANAGER_REGISTER_SETUP,
    RECOVERY,
    RECOVERY_OTP,
    RECOVERY_PASSWORD
}

data class AuthUiState(
    val screen: AuthScreen = AuthScreen.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val otp: String = "",
    val fullName: String = "",
    val username: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val registrationKind: RegistrationKind? = null,
    val schoolName: String = "",
    val province: String = "",
    val city: String = "",
    val teacherInviteCode: String = "",
    val recoveredUsername: String? = null,
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

    init { restoreSession() }

    fun retrySessionRestore() = restoreSession()

    private fun restoreSession() {
        if (restoreJob?.isActive == true) return
        restoreJob = viewModelScope.launch {
            _state.update { it.copy(isRestoringSession = true, restoreError = null, error = null) }
            repository.restoreSession()
                .onSuccess { user ->
                    if (user == null) {
                        _state.update { it.copy(isRestoringSession = false, user = null, restoreError = null) }
                    } else acceptAuthenticatedUser(user)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isRestoringSession = false, restoreError = safeAuthError(error.message))
                    }
                }
        }
    }

    fun setEmail(value: String) {
        _state.update { it.copy(email = value.trim(), error = null) }
    }
    fun setPassword(value: String) {
        _state.update { it.copy(password = value.take(72), error = null) }
    }
    fun setOtp(value: String) {
        _state.update { it.copy(otp = normalizeDigits(value).take(8), error = null) }
    }
    fun setFullName(value: String) {
        _state.update { it.copy(fullName = value.take(200), error = null) }
    }
    fun setUsername(value: String) {
        _state.update {
            it.copy(
                username = value.lowercase().filter { char -> char in 'a'..'z' || char.isDigit() || char == '_' }.take(20),
                error = null
            )
        }
    }
    fun setNewPassword(value: String) {
        _state.update { it.copy(newPassword = value.take(72), error = null) }
    }
    fun setConfirmPassword(value: String) {
        _state.update { it.copy(confirmPassword = value.take(72), error = null) }
    }
    fun setSchoolName(value: String) { _state.update { it.copy(schoolName = value.take(160), error = null) } }
    fun setProvince(value: String) { _state.update { it.copy(province = value.take(100), error = null) } }
    fun setCity(value: String) { _state.update { it.copy(city = value.take(100), error = null) } }
    fun setTeacherInviteCode(value: String) {
        _state.update { it.copy(teacherInviteCode = value.trim().take(80), error = null) }
    }

    fun showSignIn() = switchTo(AuthScreen.SIGN_IN)
    fun showRegistrationRole() = switchTo(AuthScreen.REGISTRATION_ROLE)
    fun showTeacherRegistration() {
        _state.update { it.copy(registrationKind = RegistrationKind.TEACHER) }
        switchTo(AuthScreen.TEACHER_REGISTER)
    }
    fun showManagerRegistration() {
        _state.update { it.copy(registrationKind = RegistrationKind.MANAGER) }
        switchTo(AuthScreen.MANAGER_REGISTER)
    }
    fun showRecovery() = switchTo(AuthScreen.RECOVERY)

    fun signIn() = request {
        val user = repository.signInWithPassword(state.value.email, state.value.password).getOrThrow()
        acceptAuthenticatedUser(user)
    }

    fun sendLoginOtp() = request {
        require('@' in state.value.email) { "برای ورود با کد، ایمیل معلم را وارد کنید." }
        repository.sendLoginOtp(state.value.email).getOrThrow()
        _state.update { it.copy(screen = AuthScreen.LOGIN_OTP, otp = "") }
    }

    fun verifyLoginOtp() = request {
        val user = repository.verifyLoginOtp(state.value.email, state.value.otp).getOrThrow()
        acceptAuthenticatedUser(user)
    }

    fun sendTeacherRegistrationOtp() = request {
        repository.sendTeacherRegistrationOtp(state.value.email, state.value.fullName).getOrThrow()
        _state.update { it.copy(screen = AuthScreen.TEACHER_REGISTER_OTP, otp = "") }
    }

    fun verifyTeacherRegistrationOtp() = request {
        repository.verifyTeacherRegistrationOtp(state.value.email, state.value.otp).getOrThrow()
        _state.update { it.copy(screen = AuthScreen.TEACHER_REGISTER_SETUP, otp = "") }
    }

    fun completeTeacherRegistration() = request {
        requirePasswordsMatch()
        val user = if (state.value.teacherInviteCode.isBlank()) {
            repository.completeTeacherRegistration(
                fullName = state.value.fullName,
                username = state.value.username,
                password = state.value.newPassword
            )
        } else {
            repository.completeInvitedTeacherRegistration(
                fullName = state.value.fullName,
                username = state.value.username,
                password = state.value.newPassword,
                inviteCode = state.value.teacherInviteCode
            )
        }.getOrThrow()
        acceptAuthenticatedUser(user)
    }

    fun sendManagerRegistrationOtp() = request {
        repository.sendManagerRegistrationOtp(state.value.email, state.value.fullName).getOrThrow()
        _state.update { it.copy(screen = AuthScreen.MANAGER_REGISTER_OTP, otp = "") }
    }

    fun verifyManagerRegistrationOtp() = request {
        repository.verifyManagerRegistrationOtp(state.value.email, state.value.otp).getOrThrow()
        _state.update { it.copy(screen = AuthScreen.MANAGER_REGISTER_SETUP, otp = "") }
    }

    fun completeManagerRegistration() = request {
        requirePasswordsMatch()
        val user = repository.completeManagerRegistration(
            fullName = state.value.fullName,
            username = state.value.username,
            password = state.value.newPassword,
            schoolName = state.value.schoolName,
            province = state.value.province,
            city = state.value.city
        ).getOrThrow()
        acceptAuthenticatedUser(user)
    }

    fun sendRecoveryOtp() = request {
        repository.sendRecoveryOtp(state.value.email).getOrThrow()
        _state.update { it.copy(screen = AuthScreen.RECOVERY_OTP, otp = "") }
    }

    fun verifyRecoveryOtp() = request {
        val username = repository.verifyRecoveryOtp(state.value.email, state.value.otp).getOrThrow()
        _state.update {
            it.copy(
                screen = AuthScreen.RECOVERY_PASSWORD,
                recoveredUsername = username,
                otp = "",
                newPassword = "",
                confirmPassword = ""
            )
        }
    }

    fun saveRecoveredPassword() = request {
        requirePasswordsMatch()
        val user = repository.changePassword(state.value.newPassword).getOrThrow()
        acceptAuthenticatedUser(user)
    }

    fun cancelVerifiedFlow() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        repository.signOut()
        _state.value = AuthUiState(isRestoringSession = false)
    }

    fun refreshCurrentUser() = request {
        val user = repository.refreshCurrentUser().getOrThrow()
        _state.update { it.copy(user = user) }
    }

    fun signOut() = request {
        repository.signOut().getOrThrow()
        _state.value = AuthUiState(isRestoringSession = false)
    }

    private fun acceptAuthenticatedUser(user: AppUser) {
        if (user.requiresTeacherSetup) {
            _state.update {
                it.copy(
                    screen = if (user.pendingRegistrationRole == ir.exam.app.domain.model.UserRole.MANAGER) {
                        AuthScreen.MANAGER_REGISTER_SETUP
                    } else AuthScreen.TEACHER_REGISTER_SETUP,
                    email = user.email.orEmpty(),
                    fullName = user.name,
                    username = user.username.orEmpty(),
                    password = "",
                    otp = "",
                    newPassword = "",
                    confirmPassword = "",
                    isRestoringSession = false,
                    user = null,
                    restoreError = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    user = user,
                    password = "",
                    otp = "",
                    newPassword = "",
                    confirmPassword = "",
                    isRestoringSession = false,
                    restoreError = null
                )
            }
        }
    }

    private fun requirePasswordsMatch() {
        require(state.value.newPassword.length in 8..72) { "رمز عبور باید ۸ تا ۷۲ کاراکتر باشد." }
        require(state.value.newPassword == state.value.confirmPassword) { "تکرار رمز عبور یکسان نیست." }
    }

    private fun switchTo(screen: AuthScreen) {
        val authenticatedPending = state.value.screen in setOf(
            AuthScreen.TEACHER_REGISTER_SETUP,
            AuthScreen.MANAGER_REGISTER_SETUP,
            AuthScreen.RECOVERY_PASSWORD
        )
        if (authenticatedPending && screen == AuthScreen.SIGN_IN) {
            cancelVerifiedFlow()
            return
        }
        _state.update {
            it.copy(
                screen = screen,
                otp = "",
                password = "",
                newPassword = "",
                confirmPassword = "",
                recoveredUsername = null,
                error = null
            )
        }
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

private fun normalizeDigits(value: String): String = buildString {
    value.forEach { char ->
        append(
            when (char) {
                in '۰'..'۹' -> ('0'.code + (char.code - '۰'.code)).toChar()
                in '٠'..'٩' -> ('0'.code + (char.code - '٠'.code)).toChar()
                else -> char
            }
        )
    }
}.filter(Char::isDigit)

/** پیام امن تشخیصی: فقط code و متن کوتاه، هرگز Header یا Token نمایش داده نمی‌شود. */
private fun safeAuthError(raw: String?): String {
    val clean = raw.orEmpty()
        .substringBefore("URL:")
        .substringBefore("Headers:")
        .replace(Regex("(?i)authorization[^,\n]*"), "")
        .replace(Regex("(?i)apikey[^,\n]*"), "")
        .replace(Regex("https?://\\S+"), "")
        .trim()
        .take(260)
    val text = clean.lowercase()
    return when {
        "otp_disabled" in text -> "کد تشخیصی: otp_disabled — ورود OTP در تنظیمات Email Supabase غیرفعال است."
        "signup" in text && "disabled" in text -> "کد تشخیصی: signup_disabled — ثبت‌نام ایمیلی در Supabase غیرفعال است."
        "invalid" in text && ("token" in text || "otp" in text) -> "کد نادرست یا منقضی است."
        "email not confirmed" in text -> "ایمیل کاربر تأیید نشده است."
        "invalid login credentials" in text -> "ایمیل/نام کاربری یا رمز عبور نادرست است."
        "user not found" in text -> "اطلاعات حساب پیدا نشد."
        clean.isNotBlank() -> clean
        else -> "عملیات ورود کامل نشد. دوباره تلاش کنید."
    }
}
