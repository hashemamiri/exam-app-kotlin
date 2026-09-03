package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V53.4 — سه اشکال گزارش‌شدهٔ دستگاه:
 * ۱) «کادر داخل کادر»: قاب/برچسب داخلی HTML در حالت nativeTools مخفی می‌شود.
 * ۲) «صفحه تاریک بدون ویرایشگر»: فرمول دیگر داخل WebView کوچک باز نمی‌شود؛
 *    رویداد onOpenFormula به Native می‌رود.
 * ۳) «پنجرهٔ فرمول تمام‌WebView»: FormulaHostDialog تمام‌صفحه برای متن سؤال،
 *    گزینه‌ها و جورکردنی؛ دیالوگ AlertDialog قدیمی از Builder حذف شد.
 * (پاک‌سازی V74.0: asset قدیمی question_editor.html و WebView کادر متن سؤال حذف
 * شدند؛ این تست فقط مسیر زندهٔ FormulaHostDialog فرمول را نگه می‌دارد.)
 */
class V53_4FormulaHostFrameTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val host by lazy { source("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }

    @Test
    fun `question text section stays native compose without a webview field`() {
        assertFalse("\"متن سؤال\"" in webSection)
        assertFalse("BorderStroke" in webSection)
        assertTrue("BasicTextField(" in webSection)
        assertFalse("QuestionTextFieldWebView(" in webSection)
    }

    @Test
    fun `formula host is a full screen webview dialog`() {
        assertTrue("formula-editor/formula.html" in host)
        assertTrue("usePlatformDefaultWidth = false" in host)
        assertTrue("Modifier.fillMaxSize()" in host)
        // V55 — پنجرهٔ فرمول فایل مستقل formula.html با پل ExamFormulaHost است.
        assertTrue("ExamFormulaHost.begin(" in host)
        // پایان کار با بسته‌شدن ویرایشگر مرجع (رویداد overlay=false پس از باز شدن).
        assertTrue("onResult(latestText)" in host)
    }

    @Test
    fun `question options and matching all use the full screen formula window`() {
        assertTrue("formulaHost = FormulaHostTarget(text, selStart, selEnd)" in builder)
        assertTrue("FormulaHostDialog(" in builder)
        // مسیر گزینه/جورکردنی: متن کامل فیلد + محدودهٔ occurrence
        assertTrue("FormulaTextCodec.occurrences(sourceText).getOrNull(occ)" in builder)
        assertTrue("viewModel.updateMatchingText(question.id, \"left\", it, newText)" in builder)
        // دیالوگ کوچک قدیمی دیگر در Builder نیست.
        assertFalse("QuestionEditorWebViewDialog(" in builder)
    }
}
