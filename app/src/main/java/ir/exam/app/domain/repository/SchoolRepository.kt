package ir.exam.app.domain.repository

import ir.exam.app.domain.model.BulkStudentCreateResult
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest

interface SchoolRepository {
    suspend fun getClasses(): Result<List<SchoolClass>>
    suspend fun createClass(name: String, grade: String, fieldOfStudy: String = ""): Result<Unit>
    suspend fun updateClass(id: String, name: String, grade: String, fieldOfStudy: String = ""): Result<Unit>
    suspend fun deleteClass(id: String): Result<Unit>
    suspend fun getStudents(): Result<List<StudentProfile>>
    suspend fun getClassRoster(classId: String): Result<List<StudentProfile>>
    suspend fun addStudentsToClass(classId: String, studentIds: List<String>): Result<Int>
    suspend fun addStudentToClasses(studentId: String, classIds: Set<String>): Result<Int> =
        Result.failure(UnsupportedOperationException("multi-class add not implemented"))
    suspend fun removeStudentFromClass(classId: String, studentId: String): Result<Unit>
    suspend fun setStudentActive(studentId: String, active: Boolean): Result<Unit>
    suspend fun createStudent(request: NewStudentRequest): Result<StudentCredential>
    suspend fun createStudentsBulk(classId:String?,requests:List<NewStudentRequest>):Result<BulkStudentCreateResult> =
        Result.failure(UnsupportedOperationException("bulk create not implemented"))
    suspend fun updateStudent(request: UpdateStudentRequest): Result<Unit>
    suspend fun resetStudentPassword(studentId: String, newPassword: String): Result<StudentCredential>
    suspend fun deleteStudent(studentId: String): Result<Unit>
}
