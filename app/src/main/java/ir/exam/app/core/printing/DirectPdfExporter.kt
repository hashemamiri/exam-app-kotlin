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
import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.pdf.PersianTextShaper
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
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * V70.0 — خروجی PDF «مستقیم» با openPDF (فورک آزاد iText 5 — همان کتابخانهٔ
 * اپ قدیمی؛ LGPL/MPL، سازگار با اپ بسته). برخلاف چاپ سیستمی که گفتگوی چاپ
 * را باز می‌کند، این کلاس فایل PDF را مستقیم روی URI انتخابی کاربر می‌نویسد.
 *
 * قالب خروجی همان قالب چاپ رسمی است: A4 با حاشیهٔ 40pt، سربرگ سه‌ستونهٔ
 * رسمی با آرم، نوار درس/مدت/بارم، سؤال‌های شماره‌دار، گزینه‌ها، جورکردنی،
 * تصویرهای گالری و سطرهای پاسخ؛ فونت فارسی B Nazanin از
 * `assets/fonts/bnazanin.ttf` (و در نبود آن وزیرمتن) با کدگذاری Identity-H.
 *
 * V70.1 — رفع خطای کامپایل در openPDF 1.3.43 (خاصیت isUseAscender به‌جای
 * useAscender)، نگه‌داشتن پسوند .ttf در نام فونت (تا مسیر یونیکد انتخاب
 * شود) و شکل‌نویسی فارسی با PersianTextShaper (هم‌ارز majorBidi اپ قدیمی).
 */
class DirectPdfExporter(private val context: Context) {

    private val appContext = context.applicationContext
    private val imageLoader = ImageLoader(appContext)

    suspend fun export(printable: OfficialExamPrintable, target: Uri): Result<Unit> {
        val withImages = coroutineScope {
            printable.copy(
                questions = printable.questions.map { question ->
                    question.copy(
                        images = question.imageUrls.map { url -> async { loadBitmap(url) } }
                            .awaitAll().filterNotNull()
                    )
                }
            )
        }
        return runCatching {
            val stream: OutputStream = appContext.contentResolver.openOutputStream(target)
                ?: error("نوشتن در محل انتخاب‌شده ممکن نشد.")
            stream.use { buildPdf(withImages, it) }
        }
    }

    // ------------------------------------------------------------- محتوا

