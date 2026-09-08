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
        assertTrue("مبدل هوشمند استفاده نمی‌شود", "SpeechMathConverter.convert(transcript)" in button)
        // V109 — پنجره فقط با دکمه‌های خودش بسته می‌شود؛ شنیدن پیوسته؛ n-best؛ ویرایش پیش از درج
        assertTrue("پنجره با لمس بیرون بسته می‌شود", "onDismissRequest = {}," in button)
        assertTrue("شنیدن پیوسته نیست", "fun scheduleNextSegment(" in button)
        assertTrue("n-best استفاده نمی‌شود", "SpeechMathConverter.pickBest(list)" in button && "EXTRA_MAX_RESULTS, 5" in button)
        assertTrue("متن پیش از درج قابل ویرایش نیست", "label = { Text(\"متن شنیده‌شده (قابل ویرایش)\") }" in button)
        assertTrue("اصلاح خطاهای رایج نیست", "fun correct(spoken: String): String" in source("app/src/main/java/ir/exam/app/core/speech/SpeechMathConverter.kt"))
        assertTrue("مجوز میکروفون در مانیفست نیست", "android.permission.RECORD_AUDIO" in manifest)
    }
}
