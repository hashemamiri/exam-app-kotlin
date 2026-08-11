package ir.exam.app.domain.model

data class Exam(
    val id: String,
    val title: String,
    val code: String,
    val durationMinutes: Int,
    val questions: List<Question>
)

sealed interface Question {
    val id: String
    val text: String
    val score: Double
    val images: List<String>
    val maxAnswerImages: Int
    val answerImagesRequired: Boolean
}

data class EssayQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false
) : Question

data class MultipleChoiceQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    val options: List<String>,
    val optionImages: List<String?> = emptyList(),
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false
) : Question

data class TrueFalseQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false
) : Question

data class FillBlankQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false
) : Question

data class NumericQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false
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
    override val answerImagesRequired: Boolean = false
) : Question

sealed interface StudentAnswer { val questionId: String }
data class TextAnswer(override val questionId: String, val value: String) : StudentAnswer
data class ChoiceAnswer(override val questionId: String, val selectedIndex: Int) : StudentAnswer
data class BooleanAnswer(override val questionId: String, val value: Boolean) : StudentAnswer
data class MatchingAnswer(override val questionId: String, val pairs: Map<Int, Int>) : StudentAnswer

data class StudentDraft(
    val answers: Map<String, StudentAnswer> = emptyMap(),
    val responseImages: Map<String, List<String>> = emptyMap()
)

data class SubmittedExam(
    val examId: String,
    val answers: Map<String, StudentAnswer>,
    val responseImages: Map<String, List<String>>,
    val submittedAtEpochMs: Long
)
