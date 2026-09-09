package ir.exam.app.domain.model

import android.graphics.Bitmap

data class OfficialPrintHeader(
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val school: String = "",
    val grade: String = "",
    val fieldOfStudy: String = "",
    // V62.7 — سربرگ رسمی چاپ: نام درس/تاریخ/مدت از منوی سربرگ صفحهٔ چاپ.
    val subject: String = "",
    val examDate: String = "",
    val examDuration: String = ""
)

sealed interface OfficialPrintable {
    val documentTitle: String
    val header: OfficialPrintHeader
    val footerNote: String
}

data class OfficialExamPrintable(
    override val documentTitle: String,
    override val header: OfficialPrintHeader,
    val subject: String,
    val durationMinutes: Int,
    val totalScore: Double = 0.0,
    val questions: List<OfficialPrintQuestion>,
    val includeAnswerKey: Boolean = false,
    override val footerNote: String = "نام و امضای دبیر:                              نام و امضای مدیر:"
) : OfficialPrintable

data class PrintTextSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    // V114
    val underline: Boolean = false,
    val color: String? = null,
    val size: Int? = null,
    val font: String? = null
)

data class OfficialPrintQuestion(
    val number: Int,
    val text: String,
    val score: Double,
    val options: List<String> = emptyList(),
    // V64.4 — استایل مستقل هر گزینه برای چاپ: (bold, italic, fontSizeSp?).
    val optionStyles: List<Triple<Boolean, Boolean, Float?>?> = emptyList(),
    // V68.6 — آیتم‌های جورکردنی برای چاپ (قبلاً فقط در options نگاه می‌شدیم و
    // جورکردنی چون options خالی دارد اصلاً چاپ نمی‌شد).
    val matchingLeft: List<String> = emptyList(),
    val matchingRight: List<String> = emptyList(),
    val matchingLeftStyles: List<Triple<Boolean, Boolean, Float?>?> = emptyList(),
    val matchingRightStyles: List<Triple<Boolean, Boolean, Float?>?> = emptyList(),
    val answerText: String? = null,
    val answerLines: Int = 2,
    val answerLineStyle: String = "lined",
    /** V107 — فاصلهٔ سطرِ فضای پاسخ (سانتی‌متر). */
    val answerLineSpacingCm: Float = 1.0f,
    val textAlign: String = "right",
    val imagePosition: String = "below",
    val fontFamily: String = "default",
    val fontSizeSp: Float = 16f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val imageWidthsMm: List<Float> = emptyList(),
    val imageXmm: List<Float> = emptyList(),
    val imageYmm: List<Float> = emptyList(),
    // V68 — استایل تکه‌ای متن سؤال برای چاپ (بازه‌های انحصاری).
    val textSpans: List<PrintTextSpan> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val images: List<Bitmap> = emptyList(),
    // V99.2 — چیدمانِ اشیاء از پیش‌نمایشِ چاپی (JSON) تا چاپ با آنچه
    // کاربر چیده یکی بماند و در بازِ بعدیِ پنجره ریست نشود.
    val figLayoutsJson: String = "",
    /** V99.2 — فاصلهٔ اضافیِ خطِ جداکننده از پیش‌نمایش (پیکسل). */
    val sepExtraPx: Int = 0,
    /**
     * V120 — نوع صریح سؤال («multiple»/«truefalse»/«matching»/«numeric»/
     * «fill»/«long»)؛ چون قبلاً هیچ نشانهٔ صریحی وجود نداشت،
     * `ExamHtmlPrintPayloadBuilder` مجبور بود نوع را از روی محتوای متن/گزینه‌ها
     * حدس بزند و سؤال‌های «صحیح/غلط» (که options ندارند) به‌اشتباه تشریحی
     * چاپ می‌شدند. مقدار null یعنی «نامشخص»؛ در این حالت رفتار قدیمیِ
     * حدسی دست‌نخورده می‌ماند (سازگاری با تست‌ها/فراخوان‌های قدیمی).
     */
    val questionType: String? = null
) {
    // V120 — لیست‌های موازیِ موقعیت/اندازهٔ هر تصویر (imageWidthsMm/imageXmm/
    // imageYmm) باید دقیقاً هم‌طول با imageUrls باشند وگرنه اندیس‌ها بی‌صدا
    // جابه‌جا می‌شوند و اندازه/موقعیتِ یک تصویر به تصویر دیگری می‌چسبد. این
    // سه تابع کمکی، صرف‌نظر از این‌که تولیدکننده لیست‌ها را درست پر کرده یا
    // نه، همیشه یک لیست هم‌طول با imageUrls و پرشده با پیش‌فرض امن برمی‌گردانند؛
    // مصرف‌کننده‌ها (ExamHtmlPrintPayload/OfficialPdfPrintAdapter) باید از این
    // توابع استفاده کنند نه مستقیماً از فیلدهای خام.
    /** V120 — نسخهٔ امنِ عرض هر تصویر؛ هم‌طول با imageUrls. */
    fun safeImageWidthsMm(): List<Float> = normalizeParallelList(imageWidthsMm, imageUrls.size, 80f)

    /** V120 — نسخهٔ امنِ مختصات x هر تصویر؛ هم‌طول با imageUrls. */
    fun safeImageXmm(): List<Float> = normalizeParallelList(imageXmm, imageUrls.size, 20f)

    /** V120 — نسخهٔ امنِ مختصات y هر تصویر؛ هم‌طول با imageUrls. */
    fun safeImageYmm(): List<Float> = normalizeParallelList(imageYmm, imageUrls.size, 30f)

    private fun normalizeParallelList(source: List<Float>, size: Int, fallback: Float): List<Float> =
        List(size) { index -> source.getOrNull(index) ?: fallback }
}

data class OfficialGradeReportPrintable(
    override val documentTitle: String,
    override val header: OfficialPrintHeader,
    val examTitles: List<String>,
    val rows: List<OfficialGradeRow>,
    override val footerNote: String = "مهر و امضای آموزشگاه:"
) : OfficialPrintable

data class OfficialGradeRow(
    val studentName: String,
    val scoreLines: List<String>,
    val averagePercent: Double?
)
