package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** V108 — میکروفون گفتار به متن کنار آیکن تصویر (چاپی و آنلاین). */
class V108_SpeechToTextTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun source(path: String) = File(root(), path).readText()

    @Test
    fun `mic button is wired next to the image icon and routes formulas to the formula editor`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val builder = source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        val button = source("app/src/main/java/ir/exam/app/ui/speech/SpeechToTextButton.kt")
        val manifest = source("app/src/main/AndroidManifest.xml")
        assertTrue("میکروفون کنار آیکن تصویر نیست", "ir.exam.app.ui.speech.SpeechToTextButton(" in media)
        assertTrue("متن گفتاری به متن سؤال نمی‌رود", "onSpeechText = { spoken ->" in builder)
        assertTrue("فرمول گفتاری در ویرایشگر فرمول باز نمی‌شود", "formulaHost = FormulaHostTarget(withFormula, prefix.length, withFormula.length)" in builder)
        assertTrue("زبان فارسی نیست", "requestStart(\"fa-IR\")" in button)
        assertTrue("زبان انگلیسی نیست", "requestStart(\"en-US\")" in button)
        assertTrue("مبدل هوشمند استفاده نمی‌شود", "SpeechMathConverter.convert(spoken)" in button)
        assertTrue("مجوز میکروفون در مانیفست نیست", "android.permission.RECORD_AUDIO" in manifest)
    }
}
