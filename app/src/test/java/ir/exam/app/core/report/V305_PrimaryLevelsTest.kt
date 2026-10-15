package ir.exam.app.core.report

import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.ui.student.orderFromAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

/** V305 — کارنامهٔ ۴ سطحی، ترتیب مرتب‌سازی و بُرخوردن موارد عمومی. */
class V305_PrimaryLevelsTest {
    @Test
    fun `default thresholds are not stored and invalid ones are rejected`() {
        assertNull(PrimaryLevels.parseThresholds("85,70,50"))
        assertNull(PrimaryLevels.parseThresholds(""))
        assertNull(PrimaryLevels.parseThresholds("50,70,80"))
        assertNull(PrimaryLevels.parseThresholds("90,80"))
        assertEquals(listOf(90.0, 80.0, 60.0), PrimaryLevels.parseThresholds("90،80،60"))
        assertEquals(listOf(90.0, 80.0, 60.0), PrimaryLevels.parseThresholds("۹۰,۸۰,۶۰"))
    }

    @Test
    fun `four levels follow thresholds`() {
        assertEquals("خیلی خوب", PrimaryLevels.levelOf(90.0).name)
        assertEquals("خوب", PrimaryLevels.levelOf(75.0).name)
        assertEquals("قابل قبول", PrimaryLevels.levelOf(60.0).name)
        assertEquals("نیاز به تلاش بیشتر", PrimaryLevels.levelOf(10.0).name)
        assertEquals(4, PrimaryLevels.levelOf(85.0).rank)
        assertEquals("★★☆☆", PrimaryLevels.levelOf(60.0).stars)
        // آستانه‌های سفارشی: ۷۲٪ با [۹۰،۸۰،۶۰] → بین ۶۰ و ۸۰ → قابل قبول
        assertEquals("قابل قبول", PrimaryLevels.levelOf(72.0, listOf(90.0, 80.0, 60.0)).name)
    }

    @Test
    fun `public order items are shuffled and never equal the correct order`() {
        val correct = listOf("۱", "۲", "۳", "۴")
        repeat(20) {
            val out = ExamQuestionCodec.shuffledOrderPublic(correct)
            assertEquals(correct.sorted(), out.sorted())
            assertNotEquals(correct, out)
        }
        assertEquals(listOf("الف"), ExamQuestionCodec.shuffledOrderPublic(listOf("الف")))
    }

    @Test
    fun `answer string joins trimmed items with comma`() {
        assertEquals("۱,ب,سه", ExamQuestionCodec.orderAnswerOf(listOf(" ۱ ", "ب", "سه", "  ")))
    }

    @Test
    fun `student order is restored from a valid saved answer only`() {
        val items = listOf("a", "b", "c")
        assertEquals(items, orderFromAnswer("", items))
        assertEquals(listOf("c", "a", "b"), orderFromAnswer("c,a,b", items))
        assertEquals(items, orderFromAnswer("x,y,z", items))
        assertEquals(items, orderFromAnswer("a,b", items))
        assertEquals(3, orderFromAnswer("b,a,c", items).size)
    }

    @Test
    fun `primary flag reads options json`() {
        val on = buildJsonObject { put("primary", JsonPrimitive(true)) }
        val off = buildJsonObject { }
        assertTrue(PrimaryLevels.isPrimary(on))
        assertTrue(!PrimaryLevels.isPrimary(off))
        assertEquals(listOf(85.0, 70.0, 50.0), PrimaryLevels.thresholdsOf(off))
    }
}
