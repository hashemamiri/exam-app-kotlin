package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V53.1:
 * ۱) کادر متن سؤال WebView جایگزین کادر Native قبلی در کارت سؤال است.
 * ۲) نوار ۸ آیکن کاملاً Native با ترتیب مرجع زیر کادر است؛ toolbar داخلی HTML مخفی است.
 * ۳) ویرایشگر جدول کاملاً Native با ۱۸ سبک و قرارداد `k='t'` مرجع.
 * ۴) رندر جدول در همان مسیر مشترک SVG (دانش‌آموز/Builder) و مسیر PDF.
 * ۵) WebView فقط در فایل‌های مجاز و بدون Secret.
 */
class V53WebFieldNativeToolsTableTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val webField by lazy { source("app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt") }
    private val toolIcons by lazy { source("app/src/main/java/ir/exam/app/ui/math/QuestionToolIcons.kt") }
    private val tableEditor by lazy { source("app/src/main/java/ir/exam/app/ui/figure/TableEditorDialog.kt") }
    private val tableRenderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/TableSvgRenderer.kt") }
    private val figureRenderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt") }
    private val figureSpec by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val asset by lazy { source("app/src/main/assets/question_editor/question_editor.html") }

    @Test
    fun `question card uses webview text field instead of native inline editor`() {
        assertTrue("QuestionTextWebSection(" in builder)
        assertTrue("questionFieldController" in builder)
        assertFalse("InlineMathTextEditor(" in builder.substringAfter("import"))
        assertTrue("RichTextSplitter.split" in webSection)
        assertTrue("BasicTextField(" in webSection)
        assertFalse("QuestionTextFieldWebView(" in webSection)
        assertTrue("insertFigureJson" in builder)
    }

    @Test
    fun `native toolbar has all eight icons in reference order`() {
        val order = listOf(
            "درج فرمول", "درج شکل", "درج نمودار", "درج جدول",
            "درج آناتومی بدن", "درج جدول تناوبی", "درج فیزیک", "درج شیمی"
        )
        var cursor = -1
        order.forEach { label ->
            val at = webSection.indexOf(label)
            assertTrue("missing tool: $label", at >= 0)
            assertTrue("out of order: $label", at > cursor)
            cursor = at
        }
        // همهٔ آیکن‌ها ImageVector بومی‌اند؛ هیچ آیکنی از HTML نمی‌آید.
        listOf("Formula", "Figure", "Graph", "Table", "Anatomy", "Periodic", "Physics", "Chemistry").forEach {
            assertTrue("missing native icon: $it", "val $it: ImageVector" in toolIcons)
        }
        assertTrue("QuestionToolIcons" in webSection)
    }

    @Test
    fun `html toolbar is hidden only for the native field`() {
        assertTrue("nativeTools=1" in webField)
        assertTrue("nativeToolbarHide" in asset)
        assertTrue("[?&]nativeTools=1" in asset)
        assertTrue("exam-editor-native-tools" in asset)
        // پل درج توکن و بازکردن ابزار مرجع.
        assertTrue("ExamEditorTools" in asset && "insertToken" in asset && "openTool" in asset)
        assertTrue("onOverlayChanged" in asset && "onOverlayChanged" in webField)
    }

    @Test
    fun `table editor is fully native with reference contract`() {
        assertTrue("TableEditorDialog(" in builder)
        assertTrue("TableTarget" in builder)
        // ۱۸ سبک مرجع
        listOf(
            "header", "head2", "simple", "striped", "lined", "boxed", "exam", "matrix", "truth",
            "freq", "check", "color", "account", "round", "grid", "note", "blue", "compact"
        ).forEach { assertTrue("missing style: $it", "\"$it\"" in tableRenderer) }
        assertTrue("MAX_ROWS = 15" in tableRenderer && "MAX_COLS = 10" in tableRenderer)
        // قرارداد داده مرجع
        assertTrue("buildTable" in figureSpec && "JsonPrimitive(\"t\")" in figureSpec)
        assertTrue("tableCells" in figureSpec)
        // ویرایشگر جدول WebView ندارد.
        assertFalse("android.webkit" in tableEditor)
        assertFalse("WebView" in tableEditor)
    }

    @Test
    fun `table renders through shared svg path and pdf path`() {
        // مسیر مشترک: دانش‌آموز/Builder از FigureSvgRenderer.render عبور می‌کنند.
        assertTrue("if (spec.isTable) return TableSvgRenderer.render(spec)" in figureRenderer)
        // PDF: توکن‌های %%FIG%% به تصویر برداری تبدیل می‌شوند نه JSON خام.
        assertTrue("RichTextSplitter.split(question.text)" in pdfAdapter)
        assertTrue("figureBitmap" in pdfAdapter)
        assertTrue("com.caverock.androidsvg.SVG" in pdfAdapter)
        // امنیت SVG جدول: markup تولیدی بدون اسکریپت/URL خارجی است.
        // (خود واژه‌ها در کامنت مستندات فایل مجازند؛ فقط tag/attr واقعی ممنوع است.)
        assertFalse("<script" in tableRenderer)
        assertFalse("href=" in tableRenderer)
        assertFalse("<foreignObject" in tableRenderer)
        assertFalse("<style" in tableRenderer)
    }

    @Test
    fun `webview surface stays local and secret free`() {
        assertTrue("exam-editor.local" in webField)
        assertTrue("shouldOverrideUrlLoading" in webField)
        assertTrue("allowUniversalAccessFromFileURLs = false" in webField)
        assertFalse("SUPABASE" in webField)
        assertFalse("access_token" in webField)
        // پلاک موقت انواع مرجع تا V53.2/V53.3 — JSON خام نمایش داده نمی‌شود.
        assertTrue("renderKindPlate" in figureRenderer)
    }
}
