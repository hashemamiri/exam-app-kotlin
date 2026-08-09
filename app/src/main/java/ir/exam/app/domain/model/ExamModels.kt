package ir.exam.app.domain.model

data class Exam(val id: String, val title: String, val code: String, val durationMinutes: Int, val questions: List<Question>)
sealed interface Question { val id: String; val text: String; val score: Double }
data class EssayQuestion(override val id:String, override val text:String, override val score:Double): Question
data class MultipleChoiceQuestion(override val id:String, override val text:String, override val score:Double, val options:List<String>): Question
data class TrueFalseQuestion(override val id:String, override val text:String, override val score:Double): Question
data class FillBlankQuestion(override val id:String, override val text:String, override val score:Double): Question
data class NumericQuestion(override val id:String, override val text:String, override val score:Double): Question
sealed interface StudentAnswer { val questionId:String }
data class TextAnswer(override val questionId:String, val value:String):StudentAnswer
data class ChoiceAnswer(override val questionId:String, val selectedIndex:Int):StudentAnswer
data class BooleanAnswer(override val questionId:String, val value:Boolean):StudentAnswer
data class SubmittedExam(val examId:String, val answers:Map<String, StudentAnswer>, val submittedAtEpochMs:Long)
