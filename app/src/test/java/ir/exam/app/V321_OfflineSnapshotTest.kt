package ir.exam.app

import ir.exam.app.core.cache.ReportsSnapshot
import ir.exam.app.core.cache.ReportsSnapshotPayload
import ir.exam.app.core.cache.SnapshotClass
import ir.exam.app.core.cache.SnapshotExam
import ir.exam.app.core.cache.SnapshotRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** V321 — عکس فوریِ کارنامه روی دیسک: باز شدن صفحه در حالت آفلاین و جدا بودن دادهٔ هر حساب. */
class V321_OfflineSnapshotTest {

    private val payload = ReportsSnapshotPayload(
        savedAtMillis = 1_700_000_000_000L,
        exams = listOf(SnapshotExam("e1", "میان‌ترم ریاضی", "ریاضی")),
        classes = listOf(SnapshotClass("c1", "دهم الف")),
        rows = listOf(
            SnapshotRow("s1", "علی", mapOf("e1" to 17.5), mapOf("e1" to 20.0)),
            SnapshotRow("s2", "سارا", mapOf("e1" to null), mapOf("e1" to 20.0))
        )
    )

    @Test
    fun encodeDecodeIsLossless() {
        val back = ReportsSnapshot.decode(ReportsSnapshot.encode(payload))
        assertEquals(payload, back)
    }

    @Test
    fun unknownOrBrokenFileIsIgnored() {
        assertNull(ReportsSnapshot.decode(""))
        assertNull(ReportsSnapshot.decode("{}").takeIf { it?.exams?.isNotEmpty() == true })
        assertNull(ReportsSnapshot.decode("not json"))
    }

    @Test
    fun fileNamesArePerUserAndSafe() {
        ReportsSnapshot.attachDir(java.io.File(System.getProperty("java.io.tmpdir"), "v321-snap"))
        val a = ReportsSnapshot.fileFor("user-a")?.name
        val b = ReportsSnapshot.fileFor("user/b c")?.name
        assertEquals("reports-user-a.json", a)
        assertEquals("reports-userbc.json", b)
        assertTrue(ReportsSnapshot.fileFor("user-a")!!.path.contains("v321-snap"))
    }
}
