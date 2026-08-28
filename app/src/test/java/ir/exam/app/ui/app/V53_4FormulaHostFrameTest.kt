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
 */
class V53_4FormulaHostFrameTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val asset by lazy { source("app/src/main/assets/question_editor/question_editor.html") }
    private val host by lazy { source("app/src/main/java/ir/exam/app/ui/math/FormulaHostDialog.kt") }
    private val webField by lazy { source("app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }

    @Test
    fun `reference frame and label render byte identical with no compose duplicate`() {
        // V54.4 — قاب/برچسب مرجع داخل HTML دست‌نخورده می‌ماند؛ CSS دستکاری قاب حذف شد.
        assertFalse(".field>span{display:none" in asset)
        assertFalse(".exam-question-editor-shell{padding:0 !important" in asset)
        // Compose هیچ برچسب/قاب تکراری دور WebView نمی‌کشد.
        val webSection = source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt")
        assertFalse("\"متن سؤال\"" in webSection)
        assertFalse("BorderStroke" in webSection)
        assertTrue("BasicTextField(" in webSection)
    }

    @Test
    fun `formula never boots the inline iframe in nativeTools mode`() {
        // بازتعریف __openMathEditor در حالت nativeTools رویداد را به Native می‌فرستد.
        val nativeBlock = asset.substringAfter("V53.4 — فرمول دیگر داخل همین WebView")
        assertTrue("window.__openMathEditor = function ()" in nativeBlock)
        assertTrue("ExamEditorNative.onOpenFormula(t.value || '', s, en)" in nativeBlock)
        // پل Kotlin
        assertTrue("fun onOpenFormula(text: String?, selStart: Int, selEnd: Int)" in webField)
        assertTrue("nativeOpenFormula" in webSection)
        assertTrue("controller.openTool(\"formula\")" in webSection)
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
        // حالت میزبان asset: صفحهٔ مرجع قابل مشاهده می‌ماند و ویرایشگر فوراً باز می‌شود.
        assertTrue("formulaHost = /[?&]formulaHost=1/" in asset)
        assertTrue("window.ExamEditorFormula" in asset)
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
