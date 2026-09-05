package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V87.5 — `runJs` یک lambda است، نه تابع؛ پس آرگومانِ پیش‌فرض ندارد و هر
 * فراخوانی باید هر دو پارامتر را بدهد. V87.4 چهار جا این را رعایت نکرد و
 * CI با «No value passed for parameter 'p2'» شکست.
 */
class V87_5RunJsAritryTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    /** انتهای پرانتزِ باز‌شده در [start]، با نادیده‌گرفتنِ رشته‌ها. */
    private fun closeParen(s: String, start: Int): Int {
        var depth = 1
        var i = start
        var inStr = false
        var quote = ' '
        while (i < s.length && depth > 0) {
            val ch = s[i]
            when {
                inStr && ch == '\\' -> i++
                inStr && ch == quote -> inStr = false
                !inStr && (ch == '"' || ch == '\'') -> { inStr = true; quote = ch }
                !inStr && ch == '(' -> depth++
                !inStr && ch == ')' -> depth--
            }
            i++
        }
        return i
    }

    @Test
    fun `runJs is a two parameter lambda`() {
        assertTrue("val runJs: (String, ((String?) -> Unit)?) -> Unit" in dialog)
    }

    @Test
    fun `every call site passes both arguments`() {
        val offenders = mutableListOf<Int>()
        Regex("""(?<![\w.])runJs\s*\(""").findAll(dialog).forEach { m ->
            val end = closeParen(dialog, m.range.last + 1)
            val args = dialog.substring(m.range.last + 1, end - 1)
            val after = dialog.substring(end).trimStart()
            // شمارشِ آرگومانِ سطحِ بالا
            var depth = 0
            var inStr = false
            var quote = ' '
            var count = if (args.isBlank()) 0 else 1
            var i = 0
            while (i < args.length) {
                val ch = args[i]
                when {
                    inStr && ch == '\\' -> i++
                    inStr && ch == quote -> inStr = false
                    !inStr && (ch == '"' || ch == '\'') -> { inStr = true; quote = ch }
                    !inStr && (ch == '(' || ch == '[' || ch == '{') -> depth++
                    !inStr && (ch == ')' || ch == ']' || ch == '}') -> depth--
                    !inStr && ch == ',' && depth == 0 -> count++
                }
                i++
            }
            val total = count + if (after.startsWith("{")) 1 else 0
            if (total < 2) offenders += dialog.take(m.range.first).count { it == '\n' } + 1
        }
        assertEquals("این خطوط آرگومانِ دوم را نمی‌دهند: $offenders", emptyList<Int>(), offenders)
    }

    @Test
    fun `the four V87_4 call sites pass null explicitly`() {
        // V89.3 — دکمهٔ چشم از `togglePreviewWindow` به پلِ «همیشه باز»
        // (`__qmfShowPreview`) رفت و حالا callback دارد، پس دیگر جزوِ
        // فراخوانی‌های `, null` نیست. سه موردِ دیگر سرِ جایشان‌اند.
        listOf("printStudent();\", null)", "printTeacher();\", null)", "clearAutosave")
            .forEach { assertTrue("$it اصلاح نشده", it in dialog) }
        // و سنجهٔ اصلیِ این کلاس (هر فراخوانی هر دو آرگومان را می‌دهد)
        // در آزمونِ بالا مستقل بررسی می‌شود.
    }
}
