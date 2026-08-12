package ir.exam.app.domain.model

import kotlinx.serialization.json.JsonElement

data class StudentAnswerSummary(
    val id: String,
    val examId: String,
    val title: String,
    val subject: String,
    val submittedAt: String?,
    val graded: Boolean,
    val totalGrade: Double,
    val totalScore: Double,
    val feedback: String
)

data class StudentAnswerReviewQuestion(
    val index: Int,
    val id: String,
    val type: String,
    val text: String,
    val score: Double,
    val options: List<String>,
    val leftItems: List<String>,
    val rightItems: List<String>,
    val response: JsonElement?,
    val responseImages: List<String>,
    val earnedScore: Double?,
    /** فقط RPC بعد از graded=true آن را تولید می‌کند. */
    val correctAnswer: String?,
    val explanation: String?
)

data class StudentAnswerReview(
    val id: String,
    val examId: String,
    val title: String,
    val subject: String,
    val graded: Boolean,
    val totalGrade: Double,
    val totalScore: Double,
    val feedback: String,
    val submittedAt: String?,
    val questions: List<StudentAnswerReviewQuestion>
)
