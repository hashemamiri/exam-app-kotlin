package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V269 — بستر آزمون در گزارش نظارتی + پیش‌نمایش صوت در سازنده (اپ). */
class V269_MonitorPlatformCopyGuardTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(p: String) = File(root(), p).readText()

    @Test
    fun `app reports platform and labels new events`() {
        val vm = src("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        assertTrue("put(\"platform\", kotlinx.serialization.json.JsonPrimitive(\"app\"))" in vm)
        val g = src("app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt")
        assertTrue("\"copy_attempt\" -> \"تلاش برای کپی متن\"" in g)
        assertTrue("\"print_attempt\" -> \"تلاش برای چاپ/ذخیرهٔ صفحه\"" in g)
        assertTrue("Text(\"بستر آزمون: \${monitorPlatformLabel(report)}\"" in g)
        assertTrue("\"desktop\" -> \"سایت دسکتاپ\"" in g && "\"mobile\" -> \"سایت گوشی\"" in g && "\"app\" -> \"اپ اندروید\"" in g)
    }

    @Test
    fun `builder student preview plays question audio`() {
        val p = src("app/src/main/java/ir/exam/app/ui/builder/StudentQuestionPreview.kt")
        assertTrue("question.audioUri?.takeIf { it.isNotBlank() }?.let { audio ->" in p)
        assertTrue("ir.exam.app.ui.audio.QuestionAudioPlayer(url = audio, durationMs = question.audioMs, modifier = Modifier.fillMaxWidth())" in p)
    }
}
