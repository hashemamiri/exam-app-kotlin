package ir.exam.app.ui.reports
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.StudentReport
import kotlinx.coroutines.flow.*
data class ReportsState(val reports:List<StudentReport> = emptyList(),val query:String="")
class ReportsViewModel:ViewModel(){private val _state=MutableStateFlow(ReportsState());val state=_state.asStateFlow();fun load(items:List<StudentReport>){_state.value=ReportsState(items)};fun search(q:String){_state.update{it.copy(query=q)}};val filtered=state.map{s->s.reports.filter{it.studentName.contains(s.query,true)}}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())}
