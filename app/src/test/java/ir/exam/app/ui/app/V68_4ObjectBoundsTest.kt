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
 * ۱) شکل درون‌متنی با کشیدن بدنه مثل تصاویر جابه‌جا می‌شود؛ fx و fy هر دو
 *    «مطلق از بالا-چپ بلوک» در X.fx/X.fy همان توکن %%FIG%% ذخیره می‌شوند
 *    (یک رقم اعشار) و بدون مهاجرت داده، توکن قدیمی = همان رندر درون‌متنی.
 *    V68.4.1: مطلق بودن fy یعنی شکل بین حالت ویرایش/نمایش/چاپ نمی‌پرد.
 * ۲) هر شیء آزاد (تصویر و شکل) فقط داخل ارتفاع بلوکِ سؤال خودش حرکت
 *    می‌کند — تصویر سؤال ۱ وارد سؤال ۲ نمی‌شود؛ اسلات درون‌متنی/انتهایی
 *    رزرو می‌شود تا ارتفاع بلوک ثابت بماند و آفست فقط بصری باشد.
 *    V68.4.1: clamp با «ارتفاع واقعی» شیء است (نه فرض ۰٫۶/figureHeightMm).
 * ۳) چاپ رسمی شکلِ دارای fx/fy را مثل تصویر آزاد در همان جایگاه (نسبت به
 *    ابتدای بلوک) می‌کشد؛ فضای شیء در ویرایشگر LTR است تا در برنامهٔ RTL
 *    درگ و بزرگ‌کردن آینه‌ای نشود (هر دو offset و align در RTL آینه می‌کنند).
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
        // V68.4.1 — فضای شیء LTR: برنامه RTL است و offset/align در RTL افقی را
        // آینه می‌کنند (درگ/بزرگ‌کردن برعکس می‌شد) — مثل NativeFormulaView.
        assertTrue("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)" in editor)
        // clamp عمودی با ارتفاع بلوکِ همان سؤال و «ارتفاع واقعی» شیء (نه فرضی)
        assertTrue("blockHeightMm = it.size.height / pxPerMm" in editor)
        assertTrue("(boundsHeightMm - objHeightMm).coerceAtLeast(0f)\n    else Float.MAX_VALUE" in editor)
        assertTrue("val objHeightMm = if (realHeightMm > 0f) realHeightMm else heightMm" in editor)
        assertTrue("realHeightMm = it.size.height / pxPerMm" in editor)
        // موقعیت آزاد از توکن خوانده می‌شود؛ fx و fy هر دو مطلق از بالا-چپ بلوک‌اند
        // تا شکل بین حالت ویرایش/نمایش/چاپ نپرد.
        assertTrue("WordPageLayout.figurePosMm(spec)" in editor)
        assertTrue("baseTopMm = pos?.second ?: anchorPosMm.second" in editor)
        assertTrue("onMove(x, topAbs)" in editor)
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
        // V68.4.1 — fy مطلق از بالای بلوک: در چاپ ارتفاع بلوک‌های قبلی همین
        // سؤال کم می‌شود تا شکل نسبت به ابتدای بلوک (مثل ویرایشگر) بیفتد.
        assertTrue("val flowPt = (qStart until size).fold(0f) { acc, i -> acc + measureBlock(this[i]) }" in pdfAdapter)
        // V68.4.2 — سیمبل‌های جدید باید import صریح داشته باشند (درس CI ران ۳۷۴)
        assertTrue("import androidx.compose.runtime.CompositionLocalProvider" in editor)
        assertTrue("imageYmm=(figPos?.second ?: 30f) - flowPt * (297f / 80f)" in pdfAdapter)
        // سقف ۸۰pt چاپ حفظ شده (کف نیست: شکل می‌تواند بالای جریان خودش باشد)
        assertTrue("(block.imageYmm/297f*80f).coerceAtMost(80f)" in pdfAdapter)
        // توابع جدید در layout
        assertTrue("fun withFigurePosMm(" in layout)
        assertTrue("fun figurePosMm(" in layout)
        assertTrue("const val FIGURE_POS_X_KEY: String = \"fx\"" in layout)
        assertTrue("const val FIGURE_POS_Y_KEY: String = \"fy\"" in layout)
    }
}
