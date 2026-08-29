package ir.exam.app.core.printing

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.math.FormulaTextCodec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import kotlin.math.roundToInt

/**
 * V63.0 — موتور صفحه‌بندی «ویرایشگر سند» (Word-مانند).
 *
 * تفاوت این بخش با صفحهٔ «ایجاد آزمون»: اینجا همهٔ سؤال‌های یک آزمون
 * پشت‌سرهم و در اندازهٔ واقعی کاغذ A4 چیده و صفحه‌بندی می‌شوند، دقیقاً مثل
 * یک سند ورد؛ همان اندازه‌ای که در چاپ رسمی دیده می‌شود.
 *
 * این فایل عمداً هیچ وابستگی اندرویدی/Compose ندارد (فقط ریاضی میلی‌متری) تا
 * منطق صفحه‌بندی با تست واحد JVM قابل اجرا و بررسی باشد.
 *
 * قرارداد واحد طول: میلی‌متر. یکای اندازهٔ متن: pt (هر pt = 0.3527778mm) تا
 * با `fontSizeSp` موجود در مدل سؤال هم‌خوان باشد.
 */
object WordPageLayout {

    // ابعاد واقعی کاغذ A4 (همان چیزی که چاپ رسمی استفاده می‌کند).
    const val PAGE_WIDTH_MM: Float = 210f
    const val PAGE_HEIGHT_MM: Float = 297f

    /** حاشیهٔ چهار طرف؛ کمی بیشتر از چاپ رسمی تا دستگیره‌های ویرایش جا شوند. */
    const val MARGIN_MM: Float = 14f
    const val USABLE_WIDTH_MM: Float = PAGE_WIDTH_MM - MARGIN_MM * 2f
    const val USABLE_HEIGHT_MM: Float = PAGE_HEIGHT_MM - MARGIN_MM * 2f

    /** سرصفحه (عنوان آزمون) و پاصفحه (شمارهٔ صفحه) از ارتفاع مفید کم می‌شوند. */
    const val PAGE_HEADER_MM: Float = 12f
    const val PAGE_FOOTER_MM: Float = 8f
    const val CONTENT_HEIGHT_MM: Float = USABLE_HEIGHT_MM - PAGE_HEADER_MM - PAGE_FOOTER_MM

    const val MM_PER_PT: Float = 0.3527778f
    /** ضریب ارتفاع سطر؛ برای متن درشت‌تر کمی بیشتر (مثل ورد). */
    const val LINE_HEIGHT_FACTOR: Float = 1.6f
    const val LINE_HEIGHT_FACTOR_LARGE: Float = 1.7f
    const val LARGE_TEXT_PT: Float = 18f
    /** نسبت ضخامت نویسه به اندازهٔ فونت برای متن فارسی. */
    const val AVG_CHAR_WIDTH_FACTOR: Float = 0.52f

    /** ارتفاع نمایش شکل/نمودار/جدول؛ دقیقاً همان ارتفاع رندر در NativeMathText. */
    const val FIGURE_BLOCK_HEIGHT_MM: Float = 42f
    // V63.1 — درگ/ریسایز اشیا: عرض چاپ شیء درون‌متنی داخل X خود توکن %%FIG%%
    // ذخیره می‌شود تا با همان JSON سؤال ماندگار شود (فقط خروجی چاپ).
    const val DEFAULT_FIGURE_WIDTH_MM: Float = 95f
    const val FIGURE_MIN_WIDTH_MM: Float = 40f
    const val FIGURE_MAX_WIDTH_MM: Float = 180f
    const val IMAGE_MIN_WIDTH_MM: Float = 20f
    const val IMAGE_MAX_WIDTH_MM: Float = 190f
    /** کلید عرض چاپ شیء درون‌متنی (میلی‌متر) در X توکن. */
    const val FIGURE_WIDTH_KEY: String = "wmm"
    const val MEDIA_GAP_MM: Float = 4f
    const val ANSWER_LINE_HEIGHT_MM: Float = 8f
    const val MATCHING_ROW_HEIGHT_MM: Float = 9f
    const val BLOCK_GAP_MM: Float = 6f
    const val QUESTION_HEADER_MM: Float = 8f
    const val MIN_BLOCK_HEIGHT_MM: Float = 12f

