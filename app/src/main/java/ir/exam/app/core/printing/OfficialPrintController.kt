package ir.exam.app.core.printing

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import ir.exam.app.domain.model.OfficialGradeReportPrintable

/**
 * V120 — این کنترلر قبلاً `printExam(OfficialExamPrintable)` هم داشت (چاپِ
 * مستقیمِ آزمون با موتورِ PDF بومی، مستقل از موتورِ HTML/WebView). آن تابع
 * هیچ فراخوان‌کننده‌ای در برنامه نداشت (مسیرِ زندهٔ چاپِ آزمون از «مرکز چاپ»
 * → `PrintExamStore` → موتورِ HTML/WebView است) و حذف شد تا دو موتورِ رندرِ
 * ناهم‌خوان برای یک محصول (برگهٔ چاپیِ آزمون) در کد باقی نماند. کارنامه
 * (`printReport`) تنها مصرف‌کنندهٔ زندهٔ این کلاس است.
 */
class OfficialPrintController(context: Context) {

    fun printReport(context: Context, report: OfficialGradeReportPrintable) {
        print(context, report, "report-${report.documentTitle}")
    }

    private fun print(context: Context, printable: ir.exam.app.domain.model.OfficialPrintable, jobName: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        manager.print(
            jobName.take(80),
            OfficialPdfPrintAdapter(context, printable),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        )
    }
}
