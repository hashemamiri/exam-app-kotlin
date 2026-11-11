package ir.exam.app.ui.bank

import ir.exam.app.domain.bank.ReferenceBankFormula
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType

/**
 * V360.3 — تبدیل سؤال مرجع به همان مدلی که سازندهٔ آزمون میفهمد.
 *
 * تصمیم کاربر: تصویر پاسخ نباید در سؤال قرار گیرد؛ پاسخ متنی و تصویر پاسخ
 * فقط در «بخش پاسخ» (answerExplanation / answerImages) و فقط به انتخاب
 * کاربر کپی میشوند. تصویرهای سؤال همان qHtmlاند و تصویرهای پاسخ همان
 * answerHtml؛ نیازی به انتشار دوبارهای شاردها نیست.
 */
internal fun ReferenceBankQuestion.toBankQuestionOption(
    includeAnswer: Boolean = true
): BankQuestionOption {
    val kind = when (type) {
        "multiple" -> QuestionType.MULTIPLE_CHOICE
        "truefalse" -> QuestionType.TRUE_FALSE
        "fill" -> QuestionType.FILL_BLANK
        "numeric" -> QuestionType.NUMERIC
        "matching" -> QuestionType.MATCHING
        else -> QuestionType.ESSAY
    }
    val safeType = if (kind == QuestionType.MULTIPLE_CHOICE && options.size < 2) QuestionType.ESSAY else kind
    // V360.8 — تصویر گزینهها فقط به همان گزینه وصل میشود، نه به متن سؤال.
    val qImages = questionImageUrls.take(8)
    val aImages = answerImageUrls.take(8)
    val optionImagesForDraft = if (safeType == QuestionType.MULTIPLE_CHOICE)
        options.indices.map { index -> optionImageList.getOrNull(index) } else emptyList()
    val expected = when (safeType) {
        QuestionType.FILL_BLANK -> answerText
        QuestionType.TRUE_FALSE -> when {
            answerText.contains("صحیح") -> "true"
            answerText.contains("غلط") || answerText.contains("نادرست") -> "false"
            correctIndex == 0 -> "true"
            correctIndex == 1 -> "false"
            else -> "true"
        }
        else -> ""
    }
    // V361 — حتی اگر شارد مستقیماً به مپر رسید، فرمولها توکن شوند تا در آزمون نماد ببینند.
    val textTokens = ReferenceBankFormula.mathJaxToTokens(displayText)
    val optionTokens = options.map { ReferenceBankFormula.mathJaxToTokens(it) }
    val answerTokens = ReferenceBankFormula.mathJaxToTokens(answerExplanation)
    val draft = QuestionDraft(
        type = safeType,
        text = textTokens,
        score = 1.0,
        options = optionTokens,
        correctIndex = correctIndex,
        expectedText = expected,
        images = qImages.map { MediaDraft(uri = it) },
        optionImages = optionImagesForDraft,
        answerExplanation = if (includeAnswer) answerTokens else "",
        answerImages = if (includeAnswer) aImages else emptyList(),
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
