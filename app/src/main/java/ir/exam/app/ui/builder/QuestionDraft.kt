package ir.exam.app.ui.builder

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class QuestionType { ESSAY, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, NUMERIC, MATCHING }

@Serializable
data class MediaDraft(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val xMm: Float = 20f,
    val yMm: Float = 30f,
    val widthMm: Float = 55f
)

@Serializable
data class QuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val type: QuestionType,
    val text: String = "",
    val score: Double = 1.0,
    val options: List<String> = emptyList(),
    val optionImages: List<String?> = emptyList(),
    val correctIndex: Int? = null,
    val expectedText: String = "",
    val expectedNumber: String = "",
    val tolerance: String = "0",
    val images: List<MediaDraft> = emptyList(),
    val rawPublic: JsonObject = JsonObject(emptyMap()),
    val rawAnswer: JsonObject = JsonObject(emptyMap())
)

data class AudienceClassOption(val id: String, val name: String)
data class AudienceStudentOption(val id: String, val name: String, val classNames: String? = null)

data class ExamBuilderState(
    val examId: String? = null,
    val code: String? = null,
    val loading: Boolean = false,
    val title: String = "",
    val subject: String = "",
    val durationMinutes: String = "",
    val questions: List<QuestionDraft> = emptyList(),
    val shuffleQuestions: Boolean = false,
    val shuffleOptions: Boolean = false,
    val negativeMarking: String = "0",
    val teacherMessage: String = "",
    val attemptsAllowed: Int = 1,
    val attemptOnTimeout: Boolean = false,
    val gradePolicy: String = "last",
    val attemptCooldown: String = "0",
    val audienceMode: String = "all",
    val audienceClasses: Set<String> = emptySet(),
    val audienceStudents: Set<String> = emptySet(),
    val availableClasses: List<AudienceClassOption> = emptyList(),
    val availableStudents: List<AudienceStudentOption> = emptyList(),
    val saving: Boolean = false,
    val uploadProgress: String? = null,
    val savedCode: String? = null,
    val error: String? = null
)
