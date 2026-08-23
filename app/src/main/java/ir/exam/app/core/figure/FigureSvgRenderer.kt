package ir.exam.app.core.figure

import ir.exam.app.core.math.MathSvgDocument
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

/**
 * رندر شکل‌های هندسی و نمودارها به SVG امن و مستقل — همان خروجی وب‌اپ ولی با
 * ویژگی‌های inline (بدون `<style>`، script یا URL خارجی) تا با Coil SvgDecoder
 * سازگار باشد. دستگاه مختصات همان `viewBox="0 0 360 280"` وب‌اپ است.
 */
// توابع کمکی به‌صورت top-level تعریف شده‌اند تا کلاس تودرتوی Axes هم
// بدون وابستگی به scope شیء بتواند از آن‌ها استفاده کند.

private fun fmt(v: Float): String {
    val r = round(v * 100f) / 100f
    return if (r == r.toInt().toFloat()) r.toInt().toString()
    else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
}

private fun fmtInt(v: Float): String = round(v).toInt().toString()

private fun escapeXml(value: String): String = buildString(value.length) {
    value.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(char)
        }
    }
}

private fun txt(x: Float, y: Float, s: String, color: String, anchor: String = "", serif: Boolean = false, size: Int = 14): String {
    if (s.isBlank()) return ""
    val fam = if (serif) "serif" else "sans-serif"
    val anchorAttr = if (anchor.isNotBlank()) " text-anchor=\"$anchor\"" else ""
    return "<text x=\"${fmt(x)}\" y=\"${fmt(y)}\" font-family=\"$fam\" font-size=\"$size\" font-weight=\"700\" fill=\"$color\"$anchorAttr>${escapeXml(s)}</text>"
}

object FigureSvgRenderer {

    private const val STROKE = "#2c3a50"
    private const val VERTEX_COLOR = "#243044"
    private const val SIDE_COLOR = "#15607a"
    private const val ANGLE_COLOR = "#5b52e0"
    private const val TEAL = "#1f8a78"
    private const val ARC = "#6c63f5"
    private const val WIDTH = 320f
    private const val HEIGHT = 250f

    private val barColors = listOf(
        "#6c63f5", "#27c4a8", "#f0a202", "#e4572e",
        "#4c9be8", "#9b5de5", "#00bbf9", "#f15bb5"
    )

    fun render(spec: FigureSpec): MathSvgDocument {
        // V53.1 — توکن‌های جدول (`k='t'`) مسیر رندر اختصاصی خود را دارند تا
        // در همهٔ نمایش‌ها (Builder/دانش‌آموز/چاپ) بدون تغییر فراخوان‌ها کار کنند.
        if (spec.isTable) return TableSvgRenderer.render(spec)
        // V53.2 — جدول تناوبی (`k='p'`) رندر Native کامل دارد.
        if (spec.kind == "p") return PeriodicSvgRenderer.render(spec)
        // V53.1 — آناتومی/فیزیک/شیمی تا تحویل رندر Native (V53.3)
        // پلاک عنوان‌دار امن می‌گیرند؛ نه JSON خام و نه هندسهٔ نامربوط.
        if (spec.kind in setOf("a", "s")) return renderKindPlate(spec)
        val body = renderBody(spec)
        val viewBox = if (spec.type == "parll") "0 0 380 280" else "0 0 360 280"
        val xml = wrap(body, viewBox)
        return MathSvgDocument(
            xml = xml,
            widthPx = WIDTH,
            heightPx = HEIGHT,
            cacheKey = "figure-svg-${sha256(spec.toJson())}",
            editBoxes = emptyList()
        )
    }

    fun isGeometry(spec: FigureSpec): Boolean =
        spec.type !in setOf("line", "quad", "sine", "exp", "bar", "col") &&
            !ChartSvgRenderer.supports(spec.type)

    /** پلاک موقت انواع مرجع (a/s) تا رندر Native کامل V53.3. */
    private fun renderKindPlate(spec: FigureSpec): MathSvgDocument {
        val label = when (spec.kind) {
            "a" -> "آناتومی"
            else -> "فیزیک/شیمی"
        }
        val title = spec.xStr("title").ifBlank { label }
        val body = "<rect x=\"8\" y=\"8\" width=\"304\" height=\"104\" rx=\"12\" fill=\"rgba(108,99,245,.07)\" stroke=\"#6c63f5\" stroke-width=\"1.6\"/>" +
            txt(160f, 52f, title, "#263142", "middle", size = 15) +
            txt(160f, 84f, label, "#5b52e0", "middle", size = 12)
        val xml = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 320 120\" width=\"320\" height=\"120\" overflow=\"hidden\">$body</svg>"
        return MathSvgDocument(
            xml = xml,
            widthPx = 320f,
            heightPx = 120f,
            cacheKey = "figure-plate-${sha256(spec.toJson())}",
            editBoxes = emptyList()
        )
    }

    // ------------------------------------------------------------------ core

    private fun renderBody(spec: FigureSpec): String = when (spec.type) {
        "line", "quad", "sine", "exp", "bar", "col" -> renderGraph(spec)
        // V54.1 — ۲۰ نوع نمودار جدید Native با کلیدهای X مرجع.
        in ChartSvgRenderer.SUPPORTED -> ChartSvgRenderer.body(spec)
        else -> renderGeometry(spec)
    }

    private fun wrap(inner: String, viewBox: String): String =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"$viewBox\" width=\"${fmt(WIDTH)}\" height=\"${fmt(HEIGHT)}\" overflow=\"hidden\">$inner</svg>"

    // ------------------------------------------------------------ helpers

    private data class Pt(val x: Float, val y: Float)

