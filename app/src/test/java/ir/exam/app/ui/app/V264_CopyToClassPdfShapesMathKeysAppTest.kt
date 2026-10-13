package ir.exam.app.ui.app

import ir.exam.app.core.geometry.ShapeRecognizer
import ir.exam.app.core.geometry.ShapeRecognizer.Kind
import ir.exam.app.core.geometry.ShapeRecognizer.Pt
import ir.exam.app.core.io.ReportCardPdf
import ir.exam.app.ui.student.MathKeys
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V264 — آیتم ۱۱ (کپی آزمون به کلاس) + PDF کارنامهٔ دانش‌آموز + آیتم ۱۲ (تشخیص شکل تخته، صفحه‌کلید ریاضی).
 */
class V264_CopyToClassPdfShapesMathKeysAppTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(path: String) = File(root(), path).readText()
    private val rnd = Random(7)
    private fun jitter(x: Float, y: Float, s: Float = 3f) = Pt(x + rnd.nextFloat() * 2 * s - s, y + rnd.nextFloat() * 2 * s - s)
    private fun poly(vararg v: Pt, n: Int = 60): List<Pt> {
        val out = mutableListOf<Pt>()
        val per = n / v.size
        for (i in v.indices) { val a = v[i]; val b = v[(i + 1) % v.size]; for (k in 0 until per) { val t = k.toFloat() / per; out += jitter(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) } }
        return out.take((out.size * 0.9f).toInt()) + jitter(v[0].x, v[0].y, 8f)
    }

    @Test
    fun `recognizer finds circle rect triangle line and rejects scribble`() {
        val circle = (0 until 50).map { i -> val t = i / 50f * 2 * PI.toFloat() * 0.95f; jitter(200 + 100 * cos(t), 200 + 70 * sin(t)) }
        assertEquals(Kind.CIRCLE, ShapeRecognizer.recognize(circle)?.kind)
        assertEquals(Kind.RECT, ShapeRecognizer.recognize(poly(Pt(100f, 100f), Pt(300f, 100f), Pt(300f, 250f), Pt(100f, 250f)))?.kind)
        assertEquals(Kind.TRIANGLE, ShapeRecognizer.recognize(poly(Pt(100f, 250f), Pt(300f, 250f), Pt(200f, 100f)))?.kind)
        val line = (0 until 40).map { i -> jitter(100f + i * 5, 100f + i * 2) }
        assertEquals(Kind.LINE, ShapeRecognizer.recognize(line)?.kind)
        val scribble = (0 until 60).map { i -> jitter(100f + i * 4, 150f + 40 * sin(i / 2f)) }
        assertNull(ShapeRecognizer.recognize(scribble))
        assertNull(ShapeRecognizer.recognize(circle.take(5)))
    }

    @Test
    fun `math keys keep numeric answers gradable`() {
        assertEquals("12.5", listOf("1", "2", ".", "5").fold("") { acc, k -> MathKeys.apply(acc, k, numeric = true) })
        assertEquals("12.5", MathKeys.apply("12.5", ".", numeric = true))
        assertEquals("-3", MathKeys.apply(MathKeys.apply("", "-", true), "3", true))
        assertEquals("3", MathKeys.apply("3", "-", numeric = true))
        assertEquals("1", MathKeys.apply("12", MathKeys.BACKSPACE, numeric = true))
        assertEquals("√2", MathKeys.apply("√", "2", numeric = false))
    }

    @Test
    fun `report card pdf paginates`() {
        val rows = (1..70).map { ReportCardPdf.Row("آزمون $it", "ریاضی", "18 از 20", "90%", "") }
        val pages = ReportCardPdf.layoutRows(rows)
        assertTrue(pages.size >= 2)
        assertEquals(70, pages.sumOf { it.size })
        assertEquals(1, ReportCardPdf.layoutRows(emptyList()).size)
    }

    @Test
    fun `copy to class is wired in sql, app and site`() {
        val sql = src("supabase/migrations/20261013_native_copy_exam_to_class_v264.sql")
        assertTrue("create or replace function public.native_duplicate_exam_to_class_v1(p_exam text, p_class uuid, p_operation uuid)" in sql)
        assertTrue("insert into public.exam_audience_classes(exam_id, class_id) values (v_new_id, p_class)" in sql)
        assertTrue("'audience', 'classes'" in sql)
        val repo = src("app/src/main/java/ir/exam/app/data/repository/SupabaseTeacherDashboardRepository.kt")
        assertTrue("\"native_duplicate_exam_to_class_v1\"" in repo)
        val vm = src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardViewModel.kt")
        assertTrue("fun duplicate(exam: ExamDashboardDto, classId: String? = null)" in vm)
        val screen = src("app/src/main/java/ir/exam/app/ui/dashboard/TeacherDashboardScreen.kt")
        assertTrue("Text(\"کپی به کلاس (اختیاری)\"" in screen)
        val app = src("site/src/app.js")
        assertTrue("rpcObj('native_duplicate_exam_to_class_v1', {p_exam: id, p_class: classId, p_operation: uuid()})" in app)
        assertTrue("function duplicateExamDlg(x, after)" in app)
        assertTrue("text: '🖨 ذخیرهٔ PDF', 'aria-label': 'ذخیرهٔ کارنامه به‌صورت PDF'" in app)
    }

    @Test
    fun `whiteboard shape toggle and math keypad exist in app and site`() {
        val board = src("app/src/main/java/ir/exam/app/ui/student/StudentWhiteboardDialog.kt")
        assertTrue("label = \"تشخیص شکل\"" in board)
        assertTrue("recognizeStroke(d)" in board)
        val exam = src("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        assertTrue("MathKeypad(numeric = true" in exam)
        assertTrue("MathKeypad(numeric = false" in exam)
        val results = src("app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt")
        assertTrue("ActivityResultContracts.CreateDocument(\"application/pdf\")" in results)
        val student = src("site/src/student.js")
        assertTrue("function recognizeShape(pts, minSize)" in student)
        assertTrue("'aria-label': 'تشخیص شکل'" in student)
        assertTrue("class: 'math-pad'" in student)
    }
}
