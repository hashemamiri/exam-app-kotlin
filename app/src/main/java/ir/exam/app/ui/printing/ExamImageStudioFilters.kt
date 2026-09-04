package ir.exam.app.ui.printing

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * V77.0 — فیلترهای «اسکن تمیز کتاب» + کمک‌ریاضی‌های لایهٔ اشیاء و فلش منحنی.
 *
 * قاعدهٔ طراحی: تمام منطق روی `IntArray` پیکسل و دادهٔ خالص است تا در تستِ
 * معمولیِ JVM (بدون اندروید/Robolectric) واقعاً اجرا و راستی‌آزمایی شود؛
 * پوشش‌های نازکِ `Bitmap` فقط پیکسل‌ها را می‌گیرند و برمی‌گردانند.
 * هیچ کتابخانهٔ جدیدی اضافه نشده است.
 */

/** کادرِ پیکسلی (شاملِ چپ/بالا، مستثنیِ راست/پایین). */
data class PixelBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** روشناییِ استانداردِ یک پیکسلِ ARGB (همان ضرایبِ استفاده‌شده در حالت اسکن). */
internal fun luminanceOf(pixel: Int): Int {
    val r = (pixel shr 16) and 0xFF
    val g = (pixel shr 8) and 0xFF
    val b = pixel and 0xFF
    return (r * 299 + g * 587 + b * 114) / 1000
}

internal fun packGray(v: Int): Int {
    val c = v.coerceIn(0, 255)
    return (0xFF shl 24) or (c shl 16) or (c shl 8) or c
}

/**
 * برآوردِ «کاغذِ پس‌زمینه» برای هر پیکسل: بیشینهٔ روشناییِ محلی روی یک شبکهٔ
 * درشت، سپس درون‌یابیِ دوخطی. سایهٔ کتاب و زردیِ کاغذ تغییراتِ **کم‌بسامد**اند،
 * پس با تقسیمِ تصویر بر این برآورد حذف می‌شوند و متن (پُربسامد) می‌ماند.
 *
 * @param cells تعداد خانه‌های شبکه در بزرگ‌ترین بُعد.
 */
internal fun estimateBackground(px: IntArray, w: Int, h: Int, cells: Int = 16): IntArray {
    if (w <= 0 || h <= 0 || px.size < w * h) return IntArray(max(0, w * h)) { 255 }
    val gx = max(1, min(cells, w))
    val gy = max(1, min(cells, h))
    // بیشینهٔ روشنایی در هر خانه = رنگِ کاغذ در آن ناحیه
    val grid = IntArray(gx * gy)
    for (cy in 0 until gy) {
        val y0 = cy * h / gy
        val y1 = max(y0 + 1, (cy + 1) * h / gy)
        for (cx in 0 until gx) {
            val x0 = cx * w / gx
            val x1 = max(x0 + 1, (cx + 1) * w / gx)
            var best = 0
            var y = y0
            while (y < y1) {
                var x = x0
                val row = y * w
                while (x < x1) {
                    val l = luminanceOf(px[row + x])
                    if (l > best) best = l
                    x++
                }
                y++
            }
            grid[cy * gx + cx] = if (best <= 0) 1 else best
        }
    }
    // درون‌یابیِ دوخطی تا لبه‌های خانه‌ها پله‌ای نشوند
    val out = IntArray(w * h)
    for (y in 0 until h) {
        val fy = ((y + 0.5f) * gy / h) - 0.5f
        val y0 = fy.toInt().coerceIn(0, gy - 1)
        val y1 = (y0 + 1).coerceAtMost(gy - 1)
        val ty = (fy - y0).coerceIn(0f, 1f)
        for (x in 0 until w) {
            val fx = ((x + 0.5f) * gx / w) - 0.5f
            val x0 = fx.toInt().coerceIn(0, gx - 1)
            val x1 = (x0 + 1).coerceAtMost(gx - 1)
            val tx = (fx - x0).coerceIn(0f, 1f)
            val a = grid[y0 * gx + x0].toFloat()
            val b = grid[y0 * gx + x1].toFloat()
            val c = grid[y1 * gx + x0].toFloat()
            val d = grid[y1 * gx + x1].toFloat()
            val top = a + (b - a) * tx
            val bot = c + (d - c) * tx
            out[y * w + x] = (top + (bot - top) * ty).roundToInt().coerceIn(1, 255)
        }
    }
    return out
}

