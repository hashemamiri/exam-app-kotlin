package ir.exam.app.ui.app

import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.audioChargeForBytes
import ir.exam.app.ui.builder.chargeToman
import ir.exam.app.ui.builder.imageCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V135 — صوت سؤال (آزمون آنلاین) + هزینهٔ تصویر/صوت + کادر جداگانهٔ فرمول.
 */
class V135AudioMediaCostTest {
    private fun source(path: String): String = File(path).readText()
    private val transcoder by lazy { source("app/src/main/java/ir/exam/app/core/audio/AudioTranscoder.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/audio/QuestionAudioEditorDialog.kt") }
    private val player by lazy { source("app/src/main/java/ir/exam/app/ui/audio/QuestionAudioPlayer.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val student by lazy { source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt") }
    private val codec by lazy { source("app/src/main/java/ir/exam/app/data/repository/ExamQuestionCodec.kt") }
    private val uploader by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt") }
    private val migration by lazy { source("supabase/migrations/20260909_native_media_cost_v135.sql") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }

    @Test
    fun `audio tiers are 2000 4000 6000 up to 3MB`() {
        assertEquals(0L, audioChargeForBytes(0))
        assertEquals(2_000L, audioChargeForBytes(1))
        assertEquals(2_000L, audioChargeForBytes(1L * 1024 * 1024))
        assertEquals(4_000L, audioChargeForBytes(1L * 1024 * 1024 + 1))
        assertEquals(4_000L, audioChargeForBytes(2L * 1024 * 1024))
        assertEquals(6_000L, audioChargeForBytes(3L * 1024 * 1024))
    }

    @Test
    fun `question with five images costs 6000 and audio adds its tier`() {
        val q = QuestionDraft(
            type = QuestionType.MULTIPLE_CHOICE,
            options = listOf("a", "b", "c", "d"),
            optionImages = listOf("x", null, "y", ""),
            images = List(3) { MediaDraft(uri = "u$it") }
        )
        assertEquals(5, q.imageCount())
        assertEquals(6_000L, q.chargeToman())
        val withAudio = q.copy(audioUri = "file:///a.m4a", audioBytes = 1_500_000L)
        assertEquals(10_000L, withAudio.chargeToman())
    }

    @Test
    fun `native transcoder is adaptive aac with 3MB hard cap and no ffmpeg`() {
        assertTrue("MAX_BYTES: Long = 3L * 1024L * 1024L" in transcoder)
        assertTrue("MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)" in transcoder)
        assertTrue("intArrayOf(128_000, 96_000, 80_000, 64_000, 48_000, 40_000, 32_000)" in transcoder)
        assertTrue("class AudioTooLargeException" in transcoder)
        assertTrue("fun waveform(" in transcoder && "fun transcode(" in transcoder)
        assertFalse("ffmpeg" in transcoder.lowercase())
    }

    @Test
    fun `editor trims with waveform handles and shows live size and cost`() {
        assertTrue("WaveformTrimmer(" in editor)
        assertTrue("AudioTranscoder.plannedBitrate(selMs)" in editor)
        assertTrue("حجم تقریبی خروجی" in editor)
        assertTrue("audioChargeForBytes(estBytes)" in editor)
        assertTrue("res.bytes > AudioTranscoder.MAX_BYTES" in editor)
        assertTrue("ActivityResultContracts.OpenDocument()" in editor && "arrayOf(\"audio/*\")" in editor)
    }

    @Test
    fun `student player has play pause and seekable slider with auth headers`() {
        assertTrue("Slider(" in player && "onValueChangeFinished" in player)
        assertTrue("Icons.Outlined.Pause" in player && "Icons.Outlined.PlayArrow" in player)
        assertTrue("\"Authorization\" to \"Bearer \$token\"" in player)
        assertTrue("presentation.audioUrl?.let { audio ->" in student)
        assertTrue("QuestionAudioPlayer(url = audio, durationMs = presentation.audioMs" in student)
    }

    @Test
    fun `builder wires music icon online only and persists audio fields`() {
        assertTrue("onOpenAudio = if (printMode) null else ({ audioEditorOpen = true })" in builder)
        assertTrue("QuestionAudioEditorDialog(" in builder)
        assertTrue("Icons.Outlined.MusicNote" in source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt"))
        assertTrue("values[\"audio\"] = JsonPrimitive(question.audioUri)" in codec)
        assertTrue("values[\"audioBytes\"] = JsonPrimitive(question.audioBytes)" in codec)
        assertTrue("uploadAudioAt(\"audio/\$teacherId/\$examId\"" in uploader)
        assertTrue("MAX_AUDIO_BYTES = 3 * 1024 * 1024" in uploader)
    }

    @Test
    fun `server bills images and audio for changed questions and returns breakdown`() {
        assertTrue("native_question_media_cost_v135" in migration)
        assertTrue("v_cost := v_billable * 1000 + v_img_cost + v_audio_cost;" in migration)
        assertTrue("= any(v_billed_keys)" in migration)
        assertTrue("when v_audio_bytes <= 1 * 1024 * 1024 then 2000" in migration)
        assertTrue("'image_cost', v_img_cost" in migration && "'audio_cost', v_audio_cost" in migration)
        assertTrue("'audio'" in migration && "storage.foldername(name))[1] in ('avatars','questions','option_images','matching','answers','audio')" in migration)
        assertTrue("جمع: \${state.maximumChargeToman.asToman()} تومان" in builder)
        assertTrue("تصاویر (سؤال، گزینه‌ها، جورکردنی)" in builder)
    }

    @Test
    fun `each formula has its own persistent frame in the question text box`() {
        assertTrue("MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)" in webSection)
        assertTrue("RoundedCornerShape(6.dp)" in webSection)
        assertTrue("Modifier.height(boxHeight + 6.dp)" in webSection)
    }
}
