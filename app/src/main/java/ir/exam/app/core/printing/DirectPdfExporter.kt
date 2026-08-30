package ir.exam.app.core.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.events.Event
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.BaseDirection
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import ir.exam.app.R
import ir.exam.app.core.figure.AtlasBitmapRenderer
import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureSvgRenderer
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.core.math.NativeMathFormatter
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.ui.builder.StyleSpan
import ir.exam.app.ui.builder.StyleSpanOps
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * V72.0 — خروجی PDF مستقیم با iText 7 for Android.
 *
 * برخلاف چاپ سیستمی که گفت‌وگوی چاپ را باز می‌کند، این کلاس فایل PDF را مستقیم
 * روی URI انتخابی کاربر می‌نویسد. قالب خروجی همان قالب چاپ رسمی قبلی است: A4 با
 * حاشیهٔ 40pt، سربرگ سه‌ستونه با آرم، نوار درس/مدت/بارم، سؤال‌های شماره‌دار،
 * گزینه‌ها، جورکردنی، تصویرهای گالری و سطرهای پاسخ.
 *
 * iText 7 layout برای صفحه‌بندی، جدول و متن استفاده می‌شود و PdfFontFactory با
 * کدگذاری Identity-H فونت فارسی B Nazanin را embed می‌کند. شکل‌دهی حروف عربی
 * پیش از ورود متن به layout با Android ICU انجام می‌شود؛ بنابراین جایگزینی
 * کتابخانه، خروجی فارسی و RTL قبلی را از بین نمی‌برد.
 *
 * خط لولهٔ V71 نیز حفظ شده است: ساخت روی فایل خصوصی مرحله‌ای، نهایی‌سازی
 * PdfDocument، بررسی هدر/EOF/parse/page-count/SHA-256، ثبت با
 * ContentResolver.openOutputStream در SAF و بازخوانی مقصد پیش از اعلام موفقیت.
 */
data class DirectPdfExportReceipt(
    val byteCount: Long,
    val pageCount: Int
) {
    val sizeKiB: Long get() = ((byteCount + 1_023L) / 1_024L).coerceAtLeast(1L)
}

class DirectPdfExporter(private val context: Context) {

    private val appContext = context.applicationContext
    private val imageLoader = ImageLoader(appContext)
    private val verifiedWriter = VerifiedSafPdfWriter(appContext.contentResolver)

