package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V64.2 — چهار باگ گزارش‌شدهٔ کاربر (بازبینی کدی دقیق خودش):
 * ۱) ویرایش گزینه فقط یک حرف دوام می‌آورد: remember(text, selected) با هر
 *    تایپ ریست می‌شد → کلید فقط selected + ورود خودکار ویرایش عنصر خالی
 *    (پس از Enter «آمادهٔ تایپ» واقعی است).
 * ۲) off-by-one در removeOptionAt: resizeIds/pad(size+1) بعد از removeAt
 *    یک عنصر اضافه می‌گذاشت → بدون +1.
 * ۳) توکن شکل هنگام ویرایش متن به انتهای سؤال می‌چسبید: بازسازی حالا روی
 *    «متن خام» با مرز فرمول+شکل انجام می‌شود؛ هر توکن سر جای خودش.
 * ۴) آناتومی/فیزیک (kind a/s) در ویرایشگر برچسب بود نه تصویر: مسیر
 *    AtlasFigureView (همان NativeMathText/چاپ) در ResizableFigure.
 */
class V64_2ElementEditBugfixTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }

    @Test
    fun `element editing survives typing and empty elements auto-edit`() {
        assertTrue("var editing by remember(selected) { mutableStateOf(selected && text.isEmpty()) }" in editor)
        assertFalse("remember(text, selected)" in editor)
    }

    @Test
    fun `remove option keeps ids and images aligned with options`() {
        val remove = builderVm.substringAfter("fun removeOptionAt(").substringBefore("fun moveOption(")
        assertTrue("resizeIds(question.options.size).toMutableList().apply { removeAt(index) }" in remove)
        assertTrue("pad(question.options.size).toMutableList().apply { removeAt(index) }" in remove)
        assertFalse("question.options.size + 1" in remove)
    }

    @Test
    fun `figure tokens stay in place while editing text`() {
        // بازسازی روی متن خام با مرز فرمول+شکل؛ بدون الحاق شکل به انتها
        assertTrue("figureOccurrences.forEach { add(Triple(it.start, it.endExclusive, 2)) }" in editor)
        assertFalse("onTextChange(rebuilt + suffix)" in editor)
        // شکل در حالت ویرایش داخل جریان است و بلوک نمایش دوباره نمی‌کشد
        assertTrue("if (!editable) figureOccurrences.forEachIndexed" in editor)
    }

    @Test
    fun `anatomy and science figures render the real image in the editor`() {
        assertTrue("if (spec.kind in setOf(\"a\", \"s\"))" in editor)
        assertTrue("ir.exam.app.ui.figure.AtlasFigureView(" in editor)
        assertTrue("showBlanks = false" in editor)
    }
}
