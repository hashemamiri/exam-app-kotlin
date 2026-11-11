package ir.exam.app

import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.ui.bank.toBankQuestionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V362 — پایه‌هایی که متن کامل را فقط در q_html دارند باید در کارت و آزمون دیده شوند. */
class V362_ReferenceBankAutoDetailsTest {
    @Test
    fun questionHtmlIsFallbackForEmptyTextAndOptionsStayAligned() {
        val questionImage = "https://media.example/q.webp"
        val optionImage = "https://media.example/o.webp"
        val answerImage = "https://media.example/a.webp"
        val source = ReferenceBankQuestion(
            id = 8001,
            grade = 8,
            type = "multiple",
            text = "",
            qHtml = "<div>صورت سؤال کامل<img src=\"$questionImage\"></div>",
            answerHtml = "<div>پاسخ<img src=\"$answerImage\"></div>",
            options = listOf("گزینه یک", "گزینه دو"),
            correctIndex = 1,
            imageUrls = listOf(questionImage, optionImage, answerImage),
            optionImages = listOf(optionImage, null)
        )

        assertEquals("صورت سؤال کامل", source.displayText)
        val draft = source.toBankQuestionOption(includeAnswer = false).question
        assertEquals("صورت سؤال کامل", draft.text)
        assertEquals(listOf("گزینه یک", "گزینه دو"), draft.options)
        assertEquals(1, draft.correctIndex)
        assertEquals(listOf(questionImage), draft.images.map { it.uri })
        assertEquals(listOf(optionImage, null), draft.optionImages)
        assertEquals("", draft.answerExplanation)
        assertEquals(emptyList<String>(), draft.answerImages)
    }

    /** V363 — در Gradle، user.dir پوشهٔ app/ است؛ مسیر را مثل بقیهٔ تست‌ها پیدا کن. */
    private fun sourceFile(rel: String): java.io.File {
        val candidates = listOf(java.io.File("src/main/java/$rel"), java.io.File("app/src/main/java/$rel"))
        return candidates.firstOrNull { it.isFile }
            ?: error("مسیر پیدا نشد: $rel از ${java.io.File(".").absolutePath}")
    }

    @Test
    fun fullShardAutoLoadsAndCardsRenderMetadataAndPerQuestionAnswerChip() {
        val vm = sourceFile("ir/exam/app/ui/bank/QuestionBankViewModel.kt").readText()
        val dialog = sourceFile("ir/exam/app/ui/bank/ReferenceBankDialog.kt").readText()
        val mapper = sourceFile("ir/exam/app/ui/bank/ReferenceBankMapper.kt").readText()
        assertTrue(vm.contains("loadReferenceQuestionDetails()"))
        assertTrue(vm.contains("referenceRepository.questions(grade)"))
        assertTrue(dialog.contains("detail.displayText"))
        assertTrue(dialog.contains("detail.options.forEachIndexed"))
        assertTrue(dialog.contains("row.area"))
        assertTrue(dialog.contains("افزودن پاسخ"))
        assertTrue(mapper.contains("mathJaxToTokens(displayText)"))
    }
}
