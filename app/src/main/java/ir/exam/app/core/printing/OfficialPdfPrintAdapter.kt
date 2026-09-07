package ir.exam.app.core.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
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
import android.text.style.ReplacementSpan
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.figure.AtlasBitmapRenderer
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.NativeMathCanvasRenderer
import ir.exam.app.core.math.NativeMathFormatter
import ir.exam.app.core.math.NativeMathParser
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
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

/**
 * موتور چیدمان و رسم PDF رسمی.
 *
 * یک جریان پیوسته از بلوک‌های آزمون یا کارنامه می‌سازد، فقط روی مرز بلوک/سطر
 * آن را به صفحه‌های A4 می‌بُرد و فرمول، شکل، متن و سربرگ را به‌صورت بومی رسم
 * می‌کند. این موتور مستقل از سطح HTML پیش‌نمایش/چاپ است.
 */
class OfficialPrintLayoutEngine(private val context: Context) {
    private val mathRenderer=NativeMathCanvasRenderer()
    // V69.0 — کش منابع موتور (پرفورمنس حرفه‌ای): فونت از res، چیدمان StaticLayout
    // و بیت‌مپ شکل‌ها با کلید محتوا کش می‌شوند تا در هر رندر/تغییر حرف کار
    // تکراری انجام نشود؛ بیت‌مپ و چیدمان با LruCacheK سقف حافظه دارند.
    private val typefaceCache = HashMap<String, Typeface>()
    private val layoutCache = LruCacheK<StaticLayout>(
        maxBytes = 8L * 1024L * 1024L,
        sizeOf = { layout ->
            (layout.text.length * 4L + layout.height.toLong() * 16L).coerceAtLeast(1024L)
        }
    )
    data class RenderBlock(
        val text: String? = null,
        val image: Bitmap? = null,
        val textSize: Float = 11f,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val align: String = "right",
        val fontFamily: String = "default",
        val imagePosition: String = "below",
        val imageWidthMm: Float = 80f,
        /** ارتفاع صریحِ gesture resize؛ null یعنی نسبت طبیعی bitmap. */
        val imageHeightMm: Float? = null,
        val imageXmm: Float = 20f,
        val imageYmm: Float = 30f,
        val boxed: Boolean = false,
        val spacingAfter: Float = 6f,
        val styledText: android.text.SpannableStringBuilder? = null,
        val matchRight: String? = null,
        val matchLeft: String? = null,
        val matchRightStyle: Triple<Boolean, Boolean, Float?>? = null,
        val matchLeftStyle: Triple<Boolean, Boolean, Float?>? = null,
        /** مالک بلوک؛ فقط metadata پیش‌نمایش PDF است و به UI وابسته نیست. */
        val questionIndex: Int = -1,
        /** شمارهٔ occurrence شکل در متن سؤال؛ -1 یعنی تصویر پیوست سؤال. */
        val figureOccurrence: Int = -1,
        /** خط فاصلهٔ قابل‌کشیدنِ بین سؤال‌ها. */
        val separatorQuestionIndex: Int = -1,
        /** شکل آزاد از figLayoutsJson یا خود spec آمده است. */
        val freeFigure: Boolean = false
    )
    data class Placed(val block: RenderBlock, val y: Float, val height: Float)

    /** نشانهٔ یک figure inline برای overlay لمسی پیش‌نمایش PDF. */
    data class InlineFigureMark(
        val charOffset: Int,
        val questionIndex: Int,
        val occurrence: Int,
        val widthPt: Float,
        val heightPt: Float
    )

    /** مستطیل یک شکل روی جریان پیوسته، پیش از نگاشت به صفحهٔ PDF. */
    data class FigureBounds(
        val rect: RectF,
        val questionIndex: Int,
        val occurrence: Int,
        val free: Boolean
    )

    /** محل خط فاصلهٔ یک سؤال روی جریان پیوسته. */
    data class SeparatorBounds(
        val questionIndex: Int,
        val y: Float,
        val height: Float
    )

    /** جریان آماده برای برش صفحه‌های PDF و metadata تعامل Native. */
    class EngineDocument(
        val placed: List<Placed>,
        val slices: List<Pair<Float, Float>>,
        val layouts: Map<Int, StaticLayout>,
        val inlineFigureMarks: Map<Int, List<InlineFigureMark>>,
        val figureBounds: List<FigureBounds>,
        val separatorBounds: List<SeparatorBounds>,
        val questionOrigins: Map<Int, Float>,
        val total: Float,
        val firstContentTop: Float
    ) {
        val pageCount: Int get() = slices.size.coerceAtLeast(1)
        fun questionOriginPt(index: Int): Float = questionOrigins[index] ?: 0f
    }

    // ---------------------------------------------------------------- ساخت

    /** آزمون رسمی: سؤال‌ها → جریان پیوسته و صفحه‌های PDF. */
    fun layoutExam(printable: OfficialExamPrintable): EngineDocument =
        layoutExam(printable, CONTENT_TOP)

    /**
     * همان جریان آزمون با فضای سربرگِ محاسبه‌شدهٔ renderer بومی. در نتیجه
     * headerهای قابل‌تنظیم هرگز روی سؤال اول نمی‌افتند و preview/print یک
     * نقطهٔ شروع مشترک دارند.
     */
    fun layoutExam(printable: OfficialExamPrintable, firstContentTop: Float): EngineDocument =
        build(examBlocks(printable), firstContentTop)

    /** کارنامه: همان موتور با بلوک‌های کارنامه. */
    fun layoutReport(report: OfficialGradeReportPrintable): EngineDocument =
        build(reportBlocks(report), CONTENT_TOP)

    private val pendingInlineMarks = HashMap<Int, List<InlineFigureMark>>()

