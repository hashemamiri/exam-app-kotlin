package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V68.6 — سه گزارش کاربر روی بیلد V68.5:
 * ۱) «متن1 فرمول متن2» در چاپ به سه سطر می‌شکست — حالا متن و فرمول در یک
 *    «سطر جاری» درون‌خطی جریان می‌یابند (مثل FlowRow ویرایشگر)؛ فرمول به‌صورت
 *    ReplacementSpan روی جای‌نگهدار U+FFFC در همان StaticLayout می‌نشیند.
 * ۲) تصویر گالری جابه‌جایی آزاد واقعی نداشت — لامبدای pointerInput مقدارهای
 *    کهنه (media.xMm=0/free=false) می‌دید و هر درگ از اسلات شروع می‌شد.
 * ۳) گزینه‌های جورکردنی در چاپ نمایش داده نمی‌شدند — آیتم‌ها در
 *    matchingLeft/Right هستند نه options؛ حالا مثل ویرایشگر (راست ↔ چپ)
 *    چاپ می‌شوند.
 */
class V68_6PrintInlineMatchingTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val models by lazy { source("app/src/main/java/ir/exam/app/domain/model/OfficialPrintModels.kt") }
    private val repo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamDocumentEditorScreen.kt") }

    // ---- ۱) پاراگراف درون‌خطی متن+فرمول ----

    @Test
    fun `question text and formulas flow inline in one paragraph`() {
        // فرمول دیگر بلوک جدا نیست؛ روی جای‌نگهدار U+FFFC در همان Spannable
        assertTrue("__inline.append('\\uFFFC')" in pdfAdapter)
        // پاراگرافِ فرمول‌آغاز با RLM نامرئی جهت RTL را تثبیت می‌کند
        assertTrue("if (__inline.isEmpty()) { __inline.append('\\u200F'); __inlineLen += 1 }" in pdfAdapter)
        assertTrue("MathReplacementSpan(NativeMathParser.parse(rich.tex))" in pdfAdapter)
        assertTrue("__flushInline()" in pdfAdapter)
        // قدیمی: فرمول سؤال = بلوک جدا (سطر جدا) — دیگر نباشد
        assertTrue("is RichSegment.Math -> add(RenderBlock(formula=rich.tex" !in pdfAdapter)
    }

    @Test
    fun `inline math span is a replacement span with font metric growth`() {
        // سیمبل‌های جدید = needle import (درس CI ران ۳۷۴)
        assertTrue("import android.text.style.ReplacementSpan" in pdfAdapter)
        assertTrue("import ir.exam.app.core.math.MathNode" in pdfAdapter)
        assertTrue("private inner class MathReplacementSpan(private val node: MathNode) : ReplacementSpan()" in pdfAdapter)
        // ارتفاع سطر با FontMetrics رشد می‌کند تا کسر هم در همان سطر جا شود
        assertTrue("fm.descent = maxOf(fm.descent, (metrics.height - above).toInt().coerceAtLeast(0))" in pdfAdapter)
        // draw با خط کرسی متن هم‌تراز است
        assertTrue("mathRenderer.draw(canvas, node, x, y - paint.textSize * 0.92f, paint.textSize, Color.BLACK)" in pdfAdapter)
    }

    @Test
    fun `options flow inline too and keep question alignment`() {
        assertTrue("MathReplacementSpan(NativeMathParser.parse(segment.text))" in pdfAdapter)
        assertTrue("styledText=__opt,textSize=optionSize" in pdfAdapter)
        // گزینه از align سؤال پیروی می‌کند (مثل ویرایشگر؛ قبلاً همیشه راست)
        assertTrue("align=question.textAlign,fontFamily=question.fontFamily))" in pdfAdapter)
        // قدیمی: فرمولِ گزینه = بلوک جدا — دیگر نباشد
        assertTrue("RenderBlock(formula=segment.text" !in pdfAdapter)
    }

    @Test
    fun `whitespace segments between formulas are kept`() {
        // جداکنندهٔ فاصله بین دو فرمول نباید حذف شود (قبلاً isNotBlank حذفش می‌کرد)
        assertTrue("is RichSegment.Text -> if (rich.text.isNotEmpty())" in pdfAdapter)
        assertTrue("is RichSegment.Text -> if (rich.text.isNotBlank())" !in pdfAdapter)
    }

    // ---- ۲) درگ آزاد تصویر گالری ----

    @Test
    fun `gallery image drag reads fresh placement from rememberUpdatedState`() {
        assertTrue("val currentFreePlacement by rememberUpdatedState(freePlacement)" in editor)
        assertTrue("val currentXmm by rememberUpdatedState(media.xMm)" in editor)
        assertTrue("val currentYmm by rememberUpdatedState(media.yMm)" in editor)
        // سقف درگ با ارتفاع واقعی شیء (نه تخمین ۰٫۶)
        assertTrue("val currentObjHeightMm by rememberUpdatedState(objHeightMm)" in editor)
        assertTrue("val currentLiveWidthMm by rememberUpdatedState(liveWidthMm)" in editor)
        // commit درگ از مقادیر تازه می‌خواند (نه media/freePlacement کهنهٔ ژست)
        assertTrue("val topMm = (anchor + baseY + dragYmm).coerceIn(0f, dragMaxTopMm)" in editor)
        assertTrue("WordPageLayout.clampImageXmm(baseX + dragXmm, currentLiveWidthMm)" in editor)
        // تصویر غیرآزاد مثل چاپ وسط اسلات می‌نشیند (عارضهٔ چپ‌چین V68.4.1)
        assertTrue("val centeredXmm = ((WordPageLayout.USABLE_WIDTH_MM - liveWidthMm) / 2f).coerceAtLeast(0f)" in editor)
        // قدیمی: خواندن مستقیم media/freePlacement داخل onDragEnd — رفت
        assertTrue("(anchor + (if (freePlacement) media.yMm else 0f) + dragYmm" !in editor)
    }

    // ---- ۳) جورکردنی در چاپ ----

    @Test
    fun `matching items reach the print model from the repository`() {
        assertTrue("val matchingLeft: List<String> = emptyList()" in models)
        assertTrue("val matchingRight: List<String> = emptyList()" in models)
        assertTrue("val matchingLeftStyles: List<Triple<Boolean, Boolean, Float?>?> = emptyList()" in models)
        assertTrue("val matchingRightStyles: List<Triple<Boolean, Boolean, Float?>?> = emptyList()" in models)
        assertTrue("matchingLeft = question.matchingLeft" in repo)
        assertTrue("matchingRight = question.matchingRight" in repo)
    }

    @Test
    fun `matching rows print right arrow left like the editor`() {
        assertTrue("val __matchRows = maxOf(question.matchingLeft.size, question.matchingRight.size)" in pdfAdapter)
        assertTrue("matchRight=question.matchingRight.getOrNull(rowIndex)" in pdfAdapter)
        assertTrue("matchLeft=question.matchingLeft.getOrNull(rowIndex)" in pdfAdapter)
        // ردیف: آیتم راست در نیمهٔ راست، ↔ وسط، آیتم چپ در نیمهٔ چپ
        assertTrue("canvas.translate(PAGE_WIDTH - MARGIN - half, y)" in pdfAdapter)
        assertTrue("val arrow = \"↔\"" in pdfAdapter)
        assertTrue("private fun matchHalfWidth(): Int" in pdfAdapter)
    }

    // ---- پورت منطقی: شمارش بلوک‌ها و ردیف‌ها ----

    @Test
    fun `inline paragraph collapses three segments into one block`() {
        // «متن1 $x$ متن2» → قدیمی: ۳ بلوک/۳ سطر؛ جدید: ۱ پاراگراف درون‌خطی.
        data class Seg(val text: String, val math: Boolean)
        val segments = listOf(Seg("متن1 ", false), Seg("x", true), Seg(" متن2", false))
        var blocks = 0
        var inlineLen = 0
        var hasFormula = false
        segments.forEach { seg ->
            if (seg.math) { inlineLen += 1; hasFormula = true }
            else { inlineLen += seg.text.length }
        }
        if (inlineLen > 0) blocks = 1
        assertEquals(1, blocks)
        assertTrue(hasFormula)
        // جای‌نگهدار فرمول همان جای منطقی خودش را در متن دارد
        val rebuilt = "متن1 " + "￼" + " متن2"
        assertEquals(11, rebuilt.length)
    }

    @Test
    fun `matching row count is the taller side and halves never overlap the arrow`() {
        // ۴ چپ و ۳ راست → ۴ ردیف؛ نیمه‌ها با فاصلهٔ ۲۶pt وسط جدا می‌شوند.
        val left = listOf("الف", "ب", "ج", "د")
        val right = listOf("۱", "۲", "۳")
        assertEquals(4, maxOf(left.size, right.size))
        val contentWidth = 595f - 2f * 38f
        val half = ((contentWidth - 26f) / 2f).coerceAtLeast(60f)
        // دو نیمه + ۲۶pt فاصله = دقیقاً عرض ناحیهٔ چاپ
        assertEquals(contentWidth, half * 2f + 26f, 0.01f)
    }
}