    /** یک بلوک سند: همیشه یک سؤال کامل (سند سؤال را از وسط نمی‌شکند). */
    data class WordBlock(
        val questionId: String,
        /** شمارهٔ نمایشی سؤال (۱-مبنا، همان ترتیب سند). */
        val row: Int,
        val heightMm: Float
    )

    data class WordPage(val number: Int, val blocks: List<WordBlock>)

    data class WordDocument(val pages: List<WordPage>) {
        val pageCount: Int get() = pages.size
        val blockCount: Int get() = pages.sumOf { it.blocks.size }
        fun pageOf(questionId: String): Int =
            pages.firstOrNull { page -> page.blocks.any { it.questionId == questionId } }?.number ?: 0
    }

    /** ضخامت تقریبی یک نویسهٔ فارسی بر حسب میلی‌متر. */
    fun charWidthMm(fontSizePt: Float): Float =
        (fontSizePt.coerceAtLeast(6f)) * MM_PER_PT * AVG_CHAR_WIDTH_FACTOR

    fun lineHeightMm(fontSizePt: Float): Float {
        val size = fontSizePt.coerceIn(6f, 40f)
        val factor = if (size >= LARGE_TEXT_PT) LINE_HEIGHT_FACTOR_LARGE else LINE_HEIGHT_FACTOR
        return size * MM_PER_PT * factor
    }

    /** تعداد نویسه‌های جا‌شده در یک سطر با این اندازهٔ متن. */
    fun charsPerLine(fontSizePt: Float, widthMm: Float = USABLE_WIDTH_MM): Int =
        (widthMm / charWidthMm(fontSizePt)).toInt().coerceAtLeast(8)

    /** تعداد سطرهای لازم برای یک متن (با حفظ اینترهای معلم). */
    fun lineCount(text: String, fontSizePt: Float): Int {
        val perLine = charsPerLine(fontSizePt)
        val normalized = text.replace("\r\n", "\n")
        if (normalized.isEmpty()) return 1
        return normalized.split('\n').sumOf { part ->
            // طول «مؤثر»: توکن فرمول/شکل جای کمتری از طول خامش می‌گیرد.
            val visible = visibleLength(part)
            if (visible <= 0) 1 else ((visible + perLine - 1) / perLine).coerceAtLeast(1)
        }.coerceAtLeast(1)
    }

    /**
     * طول قابل‌چاپ یک سطر: توکن‌های `$...$` و `%%FIG:...%%` به‌جای طول خام،
     * به‌اندازهٔ نمایش واقعی‌شان حساب می‌شوند تا صفحه‌بندی به چاپ نزدیک باشد.
     */
    fun visibleLength(text: String): Int {
        var length = text.length
        FormulaTextCodec.occurrences(text).forEach { occ ->
            length -= (occ.endExclusive - occ.start)
            length += 3 // فاصلهٔ یک فرمول درون‌خطی
        }
        FigureCodec.occurrences(text).forEach { occ ->
            length -= (occ.endExclusive - occ.start)
            length += 1 // شکل سطر مستقل خودش را می‌گیرد
        }
        return length.coerceAtLeast(0)
    }

    fun textHeightMm(text: String, fontSizePt: Float): Float =
        lineCount(text, fontSizePt) * lineHeightMm(fontSizePt)

    /** تعداد شکل‌های درج‌شده در یک متن (هرکدام یک سطر کامل). */
    fun figureCount(source: String): Int = FigureCodec.occurrences(source).size

    /** ارتفاع تقریبی تصویر سؤال؛ همان نسبت ۰٫۶ پیش‌نمایش ویرایشگر. */
    fun mediaHeightMm(image: MediaDraft): Float =
        image.widthMm.coerceIn(IMAGE_MIN_WIDTH_MM, IMAGE_MAX_WIDTH_MM) * 0.6f + MEDIA_GAP_MM

