package ir.exam.app.domain.model

import ir.exam.app.core.calendar.JalaliDate
import java.time.DayOfWeek
import java.time.LocalDate

enum class CalendarAudience(val wireValue: String) {
    // V61.0 — مخاطب «مدارس»: همهٔ دانش‌آموزان ثبت‌شده در مدرسه‌های انتخابی.
    ALL("all"), SCHOOLS("schools"), CLASSES("classes"), STUDENTS("students");

    companion object {
        fun fromWire(value: String?): CalendarAudience = entries.firstOrNull { it.wireValue == value } ?: ALL
    }
}

data class CalendarNote(
    val id: String,
    val date: LocalDate,
    val title: String,
    val body: String = "",
    val audience: CalendarAudience = CalendarAudience.ALL,
    val classIds: Set<String> = emptySet(),
    val studentIds: Set<String> = emptySet(),
    val schoolIds: Set<String> = emptySet()
)

data class OfficialHoliday(
    val date: LocalDate,
    val jalaliDate: JalaliDate,
    val title: String,
    val isHoliday: Boolean = true
)

data class CalendarDay(
    val jalaliDate: JalaliDate,
    val gregorianDate: LocalDate,
    val notes: List<CalendarNote>,
    val officialHolidays: List<OfficialHoliday>
) {
    val isFriday: Boolean get() = gregorianDate.dayOfWeek == DayOfWeek.FRIDAY
    val isHoliday: Boolean get() = isFriday || officialHolidays.any(OfficialHoliday::isHoliday)
}

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val days: List<CalendarDay>,
    val officialYearIsExact: Boolean,
    val holidayDataAvailable: Boolean
)

data class CalendarAudienceOption(val id: String, val label: String, val subtitle: String? = null)

data class CalendarEditor(
    val id: String? = null,
    val date: JalaliDate,
    val title: String = "",
    val body: String = "",
    val audience: CalendarAudience = CalendarAudience.ALL,
    val classIds: Set<String> = emptySet(),
    val studentIds: Set<String> = emptySet(),
    val schoolIds: Set<String> = emptySet()
)
