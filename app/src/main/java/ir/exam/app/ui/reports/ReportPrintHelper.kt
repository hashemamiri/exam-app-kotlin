package ir.exam.app.ui.reports

import android.content.Context
import ir.exam.app.core.printing.OfficialPrintController
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.repository.SupabaseProfileRepository
import ir.exam.app.domain.model.ClassGradeRow
import ir.exam.app.domain.model.OfficialGradeReportPrintable
import ir.exam.app.domain.model.OfficialGradeRow
import ir.exam.app.domain.model.OfficialPrintHeader

object ReportPrintHelper {
    suspend fun print(
        context: Context,
        title: String,
        exams: List<ExamDashboardDto>,
        rows: List<ClassGradeRow>
    ) {
        val profile = SupabaseProfileRepository(context.applicationContext).load().getOrThrow()
        val report = OfficialGradeReportPrintable(
            documentTitle = title,
            header = OfficialPrintHeader(
                province = profile.header.province,
                city = profile.header.city,
                district = profile.header.district,
                school = profile.header.school,
                grade = profile.header.grade
            ),
            examTitles = exams.map { it.title },
            rows = rows.map { row ->
                OfficialGradeRow(
                    studentName = row.studentName,
                    scoreLines = exams.map { exam ->
                        "${exam.title}: ${row.scores[exam.id]?.let { score -> "%.2f".format(score) } ?: "غایب"}"
                    },
                    averagePercent = row.averagePercent
                )
            }
        )
        OfficialPrintController(context.applicationContext).printReport(context, report)
    }
}
