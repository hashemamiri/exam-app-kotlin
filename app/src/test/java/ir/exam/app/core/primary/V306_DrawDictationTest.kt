package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V306 — پاسخ با تخته (تشریحی) و املا (جای خالی + «بخوان») در حالت ابتدایی؛ قراردادهای اپ و کُدگذاری. */
class V306_DrawDictationTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    private fun src(path: String): String = File(root(), "app/src/main/java/ir/exam/app/$path").readText()

    @Test
    fun `models carry answer mode and dictation text`() {
        val models = src("domain/model/ExamModels.kt")
        assertTrue(models.contains("val answerMode: String = \"text\""))
        assertTrue(models.contains("val dictationText: String? = null"))
    }

    @Test
    fun `payload codec reads draw and dictation modes from public json only`() {
        val payload = src("data/repository/StudentExamPayloadCodec.kt")
        assertTrue(payload.contains("obj.text(\"mode\")?.takeIf { it in setOf(\"draw\", \"voice\", \"steps\", \"proof\", \"construct\") }"))
        assertTrue(payload.contains("obj.text(\"mode\") == \"dictation\""))
        assertTrue(payload.contains("obj.text(\"speak\")"))
        // «speak» عمومی است؛ کلید پاسخ (accept) همچنان حذف می‌شود
        assertTrue(payload.contains("\"accept\""))
    }

    @Test
    fun `codec writes speak text only when teacher audio is absent`() {
        val codec = src("data/repository/ExamQuestionCodec.kt")
        assertTrue(codec.contains("if (question.audioUri.isNullOrBlank()) values[\"speak\"]"))
        assertTrue(codec.contains("values.remove(\"speak\")"))
        assertTrue(codec.contains("question.type == QuestionType.ESSAY && question.mode == \"draw\""))
    }

    @Test
    fun `builder offers draw and dictation choices in primary mode`() {
        val builder = src("ui/builder/ExamBuilderScreen.kt")
        assertTrue(builder.contains("\"construct\" to \"رسم/ساخت روی تخته\""))
        assertTrue(builder.contains("\"draw\" to \"نقاشی آزاد\""))
        assertTrue(builder.contains("\"dictation\" to \"املا (گوش بده و بنویس)\""))
        assertTrue(builder.contains("private fun SecondaryQuestionControls"))
        assertTrue(builder.contains("if (question.type == QuestionType.ESSAY)"))
    }

    @Test
    fun `student screen shows big whiteboard for draw and reads dictation text`() {
        val student = src("ui/student/StudentExamScreen.kt")
        assertTrue(student.contains("val drawAnswer = (question as? EssayQuestion)?.answerMode in setOf(\"draw\", \"construct\", \"voice\")"))
        assertTrue(student.contains("big = true"))
        assertTrue(student.contains("if (!drawAnswer) if (presentation.allowAnswerGraph || questionHasGraph) {"))
        assertTrue(student.contains("(question as? FillBlankQuestion)?.dictationText ?:"))
        assertTrue(student.contains("if(!answered && state.responseImages[q.id].orEmpty().isEmpty())"))
    }
}
