package ir.exam.app.core.printing

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import ir.exam.app.core.math.NativeMathFormatter
import ir.exam.app.domain.model.PrintDocument
import ir.exam.app.domain.model.QuestionPrintBlock
import java.io.OutputStream

/** رندر PDF واقعی A4 برای مدل قدیمی Preview؛ متن فارسی و فرمول Native را حفظ می‌کند. */
class PdfExamRenderer {
    fun write(document: PrintDocument, output: OutputStream) {
        val pdf = PdfDocument()
        try {
            document.pages.forEach { pageModel ->
                val page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageModel.number).create()
                )
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("آزمون آنلاین — صفحه ${pageModel.number}", PAGE_WIDTH / 2f, 36f, titlePaint)
                var y = 58f
                pageModel.blocks.forEach { block ->
                    if (block is QuestionPrintBlock) {
                        val text = "سؤال ${block.row} (${block.score} نمره)\n" +
                            NativeMathFormatter.renderText(block.htmlFreeText)
                        val layout = rtlLayout(text, CONTENT_WIDTH)
                        canvas.drawRoundRect(
                            MARGIN - 3f, y - 3f, PAGE_WIDTH - MARGIN + 3f,
                            y + layout.height + 8f, 4f, 4f,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.GRAY
                                style = Paint.Style.STROKE
                            }
                        )
                        canvas.save()
                        canvas.translate(MARGIN, y)
                        layout.draw(canvas)
                        canvas.restore()
                        y += layout.height + 16f
                    }
                }
                pdf.finishPage(page)
            }
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }

    private fun rtlLayout(text: String, width: Int): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create("sans", Typeface.NORMAL)
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val CONTENT_WIDTH = PAGE_WIDTH - 80
    }
}
