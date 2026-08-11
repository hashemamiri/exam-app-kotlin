package ir.exam.app.ui.classes

import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest
import ir.exam.app.domain.repository.SchoolRepository
import ir.exam.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads real class and student models from repository`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeSchoolRepository()
        val viewModel = ClassesViewModel(repository)
        viewModel.load()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.loading)
        assertEquals("هفتم الف", viewModel.state.value.classes.single().name)
        assertEquals("علی رضایی", viewModel.state.value.students.single().fullName)
    }

    @Test
    fun `create class refreshes dashboard data`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeSchoolRepository()
        val viewModel = ClassesViewModel(repository)
        viewModel.saveClass(null, "هشتم ب", "هشتم")
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.classes.size)
        assertEquals("کلاس ساخته شد.", viewModel.state.value.message)
    }

    @Test
    fun `student edit reset and delete use hardened one-time credential flow`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeSchoolRepository()
        val viewModel = ClassesViewModel(repository)
        viewModel.updateStudent(UpdateStudentRequest("s1", "علی", "رضایی", "ali_new", "male"))
        advanceUntilIdle()
        assertTrue(repository.updated)

        viewModel.resetPassword("s1", "SafePass42")
        advanceUntilIdle()
        assertEquals("SafePass42", viewModel.state.value.lastCredential?.password)

        viewModel.deleteStudent("s1")
        advanceUntilIdle()
        assertTrue(repository.deleted)
    }
}

private class FakeSchoolRepository : SchoolRepository {
    var updated = false
    var deleted = false
    private val classes = mutableListOf(SchoolClass("c1", "هفتم الف", "هفتم", total = 1))
    private val students = mutableListOf(StudentProfile("s1", "علی رضایی", username = "alirezaei"))

    override suspend fun getClasses() = Result.success(classes.toList())
    override suspend fun createClass(name: String, grade: String): Result<Unit> {
        classes += SchoolClass("c${classes.size + 1}", name, grade)
        return Result.success(Unit)
    }
    override suspend fun updateClass(id: String, name: String, grade: String) = Result.success(Unit)
    override suspend fun deleteClass(id: String) = Result.success(Unit)
    override suspend fun getStudents() = Result.success(students.toList())
    override suspend fun getClassRoster(classId: String) = Result.success(students.toList())
    override suspend fun addStudentsToClass(classId: String, studentIds: List<String>) = Result.success(studentIds.size)
    override suspend fun removeStudentFromClass(classId: String, studentId: String) = Result.success(Unit)
    override suspend fun setStudentActive(studentId: String, active: Boolean) = Result.success(Unit)
    override suspend fun createStudent(request: NewStudentRequest) = Result.success(
        StudentCredential("s2", request.username, request.password)
    )
    override suspend fun updateStudent(request: UpdateStudentRequest): Result<Unit> {
        updated = true
        return Result.success(Unit)
    }
    override suspend fun resetStudentPassword(studentId: String, newPassword: String) =
        Result.success(StudentCredential(studentId, "student", newPassword))
    override suspend fun deleteStudent(studentId: String): Result<Unit> {
        deleted = true
        return Result.success(Unit)
    }
}