    private fun poly(pts: List<Pt>): String = pts.joinToString(" ") { "${fmt(it.x)},${fmt(it.y)}" }

    private fun stroke(pts: List<Pt>): String =
        "<polygon points=\"${poly(pts)}\" fill=\"rgba(108,99,245,.06)\" stroke=\"$STROKE\" stroke-width=\"2.2\" stroke-linejoin=\"round\"/>"

    private fun dots(pts: List<Pt>): String = pts.joinToString("") { p ->
        "<circle cx=\"${fmt(p.x)}\" cy=\"${fmt(p.y)}\" r=\"3.2\" fill=\"$STROKE\"/>"
    }

    private fun centroid(pts: List<Pt>): Pt {
        var x = 0f
        var y = 0f
        pts.forEach { x += it.x; y += it.y }
        return Pt(x / pts.size, y / pts.size)
    }

    private fun mid(p: Pt, q: Pt): Pt = Pt((p.x + q.x) / 2f, (p.y + q.y) / 2f)

    private fun unit(from: Pt, to: Pt): Pt {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val l = hypot(dx, dy).takeIf { it != 0f } ?: 1f
        return Pt(dx / l, dy / l)
    }

    private fun off(p: Pt, q: Pt, k: Float): Pt {
        val dx = q.x - p.x
        val dy = q.y - p.y
        val l = hypot(dx, dy).takeIf { it != 0f } ?: 1f
        return Pt(-dy / l * k, dx / l * k)
    }

    private fun inbox(x: Float, y: Float, pad: Float = 16f): Pt =
        Pt(max(pad, min(344f, x)), max(pad, min(266f, y)))

    private fun sideLabOut(p: Pt, q: Pt, s: String, c: Pt, dist: Float): String {
        if (s.isBlank()) return ""
        val m = mid(p, q)
        var nrm = off(p, q, 1f)
        if (nrm.x * (c.x - m.x) + nrm.y * (c.y - m.y) > 0f) nrm = Pt(-nrm.x, -nrm.y)
        val pt = inbox(m.x + nrm.x * dist, m.y + nrm.y * dist + 4f, 14f)
        return txt(pt.x, pt.y, s, SIDE_COLOR, "middle", serif = true)
    }

    private fun vertLabOut(p: Pt, s: String, c: Pt, dist: Float): String {
        if (s.isBlank()) return ""
        val v = Pt(p.x - c.x, p.y - c.y)
        val l = hypot(v.x, v.y).takeIf { it != 0f } ?: 1f
        val pt = inbox(p.x + v.x / l * dist, p.y + v.y / l * dist + 4f, 14f)
        return txt(pt.x, pt.y, s, VERTEX_COLOR, "middle")
    }

    private fun degLabel(s: String): String {
        if (s.isBlank()) return ""
        return if (s.contains('°')) s else s + "°"
    }

    private fun isRight(label: String): Boolean = label.toFloatOrNull() == 90f

    private fun polar(o: Pt, deg: Float, r: Float): Pt {
        val rad = deg * PI.toFloat() / 180f
        return Pt(o.x + r * cos(rad), o.y - r * sin(rad))
    }

    private fun heading(from: Pt, to: Pt): Float =
        atan2(from.y - to.y, to.x - from.x) * 180f / PI.toFloat()

    private fun normDeg(d: Float): Float = ((d % 360f) + 360f) % 360f

    private fun rightMark(c: Pt, u: Pt, w: Pt, size: Float): String {
        val x = Pt(c.x + u.x * size, c.y + u.y * size)
        val y = Pt(c.x + w.x * size, c.y + w.y * size)
        val xy = Pt(x.x + w.x * size, x.y + w.y * size)
        return "<polyline points=\"${poly(listOf(x, xy, y))}\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.6\"/>"
    }

    private fun rightOnly(v: Pt, p: Pt, q: Pt, label: String): String {
        val u = unit(v, p)
        val w = unit(v, q)
        val mark = rightMark(v, u, w, 16f)
        val bx = v.x + (u.x + w.x) * 22f
        val by = v.y + (u.y + w.y) * 22f + 4f
        return mark + (if (label.isBlank()) "" else txt(bx, by, degLabel(label), ANGLE_COLOR, "middle"))
    }

    private fun arcByDeg(o: Pt, startDeg: Float, spanDegRaw: Float, r: Float, label: String): String {
        val span = spanDegRaw.coerceIn(0f, 360f)
        if (abs(span - 90f) < 0.51f) {
            val p = polar(o, startDeg, 80f)
            val q = polar(o, startDeg + 90f, 80f)
            return rightOnly(o, p, q, label.ifBlank { "90" })
        }
        if (span <= 0.01f) {
            val p0 = polar(o, startDeg, r)
            return "<circle cx=\"${fmt(p0.x)}\" cy=\"${fmt(p0.y)}\" r=\"2\" fill=\"$ARC\"/>" +
                (if (label.isBlank()) "" else txt(p0.x, p0.y - 10f, degLabel(label), ANGLE_COLOR, "middle"))
        }
        val s = polar(o, startDeg, r)
        val e = polar(o, startDeg + span, r)
        val large = if (span > 180f) 1 else 0
        val midH = startDeg + span / 2f
        val lp = polar(o, midH, r + 15f)
        val d = "M${fmt(s.x)},${fmt(s.y)} A${fmt(r)},${fmt(r)} 0 $large 0 ${fmt(e.x)},${fmt(e.y)}"
        return "<path d=\"$d\" fill=\"none\" stroke=\"$ARC\" stroke-width=\"1.8\"/>" +
            (if (label.isBlank()) "" else txt(lp.x, lp.y + 4f, degLabel(label), ANGLE_COLOR, "middle"))
    }

