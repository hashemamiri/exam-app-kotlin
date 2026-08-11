package ir.exam.app.core.printing

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialGradeReportPrintable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class OfficialPrintController(context: Context) {
    private val appContext = context.applicationContext
    private val imageLoader = ImageLoader(appContext)

    suspend fun printExam(context: Context, source: OfficialExamPrintable) {
        val withImages = coroutineScope {
            source.copy(
                questions = source.questions.map { question ->
                    question.copy(
                        images = question.imageUrls.map { url -> async { loadBitmap(url) } }
                            .awaitAll().filterNotNull()
                    )
                }
            )
        }
        print(context, withImages, "exam-${source.documentTitle}")
    }

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

    private suspend fun loadBitmap(url: String): android.graphics.Bitmap? {
        if (!url.startsWith("https://", true)) return null
        val request = ImageRequest.Builder(appContext)
            .data(url)
            .allowHardware(false)
            .size(1600, 1600)
            .build()
        val result = imageLoader.execute(request)
        return (result as? SuccessResult)?.drawable?.toBitmap()
    }
}
