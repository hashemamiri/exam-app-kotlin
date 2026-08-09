package ir.exam.app.ui.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.core.update.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
data class UpdateState(val loading:Boolean=false,val update:RemoteVersion?=null,val message:String?=null)
class UpdateViewModel(private val useCase:UpdateUseCase):ViewModel(){private val _state=MutableStateFlow(UpdateState());val state=_state.asStateFlow();fun check(installed:Int)=viewModelScope.launch{_state.value=UpdateState(loading=true);useCase.check(installed).onSuccess{_state.value=UpdateState(update=it,message=if(it==null)"برنامه به‌روز است" else null)}.onFailure{_state.value=UpdateState(message=it.message)}}}
