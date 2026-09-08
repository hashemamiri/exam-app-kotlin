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
        val imageXmm: Float = 20f,
        val imageYmm: Float = 30f,
        val boxed: Boolean = false,
        val spacingAfter: Float = 6f,
        val styledText: android.text.SpannableStringBuilder? = null,
        val matchRight: String? = null,
        val matchLeft: String? = null,
        val matchRightStyle: Triple<Boolean, Boolean, Float?>? = null,
        val matchLeftStyle: Triple<Boolean, Boolean, Float?>? = null
    )
    data class Placed(val block: RenderBlock, val y: Float, val height: Float)

    /** جریان آماده برای برش صفحه‌های PDF. */
    class EngineDocument(
        val placed: List<Placed>,
        val slices: List<Pair<Float, Float>>,
        val layouts: Map<Int, StaticLayout>
    )

    // ---------------------------------------------------------------- ساخت

    /** آزمون رسمی: سؤال‌ها → جریان پیوسته و صفحه‌های PDF. */
    fun layoutExam(printable: OfficialExamPrintable): EngineDocument = build(examBlocks(printable))

    /** کارنامه: همان موتور با بلوک‌های کارنامه. */
    fun layoutReport(report: OfficialGradeReportPrintable): EngineDocument =
        build(reportBlocks(report))

    private fun build(blocks: List<RenderBlock>): EngineDocument {
        val placed = placeAll(blocks)
        val total = (placed.lastOrNull()?.let { it.y + it.height } ?: 1f).coerceAtLeast(1f)
        val layouts = HashMap<Int, StaticLayout>()
        // مرزهای مجاز برش: انتهای هر بلوک و انتهای هر سطر متن. در نتیجه صفحه
        // تا جای ممکن از وسط سطر قطع نمی‌شود.
        val boundaries = sortedSetOf(0f)
        placed.forEachIndexed { index, placedBlock ->
            val layout = blockLayout(placedBlock.block)
            if (layout != null) {
                layouts[index] = layout
                for (line in 0 until layout.lineCount) boundaries.add(placedBlock.y + layout.getLineBottom(line))
            }
            boundaries.add(placedBlock.y + placedBlock.height)
        }
        return EngineDocument(
            placed = placed,
            slices = computeSlices(total, boundaries.toList()),
            layouts = layouts
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
        add(RenderBlock(text="درس: ${exam.subject}     مدت: ${exam.durationMinutes} دقیقه     بارم: ${formatScore(exam.totalScore)}",textSize=11f,bold=true,boxed=true))
        exam.questions.forEach { question ->
            // V68.4.1 — اندیس شروع بلوک‌های همین سؤال: برای تبدیل fy مطلقِ شکلِ
            // آزاد (از بالای بلوک) به آفست از جایگاه جریان خودش در چاپ.
            val qStart = size
            add(RenderBlock(text="سؤال ${question.number}     (${formatScore(question.score)} نمره)",textSize=question.fontSizeSp.coerceIn(8f,30f),bold=true,boxed=true,fontFamily=question.fontFamily,align=question.textAlign))
            // V53.1 — شکل/نمودار/جدول درون‌متنی (%%FIG%%) به‌جای JSON خام،
            // به‌صورت تصویر برداری در PDF رندر می‌شوند؛ فرمول‌ها مثل قبل.
            // V68 — بازهٔ آفست هر قطعه برای استایل تکه‌ای متن.
            val __formulas = ir.exam.app.core.math.FormulaTextCodec.occurrences(question.text)
            val __figures = ir.exam.app.core.figure.FigureCodec.occurrences(question.text)
            val __segments = RichTextSplitter.split(question.text)
            val __ranges = RichTextSplitter.segmentSourceRanges(__segments, __formulas, __figures)
            // استایل‌های تکه‌ای مستقیماً از مدل چاپ خوانده می‌شوند؛ موتور PDF
            // به مدل یا ابزارهای رابط کاربری وابسته نیست.
            val __spans = question.textSpans
            // متن و فرمول‌های پیوستهٔ سؤال در یک سطر جاری کنار هم می‌نشینند.
            // فرمول به‌صورت MathReplacementSpan روی جای‌نگهدار U+FFFC می‌نشیند و
            // StaticLayout آن را در همان سطر جریان می‌دهد؛ شکل‌های آزاد بلوک‌های
            // مستقل در جریان PDF هستند.
            var __inline = android.text.SpannableStringBuilder()
            var __inlineLen = 0
            fun __flushInline() {
                if (__inline.isEmpty()) return
                add(RenderBlock(styledText=__inline,textSize=question.fontSizeSp.coerceIn(8f,30f),bold=question.bold,italic=question.italic,align=question.textAlign,fontFamily=question.fontFamily))
                __inline = android.text.SpannableStringBuilder()
                __inlineLen = 0
            }
            __segments.forEachIndexed { segIndex, rich ->
                when (rich) {
                    is RichSegment.Math -> {
                        // V68.6 — فرمول درون‌خطی: جای‌نگهدار یک‌کاراکتری + span رندر.
                        // اگر پاراگراف با فرمول شروع شود، U+FFFC نخستین کاراکترِ
                        // «قوی» و LTR است و جهتِ FIRSTSTRONG را می‌چرخاند؛ یک
                        // RLM نامرئی (پهنای صفر) اولِ پاراگراف جهتِ راست‌به‌چپ
                        // متن فارسی را تثبیت می‌کند.
                        if (__inline.isEmpty()) { __inline.append('\u200F'); __inlineLen += 1 }
                        __inline.append('\uFFFC')
                        __inline.setSpan(
                            MathReplacementSpan(NativeMathParser.parse(rich.tex)),
                            __inlineLen, __inlineLen + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        __inlineLen += 1
                    }
                    is RichSegment.Figure -> {
                        val figPos = PrintFigureMetrics.figurePosMm(rich.spec)
                        val bmp = figureBitmap(rich.spec)
                        if (bmp != null) {
                            if (figPos != null) {
                                // V68.4 — شکلِ آزاد: مثل تصویر گالری آزاد، در
                                // جایگاه مطلق چاپ می‌شود (با تبدیل flowPt→mm).
                                __flushInline()
                                val flowPt = (qStart until size).fold(0f) { acc, i -> acc + measureBlock(this[i]) }
                                add(RenderBlock(
                                    image=bmp,
                                    imageWidthMm=PrintFigureMetrics.figureWidthMm(rich.spec),
                                    imagePosition=if (figPos != null) "free" else "below",
                                    imageXmm=figPos.first,
                                    imageYmm=figPos.second - flowPt * (210f / PAGE_WIDTH)
                                ))
                            } else {
                                // شکلِ درون‌متنیِ غیرآزاد در همان پاراگراف جاری،
                                // کنار متن و فرمول می‌نشیند، نه به‌صورت بلوک جدا.
                                if (__inline.isEmpty()) { __inline.append('\u200F'); __inlineLen += 1 }
                                __inline.append('\uFFFC')
                                __inline.setSpan(
                                    FigureReplacementSpan(bmp, PrintFigureMetrics.figureWidthMm(rich.spec)),
                                    __inlineLen, __inlineLen + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                __inlineLen += 1
                            }
                        } else {
                            __flushInline()
                            add(RenderBlock(text="[شکل]",textSize=question.fontSizeSp.coerceIn(8f,30f)))
                        }
                    }
                    is RichSegment.Text -> if (rich.text.isNotEmpty()) {
                        // استایل‌های بازه‌ای بولد/ایتالیک با Spannable اعمال می‌شوند.
                        // V68.6 — الحاق به پاراگراف درون‌خطی با شیفت آفست استایل‌ها؛
                        // تکه‌های فقط-فاصله هم حفظ می‌شوند (جداکنندهٔ دو فرمول).
                        val segStart = __ranges.getOrNull(segIndex)?.first ?: 0
                        val overlapping = __spans.any { it.end > segStart && it.start < segStart + rich.text.length }
                        val pieceText = rich.text.replace("\\$","$")
                        __inline.append(pieceText)
                        if (overlapping) {
                            var off = __inlineLen
                            PrintTextSpanSegments.split(rich.text, segStart, __spans)
                                .forEach { piece ->
                                    val a = off
                                    val b = off + piece.first.length
                                    off = b
                                    if (piece.second) __inline.setSpan(
                                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                        a, b, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    if (piece.third) __inline.setSpan(
                                        android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                                        a, b, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                        }
                        __inlineLen += pieceText.length
                    }
                }
            }
            __flushInline()
            question.options.forEachIndexed { index, option ->
                // V64.4 — استایل مستقل هر گزینه در چاپ؛ بدون استایل = مثل قبل.
                val optionStyle = question.optionStyles.getOrNull(index)
                // اندازهٔ گزینه با مقیاس کامل چاپ می‌شود و شمارهٔ آن بولد است.
                val optionSize = (optionStyle?.third ?: question.fontSizeSp) * OPTION_SCALE
                val optionBold = optionStyle?.first ?: false
                val optionItalic = optionStyle?.second ?: false
                // V68.6 — گزینه هم پاراگراف درون‌خطی: متن و فرمول گزینه در یک
                // سطر جاری کنار هم (مثل سؤال)؛ شمارهٔ گزینه بولد (V68.9).
                val __opt = android.text.SpannableStringBuilder()
                var __optLen = 0
                var optionPrefixLeft = "${index + 1}) ".length
                NativeMathFormatter.segments("${index+1}) $option").forEach { segment ->
                    if (segment.math) {
                        __opt.append('\uFFFC')
                        __opt.setSpan(
                            MathReplacementSpan(NativeMathParser.parse(segment.text)),
                            __optLen, __optLen + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        __optLen += 1
                    } else if (segment.text.isNotEmpty()) {
                        val boldTake = optionPrefixLeft.coerceIn(0, segment.text.length)
                        __opt.append(segment.text)
                        if (boldTake > 0) __opt.setSpan(
                            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            __optLen, __optLen + boldTake, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        optionPrefixLeft -= boldTake
                        __optLen += segment.text.length
                    }
                }
                if (__opt.isNotEmpty()) add(RenderBlock(styledText=__opt,textSize=optionSize,
                    bold=optionBold,italic=optionItalic,
                    align=question.textAlign,fontFamily=question.fontFamily))
            }
            // ردیف‌های جورکردنی در چاپ رسمی: آیتم راست در نیمهٔ راست، «↔» در
            // وسط و آیتم چپ در نیمهٔ چپ قرار می‌گیرند.
            val __matchRows = maxOf(question.matchingLeft.size, question.matchingRight.size)
            repeat(__matchRows) { rowIndex ->
                add(RenderBlock(
                    matchRight=question.matchingRight.getOrNull(rowIndex),
                    matchLeft=question.matchingLeft.getOrNull(rowIndex),
                    matchRightStyle=question.matchingRightStyles.getOrNull(rowIndex),
                    matchLeftStyle=question.matchingLeftStyles.getOrNull(rowIndex),
                    textSize=question.fontSizeSp.coerceIn(8f,30f),bold=question.bold,italic=question.italic,align=question.textAlign,fontFamily=question.fontFamily
                ))
            }
            // تصویر گالری با مختصات پیش‌فرض ۲۰/۳۰ هنوز جابه‌جا نشده است؛ آن را
            // در جریان طبیعیِ وسط نگه می‌داریم تا با آزادشدن تصویر دیگر به چپ
            // نپرد.
            // V120 — به‌جای خواندنِ مستقیمِ لیست‌های خامِ imageXmm/imageYmm/
            // imageWidthsMm (که قبلاً اگر تولیدکننده‌شان هم‌طول با imageUrls
            // نمی‌ساخت، اندیس‌ها بی‌صدا جابه‌جا می‌شدند)، از توابعِ کمکیِ
            // هم‌طول‌سازِ مدل استفاده می‌شود.
            val safeWidths = question.safeImageWidthsMm()
            val safeX = question.safeImageXmm()
            val safeY = question.safeImageYmm()
            question.images.forEachIndexed { index,image ->
                val rawX = safeX.getOrElse(index) { 20f }
                val rawY = safeY.getOrElse(index) { 30f }
                val isDefault = rawX == 20f && rawY == 30f
                val pos = if (question.imagePosition == "free" && isDefault) "below" else question.imagePosition
                add(RenderBlock(
                    image=image,boxed=true,imagePosition=pos,
                    imageWidthMm=safeWidths.getOrElse(index) { 80f },
                    imageXmm=rawX, imageYmm=rawY
                ))
            }
            if(exam.includeAnswerKey&&!question.answerText.isNullOrBlank())add(RenderBlock(text="پاسخ: ${NativeMathFormatter.renderText(question.answerText)}",textSize=10.5f,bold=true,fontFamily=question.fontFamily))
            else repeat(question.answerLines.coerceIn(0,12)) {
                add(RenderBlock(
                    text=if(question.answerLineStyle=="blank") " " else "................................................................................................................",
                    textSize=9f
                ))
            }
            // فاصلهٔ ثابت بین سؤال‌ها در جریان چاپ.
            add(RenderBlock(text="",spacingAfter=QUESTION_GAP_PT))
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
            if (placedBlock.y + placedBlock.height > slice.first && placedBlock.y < slice.second) {
                drawBlockAt(canvas, placedBlock.block, placedBlock.y, placedBlock.height, document.layouts[index])
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
        if (block.boxed) {
            canvas.drawRoundRect(
                MARGIN - 3f, y - 2f, PAGE_WIDTH - MARGIN + 3f, y + height - block.spacingAfter,
                5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    /** مستطیل واقعی تصویر در صفحهٔ PDF. */
    private fun imageRectPt(block: RenderBlock, top: Float): android.graphics.RectF? {
        val bitmap = block.image ?: return null
        val targetWidth=(block.imageWidthMm/210f*PAGE_WIDTH).coerceIn(40f,CONTENT_WIDTH-12f)
        val scale=minOf(targetWidth/bitmap.width,220f/bitmap.height,1f)
        val width=bitmap.width*scale;val height=bitmap.height*scale
        val left=when(block.imagePosition){"right"->PAGE_WIDTH-MARGIN-width;"left"->MARGIN;"free"->MARGIN+(block.imageXmm*MM_TO_PT).coerceIn(0f,CONTENT_WIDTH-width);else->MARGIN+(CONTENT_WIDTH-width)/2f}
        // V68.8 — سند پیوسته: y آزاد نسبت به جای جریان خودش بدون clamp به یک
        // صفحه (سند بلند است)؛ کف فقط ۰ تا از بالای سند بیرون نرود.
        val y=if(block.imagePosition=="free")
            (top+block.imageYmm*MM_TO_PT).coerceAtLeast(0f)
        else top+3f
        return android.graphics.RectF(left,y,left+width,y+height)
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
            val targetWidth=(block.imageWidthMm/210f*PAGE_WIDTH).coerceIn(40f,CONTENT_WIDTH-12f)
            val scale=minOf(targetWidth/image.width,220f/image.height,1f)
            return image.height*scale+block.spacingAfter+8f
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

    /** اندازهٔ هدف یک شکل در صفحهٔ PDF. */
    private fun figureTargetSizePt(bitmap: Bitmap, widthMm: Float): Pair<Float, Float> {
        val targetWidth = (widthMm / 210f * PAGE_WIDTH).coerceIn(40f, CONTENT_WIDTH - 12f)
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
        private val widthMm: Float
    ) : ReplacementSpan() {
        private fun targetSize(): Pair<Float, Float> = figureTargetSizePt(bitmap, widthMm)

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
        fun computeSlices(total: Float, boundaries: List<Float>): List<Pair<Float, Float>> {
            val sorted = boundaries.distinct().sorted()
            val result = mutableListOf<Pair<Float, Float>>()
            var top = 0f
            var first = true
            while (top < total - 0.01f) {
                val cap = if (first) CONTENT_BOTTOM - CONTENT_TOP else CONTENT_BOTTOM - LATER_CONTENT_TOP
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
