package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.7 — ابزارهای رسمِ بومیِ استودیو (لاک‌گیر/برچسب/فلش) مطابق مجموعهٔ استودیوی ۳۰:
 * ۱) فلش، فلش دوسر، خط، کادر، بیضی، خط آزاد، هایلایتر نیمه‌شفاف، سانسورِ پیکسلی، برچسب متنی.
 * ۲) ۴ رنگ (قرمز/آبی/مشکی/سبز) + انتخاب/جابه‌جایی + بازگردانی/انجام مجدد + حذف انتخاب/پاک کردن همه.
 * ۳) مقایسهٔ قبل/بعد (نمای تصویرِ خامِ ورودی).
 * ۴) پخت در زنجیرهٔ خروجی: bakeShapes داخل encodeCropped — درجِ تکی و هر بخشِ تفکیک
 *    شکل‌ها را دارند؛ سانسور پیکسلی واقعی (میانگین بلوک‌ها) روی بیت‌مپ.
 */
class V76_7StudioDrawToolsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val studio by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }

    @Test
    fun `all drawing tools are native with studio parity`() {
        assertTrue("data class StudioShape(" in studio)
        assertTrue("var drawMode by remember { mutableStateOf(\"none\") }" in studio)
        assertTrue("var shapes by remember { mutableStateOf(listOf<StudioShape>()) }" in studio)
        assertTrue("var redoStack by remember { mutableStateOf(listOf<StudioShape>()) }" in studio)
        assertTrue("ToolChip(\"➡️ فلش\") { setDraw(\"arrow\") }" in studio)
        assertTrue("ToolChip(\"↔️ فلش دوسر\") { setDraw(\"arrow2\") }" in studio)
        assertTrue("ToolChip(\"📏 خط\") { setDraw(\"line\") }" in studio)
        assertTrue("ToolChip(\"⬜ کادر\") { setDraw(\"rect\") }" in studio)
        assertTrue("ToolChip(\"⭕ بیضی\") { setDraw(\"ellipse\") }" in studio)
        assertTrue("ToolChip(\"✏️ خط آزاد\") { setDraw(\"free\") }" in studio)
        assertTrue("ToolChip(\"🖍️ هایلایتر\") { setDraw(\"highlighter\") }" in studio)
        assertTrue("ToolChip(\"🚫 سانسور\") { setDraw(\"censor\") }" in studio)
        assertTrue("ToolChip(\"🔤 متن\") { setDraw(\"text\") }" in studio)
        assertTrue("label = { Text(\"👆 انتخاب/جابجایی\") }" in studio)
    }

    @Test
    fun `four colors and full shape management row`() {
        assertTrue("ToolChip(\"🔴\") { drawColor = 0xFFDC2626.toInt() }" in studio)
        assertTrue("ToolChip(\"🔵\") { drawColor = 0xFF2563EB.toInt() }" in studio)
        assertTrue("ToolChip(\"⚫\") { drawColor = 0xFF111827.toInt() }" in studio)
        assertTrue("ToolChip(\"🟢\") { drawColor = 0xFF16A34A.toInt() }" in studio)
        assertTrue("ToolChip(\"↩️ بازگردانی\") {" in studio)
        assertTrue("ToolChip(\"↪️ انجام مجدد\") {" in studio)
        assertTrue("ToolChip(\"🗑️ حذف انتخاب\") {" in studio)
        assertTrue("ToolChip(\"🧹 پاک کردن همه\") {" in studio)
        // برچسب متنی با دیالوگ ورودی متن
        assertTrue("title = { Text(\"متن برچسب\") }," in studio)
        assertTrue("if (drawMode == \"text\") {" in studio)
        assertTrue("textPromptPoint = Offset(nx, ny)" in studio)
    }

    @Test
    fun `gesture model draws moves and commits shapes`() {
        assertTrue("Corner.DRAW" in studio)
        assertTrue("Corner.SHAPE_MOVE" in studio)
        assertTrue("private var dragShapeIndex by androidx.compose.runtime.mutableStateOf(-1)" in studio)
        // کلید pointerInput با drawMode تازه می‌شود
        assertTrue(".pointerInput(aspect, perspMode, splitMode, splitBoxes.size, drawMode, boxSize.width, boxSize.height)" in studio)
        // ترسیمِ نیمه‌شفافِ هایلایتر در پیش‌نمایش
        assertTrue("drawPath(path, if (hl) col.copy(alpha = 0.42f) else col, style = st)" in studio)
    }

    @Test
    fun `before-after compare uses the untouched import`() {
        assertTrue("label = { Text(\"👁 قبل/بعد\") }" in studio)
        assertTrue("if (previewOriginal && openedWith != null) openedWith!!.asImageBitmap() else imgBitmap" in studio)
        // سه نقطهٔ ورود تصویر، اسنپ‌شات «قبل» را ثبت می‌کنند
        assertTrue("openedWith = decoded" in studio)
        assertTrue("openedWith = bmp" in studio)
    }

    @Test
    fun `hit-testing and line drawing use the corrected kotlin forms`() {
        // V76.7.1 — kotlin.math.min/max فقط دوارگومان اسکالر؛ شکلِ لیستی minOrNull/maxOrNull
        assertTrue("val xsMin = xs.minOrNull() ?: nx" in studio)
        assertTrue("val ysMax = ys.maxOrNull() ?: ny" in studio)
        // drawLine عرض خط را با نام می‌گیرد؛ Stroke مالِ style= است
        assertTrue("drawLine(col, a, b2, strokeWidth = sw)" in studio)
        assertTrue("drawLine(col, a, b2, st)" !in studio)
    }

    @Test
    fun `shapes are baked into the output pipeline`() {
        assertTrue("internal fun bakeShapes(base: Bitmap, shapes: List<StudioShape>): Bitmap" in studio)
        // V77.0 — پس از deshadow/denoise، دو پارامتر بعد از shapes آمده‌اند؛
        // پین به شکلِ فعلیِ امضا به‌روز شد (خودِ زنجیره تغییری نکرده است).
        assertTrue(
            "    shapes: List<StudioShape> = emptyList(),\n" +
                "    deshadow: Boolean = false,\n" +
                "    denoise: Boolean = false\n" +
                "): Pair<String, Int>? = runCatching {" in studio
        )
        assertTrue("// V76.7 — شکل‌ها قبل از برش روی تصویر پخته می‌شوند" in studio)
        assertTrue("val painted = if (shapes.isEmpty()) bmp else bakeShapes(bmp, shapes)" in studio)
        // درج تکی و هر دو مسیر تفکیک از زنجیرهٔ شکل‌دار می‌روند
        assertTrue("encodeCropped(bmp, crop, scanOn, threshold, outSize, quality, shapes, deshadow, denoise)" in studio)
        assertTrue("encodeCropped(base, b, scanOn, threshold, outSize, quality, shapes, deshadowOn, denoiseOn)" in studio)
        assertTrue("src, rotation, deskewAngle, flip, crop, scanOn, threshold, outSize, quality, shapes" in studio)
        // سانسور پیکسلی واقعی + بیضی + سر فلش با atan2
        assertTrue("base.getPixels(px, 0, rgnW, x0i, y0i, rgnW, rgnH)" in studio)
        assertTrue("cv.drawOval(android.graphics.RectF(l, t, r, b), paint)" in studio)
        assertTrue("val ang = kotlin.math.atan2(y1 - y0, x1 - x0)" in studio)
        assertTrue("if (sp.type == \"arrow2\") head(x0, y0, ang + Math.PI.toFloat())" in studio)
    }
}
