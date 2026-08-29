package ir.exam.app.ui.app

import ir.exam.app.core.printing.UnifiedDocumentEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * V68.9 — موتور واحد سند (درخواست کاربر: «یک موتور قدرتمند بساز که چاپ و
 * ویرایشگر شبیه شوند»).
 *
 * تصمیم‌های ثبت‌شدهٔ کاربر در این نسخه:
 *  ۱) همهٔ عناصر فقط-چاپ (سطر درس/مدت/بارم، خطوط پاسخ، کادرها) در ویرایشگر
 *     هم — کم‌رنگ — دیده شوند.
 *  ۲) ویرایشگر صفحهٔ اول را بدون رزرو سربرگ از بالا شروع کند (صفحهٔ ۱ چاپ
 *     به‌خاطر سربرگ رسمی کوتاه‌تر است؛ این تفاوت با علم کاربر پذیرفته شد).
 *  ۳) یک پچ کامل.
 *
 * معماری: کلاس UnifiedDocumentEngine (داخل OfficialPdfPrintAdapter.kt) مالک
 * «یک» چیدمان/رسم است؛ آداپتور چاپ و کاغذهای ویرایشگر هر دو از آن می‌خوانند.
 *
 * بخش computeSlices تستِ واقعیِ JVM است (بدون اندروید) چون تابع خالص است؛
 * بقیه، تست‌های ساختاری (needle) مثل بقیهٔ پروژه‌اند.
 */
class V68_9UnifiedEngineWysiwygTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val adapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }
    private val mathText by lazy { source("app/src/main/java/ir/exam/app/ui/math/NativeMathText.kt") }

    // ------------------------------------------------- تست واقعی برش صفحات

    /** برش صفحه فقط روی مرز خط/بلوک می‌افتد — هرگز وسط ظرفیت خام. */
    @Test
    fun `slices end on line boundaries never mid capacity`() {
        // مرزهای سطر: 20..100 پله‌ای؛ ظرفیت صفحهٔ ۱ = 670 → مرز 660 انتخاب می‌شود نه 670.
        val boundaries = (1..50).map { it * 20f } // 20,40,...,1000
        val slices = UnifiedDocumentEngine.computeSlices(1000f, boundaries, UnifiedDocumentEngine.CONTENT_TOP)
        assertTrue(slices.isNotEmpty())
        assertTrue("first slice must end on a boundary, not raw capacity",
            abs(slices.first().second - 660f) < 0.01f)
        // هیچ برشی نباید در وسط ظرفیت خام بیفتد مگر اینکه هیچ مرزی جا نشده باشد
        slices.forEach { (top, end) ->
            val onBoundary = boundaries.any { abs(it - end) < 0.01f }
            val atTotal = abs(end - 1000f) < 0.01f
            assertTrue("slice end $end must be a line/block boundary or document end", onBoundary || atTotal)
            assertTrue("slice must advance", end > top)
        }
        // پیوستگی: انتهای هر برش = شروع برش بعد
        slices.zipWithNext { a, b -> assertTrue(abs(a.second - b.first) < 0.01f) }
        assertTrue(abs(slices.last().second - 1000f) < 0.01f)
    }

    /** صفحهٔ ۱ چاپ کوتاه‌تر است (سربرگ)؛ ویرایشگر از حاشیه شروع می‌کند (تصمیم کاربر). */
    @Test
    fun `first page capacity differs print vs editor and later pages match`() {
        val boundaries = (1..500).map { it * 10f }
        val print = UnifiedDocumentEngine.computeSlices(5000f, boundaries, UnifiedDocumentEngine.CONTENT_TOP)
        val edit = UnifiedDocumentEngine.computeSlices(5000f, boundaries, UnifiedDocumentEngine.EDITOR_FIRST_TOP)
        // چاپ: ظرفیت ۱ = 670 → مرز 670 (خودش مرز است)؛ ویرایشگر: ظرفیت ۱ = 757 → مرز 750
        assertTrue(abs(print.first().second - 670f) < 0.01f)
        assertTrue(abs(edit.first().second - 750f) < 0.01f)
        // از صفحهٔ ۲ به بعد ظرفیت چاپ 745 است؛ مرز بعدی: 670→1410
        assertTrue(abs(print[1].second - 1410f) < 0.01f)
    }

    /** بلوک/خط بلندتر از یک صفحه: برش سخت به‌عنوانfallback، بدون حلقهٔ بی‌نهایت. */
    @Test
    fun `taller than page content hard cuts without infinite loop`() {
        val slices = UnifiedDocumentEngine.computeSlices(2000f, listOf(0f, 1900f), UnifiedDocumentEngine.CONTENT_TOP)
        assertTrue(slices.isNotEmpty())
        assertTrue(abs(slices.last().second - 2000f) < 0.01f)
        slices.zipWithNext { a, b -> assertTrue(abs(a.second - b.first) < 0.01f) }
    }

    /** سند خالی/کوتاه: دقیقاً یک برش. */
    @Test
    fun `empty document yields a single slice`() {
        val slices = UnifiedDocumentEngine.computeSlices(0.5f, listOf(0f), UnifiedDocumentEngine.CONTENT_TOP)
        assertTrue(slices.size == 1)
        assertTrue(abs(slices.first().first) < 0.01f)
    }

    // ------------------------------------------------ تست‌های ساختاری موتور

    @Test
    fun `one engine class owns layout and both surfaces use it`() {
        assertTrue("class UnifiedDocumentEngine(" in adapter)
        // چاپ از همان موتور می‌کشد
        assertTrue("engine.drawFlowWindow(canvas, document, slice)" in adapter)
        assertTrue("private val engine = UnifiedDocumentEngine(context)" in adapter)
        // ویرایشگر کاغذها را با همان موتور می‌کشد
        assertTrue("UnifiedDocumentEngine(context.applicationContext)" in editor)
        assertTrue("engine.layoutExamForEditor(printable, imageBits.toMap())" in editor)
        assertTrue("engine.drawEditorPage(native, document, pageIndex, skipQuestion)" in editor)
        assertTrue("engine.hitTest(document, pageIndex, xPt, yPt)" in editor)
    }

    @Test
    fun `editor renders paper through engine and overlays only the editing question`() {
        assertTrue("private fun EnginePageView(" in editor)
        assertTrue("skipQuestion = editingIndex" in editor)
        assertTrue("document.questionOriginPt(overlayIndex) * pxPerPt" in editor)
        // سؤال در حال ویرایش همان Compose تعاملی قبلی است (تایپ/درگ/انتخاب درجا)
        assertTrue("editable = editingQuestionId == question.id" in editor)
        assertTrue("onMoveFigure = { occ, x, y -> onMoveFigure(question.id, occ, x, y) }" in editor)
    }

    @Test
    fun `options match the editor size and bold number like the editor`() {
        assertTrue("const val OPTION_SCALE = 1f" in adapter)
        assertTrue("(optionStyle?.third ?: question.fontSizeSp) * OPTION_SCALE" in adapter)
        // مسیر ۰٫۹ قدیمی حذف شده است
        assertFalse("(optionStyle?.third ?: question.fontSizeSp) * .9f" in adapter)
        // شمارهٔ گزینه بولد می‌شود (مثل Row ویرایشگر)
        assertTrue("var optionPrefixLeft = \"${'$'}{index + 1}) \".length" in adapter)
        assertTrue("optionPrefixLeft -= boldTake" in adapter)
    }

    @Test
    fun `typography unified - no extra line spacing and editor font scale pinned`() {
        assertTrue("const val LINE_SPACING_ADD_PT = 0f" in adapter)
        assertTrue(".setLineSpacing(LINE_SPACING_ADD_PT, 1f)" in adapter)
        // ویرایشگر: فونت سیستمی موتور را به‌هم نمی‌زند و فونت سؤال از همان
        // خانوادهٔ چاپ می‌آید (قبلاً fontFamily سؤال فقط در چاپ اعمال می‌شد).
        assertTrue("Density(screenDensity.density, fontScale = 1f)" in editor)
        assertTrue("draftFontFamily(question.fontFamily)" in editor)
        assertTrue("lineHeight = TextUnit.Unspecified" in mathText)
    }

    @Test
    fun `question gap and print-only elements come from one source`() {
        assertTrue("const val QUESTION_GAP_PT = 6f * MM_TO_PT" in adapter)
        // سطر درس/مدت/بارم و خطوط پاسخ و کادرها در ویرایشگر هم رسم می‌شوند (کم‌رنگ)
        assertTrue("kind=\"subject\"" in adapter)
        assertTrue("kind=\"answer\"" in adapter)
        assertTrue("if (preview) Color.argb(0x55, 0x60, 0x60, 0x60) else Color.rgb(120, 120, 120)" in adapter)
        assertTrue("preview = true" in adapter)
        // برش مرزدار: مرزها از انتهای واقعی سطرهای StaticLayout می‌آیند
        assertTrue("boundaries.add(p.y + layout.getLineBottom(line))" in adapter)
        assertTrue("fun computeSlices(total: Float, boundaries: List<Float>, firstTopPt: Float)" in adapter)
    }

    @Test
    fun `gallery images decode once for the engine not per keystroke`() {
        assertTrue("val imageSignature = remember(questions) {" in editor)
        assertTrue("remember(imageSignature) { mutableStateMapOf<String, Bitmap>() }" in editor)
        assertTrue("decodeGalleryImage(context, media.uri)" in editor)
        assertTrue("attachImages(printable, imagesById)" in adapter)
    }
}
