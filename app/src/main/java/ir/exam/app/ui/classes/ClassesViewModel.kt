package ir.exam.app.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseSchoolRepository
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassesState(
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val classes: List<SchoolClass> = emptyList(),
    val students: List<StudentProfile> = emptyList(),
    val selectedClass: SchoolClass? = null,
    val roster: List<StudentProfile> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val message: String? = null,
    val lastCredential: StudentCredential? = null
)

class ClassesViewModel(
    private val repository: SchoolRepository = SupabaseSchoolRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ClassesState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val classes = repository.getClasses().getOrElse { return@launch failLoad(it) }
        val students = repository.getStudents().getOrElse { return@launch failLoad(it) }
        _state.update { old ->
            val selected = old.selectedClass?.id?.let { id -> classes.firstOrNull { it.id == id } }
            old.copy(loading = false, classes = classes, students = students, selectedClass = selected)
        }
        state.value.selectedClass?.let { loadRoster(it.id) }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null, lastCredential = null) }
    }

    fun selectClass(item: SchoolClass) {
        _state.update { it.copy(selectedClass = item, roster = emptyList(), error = null) }
        loadRoster(item.id)
    }

    fun closeClass() {
        _state.update { it.copy(selectedClass = null, roster = emptyList()) }
    }

    fun saveClass(id: String?, name: String, grade: String) = action(
        successMessage = if (id == null) "کلاس ساخته شد." else "کلاس ویرایش شد."
    ) {
        if (id == null) repository.createClass(name, grade).getOrThrow()
        else repository.updateClass(id, name, grade).getOrThrow()
        reloadData()
    }

    fun deleteClass(id: String) = action("کلاس حذف شد؛ حساب دانش‌آموزان حفظ شد.") {
        repository.deleteClass(id).getOrThrow()
        _state.update { it.copy(selectedClass = null, roster = emptyList()) }
        reloadData()
    }

    fun addStudents(studentIds: List<String>) {
        val classId = state.value.selectedClass?.id ?: return
        action("دانش‌آموزان به کلاس اضافه شدند.") {
            repository.addStudentsToClass(classId, studentIds).getOrThrow()
            reloadData()
            loadRosterNow(classId)
        }
    }

    fun removeStudent(studentId: String) {
        val classId = state.value.selectedClass?.id ?: return
        action("دانش‌آموز از کلاس خارج شد؛ حساب او حفظ شد.") {
            repository.removeStudentFromClass(classId, studentId).getOrThrow()
            reloadData()
            loadRosterNow(classId)
        }
    }

    fun setStudentActive(studentId: String, active: Boolean) = action(
        if (active) "دانش‌آموز فعال شد." else "دانش‌آموز غیرفعال شد."
    ) {
        repository.setStudentActive(studentId, active).getOrThrow()
        reloadData()
        state.value.selectedClass?.id?.let { loadRosterNow(it) }
    }

    fun createStudent(request: NewStudentRequest) = action("حساب دانش‌آموز ساخته شد.") {
        val credential = repository.createStudent(request).getOrThrow()
        reloadData()
        request.classId?.let { loadRosterNow(it) }
        _state.update { it.copy(lastCredential = credential) }
    }

    private fun loadRoster(classId: String) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null) }
        repository.getClassRoster(classId)
            .onSuccess { roster -> _state.update { it.copy(actionLoading = false, roster = roster) } }
            .onFailure { error -> _state.update { it.copy(actionLoading = false, error = safeSchoolError(error)) } }
    }

    private suspend fun loadRosterNow(classId: String) {
        val roster = repository.getClassRoster(classId).getOrThrow()
        _state.update { it.copy(roster = roster) }
    }

    private suspend fun reloadData() {
        val classes = repository.getClasses().getOrThrow()
        val students = repository.getStudents().getOrThrow()
        _state.update { old ->
            old.copy(
                classes = classes,
                students = students,
                selectedClass = old.selectedClass?.id?.let { id -> classes.firstOrNull { it.id == id } }
            )
        }
    }

    private fun action(successMessage: String, block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null, lastCredential = null) }
        runCatching { block() }
            .onSuccess { _state.update { it.copy(actionLoading = false, message = successMessage) } }
            .onFailure { error -> _state.update { it.copy(actionLoading = false, error = safeSchoolError(error)) } }
    }

    private fun failLoad(error: Throwable) {
        _state.update { it.copy(loading = false, error = safeSchoolError(error)) }
    }
}

private fun safeSchoolError(error: Throwable): String {
    val raw = error.message.orEmpty()
        .substringBefore("URL:")
        .substringBefore("Headers:")
        .replace(Regex("(?i)authorization[^,\n]*"), "")
        .replace(Regex("(?i)apikey[^,\n]*"), "")
        .take(240)
    return raw.ifBlank { "عملیات کلاس و دانش‌آموز ناموفق بود." }
}
