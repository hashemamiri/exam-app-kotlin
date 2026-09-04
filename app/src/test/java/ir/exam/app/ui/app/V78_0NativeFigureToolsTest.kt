package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V78.0 — هفت ابزارِ درج در آزمون‌سازِ چاپ (نسخهٔ ۳۰) به همان ویرایشگرهای
 * بومی وصل شدند که آزمون‌سازِ آنلاین از V53 استفاده می‌کند. هیچ ویرایشگری
 * از نو نوشته نشده؛ فقط سیم‌کشی شده است.
 *
 * قرارداد مشترک: خروجی هر ویرایشگر یک FigureSpec است که به شکل توکنِ
 * %%FIG:{json}%% در متنِ سؤال درج می‌شود و رندرش همچنان کارِ renderFigToken
 * در HTML است — پس خروجی چاپ تغییری نمی‌کند.
 *
 * «فرمول» به‌خواستِ صریحِ کاربر بومی نشده و مسیر HTML خودش را دارد.
 */
class V78_0NativeFigureToolsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val assetText by lazy { source("app/src/main/assets/print/exam_print.html") }
    private val host by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamFigureToolHost.kt") }
    private val dialog by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamHtmlPrintDialog.kt") }

    @Test
    fun `existing native editors are reused rather than rewritten`() {
        listOf(
            "TableEditorDialog", "PeriodicEditorDialog", "FigurePickerDialog",
            "FigureTypePickerDialog", "AtlasEditorDialog", "AtlasTypePickerDialog"
        ).forEach { dlg ->
            assertTrue("ویرایشگر $dlg باید از ui/figure وارد شود", "import ir.exam.app.ui.figure.$dlg" in host)
        }
        // هیچ ویرایشگر تازه‌ای در فایل پل تعریف نشده باشد
        assertFalse("@Composable\nfun TableEditor" in host)
    }

    @Test
    fun `all seven tools are routed`() {
        listOf("table", "periodic", "figure", "graph", "anatomy", "physics", "chemistry").forEach { tool ->
            assertTrue("ابزار $tool مسیر بومی ندارد", "\"$tool\"" in host)
        }
        assertTrue("figure\", \"graph\", \"table\", \"anatomy\", \"periodic\", \"physics\", \"chemistry\"" in host)
    }

    @Test
    fun `formula deliberately stays on the html path`() {
        // V82.0 — فرمول هم بومی شد؛ حالا هیچ ابزاری استثنا نیست.
        assertFalse("activeExactTool !== 'formula'" in assetText)
        assertTrue("openFormulaEditor3" in assetText)
        // پل بومی نباید فرمول را مسیریابی کند
        // V82.0 — «formula» عمداً به میزبان اضافه شد.
        assertTrue("FORMULA = \"formula\"" in host)
    }

    @Test
    fun `token contract matches the html renderer and the online builder`() {
        assertTrue("\"%%FIG:\" + spec.toJson() + \"%%\"" in host)
        // رندرکنندهٔ HTML دست‌نخورده است
        assertTrue("function renderFigToken" in assetText)
        // همان قراردادی که FigTokenVisuals در آزمون‌ساز آنلاین می‌فهمد
        val visuals = source("app/src/main/java/ir/exam/app/ui/builder/FigTokenVisuals.kt")
        assertTrue("%%FIG:" in visuals)
    }

    @Test
    fun `asset hook and return bridge exist with safe fallback`() {
        assertTrue("ExamPrintNative.openFigureTool" in assetText)
        assertTrue("window.__qmfInsertFigToken" in assetText)
        // اگر میزبان بومی نبود، دکمهٔ HTML قبلی کار می‌کند
        assertTrue("const btn = document.getElementById(buttonMap[activeExactTool] || buttonMap.formula);" in assetText)
    }

    @Test
    fun `kotlin bridge is wired end to end`() {
        assertTrue("fun openFigureTool(" in dialog)
        assertTrue("onOpenFigureTool" in dialog)
        assertTrue("ExamFigureToolHost(" in dialog)
        assertTrue("__qmfInsertFigToken" in dialog)
    }

    @Test
    fun `atlas and figure tools keep their two-stage flow`() {
        // انتخاب نوع ← ویرایش، عیناً مثل chooseType در آزمون‌ساز آنلاین
        assertTrue("AtlasTypePickerDialog(" in host)
        assertTrue("FigureTypePickerDialog(" in host)
        assertTrue("presetType = type" in host)
    }

    @Test
    fun `out of scope areas are untouched`() {
        listOf(
            "function printStudent", "function printTeacher", "function renderPreview",
            "function renderEditor", "extracted-math-host-script", "id=\"mathEditorFrame\""
        ).forEach { keep ->
            assertTrue("بخشِ خارج از دامنه تغییر کرده: $keep", keep in assetText)
        }
    }
}
