package ir.exam.app.core.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliCalendarTest {
    @Test
    fun `official Nowruz reference dates convert exactly`() {
        assertEquals(LocalDate.of(2024, 3, 20), JalaliCalendar.toGregorian(JalaliDate(1403, 1, 1)))
        assertEquals(LocalDate.of(2025, 3, 21), JalaliCalendar.toGregorian(JalaliDate(1404, 1, 1)))
        assertEquals(LocalDate.of(2026, 3, 21), JalaliCalendar.toGregorian(JalaliDate(1405, 1, 1)))
    }

    @Test
    fun `trusted current date converts to 20 Mordad 1405`() {
        assertEquals(JalaliDate(1405, 5, 20), JalaliCalendar.fromGregorian(LocalDate.of(2026, 8, 11)))
    }

    @Test
    fun `official leap boundary is preserved`() {
        assertTrue(JalaliCalendar.isLeap(1403))
        assertEquals(30, JalaliCalendar.monthLength(1403, 12))
        assertFalse(JalaliCalendar.isLeap(1404))
        assertEquals(29, JalaliCalendar.monthLength(1404, 12))
    }

    @Test
    fun `every supported date round trips without drift`() {
        for (year in JalaliCalendar.MIN_YEAR..JalaliCalendar.MAX_YEAR) {
            for (month in 1..12) {
                for (day in 1..JalaliCalendar.monthLength(year, month)) {
                    val source = JalaliDate(year, month, day)
                    assertEquals(source, JalaliCalendar.fromGregorian(JalaliCalendar.toGregorian(source)))
                }
            }
        }
    }

    @Test
    fun `Persian and Arabic digits normalize safely`() {
        assertEquals("1405/05/20", PersianDigits.latin("۱۴۰۵/٠٥/۲۰"))
        assertEquals("۱۴۰۵", PersianDigits.convert(1405))
    }
}
