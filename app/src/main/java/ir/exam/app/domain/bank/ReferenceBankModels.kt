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
    const val SHARD_VERSION = "v3710"

    /** نشانی شارد تازه، بدون کش کهنه. */
    fun shardUrl(path: String): String = path + "?v=" + SHARD_VERSION

    /** V370 — نسخهٔ قالب شارد صفحه‌ای؛ idx با نسخهٔ دیگر یعنی شارد کامل خوانده شود. */
    const val PAGED_VERSION = "v3701"

    /** V372 — تصویر «تکه‌ای»: در هر دو بُعد ریز، یا مساحت ناچیز (نمونه: «؟» ۱۶×۳۲). */
    const val MIN_IMAGE_SIDE = 40
    const val MIN_IMAGE_AREA = 1200

    fun isTinyImage(width: Int, height: Int): Boolean =
        width > 0 && height > 0 &&
            ((width < MIN_IMAGE_SIDE && height < MIN_IMAGE_SIDE) || width * height < MIN_IMAGE_AREA)

    /** V370 — هر صفحه ~۵۰ سؤال؛ همان اندازه‌ای که سازندهٔ شارد می‌سازد. */
    const val PAGE_CHUNK = 50

    /** فهرست کوچک: شناسهٔ سؤال → شمارهٔ صفحه. */
    fun questionIndexPath(grade: Int): String = "questions/g" + grade + ".idx.json"

    /** یک صفحه از شارد سؤال. */
    fun questionPagePath(grade: Int, page: Int): String = "questions/g" + grade + ".p" + page + ".json"

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

    // V376 — «علوم و معارف اسلامی» در دادهٔ پایهٔ دهم هست ولی در این فهرست نبود.
    val TRACKS = listOf("", "عمومی", "ریاضی فیزیک", "علوم تجربی", "انسانی", "علوم و معارف اسلامی", "هنرستان", "معارف")

    /** V376 — نام‌های هم‌خوان: داده با نام کامل می‌آید، کاربر ممکن است کوتاه بگوید. */
    private val TRACK_ALIASES = mapOf(
        "معارف" to "علوم و معارف اسلامی",
        "علوم و معارف" to "علوم و معارف اسلامی",
        "ادبیات و علوم انسانی" to "انسانی"
    )

    fun trackKey(track: String): String {
        val clean = track.replace("\u200c", "").trim()
        return TRACK_ALIASES[clean] ?: clean
    }

    fun sameTrack(rowTrack: String, wanted: String): Boolean =
        wanted.isBlank() || trackKey(rowTrack) == trackKey(wanted)

    /**
     * V377 — شمار تصویرهای «صورت سؤال» در یک سطر فهرست.
     *
     * دو واقعیت داده: در فهرست پایهٔ دهم `has_image` همیشه false و `image_count`
     * صفر است ولی `image_urls` پر است؛ و در ردیف‌های قدیمی `image_urls` نشانی
     * تصویر گزینه را هم دارد. پس مبنای فیلتر «با تصویر»، خودِ نشانی‌ها هستند.
     */
    fun questionImageCount(row: ReferenceBankRow): Int =
        row.imageUrls.count { it.isNotBlank() }.let { if (it > 0) it else row.imageCount.coerceAtLeast(0) }

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
    // V376 — پایهٔ دهم هیچ سؤال «عمومی» ندارد؛ پیشنهاد آن کاربر را به صفر سطر می‌برد.
    fun tracksFor(grade: Int): List<String> {
        if (grade < TRACK_MIN_GRADE) return emptyList()
        return TRACKS.filter { !(grade == 10 && it == "عمومی") }
    }

    /** فهرست درسهای همان پایه/رشته؛ وقتی شاردی بارگذاری نشده هم فیلتر خالی نمیماند. */
    fun subjectsFor(grade: Int, track: String = ""): List<String> = when {
        grade in 1..6 -> PRIMARY_SUBJECTS
        grade in 7..9 -> MIDDLE_SUBJECTS
        grade in 10..12 -> if (track.isBlank()) ALL_HIGH_SUBJECTS
            else TRACK_SUBJECTS[track] ?: ALL_HIGH_SUBJECTS
        else -> emptyList()
    }

    /**
     * V378 — درس‌های رسمی هر پایهٔ ۱ تا ۹ (فهرست رسمیِ برنامهٔ درسی)؛ همیشه
     * در فهرست درس فیلتر بانک مرجع دیده می‌شوند.
     */
    fun curriculumSubjects(grade: Int): List<String> = when (grade) {
        1 -> listOf("آموزش قرآن", "فارسی", "نگارش فارسی", "ریاضی", "علوم تجربی", "هنر", "تربیت بدنی")
        2 -> listOf("آموزش قرآن", "هدیه‌های آسمان", "فارسی", "نگارش فارسی",
            "ریاضی", "علوم تجربی", "هنر", "تربیت بدنی")
        3, 4, 5 -> listOf("آموزش قرآن", "هدیه‌های آسمان", "فارسی", "نگارش فارسی",
            "ریاضی", "علوم تجربی", "مطالعات اجتماعی", "هنر", "تربیت بدنی")
        6 -> listOf("آموزش قرآن", "هدیه‌های آسمان", "فارسی", "نگارش فارسی",
            "ریاضی", "علوم تجربی", "مطالعات اجتماعی", "تفکر و پژوهش", "کار و فناوری", "هنر", "تربیت بدنی")
        in 7..9 -> MIDDLE_SUBJECTS + "نگارش فارسی" + "کار و فناوری"
        else -> emptyList()
    }

    /**
     * V378 — نام‌های هم‌خانوادهٔ همان درس‌های رسمی که در دادهٔ پایه‌های ۱ تا ۹
     * آمده‌اند ولی نامشان دقیقاً برابر نیست؛ فقط وقتی در داده هستند به فهرست
     * درس‌ها اضافه می‌شوند. دادهٔ خارج از خانواده (نمونهٔ ثبت‌شده: «زبان
     * انگلیسی» در پایهٔ اول) هرگز نشان داده نمی‌شود؛ با «همهٔ درس‌ها» دیده می‌شود.
     */
    fun recognizedSubjects(grade: Int): Set<String> {
        if (grade !in 1..9) return emptySet()
        val variants = setOf(
            "قرآن", "قرآن و تعلیمات اسلامی", "هدیه آسمان", "پیام‌های آسمان", "علوم"
        ) + if (grade in 7..9) setOf("زبان انگلیسی") else emptySet()
        return curriculumSubjects(grade).toSet() + variants
    }

    fun catalogPath(grade: Int): String = "catalog/g$grade.json"

    /** V376 — فهرست تکه‌ای (catalog/gN-s1..sM.json) برای پایه‌هایی که فایل کامل بزرگ است. */
    fun catalogShardPath(grade: Int, part: Int): String = "catalog/g$grade-s$part.json"

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

    /**
     * V363 — متنِ نمایشی سؤال.
     *
     * ‏شاردهای پایه‌های ۶، ۷ و ۸ فیلد `text` را خالی ساخته‌اند و متن کامل
     * فقط داخل `q_html` است. فهرست بانک و سؤالی که به آزمون افزوده می‌شود
     * همین متن را می‌خوانند؛ پس بدون این پل، سطر و سؤال بی‌متن می‌شد، در حالی
     * که پیش‌نمایش (که `q_html` را می‌خواند) سالم بود.
     *
     * @param text متن آماده (اگر پر باشد همین مقدم است)
     * @param qHtml متن کامل HTML؛ فقط وقتی `text` خالی است خوانده می‌شود
     * @param limit صفر یعنی بدون برش؛ وگرنه «…» با رعایت مرز فرمول
     */
    fun displayText(text: String, qHtml: String, limit: Int = 0): String {
        val direct = text.trim()
        val value = if (direct.isNotEmpty()) direct else htmlToText(qHtml)
        if (limit <= 0 || value.length <= limit) return balanceMath(value)
        val cut = value.take(limit)
        val safe = if (cut.count { it == '$' } % 2 == 1)
            cut.substring(0, cut.lastIndexOf('$').coerceAtLeast(0)) else cut
        val head = if (safe.contains(' ')) safe.substringBeforeLast(' ') else safe
        val trimmed = (if (head.trim().length > limit / 2) head else safe).trimEnd()
        return balanceMath(trimmed + "…")
    }

    /** HTML → متنِ ساده؛ برخلاف [plainText] نیم‌فاصله را نمی‌شکند. */
    fun htmlToText(html: String): String {
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
            .replace("&zwnj;", "\u200c")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace('\u00a0', ' ')
            .split(' ', '\n', '\t', '\r')
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
    }

    /**
     * V363 — فرمولِ بازمانده را نصفه رها نکن.
     *
     * ‏۱۷ سؤال از ۱۶٬۳۹۱ سؤال پایهٔ هشتم در خودِ منبع وسط فرمول با «…» بریده
     * شده‌اند؛ در آن‌ها تعداد «$» فرد است و اگر همان‌طور نمایش داده شود نمادِ
     * نیمه‌کاره دیده می‌شود.
     */
    fun balanceMath(value: String): String {
        val trimmed = value.trimEnd()
        if (trimmed.count { it == '$' } % 2 == 0) return trimmed
        val last = trimmed.lastIndexOf('$')
        if (last <= 0) return trimmed
        val cut = trimmed.substring(0, last).trimEnd()
        return if (cut.isEmpty()) trimmed else cut + "…"
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
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
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
) {
    /**
     * V377 — تصویرهای «صورت سؤال».
     *
     * در فهرست پایهٔ دهم ستون has_image همیشه false و image_count صفر است، ولی
     * image_urls پر است؛ و در ردیف‌های قدیمی image_urls تصویر گزینه را هم دارد.
     * پس فیلتر «با تصویر» نباید به has_image تکیه کند.
     */
    fun questionImageCount(): Int = ReferenceBankContract.questionImageCount(this)

    fun hasQuestionImage(): Boolean = questionImageCount() > 0
}

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
        get() {
            // V374 — تعدادی از سؤال‌ها تصویرشان فقط در image_urls است و در متن
            // <img> ندارد (گزارش V373: ۹۵۳ سؤال). با برشِ «فقط q_html» این
            // تصویرها در بانک و در آزمون ساخته‌شده هیچ‌جا دیده نمی‌شدند. حالا
            // پیوست‌ها هم می‌آیند: بی‌تکرار، بی تصویر گزینه، بی تصویر پاسخ.
            val inQuestion = ReferenceBankMedia.imageUrlsFromHtml(qHtml)
            val inAnswer = ReferenceBankMedia.imageUrlsFromHtml(answerHtml)
            val optionOnly = optionImageList.filterNotNull().filter { it.isNotBlank() }
            return (inQuestion + imageUrls).distinct()
                .filterNot { it in inAnswer || it in optionOnly }
        }

    /** فقط تصویرهای پاسخ. */
    val answerImageUrls: List<String>
        get() = ReferenceBankMedia.imageUrlsFromHtml(answerHtml).filter { it in imageUrls }

    /** متنِ نمایشی؛ اگر `text` خالی باشد از `q_html` ساخته می‌شود (V363). */
    val displayText: String
        get() = ReferenceBankMedia.displayText(text, qHtml)

    val answerExplanation: String
        get() = ReferenceBankMedia.plainText(answerHtml).ifBlank { answerText }
}
