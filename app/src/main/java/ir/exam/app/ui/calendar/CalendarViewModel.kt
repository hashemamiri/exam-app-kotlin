package ir.exam.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.data.repository.SupabaseCalendarRepository
import ir.exam.app.domain.model.CalendarAudience
import ir.exam.app.domain.model.CalendarAudienceOption
import ir.exam.app.domain.model.CalendarEditor
import ir.exam.app.domain.model.CalendarMonth
import ir.exam.app.domain.model.CalendarNote
import ir.exam.app.domain.model.UserRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarState(
    val year: Int,
    val month: Int,
    val selectedDate: JalaliDate,
    val monthData: CalendarMonth? = null,
    val classes: List<CalendarAudienceOption> = emptyList(),
    val students: List<CalendarAudienceOption> = emptyList(),
    val schools: List<CalendarAudienceOption> = emptyList(),
    val editor: CalendarEditor? = null,
    val loading: Boolean = true,
    val editorLoading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class CalendarViewModel(
    private val role: UserRole,
    private val repository: SupabaseCalendarRepository = SupabaseCalendarRepository(),
    today: JalaliDate = JalaliCalendar.today()
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState(today.year, today.month, today))
    val state = _state.asStateFlow()
    private var monthJob: Job? = null

    init {
        loadAudienceOptions()
        loadMonth()
    }

    fun refresh() = loadMonth()

    fun previousMonth() = shift(-1)
    fun nextMonth() = shift(1)

    fun goToday() {
        val today = JalaliCalendar.today()
        _state.update { it.copy(year = today.year, month = today.month, selectedDate = today, error = null) }
        loadMonth()
    }

    fun select(date: JalaliDate) {
        _state.update { it.copy(selectedDate = date, message = null) }
    }

    fun newNote(date: JalaliDate = state.value.selectedDate) {
        if (role != UserRole.TEACHER) return
        _state.update { it.copy(editor = CalendarEditor(date = date), error = null, message = null) }
    }

    fun edit(note: CalendarNote) {
        if (role != UserRole.TEACHER) return
        val date = JalaliCalendar.fromGregorian(note.date)
        _state.update {
            it.copy(
                editor = CalendarEditor(
                    id = note.id,
                    date = date,
                    title = note.title,
                    body = note.body,
                    audience = note.audience,
                    classIds = note.classIds,
                    studentIds = note.studentIds
                ),
                editorLoading = true,
                error = null,
                message = null
            )
        }
        viewModelScope.launch {
            repository.loadNote(note.id)
                .onSuccess { full ->
                    _state.update { current ->
                        if (current.editor?.id != note.id) current
                        else current.copy(
                            editor = current.editor.copy(
                                title = full.title,
                                body = full.body,
                                audience = full.audience,
                                classIds = full.classIds,
                                studentIds = full.studentIds,
                                schoolIds = full.schoolIds
                            ),
                            editorLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(editorLoading = false, error = safeCalendarError(error)) }
                }
        }
    }

    fun dismissEditor() {
        if (!state.value.saving) _state.update { it.copy(editor = null, editorLoading = false) }
    }

    fun setEditorTitle(value: String) = updateEditor { it.copy(title = value.take(120)) }
    fun setEditorBody(value: String) = updateEditor { it.copy(body = value.take(2000)) }
    fun setAudience(value: CalendarAudience) = updateEditor { editor ->
        editor.copy(
            audience = value,
            classIds = if (value == CalendarAudience.CLASSES) editor.classIds else emptySet(),
            studentIds = if (value == CalendarAudience.STUDENTS) editor.studentIds else emptySet(),
            schoolIds = if (value == CalendarAudience.SCHOOLS) editor.schoolIds else emptySet()
        )
    }
    fun toggleClass(id: String) = updateEditor { it.copy(classIds = it.classIds.toggle(id)) }
    fun toggleStudent(id: String) = updateEditor { it.copy(studentIds = it.studentIds.toggle(id)) }
    fun toggleSchool(id: String) = updateEditor { it.copy(schoolIds = it.schoolIds.toggle(id)) }

    fun saveEditor() {
        val editor = state.value.editor ?: return
        if (role != UserRole.TEACHER || state.value.editorLoading) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, message = null) }
            repository.save(editor)
                .onSuccess {
                    _state.update { it.copy(saving = false, editor = null, message = "پیام تقویم ذخیره شد.") }
                    loadMonth()
                }
                .onFailure { error ->
                    _state.update { it.copy(saving = false, error = safeCalendarError(error)) }
                }
        }
    }

    fun delete(note: CalendarNote) {
        if (role != UserRole.TEACHER) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, message = null) }
            repository.delete(note.id)
                .onSuccess {
                    _state.update { it.copy(saving = false, editor = null, message = "پیام حذف شد.") }
                    loadMonth()
                }
                .onFailure { error -> _state.update { it.copy(saving = false, error = safeCalendarError(error)) } }
        }
    }

    private fun shift(delta: Int) {
        val current = state.value
        runCatching { JalaliCalendar.shiftMonth(current.year, current.month, delta) }
            .onSuccess { (year, month) ->
                val selected = JalaliDate(year, month, 1)
                _state.update { it.copy(year = year, month = month, selectedDate = selected, error = null) }
                loadMonth()
            }
            .onFailure { error -> _state.update { it.copy(error = safeCalendarError(error)) } }
    }

    private fun loadMonth() {
        monthJob?.cancel()
        val targetYear = state.value.year
        val targetMonth = state.value.month
        monthJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.loadMonth(targetYear, targetMonth)
                .onSuccess { month ->
                    _state.update { current ->
                        if (current.year != targetYear || current.month != targetMonth) current
                        else current.copy(loading = false, monthData = month)
                    }
                }
                .onFailure { error -> _state.update { it.copy(loading = false, error = safeCalendarError(error)) } }
        }
    }

    private fun loadAudienceOptions() {
        if (role != UserRole.TEACHER) return
        viewModelScope.launch {
            repository.loadAudienceOptions(role)
                .onSuccess { (classes, students) ->
                    _state.update { it.copy(classes = classes, students = students) }
                }
                .onFailure { error -> _state.update { it.copy(error = safeCalendarError(error)) } }
        }
        // V61.0 — مدرسه‌های معلم برای مخاطب «مدارس»؛ خطا فقط لاگ حالت می‌شود.
        viewModelScope.launch {
            repository.loadSchoolOptions()
                .onSuccess { schools -> _state.update { it.copy(schools = schools) } }
                .onFailure { }
        }
    }

    private fun updateEditor(change: (CalendarEditor) -> CalendarEditor) {
        _state.update { state -> state.copy(editor = state.editor?.let(change), error = null) }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

private fun safeCalendarError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(260)
    .ifBlank { "دریافت تقویم ناموفق بود." }
