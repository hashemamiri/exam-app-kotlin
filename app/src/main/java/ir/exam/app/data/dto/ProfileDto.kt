package ir.exam.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val full_name: String = "کاربر",
    val role: String = "student",
    val display_name: String? = null,
    val avatar_url: String? = null,
    val avatar_public: Boolean = true
)
