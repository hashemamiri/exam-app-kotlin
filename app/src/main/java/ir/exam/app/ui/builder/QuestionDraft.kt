package ir.exam.app.ui.builder
import java.util.UUID
enum class QuestionType { ESSAY, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, NUMERIC, MATCHING }
data class QuestionDraft(val id:String=UUID.randomUUID().toString(),val type:QuestionType,val text:String="",val score:Double=1.0,val options:List<String> = emptyList(),val correctIndex:Int?=null)
data class ExamBuilderState(val title:String="",val subject:String="",val durationMinutes:String="",val questions:List<QuestionDraft> = emptyList(),val saving:Boolean=false,val error:String?=null)
