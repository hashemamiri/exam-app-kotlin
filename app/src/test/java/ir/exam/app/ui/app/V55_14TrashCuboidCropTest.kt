package ir.exam.app.ui.app

import ir.exam.app.ui.image.CropEdgeKind
import ir.exam.app.ui.image.CropGeometry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.14 — سه درخواست کاربر (پچ اول از دو پچ):
 * ۱) حذف سؤال: دکمهٔ متنی «حذف سؤال» حذف شد؛ آیکن سطل زباله کنار بارم +
 *    AlertDialog تأیید («سؤال n برای همیشه حذف شود؟») جایگزین شد.
 * ۲) تداخل شناسهٔ «box»: هم مکعب‌مستطیل هندسه بود هم نمودار جعبه‌ای؛ چون
 *    ChartSvgRenderer در renderBody مقدم است، مکعب‌مستطیل درج‌شده به‌شکل
 *    نمودار جعبه‌ای رندر می‌شد و پنجرهٔ درج شکل هم جعبه‌ای نشان می‌داد.
 *    رفع: شناسهٔ هندسه cuboid شد (svgOf مرجع هم cuboid→box نگاشت می‌کند)؛
 *    box بدون دادهٔ نمودار همچنان در شاخهٔ هندسه پشتیبانی می‌شود.
 * ۳) ویرایشگر تصویر: دستگیره‌های نامرئی قابل‌کشف نبودند («اضلاع قابل جابجایی
 *    نیست»)؛ دستگیره‌های مرئی میله‌ای وسط اضلاع + مربعی گوشه‌ها (سطح لمس 32dp)
 *    اضافه شد؛ resize بردار (dx,dy) می‌گیرد؛ گوشه‌ها با resizeDeltaForCorner و
 *    recenterAfterResize چهار حالت گوشه، گوشهٔ مقابل را ثابت نگه می‌دارند؛
 *    حرکت آزاد کل کادر از ناحیهٔ داخلی حفظ شد.
 */
class V55_14TrashCuboidCropTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val builder by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt") }
    private val gallery by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureGallery.kt") }
    private val renderer by lazy { source("app/src/main/java/ir/exam/app/core/figure/FigureSvgRenderer.kt") }
    private val editor by lazy { source("app/src/main/java/ir/exam/app/ui/image/InteractiveImageEditorDialog.kt") }

    @Test
    fun `question delete is a trash icon next to score with confirmation`() {
        assertTrue("Icons.Outlined.Delete" in builder)
        assertTrue("confirmDelete = true" in builder)
        assertTrue("برای همیشه حذف شود؟" in builder)
        assertFalse("TextButton(onClick = { viewModel.remove(question.id) }) { Text(\"حذف سؤال\") }" in builder)
    }

    @Test
    fun `cuboid geometry no longer collides with the box chart`() {
        assertTrue("FigureTemplate(\"cuboid\", \"مکعب‌مستطیل\"" in gallery)
        assertFalse("FigureTemplate(\"box\", \"مکعب‌مستطیل\"" in gallery)
        assertTrue("\"cube\", \"cuboid\", \"box\" ->" in renderer)
        // مرجع هم نام جدید را می‌فهمد.
        // نمودار جعبه‌ای دست‌نخورده است.
        assertTrue("FigureTemplate(\"box\", \"جعبه‌ای\"" in gallery)
    }

    @Test
    fun `corner resize math keeps the opposite corner fixed`() {
        // کشیدن گوشهٔ پایین-راست به بیرون = بزرگ‌شدن.
        assertEquals(40f, CropGeometry.resizeDeltaForCorner(CropEdgeKind.BOTTOM_RIGHT, 40f, 30f), 0f)
        // کشیدن گوشهٔ بالا-چپ به بیرون (منفی) = بزرگ‌شدن.
        assertEquals(40f, CropGeometry.resizeDeltaForCorner(CropEdgeKind.TOP_LEFT, -40f, -30f), 0f)
        val (x, y) = CropGeometry.recenterAfterResize(CropEdgeKind.TOP_LEFT, 40f, 400f, 400f, .5f, .5f)
        assertTrue(x < .5f && y < .5f)
        val (x2, y2) = CropGeometry.recenterAfterResize(CropEdgeKind.BOTTOM_RIGHT, 40f, 400f, 400f, .5f, .5f)
        assertTrue(x2 > .5f && y2 > .5f)
    }

    @Test
    fun `crop frame has visible edge and corner handles with free movement`() {
        assertTrue("CropHandle(CropEdgeKind.TOP_LEFT" in editor)
        assertTrue("CropHandle(CropEdgeKind.BOTTOM_RIGHT" in editor)
        assertTrue("bar = true" in editor)
        // V55.15 — callbacks از rememberUpdatedState می‌آیند تا stale نشوند.
        assertTrue("currentOnResize(edge, drag.x, drag.y)" in editor)
        // حرکت آزاد کل کادر از ناحیهٔ داخلی.
        assertTrue("currentOnMove(drag.x, drag.y)" in editor)
    }
}
