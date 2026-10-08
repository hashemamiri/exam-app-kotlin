package ir.exam.app.ui.calendar

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.network.UserFacingError
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
import kotlinx.coroutines.delay

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
    val refreshing: Boolean = false, // V227 — فقط برای نشانگر «کشیدن برای بازخوانی»
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

    // V227 — بازخوانی با کشیدن صفحه: پرچم جداگانهٔ refreshing (نه loading که با حافظهٔ موقت false می‌ماند و نه actionLoading)؛
    // یک بازخوانی هم‌زمان، حداقل ۵۰۰ms نمایش تا نشانگر Material3 بین حالت‌ها گیر نکند.
    private var refreshJob: Job? = null
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val started = System.currentTimeMillis()
            runCatching { loadMonth().join() }
            val rest = 500L - (System.currentTimeMillis() - started)
            if (rest > 0) delay(rest)
            _state.update { it.copy(refreshing = false) }
        }
    }


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

    private fun loadMonth(): Job {
        monthJob?.cancel()
        val targetYear = state.value.year
        val targetMonth = state.value.month
        monthJob = viewModelScope.launch {
            // V214 — ماهِ دیده‌شدهٔ قبلی (حافظهٔ موقت) فوراً نشان داده می‌شود؛ نسخهٔ تازه جایگزین می‌شود.
            val cacheKey = "calendar.month.$targetYear.$targetMonth"
            val cached = SessionCache.get<CalendarMonth>(cacheKey)
            _state.update { it.copy(loading = cached == null, monthData = cached ?: it.monthData, error = null) }
            repository.loadMonth(targetYear, targetMonth)
                .onSuccess { month ->
                    SessionCache.put(cacheKey, month)
                    _state.update { current ->
                        if (current.year != targetYear || current.month != targetMonth) current
                        else current.copy(loading = false, monthData = month)
                    }
                }
                .onFailure { error -> _state.update { it.copy(loading = false, error = if (cached == null) safeCalendarError(error) else null) } }
        }
        return monthJob!!
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

private fun safeCalendarError(error: Throwable): String = UserFacingError.of(error, "دریافت تقویم ناموفق بود.") // V209 — بدون متن فنی