    /** عرض چاپ یک شکل/نمودار/جدول (پیش‌فرض ۹۵ میلی‌متر مثل قبل). */
    fun figureWidthMm(spec: FigureSpec): Float =
        spec.xNum(FIGURE_WIDTH_KEY, DEFAULT_FIGURE_WIDTH_MM).coerceIn(FIGURE_MIN_WIDTH_MM, FIGURE_MAX_WIDTH_MM)

    /** ارتفاع متناسب با عرض؛ برای توکن بدون wmm دقیقاً همان 42mm قبلی. */
    fun figureHeightMm(spec: FigureSpec): Float =
        FIGURE_BLOCK_HEIGHT_MM * (figureWidthMm(spec) / DEFAULT_FIGURE_WIDTH_MM)

    /** نسخهٔ جدید spec با عرض چاپ ذخیره‌شده در X.wmm (بقیهٔ کلیدها دست‌نخورده). */
    fun withFigureWidthMm(spec: FigureSpec, widthMm: Float): FigureSpec {
        val clamped = widthMm.coerceIn(FIGURE_MIN_WIDTH_MM, FIGURE_MAX_WIDTH_MM)
        val extras = ((spec.raw["X"] as? JsonObject)?.toMutableMap() ?: mutableMapOf())
        extras[FIGURE_WIDTH_KEY] = JsonPrimitive(clamped.toInt().toString())
        val raw = spec.raw.toMutableMap()
        raw["X"] = JsonObject(extras)
        return FigureSpec(JsonObject(raw))
    }

    /** جای افقی تصویر آزاد همیشه داخل ناحیهٔ چاپ می‌ماند. */
    fun clampImageXmm(xMm: Float, widthMm: Float): Float =
        xMm.coerceIn(0f, (USABLE_WIDTH_MM - widthMm).coerceAtLeast(0f))

    // V68.4 — موقعیت آزاد شکل: X.fx و X.fy هر دو «mm مطلق از بالا-چپ بلوکِ
    // همان سؤال»اند (V68.4.1: مطلق تا شکل بین حالت ویرایش/نمایش/چاپ نپرد؛
    // چاپ ارتفاع جریان بالای شکل را هنگام رندر کم می‌کند). حضور «هر دو»
    // یعنی شکل آزاد جابه‌جا شده؛ غیبت هرکدام = همان رندر درون‌متنی قبلی.
    const val FIGURE_POS_X_KEY: String = "fx"
    const val FIGURE_POS_Y_KEY: String = "fy"

    /** جابه‌جایی آزاد ذخیره‌شدهٔ شکل (fx/fy مطلق از بالا-چپ بلوک)؛ null = درون‌متنی. */
    fun figurePosMm(spec: FigureSpec): Pair<Float, Float>? {
        val x = spec.xStr(FIGURE_POS_X_KEY, "").toFloatOrNull() ?: return null
        val y = spec.xStr(FIGURE_POS_Y_KEY, "").toFloatOrNull() ?: return null
        return x to y
    }

    /** نسخهٔ جدید spec با موقعیت آزاد X.fx/X.fy (یک رقم اعشار؛ بقیهٔ کلیدها دست‌نخورده). */
    fun withFigurePosMm(spec: FigureSpec, xMm: Float, yMm: Float): FigureSpec {
        val extras = ((spec.raw["X"] as? JsonObject)?.toMutableMap() ?: mutableMapOf())
        extras[FIGURE_POS_X_KEY] = JsonPrimitive(((xMm * 10f).roundToInt() / 10f).toString())
        extras[FIGURE_POS_Y_KEY] = JsonPrimitive(((yMm * 10f).roundToInt() / 10f).toString())
        val raw = spec.raw.toMutableMap()
        raw["X"] = JsonObject(extras)
        return FigureSpec(JsonObject(raw))
    }

