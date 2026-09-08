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
    val italic: Boolean = false
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
    val sepExtraPx: Int = 0
)

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
