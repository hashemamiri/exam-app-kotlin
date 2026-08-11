package ir.exam.app.core.calendar

import java.time.DayOfWeek
import java.time.LocalDate

/** تاریخ هجری خورشیدی بر پایهٔ الگوریتم مرجع Borkowski (همان jalaali-js). */
data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(year in JalaliCalendar.MIN_YEAR..JalaliCalendar.MAX_YEAR) { "سال شمسی خارج از بازه است." }
        require(month in 1..12) { "ماه شمسی نامعتبر است." }
        require(day in 1..JalaliCalendar.monthLength(year, month)) { "روز شمسی نامعتبر است." }
    }

    fun toIsoDate(): String = JalaliCalendar.toGregorian(this).toString()
    fun display(): String = PersianDigits.convert("$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}")
    fun longDisplay(): String = PersianDigits.convert("$day ${JalaliCalendar.MONTH_NAMES[month - 1]} $year")
}

object PersianDigits {
    private const val PERSIAN = "۰۱۲۳۴۵۶۷۸۹"
    private const val ARABIC = "٠١٢٣٤٥٦٧٨٩"

    fun convert(value: Any?): String = value.toString().map { char ->
        if (char in '0'..'9') PERSIAN[char - '0'] else char
    }.joinToString("")

    fun latin(value: String): String = value.map { char ->
        when {
            char in PERSIAN -> ('0'.code + PERSIAN.indexOf(char)).toChar()
            char in ARABIC -> ('0'.code + ARABIC.indexOf(char)).toChar()
            else -> char
        }
    }.joinToString("")
}

/**
 * تبدیل آفلاین و قطعی میلادی/شمسی. بازه عمداً به سال‌های قابل انتخاب برنامه محدود شده
 * تا تاریخ خراب هرگز وارد RPC تقویم نشود.
 */
object JalaliCalendar {
    const val MIN_YEAR = 1400
    const val MAX_YEAR = 1500

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    val WEEKDAY_NAMES = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    fun today(clockDate: LocalDate = LocalDate.now()): JalaliDate = fromGregorian(clockDate)

    fun isLeap(year: Int): Boolean = jalCal(year).leap == 0

    fun monthLength(year: Int, month: Int): Int = when (month) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isLeap(year)) 30 else 29
        else -> error("ماه شمسی نامعتبر است.")
    }

    fun toGregorian(date: JalaliDate): LocalDate {
        val gregorian = jdnToGregorian(jalaliToJdn(date.year, date.month, date.day))
        return LocalDate.of(gregorian.year, gregorian.month, gregorian.day)
    }

    fun fromGregorian(date: LocalDate): JalaliDate {
        val j = jdnToJalali(gregorianToJdn(date.year, date.monthValue, date.dayOfMonth))
        return JalaliDate(j.year, j.month, j.day)
    }

    /** تعداد خانه‌های خالی ابتدای ماه در شبکه‌ای که از شنبه شروع می‌شود. */
    fun firstDayOffset(year: Int, month: Int): Int = when (toGregorian(JalaliDate(year, month, 1)).dayOfWeek) {
        DayOfWeek.SATURDAY -> 0
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
    }

    fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val absolute = year * 12 + (month - 1) + delta
        val shiftedYear = Math.floorDiv(absolute, 12)
        val shiftedMonth = Math.floorMod(absolute, 12) + 1
        require(shiftedYear in MIN_YEAR..MAX_YEAR) { "سال انتخابی خارج از بازه است." }
        return shiftedYear to shiftedMonth
    }

    private data class JalCal(val leap: Int, val gregorianYear: Int, val marchDay: Int)
    private data class DateParts(val year: Int, val month: Int, val day: Int)

    private fun jalCal(year: Int): JalCal {
        require(year >= breaks.first() && year < breaks.last()) { "سال شمسی خارج از دامنه تبدیل است." }
        val gregorianYear = year + 621
        var leapJ = -14
        var previousBreak = breaks.first()
        var nextBreak = 0
        var jump = 0
        for (index in 1 until breaks.size) {
            nextBreak = breaks[index]
            jump = nextBreak - previousBreak
            if (year < nextBreak) break
            leapJ += floorDiv(jump, 33) * 8 + floorDiv(floorMod(jump, 33), 4)
            previousBreak = nextBreak
        }
        var n = year - previousBreak
        leapJ += floorDiv(n, 33) * 8 + floorDiv(floorMod(n, 33) + 3, 4)
        if (floorMod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = floorDiv(gregorianYear, 4) - floorDiv((floorDiv(gregorianYear, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + floorDiv(jump + 4, 33) * 33
        var leap = (floorMod(n + 1, 33) - 1) % 4
        if (leap == -1) leap = 4
        return JalCal(leap, gregorianYear, march)
    }

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Int {
        val a = floorDiv(14 - month, 12)
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + floorDiv(153 * m + 2, 5) + 365 * y + floorDiv(y, 4) -
            floorDiv(y, 100) + floorDiv(y, 400) - 32045
    }

    private fun jdnToGregorian(jdn: Int): DateParts {
        val j = jdn + 32044
        val g = floorDiv(j, 146097)
        val dg = floorMod(j, 146097)
        val c = minOf(floorDiv(dg, 36524), 3)
        val dc = dg - c * 36524
        val b = floorDiv(dc, 1461)
        val db = floorMod(dc, 1461)
        val a = minOf(floorDiv(db, 365), 3)
        val da = db - a * 365
        val y = g * 400 + c * 100 + b * 4 + a
        val m = floorDiv(da * 5 + 308, 153) - 2
        val d = da - floorDiv((m + 4) * 153, 5) + 122
        return DateParts(
            year = y - 4800 + floorDiv(m + 2, 12),
            month = floorMod(m + 2, 12) + 1,
            day = d + 1
        )
    }

    private fun jalaliToJdn(year: Int, month: Int, day: Int): Int {
        val calculation = jalCal(year)
        return gregorianToJdn(calculation.gregorianYear, 3, calculation.marchDay) +
            (month - 1) * 31 - floorDiv(month, 7) * (month - 7) + day - 1
    }

    private fun jdnToJalali(jdn: Int): DateParts {
        val gregorianYear = jdnToGregorian(jdn).year
        var year = gregorianYear - 621
        var calculation = jalCal(year)
        var firstFarvardin = gregorianToJdn(calculation.gregorianYear, 3, calculation.marchDay)
        var offset = jdn - firstFarvardin
        if (offset >= 0) {
            if (offset <= 185) return DateParts(year, 1 + floorDiv(offset, 31), floorMod(offset, 31) + 1)
            offset -= 186
            if (offset < 180) return DateParts(year, 7 + floorDiv(offset, 30), floorMod(offset, 30) + 1)
            return DateParts(year, 12, 30)
        }

        year -= 1
        calculation = jalCal(year)
        firstFarvardin = gregorianToJdn(calculation.gregorianYear, 3, calculation.marchDay)
        offset = jdn - firstFarvardin
        if (offset <= 185) return DateParts(year, 1 + floorDiv(offset, 31), floorMod(offset, 31) + 1)
        offset -= 186
        if (offset < 180) return DateParts(year, 7 + floorDiv(offset, 30), floorMod(offset, 30) + 1)
        return DateParts(year, 12, 30)
    }

    private fun floorDiv(value: Int, divisor: Int): Int = Math.floorDiv(value, divisor)
    private fun floorMod(value: Int, divisor: Int): Int = Math.floorMod(value, divisor)
}
