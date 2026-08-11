package ir.exam.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.core.update.ApkUpdateManager
import ir.exam.app.core.update.RemoteVersion
import ir.exam.app.core.update.UpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateState(
    val checking: Boolean = false,
    val update: RemoteVersion? = null,
    val message: String? = null,
    val error: String? = null,
    val downloading: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val downloadedApkPath: String? = null,
    val autoInstallPending: Boolean = false
) {
    val downloadFraction: Float?
        get() = totalBytes
            ?.takeIf { it > 0L }
            ?.let { (downloadedBytes.toDouble() / it.toDouble()).coerceIn(0.0, 1.0).toFloat() }
}

class UpdateViewModel(
    private val useCase: UpdateUseCase,
    private val apkUpdateManager: ApkUpdateManager
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun check(installedCode: Int) {
        if (state.value.checking || state.value.downloading) return
        viewModelScope.launch {
            _state.value = UpdateState(checking = true)
            useCase.check(installedCode)
                .onSuccess { remote ->
                    _state.value = if (remote == null) {
                        UpdateState(message = "برنامه شما به‌روز است.")
                    } else {
                        UpdateState(update = remote, totalBytes = remote.sizeBytes)
                    }
                }
                .onFailure { error ->
                    _state.value = UpdateState(error = safeUpdateError(error))
                }
        }
    }

    fun downloadAndInstall() {
        val remote = state.value.update ?: return
        if (state.value.downloading) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    message = null,
                    error = null,
                    downloading = true,
                    downloadedBytes = 0L,
                    totalBytes = remote.sizeBytes,
                    downloadedApkPath = null,
                    autoInstallPending = false
                )
            }

            apkUpdateManager.download(remote) { progress ->
                _state.update {
                    it.copy(
                        downloadedBytes = progress.downloadedBytes,
                        totalBytes = progress.totalBytes ?: remote.sizeBytes
                    )
                }
            }.onSuccess { apk ->
                _state.update {
                    it.copy(
                        downloading = false,
                        downloadedBytes = apk.length(),
                        totalBytes = apk.length(),
                        downloadedApkPath = apk.absolutePath,
                        autoInstallPending = true,
                        message = "دانلود و بررسی امنیتی کامل شد."
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        downloading = false,
                        error = safeUpdateError(error),
                        downloadedApkPath = null,
                        autoInstallPending = false
                    )
                }
            }
        }
    }

    fun markAutoInstallHandled() {
        _state.update { it.copy(autoInstallPending = false) }
    }

    fun reportPermissionRequired() {
        _state.update {
            it.copy(
                message = "اجازه «نصب برنامه‌های ناشناس» را برای سامانه آزمون فعال کنید و برگردید.",
                error = null
            )
        }
    }

    fun reportInstallerOpened() {
        _state.update {
            it.copy(message = "نصب‌کننده Android باز شد؛ نصب نسخه جدید را تأیید کنید.", error = null)
        }
    }

    fun reportInstallError(error: Throwable) {
        _state.update { it.copy(error = safeUpdateError(error)) }
    }
}

/** Header، URL، کلید و Token هیچ‌وقت در رابط کاربر نمایش داده نمی‌شوند. */
private fun safeUpdateError(error: Throwable): String {
    val raw = error.message.orEmpty()
        .substringBefore("URL:", missingDelimiterValue = error.message.orEmpty())
        .substringBefore("Headers:")
        .replace(Regex("(?i)authorization[^,\n]*"), "")
        .replace(Regex("(?i)apikey[^,\n]*"), "")
        .replace(Regex("https?://\\S+"), "")
        .trim()
        .take(220)
    val lower = raw.lowercase()
    return when {
        "jwt expired" in lower ->
            "نشست شبکه در حال تازه‌سازی است؛ چند لحظه بعد دوباره بررسی کنید."
        "unable to resolve host" in lower || "failed to connect" in lower ->
            "اتصال اینترنت برقرار نیست یا سرور بروزرسانی در دسترس نیست."
        "app_version" in lower && ("404" in lower || "does not exist" in lower) ->
            "جدول نسخه برنامه هنوز در Supabase راه‌اندازی نشده است."
        "permission" in lower || "اجازه" in lower ->
            raw.ifBlank { "مجوز لازم برای ادامه عملیات داده نشده است." }
        raw.isNotBlank() -> raw
        else -> "بررسی یا دریافت بروزرسانی ناموفق بود؛ دوباره تلاش کنید."
    }
}
