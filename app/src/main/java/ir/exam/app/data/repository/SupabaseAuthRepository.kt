package ir.exam.app.data.repository

import android.content.Context
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.NativeProfileDto
import ir.exam.app.data.local.AuthUserCache
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAuthRepository(context: Context) : AuthRepository {
    private val auth get() = SupabaseProvider.client.auth
    private val userCache = AuthUserCache(context)
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    override val currentUser: Flow<AppUser?> = _currentUser.asStateFlow()

    /**
     * باید پیش از currentUserOrNull صبر کنیم تا Supabase نشست ذخیره‌شده را از storage بخواند.
     * در نبود اینترنت، نمای cached پروفایل فقط وقتی استفاده می‌شود که id آن با نشست Supabase برابر باشد.
     */
    override suspend fun restoreSession(): Result<AppUser?> = runCatching {
        auth.awaitInitialization()
        val sessionUser = auth.currentUserOrNull()
        if (sessionUser == null) {
            userCache.clear()
            _currentUser.value = null
            return@runCatching null
        }

        val cachedUser = userCache.read(sessionUser.id)
        val refreshedProfile = withTimeoutOrNull(PROFILE_REFRESH_TIMEOUT_MS) {
            try {
                auth.refreshCurrentSession()
                Result.success(currentProfile())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }
        val restoredUser = refreshedProfile?.getOrNull()
            ?: cachedUser
            ?: refreshedProfile?.getOrThrow()
            ?: currentProfile()

        persistUser(restoredUser)
    }

    private suspend fun currentProfile(): AppUser {
        val sessionUser = auth.currentUserOrNull()
            ?: error("نشست ورود پیدا نشد. دوباره وارد شوید.")
        val fallbackName = sessionUser.email?.substringBefore('@').orEmpty().ifBlank { "کاربر" }

        val profile = SupabaseProvider.client.postgrest.rpc(
            "native_ensure_profile_v1",
            buildJsonObject { put("p_fallback_name", fallbackName) }
        ).decodeAs<NativeProfileDto>()
        profile.error?.takeIf(String::isNotBlank)?.let(::error)

        val role = when {
            profile.role.equals("manager", true) -> UserRole.MANAGER
            profile.role.equals("teacher", true) -> UserRole.TEACHER
            else -> UserRole.STUDENT
        }
        val realEmailStudent = role == UserRole.STUDENT &&
            !sessionUser.email.orEmpty().endsWith("@student.exam.local", ignoreCase = true)
        val requiresTeacherSetup = if (realEmailStudent) {
            val state = SupabaseProvider.client.postgrest.rpc("native_my_registration_state_v1")
                .decodeAs<JsonObject>()
            state["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
            state["requires_teacher_setup"]?.jsonPrimitive?.booleanOrNull ?: false
        } else false
        val pendingRole = if (requiresTeacherSetup) {
            val state = SupabaseProvider.client.postgrest.rpc("native_my_registration_state_v1")
                .decodeAs<JsonObject>()
            if (state["pending_role"]?.jsonPrimitive?.contentOrNull.equals("manager", true)) {
                UserRole.MANAGER
            } else UserRole.TEACHER
        } else null
        return AppUser(
            id = profile.id ?: sessionUser.id,
            name = profile.displayName?.takeIf(String::isNotBlank)
                ?: profile.fullName.orEmpty().ifBlank { fallbackName },
            email = sessionUser.email,
            role = role,
            avatarUrl = profile.avatarUrl,
            username = profile.username,
            requiresTeacherSetup = requiresTeacherSetup,
            pendingRegistrationRole = pendingRole
        )
    }

    override suspend fun signInWithPassword(identifier: String, password: String): Result<AppUser> = runCatching {
        require(password.isNotBlank()) { "رمز عبور را وارد کنید." }
        // V60.0 — نام کاربری بدون @: اول نگاشت کادر مدرسه (معلم/مدیر) از سرور؛
        // اگر نبود، همان مسیر دانش‌آموز (username@student.exam.local).
        val clean = identifier.trim().lowercase()
        val loginEmail = if ('@' !in clean && AuthIdentifier.validUsername(clean)) {
            staffLoginEmail(clean) ?: AuthIdentifier.passwordLoginEmail(clean)
        } else {
            AuthIdentifier.passwordLoginEmail(identifier)
        }
        auth.signInWith(Email) {
            email = loginEmail
            this.password = password
        }
        persistUser(currentProfile())
    }

    /** V60.0 — ایمیل ورود معلم/مدیر از روی نام کاربری؛ null اگر کادر نبود. */
    private suspend fun staffLoginEmail(username: String): String? = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_staff_login_email_v1",
            buildJsonObject { put("p_username", username) }
        ).decodeAs<JsonObject>()
        raw["email"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
    }.getOrNull()

    override suspend fun sendLoginOtp(email: String): Result<Unit> = sendOtp(
        email = email,
        createUser = false,
        fullName = null
    )

    override suspend fun verifyLoginOtp(email: String, code: String): Result<AppUser> = runCatching {
        verifyEmailCode(email, code)
        persistUser(currentProfile())
    }

    override suspend fun sendTeacherRegistrationOtp(email: String, fullName: String): Result<Unit> {
        require(fullName.trim().length in 2..200) { "نام و نام خانوادگی را کامل وارد کنید." }
        return sendOtp(email, createUser = true, fullName = fullName.trim(), registrationRole = "teacher")
    }

    override suspend fun verifyTeacherRegistrationOtp(email: String, code: String): Result<Unit> = runCatching {
        verifyEmailCode(email, code)
    }

    override suspend fun completeTeacherRegistration(
        fullName: String,
        username: String,
        password: String
    ): Result<AppUser> = runCatching {
        val name = fullName.trim()
        val normalizedUsername = username.trim().lowercase()
        require(name.length in 2..200) { "نام و نام خانوادگی را کامل وارد کنید." }
        require(AuthIdentifier.validUsername(normalizedUsername)) {
            "نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد."
        }
        validateNewPassword(password)
        check(auth.currentUserOrNull() != null) { "ابتدا کد ایمیل را تأیید کنید." }

        val response = SupabaseProvider.client.postgrest.rpc(
            "native_complete_teacher_registration_v1",
            buildJsonObject {
                put("p_full_name", name)
                put("p_username", normalizedUsername)
            }
        ).decodeAs<NativeProfileDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
        check(response.ok && response.role.equals("teacher", true)) { "تکمیل حساب معلم ناموفق بود." }

        auth.updateUser {
            this.password = password
            data {
                put("full_name", name)
            }
        }
        persistUser(currentProfile())
    }

    override suspend fun completeInvitedTeacherRegistration(
        fullName: String,
        username: String,
        password: String,
        inviteCode: String
    ): Result<AppUser> = runCatching {
        val name = fullName.trim()
        val normalizedUsername = username.trim().lowercase()
        require(name.length in 2..200) { "نام و نام خانوادگی را کامل وارد کنید." }
        require(AuthIdentifier.validUsername(normalizedUsername)) { "نام کاربری معتبر نیست." }
        require(inviteCode.trim().startsWith("TCH-") && inviteCode.trim().length >= 60) { "کد دعوت معتبر نیست." }
        validateNewPassword(password)
        check(auth.currentUserOrNull() != null) { "ابتدا کد ایمیل را تأیید کنید." }
        val response = SupabaseProvider.client.postgrest.rpc(
            "native_complete_teacher_registration_v37",
            buildJsonObject {
                put("p_full_name", name)
                put("p_username", normalizedUsername)
                put("p_invite_code", inviteCode.trim())
            }
        ).decodeAs<NativeProfileDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
        check(response.ok && response.role.equals("teacher", true)) { "عضویت معلم در مدرسه کامل نشد." }
        auth.updateUser {
            this.password = password
            data { put("full_name", name); put("registration_role", "teacher") }
        }
        persistUser(currentProfile())
    }

    override suspend fun sendManagerRegistrationOtp(email: String, fullName: String): Result<Unit> {
        require(fullName.trim().length in 2..200) { "نام و نام خانوادگی را کامل وارد کنید." }
        return sendOtp(
            email = email,
            createUser = true,
            fullName = fullName.trim(),
            registrationRole = "manager"
        )
    }

    override suspend fun verifyManagerRegistrationOtp(email: String, code: String): Result<Unit> = runCatching {
        verifyEmailCode(email, code)
    }

    override suspend fun completeManagerRegistration(
        fullName: String,
        username: String,
        password: String,
        schoolName: String,
        province: String,
        city: String
    ): Result<AppUser> = runCatching {
        val name = fullName.trim()
        val normalizedUsername = username.trim().lowercase()
        require(name.length in 2..200) { "نام و نام خانوادگی را کامل وارد کنید." }
        require(AuthIdentifier.validUsername(normalizedUsername)) { "نام کاربری معتبر نیست." }
        require(schoolName.trim().length in 2..160) { "نام مدرسه را وارد کنید." }
        validateNewPassword(password)
        check(auth.currentUserOrNull() != null) { "ابتدا کد ایمیل را تأیید کنید." }
        val response = SupabaseProvider.client.postgrest.rpc(
            "native_complete_manager_registration_v36",
            buildJsonObject {
                put("p_full_name", name)
                put("p_username", normalizedUsername)
                put("p_school_name", schoolName.trim())
                put("p_province", province.trim())
                put("p_city", city.trim())
            }
        ).decodeAs<NativeProfileDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
        check(response.ok && response.role.equals("manager", true)) { "تکمیل حساب مدیر/معاون ناموفق بود." }
        auth.updateUser {
            this.password = password
            data { put("full_name", name); put("registration_role", "manager") }
        }
        persistUser(currentProfile())
    }

    override suspend fun sendRecoveryOtp(email: String): Result<Unit> = sendOtp(
        email = email,
        createUser = false,
        fullName = null
    )

    override suspend fun verifyRecoveryOtp(email: String, code: String): Result<String?> = runCatching {
        verifyEmailCode(email, code)
        val profile = SupabaseProvider.client.postgrest.rpc("native_my_profile")
            .decodeAs<NativeProfileDto>()
        profile.error?.takeIf(String::isNotBlank)?.let(::error)
        profile.username?.takeIf(String::isNotBlank)
    }

    override suspend fun changePassword(newPassword: String): Result<AppUser> = runCatching {
        validateNewPassword(newPassword)
        check(auth.currentUserOrNull() != null) { "نشست ورود پیدا نشد." }
        auth.updateUser { password = newPassword }
        persistUser(currentProfile())
    }

    override suspend fun refreshCurrentUser(): Result<AppUser> = runCatching {
        persistUser(currentProfile())
    }

    override suspend fun signOut(): Result<Unit> {
        val result = runCatching { auth.signOut(SignOutScope.LOCAL) }
        userCache.clear()
        _currentUser.value = null
        return result
    }

    private suspend fun sendOtp(
        email: String,
        createUser: Boolean,
        fullName: String?,
        registrationRole: String? = null
    ): Result<Unit> = runCatching {
        val normalizedEmail = AuthIdentifier.requireEmail(email)
        auth.signInWith(OTP) {
            this.email = normalizedEmail
            this.createUser = createUser
            if (!fullName.isNullOrBlank()) {
                data = buildJsonObject {
                    put("full_name", fullName)
                    registrationRole?.let { put("registration_role", it) }
                }
            }
        }
    }

    private suspend fun verifyEmailCode(email: String, code: String) {
        val cleanCode = code.filter(Char::isDigit)
        require(cleanCode.length in 6..8) { "کد یک‌بارمصرف باید ۶ تا ۸ رقم باشد." }
        auth.verifyEmailOtp(OtpType.Email.EMAIL, AuthIdentifier.requireEmail(email), cleanCode)
    }

    private fun validateNewPassword(value: String) {
        require(value.length in 8..72) { "رمز عبور باید ۸ تا ۷۲ کاراکتر باشد." }
    }

    private fun persistUser(user: AppUser): AppUser {
        userCache.write(user)
        _currentUser.value = user
        return user
    }

    private companion object {
        const val PROFILE_REFRESH_TIMEOUT_MS = 5_000L
    }
}
