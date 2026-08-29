package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.printing.WordPageLayout
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V68.4 — حرکت آزاد شکل/نمودار/جدول + محدودیت حرکت همهٔ اشیا به محدودهٔ
 * خودِ همان سؤال (درخواست کاربر):
 * ۱) شکل درون‌متنی با کشیدن بدنه مثل تصاویر جابه‌جا می‌شود؛ fx مطلق از چپ
 *    بلوک و fy آفست از جای طبیعی (همان قرارداد yMm تصویر، سازگار با چاپ)
 *    در X.fx/X.fy همان توکن %%FIG%% ذخیره می‌شوند (یک رقم اعشار) و بدون
 *    مهاجرت داده، توکن قدیمی = همان رندر درون‌متنی.
 * ۲) هر شیء آزاد (تصویر و شکل) فقط داخل ارتفاع بلوکِ سؤال خودش حرکت
 *    می‌کند — تصویر سؤال ۱ وارد سؤال ۲ نمی‌شود؛ اسلات درون‌متنی/انتهایی
 *    رزرو می‌شود تا ارتفاع بلوک ثابت بماند و آفست فقط بصری باشد.
 * ۳) چاپ رسمی شکلِ دارای fx/fy را مثل تصویر آزاد در همان جایگاه می‌کشد.
 */
