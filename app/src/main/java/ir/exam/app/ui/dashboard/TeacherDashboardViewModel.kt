package ir.exam.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.SupabaseTeacherDashboardRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherDashboardState(
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val exams: List<ExamDashboardDto> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class TeacherDashboardViewModel(
    private val repository: SupabaseTeacherDashboardRepository = SupabaseTeacherDashboardRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(TeacherDashboardState())
    val state = _state.asStateFlow()
    private val duplicateOperations = mutableMapOf<String, String>()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.getMyExams()
            .onSuccess { exams -> _state.update { it.copy(loading = false, exams = exams) } }
            .onFailure { error -> _state.update { it.copy(loading = false, error = safeDashboardError(error)) } }
    }

    fun setOpen(exam: ExamDashboardDto) = action(if (exam.isOpen) "آزمون بسته شد." else "آزمون باز شد.") {
        repository.setOpen(exam.id, !exam.isOpen).getOrThrow()
    }

    fun duplicate(exam: ExamDashboardDto) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        val operation = duplicateOperations.getOrPut(exam.id) { UUID.randomUUID().toString() }
        repository.duplicateExam(exam.id, operation)
            .onSuccess { result ->
                duplicateOperations.remove(exam.id)
                val exams = repository.getMyExams().getOrThrow()
                val balance = result.balanceToman?.let { " · مانده ${formatToman(it)} تومان" }.orEmpty()
                _state.update {
                    it.copy(
                        actionLoading = false,
                        exams = exams,
                        message = "کپی با کد ${result.code} ساخته شد · کسر ${formatToman(result.costToman)} تومان$balance"
                    )
                }
            }
            .onFailure { error -> _state.update { it.copy(actionLoading = false, error = safeDashboardError(error)) } }
    }

    fun delete(exam: ExamDashboardDto) = action("آزمون و داده‌های وابسته حذف شد.") {
        repository.deleteExam(exam.id).getOrThrow()
    }

    fun clearMessage() { _state.update { it.copy(message = null, error = null) } }

    private fun action(success: String, block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        runCatching { block() }
            .onSuccess {
                val exams = repository.getMyExams().getOrThrow()
                _state.update { it.copy(actionLoading = false, exams = exams, message = success) }
            }
            .onFailure { error -> _state.update { it.copy(actionLoading = false, error = safeDashboardError(error)) } }
    }
}

private fun formatToman(value: Long): String = ir.exam.app.core.calendar.PersianDigits.convert(
    "%,d".format(java.util.Locale.US, value)
)

private fun safeDashboardError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(240)
    .ifBlank { "عملیات آزمون ناموفق بود." }
