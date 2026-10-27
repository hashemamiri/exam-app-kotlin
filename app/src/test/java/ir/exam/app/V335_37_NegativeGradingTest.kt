package ir.exam.app

import ir.exam.app.domain.grading.GradingScorePolicy as Policy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V335_37_NegativeGradingTest {
    @Test fun multiple_has_penalty_floor() = assertEquals(-1.0, Policy.minimum("multiple", 3.0, 1.0 / 3), 1e-8)
    @Test fun true_false_has_penalty_floor() = assertEquals(-0.5, Policy.minimum("truefalse", 2.0, 0.25), 1e-8)
    @Test fun essay_stays_nonnegative() = assertEquals(0.0, Policy.minimum("essay", 5.0, 0.5), 0.0)
    @Test fun numeric_stays_nonnegative() = assertEquals(0.0, Policy.minimum("numeric", 5.0, 0.5), 0.0)
    @Test fun disabled_penalty_stays_nonnegative() = assertEquals(0.0, Policy.minimum("multiple", 3.0, 0.0), 0.0)
    @Test fun structured_parts_stay_nonnegative() = assertEquals(0.0, Policy.minimum("multiple", 3.0, 0.5, true), 0.0)
    @Test fun accepts_penalty_boundary() = assertTrue(Policy.isValid(-1.0, -1.0, 3.0))
    @Test fun rejects_below_penalty() = assertFalse(Policy.isValid(-1.01, -1.0, 3.0))
    @Test fun rejects_over_maximum() = assertFalse(Policy.isValid(3.01, -1.0, 3.0))
    @Test fun accepts_zero_and_maximum() { assertTrue(Policy.isValid(0.0, -1.0, 3.0)); assertTrue(Policy.isValid(3.0, -1.0, 3.0)) }
    @Test fun rejects_nonfinite_and_null() { assertFalse(Policy.isValid(Double.NaN, -1.0, 3.0)); assertFalse(Policy.isValid(Double.POSITIVE_INFINITY, -1.0, 3.0)); assertFalse(Policy.isValid(null, -1.0, 3.0)) }
    @Test fun tolerates_only_tiny_lower_rounding() { assertTrue(Policy.isValid(-1.0, -0.999999999999, 3.0)); assertFalse(Policy.isValid(-1.001, -1.0, 3.0)) }
    @Test fun total_has_zero_floor_without_mutating_question_scores() { val g = listOf(-1.0, -2.0); assertEquals(0.0, Policy.total(g), 0.0); assertEquals(-3.0, g.sum(), 0.0) }
    @Test fun positive_total_is_exact_sum() = assertEquals(3.0, Policy.total(listOf(-1.0, 4.0)), 0.0)
    @Test(expected = IllegalArgumentException::class) fun invalid_total_is_rejected() { Policy.total(listOf(Double.NaN)) }
}
