package ir.exam.app.core.figure

/**
 * V57.0 — کدک پاسخ نامگذاری اطلس: پاسخ‌های تایپ‌شدهٔ دانش‌آموز در کادرهای
 * شماره‌دار (نشانه‌های پیکان‌دار آناتومی/فیزیک/شیمی) به‌صورت متن ساختاریافته
 * «n) پاسخ» در همان TextAnswer موجود ذخیره می‌شوند؛ بدون تغییر قرارداد سرور،
 * و معلم در تصحیح همین سطرها را می‌بیند.
 */
object AtlasBlankAnswerCodec {
    private val LINE = Regex("""^(\d+)\)\s?(.*)$""")

    fun format(values: Map<Int, String>): String =
        values.entries
            .filter { it.value.isNotBlank() }
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}) ${it.value.trim()}" }

    /** خطوط «n) پاسخ» داخل متن پاسخ. */
    fun parse(text: String): Map<Int, String> =
        text.lines().mapNotNull { line ->
            LINE.matchEntire(line.trim())?.let { m ->
                val n = m.groupValues[1].toIntOrNull() ?: return@let null
                n to m.groupValues[2]
            }
        }.toMap()

    /** بخش آزاد پاسخ (هر چیزی جز خطوط نامگذاری) — همان کادر «پاسخ شما». */
    fun freeText(text: String): String =
        text.lines().filter { LINE.matchEntire(it.trim()) == null }
            .joinToString("\n").trim('\n')

    /**
     * ترکیب دو بخش در یک TextAnswer: اول خطوط نامگذاری مرتب، سپس متن آزاد.
     * قرارداد سرور تغییری نمی‌کند و معلم در تصحیح همین متن را می‌بیند.
     */
    fun merge(blanks: Map<Int, String>, free: String): String {
        val head = format(blanks)
        return when {
            head.isEmpty() -> free
            free.isBlank() -> head
            else -> head + "\n" + free
        }
    }
}
