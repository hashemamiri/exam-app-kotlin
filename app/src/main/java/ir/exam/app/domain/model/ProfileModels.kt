package ir.exam.app.domain.model

data class ExamHeader(
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val school: String = "",
    val grade: String = "",
    val fieldOfStudy: String = ""
)

data class NativeProfile(
    val id: String,
    val fullName: String,
    val firstName: String = "",
    val lastName: String = "",
    val employeeCode: String = "",
    val phone: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val avatarPublic: Boolean = true,
    val header: ExamHeader = ExamHeader(),
    val role: UserRole
) {
    val shownName: String get() = displayName.ifBlank { fullName }
}

data class TeacherPublicProfile(
    val name: String,
    val avatarUrl: String? = null
)
