package ir.exam.app.ui.portability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabasePortabilityRepository
import ir.exam.app.domain.model.BackupPreview
import ir.exam.app.domain.model.PortableFile
import ir.exam.app.domain.model.RestoreOptions
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataPortabilityState(
    val loading: Boolean = false,
    val exportFile: PortableFile? = null,
    val preview: BackupPreview? = null,
    val options: RestoreOptions = RestoreOptions(),
    val error: String? = null,
    val message: String? = null
)

class DataPortabilityViewModel(
    private val repository: SupabasePortabilityRepository = SupabasePortabilityRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(DataPortabilityState())
    val state = _state.asStateFlow()
    private var restoreOperationId: String = UUID.randomUUID().toString()

    fun exportBackup() = viewModelScope.launch {
        _state.update { it.copy(loading = true, exportFile = null, error = null, message = null) }
        repository.exportBackup()
            .onSuccess { file -> _state.update { it.copy(loading = false, exportFile = file) } }
            .onFailure(::fail)
    }

    fun consumeExport() { _state.update { it.copy(exportFile = null) } }

    fun parseBackup(raw: String) {
        _state.update { it.copy(loading = true, preview = null, error = null, message = null) }
        runCatching { repository.parseBackup(raw) }
            .onSuccess { preview ->
                restoreOperationId = UUID.randomUUID().toString()
                _state.update { it.copy(loading = false, preview = preview) }
            }
            .onFailure(::fail)
    }

    fun reportError(error: Throwable) = fail(error)

    fun dismissPreview() { if (!state.value.loading) _state.update { it.copy(preview = null) } }
    fun setExams(value: Boolean) { _state.update { it.copy(options = it.options.copy(exams = value)) } }
    fun setClasses(value: Boolean) { _state.update { it.copy(options = it.options.copy(classes = value)) } }
    fun setMemberships(value: Boolean) { _state.update { it.copy(options = it.options.copy(memberships = value)) } }
    fun setHeader(value: Boolean) { _state.update { it.copy(options = it.options.copy(header = value)) } }

    fun restore() = viewModelScope.launch {
        val preview = state.value.preview ?: return@launch
        _state.update { it.copy(loading = true, error = null, message = null) }
        repository.restoreBackup(preview, state.value.options, restoreOperationId)
            .onSuccess { summary ->
                restoreOperationId = UUID.randomUUID().toString()
                _state.update {
                    it.copy(
                        loading = false,
                        preview = null,
                        message = "بازیابی شد: ${summary.examsCreated} آزمون، ${summary.classesCreated} کلاس، " +
                            "${summary.membershipsRestored} عضویت؛ هزینه ${summary.chargedToman} تومان."
                    )
                }
            }
            .onFailure(::fail)
    }

    private fun fail(error: Throwable) {
        val safe = error.message.orEmpty()
            .substringBefore("URL:").substringBefore("Headers:")
            .replace(Regex("(?i)authorization[^,\n]*"), "")
            .replace(Regex("(?i)apikey[^,\n]*"), "")
            .take(260).ifBlank { "عملیات پشتیبان ناموفق بود." }
        _state.update { it.copy(loading = false, error = safe) }
    }
}
