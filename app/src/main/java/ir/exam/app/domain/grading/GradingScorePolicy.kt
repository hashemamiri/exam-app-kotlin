package ir.exam.app.domain.grading

/** V335.37 — per-question penalties remain negative; only the final total has a zero floor. */
object GradingScorePolicy {
    private const val EPSILON = 1e-8

    fun minimum(type: String, max: Double, negativeMarking: Double, structured: Boolean = false): Double {
        if (structured || type.lowercase() !in setOf("multiple", "truefalse")) return 0.0
        if (!max.isFinite() || max < 0.0 || !negativeMarking.isFinite() || negativeMarking <= 0.0) return 0.0
        return -negativeMarking * max
    }

    fun isValid(value: Double?, min: Double, max: Double): Boolean =
        value != null && value.isFinite() && min.isFinite() && max.isFinite() &&
            value >= min - EPSILON && value <= max

    fun total(grades: List<Double>): Double {
        require(grades.all { it.isFinite() }) { "نمره نامعتبر است." }
        return grades.sum().coerceAtLeast(0.0)
    }
}
