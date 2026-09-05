package ir.exam.app.ui.app

import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.PrintableFromDrafts
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V86.9 — چشم همان پنجرهٔ آزمون‌سازِ چاپی را باز می‌کند، و کارتِ آزمونِ چاپی
 * پرینتر دارد که چاپ استاد/دانشجو را می‌آورد.
 */
class V86_9RealPreviewAndPrintTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val builder by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
    }
    private val center by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt").readText()
    }
    private val repo by lazy {
        File(root(), "app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    private fun q(
        type: QuestionType,
        text: String = "س",
        score: Double = 1.0,
        options: List<String> = emptyList(),
        correctIndex: Int? = null,
        expectedText: String = "",
        expectedNumber: String = "",
        tolerance: String = "0",
        matchingPairs: Map<Int, Int> = emptyMap()
    ) = QuestionDraft(
        type = type, text = text, score = score, options = options,
        correctIndex = correctIndex, expectedText = expectedText,
        expectedNumber = expectedNumber, tolerance = tolerance,
        matchingPairs = matchingPairs
    )

    @Test
    fun `the teacher answer key is produced for every question type`() {
        assertEquals(
            "ب",
            PrintableFromDrafts.answerTextFor(
                q(QuestionType.MULTIPLE_CHOICE, options = listOf("الف", "ب"), correctIndex = 1)
            )
        )
        assertEquals(
            "صحیح",
            PrintableFromDrafts.answerTextFor(q(QuestionType.TRUE_FALSE, expectedText = "true"))
        )
        assertEquals(
            "آب،اکسیژن",
            PrintableFromDrafts.answerTextFor(q(QuestionType.FILL_BLANK, expectedText = "آب|اکسیژن"))
        )
        assertEquals(
            "12 ± 0.5",
            PrintableFromDrafts.answerTextFor(
                q(QuestionType.NUMERIC, expectedNumber = "12", tolerance = "0.5")
            )
        )
        assertEquals(
            "1←3، 2←1",
            PrintableFromDrafts.answerTextFor(
                q(QuestionType.MATCHING, matchingPairs = mapOf(1 to 0, 0 to 2))
            )
        )
        // تشریحی کلید ندارد، وگرنه نسخهٔ استاد پاسخِ ساختگی چاپ می‌کند
        assertNull(PrintableFromDrafts.answerTextFor(q(QuestionType.ESSAY)))
    }

    @Test
    fun `a local exam becomes a printable with numbering and a total score`() {
        val p = PrintableFromDrafts.build(
            title = "میان‌ترم",
            subject = "ریاضی",
            header = OfficialPrintHeader(school = "فسا"),
            questions = listOf(
                q(QuestionType.ESSAY, score = 1.5),
                q(QuestionType.ESSAY, score = 2.0)
            )
        )
        assertEquals("میان‌ترم", p.documentTitle)
        assertEquals("ریاضی", p.subject)
        assertEquals("فسا", p.header.school)
        assertEquals(listOf(1, 2), p.questions.map { it.number })
        assertEquals(3.5, p.totalScore, 1e-9)
    }

    @Test
    fun `the eye opens the real printable window not the compose approximation`() {
        assertTrue("ir.exam.app.ui.printing.ExamHtmlPrintDialog(" in builder)
        assertTrue("printPreviewOf = ir.exam.app.domain.model.PrintableFromDrafts.build(" in builder)
        // و سربرگِ ذخیره‌شده را همراه می‌برد
        assertTrue("ir.exam.app.data.local.printHeaderOf(store.read())" in builder)
    }

    @Test
    fun `that window carries both the student and the teacher print buttons`() {
        // V87.4 — نام‌ها به خواستهٔ کاربر عوض شدند؛ مقصدِ پل همان است.
        assertTrue("چاپ آزمون" in dialog)
        assertTrue("چاپ با کلید" in dialog)
        assertTrue("printStudent()" in dialog)
        assertTrue("printTeacher()" in dialog)
    }

    @Test
    fun `the printer icon on a saved print exam opens the same window`() {
        assertTrue("contentDescription = \"چاپ آزمون چاپی\"" in center)
        assertTrue("htmlPrintExam = ir.exam.app.domain.model.PrintableFromDrafts.build(" in center)
        assertTrue("htmlPrintOpen = true" in center)
    }

    @Test
    fun `the server path and the local path share one mapping`() {
        // دو نسخه از منطقِ کلیدِ پاسخ یعنی نسخهٔ استاد در دو مسیر فرق می‌کند
        assertTrue("PrintableFromDrafts.questionAt(index, question)" in repo)
        assertTrue("QuestionType.MULTIPLE_CHOICE ->" !in repo)
    }

    @Test
    fun `the header mapping lives in one place`() {
        val store = File(root(), "app/src/main/java/ir/exam/app/data/local/PrintHeaderStore.kt").readText()
        assertTrue("fun printHeaderOf(" in store)
        assertTrue("ir.exam.app.data.local.printHeaderOf(headerStore.read())" in center)
    }
}
