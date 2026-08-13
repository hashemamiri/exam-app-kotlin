package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NativeProfileDto(
    val ok: Boolean = false,
    val id: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val username: String? = null,
    val role: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_public") val avatarPublic: Boolean = true,
    @SerialName("hdr_province") val headerProvince: String? = null,
    @SerialName("hdr_city") val headerCity: String? = null,
    @SerialName("hdr_district") val headerDistrict: String? = null,
    @SerialName("hdr_school") val headerSchool: String? = null,
    @SerialName("hdr_grade") val headerGrade: String? = null,
    val error: String? = null
)

@Serializable
data class TeacherPublicProfileDto(
    val ok: Boolean = false,
    val name: String? = null,
    val avatar: String? = null,
    val error: String? = null
)

@Serializable
data class ProfileSaveResponseDto(
    val ok: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val error: String? = null
)
