package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V108–V111 — میکروفون گفتار به متن کنار آیکن تصویر (فقط متن، پنجرهٔ آرام). */
class V108_SpeechToTextTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `mic button is wired next to the image icon and only inserts plain text`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val button = source("app/src/main/java/ir/exam/app/ui/speech/SpeechToTextButton.kt")
        val manifest = source("app/src/main/AndroidManifest.xml")
        assertTrue("میکروفون کنار آیکن تصویر نیست", "ir.exam.app.ui.speech.SpeechToTextButton(onText = onSpeechText)" in media)
        assertTrue("متن گفتاری به متن سؤال نمی‌رود", "onSpeechText = { spoken ->" in builder)
        assertFalse("مسیر فرمول گفتاری هنوز هست", "onSpeechFormula" in builder || "onSpeechFormula" in media || "onFormula" in button)
        assertTrue("فقط تبدیل متنی استفاده نمی‌شود", "SpeechMathConverter.convertPlain(segment)" in button)
        assertFalse("مبدل فرمول در دکمه استفاده می‌شود", "SpeechMathConverter.convert(" in button || "toLatexOnly" in button)
        assertTrue("زبان فارسی نیست", "requestStart(\"fa-IR\")" in button)
        assertTrue("زبان انگلیسی نیست", "requestStart(\"en-US\")" in button)
        assertTrue("مجوز میکروفون در مانیفست نیست", "android.permission.RECORD_AUDIO" in manifest)
    }

    @Test
    fun `dialog is calm - no partials no rms no status flicker and closes only by its buttons`() {
        val button = source("app/src/main/java/ir/exam/app/ui/speech/SpeechToTextButton.kt")
        assertTrue("onDismissRequest = {}," in button)
        assertTrue("EXTRA_PARTIAL_RESULTS, false" in button)
        assertTrue("override fun onRmsChanged(rmsDb: Float) {}".replace("rmsDb", "rmsdB") in button)
        assertTrue("override fun onPartialResults(partialResults: Bundle?) {}" in button)
        assertFalse("LinearProgressIndicator" in button || "CircularProgressIndicator" in button)
        assertTrue("n-best استفاده نمی‌شود", "SpeechMathConverter.pickBest(list)" in button)
        assertTrue("شنیدن پیوسته نیست", "private class SpeechEngineHolder" in button)
    }
}
