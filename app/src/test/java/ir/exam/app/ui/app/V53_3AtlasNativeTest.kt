package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V53.3 — آناتومی + فیزیک/شیمی Native و ویرایش دوبار-کلیک:
 * ۱) کاتالوگ کامل: ۶۷ نوع آناتومی، ۱۵ دسته، ۷۷ نگاشت فایل، ۷۰ نوع علوم، ۸+۶ دسته.
 * ۲) اطلس تصاویر در asset: ۶۷ آناتومی + ۷۰ علوم.
 * ۳) نمایش/چاپ Native با قرارداد X مرجع (lab/blank/mkName/marks).
 * ۴) آیکن‌های آناتومی/فیزیک/شیمی به ویرایشگر Native وصل‌اند؛ openTool حذف شد.
 * ۵) دوبار-کلیک توکن‌های t/p/a/s داخل WebView به ویرایشگر Native می‌رود.
 */
class V53_3AtlasNativeTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val catalog by lazy { source("app/src/main/java/ir/exam/app/core/figure/AtlasCatalog.kt") }
    private val atlasView by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasFigureView.kt") }
    private val atlasEditor by lazy { source("app/src/main/java/ir/exam/app/ui/figure/AtlasEditorDialog.kt") }
    private val bitmapRenderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/AtlasBitmapRenderer.kt") }
    private val mathText by lazy { source("app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val webSection by lazy { source("app/src/main/java/ir/exam/app/ui/builder/QuestionTextWebSection.kt") }
    private val webField by lazy { source("app/src/main/java/ir/exam/app/ui/math/QuestionTextFieldWebView.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val asset by lazy { source("app/src/main/assets/question_editor/question_editor.html") }
    private val figureSpec by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSpec.kt") }

    @Test
    fun `catalog matches reference counts`() {
        assertEquals(67, Regex("""AtlasType\("[A-Za-z0-9_]+", "[a-z0-9]+", "[^"]+", "[^"]*"\),""").findAll(catalog).count())
        assertEquals(70, Regex("""AtlasType\("[A-Za-z0-9_]+", "[a-z0-9]+", "[^"]+"\),""").findAll(catalog).count())
        assertEquals(77, Regex("""to "atlas-[0-9]+\.jpg"|to "rbc\.jpg"|to "plate\.jpg"""").findAll(catalog).count())
        // دسته‌های مرجع
        listOf("بدن", "استخوان", "ماهیچه", "رگ و خون", "سه‌بعدی").forEach {
            assertTrue("missing anatomy cat: $it", "\"$it\"" in catalog)
        }
        listOf("مدار", "نیرو", "نور", "موج", "مغناطیس", "گرما", "هسته").forEach {
            assertTrue("missing phys cat: $it", "\"$it\"" in catalog)
        }
        listOf("آزمایشگاه", "مولکول", "اتم", "انرژی", "آلی").forEach {
            assertTrue("missing chem cat: $it", "\"$it\"" in catalog)
        }
        // inferDomain مرجع
        assertTrue("scienceDomain" in catalog && "\"chem\"" in catalog)
    }

    @Test
    fun `atlas images exist for every type`() {
        val anatomyDir = File(root(), "app/src/main/assets/figure_atlas/anatomy")
        val scienceDir = File(root(), "app/src/main/assets/figure_atlas/science")
        assertEquals(67, anatomyDir.listFiles()?.size ?: 0)
        assertEquals(70, scienceDir.listFiles()?.size ?: 0)
        // هر نوع علوم دقیقاً یک تصویر هم‌نام دارد.
        Regex("""AtlasType\("([A-Za-z0-9_]+)", "[a-z0-9]+", "[^"]+"\),""").findAll(catalog).forEach { m ->
            assertTrue("missing science image: ${m.groupValues[1]}",
                File(scienceDir, "${m.groupValues[1]}.jpg").isFile)
        }
    }

    @Test
    fun `native rendering honors reference X contract in view and pdf`() {
        listOf("\"lab\", \"1\"", "\"blank\", \"1\"", "\"mkName\", \"0\"", "marks()").forEach {
            assertTrue("view missing: $it", it in atlasView)
        }
        listOf("\"lab\", \"1\"", "\"blank\", \"1\"", "\"mkName\", \"0\"", "marks()").forEach {
            assertTrue("bitmap missing: $it", it in bitmapRenderer)
        }
        // مسیر دانش‌آموز و PDF
        assertTrue("AtlasFigureView(" in mathText)
        assertTrue("AtlasBitmapRenderer.render(context, spec)" in pdfAdapter)
        // هندسهٔ خالص فلش با تست‌پذیری JVM
        assertTrue("AtlasMarkPainter.arrowHead" in atlasView && "AtlasMarkPainter.arrowHead" in bitmapRenderer)
        // بدون WebView
        assertFalse("android.webkit" in atlasView)
        assertFalse("android.webkit" in atlasEditor)
        assertFalse("android.webkit" in bitmapRenderer)
    }

    @Test
    fun `atlas editor has reference behaviors`() {
        assertTrue("آناتومی بدن انسان" in atlasEditor)
        assertTrue("\"فیزیک\"" in atlasEditor && "\"شیمی\"" in atlasEditor)
        // حداکثر ۱۲ نشانه مثل مرجع + شمارهٔ آزاد بعدی
        assertTrue("marks.size < 12" in atlasEditor)
        assertTrue("nextMarkNumber" in atlasEditor)
        // سوییچ‌های مرجع
        listOf("نمایش عنوان", "جای پاسخ", "نمایش نام‌ها").forEach {
            assertTrue("missing switch: $it", it in atlasEditor)
        }
        // پیش‌فرض‌های مرجع: bodyF / cSim / beak
        assertTrue("\"bodyF\"" in atlasEditor && "\"cSim\"" in atlasEditor && "\"beak\"" in atlasEditor)
        assertTrue("buildAtlas" in figureSpec && "AtlasMark" in figureSpec)
    }

    @Test
    fun `all three icons open native editors and webview tools are gone`() {
        assertTrue("NativeToolButton(QuestionToolIcons.Anatomy, \"درج آناتومی بدن\", onInsertAnatomy)" in webSection)
        assertTrue("NativeToolButton(QuestionToolIcons.Physics, \"درج فیزیک\", onInsertPhysics)" in webSection)
        assertTrue("NativeToolButton(QuestionToolIcons.Chemistry, \"درج شیمی\", onInsertChemistry)" in webSection)
        assertFalse("openTool(\"anatomy\")" in webSection)
        assertFalse("openTool(\"physics\")" in webSection)
        assertFalse("openTool(\"chemistry\")" in webSection)
        assertTrue("AtlasEditorDialog(" in builder)
        // V55.12 — جریان دومرحله‌ای مثل «درج شکل»: اول انتخاب نوع.
        assertTrue("AtlasTarget(kind = \"s\", domain = \"chem\", chooseType = true)" in builder)
        assertTrue("AtlasTypePickerDialog(" in builder)
    }

    @Test
    fun `double click on native token kinds opens native editor`() {
        // پل asset
        assertTrue("onEditFigure" in asset)
        assertTrue("applyEditedToken" in asset && "cancelEditToken" in asset)
        assertTrue("kind !== 't' && kind !== 'p' && kind !== 'a' && kind !== 's'" in asset)
        // پل Kotlin
        assertTrue("fun onEditFigure(specJson: String?)" in webField)
        assertTrue("applyEditedFigureJson" in webField && "cancelEditFigure" in webField)
        // اتصال Builder: هر چهار نوع به ویرایشگر Native خودش می‌رود.
        assertTrue("onEditFigureToken = { rawJson ->" in builder)
        listOf("\"t\" -> tableTarget", "\"p\" -> periodicTarget", "\"a\" -> atlasTarget", "\"s\" -> atlasTarget").forEach {
            assertTrue("missing route: $it", it in builder)
        }
        assertTrue("editingWebToken" in builder && "deliverFigure" in builder)
    }
}
