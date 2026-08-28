package ir.exam.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.core.export.XlsxSheet
import ir.exam.app.core.export.XlsxWorkbook
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.SupabaseGradingRepository
import ir.exam.app.data.repository.SupabaseSchoolRepository
import ir.exam.app.domain.model.AnalyticsSummary
import ir.exam.app.domain.model.ClassGradeRow
import ir.exam.app.domain.model.ExamQuestionAnalysis
import ir.exam.app.domain.model.SchoolClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsState(
    val loading: Boolean = true,
    val analytics: AnalyticsSummary? = null,
    val exams: List<ExamDashboardDto> = emptyList(),
    val classes: List<SchoolClass> = emptyList(),
    val selectedClass: SchoolClass? = null,
    val selectedExamIds: Set<String> = emptySet(),
    val rows: List<ClassGradeRow> = emptyList(),
    val selectedAnalysisExamId: String? = null,
    val questionAnalysis: ExamQuestionAnalysis? = null,
    val analysisLoading: Boolean = false,
    val error: String? = null
)

class ReportsViewModel(
    private val grading: SupabaseGradingRepository = SupabaseGradingRepository(),
    private val school: SupabaseSchoolRepository = SupabaseSchoolRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ReportsState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val analytics = grading.analytics().getOrThrow()
            val exams = grading.getExams().getOrThrow()
            val classes = school.getClasses().getOrThrow()
            Triple(analytics, exams, classes)
        }.onSuccess { (analytics, exams, classes) ->
            _state.update {
                it.copy(
                    loading = false,
                    analytics = analytics,
                    exams = exams,
                    classes = classes,
                    selectedExamIds = exams.mapTo(linkedSetOf(), ExamDashboardDto::id),
                    selectedAnalysisExamId = exams.firstOrNull()?.id
                )
            }
            exams.firstOrNull()?.let { loadQuestionAnalysis(it.id) }
        }.onFailure(::fail)
    }

    fun loadQuestionAnalysis(examId: String) = viewModelScope.launch {
        _state.update {
            it.copy(selectedAnalysisExamId = examId, analysisLoading = true, questionAnalysis = null, error = null)
        }
        grading.questionAnalysis(examId)
            .onSuccess { analysis -> _state.update { it.copy(analysisLoading = false, questionAnalysis = analysis) } }
            .onFailure { error -> _state.update { it.copy(analysisLoading = false, error = error.message?.take(240)) } }
    }

    fun selectClass(item: SchoolClass) {
        _state.update { it.copy(selectedClass = item) }
        computeRows()
    }

    fun toggleExam(id: String) {
        _state.update { old ->
            val selected = if (id in old.selectedExamIds) old.selectedExamIds - id else old.selectedExamIds + id
            old.copy(selectedExamIds = selected)
        }
        if (state.value.selectedClass != null) computeRows()
    }

    private fun computeRows() = viewModelScope.launch {
        val selectedClass = state.value.selectedClass ?: return@launch
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val roster = school.getClassRoster(selectedClass.id).getOrThrow()
            val selectedExams = state.value.exams.filter { it.id in state.value.selectedExamIds }
            val answerMap = selectedExams.associate { exam ->
                exam.id to grading.getAnswers(exam.id).getOrThrow()
                    .asSequence()
                    .filter { it.graded }
                    .groupBy { it.studentId }
            }
            roster.map { student ->
                val scores = selectedExams.associate { exam ->
                    val attempts = answerMap[exam.id]?.get(student.id).orEmpty()
                    exam.id to attempts.maxOfOrNull { it.totalGrade }
                }
                val percentages = selectedExams.mapNotNull { exam ->
                    val score = scores[exam.id] ?: return@mapNotNull null
                    if (exam.totalScore > 0) score * 100.0 / exam.totalScore else null
                }
                ClassGradeRow(
                    studentId = student.id,
                    studentName = student.fullName,
                    scores = scores,
                    averagePercent = percentages.average().takeUnless(Double::isNaN)
                )
            }
        }.onSuccess { rows -> _state.update { it.copy(loading = false, rows = rows) } }
            .onFailure(::fail)
    }

    fun csv(): String {
        val selectedExams = state.value.exams.filter { it.id in state.value.selectedExamIds }
        val header = listOf("نام دانش‌آموز") + selectedExams.map { it.title } + "میانگین درصد"
        val lines = mutableListOf(header)
        state.value.rows.forEach { row ->
            lines += listOf(row.studentName) + selectedExams.map { exam ->
                row.scores[exam.id]?.toString().orEmpty()
            } + (row.averagePercent?.let { "%.2f".format(it) }.orEmpty())
        }
        return "\uFEFF" + lines.joinToString("\n") { row -> row.joinToString(",") { csvCell(it) } }
    }

    fun xlsx():ByteArray {
        val selected=state.value.exams.filter{it.id in state.value.selectedExamIds}
        val rows=mutableListOf<List<Any?>>(listOf("نام دانش‌آموز")+selected.map{it.title}+"میانگین درصد")
        state.value.rows.forEach { row -> rows+=listOf(row.studentName)+selected.map{row.scores[it.id]}+row.averagePercent }
        val summary=listOf(listOf("شاخص","مقدار"),listOf("تعداد آزمون",state.value.analytics?.examCount?:0),listOf("تعداد پاسخ",state.value.analytics?.answerCount?:0),listOf("میانگین درصد",state.value.analytics?.averagePercent?:0.0))
        return XlsxWorkbook.build(listOf(XlsxSheet("لیست نمرات",rows),XlsxSheet("خلاصه",summary)))
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    fun reportError(error: Throwable) = fail(error)

    private fun fail(error: Throwable) {
        _state.update { it.copy(loading = false, error = error.message?.take(240) ?: "گزارش‌گیری ناموفق بود.") }
    }
}
