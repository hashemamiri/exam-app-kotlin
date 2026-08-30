package com.lowagie.text.pdf

/**
 * V70.1 — شکل‌نویسی (اتصال حروف) فارسی/عربی برای خروجی PDF مستقیم.
 *
 * openPDF 1.3.43 برخلاف iText 5 نام‌های جادویی majorBidi/minorBidi را حذف
 * کرده است؛ در نتیجه جریان عادی سند فقط ترتیب راست‌به‌چپ را درست می‌کند و
 * حروف را جدا (غیرمتصل) می‌نویسد. برای هم‌ارزی با خروجی اپ قدیمی، متن را
 * پیش از افزودن به سند با همان جدول داخلی openPDF شکل‌دهی می‌کنیم.
 *
 * این کلاس عمداً در پکیج com.lowagie.text.pdf قرار دارد تا بتواند از متد
 * پکیج‌-خصوصی ArabicLigaturizer.arabic_shape استفاده کند (همان موتورِ داخلی
 * openPDF؛ در dex نهایی همه‌چیز یکجا است).
 */
object PersianTextShaper {

    /** بخش‌های عربی متن را shape می‌کند؛ بقیهٔ نویسه‌ها دست‌نخورده می‌مانند. */
    fun shape(text: String): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        val out = StringBuilder(text.length + 8)
        var i = 0
        val n = chars.size
        while (i < n) {
            val c = chars[i]
            if (isArabicRun(c)) {
                val start = i
                while (i < n && isArabicRun(chars[i])) i++
                val len = i - start
                val dest = CharArray(len * 2 + 2)
                val written = ArabicLigaturizer.arabic_shape(chars, start, len, dest, 0, dest.size, 0)
                out.append(dest, 0, written)
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /** نویسه‌هایی که باید در یک اجرای شکل‌دهی با هم دیده شوند (حروف/ارقام/نیم‌فاصله). */
    private fun isArabicRun(c: Char): Boolean =
        c in '\u0600'..'\u06FF' || // عربی/فارسی (حروف، ارقام، علائم)
        c in '\u0750'..'\u077F' || // عربی تکمیلی
        c in '\u08A0'..'\u08FF' || // عربی توسعه‌یافته
        c in '\uFB50'..'\uFDFF' || // فرم‌های نمایشی (اگر از قبل shape شده باشد)
        c in '\uFE70'..'\uFEFF' || // فرم‌های نمایشی
        c == '\u200C' || c == '\u200D' // نیم‌فاصله / اتصال‌دهنده
}
