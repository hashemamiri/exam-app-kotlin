package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V76.5 — ابزارهای صفحه/صاف‌سازی استودیوی بومی (مطابق ابزارهای استودیوی ۳۰):
 * ۱) 📐 صفحه‌ای (۴ گوشه): چهار نقطهٔ شماره‌دار قابل‌کشیدن + وارپِ پرسپکتیو با
 *    Matrix.setPolyToPoly (همان «✓ اعمال صاف‌سازی» استودیو).
 * ۲) ↯ صاف‌سازیِ دقیق ±۱۵° (اسلایدر ۰٫۱°) + شبکهٔ راهنما (deskewGrid).
 * ۳) 🎯 تشخیص خودکار زاویه با پروفایلِ تصویر (واریانسِ ردیف‌های تاریک، گام ۰٫۵°).
 * ۴) جریانِ خروجی: صاف‌سازی در همان ماتریسِ چرخشِ processAndEncode اعمال می‌شود.
 */
class V76_5StudioDeskewPerspectiveTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val studio by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt") }

    @Test
    fun `page corner picker with perspective warp is native`() {
        assertTrue("📐 صفحه‌ای (۴ گوشه)" in studio)
        assertTrue("✓ اعمال صاف‌سازی" in studio)
        assertTrue("var perspMode by remember { mutableStateOf(false) }" in studio)
        assertTrue("var perspPts by remember" in studio)
        assertTrue("nearestPerspIndex(" in studio)
        assertTrue("dragPerspIndex" in studio)
        assertTrue("private fun applyPerspective(" in studio)
        assertTrue("m.setPolyToPoly(srcPts, 0, dst, 0, 4)" in studio)
        // بعد از وارپ، حالت‌ها ریست می‌شوند تا خروجی از صفحهٔ صاف شروع شود
        assertTrue("""original = warped""" in studio)
        assertTrue("deskewAngle = 0f" in studio)
    }

    @Test
    fun `fine deskew slider and grid match the studio defaults`() {
        assertTrue("valueRange = -15f..15f" in studio)
        assertTrue("var deskewAngle by remember { mutableStateOf(0f) }" in studio)
        assertTrue("var deskewGrid by remember { mutableStateOf(false) }" in studio)
        // دکمهٔ صفر کردن صاف‌سازی (معادل «↺ صفر کردن» استودیو)
        assertTrue("صفر کردن صاف‌سازی" in studio)
        // پیش‌نمایش هم همان صاف‌سازی را می‌بیند
        assertTrue("if (!perspMode) {\n                                    postRotate(rotation.toFloat() + deskewAngle)" in studio)
    }

    @Test
    fun `automatic skew detection is projection-profile based`() {
        assertTrue("🎯 تشخیص خودکار زاویه" in studio)
        assertTrue("private fun detectSkewAngle(src: Bitmap, threshold: Int): Float" in studio)
        assertTrue("Bitmap.createScaledBitmap(src, w, h, true)" in studio)
        assertTrue("bestScore = v" in studio)
        // فراخوانی واقعی با همان آستانهٔ اسکن
        assertTrue("detectSkewAngle(src, threshold)" in studio)
    }

    @Test
    fun `nativeCanvas numbers need their extension import`() {
        // nativeCanvas یک خاصیت توسعه‌ای است (AndroidCanvas_androidKt.getNativeCanvas)
        // بدون این import: «Unresolved reference 'nativeCanvas'» در کامپایل
        assertTrue("import androidx.compose.ui.graphics.nativeCanvas" in studio)
        assertTrue("c.nativeCanvas.drawCircle(" in studio)
        assertTrue("c.nativeCanvas.drawText(" in studio)
    }

    @Test
    fun `deskew flows into the output pipeline`() {
        // processAndEncode حالا deskew را در ماتریسِ چرخش اعمال می‌کند
        assertTrue("postRotate(rotation.toFloat() + deskew)" in studio)
        assertTrue("src, rotation, deskewAngle, flip, crop, scanOn, threshold, outSize, quality" in studio)
        // شبکهٔ راهنما فقط کمکیِ بصری است
        assertTrue("if (deskewGrid)" in studio)
        // pointerInput با تغییر ابعاد/حالت تازه می‌شود (هندسهٔ کهنه نماند)
        // (V76.7: drawMode هم به کلید اضافه شد)
        assertTrue(".pointerInput(aspect, perspMode, splitMode, splitBoxes.size, drawMode, boxSize.width, boxSize.height)" in studio)
    }
}
