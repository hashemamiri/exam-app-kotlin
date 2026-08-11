package ir.exam.app.domain.grading

import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.domain.model.TrueFalseQuestion
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoGraderTest {
    @Test
    fun `true false and fill blank grade correctly`() {
        val tf = TrueFalseQuestion("q1", "درست؟", 1.0)
        val fill = FillBlankQuestion("q2", "پایتخت", 2.0)
        assertEquals(1.0, TrueFalseAutoGrader(mapOf("q1" to true)).grade(tf, BooleanAnswer("q1", true)).earned, 0.001)
        assertEquals(2.0, FillBlankAutoGrader(mapOf("q2" to listOf("تهران"))).grade(fill, TextAnswer("q2", "تهران")).earned, 0.001)
    }

    @Test
    fun `matching grader awards partial credit`() {
        val question = MatchingQuestion("q3", "وصل کنید", 4.0, listOf("a", "b"), listOf("1", "2"))
        val grader = MatchingAutoGrader(mapOf("q3" to mapOf(0 to 0, 1 to 1)))
        val result = grader.grade(question, MatchingAnswer("q3", mapOf(0 to 0, 1 to 0)))
        assertEquals(2.0, result.earned, 0.001)
    }
}
