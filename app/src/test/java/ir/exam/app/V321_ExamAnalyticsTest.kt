package ir.exam.app

import ir.exam.app.core.report.ExamAnalytics
import ir.exam.app.core.report.ScoreRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V321 — کارنامهٔ تحلیلی: آمار توصیفی کلاس، توزیع نمره‌ها، رتبه‌بندی و هشدارها.
 */
class V321_ExamAnalyticsTest {

    private val rows = listOf(
        ScoreRow("s1", "الف", mapOf("e1" to 90.0, "e2" to 80.0)),
        ScoreRow("s2", "ب", mapOf("e1" to 40.0, "e2" to 20.0)),
        ScoreRow("s3", "پ", mapOf("e1" to 70.0, "e2" to 75.0)),
        ScoreRow("s4", "ت", mapOf("e1" to null, "e2" to 55.0))
    )
    private val titles = mapOf("e1" to "میان‌ترم", "e2" to "پایان‌ترم")

    @Test
    fun statsDescribeTheWholeClass() {
        val stats = ExamAnalytics.statsOf(listOf(10.0, 20.0, 30.0, 40.0))
        assertEquals(4, stats.count)
        assertEquals(25.0, stats.mean, 1e-9)
        assertEquals(25.0, stats.median, 1e-9)
        assertEquals(10.0, stats.min, 1e-9)
        assertEquals(40.0, stats.max, 1e-9)
        assertEquals(30.0, stats.spread, 1e-9)
        // همه زیر آستانهٔ ۵۰ هستند → درصد قبولی صفر
        assertEquals(0.0, stats.passRate, 1e-9)
        // با آستانهٔ ۲۵، دو نمره (۳۰ و ۴۰) قبول‌اند → ۵۰٪
        assertEquals(50.0, ExamAnalytics.statsOf(listOf(10.0, 20.0, 30.0, 40.0), passMark = 25.0).passRate, 1e-9)
    }

    @Test
    fun emptyClassDoesNotCrash() {
        val stats = ExamAnalytics.statsOf(emptyList())
        assertEquals(0, stats.count)
        assertEquals(0.0, stats.mean, 1e-9)
        val analytics = ExamAnalytics.of(emptyList(), emptyMap())
        assertTrue(analytics.isEmpty)
        assertFalse(analytics.headline.isBlank())
    }

    @Test
    fun medianHandlesEvenAndOddCounts() {
        assertEquals(2.0, ExamAnalytics.statsOf(listOf(1.0, 2.0, 3.0)).median, 1e-9)
        assertEquals(2.5, ExamAnalytics.statsOf(listOf(1.0, 2.0, 3.0, 4.0)).median, 1e-9)
    }

    @Test
    fun distributionBinsCoverAllScores() {
        val bins = ExamAnalytics.binsOf(listOf(0.0, 9.0, 10.0, 55.0, 100.0), 10)
        assertEquals(10, bins.size)
        assertEquals(5, bins.sumOf { it.count })
        // ۱۰۰ در بازهٔ آخر می‌افتد (۸۰–۹۰ نه، بلکه ۹۰–۱۰۰)
        assertEquals(1, bins.last().count)
        assertEquals(2, bins.first().count) // ۰ و ۹ هر دو در بازهٔ ۰–۱۰
    }

    @Test
    fun rankingAndPercentileAreOrdered() {
        val analytics = ExamAnalytics.of(rows, titles)
        assertEquals(listOf("s1", "s3", "s4", "s2"), analytics.ranking.map { it.studentId })
        assertEquals(1, analytics.ranking.first().rank)
        assertEquals(100.0, analytics.ranking.first().percentile, 1e-9)
        assertEquals(3, analytics.top.size)
        // دانش‌آموز بدون نمره در کارنامه نمی‌آید
        assertEquals(4, analytics.ranking.size)
    }

    @Test
    fun perExamBucketsUseSelectedExamsOnly() {
        val analytics = ExamAnalytics.of(rows, titles)
        assertEquals(2, analytics.exams.size)
        val midterm = analytics.exams.first { it.examId == "e1" }
        assertEquals(3, midterm.stats.count) // s4 نمرهٔ میان‌ترم ندارد
        assertEquals((90.0 + 40.0 + 70.0) / 3.0, midterm.stats.mean, 1e-9)
        assertEquals("e2", analytics.hardestExam?.examId)
        assertEquals("e1", analytics.easiestExam?.examId)
    }

    @Test
    fun atRiskOnlyFlagsLowOrDecliningScores() {
        val analytics = ExamAnalytics.of(rows, titles)
        assertEquals(listOf("s2"), analytics.atRisk.map { it.studentId })
        // ممتازِ با افت ۱۰درصدی هشدار نمی‌گیرد
        assertFalse(analytics.atRisk.any { it.studentId == "s1" })
        assertTrue(ExamAnalytics.declined(rows.first { it.studentId == "s1" }))
        assertFalse(ExamAnalytics.declined(rows.first { it.studentId == "s3" }))
        assertTrue(ExamAnalytics.improved(ScoreRow("x", "x", mapOf("a" to 40.0, "b" to 70.0))))
    }

    @Test
    fun headlineMentionsTheAverage() {
        val analytics = ExamAnalytics.of(rows, titles)
        assertTrue(analytics.headline.contains("میانگین"))
    }
}
