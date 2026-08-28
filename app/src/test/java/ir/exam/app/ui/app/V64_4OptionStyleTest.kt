package ir.exam.app.ui.app

import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.ui.builder.OptionStyle
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V64.4 — ورد واقعی گام ۳ (تغییر مدل داده، تأیید کاربر): استایل مستقل هر
 * گزینه (بولد/ایتالیک/اندازه). null = ارث از سؤال (سازگاری کامل عقب‌رو:
 * JSON قدیمی بدون optionStyles تغییری نمی‌کند و فیلد فقط وقتی استایلی
 * هست نوشته می‌شود). نوار ابزار با «گزینهٔ انتخابی» روی همان اثر می‌کند.
 */
class V64_4OptionStyleTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }

    // ---- ۱) قرارداد منبع ----

    @Test
    fun `toolbar formats the selected element instead of the whole question`() {
        assertTrue("fun setOptionStyle(id: String, index: Int, change: (OptionStyle) -> OptionStyle)" in builderVm)
        assertTrue("if (element != null && element.second == \"opt\")" in editor)
        assertTrue("builder.setOptionStyle(element.first, element.third) { it.copy(bold = !it.bold) }" in editor)
        assertTrue("val hasQuestion = question != null || hasElement" in editor)
        // رندر ویرایشگر استایل عنصر را نشان می‌دهد
        assertTrue("question.optionStyles.getOrNull(index)?.bold == true" in editor)
        // چاپ همان را می‌خواند
        assertTrue("val optionStyle = question.optionStyles.getOrNull(index)" in pdfAdapter)
        assertTrue("bold=optionBold,italic=optionItalic" in pdfAdapter)
    }

    @Test
    fun `all option operations keep styles aligned`() {
        // درج/حذف/جابجایی/تغییر تعداد همگی optionStyles را هم‌تراز نگه می‌دارند
        assertTrue("fun List<OptionStyle?>.padStyles(size: Int)" in builderVm)
        assertEquals(4, builderVm.split("padStyles(question.options.size).toMutableList()").size - 1)
    }

    // ---- ۲) تست اجرایی JVM: سازگاری عقب‌رو codec ----

    @Test
    fun `codec roundtrips option styles and stays backward compatible`() {
        val question = QuestionDraft(
            type = QuestionType.MULTIPLE_CHOICE,
            text = "کدام درست است؟",
            options = listOf("الف", "ب", "ج", "د"),
            optionIds = List(4) { "id$it" },
            optionImages = List(4) { null },
            optionStyles = listOf(null, OptionStyle(bold = true, fontSizeSp = 22f), null, OptionStyle(italic = true)),
            correctIndex = 1
        )
        val encoded = ExamQuestionCodec.encode(listOf(question))
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        // استایل‌ها پس از رفت‌وبرگشت حفظ می‌شوند
        assertNull(decoded.optionStyles[0])
        assertEquals(true, decoded.optionStyles[1]?.bold)
        assertEquals(22f, decoded.optionStyles[1]?.fontSizeSp)
        assertNull(decoded.optionStyles[2])
        assertEquals(true, decoded.optionStyles[3]?.italic)
        assertNull(decoded.optionStyles[3]?.fontSizeSp)
    }

    @Test
    fun `plain questions do not gain an optionStyles field`() {
        val plain = QuestionDraft(
            type = QuestionType.MULTIPLE_CHOICE,
            text = "ساده",
            options = listOf("الف", "ب"),
            optionIds = List(2) { "id$it" },
            optionImages = List(2) { null },
            correctIndex = 0
        )
        val encoded = ExamQuestionCodec.encode(listOf(plain))
        // JSON قدیمی دست‌نخورده: فیلد جدید فقط وقتی استایلی هست نوشته می‌شود
        assertFalse("optionStyles" in encoded.publicQuestions.toString())
        val decoded = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertTrue(decoded.optionStyles.all { it == null } || decoded.optionStyles.isEmpty())
    }
}
