package ir.exam.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.SupabasePortabilityRepository
import ir.exam.app.data.repository.SupabaseTeacherDashboardRepository
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.PortableFile
import ir.exam.app.ui.builder.ExamImportDraft
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherDashboardState(
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val exams: List<ExamDashboardDto> = emptyList(),
    val portabilityLoading: Boolean = false,
    val exportFile: PortableFile? = null,
    val importDraft: ExamImportDraft? = null,
    val printExam: OfficialExamPrintable? = null,
    val error: String? = null,
    val message: String? = null
)

class TeacherDashboardViewModel(
    private val repository: SupabaseTeacherDashboardRepository = SupabaseTeacherDashboardRepository(),
    private val portability: SupabasePortabilityRepository = SupabasePortabilityRepository()
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

    fun exportExam(examId: String, includeAnswerKey: Boolean = true) = viewModelScope.launch {
        _state.update { it.copy(portabilityLoading = true, exportFile = null, error = null) }
        portability.exportExam(examId, includeAnswerKey)
            .onSuccess { file -> _state.update { it.copy(portabilityLoading = false, exportFile = file) } }
            .onFailure { error -> _state.update { it.copy(portabilityLoading = false, error = safeDashboardError(error)) } }
    }

    fun consumeExport() { _state.update { it.copy(exportFile = null) } }

    fun importExam(raw: String) {
        _state.update { it.copy(portabilityLoading = true, error = null, importDraft = null) }
        runCatching { portability.parseExam(raw) }
            .onSuccess { draft -> _state.update { it.copy(portabilityLoading = false, importDraft = draft) } }
            .onFailure { error -> _state.update { it.copy(portabilityLoading = false, error = safeDashboardError(error)) } }
    }

    fun consumeImport() { _state.update { it.copy(importDraft = null) } }

    // V62.7 — headerOverride: سربرگ تنظیم‌شده در صفحهٔ «چاپ آزمون».
    fun preparePrint(
        examId: String,
        includeAnswerKey: Boolean,
        headerOverride: ir.exam.app.domain.model.OfficialPrintHeader? = null,
        // V63.5 — چیدمان چاپی محلی: فقط خروجی چاپ، نه خود آزمون.
        questionsOverride: List<ir.exam.app.ui.builder.QuestionDraft>? = null
    ) = viewModelScope.launch {
        _state.update { it.copy(portabilityLoading = true, printExam = null, error = null) }
        portability.printableExam(examId, includeAnswerKey, headerOverride, questionsOverride)
            .onSuccess { printable -> _state.update { it.copy(portabilityLoading = false, printExam = printable) } }
            .onFailure { error -> _state.update { it.copy(portabilityLoading = false, error = safeDashboardError(error)) } }
    }

    fun consumePrint() { _state.update { it.copy(printExam = null) } }

    fun reportError(error: Throwable) {
        _state.update { it.copy(portabilityLoading = false, error = safeDashboardError(error)) }
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
