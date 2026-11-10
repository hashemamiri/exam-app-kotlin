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
 *  • V360.3 — پاسخ و تصویر پاسخ فقط در بخش پاسخ قرار میگیرند.
 */
object ReferenceBankContract {
    const val BASE = "https://azmoon-media.s3.ir-thr-at1.arvanstorage.ir/reference-bank"
    const val MIN_GRADE = 1
    const val MAX_GRADE = 12
    const val PAGE_SIZE = 25

    /** V360.9 — کلید نسخهٔ شارد؛ کش لبه و مرورگر را دور میزند. */
    const val SHARD_VERSION = "v3609"

    /** نشانی شارد تازه، بدون کش کهنه. */
    fun shardUrl(path: String): String = path + "?v=" + SHARD_VERSION

    /** V360.3 — پایههای ۱ تا ۹ رشته ندارند؛ فیلد رشته فقط از پایهٔ ۱۰ میآید. */
    const val TRACK_MIN_GRADE = 10

    const val STEP_GRADE = 0
    const val STEP_TRACK = 1
    const val STEP_SUBJECT = 2
    const val STEP_CHAPTER = 3
    const val STEP_TYPE = 4
    const val STEP_DIFFICULTY = 5

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

    val TRACKS = listOf("", "عمومی", "ریاضی فیزیک", "علوم تجربی", "انسانی", "هنرستان")

    private val PRIMARY_SUBJECTS = listOf(
        "فارسی", "ریاضی", "علوم تجربی", "قرآن و تعلیمات اسلامی",
        "مطالعات اجتماعی", "هنر", "تربیت بدنی"
    )
    private val MIDDLE_SUBJECTS = listOf(
        "فارسی", "ریاضی", "علوم تجربی", "عربی", "انگلیسی",
        "مطالعات اجتماعی", "قرآن و تعلیمات اسلامی", "کامپیوتر", "هنر", "تربیت بدنی"
    )
    private val TRACK_SUBJECTS = mapOf(
        "عمومی" to listOf(
            "فارسی", "ریاضی", "فیزیک", "شیمی", "زیستشناسی", "عربی", "انگلیسی",
            "دین و زندگی", "تاریخ", "جغرافیا", "علوم اجتماعی", "فلسفه و منطق"
        ),
        "ریاضی فیزیک" to listOf(
            "ریاضی", "فیزیک", "شیمی", "فارسی", "عربی", "انگلیسی", "دین و زندگی"
        ),
        "علوم تجربی" to listOf(
            "زیستشناسی", "فیزیک", "شیمی", "ریاضی", "فارسی", "عربی", "انگلیسی", "دین و زندگی"
        ),
        "انسانی" to listOf(
            "فارسی", "عربی", "تاریخ", "جغرافیا", "علوم اجتماعی", "فلسفه و منطق",
            "اقتصاد", "ریاضی", "انگلیسی", "دین و زندگی"
        ),
        "هنرستان" to listOf(
            "فنی و حرفهای", "کاردانش", "ریاضی", "فارسی", "دین و زندگی"
        )
    )
    private val ALL_HIGH_SUBJECTS = TRACK_SUBJECTS.values.flatten().distinct().sorted()

    /** پایههای ۱ تا ۹ فیلد رشته ندارند؛ فهرست درسها ثابت و بسیار کوچک است. */
    fun tracksFor(grade: Int): List<String> =
        if (grade >= TRACK_MIN_GRADE) TRACKS else emptyList()

    /** فهرست درسهای همان پایه/رشته؛ وقتی شاردی بارگذاری نشده هم فیلتر خالی نمیماند. */
    fun subjectsFor(grade: Int, track: String = ""): List<String> = when {
        grade in 1..6 -> PRIMARY_SUBJECTS
        grade in 7..9 -> MIDDLE_SUBJECTS
        grade in 10..12 -> if (track.isBlank()) ALL_HIGH_SUBJECTS
            else TRACK_SUBJECTS[track] ?: ALL_HIGH_SUBJECTS
        else -> emptyList()
    }

