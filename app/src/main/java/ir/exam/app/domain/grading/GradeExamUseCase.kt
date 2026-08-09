package ir.exam.app.domain.grading
import ir.exam.app.domain.model.*
/** اول خودکار تصحیح می‌کند؛ سؤال تشریحی را با نمرهٔ صفر و نیازمند بررسی نگه می‌دارد. */
class GradeExamUseCase(private val graders:List<AutoGrader>){fun invoke(answerId:String,exam:Exam,answers:Map<String,StudentAnswer>):GradingResult=GradingResult(answerId,exam.questions.map{q->val a=answers[q.id];val g=graders.firstOrNull{it.canGrade(q)};if(a!=null&&g!=null)g.grade(q,a)else GradeItem(q.id,0.0,q.score,"نیازمند تصحیح دستی")})}
