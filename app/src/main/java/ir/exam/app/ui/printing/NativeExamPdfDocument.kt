package ir.exam.app.ui.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.text.TextUtils
import android.text.TextPaint
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.printing.OfficialExamImageLoader
import ir.exam.app.core.printing.OfficialPrintLayoutEngine
import ir.exam.app.domain.model.OfficialExamPrintable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.ceil

/**
 * یک PDF A4 بومی و immutable که هم PdfRenderer پیش‌نمایش و هم Print Framework
 * می‌خوانند. هیچ layout دیگری در لایهٔ preview یا print تولید نمی‌شود.
 */
internal data class NativeExamPdfDocument(
    val pdfFile: File,
    val pageCount: Int,
    val interactions: List<NativePdfInteraction>
)

/** metadata روی همان مختصات PDF برای gestureهای nativeِ پیش‌نمایش. */
internal sealed class NativePdfInteraction {
    abstract val pageIndex: Int

    data class Figure(
        override val pageIndex: Int,
        val questionIndex: Int,
        val figureIndex: Int,
        /** بخش قابل‌دیدن شکل روی همین صفحهٔ PDF؛ ممکن است crop شده باشد. */
        val bounds: RectF,
        /** مستطیل کامل شکل در دستگاه مختصات جریان پیوستهٔ PDF، بدون page crop. */
        val flowBounds: RectF,
        val questionFlowTop: Float,
        /** resize فقط در صفحه‌ای مجاز است که انتهای واقعی شکل در آن دیده می‌شود. */
        val canResize: Boolean,
        val isFree: Boolean
    ) : NativePdfInteraction()

    data class Separator(
        override val pageIndex: Int,
        val questionIndex: Int,
        val bounds: RectF
    ) : NativePdfInteraction()
}

/** ساخت فایل مشترک preview/print و پاک‌سازی سبک فایل‌های cache قدیمی. */
internal object NativeExamPdfDocumentFactory {
    private const val CACHE_PREFIX = "native-exam-"
    private const val MAX_AGE_MS = 24L * 60L * 60L * 1000L