    private fun angleArc(v: Pt, p: Pt, q: Pt, r: Float, label: String, forceRight: Boolean = false): String {
        if (forceRight || isRight(label)) return rightOnly(v, p, q, label.ifBlank { "90" })
        if (label.isBlank()) return ""
        var hp = heading(v, p)
        var hq = heading(v, q)
        var span = normDeg(hq - hp)
        if (span > 180f) {
            val tmp = hp; hp = hq; hq = tmp; span = normDeg(hq - hp)
        }
        return arcByDeg(v, hp, span, r, label)
    }

    // --------------------------------------------------------- geometry

    private fun renderGeometry(spec: FigureSpec): String {
        val t = spec.type
        val vA = spec.vertex("A").ifBlank { "A" }
        val vB = spec.vertex("B").ifBlank { "B" }
        val vC = spec.vertex("C").ifBlank { "C" }
        val vD = spec.vertex("D").ifBlank { "D" }
        val sA = spec.side("a")
        val sB = spec.side("b")
        val sC = spec.side("c")
        val aA = spec.angle("A")
        val aB = spec.angle("B")
        val aC = spec.angle("C")

        return when (t) {
            "tri", "iso", "eq", "scal", "acut", "obt" -> {
                val pts = when (t) {
                    "eq" -> listOf(Pt(180f, 48f), Pt(64f, 208f), Pt(296f, 208f))
                    "iso" -> listOf(Pt(180f, 42f), Pt(78f, 208f), Pt(282f, 208f))
                    "scal" -> listOf(Pt(214f, 36f), Pt(46f, 222f), Pt(322f, 164f))
                    "acut" -> listOf(Pt(180f, 36f), Pt(86f, 214f), Pt(270f, 204f))
                    "obt" -> listOf(Pt(52f, 72f), Pt(88f, 222f), Pt(318f, 188f))
                    else -> listOf(Pt(188f, 44f), Pt(58f, 212f), Pt(308f, 196f))
                }
                val c = centroid(pts)
                stroke(pts) + dots(pts) +
                    vertLabOut(pts[0], vA, c, 22f) +
                    vertLabOut(pts[1], vB, c, 22f) +
                    vertLabOut(pts[2], vC, c, 22f) +
                    sideLabOut(pts[1], pts[2], sA, c, 20f) +
                    sideLabOut(pts[0], pts[2], sB, c, 20f) +
                    sideLabOut(pts[0], pts[1], sC, c, 20f) +
                    angleArc(pts[0], pts[1], pts[2], 24f, aA) +
                    angleArc(pts[1], pts[0], pts[2], 24f, aB) +
                    angleArc(pts[2], pts[0], pts[1], 24f, aC)
            }
            "rtri" -> {
                val a = Pt(70f, 50f); val b = Pt(300f, 214f); val cc = Pt(70f, 214f)
                val c = centroid(listOf(a, b, cc))
                stroke(listOf(a, b, cc)) + dots(listOf(a, b, cc)) +
                    rightOnly(cc, b, a, "90") +
                    vertLabOut(a, vA, c, 20f) +
                    vertLabOut(b, vB, c, 20f) +
                    vertLabOut(cc, vC, c, 20f) +
                    sideLabOut(b, cc, sA, c, 20f) +
                    sideLabOut(a, cc, sB, c, 22f) +
                    sideLabOut(a, b, sC, c, 20f) +
                    (if (isRight(aA)) rightOnly(a, cc, b, "90") else angleArc(a, cc, b, 24f, aA)) +
                    (if (isRight(aB)) rightOnly(b, cc, a, "90") else angleArc(b, cc, a, 24f, aB))
            }
            "sq", "rect" -> {
                val w = if (t == "sq") 148f else 196f
                val h = if (t == "sq") 148f else 116f
                val qa = Pt(180f - w / 2f, 140f - h / 2f)
                val qb = Pt(180f + w / 2f, 140f - h / 2f)
                val qc = Pt(180f + w / 2f, 140f + h / 2f)
                val qd = Pt(180f - w / 2f, 140f + h / 2f)
                val c = centroid(listOf(qa, qb, qc, qd))
                val top = sA.ifBlank { if (t == "sq") spec.side("s") else "" }
                val side = sB.ifBlank { if (t == "sq") spec.side("s") else "" }
                stroke(listOf(qa, qb, qc, qd)) + dots(listOf(qa, qb, qc, qd)) +
                    vertLabOut(qa, vA, c, 20f) + vertLabOut(qb, vB, c, 20f) +
                    vertLabOut(qc, vC, c, 20f) + vertLabOut(qd, vD, c, 20f) +
                    sideLabOut(qa, qb, top, c, 20f) + sideLabOut(qb, qc, side, c, 22f) +
                    sideLabOut(qc, qd, if (t == "sq") top else sA, c, 20f) +
                    sideLabOut(qd, qa, if (t == "sq") side else sB, c, 22f) +
                    rightOnly(qa, qd, qb, "90") + rightOnly(qb, qa, qc, "90") +
                    rightOnly(qc, qb, qd, "90") + rightOnly(qd, qc, qa, "90")
            }
            "para", "rhomb" -> {
                var pa = Pt(78f, 72f); var pb = Pt(248f, 72f); var pc = Pt(292f, 208f); var pd = Pt(122f, 208f)
                if (t == "rhomb") { pa = Pt(118f, 68f); pb = Pt(268f, 68f); pc = Pt(232f, 212f); pd = Pt(82f, 212f) }
                val c = centroid(listOf(pa, pb, pc, pd))
                stroke(listOf(pa, pb, pc, pd)) + dots(listOf(pa, pb, pc, pd)) +
                    vertLabOut(pa, vA, c, 20f) + vertLabOut(pb, vB, c, 20f) +
                    vertLabOut(pc, vC, c, 20f) + vertLabOut(pd, vD, c, 20f) +
                    sideLabOut(pa, pb, sA, c, 20f) + sideLabOut(pb, pc, sB, c, 20f) +
                    sideLabOut(pc, pd, sA, c, 20f) + sideLabOut(pd, pa, sB, c, 20f) +
                    angleArc(pa, pd, pb, 22f, aA)
            }
            "trap", "itrap", "rtrap" -> {
                val pts = when (t) {
                    "itrap" -> listOf(Pt(112f, 78f), Pt(248f, 78f), Pt(292f, 210f), Pt(68f, 210f))
                    "rtrap" -> listOf(Pt(88f, 78f), Pt(236f, 78f), Pt(236f, 210f), Pt(64f, 210f))
                    else -> listOf(Pt(112f, 78f), Pt(244f, 78f), Pt(292f, 210f), Pt(64f, 210f))
                }
                val c = centroid(pts)
                val extra = if (t == "rtrap") {
                    rightOnly(pts[1], pts[0], pts[2], "90") + rightOnly(pts[2], pts[1], pts[3], "90")
                } else ""
                val hgt = spec.xStr("h")
                stroke(pts) + dots(pts) + extra +
                    vertLabOut(pts[0], vA, c, 22f) + vertLabOut(pts[1], vB, c, 22f) +
                    vertLabOut(pts[2], vC, c, 22f) + vertLabOut(pts[3], vD, c, 22f) +
                    sideLabOut(pts[0], pts[1], sA, c, 24f) + sideLabOut(pts[3], pts[2], sB, c, 24f) +
                    sideLabOut(pts[0], pts[3], sC, c, 24f) + sideLabOut(pts[1], pts[2], spec.side("d"), c, 24f) +
                    (if (hgt.isBlank()) "" else txt(318f, 148f, "h=" + hgt, SIDE_COLOR))
            }
            "circ" -> {
                val r = spec.xStr("r")
                "<circle cx=\"180\" cy=\"140\" r=\"78\" fill=\"rgba(39,196,168,.06)\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<line x1=\"180\" y1=\"140\" x2=\"258\" y2=\"140\" stroke=\"$TEAL\" stroke-width=\"1.8\"/>" +
                    "<circle cx=\"180\" cy=\"140\" r=\"3.2\" fill=\"$STROKE\"/>" +
                    txt(166f, 132f, spec.vertex("O").ifBlank { "O" }, VERTEX_COLOR, "middle") +
                    txt(248f, 118f, r.ifBlank { "r" }.let { if (it == "r") it else "r=$it" }, SIDE_COLOR)
            }
            "ang" -> {
                val measure = (spec.xNum("m", spec.angle("O").toFloatOrNull() ?: 50f)).coerceIn(0f, 360f)
                val o = Pt(180f, 158f)
                val rayLen = 96f
                val bpt = polar(o, 0f, rayLen)
                val apt = polar(o, measure, rayLen)
                val alab = inbox(polar(o, measure, rayLen + 20f).x, polar(o, measure, rayLen + 20f).y, 16f)
                val blab = inbox(polar(o, 0f, rayLen + 20f).x, polar(o, 0f, rayLen + 20f).y + 6f, 16f)
                val olab = inbox(o.x - 16f, o.y + 20f, 16f)
                var html = "<line x1=\"${fmt(o.x)}\" y1=\"${fmt(o.y)}\" x2=\"${fmt(bpt.x)}\" y2=\"${fmt(bpt.y)}\" stroke=\"$STROKE\" stroke-width=\"2.3\" stroke-linecap=\"round\"/>" +
                    "<line x1=\"${fmt(o.x)}\" y1=\"${fmt(o.y)}\" x2=\"${fmt(apt.x)}\" y2=\"${fmt(apt.y)}\" stroke=\"$STROKE\" stroke-width=\"2.3\" stroke-linecap=\"round\"/>"
                html += when {
                    measure >= 359.5f ->
                        "<circle cx=\"${fmt(o.x)}\" cy=\"${fmt(o.y)}\" r=\"34\" fill=\"none\" stroke=\"$ARC\" stroke-width=\"1.8\"/>" +
                            txt(o.x, o.y - 46f, degLabel(fmtInt(measure)), ANGLE_COLOR, "middle")
                    isRight(fmtInt(measure)) -> rightOnly(o, bpt, apt, "90")
                    else -> arcByDeg(o, 0f, measure, 32f, fmtInt(measure))
                }
                html += dots(listOf(o))
                html += txt(olab.x, olab.y, spec.vertex("O").ifBlank { "O" }, VERTEX_COLOR, "middle")
                html += txt(alab.x, alab.y, vA, VERTEX_COLOR, "middle")
                html += txt(blab.x, blab.y, vB, VERTEX_COLOR, "middle")
                html
            }
            "parll" -> {
                val n = round(spec.xNum("n", 1f)).toInt().coerceIn(1, 6)
                val y1 = 86f; val y2 = 196f
                var html = "<line x1=\"24\" y1=\"$y1\" x2=\"336\" y2=\"$y1\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<line x1=\"24\" y1=\"$y2\" x2=\"336\" y2=\"$y2\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    txt(346f, y1 + 5f, spec.vertex("d1").ifBlank { "d₁" }, VERTEX_COLOR) +
                    txt(346f, y2 + 5f, spec.vertex("d2").ifBlank { "d₂" }, VERTEX_COLOR)
                val gap = if (n == 1) 0f else 190f / (n - 1)
                val x0 = if (n == 1) 180f else 80f
                for (i in 0 until n) {
                    val tilt = spec.xNum("t$i", spec.xNum("tilt", 60f)).coerceIn(5f, 175f)
                    val sinT = sin(tilt * PI.toFloat() / 180f).let { if (abs(it) < 0.02f) 0.02f else it }
                    val cosT = cos(tilt * PI.toFloat() / 180f)
                    val pad = 22f
                    val dy = (y2 + pad) - (y1 - pad)
                    val tLen = dy / sinT
                    val xc = x0 + i * gap
                    val xt = xc - tLen * cosT / 2f
                    val xb = xc + tLen * cosT / 2f
                    html += "<line x1=\"${fmt(xt)}\" y1=\"${fmt(y1 - pad)}\" x2=\"${fmt(xb)}\" y2=\"${fmt(y2 + pad)}\" stroke=\"$ARC\" stroke-width=\"2\"/>"
                    val hit = Pt(xt + pad * cosT / sinT, y1)
                    if (isRight(tilt.toString())) {
                        html += rightOnly(hit, Pt(hit.x + 40f, hit.y), Pt(xb, y2 + pad), "90")
                    } else {
                        html += arcByDeg(hit, -tilt, tilt, 24f, tilt.toString())
                    }
                }
                html
            }
            "cube", "box" -> {
                fun iso(x: Float, y: Float, z: Float): Pt = Pt(180f + (x - z) * 0.86f, 168f - y * 0.92f + (x + z) * 0.32f)
                val l = if (t == "cube") 88f else 108f
                val w = if (t == "cube") 88f else 70f
                val h = if (t == "cube") 88f else 78f
                val a = iso(0f, 0f, 0f); val b = iso(l, 0f, 0f); val c = iso(l, 0f, w); val d = iso(0f, 0f, w)
                val e = iso(0f, h, 0f); val f = iso(l, h, 0f); val g = iso(l, h, w); val hh = iso(0f, h, w)
                val hid = "<polyline points=\"${poly(listOf(d, c, g))}\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>" +
                    "<line x1=\"${fmt(d.x)}\" y1=\"${fmt(d.y)}\" x2=\"${fmt(hh.x)}\" y2=\"${fmt(hh.y)}\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>"
                val vis = stroke(listOf(e, f, b, a)) +
                    stroke(listOf(f, g, c, b)).replace("rgba(108,99,245,.06)", "rgba(108,99,245,.10)") +
                    stroke(listOf(e, f, g, hh)).replace("rgba(108,99,245,.06)", "rgba(39,196,168,.08)") +
                    "<polyline points=\"${poly(listOf(a, b, f, e, a))}\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    "<polyline points=\"${poly(listOf(b, c))}\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    "<polyline points=\"${poly(listOf(f, g, hh, e))}\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>"
                val midAB = mid(a, b); val midAE = mid(a, e); val midAD = mid(a, d)
                hid + vis +
                    txt(midAB.x, midAB.y + 16f, if (t == "cube") spec.side("s").ifBlank { "a" } else sA.ifBlank { "a" }, SIDE_COLOR, "middle", serif = true) +
                    txt(midAE.x - 14f, midAE.y, if (t == "cube") spec.side("s") else spec.side("h").ifBlank { "h" }, SIDE_COLOR, "middle", serif = true) +
                    txt(midAD.x + 16f, midAD.y + 4f, if (t == "cube") "" else sB.ifBlank { "b" }, SIDE_COLOR, "middle", serif = true)
            }
            "cyl" -> {
                val cx = 180f; val cy = 148f; val rx = 64f; val ry = 22f; val h = 96f
                val top = cy - h / 2f; val bot = cy + h / 2f
                "<ellipse cx=\"$cx\" cy=\"$bot\" rx=\"$rx\" ry=\"$ry\" fill=\"rgba(108,99,245,.05)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<path d=\"M${fmt(cx - rx)},$top L${fmt(cx - rx)},$bot M${fmt(cx + rx)},$top L${fmt(cx + rx)},$bot\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<ellipse cx=\"$cx\" cy=\"$top\" rx=\"$rx\" ry=\"$ry\" fill=\"rgba(39,196,168,.10)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<path d=\"M${fmt(cx - rx)},$bot A$rx,$ry 0 0 0 ${fmt(cx + rx)},$bot\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>" +
                    txt(cx + rx + 18f, (top + bot) / 2f, spec.xStr("h").ifBlank { spec.side("h") }.ifBlank { "h" }, SIDE_COLOR, serif = true) +
                    txt(cx, bot + 28f, spec.xStr("r").ifBlank { "r" }.let { if (it == "r") it else "r=$it" }, SIDE_COLOR, "middle", serif = true)
            }
            "cone" -> {
                val cx = 180f; val baseY = 210f; val tip = Pt(180f, 48f); val rx = 78f; val ry = 24f
                "<ellipse cx=\"$cx\" cy=\"$baseY\" rx=\"$rx\" ry=\"$ry\" fill=\"rgba(108,99,245,.06)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<path d=\"M${fmt(cx - rx)},$baseY A$rx,$ry 0 0 0 ${fmt(cx + rx)},$baseY\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"${fmt(cx - rx)}\" y2=\"$baseY\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"${fmt(cx + rx)}\" y2=\"$baseY\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"$cx\" y2=\"$baseY\" stroke=\"$TEAL\" stroke-width=\"1.5\" stroke-dasharray=\"4 3\"/>" +
                    txt(cx + rx + 16f, (tip.y + baseY) / 2f, spec.xStr("h").ifBlank { "h" }, SIDE_COLOR, serif = true) +
                    txt(cx, baseY + 30f, spec.xStr("r").ifBlank { "r" }.let { if (it == "r") it else "r=$it" }, SIDE_COLOR, "middle", serif = true) +
                    txt(tip.x, tip.y - 10f, spec.vertex("S").ifBlank { "S" }, VERTEX_COLOR, "middle")
            }
            "sph" -> {
                "<ellipse cx=\"180\" cy=\"140\" rx=\"86\" ry=\"86\" fill=\"rgba(39,196,168,.07)\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<ellipse cx=\"180\" cy=\"140\" rx=\"86\" ry=\"28\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>" +
                    "<ellipse cx=\"180\" cy=\"140\" rx=\"86\" ry=\"28\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"1.3\" stroke-dasharray=\"5 4\" transform=\"rotate(90 180 140)\"/>" +
                    "<line x1=\"180\" y1=\"140\" x2=\"266\" y2=\"140\" stroke=\"$TEAL\" stroke-width=\"1.7\"/>" +
                    "<circle cx=\"180\" cy=\"140\" r=\"3\" fill=\"$STROKE\"/>" +
                    txt(168f, 132f, spec.vertex("O").ifBlank { "O" }, VERTEX_COLOR) +
                    txt(228f, 128f, spec.xStr("r").ifBlank { "r" }.let { if (it == "r") it else "r=$it" }, SIDE_COLOR, serif = true)
            }
            "pyr" -> {
                val tip = Pt(180f, 42f); val a = Pt(78f, 214f); val b = Pt(262f, 200f); val c = Pt(292f, 146f); val d = Pt(118f, 158f)
                "<polygon points=\"${poly(listOf(a, b, c, d))}\" fill=\"rgba(108,99,245,.06)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<line x1=\"${fmt(d.x)}\" y1=\"${fmt(d.y)}\" x2=\"${fmt(c.x)}\" y2=\"${fmt(c.y)}\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"${fmt(d.x)}\" y2=\"${fmt(d.y)}\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"${fmt(a.x)}\" y2=\"${fmt(a.y)}\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"${fmt(b.x)}\" y2=\"${fmt(b.y)}\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    "<line x1=\"${fmt(tip.x)}\" y1=\"${fmt(tip.y)}\" x2=\"${fmt(c.x)}\" y2=\"${fmt(c.y)}\" stroke=\"$STROKE\" stroke-width=\"2.1\"/>" +
                    txt(tip.x, tip.y - 10f, spec.vertex("S").ifBlank { "S" }, VERTEX_COLOR, "middle") +
                    txt(a.x - 10f, a.y + 16f, vA, VERTEX_COLOR) +
                    txt(b.x + 10f, b.y + 16f, vB, VERTEX_COLOR) +
                    txt(180f, 228f, sA.ifBlank { "a" }, SIDE_COLOR, "middle", serif = true) +
                    txt(310f, 120f, spec.xStr("h").ifBlank { "h" }, SIDE_COLOR, serif = true)
            }
            "pris" -> {
                val a = Pt(90f, 206f); val b = Pt(230f, 214f); val c = Pt(156f, 150f)
                val a2 = Pt(128f, 92f); val b2 = Pt(268f, 100f); val c2 = Pt(194f, 36f)
                "<polygon points=\"${poly(listOf(a, b, c))}\" fill=\"rgba(108,99,245,.05)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<polygon points=\"${poly(listOf(a2, b2, c2))}\" fill=\"rgba(39,196,168,.08)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<line x1=\"${fmt(a.x)}\" y1=\"${fmt(a.y)}\" x2=\"${fmt(a2.x)}\" y2=\"${fmt(a2.y)}\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<line x1=\"${fmt(b.x)}\" y1=\"${fmt(b.y)}\" x2=\"${fmt(b2.x)}\" y2=\"${fmt(b2.y)}\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    "<line x1=\"${fmt(c.x)}\" y1=\"${fmt(c.y)}\" x2=\"${fmt(c2.x)}\" y2=\"${fmt(c2.y)}\" stroke=\"$STROKE\" stroke-width=\"1.4\" stroke-dasharray=\"5 4\"/>" +
                    txt(a.x - 10f, a.y + 16f, vA, VERTEX_COLOR) +
                    txt(b.x + 10f, b.y + 16f, vB, VERTEX_COLOR) +
                    txt(c.x - 16f, c.y, vC, VERTEX_COLOR) +
                    txt(a2.x - 8f, a2.y - 6f, spec.vertex("A2").ifBlank { "A'" }, VERTEX_COLOR) +
                    txt(300f, 150f, spec.xStr("h").ifBlank { "h" }, SIDE_COLOR, serif = true)
            }
            "hex", "pent", "oct" -> {
                val sides = when (t) { "hex" -> 6; "oct" -> 8; else -> 5 }
                val pts = (0 until sides).map { i ->
                    val deg = -90f + i * (360f / sides)
                    polar(Pt(180f, 140f), deg, 88f)
                }
                val c = centroid(pts)
                val labels = listOf("A", "B", "C", "D", "E", "F", "G", "H")
                var html = stroke(pts) + dots(pts)
                for (i in 0 until sides) {
                    html += vertLabOut(pts[i], spec.vertex(labels[i]).ifBlank { labels[i] }, c, 18f)
                    if (i == 0) html += sideLabOut(pts[i], pts[(i + 1) % sides], sA.ifBlank { "a" }, c, 16f)
                }
                html
            }
            "kite" -> {
                val k = listOf(Pt(180f, 36f), Pt(268f, 120f), Pt(180f, 236f), Pt(92f, 120f))
                val c = centroid(k)
                stroke(k) + dots(k) +
                    "<line x1=\"180\" y1=\"36\" x2=\"180\" y2=\"236\" stroke=\"$TEAL\" stroke-width=\"1.3\" stroke-dasharray=\"4 3\"/>" +
                    "<line x1=\"92\" y1=\"120\" x2=\"268\" y2=\"120\" stroke=\"$TEAL\" stroke-width=\"1.3\" stroke-dasharray=\"4 3\"/>" +
                    vertLabOut(k[0], vA, c, 18f) + vertLabOut(k[1], vB, c, 18f) +
                    vertLabOut(k[2], vC, c, 18f) + vertLabOut(k[3], vD, c, 18f)
            }
            "star" -> {
                val pts = (0 until 10).map { i ->
                    val r = if (i % 2 == 0) 92f else 38f
                    polar(Pt(180f, 140f), -90f + i * 36f, r)
                }
                stroke(pts) + dots(pts)
            }
            "ell" -> {
                "<ellipse cx=\"180\" cy=\"140\" rx=\"95\" ry=\"60\" fill=\"rgba(39,196,168,.07)\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<line x1=\"180\" y1=\"140\" x2=\"275\" y2=\"140\" stroke=\"$TEAL\" stroke-width=\"1.7\"/>" +
                    "<circle cx=\"180\" cy=\"140\" r=\"3.2\" fill=\"$STROKE\"/>" +
                    txt(168f, 132f, spec.vertex("O").ifBlank { "O" }, VERTEX_COLOR, "middle") +
                    txt(232f, 128f, spec.xStr("a").ifBlank { "a" }, SIDE_COLOR, serif = true)
            }
            "ring" -> {
                "<circle cx=\"180\" cy=\"140\" r=\"72\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<circle cx=\"180\" cy=\"140\" r=\"40\" fill=\"none\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<circle cx=\"180\" cy=\"140\" r=\"3.2\" fill=\"$STROKE\"/>" +
                    txt(166f, 132f, spec.vertex("O").ifBlank { "O" }, VERTEX_COLOR, "middle") +
                    txt(206f, 150f, spec.xStr("R").ifBlank { "R" }, SIDE_COLOR, serif = true) +
                    txt(206f, 172f, spec.xStr("r").ifBlank { "r" }, SIDE_COLOR, serif = true)
            }
            "semi" -> {
                val r = spec.xStr("r")
                "<path d=\"M90,140 A90,90 0 0 1 270,140 Z\" fill=\"rgba(39,196,168,.06)\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<line x1=\"90\" y1=\"140\" x2=\"270\" y2=\"140\" stroke=\"$STROKE\" stroke-width=\"2.2\"/>" +
                    "<line x1=\"180\" y1=\"140\" x2=\"180\" y2=\"50\" stroke=\"$TEAL\" stroke-width=\"1.7\"/>" +
                    "<circle cx=\"180\" cy=\"140\" r=\"3.2\" fill=\"$STROKE\"/>" +
                    txt(168f, 132f, spec.vertex("O").ifBlank { "O" }, VERTEX_COLOR, "middle") +
                    txt(196f, 96f, r.ifBlank { "r" }.let { if (it == "r") it else "r=$it" }, SIDE_COLOR, serif = true)
            }
            "pseg", "ray", "ln" -> {
                val arrow = if (t == "ray" || t == "ln") "<polygon points=\"338,140 326,133 326,147\" fill=\"$STROKE\"/>" else ""
                val arrowStart = if (t == "ln") "<polygon points=\"22,140 34,133 34,147\" fill=\"$STROKE\"/>" else ""
                val dotStart = if (t == "pseg" || t == "ray") "<circle cx=\"30\" cy=\"140\" r=\"3.4\" fill=\"$STROKE\"/>" else ""
                "<line x1=\"30\" y1=\"140\" x2=\"332\" y2=\"140\" stroke=\"$STROKE\" stroke-width=\"2.3\" stroke-linecap=\"round\"/>" +
                    arrow + arrowStart + dotStart +
                    txt(18f, 168f, spec.vertex("A").ifBlank { "A" }, VERTEX_COLOR, "middle") +
                    txt(344f, 168f, spec.vertex("B").ifBlank { "B" }, VERTEX_COLOR, "middle")
            }
            else -> {
                // fallback امن برای شناسهٔ ناشناخته: یک کادر با نام شناسه.
                "<rect x=\"40\" y=\"80\" width=\"280\" height=\"120\" rx=\"14\" fill=\"rgba(108,99,245,.06)\" stroke=\"$STROKE\" stroke-width=\"2\"/>" +
                    txt(180f, 148f, spec.type, VERTEX_COLOR, "middle")
            }
        }
    }

