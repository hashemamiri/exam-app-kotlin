package ir.exam.app.core.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.math.NativeMathCanvasRenderer
import ir.exam.app.core.math.NativeMathFormatter
import ir.exam.app.core.math.NativeMathParser
import ir.exam.app.domain.model.OfficialExamPrintable
import androidx.core.content.res.ResourcesCompat
import ir.exam.app.R
import ir.exam.app.domain.model.OfficialGradeReportPrintable
import ir.exam.app.domain.model.OfficialPrintable
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.roundToInt

class OfficialPdfPrintAdapter(
    private val context: Context,
    private val printable: OfficialPrintable
) : PrintDocumentAdapter() {
    private val renderer = OfficialPdfRenderer(context,printable)

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(safeJobName(printable.documentTitle))
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(renderer.pageCount)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        try {
            val written = renderer.write(destination, pages, cancellationSignal)
            if (cancellationSignal.isCanceled) callback.onWriteCancelled()
            else callback.onWriteFinished(written.toTypedArray())
        } catch (error: Throwable) {
            callback.onWriteFailed(error.message ?: "ساخت PDF ناموفق بود.")
        }
    }

    private fun safeJobName(value: String): String = value.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
}

private class OfficialPdfRenderer(private val context:Context,private val printable: OfficialPrintable) {
    private val mathRenderer=NativeMathCanvasRenderer()
    private data class RenderBlock(
        val text: String? = null,
        val formula: String? = null,
        val image: Bitmap? = null,
        val textSize: Float = 11f,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val align: String = "right",
        val fontFamily: String = "default",
        val imagePosition: String = "below",
        val imageWidthMm: Float = 80f,
        val imageXmm: Float = 20f,
        val imageYmm: Float = 30f,
        val boxed: Boolean = false,
        val spacingAfter: Float = 6f
    )
    private data class PlannedBlock(val block: RenderBlock, val height: Float)
    private data class PlannedPage(val blocks: List<PlannedBlock>)

    private val pages: List<PlannedPage> = planPages()
    val pageCount: Int get() = pages.size.coerceAtLeast(1)

