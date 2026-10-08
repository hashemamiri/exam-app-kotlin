package ir.exam.app.ui.portability

import ir.exam.app.core.network.UserFacingError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabasePortabilityRepository
import ir.exam.app.domain.model.BackupPreview
import ir.exam.app.domain.model.PortableFile
import ir.exam.app.domain.model.RestoreOptions
import ir.exam.app.domain.model.StorageMaintenanceSummary
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
    val maintenance: StorageMaintenanceSummary? = null,
    val error: String? = null,
    val message: String? = null,
    /** V202 — پیام کسر هزینهٔ بازیابی؛ پنجرهٔ وسط صفحه، فقط با «تأیید» بسته می‌شود. */
    val costDialog: String? = null
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
                        costDialog = "بازیابی شد: ${summary.examsCreated} آزمون، ${summary.classesCreated} کلاس، " +
                            "${summary.membershipsRestored} عضویت.\nکسر ${"%,d".format(java.util.Locale.US, summary.chargedToman)} تومان از کیف پول با موفقیت انجام شد."
                    )
                }
            }
            .onFailure(::fail)
    }

    fun dismissCostDialog() = _state.update { it.copy(costDialog = null) }

    fun checkStorage() = runMaintenance(true)
    fun cleanStorage() = runMaintenance(false)

    private fun runMaintenance(dryRun: Boolean) = viewModelScope.launch {
        _state.update { it.copy(loading = true, maintenance = null, error = null, message = null) }
        repository.storageMaintenance(dryRun)
            .onSuccess { result ->
                _state.update {
                    it.copy(
                        loading = false,
                        maintenance = result,
                        message = if (dryRun) "بررسی Storage بدون حذف انجام شد."
                        else "پاک‌سازی مجاز Storage انجام شد."
                    )
                }
            }
            .onFailure(::fail)
    }

    private fun fail(error: Throwable) {
        val safe = UserFacingError.of(error, "عملیات پشتیبان ناموفق بود.") // V209 — بدون متن فنی
        _state.update { it.copy(loading = false, error = safe) }
    }
}
