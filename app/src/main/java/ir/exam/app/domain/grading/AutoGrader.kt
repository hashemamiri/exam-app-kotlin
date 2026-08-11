package ir.exam.app.domain.grading

import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.GradeItem
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.NumericQuestion
import ir.exam.app.domain.model.Question
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.domain.model.TrueFalseQuestion
import kotlin.math.abs

interface AutoGrader {
    fun canGrade(question: Question): Boolean
    fun grade(question: Question, answer: StudentAnswer): GradeItem
}

class MultipleChoiceAutoGrader(private val correct: Map<String, Int>) : AutoGrader {
    override fun canGrade(question: Question) = question is MultipleChoiceQuestion
    override fun grade(question: Question, answer: StudentAnswer): GradeItem {
        val value = (answer as? ChoiceAnswer)?.selectedIndex
        return question.result(value == correct[question.id])
    }
}

class TrueFalseAutoGrader(private val correct: Map<String, Boolean>) : AutoGrader {
    override fun canGrade(question: Question) = question is TrueFalseQuestion
    override fun grade(question: Question, answer: StudentAnswer): GradeItem =
        question.result((answer as? BooleanAnswer)?.value == correct[question.id])
}

class FillBlankAutoGrader(
    private val expected: Map<String, List<String>>,
    private val caseSensitive: Set<String> = emptySet()
) : AutoGrader {
    override fun canGrade(question: Question) = question is FillBlankQuestion
    override fun grade(question: Question, answer: StudentAnswer): GradeItem {
        val value = (answer as? TextAnswer)?.value?.trim().orEmpty()
        val ok = expected[question.id].orEmpty().any { target ->
            if (question.id in caseSensitive) value == target.trim()
            else value.equals(target.trim(), ignoreCase = true)
        }
        return question.result(ok)
    }
}

class NumericAutoGrader(
    private val expected: Map<String, Double>,
    private val tolerance: Map<String, Double>
) : AutoGrader {
    override fun canGrade(question: Question) = question is NumericQuestion
    override fun grade(question: Question, answer: StudentAnswer): GradeItem {
        val value = (answer as? TextAnswer)?.value?.toDoubleOrNull()
        val target = expected[question.id]
        val ok = value != null && target != null && abs(value - target) <= (tolerance[question.id] ?: 0.0)
        return question.result(ok)
    }
}

class MatchingAutoGrader(private val expected: Map<String, Map<Int, Int>>) : AutoGrader {
    override fun canGrade(question: Question) = question is MatchingQuestion
    override fun grade(question: Question, answer: StudentAnswer): GradeItem {
        val actual = (answer as? MatchingAnswer)?.pairs.orEmpty()
        val key = expected[question.id].orEmpty()
        if (key.isEmpty()) return question.result(false)
        val correctCount = key.count { (left, right) -> actual[left] == right }
        val earned = question.score * correctCount.toDouble() / key.size.toDouble()
        return GradeItem(question.id, earned.coerceIn(0.0, question.score), question.score)
    }
}

private fun Question.result(correct: Boolean) = GradeItem(id, if (correct) score else 0.0, score)
