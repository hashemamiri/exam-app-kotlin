package ir.exam.app.domain.model

import android.graphics.Bitmap

data class OfficialPrintHeader(
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val school: String = "",
    val grade: String = ""
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
    val totalScore: Double,
    val questions: List<OfficialPrintQuestion>,
    val includeAnswerKey: Boolean = false,
    override val footerNote: String = "نام و امضای دبیر:                              نام و امضای مدیر:"
) : OfficialPrintable

data class OfficialPrintQuestion(
    val number: Int,
    val text: String,
    val score: Double,
    val options: List<String> = emptyList(),
    val answerText: String? = null,
    val answerLines: Int = 2,
    val answerLineStyle: String = "lined",
    val textAlign: String = "right",
    val imagePosition: String = "below",
    val fontFamily: String = "default",
    val fontSizeSp: Float = 16f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val imageWidthsMm: List<Float> = emptyList(),
    val imageXmm: List<Float> = emptyList(),
    val imageYmm: List<Float> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val images: List<Bitmap> = emptyList()
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
