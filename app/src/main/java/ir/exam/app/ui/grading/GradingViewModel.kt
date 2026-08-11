package ir.exam.app.ui.grading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.SupabaseGradingRepository
import ir.exam.app.domain.model.AttendanceRow
import ir.exam.app.domain.model.FeedbackPhrase
import ir.exam.app.domain.model.GradingExam
import ir.exam.app.domain.model.GradingSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

data class GradingEdit(val grades: List<Double>, val feedback: String)

data class GradingUiState(
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val exams: List<ExamDashboardDto> = emptyList(),
    val selectedExam: GradingExam? = null,
    val submissions: List<GradingSubmission> = emptyList(),
    val edits: Map<String, GradingEdit> = emptyMap(),
    val feedbackBank: List<FeedbackPhrase> = emptyList(),
    val attendance: List<AttendanceRow> = emptyList(),
    val liveStatus: JsonObject? = null,
    val mode: String = "grading",
    val error: String? = null,
    val message: String? = null
)

class GradingViewModel(
    private val repository: SupabaseGradingRepository = SupabaseGradingRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(GradingUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val exams = repository.getExams().getOrElse { return@launch fail(it) }
        val feedback = repository.feedbackBank().getOrDefault(emptyList())
        _state.update { it.copy(loading = false, exams = exams, feedbackBank = feedback) }
    }

    fun selectExam(id: String) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        val exam = repository.getExam(id).getOrElse { return@launch fail(it) }
        val submissions = repository.getAnswers(id).getOrElse { return@launch fail(it) }
        val edits = submissions.associate { answer ->
            val grades = exam.questions.indices.map { index -> answer.grades.getOrElse(index) { 0.0 } }
            answer.id to GradingEdit(grades, answer.feedback)
        }
        _state.update {
            it.copy(
                actionLoading = false,
                selectedExam = exam,
                submissions = submissions,
                edits = edits,
                mode = "grading"
            )
        }
    }

    fun back() {
        _state.update { it.copy(selectedExam = null, submissions = emptyList(), attendance = emptyList(), liveStatus = null) }
    }

    fun setMode(value: String) {
        _state.update { it.copy(mode = value) }
        if (value == "attendance") loadAttendance()
    }

    fun setScore(answerId: String, questionIndex: Int, raw: String) {
        val max = state.value.selectedExam?.questions?.getOrNull(questionIndex)?.score ?: return
        val value = raw.toDoubleOrNull()?.coerceIn(0.0, max) ?: 0.0
        _state.update { old ->
            val edit = old.edits[answerId] ?: return@update old
            val grades = edit.grades.mapIndexed { index, current -> if (index == questionIndex) value else current }
            old.copy(edits = old.edits + (answerId to edit.copy(grades = grades)))
        }
    }

    fun setFeedback(answerId: String, value: String) {
        _state.update { old ->
            val edit = old.edits[answerId] ?: return@update old
            old.copy(edits = old.edits + (answerId to edit.copy(feedback = value.take(2000))))
        }
    }

    fun appendFeedback(answerId: String, phrase: String) {
        val current = state.value.edits[answerId]?.feedback.orEmpty()
        setFeedback(answerId, listOf(current, phrase).filter(String::isNotBlank).joinToString("\n"))
    }

    fun save(answerId: String) = action("نمره و بازخورد ذخیره شد.") {
        val edit = state.value.edits[answerId] ?: error("داده نمره پیدا نشد.")
        repository.saveGrade(answerId, edit.grades, edit.feedback).getOrThrow()
        reloadSelected()
    }

    fun addFeedback(text: String) = action("عبارت بازخورد ذخیره شد.") {
        repository.addFeedback(text).getOrThrow()
        val phrases = repository.feedbackBank().getOrThrow()
        _state.update { it.copy(feedbackBank = phrases) }
    }

    fun approveAutoGrades() = action("نمره‌های خودکار تأیید شدند.") {
        val id = state.value.selectedExam?.id ?: error("آزمون انتخاب نشده است.")
        repository.approveAutoGrades(id).getOrThrow()
        reloadSelected()
    }

    fun unapprove(answerId: String) = action("تأیید نمره برداشته شد.") {
        repository.unapprove(answerId).getOrThrow()
        reloadSelected()
    }

    fun extendTime(studentId: String, minutes: Int = 10) = action("زمان دانش‌آموز تمدید شد.") {
        val id = state.value.selectedExam?.id ?: error("آزمون انتخاب نشده است.")
        repository.extendTime(id, studentId, minutes).getOrThrow()
        loadAttendanceNow(id)
    }

    fun resetAttempt(studentId: String) = action("اجازه تلاش مجدد ثبت شد.") {
        val id = state.value.selectedExam?.id ?: error("آزمون انتخاب نشده است.")
        repository.resetAttempt(id, studentId).getOrThrow()
        loadAttendanceNow(id)
    }

    private fun loadAttendance() = viewModelScope.launch {
        val id = state.value.selectedExam?.id ?: return@launch
        _state.update { it.copy(actionLoading = true, error = null) }
        runCatching { loadAttendanceNow(id) }
            .onFailure(::fail)
    }

    private suspend fun loadAttendanceNow(id: String) {
        val attendance = repository.attendance(id).getOrThrow()
        val live = repository.liveStatus(id).getOrNull()
        _state.update { it.copy(actionLoading = false, attendance = attendance, liveStatus = live) }
    }

    private suspend fun reloadSelected() {
        val id = state.value.selectedExam?.id ?: return
        val submissions = repository.getAnswers(id).getOrThrow()
        val exam = state.value.selectedExam ?: return
        val edits = submissions.associate { answer ->
            answer.id to GradingEdit(
                exam.questions.indices.map { index -> answer.grades.getOrElse(index) { 0.0 } },
                answer.feedback
            )
        }
        _state.update { it.copy(submissions = submissions, edits = edits) }
    }

    private fun action(message: String, block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        runCatching { block() }
            .onSuccess { _state.update { it.copy(actionLoading = false, message = message) } }
            .onFailure(::fail)
    }

    private fun fail(error: Throwable) {
        _state.update { it.copy(loading = false, actionLoading = false, error = safeGradingError(error)) }
    }
}

private fun safeGradingError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:").substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(260).ifBlank { "عملیات تصحیح ناموفق بود." }
