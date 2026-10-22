package ir.exam.app

import ir.exam.app.ui.builder.BuilderIssueLevel
import ir.exam.app.ui.builder.BuilderPreflight
import ir.exam.app.ui.builder.BuilderPreflightReport
import ir.exam.app.ui.builder.HotspotRect
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V321 — «بررسی نهایی» آزمون‌ساز: خطاهای بازدارنده پیش از ذخیره گرفته می‌شوند.
 */
class V321_BuilderPreflightTest {

    private fun mc(text: String = "۲+۲ چند است؟", options: List<String> = listOf("۳", "۴", "۵", "۶"), correct: Int? = 1) =
        QuestionDraft(type = QuestionType.MULTIPLE_CHOICE, text = text, score = 2.0, options = options, correctIndex = correct)

    @Test
    fun healthyExamCanBeSaved() {
        val report = BuilderPreflight.check("آزمون ریاضی", listOf(mc(), mc("۳×۳ چند است؟", correct = 2)))
        assertTrue(report.canSave)
        assertTrue(report.isEmpty)
        assertEquals(4.0, report.totalScore, 1e-9)
        assertTrue(report.summary.contains("همه‌چیز مرتب است"))
    }

    @Test
    fun emptyExamIsBlocked() {
        val report = BuilderPreflight.check("آزمون", emptyList())
        assertFalse(report.canSave)
        assertTrue(report.blockers.any { it.code == "questions.empty" })
        assertTrue(report.blockers.all { it.questionIndex == 0 })
    }

    @Test
    fun blankTextAndWrongScoreAreBlockers() {
        val report = BuilderPreflight.check("آزمون", listOf(mc(text = "")))
        assertTrue(report.blockers.any { it.code == "text.blank" })
        val zeroScore = BuilderPreflight.check("آزمون", listOf(mc().copy(score = 0.0)))
        assertTrue(zeroScore.blockers.any { it.code == "score.invalid" })
        assertEquals("سؤال 1", zeroScore.blockers.first().where)
    }

    @Test
    fun multipleChoiceNeedsOptionsAndCorrectAnswer() {
        val noOptions = BuilderPreflight.check("آزمون", listOf(mc(options = listOf("۴"))))
        assertTrue(noOptions.blockers.any { it.code == "mc.options" })
        val noCorrect = BuilderPreflight.check("آزمون", listOf(mc(correct = null)))
        assertTrue(noCorrect.blockers.any { it.code == "mc.correct" })
        val duplicates = BuilderPreflight.check("آزمون", listOf(mc(options = listOf("۴", "۴", "۵", "۶"))))
        assertTrue(duplicates.warnings.any { it.code == "mc.duplicate" })
        assertTrue(duplicates.canSave)
    }

    @Test
    fun numericAndFillNeedValidAnswers() {
        val numeric = QuestionDraft(type = QuestionType.NUMERIC, text = "جذر ۱۶؟", expectedNumber = "چهار", tolerance = "0")
        val report = BuilderPreflight.check("آزمون", listOf(numeric))
        assertTrue(report.blockers.any { it.code == "numeric.expected" })
        val fill = QuestionDraft(type = QuestionType.FILL_BLANK, text = "پایتخت ایران ____ است.", expectedText = "  ")
        assertTrue(BuilderPreflight.check("آزمون", listOf(fill)).blockers.any { it.code == "fill.expected" })
    }

    @Test
    fun matchingRequiresPairs() {
        val matching = QuestionDraft(
            type = QuestionType.MATCHING, text = "جور کنید",
            matchingLeft = listOf("الف", "ب"), matchingRight = listOf("۱", "۲"),
            matchingPairs = mapOf(0 to 0)
        )
        val report = BuilderPreflight.check("آزمون", listOf(matching))
        assertTrue(report.blockers.any { it.code == "match.pairs" })
    }

    @Test
    fun duplicateQuestionsAreWarnedAndIssuesAreSorted() {
        val report = BuilderPreflight.check("آزمون", listOf(mc(), mc(), mc(text = "")))
        assertTrue(report.warnings.any { it.code == "question.duplicate" })
        assertEquals(report.issues.map { it.questionIndex }.sorted(), report.issues.map { it.questionIndex })
        assertFalse(report.canSave)
        assertEquals(3, report.questionCount)
    }

    @Test
    fun blankTitleIsOnlyAWarning() {
        val report = BuilderPreflight.check("", listOf(mc()))
        assertTrue(report.canSave)
        assertTrue(report.warnings.any { it.code == "title.blank" })
    }

    @Test
    fun reportTextListsEveryIssue() {
        val report = BuilderPreflight.check("آزمون", listOf(mc(text = "")))
        val text = BuilderPreflight.asText(report)
        assertTrue(text.contains("بررسی نهایی"))
        assertTrue(text.contains("سؤال 1"))
        assertEquals(BuilderIssueLevel.BLOCKER, report.issues.first { it.questionIndex == 1 }.level)
    }

    @Test
    fun trueFalseAcceptsExpectedTextFromNativeEditorAndBlocksUnselected() {
        // V333.1 — رگرسیون کشف‌شده توسط تست واقعی شبیه‌ساز Pixel 6:
        // دکمهٔ «صحیح/غلط» در آزمون‌ساز بومی expectedText="true"/"false" را پر می‌کند.
        val unselected = QuestionDraft(type = QuestionType.TRUE_FALSE, text = "آیا زمین گرد است؟")
        val blocked = BuilderPreflight.check("آزمون", listOf(unselected))
        assertFalse(blocked.canSave)
        assertTrue(blocked.blockers.any { it.code == "tf.correct" })

        val trueSelected = unselected.copy(expectedText = "true")
        val falseSelected = unselected.copy(expectedText = "false")
        assertTrue(BuilderPreflight.check("آزمون", listOf(trueSelected)).canSave)
        assertTrue(BuilderPreflight.check("آزمون", listOf(falseSelected)).canSave)
    }

    @Test
    fun primaryOrderAndHotspotModesValidateActualEditorFields() {
        val orderValid = QuestionDraft(
            type = QuestionType.FILL_BLANK,
            text = "به ترتیب بنویسید",
            mode = "order",
            orderItems = listOf("بهار", "تابستان", "پاییز")
        )
        assertTrue(BuilderPreflight.check("آزمون ابتدایی", listOf(orderValid)).canSave)

        val orderInvalid = orderValid.copy(orderItems = listOf("فقط یکی"))
        assertTrue(BuilderPreflight.check("آزمون ابتدایی", listOf(orderInvalid)).blockers.any { it.code == "fill.order" })

        val hotspotValid = QuestionDraft(
            type = QuestionType.MULTIPLE_CHOICE,
            text = "روی برگ کلیک کنید",
            mode = "hotspot",
            options = listOf("", "", "", ""),
            hotspots = listOf(HotspotRect(10f, 10f, 18f, 18f), HotspotRect(50f, 50f, 18f, 18f), HotspotRect(0f, 0f, 0f, 0f), HotspotRect(0f, 0f, 0f, 0f)),
            correctIndex = 1
        )
        assertTrue(BuilderPreflight.check("آزمون ابتدایی", listOf(hotspotValid)).canSave)
    }
}