    // ----------------------------------------------------------- graph

    private class Axes(
        val xmin: Float, val xmax: Float, val ymin: Float, val ymax: Float
    ) {
        private val l = 42f; private val t = 22f; private val r = 338f; private val b = 248f
        fun xof(x: Float): Float = l + (x - xmin) / (xmax - xmin) * (r - l)
        fun yof(y: Float): Float = b - (y - ymin) / (ymax - ymin) * (b - t)
        fun html(): String {
            val ox = xof(0f).let { if (it < l || it > r) l else it }
            val oy = yof(0f).let { if (it < t || it > b) b else it }
            var h = "<rect x=\"$l\" y=\"$t\" width=\"${fmt(r - l)}\" height=\"${fmt(b - t)}\" fill=\"#fbfcfe\" stroke=\"#d5dce6\"/>"
            var gx = ceil(xmin)
            while (gx <= xmax) {
                val xx = xof(gx)
                h += "<line x1=\"${fmt(xx)}\" y1=\"$t\" x2=\"${fmt(xx)}\" y2=\"$b\" stroke=\"#eef1f6\"/>"
                if (gx != 0f) h += txt(xx, min(b + 14f, 272f), fmtInt(gx), "#4a5870", "middle", size = 11)
                gx += 1f
            }
            var gy = ceil(ymin)
            while (gy <= ymax) {
                val yy = yof(gy)
                h += "<line x1=\"$l\" y1=\"${fmt(yy)}\" x2=\"$r\" y2=\"${fmt(yy)}\" stroke=\"#eef1f6\"/>"
                if (gy != 0f) h += txt(l - 6f, yy + 3f, fmtInt(gy), "#4a5870", "end", size = 11)
                gy += 1f
            }
            h += "<line x1=\"$l\" y1=\"${fmt(oy)}\" x2=\"$r\" y2=\"${fmt(oy)}\" stroke=\"$STROKE\" stroke-width=\"1.6\"/>" +
                "<line x1=\"${fmt(ox)}\" y1=\"$t\" x2=\"${fmt(ox)}\" y2=\"$b\" stroke=\"$STROKE\" stroke-width=\"1.6\"/>" +
                "<polygon points=\"$r,${fmt(oy)} ${fmt(r - 7f)},${fmt(oy - 4f)} ${fmt(r - 7f)},${fmt(oy + 4f)}\" fill=\"$STROKE\"/>" +
                "<polygon points=\"${fmt(ox)},$t ${fmt(ox - 4f)},${fmt(t + 7f)} ${fmt(ox + 4f)},${fmt(t + 7f)}\" fill=\"$STROKE\"/>" +
                txt(r - 4f, oy - 8f, "x", "#4a5870", size = 11) +
                txt(ox + 8f, t + 12f, "y", "#4a5870", size = 11)
            return h
        }
    }

