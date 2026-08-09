package ir.exam.app.domain.repository
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.SubmittedExam
interface ExamRepository {
    suspend fun joinByCode(code:String): Result<Exam>
    suspend fun submitAttempt(attempt: SubmittedExam): Result<Unit>
}
