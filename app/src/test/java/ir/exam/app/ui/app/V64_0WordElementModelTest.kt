package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V64.0 — گام نخست «ورد واقعی» (درخواست کاربر):
 * ۱) نقطه‌چین/خطوط پاسخ از ویرایشگر حذف شد؛ فضای پاسخ را کاربر با اینتر
 *    در متن سؤال می‌سازد (چاپ همچنان خطوط خودش را دارد).
 * ۲) هر عنصر سند مستقل است: هر گزینهٔ سؤال و هر سمت جفت جورکردنی
 *    بلوک جدا با انتخاب/ویرایش خودش (WordElement) — مثل Word که هر
 *    پاراگراف/باکس جدا انتخاب می‌شود؛ کلیک اول انتخاب، کلیک دوم ویرایش.
 */
class V64_0WordElementModelTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }

    @Test
    fun `answer dotted lines are gone from the editor`() {
        assertFalse("answerLineStyle == \"lined\"" in editor)
        assertFalse("ANSWER_LINE_HEIGHT_MM, zoom" in editor)
    }

    @Test
    fun `every option and matching side is its own selectable element`() {
        assertTrue("fun WordElement(" in editor)
        assertTrue("selected = selectedElement == (\"opt\" to index)" in editor)
        assertTrue("selected = selectedElement == (\"mL\" to index)" in editor)
        assertTrue("selected = selectedElement == (\"mR\" to index)" in editor)
        // کلیک اول انتخاب، کلیک دوم ویرایش درجا
        // V64.3 — ویرایش کنترل‌شده از بالا (onStartEdit).
        assertTrue("if (selected) onStartEdit() else onSelect()" in editor)
        // ذخیره از توابع موجود ویومدل
        assertTrue("builder.updateOption(questionId, index, text)" in editor)
        assertTrue("builder.updateMatchingText(questionId, \"left\", index, text)" in editor)
        assertTrue("builder.updateMatchingText(questionId, \"right\", index, text)" in editor)
    }

    @Test
    fun `element selection is exclusive with object and question selection`() {
        assertTrue("var selectedElement by remember" in editor)
        // V64.3 — پاک‌سازی انحصاری حالا editingElement را هم شامل می‌شود.
        assertTrue("selectedElement = null; editingElement = null" in editor)
    }
}
