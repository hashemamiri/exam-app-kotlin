package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V64.3 — بازآرایی معماری از بازبینی کدی کاربر (پچ پیشنهادی V64.1.1):
 * ۱) editing به state بالابردهٔ editingElement تبدیل شد: کنترل صریح از
 *    صفحه؛ لمس دوم = onStartEdit برای «هر» عنصری (نه فقط خالی) و Enter
 *    عنصر تازه را مستقیم در حالت ویرایش می‌گذارد.
 * ۲) ویرایش قطعه‌ای از منطق offset دست‌ساز به RichTextSplitter.split/
 *    reconstruct مهاجرت کرد (کد کمتر، ابزار core تست‌شده).
 * ۳) تست اجرایی JVM برای reconstruct (حفظ جای شکل/فرمول).
 */
class V64_3HoistedEditReconstructTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }

    // ---- ۱) قرارداد منبع ----

    @Test
    fun `element editing is hoisted and explicit`() {
        assertTrue("var editingElement by remember" in editor)
        assertTrue("onStartEditElement: (String, String, Int) -> Unit" in editor)
        assertTrue("if (selected) onStartEdit() else onSelect()" in editor)
        // Enter عنصر جدید را مستقیم در حالت ویرایش می‌گذارد
        assertTrue("editingElement = Triple(questionId, \"opt\", index + 1)" in editor)
        // state محلیِ ریست‌شونده حذف شد
        assertFalse("remember(text, selected)" in editor)
        assertFalse("mutableStateOf(selected && text.isEmpty())" in editor)
    }

    @Test
    fun `piecewise editing uses the tested core splitter`() {
        assertTrue("RichTextSplitter.split(question.text)" in editor)
        assertTrue("RichTextSplitter.reconstruct(parts, partIndex, value)" in editor)
        // منطق دست‌ساز offset حذف شد
        assertFalse("FormulaTextCodec.occurrences(raw)" in editor)
        assertFalse("tokens.forEach { (fromIdx, toIdx, kindOf) ->" in editor)
    }

    // ---- ۲) تست اجرایی JVM (از پچ پیشنهادی کاربر) ----

    @Test
    fun `editing a text segment keeps inline figures and formulas in place`() {
        val figureToken = "%%FIG:{\"t\":\"tri\"}%%"
        val text = "متن اول \$x\$ قبل " + figureToken + " بعد از شکل"
        val parts = RichTextSplitter.split(text)
        val textIndex = parts.indexOfLast { it is RichSegment.Text }
        val rebuilt = RichTextSplitter.reconstruct(parts, textIndex, "بعد ویرایش‌شده")

        val figureOccs = FigureCodec.occurrences(rebuilt)
        val formulaOccs = FormulaTextCodec.occurrences(rebuilt)
        assertTrue("شکل باید حفظ شود", figureOccs.size == 1)
        assertTrue("فرمول باید حفظ شود", formulaOccs.size == 1)
        val editedPos = rebuilt.indexOf("بعد ویرایش‌شده")
        assertTrue("متن ویرایش‌شده باید حاضر باشد", editedPos >= 0)
        assertTrue("شکل باید قبل از متن ویرایش‌شده بماند (به انتها نچسبد)",
            figureOccs[0].start < editedPos)
        assertTrue("فرمول باید قبل از شکل بماند (ترتیب حفظ شود)",
            formulaOccs[0].start < figureOccs[0].start)
    }

    @Test
    fun `reconstruct editing the first segment shifts tokens together`() {
        val text = "آغاز %%FIG:{\"t\":\"tri\"}%% پایان"
        val parts = RichTextSplitter.split(text)
        val firstText = parts.indexOfFirst { it is RichSegment.Text }
        val rebuilt = RichTextSplitter.reconstruct(parts, firstText, "شروع طولانی‌تر ")
        val occ = FigureCodec.occurrences(rebuilt).single()
        assertTrue(rebuilt.startsWith("شروع طولانی‌تر "))
        assertTrue("شکل بعد از متن جدید و قبل از «پایان»",
            occ.start == "شروع طولانی‌تر ".length && rebuilt.substring(occ.endExclusive).contains("پایان"))
    }
}
