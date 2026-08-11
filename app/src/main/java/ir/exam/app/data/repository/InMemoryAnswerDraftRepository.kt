package ir.exam.app.data.repository

import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.repository.AnswerDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryAnswerDraftRepository : AnswerDraftRepository {
    private val data = MutableStateFlow<Map<String, StudentDraft>>(emptyMap())
    override fun observe(examId: String): Flow<StudentDraft> = data.map { it[examId] ?: StudentDraft() }
    override suspend fun save(examId: String, draft: StudentDraft) { data.value = data.value + (examId to draft) }
    override suspend fun clear(examId: String) { data.value = data.value - examId }
}
