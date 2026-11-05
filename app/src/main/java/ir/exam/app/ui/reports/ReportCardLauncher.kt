package ir.exam.app.ui.reports

import android.content.Context
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.ManagerClassReport
import ir.exam.app.data.repository.ManagerClassStats
import ir.exam.app.data.repository.SupabaseProfileRepository
import ir.exam.app.domain.model.ClassGradeRow
import ir.exam.app.domain.model.ExamHeader
import ir.exam.app.domain.model.OfficialPrintHeader
import kotlinx.serialization.json.JsonObject

/**
 * V339 — سربرگ مدرسه را از پروفایل می‌خواند و دادهٔ موتور قالب‌ها را می‌سازد.
 * اگر پروفایل در دسترس نباشد (آفلاین) کارنامه با سربرگ خالی ساخته می‌شود،
 * نه اینکه خروجی کلاً از کار بیفتد.
 */
object ReportCardLauncher {

    private suspend fun header(context: Context): Pair<OfficialPrintHeader, String> {
        val profile = runCatching {
            SupabaseProfileRepository(context.applicationContext).load().getOrNull()
        }.getOrNull()
        val h: ExamHeader = profile?.header ?: ExamHeader()
        return OfficialPrintHeader(
            province = h.province,
            city = h.city,
            district = h.district,
            school = h.school,
            grade = h.grade,
            fieldOfStudy = h.fieldOfStudy
        ) to (profile?.shownName ?: "")
    }

    suspend fun forClass(
        context: Context,
        className: String,
        exams: List<ExamDashboardDto>,
        rows: List<ClassGradeRow>
    ): JsonObject {
        val (h, teacher) = header(context)
        return ReportCardPayload.forClass(className, exams, rows, h, teacher, h.school)
    }

    suspend fun forStudent(
        context: Context,
        studentName: String,
        className: String,
        exams: List<ExamDashboardDto>,
        rows: List<ClassGradeRow>
    ): JsonObject {
        val (h, teacher) = header(context)
        return ReportCardPayload.forStudent(studentName, className, exams, rows, h, teacher, h.school)
    }

    suspend fun forGrades(context: Context, studentName: String, grades: List<StudentGradeCard>): JsonObject {
        val (h, shownName) = header(context)
        val name = studentName.ifBlank { shownName }.ifBlank { "دانش‌آموز" }
        return ReportCardPayload.forGrades(name, grades, h)
    }

    /* ---- V340 — کارنامهٔ مدیر: کلاس، دانش‌آموز، دفترچهٔ مدرسه ---- */

    suspend fun forManagerClass(
        context: Context,
        report: ManagerClassReport,
        teacher: String = "",
        schoolName: String = ""
    ): JsonObject {
        val (h, _) = header(context)
        return ReportCardPayload.forManagerClass(report, h, teacher, schoolName.ifBlank { h.school })
    }

    suspend fun forManagerStudent(
        context: Context,
        report: ManagerClassReport,
        studentName: String,
        schoolName: String = ""
    ): JsonObject {
        val (h, _) = header(context)
        return ReportCardPayload.forManagerStudent(report, studentName, h, schoolName.ifBlank { h.school })
    }

    suspend fun forManagerSchool(
        context: Context,
        stats: ManagerClassStats,
        reports: List<JsonObject>
    ): JsonObject {
        val (h, _) = header(context)
        return ReportCardPayload.forManagerSchool(stats, reports, h)
    }
}