/**
 * «حذف سایه و زردیِ کاغذ»: هر پیکسل بر کاغذِ محلیِ خودش تقسیم می‌شود، پس
 * کاغذ در همه‌جا سفیدِ یکدست می‌شود و فقط جوهر تیره می‌ماند. خروجی
 * خاکستری است (زردی ذاتاً حذف می‌شود چون رنگ کاغذ مبنا قرار می‌گیرد).
 *
 * @param strength ۰ = بی‌اثر، ۱ = کاملِ اثر.
 *
 * **محدودیتِ صادقانه (شبیه‌سازی‌شده)**: اگر ناحیهٔ خاکستریِ *بزرگی* (مثل یک عکسِ
 * تمام‌صفحه) در تصویر باشد، آن ناحیه هم «کاغذ» فرض و سفید می‌شود. برای همین این
 * فیلتر پیش‌فرض **خاموش** است و کاربر آگاهانه روشنش می‌کند؛ برای عکسِ صفحهٔ
 * کتاب/جزوه (کاربردِ هدف) درست عمل می‌کند.
 */
internal fun flattenShadow(px: IntArray, w: Int, h: Int, strength: Float = 1f): IntArray {
    if (w <= 0 || h <= 0 || px.size < w * h) return px
    val s = strength.coerceIn(0f, 1f)
    if (s <= 0f) return px.copyOf()
    val bg = estimateBackground(px, w, h)
    val out = IntArray(w * h)
    for (i in 0 until w * h) {
        val l = luminanceOf(px[i])
        val paper = bg[i].coerceAtLeast(1)
        val normalized = (l * 255f / paper).roundToInt().coerceIn(0, 255)
        val mixed = (l + (normalized - l) * s).roundToInt().coerceIn(0, 255)
        out[i] = packGray(mixed)
    }
    return out
}

/**
 * «حذف نویز و لکه» — نسخهٔ حافظِ خط.
 *
 * میانهٔ سادهٔ ۳×۳ لکه‌ها را می‌بَرد ولی **خطوطِ یک‌پیکسلی را هم پاک می‌کند**
 * (در شبیه‌سازی اثبات شد: خطِ ۱px کاملاً محو می‌شد) — و حروف فارسی پر از
 * خطوطِ نازک‌اند. پس فقط پیکسل‌هایی اصلاح می‌شوند که واقعاً **منفرد** باشند:
 * پیکسلِ تیره‌ای که در همسایگیِ ۸‌تایی‌اش کمتر از `minNeighbors` همسایهٔ تیره
 * دارد = لکه؛ وگرنه دست‌نخورده می‌ماند. پیکسل‌های روشنِ منفرد (سوراخِ سفید
 * وسطِ جوهر) هم به همین شکل پر می‌شوند.
 *
 * @param minNeighbors حداقل همسایهٔ هم‌جنس تا پیکسل «بخشی از خط» شمرده شود.
 */
internal fun despeckle(px: IntArray, w: Int, h: Int, minNeighbors: Int = 2): IntArray {
    if (w < 3 || h < 3 || px.size < w * h) return px.copyOf()
    val lum = IntArray(w * h) { luminanceOf(px[it]) }
    // آستانهٔ تیرگی از میانگینِ تصویر (بدون نیاز به تنظیمِ کاربر)
    var sum = 0L
    for (v in lum) sum += v
    val mean = (sum / (w * h)).toInt()
    val darkAt = { i: Int -> lum[i] < mean }
    val out = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                out[i] = packGray(lum[i]); continue
            }
            val self = darkAt(i)
            var same = 0
            var otherSum = 0
            for (dy in -1..1) {
                val row = (y + dy) * w
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val j = row + x + dx
                    if (darkAt(j) == self) same++ else otherSum += lum[j]
                }
            }
            out[i] = if (same < minNeighbors && same < 8) {
                // پیکسلِ منفرد → میانگینِ همسایه‌های مخالف (حذفِ لکه/سوراخ)
                packGray(otherSum / (8 - same).coerceAtLeast(1))
            } else {
                packGray(lum[i])
            }
        }
    }
    return out
}

