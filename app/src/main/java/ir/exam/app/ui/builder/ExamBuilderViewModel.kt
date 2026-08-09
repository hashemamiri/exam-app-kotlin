package ir.exam.app.ui.builder
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*
/** منبع حقیقت آزمون‌ساز؛ هر تغییر فقط state را عوض می‌کند و UI از روی آن رندر می‌شود. */
class ExamBuilderViewModel:ViewModel(){
 private val _state=MutableStateFlow(ExamBuilderState());val state=_state.asStateFlow()
 fun setTitle(v:String){_state.update{it.copy(title=v)}}
 fun setSubject(v:String){_state.update{it.copy(subject=v)}}
 fun setDuration(v:String){_state.update{it.copy(durationMinutes=v.filter(Char::isDigit))}}
 fun addQuestion(type:QuestionType){val q=when(type){QuestionType.MULTIPLE_CHOICE->QuestionDraft(type=type,options=List(4){""});else->QuestionDraft(type=type)};_state.update{it.copy(questions=it.questions+q)}}
 fun updateText(id:String,text:String){_state.update{s->s.copy(questions=s.questions.map{if(it.id==id)it.copy(text=text)else it})}}
 fun updateOption(id:String,index:Int,text:String){_state.update{s->s.copy(questions=s.questions.map{q->if(q.id==id)q.copy(options=q.options.mapIndexed{i,v->if(i==index)text else v})else q})}}
 fun setCorrect(id:String,index:Int){_state.update{s->s.copy(questions=s.questions.map{if(it.id==id)it.copy(correctIndex=index)else it})}}
 fun remove(id:String){_state.update{s->s.copy(questions=s.questions.filterNot{it.id==id})}}
 fun move(from:Int,to:Int){_state.update{s->val list=s.questions.toMutableList();if(from in list.indices&&to in list.indices){list.add(to,list.removeAt(from))};s.copy(questions=list)}}
}
