package ir.exam.app.domain.repository

import ir.exam.app.domain.model.StudentDraft
import kotlinx.coroutines.flow.Flow

interface AnswerDraftRepository {
    fun observe(examId: String): Flow<StudentDraft>
    suspend fun load(examId: String): StudentDraft
    suspend fun save(examId: String, draft: StudentDraft)
    suspend fun clear(examId: String)
}
