package ir.exam.app.core.figure

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * V319 — بستهٔ سوم ابزارهای آموزشی تعاملی (۳۰ ابزار)، مکمل [PrimaryMathSvgRenderer] و
 * [AdvancedMathSvgRenderer]. همان بوم ۳۶۰×۲۸۰ و همان قرارداد پارامترهای `X`.
 *
 * پورت یک‌به‌یک موتور وب: `graph_fig.js → axisExSvg()` (بلوک V319).
 *
 *  • دبستان: نردبان تبدیل واحد، محیط و مساحت، نمودار تصویری/میله‌ای، گرد کردن، تقسیم با باقی‌مانده،
 *    دسته‌بندی اشکال، اجزای دایره، استوانه و مخروط، تناسب، دستگاه دو معادله.
 *  • متوسطه و علوم: نمودار میله‌ای آماری، سهمی، زاویهٔ مرکزی/محاطی، آونگ، سطح شیب‌دار، مدار الکتریکی،
 *    عدسی، منحنی گرمایش، DNA، سلول.
 *  • انسانی و دانشگاه: خط زمان، نقشه و جهت‌ها، اجزای جمله، بردارها، ماتریس، انتگرال معین، فلوچارت،
 *    پشته/صف، مرتب‌سازی، رگرسیون.
 */
object NextMathSvgRenderer {
    val SUPPORTED: Set<String> = setOf(
        "axunitconv", "axperimeter", "axpictograph", "axrounding", "axdivision", "axshapeid",
        "axcircleparts", "axcylinder", "axproportion", "axlinear", "axbarchart", "axparabola",
        "axinthales", "axpendulum", "axincline", "axcircuit", "axoptics", "axheatcurve",
        "axdna", "axcell", "axtimeline", "axmapgeo", "axsentence", "axvectors", "axmatrix",
        "axintegral", "axflowchart", "axstack", "axsort", "axregression"
    )

    private const val ST = "#2c3a50"
    private const val MU = "#4a5870"
    private const val ACC = "#6c63f5"
    private const val GRN = "#27c4a8"
    private const val GLD = "#f0a202"
    private const val PT = "#d6336c"
    private const val GR = "#d5dce6"

    fun body(spec: FigureSpec): String {
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else tx(180f, 14f, title, "#1a2433", "middle", true, 13)
        val inner = when (spec.type) {
            "axunitconv" -> unitLadder(spec)
            "axperimeter" -> perimeterArea(spec)
            "axpictograph" -> pictograph(spec)
            "axrounding" -> rounding(spec)
            "axdivision" -> divisionGroups(spec)
            "axshapeid" -> shapeId(spec)
            "axcircleparts" -> circleParts(spec)
            "axcylinder" -> cylinderCone(spec)
            "axproportion" -> proportion(spec)
            "axlinear" -> linearSystem(spec)
            "axbarchart" -> barStats(spec)
            "axparabola" -> parabola(spec)
            "axinthales" -> inscribed(spec)
            "axpendulum" -> pendulum(spec)
            "axincline" -> incline(spec)
            "axcircuit" -> circuit(spec)
            "axoptics" -> optics(spec)
            "axheatcurve" -> heatCurve(spec)
            "axdna" -> dna(spec)
            "axcell" -> cell(spec)
            "axtimeline" -> timeline(spec)
            "axmapgeo" -> mapGeo(spec)
            "axsentence" -> sentence(spec)
            "axvectors" -> vectors(spec)
            "axmatrix" -> matrixMul(spec)
            "axintegral" -> definiteIntegral(spec)
            "axflowchart" -> flowchart(spec)
            "axstack" -> stackQueue(spec)
            "axsort" -> bubbleSort(spec)
            "axregression" -> regression(spec)
            else -> return AdvancedMathSvgRenderer.body(spec)
        }
        return head + inner
    }