    fun write(
        destination: ParcelFileDescriptor,
        ranges: Array<out PageRange>,
        cancellation: CancellationSignal
    ): List<PageRange> {
        val pdf = PdfDocument()
        val writtenPages = mutableListOf<Int>()
        try {
            pages.forEachIndexed { index, pageModel ->
                if (cancellation.isCanceled) return@forEachIndexed
                val pageNumber = index + 1
                if (!isPageRequested(index, ranges)) return@forEachIndexed
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                drawPage(page.canvas, pageModel, pageNumber, pages.size)
                pdf.finishPage(page)
                writtenPages += index
            }
            FileOutputStream(destination.fileDescriptor).use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        return collapseRanges(writtenPages)
    }

    private fun planPages(): List<PlannedPage> {
        val blocks = when (printable) {
            is OfficialExamPrintable -> examBlocks(printable)
            is OfficialGradeReportPrintable -> reportBlocks(printable)
        }
        val result = mutableListOf<PlannedPage>()
        var current = mutableListOf<PlannedBlock>()
        var used = 0f
        blocks.forEach { block ->
            val height = measureBlock(block).coerceAtMost(CONTENT_HEIGHT)
            if (used + height > CONTENT_HEIGHT && current.isNotEmpty()) {
                result += PlannedPage(current)
                current = mutableListOf()
                used = 0f
            }
            current += PlannedBlock(block, height)
            used += height
        }
        if (current.isNotEmpty()) result += PlannedPage(current)
        return result.ifEmpty { listOf(PlannedPage(emptyList())) }
    }

    private fun examBlocks(exam: OfficialExamPrintable): List<RenderBlock> = buildList {
        add(RenderBlock(text="درس: ${exam.subject}     مدت: ${exam.durationMinutes} دقیقه     بارم: ${formatScore(exam.totalScore)}",textSize=11f,bold=true,boxed=true))
        exam.questions.forEach { question ->
            add(RenderBlock(text="سؤال ${question.number}     (${formatScore(question.score)} نمره)",textSize=question.fontSizeSp.coerceIn(8f,30f),bold=true,boxed=true,fontFamily=question.fontFamily,align=question.textAlign))
            NativeMathFormatter.segments(question.text).forEach { segment ->
                if(segment.math) add(RenderBlock(formula=segment.text,textSize=question.fontSizeSp.coerceIn(8f,30f),bold=question.bold,italic=question.italic,align=question.textAlign,fontFamily=question.fontFamily))
                else splitText(segment.text.replace("\\$","$"),700).forEach { add(RenderBlock(text=it,textSize=question.fontSizeSp.coerceIn(8f,30f),bold=question.bold,italic=question.italic,align=question.textAlign,fontFamily=question.fontFamily)) }
            }
            question.options.forEachIndexed { index, option ->
                NativeMathFormatter.segments("${index+1}) $option").forEach { segment ->
                    if(segment.math)add(RenderBlock(formula=segment.text,textSize=question.fontSizeSp*.9f,fontFamily=question.fontFamily))
                    else add(RenderBlock(text=segment.text,textSize=question.fontSizeSp*.9f,fontFamily=question.fontFamily))
                }
            }
            question.images.forEachIndexed { index,image -> add(RenderBlock(
                image=image,boxed=true,imagePosition=question.imagePosition,
                imageWidthMm=question.imageWidthsMm.getOrNull(index)?:80f,
                imageXmm=question.imageXmm.getOrNull(index)?:20f,imageYmm=question.imageYmm.getOrNull(index)?:30f
            )) }
            if(exam.includeAnswerKey&&!question.answerText.isNullOrBlank())add(RenderBlock(text="پاسخ: ${NativeMathFormatter.renderText(question.answerText)}",textSize=10.5f,bold=true,fontFamily=question.fontFamily))
            else repeat(question.answerLines.coerceIn(0,12)) {
                add(RenderBlock(
                    text=if(question.answerLineStyle=="blank") " " else "................................................................................................................",
                    textSize=9f
                ))
            }
            add(RenderBlock(text="",spacingAfter=9f))
        }
    }

    private fun reportBlocks(report: OfficialGradeReportPrintable): List<RenderBlock> = buildList {
        add(RenderBlock(
            text = "آزمون‌ها: ${report.examTitles.joinToString("، ")}",
            textSize = 10.5f,
            bold = true,
            boxed = true
        ))
        report.rows.forEachIndexed { index, row ->
            val average = row.averagePercent?.let { "%.1f%%".format(it) } ?: "—"
            val chunks = row.scoreLines.chunked(8).ifEmpty { listOf(emptyList()) }
            chunks.forEachIndexed { chunkIndex, scores ->
                val text = buildString {
                    if (chunkIndex == 0) {
                        append(index + 1).append(". ").append(row.studentName)
                            .append("     میانگین: ").append(average)
                    } else append("ادامه نمرات ").append(row.studentName)
                    if (scores.isNotEmpty()) append("\n").append(scores.joinToString("     "))
                }
                add(RenderBlock(text = text, textSize = 10.5f, bold = chunkIndex == 0, boxed = true, spacingAfter = 5f))
            }
        }
    }

    private fun drawPage(canvas: Canvas, page: PlannedPage, pageNumber: Int, totalPages: Int) {
        canvas.drawColor(Color.WHITE)
        drawHeader(canvas, pageNumber, totalPages)
        var y = CONTENT_TOP
        page.blocks.forEach { planned ->
            val block = planned.block
            if (block.boxed) {
                canvas.drawRoundRect(
                    MARGIN - 3f, y - 2f, PAGE_WIDTH - MARGIN + 3f, y + planned.height - block.spacingAfter,
                    5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = Color.rgb(120, 120, 120)
                        strokeWidth = 0.8f
                    }
                )
            }
            block.image?.let { drawImage(canvas, it, y, planned.height - block.spacingAfter,block) }
            block.formula?.let { formula -> mathRenderer.draw(canvas,NativeMathParser.parse(formula),MARGIN,y,block.textSize,Color.BLACK) }
            block.text?.takeIf(String::isNotEmpty)?.let { text ->
                val layout = textLayout(text, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily)
                canvas.save()
                canvas.translate(MARGIN, y)
                layout.draw(canvas)
                canvas.restore()
            }
            y += planned.height
        }
        drawFooter(canvas, pageNumber, totalPages)
    }

    private fun drawHeader(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRect(MARGIN, 25f, PAGE_WIDTH - MARGIN, HEADER_BOTTOM, border)
        val header = printable.header
        drawRtl(
            canvas,
            listOf(
                "استان: ${header.province}",
                "شهر: ${header.city}",
                "منطقه: ${header.district}",
                "مدرسه: ${header.school}",
                "پایه: ${header.grade}"
            ).joinToString("\n"),
            PAGE_WIDTH - MARGIN - 8f,
            37f,
            9.5f,
            false,
            170
        )
        drawCentered(canvas, "بسمه تعالی\n${printable.documentTitle}", PAGE_WIDTH / 2f, 39f, 11.5f, true)
        val date = JalaliCalendar.fromGregorian(LocalDate.now()).display()
        drawRtl(canvas, "تاریخ: $date\nصفحه: $pageNumber از $totalPages", 175f, 40f, 9.5f, false, 130)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 8.5f; textAlign = Paint.Align.RIGHT }
        canvas.drawText(printable.footerNote, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 25f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("سامانه آزمون Native · $pageNumber/$totalPages", MARGIN, PAGE_HEIGHT - 25f, paint)
    }

