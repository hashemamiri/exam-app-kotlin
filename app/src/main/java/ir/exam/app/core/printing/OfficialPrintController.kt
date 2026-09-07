package ir.exam.app.core.printing

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import ir.exam.app.domain.model.OfficialGradeReportPrintable

/** چاپ رسمی PDF برای کارنامه؛ آزمون‌ها فقط از NativeExamPrintLauncher می‌گذرند. */
class OfficialPrintController(context: Context) {
    private val appContext = context.applicationContext

    fun printReport(context: Context, report: OfficialGradeReportPrintable) {
        print(context, report, "report-${report.documentTitle}")
    }

    private fun print(context: Context, printable: ir.exam.app.domain.model.OfficialPrintable, jobName: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        manager.print(
            jobName.take(80),
            OfficialPdfPrintAdapter(appContext, printable),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
    }
}
