package ir.exam.app.domain.model

data class Exam(
    val id: String,
    val title: String,
    val code: String,
    val durationMinutes: Int,
    val questions: List<Question>,
    val subject: String = "",
    val teacherMessage: String? = null,
    /** زمان محلی متناظر با expires_at سرور؛ null یعنی بدون محدودیت. */
    val deadlineEpochMs: Long? = null,
    val shuffleQuestions: Boolean = false,
    val shuffleOptions: Boolean = false,
    val attemptsAllowed: Int = 1,
    val attemptNumber: Int? = null,
    val attemptsRemaining: Int? = null,
    val questionPresentation: Map<String, QuestionPresentation> = emptyMap()
)

data class QuestionPresentation(
    val textAlign: String = "right",
    val imagePosition: String = "below",
    val fontFamily: String = "default",
    val fontSizeSp: Float = 16f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val answerLines: Int = 2,
    val answerLineStyle: String = "lined",
    val media: List<QuestionMediaPresentation> = emptyList()
)

data class QuestionMediaPresentation(
    val xMm: Float = 20f,
    val yMm: Float = 30f,
    val widthMm: Float = 55f
)

sealed interface Question {
    val id: String
    val text: String
    val score: Double
    val images: List<String>
    val maxAnswerImages: Int
    val answerImagesRequired: Boolean
    /** اندیس اصلی سرور؛ پاسخ‌های آرایه‌ای همیشه با این ترتیب ارسال می‌شوند. */
    val originalIndex: Int
}

data class EssayQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0
) : Question

data class MultipleChoiceQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    val options: List<String>,
    val optionImages: List<String?> = emptyList(),
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    /** هر گزینه نمایشی به کدام اندیس اصلی سرور اشاره می‌کند. */
    val optionOriginalIndices: List<Int> = options.indices.toList()
) : Question

data class TrueFalseQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0
) : Question

data class FillBlankQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0
) : Question

data class NumericQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0
) : Question

data class MatchingQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    val leftItems: List<String>,
    val rightItems: List<String>,
    val leftImages: List<String?> = emptyList(),
    val rightImages: List<String?> = emptyList(),
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    /** هر عضو نمایشی ستون راست به کدام اندیس اصلی کلید پاسخ اشاره می‌کند. */
    val rightOriginalIndices: List<Int> = rightItems.indices.toList()
) : Question

sealed interface StudentAnswer { val questionId: String }
data class TextAnswer(override val questionId: String, val value: String) : StudentAnswer
data class ChoiceAnswer(override val questionId: String, val selectedIndex: Int) : StudentAnswer
data class BooleanAnswer(override val questionId: String, val value: Boolean) : StudentAnswer
data class MatchingAnswer(override val questionId: String, val pairs: Map<Int, Int>) : StudentAnswer

data class StudentDraft(
    val answers: Map<String, StudentAnswer> = emptyMap(),
    val responseImages: Map<String, List<String>> = emptyMap(),
    val flaggedQuestionIds: Set<String> = emptySet(),
    val lastQuestionIndex: Int = 0
)

data class SubmittedExam(
    val examId: String,
    val answers: Map<String, StudentAnswer>,
    val responseImages: Map<String, List<String>>,
    val submittedAtEpochMs: Long
)

sealed interface SubmissionOutcome {
    data class Sent(val receipt: String? = null) : SubmissionOutcome
    data class Queued(val actionId: String) : SubmissionOutcome
}

data class PendingSubmissionStatus(
    val id: String,
    val examId: String,
    val state: String,
    val attempts: Int,
    val createdAt: Long,
    val lastError: String?
)
