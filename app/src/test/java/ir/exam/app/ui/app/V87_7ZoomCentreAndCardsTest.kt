package ir.exam.app.ui.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V87.7 — زومِ جهت-آگنوستیک، کارتی‌شدنِ سؤال‌ها، هدرِ سفید و پیامِ محوشونده.
 */
class V87_7ZoomCentreAndCardsTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }
    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }

    @Test
    fun `centring asks the browser for the scroll range instead of guessing its sign`() {
        // در ظرفِ rtl دامنهٔ scrollLeft می‌تواند 0..over یا -over..0 باشد؛
        // V87.4 مقدارِ مثبت نوشت و WebView آن را به صفر گیره کرد.
        assertTrue("var min = pb.scrollLeft;" in asset)
        assertTrue("var max = pb.scrollLeft;" in asset)
        assertTrue("Math.round((min + max) / 2)" in asset)
        assertTrue("over > 1 ? Math.round(over / 2) : 0" !in asset)
    }

    @Test
    fun `with nothing to scroll it returns to the start`() {
        assertTrue("if (over <= 1) { pb.scrollLeft = 0; return; }" in asset)
    }

    @Test
    fun `the questions heading and total chip are gone from the editor`() {
        assertTrue("<h2 style=\"display:none\">" in asset)
        // ولی گره‌ها می‌مانند چون کد مقدارشان را به‌روز می‌کند
        assertTrue("id=\"totalScoreView\"" in asset)
        assertTrue("id=\"emptyMsg\" style=\"display:none\"" in asset)
        // برگهٔ چاپیِ خالی باید توضیحش را نگه دارد
        assertTrue("هنوز سؤالی به آزمون اضافه نشده است." in asset)
    }

    @Test
    fun `every question is a card, not just the active one`() {
        assertTrue("card.style.display = '';" in asset)
        assertTrue("card.style.display = collapsed ? 'none' : ''" !in asset)
        assertTrue(".question-card.collapsed > *:not(.q-header)" in asset)
    }

    @Test
    fun `the card header keeps its icons`() {
        assertTrue("class=\"q-move-group\"" in asset)
        assertTrue("class=\"q-remove\"" in asset)
        assertTrue("class=\"q-number\"" in asset)
        assertTrue("class=\"q-type-badge\"" in asset)
    }

    @Test
    fun `the header is white and its contents stay legible`() {
        assertTrue(".background(Color.White)" in dialog)
        assertTrue("tint = Color(0xFF1E3A8A)" in dialog)
        assertTrue("color = Color(0xFF111827)" in dialog)
    }

    @Test
    fun `status messages appear in the middle and fade by themselves`() {
        assertTrue("AnimatedVisibility(" in dialog)
        assertTrue("fadeIn()" in dialog)
        assertTrue("fadeOut()" in dialog)
        assertTrue("modifier = Modifier.align(Alignment.Center)" in dialog)
        assertTrue("kotlinx.coroutines.delay(2600)" in dialog)
    }
}
