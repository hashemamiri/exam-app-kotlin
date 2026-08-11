package ir.exam.app.ui.reports

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.domain.model.ClassGradeRow

object ReportPrintHelper {
    fun print(
        context: Context,
        title: String,
        exams: List<ExamDashboardDto>,
        rows: List<ClassGradeRow>
    ) {
        val html = buildString {
            append("<html dir='rtl'><head><meta charset='utf-8'><style>")
            append("body{font-family:sans-serif;padding:20px}h1{text-align:center}table{border-collapse:collapse;width:100%}th,td{border:1px solid #333;padding:6px;text-align:center}th{background:#eee}")
            append("</style></head><body><h1>${title.escape()}</h1><table><tr><th>دانش‌آموز</th>")
            exams.forEach { append("<th>${it.title.escape()}</th>") }
            append("<th>میانگین٪</th></tr>")
            rows.forEach { row ->
                append("<tr><td>${row.studentName.escape()}</td>")
                exams.forEach { exam -> append("<td>${row.scores[exam.id]?.toString().orEmpty().escape()}</td>") }
                append("<td>${row.averagePercent?.let { "%.2f".format(it) }.orEmpty()}</td></tr>")
            }
            append("</table></body></html>")
        }
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(
                    "grade-report",
                    view.createPrintDocumentAdapter("گزارش نمرات"),
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun String.escape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