    private fun build(blocks: List<RenderBlock>, firstContentTop: Float): EngineDocument {
        val placed = placeAll(blocks)
        // بلوکِ آزاد slot خود را در جریان نگه می‌دارد، اما ممکن است خود تصویر
        // با y آزاد خیلی پایین‌تر از slot قرار گیرد. total باید extent واقعی
        // تصویر را هم ببیند تا صفحه‌های لازم ساخته شوند.
        var total = (placed.lastOrNull()?.let { it.y + it.height } ?: 1f).coerceAtLeast(1f)
        val layouts = HashMap<Int, StaticLayout>()
        val inlineMarks = HashMap<Int, List<InlineFigureMark>>()
        val questionOrigins = linkedMapOf<Int, Float>()
        val figures = mutableListOf<FigureBounds>()
        val separators = mutableListOf<SeparatorBounds>()
        // مرزهای مجاز برش: انتهای هر بلوک و انتهای هر سطر متن. در نتیجه صفحه
        // تا جای ممکن از وسط سطر قطع نمی‌شود.
        val boundaries = sortedSetOf(0f)
        placed.forEachIndexed { index, placedBlock ->
            val block = placedBlock.block
            if (block.questionIndex >= 0) questionOrigins.putIfAbsent(block.questionIndex, placedBlock.y)
            val layout = blockLayout(block)
            if (layout != null) {
                layouts[index] = layout
                for (line in 0 until layout.lineCount) boundaries.add(placedBlock.y + layout.getLineBottom(line))
            }
            pendingInlineMarks.remove(index)?.let { inlineMarks[index] = it }
            val imageRect = imageRectPt(block, placedBlock.y)
            if (imageRect != null && block.imagePosition == "free") {
                // مرزهای واقعی شکل آزاد به الگوریتم slice داده می‌شوند. در غیر
                // این صورت شکلِ کشیده‌شده پس از page break ممکن بود فقط در
                // صفحهٔ slot خودش draw شود و در صفحهٔ مقصد دیده نشود.
                total = maxOf(total, imageRect.bottom)
                boundaries.add(imageRect.top.coerceAtLeast(0f))
                boundaries.add(imageRect.bottom)
            }
            if (imageRect != null && block.questionIndex >= 0 && block.figureOccurrence >= 0) {
                figures += FigureBounds(imageRect, block.questionIndex, block.figureOccurrence, block.freeFigure)
            }
            if (block.separatorQuestionIndex >= 0) {
                separators += SeparatorBounds(block.separatorQuestionIndex, placedBlock.y, placedBlock.height)
            }
            boundaries.add(placedBlock.y + placedBlock.height)
        }
        inlineMarks.forEach { (blockIndex, marks) ->
            val layout = layouts[blockIndex] ?: return@forEach
            val placedBlock = placed[blockIndex]
            marks.forEach { mark ->
                val line = layout.getLineForOffset(mark.charOffset.coerceIn(0, layout.text.length))
                val left = MARGIN + replacementLeftPt(layout, mark.charOffset, mark.widthPt)
                val baseline = layout.getLineBaseline(line)
                val top = placedBlock.y + baseline - placedBlock.block.textSize * .92f
                figures += FigureBounds(
                    rect = RectF(left, top, left + mark.widthPt, top + mark.heightPt),
                    questionIndex = mark.questionIndex,
                    occurrence = mark.occurrence,
                    free = false
                )
            }
        }
        return EngineDocument(
            placed = placed,
            slices = computeSlices(total, boundaries.toList(), firstContentTop),
            layouts = layouts,
            inlineFigureMarks = inlineMarks,
            figureBounds = figures,
            separatorBounds = separators,
            questionOrigins = questionOrigins,
            total = total,
            firstContentTop = firstContentTop
        )
    }

    /** چیدمان پیوسته: y تجمعی همهٔ بلوک‌ها بدون صفحه‌بندی بلوک‌به‌بلوک. */
    private fun placeAll(blocks: List<RenderBlock>): List<Placed> {
        val out = mutableListOf<Placed>()
        var y = 0f
        blocks.forEach { block ->
            val h = measureBlock(block)
            out += Placed(block, y, h)
            y += h
        }
        return out
    }

    // ------------------------------------------------------------- بلوک‌ها

