package ir.exam.app.core.printing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V72.0.1 — رگرسیون کامپایل و شکل‌دهی مستقل از کلاس ICU غیرقابل‌دسترسی Android. */
class V72_0_1PersianShaperTest {
    @Test
    fun `persian shaping stays pure Kotlin and preserves non Arabic text`() {
        val shaped = PersianTextShaper.shape("سلام فارسی")
        assertTrue(shaped.isNotEmpty())
        assertNotEquals("سلام فارسی", shaped)
        assertEquals("English 123", PersianTextShaper.shape("English 123"))
    }
}
