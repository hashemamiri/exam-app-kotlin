package ir.exam.app.data.repository

import ir.exam.app.ui.builder.PrimaryBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V310.9 — دروس هر پایهٔ ابتدایی طبق فهرست رسمی؛ «بدون پایه» / پایهٔ نامعتبر همهٔ دروس. */
class V310_9_GradeSubjectsTest {
    @Test
    fun `grade one has the first-grade subjects only`() {
        assertEquals(
            listOf("آموزش قرآن", "فارسی", "نگارش فارسی", "ریاضی", "علوم تجربی"),
            PrimaryBank.subjectsFor(1)
        )
    }

    @Test
    fun `grade two adds the heavenly gifts subject`() {
        assertTrue("هدیه‌های آسمان" in PrimaryBank.subjectsFor(2))
        assertTrue("مطالعات اجتماعی" !in PrimaryBank.subjectsFor(2))
    }

    @Test
    fun `grades three to five add social studies`() {
        for (g in 3..5) assertTrue("مطالعات اجتماعی" in PrimaryBank.subjectsFor(g))
        assertTrue("تفکر و پژوهش" !in PrimaryBank.subjectsFor(5))
    }

    @Test
    fun `grade six adds thinking and work technology`() {
        val six = PrimaryBank.subjectsFor(6)
        assertEquals(9, six.size)
        assertTrue("تفکر و پژوهش" in six && "کار و فناوری" in six)
    }

    @Test
    fun `no grade or invalid grade returns every subject`() {
        assertEquals(PrimaryBank.SUBJECTS, PrimaryBank.subjectsFor(0))
        assertEquals(PrimaryBank.SUBJECTS, PrimaryBank.subjectsFor(9))
        assertEquals(9, PrimaryBank.SUBJECTS.size)
        assertTrue("علوم" !in PrimaryBank.SUBJECTS)
    }
}