    suspend fun export(
        printable: OfficialExamPrintable,
        target: Uri
    ): Result<DirectPdfExportReceipt> = withContext(Dispatchers.IO) {
        var stage: File? = null
        try {
            val withImages = hydrateImages(printable)
            val staged = createStageFile()
            stage = staged
            FileOutputStream(staged).use { output ->
                buildPdf(withImages, output)
                output.flush()
                output.channel.force(true)
                output.fd.sync()
            }

            // موفقیت مرحلهٔ ساخت فقط وقتی پذیرفته می‌شود که iText 7 فایل را
            // بخواند و envelope، تعداد صفحه، اندازه و SHA-256 معتبر باشند.
            val artifact = PdfArtifactVerifier.inspect(staged)
            verifiedWriter.commit(staged, target, artifact)
            Result.success(
                DirectPdfExportReceipt(
                    byteCount = artifact.byteCount,
                    pageCount = artifact.pageCount
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            runCatching { appContext.contentResolver.delete(target, null, null) }
            Result.failure(error)
        } finally {
            stage?.delete()
        }
    }

    private suspend fun hydrateImages(printable: OfficialExamPrintable): OfficialExamPrintable = coroutineScope {
        printable.copy(
            questions = printable.questions.map { question ->
                question.copy(
                    images = question.imageUrls.map { url -> async { loadBitmapSafely(url) } }
                        .awaitAll().filterNotNull()
                )
            }
        )
    }

    private fun createStageFile(): File {
        val directory = File(appContext.cacheDir, "verified-pdf-staging")
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("ساخت فضای موقت امن برای PDF ممکن نشد.")
        }
        val staleBefore = System.currentTimeMillis() - STALE_STAGE_MAX_AGE_MS
        directory.listFiles().orEmpty()
            .filter { it.isFile && it.lastModified() < staleBefore }
            .forEach { it.delete() }
        return File.createTempFile("exam-", ".pdf", directory)
    }

    // ------------------------------------------------------------- محتوا

    private fun buildPdf(printable: OfficialExamPrintable, out: OutputStream) {
        val base = loadPdfFont("fonts/bnazanin.ttf", R.font.vazirmatn_regular)
        val boldBase = loadPdfFont("fonts/bnazanin_bold.ttf", R.font.vazirmatn_bold)
        val writer = PdfWriter(
            out,
            WriterProperties()
                .setFullCompressionMode(true)
                .setCompressionLevel(9)
        )
        val pdf = PdfDocument(writer)
        // Document.close باید xref/EOF را کامل کند، اما stream مرحله‌ای را نبندد
        // تا پس از آن flush + fsync واقعی انجام شود.
        pdf.setCloseWriter(false)
        pdf.documentInfo
            .setTitle(printable.documentTitle.ifBlank { "آزمون" })
            .setSubject(printable.subject.ifBlank { printable.header.subject })
            .setCreator("Native Exam Online · iText 7")
            .addCreationDate()
        printable.header.school.takeIf(String::isNotBlank)?.let(pdf.documentInfo::setAuthor)
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, FooterHandler(loadLatinFont()))

        val document = Document(pdf, PageSize.A4)
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)
        try {
            addHeader(document, printable.header, boldBase)
            addSubject(document, printable, boldBase)
            printable.questions.forEach { question ->
                addQuestion(document, question, printable.includeAnswerKey, base, boldBase)
            }
            // امضای دبیر/مدیر فقط در پایان برگه (همان قالب چاپ).
            if (printable.footerNote.isNotBlank()) {
                document.add(
                    paragraph(TextAlignment.RIGHT, 9f, base, boldBase)
                        .add(styledText(printable.footerNote, base, boldBase, 9f, false, false))
                        .setMarginTop(18f)
                )
            }
        } finally {
            document.close()
        }
    }

    private fun addHeader(
        document: Document,
        header: OfficialPrintHeader,
        boldBase: PdfFont
    ) {
        val table = Table(UnitValue.createPointArray(floatArrayOf(SIDE_COL_WIDTH, CENTER_COL_WIDTH, LEFT_COL_WIDTH)), true)
            .setFixedLayout()
            .setWidth(CONTENT_WIDTH)

        // سطر آرم وسط (همان print/emblem.png).
        val emblemCell = Cell(1, 3)
            .setBorder(Border.NO_BORDER)
            .setPadding(0f)
            .setHeight(36f)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
        runCatching {
            Image(ImageDataFactory.create(appContext.assets.open("print/emblem.png").use { it.readBytes() }))
                .scaleToFit(30f, 30f)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
        }.onSuccess { emblemCell.add(it) }
        table.addCell(emblemCell)

        val rows = listOf(
            Triple("نام:", "وزارت آموزش و پرورش جمهوری اسلامی ایران", "تاریخ آزمون: ${header.examDate}"),
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
        rows.forEach { (right, center, left) ->
            table.addCell(headerCell(right, boldBase, TextAlignment.RIGHT, SIDE_COL_WIDTH))
            table.addCell(headerCell(center, boldBase, TextAlignment.CENTER, CENTER_COL_WIDTH))
            table.addCell(headerCell(left, boldBase, TextAlignment.RIGHT, LEFT_COL_WIDTH))
        }
        document.add(table)
    }

    private fun headerCell(
        value: String,
        font: PdfFont,
        align: TextAlignment,
        width: Float
    ): Cell {
        val fitted = ellipsize(sh(value), font, 8.6f, width - 3f)
        return Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(1.5f)
            .setTextAlignment(align)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .add(
                paragraph(align, 8.6f, font, font)
                    .add(styledText(fitted, font, font, 8.6f, false, false))
            )
    }

    private fun addSubject(document: Document, printable: OfficialExamPrintable, boldBase: PdfFont) {
        val text = "درس: ${printable.subject.ifBlank { "—" }}" +
            "     مدت: ${printable.durationMinutes} دقیقه" +
            "     بارم: ${formatScore(printable.totalScore)}"
        addBoxedLine(document, text, boldBase, 11f, TextAlignment.RIGHT)
    }

    private fun addBoxedLine(
        document: Document,
        value: String,
        font: PdfFont,
        size: Float,
        align: TextAlignment
    ) {
        val cell = Cell()
            .setBorder(SolidBorder(0.7f))
            .setPadding(4f)
            .setTextAlignment(align)
            .add(
                paragraph(align, size, font, font)
                    .add(styledText(value, font, font, size, false, false))
            )
        document.add(Table(1).setFixedLayout().setWidth(CONTENT_WIDTH).addCell(cell))
    }

    private fun addQuestion(
        document: Document,
        q: OfficialPrintQuestion,
        includeKey: Boolean,
        base: PdfFont,
        boldBase: PdfFont
    ) {
        val size = q.fontSizeSp.coerceIn(8f, 30f)
        addBoxedLine(
            document,
            "سؤال ${q.number}     (${formatScore(q.score)} نمره)",
            boldBase,
            size,
            alignmentFor(q.textAlign)
        )
        addQuestionText(document, q, size, base, boldBase)
        addOptions(document, q, base, boldBase)
        addMatching(document, q, size, base, boldBase)
        addGalleryImages(document, q)
        addAnswer(document, q, includeKey, base, boldBase)
        document.add(Paragraph(" ").setFixedLeading(10f))
    }

    private fun addQuestionText(
        document: Document,
        q: OfficialPrintQuestion,
        size: Float,
        base: PdfFont,
        boldBase: PdfFont
    ) {
        val formulas = FormulaTextCodec.occurrences(q.text)
        val figures = FigureCodec.occurrences(q.text)
        val segments = RichTextSplitter.split(q.text, formulas, figures)
        val ranges = RichTextSplitter.segmentSourceRanges(segments, formulas, figures)
        val spans = q.textSpans.map { StyleSpan(it.start, it.end, it.bold, it.italic) }
        val align = alignmentFor(q.textAlign)
        var current = paragraph(align, size, base, base)
        fun flush() {
            if (!current.isEmpty()) document.add(current)
            current = paragraph(align, size, base, base)
        }
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is RichSegment.Text -> segment.text.split('\n').forEachIndexed { partIndex, part ->
                    if (partIndex > 0) flush()
                    if (part.isNotEmpty()) {
                        val offset = ranges.getOrNull(index)?.first ?: 0
                        StyleSpanOps.splitBySpans(part, offset, spans).forEach { (piece, bold, italic) ->
                            current.add(styledText(piece.replace("\\$", "$"), base, boldBase, size, bold, italic))
                        }
                    }
                }
                is RichSegment.Math -> current.add(
                    styledText(
                        NativeMathFormatter.renderTex(segment.tex),
                        base,
                        boldBase,
                        size,
                        q.bold,
                        q.italic
                    )
                )
                is RichSegment.Figure -> {
                    flush()
                    val bitmap = figureBitmap(segment.spec)
                    if (bitmap != null) {
                        document.add(imageParagraph(bitmap, figureWidthPt(segment.spec)))
                    } else {
                        document.add(
                            paragraph(TextAlignment.RIGHT, size, base, base)
                                .add(styledText("[شکل]", base, boldBase, size, false, false))
                        )
                    }
                }
            }
        }
        flush()
    }

    private fun addOptions(document: Document, q: OfficialPrintQuestion, base: PdfFont, boldBase: PdfFont) {
        q.options.forEachIndexed { index, option ->
            val style = q.optionStyles.getOrNull(index)
            val size = (style?.third ?: q.fontSizeSp).coerceIn(8f, 30f)
            val bold = style?.first ?: false
            val italic = style?.second ?: false
            val optionParagraph = paragraph(alignmentFor(q.textAlign), size, base, base)
            optionParagraph.add(styledText("${index + 1}) ", base, boldBase, size, true, false))
            NativeMathFormatter.segments(option).forEach { segment ->
                if (segment.math) {
                    optionParagraph.add(
                        styledText(
                            NativeMathFormatter.renderTex(segment.text),
                            base,
                            boldBase,
                            size,
                            bold,
                            italic
                        )
                    )
                } else {
                    optionParagraph.add(
                        styledText(segment.text.replace("\\$", "$"), base, boldBase, size, bold, italic)
                    )
                }
            }
            document.add(optionParagraph)
        }
    }

    private fun addMatching(
        document: Document,
        q: OfficialPrintQuestion,
        size: Float,
        base: PdfFont,
        boldBase: PdfFont
    ) {
        val rows = maxOf(q.matchingLeft.size, q.matchingRight.size)
        if (rows == 0) return
        repeat(rows) { rowIndex ->
            val right = q.matchingRight.getOrNull(rowIndex).orEmpty()
            val left = q.matchingLeft.getOrNull(rowIndex).orEmpty()
            val rightStyle = q.matchingRightStyles.getOrNull(rowIndex)
            val leftStyle = q.matchingLeftStyles.getOrNull(rowIndex)
            val table = Table(
                UnitValue.createPointArray(floatArrayOf(CONTENT_WIDTH / 2f - 12f, 24f, CONTENT_WIDTH / 2f - 12f)),
                true
            ).setFixedLayout().setWidth(CONTENT_WIDTH)
            table.addCell(
                matchingCell(
                    NativeMathFormatter.renderText(right),
                    base,
                    boldBase,
                    size,
                    rightStyle?.first ?: false,
                    rightStyle?.second ?: false,
                    TextAlignment.RIGHT
                )
            )
            table.addCell(
                matchingCell("↔", base, boldBase, size, false, false, TextAlignment.CENTER)
            )
            table.addCell(
                matchingCell(
                    NativeMathFormatter.renderText(left),
                    base,
                    boldBase,
                    size,
                    leftStyle?.first ?: false,
                    leftStyle?.second ?: false,
                    TextAlignment.RIGHT
                )
            )
            document.add(table)
        }
    }

    private fun matchingCell(
        value: String,
        base: PdfFont,
        boldBase: PdfFont,
        size: Float,
        bold: Boolean,
        italic: Boolean,
        align: TextAlignment
    ): Cell = Cell()
        .setBorder(Border.NO_BORDER)
        .setPadding(1f)
        .setTextAlignment(align)
        .add(
            paragraph(align, size, base, base)
                .add(styledText(value, base, boldBase, size, bold, italic))
        )

    private fun addGalleryImages(document: Document, q: OfficialPrintQuestion) {
        q.images.forEach { bitmap -> document.add(imageParagraph(bitmap, CONTENT_WIDTH)) }
    }

    private fun addAnswer(
        document: Document,
        q: OfficialPrintQuestion,
        includeKey: Boolean,
        base: PdfFont,
        boldBase: PdfFont
    ) {
        if (includeKey && !q.answerText.isNullOrBlank()) {
            val answer = q.answerText.orEmpty()
            document.add(
                paragraph(TextAlignment.RIGHT, 10.5f, base, base)
                    .add(
                        styledText(
                            "پاسخ: ${NativeMathFormatter.renderText(answer)}",
                            base,
                            boldBase,
                            10.5f,
                            true,
                            false
                        )
                    )
            )
        } else {
            val dots = if (q.answerLineStyle == "blank") " " else ".".repeat(100)
            repeat(q.answerLines.coerceIn(0, 12)) {
                document.add(
                    paragraph(TextAlignment.RIGHT, 9f, base, base)
                        .add(styledText(dots, base, boldBase, 9f, false, false))
                )
            }
        }
    }

    // ------------------------------------------------------------- کمکی‌ها

    private fun paragraph(
        align: TextAlignment,
        size: Float,
        base: PdfFont,
        paragraphFont: PdfFont
    ): Paragraph = Paragraph()
        .setTextAlignment(align)
        .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
        .setFont(paragraphFont)
        .setFontSize(size)
        .setFixedLeading(size * 1.35f)
        .setMargin(0f)

    private fun styledText(
        value: String,
        base: PdfFont,
        boldBase: PdfFont,
        size: Float,
        bold: Boolean,
        italic: Boolean
    ): Text {
        val text = Text(sh(value))
            .setFont(if (bold) boldBase else base)
            .setFontSize(size)
            .setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
        if (italic) text.setItalic()
        return text
    }

    private fun imageParagraph(bitmap: Bitmap, maxWidth: Float): Paragraph {
        val image = Image(ImageDataFactory.create(bitmapToPng(bitmap)))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
        if (image.imageWidth > maxWidth) image.scaleToFit(maxWidth, 480f)
        return Paragraph()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(4f)
            .setMarginBottom(4f)
            .add(image)
    }

    /** شکل‌دهی فارسی پیش از layout؛ bidi و wrapping نهایی را iText 7 انجام می‌دهد. */
    private fun sh(text: String): String = PersianTextShaper.shape(text)

    private fun alignmentFor(align: String): TextAlignment = when (align) {
        "center" -> TextAlignment.CENTER
        "left" -> TextAlignment.LEFT
        else -> TextAlignment.RIGHT
    }

    private fun figureWidthPt(spec: FigureSpec): Float =
        (WordPageLayout.figureWidthMm(spec) * (PAGE_WIDTH / 210f)).coerceIn(60f, CONTENT_WIDTH)

    private fun formatScore(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    private fun ellipsize(value: String, font: PdfFont, size: Float, width: Float): String {
        if (font.getWidth(value, size) <= width) return value
        var end = value.length
        while (end > 1 && font.getWidth(value.substring(0, end) + "…", size) > width) end--
        return value.substring(0, end) + "…"
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun loadPdfFont(asset: String, resFallback: Int): PdfFont {
        val assetBytes = runCatching { appContext.assets.open(asset).use { it.readBytes() } }.getOrNull()
        val resourceBytes = runCatching {
            appContext.resources.openRawResource(resFallback).use { it.readBytes() }
        }.getOrNull()
        return runCatching {
            PdfFontFactory.createFont(
                assetBytes ?: resourceBytes ?: throw IOException("فونت PDF موجود نیست: $asset"),
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )
        }.getOrElse { loadLatinFont() }
    }

    private fun loadLatinFont(): PdfFont =
        runCatching { PdfFontFactory.createFont(StandardFonts.HELVETICA) }
            .getOrElse { throw IllegalStateException("فونت پایهٔ iText 7 ساخته نشد.", it) }

    private suspend fun loadBitmapSafely(url: String): Bitmap? = try {
        loadBitmap(url)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        if (!url.startsWith("https://", true)) return null
        val request = ImageRequest.Builder(appContext)
            .data(url)
            .allowHardware(false)
            .size(1600, 1600)
            .build()
        val result = imageLoader.execute(request)
        return (result as? SuccessResult)?.drawable?.toBitmap()
    }

    // V69.0 — کش LRU بیت‌مپ شکل‌ها (همان کلاس کش موتور، بدون وابستگی اندروید).
    private val figureCache = LruCacheK<Bitmap>(
        maxBytes = 16L * 1024L * 1024L,
        sizeOf = { bmp -> (bmp.width.toLong() * bmp.height.toLong() * 4L).coerceAtLeast(4096L) }
    )

    /** V53.1 — رندر برداری شکل/نمودار/جدول به bitmap (AndroidSVG، بدون WebView). */
    private fun figureBitmap(spec: FigureSpec): Bitmap? = figureCache[spec.raw.toString()] ?: runCatching {
        if (spec.kind in setOf("a", "s")) {
            AtlasBitmapRenderer.render(appContext, spec)
        } else {
            val rendered = FigureSvgRenderer.render(spec)
            val svg = com.caverock.androidsvg.SVG.getFromString(rendered.xml)
            val scale = 2f
            val width = (rendered.widthPx * scale).roundToInt().coerceAtLeast(1)
            val height = (rendered.heightPx * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            svg.documentWidth = width.toFloat()
            svg.documentHeight = height.toFloat()
            svg.renderToCanvas(canvas)
            bitmap
        }
    }.getOrNull()?.also { figureCache.put(spec.raw.toString(), it) }

    /** پانوشت هر صفحه: فقط شمارهٔ صفحه (لاتین با فونت استاندارد iText 7). */
    private class FooterHandler(
        private val latin: PdfFont
    ) : IEventHandler {
        override fun handleEvent(event: Event) {
            val pageEvent = event as? PdfDocumentEvent ?: return
            val pdf = pageEvent.document
            val page = pageEvent.page
            val canvas = PdfCanvas(page.newContentStreamAfter(), page.resources, pdf)
            canvas.beginText()
                .setFontAndSize(latin, 8f)
                .setTextMatrix(MARGIN, 28f)
                .showText("Native Exam Online · ${pdf.getPageNumber(page)}")
                .endText()
            canvas.release()
        }
    }

    companion object {
        const val PAGE_WIDTH = 595f
        const val MARGIN = 40f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2f
        const val SIDE_COL_WIDTH = 130f
        const val CENTER_COL_WIDTH = 235f
        const val LEFT_COL_WIDTH = 130f
        private const val STALE_STAGE_MAX_AGE_MS = 24L * 60L * 60L * 1_000L
    }
}
