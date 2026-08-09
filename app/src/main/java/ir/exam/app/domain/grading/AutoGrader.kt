package ir.exam.app.domain.grading
import ir.exam.app.domain.model.*
/** قرارداد تصحیح خودکار؛ هر نوع سؤال پیاده‌سازی جدا و قابل تست دارد. */
interface AutoGrader { fun canGrade(question:Question):Boolean; fun grade(question:Question,answer:StudentAnswer):GradeItem }
class MultipleChoiceAutoGrader(private val correct:Map<String,Int>):AutoGrader{override fun canGrade(question:Question)=question is MultipleChoiceQuestion;override fun grade(question:Question,answer:StudentAnswer):GradeItem{val q=question as MultipleChoiceQuestion;val ok=(answer as? ChoiceAnswer)?.selectedIndex==correct[q.id];return GradeItem(q.id,if(ok)q.score else 0.0,q.score)}}
class NumericAutoGrader(private val expected:Map<String,Double>,private val tolerance:Map<String,Double>):AutoGrader{override fun canGrade(question:Question)=question is NumericQuestion;override fun grade(question:Question,answer:StudentAnswer):GradeItem{val q=question as NumericQuestion;val value=(answer as? TextAnswer)?.value?.toDoubleOrNull();val ok=value!=null&&kotlin.math.abs(value-(expected[q.id]?:Double.NaN))<=(tolerance[q.id]?:0.0);return GradeItem(q.id,if(ok)q.score else 0.0,q.score)}}
