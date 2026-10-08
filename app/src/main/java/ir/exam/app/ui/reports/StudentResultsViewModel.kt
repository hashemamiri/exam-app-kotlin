package ir.exam.app.ui.reports

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.network.UserFacingError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseGradingRepository
import ir.exam.app.domain.model.StudentAnswerReview
import ir.exam.app.domain.model.StudentAnswerSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class StudentGradeCard(
    val examId: String,
    val title: String,
    val subject: String,
    val grade: Double,
    val total: Double,
    val feedback: String,
    val submittedAt: String?
) {
    val percent: Double get() = if (total > 0) grade * 100.0 / total else 0.0
}

data class StudentResultsState(
    val loading: Boolean = true,
    val detailLoading: Boolean = false,
    val grades: List<StudentGradeCard> = emptyList(),
    val answers: List<StudentAnswerSummary> = emptyList(),
    val selectedAnswer: StudentAnswerReview? = null,
    val error: String? = null
)

/** V212 — بستهٔ حافظهٔ موقت نتایج دانش‌آموز */
private data class StudentResultsCache(val grades: List<StudentGradeCard>, val answers: List<StudentAnswerSummary>)
private const val CACHE_KEY = "student.results"

class StudentResultsViewModel(
    private val repository: SupabaseGradingRepository = SupabaseGradingRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(StudentResultsState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        // V212 — نتایج قبلی فوراً نشان داده می‌شوند؛ دو درخواست هم‌زمان و نتیجهٔ تازه جایگزین می‌شود.
        val cached = SessionCache.get<StudentResultsCache>(CACHE_KEY)
        if (cached != null) _state.value = StudentResultsState(loading = false, grades = cached.grades, answers = cached.answers)
        else _state.update { it.copy(loading = true, error = null) }
        runCatching {
            coroutineScope {
                val grades = async { repository.myGrades().getOrThrow().mapNotNull(::parseGrade) }
                val answers = async { repository.myAnswerSummaries().getOrThrow() }
                grades.await() to answers.await()
            }
        }.onSuccess { (grades, answers) ->
            SessionCache.put(CACHE_KEY, StudentResultsCache(grades, answers))
            _state.value = StudentResultsState(loading = false, grades = grades, answers = answers)
        }.onFailure { error ->
            _state.update { it.copy(loading = false, error = if (cached == null) safeResultError(error) else null) }
        }
    }

    fun openAnswer(answerId: String) = viewModelScope.launch {
        if (state.value.detailLoading) return@launch
        _state.update { it.copy(detailLoading = true, selectedAnswer = null, error = null) }
        repository.myAnswerDetail(answerId)
            .onSuccess { detail -> _state.update { it.copy(detailLoading = false, selectedAnswer = detail) } }
            .onFailure { error ->
                _state.update { it.copy(detailLoading = false, error = safeResultError(error)) }
            }
    }

    fun closeAnswer() {
        _state.update { it.copy(selectedAnswer = null) }
    }

    private fun parseGrade(raw: JsonObject): StudentGradeCard? {
        val examId = raw.text("exam_id") ?: raw.text("id") ?: return null
        return StudentGradeCard(
            examId = examId,
            title = raw.text("title") ?: "آزمون",
            subject = raw.text("subject").orEmpty(),
            grade = raw.number("total_grade") ?: 0.0,
            total = raw.number("total_score") ?: 0.0,
            feedback = raw.text("feedback").orEmpty(),
            submittedAt = raw.text("graded_at") ?: raw.text("submitted_at")
        )
    }
}

private fun safeResultError(error: Throwable): String = UserFacingError.of(error, "دریافت نتایج ناموفق بود.") // V209 — بدون متن فنی

private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
