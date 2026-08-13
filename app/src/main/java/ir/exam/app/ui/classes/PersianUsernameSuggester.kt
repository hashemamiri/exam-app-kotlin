package ir.exam.app.ui.classes

/** پیشنهاد قابل‌ویرایش نام کاربری؛ تصمیم نهایی یکتایی همچنان با سرور است. */
object PersianUsernameSuggester {
    private val words = mapOf(
        "علی" to "ali", "محمد" to "mohammad", "رضا" to "reza", "حسین" to "hossein",
        "حسن" to "hasan", "زهرا" to "zahra", "فاطمه" to "fatemeh", "مریم" to "maryam",
        "احمد" to "ahmad", "امیر" to "amir", "سارا" to "sara", "نرگس" to "narges",
        "احمدی" to "ahmadi", "رضایی" to "rezaei", "محمدی" to "mohammadi",
        "حسینی" to "hosseini", "کریمی" to "karimi", "مرادی" to "moradi",
        "اکبری" to "akbari", "جعفری" to "jafari", "نادری" to "naderi", "کاظمی" to "kazemi"
    )
    private val letters = mapOf(
        'ا' to "a", 'آ' to "a", 'ب' to "b", 'پ' to "p", 'ت' to "t", 'ث' to "s",
        'ج' to "j", 'چ' to "ch", 'ح' to "h", 'خ' to "kh", 'د' to "d", 'ذ' to "z",
        'ر' to "r", 'ز' to "z", 'ژ' to "zh", 'س' to "s", 'ش' to "sh", 'ص' to "s",
        'ض' to "z", 'ط' to "t", 'ظ' to "z", 'ع' to "a", 'غ' to "gh", 'ف' to "f",
        'ق' to "gh", 'ک' to "k", 'ك' to "k", 'گ' to "g", 'ل' to "l", 'م' to "m",
        'ن' to "n", 'و' to "v", 'ه' to "h", 'ی' to "y", 'ي' to "y"
    )
    private val persianDigits = "۰۱۲۳۴۵۶۷۸۹"

    fun suggest(firstName: String, lastName: String, suffix: Int? = null): String {
        val parts = listOf(firstName, lastName)
            .map(::transliteratePart)
            .filter(String::isNotBlank)
        var base = parts.joinToString("_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(17)
        if (base.length < 4) base = (base + "_user").take(17)
        val tail = suffix?.takeIf { it > 0 }?.let { "_${it.toString().padStart(2, '0')}" }.orEmpty()
        return (base.take(20 - tail.length) + tail).trim('_')
    }

    private fun transliteratePart(raw: String): String {
        val clean = raw.trim().replace('‌', ' ')
        words[clean]?.let { return it }
        return buildString {
            clean.lowercase().forEach { char ->
                when {
                    char in 'a'..'z' || char.isDigit() -> append(char)
                    char in persianDigits -> append(persianDigits.indexOf(char))
                    char == ' ' || char == '-' || char == '_' -> append('_')
                    else -> append(letters[char].orEmpty())
                }
            }
        }.replace(Regex("_+"), "_").trim('_')
    }
}
