package ir.exam.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.core.ui.AppFont
import ir.exam.app.core.ui.AppearancePreferences
import ir.exam.app.core.ui.NeumorphicPalette
import ir.exam.app.core.ui.ThemeMode
import ir.exam.app.data.repository.SupabaseProfileRepository
import ir.exam.app.domain.model.ExamHeader
import ir.exam.app.domain.model.NativeProfile
import ir.exam.app.domain.model.TeacherPublicProfile
import ir.exam.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSettingsState(
    val profile: NativeProfile? = null,
    val teacher: TeacherPublicProfile? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val accountSaving: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val savedVersion: Int = 0,
    val error: String? = null,
    val message: String? = null
)

class ProfileSettingsViewModel(
    context: Context,
    private val role: UserRole,
    private val repository: SupabaseProfileRepository = SupabaseProfileRepository(context),
    private val appearance: AppearancePreferences = AppearancePreferences(context)
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSettingsState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.load()
            .onSuccess { profile ->
                _state.update { it.copy(loading = false, profile = profile) }
                if (role == UserRole.STUDENT) loadTeacher()
            }
            .onFailure { error -> _state.update { it.copy(loading = false, error = safeProfileError(error)) } }
    }

    fun setDisplayName(value: String) = updateProfile { it.copy(displayName = value.take(100)) }
    fun setFirstName(value: String) = updateProfile { it.copy(firstName = value.take(100)) }
    fun setLastName(value: String) = updateProfile { it.copy(lastName = value.take(100)) }
    fun setEmployeeCode(value: String) = updateProfile {
        it.copy(employeeCode = value.uppercase().filter { c -> c in 'A'..'Z' || c.isDigit() || c == '_' || c == '-' }.take(30))
    }
    fun setPhone(value: String) = updateProfile { it.copy(phone = value.filter(Char::isDigit).take(11)) }
    fun setAvatarPublic(value: Boolean) = updateProfile { it.copy(avatarPublic = value) }
    fun setProvince(value: String) = updateHeader { it.copy(province = value.take(120)) }
    fun setCity(value: String) = updateHeader { it.copy(city = value.take(120)) }
    fun setDistrict(value: String) = updateHeader { it.copy(district = value.take(120)) }
    fun setSchool(value: String) = updateHeader { it.copy(school = value.take(120)) }
    fun setGrade(value: String) = updateHeader { it.copy(grade = value.take(120)) }
    fun setFieldOfStudy(value: String) = updateHeader { it.copy(fieldOfStudy = value.take(120)) }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        val profile = state.value.profile ?: return@launch
        _state.update { it.copy(uploadingAvatar = true, error = null, message = null) }
        runCatching {
            val url = repository.uploadAvatar(uri).getOrThrow()
            repository.save(profile.copy(avatarUrl = url)).getOrThrow()
        }
            .onSuccess { saved -> markSaved(saved, "عکس پروفایل ذخیره شد.") }
            .onFailure { error -> _state.update { it.copy(uploadingAvatar = false, error = safeProfileError(error)) } }
    }

    fun removeAvatar() {
        val profile = state.value.profile ?: return
        saveProfile(profile.copy(avatarUrl = null), "عکس پروفایل حذف شد.")
    }

    fun save() {
        val profile = state.value.profile ?: return
        saveProfile(profile, "پروفایل و سربرگ ذخیره شد.")
    }

    fun changePassword(password: String, confirmation: String) = accountAction {
        require(password.length in 8..72) { "رمز عبور باید ۸ تا ۷۲ کاراکتر باشد." }
        require(password == confirmation) { "تکرار رمز عبور یکسان نیست." }
        repository.changePassword(password).getOrThrow()
        "رمز عبور با موفقیت تغییر کرد."
    }

    fun changeEmail(email: String) = accountAction {
        repository.changeEmail(email).getOrThrow()
        "پیام تأیید به ایمیل جدید ارسال شد. تا تأیید، ایمیل فعلی معتبر می‌ماند."
    }

    fun changeTeacherUsername(username: String) = accountAction {
        require(role != UserRole.STUDENT) { "تغییر نام کاربری فقط برای حساب کادر مدرسه مجاز است." }
        val saved = repository.changeTeacherUsername(username).getOrThrow()
        _state.update { current ->
            current.copy(
                profile = current.profile?.copy(username = saved),
                savedVersion = current.savedVersion + 1
            )
        }
        "نام کاربری نمایشی تغییر کرد. ورود معلم همچنان با ایمیل انجام می‌شود."
    }

    fun clearStatus() {
        _state.update { it.copy(error = null, message = null) }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { appearance.setTheme(mode) }
    fun setFontScale(scale: Float) = viewModelScope.launch { appearance.setFontScale(scale) }
    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch { appearance.setDynamicColors(enabled) }
    fun setAppFont(font: AppFont) = viewModelScope.launch { appearance.setAppFont(font) }
    fun setNeumorphicPalette(palette: NeumorphicPalette) = viewModelScope.launch {
        appearance.setNeumorphicPalette(palette)
    }
    fun setNeumorphicDepth(depth: Float) = viewModelScope.launch {
        appearance.setNeumorphicDepth(depth)
    }
    fun resetAppearance() = viewModelScope.launch { appearance.reset() }

    private fun saveProfile(profile: NativeProfile, message: String) = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, message = null) }
        repository.save(profile)
            .onSuccess { markSaved(it, message) }
            .onFailure { error -> _state.update { it.copy(saving = false, error = safeProfileError(error)) } }
    }

    private fun accountAction(block: suspend () -> String) = viewModelScope.launch {
        _state.update { it.copy(accountSaving = true, error = null, message = null) }
        runCatching { block() }
            .onSuccess { message -> _state.update { it.copy(accountSaving = false, message = message) } }
            .onFailure { error ->
                _state.update { it.copy(accountSaving = false, error = safeProfileError(error)) }
            }
    }

    private fun markSaved(profile: NativeProfile, message: String) {
        _state.update {
            it.copy(
                profile = profile,
                saving = false,
                uploadingAvatar = false,
                savedVersion = it.savedVersion + 1,
                message = message,
                error = null
            )
        }
    }

    private fun loadTeacher() = viewModelScope.launch {
        repository.teacherPublicProfile()
            .onSuccess { teacher -> _state.update { it.copy(teacher = teacher) } }
            .onFailure { /* نشان معلم فرعی است؛ شکست آن کل پروفایل را از کار نمی‌اندازد. */ }
    }

    private fun updateProfile(change: (NativeProfile) -> NativeProfile) {
        _state.update { current -> current.copy(profile = current.profile?.let(change), error = null, message = null) }
    }

    private fun updateHeader(change: (ExamHeader) -> ExamHeader) = updateProfile { profile ->
        profile.copy(header = change(profile.header))
    }
}

private fun safeProfileError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(260)
    .ifBlank { "عملیات پروفایل ناموفق بود." }