    private fun buildPdf(printable: OfficialExamPrintable, out: OutputStream) {
        val base = loadBaseFont("fonts/bnazanin.ttf", R.font.vazirmatn_regular)
            ?: BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
        val boldBase = loadBaseFont("fonts/bnazanin_bold.ttf", R.font.vazirmatn_bold) ?: base
        val document = Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)
        val writer = PdfWriter.getInstance(document, out)
        writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL)
        writer.setPageEvent(FooterHelper())
        document.open()
        try {
            addHeader(document, printable.header, base, boldBase)
            addSubject(document, printable, boldBase)
            printable.questions.forEach { question ->
                addQuestion(document, question, printable.includeAnswerKey, base, boldBase)
            }
            // امضای دبیر/مدیر فقط در پایان برگه (همان قالب چاپ).
            if (printable.footerNote.isNotBlank()) {
                val note = Paragraph(sh(printable.footerNote), font(base, boldBase, 9f, false, false))
                note.alignment = Element.ALIGN_RIGHT
                note.spacingBefore = 18f
                document.add(note)
            }
        } finally {
            document.close()
        }
    }

    private fun addHeader(document: Document, header: OfficialPrintHeader, base: BaseFont, boldBase: BaseFont) {
        val table = PdfPTable(3)
        table.setTotalWidth(floatArrayOf(SIDE_COL_WIDTH, CENTER_COL_WIDTH, LEFT_COL_WIDTH))
        table.setLockedWidth(true)
        table.defaultCell.border = Rectangle.NO_BORDER
        table.defaultCell.setPadding(1.5f)
        table.defaultCell.isUseAscender = true

        // سطر آرم وسط (همان print/emblem.png).
        val emblem = runCatching {
            Image.getInstance(appContext.assets.open("print/emblem.png").use { it.readBytes() })
        }.getOrNull()
        emblem?.scaleToFit(30f, 30f)
        val emblemCell = if (emblem != null) PdfPCell(emblem, false) else PdfPCell()
        emblemCell.border = Rectangle.NO_BORDER
        emblemCell.horizontalAlignment = Element.ALIGN_CENTER
        emblemCell.verticalAlignment = Element.ALIGN_MIDDLE
        emblemCell.colspan = 3
        emblemCell.fixedHeight = 36f
        emblemCell.setPadding(0f)
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
            table.addCell(headerCell(right, boldBase, Element.ALIGN_RIGHT, SIDE_COL_WIDTH))
            table.addCell(headerCell(center, boldBase, Element.ALIGN_CENTER, CENTER_COL_WIDTH))
            table.addCell(headerCell(left, boldBase, Element.ALIGN_RIGHT, LEFT_COL_WIDTH))
        }
        document.add(table)
    }

    private fun headerCell(text: String, base: BaseFont, align: Int, width: Float): PdfPCell {
        val f = Font(base, 8.6f, Font.NORMAL)
        val cell = PdfPCell(Paragraph(ellipsize(sh(text), f, width - 3f), f))
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = align
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.setPadding(1.5f)
        cell.isUseAscender = true
        return cell
    }

    private fun addSubject(document: Document, printable: OfficialExamPrintable, boldBase: BaseFont) {
        val text = "درس: ${printable.subject.ifBlank { "—" }}" +
            "     مدت: ${printable.durationMinutes} دقیقه" +
            "     بارم: ${formatScore(printable.totalScore)}"
        addBoxedLine(document, text, Font(boldBase, 11f, Font.NORMAL), Element.ALIGN_RIGHT)
    }

    private fun addBoxedLine(document: Document, text: String, font: Font, align: Int) {
        val table = PdfPTable(1)
        table.widthPercentage = 100f
        val cell = PdfPCell(Paragraph(sh(text), font))
        cell.border = Rectangle.BOX
        cell.borderWidth = 0.7f
        cell.horizontalAlignment = align
        cell.setPadding(4f)
        cell.isUseAscender = true
        table.addCell(cell)
        document.add(table)
    }

    private fun addQuestion(
        document: Document,
        q: OfficialPrintQuestion,
        includeKey: Boolean,
        base: BaseFont,
        boldBase: BaseFont
    ) {
        val size = q.fontSizeSp.coerceIn(8f, 30f)
        addBoxedLine(
            document,
            "سؤال ${q.number}     (${formatScore(q.score)} نمره)",
            Font(boldBase, size, Font.NORMAL),
            alignmentFor(q.textAlign)
        )
        addQuestionText(document, q, size, base, boldBase)
        addOptions(document, q, base, boldBase)
        addMatching(document, q, size, base, boldBase)
        addGalleryImages(document, q)
        addAnswer(document, q, includeKey, base, boldBase)
        val gap = Paragraph(" ")
        gap.leading = 10f
        document.add(gap)
    }

    private fun addQuestionText(
        document: Document,
        q: OfficialPrintQuestion,
        size: Float,
        base: BaseFont,
        boldBase: BaseFont
    ) {
        val formulas = FormulaTextCodec.occurrences(q.text)
        val figures = FigureCodec.occurrences(q.text)
        val segments = RichTextSplitter.split(q.text, formulas, figures)
        val ranges = RichTextSplitter.segmentSourceRanges(segments, formulas, figures)
        val spans = q.textSpans.map { StyleSpan(it.start, it.end, it.bold, it.italic) }
        var paragraph = newParagraph(alignmentFor(q.textAlign), size)
        fun flush() {
            if (!paragraph.isEmpty()) document.add(paragraph)
            paragraph = newParagraph(alignmentFor(q.textAlign), size)
        }
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is RichSegment.Text -> segment.text.split('\n').forEachIndexed { partIndex, part ->
                    if (partIndex > 0) flush()
                    if (part.isNotEmpty()) {
                        val offset = ranges.getOrNull(index)?.first ?: 0
                        StyleSpanOps.splitBySpans(part, offset, spans).forEach { (piece, bold, italic) ->
                            paragraph.add(Chunk(sh(piece.replace("\\$", "$")), font(base, boldBase, size, bold, italic)))
                        }
                    }
                }
                is RichSegment.Math -> paragraph.add(
                    Chunk(sh(NativeMathFormatter.renderTex(segment.tex)), font(base, boldBase, size, q.bold, q.italic))
                )
                is RichSegment.Figure -> {
                    flush()
                    val bitmap = figureBitmap(segment.spec)
                    if (bitmap != null) {
                        document.add(imageParagraph(bitmap, figureWidthPt(segment.spec)))
                    } else {
                        document.add(Paragraph(sh("[شکل]"), font(base, boldBase, size, false, false)))
                    }
                }
            }
        }
        flush()
    }

    private fun addOptions(document: Document, q: OfficialPrintQuestion, base: BaseFont, boldBase: BaseFont) {
        q.options.forEachIndexed { index, option ->
            val style = q.optionStyles.getOrNull(index)
            val size = (style?.third ?: q.fontSizeSp).coerceIn(8f, 30f)
            val bold = style?.first ?: false
            val italic = style?.second ?: false
            val paragraph = newParagraph(alignmentFor(q.textAlign), size)
            paragraph.add(Chunk("${index + 1}) ", font(base, boldBase, size, true, false)))
            NativeMathFormatter.segments(option).forEach { segment ->
                if (segment.math) {
                    paragraph.add(Chunk(sh(NativeMathFormatter.renderTex(segment.text)), font(base, boldBase, size, bold, italic)))
                } else {
                    paragraph.add(Chunk(sh(segment.text.replace("\\$", "$")), font(base, boldBase, size, bold, italic)))
                }
            }
            document.add(paragraph)
        }
    }

    private fun addMatching(
        document: Document,
        q: OfficialPrintQuestion,
        size: Float,
        base: BaseFont,
        boldBase: BaseFont
    ) {
        val rows = maxOf(q.matchingLeft.size, q.matchingRight.size)
        if (rows == 0) return
        repeat(rows) { rowIndex ->
            val right = q.matchingRight.getOrNull(rowIndex).orEmpty()
            val left = q.matchingLeft.getOrNull(rowIndex).orEmpty()
            val rightStyle = q.matchingRightStyles.getOrNull(rowIndex)
            val leftStyle = q.matchingLeftStyles.getOrNull(rowIndex)
            val table = PdfPTable(3)
            table.setTotalWidth(floatArrayOf(CONTENT_WIDTH / 2f - 12f, 24f, CONTENT_WIDTH / 2f - 12f))
            table.setLockedWidth(true)
            table.defaultCell.border = Rectangle.NO_BORDER
            table.defaultCell.setPadding(1f)
            table.defaultCell.isUseAscender = true
            val rightCell = PdfPCell(
                Paragraph(
                    sh(NativeMathFormatter.renderText(right)),
                    font(base, boldBase, size, rightStyle?.first ?: false, rightStyle?.second ?: false)
                )
            )
            rightCell.border = Rectangle.NO_BORDER
            rightCell.horizontalAlignment = Element.ALIGN_RIGHT
            val arrowCell = PdfPCell(Paragraph("↔", font(base, boldBase, size, false, false)))
            arrowCell.border = Rectangle.NO_BORDER
            arrowCell.horizontalAlignment = Element.ALIGN_CENTER
            val leftCell = PdfPCell(
                Paragraph(
                    sh(NativeMathFormatter.renderText(left)),
                    font(base, boldBase, size, leftStyle?.first ?: false, leftStyle?.second ?: false)
                )
            )
            leftCell.border = Rectangle.NO_BORDER
            leftCell.horizontalAlignment = Element.ALIGN_RIGHT
            table.addCell(rightCell)
            table.addCell(arrowCell)
            table.addCell(leftCell)
            document.add(table)
        }
    }

    private fun addGalleryImages(document: Document, q: OfficialPrintQuestion) {
        q.images.forEach { bitmap -> document.add(imageParagraph(bitmap, CONTENT_WIDTH)) }
    }

    private fun addAnswer(
        document: Document,
        q: OfficialPrintQuestion,
        includeKey: Boolean,
        base: BaseFont,
        boldBase: BaseFont
    ) {
        if (includeKey && !q.answerText.isNullOrBlank()) {
            val answer = q.answerText.orEmpty()
            val paragraph = newParagraph(Element.ALIGN_RIGHT, 10.5f)
            paragraph.add(
                Chunk(sh("پاسخ: ${NativeMathFormatter.renderText(answer)}"), font(base, boldBase, 10.5f, true, false))
            )
            document.add(paragraph)
        } else {
            val dots = if (q.answerLineStyle == "blank") " " else ".".repeat(100)
            repeat(q.answerLines.coerceIn(0, 12)) {
                document.add(Paragraph(dots, font(base, boldBase, 9f, false, false)))
            }
        }
    }

    // ------------------------------------------------------------- کمکی‌ها

    private fun imageParagraph(bitmap: Bitmap, maxWidth: Float): Paragraph {
        val image = Image.getInstance(bitmapToPng(bitmap))
        if (image.plainWidth > maxWidth) image.scaleToFit(maxWidth, 480f)
        image.alignment = Image.ALIGN_CENTER
        val paragraph = Paragraph()
        paragraph.add(image)
        paragraph.spacingBefore = 4f
        paragraph.spacingAfter = 4f
        return paragraph
    }

    private fun newParagraph(align: Int, size: Float): Paragraph = Paragraph().apply {
        alignment = align
        leading = size * 1.35f
    }

    private fun font(base: BaseFont, boldBase: BaseFont, size: Float, bold: Boolean, italic: Boolean): Font {
        val bf = if (bold) boldBase else base
        val style = (if (bold) Font.BOLD else 0) or (if (italic) Font.ITALIC else 0)
        return Font(bf, size, style)
    }

    /** شکل‌نویسی فارسی/عربی (اتصال حروف) — هم‌ارز majorBidi در اپ قدیمی. */
    private fun sh(text: String): String = PersianTextShaper.shape(text)

    private fun alignmentFor(align: String): Int = when (align) {
        "center" -> Element.ALIGN_CENTER
        "left" -> Element.ALIGN_LEFT
        else -> Element.ALIGN_RIGHT
    }

    private fun figureWidthPt(spec: FigureSpec): Float =
        (WordPageLayout.figureWidthMm(spec) * (PAGE_WIDTH / 210f)).coerceIn(60f, CONTENT_WIDTH)

    private fun formatScore(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    private fun ellipsize(text: String, font: Font, width: Float): String {
        val bf = font.baseFont ?: return text
        if (bf.getWidthPoint(text, font.size) <= width) return text
        var end = text.length
        while (end > 1 && bf.getWidthPoint(text.substring(0, end) + "…", font.size) > width) end--
        return text.substring(0, end) + "…"
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun loadBaseFont(asset: String, resFallback: Int): BaseFont? {
        val assetBytes = runCatching { appContext.assets.open(asset).use { it.readBytes() } }.getOrNull()
        if (assetBytes != null) {
            // نام فونت باید به .ttf ختم شود؛ وگرنه openPDF 1.3.43 مسیر
            // TrueType/یونیکد را انتخاب نمی‌کند و فونت هرگز بارگذاری نمی‌شود.
            return BaseFont.createFont(
                asset,
                BaseFont.IDENTITY_H, BaseFont.EMBEDDED, BaseFont.CACHED, assetBytes, null
            )
        }
        val resBytes = runCatching { appContext.resources.openRawResource(resFallback).use { it.readBytes() } }.getOrNull()
        if (resBytes != null) {
            return BaseFont.createFont(
                asset, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, BaseFont.CACHED, resBytes, null
            )
        }
        return null
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
    }.getOrNull()?.also { figureCache.put(spec.raw.toString(), it) }

    /** پانوشت هر صفحه: فقط شمارهٔ صفحه (لاتین با Helvetica — بدون درگیری bidi). */
    private class FooterHelper : PdfPageEventHelper() {
        private val latin: BaseFont =
            BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
        override fun onEndPage(writer: PdfWriter, document: Document) {
            val cb = writer.directContent
            cb.beginText()
            cb.setFontAndSize(latin, 8f)
            cb.setTextMatrix(DirectPdfExporter.MARGIN, 28f)
            cb.showText("Native Exam Online · ${writer.pageNumber}")
            cb.endText()
        }
    }

    companion object {
        const val PAGE_WIDTH = 595f
        const val MARGIN = 40f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2f
        const val SIDE_COL_WIDTH = 130f
        const val CENTER_COL_WIDTH = 235f
        const val LEFT_COL_WIDTH = 130f
    }
}