    private fun examBlocks(exam: OfficialExamPrintable): List<RenderBlock> = buildList {
        pendingInlineMarks.clear()
        add(
            RenderBlock(
                text = "درس: ${exam.subject}     مدت: ${exam.durationMinutes} دقیقه     بارم: ${formatScore(exam.totalScore)}",
                textSize = 11f,
                bold = true,
                boxed = true
            )
        )
        exam.questions.forEachIndexed { questionIndex, question ->
            // اندیس شروع بلوک‌های همین سؤال؛ مختصات y شکل‌های آزاد نسبت به
            // ابتدای همین سؤال ذخیره می‌شوند، نه نسبت به یک صفحهٔ موقت.
            val qStart = size
            val storedLayouts = PrintPreviewLayoutCodec.decode(question.figLayoutsJson)
            var figureOccurrence = 0

            fun addQuestionBlock(block: RenderBlock) {
                add(block.copy(questionIndex = questionIndex))
            }

            addQuestionBlock(
                RenderBlock(
                    text = "سؤال ${question.number}     (${formatScore(question.score)} نمره)",
                    textSize = question.fontSizeSp.coerceIn(8f, 30f),
                    bold = true,
                    boxed = true,
                    fontFamily = question.fontFamily,
                    align = question.textAlign
                )
            )
            // شکل/نمودار/جدول درون‌متنی (%%FIG%%) و فرمول‌ها در همان جریان
            // PDF رسم می‌شوند؛ هیچ asset یا runtime HTML در این مسیر وجود ندارد.
            val formulas = ir.exam.app.core.math.FormulaTextCodec.occurrences(question.text)
            val figures = ir.exam.app.core.figure.FigureCodec.occurrences(question.text)
            val segments = RichTextSplitter.split(question.text)
            val ranges = RichTextSplitter.segmentSourceRanges(segments, formulas, figures)
            val spans = question.textSpans
            var inline = android.text.SpannableStringBuilder()
            var inlineLen = 0
            var pendingMarks = mutableListOf<InlineFigureMark>()

            fun flushInline() {
                if (inline.isEmpty()) return
                val blockIndex = size
                addQuestionBlock(
                    RenderBlock(
                        styledText = inline,
                        textSize = question.fontSizeSp.coerceIn(8f, 30f),
                        bold = question.bold,
                        italic = question.italic,
                        align = question.textAlign,
                        fontFamily = question.fontFamily
                    )
                )
                if (pendingMarks.isNotEmpty()) {
                    pendingInlineMarks[blockIndex] = pendingMarks
                    pendingMarks = mutableListOf()
                }
                inline = android.text.SpannableStringBuilder()
                inlineLen = 0
            }

            segments.forEachIndexed { segmentIndex, rich ->
                when (rich) {
                    is RichSegment.Math -> {
                        // اگر پاراگراف با فرمول شروع شود، RLM جهت راست‌به‌چپ
                        // متن فارسی را تثبیت می‌کند.
                        if (inline.isEmpty()) {
                            inline.append('\u200F')
                            inlineLen += 1
                        }
                        inline.append('\uFFFC')
                        inline.setSpan(
                            MathReplacementSpan(NativeMathParser.parse(rich.tex)),
                            inlineLen,
                            inlineLen + 1,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        inlineLen += 1
                    }

                    is RichSegment.Figure -> {
                        val occurrence = figureOccurrence++
                        val stored = storedLayouts[occurrence]
                        val specPosition = PrintFigureMetrics.figurePosMm(rich.spec)
                        val bitmap = figureBitmap(rich.spec)
                        if (bitmap != null) {
                            // حرکت روی PDF، شکل inline را به شکل آزاد تبدیل می‌کند.
                            // مختصات حالت آزاد همواره از ابتدای همان سؤال خوانده
                            // می‌شود تا page-breakهای بعدی جای آن را تغییر ندهند.
                            val isFree = stored?.free == true || specPosition != null
                            val widthMm = stored?.widthMm ?: PrintFigureMetrics.figureWidthMm(rich.spec)
                            // مقدار height فقط پس از resize صریح یا از layout قدیمی می‌آید؛
                            // در غیر این صورت نسبت طبیعی bitmap حفظ می‌شود.
                            val heightMm = stored?.heightMm
                            if (isFree) {
                                flushInline()
                                val flowPt = (qStart until size).sumOf { measureBlock(this[it]).toDouble() }.toFloat()
                                val xMm = stored?.xMm ?: specPosition?.first ?: 0f
                                val yMm = stored?.yMm ?: specPosition?.second ?: 0f
                                addQuestionBlock(
                                    RenderBlock(
                                        image = bitmap,
                                        imageWidthMm = widthMm,
                                        imageHeightMm = heightMm,
                                        imagePosition = "free",
                                        imageXmm = xMm,
                                        // drawImageAt به top بلوک این مقدار را
                                        // اضافه می‌کند؛ با کم‌کردن flow، y نهایی
                                        // دقیقاً نسبت به ابتدای سؤال باقی می‌ماند.
                                        imageYmm = yMm - flowPt / MM_TO_PT,
                                        figureOccurrence = occurrence,
                                        freeFigure = true
                                    )
                                )
                            } else {
                                if (inline.isEmpty()) {
                                    inline.append('\u200F')
                                    inlineLen += 1
                                }
                                val target = figureTargetSizePt(bitmap, widthMm, heightMm)
                                pendingMarks += InlineFigureMark(
                                    charOffset = inlineLen,
                                    questionIndex = questionIndex,
                                    occurrence = occurrence,
                                    widthPt = target.first,
                                    heightPt = target.second
                                )
                                inline.append('\uFFFC')
                                inline.setSpan(
                                    FigureReplacementSpan(bitmap, widthMm, heightMm),
                                    inlineLen,
                                    inlineLen + 1,
                                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                inlineLen += 1
                            }
                        } else {
                            flushInline()
                            addQuestionBlock(
                                RenderBlock(
                                    text = "[شکل]",
                                    textSize = question.fontSizeSp.coerceIn(8f, 30f)
                                )
                            )
                        }
                    }

                    is RichSegment.Text -> if (rich.text.isNotEmpty()) {
                        val segmentStart = ranges.getOrNull(segmentIndex)?.first ?: 0
                        val overlapping = spans.any {
                            it.end > segmentStart && it.start < segmentStart + rich.text.length
                        }
                        val pieceText = rich.text.replace("\\$", "$")
                        inline.append(pieceText)
                        if (overlapping) {
                            var offset = inlineLen
                            PrintTextSpanSegments.split(rich.text, segmentStart, spans).forEach { piece ->
                                val from = offset
                                val to = offset + piece.first.length
                                offset = to
                                if (piece.second) {
                                    inline.setSpan(
                                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                        from,
                                        to,
                                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                                if (piece.third) {
                                    inline.setSpan(
                                        android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                                        from,
                                        to,
                                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                            }
                        }
                        inlineLen += pieceText.length
                    }
                }
            }
            flushInline()

            question.options.forEachIndexed { index, option ->
                val optionStyle = question.optionStyles.getOrNull(index)
                val optionSize = (optionStyle?.third ?: question.fontSizeSp) * OPTION_SCALE
                val optionBold = optionStyle?.first ?: false
                val optionItalic = optionStyle?.second ?: false
                val optionText = android.text.SpannableStringBuilder()
                var optionLength = 0
                var prefixLength = "${index + 1}) ".length
                NativeMathFormatter.segments("${index + 1}) $option").forEach { segment ->
                    if (segment.math) {
                        optionText.append('\uFFFC')
                        optionText.setSpan(
                            MathReplacementSpan(NativeMathParser.parse(segment.text)),
                            optionLength,
                            optionLength + 1,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        optionLength += 1
                    } else if (segment.text.isNotEmpty()) {
                        val boldLength = prefixLength.coerceIn(0, segment.text.length)
                        optionText.append(segment.text)
                        if (boldLength > 0) {
                            optionText.setSpan(
                                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                optionLength,
                                optionLength + boldLength,
                                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        prefixLength -= boldLength
                        optionLength += segment.text.length
                    }
                }
                if (optionText.isNotEmpty()) {
                    addQuestionBlock(
                        RenderBlock(
                            styledText = optionText,
                            textSize = optionSize,
                            bold = optionBold,
                            italic = optionItalic,
                            align = question.textAlign,
                            fontFamily = question.fontFamily
                        )
                    )
                }
            }

            val matchingRows = maxOf(question.matchingLeft.size, question.matchingRight.size)
            repeat(matchingRows) { rowIndex ->
                addQuestionBlock(
                    RenderBlock(
                        matchRight = question.matchingRight.getOrNull(rowIndex),
                        matchLeft = question.matchingLeft.getOrNull(rowIndex),
                        matchRightStyle = question.matchingRightStyles.getOrNull(rowIndex),
                        matchLeftStyle = question.matchingLeftStyles.getOrNull(rowIndex),
                        textSize = question.fontSizeSp.coerceIn(8f, 30f),
                        bold = question.bold,
                        italic = question.italic,
                        align = question.textAlign,
                        fontFamily = question.fontFamily
                    )
                )
            }

            question.images.forEachIndexed { index, image ->
                val rawX = question.imageXmm.getOrNull(index) ?: 20f
                val rawY = question.imageYmm.getOrNull(index) ?: 30f
                val isDefault = rawX == 20f && rawY == 30f
                val position = if (question.imagePosition == "free" && isDefault) "below" else question.imagePosition
                addQuestionBlock(
                    RenderBlock(
                        image = image,
                        boxed = true,
                        imagePosition = position,
                        imageWidthMm = question.imageWidthsMm.getOrNull(index) ?: 80f,
                        imageXmm = rawX,
                        imageYmm = rawY
                    )
                )
            }

            if (exam.includeAnswerKey && !question.answerText.isNullOrBlank()) {
                addQuestionBlock(
                    RenderBlock(
                        text = "پاسخ: ${NativeMathFormatter.renderText(question.answerText)}",
                        textSize = 10.5f,
                        bold = true,
                        fontFamily = question.fontFamily
                    )
                )
            } else {
                repeat(question.answerLines.coerceIn(0, 12)) {
                    addQuestionBlock(
                        RenderBlock(
                            text = if (question.answerLineStyle in setOf("blank", "plain")) " "
                            else "................................................................................................................",
                            textSize = 9f
                        )
                    )
                }
            }

            // این هم همان فاصلهٔ قدیمی است، اما توسط gesture PDF بومی تنظیم و
            // در مقدار پایدار sepExtraPx نگه‌داری می‌شود.
            addQuestionBlock(
                RenderBlock(
                    text = "",
                    spacingAfter = QUESTION_GAP_PT + question.sepExtraPx.coerceIn(0, 1_500) * .75f,
                    separatorQuestionIndex = questionIndex
                )
            )
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

    // -------------------------------------------------------------- رسم

    /** جریانِ بخشِ جاری سند را روی بوم PDF رسم می‌کند. */
    fun drawFlowWindow(
        canvas: Canvas,
        document: EngineDocument,
        slice: Pair<Float, Float>
    ) {
        document.placed.forEachIndexed { index, placedBlock ->
            val block = placedBlock.block
            val slotIntersects = placedBlock.y + placedBlock.height > slice.first && placedBlock.y < slice.second
            // شکل آزاد ممکن است خارج از محدودهٔ slot اولیه و حتی در page بعد
            // باشد. خود extent تصویر، نه فقط slot، تعیین می‌کند در این slice
            // رسم شود؛ clip صفحه قسمت لازم را جدا می‌کند.
            val freeImageIntersects = if (block.imagePosition == "free") {
                imageRectPt(block, placedBlock.y)?.let { rect ->
                    rect.bottom > slice.first && rect.top < slice.second
                } == true
            } else {
                false
            }
            if (slotIntersects || freeImageIntersects) {
                drawBlockAt(canvas, block, placedBlock.y, placedBlock.height, document.layouts[index])
            }
        }
    }

    private fun drawBlockAt(
        canvas: Canvas,
        block: RenderBlock,
        y: Float,
        height: Float,
        cachedLayout: StaticLayout? = null
    ) {
        if (block.separatorQuestionIndex >= 0) {
            val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(144, 160, 176)
                strokeWidth = .8f
                pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f)
            }
            canvas.drawLine(MARGIN, y + 2f, PAGE_WIDTH - MARGIN, y + 2f, separatorPaint)
        }
        if (block.boxed) {
            // تصویر آزاد در جای visual خودش قاب می‌گیرد، نه در slot قدیمی؛
            // وگرنه هنگام عبور از page break قاب در صفحه‌ای جدا از تصویر می‌افتاد.
            val box = if (block.imagePosition == "free") {
                imageRectPt(block, y)?.let { image ->
                    RectF(image.left - 3f, image.top - 2f, image.right + 3f, image.bottom + 6f)
                }
            } else {
                null
            } ?: RectF(MARGIN - 3f, y - 2f, PAGE_WIDTH - MARGIN + 3f, y + height - block.spacingAfter)
            canvas.drawRoundRect(
                box,
                5f,
                5f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.rgb(120, 120, 120)
                    strokeWidth = 0.8f
                }
            )
        }
        block.image?.let { drawImageAt(canvas, it, y, block) }
        block.text?.takeIf(String::isNotEmpty)?.let { text ->
            val layout = cachedLayout ?: textLayout(text, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily)
            canvas.save(); canvas.translate(MARGIN, y); layout.draw(canvas); canvas.restore()
        }
        block.styledText?.let { sb ->
            val layout = cachedLayout ?: styledLayout(sb, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily)
            canvas.save(); canvas.translate(MARGIN, y); layout.draw(canvas); canvas.restore()
        }
        if (block.matchRight != null || block.matchLeft != null) {
            val half = matchHalfWidth()
            block.matchRight?.let {
                val layout = textLayout(it, block.matchRightStyle?.third ?: block.textSize,
                    block.matchRightStyle?.first ?: block.bold, half,
                    block.matchRightStyle?.second ?: block.italic, "right", block.fontFamily)
                canvas.save(); canvas.translate(PAGE_WIDTH - MARGIN - half, y); layout.draw(canvas); canvas.restore()
            }
            block.matchLeft?.let {
                val layout = textLayout(it, block.matchLeftStyle?.third ?: block.textSize,
                    block.matchLeftStyle?.first ?: block.bold, half,
                    block.matchLeftStyle?.second ?: block.italic, "left", block.fontFamily)
                canvas.save(); canvas.translate(MARGIN, y); layout.draw(canvas); canvas.restore()
            }
            val arrowPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = block.textSize; typeface = persianTypeface(false)
            }
            val arrow = "↔"
            canvas.drawText(arrow, PAGE_WIDTH / 2f - arrowPaint.measureText(arrow) / 2f, y + block.textSize, arrowPaint)
        }
    }

    /**
     * ضلع چپ ReplacementSpan در خط RTL با هر دو مرز کاراکتر تعیین می‌شود؛
     * getPrimaryHorizontal در RTL ممکن است مرز راست را بدهد.
     */
    private fun replacementLeftPt(layout: StaticLayout, offset: Int, widthPt: Float): Float {
        val start = offset.coerceIn(0, layout.text.length)
        val end = (start + 1).coerceAtMost(layout.text.length)
        val first = layout.getPrimaryHorizontal(start)
        val second = layout.getPrimaryHorizontal(end)
        val line = layout.getLineForOffset(start)
        val min = layout.getLineLeft(line)
        val max = (layout.getLineRight(line) - widthPt).coerceAtLeast(min)
        return minOf(first, second).coerceIn(min, max)
    }

    /** مستطیل واقعی تصویر در جریان PDF. */
    private fun imageRectPt(block: RenderBlock, top: Float): RectF? {
        val bitmap = block.image ?: return null
        val (width, height) = figureTargetSizePt(bitmap, block.imageWidthMm, block.imageHeightMm)
        val left=when(block.imagePosition){"right"->PAGE_WIDTH-MARGIN-width;"left"->MARGIN;"free"->MARGIN+(block.imageXmm*MM_TO_PT).coerceIn(0f,CONTENT_WIDTH-width);else->MARGIN+(CONTENT_WIDTH-width)/2f}
        // V68.8 — سند پیوسته: y آزاد نسبت به جای جریان خودش بدون clamp به یک
        // صفحه (سند بلند است)؛ کف فقط ۰ تا از بالای سند بیرون نرود.
        val y=if(block.imagePosition=="free")
            (top+block.imageYmm*MM_TO_PT).coerceAtLeast(0f)
        else top+3f
        return RectF(left,y,left+width,y+height)
    }

    private fun drawImageAt(canvas: Canvas, bitmap: Bitmap, top: Float, block: RenderBlock) {
        val rect = imageRectPt(block, top) ?: return
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    // ----------------------------------------------------------- اندازه‌گیری

    private fun blockLayout(block: RenderBlock): StaticLayout? = when {
        block.styledText != null -> styledLayout(block.styledText!!, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily)
        !block.text.isNullOrEmpty() -> textLayout(block.text!!, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily)
        else -> null
    }

    private fun measureBlock(block: RenderBlock): Float {
        block.image?.let { image ->
            val (_, height) = figureTargetSizePt(image, block.imageWidthMm, block.imageHeightMm)
            return height + block.spacingAfter + 8f
        }
        // V68.6 — ارتفاع ردیف جورکردنی: بلندترِ دو نیمه (آیتم‌ها می‌شکنند).
        if (block.matchRight != null || block.matchLeft != null) {
            val half = matchHalfWidth()
            val rightHeight = block.matchRight?.let {
                textLayout(it, block.matchRightStyle?.third ?: block.textSize,
                    block.matchRightStyle?.first ?: block.bold, half,
                    block.matchRightStyle?.second ?: block.italic, "right", block.fontFamily).height.toFloat()
            } ?: 0f
            val leftHeight = block.matchLeft?.let {
                textLayout(it, block.matchLeftStyle?.third ?: block.textSize,
                    block.matchLeftStyle?.first ?: block.bold, half,
                    block.matchLeftStyle?.second ?: block.italic, "left", block.fontFamily).height.toFloat()
            } ?: 0f
            return maxOf(rightHeight, leftHeight) + block.spacingAfter + 4f
        }
        block.styledText?.let {
            return styledLayout(it, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily).height + block.spacingAfter + 4f
        }
        val text = block.text.orEmpty()
        if (text.isEmpty()) return block.spacingAfter
        return textLayout(text, block.textSize, block.bold, CONTENT_WIDTH.roundToInt(),block.italic,block.align,block.fontFamily).height + block.spacingAfter + 4f
    }

    // V68.6 — عرض هر نیمهٔ ردیف جورکردنی (۲۶pt وسط برای «↔»).
    private fun matchHalfWidth(): Int = (((CONTENT_WIDTH - 26f) / 2f).coerceAtLeast(60f)).toInt()

    /**
     * اندازهٔ هدف یک شکل در صفحهٔ PDF.
     *
     * ارتفاع null یعنی نسبت bitmap و سقف قدیمی 220pt. هنگامی که کاربر دستگیرهٔ
     * resize را می‌کشد، هر دو بُعد در layout بومی ذخیره می‌شوند؛ در آن حالت
     * ارتفاع صریح باید بی‌کم‌وکاست در PDF، PdfRenderer و چاپ دیده شود.
     */
    private fun figureTargetSizePt(
        bitmap: Bitmap,
        widthMm: Float,
        requestedHeightMm: Float? = null
    ): Pair<Float, Float> {
        val targetWidth = (widthMm / 210f * PAGE_WIDTH).coerceIn(40f, CONTENT_WIDTH - 12f)
        val requestedHeight = requestedHeightMm
            ?.takeIf { it.isFinite() }
            ?.times(MM_TO_PT)
            ?.coerceIn(8f * MM_TO_PT, 900f * MM_TO_PT)
        if (requestedHeight != null) return targetWidth to requestedHeight

        val scale = minOf(targetWidth / bitmap.width, 220f / bitmap.height, 1f)
        return (bitmap.width * scale) to (bitmap.height * scale)
    }

    /**
     * V68.6 — فرمول درون‌خطی داخل پاراگراف چاپ: مثل ImageSpan روی جای‌نگهدار
     * U+FFFC می‌نشیند؛ StaticLayout عرض را از getSize می‌گیرد و با FontMetrics
     * ارتفاع سطر را رشد می‌دهد تا کسرها هم در همان سطر جا شوند. draw با خط
     * کرسی متن هم‌تراز است تا سمبل‌ها با خط پایهٔ متن PDF هماهنگ بمانند.
     */
    private inner class MathReplacementSpan(private val node: MathNode) : ReplacementSpan() {
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            val size = paint.textSize
            val metrics = mathRenderer.measure(node, size)
            if (fm != null) {
                // فرمول از خط کرسی شروع می‌شود: بالا به‌اندازهٔ متنِ عادی و
                // پایین به‌اندازهٔ باقی‌ماندهٔ ارتفاع فرمول جا می‌گیرد.
                val above = (size * 0.92f).toInt()
                fm.ascent = minOf(fm.ascent, -above)
                fm.descent = maxOf(fm.descent, (metrics.height - above).toInt().coerceAtLeast(0))
                fm.top = minOf(fm.top, fm.ascent)
                fm.bottom = maxOf(fm.bottom, fm.descent)
            }
            return metrics.width.toInt().coerceAtLeast(2)
        }

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            mathRenderer.draw(canvas, node, x, y - paint.textSize * 0.92f, paint.textSize, Color.BLACK)
        }
    }

    /**
     * شکل، نمودار یا جدولِ درون‌متنیِ غیرآزاد در همان پاراگرافِ متن، کنار
     * فرمول و متن می‌نشیند؛ بنابراین ردیفِ چند شکل بی‌دلیل به چند سطر عمودی
     * نمی‌شکند.
     */
    private inner class FigureReplacementSpan(
        private val bitmap: Bitmap,
        private val widthMm: Float,
        private val heightMm: Float?
    ) : ReplacementSpan() {
        private fun targetSize(): Pair<Float, Float> = figureTargetSizePt(bitmap, widthMm, heightMm)

        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
            val (w, h) = targetSize()
            if (fm != null) {
                val above = (paint.textSize * 0.92f).toInt()
                fm.ascent = minOf(fm.ascent, -above)
                fm.descent = maxOf(fm.descent, (h - above).toInt().coerceAtLeast(0))
                fm.top = minOf(fm.top, fm.ascent)
                fm.bottom = maxOf(fm.bottom, fm.descent)
            }
            return w.toInt().coerceAtLeast(2)
        }

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val (w, h) = targetSize()
            val dest = android.graphics.RectF(x, y - paint.textSize * 0.92f, x + w, y - paint.textSize * 0.92f + h)
            canvas.drawBitmap(bitmap, null, dest, null)
        }
    }

    fun textLayout(text:String,size:Float,bold:Boolean,width:Int,italic:Boolean=false,align:String="right",fontFamily:String="default"):StaticLayout {
        val key = layoutKey(text, size, bold, italic, align, fontFamily, width)
        return layoutCache.get(key) ?: styledLayout(
            android.text.SpannableStringBuilder(text), size, bold, width, italic, align, fontFamily
        ).also { layoutCache.put(key, it) }
    }

    /** V68 — چیدمان متن با استایل تکه‌ای (Spannable): بولد/ایتالیک درون‌خطی. */
    private fun styledLayout(text: android.text.SpannableStringBuilder, size: Float, bold: Boolean, width: Int, italic: Boolean = false, align: String = "right", fontFamily: String = "default"): StaticLayout {
        val base = fontFamilyFrom(fontFamily)
        val style=when{bold&&italic->Typeface.BOLD_ITALIC;bold->Typeface.BOLD;italic->Typeface.ITALIC;else->Typeface.NORMAL}
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.BLACK;textSize=size;typeface=Typeface.create(base,style)}
        val alignment=when(align){"center"->Layout.Alignment.ALIGN_CENTER;"left"->Layout.Alignment.ALIGN_OPPOSITE;else->Layout.Alignment.ALIGN_NORMAL}
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(LINE_SPACING_ADD_PT, 1f)
            .setIncludePad(false)
            .build()
    }

    /** V69.0 — فونت پایهٔ خانوادهٔ داده‌شده (کش‌شده)؛ پیش‌فرض بدون فونت فارسی. */
    private fun fontFamilyFrom(fontFamily: String): Typeface = typefaceCache.getOrPut(fontFamily.lowercase()) {
        when(fontFamily.lowercase()){
            "vazir","vazirmatn"->ResourcesCompat.getFont(context,R.font.vazirmatn_regular)
            "shabnam"->ResourcesCompat.getFont(context,R.font.shabnam_regular)
            "sahel"->ResourcesCompat.getFont(context,R.font.sahel_regular)
            else->Typeface.create("sans",Typeface.NORMAL)
        } ?: Typeface.create("sans",Typeface.NORMAL)
    }

    /**
     * V62.8 — فونت فارسی موتور: B Nazanin اگر کاربر فایل مجاز خود را در
     * assets/fonts/bnazanin.ttf گذاشته باشد؛ در غیر این صورت وزیرمتن.
     */
    fun persianTypeface(bold: Boolean): Typeface {
        val nazanin = nazaninCache ?: runCatching {
            Typeface.createFromAsset(context.assets, "fonts/bnazanin.ttf")
        }.getOrNull()?.also { nazaninCache = it }
        val base = nazanin
            ?: ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
            ?: Typeface.create("sans", Typeface.NORMAL)
        return Typeface.create(base, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private var nazaninCache: Typeface? = null

    /** V53.1 — رندر برداری شکل/نمودار/جدول به bitmap برای PDF (AndroidSVG، بدون WebView). */
    private fun figureBitmap(spec: FigureSpec): Bitmap? = figureBitmapCache[spec.raw.toString()] ?: runCatching {
        // V53.3 — آناتومی/فیزیک/شیمی از تصویر اطلس + نشانه‌های Native رندر می‌شوند.
        if (spec.kind in setOf("a", "s")) {
            AtlasBitmapRenderer.render(context, spec)
        } else {
            val document = FigureSvgRenderer.render(spec)
            val svg = com.caverock.androidsvg.SVG.getFromString(document.xml)
            val scale = 2f
            val width = (document.widthPx * scale).roundToInt().coerceAtLeast(1)
            val height = (document.heightPx * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()
            svg.renderToCanvas(canvas)
            bitmap
        }
    }.getOrNull()?.also { figureBitmapCache.put(spec.raw.toString(), it) }

    // V69.0 — کش بیت‌مپ شکل‌ها با سقف (LruCacheK) به‌جای HashMap بدون سقف.
    private val figureBitmapCache = LruCacheK<Bitmap>(
        maxBytes = 24L * 1024L * 1024L,
        sizeOf = { bmp -> (bmp.width.toLong() * bmp.height.toLong() * 4L).coerceAtLeast(4096L) }
    )

    private fun formatScore(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        // حاشیهٔ ۱۴ میلی‌متری A4، معادل تقریباً ۴۰pt در PDF.
        const val MARGIN = 40f
        // تبدیل واقعی mm→pt برای چیدمان آزاد.
        const val MM_TO_PT = PAGE_WIDTH / 210f
        const val HEADER_BOTTOM = 112f
        const val CONTENT_TOP = 125f
        // V63.8 — سربرگ فقط صفحهٔ اول است؛ صفحات بعدی از بالاتر شروع می‌شوند.
        const val LATER_CONTENT_TOP = 50f
        // V62.7 — عرض ثابت سه ستون سربرگ رسمی (راست/وسط/چپ) + ارتفاع سطر.
        const val SIDE_COL_WIDTH = 130f
        const val CENTER_COL_WIDTH = 235f
        const val LEFT_COL_WIDTH = 130f
        const val HEADER_ROW_HEIGHT = 13f
        const val CONTENT_BOTTOM = 795f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        const val CONTENT_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP
        // V69.0 — کلید کش چیدمان متن (خالص و JVM-تست‌پذیر؛ متن آخر است تا «|» داخل
        // متن با فیلدهای دیگر تداخل نکند).
        fun layoutKey(text: String, size: Float, bold: Boolean, italic: Boolean, align: String, fontFamily: String, width: Int): String =
            "$size|${if (bold) 1 else 0}${if (italic) 1 else 0}|$align|${fontFamily.lowercase()}|$width|$text"
        // مقیاس کامل گزینه‌ها در خروجی PDF.
        const val OPTION_SCALE = 1f
        // بدون فاصلهٔ سطر اضافه در چاپ.
        const val LINE_SPACING_ADD_PT = 0f
        // فاصلهٔ سؤال‌ها: ۶mm تبدیل‌شده به pt.
        const val QUESTION_GAP_PT = 6f * MM_TO_PT
        /** برش جریان پیوسته به صفحه‌های A4 روی مرزهای امن. */
        fun computeSlices(
            total: Float,
            boundaries: List<Float>,
            firstContentTop: Float = CONTENT_TOP
        ): List<Pair<Float, Float>> {
            val sorted = boundaries.distinct().sorted()
            val result = mutableListOf<Pair<Float, Float>>()
            var top = 0f
            var first = true
            while (top < total - 0.01f) {
                val cap = if (first) CONTENT_BOTTOM - firstContentTop else CONTENT_BOTTOM - LATER_CONTENT_TOP
                val limit = (top + cap).coerceAtMost(total)
                val end = sorted.lastOrNull { it > top + 0.01f && it <= limit } ?: limit
                if (end <= top + 0.01f) break
                result += top to end
                top = end
                first = false
            }
            return result.ifEmpty { listOf(0f to total.coerceAtLeast(1f)) }
        }

    }
}

private class OfficialPdfRenderer(private val context:Context,private val printable: OfficialPrintable) {
    // جریان محتوای آزمون/کارنامه از موتور چیدمان PDF می‌آید؛ این کلاس
    // سربرگ، پاصفحه و صفحه‌های واقعی PDF را می‌سازد.
    private val engine = OfficialPrintLayoutEngine(context)
    private val MARGIN = OfficialPrintLayoutEngine.MARGIN
    private val PAGE_WIDTH = OfficialPrintLayoutEngine.PAGE_WIDTH
    private val PAGE_HEIGHT = OfficialPrintLayoutEngine.PAGE_HEIGHT
    private val CONTENT_TOP = OfficialPrintLayoutEngine.CONTENT_TOP
    private val LATER_CONTENT_TOP = OfficialPrintLayoutEngine.LATER_CONTENT_TOP

    private val document: OfficialPrintLayoutEngine.EngineDocument = when (printable) {
        is OfficialExamPrintable -> engine.layoutExam(printable)
        is OfficialGradeReportPrintable -> engine.layoutReport(printable)
    }

    // V68.8 needles (ساختار پیوسته برای تست رگرسیون — چیدمان واقعی در موتور):
    private fun placeContinuous(): List<Pair<Any, Float>> = document.placed.map { it.block to it.y }
    private fun slicePages(): List<Pair<Float, Float>> = document.slices
    val pageCount: Int get() = slicePages().size.coerceAtLeast(1)

    fun write(
        destination: ParcelFileDescriptor,
        ranges: Array<out PageRange>,
        cancellation: CancellationSignal
    ): List<PageRange> {
        val pdf = PdfDocument()
        val writtenPages = mutableListOf<Int>()
        try {
            slicePages().forEachIndexed { index, slice ->
                if (cancellation.isCanceled) return@forEachIndexed
                val pageNumber = index + 1
                if (!isPageRequested(index, ranges)) return@forEachIndexed
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                drawSlice(page.canvas, slice, pageNumber, slicePages().size)
                pdf.finishPage(page)
                writtenPages += index
            }
            FileOutputStream(destination.fileDescriptor).use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        return collapseRanges(writtenPages)
    }

    /**
     * V68.8 — هر صفحه یک «برش» از سند پیوسته است؛ V68.9: برش فقط روی مرز
     * خط/بلوک می‌افتد (وسط سطر نصف نمی‌شود) و رسم بلوک‌ها از موتور واحد است.
     */
    private fun drawSlice(canvas: Canvas, slice: Pair<Float, Float>, pageNumber: Int, totalPages: Int) {
        canvas.drawColor(Color.WHITE)
        if (pageNumber == 1) drawHeader(canvas, pageNumber, totalPages)
        val dstTop = if (pageNumber == 1) CONTENT_TOP else LATER_CONTENT_TOP
        val sliceH = (slice.second - slice.first).coerceAtLeast(0f)
        canvas.save()
        canvas.clipRect(MARGIN - 6f, dstTop, PAGE_WIDTH - MARGIN + 6f, dstTop + sliceH)
        canvas.translate(0f, dstTop - slice.first)
        engine.drawFlowWindow(canvas, document, slice)
        canvas.restore()
        drawFooter(canvas, pageNumber, totalPages)
    }

    /**
     * V62.7 — سربرگ رسمی سه‌ستونه طبق طرح کاربر (هر مورد در یک سطر):
     * ۱) فقط آرم وسط، بالاتر از همه.
     * ۲) نام | وزارت آموزش و پرورش جمهوری اسلامی ایران | تاریخ آزمون
     * ۳) نام خانوادگی | اداره کل آموزش و پرورش استان … | مدت آزمون
     * ۴) نام پدر | مدیریت آموزش و پرورش شهر/شهرستان …(…) | پایه
     * ۵) نام درس | نام مدرسه | رشته
     * قالب با هر طول متنی ثابت می‌ماند: سه ستون با عرض ثابت و ellipsize.
     */
    private fun drawHeader(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRect(MARGIN, 22f, PAGE_WIDTH - MARGIN, OfficialPrintLayoutEngine.HEADER_BOTTOM, border)
        val header = printable.header
        // سطر ۱ — آرم وسط.
        emblemBitmap()?.let { emblem ->
            val size = 30f
            canvas.drawBitmap(
                emblem, null,
                android.graphics.RectF(
                    PAGE_WIDTH / 2f - size / 2f, 25f,
                    PAGE_WIDTH / 2f + size / 2f, 25f + size
                ),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
        }
        val rightX = PAGE_WIDTH - MARGIN - 6f
        val leftColRight = MARGIN + OfficialPrintLayoutEngine.LEFT_COL_WIDTH + 6f
        val rows = listOf(
            Triple("نام:", "وزارت آموزش و پرورش جمهوری اسلامی ایران", "تاریخ آزمون: ${header.examDate}"),
            // V62.8 — مدت همیشه با پسوند «دقیقه» (مثلاً: مدت آزمون: 120 دقیقه).
            Triple(
                "نام خانوادگی:",
                "اداره کل آموزش و پرورش استان ${header.province}",
                "مدت آزمون: " + header.examDuration.takeIf(String::isNotBlank)?.let { "$it دقیقه" }.orEmpty()
            ),
            Triple(
                "نام پدر:",
                "مدیریت آموزش و پرورش شهر/شهرستان ${header.city}" +
                    header.district.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty(),
                "پایه: ${header.grade}"
            ),
            Triple("نام درس: ${header.subject}", header.school, "رشته: ${header.fieldOfStudy}")
        )
        var rowTop = 58f
        rows.forEach { (right, center, left) ->
            drawHeaderCell(canvas, right, rightX, rowTop, Paint.Align.RIGHT, OfficialPrintLayoutEngine.SIDE_COL_WIDTH)
            drawHeaderCell(canvas, center, PAGE_WIDTH / 2f, rowTop, Paint.Align.CENTER, OfficialPrintLayoutEngine.CENTER_COL_WIDTH)
            drawHeaderCell(canvas, left, leftColRight, rowTop, Paint.Align.RIGHT, OfficialPrintLayoutEngine.LEFT_COL_WIDTH)
            rowTop += OfficialPrintLayoutEngine.HEADER_ROW_HEIGHT
        }
        val date = JalaliCalendar.fromGregorian(LocalDate.now()).display()
        drawRtl(canvas, "$date · صفحه $pageNumber از $totalPages", 175f, 25f, 7.5f, false, 130)
    }

    /** یک سلول سربرگ: تک‌سطری با برش انتها تا قالب سه‌ستونه هرگز بهم نریزد. */
    private fun drawHeaderCell(canvas: Canvas, text: String, x: Float, top: Float, align: Paint.Align, width: Float) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.6f
            textAlign = align
            typeface = engine.persianTypeface(true)
        }
        val clipped = android.text.TextUtils.ellipsize(text, paint, width, android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(clipped, x, top, paint)
    }

    private fun emblemBitmap(): Bitmap? = emblemCache ?: runCatching {
        context.assets.open("print/emblem.png").use(android.graphics.BitmapFactory::decodeStream)
    }.getOrNull()?.also { emblemCache = it }

    private var emblemCache: Bitmap? = null

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 8.5f; textAlign = Paint.Align.RIGHT }
        // V63.8 — امضای دبیر/مدیر فقط پایان صفحهٔ آخر (درخواست کاربر).
        if (pageNumber == totalPages) canvas.drawText(printable.footerNote, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 25f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("آزمون آنلاین Native · $pageNumber/$totalPages", MARGIN, PAGE_HEIGHT - 25f, paint)
    }

    private fun drawRtl(canvas: Canvas, text: String, right: Float, top: Float, size: Float, bold: Boolean, width: Int) {
        val layout = engine.textLayout(text, size, bold, width)
        canvas.save()
        canvas.translate(right - width, top)
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
}