/**
 * «برش خودکار حاشیه‌های سفید»: کاغذ را از روی روشناییِ غالبِ لبه‌ها می‌شناسد و
 * کوچک‌ترین کادرِ شاملِ محتوای تیره را برمی‌گرداند (به‌اضافهٔ کمی حاشیه).
 * اگر چیزی پیدا نشود، کلِ تصویر برگردانده می‌شود (هرگز کادرِ خالی نمی‌دهد).
 *
 * @param tolerance فاصله از رنگ کاغذ که هنوز «کاغذ» حساب می‌شود.
 * @param padRatio حاشیهٔ امنِ افزوده، نسبت به بزرگ‌ترین بُعد.
 */
internal fun autoCropBounds(
    px: IntArray,
    w: Int,
    h: Int,
    tolerance: Int = 28,
    padRatio: Float = 0.01f
): PixelBounds {
    val whole = PixelBounds(0, 0, max(w, 0), max(h, 0))
    if (w <= 2 || h <= 2 || px.size < w * h) return whole
    // رنگِ کاغذ = میانهٔ روشناییِ نوارِ لبه‌ها (حاشیه معمولاً خالی است)
    val edge = ArrayList<Int>(2 * (w + h))
    for (x in 0 until w) {
        edge.add(luminanceOf(px[x]))
        edge.add(luminanceOf(px[(h - 1) * w + x]))
    }
    for (y in 0 until h) {
        edge.add(luminanceOf(px[y * w]))
        edge.add(luminanceOf(px[y * w + w - 1]))
    }
    edge.sort()
    val paper = edge[edge.size / 2]
    val limit = paper - tolerance
    var left = w
    var top = h
    var right = -1
    var bottom = -1
    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            if (luminanceOf(px[row + x]) < limit) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return whole
    val pad = (max(w, h) * padRatio).roundToInt().coerceAtLeast(0)
    return PixelBounds(
        left = (left - pad).coerceAtLeast(0),
        top = (top - pad).coerceAtLeast(0),
        right = (right + 1 + pad).coerceAtMost(w),
        bottom = (bottom + 1 + pad).coerceAtMost(h)
    )
}

/** کادرِ پیکسلی → کادرِ نرمال‌شدهٔ ۰..۱ که زنجیرهٔ برشِ استودیو می‌فهمد. */
internal fun boundsToCropRect(b: PixelBounds, w: Int, h: Int): Rect {
    if (w <= 0 || h <= 0) return Rect(0f, 0f, 1f, 1f)
    val l = (b.left.toFloat() / w).coerceIn(0f, 1f)
    val t = (b.top.toFloat() / h).coerceIn(0f, 1f)
    val r = (b.right.toFloat() / w).coerceIn(l + 0.01f, 1f)
    val bt = (b.bottom.toFloat() / h).coerceIn(t + 0.01f, 1f)
    return Rect(l, t, r, bt)
}

// ---------------------------------------------------------------------------
// پوشش‌های Bitmap (نازک — فقط پیکسل می‌گیرند/می‌دهند)
// ---------------------------------------------------------------------------

internal fun bitmapPixels(bmp: Bitmap): IntArray {
    val px = IntArray(bmp.width * bmp.height)
    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
    return px
}

internal fun pixelsToBitmap(px: IntArray, w: Int, h: Int): Bitmap =
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(px, 0, w, 0, 0, w, h)
    }

/** اجرای فیلترهای انتخاب‌شدهٔ «اسکن تمیز کتاب» روی یک بیت‌مپ. */
internal fun applyBookScan(src: Bitmap, shadow: Boolean, denoise: Boolean): Bitmap = runCatching {
    if (!shadow && !denoise) return@runCatching src
    val w = src.width
    val h = src.height
    var px = bitmapPixels(src)
    if (shadow) px = flattenShadow(px, w, h, 1f)
    if (denoise) px = despeckle(px, w, h)
    pixelsToBitmap(px, w, h)
}.getOrNull() ?: src

