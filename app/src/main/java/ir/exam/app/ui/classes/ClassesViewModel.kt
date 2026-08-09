package ir.exam.app.ui.classes
import androidx.lifecycle.ViewModel
import ir.exam.app.domain.model.SchoolClass
import kotlinx.coroutines.flow.*
data class ClassesState(val classes:List<SchoolClass> = emptyList(),val selected:SchoolClass?=null,val query:String="")
/** مدیریت کلاس و انتخاب مخاطب آزمون؛ Repository در مرحله اتصال Supabase داده واقعی می‌دهد. */
class ClassesViewModel:ViewModel(){private val _state=MutableStateFlow(ClassesState());val state=_state.asStateFlow();fun setClasses(v:List<SchoolClass>){_state.update{it.copy(classes=v)}};fun select(id:String){_state.update{s->s.copy(selected=s.classes.firstOrNull{it.id==id})}};fun search(v:String){_state.update{it.copy(query=v)}}}
