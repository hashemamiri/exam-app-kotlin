package ir.exam.app.domain.bank

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * V360 — قرارداد بانک سؤال مرجع.
 *
 * قید کاربر: سؤالها و تصویرها در همان ذخیرهٔ S3سازگار فعلی بمانند و
 * هیچ حجمی به APK یا باندل سایت اضافه نشود. اپ فقط شارد فهرست پایه را
 * میگیرد؛ متن کامل و تصویرها هنگام نیاز خوانده میشوند.
 *
 * تصمیمهای کاربر:
 *  • «تشریحی کوتاه» → «تشریحی»؛ «درسنامه» حذف.
 *  • دشواری چهار سطح دارد؛ «خیلی دشوار» = ۴.
 *  • هیچ فیلد منبع، مجوز یا ارجاع حقوقی نگهداری نمیشود.
 */
object ReferenceBankContract {
    const val BASE = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank"
    const val MIN_GRADE = 1
    const val MAX_GRADE = 12
    const val PAGE_SIZE = 25

    val DIFFICULTY_LABELS = listOf("تعییننشده", "ساده", "متوسط", "دشوار", "خیلی دشوار")

    val TYPE_MAP = mapOf(
        "تستی" to "multiple",
        "تشریحی" to "essay",
        "تشریحیکوتاه" to "essay",
        "جایخالی" to "fill",
        "کوتاهپاسخ" to "fill",
        "صحیح/غلط" to "truefalse",
        "دستهبندی" to "matching",
        "مرتبسازی" to "matching"
    )
    val DROPPED_TYPES = setOf("درسنامه")

    fun catalogPath(grade: Int): String = "catalog/g$grade.json"
    fun questionsPath(grade: Int): String = "questions/g$grade.json"
    fun imageUrl(base: String, file: String): String = base.trimEnd('/') + "/images/" + file
}

@Serializable
data class ReferenceBankRow(
    val id: Long,
    val grade: Int = 1,
    val track: String = "",
    val subject: String = "",
    val chapter: String = "",
    val topic: String = "",
    val area: String = "",
    @SerialName("type_fa") val typeFa: String = "",
    val type: String = "essay",
    val difficulty: Int = 0,
    @SerialName("difficulty_fa") val difficultyFa: String = "",
    val score: Double = 1.0,
    val text: String = "",
    @SerialName("has_image") val hasImage: Boolean = false,
    @SerialName("image_count") val imageCount: Int = 0,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList()
)

@Serializable
data class ReferenceBankQuestion(
    val id: Long,
    val grade: Int = 1,
    val track: String = "",
    val subject: String = "",
    val chapter: String = "",
    val topic: String = "",
    @SerialName("type_fa") val typeFa: String = "",
    val type: String = "essay",
    val difficulty: Int = 0,
    @SerialName("difficulty_fa") val difficultyFa: String = "",
    val text: String = "",
    @SerialName("q_html") val qHtml: String = "",
    @SerialName("answer_html") val answerHtml: String = "",
    val options: List<String> = emptyList(),
    @SerialName("correct_index") val correctIndex: Int? = null,
    @SerialName("answer_text") val answerText: String = "",
    @SerialName("image_urls") val imageUrls: List<String> = emptyList()
)
