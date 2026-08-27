package ir.exam.app.data.dto

import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SchoolClassDto(
    val id: String,
    val name: String,
    val grade: String? = null,
    @SerialName("field_of_study") val fieldOfStudy: String? = null,
    val boys: Int = 0,
    val girls: Int = 0,
    val total: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    // V62.6 — وضعیت اشتراک کلاس با مدیر.
    @SerialName("shared_with_manager") val sharedWithManager: Boolean = false
) {
    fun toDomain() = SchoolClass(id, name, grade, fieldOfStudy, boys, girls, total, createdAt, sharedWithManager)
}

@Serializable
data class StudentProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val username: String? = null,
    val gender: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("class_names") val classNames: String? = null,
    @SerialName("father_name") val fatherName: String? = null,
    val grade: String? = null,
    @SerialName("field_of_study") val fieldOfStudy: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("can_manage") val canManage: Boolean = true,
    @SerialName("in_my_list") val inMyList: Boolean = true,
    // V62.8 — وضعیت اشتراک با مدیر از my_students (ستون V62.6).
    @SerialName("shared_with_manager") val sharedWithManager: Boolean = false
) {
    fun toDomain() = StudentProfile(
        id = id,
        fullName = fullName.ifBlank { listOfNotNull(firstName, lastName).joinToString(" ") },
        firstName = firstName,
        lastName = lastName,
        username = username,
        gender = gender,
        active = isActive,
        classNames = classNames,
        fatherName = fatherName,
        grade = grade,
        fieldOfStudy = fieldOfStudy,
        avatarUrl = avatarUrl,
        canManageAccount = canManage,
        inMyList = inMyList,
        sharedWithManager = sharedWithManager
    )
}
