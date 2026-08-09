package ir.exam.app.ui.image
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.*
import ir.exam.app.domain.repository.ImageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
data class ImageEditorState(val request:ImageEditRequest?=null,val loading:Boolean=false,val error:String?=null,val result:PreparedImage?=null)
/** Editor یک خروجی PreparedImage تولید می‌کند؛ upload نباید editor را دوباره باز کند. */
class ImageEditorViewModel(private val repo:ImageRepository):ViewModel(){
 private val _state=MutableStateFlow(ImageEditorState());val state=_state.asStateFlow()
 fun open(uri:Uri,square:Boolean=false){_state.value=ImageEditorState(request=ImageEditRequest(uri,forceSquare=square))}
 fun rotate(delta:Int){_state.update{s->s.copy(request=s.request?.copy(rotationDegrees=(s.request.rotationDegrees+delta)%360)}}
 fun crop(rect:CropRect){_state.update{s->s.copy(request=s.request?.copy(crop=rect))}}
 fun apply(){val r=state.value.request?:return;viewModelScope.launch{_state.update{it.copy(loading=true,error=null)};repo.prepare(r).onSuccess{v->_state.update{it.copy(loading=false,result=v)}}.onFailure{e->_state.update{it.copy(loading=false,error=e.message)}}}}
 fun cancel(){_state.value=ImageEditorState()}
}
