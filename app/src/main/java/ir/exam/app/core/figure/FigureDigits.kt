package ir.exam.app.core.figure

/**
 * V137.5 — کلید سراسریِ «اعداد فارسی» برای اعدادِ رسم‌شده در ابزارها: ابزارهای هندسیِ تخته (خط‌کش/نقاله)،
 * زمینهٔ محور مختصات تخته، محورها، نمودارها، شکل‌ها، جدول‌ها و جدول تناوبی (رندرکننده‌های SVG بومی) و همچنین
 * موتور وبِ پیش‌نمایش/چاپ (از راه payload → window.__figPersianDigits).
 *
 * مقدار از [ir.exam.app.core.ui.AppearanceSettings.persianDigits] در MainActivity اینجا نوشته می‌شود تا
 * رندرکننده‌های غیرِ Compose (Canvas/SVG) بدون دسترسی به CompositionLocal آن را بخوانند.
 */
object FigureDigits {
    @Volatile
    var persian: Boolean = false

    private const val PERSIAN = "۰۱۲۳۴۵۶۷۸۹"

    /** اگر کلید روشن باشد، ارقام لاتینِ رشته را فارسی می‌کند؛ وگرنه همان رشته. */
    fun apply(s: String): String {
        if (!persian || s.isEmpty()) return s
        var changed = false
        val sb = StringBuilder(s.length)
        for (ch in s) {
            if (ch in '0'..'9') { sb.append(PERSIAN[ch - '0']); changed = true } else sb.append(ch)
        }
        return if (changed) sb.toString() else s
    }
}
