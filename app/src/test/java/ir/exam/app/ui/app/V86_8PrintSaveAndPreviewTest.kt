package ir.exam.app.ui.app

import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.ui.printing.ExamHtmlPrintPayloadBuilder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V86.8 — تیک در مسیرِ چاپ محلی ذخیره می‌کند، چشم پیش‌نمایش را باز می‌کند،
 * و همهٔ میدان‌های سربرگ به برگهٔ چاپ می‌رسند.
 */
class V86_8PrintSaveAndPreviewTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val builder by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
    }
    private val center by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt").readText()
    }

    private fun printable(subject: String = "ریاضی") = OfficialExamPrintable(
        documentTitle = "آزمون",
        subject = subject,
        durationMinutes = 90,
        header = OfficialPrintHeader(school = "فسا", examDate = "1404/03/01"),
        questions = emptyList()
    )

    private fun fieldsOf(extra: Map<String, String>) =
        ExamHtmlPrintPayloadBuilder.build(printable(), extra)["fields"]!!.jsonObject

    @Test
    fun `without saved settings the payload is exactly what it always was`() {
        val f = fieldsOf(emptyMap())
        assertEquals("classic", f["f_headerTemplate"]!!.jsonPrimitive.content)
        assertEquals("ریاضی", f["f_course"]!!.jsonPrimitive.content)
        assertEquals("فسا", f["f_branch"]!!.jsonPrimitive.content)
    }

    @Test
    fun `every saved header field reaches the sheet not just the first four`() {
        val f = fieldsOf(
            mapOf(
                "f_professor" to "دکتر امیری",
                "f_department" to "مهندسی",
                "f_examType" to "پایان‌ترم",
                "f_gradesDate" to "1404/04/01",
                "f_studentCount" to "۳۰",
                "f_sheets" to "۲",
                "f_tools" to "ماشین‌حساب",
                "f_intro" to "به نام خدا"
            )
        )
        assertEquals("دکتر امیری", f["f_professor"]!!.jsonPrimitive.content)
        assertEquals("مهندسی", f["f_department"]!!.jsonPrimitive.content)
        assertEquals("پایان‌ترم", f["f_examType"]!!.jsonPrimitive.content)
        assertEquals("1404/04/01", f["f_gradesDate"]!!.jsonPrimitive.content)
        assertEquals("ماشین‌حساب", f["f_tools"]!!.jsonPrimitive.content)
        assertEquals("به نام خدا", f["f_intro"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an explicit value wins over the one derived from the exam`() {
        val f = fieldsOf(mapOf("f_course" to "فیزیک ۲"))
        assertEquals("فیزیک ۲", f["f_course"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a blank saved value must not wipe a good derived one`() {
        val f = fieldsOf(mapOf("f_course" to "", "f_branch" to "   "))
        assertEquals("ریاضی", f["f_course"]!!.jsonPrimitive.content)
        assertEquals("فسا", f["f_branch"]!!.jsonPrimitive.content)
    }

    @Test
    fun `only header keys may be injected`() {
        val f = fieldsOf(mapOf("evil" to "x", "questions" to "y"))
        assertFalse("evil" in f)
        assertFalse("questions" in f)
    }

    @Test
    fun `the tick does not hit the server on the print route`() {
        // چون آزمونِ چاپی عنوان و مخاطب ندارد و خطای «عنوان آزمون را وارد کنید» می‌گرفت
        assertTrue("onClick = { if (printMode) askPrintName = true else confirmSave = true }" in builder)
        assertTrue("ir.exam.app.data.local.PrintExamStore(nameContext)" in builder)
    }

    @Test
    fun `the tick asks for a name and refuses an empty one`() {
        assertTrue("Text(\"ذخیره آزمون چاپی\")" in builder)
        assertTrue("enabled = printExamName.isNotBlank() && state.questions.isNotEmpty()" in builder)
    }

    @Test
    fun `the eye opens the preview and only on the print route`() {
        assertTrue("if (printMode) {" in builder)
        assertTrue("contentDescription = \"پیش‌نمایش آزمون\"" in builder)
        assertTrue("onClick = { previewAll = true }" in builder)
    }

    @Test
    fun `saved print exams show up in the print centre and can be removed`() {
        assertTrue("items(localExams, key = { \"local-\" + it.id })" in center)
        assertTrue("printExamStore.delete(rec.id)" in center)
        assertTrue("onOpenLocalPrintExam(rec.id)" in center)
        // نشانه‌ای که آزمونِ محلی را از آزمونِ سرور جدا کند
        assertTrue("Text(\"چاپی\")" in center)
    }

    @Test
    fun `the empty state accounts for local exams too`() {
        assertTrue("state.exams.isEmpty() && localExams.isEmpty() && !state.loading" in center)
    }
}
