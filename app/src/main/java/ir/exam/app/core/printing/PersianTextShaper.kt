package ir.exam.app.core.printing

/**
 * شکل‌دهی سبک فارسی/عربی برای خروجی PDF مستقیم.
 *
 * iText 7 Core مسئول bidi/layout است؛ این مرحله فقط اتصال حروف را با جدول
 * Unicode Presentation Forms انجام می‌دهد تا خروجی فارسی روی Android بدون
 * وابستگی به کلاس‌های غیرقابل‌دسترسی ICU یا pdfCalligraph جدا و گسسته نشود.
 * نویسه‌های ناشناخته دست‌نخورده می‌مانند و BaseDirection در iText 7 ترتیب RTL
 * را مدیریت می‌کند.
 */
internal object PersianTextShaper {
    private data class Forms(
        val isolated: Char,
        val final: Char?,
        val initial: Char?,
        val medial: Char?,
        val joinsPrevious: Boolean,
        val joinsNext: Boolean
    )

    // ترتیب فرم‌ها: isolated, final, initial, medial.
    private val forms: Map<Char, Forms> = mapOf(
        '\u0621' to Forms('\uFE80', null, null, null, false, false), // ء
        '\u0622' to Forms('\uFE81', '\uFE82', null, null, true, false), // آ
        '\u0623' to Forms('\uFE83', '\uFE84', null, null, true, false), // أ
        '\u0624' to Forms('\uFE85', '\uFE86', null, null, true, false), // ؤ
        '\u0625' to Forms('\uFE87', '\uFE88', null, null, true, false), // إ
        '\u0626' to Forms('\uFE89', '\uFE8A', '\uFE8B', '\uFE8C', true, true), // ئ
        '\u0627' to Forms('\uFE8D', '\uFE8E', null, null, true, false), // ا
        '\u0628' to Forms('\uFE8F', '\uFE90', '\uFE91', '\uFE92', true, true), // ب
        '\u0629' to Forms('\uFE93', '\uFE94', null, null, true, false), // ة
        '\u062A' to Forms('\uFE95', '\uFE96', '\uFE97', '\uFE98', true, true), // ت
        '\u062B' to Forms('\uFE99', '\uFE9A', '\uFE9B', '\uFE9C', true, true), // ث
        '\u062C' to Forms('\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0', true, true), // ج
        '\u062D' to Forms('\uFEA1', '\uFEA2', '\uFEA3', '\uFEA4', true, true), // ح
        '\u062E' to Forms('\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8', true, true), // خ
        '\u062F' to Forms('\uFEA9', '\uFEAA', null, null, true, false), // د
        '\u0630' to Forms('\uFEAB', '\uFEAC', null, null, true, false), // ذ
        '\u0631' to Forms('\uFEAD', '\uFEAE', null, null, true, false), // ر
        '\u0632' to Forms('\uFEAF', '\uFEB0', null, null, true, false), // ز
        '\u0633' to Forms('\uFEB1', '\uFEB2', '\uFEB3', '\uFEB4', true, true), // س
        '\u0634' to Forms('\uFEB5', '\uFEB6', '\uFEB7', '\uFEB8', true, true), // ش
        '\u0635' to Forms('\uFEB9', '\uFEBA', '\uFEBB', '\uFEBC', true, true), // ص
        '\u0636' to Forms('\uFEBD', '\uFEBE', '\uFEBF', '\uFEC0', true, true), // ض
        '\u0637' to Forms('\uFEC1', '\uFEC2', '\uFEC3', '\uFEC4', true, true), // ط
        '\u0638' to Forms('\uFEC5', '\uFEC6', '\uFEC7', '\uFEC8', true, true), // ظ
        '\u0639' to Forms('\uFEC9', '\uFECA', '\uFECB', '\uFECC', true, true), // ع
        '\u063A' to Forms('\uFECD', '\uFECE', '\uFECF', '\uFED0', true, true), // غ
        '\u0641' to Forms('\uFED1', '\uFED2', '\uFED3', '\uFED4', true, true), // ف
        '\u0642' to Forms('\uFED5', '\uFED6', '\uFED7', '\uFED8', true, true), // ق
        '\u0643' to Forms('\uFED9', '\uFEDA', '\uFEDB', '\uFEDC', true, true), // ک عربی
        '\u0644' to Forms('\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0', true, true), // ل
        '\u0645' to Forms('\uFEE1', '\uFEE2', '\uFEE3', '\uFEE4', true, true), // م
        '\u0646' to Forms('\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8', true, true), // ن
        '\u0647' to Forms('\uFEE9', '\uFEEA', '\uFEEB', '\uFEEC', true, true), // ه
        '\u0648' to Forms('\uFEED', '\uFEEE', null, null, true, false), // و
        '\u0649' to Forms('\uFEEF', '\uFEF0', null, null, true, false), // ى
        '\u064A' to Forms('\uFEEF', '\uFEF0', '\uFEF1', '\uFEF2', true, true), // ي
        '\u0671' to Forms('\uFB50', '\uFB51', null, null, true, false), // ٱ
        '\u067E' to Forms('\uFB56', '\uFB57', '\uFB58', '\uFB59', true, true), // پ
        '\u0686' to Forms('\uFB7A', '\uFB7B', '\uFB7C', '\uFB7D', true, true), // چ
        '\u0698' to Forms('\uFB8A', '\uFB8B', null, null, true, false), // ژ
        '\u06A9' to Forms('\uFB8E', '\uFB8F', '\uFB90', '\uFB91', true, true), // ک فارسی
        '\u06AF' to Forms('\uFB92', '\uFB93', '\uFB94', '\uFB95', true, true), // گ
        '\u06CC' to Forms('\uFBFC', '\uFBFD', '\uFBE8', '\uFBE9', true, true) // ی فارسی
    )

    fun shape(text: String): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        val output = StringBuilder(text.length)
        chars.indices.forEach { index ->
            val current = forms[chars[index]]
            if (current == null) {
                output.append(chars[index])
                return@forEach
            }
            val previous = findPreviousBase(chars, index)
            val next = findNextBase(chars, index)
            val joinsPrevious = previous?.let { forms[it]?.joinsNext == true } == true && current.joinsPrevious
            val joinsNext = next?.let { forms[it]?.joinsPrevious == true } == true && current.joinsNext
            output.append(
                when {
                    joinsPrevious && joinsNext -> current.medial
                    joinsPrevious -> current.final
                    joinsNext -> current.initial
                    else -> current.isolated
                } ?: current.isolated
            )
        }
        return output.toString()
    }

    private fun findPreviousBase(chars: CharArray, index: Int): Char? {
        var cursor = index - 1
        while (cursor >= 0 && isTransparent(chars[cursor])) cursor--
        return chars.getOrNull(cursor)?.takeIf { forms.containsKey(it) }
    }

    private fun findNextBase(chars: CharArray, index: Int): Char? {
        var cursor = index + 1
        while (cursor < chars.size && isTransparent(chars[cursor])) cursor++
        return chars.getOrNull(cursor)?.takeIf { forms.containsKey(it) }
    }

    private fun isTransparent(c: Char): Boolean =
        c in '\u064B'..'\u065F' || c == '\u0670' || c in '\u06D6'..'\u06ED'
}
