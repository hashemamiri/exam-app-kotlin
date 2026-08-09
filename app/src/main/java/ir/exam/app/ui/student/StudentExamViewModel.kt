package ir.exam.app.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.domain.model.*
import ir.exam.app.domain.repository.AnswerDraftRepository
import ir.exam.app.domain.repository.ExamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentExamUiState(
    val code:String="", val exam:Exam?=null, val answers:Map<String,StudentAnswer> = emptyMap(),
    val questionIndex:Int=0, val remainingSeconds:Long=0, val loading:Boolean=false,
    val submitting:Boolean=false, val finished:Boolean=false, val error:String?=null
)
/** منطق آزمون دانش‌آموز: ورود با کد، پاسخ‌های موقت، تایمر و ارسال نهایی. */
class StudentExamViewModel(private val exams:ExamRepository, private val drafts:AnswerDraftRepository):ViewModel() {
    private val _state=MutableStateFlow(StudentExamUiState()); val state=_state.asStateFlow(); private var timer:Job?=null
    fun setCode(value:String){ _state.update{it.copy(code=value.trim(),error=null)} }
    fun join(){ viewModelScope.launch { _state.update{it.copy(loading=true,error=null)}; exams.joinByCode(state.value.code).onSuccess { exam ->
        _state.update{it.copy(exam=exam,remainingSeconds=exam.durationMinutes*60L)}; startTimer(); observeDrafts(exam.id)
    }.onFailure{e->_state.update{it.copy(error=e.message?:"ورود به آزمون ممکن نیست")}}; _state.update{it.copy(loading=false)} } }
    private fun observeDrafts(examId:String)=viewModelScope.launch { drafts.observe(examId).collect{a->_state.update{it.copy(answers=a)}} }
    private fun startTimer(){ timer?.cancel(); timer=viewModelScope.launch { while(state.value.remainingSeconds>0 && !state.value.finished){delay(1000);_state.update{it.copy(remainingSeconds=(it.remainingSeconds-1).coerceAtLeast(0))}}; if(state.value.remainingSeconds==0L) submit() } }
    fun answer(answer:StudentAnswer){ val exam=state.value.exam?:return; val next=state.value.answers+ (answer.questionId to answer); _state.update{it.copy(answers=next)}; viewModelScope.launch{drafts.save(exam.id,next)} }
    fun goTo(index:Int){ val max=(state.value.exam?.questions?.lastIndex?:0); _state.update{it.copy(questionIndex=index.coerceIn(0,max))} }
    fun submit(){ val exam=state.value.exam?:return; if(state.value.submitting||state.value.finished)return; viewModelScope.launch{_state.update{it.copy(submitting=true)}; val attempt=SubmittedExam(exam.id,state.value.answers,System.currentTimeMillis()); exams.submitAttempt(attempt).onSuccess{drafts.clear(exam.id);timer?.cancel();_state.update{it.copy(submitting=false,finished=true)}}.onFailure{e->_state.update{it.copy(submitting=false,error=e.message?:"ارسال پاسخ ناموفق بود")}}} }
    override fun onCleared(){timer?.cancel()}
}
