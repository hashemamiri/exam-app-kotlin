package ir.exam.app.ui.reports

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.network.UserFacingError
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** V212 — بستهٔ حافظهٔ موقت صفحهٔ آمار */
private data class ReportsCache(val analytics: AnalyticsSummary, val exams: List<ExamDashboardDto>, val classes: List<SchoolClass>)
private const val CACHE_KEY = "reports.bundle"

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
        // V212 — دادهٔ قبلی فوراً نشان داده می‌شود؛ سه درخواست هم‌زمان (نه پشت سر هم) و نتیجه جایگزین می‌شود.
        val cached = SessionCache.get<ReportsCache>(CACHE_KEY)
        if (cached != null && state.value.exams.isEmpty()) applyLoaded(cached.analytics, cached.exams, cached.classes)
        _state.update { it.copy(loading = cached == null && it.exams.isEmpty(), error = null) }
        runCatching {
            coroutineScope {
                val analytics = async { grading.analytics().getOrThrow() }
                val exams = async { grading.getExams().getOrThrow() }
                val classes = async { school.getClasses().getOrThrow() }
                Triple(analytics.await(), exams.await(), classes.await())
            }
        }.onSuccess { (analytics, exams, classes) ->
            SessionCache.put(CACHE_KEY, ReportsCache(analytics, exams, classes))
            applyLoaded(analytics, exams, classes)
            // V208 — تحلیل کیفیت سؤال از صفحهٔ آمار حذف شد؛ بارگیری خودکار آن هم لازم نیست.
        }.onFailure { if (cached == null) fail(it) else _state.update { s -> s.copy(loading = false) } }
    }

    private fun applyLoaded(analytics: AnalyticsSummary, exams: List<ExamDashboardDto>, classes: List<SchoolClass>) {
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
    }

    fun loadQuestionAnalysis(examId: String) = viewModelScope.launch {
        _state.update {
            it.copy(selectedAnalysisExamId = examId, analysisLoading = true, questionAnalysis = null, error = null)
        }
        grading.questionAnalysis(examId)
            .onSuccess { analysis -> _state.update { it.copy(analysisLoading = false, questionAnalysis = analysis) } }
            .onFailure { error -> _state.update { it.copy(analysisLoading = false, error = UserFacingError.of(error, "تحلیل ناموفق بود.")) } }
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
            // دریافت پاسخ آزمون‌های مستقل به‌صورت موازی؛ نتیجه همچنان با همان
            // ترتیب selectedExams ساخته می‌شود.
            val answerMap = coroutineScope {
                selectedExams.map { exam ->
                    async {
                        exam.id to grading.getAnswers(exam.id).getOrThrow()
                            .asSequence()
                            .filter { it.graded }
                            .groupBy { it.studentId }
                    }
                }.associate { it.await() }
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
        _state.update { it.copy(loading = false, error = UserFacingError.of(error, "گزارش‌گیری ناموفق بود.")) }
    }
}
