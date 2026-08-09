package ir.exam.app.ui.image

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.CropRect
import ir.exam.app.domain.model.ImageEditRequest
import ir.exam.app.domain.model.PreparedImage
import ir.exam.app.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageEditorState(
    val request: ImageEditRequest? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val result: PreparedImage? = null
)

/** فقط یک بار خروجی editor را آماده می‌کند؛ آپلود بعدی editor را دوباره باز نمی‌کند. */
class ImageEditorViewModel(private val repository: ImageRepository) : ViewModel() {
    private val _state = MutableStateFlow(ImageEditorState())
    val state = _state.asStateFlow()

    fun open(uri: Uri, square: Boolean = false) {
        _state.value = ImageEditorState(request = ImageEditRequest(source = uri, forceSquare = square))
    }

    fun rotate(delta: Int) {
        _state.update { old ->
            val request = old.request ?: return@update old
            old.copy(request = request.copy(rotationDegrees = (request.rotationDegrees + delta) % 360))
        }
    }

    fun crop(rect: CropRect) {
        _state.update { old -> old.copy(request = old.request?.copy(crop = rect)) }
    }

    fun apply() {
        val request = state.value.request ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.prepare(request)
                .onSuccess { image -> _state.update { it.copy(loading = false, result = image) } }
                .onFailure { error -> _state.update { it.copy(loading = false, error = error.message ?: "ویرایش تصویر ناموفق بود") } }
        }
    }

    fun cancel() { _state.value = ImageEditorState() }
}