    // ---------------------------------------------------------------- دبستان
    private fun unitLadder(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "len").lowercase()
        val v = spec.xNum("val", 1f)
        val names = when (kind) {
            "mass" -> listOf("تن", "کیلوگرم", "گرم")
            "area" -> listOf("متر مربع", "دسی‌متر مربع", "سانتی‌متر مربع")
            else -> listOf("کیلومتر", "متر", "میلی‌متر")
        }
        val fac = if (kind == "area") 100 else 1000
        val sb = StringBuilder()
        for (i in 0..2) {
            val y = 44f + i * 66f
            sb.append(rc(56f, y, 176f, 46f, if (i == 1) "rgba(108,99,245,.18)" else "#ffffff", ST, 1.6f, 10f))
            sb.append(tx(144f, y + 20f, names[i], ST, "middle", true, 13))
            val valText = num((v * Math.pow(fac.toDouble(), i.toDouble()) * 1000).roundToInt() / 1000f)
            sb.append(tx(144f, y + 39f, valText, ACC, "middle", true, 13))
            sb.append(tx(240f, y + 27f, "×$fac", MU, "start", false, 10))
            if (i < 2) {
                sb.append(ln(144f, y + 46f, 144f, y + 62f, PT, 1.6f))
                sb.append(poly("139.5,${f(y + 54f)} 148.5,${f(y + 54f)} 144,${f(y + 64f)}", PT, "none", 0f))
                sb.append(tx(154f, y + 60f, "× $fac", PT, "start", true, 11))
            }
        }
        sb.append(tx(180f, 262f, "هر پله ×$fac — برگشت ÷$fac", MU, "middle", true, 12))
        return sb.toString()
    }

    private fun perimeterArea(spec: FigureSpec): String {
        val shape = spec.xStr("shape", "rect").lowercase()
        val w = max(1f, spec.xNum("w", 6f))
        val h = max(1f, spec.xNum("h", 4f))
        val sb = StringBuilder()
        var perimeter = 0f
        var area = 0f
        val formula: String
        when (shape) {
            "circle" -> {
                val rad = min(84f, 15f * w)
                sb.append(cls(180f, 130f, rad, "rgba(108,99,245,.16)", ACC, 2.2f))
                sb.append(ln(180f, 130f, 180f + rad, 130f, PT, 1.6f)).append(cls(180f, 130f, 3.2f, PT, "none", 0f))
                sb.append(tx(180f + rad / 2f, 122f, "r = " + num(w), PT, "middle", true, 11))
                perimeter = (2 * PI * w).toFloat(); area = (PI * w * w).toFloat()
                formula = "محیط = ۲πr  ·  مساحت = πr²"
            }
            "tri" -> {
                val x0 = 100f; val y0 = 206f; val tw = min(180f, 20f * w); val th = min(120f, 20f * h)
                sb.append(poly("$x0,$y0 ${f(x0 + tw)},$y0 $x0,${f(y0 - th)}", "rgba(39,196,168,.16)", ACC, 2.2f))
                sb.append(rc(x0, y0 - 12f, 12f, 12f, "none", ST, 1.2f))
                sb.append(tx(x0 + tw / 2f, y0 + 16f, num(w), MU, "middle", true, 11))
                sb.append(tx(x0 - 8f, y0 - th / 2f, num(h), MU, "end", true, 11))
                perimeter = w + h + sqrt(w * w + h * h); area = w * h / 2f
                formula = "مساحت مثلث = ½ × قاعده × ارتفاع"
            }
            "poly" -> {
                val n = spec.xNum("n", 5f).toInt().coerceIn(3, 8)
                val pts = StringBuilder()
                for (i in 0 until n) {
                    val a = -PI / 2 + i * 2 * PI / n
                    pts.append(f(180f + 76f * cos(a).toFloat())).append(',').append(f(126f + 76f * sin(a).toFloat())).append(' ')
                }
                sb.append(poly(pts.toString().trim(), "rgba(240,162,2,.16)", GLD, 2.2f))
                val side = (2 * 76f * sin(PI / n) / 14.0).toFloat()
                sb.append(tx(180f, 222f, "$n ضلع", ST, "middle", true, 12))
                perimeter = n * side
                area = (0.5f * n * side * side / (4 * kotlin.math.tan(PI / n))).toFloat()
                formula = "چندضلعی منتظم — محیط = n × ضلع"
            }
            else -> {
                val rw = min(220f, 30f * w); val rh = min(120f, 30f * h)
                val rx0 = 180f - rw / 2f; val ry0 = 126f - rh / 2f
                sb.append(rc(rx0, ry0, rw, rh, "rgba(108,99,245,.16)", ACC, 2.2f))
                sb.append(tx(180f, ry0 + rh + 18f, "عرض = " + num(w), MU, "middle", true, 11))
                sb.append(tx(rx0 - 8f, ry0 + rh / 2f + 4f, "ارتفاع = " + num(h), MU, "end", true, 11))
                perimeter = 2 * (w + h); area = w * h
                formula = "محیط = ۲(طول + عرض)  ·  مساحت = طول × عرض"
            }
        }
        sb.append(tx(180f, 30f, formula, MU, "middle", false, 10))
        sb.append(tx(180f, 244f, "محیط ≈ " + num((perimeter * 100).roundToInt() / 100f), ST, "middle", true, 13))
        sb.append(tx(180f, 262f, "مساحت ≈ " + num((area * 100).roundToInt() / 100f), ACC, "middle", true, 13))
        return sb.toString()
    }

    private fun pictograph(spec: FigureSpec): String {
        val mode = spec.xStr("mode", "picto").lowercase()
        var data = ints(spec, "data", "4,7,3,6")
        if (data.isEmpty()) data = listOf(4, 7, 3, 6)
        var labs = splitList(spec.xStr("labs", "سیب,موز,پرتقال,انگور"))
        if (labs.isEmpty()) labs = listOf("سیب", "موز", "پرتقال", "انگور")
        val maxV = max(1, data.maxOrNull() ?: 1)
        val rows = min(5, data.size)
        val top = 46f
        val gap = min(38f, 168f / rows)
        val sb = StringBuilder()
        sb.append(ln(96f, top, 96f, top + rows * gap + 6f, ST, 1.6f))
        sb.append(ln(96f, top + rows * gap + 6f, 330f, top + rows * gap + 6f, ST, 1.6f))
        for (i in 0 until rows) {
            val y = top + i * gap + gap / 2f
            sb.append(tx(88f, y + 4f, labs.getOrElse(i) { "دستهٔ ${i + 1}" }, MU, "end", true, 10))
            if (mode == "bar") {
                val bw = data[i] / maxV.toFloat() * 214f
                sb.append(rc(100f, y - 9f, max(2f, bw), 18f, "rgba(108,99,245,.55)", ACC, 1.2f, 4f))
                sb.append(tx(104f + bw, y + 4f, data[i].toString(), ST, "start", true, 10))
            } else {
                for (j in 0 until min(10, data[i])) {
                    sb.append(cls(112f + j * 21f, y, 7.5f, "rgba(39,196,168,.85)", "#ffffff", 1f))
                }
                sb.append(tx(112f + min(10, data[i]) * 21f + 4f, y + 4f, data[i].toString(), ST, "start", true, 10))
            }
        }
        sb.append(tx(180f, 254f, if (mode == "bar") "نمودار میله‌ای — مقایسهٔ تعداد" else "نمودار تصویری — هر شکل نمایندهٔ ۱ عدد", MU, "middle", true, 11))
        return sb.toString()
    }

    private fun rounding(spec: FigureSpec): String {
        val n = spec.xNum("n", 147f)
        val ten = spec.xStr("mode", "ten") != "hundred"
        val step = if (ten) 10f else 100f
        val lo = kotlin.math.floor(n / step) * step
        val hi = lo + step
        val l2 = lo - step
        val h2 = hi + step
        fun px(x: Float) = 60f + (x - l2) / (h2 - l2) * 240f
        val y = 150f
        val sb = StringBuilder()
        sb.append(ln(52f, y, 320f, y, ST, 2f))
        for (v in listOf(l2, lo, hi, h2)) {
            sb.append(ln(px(v), y - 7f, px(v), y + 7f, ST, 1.5f))
            sb.append(tx(px(v), y + 22f, num(v), MU, "middle", false, 10))
        }
        sb.append(cls(px(lo), y, 4.5f, "#ffffff", PT, 1.8f)).append(cls(px(hi), y, 4.5f, "#ffffff", PT, 1.8f))
        sb.append(cls(px(n), y - 26f, 5f, PT, "#ffffff", 1.2f))
        sb.append(tx(px(n), y - 36f, num(n), PT, "middle", true, 12))
        val near = if (abs(n - lo) <= abs(hi - n)) lo else hi
        sb.append(path("M ${f(px(n))} ${f(y - 20f)} Q ${f((px(n) + px(near)) / 2f)} ${f(y - 52f)} ${f(px(near))} ${f(y - 8f)}", "none", ACC, 1.8f, "5 4"))
        sb.append(tx(180f, 218f, num(n) + " ≈ " + num(near) + "   (گرد به " + (if (ten) "دهگان" else "صدگان") + ")", ST, "middle", true, 13))
        sb.append(tx(180f, 240f, "اختلاف تا " + num(lo) + " = " + num(abs(n - lo)) + "  ·  تا " + num(hi) + " = " + num(abs(hi - n)), MU, "middle", false, 10))
        sb.append(tx(180f, 262f, "رقم گردکردن: " + num(step), MU, "middle", false, 10))
        return sb.toString()
    }

    private fun divisionGroups(spec: FigureSpec): String {
        val a = spec.xNum("a", 17f).toInt().coerceIn(0, 99)
        val b = spec.xNum("b", 5f).toInt().coerceIn(1, 12)
        val q = a / b
        val r = a - q * b
        val sb = StringBuilder()
        for (i in 0 until min(q, 6)) {
            val cx = 74f + (i % 3) * 78f
            val cy = 74f + (i / 3) * 70f
            sb.append(cls(cx, cy, 30f, "rgba(108,99,245,.10)", ACC, 1.6f))
            for (j in 0 until b) {
                val ang = -PI / 2 + j * 2 * PI / b
                sb.append(cls(cx + 17f * cos(ang).toFloat(), cy + 17f * sin(ang).toFloat(), 4.2f, ACC, "#ffffff", 1f))
            }
        }
        if (q > 6) sb.append(tx(180f, 210f, "و ${q - 6} گروه دیگر…", MU, "middle", false, 10))
        for (k in 0 until r) sb.append(cls(300f + (k % 2) * 14f, 60f + k * 14f, 5f, PT, "#ffffff", 1f))
        if (r > 0) sb.append(tx(320f, 30f, "باقی‌مانده", PT, "middle", true, 10))
        sb.append(tx(180f, 236f, "$a ÷ $b = $q باقی‌ماندهٔ $r", ST, "middle", true, 13))
        sb.append(tx(180f, 258f, "$b × $q + $r = $a", ACC, "middle", true, 12))
        return sb.toString()
    }

    private fun shapeId(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "tri").lowercase()
        val sb = StringBuilder()
        if (kind == "circle") {
            sb.append(cls(180f, 128f, 80f, "rgba(39,196,168,.16)", ACC, 2.2f))
            sb.append(cls(180f, 128f, 3.4f, PT, "none", 0f))
            sb.append(tx(180f, 122f, "مرکز", PT, "middle", true, 10))
            sb.append(tx(180f, 232f, "دایره — بی‌ضلع و بی‌زاویه", ST, "middle", true, 13))
            sb.append(tx(180f, 254f, "هر نقطه روی دایره به مرکز فاصلهٔ یکسان دارد", MU, "middle", false, 10))
            return sb.toString()
        }
        val n = when (kind) {
            "tri" -> 3
            "square", "rect" -> 4
            else -> spec.xNum("n", 5f).toInt().coerceIn(3, 8)
        }
        val pts = StringBuilder()
        for (i in 0 until n) {
            val ang = -PI / 2 + i * 2 * PI / n
            val x = 180f + 80f * cos(ang).toFloat()
            val y = if (kind == "rect") 128f + (80f * 0.62f) * sin(ang).toFloat() else 128f + 80f * sin(ang).toFloat()
            pts.append(f(x)).append(',').append(f(y)).append(' ')
        }
        sb.append(poly(pts.toString().trim(), "rgba(108,99,245,.16)", ACC, 2.2f))
        sb.append(cls(180f, 128f, 3.4f, PT, "none", 0f))
        val name = when (kind) {
            "tri" -> "مثلث"
            "square" -> "مربع"
            "rect" -> "مستطیل"
            else -> "$n" + "ضلعی (چندضلعی)"
        }
        sb.append(tx(180f, 232f, "$name — $n ضلع و $n زاویه", ST, "middle", true, 13))
        val note = when (kind) {
            "square" -> "مربع: چهار ضلع برابر و چهار زاویهٔ ۹۰°"
            "rect" -> "مستطیل: ضلع‌های روبه‌رو برابر و چهار زاویهٔ ۹۰°"
            else -> "مجموع زاویه‌های داخلی = ($n − ۲) × ۱۸۰°"
        }
        sb.append(tx(180f, 254f, note, MU, "middle", false, 10))
        return sb.toString()
    }

    private fun circleParts(spec: FigureSpec): String {
        val show = spec.xStr("show", "all")
        fun on(k: String) = show == "all" || show == k
        val cx = 180f; val cy = 138f; val r = 86f
        val sb = StringBuilder()
        sb.append(cls(cx, cy, r, "rgba(108,99,245,.06)", ACC, 2.2f)).append(cls(cx, cy, 4f, ST, "none", 0f))
        sb.append(tx(cx, cy + 16f, "O", ST, "middle", true, 11))
        if (on("radius")) {
            sb.append(ln(cx, cy, cx + r * cos(-PI / 4).toFloat(), cy + r * sin(-PI / 4).toFloat(), PT, 2f))
            sb.append(tx(cx + 36f, cy - 30f, "شعاع r", PT, "start", true, 11))
        }
        if (on("diameter")) {
            sb.append(ln(cx - r, cy, cx + r, cy, "#1f8f7a", 2f))
            sb.append(tx(cx - 62f, cy - 8f, "قطر 2r", "#1f8f7a", "middle", true, 11))
        }
        if (on("chord")) {
            sb.append(ln(cx - 66f, cy + 46f, cx + 30f, cy + 74f, GLD, 2f))
            sb.append(tx(cx - 34f, cy + 92f, "وتر", GLD, "middle", true, 11))
        }
        if (on("tangent")) {
            sb.append(ln(cx - 96f, cy + r, cx + 96f, cy + r, "#8e44ad", 2f, "5 4"))
            sb.append(tx(cx + 92f, cy + r + 14f, "خط مماس", "#8e44ad", "end", true, 11))
        }
        if (on("arc")) {
            val a0 = PI * 1.15; val a1 = PI * 1.75
            sb.append(path("M ${f(cx + r * cos(a0).toFloat())} ${f(cy + r * sin(a0).toFloat())} A ${f(r)} ${f(r)} 0 0 1 ${f(cx + r * cos(a1).toFloat())} ${f(cy + r * sin(a1).toFloat())}", "none", PT, 3.4f))
            sb.append(tx(cx - 66f, cy - 40f, "کمان", PT, "end", true, 11))
        }
        sb.append(tx(180f, 250f, "اجزای دایره — مرکز، شعاع، قطر، وتر، مماس و کمان", MU, "middle", true, 11))
        return sb.toString()
    }

    private fun cylinderCone(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "cylinder").lowercase()
        val r = max(1f, spec.xNum("r", 3f))
        val h = max(1f, spec.xNum("h", 6f))
        val rr = min(66f, 12f * r)
        val hh = min(120f, 14f * h)
        val cx = 168f; val bot = 168f; val top = bot - hh
        val ry = rr * 0.34f
        val sb = StringBuilder()
        if (kind == "cone") {
            sb.append(path("M ${f(cx - rr)} ${f(bot)} L $cx ${f(top)} L ${f(cx + rr)} ${f(bot)}", "rgba(240,162,2,.16)", GLD, 2.2f))
            sb.append(path("M ${f(cx - rr)} ${f(bot)} A ${f(rr)} ${f(ry)} 0 0 0 ${f(cx + rr)} ${f(bot)}", "rgba(240,162,2,.10)", GLD, 2.2f))
            sb.append(path("M ${f(cx - rr)} ${f(bot)} A ${f(rr)} ${f(ry)} 0 0 1 ${f(cx + rr)} ${f(bot)}", "none", GLD, 1.4f, "5 4"))
            sb.append(ln(cx, bot, cx + rr, bot, PT, 1.6f)).append(tx(cx + rr / 2f, bot + 16f, "r = " + num(r), PT, "middle", true, 10))
            sb.append(ln(cx, bot, cx, top, PT, 1.6f, "4 3")).append(tx(cx - 8f, top + hh / 2f, "h = " + num(h), PT, "end", true, 10))
            val l = sqrt(r * r + h * h)
            sb.append(tx(180f, 218f, "حجم = ⅓πr²h ≈ " + num((PI * r * r * h / 3 * 100).roundToInt() / 100f), ST, "middle", true, 12))
            sb.append(tx(180f, 240f, "سطح کل = πr(r + l)، l = √(r² + h²) ≈ " + num((l * 100).roundToInt() / 100f), ACC, "middle", true, 11))
            sb.append(tx(180f, 260f, "مخروط — قاعدهٔ دایره‌ای و یک رأس", MU, "middle", false, 10))
            return sb.toString()
        }
        sb.append(path("M ${f(cx - rr)} ${f(top)} L ${f(cx - rr)} ${f(bot)} M ${f(cx + rr)} ${f(top)} L ${f(cx + rr)} ${f(bot)}", "none", ACC, 2.2f))
        sb.append(path("M ${f(cx - rr)} ${f(bot)} A ${f(rr)} ${f(ry)} 0 0 0 ${f(cx + rr)} ${f(bot)}", "none", ACC, 2.2f))
        sb.append(path("M ${f(cx - rr)} ${f(bot)} A ${f(rr)} ${f(ry)} 0 0 1 ${f(cx + rr)} ${f(bot)}", "none", ACC, 1.4f, "5 4"))
        sb.append("<ellipse cx=\"${f(cx)}\" cy=\"${f(top)}\" rx=\"${f(rr)}\" ry=\"${f(ry)}\" fill=\"rgba(108,99,245,.18)\" stroke=\"$ACC\" stroke-width=\"2\"/>")
        sb.append(ln(cx, top, cx + rr, top, PT, 1.6f)).append(tx(cx + rr / 2f, top - 14f, "r = " + num(r), PT, "middle", true, 10))
        sb.append(ln(cx + rr + 18f, top, cx + rr + 18f, bot, PT, 1.6f, "4 3")).append(tx(cx + rr + 24f, top + hh / 2f, "h = " + num(h), PT, "start", true, 10))
        sb.append(tx(180f, 214f, "حجم = πr²h ≈ " + num((PI * r * r * h * 100).roundToInt() / 100f), ST, "middle", true, 12))
        sb.append(tx(180f, 236f, "سطح کل = ۲πr(r + h) ≈ " + num((2 * PI * r * (r + h) * 100).roundToInt() / 100f), ACC, "middle", true, 11))
        sb.append(tx(180f, 258f, "استوانه — دو قاعدهٔ دایره‌ای موازی", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun proportion(spec: FigureSpec): String {
        val a = max(0.1f, spec.xNum("a", 3f))
        val b = max(0.1f, spec.xNum("b", 4f))
        val c = max(0.1f, spec.xNum("c", 6f))
        val x = b * c / a
        val y0 = 96f
        val sb = StringBuilder()
        sb.append(tx(96f, y0 - 8f, num(a), ACC, "middle", true, 20))
        sb.append(ln(66f, y0, 126f, y0, ST, 2f))
        sb.append(tx(96f, y0 + 30f, num(b), ACC, "middle", true, 20))
        sb.append(tx(180f, y0 + 12f, "=", ST, "middle", true, 20))
        sb.append(tx(264f, y0 - 8f, num(c), "#1f8f7a", "middle", true, 20))
        sb.append(ln(234f, y0, 294f, y0, ST, 2f))
        sb.append(tx(264f, y0 + 30f, "x", PT, "middle", true, 20))
        sb.append(path("M 120 ${f(y0 - 22f)} L 250 ${f(y0 + 44f)}", "none", GLD, 1.3f, "4 4"))
        sb.append(path("M 250 ${f(y0 - 22f)} L 120 ${f(y0 + 44f)}", "none", GLD, 1.3f, "4 4"))
        sb.append(tx(180f, 224f, "a × x = b × c  →  x = (b × c) ÷ a", ST, "middle", true, 12))
        sb.append(tx(180f, 246f, num(a) + " × x = " + num(b) + " × " + num(c) + "  →  x = " + num((x * 100).roundToInt() / 100f), ACC, "middle", true, 12))
        sb.append(tx(180f, 266f, "تناسب مستقیم — جدول نسبت", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun linearSystem(spec: FigureSpec): String {
        val m1 = spec.xNum("m1", 1f); val b1 = spec.xNum("b1", 2f)
        val m2 = spec.xNum("m2", -1f); val b2 = spec.xNum("b2", 4f)
        val ox = 180f; val oy = 148f; val sc = 20f
        val sb = StringBuilder()
        for (g in -6..6) {
            sb.append(ln(ox + g * sc, 44f, ox + g * sc, 250f, GR, 0.7f))
            if (g != 0) sb.append(tx(ox + g * sc, oy + 14f, g.toString(), MU, "middle", false, 9))
        }
        for (g in -5..5) {
            sb.append(ln(52f, oy - g * sc, 320f, oy - g * sc, GR, 0.7f))
            if (g != 0) sb.append(tx(ox - 12f, oy - g * sc + 3f, g.toString(), MU, "end", false, 9))
        }
        sb.append(ln(46f, oy, 322f, oy, ST, 1.8f)).append(arrowHeadRight(322f, oy))
        sb.append(ln(ox, 254f, ox, 42f, ST, 1.8f)).append(arrowHeadUp(ox, 42f))
        sb.append(tx(326f, oy + 14f, "x", ST, "middle", true, 12))
        sb.append(tx(ox - 12f, 40f, "y", ST, "middle", true, 12))
        fun px(v: Float) = ox + v * sc
        fun py(v: Float) = oy - v * sc
        sb.append(ln(px(-6f), py(m1 * -6f + b1), px(6f), py(m1 * 6f + b1), ACC, 2.2f))
        sb.append(ln(px(-6f), py(m2 * -6f + b2), px(6f), py(m2 * 6f + b2), PT, 2.2f))
        val den = m1 - m2
        if (abs(den) > 1e-6f) {
            val ix = (b2 - b1) / den
            val iy = m1 * ix + b1
            sb.append(cls(px(ix), py(iy), 5.5f, "#1f8f7a", "#ffffff", 1.2f))
            sb.append(tx(min(max(px(ix) + 8f, 60f), 300f), min(max(py(iy) - 10f, 56f), 240f),
                "(" + num((ix * 100).roundToInt() / 100f) + ", " + num((iy * 100).roundToInt() / 100f) + ")", "#1f8f7a", "start", true, 11))
            sb.append(tx(180f, 268f, "نقطهٔ تقاطع — جواب دستگاه", "#1f8f7a", "middle", true, 11))
        } else {
            sb.append(tx(180f, 268f, "دو خط موازی‌اند — دستگاه جواب ندارد", PT, "middle", true, 11))
        }
        sb.append(tx(70f, 56f, "y = " + num(m1) + "x + " + num(b1), ACC, "start", true, 11))
        sb.append(tx(70f, 74f, "y = " + num(m2) + "x + " + num(b2), PT, "start", true, 11))
        return sb.toString()
    }

    // ---------------------------------------------------------------- متوسطه و علوم
    private fun barStats(spec: FigureSpec): String {
        var data = ints(spec, "data", "4,7,3,6,5")
        if (data.isEmpty()) data = listOf(4, 7, 3, 6, 5)
        val labs = splitList(spec.xStr("labs", "الف,ب,ج,د,ه"))
        val show = spec.xStr("show", "mean")
        val n = data.size
        val maxV = max(1, data.maxOrNull() ?: 1)
        val x0 = 66f; val y0 = 216f; val w = 260f; val h = 150f
        val bw = min(34f, w / (n * 1.6f))
        val sb = StringBuilder()
        sb.append(ln(x0 - 8f, y0, x0 + w, y0, ST, 1.8f)).append(ln(x0 - 8f, y0, x0 - 8f, y0 - h - 8f, ST, 1.8f))
        for (i in 0 until n) {
            val bx = x0 + 16f + i * (w - 20f) / n
            val bh = data[i] / maxV.toFloat() * h
            sb.append(rc(bx, y0 - bh, bw, bh, if (i % 2 == 1) "rgba(39,196,168,.75)" else "rgba(108,99,245,.75)", "#ffffff", 1.2f, 4f))
            sb.append(tx(bx + bw / 2f, y0 - bh - 6f, data[i].toString(), ST, "middle", true, 10))
            sb.append(tx(bx + bw / 2f, y0 + 16f, labs.getOrElse(i) { "مورد ${i + 1}" }, MU, "middle", false, 10))
        }
        val sum = data.sum()
        val mean = sum / n.toFloat()
        val sorted = data.sorted()
        val median = if (n % 2 == 1) sorted[(n - 1) / 2].toFloat() else (sorted[n / 2 - 1] + sorted[n / 2]) / 2f
        if (show == "mean") {
            val y = y0 - mean / maxV * h
            sb.append(ln(x0 - 4f, y, x0 + w, y, PT, 1.8f, "6 4"))
            sb.append(tx(x0 + w, y - 6f, "میانگین " + num((mean * 100).roundToInt() / 100f), PT, "end", true, 10))
        }
        sb.append(tx(180f, 248f, "میانگین = " + num((mean * 100).roundToInt() / 100f) + "  ·  میانه = " + num(median) + "  ·  بیشترین = $maxV", ST, "middle", true, 11))
        sb.append(tx(180f, 268f, "نمودار میله‌ای داده‌های آماری", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun parabola(spec: FigureSpec): String {
        val a = spec.xNum("a", 1f).let { if (abs(it) < 1e-6f) 1f else it }
        val b = spec.xNum("b", -2f)
        val c = spec.xNum("c", -3f)
        val ox = 180f; val oy = 150f; val sc = 19f
        val sb = StringBuilder()
        for (g in -6..6) {
            sb.append(ln(ox + g * sc, 34f, ox + g * sc, 258f, GR, 0.6f))
            sb.append(ln(48f, oy - g * sc, 322f, oy - g * sc, GR, 0.6f))
        }
        sb.append(ln(42f, oy, 322f, oy, ST, 1.8f)).append(arrowHeadRight(322f, oy))
        sb.append(ln(ox, 262f, ox, 30f, ST, 1.8f)).append(arrowHeadUp(ox, 30f))
        sb.append(tx(326f, oy + 14f, "x", ST, "middle", true, 12)).append(tx(ox - 12f, 28f, "y", ST, "middle", true, 12))
        val d = StringBuilder()
        var started = false
        for (i in -70..70) {
            val x = i / 10f
            val y = a * x * x + b * x + c
            if (abs(x) > 6.2f || abs(y) > 8f) { started = false; continue }
            d.append(if (started) " L " else "M ").append(f(ox + x * sc)).append(' ').append(f(oy - y * sc))
            started = true
        }
        if (d.isNotEmpty()) sb.append(path(d.toString(), "none", ACC, 2.6f))
        val vx = -b / (2 * a)
        val vy = a * vx * vx + b * vx + c
        if (abs(vx) <= 6f && abs(vy) <= 8f) {
            sb.append(cls(ox + vx * sc, oy - vy * sc, 5f, PT, "#ffffff", 1.2f))
            sb.append(tx(min(max(ox + vx * sc + 8f, 60f), 280f), min(max(oy - vy * sc - 10f, 40f), 250f),
                "رأس (" + num((vx * 100).roundToInt() / 100f) + ", " + num((vy * 100).roundToInt() / 100f) + ")", PT, "start", true, 10))
        }
        sb.append(ln(ox + vx * sc, 258f, ox + vx * sc, 34f, GLD, 1.4f, "5 4"))
        val delta = b * b - 4 * a * c
        val note: String
        if (delta > 0) {
            val r1 = (-b + sqrt(delta)) / (2 * a)
            val r2 = (-b - sqrt(delta)) / (2 * a)
            if (abs(r1) <= 6f) sb.append(cls(ox + r1 * sc, oy, 4.5f, "#1f8f7a", "#ffffff", 1.2f))
            if (abs(r2) <= 6f) sb.append(cls(ox + r2 * sc, oy, 4.5f, "#1f8f7a", "#ffffff", 1.2f))
            note = "ریشه‌ها: " + num((r2 * 100).roundToInt() / 100f) + " و " + num((r1 * 100).roundToInt() / 100f)
        } else if (delta == 0f) {
            note = "یک ریشهٔ مضاعف: " + num((vx * 100).roundToInt() / 100f)
        } else {
            note = "ریشهٔ حقیقی ندارد (Δ < 0)"
        }
        sb.append(tx(180f, 268f, note + "  ·  Δ = " + num(delta), MU, "middle", false, 10))
        sb.append(tx(66f, 44f, "y = " + num(a) + "x² + " + num(b) + "x + " + num(c), ACC, "start", true, 12))
        return sb.toString()
    }

    private fun inscribed(spec: FigureSpec): String {
        val deg = spec.xNum("ang", 60f).coerceIn(20f, 160f)
        val cx = 180f; val cy = 150f; val r = 88f
        val a0 = -PI / 2 - deg * PI / 360
        val a1 = -PI / 2 + deg * PI / 360
        val ax = cx + r * cos(a0).toFloat(); val ay = cy + r * sin(a0).toFloat()
        val bx = cx + r * cos(a1).toFloat(); val by = cy + r * sin(a1).toFloat()
        val mid = a1 + (2 * PI - deg * PI / 180) / 2
        val ccx = cx + r * cos(mid).toFloat(); val ccy = cy + r * sin(mid).toFloat()
        val sb = StringBuilder()
        sb.append(cls(cx, cy, r, "rgba(108,99,245,.05)", ACC, 2f))
        sb.append(path("M ${f(ax)} ${f(ay)} A ${f(r)} ${f(r)} 0 0 1 ${f(bx)} ${f(by)}", "none", PT, 3.6f))
        sb.append(ln(cx, cy, ax, ay, ST, 1.8f)).append(ln(cx, cy, bx, by, ST, 1.8f))
        sb.append(ln(ccx, ccy, ax, ay, GLD, 1.8f)).append(ln(ccx, ccy, bx, by, GLD, 1.8f))
        sb.append(cls(cx, cy, 4f, ST, "none", 0f)).append(cls(ax, ay, 4.5f, PT, "#ffffff", 1.2f))
        sb.append(cls(bx, by, 4.5f, PT, "#ffffff", 1.2f)).append(cls(ccx, ccy, 4.5f, GLD, "#ffffff", 1.2f))
        sb.append(tx(cx, cy + 18f, "O", ST, "middle", true, 11))
        sb.append(tx(ax - 10f, ay - 6f, "A", PT, "end", true, 11)).append(tx(bx + 10f, by - 6f, "B", PT, "start", true, 11))
        sb.append(tx(ccx, if (ccy < cy) ccy - 10f else ccy + 18f, "C", GLD, "middle", true, 11))
        sb.append(tx(180f, 40f, "زاویهٔ مرکزی = " + num(deg) + "°", PT, "middle", true, 12))
        sb.append(tx(180f, 250f, "زاویهٔ محاطی = نصف زاویهٔ مرکزی = " + num((deg / 2 * 10).roundToInt() / 10f) + "°", GLD, "middle", true, 12))
        sb.append(tx(180f, 268f, "زاویه‌های محاطی روی یک کمان با هم برابرند", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun pendulum(spec: FigureSpec): String {
        val len = max(0.1f, spec.xNum("len", 1f))
        val amp = spec.xNum("amp", 20f).coerceIn(5f, 60f)
        val period = (2 * PI * sqrt(len / 9.8)).toFloat()
        val px = 180f; val py = 46f; val plen = 150f
        val th = amp * PI / 180
        val bx = px + plen * sin(th).toFloat(); val by = py + plen * cos(th).toFloat()
        val sb = StringBuilder()
        sb.append(rc(150f, 34f, 60f, 12f, ST, ST, 1f, 5f)).append(tx(180f, 30f, "تکیه‌گاه", MU, "middle", false, 10))
        sb.append(path("M $px $py A ${f(plen)} ${f(plen)} 0 0 1 ${f(px + plen)} $py", "none", GR, 1.4f, "5 4"))
        sb.append(ln(px, py, bx, by, ST, 2f))
        sb.append(cls(bx, by, 12f, "rgba(214,51,108,.85)", PT, 1.4f)).append(cls(px, py, 3.4f, ST, "none", 0f))
        sb.append(path("M ${f(bx)} ${f(by)} L ${f(bx)} ${f(py + plen)}", "none", MU, 1.2f, "4 4"))
        sb.append(path("M ${f(bx)} ${f(py + plen)} A ${f(plen)} ${f(plen)} 0 0 1 ${f(px + plen)} ${f(py + plen - 0.1f)}", "none", MU, 1f, "3 4"))
        sb.append(tx(px + 8f, py + plen / 2f, "L = " + num(len) + " m", PT, "start", true, 11))
        sb.append(tx(250f, py + 40f, "θ = " + num(amp) + "°", GLD, "start", true, 11))
        sb.append(tx(180f, 236f, "دورهٔ آونگ T = ۲π√(L / g) ≈ " + num((period * 100).roundToInt() / 100f) + " s", ST, "middle", true, 13))
        sb.append(tx(180f, 258f, "g ≈ ۹٫۸ m/s²   ·   مستقل از جرم گلوله", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun incline(spec: FigureSpec): String {
        val ang = spec.xNum("ang", 30f).coerceIn(10f, 60f)
        val m = max(1f, spec.xNum("mass", 5f))
        val ax = 74f; val ay = 210f; val len = 220f
        val rad = ang * PI / 180
        val bx = ax + len
        val cxp = ax + len * cos(rad).toFloat(); val cyp = ay - len * sin(rad).toFloat()
        val sb = StringBuilder()
        sb.append(poly("$ax,$ay ${f(bx)},$ay ${f(cxp)},${f(cyp)}", "rgba(108,99,245,.10)", ACC, 2f))
        val t = 0.55f
        val px = ax + len * t * cos(rad).toFloat(); val py = ay - len * t * sin(rad).toFloat()
        sb.append(poly("${f(px - 9f)},${f(py - 20f)} ${f(px + 11f)},${f(py - 6f)} ${f(px + 2f)},${f(py + 3f)} ${f(px - 18f)},${f(py - 11f)}", "rgba(240,162,2,.9)", "#8a5b00", 1.2f))
        val wgt = m * 9.8f
        val sv = sin(rad).toFloat(); val cv = cos(rad).toFloat()
        sb.append(ln(px, py, px, py + 34f, PT, 1.8f))
        sb.append(poly("${f(px - 4.5f)},${f(py + 26f)} ${f(px + 4.5f)},${f(py + 26f)} ${f(px)},${f(py + 34f)}", PT, "none", 0f))
        sb.append(tx(px, py + 48f, "mg = " + num((wgt * 10).roundToInt() / 10f) + " N", PT, "middle", true, 10))
        sb.append(ln(px, py, px - sv * 44f, py + cv * 44f, GLD, 1.8f))
        sb.append(poly("${f(px - sv * 44f - 4f)},${f(py + cv * 44f - 3f)} ${f(px - sv * 44f + 4f)},${f(py + cv * 44f + 3f)} ${f(px - sv * 44f + 1f)},${f(py + cv * 44f + 9f)}", GLD, "none", 0f))
        sb.append(tx(px - sv * 62f - 14f, py + cv * 62f + 24f, "mg·sinθ = " + num((wgt * sv * 10).roundToInt() / 10f) + " N", GLD, "middle", true, 10))
        sb.append(ln(px, py, px - sv * 40f, py - cv * 40f, "#1f8f7a", 1.8f))
        sb.append(tx(px - sv * 54f - 6f, py - cv * 54f - 8f, "N = " + num((wgt * cv * 10).roundToInt() / 10f) + " N", "#1f8f7a", "middle", true, 10))
        sb.append(path("M ${f(ax + 34f)} $ay A 34 34 0 0 0 ${f(ax + 34f * cos(rad).toFloat())} ${f(ay - 34f * sin(rad).toFloat())}", "none", ST, 1.4f))
        sb.append(tx(ax + 46f, ay - 8f, "θ = " + num(ang) + "°", ST, "start", true, 11))
        sb.append(tx(180f, 236f, "نیروی موازی با سطح: F = mg·sinθ", ST, "middle", true, 12))
        sb.append(tx(180f, 258f, "نیروی عمود بر سطح: N = mg·cosθ   ·   m = " + num(m) + " kg", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun circuit(spec: FigureSpec): String {
        val parallel = spec.xStr("mode", "series") == "parallel"
        val r1 = max(0.5f, spec.xNum("r1", 4f))
        val r2 = max(0.5f, spec.xNum("r2", 6f))
        val v = max(0.1f, spec.xNum("v", 12f))
        val req = if (parallel) (r1 * r2) / (r1 + r2) else r1 + r2
        val i = v / req
        val sb = StringBuilder()
        sb.append(rc(46f, 96f, 16f, 84f, "#ffffff", ST, 1.8f, 3f))
        sb.append(ln(46f, 110f, 62f, 110f, ST, 1.6f)).append(ln(46f, 166f, 62f, 166f, ST, 1.6f))
        sb.append(ln(54f, 120f, 54f, 156f, ST, 1.4f)).append(ln(48f, 124f, 60f, 124f, ST, 1.4f)).append(ln(54f, 134f, 54f, 142f, ST, 1.4f))
        sb.append(tx(38f, 142f, "V=" + num(v) + "V", MU, "middle", true, 10))
        if (parallel) {
            sb.append(ln(62f, 110f, 320f, 110f, ST, 1.8f)).append(ln(62f, 166f, 320f, 166f, ST, 1.8f)).append(ln(320f, 110f, 320f, 166f, ST, 1.8f))
            sb.append(rc(128f, 96f, 64f, 28f, "rgba(108,99,245,.16)", ACC, 1.8f, 4f)).append(tx(160f, 114f, "R₁=" + num(r1) + "Ω", ST, "middle", true, 11))
            sb.append(rc(128f, 152f, 64f, 28f, "rgba(39,196,168,.18)", "#1f8f7a", 1.8f, 4f)).append(tx(160f, 170f, "R₂=" + num(r2) + "Ω", ST, "middle", true, 11))
            sb.append(tx(240f, 102f, "I₁=" + num((v / r1 * 100).roundToInt() / 100f) + "A", ACC, "start", true, 10))
            sb.append(tx(240f, 186f, "I₂=" + num((v / r2 * 100).roundToInt() / 100f) + "A", "#1f8f7a", "start", true, 10))
        } else {
            sb.append(ln(62f, 110f, 120f, 110f, ST, 1.8f)).append(ln(120f, 96f, 188f, 96f, ST, 1.8f)).append(ln(188f, 110f, 320f, 110f, ST, 1.8f))
            sb.append(ln(62f, 166f, 320f, 166f, ST, 1.8f)).append(ln(320f, 110f, 320f, 166f, ST, 1.8f))
            sb.append(rc(120f, 82f, 68f, 28f, "rgba(108,99,245,.16)", ACC, 1.8f, 4f)).append(tx(154f, 100f, "R₁=" + num(r1) + "Ω", ST, "middle", true, 11))
            sb.append(rc(212f, 82f, 68f, 28f, "rgba(39,196,168,.18)", "#1f8f7a", 1.8f, 4f)).append(tx(246f, 100f, "R₂=" + num(r2) + "Ω", ST, "middle", true, 11))
            sb.append(ln(188f, 96f, 212f, 96f, ST, 1.8f))
            sb.append(tx(262f, 138f, "I = " + num((i * 100).roundToInt() / 100f) + " A", ST, "middle", true, 11))
        }
        sb.append(tx(180f, 212f, (if (parallel) "مدار موازی" else "مدار سری") + "  ·  R کل = " + num((req * 100).roundToInt() / 100f) + " Ω", ST, "middle", true, 13))
        sb.append(tx(180f, 234f, "جریان کل I = V ÷ R ≈ " + num((i * 100).roundToInt() / 100f) + " A", ACC, "middle", true, 12))
        sb.append(tx(180f, 256f, if (parallel) "در موازی: ۱/R = ۱/R₁ + ۱/R₂ و ولتاژها برابر" else "در سری: R = R₁ + R₂ و جریان‌ها برابر", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun optics(spec: FigureSpec): String {
        val concave = spec.xStr("kind", "convex") == "concave"
        val f = max(2f, spec.xNum("f", 10f))
        val d = max(1f, spec.xNum("d", 30f))
        val cx = 190f; val cy = 152f; val s = 2.4f
        val di = if (concave) (f * d) / (d + f) else (f * d) / (d - f)
        val sb = StringBuilder()
        sb.append(ln(36f, cy, 330f, cy, ST, 1.6f)).append(arrowHeadRight(330f, cy))
        sb.append(path("M $cx ${f(cy - 74f)} Q ${f(cx + 22f)} ${f(cy)} $cx ${f(cy + 74f)}", "none", ACC, 2.4f))
        if (concave) sb.append(path("M $cx ${f(cy - 74f)} Q ${f(cx + 10f)} ${f(cy)} $cx ${f(cy + 74f)}", "none", ACC, 2.4f))
        sb.append(tx(cx + 26f, cy - 82f, if (concave) "عدسی مقعر" else "عدسی محدب", ACC, "middle", true, 11))
        sb.append(cls(cx - f * s, cy, 3.4f, PT, "none", 0f)).append(cls(cx + f * s, cy, 3.4f, PT, "none", 0f))
        sb.append(tx(cx - f * s, cy + 16f, "F", PT, "middle", true, 11)).append(tx(cx + f * s, cy + 16f, "F'", PT, "middle", true, 11))
        val ox = cx - d * s
        val oh = 46f
        sb.append(ln(ox, cy, ox, cy - oh, "#1f8f7a", 2.4f))
        sb.append(poly("${f(ox - 4f)},${f(cy - oh + 8f)} ${f(ox + 4f)},${f(cy - oh + 8f)} ${f(ox)},${f(cy - oh)}", "#1f8f7a", "none", 0f))
        sb.append(tx(ox, cy + 18f, "جسم", "#1f8f7a", "middle", true, 10))
        val ix = cx + di * s
        val ih = oh * (di / d)
        sb.append(ln(ix, cy, ix, cy + ih, GLD, 2.4f))
        sb.append(poly("${f(ix - 4f)},${f(cy + ih - 8f)} ${f(ix + 4f)},${f(cy + ih - 8f)} ${f(ix)},${f(cy + ih)}", GLD, "none", 0f))
        sb.append(tx(ix, cy - 8f, "تصویر", GLD, "middle", true, 10))
        sb.append(ln(ox, cy - oh, cx, cy - oh, MU, 1.1f, "4 4"))
        sb.append(ln(cx, cy - oh, cx + f * s, cy, MU, 1.1f, "4 4"))
        sb.append(ln(ox, cy - oh, cx, cy, MU, 1.2f))
        sb.append(tx(180f, 234f, "۱/f = ۱/فاصلهٔ جسم + ۱/فاصلهٔ تصویر", ST, "middle", true, 12))
        sb.append(tx(180f, 254f, "f = " + num(f) + " cm · d = " + num(d) + " cm → تصویر در " + num((di * 100).roundToInt() / 100f) + " cm", ACC, "middle", true, 11))
        sb.append(tx(180f, 272f, "بزرگنمایی = " + num((abs(di / d) * 100).roundToInt() / 100f) + " برابر", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun heatCurve(spec: FigureSpec): String {
        val x0 = 62f; val y0 = 214f; val w = 260f; val h = 160f
        fun px(u: Float) = x0 + u / 100f * w
        fun py(v: Float) = y0 - (v + 15f) / 130f * h
        val sb = StringBuilder()
        sb.append(ln(x0 - 8f, y0, x0 + w + 6f, y0, ST, 1.6f)).append(ln(x0 - 8f, y0, x0 - 8f, y0 - h - 8f, ST, 1.6f))
        sb.append(tx(x0 + w / 2f, y0 + 20f, "گرما (زمان)", MU, "middle", true, 10))
        sb.append(tx(x0 - 20f, y0 - h - 4f, "دما", MU, "middle", true, 10))
        sb.append(path("M ${f(px(0f))} ${f(py(-10f))} L ${f(px(20f))} ${f(py(0f))} L ${f(px(40f))} ${f(py(0f))} L ${f(px(60f))} ${f(py(100f))} L ${f(px(80f))} ${f(py(100f))} L ${f(px(100f))} ${f(py(110f))}", "none", ACC, 2.6f))
        sb.append(ln(x0 - 12f, py(0f), x0 + w, py(0f), GR, 1f, "4 4"))
        sb.append(tx(x0 - 18f, py(0f) + 4f, "۰°C", PT, "end", true, 10))
        sb.append(ln(x0 - 12f, py(100f), x0 + w, py(100f), GR, 1f, "4 4"))
        sb.append(tx(x0 - 18f, py(100f) + 4f, "۱۰۰°C", PT, "end", true, 10))
        sb.append(tx(px(10f), py(-4f) - 10f, "جامد", ST, "middle", true, 10))
        sb.append(tx(px(30f), py(0f) - 12f, "ذوب (دما ثابت)", PT, "middle", true, 10))
        sb.append(tx(px(50f), py(50f), "مایع", ST, "middle", true, 10))
        sb.append(tx(px(70f), py(100f) - 12f, "جوش (دما ثابت)", PT, "middle", true, 10))
        sb.append(tx(px(92f), py(108f) + 12f, "بخار", ST, "middle", true, 10))
        sb.append(tx(180f, 250f, "منحنی گرمایش آب — در نقطهٔ ذوب و جوش دما تغییر نمی‌کند", MU, "middle", true, 11))
        sb.append(tx(180f, 270f, "گرمای نهان: Q = mL  ·  گرمای ویژه: Q = mcΔT", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun dna(spec: FigureSpec): String {
        val flat = spec.xStr("mode", "helix") == "flat"
        val seq = spec.xStr("seq", "ATGC").uppercase().take(6).ifBlank { "ATGC" }
        fun pair(b: Char) = when (b) { 'A' -> 'T'; 'T' -> 'A'; 'G' -> 'C'; 'C' -> 'G'; else -> '?' }
        val sb = StringBuilder()
        if (flat) {
            sb.append(ln(96f, 52f, 96f, 224f, ACC, 2.6f)).append(ln(264f, 52f, 264f, 224f, ACC, 2.6f))
            seq.forEachIndexed { i, ch ->
                val y = 68f + i * 26f
                sb.append(ln(96f, y, 264f, y, GR, 1.8f))
                sb.append(tx(86f, y + 5f, ch.toString(), PT, "end", true, 13))
                sb.append(tx(274f, y + 5f, pair(ch).toString(), "#1f8f7a", "start", true, 13))
                sb.append(tx(180f, y + 5f, if (ch == 'A' || ch == 'T') "۲ پیوند هیدروژنی" else "۳ پیوند هیدروژنی", MU, "middle", false, 9))
            }
            sb.append(tx(180f, 244f, "DNA — دو رشتهٔ مکمل و جفت‌بازهای A–T و G–C", ST, "middle", true, 12))
            return sb.toString()
        }
        val d1 = StringBuilder(); val d2 = StringBuilder()
        for (i in 0..60) {
            val x = 70f + i * 3.6f
            val ph = i / 60.0 * PI * 4
            d1.append(if (i == 0) "M " else " L ").append(f(x)).append(' ').append(f(132f + 34f * sin(ph).toFloat()))
            d2.append(if (i == 0) "M " else " L ").append(f(x)).append(' ').append(f(132f - 34f * sin(ph).toFloat()))
        }
        sb.append(path(d1.toString(), "none", ACC, 2.6f)).append(path(d2.toString(), "none", PT, 2.6f))
        for (i in 0..8) {
            val x = 74f + i * 27f
            val ph = (x - 70f) / 3.6f / 60.0 * PI * 4
            val y1 = 132f + 34f * sin(ph).toFloat()
            val y2 = 132f - 34f * sin(ph).toFloat()
            sb.append(ln(x, y1, x, y2, GR, 1.4f))
            if (i < seq.length) {
                sb.append(tx(x - 5f, y1 + 4f, seq[i].toString(), ST, "end", true, 10))
                sb.append(tx(x + 5f, y2 + 4f, pair(seq[i]).toString(), ST, "start", true, 10))
            }
        }
        sb.append(tx(180f, 214f, "مارپیچ دوگانهٔ DNA — پیوند فسفودی‌استر و جفت‌بازهای مکمل", ST, "middle", true, 11))
        sb.append(tx(180f, 236f, "A با T و G با C جفت می‌شوند", MU, "middle", false, 10))
        sb.append(tx(180f, 256f, "توالی نمونه: $seq", ACC, "middle", true, 11))
        return sb.toString()
    }

    private fun cell(spec: FigureSpec): String {
        val plant = spec.xStr("kind", "animal") == "plant"
        val sb = StringBuilder()
        sb.append(path("M 72 152 Q 76 66 180 62 Q 284 66 288 152 Q 284 236 180 240 Q 76 236 72 152 Z",
            if (plant) "rgba(39,196,168,.10)" else "rgba(108,99,245,.08)", if (plant) "#1f8f7a" else ACC, 2.6f))
        sb.append(cls(150f, 138f, 34f, "rgba(108,99,245,.22)", ACC, 2f))
        sb.append(tx(150f, 142f, "هسته", ST, "middle", true, 10)).append(tx(150f, 156f, "DNA", MU, "middle", false, 8))
        sb.append("<ellipse cx=\"236\" cy=\"112\" rx=\"26\" ry=\"13\" fill=\"rgba(240,162,2,.35)\" stroke=\"$GLD\" stroke-width=\"1.6\" transform=\"rotate(-18 236 112)\"/>")
        sb.append(tx(236f, 116f, "میتوکندری", ST, "middle", true, 8))
        if (plant) {
            sb.append("<ellipse cx=\"214\" cy=\"196\" rx=\"34\" ry=\"22\" fill=\"rgba(39,196,168,.25)\" stroke=\"#1f8f7a\" stroke-width=\"1.6\"/>")
            sb.append(tx(214f, 200f, "واکوئل", ST, "middle", true, 9))
            sb.append("<ellipse cx=\"112\" cy=\"200\" rx=\"18\" ry=\"10\" fill=\"rgba(39,196,168,.4)\" stroke=\"#1f8f7a\" stroke-width=\"1.4\" transform=\"rotate(24 112 200)\"/>")
            sb.append(tx(112f, 204f, "کلروپلاست", ST, "middle", true, 8))
            sb.append(rc(70f, 60f, 220f, 12f, "rgba(240,162,2,.25)", "#8a5b00", 1.4f, 4f))
            sb.append(tx(180f, 70f, "دیوارهٔ سلولی", ST, "middle", true, 9))
        } else {
            sb.append("<ellipse cx=\"232\" cy=\"200\" rx=\"26\" ry=\"18\" fill=\"rgba(214,51,108,.18)\" stroke=\"$PT\" stroke-width=\"1.6\"/>")
            sb.append(tx(232f, 204f, "واکوئل کوچک", ST, "middle", true, 8))
        }
        sb.append(tx(180f, 258f, if (plant) "سلول گیاهی — دیوارهٔ سلولی، کلروپلاست و واکوئل بزرگ" else "سلول جانوری — غشا، هسته و میتوکندری", ST, "middle", true, 11))
        sb.append(tx(180f, 274f, "هر اندامک کار مشخصی دارد", MU, "middle", false, 9))
        return sb.toString()
    }

    // ---------------------------------------------------------------- انسانی و دانشگاه
    private fun timeline(spec: FigureSpec): String {
        var items = splitList(spec.xStr("events", "۵۵۹:سقوط ساسانیان,۶۲۲:هجرت پیامبر,۱۲۵۸:ایلخانان,۱۵۰۱:صفویان"))
        if (items.isEmpty()) items = listOf("۵۵۹:سقوط ساسانیان", "۶۲۲:هجرت پیامبر", "۱۲۵۸:ایلخانان", "۱۵۰۱:صفویان")
        items = items.take(6)
        val n = items.size
        val x0 = 56f; val x1 = 320f; val y = 148f
        val sb = StringBuilder()
        sb.append(ln(x0 - 12f, y, x1 + 12f, y, ST, 2.4f)).append(arrowHeadRight(x1 + 12f, y))
        items.forEachIndexed { i, raw ->
            val parts = raw.split(":", "：")
            val year = parts.getOrNull(0)?.trim() ?: ""
            val label = parts.getOrNull(1)?.trim() ?: ""
            val px = if (n == 1) (x0 + x1) / 2f else x0 + (x1 - x0) * i / (n - 1)
            val up = i % 2 == 0
            sb.append(cls(px, y, 5.5f, if (i % 2 == 1) GLD else ACC, "#ffffff", 1.2f))
            sb.append(ln(px, y, px, if (up) y - 34f else y + 34f, GR, 1.4f, "4 4"))
            sb.append(tx(px, if (up) y - 44f else y + 50f, year, ST, "middle", true, 11))
            sb.append(tx(px, if (up) y - 62f else y + 68f, label, MU, "middle", false, 9))
        }
        sb.append(tx(180f, 258f, "خط زمان تاریخی — ترتیب رویدادها از چپ به راست", ST, "middle", true, 11))
        return sb.toString()
    }

    private fun mapGeo(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "compass").lowercase()
        val sb = StringBuilder()
        if (kind == "map") {
            for (i in 0..6) {
                sb.append(ln(66f + i * 38f, 54f, 66f + i * 38f, 220f, GR, 0.8f))
                sb.append(ln(66f, 54f + i * 27.7f, 294f, 54f + i * 27.7f, GR, 0.8f))
            }
            sb.append(rc(60f, 48f, 240f, 178f, "none", ST, 1.8f))
            sb.append(poly("176,84 214,96 232,140 208,184 168,196 146,152 152,106", "rgba(39,196,168,.28)", "#1f8f7a", 1.8f))
            sb.append(cls(196f, 140f, 5f, PT, "#ffffff", 1.2f)).append(tx(206f, 136f, "تهران", ST, "start", true, 10))
            sb.append(tx(180f, 244f, "نقشه — موقعیت و مختصات جغرافیایی", ST, "middle", true, 11))
            sb.append(ln(60f, 268f, 140f, 268f, ST, 2.4f)).append(ln(60f, 264f, 60f, 272f, ST, 1.6f)).append(ln(140f, 264f, 140f, 272f, ST, 1.6f))
            sb.append(tx(100f, 262f, "مقیاس ۱۰۰ کیلومتر", MU, "middle", false, 9))
            return sb.toString()
        }
        val cx = 180f; val cy = 146f; val r = 62f
        sb.append(cls(cx, cy, r, "rgba(108,99,245,.06)", ACC, 2f))
        val dirs = listOf("شمال" to -PI / 2, "شمال‌شرق" to -PI / 4, "شرق" to 0.0, "جنوب‌شرق" to PI / 4,
            "جنوب" to PI / 2, "جنوب‌غرب" to 3 * PI / 4, "غرب" to PI, "شمال‌غرب" to 5 * PI / 4)
        dirs.forEachIndexed { i, (name, ang) ->
            sb.append(ln(cx, cy, cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat(), if (i % 2 == 1) GR else ST, if (i % 2 == 1) 1f else 1.8f))
            sb.append(tx(cx + (r + 22f) * cos(ang).toFloat(), cy + (r + 22f) * sin(ang).toFloat() + 3f, name, if (i == 0) PT else MU, "middle", i == 0, 9))
        }
        sb.append(poly("180,86 187,152 180,144 173,152", PT, PT, 1f))
        sb.append(poly("180,206 173,140 180,148 187,140", ACC, ACC, 1f))
        sb.append(tx(180f, 246f, "جهت‌های جغرافیایی — شمال بالا، جنوب پایین", ST, "middle", true, 11))
        sb.append(tx(180f, 266f, "شمال ← شرق ← جنوب ← غرب (در جهت عقربه‌های ساعت)", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun sentence(spec: FigureSpec): String {
        val subj = spec.xStr("subj", "کودکان")
        val verb = spec.xStr("verb", "بازی می‌کنند")
        val obj = spec.xStr("obj", "")
        val nominal = spec.xStr("mode", "verbal") == "nominal"
        val sb = StringBuilder()
        sb.append(rc(40f, 66f, 120f, 52f, "rgba(108,99,245,.16)", ACC, 1.8f, 10f))
        sb.append(tx(100f, 88f, subj, ST, "middle", true, 13)).append(tx(100f, 106f, "نهاد", MU, "middle", false, 9))
        sb.append(rc(200f, 66f, 132f, 52f, "rgba(39,196,168,.18)", "#1f8f7a", 1.8f, 10f))
        sb.append(tx(266f, 88f, verb, ST, "middle", true, 13))
        sb.append(tx(266f, 106f, if (nominal) "گزاره (مسند)" else "گزاره (فعل)", MU, "middle", false, 9))
        sb.append(ln(162f, 92f, 198f, 92f, ST, 1.8f)).append(arrowHeadRight(198f, 92f))
        if (obj.isNotBlank()) {
            sb.append(rc(110f, 150f, 140f, 46f, "rgba(240,162,2,.18)", GLD, 1.8f, 10f))
            sb.append(tx(180f, 170f, obj, ST, "middle", true, 12)).append(tx(180f, 187f, "مفعول", MU, "middle", false, 9))
            sb.append(ln(180f, 120f, 180f, 148f, GLD, 1.6f, "5 4"))
        }
        sb.append(tx(180f, 218f, "نهاد + گزاره = جملهٔ کامل", ST, "middle", true, 12))
        sb.append(tx(180f, 240f, if (nominal) "جملهٔ اسمیه: نهاد + مسند (بودن حذف می‌شود)" else "جملهٔ فعلیه: نهاد + فعل (شناسه با نهاد مطابق است)", MU, "middle", false, 10))
        sb.append(tx(180f, 262f, subj + " " + (if (obj.isNotBlank()) obj + " " else "") + verb, ACC, "middle", true, 12))
        return sb.toString()
    }

    private fun vectors(spec: FigureSpec): String {
        val ax1 = spec.xNum("ax1", 3f); val ay1 = spec.xNum("ay1", 1f)
        val bx = spec.xNum("bx", 1f); val by = spec.xNum("by", 2f)
        val diff = spec.xStr("mode", "sum") == "diff"
        val ox = 170f; val oy = 176f; val sc = 26f
        val sb = StringBuilder()
        sb.append(ln(56f, oy, 320f, oy, ST, 1.6f)).append(arrowHeadRight(320f, oy))
        sb.append(ln(ox, 264f, ox, 46f, ST, 1.6f)).append(arrowHeadUp(ox, 46f))
        sb.append(tx(324f, oy + 14f, "x", ST, "middle", true, 11)).append(tx(ox - 12f, 44f, "y", ST, "middle", true, 11))
        for (i in -4..5) {
            if (i == 0) continue
            sb.append(tx(ox + i * sc, oy + 13f, i.toString(), MU, "middle", false, 8))
            sb.append(tx(ox - 10f, oy - i * sc + 3f, i.toString(), MU, "end", false, 8))
        }
        val axx = ox + ax1 * sc; val ayy = oy - ay1 * sc
        val bxx = ox + bx * sc; val byy = oy - by * sc
        val rx = ox + (if (diff) ax1 - bx else ax1 + bx) * sc
        val ry = oy - (if (diff) ay1 - by else ay1 + by) * sc
        sb.append(ln(ox, oy, rx, ry, "rgba(39,196,168,.35)", 8f))
        sb.append(ln(ox, oy, axx, ayy, ACC, 2.4f))
        sb.append(poly("${f(axx)},${f(ayy)} ${f(axx - 9f)},${f(ayy + 4f)} ${f(axx - 5f)},${f(ayy + 10f)}", ACC, "none", 0f))
        sb.append(ln(ox, oy, bxx, byy, GLD, 2.4f))
        sb.append(poly("${f(bxx)},${f(byy)} ${f(bxx - 9f)},${f(byy + 4f)} ${f(bxx - 5f)},${f(byy + 10f)}", GLD, "none", 0f))
        sb.append(ln(ox, oy, rx, ry, PT, 2.6f))
        sb.append(poly("${f(rx)},${f(ry)} ${f(rx - 10f)},${f(ry + 4f)} ${f(rx - 6f)},${f(ry + 11f)}", PT, "none", 0f))
        sb.append(tx(axx + 8f, ayy - 8f, "a", ACC, "start", true, 12))
        sb.append(tx(bxx + 8f, byy - 8f, "b", GLD, "start", true, 12))
        sb.append(tx(rx + 8f, ry - 14f, if (diff) "a − b" else "a + b", PT, "start", true, 12))
        val sx = if (diff) ax1 - bx else ax1 + bx
        val sy = if (diff) ay1 - by else ay1 + by
        sb.append(tx(180f, 252f, (if (diff) "تفاضل" else "جمع") + " بردارها = (" + num(sx) + ", " + num(sy) + ")", ST, "middle", true, 12))
        sb.append(tx(180f, 272f, "اندازهٔ بردار = √(x² + y²)", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun matrixMul(spec: FigureSpec): String {
        val aRows = spec.xStr("a", "1,2;3,4").split(";")
        val bRows = spec.xStr("b", "5,6;7,8").split(";")
        fun get(rows: List<String>, r: Int, c: Int): Float {
            val rr = rows.getOrNull(r)?.split(",", "،") ?: return 0f
            return faEnDigits(rr.getOrNull(c)?.trim() ?: "0").toFloatOrNull() ?: 0f
        }
        val c11 = get(aRows, 0, 0) * get(bRows, 0, 0) + get(aRows, 0, 1) * get(bRows, 1, 0)
        val c12 = get(aRows, 0, 0) * get(bRows, 0, 1) + get(aRows, 0, 1) * get(bRows, 1, 1)
        val c21 = get(aRows, 1, 0) * get(bRows, 0, 0) + get(aRows, 1, 1) * get(bRows, 1, 0)
        val c22 = get(aRows, 1, 0) * get(bRows, 0, 1) + get(aRows, 1, 1) * get(bRows, 1, 1)
        val sb = StringBuilder()
        fun draw(x0: Float, y0: Float, v: Array<Float>, col: String, tag: String) {
            for (r in 0..1) for (c in 0..1) {
                sb.append(tx(x0 + 22f + c * 32f, y0 + 22f + r * 26f, num((v[r * 2 + c] * 100).roundToInt() / 100f), ST, "middle", true, 12))
            }
            sb.append(tx(x0 + 5f, y0 + 22f, "[", col, "middle", true, 26))
            sb.append(tx(x0 + 71f, y0 + 22f, "]", col, "middle", true, 26))
            sb.append(tx(x0 + 38f, y0 + 66f, tag, col, "middle", false, 9))
        }
        draw(28f, 92f, arrayOf(get(aRows, 0, 0), get(aRows, 0, 1), get(aRows, 1, 0), get(aRows, 1, 1)), ACC, "A")
        sb.append(tx(118f, 122f, "×", ST, "middle", true, 16))
        draw(132f, 92f, arrayOf(get(bRows, 0, 0), get(bRows, 0, 1), get(bRows, 1, 0), get(bRows, 1, 1)), GLD, "B")
        sb.append(tx(222f, 122f, "=", ST, "middle", true, 16))
        draw(238f, 92f, arrayOf(c11, c12, c21, c22), PT, "A×B")
        sb.append(tx(180f, 202f, "درایهٔ (۱،۱): " + num(get(aRows, 0, 0)) + "×" + num(get(bRows, 0, 0)) + " + " + num(get(aRows, 0, 1)) + "×" + num(get(bRows, 1, 0)) + " = " + num(c11), ST, "middle", true, 11))
        sb.append(tx(180f, 224f, "ضرب ماتریس: سطر × ستون و جمع حاصل‌ضرب‌ها", MU, "middle", false, 10))
        sb.append(tx(180f, 246f, "مرتبهٔ حاصل = ۲×۲  ·  A دارای ۲ سطر و B دارای ۲ ستون", MU, "middle", false, 10))
        sb.append(tx(180f, 268f, "det(A×B) = det(A) × det(B)", ACC, "middle", true, 10))
        return sb.toString()
    }

    private fun definiteIntegral(spec: FigureSpec): String {
        val fn = spec.xStr("f", "quad").lowercase()
        var lo = spec.xNum("lo", 0f)
        var hi = spec.xNum("hi", 2f)
        if (hi <= lo) hi = lo + 1f
        fun fx(v: Float): Float = when (fn) {
            "sin" -> sin(v)
            "line" -> 0.6f * v + 0.4f
            else -> 0.25f * v * v
        }
        val x0 = 62f; val y0 = 212f; val w = 250f; val h = 140f
        val maxV = max(hi, 1f)
        val maxY = max(fx(maxV), 0.001f)
        fun px(v: Float) = x0 + v / maxV * w
        fun py(v: Float) = y0 - v / maxY * h
        val sb = StringBuilder()
        sb.append(ln(x0 - 8f, y0, x0 + w + 8f, y0, ST, 1.6f)).append(ln(x0 - 8f, y0, x0 - 8f, 56f, ST, 1.6f))
        val shade = StringBuilder()
        val steps = 40
        for (i in 0..steps) {
            val xv = lo + (hi - lo) * i / steps
            shade.append(f(px(xv))).append(',').append(f(y0)).append(' ').append(f(px(xv))).append(',').append(f(py(fx(xv)))).append(' ')
        }
        sb.append("<polygon points=\"$shade${f(px(hi))},${f(y0)}\" fill=\"rgba(39,196,168,.28)\"/>")
        val curve = StringBuilder()
        for (i in 0..80) {
            val xv = maxV * i / 80f
            curve.append(if (i == 0) "M " else " L ").append(f(px(xv))).append(' ').append(f(py(fx(xv))))
        }
        sb.append(path(curve.toString(), "none", ACC, 2.4f))
        sb.append(ln(px(lo), y0, px(lo), py(fx(lo)), PT, 1.6f, "5 4")).append(ln(px(hi), y0, px(hi), py(fx(hi)), PT, 1.6f, "5 4"))
        sb.append(tx(px(lo), y0 + 16f, "a = " + num(lo), PT, "middle", true, 10))
        sb.append(tx(px(hi), y0 + 16f, "b = " + num(hi), PT, "middle", true, 10))
        var sum = 0.0
        val n2 = 200
        for (i in 0 until n2) sum += fx(lo + (hi - lo) * (i + 0.5f) / n2) * (hi - lo) / n2
        sb.append(tx(180f, 238f, "∫ f(x) dx از " + num(lo) + " تا " + num(hi) + " ≈ " + num((sum * 1000).roundToInt() / 1000f), ST, "middle", true, 12))
        sb.append(tx(180f, 258f, "انتگرال معین = مساحت زیر نمودار (با علامت)", MU, "middle", false, 10))
        sb.append(tx(180f, 274f, "روش: تقسیم به مستطیل‌های باریک و جمع مساحت‌ها", MU, "middle", false, 9))
        return sb.toString()
    }

    private fun flowchart(spec: FigureSpec): String {
        var steps = splitList(spec.xStr("steps", "شروع,ورودی: n,n > 0 ؟,چاپ n,پایان"))
        if (steps.isEmpty()) steps = listOf("شروع", "ورودی: n", "n > 0 ؟", "چاپ n", "پایان")
        steps = steps.take(5)
        val sb = StringBuilder()
        var y = 30f
        steps.forEachIndexed { i, label ->
            val isStart = i == 0
            val isEnd = i == steps.size - 1
            val isDec = !isStart && !isEnd && (label.contains("؟") || label.contains("?"))
            val hh = if (isDec) 42f else 24f
            when {
                isStart || isEnd -> sb.append("<rect x=\"112\" y=\"${f(y)}\" width=\"136\" height=\"${f(hh)}\" rx=\"13\" fill=\"" +
                    (if (isStart) "rgba(39,196,168,.25)" else "rgba(214,51,108,.18)") + "\" stroke=\"" +
                    (if (isStart) "#1f8f7a" else PT) + "\" stroke-width=\"1.8\"/>")
                isDec -> sb.append(poly("180,${f(y)} 248,${f(y + hh / 2f)} 180,${f(y + hh)} 112,${f(y + hh / 2f)}", "rgba(240,162,2,.22)", GLD, 1.8f))
                else -> sb.append(rc(112f, y, 136f, hh, "rgba(108,99,245,.16)", ACC, 1.8f, 5f))
            }
            sb.append(tx(180f, y + hh / 2f + 4f, label, ST, "middle", true, 10))
            y += hh
            if (i < steps.size - 1) {
                sb.append(ln(180f, y, 180f, y + 14f, ST, 1.6f))
                sb.append(poly("175.5,${f(y + 8f)} 184.5,${f(y + 8f)} 180,${f(y + 15f)}", ST, "none", 0f))
                y += 15f
            }
        }
        sb.append(tx(180f, 258f, "بیضی = شروع/پایان · مستطیل = پردازش · لوزی = شرط", MU, "middle", true, 10))
        return sb.toString()
    }

    private fun stackQueue(spec: FigureSpec): String {
        val queue = spec.xStr("mode", "stack") == "queue"
        var items = ints(spec, "items", "3,5,8")
        if (items.isEmpty()) items = listOf(3, 5, 8)
        val pop = spec.xStr("op", "push") == "pop"
        val sb = StringBuilder()
        if (queue) {
            sb.append(rc(64f, 88f, 232f, 52f, "none", ST, 2f))
            sb.append(ln(64f, 88f, 64f, 140f, ST, 2f)).append(ln(296f, 88f, 296f, 140f, ST, 2f))
            items.forEachIndexed { i, v ->
                sb.append(rc(74f + i * 56f, 96f, 48f, 36f, "rgba(39,196,168,.20)", "#1f8f7a", 1.6f, 5f))
                sb.append(tx(98f + i * 56f, 120f, v.toString(), ST, "middle", true, 13))
            }
            sb.append(tx(76f, 168f, "خروج (front)", PT, "start", true, 10)).append(ln(88f, 156f, 88f, 140f, PT, 1.6f))
            sb.append(tx(284f, 168f, "ورود (rear)", GRN, "end", true, 10)).append(ln(284f, 156f, 284f, 140f, GRN, 1.6f))
            sb.append(tx(180f, 210f, "صف (Queue) — اولین ورودی، اولین خروجی (FIFO)", ST, "middle", true, 12))
            sb.append(tx(180f, 232f, "افزودن از انتها و حذف از ابتدا", MU, "middle", false, 10))
            return sb.toString()
        }
        sb.append(ln(120f, 76f, 120f, 216f, ST, 2f)).append(ln(240f, 76f, 240f, 216f, ST, 2f)).append(ln(120f, 216f, 240f, 216f, ST, 2f))
        items.forEachIndexed { i, v ->
            val y = 216f - (i + 1) * 34f
            sb.append(rc(122f, y, 116f, 32f, if (i == items.size - 1) "rgba(214,51,108,.22)" else "rgba(108,99,245,.16)",
                if (i == items.size - 1) PT else ACC, 1.6f, 4f))
            sb.append(tx(180f, y + 21f, v.toString(), ST, "middle", true, 13))
        }
        sb.append(tx(272f, 96f, if (pop) "pop → " + items.last() else "push", PT, "start", true, 11))
        sb.append(tx(180f, 244f, "پشته (Stack) — آخرین ورودی، اولین خروجی (LIFO)", ST, "middle", true, 12))
        sb.append(tx(180f, 266f, if (pop) "حذف فقط از بالای پشته ممکن است" else "افزودن فقط به بالای پشته ممکن است", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun bubbleSort(spec: FigureSpec): String {
        var vals = ints(spec, "vals", "5,3,8,1")
        if (vals.isEmpty()) vals = listOf(5, 3, 8, 1)
        val step = spec.xNum("step", 0f).toInt().coerceIn(0, 4)
        val arr = vals.toMutableList()
        for (i in 0 until step) {
            for (j in 0 until max(0, arr.size - 1 - i)) {
                if (arr[j] > arr[j + 1]) { val t = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = t }
            }
        }
        val bw = min(54f, 220f / max(1, arr.size))
        val x0 = 180f - (arr.size * bw) / 2f
        val sb = StringBuilder()
        arr.forEachIndexed { i, v ->
            val h = 26f + abs(v) * 6f
            val active = step == 0 && i < 2
            sb.append(rc(x0 + i * bw + 3f, 200f - h, bw - 6f, h, if (active) "rgba(214,51,108,.22)" else "rgba(108,99,245,.18)", if (active) PT else ACC, 1.8f, 5f))
            sb.append(tx(x0 + i * bw + bw / 2f, 200f - h / 2f + 5f, v.toString(), ST, "middle", true, 14))
        }
        sb.append(ln(x0, 200f, x0 + arr.size * bw, 200f, ST, 2f))
        sb.append(tx(180f, 226f, "مرتب‌سازی حبابی — دور $step از " + max(1, arr.size - 1), ST, "middle", true, 12))
        sb.append(tx(180f, 248f, "در هر دور بزرگ‌ترین عدد به انتها می‌رود", MU, "middle", false, 10))
        sb.append(tx(180f, 268f, "تعداد مقایسه‌ها ≈ n(n−1)/2 = " + (arr.size * (arr.size - 1) / 2), MU, "middle", false, 10))
        return sb.toString()
    }

    private fun regression(spec: FigureSpec): String {
        var pts = pairs(spec.xStr("pts", "1,2;2,3;3,5;4,4;5,6"))
        if (pts.isEmpty()) pts = listOf(1f to 2f, 2f to 3f, 3f to 5f, 4f to 4f, 5f to 6f)
        val n = pts.size
        val mx = pts.sumOf { it.first.toDouble() }.toFloat() / n
        val my = pts.sumOf { it.second.toDouble() }.toFloat() / n
        var sxy = 0f; var sxx = 0f; var syy = 0f
        for ((x, y) in pts) { sxy += (x - mx) * (y - my); sxx += (x - mx) * (x - mx); syy += (y - my) * (y - my) }
        val b = if (sxx == 0f) 0f else sxy / sxx
        val a = my - b * mx
        val r = if (sxx > 0f && syy > 0f) sxy / sqrt(sxx * syy) else 0f
        val x0 = 62f; val y0 = 210f; val w = 250f; val h = 148f
        var maxX = 1f; var maxY = 1f
        for ((x, y) in pts) { maxX = max(maxX, x); maxY = max(maxY, y) }
        val sb = StringBuilder()
        sb.append(ln(x0 - 8f, y0, x0 + w + 8f, y0, ST, 1.6f)).append(ln(x0 - 8f, y0, x0 - 8f, 54f, ST, 1.6f))
        sb.append(ln(x0 + 0f, y0 - a / (maxY * 1.15f) * h, x0 + maxX * 1.15f / (maxX * 1.15f) * w, y0 - (a + b * maxX * 1.15f) / (maxY * 1.15f) * h, ACC, 2.4f))
        for ((x, y) in pts) {
            val px = x0 + x / (maxX * 1.15f) * w
            val py = y0 - y / (maxY * 1.15f) * h
            sb.append(cls(px, py, 4.5f, PT, "#ffffff", 1.2f))
            sb.append(ln(px, py, px, y0 - (a + b * x) / (maxY * 1.15f) * h, GR, 1f, "3 3"))
        }
        sb.append(tx(180f, 232f, "y = " + num((b * 100).roundToInt() / 100f) + "x + " + num((a * 100).roundToInt() / 100f) + "   ·   r = " + num((r * 100).roundToInt() / 100f), ST, "middle", true, 12))
        sb.append(tx(180f, 252f, "رگرسیون کمترین مربعات — خط بهترین برازش", MU, "middle", false, 10))
        sb.append(tx(180f, 272f, "r² = " + num((r * r * 100).roundToInt() / 100f) + "  (نزدیک به ۱ یعنی برازش خوب)", MU, "middle", false, 10))
        return sb.toString()
    }

    // ---------------------------------------------------------------- helpers
    private fun ints(spec: FigureSpec, key: String, def: String): List<Int> =
        spec.xList(key, def).mapNotNull { faEnDigits(it).trim().toIntOrNull() }

    private fun pairs(raw: String): List<Pair<Float, Float>> {
        val out = mutableListOf<Pair<Float, Float>>()
        val re = Regex("(-?\\d+(?:\\.\\d+)?)\\s*[,،]\\s*(-?\\d+(?:\\.\\d+)?)")
        for (m in re.findAll(faEnDigits(raw))) out.add((m.groupValues[1].toFloatOrNull() ?: 0f) to (m.groupValues[2].toFloatOrNull() ?: 0f))
        return out
    }

    private fun num(v: Float): String {
        val r = (v * 100).roundToInt() / 100f
        return if (r == r.roundToInt().toFloat()) r.roundToInt().toString() else r.toString()
    }

    private fun f(v: Float): String = num(v)

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun ln(x1: Float, y1: Float, x2: Float, y2: Float, color: String, w: Float, dash: String = ""): String =
        "<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"$color\" stroke-width=\"${f(w)}\" stroke-linecap=\"round\"" +
            (if (dash.isNotBlank()) " stroke-dasharray=\"$dash\"" else "") + "/>"

    private fun rc(x: Float, y: Float, w: Float, h: Float, fill: String, stroke: String, sw: Float, rx: Float = 0f): String =
        "<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(w)}\" height=\"${f(h)}\"" +
            (if (rx > 0f) " rx=\"${f(rx)}\"" else "") + " fill=\"$fill\"" +
            (if (stroke.isNotBlank() && stroke != "none") " stroke=\"$stroke\" stroke-width=\"${f(sw)}\"" else "") + "/>"

    private fun cls(cx: Float, cy: Float, r: Float, fill: String, stroke: String, sw: Float): String =
        "<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"$fill\"" +
            (if (stroke.isNotBlank() && stroke != "none") " stroke=\"$stroke\" stroke-width=\"${f(sw)}\"" else "") + "/>"

    private fun poly(pts: String, fill: String, stroke: String, sw: Float): String =
        "<polygon points=\"$pts\" fill=\"$fill\"" +
            (if (stroke.isNotBlank() && stroke != "none") " stroke=\"$stroke\" stroke-width=\"${f(sw)}\" stroke-linejoin=\"round\"" else "") + "/>"

    private fun path(d: String, fill: String, stroke: String, sw: Float, dash: String = ""): String =
        "<path d=\"$d\" fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"${f(sw)}\" stroke-linejoin=\"round\" stroke-linecap=\"round\"" +
            (if (dash.isNotBlank()) " stroke-dasharray=\"$dash\"" else "") + "/>"

    private fun arrowHeadRight(x: Float, y: Float): String =
        poly("${f(x)},${f(y)} ${f(x - 8f)},${f(y - 4.5f)} ${f(x - 8f)},${f(y + 4.5f)}", ST, "none", 0f)

    private fun arrowHeadUp(x: Float, y: Float): String =
        poly("${f(x)},${f(y)} ${f(x - 4.5f)},${f(y + 8f)} ${f(x + 4.5f)},${f(y + 8f)}", ST, "none", 0f)

    private fun tx(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean = false, size: Int = 11): String {
        if (s.isBlank()) return ""
        val weight = if (bold) " font-weight=\"700\"" else ""
        return "<text x=\"${f(x)}\" y=\"${f(y)}\" direction=\"ltr\" font-family=\"sans-serif\" font-size=\"$size\"" +
            "$weight fill=\"$color\" text-anchor=\"$anchor\">${esc(FigureDigits.apply(s))}</text>"
    }
}
