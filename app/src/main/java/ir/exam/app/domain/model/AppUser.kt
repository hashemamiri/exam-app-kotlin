package ir.exam.app.domain.model

enum class UserRole { TEACHER, STUDENT }
data class AppUser(val id: String, val name: String, val email: String?, val role: UserRole, val avatarUrl: String? = null)