    /** برآورد جای عمودی حالت آزاد در پیش‌نمایش (هم‌ارز سقف 80pt چاپ). */
    fun freePreviewYmm(yMm: Float): Float = (yMm / PAGE_HEIGHT_MM * 28f).coerceAtMost(28f)

    /** ارتفاع تقریبی یک سؤال کامل بر حسب میلی‌متر (سرصفحه + متن + گزینه/جورکردنی + تصویر + خطوط پاسخ). */
    fun questionHeightMm(question: QuestionDraft): Float {
        val fontSizePt = question.fontSizeSp.coerceIn(8f, 30f)
        var height = QUESTION_HEADER_MM
        height += textHeightMm(question.text, fontSizePt)
        FigureCodec.occurrences(question.text).forEach { height += figureHeightMm(it.spec) }
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> question.options.forEach { option ->
                height += textHeightMm(option, fontSizePt) + 1.5f
            }
            QuestionType.TRUE_FALSE -> height += lineHeightMm(fontSizePt) + 1.5f
            QuestionType.MATCHING -> {
                val rows = maxOf(question.matchingLeft.size, question.matchingRight.size)
                height += rows * MATCHING_ROW_HEIGHT_MM
                height += question.matchingLeftImages.filterNotNull().size * FIGURE_BLOCK_HEIGHT_MM
                height += question.matchingRightImages.filterNotNull().size * FIGURE_BLOCK_HEIGHT_MM
            }
            else -> Unit
        }
        height += question.images.sumOf { mediaHeightMm(it).toDouble() }.toFloat()
        if (question.type == QuestionType.ESSAY && question.answerLineStyle == "lined") {
            height += question.answerLines.coerceIn(0, 40) * ANSWER_LINE_HEIGHT_MM
        }
        return height.coerceAtLeast(MIN_BLOCK_HEIGHT_MM)
    }

    /** ساخت بلوک‌های سند از فهرست سؤال‌ها (ترتیب سند = ترتیب سؤال‌ها). */
    fun blocksOf(questions: List<QuestionDraft>): List<WordBlock> =
        questions.mapIndexed { index, question ->
            WordBlock(
                questionId = question.id,
                row = index + 1,
                heightMm = questionHeightMm(question)
            )
        }

    /**
     * صفحه‌بندی: بلوک‌ها به همان ترتیب پشت‌سرهم چیده می‌شوند و وقتی ارتفاع
     * مفید صفحه پر شد، صفحهٔ بعدی شروع می‌شود. بلوکی که خودش از یک صفحه
     * بزرگ‌تر است تنها در صفحهٔ خودش می‌آید (مثل ورد: از وسط بریده نمی‌شود).
     */
    fun paginate(blocks: List<WordBlock>, contentHeightMm: Float = CONTENT_HEIGHT_MM): WordDocument {
        val limit = if (contentHeightMm > 0f) contentHeightMm else CONTENT_HEIGHT_MM
        val pages = mutableListOf<WordPage>()
        var current = mutableListOf<WordBlock>()
        var used = 0f
        blocks.forEach { block ->
            val gap = if (current.isEmpty()) 0f else BLOCK_GAP_MM
            if (used + gap + block.heightMm > limit && current.isNotEmpty()) {
                pages += WordPage(pages.size + 1, current)
                current = mutableListOf()
                used = 0f
            }
            current += block
            used += (if (used > 0f) BLOCK_GAP_MM else 0f) + block.heightMm
        }
        if (current.isNotEmpty()) pages += WordPage(pages.size + 1, current)
        return WordDocument(pages)
    }

    /** مسیر کامل: سؤال‌ها → بلوک‌ها → صفحه‌های A4. */
    fun documentOf(questions: List<QuestionDraft>): WordDocument = paginate(blocksOf(questions))

    /** نسبت تبدیل میلی‌متر به dp برای نمایش با این بزرگ‌نمایی. */
    fun mmToDp(mm: Float, zoom: Float): Float = mm * zoom
}
