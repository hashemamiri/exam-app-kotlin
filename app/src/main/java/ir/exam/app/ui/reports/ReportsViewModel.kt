package ir.exam.app.ui.reports

import ir.exam.app.core.cache.ReportsSnapshot
import ir.exam.app.core.cache.ReportsSnapshotPayload
import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.cache.SnapshotClass
import ir.exam.app.core.cache.SnapshotExam
import ir.exam.app.core.cache.SnapshotRow
import ir.exam.app.core.network.UserFacingError
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.core.export.XlsxSheet
import ir.exam.app.core.export.XlsxWorkbook
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.repository.SupabaseGradingRepository
import ir.exam.app.data.repository.SupabaseSchoolRepository
import ir.exam.app.domain.model.AnalyticsSummary
import ir.exam.app.domain.model.ClassGradeRow
import ir.exam.app.domain.model.ExamQuestionAnalysis
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.ui.classes.TeacherSchoolItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** V212 — بستهٔ حافظهٔ موقت صفحهٔ آمار */
private data class ReportsCache(val analytics: AnalyticsSummary, val exams: List<ExamDashboardDto>, val classes: List<SchoolClass>)
private const val CACHE_KEY = "reports.bundle"

data class ReportsState(
    val loading: Boolean = true,
    val analytics: AnalyticsSummary? = null,
    val exams: List<ExamDashboardDto> = emptyList(),
    val classes: List<SchoolClass> = emptyList(),
    // V277 — فیلتر پلکانی کارنامه: مدرسه (اگر هست) → کلاس → دانش‌آموزان → آزمون‌ها → نمایش
    val schools: List<TeacherSchoolItem> = emptyList(),
    val selectedSchoolId: String? = null,
    val schoolClasses: List<SchoolClass> = emptyList(),
    val selectedClass: SchoolClass? = null,
    val roster: List<StudentProfile> = emptyList(),
    val rosterLoading: Boolean = false,
    val selectedStudentIds: Set<String> = emptySet(),
    val selectedExamIds: Set<String> = emptySet(),
    val rows: List<ClassGradeRow> = emptyList(),
    val rowsShown: Boolean = false,
    val selectedAnalysisExamId: String? = null,
    val questionAnalysis: ExamQuestionAnalysis? = null,
    val analysisLoading: Boolean = false,
    val error: String? = null,
    /** V321 — داده از عکس فوریِ روی‌دیسک آمده است (بدون شبکه). */
    val offline: Boolean = false,
    /** زمان ذخیرهٔ عکس فوری (میلی‌ثانیهٔ یونیکس) برای نمایش «آخرین به‌روزرسانی». */
    val offlineSavedAt: Long? = null
) {
    /** کلاس‌های قابل انتخاب: کلاس‌های مدرسهٔ انتخاب‌شده یا همهٔ کلاس‌های معلم. */
    val classOptions: List<SchoolClass> get() = if (selectedSchoolId != null) schoolClasses else classes
}

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
        // V321 — اگر رم خالی است، آخرین وضعیتِ ذخیره‌شده روی دیسک نشان داده می‌شود (باز شدن آفلاین)
        if (cached == null && state.value.exams.isEmpty()) applyOfflineSnapshot()
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
            saveSnapshot(exams, classes)
            // V208 — تحلیل کیفیت سؤال از صفحهٔ آمار حذف شد؛ بارگیری خودکار آن هم لازم نیست.
        }.onFailure {
            // V321 — شبکه نبود: اگر عکس فوری داریم، صفحه خالی نشود.
            if (cached == null && state.value.exams.isEmpty() && applyOfflineSnapshot()) {
                _state.update { s -> s.copy(loading = false) }
            } else if (cached == null) fail(it) else _state.update { s -> s.copy(loading = false) }
        }
        // V277 — مدرسه‌ها (اگر معلم عضو مدرسه‌ای نباشد، مرحلهٔ «مدرسه» نمایش داده نمی‌شود)
        loadSchools()
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

    /** V277 — همان RPC نمای «مدارس» (native_teacher_schools_v61)؛ خطا = بدون مدرسه. */
    private suspend fun loadSchools() {
        val list = runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_teacher_schools_v61").decodeAs<JsonObject>()
            raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
            (raw["items"] as? JsonArray).orEmpty().mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                TeacherSchoolItem(
                    id = id,
                    name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    province = obj["province"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    city = obj["city"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    classCount = obj["class_count"]?.jsonPrimitive?.intOrNull ?: 0
                )
            }
        }.getOrDefault(emptyList())
        _state.update { it.copy(schools = list) }
    }

    fun loadQuestionAnalysis(examId: String) = viewModelScope.launch {
        _state.update {
            it.copy(selectedAnalysisExamId = examId, analysisLoading = true, questionAnalysis = null, error = null)
        }
        grading.questionAnalysis(examId)
            .onSuccess { analysis -> _state.update { it.copy(analysisLoading = false, questionAnalysis = analysis) } }
            .onFailure { error -> _state.update { it.copy(analysisLoading = false, error = UserFacingError.of(error, "تحلیل ناموفق بود.")) } }
    }

    /** V277 — انتخاب مدرسه (null = همهٔ کلاس‌های من)؛ کلاس/دانش‌آموز/نتیجه پاک می‌شود. */
    fun selectSchool(schoolId: String?) = viewModelScope.launch {
        _state.update { it.copy(selectedSchoolId = schoolId, schoolClasses = emptyList(), selectedClass = null, roster = emptyList(), selectedStudentIds = emptySet(), rows = emptyList(), rowsShown = false, error = null) }
        if (schoolId == null) return@launch
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "native_teacher_school_classes_v61",
                buildJsonObject { put("p_school", JsonPrimitive(schoolId)) }
            ).decodeList<SchoolClassDto>().map(SchoolClassDto::toDomain)
        }.onSuccess { list -> _state.update { it.copy(schoolClasses = list) } }
            .onFailure(::fail)
    }

    fun selectClass(item: SchoolClass) = viewModelScope.launch {
        _state.update { it.copy(selectedClass = item, roster = emptyList(), selectedStudentIds = emptySet(), rows = emptyList(), rowsShown = false, rosterLoading = true, error = null) }
        school.getClassRoster(item.id)
            .onSuccess { roster -> _state.update { it.copy(rosterLoading = false, roster = roster, selectedStudentIds = roster.mapTo(linkedSetOf(), StudentProfile::id)) } }
            .onFailure { e -> _state.update { it.copy(rosterLoading = false) }; fail(e) }
    }

    fun toggleStudent(id: String) = _state.update { old ->
        old.copy(selectedStudentIds = if (id in old.selectedStudentIds) old.selectedStudentIds - id else old.selectedStudentIds + id, rowsShown = false)
    }

    fun setAllStudents(all: Boolean) = _state.update { old ->
        old.copy(selectedStudentIds = if (all) old.roster.mapTo(linkedSetOf(), StudentProfile::id) else emptySet(), rowsShown = false)
    }

    /** V279 — نتیجهٔ پنجره‌های انتخاب. */
    fun setStudents(ids: Set<String>) = _state.update { old -> old.copy(selectedStudentIds = ids.filterTo(linkedSetOf()) { id -> old.roster.any { it.id == id } }, rowsShown = false) }
    fun setExams(ids: Set<String>) = _state.update { old -> old.copy(selectedExamIds = ids.filterTo(linkedSetOf()) { id -> old.exams.any { it.id == id } }, rowsShown = false) }

    fun toggleExam(id: String) = _state.update { old ->
        val selected = if (id in old.selectedExamIds) old.selectedExamIds - id else old.selectedExamIds + id
        old.copy(selectedExamIds = selected, rowsShown = false)
    }

    fun setAllExams(all: Boolean) = _state.update { old ->
        old.copy(selectedExamIds = if (all) old.exams.mapTo(linkedSetOf(), ExamDashboardDto::id) else emptySet(), rowsShown = false)
    }

    /** V277 — دکمهٔ «نمایش کارنامه‌ها»: فقط دانش‌آموزان و آزمون‌های انتخاب‌شده. */
    fun showReports() = computeRows()

    private fun computeRows() = viewModelScope.launch {
        val snapshot = state.value
        if (snapshot.selectedExamIds.isEmpty()) return@launch
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            val selectedExams = snapshot.exams.filter { it.id in snapshot.selectedExamIds }
            // دریافت پاسخ آزمون‌های مستقل به‌صورت موازی؛ نتیجه همچنان با همان
            // ترتیب selectedExams ساخته می‌شود.
            val answerMap = coroutineScope {
                selectedExams.map { exam ->
                    async {
                        exam.id to grading.getAnswers(exam.id).getOrThrow()
                            .asSequence()
                            .filter { it.graded }
                            .groupBy { it.studentId ?: "answer:${it.id}" }
                    }
                }.associate { it.await() }
            }
            // V335.38 — no fabricated class or profile lookup; names come from owned answers.
            val roster = if (snapshot.selectedClass != null) {
                snapshot.roster.filter { it.id in snapshot.selectedStudentIds }
            } else {
                answerMap.values.flatMap { it.entries }.distinctBy { it.key }.map { (id, attempts) ->
                    StudentProfile(id = id, fullName = attempts.first().studentName.ifBlank { "دانش‌آموز" })
                }
            }
            roster.map { student ->
                val scores = selectedExams.associate { exam ->
                    val attempts = answerMap[exam.id]?.get(student.id).orEmpty()
                    exam.id to attempts.maxOfOrNull { it.totalGrade }
                }
                val maxScores = selectedExams.associate { exam ->
                    // V255 — پیک تصادفی: سقف نمره از meta.max_score همان پاسخ.
                    val attempts = answerMap[exam.id]?.get(student.id).orEmpty()
                    exam.id to (attempts.maxByOrNull { it.totalGrade }?.maxScore ?: exam.totalScore)
                }
                val percentages = selectedExams.mapNotNull { exam ->
                    val score = scores[exam.id] ?: return@mapNotNull null
                    val max = maxScores[exam.id] ?: 0.0
                    if (max > 0) score * 100.0 / max else null
                }
                ClassGradeRow(
                    studentId = student.id,
                    studentName = student.fullName,
                    scores = scores,
                    averagePercent = percentages.average().takeUnless(Double::isNaN),
                    maxScores = maxScores
                )
            }
        }.onSuccess { rows ->
            _state.update { it.copy(loading = false, rows = rows, rowsShown = true) }
            val exams = state.value.exams
            if (exams.isNotEmpty()) saveSnapshot(exams, state.value.classes, rows)
        }.onFailure(::fail)
    }

    /** V321 — کلید کاربر برای فایل عکس فوری (خروج از حساب = فایل دیگر). */
    private fun userKey(): String = runCatching {
        SupabaseProvider.client.auth.currentUserOrNull()?.id
    }.getOrNull()?.takeIf(String::isNotBlank) ?: "guest"

    /** V321 — نوشتن عکس فوریِ کارنامه روی دیسک (فقط عنوان/نام/درصد). */
    private fun saveSnapshot(
        exams: List<ExamDashboardDto>,
        classes: List<SchoolClass>,
        rows: List<ClassGradeRow> = state.value.rows
    ) {
        val payload = ReportsSnapshotPayload(
            savedAtMillis = System.currentTimeMillis(),
            exams = exams.map { SnapshotExam(it.id, it.title, it.subject.orEmpty()) },
            classes = classes.map { SnapshotClass(it.id, it.name) },
            rows = rows.map { row ->
                SnapshotRow(
                    studentId = row.studentId,
                    name = row.studentName,
                    scores = row.scores,
                    maxScores = row.maxScores
                )
            }
        )
        ReportsSnapshot.write(userKey(), payload)
    }

    /**
     * V321 — خواندن عکس فوری. اگر داده‌ای باشد، آزمون‌ها/کلاس‌ها و سطرهای آخر نمایش داده می‌شوند
     * و پرچم `offline` روشن می‌شود. خروجی: آیا چیزی بازیابی شد؟
     */
    private fun applyOfflineSnapshot(): Boolean {
        val snap = ReportsSnapshot.read(userKey()) ?: return false
        val exams = snap.exams.map { ExamDashboardDto(id = it.id, title = it.title, subject = it.subject.takeIf(String::isNotBlank)) }
        val classes = snap.classes.map { SchoolClass(id = it.id, name = it.name) }
        val rows = snap.rows.map { row ->
            val percentages = row.scores.mapNotNull { (examId, score) ->
                val max = row.maxScores[examId] ?: 0.0
                if (score != null && max > 0) score * 100.0 / max else null
            }
            ClassGradeRow(
                studentId = row.studentId,
                studentName = row.name,
                scores = row.scores,
                averagePercent = percentages.average().takeUnless(Double::isNaN),
                maxScores = row.maxScores
            )
        }
        _state.update {
            it.copy(
                loading = false,
                offline = true,
                offlineSavedAt = snap.savedAtMillis.takeIf { at -> at > 0 },
                exams = if (it.exams.isEmpty()) exams else it.exams,
                classes = if (it.classes.isEmpty()) classes else it.classes,
                rows = if (it.rows.isEmpty()) rows else it.rows,
                rowsShown = it.rowsShown || rows.isNotEmpty()
            )
        }
        return true
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
