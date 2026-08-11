package ir.exam.app.domain.model

import java.time.LocalDate

data class SchoolClass(
    val id: String,
    val name: String,
    val grade: String? = null,
    val boys: Int = 0,
    val girls: Int = 0,
    val total: Int = 0,
    val createdAt: String? = null
)

data class StudentProfile(
    val id: String,
    val fullName: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val gender: String? = null,
    val active: Boolean = true,
    val classNames: String? = null,
    val fatherName: String? = null,
    val grade: String? = null,
    val avatarUrl: String? = null
)

data class NewStudentRequest(
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val gender: String,
    val fatherName: String = "",
    val grade: String = "",
    val classId: String? = null
)

data class StudentCredential(
    val id: String,
    val username: String,
    val password: String
)

data class UpdateStudentRequest(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val gender: String,
    val fatherName: String = "",
    val grade: String = ""
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val message: String?,
    val createdBy: String
)
