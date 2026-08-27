package ir.exam.app.ui.app

import ir.exam.app.core.figure.FigureCodec
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.printing.WordPageLayout
import ir.exam.app.ui.builder.MediaDraft
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V63.1 — پچ ۲ ویرایشگر سند Word-مانند: جابه‌جایی و تغییر اندازهٔ اشیا.
 * ۱) تصویر سؤال: کشیدن بدنه (حالت آزاد) جابه‌جا و دستگیرهٔ گوشه اندازه؛
 *    مقادیر میلی‌متری در MediaDraft ذخیره و مستقیم به چاپ می‌روند.
 * ۲) شکل/نمودار/جدول درون‌متنی: دستگیرهٔ اندازه؛ عرض در X.wmm خود توکن
 *    %%FIG%% ذخیره می‌شود (ماندگار با JSON سؤال) و چاپ رسمی همان را می‌خواند.
 * ۳) صفحه‌بندی ارتفاع واقعی اشیا را حساب می‌کند (شکل بزرگ‌تر = بلوک بلندتر).
 */
class V63_1DocObjectDragResizeTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val layout by lazy { source("app/src/main/java/ir/exam/app/core/printing/WordPageLayout.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }

    // ---- ۱) قراردادهای منبع ----

    @Test
    fun `question images gain drag and resize on the page`() {
        assertTrue("fun DraggableQuestionImage(" in editor)
        assertTrue("detectDragGestures" in editor)
        assertTrue("onMoveImage = builder::moveImage" in editor)
        assertTrue("onResizeImage = builder::resizeImage" in editor)
        // جابه‌جایی بدنه فقط در حالت آزاد؛ ریسایز همیشه
        assertTrue("freePlacement = question.imagePosition == \"free\"" in editor)
        // V63.8 — دستگیره/لکهٔ آبی حذف شد؛ اندازه با +/− نوار ابزار است.
        assertTrue("fun ResizeHandle(" !in editor)
        assertTrue("detectTapGestures(onTap = { onSelect() })" in editor)
        // رندر ثابت قدیمی تصویر حذف شد
        assertFalse("contentDescription = \"تصویر سؤال\",\n                modifier = Modifier\n                    .width(WordPageLayout.mmToDp(media.widthMm.coerceIn(5f, 182f), zoom).dp)" in editor)
    }

    @Test
    fun `inline figures gain a resize handle persisted inside the token`() {
        assertTrue("fun ResizableFigure(" in editor)
        assertTrue("InlineFigureView(spec = spec" in editor)
        assertTrue("WordPageLayout.withFigureWidthMm(occ.spec, widthMm)" in editor)
        // چاپ رسمی عرض ذخیره‌شده را می‌خواند (۹۵ ثابت قدیمی حذف شد)
        assertTrue("WordPageLayout.figureWidthMm(rich.spec)" in pdfAdapter)
        assertFalse("imageWidthMm=95f" in pdfAdapter)
    }

    // ---- ۲) تست‌های اجرایی JVM ----

    @Test
    fun `figure width lives inside the token and clamps to the printable range`() {
        val spec = FigureSpec.parse("{\"t\":\"tri\"}")!!
        assertEquals(WordPageLayout.DEFAULT_FIGURE_WIDTH_MM, WordPageLayout.figureWidthMm(spec), 0.001f)
        val wide = WordPageLayout.withFigureWidthMm(spec, 150f)
        assertEquals(150f, WordPageLayout.figureWidthMm(wide), 0.001f)
        // clamp دو طرف
        assertEquals(WordPageLayout.FIGURE_MIN_WIDTH_MM,
            WordPageLayout.figureWidthMm(WordPageLayout.withFigureWidthMm(spec, 1f)), 0.001f)
        assertEquals(WordPageLayout.FIGURE_MAX_WIDTH_MM,
            WordPageLayout.figureWidthMm(WordPageLayout.withFigureWidthMm(spec, 999f)), 0.001f)
        // عرض داخل خود توکن ذخیره می‌شود و پس از decode برمی‌گردد
        val token = "%%FIG:" + wide.toJson() + "%%"
        val parsed = FigureCodec.occurrences(token).single().spec
        assertEquals(150f, WordPageLayout.figureWidthMm(parsed), 0.001f)
        // بقیهٔ کلیدهای توکن دست‌نخورده می‌مانند
        assertEquals("tri", parsed.type)
    }

    @Test
    fun `bigger objects really grow the paginated block`() {
        val small = "%%FIG:{\"t\":\"tri\"}%%"
        val big = "%%FIG:" + WordPageLayout.withFigureWidthMm(FigureSpec.parse("{\"t\":\"tri\"}")!!, 180f).toJson() + "%%"
        val smallH = WordPageLayout.figureHeightMm(FigureCodec.occurrences(small).single().spec)
        val bigH = WordPageLayout.figureHeightMm(FigureCodec.occurrences(big).single().spec)
        assertEquals(WordPageLayout.FIGURE_BLOCK_HEIGHT_MM, smallH, 0.001f)
        assertTrue(bigH > smallH)
        // ارتفاع تصویر با عرض و نسبت پیش‌نمایش (۰٫۶) رشد می‌کند
        val narrow = WordPageLayout.mediaHeightMm(MediaDraft(uri = "u", widthMm = 40f))
        val wide = WordPageLayout.mediaHeightMm(MediaDraft(uri = "u", widthMm = 120f))
        assertEquals(40f * 0.6f + WordPageLayout.MEDIA_GAP_MM, narrow, 0.001f)
        assertTrue(wide > narrow)
    }

    @Test
    fun `free image x stays inside the printable area`() {
        assertEquals(0f, WordPageLayout.clampImageXmm(-10f, 60f), 0.001f)
        val maxX = WordPageLayout.USABLE_WIDTH_MM - 60f
        assertEquals(maxX, WordPageLayout.clampImageXmm(999f, 60f), 0.001f)
        // تصویر پهن‌تر از صفحه به صفر می‌چسبد
        assertEquals(0f, WordPageLayout.clampImageXmm(50f, 400f), 0.001f)
    }
}