/** رنگِ پیکسل در مختصاتِ نرمال‌شده — برای قطره‌چکان. */
internal fun samplePixelColor(bmp: Bitmap, nx: Float, ny: Float): Int {
    val x = (nx * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
    val y = (ny * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
    return bmp.getPixel(x, y)
}

// ---------------------------------------------------------------------------
// فلش منحنی — نمونه‌برداریِ بزیه (بدون API دیفرکتِ Compose)
// ---------------------------------------------------------------------------

/**
 * نقطهٔ کنترلِ منحنی: وسطِ پاره‌خط، جابه‌جا شده در راستای **عمود** بر آن.
 * `curve` مثبت/منفی جهتِ خمیدگی را عوض می‌کند.
 */
internal fun curveControlPoint(a: Offset, b: Offset, curve: Float): Offset {
    val mx = (a.x + b.x) / 2f
    val my = (a.y + b.y) / 2f
    val dx = b.x - a.x
    val dy = b.y - a.y
    return Offset(mx - dy * curve, my + dx * curve)
}

/**
 * نمونه‌برداریِ منحنیِ درجه‌دومِ بزیه به چند پاره‌خط. عمداً به‌جای
 * `quadraticBezierTo` (که در Compose منسوخ شده و در لاگ CI هشدار می‌دهد)
 * استفاده می‌شود: هم بدونِ ریسکِ API، هم عیناً قابلِ تست در JVM.
 */
internal fun bezierPolyline(a: Offset, b: Offset, curve: Float, steps: Int = 24): List<Offset> {
    val n = steps.coerceAtLeast(2)
    val c = curveControlPoint(a, b, curve)
    val pts = ArrayList<Offset>(n + 1)
    for (i in 0..n) {
        val t = i.toFloat() / n
        val u = 1f - t
        val x = u * u * a.x + 2f * u * t * c.x + t * t * b.x
        val y = u * u * a.y + 2f * u * t * c.y + t * t * b.y
        pts.add(Offset(x, y))
    }
    return pts
}

// ---------------------------------------------------------------------------
// لایهٔ پیشرفتهٔ اشیاء — ترتیب، هم‌ترازی، توزیع (منطقِ خالص)
// ---------------------------------------------------------------------------

/** کادرِ محیطیِ یک شکل در مختصات نرمال‌شده. */
internal fun shapeBounds(sp: StudioShape): Rect {
    if (sp.points.isEmpty()) return Rect(0f, 0f, 0f, 0f)
    val xs = sp.points.map { it.x }
    val ys = sp.points.map { it.y }
    val l = xs.minOrNull() ?: 0f
    val t = ys.minOrNull() ?: 0f
    val r = xs.maxOrNull() ?: 0f
    val b = ys.maxOrNull() ?: 0f
    return Rect(l, t, r, b)
}

/** جابه‌جاییِ یک شکل (بدونِ تغییرِ شکل و اندازه). */
internal fun translateShape(sp: StudioShape, dx: Float, dy: Float): StudioShape =
    sp.copy(points = sp.points.map { Offset(it.x + dx, it.y + dy) })

/**
 * تغییرِ ترتیبِ رسم. عملیات: `front`/`back`/`forward`/`backward`.
 * اندیسِ جدیدِ همان شکل هم برگردانده می‌شود تا انتخابِ کاربر گم نشود.
 */
internal fun reorderShape(
    shapes: List<StudioShape>,
    index: Int,
    op: String
): Pair<List<StudioShape>, Int> {
    if (index !in shapes.indices || shapes.size < 2) return shapes to index
    val target = when (op) {
        "front" -> shapes.lastIndex
        "back" -> 0
        "forward" -> (index + 1).coerceAtMost(shapes.lastIndex)
        "backward" -> (index - 1).coerceAtLeast(0)
        else -> index
    }
    if (target == index) return shapes to index
    val list = shapes.toMutableList()
    val item = list.removeAt(index)
    list.add(target, item)
    return list to target
}

/**
 * هم‌ترازیِ گروهی. حالت‌ها: `left`/`hcenter`/`right`/`top`/`vcenter`/`bottom`.
 * مرجع، کادرِ محیطیِ کلِ انتخاب است. شکل‌های **قفل‌شده** جابه‌جا نمی‌شوند.
 */
internal fun alignShapes(
    shapes: List<StudioShape>,
    indices: List<Int>,
    mode: String
): List<StudioShape> {
    val valid = indices.filter { it in shapes.indices && !shapes[it].locked }
    if (valid.size < 2) return shapes
    val boxes = valid.map { shapeBounds(shapes[it]) }
    val gl = boxes.minOf { it.left }
    val gt = boxes.minOf { it.top }
    val gr = boxes.maxOf { it.right }
    val gb = boxes.maxOf { it.bottom }
    val list = shapes.toMutableList()
    valid.forEachIndexed { i, si ->
        val bx = boxes[i]
        val dx = when (mode) {
            "left" -> gl - bx.left
            "right" -> gr - bx.right
            "hcenter" -> (gl + gr) / 2f - (bx.left + bx.right) / 2f
            else -> 0f
        }
        val dy = when (mode) {
            "top" -> gt - bx.top
            "bottom" -> gb - bx.bottom
            "vcenter" -> (gt + gb) / 2f - (bx.top + bx.bottom) / 2f
            else -> 0f
        }
        if (dx != 0f || dy != 0f) list[si] = translateShape(list[si], dx, dy)
    }
    return list
}

/**
 * توزیعِ یکنواخت: فاصلهٔ مرکزِ شکل‌ها بین اولین و آخرین شکل مساوی می‌شود.
 * کمتر از سه شکل معنایی ندارد و بدون تغییر برمی‌گردد.
 */
internal fun distributeShapes(
    shapes: List<StudioShape>,
    indices: List<Int>,
    horizontal: Boolean
): List<StudioShape> {
    val valid = indices.filter { it in shapes.indices }
    if (valid.size < 3) return shapes
    val centerOf = { si: Int ->
        val b = shapeBounds(shapes[si])
        if (horizontal) (b.left + b.right) / 2f else (b.top + b.bottom) / 2f
    }
    val ordered = valid.sortedBy { centerOf(it) }
    val first = centerOf(ordered.first())
    val last = centerOf(ordered.last())
    val step = (last - first) / (ordered.size - 1)
    val list = shapes.toMutableList()
    ordered.forEachIndexed { i, si ->
        if (i == 0 || i == ordered.lastIndex) return@forEachIndexed
        if (shapes[si].locked) return@forEachIndexed
        val delta = (first + step * i) - centerOf(si)
        if (abs(delta) > 0.00001f) {
            list[si] = if (horizontal) translateShape(list[si], delta, 0f)
            else translateShape(list[si], 0f, delta)
        }
    }
    return list
}

/** شماره‌های گروهِ استفاده‌شده (۰ = بدون گروه). */
internal fun nextGroupId(shapes: List<StudioShape>): Int =
    (shapes.maxOfOrNull { it.group } ?: 0) + 1

/** اندیسِ همهٔ هم‌گروهی‌های یک شکل (اگر گروهی نداشته باشد، فقط خودش). */
internal fun groupMembers(shapes: List<StudioShape>, index: Int): List<Int> {
    if (index !in shapes.indices) return emptyList()
    val g = shapes[index].group
    if (g == 0) return listOf(index)
    return shapes.indices.filter { shapes[it].group == g }
}

/** برچسبِ فارسیِ یک شیء در فهرست لایه‌ها. */
internal fun shapeLabel(sp: StudioShape, index: Int): String {
    val name = when (sp.type) {
        "arrow" -> "فلش"
        "arrow2" -> "فلش دوسر"
        "curve" -> "فلش منحنی"
        "line" -> "خط"
        "rect" -> "کادر"
        "ellipse" -> "بیضی"
        "free" -> "خط آزاد"
        "highlighter" -> "هایلایتر"
        "censor" -> "سانسور"
        "text" -> "متن: " + sp.text.take(14)
        else -> sp.type
    }
    val tags = buildString {
        if (sp.group != 0) append(" [گروه ").append(sp.group).append("]")
        if (sp.locked) append(" 🔒")
        if (sp.hidden) append(" 🚫")
    }
    return "" + (index + 1) + ". " + name + tags
}

/**
 * هدفِ عملیاتِ گروهی: اگر شکلِ انتخاب‌شده گروه دارد، اعضای گروه؛ وگرنه **همهٔ**
 * شکل‌های قابلِ جابه‌جایی. این‌طور دکمه‌های هم‌ترازی روی انتخابِ تکی هم کار
 * می‌کنند (رفتارِ موردانتظارِ کاربر) به‌جای آنکه بی‌اثر باشند.
 */
internal fun layerActionTargets(shapes: List<StudioShape>, selected: Int): List<Int> {
    if (selected in shapes.indices && shapes[selected].group != 0) {
        return groupMembers(shapes, selected)
    }
    return shapes.indices.filter { !shapes[it].locked }
}
