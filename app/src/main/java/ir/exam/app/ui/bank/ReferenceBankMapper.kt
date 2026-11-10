package ir.exam.app.ui.bank

import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType

/** V360 — تبدیل سؤال مرجع به همان مدلی که سازندهٔ آزمون میفهمد. */
internal fun ReferenceBankQuestion.toBankQuestionOption(): BankQuestionOption {
    val kind = when (type) {
        "multiple" -> QuestionType.MULTIPLE_CHOICE
        "truefalse" -> QuestionType.TRUE_FALSE
        "fill" -> QuestionType.FILL_BLANK
        "numeric" -> QuestionType.NUMERIC
        "matching" -> QuestionType.MATCHING
        else -> QuestionType.ESSAY
    }
    val safeType = if (kind == QuestionType.MULTIPLE_CHOICE && options.size < 2) QuestionType.ESSAY else kind
    val draft = QuestionDraft(
        type = safeType,
        text = text,
        score = 1.0,
        options = options,
        correctIndex = correctIndex,
        expectedText = answerText,
        images = imageUrls.take(8).map { MediaDraft(uri = it) },
        grade = grade,
        subject = subject,
        topic = topic,
        difficulty = difficulty.coerceIn(0, 4)
    )
    return BankQuestionOption(
        id = id,
        subject = subject,
        question = draft,
        examTitle = "بانک مرجع"
    )
}
