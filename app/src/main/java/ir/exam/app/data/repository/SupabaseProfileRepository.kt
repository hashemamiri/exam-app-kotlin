package ir.exam.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Patterns
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import ir.exam.app.data.dto.NativeProfileDto
import ir.exam.app.data.dto.ProfileSaveResponseDto
import ir.exam.app.data.dto.TeacherPublicProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.ExamHeader
import ir.exam.app.domain.model.NativeProfile
import ir.exam.app.domain.model.TeacherPublicProfile
import ir.exam.app.domain.model.UserRole
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseProfileRepository(context: Context) {
    private val imageUploader = SupabaseQuestionImageUploader(context)

    suspend fun load(): Result<NativeProfile> = runCatching {
        val dto = SupabaseProvider.client.postgrest.rpc("native_my_profile")
            .decodeAs<NativeProfileDto>()
        dto.error?.takeIf(String::isNotBlank)?.let(::error)
        val id = dto.id ?: currentUserId()
        val role = when {
            dto.role.equals("manager", true) -> UserRole.MANAGER
            dto.role.equals("teacher", true) -> UserRole.TEACHER
            else -> UserRole.STUDENT
        }
        val details = if (role == UserRole.TEACHER) {
            SupabaseProvider.client.postgrest.rpc("native_my_teacher_details_v40").decodeAs<JsonObject>()
        } else null
        NativeProfile(
            id = id,
            fullName = dto.fullName.orEmpty().ifBlank { "کاربر" },
            firstName = details.text("first_name").ifBlank { dto.fullName.orEmpty().substringBefore(' ') },
            lastName = details.text("last_name").ifBlank { dto.fullName.orEmpty().substringAfter(' ', "") },
            employeeCode = details.text("employee_code"),
            phone = details.text("phone"),
            displayName = dto.displayName.orEmpty(),
            username = dto.username.orEmpty(),
            avatarUrl = dto.avatarUrl,
            avatarPublic = dto.avatarPublic,
            header = ExamHeader(
                province = dto.headerProvince.orEmpty(),
                city = dto.headerCity.orEmpty(),
                district = dto.headerDistrict.orEmpty(),
                school = dto.headerSchool.orEmpty(),
                grade = dto.headerGrade.orEmpty(),
                fieldOfStudy = dto.headerField.orEmpty()
            ),
            role = role
        )
    }

    suspend fun teacherPublicProfile(): Result<TeacherPublicProfile?> = runCatching {
        val dto = SupabaseProvider.client.postgrest.rpc("teacher_public_profile")
            .decodeAs<TeacherPublicProfileDto>()
        dto.error?.takeIf(String::isNotBlank)?.let(::error)
        if (!dto.ok || dto.name.isNullOrBlank()) null else TeacherPublicProfile(dto.name, dto.avatar)
    }

    suspend fun uploadAvatar(uri: Uri): Result<String> = runCatching {
        imageUploader.uploadAvatar(currentUserId(), uri)
    }

    suspend fun save(profile: NativeProfile): Result<NativeProfile> = runCatching {
        validate(profile)
        val response = SupabaseProvider.client.postgrest.rpc(
            "native_save_profile_v28",
            buildJsonObject {
                put("p_display_name", profile.displayName.trim().ifBlank { null })
                put("p_avatar_url", profile.avatarUrl)
                put("p_avatar_public", profile.avatarPublic)
                put("p_hdr_province", profile.header.province.trim())
                put("p_hdr_city", profile.header.city.trim())
                put("p_hdr_district", profile.header.district.trim())
                put("p_hdr_school", profile.header.school.trim())
                put("p_hdr_grade", profile.header.grade.trim())
                put("p_hdr_field", profile.header.fieldOfStudy.trim())
            }
        ).decodeAs<ProfileSaveResponseDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
        if (profile.role == UserRole.TEACHER) {
            val details = SupabaseProvider.client.postgrest.rpc(
                "native_save_teacher_details_v40",
                buildJsonObject {
                    put("p_first_name", profile.firstName.trim())
                    put("p_last_name", profile.lastName.trim())
                    put("p_employee_code", profile.employeeCode.trim())
                    put("p_phone", profile.phone.trim())
                }
            ).decodeAs<JsonObject>()
            details["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        }
        profile.copy(
            fullName = listOf(profile.firstName, profile.lastName).filter(String::isNotBlank)
                .joinToString(" ").ifBlank { profile.fullName },
            avatarUrl = response.avatarUrl ?: profile.avatarUrl
        )
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = runCatching {
        require(newPassword.length in 8..72) { "رمز عبور باید ۸ تا ۷۲ کاراکتر باشد." }
        check(SupabaseProvider.client.auth.currentUserOrNull() != null) { "نشست ورود پیدا نشد." }
        SupabaseProvider.client.auth.updateUser { password = newPassword }
    }

    /**
     * V62.5 — تأیید رمز فعلی پیش از تغییر رمز: با ایمیل نشست فعلی و رمز
     * واردشده دوباره signIn می‌شود؛ رمز اشتباه = خطای سوپابیس. چیزی ذخیره
     * نمی‌شود و نشست همان کاربر می‌ماند.
     */
    suspend fun verifyCurrentPassword(currentPassword: String): Result<Unit> = runCatching {
        require(currentPassword.isNotBlank()) { "رمز فعلی را وارد کنید." }
        val email = SupabaseProvider.client.auth.currentUserOrNull()?.email
            ?.takeIf(String::isNotBlank) ?: error("نشست ورود پیدا نشد.")
        runCatching {
            SupabaseProvider.client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = currentPassword
            }
        }.getOrElse { error("رمز فعلی نادرست است.") }
    }

    /** V62.5 — فراموشی رمز فعلی: کد ۶ تا ۸ رقمی فقط به ایمیل خود حساب می‌رود. */
    suspend fun sendPasswordRecoveryOtp(email: String): Result<Unit> = runCatching {
        val sessionEmail = SupabaseProvider.client.auth.currentUserOrNull()?.email
            ?.takeIf(String::isNotBlank) ?: error("نشست ورود پیدا نشد.")
        require(sessionEmail.equals(email.trim(), ignoreCase = true)) {
            "کد بازیابی فقط به ایمیل همین حساب ارسال می‌شود."
        }
        SupabaseProvider.client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.OTP) {
            this.email = sessionEmail
            createUser = false
        }
    }

    /** V62.5 — تأیید کد بازیابی؛ پس از موفقیت، changePassword بدون رمز قبلی مجاز است. */
    suspend fun verifyPasswordRecoveryOtp(email: String, code: String): Result<Unit> = runCatching {
        val clean = code.filter(Char::isDigit)
        require(clean.length in 6..8) { "کد بازیابی ۶ تا ۸ رقم است." }
        SupabaseProvider.client.auth.verifyEmailOtp(
            io.github.jan.supabase.auth.OtpType.Email.EMAIL,
            email.trim().lowercase(),
            clean
        )
    }

    suspend fun changeEmail(newEmail: String): Result<Unit> = runCatching {
        val clean = newEmail.trim().lowercase()
        require(Patterns.EMAIL_ADDRESS.matcher(clean).matches() && clean.length <= 254) {
            "ایمیل جدید معتبر نیست."
        }
        val current = SupabaseProvider.client.auth.currentUserOrNull()
            ?: error("نشست ورود پیدا نشد.")
        require(!current.email.equals(clean, ignoreCase = true)) { "ایمیل جدید با ایمیل فعلی یکسان است." }
        SupabaseProvider.client.auth.updateUser { email = clean }
    }

    /**
     * V59.1 — حذف کامل حساب معلم/مدیر: Edge function manage-student با اکشن
     * delete_account؛ دانش‌آموزان مشترک به لیست دیگر منتقل و بقیه حذف می‌شوند
     * و در پایان خود حساب پاک و نشست باطل می‌شود.
     */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val raw = try {
            SupabaseProvider.client.functions.invoke(
                "manage-student",
                body = buildJsonObject { put("action", "delete_account") }
            ).body<JsonObject>()
        } catch (error: Throwable) {
            // V59.2 — پاسخ‌های غیر ۲۰۰ (بدنهٔ JSON خام) اینجا استثنا می‌شوند؛
            // «عملیات ناشناخته» یعنی نسخهٔ سرور تابع هنوز به‌روز نشده است.
            val message = error.message.orEmpty()
            if ("عملیات ناشناخته" in message) {
                error("نسخهٔ سرور به‌روز نیست؛ تابع manage-student باید دوباره منتشر (deploy) شود.")
            }
            Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(message)?.groupValues?.get(1)?.let(::error)
            throw error
        }
        (raw["error"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf(String::isNotBlank)?.let { serverError ->
                if ("عملیات ناشناخته" in serverError) {
                    error("نسخهٔ سرور به‌روز نیست؛ تابع manage-student باید دوباره منتشر (deploy) شود.")
                }
                error(serverError)
            }
        // V59.3 — نشست سروری با حذف حساب باطل شده؛ فقط پاک‌سازی محلی لازم است
        // (signOut سروری برای کاربر حذف‌شده 403 می‌دهد و session محلی می‌ماند).
        runCatching {
            SupabaseProvider.client.auth.signOut(io.github.jan.supabase.auth.SignOutScope.LOCAL)
        }
        Unit
    }

    suspend fun changeTeacherUsername(username: String): Result<String> = runCatching {
        val clean = username.trim().lowercase()
        require(Regex("^[a-z0-9_]{4,20}$").matches(clean)) {
            "نام کاربری باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد."
        }
        val response = SupabaseProvider.client.postgrest.rpc(
            "native_update_my_username_v1",
            buildJsonObject { put("p_username", clean) }
        ).decodeAs<JsonObject>()
        response["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        response["username"]?.jsonPrimitive?.contentOrNull ?: clean
    }

    private fun validate(profile: NativeProfile) {
        require(profile.displayName.length <= 100) { "نام نمایشی حداکثر ۱۰۰ نویسه است." }
        require(profile.firstName.length <= 100 && profile.lastName.length <= 100) { "نام و نام خانوادگی حداکثر ۱۰۰ نویسه است." }
        require(profile.employeeCode.isBlank() || Regex("^[A-Za-z0-9_-]{1,30}$").matches(profile.employeeCode)) { "کد پرسنلی معتبر نیست." }
        require(profile.phone.isBlank() || Regex("^09[0-9]{9}$").matches(profile.phone)) { "شماره تلفن باید ۱۱ رقم و با 09 شروع شود." }
        listOf(
            "استان" to profile.header.province,
            "شهر" to profile.header.city,
            "منطقه" to profile.header.district,
            "مدرسه" to profile.header.school,
            "پایه" to profile.header.grade,
            "رشته" to profile.header.fieldOfStudy
        ).forEach { (label, value) -> require(value.length <= 120) { "$label حداکثر ۱۲۰ نویسه است." } }
        require(profile.avatarUrl == null || profile.avatarUrl.startsWith("https://")) { "نشانی عکس پروفایل معتبر نیست." }
    }

    private fun JsonObject?.text(key: String): String =
        this?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun currentUserId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست ورود پیدا نشد.")
}
