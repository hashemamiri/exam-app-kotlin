package ir.exam.app.ui.app

import androidx.compose.ui.geometry.Offset
import ir.exam.app.ui.printing.StudioShape
import ir.exam.app.ui.printing.alignShapes
import ir.exam.app.ui.printing.autoCropBounds
import ir.exam.app.ui.printing.bezierPolyline
import ir.exam.app.ui.printing.boundsToCropRect
import ir.exam.app.ui.printing.curveControlPoint
import ir.exam.app.ui.printing.despeckle
import ir.exam.app.ui.printing.distributeShapes
import ir.exam.app.ui.printing.flattenShadow
import ir.exam.app.ui.printing.groupMembers
import ir.exam.app.ui.printing.layerActionTargets
import ir.exam.app.ui.printing.luminanceOf
import ir.exam.app.ui.printing.nextGroupId
import ir.exam.app.ui.printing.reorderShape
import ir.exam.app.ui.printing.shapeBounds
import ir.exam.app.ui.printing.shapeLabel
import ir.exam.app.ui.printing.translateShape
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V77.0 — فیلترهای اسکن کتاب + قطره‌چکان + لایهٔ اشیاء + فلش منحنی + حالت تاریک.
 * این تست‌ها **منطق واقعی** را اجرا می‌کنند (نه فقط پینِ متنی)، چون کلِ فیلترها
 * روی IntArray نوشته شده‌اند و در JVM بدون اندروید قابل اجرا هستند.
 */
class V77_0StudioFiltersAndLayersTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val studio by lazy {
        File(root(), "app/src/main/java/ir/exam/app/ui/printing/ExamImageStudioCore.kt").readText()
    }

    private val WHITE = 0xFFFFFFFF.toInt()
    private val BLACK = 0xFF000000.toInt()

    private fun page(w: Int, h: Int, fill: Int = WHITE) = IntArray(w * h) { fill }

    private fun rgb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    // ---------------- حذف سایه و زردی ----------------

    @Test
    fun `shadow and paper yellowing are flattened while ink survives`() {
        val w = 60; val h = 60
        val px = page(w, h)
        // کاغذِ زردِ با سایهٔ شدید از چپ (تیره) به راست (روشن)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val base = 120 + (135 * x / (w - 1))
                px[y * w + x] = rgb(base, base, (base * 0.80f).toInt())
            }
        }
        // دو کلمهٔ تیره، یکی در ناحیهٔ سایه و یکی در ناحیهٔ روشن
        for (y in 25 until 35) {
            for (x in 10 until 20) px[y * w + x] = BLACK
            for (x in 42 until 52) px[y * w + x] = BLACK
        }
        val before = (0 until w).map { luminanceOf(px[5 * w + it]) }
        val out = flattenShadow(px, w, h, 1f)
        val after = (0 until w).map { luminanceOf(out[5 * w + it]) }

        val spreadBefore = before.max() - before.min()
        val spreadAfter = after.max() - after.min()
        assertTrue("سایه حذف نشد: $spreadBefore → $spreadAfter", spreadAfter < spreadBefore / 4)
        assertTrue("کاغذ سفید نشد", after.min() > 230)
        // جوهر در هر دو ناحیه تیره مانده است
        assertTrue("جوهرِ ناحیهٔ سایه از بین رفت", luminanceOf(out[30 * w + 15]) < 100)
        assertTrue("جوهرِ ناحیهٔ روشن از بین رفت", luminanceOf(out[30 * w + 46]) < 100)
    }

    @Test
    fun `zero strength leaves the image untouched`() {
        val w = 12; val h = 12
        val px = IntArray(w * h) { rgb(200, 190, 150) }
        assertTrue(flattenShadow(px, w, h, 0f).contentEquals(px))
    }

    // ---------------- حذف نویز و لکه ----------------

    @Test
    fun `isolated specks are removed`() {
        val w = 21; val h = 21
        val px = page(w, h)
        px[10 * w + 10] = BLACK
        val out = despeckle(px, w, h)
        assertEquals("لکهٔ منفرد پاک نشد", 255, luminanceOf(out[10 * w + 10]))
    }

    @Test
    fun `hairline strokes survive denoising`() {
        // درسِ شبیه‌سازی: میانهٔ سادهٔ ۳×۳ خطِ ۱ پیکسلی را کاملاً پاک می‌کرد.
        for (width in 1..3) {
            val w = 21; val h = 21
            val px = page(w, h)
            for (y in 3 until 18) {
                for (k in 0 until width) px[y * w + 5 + k] = BLACK
            }
            val out = despeckle(px, w, h)
            assertEquals("خطِ ${width}px پاک شد", 0, luminanceOf(out[10 * w + 5]))
        }
    }

    @Test
    fun `diagonal hairline survives and white holes are filled`() {
        val w = 21; val h = 21
        val diag = page(w, h)
        for (d in 3 until 18) diag[d * w + d] = BLACK
        assertEquals("خطِ مورب پاک شد", 0, luminanceOf(despeckle(diag, w, h)[10 * w + 10]))

        val blob = page(w, h)
        for (y in 5 until 16) for (x in 5 until 16) blob[y * w + x] = BLACK
        blob[10 * w + 10] = WHITE
        assertEquals("سوراخِ سفید پر نشد", 0, luminanceOf(despeckle(blob, w, h)[10 * w + 10]))
    }

    // ---------------- برش خودکار حاشیه ----------------

    @Test
    fun `auto crop finds the exact ink box`() {
        val w = 100; val h = 80
        val px = page(w, h)
        for (y in 30 until 50) for (x in 20 until 70) px[y * w + x] = BLACK
        val b = autoCropBounds(px, w, h, padRatio = 0f)
        assertEquals(20, b.left)
        assertEquals(30, b.top)
        assertEquals(70, b.right)
        assertEquals(50, b.bottom)
        assertEquals(50, b.width)
        assertEquals(20, b.height)
    }

    @Test
    fun `auto crop never returns an empty box for a blank page`() {
        val w = 40; val h = 30
        val b = autoCropBounds(page(w, h), w, h)
        assertEquals(0, b.left); assertEquals(0, b.top)
        assertEquals(w, b.right); assertEquals(h, b.bottom)
    }

    @Test
    fun `bounds convert to a normalised crop rect`() {
        val b = autoCropBounds(
            page(100, 80).also { px ->
                for (y in 30 until 50) for (x in 20 until 70) px[y * 100 + x] = BLACK
            },
            100, 80, padRatio = 0f
        )
        val r = boundsToCropRect(b, 100, 80)
        assertEquals(0.2f, r.left, 0.0001f)
        assertEquals(0.375f, r.top, 0.0001f)
        assertEquals(0.7f, r.right, 0.0001f)
        assertEquals(0.625f, r.bottom, 0.0001f)
    }

    // ---------------- فلش منحنی ----------------

    @Test
    fun `bezier keeps endpoints and bulges by the curve amount`() {
        val a = Offset(0f, 0f); val b = Offset(1f, 0f)
        val straight = bezierPolyline(a, b, 0f)
        assertEquals(a, straight.first())
        assertEquals(b, straight.last())
        assertTrue("خمیدگیِ صفر باید خطِ صاف بدهد", straight.all { kotlin.math.abs(it.y) < 1e-6f })

        val curved = bezierPolyline(a, b, 0.25f)
        assertEquals(25, curved.size)
        assertEquals(a, curved.first())
        assertEquals(b, curved.last())
        assertEquals(0.125f, curved[curved.size / 2].y, 0.0001f)
        // علامتِ مخالف = خمیدگی به سمت دیگر
        assertEquals(-0.125f, bezierPolyline(a, b, -0.25f)[12].y, 0.0001f)
    }

    @Test
    fun `control point is perpendicular to the segment`() {
        val c = curveControlPoint(Offset(0f, 0f), Offset(2f, 0f), 0.5f)
        assertEquals(1f, c.x, 0.0001f)
        assertEquals(1f, c.y, 0.0001f)
    }

    // ---------------- لایهٔ اشیاء ----------------

    private fun box(l: Float, t: Float, r: Float, b: Float, type: String = "rect") =
        StudioShape(type = type, points = listOf(Offset(l, t), Offset(r, b)))

    @Test
    fun `reorder moves a shape and reports its new index`() {
        val s = List(5) { box(it * 0.1f, 0f, it * 0.1f + 0.05f, 0.1f) }
        val (front, fi) = reorderShape(s, 1, "front")
        assertEquals(4, fi)
        assertEquals(s[1], front[4])
        val (back, bi) = reorderShape(s, 3, "back")
        assertEquals(0, bi)
        assertEquals(s[3], back[0])
        val (fwd, wi) = reorderShape(s, 2, "forward")
        assertEquals(3, wi)
        assertEquals(s[2], fwd[3])
        val (bwd, di) = reorderShape(s, 2, "backward")
        assertEquals(1, di)
        assertEquals(s[2], bwd[1])
        // بی‌معنا‌ها بی‌اثرند
        assertEquals(s, reorderShape(s, 4, "front").first)
        assertEquals(s, reorderShape(s, 99, "front").first)
    }

    @Test
    fun `align snaps edges without resizing`() {
        val s = listOf(
            box(0.1f, 0.1f, 0.2f, 0.2f),
            box(0.5f, 0.3f, 0.7f, 0.4f),
            box(0.8f, 0.6f, 0.9f, 0.7f)
        )
        val idx = listOf(0, 1, 2)
        val left = alignShapes(s, idx, "left")
        assertTrue(left.all { kotlin.math.abs(shapeBounds(it).left - 0.1f) < 0.0001f })
        // اندازه‌ها عوض نشده‌اند
        s.indices.forEach { i ->
            assertEquals(shapeBounds(s[i]).width, shapeBounds(left[i]).width, 0.0001f)
        }
        val right = alignShapes(s, idx, "right")
        assertTrue(right.all { kotlin.math.abs(shapeBounds(it).right - 0.9f) < 0.0001f })
        val centers = alignShapes(s, idx, "hcenter").map {
            val b = shapeBounds(it); (b.left + b.right) / 2f
        }
        assertTrue(centers.all { kotlin.math.abs(it - centers[0]) < 0.0001f })
        val top = alignShapes(s, idx, "top")
        assertTrue(top.all { kotlin.math.abs(shapeBounds(it).top - 0.1f) < 0.0001f })
        val bottom = alignShapes(s, idx, "bottom")
        assertTrue(bottom.all { kotlin.math.abs(shapeBounds(it).bottom - 0.7f) < 0.0001f })
    }

    @Test
    fun `locked shapes are never moved by align`() {
        val s = listOf(
            box(0.1f, 0.1f, 0.2f, 0.2f),
            box(0.5f, 0.3f, 0.7f, 0.4f).copy(locked = true),
            box(0.8f, 0.6f, 0.9f, 0.7f)
        )
        val out = alignShapes(s, listOf(0, 1, 2), "left")
        assertEquals("شیء قفل‌شده جابه‌جا شد", s[1], out[1])
        assertNotEquals(s[2], out[2])
    }

    @Test
    fun `distribute equalises spacing and keeps the ends fixed`() {
        val s = listOf(
            box(0.10f, 0f, 0.10f, 0.1f),
            box(0.15f, 0f, 0.15f, 0.1f),
            box(0.50f, 0f, 0.50f, 0.1f),
            box(0.90f, 0f, 0.90f, 0.1f)
        )
        val out = distributeShapes(s, listOf(0, 1, 2, 3), horizontal = true)
        val cs = out.map { val b = shapeBounds(it); (b.left + b.right) / 2f }.sorted()
        assertEquals(0.10f, cs.first(), 0.0001f)
        assertEquals(0.90f, cs.last(), 0.0001f)
        val g1 = cs[1] - cs[0]
        val g2 = cs[2] - cs[1]
        val g3 = cs[3] - cs[2]
        assertEquals(g1, g2, 0.0001f)
        assertEquals(g2, g3, 0.0001f)
        // کمتر از سه شکل بی‌معناست
        assertEquals(s, distributeShapes(s, listOf(0, 1), horizontal = true))
    }

    @Test
    fun `grouping keeps members together and ungrouping frees them`() {
        val s = listOf(box(0f, 0f, 0.1f, 0.1f), box(0.5f, 0.5f, 0.6f, 0.6f), box(0.8f, 0f, 0.9f, 0.1f))
        assertEquals(1, nextGroupId(s))
        val grouped = s.mapIndexed { i, sp -> if (i < 2) sp.copy(group = 1) else sp }
        assertEquals(listOf(0, 1), groupMembers(grouped, 0))
        assertEquals(listOf(2), groupMembers(grouped, 2))
        assertEquals(2, nextGroupId(grouped))
        // هدفِ عملیات: با گروه = فقط اعضا؛ بدون گروه = همهٔ آزادها
        assertEquals(listOf(0, 1), layerActionTargets(grouped, 0))
        assertEquals(listOf(0, 1, 2), layerActionTargets(s, 2))
        assertEquals(listOf(0, 2), layerActionTargets(
            s.mapIndexed { i, sp -> if (i == 1) sp.copy(locked = true) else sp }, 0
        ))
    }

    @Test
    fun `translate moves every point by the same delta`() {
        val sp = StudioShape(type = "free", points = listOf(Offset(0.1f, 0.2f), Offset(0.3f, 0.4f)))
        val moved = translateShape(sp, 0.05f, -0.05f)
        assertEquals(0.15f, moved.points[0].x, 0.0001f)
        assertEquals(0.15f, moved.points[0].y, 0.0001f)
        assertEquals(0.35f, moved.points[1].x, 0.0001f)
        assertEquals(0.35f, moved.points[1].y, 0.0001f)
        assertEquals(sp.points.size, moved.points.size)
    }

    @Test
    fun `layer labels are human readable and show state`() {
        assertTrue(shapeLabel(box(0f, 0f, 1f, 1f), 0).contains("کادر"))
        assertTrue(shapeLabel(box(0f, 0f, 1f, 1f), 0).startsWith("1."))
        assertTrue(shapeLabel(box(0f, 0f, 1f, 1f, "curve"), 4).contains("فلش منحنی"))
        val tagged = box(0f, 0f, 1f, 1f).copy(locked = true, hidden = true, group = 3)
        val label = shapeLabel(tagged, 1)
        assertTrue(label.contains("گروه 3"))
        assertTrue(label.contains("🔒"))
        assertTrue(label.contains("🚫"))
    }

    // ---------------- سیم‌کشیِ رابط ----------------

    @Test
    fun `studio exposes the new tools`() {
        assertTrue("📖 حذف سایه و زردی" in studio)
        assertTrue("🧽 حذف نویز و لکه" in studio)
        assertTrue("✂️ برش خودکار حاشیه" in studio)
        assertTrue("💧 قطره‌چکان" in studio)
        assertTrue("🪝 فلش منحنی" in studio)
        assertTrue("🌙 حالت تاریک" in studio)
        assertTrue("🗂 لایهٔ اشیاء" in studio)
        // فیلترها واقعاً وارد زنجیرهٔ خروجی شده‌اند
        assertTrue("applyBookScan(bmp, deshadow, denoise)" in studio)
        assertTrue("deshadowOn, denoiseOn" in studio)
        // شیء پنهان نه در پیش‌نمایش و نه در خروجی نیست
        assertTrue("if (sp.hidden) return@forEach" in studio)
        assertTrue("if (sp.hidden) return@forEachIndexed" in studio)
        // قفل‌شده انتخاب نمی‌شود؛ هم‌گروه‌ها با هم می‌روند
        assertTrue("if (sp.locked || sp.hidden) return@forEachIndexed" in studio)
        assertTrue("val movers = groupMembers(shapes, dragShapeIndex)" in studio)
    }
}
