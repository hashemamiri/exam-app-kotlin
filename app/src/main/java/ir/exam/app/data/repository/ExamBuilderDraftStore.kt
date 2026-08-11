package ir.exam.app.data.repository

import ir.exam.app.data.local.ExamBuilderDraftDao
import ir.exam.app.data.local.ExamBuilderDraftEntity
import ir.exam.app.ui.builder.ExamBuilderState
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExamBuilderDraftPayload(
    val examId: String? = null,
    val title: String,
    val subject: String,
    val durationMinutes: String,
    val questions: List<QuestionDraft>,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val negativeMarking: String,
    val teacherMessage: String,
    val attemptsAllowed: Int,
    val attemptOnTimeout: Boolean,
    val gradePolicy: String,
    val attemptCooldown: String,
    val audienceMode: String,
    val audienceClasses: Set<String>,
    val audienceStudents: Set<String>,
    val savedAt: Long
)

class ExamBuilderDraftStore(private val dao: ExamBuilderDraftDao) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    suspend fun load(userId: String): ExamBuilderDraftPayload? = dao.get(userId)?.let { entity ->
        runCatching { json.decodeFromString<ExamBuilderDraftPayload>(entity.payloadJson) }.getOrNull()
    }

    suspend fun save(userId: String, state: ExamBuilderState) {
        if (state.title.isBlank() && state.questions.isEmpty()) {
            dao.delete(userId)
            return
        }
        val now = System.currentTimeMillis()
        val payload = ExamBuilderDraftPayload(
            examId = state.examId,
            title = state.title,
            subject = state.subject,
            durationMinutes = state.durationMinutes,
            questions = state.questions,
            shuffleQuestions = state.shuffleQuestions,
            shuffleOptions = state.shuffleOptions,
            negativeMarking = state.negativeMarking,
            teacherMessage = state.teacherMessage,
            attemptsAllowed = state.attemptsAllowed,
            attemptOnTimeout = state.attemptOnTimeout,
            gradePolicy = state.gradePolicy,
            attemptCooldown = state.attemptCooldown,
            audienceMode = state.audienceMode,
            audienceClasses = state.audienceClasses,
            audienceStudents = state.audienceStudents,
            savedAt = now
        )
        dao.upsert(
            ExamBuilderDraftEntity(
                ownerUserId = userId,
                examId = state.examId,
                payloadJson = json.encodeToString(payload),
                updatedAt = now
            )
        )
    }

    suspend fun clear(userId: String) = dao.delete(userId)
}
