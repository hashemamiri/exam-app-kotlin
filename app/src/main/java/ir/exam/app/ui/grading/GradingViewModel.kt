package ir.exam.app.ui.grading
import androidx.lifecycle.ViewModel
import ir.exam.app.domain.model.*
import kotlinx.coroutines.flow.*
data class GradingUiState(val result:GradingResult?=null,val activeQuestionId:String?=null)
/** محل نمره‌دهی دستی و بازخورد؛ هر تغییر نمره فوراً total را از نو محاسبه می‌کند. */
class GradingViewModel:ViewModel(){private val _state=MutableStateFlow(GradingUiState());val state=_state.asStateFlow();fun load(result:GradingResult){_state.value=GradingUiState(result)};fun setManual(questionId:String,score:Double,feedback:String?){_state.update{s->s.copy(result=s.result?.copy(items=s.result.items.map{if(it.questionId==questionId)it.copy(earned=score.coerceIn(0.0,it.max),feedback=feedback)else it}))}}}
