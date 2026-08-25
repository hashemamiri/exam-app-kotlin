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
    val optionIds: List<String> = emptyList(),
    val optionImages: List<String?> = emptyList(),
    val correctIndex: Int? = null,
    val expectedText: String = "",
    val expectedNumber: String = "",
    val tolerance: String = "0",
    val caseSensitive: Boolean = false,
    val matchingLeft: List<String> = emptyList(),
    val matchingLeftIds: List<String> = emptyList(),
    val matchingRight: List<String> = emptyList(),
    val matchingRightIds: List<String> = emptyList(),
    val matchingPairs: Map<Int, Int> = emptyMap(),
    val matchingLeftImages: List<String?> = emptyList(),
    val matchingRightImages: List<String?> = emptyList(),
    val answerImageMode: String = "no",
    val maxAnswerImages: Int = 0,
    /** V58.0 — اجازهٔ رسم نمودار پاسخ توسط دانش‌آموز. */
    val allowAnswerGraph: Boolean = false,
    val images: List<MediaDraft> = emptyList(),
    val textAlign: String = "right",
    val imagePosition: String = "below",
    val fontFamily: String = "default",
    val fontSizeSp: Float = 16f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val answerLines: Int = 2,
    val answerLineStyle: String = "lined",
    val rawPublic: JsonObject = JsonObject(emptyMap()),
    val rawAnswer: JsonObject = JsonObject(emptyMap())
)

data class AudienceClassOption(val id: String, val name: String)
data class AudienceStudentOption(val id: String, val name: String, val classNames: String? = null)
data class BankCategoryOption(val id: Long, val name: String, val count: Int = 0)
data class BankQuestionOption(
    val id: Long,
    val subject: String?,
    val question: QuestionDraft,
    val categoryIds: Set<Long> = emptySet(),
    val categoryNames: List<String> = emptyList()
)

data class ExamBuilderState(
    val examId: String? = null,
    val code: String? = null,
    val loading: Boolean = false,
    val title: String = "",
    val subject: String = "",
    val durationMinutes: String = "",
    val opensAtIso: String? = null,
    val closesAtIso: String? = null,
    val questions: List<QuestionDraft> = emptyList(),
    val shuffleQuestions: Boolean = false,
    val shuffleOptions: Boolean = false,
    val negativeMarking: String = "",
    val teacherMessage: String = "",
    val attemptsAllowed: Int = 1,
    val attemptOnTimeout: Boolean = false,
    val gradePolicy: String = "last",
    val attemptCooldown: String = "",
    val audienceMode: String = "all",
    val audienceClasses: Set<String> = emptySet(),
    val audienceStudents: Set<String> = emptySet(),
    val availableClasses: List<AudienceClassOption> = emptyList(),
    val availableStudents: List<AudienceStudentOption> = emptyList(),
    val bankQuestions: List<BankQuestionOption> = emptyList(),
    val bankCategories: List<BankCategoryOption> = emptyList(),
    val bankQuery: String = "",
    val selectedBankCategory: Long? = null,
    val importedBy: String? = null,
    val recoverableDraft: ir.exam.app.data.repository.ExamBuilderDraftPayload? = null,
    val saving: Boolean = false,
    val bankLoading: Boolean = false,
    val uploadProgress: String? = null,
    val savedCode: String? = null,
    val chargedToman: Long = 0,
    val walletBalanceToman: Long? = null,
    val error: String? = null,
    /** V58.0 — پیام گذرای موفقیت (مثلاً «به بانک سؤال اضافه شد»). */
    val notice: String? = null
) {
    val maximumChargeToman: Long get() = questions.size * 1_000L
}

data class ExamSaveResult(
    val code: String,
    val chargedToman: Long,
    val walletBalanceToman: Long?
)

data class ExamImportDraft(
    val title: String,
    val subject: String,
    val durationMinutes: Int,
    val negativeMarking: Double,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val teacherMessage: String,
    val attemptsAllowed: Int,
    val attemptOnTimeout: Boolean,
    val gradePolicy: String,
    val attemptCooldown: Int,
    val questions: List<QuestionDraft>,
    val opensAtIso: String? = null,
    val closesAtIso: String? = null,
    val exportedBy: String? = null
)
