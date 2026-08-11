package ir.exam.app.domain.model

import kotlinx.serialization.json.JsonElement

data class GradeItem(
    val questionId: String,
    val earned: Double,
    val max: Double,
    val feedback: String? = null
)

data class GradingResult(val answerId: String, val items: List<GradeItem>) {
    val total: Double get() = items.sumOf(GradeItem::earned)
    val max: Double get() = items.sumOf(GradeItem::max)
}

data class StudentReport(
    val studentId: String,
    val studentName: String,
    val result: GradingResult,
    val percent: Double
)

data class GradingQuestion(
    val id: String,
    val type: String,
    val text: String,
    val score: Double,
    val options: List<String> = emptyList(),
    val images: List<String> = emptyList()
)

data class GradingExam(
    val id: String,
    val title: String,
    val subject: String?,
    val totalScore: Double,
    val questions: List<GradingQuestion>
)

data class GradingSubmission(
    val id: String,
    val examId: String,
    val studentId: String?,
    val studentName: String,
    val responses: List<JsonElement>,
    val responseImages: Map<String, List<String>>,
    val grades: List<Double>,
    val totalGrade: Double,
    val feedback: String,
    val graded: Boolean,
    val autoGraded: Boolean,
    val attemptNo: Int,
    val submittedAt: String?
)

data class FeedbackPhrase(val id: Long, val text: String)

data class AttendanceRow(
    val studentId: String,
    val fullName: String,
    val username: String? = null,
    val status: String,
    val totalGrade: Double? = null,
    val submittedAt: String? = null,
    val expiresAt: String? = null,
    val minutesLeft: Int? = null,
    val attempts: Int = 0,
    val attemptsAllowed: Int = 1,
    val abandoned: Int = 0
)

data class AnalyticsSummary(
    val examCount: Int,
    val answerCount: Int,
    val gradedCount: Int,
    val pendingCount: Int,
    val averagePercent: Double
)

data class ClassGradeRow(
    val studentId: String,
    val studentName: String,
    val scores: Map<String, Double?>,
    val averagePercent: Double?
)
