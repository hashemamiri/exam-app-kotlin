package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V68.5 — پارتی چاپ/ویرایشگر + راست‌به‌چپ‌سازی جدول‌ها + فرمول ترازدار:
 * ۱) مقیاس چیدمان آزاد در چاپ رسمی واقعی شد (mm→pt = 595/210 ≈ ۲٫۸۳) —
 *    پیش‌تر y با /297*80 تقریباً ۱۰ برابر فشرده می‌شد و چاپ با ویرایشگر
 *    «به هم می‌ریخت» (گزارش کاربر).
 * ۲) فرمول در چاپ مثل متن سؤال تراز می‌شود (پیش‌فرض راست‌چین)؛ قبلاً همیشه
 *    از حاشیهٔ چپ کشیده می‌شد و زیر متنِ راست‌چین چپ‌چین دیده می‌شد.
 * ۳) جدول فارسی راست‌به‌چپ شد (ستون اول در راست)؛ جدول تناوبی هم به درخواست
 *    کاربر آینه شد (گروه ۱ در راست — معکوس V55.13).
 * ۴) پیام بازیابی پیش‌نویس فقط هنگام «ایجاد» آزمون می‌آید؛ ویرایش فقط خودِ
 *    آزمون را باز می‌کند.
 */
class V68_5PrintParityRtlTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val tableSvg by lazy { source("app/src/main/java/ir/exam/app/core/figure/TableSvgRenderer.kt") }
    private val periodicSvg by lazy { source("app/src/main/java/ir/exam/app/core/figure/PeriodicSvgRenderer.kt") }
    private val periodicDialog by lazy { source("app/src/main/java/ir/exam/app/ui/figure/PeriodicEditorDialog.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }

    // ---- ۱) مقیاس واقعی mm→pt برای چیدمان آزاد ----

    @Test
    fun `free placement uses the real mm to pt scale in official print`() {
        assertTrue("const val MM_TO_PT = PAGE_WIDTH / 210f" in pdfAdapter)
        // x آزاد: از حاشیهٔ چپ با مقیاس واقعی (قبلاً /210*CONTENT_WIDTH فشرده بود)
        assertTrue("MARGIN+(block.imageXmm*MM_TO_PT).coerceIn(0f,CONTENT_WIDTH-width)" in pdfAdapter)
        // y آزاد: مقیاس واقعی + سقفِ پایین ناحیهٔ چاپ (قبلاً /297*80 ≈ ۱۰ برابر فشرده)
        assertTrue("(top+block.imageYmm*MM_TO_PT).coerceAtMost(PAGE_HEIGHT-MARGIN-height)" in pdfAdapter)
        // شکل آزاد: تبدیل جریان با همان مقیاس برمی‌گرداند تا نتیجه blockTop+fy شود
        assertTrue("imageYmm=(figPos?.second ?: 30f) - flowPt * (210f / PAGE_WIDTH)" in pdfAdapter)
    }

    @Test
    fun `mm to pt scale matches the a4 page geometry`() {
        // A4: ۵۹۵pt ÷ ۲۱۰mm — پیش‌تر عمودی ۸۰/۲۹۷ ≈ ۰٫۲۷ (۱۰ برابر فشرده) بود.
        val mmToPt = 595f / 210f
        assertEquals(2.8333f, mmToPt, 0.001f)
        // آفست ۲۰ میلی‌متری در چاپ ≈ ۵۶٫۷pt (نه ۵٫۴pt قبلی) — مثل ویرایشگر.
        assertEquals(56.67f, 20f * mmToPt, 0.05f)
    }

    // ---- ۲) فرمول با تراز متن سؤال ----

    @Test
    fun `formulas align with the question text in official print`() {
        assertTrue("val formulaX = when (block.align)" in pdfAdapter)
        assertTrue("\"center\" -> MARGIN + (CONTENT_WIDTH - formulaWidth) / 2f" in pdfAdapter)
        assertTrue("\"left\" -> MARGIN" in pdfAdapter)
        assertTrue("else -> PAGE_WIDTH - MARGIN - formulaWidth" in pdfAdapter)
        // قدیمی: همیشه MARGIN (چپ) — دیگر نباشد
        assertTrue("mathRenderer.draw(canvas,NativeMathParser.parse(formula),MARGIN,y,block.textSize" !in pdfAdapter)
    }

    // ---- ۳) جدول و جدول تناوبی راست‌به‌چپ ----

    @Test
    fun `persian tables lay out right to left with first column on the right`() {
        // ستون اولِ داده در راست (آینهٔ افقی)؛ سرستون روی ایندکس منطقی می‌ماند
        assertTrue("val cx = x0 + (cols - 1 - c) * cellW" in tableSvg)
        assertTrue("table-svg-rtl2-" in tableSvg)
        // قدیمی: cx = x0 + c * cellW (چپ‌به‌راست) — دیگر در چیدمان نباشد
        val renderBody = tableSvg.substringAfter("fun render(spec: FigureSpec)")
        assertTrue("val cx = x0 + c * cellW" !in renderBody)
    }

    @Test
    fun `periodic table mirrors with group one on the right`() {
        // SVG: گروه ۱ در راست + لیبل دوره در چپ گرید + کش نسخهٔ جدید
        assertTrue("val x = PAD + (groups.size - 1 - ci) * step" in periodicSvg)
        assertTrue("PAD + groups.size * step + LABEL / 2f" in periodicSvg)
        assertTrue("periodic-svg-rtl2-" in periodicSvg)
        // دیالوگ: ترتیب دستی معکوس (provider LTR طبق V55.13 حفظ شده)
        assertTrue("groups.reversed().forEach { g ->" in periodicDialog)
        assertTrue("(3..17).reversed().forEach { g ->" in periodicDialog)
        assertTrue("CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)" in periodicDialog)
    }

    // ---- ۴) پیش‌نویس فقط هنگام ایجاد ----

    @Test
    fun `draft recovery is offered only when creating a new exam`() {
        assertTrue("if (initialImport == null && initialExamId == null && ownerUserId.isNotBlank())" in builderVm)
        // حالت ویرایش دیگر پیشنهاد بازیابی نمی‌دهد؛ فقط خود آزمون باز می‌شود.
        assertTrue("if (initialImport == null && ownerUserId.isNotBlank()) {\n                    val draft = draftStore.load" !in builderVm)
    }
}
