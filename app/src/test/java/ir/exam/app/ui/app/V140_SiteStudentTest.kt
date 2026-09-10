package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V140 — سایت فاز ۳: پنل دانش‌آموز؛ قراردادها عیناً مطابق SupabaseStudentExamRepository/StudentExamPayloadCodec. */
class V140_SiteStudentTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `student module mirrors app contracts`() {
        val js = source("site/src/student.js")
        assertTrue("S.rpcObj('get_exam_for_student', {p_code: code.trim()})" in js)
        assertTrue("'native_submit_queued_answer_v1', {p_operation: uuid(), p_exam: ex.id, p_responses: responses, p_images: images, p_meta:" in js)
        assertTrue("'native_monitor_upsert_v1', {p_exam:" in js)
        // پاک‌سازی کلید پاسخ مثل StudentExamPayloadCodec.answerFields
        assertTrue("['correctOption', 'correctIndex', 'correctAnswer', 'accept', 'answer', 'tolerance', 'matchAnswer', 'explanation', 'answer_key']" in js)
        // StableExamShuffle: LCG 1664525/1013904223 و seed «student:exam:index:options»
        assertTrue("1664525n + 1013904223n" in js && "':options'" in js && "':questions'" in js && "':matching'" in js)
        // پاسخ‌ها به ترتیب اصلی سؤال (PendingSubmissionCodec) و تصاویر پاسخ در مسیر answers/<student>/<exam>/<question>
        assertTrue("sort(function (a, b) { return a.originalIndex - b.originalIndex; })" in js)
        assertTrue("'answers/' + S.user().id + '/' + examId + '/' + questionId + '/'" in js)
        assertTrue("var WHITEBOARD_MAX_PAGES = 6;" in js)
        // V135.9 — پیوستن به آزمون نیمه‌تمام به‌جای بازشدن خودکار
        assertTrue("'پیوستن به آزمون'" in js && "آزمون نیمه‌تمام دارید" in js)
        assertTrue("question_time_ms" in js && "question_labels" in js && "app_leave" in js)
        assertFalse("service_role" in js)
    }

    @Test
    fun `app shell routes join page and build includes student source`() {
        assertTrue("window.SiteStudent.page(c, view.arg)" in source("site/src/app.js"))
        assertTrue("\"student.js\"" in source("site/build_site.py"))
        assertTrue("SUPABASE_ANON_KEY" in source("site/src/template.html"))
        assertFalse("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSI" in source("site/index.html"))
    }
}
