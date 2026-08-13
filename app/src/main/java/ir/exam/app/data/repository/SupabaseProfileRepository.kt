package ir.exam.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Patterns
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.NativeProfileDto
import ir.exam.app.data.dto.ProfileSaveResponseDto
import ir.exam.app.data.dto.TeacherPublicProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.ExamHeader
import ir.exam.app.domain.model.NativeProfile
import ir.exam.app.domain.model.TeacherPublicProfile
import ir.exam.app.domain.model.UserRole
import kotlinx.serialization.json.JsonObject
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
        NativeProfile(
            id = id,
            fullName = dto.fullName.orEmpty().ifBlank { "کاربر" },
            displayName = dto.displayName.orEmpty(),
            username = dto.username.orEmpty(),
            avatarUrl = dto.avatarUrl,
            avatarPublic = dto.avatarPublic,
            header = ExamHeader(
                province = dto.headerProvince.orEmpty(),
                city = dto.headerCity.orEmpty(),
                district = dto.headerDistrict.orEmpty(),
                school = dto.headerSchool.orEmpty(),
                grade = dto.headerGrade.orEmpty()
            ),
            role = if (dto.role.equals("teacher", true)) UserRole.TEACHER else UserRole.STUDENT
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
            "native_save_profile",
            buildJsonObject {
                put("p_display_name", profile.displayName.trim().ifBlank { null })
                put("p_avatar_url", profile.avatarUrl)
                put("p_avatar_public", profile.avatarPublic)
                put("p_hdr_province", profile.header.province.trim())
                put("p_hdr_city", profile.header.city.trim())
                put("p_hdr_district", profile.header.district.trim())
                put("p_hdr_school", profile.header.school.trim())
                put("p_hdr_grade", profile.header.grade.trim())
            }
        ).decodeAs<ProfileSaveResponseDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
        profile.copy(avatarUrl = response.avatarUrl ?: profile.avatarUrl)
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = runCatching {
        require(newPassword.length in 8..72) { "رمز عبور باید ۸ تا ۷۲ کاراکتر باشد." }
        check(SupabaseProvider.client.auth.currentUserOrNull() != null) { "نشست ورود پیدا نشد." }
        SupabaseProvider.client.auth.updateUser { password = newPassword }
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
        listOf(
            "استان" to profile.header.province,
            "شهر" to profile.header.city,
            "منطقه" to profile.header.district,
            "مدرسه" to profile.header.school,
            "پایه" to profile.header.grade
        ).forEach { (label, value) -> require(value.length <= 120) { "$label حداکثر ۱۲۰ نویسه است." } }
        require(profile.avatarUrl == null || profile.avatarUrl.startsWith("https://")) { "نشانی عکس پروفایل معتبر نیست." }
    }

    private fun currentUserId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست ورود پیدا نشد.")
}