class V68_4ObjectBoundsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val layout by lazy { source("app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }

    // ---- ۱) ذخیره/بازیابی موقعیت آزاد شکل در خود توکن ----

    @Test
    fun `figure free position roundtrips through the token with one decimal`() {
        val spec = FigureSpec.parse("{\"t\":\"tri\"}")!!
        // توکن قدیمی بدون موقعیت = درون‌متنی
        assertNull(WordPageLayout.figurePosMm(spec))
        // ذخیرهٔ موقعیت مطلق نسبت به بالا-چپ بلوک سؤال
        val moved = WordPageLayout.withFigurePosMm(spec, 63.25f, 41.8f)
        val pos = WordPageLayout.figurePosMm(moved)!!
        assertEquals(63.3f, pos.first, 0.001f)
        assertEquals(41.8f, pos.second, 0.001f)
        // مقادیر داخل خود توکن ماندگارند و پس از encode/decode برمی‌گردند
        val token = "%%FIG:" + moved.toJson() + "%%"
        val parsed = FigureCodec.occurrences(token).single().spec
        assertEquals(63.3f, WordPageLayout.figurePosMm(parsed)!!.first, 0.001f)
        assertEquals(41.8f, WordPageLayout.figurePosMm(parsed)!!.second, 0.001f)
        // بقیهٔ مشخصات توکن دست‌نخورده می‌مانند
        assertEquals("tri", parsed.type)
        assertEquals(WordPageLayout.DEFAULT_FIGURE_WIDTH_MM, WordPageLayout.figureWidthMm(parsed), 0.001f)
    }

    @Test
    fun `fx and fy must both be present for free placement`() {
        // فقط یکی از دو کلید = مثل دادهٔ قدیمی درون‌متنی رندر می‌شود
        val onlyX = FigureSpec.parse("""{"t":"cir","X":{"fx":"40.0"}}""")!!
        assertNull(WordPageLayout.figurePosMm(onlyX))
        val onlyY = FigureSpec.parse("""{"t":"cir","X":{"fy":"12.0"}}""")!!
        assertNull(WordPageLayout.figurePosMm(onlyY))
        // مقدار غیرعددی هم درون‌متنی تلقی می‌شود
        val junk = FigureSpec.parse("""{"t":"cir","X":{"fx":"abc","fy":"5"}}""")!!
        assertNull(WordPageLayout.figurePosMm(junk))
    }

    @Test
    fun `position and width keys coexist inside the same token`() {
        val spec = WordPageLayout.withFigureWidthMm(FigureSpec.parse("{\"t\":\"tab\"}")!!, 130f)
        val moved = WordPageLayout.withFigurePosMm(spec, 12.5f, 88f)
        // wmm قبلی حفظ می‌شود و fx/fy کنارش می‌نشیند
        assertEquals(130f, WordPageLayout.figureWidthMm(moved), 0.001f)
        assertEquals(12.5f, WordPageLayout.figurePosMm(moved)!!.first, 0.001f)
        assertEquals(88f, WordPageLayout.figurePosMm(moved)!!.second, 0.001f)
        assertTrue("\"fx\"" in moved.toJson())
        assertTrue("\"fy\"" in moved.toJson())
        assertTrue("\"wmm\"" in moved.toJson())
    }

    // ---- ۲) clamp بلوکی: شیء فقط داخل بلوک خودِ سؤال ----

    @Test
    fun `object motion clamps to the bounds of its own question block`() {
        // همان فرمول clamp عمودی ویرایشگر: top ∈ [0, blockHeight − objectHeight]
        val blockHeightMm = 120f
        val objectHeightMm = 42f
        val maxTopMm = (blockHeightMm - objectHeightMm).coerceAtLeast(0f)
        assertEquals(78f, maxTopMm, 0.001f)
        // رها کردن پایین‌تر از مرز سؤال = چسبیده به انتهای همان بلوک
        assertEquals(78f, 999f.coerceIn(0f, maxTopMm), 0.001f)
        // بالاتر از ابتدای بلوک هم نمی‌رود
        assertEquals(0f, (-5f).coerceIn(0f, maxTopMm), 0.001f)
        // شیء بلندتر از بلوک: فقط ابتدای بلوک
        assertEquals(0f, (120f - 200f).coerceAtLeast(0f), 0.001f)
        // افقی: همان clamp ناحیهٔ چاپ قبلی
        assertEquals(0f, WordPageLayout.clampImageXmm(-10f, 60f), 0.001f)
    }

    // ---- ۳) قراردادهای منبع (ویرایشگر، چاپ، ViewModel) ----

    @Test
    fun `editor drags figures and clamps every object to its question block`() {
        // درگ بدنهٔ شکل مثل تصاویر + قفل اشیا
        assertTrue("if (selected && !locked) Modifier.pointerInput(spec.raw, anchorPosMm, boundsHeightMm, shownWidthMm)" in editor)
        // clamp عمودی با ارتفاع بلوکِ همان سؤال (شکل و تصویر هر دو)
        assertTrue("blockHeightMm = it.size.height / pxPerMm" in editor)
        assertTrue("(boundsHeightMm - heightMm).coerceAtLeast(0f)" in editor)
        assertTrue("(boundsHeightMm - heightMm).coerceAtLeast(0f)\n    else Float.MAX_VALUE" in editor)
        // موقعیت آزاد از توکن خوانده می‌شود
        assertTrue("WordPageLayout.figurePosMm(spec)" in editor)
        // fy = آفست از جای طبیعی (مثل yMm تصویر): رندر = لنگر + آفست، commit = مطلق − لنگر
        assertTrue("baseTopMm = anchorPosMm.second + (pos?.second ?: 0f)" in editor)
        assertTrue("onMove(x, topAbs - anchorPosMm.second)" in editor)
        // اسلات طبیعی اندازه‌گیری می‌شود تا آفست بصری از آن محاسبه شود
        assertTrue("figureAnchors[occIndex]" in editor)
        assertTrue("imageSlotTops[media.id]" in editor)
        // آفست ذخیره‌شدهٔ تصویر می‌تواند منفی باشد (بالاتر از اسلات، داخل همان بلوک)
        assertTrue("yMm = yMm.coerceIn(-300f, 300f)" in builder)
    }

    @Test
    fun `figure move threads through the document to the token`() {
        // wiring از سطح بالا تا بلوک سؤال
        assertTrue("onMoveFigure: (String, Int, Float, Float) -> Unit" in editor)
        assertTrue("onMoveFigure = { occ, x, y -> onMoveFigure(question.id, occ, x, y) }" in editor)
        // commit نهایی در X.fx/X.fy همان توکن
        assertTrue("WordPageLayout.withFigurePosMm(occ.spec, xMm, yMm)" in editor)
        assertTrue("onMove = { xMm, yMm -> onMoveFigure(occIndex, xMm, yMm) }" in editor)
        assertTrue("onMove = { xMm, yMm -> onMoveFigure(occurrenceIndex, xMm, yMm) }" in editor)
    }

    @Test
    fun `official print renders positioned figures through the free-image path`() {
        assertTrue("val figPos = WordPageLayout.figurePosMm(rich.spec)" in pdfAdapter)
        assertTrue("imagePosition=if (figPos != null) \"free\" else \"below\"" in pdfAdapter)
        // y چاپ دیگر منفی نمی‌شود (آفست منفی = بالای بلوک همان سؤال)
        assertTrue("(block.imageYmm/297f*80f).coerceIn(0f,80f)" in pdfAdapter)
        // توابع جدید در layout
        assertTrue("fun withFigurePosMm(" in layout)
        assertTrue("fun figurePosMm(" in layout)
        assertTrue("const val FIGURE_POS_X_KEY: String = \"fx\"" in layout)
        assertTrue("const val FIGURE_POS_Y_KEY: String = \"fy\"" in layout)
    }
}