    fun create(
        context: Context,
        printable: OfficialExamPrintable,
        headerFields: Map<String, String>
    ): NativeExamPdfDocument {
        val appContext = context.applicationContext
        cleanExpired(appContext.cacheDir)
        val renderer = NativeExamPdfRenderer(appContext, printable, headerFields)
        val file = File.createTempFile(CACHE_PREFIX, ".pdf", appContext.cacheDir)
        try {
            FileOutputStream(file).use(renderer::writeAll)
            return NativeExamPdfDocument(file, renderer.pageCount, renderer.interactions)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    private fun cleanExpired(directory: File) {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        directory.listFiles()
            ?.filter { it.name.startsWith(CACHE_PREFIX) && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}

/**
 * چاپ مستقیم نیز ابتدا همین فایل را کامل می‌سازد؛ سپس adapter فقط bytes همان
 * PDF را به Print Framework تحویل می‌دهد. بنابراین preview و خروجی چاپی یک
 * منبع واحد دارند، نه دو renderer هم‌خانواده.
 */
internal object NativeExamPrintLauncher {
    suspend fun print(
        context: Context,
        source: OfficialExamPrintable,
        headerFields: Map<String, String>,
        mode: String
    ): Result<Unit> = try {
        val printable = source.copy(includeAnswerKey = mode == "teacher")
        // Coil ممکن است برای تصویر خصوصی به دیسک/شبکه برود؛ مسیر مستقیم چاپ
        // نباید تا پایان این مرحله رابط کاربری را روی Main مسدود کند.
        val withImages = withContext(Dispatchers.IO) {
            OfficialExamImageLoader.load(context.applicationContext, printable)
        }
        val document = withContext(Dispatchers.Default) {
            NativeExamPdfDocumentFactory.create(context.applicationContext, withImages, headerFields)
        }
        try {
            withContext(Dispatchers.Main.immediate) {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                    ?: error("سرویس چاپ در این دستگاه در دسترس نیست.")
                manager.print(
                    safeJobName("${documentTitleOf(printable)}-${if (mode == "teacher") "key" else "student"}"),
                    NativeExamPdfPrintAdapter(document, deleteWhenFinished = true),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
            }
        } catch (error: Throwable) {
            document.pdfFile.delete()
            throw error
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun documentTitleOf(printable: OfficialExamPrintable): String =
        printable.documentTitle.ifBlank { "آزمون" }
}

/** Adapter فقط PDF از پیش‌ساخته را کپی می‌کند و renderer تازه‌ای اجرا نمی‌کند. */
internal class NativeExamPdfPrintAdapter(
    private val document: NativeExamPdfDocument,
    private val deleteWhenFinished: Boolean
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(safeJobName(document.pdfFile.nameWithoutExtension))
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(document.pageCount)
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
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }
            FileInputStream(document.pdfFile).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        if (cancellationSignal.isCanceled) break
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (cancellationSignal.isCanceled) callback.onWriteCancelled()
            // سند کامل و immutable به spooler می‌رسد؛ خود Print Framework انتخاب
            // صفحه را روی PDF اعمال می‌کند، بدون بازچینی یا raster کردن خروجی.
            else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Throwable) {
            callback.onWriteFailed(error.message ?: "نوشتن PDF ناموفق بود.")
        }
    }

    override fun onFinish() {
        if (deleteWhenFinished) document.pdfFile.delete()
    }
}

/** renderer مخصوص آزمون چاپی که header ذخیره‌شده و engine PDF را به هم وصل می‌کند. */
private class NativeExamPdfRenderer(
    context: Context,
    private val printable: OfficialExamPrintable,
    headerFields: Map<String, String>
) {
    private val appContext = context.applicationContext
    private val engine = OfficialPrintLayoutEngine(appContext)
    private val header = NativeExamHeader(appContext, printable, headerFields, engine)
    private val document = engine.layoutExam(printable, header.contentTop)

    val pageCount: Int get() = document.pageCount
    val interactions: List<NativePdfInteraction> = buildInteractions()

    fun writeAll(output: java.io.OutputStream) {
        val pdf = PdfDocument()
        try {
            document.slices.forEachIndexed { pageIndex, slice ->
                val page = pdf.startPage(
                    PdfDocument.PageInfo.Builder(
                        OfficialPrintLayoutEngine.PAGE_WIDTH,
                        OfficialPrintLayoutEngine.PAGE_HEIGHT,
                        pageIndex + 1
                    ).create()
                )
                drawPage(page.canvas, pageIndex, slice)
                pdf.finishPage(page)
            }
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }

    private fun drawPage(canvas: Canvas, pageIndex: Int, slice: Pair<Float, Float>) {
        canvas.drawColor(Color.WHITE)
        val contentTop = contentTopFor(pageIndex)
        if (pageIndex == 0) header.draw(canvas, pageIndex + 1, pageCount)
        val contentHeight = (slice.second - slice.first).coerceAtLeast(0f)
        canvas.save()
        canvas.clipRect(
            OfficialPrintLayoutEngine.MARGIN - 6f,
            contentTop,
            OfficialPrintLayoutEngine.PAGE_WIDTH - OfficialPrintLayoutEngine.MARGIN + 6f,
            contentTop + contentHeight
        )
        canvas.translate(0f, contentTop - slice.first)
        engine.drawFlowWindow(canvas, document, slice)
        canvas.restore()
        drawFooter(canvas, pageIndex + 1, pageCount)
    }

    private fun buildInteractions(): List<NativePdfInteraction> = buildList {
        document.figureBounds.forEach { figure ->
            document.slices.forEachIndexed { pageIndex, slice ->
                val visibleTop = maxOf(figure.rect.top, slice.first)
                val visibleBottom = minOf(figure.rect.bottom, slice.second)
                if (visibleBottom <= visibleTop) return@forEachIndexed
                val top = contentTopFor(pageIndex)
                add(
                    NativePdfInteraction.Figure(
                        pageIndex = pageIndex,
                        questionIndex = figure.questionIndex,
                        figureIndex = figure.occurrence,
                        bounds = RectF(
                            figure.rect.left,
                            top + visibleTop - slice.first,
                            figure.rect.right,
                            top + visibleBottom - slice.first
                        ),
                        // bounds بالا فقط برش visible این صفحه است. برای اینکه
                        // move/resize یک شکلِ دوصفحه‌ای از ارتفاع crop‌شده محاسبه
                        // نشود، geometry کاملِ جریان جداگانه نگه داشته می‌شود.
                        flowBounds = RectF(figure.rect),
                        questionFlowTop = document.questionOriginPt(figure.questionIndex),
                        canResize = figure.rect.bottom <= slice.second + .01f,
                        isFree = figure.free
                    )
                )
            }
        }
        document.separatorBounds.forEach { separator ->
            val pageIndex = document.slices.indexOfFirst { (from, to) ->
                separator.y >= from - .01f && separator.y <= to + .01f
            }
            if (pageIndex >= 0) {
                val slice = document.slices[pageIndex]
                val y = contentTopFor(pageIndex) + separator.y - slice.first + 2f
                add(
                    NativePdfInteraction.Separator(
                        pageIndex = pageIndex,
                        questionIndex = separator.questionIndex,
                        bounds = RectF(
                            OfficialPrintLayoutEngine.MARGIN,
                            y - 10f,
                            OfficialPrintLayoutEngine.PAGE_WIDTH - OfficialPrintLayoutEngine.MARGIN,
                            y + 12f
                        )
                    )
                )
            }
        }
    }

    private fun contentTopFor(pageIndex: Int): Float =
        if (pageIndex == 0) header.contentTop else OfficialPrintLayoutEngine.LATER_CONTENT_TOP

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            typeface = engine.persianTypeface(false)
        }
        if (pageNumber == totalPages) {
            canvas.drawText(
                printable.footerNote,
                OfficialPrintLayoutEngine.PAGE_WIDTH - OfficialPrintLayoutEngine.MARGIN,
                OfficialPrintLayoutEngine.PAGE_HEIGHT - 25f,
                paint
            )
        }
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "آزمون PDF بومی · $pageNumber/$totalPages",
            OfficialPrintLayoutEngine.MARGIN,
            OfficialPrintLayoutEngine.PAGE_HEIGHT - 25f,
            paint
        )
    }
}

/** سربرگ بومی با استفاده از همان schema و مقدارهای ذخیره‌شدهٔ هفت قالب. */
private class NativeExamHeader(
    private val context: Context,
    private val printable: OfficialExamPrintable,
    rawFields: Map<String, String>,
    private val engine: OfficialPrintLayoutEngine
) {
    private val schema = loadHeaderSchema(context)
    private val selectedTemplate = schema?.templates?.firstOrNull {
        it.id == rawFields["f_headerTemplate"]
    } ?: schema?.templates?.firstOrNull()
    private val allFields = schema?.templates.orEmpty().flatMap { it.fields }.associateBy { it.id }
    private val values = normalizedValues(rawFields)
    private val intro = selectedTemplate?.fields
        ?.firstOrNull { it.id == "f_intro" || it.id.endsWith("_intro") }
        ?.let { values[it.id].orEmpty().trim() }
        .orEmpty()
    private val visibleFields: List<HeaderValue> = buildVisibleFields()
    private val standardFields = visibleFields.filterNot { it.full }
    private val fullFields = visibleFields.filter { it.full }
    private val standardRows = ceil(standardFields.size / 3f).toInt()
    private val introLines = if (intro.isBlank()) 0 else minOf(3, intro.lineSequence().count().coerceAtLeast(1))

    /** فضای دقیق نخستین صفحه که به engine داده می‌شود. */
    val contentTop: Float = (
        70f + standardRows * 18f + fullFields.size * 18f + introLines * 14f + 16f
    ).coerceIn(125f, 250f)

    fun draw(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val left = OfficialPrintLayoutEngine.MARGIN
        val right = OfficialPrintLayoutEngine.PAGE_WIDTH - OfficialPrintLayoutEngine.MARGIN
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(38, 56, 77)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(left, 18f, right, contentTop - 8f, border)
        drawLogo(canvas, logoPathFor(selectedTemplate?.id.orEmpty()), right - 38f, 24f)
        drawLogo(canvas, "print/emblem.png", left + 8f, 24f)

        val center = (left + right) / 2f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 59, 86)
            textAlign = Paint.Align.CENTER
            textSize = 12f
            typeface = engine.persianTypeface(true)
        }
        canvas.drawText(templateTitle(selectedTemplate?.id.orEmpty()), center, 35f, titlePaint)
        titlePaint.textSize = 10.5f
        canvas.drawText(printable.documentTitle.ifBlank { "آزمون" }, center, 50f, titlePaint)
        titlePaint.textSize = 6.8f
        titlePaint.color = Color.DKGRAY
        canvas.drawText(selectedTemplate?.label.orEmpty(), center, 62f, titlePaint)

        var y = 72f
        standardFields.chunked(3).forEach { row ->
            row.forEachIndexed { index, item ->
                val cellRight = right - index * ((right - left) / 3f)
                drawCell(canvas, item, cellRight, y, (right - left) / 3f - 6f)
            }
            y += 18f
        }
        fullFields.forEach { item ->
            drawCell(canvas, item, right - 5f, y, right - left - 10f)
            y += 18f
        }
        if (intro.isNotBlank()) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 7.7f
                textAlign = Paint.Align.RIGHT
                typeface = engine.persianTypeface(false)
            }
            val clipped = TextUtils.ellipsize(
                intro.replace('\n', ' '),
                paint,
                right - left - 12f,
                TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(clipped, right - 6f, y + 5f, paint)
        }
        val stamp = "${JalaliCalendar.fromGregorian(LocalDate.now()).display()} · صفحه $pageNumber از $totalPages"
        val stampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 6.8f
            textAlign = Paint.Align.LEFT
            typeface = engine.persianTypeface(false)
        }
        canvas.drawText(stamp, left + 5f, contentTop - 12f, stampPaint)
    }

    private fun buildVisibleFields(): List<HeaderValue> {
        val current = selectedTemplate?.fields.orEmpty()
            .filterNot { it.id == "f_intro" || it.id.endsWith("_intro") || it.id == "opt_footerText" }
            .mapNotNull { field ->
                values[field.id]?.trim()?.takeIf(String::isNotBlank)?.let { HeaderValue(field.label, it, field.full) }
            }
            .toMutableList()

        // همان fallbackهای قبلی: اگر قالبی هنوز مقدار اختصاصی ندارد، اطلاعات
        // پایهٔ درس/تاریخ/مدت از سربرگ عمومی حذف نمی‌شود.
        listOf("f_course", "f_branch", "f_examDate", "f_duration", "f_professor", "f_examType")
            .forEach { id ->
                val value = values[id]?.trim().orEmpty()
                if (value.isNotBlank() && current.none { it.label == labelFor(id) && it.value == value }) {
                    current += HeaderValue(labelFor(id), value, false)
                }
            }
        formatScore(printable.totalScore).takeIf { it.isNotBlank() && it != "0" }?.let {
            current += HeaderValue("بارم کل:", it, false)
        }
        return current
    }

    private fun normalizedValues(input: Map<String, String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        input.forEach { (key, value) -> if (key in allFields || key == "f_headerTemplate") out[key] = value }
        val subject = printable.subject.ifBlank { printable.header.subject }.ifBlank { printable.documentTitle }
        val duration = when {
            printable.durationMinutes > 0 -> "${printable.durationMinutes} دقیقه"
            printable.header.examDuration.isNotBlank() -> printable.header.examDuration + " دقیقه"
            else -> ""
        }
        fun supply(match: (String) -> Boolean, value: String) {
            if (value.isBlank()) return
            allFields.keys.filter(match).forEach { id -> if (out[id].isNullOrBlank()) out[id] = value }
        }
        supply({ it.contains("course", true) }, subject)
        supply({ it.contains("date", true) }, printable.header.examDate)
        supply({ it.contains("duration", true) }, duration)
        supply({ it.contains("branch", true) || it.contains("schoolName", true) }, printable.header.school)
        supply({ it.contains("grade", true) || it.contains("class", true) || it.contains("degree", true) }, printable.header.grade)
        supply({ it.contains("major", true) }, printable.header.fieldOfStudy)
        supply({ it.contains("educationOffice", true) || it.contains("districtOffice", true) }, printable.header.city)
        supply({ it.contains("generalOffice", true) }, printable.header.province)
        supply({ it == "h2_totalScore" }, formatScore(printable.totalScore))
        // fallbackهای قراردادی قدیم، حتی اگر schema در دسترس نبود.
        out.putIfAbsent("f_course", subject)
        out.putIfAbsent("f_branch", printable.header.school)
        out.putIfAbsent("f_examDate", printable.header.examDate)
        out.putIfAbsent("f_duration", duration)
        return out
    }

    private fun drawCell(canvas: Canvas, item: HeaderValue, right: Float, top: Float, width: Float) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 7.2f
            textAlign = Paint.Align.RIGHT
            typeface = engine.persianTypeface(false)
        }
        val source = "${item.label} ${item.value}"
        val clipped = TextUtils.ellipsize(source, paint, width, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clipped, right, top + 8f, paint)
    }

    private fun drawLogo(canvas: Canvas, path: String, left: Float, top: Float) {
        bitmap(path)?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + 30f, top + 30f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
    }

    private fun bitmap(path: String): Bitmap? = runCatching {
        context.assets.open(path).use(android.graphics.BitmapFactory::decodeStream)
    }.getOrNull()

    private fun labelFor(id: String): String = allFields[id]?.label ?: when (id) {
        "f_course" -> "نام درس:"
        "f_branch" -> "واحد:"
        "f_examDate" -> "تاریخ امتحان:"
        "f_duration" -> "مدت زمان امتحان:"
        "f_professor" -> "استاد:"
        "f_examType" -> "نوع امتحان:"
        else -> id
    }

    private fun templateTitle(id: String): String = when (id) {
        "formal" -> "بسمه تعالی · ادارهٔ امتحانات"
        "sama" -> "آموزشکدهٔ سما"
        "school" -> "آموزش و پرورش"
        "edu" -> "ادارهٔ کل آموزش و پرورش"
        "detailed-school" -> "مدیریت آموزش و پرورش"
        "ministry" -> "وزارت آموزش و پرورش"
        else -> "دانشگاه آزاد اسلامی"
    }

    private fun logoPathFor(id: String): String = when (id) {
        "formal" -> "print/logos/logo_formal.png"
        "sama" -> "print/logos/logo_sama.png"
        "school", "edu", "ministry" -> "print/logos/logo_ministry.png"
        "detailed-school" -> "print/emblem.png"
        else -> "print/logos/logo_azad.png"
    }

    private fun formatScore(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    private data class HeaderValue(val label: String, val value: String, val full: Boolean)
}

private fun safeJobName(value: String): String =
    value.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80).ifBlank { "exam" }