    private fun ceil(v: Float): Float = kotlin.math.ceil(v.toDouble()).toFloat()

    private fun renderGraph(spec: FigureSpec): String {
        val t = spec.type
        val xmin = spec.xNum("xmin", -5f)
        val xmax = spec.xNum("xmax", 5f).let { if (it <= xmin) xmin + 2f else it }
        val ymin = spec.xNum("ymin", -4f)
        val ymax = spec.xNum("ymax", 4f).let { if (it <= ymin) ymin + 2f else it }
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else txt(180f, 14f, title, "#1a2433", "middle", size = 13)

        if (t in setOf("line", "quad", "sine", "exp")) {
            val ax = Axes(xmin, xmax, ymin, ymax)
            val body = when (t) {
                "line" -> plotFn(ax, { x -> spec.xNum("m", 1f) * x + spec.xNum("b", 0f) }, xmin, xmax)
                "quad" -> plotFn(ax, { x -> spec.xNum("a", 1f) * x * x + spec.xNum("b", 0f) * x + spec.xNum("c", 0f) }, xmin, xmax)
                "sine" -> plotFn(ax, { x -> spec.xNum("A", 1f) * sin(spec.xNum("w", 1f) * x + spec.xNum("ph", 0f)) }, xmin, xmax)
                else -> plotFn(ax, { x -> spec.xNum("a", 1f) * exp(spec.xNum("b", 0.5f) * x) }, xmin, xmax)
            }
            return head + ax.html() + body
        }

        // bar / col
        val labels = spec.xList("labs", "A,B,C,D")
        val values = spec.xList("vals", "4,7,3,6").map { it.toFloatOrNull() ?: 0f }
        val padded = values + List((labels.size - values.size).coerceAtLeast(0)) { 0f }
        val maxv = (padded + 1f).maxOrNull() ?: 1f
        val n = labels.size
        val gap = 12f
        val l = 48f; val tp = 28f; val r = 340f; val b = 230f
        val bw = max(12f, (r - l - gap * (n + 1)) / n)
        var h = "<rect x=\"$l\" y=\"$tp\" width=\"${fmt(r - l)}\" height=\"${fmt(b - tp)}\" fill=\"#fbfcfe\" stroke=\"#d5dce6\"/>"
        h += "<line x1=\"$l\" y1=\"$b\" x2=\"$r\" y2=\"$b\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>"
        h += "<line x1=\"$l\" y1=\"$tp\" x2=\"$l\" y2=\"$b\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>"
        for (i in 0 until n) {
            val bh = (max(0f, padded[i]) / maxv) * (b - tp - 12f)
            val x = l + gap + i * (bw + gap)
            val y = b - bh
            h += "<rect x=\"${fmt(x)}\" y=\"${fmt(y)}\" width=\"${fmt(bw)}\" height=\"${fmt(bh)}\" rx=\"5\" fill=\"${barColors[i % barColors.size]}\"/>"
            h += txt(x + bw / 2f, b + 16f, labels.getOrElse(i) { "" }, "#1a2433", "middle", size = 11)
            h += txt(x + bw / 2f, y - 5f, fmtInt(padded[i]), "#1a2433", "middle", size = 11)
        }
        return head + h
    }

    private fun plotFn(ax: Axes, fn: (Float) -> Float, xmin: Float, xmax: Float): String {
        val pts = mutableListOf<String>()
        val n = 80
        for (i in 0..n) {
            val x = xmin + (xmax - xmin) * i / n
            val y = fn(x)
            if (y.isFinite()) pts += "${fmt(ax.xof(x))},${fmt(ax.yof(y))}"
        }
        if (pts.size < 2) return ""
        return "<polyline points=\"${pts.joinToString(" ")}\" fill=\"none\" stroke=\"$ARC\" stroke-width=\"2.2\" stroke-linejoin=\"round\"/>"
    }

    // ------------------------------------------------------------ utils

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
