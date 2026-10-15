package ir.exam.app.core.primary

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V309 / ۱۰ — پاسخ صوتی در سایت: ضبط دانش‌آموز، سقف مدت معلم، و پخش صوت در تصحیح. */
class V309_Site_VoiceAnswerTest {
    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists() && !File(dir, "settings.gradle").exists()) dir = dir.parentFile
        return dir ?: File("").absoluteFile
    }

    @Test
    fun `student site records voice answers with teacher limit and uploads audio with its own extension`() {
        val student = File(root(), "site/src/student.js").readText()
        assertTrue(student.contains("function voiceAnswerBlock(q, box)"))
        assertTrue(student.contains("q.mode === 'voice'"))
        assertTrue(student.contains("new MediaRecorder(stream, opts)"))
        assertTrue(student.contains("Math.max(10, Math.min(300, Number(q.voiceMaxSeconds) || 60))"))
        assertTrue(student.contains("function audioExtOf(t)"))
        assertTrue(student.contains("(isAudio ? '.' + audioExtOf(blob.type) : '.webp')"))
    }

    @Test
    fun `builder offers voice answers and stores the teacher limit`() {
        val builder = File(root(), "site/src/builder.js").readText()
        assertTrue(builder.contains("['voice', 'پاسخ صوتی (ضبط)']"))
        assertTrue(builder.contains("v.voice_max_seconds = Math.max(10, Math.min(300, Number(q.voiceMaxSeconds) || 60));"))
        assertTrue(builder.contains("q.voiceMaxSeconds = Number(obj.voice_max_seconds) || 0;"))
        assertTrue(builder.contains("function voiceSecondsEditor(q, redraw)"))
    }

    @Test
    fun `teacher grading shows audio answers with a player instead of an image`() {
        val admin = File(root(), "site/src/admin.js").readText()
        assertTrue(admin.contains("function answerMedia(u)"))
        assertTrue(admin.contains("el('audio', {src: u, controls: 'controls', preload: 'none'})"))
        assertTrue(admin.contains("qi.map(function (u) { return answerMedia(u)") || admin.contains("answerMedia(u)"))
    }
}