    private fun drawImage(canvas: Canvas, bitmap: Bitmap, top: Float, availableHeight: Float,block:RenderBlock) {
        val targetWidth=(block.imageWidthMm/210f*PAGE_WIDTH).coerceIn(40f,CONTENT_WIDTH-12f)
        val maxHeight=availableHeight.coerceAtMost(220f)
        val scale=minOf(targetWidth/bitmap.width,maxHeight/bitmap.height,1f)
        val width=bitmap.width*scale;val height=bitmap.height*scale
        val left=when(block.imagePosition){"right"->PAGE_WIDTH-MARGIN-width;"left"->MARGIN;"free"->MARGIN+(block.imageXmm/210f*CONTENT_WIDTH).coerceIn(0f,CONTENT_WIDTH-width);else->MARGIN+(CONTENT_WIDTH-width)/2f}
        val y=if(block.imagePosition=="free")top+(block.imageYmm/297f*80f).coerceAtMost(80f)else top+3f
        canvas.drawBitmap(bitmap,null,android.graphics.RectF(left,y,left+width,y+height),null)
    }

    private fun measureBlock(block: RenderBlock): Float {
        block.image?.let { image ->
            val targetWidth=(block.imageWidthMm/210f*PAGE_WIDTH).coerceIn(40f,CONTENT_WIDTH-12f)
            val scale=minOf(targetWidth/image.width,220f/image.height,1f)
            return image.height*scale+block.spacingAfter+8f
        }
        block.formula?.let { return mathRenderer.measure(NativeMathParser.parse(it),block.textSize).height+block.spacingAfter+5f }
        val text = block.text.orEmpty()
        if (text.isEmpty()) return block.spacingAfter
        return textLayout(text, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily).height + block.spacingAfter + 4f
    }

    private fun textLayout(text:String,size:Float,bold:Boolean,width:Int,italic:Boolean=false,align:String="right",fontFamily:String="default"):StaticLayout {
        val base=when(fontFamily.lowercase()){"vazir","vazirmatn"->ResourcesCompat.getFont(context,R.font.vazirmatn_regular);"shabnam"->ResourcesCompat.getFont(context,R.font.shabnam_regular);"sahel"->ResourcesCompat.getFont(context,R.font.sahel_regular);else->Typeface.create("sans",Typeface.NORMAL)}
        val style=when{bold&&italic->Typeface.BOLD_ITALIC;bold->Typeface.BOLD;italic->Typeface.ITALIC;else->Typeface.NORMAL}
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.BLACK;textSize=size;typeface=Typeface.create(base,style)}
        val alignment=when(align){"center"->Layout.Alignment.ALIGN_CENTER;"left"->Layout.Alignment.ALIGN_OPPOSITE;else->Layout.Alignment.ALIGN_NORMAL}
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun drawRtl(canvas: Canvas, text: String, right: Float, top: Float, size: Float, bold: Boolean, width: Int) {
        val layout = textLayout(text, size, bold, width)
        canvas.save()
        canvas.translate(right - width, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawCentered(canvas: Canvas, text: String, centerX: Float, top: Float, size: Float, bold: Boolean) {
        val width = 220
        val layout = textLayout(text, size, bold, width)
        canvas.save()
        canvas.translate(centerX - width / 2f, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun isPageRequested(index: Int, ranges: Array<out PageRange>): Boolean =
        ranges.any { it == PageRange.ALL_PAGES || index in it.start..it.end }

    private fun collapseRanges(indices: List<Int>): List<PageRange> {
        if (indices.isEmpty()) return emptyList()
        val result = mutableListOf<PageRange>()
        var start = indices.first()
        var end = start
        indices.drop(1).forEach { value ->
            if (value == end + 1) end = value
            else {
                result += PageRange(start, end)
                start = value
                end = value
            }
        }
        result += PageRange(start, end)
        return result
    }

    private fun splitText(value: String, chunk: Int): List<String> =
        if (value.length <= chunk) listOf(value) else value.chunked(chunk)

    private fun formatScore(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 38f
        const val HEADER_BOTTOM = 112f
        const val CONTENT_TOP = 125f
        const val CONTENT_BOTTOM = 795f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        const val CONTENT_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP
    }
}
