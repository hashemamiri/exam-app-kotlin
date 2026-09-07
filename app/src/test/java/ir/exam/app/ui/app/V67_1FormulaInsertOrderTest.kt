package ir.exam.app.ui.app

import ir.exam.app.core.text.RichTextSplitter
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V67.1 — رفع «درج فرمول به ابتدای متن»:
 * ریشه: auto-open مرجع در load + 60ms با textarea خالی و انتخاب (0,0) بازهٔ
 * __HOST_SAVED را ثبت می‌کرد؛ begin ما بعداً value/selection را درست می‌گذاشت
 * اما چون مودال از قبل باز بود، بازه هرگز دوباره ثبت نمی‌شد و mfApply فرمول
 * را در آفست صفر درج می‌کرد.
 */
class V67_1FormulaInsertOrderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val asset by lazy { source("app/src/main/assets/formula_editor/formula.html") }
    private val version by lazy { source("app/src/main/assets/formula_editor/version.txt").trim() }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val controller by lazy { source("app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt") }
    private val section by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }

    // ---------- اجرایی JVM: بازهٔ تغییر پسوند/پیشوند مشترک ----------

    @Test
    fun `change range covers the inserted formula and ends right after it`() {
        val old = "با توجه به شکل پاسخ دهید"
        val caret = "با توجه به شکل".length
        val token = "\$x^2\$"
        val new = old.substring(0, caret) + token + old.substring(caret)
        val range = RichTextSplitter.changeRangeAfterEdit(old, new)
        assertEquals(caret, range.first)
        assertEquals(caret + token.length, range.last + 1)
    }

    @Test
    fun `change range handles append, replace and identical texts`() {
        // الحاق به انتها
        val appended = RichTextSplitter.changeRangeAfterEdit("سلام", "سلام \$a\$")
        assertEquals(4, appended.first)
        assertEquals("سلام \$a\$".length, appended.last + 1)
        // بدون تغییر: بازهٔ خالی و without crash
        assertEquals(IntRange.EMPTY, RichTextSplitter.changeRangeAfterEdit("سلام", "سلام"))
        // جایگزینی وسط — پیشوند/پسوند مشترک، دلیمترهای $ را هم می‌بینند؛
        // بازهٔ تغییر باید داخل توکن بماند.
        val mid = RichTextSplitter.changeRangeAfterEdit("اب \$a\$ ج", "اب \$bbb\$ ج")
        assertEquals(4, mid.first)
        assertTrue(mid.last + 1 <= "اب \$bbb\$ ج".length - 2)
    }

    // ---------- قرارداد asset: همگام‌سازی بازه وقتی مودال از پیش باز است ----------

    @Test
    fun `begin re-syncs saved range when auto-open won the race`() {
        val bridge = asset.substringAfter("exam-formula-native-bridge")
        assertTrue("window.__beginRangeSynced = false" in bridge)
        assertTrue("__beginRangeSynced) {" in bridge)
        // حلقهٔ بازکردن V55.1 و رفتار خطای آن دست‌نخورده ماند.
        assertTrue("modal.classList.contains('open')" in bridge)
        assertTrue("FORMULA_OPEN_TIMEOUT" in bridge)
        assertTrue("window.__aoNativeClosing = false" in bridge)
        // انتخاب ارسالی Native همچنان در begin اعمال می‌شود.
        assertTrue("t.setSelectionRange(s | 0, e | 0)" in bridge)
    }

    @Test
    fun `asset version badge reflects the range-sync fix`() {
        assertEquals("v67.1-range-sync", version)
        assertTrue("__nativeBridgeVersion = 'N67.1'" in asset)
    }

    // ---------- قرارداد Kotlin: جریان مکان‌نما از نتیجهٔ پنجره تا کادر ----------

    @Test
    fun `caret offset flows from formula dialog result to native field`() {
        assertTrue("var pendingCaretOffset: Int? = null" in controller)
        assertTrue("questionFieldController.pendingCaretOffset =" in builder)
        assertTrue("RichTextSplitter.changeRangeAfterEdit(target.text, newText).last + 1" in builder)
        assertTrue("controller.pendingCaretOffset" in section)
        assertTrue("import ir.exam.app.core.text.RichTextSplitter" in builder)
    }
}
