package ir.exam.app.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V87.4 — بازچینشِ کنترل‌های آزمون‌سازِ چاپی: هدرِ سه‌جزئی، دکمه‌های شناور،
 * منویِ رادیالِ مشترک، و زومِ وسط‌چین.
 */
class V87_4PrintBuilderChromeTest {

    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val dialog by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt").readText()
    }
    private val asset by lazy {
        File(root(), "app/src/main/assets/print/exam_print.html").readText()
    }

    @Test
    fun `the header carries only back, a title and the header button`() {
        assertTrue("\"ساخت آزمون\"" in dialog)
        assertTrue("آزمون جدید — آزمون‌ساز" !in dialog)
        assertTrue("Icons.AutoMirrored.Outlined.ArrowBack" in dialog)
        assertTrue("\"سربرگ\"" in dialog)
        assertTrue("onLongClick" in dialog)
    }

    @Test
    fun `the second command bar is gone`() {
        listOf(
            "💾 ذخیره", "📂 بازکردن", "🖨 چاپ دانشجو", "✅ چاپ استاد",
            "➕ سوال جدید", "👁 پیش‌نمایش", "🩺 بررسی فرمول", "🗂 مدیریت سؤال"
        ).forEach { assertTrue("$it هنوز دکمهٔ نوار است", "NativeBarButton(\"$it\")" !in dialog) }
    }

    @Test
    fun `four floating controls replace it`() {
        assertEquals(4, Regex("FloatingActionButton\\(").findAll(dialog).count())
        assertTrue("Icons.Outlined.Check" in dialog)
        assertTrue("Icons.Outlined.Print" in dialog)
        assertTrue("Icons.Outlined.Visibility" in dialog)
        assertTrue("Text(\"+\"" in dialog)
    }

    @Test
    fun `the plus reuses the online builder menu instead of a new one`() {
        assertTrue("BuilderRadialMenuOverlay(" in dialog)
        // «وارد کردن» همان دکمهٔ حذف‌شدهٔ «بازکردن» را پوشش می‌دهد
        assertTrue(Regex("onImport = \\{[\\s\\S]{0,140}openExamPicker\\.launch").containsMatchIn(dialog))
    }

    @Test
    fun `every question type still reaches the page bridge`() {
        listOf(
            "MULTIPLE_CHOICE -> \"multiple\"", "TRUE_FALSE -> \"truefalse\"",
            "ESSAY -> \"long\"", "FILL_BLANK -> \"fill\"",
            "NUMERIC -> \"numeric\"", "MATCHING -> \"matching\""
        ).forEach { assertTrue("نگاشتِ $it نیست", it in dialog) }
        assertTrue("pickQuestionType('" in dialog)
    }

    @Test
    fun `the printer offers the two names the user asked for`() {
        assertTrue("Text(\"🖨 چاپ آزمون\"" in dialog)
        assertTrue("Text(\"✅ چاپ با کلید\"" in dialog)
        assertTrue("printStudent();" in dialog)
        assertTrue("printTeacher();" in dialog)
    }

    @Test
    fun `restore is a centred native dialog and the floating banner stands down`() {
        assertTrue("Text(\"بازیابی آزمون\")" in dialog)
        assertTrue("window.restoreAutosave()" in dialog)
        assertTrue("window.clearAutosave()" in dialog)
        assertTrue("!restoreAsked" in dialog)
        // بنر فقط داخلِ اپ خاموش است؛ در مرورگر همان قبلی
        assertTrue("window.__qmfMaybeShowBanner = maybeShowBanner" in asset)
        assertTrue("if (!(window.ExamPrintNative" in asset)
    }

    @Test
    fun `the empty-state notices are gone from the editor but not from print`() {
        assertTrue("هنوز سوالی ندارید" !in asset)
        assertTrue("id=\"emptyMsg\" style=\"display:none\"" in asset)
        // گره باید بماند: کد آن را show/hide می‌کند
        assertTrue(Regex("emptyMsg").findAll(asset).count() >= 4)
        // برگهٔ چاپیِ خالی باید توضیح بدهد
        assertTrue("هنوز سؤالی به آزمون اضافه نشده است." in asset)
    }

    @Test
    fun `zooming keeps the sheet centred`() {
        assertTrue("window.qmfCenterPreviewScroll" in asset)
        assertTrue(
            Regex("qmfSetPreviewZoom[\\s\\S]{0,700}qmfCenterPreviewScroll\\(\\)").containsMatchIn(asset)
        )
        // V87.7 — بازگشت به ۱x باید اسکرول را صفر کند، و مرکزیابی دیگر
        // علامتِ scrollLeft را حدس نمی‌زند بلکه دامنه را می‌سنجد.
        assertTrue("if (over <= 1) { pb.scrollLeft = 0; return; }" in asset)
        assertTrue("Math.round((min + max) / 2)" in asset)
        // مبدأِ RTL از V86.6 نباید عوض شود
        assertTrue("'transform-origin', 'top right'" in asset)
    }

    @Test
    fun `removing buttons did not remove any bridge`() {
        listOf("__qmfQuestionList", "__qmfFormulaDiag", "__qmfExportJson", "__qmfSaveNow")
            .forEach { assertTrue("پل $it حذف شده", it in asset) }
    }
}
