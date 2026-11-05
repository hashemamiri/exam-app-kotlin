package ir.exam.app.ui.reports

import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.domain.model.ClassGradeRow
import ir.exam.app.domain.model.OfficialPrintHeader
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * V339 — دادهٔ موتور قالب‌های کارنامه (`assets/reportcard/reportcard.js`).
 *
 * همان قرارداد سایت (`site/src/reportcard.js`) تا خروجی PDF سایت و اپ یکسان باشد.
 * این فایل فقط داده می‌سازد و هیچ کار اندرویدی نمی‌کند؛ بنابراین با تست واحد JVM
 * بررسی می‌شود (بدون امولاتور).
 */
object ReportCardPayload {

    /** سال تحصیلی از تاریخ شمسی: مهر به بعد سال جاری، وگرنه سال قبل. */
    fun academicYear(today: JalaliDate = JalaliCalendar.today()): String {
        val start = if (today.month >= 7) today.year else today.year - 1
        return "سال تحصیلی " + PersianDigits.convert("$start–${start + 1}")
    }

    private fun schoolJson(header: OfficialPrintHeader?, fallbackName: String): JsonObject = buildJsonObject {
        put("name", (header?.school.orEmpty()).ifBlank { fallbackName }.ifBlank { "مدرسه" })
        put("province", header?.province.orEmpty())
        put("city", header?.city.orEmpty())
        put("district", header?.district.orEmpty())
    }

    private fun base(
        scope: String,
        className: String,
        header: OfficialPrintHeader?,
        teacher: String,
        schoolName: String,
        today: JalaliDate
    ): MutableMap<String, kotlinx.serialization.json.JsonElement> = mutableMapOf(
        "scope" to kotlinx.serialization.json.JsonPrimitive(scope),
        "school" to schoolJson(header, schoolName),
        "year" to kotlinx.serialization.json.JsonPrimitive(academicYear(today)),
        "date" to kotlinx.serialization.json.JsonPrimitive(today.display()),
        "className" to kotlinx.serialization.json.JsonPrimitive(className),
        "grade" to kotlinx.serialization.json.JsonPrimitive(header?.grade.orEmpty()),
        "field" to kotlinx.serialization.json.JsonPrimitive(header?.fieldOfStudy.orEmpty()),
        "teacher" to kotlinx.serialization.json.JsonPrimitive(teacher),
        "structure" to kotlinx.serialization.json.JsonPrimitive("combined"),
        "code" to kotlinx.serialization.json.JsonPrimitive(PersianDigits.convert((System.currentTimeMillis() % 1000000).toString()))
    )

    private fun obj(map: Map<String, kotlinx.serialization.json.JsonElement>): JsonObject = JsonObject(map)

    /** کارنامهٔ کلاس: ستون = آزمون، سطر = دانش‌آموز. */
    fun forClass(
        className: String,
        exams: List<ExamDashboardDto>,
        rows: List<ClassGradeRow>,
        header: OfficialPrintHeader? = null,
        teacher: String = "",
        schoolName: String = "",
        today: JalaliDate = JalaliCalendar.today()
    ): JsonObject {
        val fields = base("class", className, header, teacher, schoolName, today)
        fields["cols"] = buildJsonArray {
            exams.forEach { exam ->
                add(buildJsonObject {
                    put("key", exam.id)
                    put("title", exam.title.ifBlank { "آزمون" })
                    put("subject", exam.subject.orEmpty().ifBlank { exam.title.ifBlank { "آزمون" } })
                    put("max", maxOfExam(exam, rows))
                })
            }
        }
        val percents = rows.mapNotNull { it.averagePercent }
        fields["classInfo"] = buildJsonObject {
            put("count", rows.size)
            if (percents.isNotEmpty()) put("average", percents.average())
        }
        fields["rows"] = rowsJson(exams, rows)
        return obj(fields)
    }

    /** کارنامهٔ یک دانش‌آموز از همان دادهٔ کلاس (رتبه از کل کلاس حساب می‌شود). */
    fun forStudent(
        studentName: String,
        className: String,
        exams: List<ExamDashboardDto>,
        rows: List<ClassGradeRow>,
        header: OfficialPrintHeader? = null,
        teacher: String = "",
        schoolName: String = "",
        today: JalaliDate = JalaliCalendar.today()
    ): JsonObject {
        val fields = base("student", className, header, teacher, schoolName, today).toMutableMap()
        fields["scope"] = kotlinx.serialization.json.JsonPrimitive("student")
        fields["student"] = buildJsonObject { put("name", studentName) }
        fields["cols"] = buildJsonArray {
            exams.forEach { exam ->
                add(buildJsonObject {
                    put("key", exam.id)
                    put("title", exam.title.ifBlank { "آزمون" })
                    put("subject", exam.subject.orEmpty().ifBlank { exam.title.ifBlank { "آزمون" } })
                    put("max", maxOfExam(exam, rows))
                })
            }
        }
        val percents = rows.mapNotNull { it.averagePercent }
        fields["classInfo"] = buildJsonObject {
            put("count", rows.size)
            if (percents.isNotEmpty()) put("average", percents.average())
        }
        fields["rows"] = rowsJson(exams, rows)
        return obj(fields)
    }

    /** کارنامهٔ «نتایج من»: فقط نمره‌های خود دانش‌آموز (بدون دادهٔ کلاس). */
    fun forGrades(
        studentName: String,
        grades: List<StudentGradeCard>,
        header: OfficialPrintHeader? = null,
        className: String = "",
        today: JalaliDate = JalaliCalendar.today()
    ): JsonObject {
        val fields = base("student", className, header, "", "", today).toMutableMap()
        fields["student"] = buildJsonObject { put("name", studentName) }
        fields["cols"] = buildJsonArray {
            grades.forEachIndexed { index, g ->
                add(buildJsonObject {
                    put("key", "g$index")
                    put("title", g.title.ifBlank { "آزمون" })
                    put("subject", g.subject.ifBlank { g.title.ifBlank { "درس" } })
                    put("max", if (g.total > 0) g.total else 20.0)
                })
            }
        }
        fields["classInfo"] = buildJsonObject { put("count", 1) }
        fields["rows"] = buildJsonArray {
            add(buildJsonObject {
                put("name", studentName)
                putJsonObject("vals") { grades.forEachIndexed { index, g -> put("g$index", g.grade) } }
                putJsonObject("pcts") { grades.forEachIndexed { index, g -> put("g$index", g.percent) } }
            })
        }
        val feedback = grades.map { it.feedback }.filter { it.isNotBlank() }.take(3).joinToString(" · ")
        if (feedback.isNotBlank()) fields["feedback"] = kotlinx.serialization.json.JsonPrimitive(feedback)
        return obj(fields)
    }

    private fun rowsJson(exams: List<ExamDashboardDto>, rows: List<ClassGradeRow>) = buildJsonArray {
        rows.forEach { row ->
            add(buildJsonObject {
                put("name", row.studentName)
                putJsonObject("vals") {
                    exams.forEach { exam ->
                        put(exam.id, row.scores[exam.id])
                    }
                }
            })
        }
    }

    /** سقف نمرهٔ هر آزمون: بیشترین سقفِ ثبت‌شده در پاسخ‌ها (پیک تصادفی)، وگرنه بارم آزمون. */
    internal fun maxOfExam(exam: ExamDashboardDto, rows: List<ClassGradeRow>): Double {
        val fromRows = rows.mapNotNull { it.maxScores[exam.id] }.filter { it > 0 }.maxOrNull()
        return fromRows ?: if (exam.totalScore > 0) exam.totalScore else 20.0
    }
}
