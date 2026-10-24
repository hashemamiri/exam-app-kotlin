package ir.exam.app.core.figure

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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

    fun interactiveRegions(spec: FigureSpec): List<AdvancedMathSvgRenderer.InteractiveRegion> {
        val out = mutableListOf<AdvancedMathSvgRenderer.InteractiveRegion>()
        if (spec.type == "axunitconv") unitLadderRender(spec, out)
        return out
    }

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
    private fun fmtClean(v: Double): String {
        val av = abs(v)
        if (av >= 1e-6 && abs(v - kotlin.math.round(v)) < 1e-6) return kotlin.math.round(v).toLong().toString()
        if (av == 0.0) return "0"
        return String.format(java.util.Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
    }

    private fun unitLadder(spec: FigureSpec): String = unitLadderRender(spec, null)

    private fun unitLadderRender(spec: FigureSpec, regions: MutableList<AdvancedMathSvgRenderer.InteractiveRegion>?): String {
        val kind = spec.xStr("kind", "len").lowercase()
        val v = spec.xNum("val", 1f).toDouble()
        val blank = spec.xStr("blank", "0") == "1"
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov")
        val ansMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ans")

        data class UnitChain(val title: String, val units: List<String>, val factors: List<Int>)
        val chain = when (kind) {
            "mass" -> UnitChain("جرم", listOf("t", "kg", "g", "mg"), listOf(1000, 1000, 1000))
            "area" -> UnitChain("مساحت", listOf("m²", "dm²", "cm²", "mm²"), listOf(100, 100, 100))
            "vol" -> UnitChain("حجم", listOf("m³", "L (dm³)", "mL (cm³)"), listOf(1000, 1000))
            "time" -> UnitChain("زمان", listOf("ساعت", "دقیقه", "ثانیه"), listOf(60, 60))
            else -> UnitChain("طول", listOf("km", "m", "cm", "mm"), listOf(1000, 100, 10))
        }

        val n = chain.units.size
        val vals = ArrayList<Double>(n)
        var cur = v
        vals.add(cur)
        for (fac in chain.factors) {
            cur *= fac
            vals.add(cur)
        }

        val sb = StringBuilder()
        sb.append(tx(180f, 22f, "نردبان تبدیل واحد ${chain.title}", "#1e293b", "middle", true, 12))

        val cardW = if (n == 4) 74f else 94f
        val cardH = 62f
        val gap = if (n == 4) 11f else 20f
        val totalW = n * cardW + (n - 1) * gap
        val x0 = (360f - totalW) / 2f
        val yCard = 104f
        val cardTints = listOf("#eef2ff", "#ecfdf5", "#fffbeb", "#fdf2f8")
        val cardBrds = listOf(ACC, "#0d9488", "#d97706", PT)

        // کمان‌های ضرب (بالا) و تقسیم (پایین) بین پله‌های مجاور
        for (i in 0 until n - 1) {
            val xA = x0 + i * (cardW + gap) + cardW * 0.72f
            val xB = x0 + (i + 1) * (cardW + gap) + cardW * 0.28f
            val xm = (xA + xB) / 2f
            val fac = chain.factors[i]
            // پیکان بالایی: ضرب (از واحد بزرگ به کوچک)
            sb.append(path("M ${f(xA)} ${f(yCard - 4f)} Q ${f(xm)} ${f(yCard - 38f)} ${f(xB)} ${f(yCard - 4f)}", "none", ACC, 1.7f))
            sb.append(poly("${f(xB)},${f(yCard - 4f)} ${f(xB - 6f)},${f(yCard - 10f)} ${f(xB - 1.5f)},${f(yCard - 12f)}", ACC, "none", 0f))
            sb.append(rc(xm - 21f, yCard - 42f, 42f, 16f, "#ffffff", ACC, 1f, 5f))
            sb.append(tx(xm, yCard - 31f, "×$fac", ACC, "middle", true, 9))

            // پیکان پایینی: تقسیم (از واحد کوچک به بزرگ)
            val yBot = yCard + cardH
            sb.append(path("M ${f(xB)} ${f(yBot + 4f)} Q ${f(xm)} ${f(yBot + 38f)} ${f(xA)} ${f(yBot + 4f)}", "none", "#0f766e", 1.6f, "4 2.5"))
            sb.append(poly("${f(xA)},${f(yBot + 4f)} ${f(xA + 6f)},${f(yBot + 10f)} ${f(xA + 1.5f)},${f(yBot + 12f)}", "#0f766e", "none", 0f))
            sb.append(rc(xm - 21f, yBot + 26f, 42f, 16f, "#ffffff", "#0f766e", 1f, 5f))
            sb.append(tx(xm, yBot + 37f, "÷$fac", "#0f766e", "middle", true, 9))
        }

        // کارت‌های مقدار و واحد (هر خانه قابل کلیک برای ویرایش یا خالی‌کردن بدون علامت سؤال!)
        for (i in 0 until n) {
            val cx = x0 + i * (cardW + gap)
            val key = "u$i"
            val autoEmpty = blank && i > 0
            val isEmpty = key in emptySet || autoEmpty
            val defVal = fmtClean(vals[i])
            regions?.add(AdvancedMathSvgRenderer.InteractiveRegion(key, "مقدار بر حسب ${chain.units[i]}", defVal, cx, yCard, cardW, cardH, defaultEmpty = autoEmpty))

            sb.append(rc(cx, yCard, cardW, cardH, if (isEmpty) "#ffffff" else cardTints[i % cardTints.size], if (isEmpty) "#4f46e5" else cardBrds[i % cardBrds.size], if (isEmpty) 1.9f else 1.7f, 8f))
            // نوار واحد در پایین کارت
            sb.append(rc(cx + 3f, yCard + cardH - 20f, cardW - 6f, 17f, "rgba(15,23,42,.06)", "none", 0f, 5f))
            sb.append(tx(cx + cardW / 2f, yCard + cardH - 8f, chain.units[i], MU, "middle", true, 10))

            if (isEmpty) {
                sb.append(rcDash(cx + 5f, yCard + 5f, cardW - 10f, 32f, "#f8fafc", "#4f46e5", 1.4f, 5f, "4 2.5"))
                val stu = ansMap[key]?.trim().orEmpty()
                if (stu.isNotEmpty()) {
                    sb.append(tx(cx + cardW / 2f, yCard + 26f, stu, "#1d4ed8", "middle", true, if (stu.length > 6) 10 else 12))
                }
            } else {
                val disp = ovMap[key]?.trim() ?: defVal
                sb.append(tx(cx + cardW / 2f, yCard + 26f, disp, ST, "middle", true, if (disp.length > 6) 10 else 12))
            }
        }

        sb.append(tx(180f, 256f, "از راست/چپ: ضرب در حرکت به واحد کوچک‌تر، تقسیم به واحد بزرگ‌تر", MU, "middle", false, 10))
        return sb.toString()
    }

    private fun perimeterArea(spec: FigureSpec): String {
        val shape = spec.xStr("shape", "rect").lowercase()
        val show = spec.xStr("show", "both").lowercase() // both | perim | area | none
        val w = max(1f, spec.xNum("w", 6f))
        val h = max(1f, spec.xNum("h", 4f))
        val aTop = max(1f, spec.xNum("a", max(1f, w - 2f)))
        val sb = StringBuilder()

        var perStr = ""
        var areaStr = ""

        when (shape) {
            "square" -> {
                val sPx = 124f
                val rx = 180f - sPx / 2f
                val ry = 42f
                sb.append(rc(rx, ry, sPx, sPx, "rgba(108,99,245,.13)", ACC, 2.2f, 3f))
                sb.append(rc(rx, ry + sPx - 11f, 11f, 11f, "none", ACC, 1.3f))
                // تیک تساوی ۴ ضلع
                sb.append(ln(180f, ry - 4f, 180f, ry + 4f, PT, 1.8f))
                sb.append(ln(180f, ry + sPx - 4f, 180f, ry + sPx + 4f, PT, 1.8f))
                sb.append(ln(rx - 4f, ry + sPx / 2f, rx + 4f, ry + sPx / 2f, PT, 1.8f))
                sb.append(ln(rx + sPx - 4f, ry + sPx / 2f, rx + sPx + 4f, ry + sPx / 2f, PT, 1.8f))
                sb.append(tx(180f, ry - 10f, "a = ${num(w)}", ST, "middle", true, 12))
                perStr = "P = 4a = ${num(4f * w)}"
                areaStr = "S = a² = ${num(w * w)}"
            }
            "circle" -> {
                val rPx = min(72f, max(36f, w * 11f))
                val cx = 180f; val cy = 112f
                sb.append(cls(cx, cy, rPx, "rgba(39,196,168,.15)", GRN, 2.2f))
                sb.append(cls(cx, cy, 3.2f, ST, "none", 0f))
                sb.append(ln(cx, cy, cx + rPx, cy, PT, 2f))
                sb.append(tx(cx + rPx / 2f, cy - 8f, "r = ${num(w)}", PT, "middle", true, 11))
                perStr = "P = 2πr ≈ ${num((2 * PI * w).toFloat())}"
                areaStr = "S = πr² ≈ ${num((PI * w * w).toFloat())}"
            }
            "tri" -> {
                val scale = min(180f / w, 120f / h)
                val bw = max(60f, w * scale)
                val bh = max(50f, h * scale)
                val x0 = 180f - bw / 2f
                val y0 = 176f
                sb.append(poly("${f(x0)},${f(y0)} ${f(x0 + bw)},${f(y0)} ${f(x0)},${f(y0 - bh)}", "rgba(240,162,2,.15)", GLD, 2.2f))
                sb.append(rc(x0, y0 - 11f, 11f, 11f, "none", ST, 1.3f))
                val hyp = sqrt(w * w + h * h)
                sb.append(tx(x0 + bw / 2f, y0 + 18f, "a = ${num(w)}", ST, "middle", true, 11))
                sb.append(tx(x0 - 10f, y0 - bh / 2f, "b = ${num(h)}", ST, "end", true, 11))
                sb.append(tx(x0 + bw / 2f + 14f, y0 - bh / 2f - 4f, "c ≈ ${num(hyp)}", PT, "start", true, 11))
                perStr = "P = ${num(w + h + hyp)}"
                areaStr = "S = ½ab = ${num(w * h / 2f)}"
            }
            "para" -> {
                val bw = 156f; val bh = 96f; val skew = 34f
                val x0 = 96f; val y0 = 172f
                sb.append(poly("${f(x0)},${f(y0)} ${f(x0 + bw)},${f(y0)} ${f(x0 + bw + skew)},${f(y0 - bh)} ${f(x0 + skew)},${f(y0 - bh)}", "rgba(108,99,245,.13)", ACC, 2.2f))
                sb.append(ln(x0 + skew, y0 - bh, x0 + skew, y0, PT, 1.6f, "4 3"))
                sb.append(rc(x0 + skew, y0 - 10f, 10f, 10f, "none", PT, 1.2f))
                val side = hypot(h, max(1f, w * 0.35f)).let { kotlin.math.round(it * 10f) / 10f }
                sb.append(tx(x0 + bw / 2f, y0 + 18f, "قاعده b = ${num(w)}", ST, "middle", true, 11))
                sb.append(tx(x0 + skew + 6f, y0 - bh / 2f, "h = ${num(h)}", PT, "start", true, 11))
                perStr = "P ≈ ${num(2f * (w + side))}"
                areaStr = "S = b×h = ${num(w * h)}"
            }
            "trap" -> {
                val bBig = max(w, aTop + 1f)
                val bSmall = min(w, aTop)
                val wPx = 176f
                val topPx = (bSmall / bBig * wPx).coerceIn(64f, 148f)
                val hPx = 98f
                val x0 = 180f - wPx / 2f
                val xTop = 180f - topPx / 2f
                val y0 = 172f
                sb.append(poly("${f(x0)},${f(y0)} ${f(x0 + wPx)},${f(y0)} ${f(xTop + topPx)},${f(y0 - hPx)} ${f(xTop)},${f(y0 - hPx)}", "rgba(39,196,168,.15)", "#0d9488", 2.2f))
                sb.append(ln(xTop, y0 - hPx, xTop, y0, PT, 1.6f, "4 3"))
                sb.append(rc(xTop, y0 - 10f, 10f, 10f, "none", PT, 1.2f))
                val leg = sqrt(((bBig - bSmall) / 2f) * ((bBig - bSmall) / 2f) + h * h)
                sb.append(tx(180f, y0 - hPx - 8f, "a = ${num(bSmall)}", ST, "middle", true, 11))
                sb.append(tx(180f, y0 + 18f, "b = ${num(bBig)}", ST, "middle", true, 11))
                sb.append(tx(xTop + 6f, y0 - hPx / 2f, "h = ${num(h)}", PT, "start", true, 11))
                perStr = "P ≈ ${num(bBig + bSmall + 2f * leg)}"
                areaStr = "S = ½(a+b)h = ${num((bBig + bSmall) * h / 2f)}"
            }
            "rhomb" -> {
                val d1Px = 164f; val d2Px = 106f
                val cx = 180f; val cy = 112f
                sb.append(poly("${f(cx)},${f(cy - d2Px / 2f)} ${f(cx + d1Px / 2f)},${f(cy)} ${f(cx)},${f(cy + d2Px / 2f)} ${f(cx - d1Px / 2f)},${f(cy)}", "rgba(214,51,108,.12)", PT, 2.2f))
                sb.append(ln(cx - d1Px / 2f, cy, cx + d1Px / 2f, cy, ACC, 1.5f, "4 3"))
                sb.append(ln(cx, cy - d2Px / 2f, cx, cy + d2Px / 2f, "#0d9488", 1.5f, "4 3"))
                val side = sqrt((w / 2f) * (w / 2f) + (h / 2f) * (h / 2f))
                sb.append(tx(cx + d1Px / 4f, cy - 6f, "d₁=${num(w)}", ACC, "middle", true, 10))
                sb.append(tx(cx + 6f, cy + d2Px / 4f + 4f, "d₂=${num(h)}", "#0d9488", "start", true, 10))
                perStr = "P = 4a ≈ ${num(4f * side)}"
                areaStr = "S = ½d₁d₂ = ${num(w * h / 2f)}"
            }
            "poly" -> {
                val n = spec.xNum("n", 5f).toInt().coerceIn(3, 10)
                val rPx = 68f; val cx = 180f; val cy = 110f
                val pts = (0 until n).joinToString(" ") { i ->
                    val a = -PI / 2 + 2 * PI * i / n
                    "${f(cx + (rPx * cos(a)).toFloat())},${f(cy + (rPx * sin(a)).toFloat())}"
                }
                sb.append(poly(pts, "rgba(108,99,245,.13)", ACC, 2.2f))
                val apothem = (w / (2.0 * kotlin.math.tan(PI / n))).toFloat()
                sb.append(tx(cx, cy + rPx + 18f, "$n ضلعی منتظم — ضلع a = ${num(w)}", ST, "middle", true, 11))
                perStr = "P = ${n}a = ${num(n * w)}"
                areaStr = "S ≈ ${num(n * w * apothem / 2f)}"
            }
            else -> {
                val scale = min(204f / w, 122f / h)
                val rw = max(72f, w * scale)
                val rh = max(48f, h * scale)
                val rx = 180f - rw / 2f
                val ry = 108f - rh / 2f
                sb.append(rc(rx, ry, rw, rh, "rgba(108,99,245,.13)", ACC, 2.2f, 3f))
                sb.append(rc(rx, ry + rh - 11f, 11f, 11f, "none", ACC, 1.3f))
                sb.append(tx(180f, ry - 9f, "طول = ${num(w)}", ST, "middle", true, 12))
                sb.append(tx(rx + rw + 10f, ry + rh / 2f + 4f, "عرض = ${num(h)}", ST, "start", true, 12))
                perStr = "محیط P = ${num(2f * (w + h))}"
                areaStr = "مساحت S = ${num(w * h)}"
            }
        }

        // نمایش انتخابی زیر شکل: هر دو (both)، فقط محیط (perim)، فقط مساحت (area)، یا هیچ‌کدام (none)
        val footer = when (show) {
            "none", "0" -> ""
            "perim", "p" -> perStr
            "area", "s" -> areaStr
            else -> "$perStr   |   $areaStr"
        }
        if (footer.isNotEmpty()) {
            sb.append(rc(22f, 232f, 316f, 32f, "#eef2ff", ACC, 1.5f, 8f))
            sb.append(tx(180f, 253f, footer, "#1e1b4b", "middle", true, 12))
        }
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
        val nVal = spec.xNum("n", 147f).toDouble()
        val mode = spec.xStr("mode", "ten").lowercase()
        val labelMode = spec.xStr("label", "1")
        val step = when (mode) {
            "thousand" -> 1000.0
            "hundred" -> 100.0
            "one" -> 1.0
            "tenth" -> 0.1
            "hundredth" -> 0.01
            else -> 10.0
        }
        val modeFa = when (mode) {
            "thousand" -> "هزارگان"
            "hundred" -> "صدگان"
            "one" -> "یکان"
            "tenth" -> "دهم"
            "hundredth" -> "صدم"
            else -> "دهگان"
        }

        val lo = kotlin.math.floor(nVal / step + 1e-9) * step
        val hi = lo + step
        val mid = lo + step / 2.0
        val roundsUp = nVal >= mid - 1e-9
        val ans = if (roundsUp) hi else lo

        val xL = 44f
        val xR = 316f
        val yLine = 136f
        fun px(v: Double): Float = (xL + ((v - lo) / (hi - lo)).coerceIn(0.0, 1.0) * (xR - xL)).toFloat()

        val sb = StringBuilder()
        sb.append(tx(180f, 26f, "گرد کردن عدد ${fmtClean(nVal)} به تقریب $modeFa", "#1e293b", "middle", true, 12))

        // دو ناحیهٔ رنگی (کمتر از وسط = گرد به پایین، از وسط به بعد = گرد به بالا)
        val xMid = (xL + xR) / 2f
        sb.append(rc(xL, yLine - 16f, xMid - xL, 16f, "rgba(148,163,184,.16)", "none", 0f, 3f))
        sb.append(rc(xMid, yLine - 16f, xR - xMid, 16f, "rgba(39,196,168,.18)", "none", 0f, 3f))

        // محور مدرج ۱۰ قسمتی + نقطهٔ وسط (mid)
        sb.append(ln(xL - 12f, yLine, xR + 12f, yLine, ST, 2.2f))
        for (i in 0..10) {
            val tx0 = xL + i * (xR - xL) / 10f
            val isEnd = i == 0 || i == 10
            val isMid = i == 5
            val hTick = when { isEnd -> 12f; isMid -> 9f; else -> 5f }
            sb.append(ln(tx0, yLine - hTick, tx0, yLine + hTick, if (isMid) "#d97706" else ST, if (isEnd || isMid) 2f else 1.1f))
        }
        sb.append(tx(xL, yLine + 28f, fmtClean(lo), ST, "middle", true, 12))
        sb.append(tx(xMid, yLine + 26f, "وسط (${fmtClean(mid)})", "#b45309", "middle", true, 10))
        sb.append(tx(xR, yLine + 28f, fmtClean(hi), ST, "middle", true, 12))

        // نقطهٔ عدد مورد نظر روی محور + کمان جهت گرد شدن
        val nx = px(nVal)
        val txAns = if (roundsUp) xR else xL
        if (labelMode == "1" && abs(nx - txAns) > 6f) {
            val qx = (nx + txAns) / 2f
            sb.append(path("M ${f(nx)} ${f(yLine - 12f)} Q ${f(qx)} ${f(yLine - 58f)} ${f(txAns)} ${f(yLine - 14f)}", "none", "#0d9488", 2f, "5 3"))
            sb.append(cls(txAns, yLine, 6.5f, "#10b981", "#ffffff", 1.6f))
        }
        sb.append(cls(nx, yLine, 6.5f, PT, "#ffffff", 1.8f))
        sb.append(rc(nx - 24f, yLine - 44f, 48f, 20f, "#fff1f2", PT, 1.3f, 5f))
        sb.append(tx(nx, yLine - 30f, fmtClean(nVal), PT, "middle", true, 11))

        if (labelMode != "0") {
            val ask = labelMode == "2"
            sb.append(rc(28f, 224f, 304f, 34f, if (ask) "#fffbeb" else "#ecfdf5", if (ask) GLD else "#10b981", 1.6f, 8f))
            val btm = if (ask) "${fmtClean(nVal)}  ≈  ؟   (گرد به $modeFa)" else "${fmtClean(nVal)}  ≈  ${fmtClean(ans)}   (گرد به $modeFa)"
            sb.append(tx(180f, 246f, btm, if (ask) "#92400e" else "#065f46", "middle", true, 13))
        }
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

    /**
     * V326 — بازنویسی «اجزای دایره».
     * پیش از این وتر، مماس و کمان از مختصات دستیِ ثابت استفاده می‌کردند و سرشان روی محیط
     * نمی‌نشست (وتر ۷ پیکسل بیرون/درون می‌افتاد) و برچسب‌ها روی محیط نوشته می‌شدند.
     * اکنون همهٔ اجزا از روی همان شعاع محاسبه می‌شوند و برچسب‌ها بیرون دایره می‌نشینند.
     */
    private fun circleParts(spec: FigureSpec): String {
        val show = spec.xStr("show", "all")
        fun on(k: String) = show == "all" || show == k
        val cx = 180f; val cy = 118f; val r = 74f
        fun pt(deg: Double): Pair<Float, Float> {
            val a = Math.toRadians(deg)
            return (cx + r * kotlin.math.cos(a).toFloat()) to (cy + r * kotlin.math.sin(a).toFloat())
        }
        /** برچسب بیرون دایره: گزینه‌های چیدمان تا روی محیط/خط نیفتد */
        fun outLabel(deg: Double, text: String, color: String, gap: Float = 15f): String {
            val a = Math.toRadians(deg)
            val ux = kotlin.math.cos(a).toFloat(); val uy = kotlin.math.sin(a).toFloat()
            val lx = cx + ux * (r + gap)
            val ly = cy + uy * (r + gap) + (if (uy > 0.4f) 11f else if (uy < -0.4f) -5f else 4f)
            val anchor = if (ux > 0.3f) "start" else if (ux < -0.3f) "end" else "middle"
            return tx(lx, ly, text, color, anchor, true, 11)
        }
        val sb = StringBuilder()
        sb.append(cls(cx, cy, r, "rgba(108,99,245,.05)", ST, 2.2f))
        if (on("radius")) {
            val p = pt(-42.0)
            sb.append(ln(cx, cy, p.first, p.second, PT, 2.2f)).append(cls(p.first, p.second, 3.6f, PT, "none", 0f))
            sb.append(outLabel(-42.0, "شعاع r", PT))
        }
        if (on("diameter")) {
            val a1 = pt(180.0); val a2 = pt(0.0)
            sb.append(ln(a1.first, a1.second, a2.first, a2.second, "#1f8f7a", 2.2f))
            sb.append(tx(cx - 34f, cy - 9f, "قطر 2r", "#1f8f7a", "middle", true, 11))
        }
        if (on("chord")) {
            val c1 = pt(32.0); val c2b = pt(148.0)
            sb.append(ln(c1.first, c1.second, c2b.first, c2b.second, GLD, 2.2f))
            sb.append(cls(c1.first, c1.second, 3.4f, GLD, "none", 0f)).append(cls(c2b.first, c2b.second, 3.4f, GLD, "none", 0f))
            sb.append(tx(cx + 6f, (c1.second + c2b.second) / 2f - 8f, "وتر", GLD, "start", true, 11))
        }
        if (on("tangent")) {
            sb.append(ln(cx - 96f, cy + r, cx + 96f, cy + r, "#8e44ad", 2.2f, "6 4"))
            sb.append(cls(cx, cy + r, 3.4f, "#8e44ad", "none", 0f))
            sb.append(tx(cx + 100f, cy + r + 5f, "مماس", "#8e44ad", "start", true, 11))
        }
        if (on("arc")) {
            val a0 = Math.toRadians(196.0); val a1 = Math.toRadians(250.0)
            val p0x = cx + r * kotlin.math.cos(a0).toFloat(); val p0y = cy + r * kotlin.math.sin(a0).toFloat()
            val p1x = cx + r * kotlin.math.cos(a1).toFloat(); val p1y = cy + r * kotlin.math.sin(a1).toFloat()
            sb.append(path("M ${f(p0x)} ${f(p0y)} A ${f(r)} ${f(r)} 0 0 1 ${f(p1x)} ${f(p1y)}", "none", PT, 4f))
            sb.append(outLabel(223.0, "کمان", PT))
        }
        sb.append(cls(cx, cy, 3.6f, ST, "none", 0f)).append(tx(cx - 9f, cy + 15f, "O", ST, "middle", true, 11))
        sb.append(tx(180f, 258f, "اجزای دایره — مرکز، شعاع، قطر، وتر، مماس و کمان", MU, "middle", true, 11))
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

    private fun formatLinearEq(m: Float, b: Float): String {
        val mZero = abs(m) < 1e-4f
        val bZero = abs(b) < 1e-4f
        if (mZero) return "y = ${num(b)}"
        val mTerm = when {
            abs(m - 1f) < 1e-4f -> "x"
            abs(m + 1f) < 1e-4f -> "−x"
            m < 0f -> "−${num(abs(m))}x"
            else -> "${num(m)}x"
        }
        if (bZero) return "y = $mTerm"
        val bPart = if (b > 0f) " + ${num(b)}" else " − ${num(abs(b))}"
        return "y = $mTerm$bPart"
    }

    private fun linearSystem(spec: FigureSpec): String {
        val m1 = spec.xNum("m1", 1f); val b1 = spec.xNum("b1", 2f)
        val m2 = spec.xNum("m2", -1f); val b2 = spec.xNum("b2", 4f)
        val show = spec.xStr("show", "all").lowercase() // all | eq | none
        val sb = StringBuilder()

        // ۱) نوار معادله‌ها کاملاً بیرون از صفحهٔ مختصات (بالای شبکه: y = 6..36)
        if (show != "none") {
            sb.append(rc(18f, 6f, 324f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(ln(28f, 21f, 44f, 21f, ACC, 2.5f))
            sb.append(tx(50f, 25f, formatLinearEq(m1, b1), ACC, "start", true, 11))
            sb.append(ln(188f, 21f, 204f, 21f, "#0d9488", 2.5f))
            sb.append(tx(210f, 25f, formatLinearEq(m2, b2), "#0d9488", "start", true, 11))
        }

        // ۲) صفحهٔ مختصات تمیز و بدون هیچ متن اضافی روی شبکه (y = 44..244)
        val ox = 186f; val oy = 144f; val sc = 16.5f
        for (i in -7..7) {
            val gx = ox + i * sc
            if (gx in 52f..320f) sb.append(ln(gx, 44f, gx, 244f, "#eef1f6", 1f))
        }
        for (j in -6..6) {
            val gy = oy - j * sc
            if (gy in 44f..244f) sb.append(ln(52f, gy, 320f, gy, "#eef1f6", 1f))
        }
        sb.append(rc(52f, 44f, 268f, 200f, "none", "#cbd5e1", 1.1f))
        sb.append(ln(52f, oy, 320f, oy, ST, 1.5f))
        sb.append(ln(ox, 44f, ox, 244f, ST, 1.5f))
        sb.append(arrowHeadRight(320f, oy)); sb.append(arrowHeadUp(ox, 44f))

        fun px(v: Float) = ox + v * sc
        fun py(v: Float) = oy - v * sc
        // V322 — شیب/عرض از مبدأ بزرگ خط را با y ده‌ها هزار بیرون از بوم می‌برد؛ به کادر محور مهار می‌شود.
        fun pyc(v: Float) = py(v).coerceIn(44f, 254f)
        fun pxc(v: Float) = px(v).coerceIn(46f, 326f)

        fun drawClippedLine(m: Float, b: Float, col: String, dash: String = "") {
            val pts = mutableListOf<Pair<Float, Float>>()
            val xMin = -8.12f; val xMax = 8.12f
            val yMin = -6.06f; val yMax = 6.06f
            val yAtMin = m * xMin + b
            if (yAtMin in yMin..yMax) pts += xMin to yAtMin
            val yAtMax = m * xMax + b
            if (yAtMax in yMin..yMax) pts += xMax to yAtMax
            if (abs(m) > 1e-4f) {
                val xAtBot = (yMin - b) / m
                if (xAtBot in xMin..xMax) pts += xAtBot to yMin
                val xAtTop = (yMax - b) / m
                if (xAtTop in xMin..xMax) pts += xAtTop to yMax
            }
            val uniq = pts.distinctBy { (kotlin.math.round(it.first * 20f) to kotlin.math.round(it.second * 20f)) }
            if (uniq.size >= 2) {
                val a = uniq.first(); val z = uniq.last()
                sb.append(ln(pxc(a.first), pyc(a.second), pxc(z.first), pyc(z.second), col, 2.3f, dash))
            } else {
                sb.append(ln(pxc(-7f), pyc(m * -7f + b), pxc(7f), pyc(m * 7f + b), col, 2.3f, dash))
            }
        }

        val sameSlope = abs(m1 - m2) < 1e-3f
        val sameIntercept = abs(b1 - b2) < 1e-3f
        drawClippedLine(m1, b1, ACC)
        drawClippedLine(m2, b2, "#0d9488", if (sameSlope && sameIntercept) "6 4" else "")

        var statusTxt = ""
        var statusCol = "#1e293b"
        if (!sameSlope) {
            val ix = (b2 - b1) / (m1 - m2)
            val iy = m1 * ix + b1
            if (px(ix) in 52f..320f && py(iy) in 44f..244f) {
                sb.append(cls(px(ix), py(iy), 5f, PT, "#ffffff", 1.6f))
            }
            statusTxt = "جواب دستگاه (نقطهٔ برخورد):  (${num(ix)}, ${num(iy)})"
            statusCol = PT
        } else if (sameIntercept) {
            statusTxt = "دو خط منطبق‌اند — دستگاه بی‌شمار جواب دارد"
            statusCol = "#0f766e"
        } else {
            statusTxt = "دو خط موازی‌اند — دستگاه جواب ندارد"
            statusCol = "#b45309"
        }

        // ۳) نوار جواب در پایین شبکه (بیرون از صفحهٔ مختصات: y = 250..274)
        if (show == "all") {
            sb.append(rc(24f, 250f, 312f, 24f, "#f8fafc", "#cbd5e1", 1.1f, 6f))
            sb.append(tx(180f, 266f, statusTxt, statusCol, "middle", true, 11))
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------- متوسطه و علوم
    private fun barStats(spec: FigureSpec): String {
        var data = ints(spec, "data", "4,7,3,6,5").take(10)
        if (data.isEmpty()) data = listOf(4, 7, 3, 6, 5)
        // V322 — دادهٔ منفی ارتفاع منفی (<rect height="-...">) می‌ساخت؛ مقادیر به [0, 999] مهار می‌شوند.
        data = data.map { it.coerceIn(0, 999) }
        val labs = spec.xList("labs", "الف,ب,ج,د,ه")
        val show = spec.xStr("show", "mean").lowercase() // mean | median | mode | all | none

        val rawMax = (data.maxOrNull() ?: 1).coerceAtLeast(1)
        val niceMax = when {
            rawMax <= 5 -> 5f
            rawMax <= 10 -> 10f
            else -> (kotlin.math.ceil(rawMax / 5.0) * 5.0).toFloat()
        }
        val x0 = 50f; val y0 = 216f; val w = 282f; val h = 166f
        val bw = w / data.size * 0.62f
        val sb = StringBuilder()

        // خطوط افقی شبکه و اعداد محور عمودی
        for (t in 0..4) {
            val tv = niceMax * t / 4f
            val ty = y0 - (tv / niceMax) * h
            sb.append(ln(x0, ty, x0 + w, ty, if (t == 0) ST else "#e2e8f0", if (t == 0) 1.7f else 1f))
            sb.append(tx(x0 - 6f, ty + 4f, num(tv), MU, "end", false, 9))
        }
        sb.append(ln(x0, y0 - h - 8f, x0, y0, ST, 1.7f))

        val cols = listOf(ACC, GRN, GLD, PT, "#4c9be8", "#9b5de5", "#0d9488", "#ea580c", "#2563eb", "#be185d")
        for (i in data.indices) {
            val bh = data[i] / niceMax * h
            val bx = x0 + (i + 0.5f) * (w / data.size) - bw / 2f
            sb.append(rc(bx, y0 - bh, bw, bh, cols[i % cols.size], ST, 1.2f, 3f))
            sb.append(tx(bx + bw / 2f, y0 - bh - 5f, data[i].toString(), ST, "middle", true, 10))
            sb.append(tx(bx + bw / 2f, y0 + 16f, labs.getOrElse(i) { (i + 1).toString() }, MU, "middle", true, 10))
        }

        val mean = data.average().toFloat()
        val sorted = data.sorted()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2].toFloat()
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        val rangeVal = (sorted.last() - sorted.first())
        val freqMap = data.groupingBy { it }.eachCount()
        val maxFreq = freqMap.values.maxOrNull() ?: 1
        val modes = freqMap.filterValues { it == maxFreq }.keys.sorted()
        val modeStr = if (maxFreq <= 1 && data.size > 1) "ندارد" else modes.joinToString("، ")
        val variance = data.map { (it - mean) * (it - mean) }.average().toFloat()
        val sd = sqrt(variance)

        if (show == "mean" || show == "all") {
            val my = y0 - mean / niceMax * h
            sb.append(ln(x0, my, x0 + w, my, PT, 1.9f, "6 4"))
        }
        if (show == "median" || show == "all") {
            val mdy = y0 - median / niceMax * h
            sb.append(ln(x0, mdy, x0 + w, mdy, "#0d9488", 1.8f, "3 3"))
        }

        if (show != "none") {
            val summary = when (show) {
                "median" -> "میانه = ${num(median)}"
                "mode" -> "مد (نما) = $modeStr   |   دامنهٔ تغییرات R = $rangeVal"
                "all" -> "میانگین=${num(mean)} ، میانه=${num(median)} ، مد=$modeStr ، σ=${num(sd)}"
                else -> "میانگین (x̄) = ${num(mean)}"
            }
            sb.append(rc(18f, 240f, 324f, 28f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 258f, summary, "#1e293b", "middle", true, 11))
        }
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
        // V322 — با b بزرگ، محور تقارن با مختصات هزاران پیکسل بیرون از بوم رسم می‌شد.
        if (abs(vx) <= 6.2f) sb.append(ln(ox + vx * sc, 258f, ox + vx * sc, 34f, GLD, 1.4f, "5 4"))
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

    private fun vecArrow(x1: Float, y1: Float, x2: Float, y2: Float, color: String, w: Float = 2.2f, head: Float = 7.5f, dash: String = ""): String {
        val an = atan2(y2 - y1, x2 - x1)
        val pts = "${f(x2)},${f(y2)} ${f(x2 - head * cos(an - 0.44f))},${f(y2 - head * sin(an - 0.44f))} ${f(x2 - head * cos(an + 0.44f))},${f(y2 - head * sin(an + 0.44f))}"
        return ln(x1, y1, x2, y2, color, w, dash) + poly(pts, color, "none", 0f)
    }

    private fun pendulum(spec: FigureSpec): String {
        val lenM = max(0.1f, spec.xNum("len", 1f))
        val amp = spec.xNum("amp", 24f).coerceIn(5f, 65f)
        val gVal = max(1f, spec.xNum("g", 9.8f))
        val mode = spec.xStr("mode", "period").lowercase() // period | forces | energy | quiz

        val px = 180f
        val py = 36f
        val plen = 148f
        val rad = amp * PI / 180.0
        val bx = px + (plen * sin(rad)).toFloat()
        val by = py + (plen * cos(rad)).toFloat()
        val lx = px - (plen * sin(rad)).toFloat()
        val ly = by
        val eqY = py + plen

        val sb = StringBuilder()
        // سقف هاشورخوردهٔ مکانیکی و لولای آونگ
        sb.append(ln(116f, py, 244f, py, ST, 3f))
        for (hx in 122..238 step 14) {
            sb.append(ln(hx.toFloat(), py, hx - 7f, py - 9f, "#64748b", 1.3f))
        }
        sb.append(cls(px, py, 4.5f, ST, "#ffffff", 1.3f))

        // خط قائم تعادل + مسیر کمان واقعی حرکت گلوله حول نقطهٔ تعادل (px, py+plen)
        sb.append(ln(px, py, px, eqY + 14f, "#94a3b8", 1.4f, "5 4"))
        sb.append(path("M ${f(lx)} ${f(ly)} A ${f(plen)} ${f(plen)} 0 0 0 ${f(bx)} ${f(by)}", "none", ACC, 1.7f, "5 3"))
        sb.append(cls(lx, ly, 10f, "rgba(148,163,184,.18)", "#94a3b8", 1.2f))
        sb.append(cls(px, eqY, 10f, "rgba(39,196,168,.15)", GRN, 1.2f))

        // کمان زاویهٔ انحراف θ در بالای نخ (شعاع ۴۲ از نقطهٔ آویز)
        val rAng = 42f
        val ax = px + (rAng * sin(rad)).toFloat()
        val ay = py + (rAng * cos(rad)).toFloat()
        sb.append(path("M ${f(px)} ${f(py + rAng)} A ${f(rAng)} ${f(rAng)} 0 0 0 ${f(ax)} ${f(ay)}", "none", "#d97706", 2f))
        sb.append(tx(px + (rAng * sin(rad / 2.0)).toFloat() + 8f, py + (rAng * cos(rad / 2.0)).toFloat() + 12f, "θ=${num(amp)}°", "#b45309", "start", true, 10))

        // نخ و گلولهٔ آونگ
        sb.append(ln(px, py, bx, by, ST, 2.2f))
        sb.append(tx((px + bx) / 2f + 10f, (py + by) / 2f - 4f, "L = ${num(lenM)} m", ST, "start", true, 10))

        if (mode == "forces") {
            // نمایش نیروهای وارد بر گلوله: وزن mg، کشش نخ T، مؤلفهٔ بازگرداننده mg·sinθ
            sb.append(vecArrow(bx, by, bx, by + 42f, ACC, 2.1f, 7f))
            sb.append(tx(bx + 6f, by + 44f, "mg", ACC, "start", true, 10))
            val ux = (px - bx) / plen; val uy = (py - by) / plen
            sb.append(vecArrow(bx, by, bx + ux * 40f, by + uy * 40f, "#0d9488", 2.1f, 7f))
            sb.append(tx(bx + ux * 40f - 8f, by + uy * 40f, "T", "#0d9488", "end", true, 10))
            val txTan = -cos(rad).toFloat(); val tyTan = sin(rad).toFloat()
            sb.append(vecArrow(bx, by, bx + txTan * 34f, by + tyTan * 34f, PT, 2f, 6.5f))
            sb.append(tx(bx + txTan * 36f - 4f, by + tyTan * 36f + 10f, "mg·sinθ", PT, "end", true, 9))
        } else if (mode == "energy") {
            sb.append(ln(px - 10f, by, bx, by, "#0d9488", 1.3f, "3 3"))
            sb.append(tx(px - 14f, (by + eqY) / 2f + 4f, "h", "#0d9488", "end", true, 11))
        }

        sb.append(cls(bx, by, 13f, PT, ST, 1.9f))

        val period = (2 * PI * sqrt(lenM / gVal)).toFloat()
        val hRise = (lenM * (1.0 - cos(rad))).toFloat()
        if (mode != "quiz") {
            val btm = when (mode) {
                "forces" -> "نیروی بازگرداننده: F = mg·sinθ   |   دوره: T ≈ ${num(period)} s"
                "energy" -> "اوج ارتفاع: h = L(1−cosθ) ≈ ${num(hRise)} m   |   T ≈ ${num(period)} s"
                else -> "دورهٔ نوسان:  T = 2π√(L/g) ≈ ${num(period)} s"
            }
            sb.append(rc(20f, 240f, 320f, 28f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 258f, btm, "#1e293b", "middle", true, 11))
        }
        return sb.toString()
    }

    private fun incline(spec: FigureSpec): String {
        val ang = spec.xNum("ang", 30f).coerceIn(10f, 70f)
        val mass = max(0.5f, spec.xNum("mass", 5f))
        val mu = spec.xNum("mu", 0f).coerceIn(0f, 1.2f)
        val gVal = max(1f, spec.xNum("g", 9.8f))
        val show = spec.xStr("show", "all").lowercase() // all | vec | plain

        val rad = ang * PI / 180.0
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        // مثلث قائم‌الزاویهٔ دقیق سطح شیب‌دار: A=(ax, yBase) زاویهٔ θ، B=(bx, yBase) قائمه، C=(bx, cyPeak) قله
        val ax = 48f
        val yBase = 216f
        val hypLen = min(228f / cosA, 146f / sinA).coerceAtMost(236f)
        val bx = ax + hypLen * cosA
        val cyPeak = yBase - hypLen * sinA

        val sb = StringBuilder()
        sb.append(ln(30f, yBase, 332f, yBase, "#94a3b8", 1.5f))
        sb.append(poly("${f(ax)},${f(yBase)} ${f(bx)},${f(yBase)} ${f(bx)},${f(cyPeak)}", "#f1f5f9", ST, 2.2f))
        // علامت زاویهٔ قائمه در B و کمان دقیق زاویهٔ شیب θ در رأس A
        sb.append(rc(bx - 11f, yBase - 11f, 11f, 11f, "none", ST, 1.3f))
        val rArc = 34f
        sb.append(path("M ${f(ax + rArc)} ${f(yBase)} A ${f(rArc)} ${f(rArc)} 0 0 0 ${f(ax + rArc * cosA)} ${f(yBase - rArc * sinA)}", "none", "#d97706", 2f))
        sb.append(tx(ax + (rArc + 14f) * cos(rad / 2.0).toFloat(), yBase - (rArc + 12f) * sin(rad / 2.0).toFloat() + 3f, "${num(ang)}°", "#b45309", "middle", true, 10))

        // بردارهای یکه در راستای شیب به بالا uUp=(cosθ,-sinθ) و عمود بر سطح به بیرون uNorm=(-sinθ,-cosθ)
        val uUpX = cosA; val uUpY = -sinA
        val uNormX = -sinA; val uNormY = -cosA

        // جرم m چرخیده با زاویهٔ دقیق θ مماس بر وتر سطح شیب‌دار
        val tPos = 0.54f
        val px = ax + tPos * (bx - ax)
        val py = yBase + tPos * (cyPeak - yBase)
        val bw = 42f; val bh = 26f
        val p1x = px - (bw / 2f) * uUpX; val p1y = py - (bw / 2f) * uUpY
        val p2x = px + (bw / 2f) * uUpX; val p2y = py + (bw / 2f) * uUpY
        val p3x = p2x + bh * uNormX; val p3y = p2y + bh * uNormY
        val p4x = p1x + bh * uNormX; val p4y = p1y + bh * uNormY
        sb.append(poly("${f(p1x)},${f(p1y)} ${f(p2x)},${f(p2y)} ${f(p3x)},${f(p3y)} ${f(p4x)},${f(p4y)}", "rgba(108,99,245,.20)", ACC, 2f))

        val cmX = px + (bh / 2f) * uNormX
        val cmY = py + (bh / 2f) * uNormY
        sb.append(cls(cmX, cmY, 3f, ST, "none", 0f))

        if (show != "plain") {
            val vMg = 52f
            val vN = vMg * cosA
            val vPar = max(22f, vMg * sinA)
            // ۱) وزن mg قائم به پایین
            sb.append(vecArrow(cmX, cmY, cmX, cmY + vMg, ACC, 2.2f, 7f))
            sb.append(tx(cmX + 6f, cmY + vMg + 4f, "mg", ACC, "start", true, 10))
            // ۲) نیروی عمودی تکیه‌گاه N عمود بر سطح به بیرون
            sb.append(vecArrow(cmX, cmY, cmX + vN * uNormX, cmY + vN * uNormY, "#0d9488", 2.2f, 7f))
            sb.append(tx(cmX + (vN + 11f) * uNormX, cmY + (vN + 11f) * uNormY + 3f, "N", "#0d9488", "middle", true, 11))
            // ۳) مؤلفهٔ عمود بر سطح mg·cosθ (در خلاف جهت N)
            sb.append(vecArrow(cmX, cmY, cmX - vN * uNormX, cmY - vN * uNormY, "#64748b", 1.5f, 6f, "4 3"))
            // ۴) مؤلفهٔ موازی سطح به طرف پایین شیب: mg·sinθ (در جهت -uUp)
            sb.append(vecArrow(cmX, cmY, cmX - vPar * uUpX, cmY - vPar * uUpY, PT, 2.3f, 7f))
            sb.append(tx(cmX - (vPar + 8f) * uUpX - 4f, cmY - (vPar + 8f) * uUpY + 4f, "mg·sinθ", PT, "end", true, 10))
            // ۵) نیروی اصطکاک f موازی سطح به طرف بالای شیب (وقتی μ > 0)
            if (mu > 0.001f) {
                val vF = min(vPar, max(20f, mu * vN))
                sb.append(vecArrow(cmX, cmY, cmX + vF * uUpX, cmY + vF * uUpY, "#dc2626", 2.1f, 6.5f))
                sb.append(tx(cmX + (vF + 8f) * uUpX + 4f, cmY + (vF + 8f) * uUpY, "f", "#dc2626", "start", true, 11))
            }
        } else {
            sb.append(tx(cmX, cmY + 4f, "m", ST, "middle", true, 11))
        }

        if (show == "all") {
            val fPar = mass * gVal * sinA
            val nForce = mass * gVal * cosA
            val fFric = mu * nForce
            val acc = max(0f, (fPar - fFric) / mass)
            val summary = if (mu > 0.001f) {
                "m=${num(mass)}kg ، μ=${num(mu)}  |  N≈${num(nForce)}N ، a≈${num(acc)} m/s²"
            } else {
                "m=${num(mass)}kg  |  mg·sinθ≈${num(fPar)}N ، N≈${num(nForce)}N ، a≈${num(acc)} m/s²"
            }
            sb.append(rc(16f, 240f, 328f, 28f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 258f, summary, "#1e293b", "middle", true, 10))
        }
        return sb.toString()
    }

    private fun circuit(spec: FigureSpec): String {
        val mode = spec.xStr("mode", "series").lowercase() // series | parallel | mixed
        val r1 = max(0.5f, spec.xNum("r1", 4f))
        val r2 = max(0.5f, spec.xNum("r2", 6f))
        val r3 = max(0f, spec.xNum("r3", 0f))
        val v = max(0.5f, spec.xNum("v", 12f))
        val show = spec.xStr("show", "all").lowercase() // all | given
        val sb = StringBuilder()

        fun drawBattery(bx: Float, cy: Float) {
            sb.append(ln(bx, cy - 16f, bx, cy + 16f, PT, 3.2f))
            sb.append(ln(bx - 7f, cy - 9f, bx - 7f, cy + 9f, ST, 2.2f))
            sb.append(tx(bx + 6f, cy - 18f, "+", PT, "start", true, 11))
            sb.append(tx(bx - 16f, cy + 24f, "${num(v)}V", PT, "middle", true, 11))
        }

        fun drawAmmeter(ax: Float, ay: Float) {
            sb.append(cls(ax, ay, 12f, "#ffffff", "#0d9488", 1.8f))
            sb.append(tx(ax, ay + 4f, "A", "#0d9488", "middle", true, 11))
        }

        val req: Float
        when (mode) {
            "parallel" -> {
                val hasR3 = r3 >= 0.5f
                req = if (hasR3) 1f / (1f / r1 + 1f / r2 + 1f / r3) else (r1 * r2) / (r1 + r2)
                // حلقهٔ اصلی چپ (باتری) + شاخه‌های موازی
                sb.append(ln(54f, 44f, 298f, 44f, ST, 2f))
                sb.append(ln(54f, 206f, 298f, 206f, ST, 2f))
                sb.append(ln(54f, 44f, 54f, 112f, ST, 2f))
                sb.append(ln(54f, 138f, 54f, 206f, ST, 2f))
                drawBattery(54f, 125f)
                drawAmmeter(96f, 44f)

                val branchYs = if (hasR3) listOf(76f, 126f, 176f) else listOf(92f, 160f)
                val branchRs = if (hasR3) listOf(r1, r2, r3) else listOf(r1, r2)
                sb.append(ln(136f, branchYs.first(), 136f, branchYs.last(), ST, 2f))
                sb.append(ln(298f, 44f, 298f, 206f, ST, 2f))
                sb.append(ln(136f, 44f, 136f, branchYs.first(), ST, 2f))
                sb.append(ln(136f, branchYs.last(), 136f, 206f, ST, 2f))

                branchYs.forEachIndexed { idx, by ->
                    sb.append(ln(136f, by, 182f, by, ST, 2f))
                    sb.append(ln(252f, by, 298f, by, ST, 2f))
                    sb.append(rc(182f, by - 12f, 70f, 24f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                    sb.append(tx(217f, by + 4f, "R${idx + 1}=${num(branchRs[idx])}Ω", ST, "middle", true, 10))
                }
            }
            "mixed" -> {
                val r3Eff = if (r3 >= 0.5f) r3 else 6f
                val rPar = (r2 * r3Eff) / (r2 + r3Eff)
                req = r1 + rPar
                sb.append(ln(52f, 54f, 104f, 54f, ST, 2f))
                sb.append(rc(104f, 42f, 64f, 24f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                sb.append(tx(136f, 58f, "R1=${num(r1)}Ω", ST, "middle", true, 10))
                sb.append(ln(168f, 54f, 196f, 54f, ST, 2f))
                // دو شاخهٔ موازی R2 و R3
                sb.append(ln(196f, 34f, 196f, 78f, ST, 2f))
                sb.append(ln(298f, 34f, 298f, 202f, ST, 2f))
                listOf(34f to ("R2" to r2), 78f to ("R3" to r3Eff)).forEach { (by, pair) ->
                    sb.append(ln(196f, by, 214f, by, ST, 2f))
                    sb.append(rc(214f, by - 11f, 66f, 22f, "rgba(39,196,168,.16)", "#0d9488", 1.8f, 4f))
                    sb.append(tx(247f, by + 4f, "${pair.first}=${num(pair.second)}Ω", ST, "middle", true, 9))
                    sb.append(ln(280f, by, 298f, by, ST, 2f))
                }
                sb.append(ln(52f, 202f, 298f, 202f, ST, 2f))
                sb.append(ln(52f, 54f, 52f, 202f, ST, 2f))
                drawBattery(156f, 202f)
                drawAmmeter(246f, 202f)
            }
            else -> {
                val hasR3 = r3 >= 0.5f
                req = if (hasR3) r1 + r2 + r3 else r1 + r2
                // V326 — قطع سیم‌ها زیر مستطیل مقاومت‌ها تا خط زیر جعبه پوشیده نشود
                if (hasR3) {
                    sb.append(ln(50f, 64f, 78f, 64f, ST, 2f))
                    sb.append(rc(78f, 51f, 60f, 26f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                    sb.append(tx(108f, 68f, "R1=${num(r1)}Ω", ST, "middle", true, 9))
                    sb.append(ln(138f, 64f, 156f, 64f, ST, 2f))
                    sb.append(rc(156f, 51f, 60f, 26f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                    sb.append(tx(186f, 68f, "R2=${num(r2)}Ω", ST, "middle", true, 9))
                    sb.append(ln(216f, 64f, 234f, 64f, ST, 2f))
                    sb.append(rc(234f, 51f, 60f, 26f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                    sb.append(tx(264f, 68f, "R3=${num(r3)}Ω", ST, "middle", true, 9))
                    sb.append(ln(294f, 64f, 312f, 64f, ST, 2f))
                } else {
                    sb.append(ln(50f, 64f, 94f, 64f, ST, 2f))
                    sb.append(rc(94f, 50f, 72f, 28f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                    sb.append(tx(130f, 68f, "R1 = ${num(r1)}Ω", ST, "middle", true, 10))
                    sb.append(ln(166f, 64f, 200f, 64f, ST, 2f))
                    sb.append(rc(200f, 50f, 72f, 28f, "rgba(108,99,245,.15)", ACC, 1.9f, 4f))
                    sb.append(tx(236f, 68f, "R2 = ${num(r2)}Ω", ST, "middle", true, 10))
                    sb.append(ln(272f, 64f, 312f, 64f, ST, 2f))
                }
                sb.append(ln(50f, 64f, 50f, 198f, ST, 2f))
                sb.append(ln(312f, 64f, 312f, 198f, ST, 2f))
                sb.append(ln(50f, 198f, 152f, 198f, ST, 2f))
                sb.append(ln(166f, 198f, 240f, 198f, ST, 2f))
                sb.append(ln(264f, 198f, 312f, 198f, ST, 2f))
                sb.append(ln(152f, 182f, 152f, 214f, ST, 2.2f))
                sb.append(ln(166f, 174f, 166f, 222f, PT, 3.2f))
                sb.append(tx(159f, 168f, "${num(v)}V", PT, "middle", true, 11))
                drawAmmeter(252f, 198f)
            }
        }

        val curI = v / req
        val powW = v * curI
        if (show != "given") {
            sb.append(rc(16f, 238f, 328f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 258f, "R = ${num(req)} Ω   |   I = ${num(curI)} A   |   P = ${num(powW)} W", "#1e293b", "middle", true, 11))
        }
        return sb.toString()
    }

    private fun optics(spec: FigureSpec): String {
        val concave = spec.xStr("kind", "convex").lowercase() == "concave"
        val show = spec.xStr("show", "all").lowercase() // all | rays | blank
        // V322 — دو باگ هندسی قبلی حفظ و هندسهٔ کامل عدسی محدب/مقعر + پرتوهای مجازی اصلاح شد:
        val f = spec.xNum("f", 10f).coerceIn(2f, 25f)
        val d = spec.xNum("d", 30f).coerceIn(2f, 60f)
        val atFocus = !concave && abs(d - f) < 0.05f
        val lx = 180f; val oy = 128f
        val sc = (130f / max(max(d, 2f * f), 20f)).coerceIn(2.0f, 5.2f)
        val fPx = f * sc
        val dPx = d * sc

        val sb = StringBuilder()
        // محور اصلی نوری
        sb.append(ln(16f, oy, 344f, oy, "#64748b", 1.4f))

        // رسم دقیق عدسی دوطرفه (محدب همگرا یا مقعر واگرا) + محور تقارن عدسی
        val topY = 48f; val botY = 208f
        if (!concave) {
            sb.append(path("M ${f(lx)} ${f(topY)} Q ${f(lx + 16f)} ${f(oy)} ${f(lx)} ${f(botY)} Q ${f(lx - 16f)} ${f(oy)} ${f(lx)} ${f(topY)} Z", "rgba(56,189,248,.18)", "#0284c7", 2f))
            sb.append(poly("${f(lx)},${f(topY - 4f)} ${f(lx - 5f)},${f(topY + 5f)} ${f(lx + 5f)},${f(topY + 5f)}", "#0284c7", "none", 0f))
            sb.append(poly("${f(lx)},${f(botY + 4f)} ${f(lx - 5f)},${f(botY - 5f)} ${f(lx + 5f)},${f(botY - 5f)}", "#0284c7", "none", 0f))
        } else {
            sb.append(path("M ${f(lx - 11f)} ${f(topY)} L ${f(lx + 11f)} ${f(topY)} Q ${f(lx + 3f)} ${f(oy)} ${f(lx + 11f)} ${f(botY)} L ${f(lx - 11f)} ${f(botY)} Q ${f(lx - 3f)} ${f(oy)} ${f(lx - 11f)} ${f(topY)} Z", "rgba(56,189,248,.18)", "#0284c7", 2f))
        }
        sb.append(ln(lx, topY, lx, botY, "#0284c7", 1f, "3 3"))
        sb.append(tx(lx + 5f, oy + 14f, "O", MU, "start", true, 9))

        // نقاط کانونی F، 2F و F'، 2F'
        listOf(
            (lx - fPx) to "F", (lx + fPx) to "F'",
            (lx - 2f * fPx) to "2F", (lx + 2f * fPx) to "2F'"
        ).forEach { (pxPt, lbl) ->
            if (pxPt in 22f..338f) {
                sb.append(cls(pxPt, oy, 3f, "#d97706", "none", 0f))
                sb.append(tx(pxPt, oy + 15f, lbl, "#b45309", "middle", true, 9))
            }
        }

        // جسم AB در سمت چپ عدسی
        val objX = (lx - dPx).coerceIn(24f, lx - 14f)
        val objH = 44f
        val objTop = oy - objH
        sb.append(vecArrow(objX, oy, objX, objTop, "#0d9488", 2.6f, 7.5f))
        sb.append(tx(objX, oy + 26f, "جسم", "#0f766e", "middle", true, 9))

        val fSigned = if (concave) -f else f
        val di = if (atFocus) 9999f else 1f / (1f / fSigned - 1f / d)
        val mag = if (atFocus) 0f else -di / d // منفی = وارونه، مثبت = مستقیم

        if (show != "blank") {
            // پرتو ۱: موازی محور اصلی تا عدسی
            sb.append(ln(objX, objTop, lx, objTop, "#ea580c", 1.8f))

            if (atFocus) {
                // جسم روی کانون عدسی همگرا → پرتوهای خروجی موازی، تصویر در بی‌نهایت
                val slope = objH / fPx
                val xEnd = 340f
                sb.append(ln(lx, objTop, xEnd, objTop + slope * (xEnd - lx), "#ea580c", 1.8f))
                sb.append(ln(objX, objTop, xEnd, oy + slope * (xEnd - lx), PT, 1.7f))
            } else if (!concave && di > 0f) {
                // عدسی محدب با d > f: تصویر حقیقی و وارونه در سمت راست
                val rawImgX = lx + di * sc
                val imgX = rawImgX.coerceIn(lx + 14f, 336f)
                val imgH = (objH * abs(mag)).coerceIn(10f, 74f)
                val imgTipY = oy + imgH
                sb.append(ln(lx, objTop, imgX, imgTipY, "#ea580c", 1.8f))
                sb.append(ln(objX, objTop, imgX, imgTipY, PT, 1.7f))
                if (show == "all") {
                    sb.append(vecArrow(imgX, oy, imgX, imgTipY, ACC, 2.5f, 7f))
                }
            } else if (!concave && di < 0f) {
                // عدسی محدب با d < f (ذره‌بین): پرتوها واگرا و امتداد مجازی در سمت چپ برخورد می‌کنند
                val imgX = (lx + di * sc).coerceIn(20f, lx - 10f)
                val imgH = (objH * abs(mag)).coerceIn(18f, 76f)
                val imgTipY = oy - imgH
                val slope1 = objH / fPx
                sb.append(ln(lx, objTop, 338f, (objTop + slope1 * (338f - lx)).coerceIn(36f, 224f), "#ea580c", 1.8f))
                sb.append(ln(lx, objTop, imgX, imgTipY, "#ea580c", 1.5f, "4 3"))
                val slope2 = objH / (lx - objX)
                sb.append(ln(objX, objTop, 338f, (oy + slope2 * (338f - lx)).coerceIn(36f, 224f), PT, 1.7f))
                sb.append(ln(objX, objTop, imgX, imgTipY, PT, 1.5f, "4 3"))
                if (show == "all") {
                    sb.append(vecArrow(imgX, oy, imgX, imgTipY, ACC, 2.3f, 6.5f, "4 3"))
                }
            } else {
                // عدسی مقعر (واگرا): پرتو موازی طوری واگرا می‌شود که امتداد مجازی‌اش از کانون چپ F بگذرد!
                val fLeftX = lx - fPx
                val slopeDiv = (objTop - oy) / (lx - fLeftX) // منفی (رو به بالا در سمت راست)
                sb.append(ln(lx, objTop, 338f, (objTop + slopeDiv * (338f - lx)).coerceIn(34f, 220f), "#ea580c", 1.8f))
                sb.append(ln(lx, objTop, fLeftX.coerceAtLeast(20f), oy, "#ea580c", 1.5f, "4 3"))
                // پرتو دوم از مرکز نوری O
                val slopeO = (oy - objTop) / (lx - objX)
                sb.append(ln(objX, objTop, 338f, (oy + slopeO * (338f - lx)).coerceIn(34f, 224f), PT, 1.7f))
                val imgX = (lx + di * sc).coerceIn(fLeftX + 4f, lx - 6f)
                val imgH = (objH * abs(mag)).coerceIn(10f, objH - 4f)
                if (show == "all") {
                    sb.append(vecArrow(imgX, oy, imgX, oy - imgH, ACC, 2.3f, 6f, "4 3"))
                }
            }
        }

        val diText = if (atFocus) "بی‌نهایت" else "${num(di)} cm"
        val kindInfo = when {
            atFocus -> "پرتوهای خروجی موازی"
            di > 0f -> "حقیقی و وارونه"
            else -> "مجازی و مستقیم"
        }
        if (show == "all") {
            sb.append(rc(14f, 240f, 332f, 28f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 258f, "f=${num(f)}cm ، p=${num(d)}cm  →  q = $diText ($kindInfo)", "#1e293b", "middle", true, 10))
        } else {
            sb.append(tx(180f, 258f, "عدسی ${if (concave) "مقعر (واگرا)" else "محدب (همگرا)"}:  f = ${num(f)} cm ، p = ${num(d)} cm", MU, "middle", true, 11))
        }
        return sb.toString()
    }

    private fun heatCurve(spec: FigureSpec): String {
        val t0 = spec.xNum("t0", -20f).coerceIn(-60f, -2f)
        val t1 = spec.xNum("t1", 120f).coerceIn(105f, 180f)
        val massG = max(1f, spec.xNum("m", 100f))
        val mode = spec.xStr("mode", "full").lowercase() // full | stages | quiz

        val x0 = 52f; val y0 = 212f; val w = 278f; val h = 166f
        val sb = StringBuilder()
        fun px(u: Float) = x0 + u / 100f * w
        fun py(v: Float) = y0 - ((v - (t0 - 5f)) / ((t1 + 10f) - (t0 - 5f))).coerceIn(0f, 1f) * h

        // خطوط راهنمای دمای ذوب ۰°C و جوش ۱۰۰°C
        sb.append(ln(x0, py(0f), x0 + w, py(0f), "#cbd5e1", 1.2f, "4 4"))
        sb.append(ln(x0, py(100f), x0 + w, py(100f), "#cbd5e1", 1.2f, "4 4"))
        sb.append(ln(x0, y0, x0 + w + 8f, y0, ST, 1.8f)); sb.append(arrowHeadRight(x0 + w + 8f, y0))
        sb.append(ln(x0, y0 - h - 10f, x0, y0, ST, 1.8f)); sb.append(arrowHeadUp(x0, y0 - h - 10f))

        sb.append(tx(x0 - 6f, py(t0) + 4f, "${num(t0)}°", MU, "end", true, 9))
        sb.append(tx(x0 - 6f, py(0f) + 4f, "0°C", "#0284c7", "end", true, 10))
        sb.append(tx(x0 - 6f, py(100f) + 4f, "100°C", PT, "end", true, 10))
        sb.append(tx(x0 + 8f, y0 - h - 2f, "دما (°C)", MU, "start", true, 10))

        // ۵ مرحلهٔ گرمایش: ۱) یخ، ۲) ذوب در ۰°C، ۳) آب مایع، ۴) تبخیر در ۱۰۰°C، ۵) بخار
        val pts = listOf(0f to t0, 14f to 0f, 36f to 0f, 60f to 100f, 88f to 100f, 100f to t1)
        for (i in 0 until pts.size - 1) {
            val isPhaseChange = i == 1 || i == 3
            sb.append(ln(px(pts[i].first), py(pts[i].second), px(pts[i + 1].first), py(pts[i + 1].second), if (isPhaseChange) PT else ACC, 2.8f))
            sb.append(cls(px(pts[i].first), py(pts[i].second), 3.2f, if (isPhaseChange) PT else ACC, "#ffffff", 1f))
        }
        sb.append(cls(px(100f), py(t1), 3.2f, ACC, "#ffffff", 1f))

        if (mode == "quiz") {
            sb.append(tx(px(7f), py(t0 / 2f) - 8f, "(۱)", ST, "middle", true, 10))
            sb.append(tx(px(25f), py(0f) - 8f, "(۲)", PT, "middle", true, 10))
            sb.append(tx(px(48f), py(50f) - 8f, "(۳)", ST, "middle", true, 10))
            sb.append(tx(px(74f), py(100f) - 8f, "(۴)", PT, "middle", true, 10))
            sb.append(tx(px(94f), py((100f + t1) / 2f) - 8f, "(۵)", ST, "middle", true, 10))
            sb.append(tx(180f, 254f, "گرمای کل Q — مراحل (۱) تا (۵) منحنی گرمایش (${num(massG)}g آب) را مشخص کنید", MU, "middle", true, 10))
        } else {
            sb.append(tx(px(8f), py(t0 / 2f) - 8f, "یخ", "#0284c7", "middle", true, 10))
            sb.append(tx(px(25f), py(0f) - 8f, if (mode == "full") "ذوب (mLf)" else "ذوب", PT, "middle", true, 9))
            sb.append(tx(px(47f), py(52f) - 8f, "آب", ACC, "middle", true, 10))
            sb.append(tx(px(74f), py(100f) - 8f, if (mode == "full") "جوش (mLv)" else "تبخیر", PT, "middle", true, 9))
            sb.append(tx(px(94f), py((100f + t1) / 2f) - 8f, "بخار", "#d97706", "middle", true, 10))

            // محاسبهٔ واقعی گرمای کل بر حسب کیلوژول برای m گرم آب
            val qIce = massG * 2.1f * abs(t0)
            val qMelt = massG * 334f
            val qWater = massG * 4.18f * 100f
            val qBoil = massG * 2260f
            val qSteam = massG * 2.0f * (t1 - 100f)
            val qTotalKj = (qIce + qMelt + qWater + qBoil + qSteam) / 1000f
            sb.append(rc(20f, 236f, 320f, 28f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 254f, "جرم m = ${num(massG)}g  |  گرمای کل ذوب و تبخیر: Q ≈ ${num(qTotalKj)} kJ", "#1e293b", "middle", true, 10))
        }
        return sb.toString()
    }

    private fun dna(spec: FigureSpec): String {
        val mode = spec.xStr("mode", "helix").lowercase() // helix | flat | mrna | quiz
        val showLabel = spec.xStr("label", "1") != "0"
        val seq = spec.xStr("seq", "ATGCGATC").uppercase().filter { it in "ATGC" }.take(10).ifEmpty { "ATGC" }
        val sb = StringBuilder()

        fun compDna(b: Char) = when (b) { 'A' -> 'T'; 'T' -> 'A'; 'G' -> 'C'; 'C' -> 'G'; else -> 'T' }
        fun compRna(b: Char) = when (b) { 'A' -> 'U'; 'T' -> 'A'; 'G' -> 'C'; 'C' -> 'G'; else -> 'U' }
        fun baseCol(b: Char) = when (b) { 'A' -> ACC; 'T' -> PT; 'G' -> "#0d9488"; 'C' -> "#d97706"; 'U' -> "#ea580c"; else -> MU }
        fun hBonds(b: Char) = if (b == 'G' || b == 'C') 3 else 2

        val totalHBonds = seq.sumOf { hBonds(it) }
        val gcPct = kotlin.math.round(seq.count { it == 'G' || it == 'C' } * 100f / seq.length).toInt()

        if (mode == "flat" || mode == "mrna" || mode == "quiz") {
            val isRna = mode == "mrna"
            val isQuiz = mode == "quiz"
            val n = seq.length
            val xL = 38f; val xR = 322f
            val step = (xR - xL) / (n + 0.4f)
            val yTop = 66f; val yBot = 178f

            // ستون فقرات قند-فسفات با جهت 5'→3' و 3'→5'
            sb.append(ln(xL, yTop, xR, yTop, ST, 3.2f))
            sb.append(ln(xL, yBot, xR, yBot, if (isRna) "#ea580c" else ST, 3.2f))
            sb.append(tx(22f, yTop + 4f, "5'", ACC, "middle", true, 11))
            sb.append(tx(340f, yTop + 4f, "3'", ACC, "middle", true, 11))
            sb.append(tx(22f, yBot + 4f, if (isRna) "3'" else "3'", PT, "middle", true, 11))
            sb.append(tx(340f, yBot + 4f, if (isRna) "5'" else "5'", PT, "middle", true, 11))

            for (i in 0 until n) {
                val x = xL + (i + 0.7f) * step
                val b1 = seq[i]
                val b2 = if (isRna) compRna(b1) else compDna(b1)
                val hb = hBonds(b1)

                // خطوط پیوند هیدروژنی دوگانه (A=T) و سه‌گانه (G≡C)
                val offsets = if (hb == 3) listOf(-3.5f, 0f, 3.5f) else listOf(-2.2f, 2.2f)
                offsets.forEach { dx ->
                    sb.append(ln(x + dx, yTop + 26f, x + dx, yBot - 26f, "#94a3b8", 1.3f, "3 2"))
                }

                // باز رشتهٔ اول (الگو)
                sb.append(rc(x - 12f, yTop + 4f, 24f, 22f, "#ffffff", baseCol(b1), 1.8f, 5f))
                sb.append(tx(x, yTop + 19f, b1.toString(), baseCol(b1), "middle", true, 12))

                // باز رشتهٔ مکمل (یا خانهٔ خالی در حالت آزمون quiz)
                if (isQuiz) {
                    sb.append(rcDash(x - 12f, yBot - 26f, 24f, 22f, "#f8fafc", "#4f46e5", 1.5f, 5f, "4 2"))
                } else {
                    sb.append(rc(x - 12f, yBot - 26f, 24f, 22f, "#ffffff", baseCol(b2), 1.8f, 5f))
                    sb.append(tx(x, yBot - 11f, b2.toString(), baseCol(b2), "middle", true, 12))
                }
            }

            if (showLabel) {
                val foot = when {
                    isQuiz -> "رشتهٔ مکمل را بنویسید (A=T با ۲ پیوند و G≡C با ۳ پیوند هیدروژنی)"
                    isRna -> "رونویسی DNA به mRNA (جایگزینی U به‌جای T) — GC = $gcPct%"
                    else -> "جفت‌بازهای مکمل (5'→3' / 3'→5'):  مجموع پیوند هیدروژنی = $totalHBonds  |  GC = $gcPct%"
                }
                sb.append(rc(14f, 228f, 332f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
                sb.append(tx(180f, 248f, foot, "#1e293b", "middle", true, 10))
            }
            return sb.toString()
        }

        // مارپیچ دوگانهٔ DNA با نمایش جفت‌بازها، جهت دو رشته و آمار پیوندهای هیدروژنی
        sb.append(tx(34f, 34f, "5'", ACC, "middle", true, 11))
        sb.append(tx(326f, 34f, "3'", PT, "middle", true, 11))
        val nPts = 54
        var p1 = ""; var p2 = ""
        for (i in 0..nPts) {
            val t = i.toFloat() / nPts
            val x = 36f + t * 288f
            val y1 = 122f + 46f * sin(t * 2.2 * PI).toFloat()
            val y2 = 122f - 46f * sin(t * 2.2 * PI).toFloat()
            p1 += (if (i == 0) "M " else " L ") + "${f(x)} ${f(y1)}"
            p2 += (if (i == 0) "M " else " L ") + "${f(x)} ${f(y2)}"
        }

        val rungs = seq.length.coerceIn(4, 10)
        for (k in 0 until rungs) {
            val t = (k + 0.5f) / rungs
            val x = 36f + t * 288f
            val s = sin(t * 2.2 * PI).toFloat()
            val y1 = 122f + 46f * s
            val y2 = 122f - 46f * s
            val b1 = seq[k % seq.length]
            val b2 = compDna(b1)
            val ym = (y1 + y2) / 2f
            sb.append(ln(x, y1, x, ym - 2f, baseCol(b1), 2.6f))
            sb.append(ln(x, ym + 2f, x, y2, baseCol(b2), 2.6f))
            if (abs(y1 - y2) > 26f) {
                sb.append(rc(x - 8f, y1 + (if (y2 > y1) 4f else -18f), 16f, 14f, "#ffffff", baseCol(b1), 1f, 3f))
                sb.append(tx(x, y1 + (if (y2 > y1) 14f else -8f), b1.toString(), baseCol(b1), "middle", true, 9))
                sb.append(rc(x - 8f, y2 + (if (y1 > y2) 4f else -18f), 16f, 14f, "#ffffff", baseCol(b2), 1f, 3f))
                sb.append(tx(x, y2 + (if (y1 > y2) 14f else -8f), b2.toString(), baseCol(b2), "middle", true, 9))
            }
        }
        sb.append(path(p1, "none", ACC, 3.2f))
        sb.append(path(p2, "none", PT, 3.2f))

        if (showLabel) {
            sb.append(rc(16f, 232f, 328f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 252f, "مارپیچ دوگانهٔ DNA ($seq) — پیوند هیدروژنی: $totalHBonds  |  GC: $gcPct%", "#1e293b", "middle", true, 10))
        }
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
        val dir = spec.xStr("dir", "ltr").lowercase()
        val mode = spec.xStr("mode", "full").lowercase()
        val rtl = dir == "rtl"
        val n = items.size
        val x0 = 58f; val x1 = 302f; val y = 138f
        val sb = StringBuilder()

        // محور اصلی خط زمان با پیکان جهت پیشروی زمان
        sb.append(ln(x0 - 24f, y, x1 + 24f, y, ST, 2.4f))
        if (rtl) {
            sb.append(poly("${f(x0 - 26f)},${f(y)} ${f(x0 - 17f)},${f(y - 4.5f)} ${f(x0 - 17f)},${f(y + 4.5f)}", ST, "none", 0f))
        } else {
            sb.append(arrowHeadRight(x1 + 26f, y))
        }

        val parsedYears = mutableListOf<Int?>()
        val xs = FloatArray(n)
        items.forEachIndexed { i, raw ->
            val parts = raw.split(":", "：")
            val yearRaw = parts.getOrNull(0)?.trim().orEmpty()
            val labelRaw = parts.drop(1).joinToString(":").trim()
            parsedYears.add(faEnDigits(yearRaw).filter { it.isDigit() || it == '-' }.toIntOrNull())
            val t = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            val px = if (rtl) x1 - (x1 - x0) * t else x0 + (x1 - x0) * t
            xs[i] = px

            // ۴ سطح پلکانی (نزدیک/دور در بالا و پایین) تا در ۵ یا ۶ رویداد هیچ برچسبی به کناری برخورد نکند
            val up = i % 2 == 0
            val far = (i / 2) % 2 == 1 && n >= 5
            val stemLen = if (far) 56f else 28f
            val cardY = if (up) y - stemLen - 34f else y + stemLen + 2f
            val col = when (i % 3) {
                0 -> ACC
                1 -> "#0d9488"
                else -> "#d97706"
            }

            sb.append(ln(px, y, px, if (up) cardY + 34f else cardY, "#94a3b8", 1.3f, "3 2.5"))
            sb.append(cls(px, y, 5.2f, col, "#ffffff", 1.5f))

            val yrTxt = if (mode == "blank_year") "؟" else yearRaw
            val evTxt = if (mode == "blank_event") "؟" else labelRaw
            val cardW = 86f
            val cx = px.coerceIn(cardW / 2f + 6f, 354f - cardW / 2f)
            sb.append(rc(cx - cardW / 2f, cardY, cardW, 34f, "#f8fafc", col, 1.3f, 7f))
            sb.append(tx(cx, cardY + 13f, yrTxt, if (yrTxt == "؟") PT else col, "middle", true, 10))
            sb.append(tx(cx, cardY + 27f, evTxt, if (evTxt == "؟") PT else ST, "middle", evTxt == "؟", 8))
        }

        if (mode == "diff" && n >= 2) {
            for (i in 0 until n - 1) {
                val yA = parsedYears[i]; val yB = parsedYears[i + 1]
                if (yA != null && yB != null) {
                    val mx = (xs[i] + xs[i + 1]) / 2f
                    sb.append(tx(mx, y - 7f, "Δ=${abs(yB - yA)}", "#be123c", "middle", true, 8))
                }
            }
        }

        val dirText = if (rtl) "راست به چپ" else "چپ به راست"
        sb.append(tx(180f, 262f, "خط زمان تاریخی — ترتیب رویدادها از $dirText", MU, "middle", true, 10))
        return sb.toString()
    }

    private fun mapGeo(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "compass").lowercase()
        val sb = StringBuilder()
        if (kind == "map") {
            val mapX = 52f; val mapY = 30f; val mapW = 256f; val mapH = 172f
            sb.append(rc(mapX, mapY, mapW, mapH, "#f8fafc", ST, 1.6f, 4f))

            // دریای خزر (شمال) و خلیج فارس و دریای عمان (جنوب) برای تشخیص جغرافیایی فوری
            sb.append(poly("146,32 184,32 180,58 152,60", "#bae6fd", "#0284c7", 1.1f))
            sb.append(poly("104,158 164,180 218,176 266,194 266,201 104,201", "#bae6fd", "#0284c7", 1.1f))

            // شبکهٔ مدارها و نصف‌النهارها با درجهٔ عرض و طول جغرافیایی
            val lons = listOf(45, 50, 55, 60)
            val lats = listOf(40, 35, 30, 25)
            lons.forEachIndexed { idx, lon ->
                val gx = mapX + 24f + idx * 68f
                sb.append(ln(gx, mapY, gx, mapY + mapH, "#cbd5e1", 0.8f, "3 3"))
                sb.append(tx(gx, mapY - 5f, "${lon}°E", MU, "middle", false, 8))
            }
            lats.forEachIndexed { idx, lat ->
                val gy = mapY + 20f + idx * 44f
                sb.append(ln(mapX, gy, mapX + mapW, gy, "#cbd5e1", 0.8f, "3 3"))
                sb.append(tx(mapX - 5f, gy + 3f, "${lat}°N", MU, "end", false, 8))
            }

            // مرز ساده‌شده و قابل‌تشخیص فلات ایران در بازهٔ 44..63E و 25..40N
            fun lonToX(lon: Float) = mapX + 24f + (lon - 45f) / 5f * 68f
            fun latToY(lat: Float) = mapY + 20f + (40f - lat) / 5f * 44f
            val borderCoords = listOf(
                44.5f to 39.4f, 48.2f to 38.6f, 49.2f to 37.3f, 53.8f to 37.0f,
                54.8f to 38.0f, 59.4f to 37.5f, 61.2f to 35.6f, 60.8f to 31.0f,
                61.6f to 25.3f, 57.2f to 25.6f, 56.2f to 27.1f, 53.0f to 26.8f,
                50.4f to 29.0f, 48.3f to 30.3f, 47.6f to 31.2f, 45.6f to 33.8f,
                46.1f to 35.6f, 44.8f to 37.8f
            )
            val borderPts = borderCoords.joinToString(" ") { "${f(lonToX(it.first))},${f(latToY(it.second))}" }
            sb.append(poly(borderPts, "rgba(16,185,129,.20)", "#0f766e", 1.8f))

            // شهرهای قابل تنظیم با مختصات (طول,عرض)
            val rawCities = spec.xStr("cities", "تهران:51.4,35.7;تبریز:46.3,38.1;مشهد:59.6,36.3;شیراز:52.5,29.6")
                .split(Regex("[;؛]")).map { it.trim() }.filter { it.isNotBlank() }.take(5)
            rawCities.forEachIndexed { idx, item ->
                val kv = item.split(":", "：")
                val name = kv.getOrNull(0)?.trim().orEmpty()
                val cc = kv.getOrNull(1)?.split(",", "،") ?: emptyList()
                val lon = faEnDigits(cc.getOrNull(0)?.trim().orEmpty()).toFloatOrNull() ?: 51.4f
                val lat = faEnDigits(cc.getOrNull(1)?.trim().orEmpty()).toFloatOrNull() ?: 35.7f
                val cx = lonToX(lon).coerceIn(mapX + 10f, mapX + mapW - 10f)
                val cy = latToY(lat).coerceIn(mapY + 10f, mapY + mapH - 10f)
                sb.append(cls(cx, cy, if (idx == 0) 4.4f else 3.4f, if (idx == 0) PT else ACC, "#ffffff", 1.1f))
                sb.append(tx(cx, cy - 6f, name, ST, "middle", true, 8))
            }

            // نماد شمال جغرافیایی (N↑) در گوشهٔ بالا-راست نقشه
            sb.append(arrowHeadUp(mapX + mapW - 16f, mapY + 12f))
            sb.append(ln(mapX + mapW - 16f, mapY + 14f, mapX + mapW - 16f, mapY + 30f, PT, 1.8f))
            sb.append(tx(mapX + mapW - 16f, mapY + 41f, "N", PT, "middle", true, 9))

            // خط‌کش مقیاس استاندارد سیاه‌وسفید بدون برخورد متنی
            val sx0 = 60f; val sy0 = 222f
            sb.append(rc(sx0, sy0, 40f, 6f, ST, ST, 1f))
            sb.append(rc(sx0 + 40f, sy0, 40f, 6f, "#ffffff", ST, 1f))
            sb.append(tx(sx0, sy0 + 17f, "0", MU, "middle", false, 8))
            sb.append(tx(sx0 + 40f, sy0 + 17f, "250", MU, "middle", false, 8))
            sb.append(tx(sx0 + 82f, sy0 + 17f, "500 km", MU, "start", false, 8))
            sb.append(tx(244f, 232f, "خلیج فارس", "#0369a1", "middle", true, 9))
            sb.append(tx(180f, 260f, "نقشهٔ مختصات جغرافیایی (مدارها و نصف‌النهارها)", ST, "middle", true, 10))
            return sb.toString()
        }

        // گل‌باد استاندارد ۸جهته بدون هم‌پوشانی عقربه‌ها در مرکز
        val quiz = kind == "compass_quiz"
        val cx = 180f; val cy = 128f; val rOuter = 68f; val rOrd = 44f; val rMid = 13f
        sb.append(cls(cx, cy, rOuter + 6f, "rgba(108,99,245,.05)", "#cbd5e1", 1.2f))
        sb.append(cls(cx, cy, rOrd + 4f, "none", "#e2e8f0", 1f))

        // ۴ پرهٔ فرعی (شمال‌شرق، جنوب‌شرق، جنوب‌غرب، شمال‌غرب)
        val ordAngles = listOf(-45.0, 45.0, 135.0, 225.0)
        ordAngles.forEach { deg ->
            val rad = Math.toRadians(deg)
            val lRad = Math.toRadians(deg - 22.0)
            val rRad = Math.toRadians(deg + 22.0)
            val tipX = cx + rOrd * cos(rad).toFloat(); val tipY = cy + rOrd * sin(rad).toFloat()
            val lx = cx + rMid * 0.8f * cos(lRad).toFloat(); val ly = cy + rMid * 0.8f * sin(lRad).toFloat()
            val rx = cx + rMid * 0.8f * cos(rRad).toFloat(); val ry = cy + rMid * 0.8f * sin(rRad).toFloat()
            sb.append(poly("${f(cx)},${f(cy)} ${f(lx)},${f(ly)} ${f(tipX)},${f(tipY)} ${f(rx)},${f(ry)}", "#cbd5e1", "#475569", 1.1f))
        }

        // ۴ پرهٔ اصلی دوقسمتی (شمال قرمز برجسته، شرق/جنوب/غرب سرمه‌ای-روشن) — تمام اضلاع دقیقاً در (cx, cy) تلاقی می‌کنند
        val cardAngles = listOf(-90.0, 0.0, 90.0, 180.0)
        cardAngles.forEachIndexed { idx, deg ->
            val rad = Math.toRadians(deg)
            val lRad = Math.toRadians(deg - 45.0)
            val rRad = Math.toRadians(deg + 45.0)
            val tipX = cx + rOuter * cos(rad).toFloat(); val tipY = cy + rOuter * sin(rad).toFloat()
            val lx = cx + rMid * cos(lRad).toFloat(); val ly = cy + rMid * sin(lRad).toFloat()
            val rx = cx + rMid * cos(rRad).toFloat(); val ry = cy + rMid * sin(rRad).toFloat()
            val darkFill = if (idx == 0) PT else ACC
            val lightFill = if (idx == 0) "#fda4af" else "#c7d2fe"
            sb.append(poly("${f(cx)},${f(cy)} ${f(lx)},${f(ly)} ${f(tipX)},${f(tipY)}", darkFill, ST, 1.1f))
            sb.append(poly("${f(cx)},${f(cy)} ${f(tipX)},${f(tipY)} ${f(rx)},${f(ry)}", lightFill, ST, 1.1f))
        }
        sb.append(cls(cx, cy, 4.5f, "#ffffff", ST, 1.8f))

        val dirs = listOf(
            Triple("شمال (0°)", "۱", -90.0),
            Triple("شمال‌شرق", "۲", -45.0),
            Triple("شرق (90°)", "۳", 0.0),
            Triple("جنوب‌شرق", "۴", 45.0),
            Triple("جنوب (180°)", "۵", 90.0),
            Triple("جنوب‌غرب", "۶", 135.0),
            Triple("غرب (270°)", "۷", 180.0),
            Triple("شمال‌غرب", "۸", -135.0)
        )
        dirs.forEachIndexed { i, (name, numLbl, deg) ->
            val rad = Math.toRadians(deg)
            val dist = if (i % 2 == 0) rOuter + 21f else rOrd + 26f
            val lx = cx + dist * cos(rad).toFloat()
            val ly = cy + dist * sin(rad).toFloat() + 4f
            val txt = if (quiz && i > 0) "($numLbl) ؟" else name
            val col = if (i == 0) PT else if (quiz) ACC else if (i % 2 == 0) ST else MU
            sb.append(tx(lx, ly, txt, col, "middle", i % 2 == 0 || quiz, if (i % 2 == 0) 10 else 9))
        }
        sb.append(tx(180f, 240f, if (quiz) "گل‌باد جغرافیایی — نام جهت‌های شماره‌گذاری‌شده را بنویسید" else "گل‌باد ۸جهته — جهت‌های اصلی و فرعی جغرافیایی", ST, "middle", true, 10))
        sb.append(tx(180f, 260f, "شمال (۰°) ← شرق (۹۰°) ← جنوب (۱۸۰°) ← غرب (۲۷۰°)", MU, "middle", false, 9))
        return sb.toString()
    }

    private fun sentence(spec: FigureSpec): String {
        val subj = spec.xStr("subj", "کودکان").ifBlank { "کودکان" }
        val verb = spec.xStr("verb", "بازی می‌کنند").ifBlank { "بازی می‌کنند" }
        val obj = spec.xStr("obj", "").trim()
        val prep = spec.xStr("prep", "").trim()
        val mode = spec.xStr("mode", "verbal").lowercase()
        val nominal = mode == "nominal"
        val blank = mode == "blank"
        val sb = StringBuilder()

        // نوار جملهٔ کامل به ترتیب طبیعی راست‌به‌چپ فارسی در بالای نمودار
        val fullWords = listOf(subj, prep, obj, verb).filter { it.isNotBlank() }.joinToString(" ")
        sb.append(rc(30f, 22f, 300f, 30f, "#eef2ff", ACC, 1.4f, 8f))
        sb.append(tx(180f, 42f, "جمله: «$fullWords»", ST, "middle", true, 11))

        // انشعاب درختی راست‌به‌چپ: نهاد در سمت راست، گزاره در سمت چپ!
        val predParts = mutableListOf<Triple<String, String, String>>() // (word, role, color)
        if (prep.isNotBlank()) predParts.add(Triple(prep, "متمم / قید", "#0284c7"))
        if (obj.isNotBlank()) predParts.add(Triple(obj, "مفعول", GLD))
        predParts.add(Triple(verb, if (nominal) "مسند و فعل اسنادی" else "فعل", "#0d9488"))

        val subjW = 86f
        val subjCx = 290f
        val predRight = 236f
        val predLeft = 26f
        val predCx = (predLeft + predRight) / 2f

        // خطوط درخت از نوار جمله به «نهاد» (راست) و «گزاره» (چپ)
        sb.append(ln(180f, 52f, 180f, 64f, "#64748b", 1.5f))
        sb.append(ln(predCx, 64f, subjCx, 64f, "#64748b", 1.5f))
        sb.append(ln(subjCx, 64f, subjCx, 78f, ACC, 1.6f))
        sb.append(ln(predCx, 64f, predCx, 78f, "#0d9488", 1.6f))

        // سرشاخهٔ نهاد (سمت راست) و سرشاخهٔ گزاره (سمت چپ)
        sb.append(rc(subjCx - subjW / 2f, 78f, subjW, 24f, "rgba(108,99,245,.15)", ACC, 1.5f, 6f))
        sb.append(tx(subjCx, 94f, if (blank) "بخش اول: ؟" else "نهاد", ACC, "middle", true, 10))
        sb.append(rc(predCx - 54f, 78f, 108f, 24f, "rgba(13,148,136,.15)", "#0d9488", 1.5f, 6f))
        sb.append(tx(predCx, 94f, if (blank) "بخش دوم: ؟" else "گزاره", "#0f766e", "middle", true, 10))

        // کارت واژهٔ نهاد در پایین سمت راست
        sb.append(ln(subjCx, 102f, subjCx, 134f, ACC, 1.4f, "4 3"))
        sb.append(rc(subjCx - subjW / 2f, 134f, subjW, 54f, "#ffffff", ACC, 1.7f, 9f))
        sb.append(tx(subjCx, 156f, subj, ST, "middle", true, 11))
        sb.append(tx(subjCx, 177f, if (blank) "نقش: ؟" else "نهاد (مسندالیه)", if (blank) PT else MU, "middle", blank, 9))

        // کارت‌های اجزای گزاره از راست به چپ (متمم/قید ← مفعول ← فعل)
        val k = predParts.size
        val gap = 8f
        val cardW = ((predRight - predLeft) - gap * (k - 1)) / k
        sb.append(ln(predCx, 102f, predCx, 118f, "#0d9488", 1.4f))
        val firstCx = predRight - cardW / 2f
        val lastCx = predLeft + cardW / 2f
        if (k > 1) sb.append(ln(lastCx, 118f, firstCx, 118f, "#0d9488", 1.4f))

        predParts.forEachIndexed { idx, (word, role, col) ->
            // idx=0 راست‌ترین جزء گزاره و آخرین عنصر (فعل) چپ‌ترین جزء گزاره است
            val cx = predRight - cardW / 2f - idx * (cardW + gap)
            sb.append(ln(cx, 118f, cx, 134f, col, 1.4f, "4 3"))
            sb.append(rc(cx - cardW / 2f, 134f, cardW, 54f, "#ffffff", col, 1.7f, 9f))
            sb.append(tx(cx, 156f, word, ST, "middle", true, if (k >= 3) 9 else 11))
            sb.append(tx(cx, 177f, if (blank) "نقش: ؟" else role, if (blank) PT else col, "middle", true, 8))
        }

        val hint = when {
            blank -> "نقش دستوری هر یک از اجزای مشخص‌شده در جمله را بنویسید"
            nominal -> "جملهٔ اسنادی (اسمیه): نهاد (راست) + گزاره شامل مسند و فعل اسنادی (چپ)"
            else -> "ساختار راست‌به‌چپ جملهٔ فارسی: نهاد در آغاز (راست) و فعل در پایان (چپ)"
        }
        sb.append(tx(180f, 224f, "نهاد + گزاره = جملهٔ کامل", ST, "middle", true, 11))
        sb.append(tx(180f, 248f, hint, MU, "middle", false, 9))
        return sb.toString()
    }

    private fun vectors(spec: FigureSpec): String {
        val ax1 = spec.xNum("ax1", 3f); val ay1 = spec.xNum("ay1", 1f)
        val bx = spec.xNum("bx", 1f); val by = spec.xNum("by", 2f)
        val mode = spec.xStr("mode", "sum").lowercase()
        val diff = mode == "diff"
        val isDot = mode == "dot"
        val quiz = mode == "quiz"
        val ox = 166f; val oy = 166f; val sc = 24f
        val sb = StringBuilder()

        // شبکهٔ شطرنجی ملایم برای خواندن دقیق مؤلفه‌های x و y
        for (gx in -4..5) {
            val xx = ox + gx * sc
            if (xx in 56f..318f) sb.append(ln(xx, 44f, xx, 232f, "#f1f5f9", 0.9f))
        }
        for (gy in -2..5) {
            val yy = oy - gy * sc
            if (yy in 44f..232f) sb.append(ln(56f, yy, 318f, yy, "#f1f5f9", 0.9f))
        }

        sb.append(ln(56f, oy, 320f, oy, ST, 1.5f)).append(arrowHeadRight(320f, oy))
        sb.append(ln(ox, 232f, ox, 44f, ST, 1.5f)).append(arrowHeadUp(ox, 44f))
        sb.append(tx(326f, oy + 14f, "x", ST, "middle", true, 11)).append(tx(ox - 12f, 44f, "y", ST, "middle", true, 11))
        for (i in -4..5) {
            if (i == 0) continue
            val xx = ox + i * sc
            if (xx in 62f..312f) sb.append(tx(xx, oy + 12f, i.toString(), MU, "middle", false, 8))
            val yy = oy - i * sc
            if (yy in 50f..226f) sb.append(tx(ox - 7f, yy + 3f, i.toString(), MU, "end", false, 8))
        }

        fun vecArrow(x1: Float, y1: Float, x2: Float, y2: Float, col: String, sw: Float, head: Float = 9f, dash: String = "") {
            val dx = x2 - x1; val dy = y2 - y1
            val len = hypot(dx, dy)
            if (len < 2f) return
            val ux = dx / len; val uy = dy / len
            val bxTip = x2 - ux * head; val byTip = y2 - uy * head
            sb.append(ln(x1, y1, bxTip, byTip, col, sw, dash))
            val wing = head * 0.46f
            sb.append(poly("${f(x2)},${f(y2)} ${f(bxTip - uy * wing)},${f(byTip + ux * wing)} ${f(bxTip + uy * wing)},${f(byTip - ux * wing)}", col, "none", 0f))
        }

        // V322 — مؤلفه‌های بزرگ پیکان را با مختصات ده‌ها هزار بیرون از بوم می‌کشیدند؛ حالا نوک‌ها در کادر مهار می‌شوند.
        val axx = (ox + ax1 * sc).coerceIn(56f, 318f); val ayy = (oy - ay1 * sc).coerceIn(48f, 228f)
        val bxx = (ox + bx * sc).coerceIn(56f, 318f); val byy = (oy - by * sc).coerceIn(48f, 228f)
        val rx = (ox + (if (diff) ax1 - bx else ax1 + bx) * sc).coerceIn(56f, 318f)
        val ry = (oy - (if (diff) ay1 - by else ay1 + by) * sc).coerceIn(48f, 228f)

        if (!isDot) {
            if (diff) {
                // نمایش هندسی −b و قاعدهٔ متوازی‌الاضلاع برای a − b
                val nbx = (ox - bx * sc).coerceIn(56f, 318f)
                val nby = (oy + by * sc).coerceIn(48f, 228f)
                vecArrow(ox, oy, nbx, nby, "#cbd5e1", 1.5f, 7f, "4 3")
                sb.append(ln(axx, ayy, rx, ry, "#94a3b8", 1.3f, "4 3"))
                sb.append(ln(nbx, nby, rx, ry, "#94a3b8", 1.3f, "4 3"))
            } else {
                // خطوط نقطه‌چین متوازی‌الاضلاع جمع بردارها
                sb.append(ln(axx, ayy, rx, ry, "#94a3b8", 1.4f, "4 3"))
                sb.append(ln(bxx, byy, rx, ry, "#94a3b8", 1.4f, "4 3"))
            }
            vecArrow(ox, oy, rx, ry, PT, 2.7f, 9.5f)
            val rLbl = if (quiz) "R = ؟" else if (diff) "a − b" else "a + b"
            sb.append(tx(rx + (if (rx >= ox) 8f else -8f), ry + (if (ry <= oy) -6f else 14f), rLbl, PT, if (rx >= ox) "start" else "end", true, 11))
        } else {
            // نمایش کمان زاویهٔ θ بین دو بردار در حالت ضرب داخلی
            val angA = atan2((oy - ayy).toDouble(), (axx - ox).toDouble())
            val angB = atan2((oy - byy).toDouble(), (bxx - ox).toDouble())
            val rArc = 24f
            val sxArc = ox + rArc * cos(angA).toFloat(); val syArc = oy - rArc * sin(angA).toFloat()
            val exArc = ox + rArc * cos(angB).toFloat(); val eyArc = oy - rArc * sin(angB).toFloat()
            sb.append(path("M ${f(sxArc)} ${f(syArc)} Q ${f(ox + rArc * 1.15f * cos((angA + angB) / 2).toFloat())} ${f(oy - rArc * 1.15f * sin((angA + angB) / 2).toFloat())} ${f(exArc)} ${f(eyArc)}", "none", PT, 1.6f))
            sb.append(tx(ox + 32f * cos((angA + angB) / 2).toFloat(), oy - 32f * sin((angA + angB) / 2).toFloat() + 3f, "θ", PT, "middle", true, 10))
        }

        vecArrow(ox, oy, axx, ayy, ACC, 2.4f, 8.5f)
        vecArrow(ox, oy, bxx, byy, "#d97706", 2.4f, 8.5f)
        sb.append(tx(axx + (if (axx >= ox) 6f else -6f), ayy + (if (ayy <= oy) -6f else 12f), "a(${num(ax1)},${num(ay1)})", ACC, if (axx >= ox) "start" else "end", true, 9))
        sb.append(tx(bxx + (if (bxx >= ox) 6f else -6f), byy + (if (byy <= oy) -6f else 12f), "b(${num(bx)},${num(by)})", "#b45309", if (bxx >= ox) "start" else "end", true, 9))

        if (isDot) {
            val dotVal = ax1 * bx + ay1 * by
            val magA = hypot(ax1, ay1); val magB = hypot(bx, by)
            val cosT = if (magA * magB > 1e-4f) (dotVal / (magA * magB)).coerceIn(-1f, 1f) else 1f
            val deg = (acos(cosT.toDouble()) * 180.0 / PI).roundToInt()
            sb.append(tx(180f, 250f, "ضرب داخلی: a · b = ${num(dotVal)}   |   زاویهٔ θ ≈ ${deg}°", ST, "middle", true, 11))
            sb.append(tx(180f, 269f, "a · b = ax·bx + ay·by = |a||b|cosθ", MU, "middle", false, 9))
        } else {
            val sx = if (diff) ax1 - bx else ax1 + bx
            val sy = if (diff) ay1 - by else ay1 + by
            val magR = hypot(sx, sy)
            if (quiz) {
                sb.append(tx(180f, 252f, "بردار برآیند R و اندازهٔ |R| را بیابید", ST, "middle", true, 11))
            } else {
                sb.append(tx(180f, 250f, (if (diff) "تفاضل بردارها: a − b = (" else "جمع بردارها: a + b = (") + num(sx) + ", " + num(sy) + ")", ST, "middle", true, 11))
                sb.append(tx(180f, 269f, "|R| = √(${num(sx)}² + ${num(sy)}²) ≈ ${num((magR * 100f).roundToInt() / 100f)}", MU, "middle", false, 9))
            }
        }
        return sb.toString()
    }

    private fun matrixMul(spec: FigureSpec): String {
        val op = spec.xStr("op", "mul").lowercase()
        val aRows = spec.xStr("a", "1,2;3,4").split(";", "؛")
        val bRows = spec.xStr("b", "5,6;7,8").split(";", "؛")
        fun get(rows: List<String>, r: Int, c: Int): Float {
            val rr = rows.getOrNull(r)?.split(",", "،") ?: return 0f
            return faEnDigits(rr.getOrNull(c)?.trim() ?: "0").toFloatOrNull() ?: 0f
        }
        val a00 = get(aRows, 0, 0); val a01 = get(aRows, 0, 1); val a10 = get(aRows, 1, 0); val a11 = get(aRows, 1, 1)
        val b00 = get(bRows, 0, 0); val b01 = get(bRows, 0, 1); val b10 = get(bRows, 1, 0); val b11 = get(bRows, 1, 1)
        val detA = a00 * a11 - a01 * a10
        val detB = b00 * b11 - b01 * b10

        val sb = StringBuilder()
        // رسم کروشهٔ برداری تمام‌قد واقعی دور هر ماتریس ۲×۲ (بدون مشکل نصفه‌ماندن کاراکتر «[»)
        fun drawMat(x0: Float, y0: Float, vals: List<String>, col: String, tag: String) {
            val mw = 84f; val mh = 60f
            sb.append(rc(x0 + 4f, y0 + 2f, mw - 8f, mh - 4f, "#f8fafc", "none", 0f, 4f))
            sb.append(ln(x0 + mw / 2f, y0 + 6f, x0 + mw / 2f, y0 + mh - 6f, "#e2e8f0", 1f))
            sb.append(ln(x0 + 8f, y0 + mh / 2f, x0 + mw - 8f, y0 + mh / 2f, "#e2e8f0", 1f))
            // کروشهٔ چپ و راست
            sb.append(path("M ${f(x0 + 9f)} ${f(y0)} L ${f(x0 + 2f)} ${f(y0)} L ${f(x0 + 2f)} ${f(y0 + mh)} L ${f(x0 + 9f)} ${f(y0 + mh)}", "none", col, 2.2f))
            sb.append(path("M ${f(x0 + mw - 9f)} ${f(y0)} L ${f(x0 + mw - 2f)} ${f(y0)} L ${f(x0 + mw - 2f)} ${f(y0 + mh)} L ${f(x0 + mw - 9f)} ${f(y0 + mh)}", "none", col, 2.2f))
            for (r in 0..1) for (c in 0..1) {
                val cellTxt = vals[r * 2 + c]
                val fs = if (cellTxt.length >= 5) 9 else if (cellTxt.length >= 4) 10 else 12
                sb.append(tx(x0 + 23f + c * 38f, y0 + 20f + r * 28f, cellTxt, if (cellTxt == "؟") PT else ST, "middle", true, fs))
            }
            sb.append(tx(x0 + mw / 2f, y0 + mh + 16f, tag, col, "middle", true, 10))
        }
        fun fmt(v: Float): String = num((v * 100f).roundToInt() / 100f)

        if (op == "inv") {
            drawMat(36f, 66f, listOf(fmt(a00), fmt(a01), fmt(a10), fmt(a11)), ACC, "A")
            sb.append(tx(148f, 100f, "⇒", ST, "middle", true, 16))
            if (abs(detA) < 1e-4f) {
                sb.append(rc(176f, 74f, 152f, 44f, "#fff1f2", PT, 1.5f, 8f))
                sb.append(tx(252f, 100f, "det(A) = 0 (وارون‌ناپذیر)", PT, "middle", true, 11))
            } else {
                sb.append(tx(192f, 100f, "1/${fmt(detA)}", PT, "middle", true, 12))
                drawMat(222f, 66f, listOf(fmt(a11), fmt(-a01), fmt(-a10), fmt(a00)), PT, "A⁻¹")
            }
            sb.append(tx(180f, 186f, "|A| = (${fmt(a00)}×${fmt(a11)}) − (${fmt(a01)}×${fmt(a10)}) = ${fmt(detA)}", ST, "middle", true, 11))
            sb.append(tx(180f, 214f, "قانون وارون ۲×۲: جابه‌جایی قطر اصلی و قرینه‌کردن قطر فرعی تقسیم بر |A|", MU, "middle", false, 9))
            sb.append(tx(180f, 242f, "A × A⁻¹ = I₂", ACC, "middle", true, 11))
            return sb.toString()
        }

        val quiz = op == "quiz"
        val (sym, resVals, resTag) = when (op) {
            "add" -> Triple("+", listOf(fmt(a00 + b00), fmt(a01 + b01), fmt(a10 + b10), fmt(a11 + b11)), "A + B")
            "sub" -> Triple("−", listOf(fmt(a00 - b00), fmt(a01 - b01), fmt(a10 - b10), fmt(a11 - b11)), "A − B")
            else -> {
                val c11 = a00 * b00 + a01 * b10
                val c12 = a00 * b01 + a01 * b11
                val c21 = a10 * b00 + a11 * b10
                val c22 = a10 * b01 + a11 * b11
                Triple("×", if (quiz) listOf("؟", "؟", "؟", "؟") else listOf(fmt(c11), fmt(c12), fmt(c21), fmt(c22)), "A × B")
            }
        }

        drawMat(14f, 66f, listOf(fmt(a00), fmt(a01), fmt(a10), fmt(a11)), ACC, "A")
        sb.append(tx(109f, 100f, sym, ST, "middle", true, 16))
        drawMat(120f, 66f, listOf(fmt(b00), fmt(b01), fmt(b10), fmt(b11)), "#d97706", "B")
        sb.append(tx(215f, 100f, "=", ST, "middle", true, 16))
        drawMat(226f, 66f, resVals, PT, resTag)

        if (op == "add" || op == "sub") {
            sb.append(tx(180f, 188f, "درایه‌های متناظر هم‌مکان با هم ${if (op == "add") "جمع" else "تفریق"} می‌شوند", ST, "middle", true, 11))
            sb.append(tx(180f, 214f, "شرط جمع و تفریق: هم‌مرتبه بودن دو ماتریس (۲×۲)", MU, "middle", false, 10))
        } else if (quiz) {
            sb.append(tx(180f, 192f, "حاصل‌ضرب ماتریس A × B را محاسبه کنید (سطر × ستون)", ST, "middle", true, 11))
            sb.append(tx(180f, 218f, "det(A) = ${fmt(detA)}   ·   det(B) = ${fmt(detB)}", MU, "middle", false, 10))
        } else {
            val c11 = a00 * b00 + a01 * b10
            sb.append(tx(180f, 186f, "درایهٔ (۱،۱): ${fmt(a00)}×${fmt(b00)} + ${fmt(a01)}×${fmt(b10)} = ${fmt(c11)}", ST, "middle", true, 11))
            sb.append(tx(180f, 212f, "ضرب ماتریسی: ضرب داخلی هر سطر از A در هر ستون از B", MU, "middle", false, 9))
            sb.append(tx(180f, 238f, "|A| = ${fmt(detA)}  ·  |B| = ${fmt(detB)}  ·  |A×B| = ${fmt(detA * detB)}", ACC, "middle", true, 10))
        }
        return sb.toString()
    }

    private fun definiteIntegral(spec: FigureSpec): String {
        val fn = spec.xStr("f", "quad").lowercase()
        // V322 — بازهٔ منفی/بزرگ نقاط سایه‌زنی را بیرون از بوم می‌گذاشت؛ حالا بازه در ناحیهٔ رسم مهار می‌شود.
        var lo = spec.xNum("lo", 0f).coerceIn(0f, 20f)
        var hi = spec.xNum("hi", 2f).coerceIn(0f, 20f)
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
        steps = steps.take(6)
        val mode = spec.xStr("mode", "branch").lowercase()
        val quiz = mode == "quiz"
        val n = steps.size
        val cx = 166f
        val sb = StringBuilder()

        fun isIoStep(s: String): Boolean {
            val t = s.lowercase()
            return t.contains("ورودی") || t.contains("خروجی") || t.contains("بخوان") || t.contains("چاپ") ||
                t.contains("نمایش") || t.contains("دریافت") || t.contains("input") || t.contains("print")
        }
        fun isDecStep(i: Int, s: String): Boolean =
            i > 0 && i < n - 1 && (s.contains("؟") || s.contains("?") || s.startsWith("اگر "))

        // محاسبهٔ خودکار ارتفاع گره‌ها و فاصله‌ها تا حتی ۶ مرحله بدون برخورد با راهنمای پایین در بوم جا شود
        val heights = steps.mapIndexed { i, s -> if (isDecStep(i, s)) 36f else 24f }
        val totalBoxH = heights.sum()
        val gap = if (n <= 1) 0f else ((214f - totalBoxH) / (n - 1)).coerceIn(10f, 18f)
        val topStart = 20f + max(0f, (214f - (totalBoxH + gap * (n - 1))) / 2f)

        val topY = FloatArray(n)
        val botY = FloatArray(n)
        var curY = topStart
        for (i in 0 until n) {
            topY[i] = curY
            botY[i] = curY + heights[i]
            curY = botY[i] + gap
        }

        fun downArrow(x: Float, y1: Float, y2: Float, col: String = ST) {
            sb.append(ln(x, y1, x, y2 - 5f, col, 1.6f))
            sb.append(poly("${f(x - 4f)},${f(y2 - 6f)} ${f(x + 4f)},${f(y2 - 6f)} ${f(x)},${f(y2)}", col, "none", 0f))
        }
        fun leftArrowHead(x: Float, y: Float, col: String) {
            sb.append(poly("${f(x)},${f(y)} ${f(x + 6.5f)},${f(y - 3.8f)} ${f(x + 6.5f)},${f(y + 3.8f)}", col, "none", 0f))
        }
        fun rightArrowHead(x: Float, y: Float, col: String) {
            sb.append(poly("${f(x)},${f(y)} ${f(x - 6.5f)},${f(y - 3.8f)} ${f(x - 6.5f)},${f(y + 3.8f)}", col, "none", 0f))
        }

        var decIdx = -1
        steps.forEachIndexed { i, rawLbl ->
            val y = topY[i]; val hh = heights[i]; val my = y + hh / 2f
            val isStart = i == 0
            val isEnd = i == n - 1
            val isDec = isDecStep(i, rawLbl)
            if (isDec && decIdx < 0) decIdx = i
            val isIo = !isStart && !isEnd && !isDec && isIoStep(rawLbl)
            val dispLbl = if (quiz && isDec) "شرط ؟" else rawLbl

            when {
                isStart || isEnd -> {
                    sb.append(rc(cx - 58f, y, 116f, hh, if (isStart) "rgba(39,196,168,.24)" else "rgba(214,51,108,.18)", if (isStart) "#0f766e" else PT, 1.8f, hh / 2f))
                }
                isDec -> {
                    sb.append(poly("${f(cx)},${f(y)} ${f(cx + 68f)},${f(my)} ${f(cx)},${f(y + hh)} ${f(cx - 68f)},${f(my)}", "rgba(240,162,2,.22)", "#d97706", 1.8f))
                }
                isIo -> {
                    // متوازی‌الاضلاع استاندارد ISO برای ورودی و خروجی!
                    val sk = 10f
                    sb.append(poly("${f(cx - 60f + sk)},${f(y)} ${f(cx + 66f + sk)},${f(y)} ${f(cx + 60f - sk)},${f(y + hh)} ${f(cx - 66f - sk)},${f(y + hh)}", "rgba(14,165,233,.16)", "#0284c7", 1.7f))
                }
                else -> {
                    sb.append(rc(cx - 64f, y, 128f, hh, "rgba(108,99,245,.15)", ACC, 1.7f, 5f))
                }
            }
            sb.append(tx(cx, my + 4f, dispLbl, if (quiz && isDec) PT else ST, "middle", true, 9))

            if (i < n - 1) {
                if (mode == "loop" && decIdx >= 0 && i == decIdx + 1 && i < n - 1) {
                    // در حالت حلقه، خروجی بدنه به بالای شرط برمی‌گردد و شاخهٔ «خیر» به پایان می‌رود
                    val loopX = 64f
                    val backY = topY[decIdx] - gap / 2f
                    sb.append(path("M ${f(cx - 64f)} ${f(my)} L ${f(loopX)} ${f(my)} L ${f(loopX)} ${f(backY)} L ${f(cx - 4f)} ${f(backY)}", "none", ACC, 1.6f))
                    rightArrowHead(cx - 2f, backY, ACC)
                    sb.append(tx(loopX - 4f, (my + backY) / 2f, "تکرار", ACC, "end", true, 8))
                } else {
                    downArrow(cx, botY[i], topY[i + 1], if (isDec) "#059669" else ST)
                    if (isDec && mode != "seq") {
                        sb.append(tx(cx - 18f, botY[i] + gap * 0.72f, "بله", "#059669", "end", true, 8))
                    }
                }
            }
        }

        // شاخهٔ «خیر» از گوشهٔ راست لوزی تصمیم به گام پایانی
        if (decIdx >= 0 && mode != "seq" && decIdx < n - 1) {
            val decMy = (topY[decIdx] + botY[decIdx]) / 2f
            val endIdx = n - 1
            val endMy = (topY[endIdx] + botY[endIdx]) / 2f
            val rxBranch = 294f
            sb.append(path("M ${f(cx + 68f)} ${f(decMy)} L ${f(rxBranch)} ${f(decMy)} L ${f(rxBranch)} ${f(endMy)} L ${f(cx + 62f)} ${f(endMy)}", "none", PT, 1.6f))
            leftArrowHead(cx + 59f, endMy, PT)
            sb.append(tx(cx + 84f, decMy - 5f, "خیر", PT, "start", true, 8))
        }

        sb.append(tx(180f, 262f, "کپسول: شروع/پایان · متوازی‌الاضلاع: ورودی/خروجی · لوزی: شرط · مستطیل: پردازش", MU, "middle", true, 8))
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
        // V322 — پیش‌تر ارتفاع = ۲۶ + |v|×۶ بود و مقدار ۹۹۹ میله را هزاران پیکسل بالا می‌برد؛
        // حالا تناسب حفظ می‌شود ولی بلندترین میله در ۱۳۶ پیکسل می‌ماند.
        val maxAbs = max(1, arr.maxOfOrNull { abs(it) } ?: 1)
        val x0 = 180f - (arr.size * bw) / 2f
        val sb = StringBuilder()
        arr.forEachIndexed { i, v ->
            val h = 26f + abs(v) / maxAbs.toFloat() * 110f
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

    private fun rcDash(x: Float, y: Float, w: Float, h: Float, fill: String, stroke: String, sw: Float, rx: Float = 0f, dash: String = "4 2"): String =
        "<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(w)}\" height=\"${f(h)}\"" +
            (if (rx > 0f) " rx=\"${f(rx)}\"" else "") + " fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"${f(sw)}\" stroke-dasharray=\"$dash\"/>"

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
        // V322 — همان جاگیری خودکار برچسب (کوچک‌سازی، کوتاه‌سازی با «…» و مهار مختصات در بوم).
        val fit = fitLabel(x, s, anchor, size)
        return "<text x=\"${f(fit.first)}\" y=\"${f(y)}\" direction=\"ltr\" font-family=\"sans-serif\" font-size=\"${fit.third}\"" +
            "$weight fill=\"$color\" text-anchor=\"$anchor\">${esc(FigureDigits.apply(fit.second))}</text>"
    }

    /** (x نهایی، متن نهایی، اندازهٔ فونت) برای جا شدن برچسب در بوم ۳۶۰ پیکسلی. */
    private fun fitLabel(x: Float, s: String, anchor: String, size: Int): Triple<Float, String, Int> {
        val maxW = 344f
        var txt = s
        var fs = size
        var est = txt.length * fs * 0.55f
        if (est > maxW) {
            fs = max(7, (maxW / (txt.length * 0.55f) * fs).toInt())
            est = txt.length * fs * 0.55f
        }
        if (est > maxW) {
            val keep = max(2, (maxW / (fs * 0.55f)).toInt() - 1)
            txt = txt.take(keep) + "…"
            est = txt.length * fs * 0.55f
        }
        val half = est / 2f
        val lo = when (anchor) { "middle" -> half + 3f; "end" -> est + 3f; else -> 3f }
        val hi = when (anchor) { "middle" -> 357f - half; "end" -> 357f; else -> 357f - est }
        val xx = if (lo <= hi) x.coerceIn(lo, hi) else x.coerceIn(3f, 357f)
        return Triple(xx, txt, fs)
    }
}
