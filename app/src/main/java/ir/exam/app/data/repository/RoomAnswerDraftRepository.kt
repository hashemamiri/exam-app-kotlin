package ir.exam.app.data.repository

import ir.exam.app.data.local.AnswerDraftDao
import ir.exam.app.data.local.AnswerDraftEntity
import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.repository.AnswerDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAnswerDraftRepository(private val dao: AnswerDraftDao) : AnswerDraftRepository {
    override fun observe(examId: String): Flow<StudentDraft> =
        dao.observe(examId).map { entity ->
            entity?.answersJson?.let(StudentDraftJsonCodec::decode) ?: StudentDraft()
        }

    override suspend fun load(examId: String): StudentDraft =
        dao.find(examId)?.answersJson?.let(StudentDraftJsonCodec::decode) ?: StudentDraft()

    override suspend fun save(examId: String, draft: StudentDraft) {
        dao.upsert(
            AnswerDraftEntity(
                examId = examId,
                answersJson = StudentDraftJsonCodec.encode(draft),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clear(examId: String) = dao.delete(examId)
}
