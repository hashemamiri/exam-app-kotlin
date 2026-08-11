package ir.exam.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseGradingRepository
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
    val grades: List<StudentGradeCard> = emptyList(),
    val answers: List<JsonObject> = emptyList(),
    val error: String? = null
)

class StudentResultsViewModel(
    private val repository: SupabaseGradingRepository = SupabaseGradingRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(StudentResultsState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val grades = repository.myGrades().getOrThrow().mapNotNull(::parseGrade)
            val answers = repository.myAnswers().getOrThrow()
            grades to answers
        }.onSuccess { (grades, answers) ->
            _state.value = StudentResultsState(loading = false, grades = grades, answers = answers)
        }.onFailure { error ->
            _state.update { it.copy(loading = false, error = error.message?.take(240) ?: "دریافت نتایج ناموفق بود.") }
        }
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

private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