    fun catalogPath(grade: Int): String = "catalog/g$grade.json"
    fun questionsPath(grade: Int): String = "questions/g$grade.json"
    fun imageUrl(base: String, file: String): String = base.trimEnd('/') + "/images/" + file
}

/**
 * V360.3 — جداسازی تصویرهای سؤال و پاسخ، بدون هیچ فایل تازهای روی فضای ابری.
 *
 * شاردهای منتشرشده تصویرها را یکجا در image_urls دارند؛ ولی q_html و
 * answer_html هر کدام همان تصویرهای خودشان را با برچسب img دارند. پس
 * تصویر پاسخ از روی answer_html شناخته میشود و هرگز وارد images سؤال
 * نمیشود. این منطق خالص کاتلین است و بدون اندروید تست میشود.
 */
object ReferenceBankMedia {
    fun imageUrlsFromHtml(html: String): List<String> {
        val out = mutableListOf<String>()
        var cursor = 0
        while (cursor < html.length) {
            val tag = html.indexOf("<img", cursor, ignoreCase = true)
            if (tag < 0) break
            val end = html.indexOf('>', tag)
            if (end < 0) break
            val chunk = html.substring(tag, end)
            val src = chunk.indexOf("src", ignoreCase = true)
            if (src >= 0) {
                var q = src + 3
                while (q < chunk.length && (chunk[q] == ' ' || chunk[q] == '=')) q++
                if (q < chunk.length && (chunk[q] == '"' || chunk[q] == '\'')) {
                    val quote = chunk[q]
                    val close = chunk.indexOf(quote, q + 1)
                    if (close > q + 1) out += chunk.substring(q + 1, close)
                }
            }
            cursor = end + 1
        }
        return out.distinct()
    }

    /** متن ساده برای «پاسخ / راهنما»؛ بدون هیچ وابستگی اندرویدی. */
    fun plainText(html: String): String {
        val sb = StringBuilder()
        var inTag = false
        for (c in html) {
            when {
                c == '<' -> { inTag = true; sb.append(' ') }
                c == '>' -> inTag = false
                !inTag -> sb.append(c)
            }
        }
        return sb.toString()
            .replace("&zwnj;", " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace('\u200c', ' ')
            .replace('\u00a0', ' ')
            .split(' ', '\n', '\t', '\r')
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
    }

    /** جفت (تصویرهای سؤال، تصویرهای پاسخ). */
    fun splitImages(qHtml: String, answerHtml: String, all: List<String>): Pair<List<String>, List<String>> {
        val answer = imageUrlsFromHtml(answerHtml).filter { it in all }.distinct()
        val question = all.filterNot { it in answer }
        return question to answer
    }
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
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    /**
     * V360.9 — nullable است تا بتوان «شارد کهنه» (بدون این فیلد) را از
     * «سؤال بدون تصویر گزینه» (فیلد هست ولی همگی null) تشخیص داد.
     */
    @SerialName("option_images") val optionImages: List<String?>? = null
) {
    val optionImageList: List<String?>
        get() = optionImages.orEmpty()

    /** آیا شارد قدیمی است؟ یعنی فیلد option_images اصلاً وجود ندارد. */
    val shardIsStale: Boolean
        get() = optionImages == null

    /** فقط تصویرهای صورت سؤال؛ تصویر گزینهها و پاسخ هرگز اینجا نمیآیند. */
    val questionImageUrls: List<String>
        get() = ReferenceBankMedia.imageUrlsFromHtml(qHtml).filter { it in imageUrls }

    /** فقط تصویرهای پاسخ. */
    val answerImageUrls: List<String>
        get() = ReferenceBankMedia.imageUrlsFromHtml(answerHtml).filter { it in imageUrls }

    val answerExplanation: String
        get() = ReferenceBankMedia.plainText(answerHtml).ifBlank { answerText }
}
