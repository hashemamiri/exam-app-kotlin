package ir.exam.app.data.repository
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.repository.AnswerDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryAnswerDraftRepository : AnswerDraftRepository {
    private val data = MutableStateFlow<Map<String, Map<String, StudentAnswer>>>(emptyMap())
    override fun observe(examId: String): Flow<Map<String, StudentAnswer>> = data.map { it[examId].orEmpty() }
    override suspend fun save(examId: String, answers: Map<String, StudentAnswer>) { data.value = data.value + (examId to answers) }
    override suspend fun clear(examId: String) { data.value = data.value - examId }
}
