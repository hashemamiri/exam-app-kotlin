package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V139 — سایت فاز ۲: سازندهٔ آزمون در مرورگر؛ قرارداد ذخیره/کدگذاری عیناً مطابق اپ. */
class V139_SiteBuilderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String): String = File(root(), path).readText()

    @Test
    fun `builder mirrors native_save_exam_v2 payload and codec`() {
        val js = source("site/src/builder.js")
        listOf("operation_id", "shuffle_q", "shuffle_opt", "neg_marking", "teacher_message", "attempts_allowed", "attempt_on_timeout",
            "grade_policy", "attempt_cooldown", "answer_key", "audience", "classes", "students", "schools").forEach { assertTrue(it, "$it:" in js) }
        assertTrue("S.rpc('native_save_exam_v2', {p_payload: payload})" in js)
        listOf("my_classes", "my_students_for_pick", "native_teacher_schools_v61", "get_exam_audience", "native_exam_audience_schools_v61",
            "native_bank_snapshot_v1", "native_bank_add_v2").forEach { assertTrue(it, "'$it'" in js) }
        // ExamQuestionCodec: کلیدهای پاسخ در سؤال عمومی نمی‌مانند
        assertTrue("['correctOption', 'correctAnswer', 'accept', 'answer', 'tolerance', 'caseSensitive', 'matchAnswer', 'pairs']" in js)
        assertTrue("k.correctOption = q.correctIndex" in js && "k.matchAnswer = {}" in js && "k.accept = q.expectedText.split('|')" in js)
        // آپلود تصویر (V144): از طریق S.uploadMedia — همان پوشه‌ها/باکت SupabaseQuestionImageUploader در مسیر fallback
        assertTrue("S.uploadMedia(blob, 'image', folder, examId, 'webp', 'image/webp')" in js)
        val app = source("site/src/app.js")
        assertTrue("var MEDIA_BUCKET = 'exam-images'" in app)
        assertTrue("'/storage/v1/object/' + MEDIA_BUCKET + '/' + path" in app && "'x-upsert': 'false'" in app)
        assertTrue("const val BUCKET = \"exam-images\"" in source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt"))
        assertTrue("'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'" in js)
        // گفتار: پنجره خودش بسته نمی‌شود (V109)
        assertTrue("rec.onend = function () { try { rec.start(); } catch (e) {} }" in js)
        assertFalse("service_role" in js)
    }

    @Test
    fun `app shell routes builder and includes builder source`() {
        val app = source("site/src/app.js")
        assertTrue("window.SiteBuilder.page(c, view.arg)" in app)
        assertTrue("window.SiteBuilder.printExamsSection" in app)
        assertTrue("onSnapshot: opts.onSnapshot" in app && "ExamPrintRenderer.layoutSnapshot()" in app)
        assertTrue("o.figLayoutsJson = JSON.stringify(q.figLayouts)" in app)
        assertTrue("\"builder.js\"" in source("site/build_site.py"))
    }
}
