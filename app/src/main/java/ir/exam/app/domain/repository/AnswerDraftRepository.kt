package ir.exam.app.domain.repository
import ir.exam.app.domain.model.StudentAnswer
import kotlinx.coroutines.flow.Flow
interface AnswerDraftRepository {
    fun observe(examId:String): Flow<Map<String, StudentAnswer>>
    suspend fun save(examId:String, answers:Map<String, StudentAnswer>)
    suspend fun clear(examId:String)
}
