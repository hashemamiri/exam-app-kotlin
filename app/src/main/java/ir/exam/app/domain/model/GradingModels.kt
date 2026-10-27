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

data class GradingConstructionTarget(val label: String, val x: Double, val y: Double, val tolerance: Double, val score: Double)
data class GradingConstructionConstraint(
    val id: String = "",
    val type: String,
    val points: List<String>,
    val value: Double = 0.0,
    val tolerance: Double = 0.5,
    val score: Double = 1.0
)

data class GradingQuestion(
    val id: String,
    val type: String,
    val text: String,
    val score: Double,
    val options: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val rubric: String = "",
    val sectionTitle: String = "",
    val topic: String = "",
    val solutionSteps: List<String> = emptyList(),
    val assessmentParts: List<AssessmentPart> = emptyList(),
    val constructionPrompt: String = "",
    val constructionTargets: List<GradingConstructionTarget> = emptyList(),
    val constructionConstraints: List<GradingConstructionConstraint> = emptyList(),
    val constructionLabels: List<String> = emptyList(),
    val proofAssumptions: String = "",
    val proofGoal: String = "",
    val answerMode: String = ""
)

data class GradingExam(
    val id: String,
    val title: String,
    val subject: String?,
    val totalScore: Double,
    val questions: List<GradingQuestion>,
    val negativeMarking: Double = 0.0
) {
    fun minimumScore(index: Int): Double {
        val q = questions.getOrNull(index) ?: return 0.0
        return ir.exam.app.domain.grading.GradingScorePolicy.minimum(q.type, q.score, negativeMarking, q.assessmentParts.isNotEmpty())
    }
}

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
    val submittedAt: String?,
    // V255 — پیک تصادفی: شاخص سؤال‌های نمایش‌داده‌شده و سقف نمرهٔ همین پاسخ (null = همهٔ سؤال‌ها/بارم کل آزمون).
    val shownQuestions: Set<Int>? = null,
    val maxScore: Double? = null,
    val partGrades: Map<String, Double> = emptyMap(),
    val partFeedback: Map<String, String> = emptyMap()
) {
    fun isShown(index: Int): Boolean = shownQuestions == null || index in shownQuestions
}

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
    val averagePercent: Double?,
    // V277 — سقف نمرهٔ هر آزمون برای همین دانش‌آموز (پیک تصادفی: از خود پاسخ) تا کارنامهٔ تکی «نمره از سقف» نشان دهد
    val maxScores: Map<String, Double> = emptyMap()
)

data class QuestionAnalysisRow(
    val index: Int,
    val text: String,
    val maxScore: Double,
    val gradedCount: Int,
    val answeredCount: Int,
    val averagePercent: Double,
    val omitPercent: Double,
    val discrimination: Double?,
    val pointBiserial: Double?,
    val level: String
)

data class ExamQuestionAnalysis(
    val examId: String,
    val answerCount: Int,
    val cronbachAlpha: Double?,
    val questions: List<QuestionAnalysisRow>
)
