package ir.exam.app.core.figure

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * V318 — بستهٔ جامع ابزارهای آموزشی تعاملی (۴۰ ابزار) از دبستان تا دانشگاه.
 *
 * این رندرکننده مکمل [PrimaryMathSvgRenderer] است؛ همان بوم ۳۶۰×۲۸۰ و همان قرارداد `X` وب‌اپ.
 * پورت یک‌به‌یک موتور وب: `graph_fig.js → axisExSvg()` (بلوک V318).
 *
 * گروه‌ها:
 *  • دبستان: کوئیزنر، هرم اعداد، چرخ ضرب، جدول ضرب پازل، گستردهٔ احجام، شمارش مکعب، تقویم،
 *    اهرم/قرقره/چرخ‌دنده، چرخه‌های علوم، قطار کلمه‌سازی.
 *  • متوسطهٔ اول: کاشی‌های جبر، کن‌کن، گستردهٔ تاس، تبدیلات هندسی، کولیس و ریزسنج.
 *  • متوسطهٔ دوم/کنکور: جدول تعیین علامت، دایرهٔ مثلثاتی، تناظر تابع، درخت احتمال، مقاطع مخروطی،
 *    آرایش الکترونی/لوئیس، مربع پانت و شجره‌نامه، نوار قلب و نوسان.
 *  • انسانی/هنر/فنی: تقطیع عروضی، جدول ارزش و مربع تقابل، سند T حسابداری، خطوط حامل و چرخهٔ رنگ.
 *  • دانشگاه: کارنو، گیت منطقی، DFA، درخت BST/هرم، بود/نایکوئیست، تیر و دیاگرام V/M،
 *    دایرهٔ مور، چرخهٔ ترمودینامیکی، شبکهٔ CPM/PERT، بنزن/سیکلوهگزان، منحنی نرمال، عرضه و تقاضا،
 *    ماتریس بازی و IS-LM.
 */
object AdvancedMathSvgRenderer {
    val SUPPORTED: Set<String> = setOf(
        "axrod", "axtower", "axmulwheel", "axmultgrid", "axnet", "axcubes", "axcalendar", "axlever",
        "axcycle", "axwordtrain", "axtiles", "axkenken", "axdicefold", "axtransform", "axcaliper",
        "axsigntable", "axtrigcircle", "axmapping", "axprobtree", "axconic", "axorbital", "axpunnett",
        "axecg", "axprosody", "axtruthtable", "axledger", "axstaff", "axkmap", "axgate", "axdfa",
        "axbst", "axbode", "axbeam", "axmohr", "axpv", "axpert", "axbenzene", "axnormal", "axsupply", "axgame",
        // V327 — رنگ‌آمیزی، فارسی و علوم دبستان
        "axpaintgrid", "axletterforms", "axsyllablechart", "axmagnet", "axsenses", "axseedgrow", "axbodyparts"
    )

    private const val ST = "#2c3a50"
    private const val MU = "#4a5870"
    private const val ACC = "#6c63f5"
    private const val GRN = "#27c4a8"
    private const val GLD = "#f0a202"
    private const val PT = "#d6336c"

    fun body(spec: FigureSpec): String {
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else tx(180f, 14f, title, "#1a2433", "middle", bold = true, size = 13)
        return head + when (spec.type) {
            "axrod" -> rods(spec)
            "axtower" -> numberTower(spec)
            "axmulwheel" -> multiplicationWheel(spec)
            "axmultgrid" -> multiplicationGrid(spec)
            "axnet" -> solidNet(spec)
            "axcubes" -> cubeStack(spec)
            "axcalendar" -> calendar(spec)
            "axlever" -> simpleMachine(spec)
            "axcycle" -> scienceCycle(spec)
            "axwordtrain" -> wordTrain(spec)
            // V327 — ابزارهای تازهٔ دبستان
            "axpaintgrid" -> paintGrid(spec)
            "axletterforms" -> letterForms(spec)
            "axsyllablechart" -> syllableChart(spec)
            "axmagnet" -> magnetSpec(spec)
            "axsenses" -> fiveSenses(spec)
            "axseedgrow" -> seedGrow(spec)
            "axbodyparts" -> bodyParts(spec)
            "axtiles" -> algebraTiles(spec)
            "axkenken" -> kenKen(spec)
            "axdicefold" -> diceFold(spec)
            "axtransform" -> geometricTransform(spec)
            "axcaliper" -> caliper(spec)
            "axsigntable" -> signTable(spec)
            "axtrigcircle" -> trigCircle(spec)
            "axmapping" -> mappingDiagram(spec)
            "axprobtree" -> probabilityTree(spec)
            "axconic" -> conicSection(spec)
            "axorbital" -> orbitalDiagram(spec)
            "axpunnett" -> punnettSquare(spec)
            "axecg" -> ecgStrip(spec)
            "axprosody" -> prosodyScan(spec)
            "axtruthtable" -> truthTable(spec)
            "axledger" -> ledgerT(spec)
            "axstaff" -> musicStaff(spec)
            "axkmap" -> karnaughMap(spec)
            "axgate" -> logicGate(spec)
            "axdfa" -> automaton(spec)
            "axbst" -> binaryTree(spec)
            "axbode" -> bodePlot(spec)
            "axbeam" -> beamDiagrams(spec)
            "axmohr" -> mohrCircle(spec)
            "axpv" -> pvCycle(spec)
            "axpert" -> projectNetwork(spec)
            "axbenzene" -> organicStructure(spec)
            "axnormal" -> normalCurve(spec)
            "axsupply" -> supplyDemand(spec)
            "axgame" -> gameMatrix(spec)
            else -> ""
        }
    }

    // ============================================================ دبستان
    private fun rods(spec: FigureSpec): String {
        val list = ints(spec, "nums", "1,2,3,4,5,6,7,8,9,10").ifEmpty { (1..10).toList() }.take(10)
        val blank = spec.xStr("blank", "0") == "1"
        val pal = listOf("#ffffff", "#e53935", "#8bc34a", "#8e24aa", "#fdd835", "#1b5e20", "#37474f", "#6d4c41", "#1e88e5", "#fb8c00")
        val sb = StringBuilder()
        list.forEachIndexed { i, raw ->
            val u = max(1, min(10, raw))
            val y = 16f + i * 18.5f
            sb.append(rc(44f, y, u * 21f, 14f, pal[u - 1], ST, 1.2f, 3f))
            sb.append(tx(36f, y + 11f, if (blank) "؟" else u.toString(), MU, "end", bold = true, size = 10))
        }
        sb.append(tx(180f, 268f, if (blank) "میله‌ها را بشمار: ؟" else "میله‌های کوئیزنر (۱ تا ۱۰)", ST, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun numberTower(spec: FigureSpec): String {
        var base = ints(spec, "base", "1,2,3,4").ifEmpty { listOf(1, 2, 3, 4) }.take(6)
        val minus = spec.xStr("op", "+") == "-"
        val blank = spec.xStr("blank", "0") == "1"
        val levels = mutableListOf(base)
        var cur = base
        while (cur.size > 1) {
            val next = mutableListOf<Int>()
            for (i in 0 until cur.size - 1) next.add(if (minus) abs(cur[i] - cur[i + 1]) else cur[i] + cur[i + 1])
            levels.add(next); cur = next
        }
        val bh = min(34f, 216f / levels.size)
        val bw = min(58f, 300f / base.size)
        val sb = StringBuilder()
        for (row in levels.indices) {
            val lvIdx = levels.size - 1 - row
            val arr = levels[lvIdx]
            val ly = 24f + row * bh
            for (k in arr.indices) {
                val bx = 180f + (k - (arr.size - 1) / 2f) * bw - bw / 2f
                val hide = blank && (lvIdx + k) % 3 == 1
                sb.append(rc(bx, ly, bw - 3f, bh - 3f, if (hide) "#fff8e1" else if (lvIdx == 0) "rgba(108,99,245,.16)" else "#ffffff", if (hide) PT else ST, 1.6f, 4f))
                sb.append(tx(bx + (bw - 3f) / 2f, ly + bh * 0.66f, if (hide) "؟" else arr[k].toString(), if (hide) PT else ST, "middle", bold = true, size = 13))
            }
        }
        sb.append(tx(180f, 268f, if (minus) "هر آجر = تفاضل دو آجر زیرین" else "هر آجر = جمع دو آجر زیرین", MU, "middle", size = 11))
        return sb.toString()
    }

    private fun multiplicationWheel(spec: FigureSpec): String {
        val c = spec.xNum("c", 3f).toInt().coerceIn(1, 12)
        val n = spec.xNum("n", 10f).toInt().coerceIn(3, 12)
        val plus = spec.xStr("op", "×") == "+"
        val hide = spec.xStr("hide", "0") == "1"
        val sb = StringBuilder()
        sb.append(cls(180f, 142f, 96f, "none", ACC, 1.6f))
        sb.append(cls(180f, 142f, 34f, "rgba(108,99,245,.16)", ACC, 2.2f))
        sb.append(tx(180f, 140f, c.toString(), ACC, "middle", bold = true, size = 18))
        sb.append(tx(180f, 158f, if (plus) "+" else "×", ACC, "middle", bold = true, size = 12))
        for (j in 0 until n) {
            val a = (-PI / 2 + j * 2 * PI / n)
            val ox = 180f + 96f * cos(a).toFloat()
            val oy = 142f + 96f * sin(a).toFloat()
            val v = if (plus) c + (j + 1) else c * (j + 1)
            val h = hide && j % 3 == 2
            sb.append(ln(180f + 34f * cos(a).toFloat(), 142f + 34f * sin(a).toFloat(), ox, oy, "#d5dce6", 1.6f))
            sb.append(cls(ox, oy, 17f, if (h) "#fff8e1" else "#ffffff", if (h) PT else ST, 1.8f))
            sb.append(tx(ox, oy + 5f, if (h) "؟" else v.toString(), if (h) PT else ST, "middle", bold = true, size = 12))
        }
        return sb.toString()
    }

    private fun multiplicationGrid(spec: FigureSpec): String {
        var rows = ints(spec, "rows", "2,4,7").ifEmpty { listOf(2, 4, 7) }.take(4)
        var cols = ints(spec, "cols", "3,5,8").ifEmpty { listOf(3, 5, 8) }.take(4)
        val blank = spec.xNum("blank", 0f).toInt().coerceIn(0, 2)
        val cw = min(56f, 268f / (cols.size + 1))
        val ch = min(42f, 216f / (rows.size + 1))
        val x0 = 180f - cw * (cols.size + 1) / 2f
        val y0 = 26f + (218f - ch * (rows.size + 1)) / 2f
        val sb = StringBuilder()
        for (c in cols.indices) {
            sb.append(rc(x0 + (c + 1) * cw, y0, cw, ch, "rgba(39,196,168,.18)", ST, 1.4f))
            sb.append(tx(x0 + (c + 1.5f) * cw, y0 + ch * 0.66f, if (blank == 2) "؟" else cols[c].toString(), if (blank == 2) PT else ST, "middle", bold = true, size = 14))
        }
        for (r in rows.indices) {
            sb.append(rc(x0, y0 + (r + 1) * ch, cw, ch, "rgba(39,196,168,.18)", ST, 1.4f))
            sb.append(tx(x0 + cw / 2f, y0 + (r + 1.5f) * ch, if (blank == 2) "؟" else rows[r].toString(), if (blank == 2) PT else ST, "middle", bold = true, size = 14))
            for (c in cols.indices) {
                val v = rows[r] * cols[c]
                val h = blank == 1 && (r + c) % 2 == 1
                sb.append(rc(x0 + (c + 1) * cw, y0 + (r + 1) * ch, cw, ch, if (h) "#fff8e1" else "#ffffff", ST, 1.4f))
                sb.append(tx(x0 + (c + 1.5f) * cw, y0 + (r + 1.5f) * ch, if (h) "؟" else v.toString(), if (h) PT else ACC, "middle", bold = true, size = 14))
            }
        }
        sb.append(rc(x0, y0, cw, ch, "rgba(108,99,245,.16)", ST, 1.8f))
        sb.append(tx(x0 + cw / 2f, y0 + ch * 0.66f, "×", ACC, "middle", bold = true, size = 15))
        return sb.toString()
    }

    private fun solidNet(spec: FigureSpec): String {
        val shape = spec.xStr("shape", "cube").lowercase()
        val mode = spec.xStr("mode", "color").lowercase()
        val pal = listOf("#ef5350", "#42a5f5", "#66bb6a", "#fdd835", "#ab47bc", "#fb8c00")
        fun fillOf(i: Int) = if (mode == "outline") "#ffffff" else pal[i % pal.size]
        val sb = StringBuilder()
        fun face(x: Float, y: Float, w: Float, h: Float, i: Int, lbl: String) {
            sb.append(rc(x, y, w, h, fillOf(i), ST, 1.6f))
            sb.append(tx(x + w / 2f, y + h / 2f + 5f, if (mode == "blank") "؟" else lbl, if (mode == "outline") ST else "#ffffff", "middle", bold = true, size = 13))
        }
        if (shape == "cube" || shape == "cuboid") {
            val w = if (shape == "cuboid") 70f else 52f
            val h = 52f
            val x0 = 180f - w * 1.5f
            val y0 = 60f
            face(x0, y0, w, h, 0, "۱"); face(x0 + w, y0, w, h, 1, "۲"); face(x0 + 2 * w, y0, w, h, 2, "۳"); face(x0 + 3 * w, y0, w, h, 3, "۴")
            face(x0 + w, y0 - h, w, h, 4, "۵"); face(x0 + w, y0 + h, w, h, 5, "۶")
        } else if (shape == "pyramid") {
            val u = 52f
            face(180f - u, 96f, u * 2, u * 2, 0, "۱")
            sb.append(poly("${f(180f - u)},96 ${f(180f)},26 ${f(180f + u)},96", fillOf(1), ST, 1.6f))
            sb.append(tx(180f, 76f, if (mode == "blank") "؟" else "۲", if (mode == "outline") ST else "#ffffff", "middle", bold = true, size = 13))
            sb.append(poly("${f(180f - u)},${f(96f + u * 2)} ${f(180f)},${f(96f + u * 3)} ${f(180f + u)},${f(96f + u * 2)}", fillOf(2), ST, 1.6f))
            sb.append(poly("${f(180f - u)},96 ${f(180f - u)},${f(96f + u * 2)} ${f(180f - u * 2)},${f(96f + u)}", fillOf(3), ST, 1.6f))
            sb.append(tx(180f - u * 1.4f, 96f + u, if (mode == "blank") "؟" else "۴", if (mode == "outline") ST else "#ffffff", "middle", bold = true, size = 13))
            sb.append(poly("${f(180f + u)},96 ${f(180f + u)},${f(96f + u * 2)} ${f(180f + u * 2)},${f(96f + u)}", fillOf(4), ST, 1.6f))
            sb.append(tx(180f + u * 1.4f, 96f + u, if (mode == "blank") "؟" else "۵", if (mode == "outline") ST else "#ffffff", "middle", bold = true, size = 13))
        } else {
            val t = 36f; val rw = 52f; val rh = 74f; val rx = 180f - rw * 1.5f; val ry = 92f
            face(rx, ry, rw, rh, 0, "۱"); face(rx + rw, ry, rw, rh, 1, "۲"); face(rx + 2 * rw, ry, rw, rh, 2, "۳")
            sb.append(poly("${f(rx)},${f(ry)} ${f(rx + rw / 2f)},${f(ry - t)} ${f(rx + rw)},${f(ry)}", fillOf(3), ST, 1.6f))
            sb.append(poly("${f(rx)},${f(ry + rh)} ${f(rx + rw / 2f)},${f(ry + rh + t)} ${f(rx + rw)},${f(ry + rh)}", fillOf(4), ST, 1.6f))
        }
        val caption = when (shape) {
            "pyramid" -> "گستردهٔ هرم — ۵ وجه"
            "cuboid" -> "گستردهٔ مکعب‌مستطیل — ۶ وجه"
            "cube" -> "گستردهٔ مکعب — ۶ وجه"
            else -> "گستردهٔ منشور — ۵ وجه"
        }
        sb.append(tx(180f, 268f, caption, MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun cubeStack(spec: FigureSpec): String {
        val view = spec.xStr("view", "iso").lowercase()
        val rowsRaw = spec.xStr("map", "2,1,3;1,0,1;2,2,1").split(Regex("[;؛]"))
        val map = rowsRaw.map { rw -> rw.split(Regex("[,،]")).map { max(0, min(4, faEnDigits(it.trim()).toIntOrNull() ?: 0)) } }.filter { it.isNotEmpty() }.take(4)
        val grid = map.ifEmpty { listOf(listOf(2, 1, 3), listOf(1, 0, 1), listOf(2, 2, 1)) }
        val total = grid.sumOf { r -> r.sum() }
        val sb = StringBuilder()
        if (view == "top" || view == "front" || view == "side") {
            val n = grid.size
            val m = grid.maxOf { it.size }.coerceAtLeast(1)
            val gw = min(52f, 260f / n)
            val gh = min(46f, 200f / m)
            for (r in 0 until n) for (c in 0 until grid[r].size) {
                val hh = grid[r][c]
                val x = 180f + (r - (n - 1) / 2f) * gw - gw / 2f
                val y = 34f + (c - (m - 1) / 2f) * gh
                sb.append(rc(x, y, gw - 3f, gh - 3f, if (hh > 0) "rgba(108,99,245,.22)" else "#ffffff", ST, 1.4f, 3f))
                sb.append(tx(x + (gw - 3f) / 2f, y + gh * 0.62f, if (hh > 0) hh.toString() else "۰", ST, "middle", bold = true, size = 12))
            }
        } else {
            val s = 30f
            for (ir in grid.indices.reversed()) {
                for (ic in grid[ir].indices) {
                    val hh = grid[ir][ic]
                    if (hh > 0) {
                        val ox = 92f + (ic - ir) * s * 0.9f
                        val oy = 196f - (ic + ir) * s * 0.5f
                        val topY = oy - (hh - 1) * s * 0.98f - s
                        sb.append(poly("${f(ox)},${f(topY)} ${f(ox + s * 0.9f)},${f(topY + s * 0.5f)} ${f(ox)},${f(topY + s)} ${f(ox - s * 0.9f)},${f(topY + s * 0.5f)}", "#f7f9fd", "#9fb0c6", 1f))
                        sb.append(poly("${f(ox - s * 0.9f)},${f(topY + s * 0.5f)} ${f(ox)},${f(topY + s)} ${f(ox)},${f(oy)} ${f(ox - s * 0.9f)},${f(oy - s * 0.5f)}", "rgba(108,99,245,.14)", "#9fb0c6", 1f))
                        sb.append(poly("${f(ox)},${f(topY + s)} ${f(ox + s * 0.9f)},${f(topY + s * 0.5f)} ${f(ox + s * 0.9f)},${f(oy - s * 0.5f)} ${f(ox)},${f(oy)}", "rgba(39,196,168,.14)", "#9fb0c6", 1f))
                    }
                }
            }
        }
        val hide = spec.xStr("hide", "0") == "1"
        val cap = when (view) {
            "top" -> "نمای از بالا"
            "front" -> "نمای روبه‌رو"
            "side" -> "نمای از پهلو"
            else -> "تعداد مکعب‌ها: " + if (hide) "؟" else total.toString()
        }
        sb.append(tx(180f, 268f, cap, MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun calendar(spec: FigureSpec): String {
        val month = spec.xStr("month", "مهر")
        val start = spec.xNum("start", 3f).toInt().coerceIn(0, 6)
        val days = spec.xNum("days", 30f).toInt().coerceIn(20, 31)
        val mark = ints(spec, "mark", "12,23")
        val blank = spec.xStr("blank", "0") == "1"
        val wd = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val cw = 44f; val ch = 30f
        val x0 = 180f - cw * 3.5f
        val y0 = 46f
        val sb = StringBuilder()
        sb.append(tx(180f, 30f, "تقویم $month", ST, "middle", bold = true, size = 14))
        for (d in 0..6) {
            sb.append(rc(x0 + d * cw, y0, cw, ch, "rgba(108,99,245,.16)", ST, 1.2f))
            sb.append(tx(x0 + d * cw + cw / 2f, y0 + ch * 0.68f, wd[6 - d], ACC, "middle", bold = true, size = 12))
        }
        for (i in 0 until days) {
            val cell = i + start
            val col = 6 - (cell % 7)
            val row = cell / 7
            val day = i + 1
            val bx = x0 + col * cw
            val by = y0 + ch + row * ch
            val isMark = mark.contains(day)
            val isBlank = blank && day % 5 == 0
            sb.append(rc(bx, by, cw, ch, if (isMark) "rgba(39,196,168,.26)" else "#ffffff", ST, 1.1f))
            sb.append(tx(bx + cw / 2f, by + ch * 0.66f, if (isBlank) "؟" else day.toString(), if (isBlank) PT else if (isMark) "#00796b" else ST, "middle", bold = isMark || isBlank, size = 12))
        }
        return sb.toString()
    }

    private fun simpleMachine(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "lever1").lowercase()
        val load = spec.xNum("load", 60f)
        val effort = spec.xNum("effort", 20f)
        val l1 = max(0.5f, spec.xNum("l1", 2f))
        val l2 = max(0.5f, spec.xNum("l2", 1f))
        val sb = StringBuilder()
        if (kind == "pulley") {
            sb.append(cls(180f, 60f, 26f, "rgba(108,99,245,.16)", ACC, 2.2f))
            sb.append(cls(236f, 110f, 20f, "rgba(39,196,168,.18)", GRN, 2.2f))
            sb.append(ln(180f, 86f, 180f, 130f, ST, 2f)); sb.append(arrow(180f, 130f, 180f, 168f, PT, 2f, 9f))
            sb.append(ln(180f, 34f, 300f, 34f, ST, 2f)); sb.append(ln(256f, 110f, 300f, 110f, ST, 2f))
            sb.append(arrow(300f, 110f, 300f, 168f, ACC, 2f, 9f))
            sb.append(tx(180f, 188f, "بار: ${num(load)} N", PT, "middle", bold = true, size = 12))
            sb.append(tx(300f, 188f, "نیرو: ${num(effort)} N", ACC, "middle", bold = true, size = 12))
            sb.append(tx(180f, 252f, "قرقرهٔ متحرک — مزیت مکانیکی ≈ ۲", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        if (kind == "gears") {
            val ra = 40f; val rb = 22f
            sb.append(cls(140f, 130f, ra, "rgba(108,99,245,.18)", ACC, 2.2f))
            sb.append(cls(140f + ra + rb, 130f, rb, "rgba(240,162,2,.22)", GLD, 2.2f))
            for (j in 0 until 16) {
                val a = j * PI / 8
                sb.append(ln(140f + (ra - 5f) * cos(a).toFloat(), 130f + (ra - 5f) * sin(a).toFloat(), 140f + (ra + 5f) * cos(a).toFloat(), 130f + (ra + 5f) * sin(a).toFloat(), ACC, 3f))
            }
            for (j in 0 until 10) {
                val a = j * PI / 5
                sb.append(ln(140f + ra + (rb - 4f) * cos(a).toFloat(), 130f + (rb - 4f) * sin(a).toFloat(), 140f + ra + (rb + 4f) * cos(a).toFloat(), 130f + (rb + 4f) * sin(a).toFloat(), GLD, 3f))
            }
            sb.append(tx(140f, 136f, "۴۰T", ACC, "middle", bold = true, size = 12))
            sb.append(tx(140f + ra + rb, 136f, "۲۲T", "#a06a00", "middle", bold = true, size = 11))
            sb.append(tx(180f, 240f, "چرخ‌دنده — نسبت انتقال = ${num(max(0.1f, ra / rb))}", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        var fulc = 180f
        var x1 = 55f
        var x2 = 305f
        val y = 150f
        if (kind == "lever2" || kind == "lever3") { x1 = 75f; x2 = 325f; fulc = x1 + 20f }
        var ldX = x1 + 45f
        var efX = x2 - 45f
        when (kind) {
            "lever1" -> { ldX = x1 + 45f; efX = x2 - 45f }
            "lever2" -> { ldX = 220f; efX = x2 - 30f }
            else -> { ldX = x2 - 30f; efX = 220f }
        }
        sb.append(ln(x1, y, x2, y, ST, 7f))
        sb.append(poly("${f(fulc)},${f(y + 4f)} ${f(fulc - 20f)},${f(y + 46f)} ${f(fulc + 20f)},${f(y + 46f)}", "rgba(108,99,245,.25)", ST, 2f))
        sb.append(arrow(ldX, y - 8f, ldX, y - 58f, PT, 3f, 10f))
        sb.append(rc(ldX - 26f, y - 96f, 52f, 34f, "rgba(214,51,108,.16)", PT, 1.8f, 6f))
        sb.append(tx(ldX, y - 74f, "${num(load)} N", PT, "middle", bold = true, size = 11))
        sb.append(arrow(efX, y - 8f, efX, y - 58f, ACC, 3f, 10f))
        sb.append(rc(efX - 26f, y - 96f, 52f, 34f, "rgba(108,99,245,.16)", ACC, 1.8f, 6f))
        sb.append(tx(efX, y - 74f, "${num(effort)} N", ACC, "middle", bold = true, size = 11))
        val name = when (kind) { "lever1" -> "اول"; "lever2" -> "دوم"; else -> "سوم" }
        sb.append(tx(180f, y + 70f, "اهرم نوع $name — مزیت مکانیکی = ${num(l1 / l2)}", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun scienceCycle(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "water").lowercase()
        val labels = when (kind) {
            "frog" -> listOf("تخم", "نوزاد", "قورباغهٔ جوان", "قورباغهٔ بالغ")
            "plant" -> listOf("دانه", "جوانه", "گیاه کوچک", "گیاه گل‌دار")
            "food" -> listOf("خورشید", "گیاه", "گیاه‌خوار", "گوشت‌خوار")
            else -> listOf("تبخیر", "میعان", "بارش", "جمع‌آوری")
        }
        val sb = StringBuilder()
        val cx = 180f; val cy = 138f; val r = 86f
        for (i in 0..3) {
            val a = -PI / 2 + i * PI / 2
            val nx = cx + r * cos(a).toFloat()
            val ny = cy + r * 0.86f * sin(a).toFloat()
            val na = a + PI / 4
            sb.append(path("M ${f(cx + r * 1.02f * cos(na - 0.22).toFloat())} ${f(cy + r * 0.88f * sin(na - 0.22).toFloat())} A ${f(r * 1.02f)} ${f(r * 0.88f)} 0 0 1 ${f(cx + r * 1.02f * cos(na + 0.22).toFloat())} ${f(cy + r * 0.88f * sin(na + 0.22).toFloat())}", "none", ACC, 2.2f))
            val tipX = cx + r * 1.02f * cos(na + 0.22).toFloat()
            val tipY = cy + r * 0.88f * sin(na + 0.22).toFloat()
            val ta = na + PI / 2 + 0.5
            sb.append(poly("${f(tipX)},${f(tipY)} ${f(tipX - 9f * cos(ta - 0.4).toFloat())},${f(tipY - 9f * sin(ta - 0.4).toFloat())} ${f(tipX - 9f * cos(ta + 0.4).toFloat())},${f(tipY - 9f * sin(ta + 0.4).toFloat())}", ACC, "none", 0f))
            sb.append(cls(nx, ny, 30f, "#ffffff", ST, 1.8f))
            sb.append(tx(nx, ny + 5f, labels[i], ST, "middle", bold = true, size = 11))
        }
        val cap = when (kind) {
            "frog" -> "دگردیسی قورباغه"
            "plant" -> "مراحل رشد گیاه"
            "food" -> "زنجیرهٔ غذایی"
            else -> "چرخهٔ آب در طبیعت"
        }
        sb.append(tx(180f, 262f, cap, MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun wordTrain(spec: FigureSpec): String {
        val syl = splitList(spec.xStr("syll", "با,با,با")).take(5).ifEmpty { listOf("با", "با", "با") }
        val word = spec.xStr("word", "بابا")
        val blank = spec.xStr("mode", "join").lowercase() == "blank"
        val x = 180f - (syl.size * 74f) / 2f
        val sb = StringBuilder()
        sb.append(ln(x - 20f, 120f, x + syl.size * 74f + 6f, 120f, ST, 3f))
        syl.forEachIndexed { i, s ->
            val sx = x + i * 74f
            sb.append(poly("${f(sx)},92 ${f(sx + 62f)},92 ${f(sx + 62f)},142 ${f(sx)},142", "rgba(108,99,245,.16)", ST, 1.8f))
            sb.append(cls(sx + 12f, 152f, 7f, "#ffffff", ST, 1.6f))
            sb.append(cls(sx + 50f, 152f, 7f, "#ffffff", ST, 1.6f))
            sb.append(tx(sx + 31f, 128f, s, ST, "middle", bold = true, size = 16))
        }
        sb.append(rc(88f, 186f, 184f, 44f, if (blank) "#fff8e1" else "rgba(39,196,168,.18)", if (blank) PT else GRN, 2f, 8f))
        sb.append(tx(180f, 214f, if (blank) "کلمهٔ ساخته‌شده: ؟" else "کلمه: $word", if (blank) PT else "#00796b", "middle", bold = true, size = 15))
        return sb.toString()
    }

    // ============================================================ متوسطهٔ اول
    private fun algebraTiles(spec: FigureSpec): String {
        var x2 = spec.xNum("x2", 1f).toInt().coerceIn(0, 4)
        var xc = spec.xNum("x", 2f).toInt().coerceIn(0, 8)
        var k = spec.xNum("k", 3f).toInt().coerceIn(0, 12)
        val neg = spec.xStr("neg", "0") == "1"
        if (neg) { xc = -xc; k = -k }
        val a = 54f; val s = 18f
        val x0 = 34f; val y0 = 26f
        val fill = if (neg) "rgba(214,51,108,.18)" else "rgba(108,99,245,.18)"
        val sb = StringBuilder()
        for (i in 0 until x2) {
            val tx0 = x0 + (i % 2) * (a + 8f)
            val ty0 = y0 + (i / 2) * (a + 8f)
            sb.append(rc(tx0, ty0, a, a, fill, ACC, 1.8f, 4f))
            sb.append(tx(tx0 + a / 2f, ty0 + a / 2f + 6f, "x²", ACC, "middle", bold = true, size = 17))
        }
        val colX = x0 + (if (x2 > 0) 2 * (a + 8f) + 6f else 0f)
        for (j in 0 until abs(xc)) {
            val tx0 = colX + (j % 2) * (a + 6f)
            val ty0 = y0 + (j / 2) * (s + 6f)
            sb.append(rc(tx0, ty0, a, s, fill, ACC, 1.5f, 3f))
            sb.append(tx(tx0 + a / 2f, ty0 + s - 4f, "x", ACC, "middle", bold = true, size = 12))
        }
        val colK = colX + (if (xc != 0) 2 * (a + 6f) + 6f else 0f)
        for (t in 0 until abs(k)) {
            val tx0 = colK + (t % 3) * (s + 6f)
            val ty0 = y0 + (t / 3) * (s + 6f)
            sb.append(rc(tx0, ty0, s, s, fill, ST, 1.4f, 3f))
            sb.append(tx(tx0 + s / 2f, ty0 + s - 4f, "۱", ST, "middle", bold = true, size = 11))
        }
        val expr = (if (neg) "−(" else "") + (if (x2 != 0) "$x2" + "x² " else "") +
            (if (xc != 0) "+ ${abs(xc)}x " else "") + (if (k != 0) "+ ${abs(k)}" else "") + (if (neg) ")" else "")
        sb.append(tx(180f, 246f, "مدل کاشی‌ها:  $expr", ST, "middle", bold = true, size = 13))
        sb.append(tx(180f, 266f, "آبی = مثبت • قرمز = منفی", MU, "middle", size = 11))
        return sb.toString()
    }

    private fun kenKen(spec: FigureSpec): String {
        val size = spec.xNum("size", 4f).toInt().coerceIn(3, 6)
        val cages = spec.xStr("cages", "11,12|6|+;13,14|7|+;21,22|3|×;23,24,31,32|10|+;33,34|4|×;41,42,43,44|10|+")
            .split(Regex("[;؛]")).filter { it.isNotBlank() }
        val cell = min(52f, 240f / size)
        val x0 = 180f - cell * size / 2f
        val y0 = 34f
        val owner = HashMap<String, Int>()
        val head = HashMap<String, String>()
        val cellsOf = ArrayList<List<String>>()
        cages.forEachIndexed { ci, cg ->
            val parts = cg.split("|")
            if (parts.size < 2) return@forEachIndexed
            val cells = parts[0].split(Regex("[,،]")).map { it.trim() }.filter { it.isNotEmpty() }
            cellsOf.add(cells)
            cells.forEachIndexed { j, c -> owner[c] = ci; if (j == 0) head[c] = (parts.getOrNull(1) ?: "") + (parts.getOrNull(2) ?: "") }
        }
        val sb = StringBuilder()
        for (r in 1..size) for (c in 1..size) {
            val key = "$r$c"
            val x = x0 + (c - 1) * cell
            val y = y0 + (r - 1) * cell
            sb.append(rc(x, y, cell, cell, "#ffffff", "#b9c4d4", 1f))
            head[key]?.let { sb.append(tx(x + 4f, y + 13f, it, ACC, "start", bold = true, size = 10)) }
        }
        cellsOf.forEach { cells ->
            cells.forEach { c ->
                val r = c.getOrNull(0)?.digitToIntOrNull() ?: return@forEach
                val cc = c.getOrNull(1)?.digitToIntOrNull() ?: return@forEach
                val x = x0 + (cc - 1) * cell
                val y = y0 + (r - 1) * cell
                val up = cells.indexOf("${r - 1}$cc") < 0
                val dn = cells.indexOf("${r + 1}$cc") < 0
                val lf = cells.indexOf("$r${cc - 1}") < 0
                val rt = cells.indexOf("$r${cc + 1}") < 0
                if (up) sb.append(ln(x, y, x + cell, y, ST, 2.6f))
                if (dn) sb.append(ln(x, y + cell, x + cell, y + cell, ST, 2.6f))
                if (lf) sb.append(ln(x, y, x, y + cell, ST, 2.6f))
                if (rt) sb.append(ln(x + cell, y, x + cell, y + cell, ST, 2.6f))
            }
        }
        sb.append(tx(180f, y0 + cell * size + 26f, "کن‌کن — عدد هر سطر و ستون بدون تکرار", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun diceFold(spec: FigureSpec): String {
        val mode = spec.xStr("mode", "dots").lowercase()
        val top = spec.xNum("top", 1f).toInt().coerceIn(1, 6)
        val ask = spec.xStr("ask", "0") == "1"
        val u = 56f
        val x0 = 180f - u * 1.5f
        val y0 = 62f
        val faces = listOf(
            Triple(0, 1, top), Triple(1, 1, top % 6 + 1), Triple(2, 1, (top + 1) % 6 + 1), Triple(3, 1, (top + 2) % 6 + 1),
            Triple(1, 0, 7 - top), Triple(1, 2, 7 - ((top % 6) + 1))
        )
        val pips = mapOf(
            1 to listOf(0 to 0), 2 to listOf(-1 to -1, 1 to 1), 3 to listOf(-1 to -1, 0 to 0, 1 to 1),
            4 to listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1), 5 to listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1),
            6 to listOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)
        )
        val sb = StringBuilder()
        faces.forEachIndexed { fi, fc ->
            val fx = x0 + fc.first * u
            val fy = y0 + fc.second * u
            val v = fc.third
            val hide = ask && fi > 2
            sb.append(rc(fx, fy, u, u, if (hide) "#fff8e1" else "#ffffff", ST, 1.6f))
            if (mode == "num" || hide) sb.append(tx(fx + u / 2f, fy + u / 2f + 6f, if (hide) "؟" else v.toString(), if (hide) PT else ST, "middle", bold = true, size = 18))
            else pips[v]?.forEach { p -> sb.append(cls(fx + u / 2f + p.first * 15f, fy + u / 2f + p.second * 15f, 4.6f, ST, "none", 0f)) }
        }
        sb.append(tx(180f, 250f, "گستردهٔ تاس — مجموع وجه‌های روبه‌رو = ۷", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun geometricTransform(spec: FigureSpec): String {
        val op = spec.xStr("op", "translate").lowercase()
        val shape = spec.xStr("shape", "tri").lowercase()
        val grid = spec.xNum("grid", 8f).toInt().coerceIn(4, 10)
        // V322 — جابه‌جایی بزرگ شکل را با مختصات ده‌ها هزار بیرون از بوم می‌برد؛ به اندازهٔ شبکه مهار می‌شود.
        val dx = spec.xNum("dx", 3f).coerceIn(-4f, 4f)
        val dy = spec.xNum("dy", 2f).coerceIn(-4f, 4f)
        val ang = spec.xNum("ang", 90f)
        val axis = spec.xStr("axis", "y").lowercase()
        val u = min(26f, 240f / grid)
        val ox = 180f - u * grid / 2f
        val oy = 26f + (220f - u * grid) / 2f + u * grid
        val sb = StringBuilder()
        for (i in 0..grid) {
            sb.append(ln(ox + i * u, oy - grid * u, ox + i * u, oy, "#e3e9f2", 1f))
            sb.append(ln(ox, oy - i * u, ox + grid * u, oy - i * u, "#e3e9f2", 1f))
        }
        sb.append(ln(ox, oy, ox + grid * u, oy, ST, 1.8f))
        sb.append(ln(ox, oy, ox, oy - grid * u, ST, 1.8f))
        val base = when (shape) {
            "sq" -> listOf(2f to 2f, 4f to 2f, 4f to 4f, 2f to 4f)
            "rect" -> listOf(2f to 2f, 5f to 2f, 5f to 4f, 2f to 4f)
            else -> listOf(2f to 2f, 5f to 2f, 3f to 4f)
        }
        fun pt(p: Pair<Float, Float>): Pair<Float, Float> = (ox + p.first * u) to (oy - p.second * u)
        fun svg(pts: List<Pair<Float, Float>>, fill: String, col: String): String =
            poly(pts.joinToString(" ") { val q = pt(it); "${f(q.first)},${f(q.second)}" }, fill, col, 2f)
        val mapped = base.map { p ->
            when (op) {
                "rotate" -> {
                    val rad = ang * PI / 180
                    val ddx = p.first - 3f
                    val ddy = p.second - 3f
                    (3f + ddx * cos(rad).toFloat() - ddy * sin(rad).toFloat()) to (3f + ddx * sin(rad).toFloat() + ddy * cos(rad).toFloat())
                }
                "reflect" -> if (axis == "x") p.first to (6f - p.second) else (6f - p.first) to p.second
                else -> (p.first + dx) to (p.second + dy)
            }
        }
        sb.append(svg(base, "rgba(108,99,245,.12)", ACC))
        sb.append(svg(mapped, "rgba(39,196,168,.22)", GRN))
        if (op == "reflect") {
            if (axis == "x") sb.append(ln(ox, oy - 3f * u, ox + grid * u, oy - 3f * u, PT, 1.6f))
            else sb.append(ln(ox + 3f * u, oy, ox + 3f * u, oy - grid * u, PT, 1.6f))
        }
        if (op == "translate") {
            val p1 = pt(base[0]); val p2 = pt(mapped[0])
            sb.append(arrow(p1.first, p1.second, p2.first, p2.second, PT, 1.6f, 8f))
        }
        val name = when (op) {
            "rotate" -> "دوران ${num(ang)}° حول (۳,۳)"
            "reflect" -> "بازتاب نسبت به محور " + if (axis == "x") "x" else "y"
            else -> "انتقال با بردار (${num(dx)}, ${num(dy)})"
        }
        sb.append(tx(180f, 258f, name, MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun caliper(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "vernier").lowercase()
        // V322 — مقدار خوانده‌شده بزرگ‌تر از «حداکثر» عقربه/درجه را بیرون از خط‌کش می‌برد؛ حالا مهار می‌شود.
        val mx = max(10f, spec.xNum("max", 50f)).coerceAtMost(999f)
        val v = max(0f, spec.xNum("val", 23.4f)).coerceAtMost(mx)
        val sb = StringBuilder()
        if (kind == "micrometer") {
            sb.append(rc(40f, 96f, 200f, 30f, "rgba(108,99,245,.14)", ST, 2f, 4f))
            sb.append(rc(240f, 86f, 46f, 50f, "rgba(39,196,168,.18)", ST, 2f, 4f))
            sb.append(rc(28f, 86f, 14f, 50f, ST, "none", 0f, 3f))
            sb.append(ln(286f, 96f, 312f, 96f, ST, 6f)); sb.append(ln(286f, 116f, 312f, 116f, ST, 6f))
            var i = 0
            while (i <= mx.toInt()) {
                val x = 40f + i / mx * 200f
                sb.append(ln(x, 96f, x, if (i % 10 == 0) 112f else 104f, ST, 1.2f))
                if (i % 10 == 0) sb.append(tx(x, 90f, i.toString(), MU, "middle", size = 9))
                i += 5
            }
            val frac = v - kotlin.math.floor(v)
            for (j in 0 until 50) {
                val a = j * PI / 25
                sb.append(ln(263f + 22f * cos(a).toFloat(), 111f + 22f * sin(a).toFloat(), 263f + 22f * cos(a + 0.02).toFloat(), 111f + 22f * sin(a + 0.02).toFloat(), ST, 1f))
            }
            sb.append(ln(263f + 30f * cos(frac * 2 * PI).toFloat(), 111f + 30f * sin(frac * 2 * PI).toFloat(), 263f, 111f, PT, 2.4f))
            sb.append(tx(180f, 210f, "ریزسنج: ${num(v)} mm", ST, "middle", bold = true, size = 15))
            sb.append(tx(180f, 236f, "خواندن: ${num(kotlin.math.floor(v))} mm + ${num(Math.round(frac * 100).toFloat())} صدم", MU, "middle", size = 12))
            return sb.toString()
        }
        sb.append(rc(26f, 110f, 308f, 26f, "#ffffff", ST, 2f, 3f))
        sb.append(rc(26f, 136f, 308f, 24f, "rgba(108,99,245,.10)", ST, 1.6f, 3f))
        sb.append(rc(18f, 96f, 16f, 54f, ST, "none", 0f, 3f))
        sb.append(rc(326f, 96f, 16f, 54f, ST, "none", 0f, 3f))
        sb.append(ln(34f, 80f, 34f, 96f, ST, 4f)); sb.append(ln(326f, 80f, 326f, 96f, ST, 4f))
        var i = 0
        while (i <= mx.toInt()) {
            val x = 34f + i / mx * 292f
            val maj = i % 10 == 0; val mid = i % 5 == 0
            sb.append(ln(x, 110f, x, 110f + if (maj) 14f else if (mid) 9f else 5f, "#0d47a1", if (maj) 1.4f else 0.8f))
            if (maj) sb.append(tx(x, 104f, i.toString(), "#0d47a1", "middle", bold = true, size = 9))
            i += 1
        }
        val off = 34f + v / mx * 292f
        for (j in 0..10) {
            val x = off + j * 4f
            sb.append(ln(x, 136f, x, 136f + if (j % 5 == 0) 14f else 8f, "#00695c", 1.2f))
            if (j % 5 == 0) sb.append(tx(x, 162f, (j / 10).toString(), "#00695c", "middle", size = 8))
        }
        sb.append(cls(off, 148f, 3.4f, PT, "none", 0f))
        sb.append(tx(180f, 206f, "کولیس: ${num(v)} میلی‌متر", ST, "middle", bold = true, size = 15))
        sb.append(tx(180f, 232f, "کولیس ورنیه — دقت ۰٫۱ میلی‌متر", MU, "middle", size = 12))
        return sb.toString()
    }

    // ============================================================ متوسطهٔ دوم
    private fun signTable(spec: FigureSpec): String {
        val roots = ints(spec, "roots", "-2,1,3").take(4).ifEmpty { listOf(-2, 1, 3) }.sorted()
        val mult = ints(spec, "mult", "").toMutableList()
        while (mult.size < roots.size) mult.add(1)
        val lead = if (spec.xNum("lead", 1f) >= 0) 1 else -1
        val cols = roots.size + 1
        val width = min(300f, 62f * cols + 20f * roots.size)
        val cw = (width - 26f) / cols
        val x0 = 180f - width / 2f + 13f
        val y0 = 34f
        val rh = min(30f, 150f / (roots.size + 2))
        val samples = (0 until cols).map { i ->
            when {
                i == 0 -> roots[0] - 1f
                i == cols - 1 -> roots.last() + 1f
                else -> (roots[i - 1] + roots[i]) / 2f
            }
        }
        val sb = StringBuilder()
        sb.append(rc(x0, y0, cw * cols, rh, "rgba(108,99,245,.16)", ST, 1.4f))
        sb.append(tx(x0 - 6f, y0 + rh * 0.68f, "x", ST, "end", bold = true, size = 12))
        for (j in 0 until cols) {
            val lo = if (j == 0) "−∞" else num(roots[j - 1].toFloat())
            val hi = if (j == cols - 1) "+∞" else num(roots[j].toFloat())
            sb.append(tx(x0 + j * cw + cw / 2f, y0 + rh * 0.68f, "($lo, $hi)", MU, "middle", bold = true, size = 10))
            if (j < cols - 1) sb.append(ln(x0 + (j + 1) * cw, y0, x0 + (j + 1) * cw, y0 + rh, ST, 1.2f))
        }
        roots.forEachIndexed { i, rt ->
            val ry = y0 + rh * (i + 1)
            val even = abs(mult[i]) % 2 == 0
            sb.append(ln(x0, ry, x0 + cw * cols, ry, ST, 1.2f))
            val fname = "(x " + (if (rt < 0) "+ ${-rt}" else "− $rt") + ")" + (if (even) "²" else "")
            sb.append(tx(x0 - 6f, ry + rh * 0.68f, fname, ST, "end", bold = true, size = 10))
            for (j in 0 until cols) {
                val s = if (even) 1 else if (samples[j] > rt) 1 else -1
                sb.append(tx(x0 + j * cw + cw / 2f, ry + rh * 0.68f, if (s > 0) "+" else "−", if (s > 0) "#1b7f4c" else PT, "middle", bold = true, size = 14))
                if (j < cols - 1) sb.append(ln(x0 + (j + 1) * cw, ry, x0 + (j + 1) * cw, ry + rh, "#c8d2e0", 1f))
            }
            sb.append(tx(x0 + cw * cols + 4f, ry + rh * 0.68f, "۰", PT, "start", bold = true, size = 12))
        }
        val fy = y0 + rh * (roots.size + 1)
        sb.append(ln(x0, fy, x0 + cw * cols, fy, ST, 1.6f))
        sb.append(tx(x0 - 6f, fy + rh * 0.68f, "f(x)", ACC, "end", bold = true, size = 11))
        for (j in 0 until cols) {
            var s = lead
            roots.forEachIndexed { i, rt -> s *= if (abs(mult[i]) % 2 == 0) 1 else if (samples[j] > rt) 1 else -1 }
            sb.append(tx(x0 + j * cw + cw / 2f, fy + rh * 0.68f, if (s > 0) "+" else "−", if (s > 0) "#1b7f4c" else PT, "middle", bold = true, size = 15))
            if (j < cols - 1) sb.append(ln(x0 + (j + 1) * cw, fy, x0 + (j + 1) * cw, fy + rh, ST, 1.2f))
        }
        sb.append(rc(x0, fy, cw * cols, rh, "rgba(39,196,168,.12)", ST, 1.6f))
        sb.append(tx(180f, fy + rh + 28f, "جدول تعیین علامت — ریشه‌ها: " + roots.joinToString(" , ") { num(it.toFloat()) }, MU, "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun trigCircle(spec: FigureSpec): String {
        val deg = spec.xNum("deg", 135f)
        val show = spec.xStr("show", "1") != "0"
        val cx = 176f; val cy = 140f; val r = 96f
        val rad = deg * PI / 180
        val px = cx + r * cos(rad).toFloat()
        val py = cy - r * sin(rad).toFloat()
        val sb = StringBuilder()
        sb.append(cls(cx, cy, r, "rgba(227,242,253,.5)", "#1565c0", 1.8f))
        sb.append(ln(cx - r - 22f, cy, cx + r + 22f, cy, ST, 1.4f)); sb.append(arrow(cx + r + 14f, cy, cx + r + 30f, cy, ST, 1.4f, 7f))
        sb.append(ln(cx, cy + r + 22f, cx, cy - r - 22f, ST, 1.4f)); sb.append(arrow(cx, cy - r - 14f, cx, cy - r - 30f, ST, 1.4f, 7f))
        sb.append(tx(cx + r + 32f, cy + 4f, "x", ST, "start", bold = true, size = 12))
        sb.append(tx(cx + 6f, cy - r - 26f, "y", ST, "start", bold = true, size = 12))
        sb.append(ln(cx, cy, px, py, PT, 2.4f)); sb.append(cls(px, py, 4.6f, PT, "none", 0f))
        if (show) {
            sb.append(path("M ${f(px)} ${f(py)} L ${f(px)} ${f(cy)} L ${f(cx)} ${f(cy)}", "none", GRN, 1.6f, "4 3"))
            sb.append(ln(px, cy, px, py, "rgba(39,196,168,.35)", 6f))
            sb.append(ln(cx, cy, px, cy, "rgba(108,99,245,.35)", 6f))
            sb.append(tx(px + if (px >= cx) 7f else -7f, (py + cy) / 2f + 4f, "sin", GRN, if (px >= cx) "start" else "end", bold = true, size = 11))
            sb.append(tx((cx + px) / 2f, cy - 7f, "cos", ACC, "middle", bold = true, size = 11))
        }
        sb.append(path("M ${f(cx + 34f)} ${f(cy)} A 34 34 0 0 0 ${f(cx + 34f * cos(rad).toFloat())} ${f(cy - 34f * sin(rad).toFloat())}", "none", PT, 1.8f))
        sb.append(tx(cx + 48f * cos(rad / 2).toFloat(), cy - 48f * sin(rad / 2).toFloat() + 4f, "${Math.round(deg)}°", PT, "middle", bold = true, size = 11))
        sb.append(tx(cx - 12f, cy + 16f, "O", MU, "middle", bold = true, size = 11))
        sb.append(tx(cx + r + 4f, cy + 16f, "(1,0)", MU, "middle", size = 9))
        val sn = Math.round(sin(rad) * 1000f) / 1000.0
        val cs = Math.round(cos(rad) * 1000f) / 1000.0
        sb.append(tx(180f, 254f, "sin ${Math.round(deg)}° = $sn   •   cos ${Math.round(deg)}° = $cs", ST, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun mappingDiagram(spec: FigureSpec): String {
        val from = splitList(spec.xStr("from", "1,2,3")).take(5).ifEmpty { listOf("1", "2", "3") }
        val to = splitList(spec.xStr("to", "a,b,c")).take(5).ifEmpty { listOf("a", "b", "c") }
        val arrows = spec.xStr("arrows", "1-a;2-a;3-b").split(Regex("[;؛]")).map { it.trim().split(Regex("[-:→]")).map { s -> s.trim() } }.filter { it.size >= 2 }
        val lx = 96f; val rx = 264f; val top = 44f
        val step = min(44f, 190f / max(from.size, to.size))
        val sb = StringBuilder()
        sb.append("<ellipse cx=\"${f(lx)}\" cy=\"${f(top + 90f)}\" rx=\"52\" ry=\"104\" fill=\"rgba(108,99,245,.10)\" stroke=\"$ACC\" stroke-width=\"2\"/>")
        sb.append("<ellipse cx=\"${f(rx)}\" cy=\"${f(top + 90f)}\" rx=\"52\" ry=\"104\" fill=\"rgba(39,196,168,.10)\" stroke=\"$GRN\" stroke-width=\"2\"/>")
        sb.append(tx(lx, 30f, "دامنه", ACC, "middle", bold = true, size = 12))
        sb.append(tx(rx, 30f, "برد", "#00796b", "middle", bold = true, size = 12))
        val pos = HashMap<String, Float>()
        from.forEachIndexed { i, v -> val yy = top + 30f + i * step; pos[v] = yy; sb.append(cls(lx, yy, 4f, ACC, "none", 0f)); sb.append(tx(lx + 10f, yy + 5f, v, ST, "start", bold = true, size = 13)) }
        to.forEachIndexed { i, v -> val yy = top + 30f + i * step; pos[v] = yy; sb.append(cls(rx, yy, 4f, GRN, "none", 0f)); sb.append(tx(rx + 10f, yy + 5f, v, ST, "start", bold = true, size = 13)) }
        var isFn = true
        val seen = HashSet<String>()
        arrows.forEach { pr ->
            val y1 = pos[pr[0]]; val y2 = pos[pr[1]]
            if (y1 != null && y2 != null) {
                if (!seen.add(pr[0])) isFn = false
                sb.append(arrow(lx + 12f, y1, rx - 12f, y2, "rgba(214,51,108,.85)", 1.6f, 7f))
            }
        }
        sb.append(tx(180f, 258f, (if (isFn) "این تناظر یک تابع است" else "این تناظر تابع نیست") + " — ${arrows.size} پیکان", if (isFn) "#1b7f4c" else PT, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun probabilityTree(spec: FigureSpec): String {
        var a = splitList(spec.xStr("first", "شیر,خط")).take(3).ifEmpty { listOf("شیر", "خط") }
        var b = splitList(spec.xStr("second", "شیر,خط")).take(3).ifEmpty { listOf("شیر", "خط") }
        val showProb = spec.xStr("mode", "prob") != "count"
        val sb = StringBuilder()
        val rootX = 56f; val rootY = 140f
        sb.append(cls(rootX, rootY, 5f, ST, "none", 0f))
        val ys = if (a.size == 1) listOf(140f) else a.indices.map { 60f + it * (170f / max(1, a.size - 1)) }
        a.forEachIndexed { i, av ->
            val y1 = ys[i]
            sb.append(arrow(rootX, rootY, 142f, y1, ST, 1.6f, 7f))
            sb.append(tx((rootX + 152f) / 2f - 6f, (rootY + y1) / 2f - 6f, av, ST, "middle", bold = true, size = 11))
            b.forEachIndexed { j, bv ->
                val y2 = y1 + (j - (b.size - 1) / 2f) * 40f
                sb.append(arrow(152f, y1, 274f, y2, "rgba(108,99,245,.8)", 1.4f, 6f))
                sb.append(tx(206f, (y1 + y2) / 2f - 5f, bv, MU, "middle", size = 10))
                val lab = if (showProb) "${Math.round(100f / (a.size * b.size))}٪" else "$av , $bv"
                sb.append(tx(290f, y2 + 4f, lab, "#00796b", "start", bold = true, size = 10))
            }
            if (showProb) sb.append(tx(154f, y1 - 8f, "${Math.round(100f / a.size)}٪", ACC, "start", bold = true, size = 10))
        }
        sb.append(tx(180f, 262f, "درخت احتمال دو مرحله‌ای", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun conicSection(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "ellipse").lowercase()
        val a = max(0.5f, spec.xNum("a", 4f))
        val b = max(0.5f, spec.xNum("b", 2f))
        val cx = 180f; val cy = 136f
        val s = min(min(240f / (2 * max(a, b) + 2), 92f / b), 26f)
        val sb = StringBuilder()
        sb.append(ln(cx - 150f, cy, cx + 158f, cy, ST, 1.4f)); sb.append(arrow(cx + 146f, cy, cx + 164f, cy, ST, 1.4f, 7f))
        sb.append(ln(cx, cy + 110f, cx, cy - 112f, ST, 1.4f)); sb.append(arrow(cx, cy - 100f, cx, cy - 118f, ST, 1.4f, 7f))
        sb.append(tx(cx + 164f, cy + 16f, "x", ST, "start", bold = true, size = 12))
        sb.append(tx(cx - 16f, cy - 106f, "y", ST, "start", bold = true, size = 12))
        if (kind == "hyperbola") {
            sb.append(ln(cx - 130f, cy - 88f, cx + 130f, cy + 88f, "#b0bec5", 1f, "5 4"))
            sb.append(ln(cx - 130f, cy + 88f, cx + 130f, cy - 88f, "#b0bec5", 1f, "5 4"))
            sb.append(path("M ${f(cx - a * s)} ${f(cy - 104f)} Q ${f(cx - a * s * 0.35f)} ${f(cy)} ${f(cx - a * s)} ${f(cy + 104f)}", "none", PT, 2.4f))
            sb.append(path("M ${f(cx + a * s)} ${f(cy - 104f)} Q ${f(cx + a * s * 0.35f)} ${f(cy)} ${f(cx + a * s)} ${f(cy + 104f)}", "none", PT, 2.4f))
        } else if (kind == "parabola") {
            sb.append(path("M ${f(cx - 120f)} ${f(cy - 96f)} Q ${f(cx - 120f)} ${f(cy)} ${f(cx)} ${f(cy)} Q ${f(cx + 120f)} ${f(cy)} ${f(cx + 120f)} ${f(cy - 96f)}", "none", PT, 2.4f))
            sb.append(ln(cx, cy, cx, cy - 96f, "#b0bec5", 1f, "5 4"))
            sb.append(ln(cx - 132f, cy - 96f, cx + 132f, cy - 96f, GRN, 1.6f, "6 4"))
            sb.append(cls(cx, cy - 40f, 4.4f, PT, "none", 0f))
            sb.append(tx(cx + 8f, cy - 38f, "F", PT, "start", bold = true, size = 12))
        } else {
            sb.append("<ellipse cx=\"${f(cx)}\" cy=\"${f(cy)}\" rx=\"${f(a * s)}\" ry=\"${f(b * s)}\" fill=\"rgba(108,99,245,.10)\" stroke=\"$ACC\" stroke-width=\"2.2\"/>")
            val fr = sqrt(max(0f, a * a - b * b)) * s
            sb.append(cls(cx + fr, cy, 4.2f, PT, "none", 0f)); sb.append(cls(cx - fr, cy, 4.2f, PT, "none", 0f))
            sb.append(tx(cx + fr + 6f, cy - 8f, "F'", PT, "start", bold = true, size = 11))
            sb.append(tx(cx - fr - 16f, cy - 8f, "F", PT, "start", bold = true, size = 11))
            sb.append(ln(cx, cy, cx + a * s, cy, ACC, 1.4f)); sb.append(tx(cx + a * s / 2f, cy + 15f, "a", ACC, "middle", bold = true, size = 11))
            sb.append(ln(cx, cy, cx, cy - b * s, GRN, 1.4f)); sb.append(tx(cx + 8f, cy - b * s / 2f, "b", "#00796b", "start", bold = true, size = 11))
        }
        val cap = when (kind) { "hyperbola" -> "هذلولی"; "parabola" -> "سهمی"; else -> "بیضی" }
        sb.append(tx(180f, 262f, cap, MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun orbitalDiagram(spec: FigureSpec): String {
        val z = spec.xNum("z", 26f).toInt().coerceIn(1, 36)
        val mode = spec.xStr("mode", "box").lowercase()
        val sb = StringBuilder()
        if (mode == "lewis") {
            val sym = spec.xStr("sym", "Na")
            val valE = spec.xNum("val", 1f).toInt().coerceIn(1, 8)
            sb.append(cls(178f, 120f, 52f, "rgba(108,99,245,.10)", ACC, 2.2f))
            sb.append(tx(178f, 136f, sym, ST, "middle", bold = true, size = 34))
            for (i in 0 until valE) {
                val a = -PI / 2 + i * PI / 4
                sb.append(cls(178f + 68f * cos(a).toFloat(), 120f + 68f * sin(a).toFloat(), 5f, PT, "none", 0f))
            }
            sb.append(tx(180f, 226f, "ساختار لوئیس — $valE الکترون ظرفیت", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        val subs = listOf("1s" to 2, "2s" to 2, "2p" to 6, "3s" to 2, "3p" to 6, "4s" to 2, "3d" to 10, "4p" to 6)
        var left = z
        val boxes = mutableListOf<Triple<String, Int, Int>>()
        for ((name, cap) in subs) {
            if (left <= 0) break
            val take = min(cap, left); left -= take
            boxes.add(Triple(name, cap, take))
        }
        val bw = 36f
        boxes.forEachIndexed { i, bx ->
            val row = i / 3; val col = i % 3
            val x = 34f + col * 100f
            val y = 44f + row * 66f
            sb.append(tx(x, y - 8f, bx.first, ST, "start", bold = true, size = 12))
            val orbs = bx.second / 2
            for (j in 0 until orbs) {
                val bxX = x + j * (bw + 2f)
                val occ = bx.third - j * 2
                sb.append(rc(bxX, y, bw, 24f, if (occ > 0) "rgba(108,99,245,.10)" else "#ffffff", ST, 1.4f, 3f))
                if (occ == 1) sb.append(tx(bxX + bw / 2f, y + 17f, "↑", ACC, "middle", bold = true, size = 12))
                else if (occ >= 2) {
                    sb.append(tx(bxX + bw / 2f - 7f, y + 17f, "↑", ACC, "middle", bold = true, size = 12))
                    sb.append(tx(bxX + bw / 2f + 7f, y + 17f, "↓", PT, "middle", bold = true, size = 12))
                }
            }
        }
        sb.append(tx(180f, 262f, "آرایش الکترونی اتم شمارهٔ $z", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun punnettSquare(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "punnett").lowercase()
        val sb = StringBuilder()
        if (kind == "pedigree") {
            sb.append(poly("${f(180f)},34 ${f(196f)},62 ${f(164f)},62", "#ffffff", ST, 1.8f))
            sb.append(cls(180f, 88f, 14f, "rgba(108,99,245,.18)", ST, 1.8f))
            sb.append(cls(244f, 88f, 14f, "#ffffff", ST, 1.8f))
            sb.append(ln(180f, 62f, 180f, 74f, ST, 1.6f)); sb.append(ln(180f, 74f, 244f, 74f, ST, 1.6f))
            sb.append(ln(212f, 74f, 212f, 102f, ST, 1.6f))
            sb.append(ln(196f, 130f, 236f, 130f, ST, 1.6f)); sb.append(ln(216f, 130f, 216f, 146f, ST, 1.6f))
            sb.append(rc(203f, 146f, 26f, 26f, "rgba(214,51,108,.25)", ST, 1.8f))
            sb.append(cls(180f, 171f, 13f, "#ffffff", ST, 1.8f))
            sb.append(cls(252f, 171f, 13f, "rgba(214,51,108,.25)", ST, 1.8f))
            sb.append(ln(196f, 130f, 196f, 158f, ST, 1.6f)); sb.append(ln(236f, 130f, 236f, 158f, ST, 1.6f))
            sb.append(ln(180f, 102f, 180f, 108f, ST, 1.6f)); sb.append(ln(244f, 102f, 244f, 108f, ST, 1.6f))
            sb.append(tx(180f, 230f, "مربع = پسر   ●  دایره = دختر   •  رنگی = بیمار", MU, "middle", bold = true, size = 11))
            sb.append(tx(180f, 254f, "شجره‌نامهٔ وراثت", ST, "middle", bold = true, size = 13))
            return sb.toString()
        }
        var p1 = spec.xStr("p1", "Aa").ifBlank { "Aa" }
        var p2 = spec.xStr("p2", "Aa").ifBlank { "Aa" }
        if (p1.length != 2) p1 = "Aa"
        if (p2.length != 2) p2 = "Aa"
        val mode = spec.xStr("mode", "genotype").lowercase()
        sb.append(rc(42f, 84f, 60f, 34f, "rgba(108,99,245,.16)", ACC, 1.8f, 6f))
        sb.append(tx(72f, 106f, p1, ACC, "middle", bold = true, size = 15))
        sb.append(rc(42f, 126f, 60f, 34f, "rgba(39,196,168,.16)", GRN, 1.8f, 6f))
        sb.append(tx(72f, 148f, p2, GRN, "middle", bold = true, size = 15))
        fun dominant(g1: Char, g2: Char): String = if (g1.isUpperCase()) "$g1$g2" else if (g2.isUpperCase()) "$g2$g1" else "$g1$g2"
        val cells = ArrayList<String>()
        for (a in 0..1) for (b in 0..1) cells.add(dominant(p1[a], p2[b]))
        val u = 54f; val x0 = 148f; val y0 = 84f
        for (r in 0..1) {
            sb.append(tx(x0 + r * u + u / 2f, y0 - 8f, p2[r].toString(), GRN, "middle", bold = true, size = 13))
            sb.append(tx(x0 - 14f, y0 + r * u + u / 2f + 5f, p1[r].toString(), ACC, "middle", bold = true, size = 13))
            for (c in 0..1) {
                val cell = cells[r * 2 + c]
                val filled = cell.all { it.isUpperCase() }
                sb.append(rc(x0 + c * u, y0 + r * u, u, u, if (filled) "rgba(214,51,108,.16)" else "#ffffff", ST, 1.8f))
                sb.append(tx(x0 + c * u + u / 2f, y0 + r * u + u / 2f + 6f, cell, ST, "middle", bold = true, size = 16))
            }
        }
        sb.append(tx(180f, 238f, if (mode == "ratio") "نسبت فنوتیپی: ۳ : ۱" else "تلاقی $p1 × $p2", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun ecgStrip(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "ecg").lowercase()
        val sb = StringBuilder()
        if (kind == "shm") {
            val amp = max(0.2f, spec.xNum("A", 1f))
            val per = max(0.5f, spec.xNum("T", 2f))
            val x0 = 40f; val x1 = 330f; val y = 140f
            val a = 62f * amp
            sb.append(ln(x0, y, x1 + 8f, y, ST, 1.6f)); sb.append(arrow(x1, y, x1 + 12f, y, ST, 1.6f, 7f))
            sb.append(ln(x0, y + 80f, x0, y - 84f, ST, 1.6f)); sb.append(arrow(x0, y - 74f, x0, y - 90f, ST, 1.6f, 7f))
            val pts = StringBuilder()
            for (i in 0..140) {
                val xx = x0 + (x1 - x0) * i / 140f
                val yy = y - a * sin(2 * PI * (i / 140.0) * 2).toFloat()
                pts.append("${f(xx)},${f(yy)} ")
            }
            sb.append("<polyline points=\"${pts.toString().trim()}\" fill=\"none\" stroke=\"$PT\" stroke-width=\"2.2\"/>")
            sb.append(ln(x0, y - a, x0 + 6f, y - a, GRN, 1.4f))
            sb.append(tx(x0 + 10f, y - a + 4f, "A", "#00796b", "start", bold = true, size = 11))
            sb.append(tx(182f, 258f, "نوسان هماهنگ ساده — دامنه ${num(amp)} ، دوره ${num(per)}", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        val n = spec.xNum("n", 2f).toInt().coerceIn(1, 3)
        val label = spec.xStr("label", "1") != "0"
        for (gj in 0 until 30) for (gi in 0 until 18) {
            sb.append("<circle cx=\"${f(36f + gj * 10f)}\" cy=\"${f(40f + gi * 10f)}\" r=\"0.9\" fill=\"#f3b6c2\"/>")
        }
        val x0 = 34f; val w = 300f / n; val y = 168f
        val rel = listOf(
            0f to 0f, 0.10f to 0f, 0.16f to -12f, 0.22f to 0f, 0.30f to 0f,
            0.34f to 8f, 0.38f to -74f, 0.42f to 22f, 0.46f to 0f, 0.60f to 0f, 0.68f to -22f, 0.76f to 0f, 1f to 0f
        )
        var d = ""
        for (b in 0 until n) {
            val ex = x0 + b * w
            d += if (b == 0) "M ${f(ex)} ${f(y)} " else "L ${f(ex)} ${f(y)} "
            rel.forEach { p -> d += "L ${f(ex + p.first * w)} ${f(y + p.second)} " }
        }
        sb.append(ln(x0 - 8f, y, x0 + w * n + 8f, y, "#9aa7ba", 1f))
        sb.append(path(d, "none", PT, 2.2f))
        if (label) for (b in 0 until n) {
            val ex = x0 + b * w
            sb.append(tx(ex + 0.16f * w, y - 26f, "P", "#0d47a1", "middle", bold = true, size = 10))
            sb.append(tx(ex + 0.38f * w, y - 86f, "QRS", "#0d47a1", "middle", bold = true, size = 10))
            sb.append(tx(ex + 0.68f * w, y - 36f, "T", "#0d47a1", "middle", bold = true, size = 10))
        }
        sb.append(tx(180f, 258f, if (kind == "ecg") "نوار قلب (ECG) — موج‌های P و QRS و T" else "نوار نوسان‌نگار", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    // ============================================================ انسانی و هنر
    private fun prosodyScan(spec: FigureSpec): String {
        var arkan = spec.xStr("line", "فعولن فعولن فعولن فعل").split(Regex("\\s+")).filter { it.isNotBlank() }.take(8)
        if (arkan.isEmpty()) arkan = listOf("فعولن")
        val showTaqti = spec.xStr("mode", "arkan").lowercase() == "taqti"
        val dict = mapOf(
            "فعولن" to "∪ − −", "مفاعیلن" to "∪ − − −", "فاعلاتن" to "− ∪ − −", "مستفعلن" to "− − ∪ −",
            "متفاعلن" to "∪ − ∪ −", "مفتعلن" to "− ∪ ∪ −", "مفاعلن" to "∪ − ∪ −", "فاعلن" to "− ∪ −",
            "فع" to "− ∪", "فعل" to "− −", "فعول" to "∪ − −", "مفاعیل" to "∪ − − −", "فعولات" to "∪ − − −"
        )
        val step = min(96f, 300f / arkan.size)
        val sb = StringBuilder()
        arkan.forEachIndexed { i, ar ->
            val sym = dict[ar] ?: "∪ − −"
            val bw = step - 10f
            val bx = 30f + i * step
            sb.append(rc(bx, 44f, bw, 40f, "rgba(108,99,245,.10)", ACC, 1.4f, 5f))
            sb.append(tx(bx + bw / 2f, 70f, ar, ST, "middle", bold = true, size = 13))
            sb.append(rc(bx, 96f, bw, 34f, "#ffffff", ST, 1.2f, 5f))
            sb.append(tx(bx + bw / 2f, 119f, sym, "#0d47a1", "middle", bold = true, size = 13))
            if (!showTaqti) sb.append(tx(bx + bw / 2f, 150f, "رکن ${i + 1}", MU, "middle", size = 9))
        }
        sb.append(tx(180f, 226f, if (showTaqti) "تقطیع: " + arkan.joinToString("   ") { dict[it] ?: "∪ − −" } else "هجاهای کوتاه (∪) و بلند (−)", MU, "middle", bold = true, size = 11))
        sb.append(tx(180f, 250f, "وزن شعر: " + arkan.joinToString(" "), ST, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun truthTable(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "truth").lowercase()
        val sb = StringBuilder()
        if (kind == "square") {
            sb.append(rc(126f, 34f, 108f, 44f, "rgba(108,99,245,.14)", ACC, 1.8f, 6f))
            sb.append(tx(180f, 62f, "موجبهٔ کلی", ACC, "middle", bold = true, size = 11))
            sb.append(rc(126f, 182f, 108f, 44f, "rgba(108,99,245,.14)", ACC, 1.8f, 6f))
            sb.append(tx(180f, 210f, "موجبهٔ جزئی", ACC, "middle", bold = true, size = 11))
            sb.append(rc(24f, 108f, 104f, 44f, "rgba(39,196,168,.14)", GRN, 1.8f, 6f))
            sb.append(tx(76f, 136f, "سالبهٔ کلی", "#00796b", "middle", bold = true, size = 11))
            sb.append(rc(232f, 108f, 104f, 44f, "rgba(39,196,168,.14)", GRN, 1.8f, 6f))
            sb.append(tx(284f, 136f, "سالبهٔ جزئی", "#00796b", "middle", bold = true, size = 11))
            sb.append(ln(150f, 78f, 120f, 108f, PT, 1.4f)); sb.append(tx(124f, 96f, "تضاد", PT, "middle", bold = true, size = 9))
            sb.append(ln(210f, 78f, 240f, 108f, PT, 1.4f)); sb.append(tx(238f, 96f, "تضاد", PT, "middle", bold = true, size = 9))
            sb.append(ln(150f, 182f, 120f, 152f, PT, 1.4f)); sb.append(tx(124f, 174f, "تداخل", PT, "middle", bold = true, size = 9))
            sb.append(ln(210f, 182f, 240f, 152f, PT, 1.4f)); sb.append(tx(238f, 174f, "تداخل", PT, "middle", bold = true, size = 9))
            sb.append(ln(180f, 78f, 180f, 182f, "#9aa7ba", 1.4f, "5 4"))
            sb.append(tx(180f, 130f, "تناقض", "#607d8b", "middle", bold = true, size = 9))
            sb.append(tx(180f, 254f, "مربع تقابل ارسطویی", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        val rows = listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        val expr = spec.xStr("expr", "p و q")
        val cols = listOf("p", "q", "p ∧ q", "p ∨ q", "p → q")
        val widths = listOf(34f, 34f, 60f, 60f, 60f)
        val wTotal = widths.sum()
        var x = 180f - wTotal / 2f
        val y0 = 52f
        val rh = 30f
        var cursor = x
        cols.forEachIndexed { i, c ->
            sb.append(rc(cursor, y0, widths[i], rh, "rgba(108,99,245,.16)", ST, 1.4f))
            sb.append(tx(cursor + widths[i] / 2f, y0 + 20f, c, ST, "middle", bold = true, size = 11))
            cursor += widths[i]
        }
        rows.forEachIndexed { ri, rw ->
            val ry = y0 + (ri + 1) * rh
            val (p, q) = rw
            val vals = listOf(if (p == 1) 1 else 0, if (q == 1) 1 else 0, if (p == 1 && q == 1) 1 else 0, if (p == 1 || q == 1) 1 else 0, if (p == 1 && q == 0) 0 else 1)
            var cx = x
            vals.forEachIndexed { vi, v ->
                sb.append(rc(cx, ry, widths[vi], rh, "#ffffff", ST, 1f))
                sb.append(tx(cx + widths[vi] / 2f, ry + 20f, if (v == 1) "د" else "ن", if (vi > 1) (if (v == 1) "#1b7f4c" else PT) else ST, "middle", bold = true, size = 12))
                cx += widths[vi]
            }
        }
        sb.append(tx(180f, y0 + 5 * rh + 26f, "جدول ارزش — $expr   (د = درست ، ن = نادرست)", MU, "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun ledgerT(spec: FigureSpec): String {
        val title = spec.xStr("title", "صندوق")
        val debit = splitList(spec.xStr("debit", "12000,8500")).take(4)
        val credit = splitList(spec.xStr("credit", "6000,3000")).take(4)
        val w = 300f; val x = 30f; val y = 52f; val colW = w / 2f
        val sb = StringBuilder()
        sb.append(rc(x, y, w, 26f, "rgba(108,99,245,.14)", ST, 1.6f))
        sb.append(rc(x, y, colW, 26f, "rgba(108,99,245,.20)", ST, 1.6f))
        sb.append(tx(x + colW / 2f, y + 18f, "بدهکار", ACC, "middle", bold = true, size = 12))
        sb.append(tx(x + colW + colW / 2f, y + 18f, "بستانکار", "#00796b", "middle", bold = true, size = 12))
        val rows = max(debit.size, credit.size)
        val rh = 26f
        var sumD = 0.0
        var sumC = 0.0
        for (r in 0 until rows) {
            val ry = y + 26f + r * rh
            sb.append(rc(x, ry, w, rh, "#ffffff", "#c8d2e0", 1f))
            debit.getOrNull(r)?.let { sumD += it.toDoubleOrNull() ?: 0.0; sb.append(tx(x + colW - 8f, ry + 18f, it, ST, "end", size = 11)) }
            credit.getOrNull(r)?.let { sumC += it.toDoubleOrNull() ?: 0.0; sb.append(tx(x + w - 8f, ry + 18f, it, ST, "end", size = 11)) }
        }
        val ty = y + 26f + rows * rh
        sb.append(ln(x + colW, y, x + colW, ty + rh, ST, 1.8f))
        sb.append(ln(x, ty, x + w, ty, ST, 1.8f)); sb.append(ln(x, ty + rh, x + w, ty + rh, ST, 1.8f))
        sb.append(ln(x, y, x, ty + rh, ST, 1.8f)); sb.append(ln(x + w, y, x + w, ty + rh, ST, 1.8f))
        sb.append(tx(x + colW - 8f, ty + 18f, group(sumD.toLong()), ACC, "end", bold = true, size = 12))
        sb.append(tx(x + w - 8f, ty + 18f, group(sumC.toLong()), "#00796b", "end", bold = true, size = 12))
        sb.append(tx(x + 8f, ty + 18f, "جمع", MU, "start", bold = true, size = 11))
        sb.append(tx(180f, ty + rh + 28f, if (sumD == sumC) "تراز است ✓" else "تراز نیست ✗", if (sumD == sumC) "#1b7f4c" else PT, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun musicStaff(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "staff").lowercase()
        val sb = StringBuilder()
        if (kind == "color") {
            val cols = listOf("#e53935", "#fb8c00", "#fdd835", "#7cb342", "#43a047", "#00897b", "#00acc1", "#1e88e5", "#3949ab", "#8e24aa", "#d81b60", "#6d4c41")
            val cx = 180f; val cy = 136f; val r = 96f; val ri = 46f
            for (i in 0 until 12) {
                val a0 = -PI / 2 + i * PI / 6
                val a1 = a0 + PI / 6
                sb.append(path("M ${f(cx + ri * cos(a0).toFloat())} ${f(cy + ri * sin(a0).toFloat())} L ${f(cx + r * cos(a0).toFloat())} ${f(cy + r * sin(a0).toFloat())} A ${f(r)} ${f(r)} 0 0 1 ${f(cx + r * cos(a1).toFloat())} ${f(cy + r * sin(a1).toFloat())} L ${f(cx + ri * cos(a1).toFloat())} ${f(cy + ri * sin(a1).toFloat())} A ${f(ri)} ${f(ri)} 0 0 0 ${f(cx + ri * cos(a0).toFloat())} ${f(cy + ri * sin(a0).toFloat())} Z", cols[i], "#ffffff", 1.4f))
            }
            sb.append(cls(cx, cy, ri, "#ffffff", ST, 1.6f))
            sb.append(tx(180f, 254f, "چرخهٔ ۱۲ رنگ ایتن", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        val clef = spec.xStr("clef", "treble").lowercase()
        // V322 — شمارهٔ نتِ نامعتبر (۰ یا ۹۹۹) نُت را بیرون از حامل می‌کشید؛ حالا در بازهٔ نمایش مهار می‌شود.
        val notes = ints(spec, "notes", "60,62,64,65,67").take(8).ifEmpty { listOf(60, 62, 64, 65, 67) }.map { it.coerceIn(53, 74) }
        val x0 = 40f; val x1 = 330f; val top = 84f; val gap = 14f
        for (i in 0 until 5) sb.append(ln(x0, top + i * gap, x1, top + i * gap, ST, 1.4f))
        sb.append(ln(x0, top + 4 * gap, x0, top, ST, 2f))
        sb.append(tx(52f, 148f, if (clef == "bass") "𝄢" else "𝄞", ST, "start", bold = true, size = 40))
        val names = mapOf(60 to "دو", 62 to "ر", 64 to "می", 65 to "فا", 67 to "سل", 69 to "لا", 71 to "سی", 72 to "دوٴ", 59 to "سی")
        notes.forEachIndexed { i, nn ->
            val nx = 96f + i * min(32f, if (notes.size > 1) 210f / (notes.size - 1) else 30f)
            val yy = top + 4 * gap - (nn - 60) * (gap / 2f)
            sb.append(cls(nx, yy, 6.2f, ST, "none", 0f))
            sb.append(ln(nx + 5f, yy, nx + 5f, yy - 30f, ST, 1.4f))
            sb.append(tx(nx, 200f, names[nn] ?: nn.toString(), MU, "middle", size = 10))
        }
        sb.append(tx(180f, 240f, if (clef == "bass") "خطوط حامل — کلید فا" else "خطوط حامل — کلید سل", ST, "middle", bold = true, size = 13))
        return sb.toString()
    }

    // ============================================================ دانشگاه
    private fun karnaughMap(spec: FigureSpec): String {
        val vars = spec.xNum("vars", 3f).toInt().coerceIn(2, 4)
        val ones = ints(spec, "ones", "0,1,3,5")
        val groups = spec.xStr("groups", "").split(Regex("[;؛]")).filter { it.isNotBlank() }
        // V318.1 — چیدمان درست: ۲ متغیره ۲×۲، ۳ متغیره ۲×۴ (سطر=A، ستون=BC خاکستری)، ۴ متغیره ۴×۴
        val rowN = if (vars >= 4) 4 else 2
        val colN = if (vars == 2) 2 else 4
        val rowLbl = if (vars >= 4) listOf("00", "01", "11", "10") else listOf("0", "1")
        val colLbl = if (vars == 2) listOf("0", "1") else listOf("00", "01", "11", "10")
        val cell = min(56f, 220f / max(rowN, colN))
        val x0 = 180f - cell * colN / 2f + 14f
        val y0 = 56f
        val gray = listOf(0, 1, 3, 2)
        fun valueAt(r: Int, c: Int): Int = when (vars) {
            2 -> r * 2 + c
            3 -> r * 4 + gray[c]
            else -> gray[r] * 4 + gray[c]
        }
        val sb = StringBuilder()
        for (c in 0 until colN) {
            sb.append(rc(x0 + c * cell, y0 - 26f, cell, 26f, "rgba(108,99,245,.14)", ST, 1.2f))
            sb.append(tx(x0 + c * cell + cell / 2f, y0 - 8f, colLbl[c], ACC, "middle", bold = true, size = 11))
        }
        for (r in 0 until rowN) {
            sb.append(rc(x0 - 34f, y0 + r * cell, 34f, cell, "rgba(108,99,245,.14)", ST, 1.2f))
            sb.append(tx(x0 - 17f, y0 + r * cell + cell / 2f + 4f, rowLbl[r], ACC, "middle", bold = true, size = 11))
            for (c in 0 until colN) {
                val idx = valueAt(r, c)
                val on = ones.contains(idx)
                sb.append(rc(x0 + c * cell, y0 + r * cell, cell, cell, if (on) "rgba(39,196,168,.26)" else "#ffffff", ST, 1.4f))
                sb.append(tx(x0 + c * cell + cell / 2f, y0 + r * cell + cell / 2f + 6f, if (on) "۱" else "۰", if (on) "#00796b" else "#9aa7ba", "middle", bold = true, size = 14))
            }
        }
        groups.forEachIndexed { gi, g ->
            val cells = g.split(Regex("[,،]")).mapNotNull { faEnDigits(it.trim()).toIntOrNull() }
            val pos = cells.mapNotNull { v ->
                var found: Pair<Int, Int>? = null
                for (r in 0 until rowN) for (c in 0 until colN) if (valueAt(r, c) == v) found = r to c
                found
            }
            if (pos.isNotEmpty()) {
                val minR = pos.minOf { it.first }; val maxR = pos.maxOf { it.first }
                val minC = pos.minOf { it.second }; val maxC = pos.maxOf { it.second }
                val gx = x0 + minC * cell + 3f + gi * 2f
                val gy = y0 + minR * cell + 3f + gi * 2f
                val gw = (maxC - minC + 1) * cell - 6f
                val gh = (maxR - minR + 1) * cell - 6f
                sb.append("<rect x=\"${f(gx)}\" y=\"${f(gy)}\" width=\"${f(gw)}\" height=\"${f(gh)}\" rx=\"10\" fill=\"none\" stroke=\"${if (gi % 2 == 1) PT else ACC}\" stroke-width=\"2\" stroke-dasharray=\"6 4\"/>")
            }
        }
        sb.append(tx(180f, y0 + rowN * cell + 26f, "جدول کارنو — $vars متغیره", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun logicGate(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "and").lowercase()
        val a = if (spec.xStr("a", "1") == "1") 1 else 0
        val b = if (spec.xStr("b", "0") == "1") 1 else 0
        var out = when (kind) {
            "and", "nand" -> if (a == 1 && b == 1) 1 else 0
            "or", "nor" -> if (a == 1 || b == 1) 1 else 0
            "xor" -> if (a != b) 1 else 0
            else -> 1 - a
        }
        if (kind == "nand" || kind == "nor") out = 1 - out
        val gx = 118f; val gy = 92f; val gw = 96f; val gh = 84f
        val sb = StringBuilder()
        if (kind == "not") {
            sb.append(poly("${f(gx)},${f(gy)} ${f(gx)},${f(gy + gh)} ${f(gx + gw)},${f(gy + gh / 2f)}", "rgba(108,99,245,.14)", ST, 2f))
            sb.append(cls(gx + gw + 8f, gy + gh / 2f, 8f, "#ffffff", ST, 2f))
            sb.append(ln(gx - 52f, gy + gh / 2f, gx, gy + gh / 2f, ST, 2f))
            sb.append(tx(gx - 30f, gy + gh / 2f - 10f, "A = $a", ACC, "middle", bold = true, size = 12))
            sb.append(ln(gx + gw + 16f, gy + gh / 2f, gx + gw + 56f, gy + gh / 2f, PT, 2f))
            sb.append(tx(gx + gw + 40f, gy + gh / 2f - 10f, "Y = $out", PT, "middle", bold = true, size = 12))
        } else {
            if (kind == "and" || kind == "nand") {
                sb.append(path("M ${f(gx)},${f(gy)} L ${f(gx + gw * 0.5f)},${f(gy)} A ${f(gh / 2f)} ${f(gh / 2f)} 0 0 1 ${f(gx + gw * 0.5f)},${f(gy + gh)} L ${f(gx)},${f(gy + gh)} Z", "rgba(108,99,245,.14)", ST, 2f))
            } else {
                sb.append(path("M ${f(gx)},${f(gy)} L ${f(gx + gw * 0.55f)},${f(gy)} Q ${f(gx + gw)},${f(gy + gh / 2f)} ${f(gx + gw * 0.55f)},${f(gy + gh)} L ${f(gx)},${f(gy + gh)} Z", "rgba(108,99,245,.14)", ST, 2f))
                if (kind == "xor") sb.append(path("M ${f(gx - 12f)},${f(gy)} Q ${f(gx + 22f)},${f(gy + gh / 2f)} ${f(gx - 12f)},${f(gy + gh)}", "none", ST, 2f))
            }
            if (kind == "nand" || kind == "nor") sb.append(cls(gx + gw + 8f, gy + gh / 2f, 8f, "#ffffff", ST, 2f))
            sb.append(ln(gx - 56f, gy + 22f, gx + 4f, gy + 22f, ST, 2f))
            sb.append(tx(gx - 40f, gy + 12f, "A = $a", ACC, "middle", bold = true, size = 12))
            sb.append(ln(gx - 56f, gy + 62f, gx + 4f, gy + 62f, ST, 2f))
            sb.append(tx(gx - 40f, gy + 52f, "B = $b", ACC, "middle", bold = true, size = 12))
            val ox = gx + gw + (if (kind == "nand" || kind == "nor") 16f else 6f)
            sb.append(ln(ox, gy + gh / 2f, ox + 52f, gy + gh / 2f, PT, 2f))
            sb.append(tx(ox + 26f, gy + gh / 2f - 10f, "Y = $out", PT, "middle", bold = true, size = 12))
        }
        val names = mapOf("and" to "AND — و", "or" to "OR — یا", "not" to "NOT — نقیض", "nand" to "NAND — نقیض و", "nor" to "NOR — نقیض یا", "xor" to "XOR — یا انحصاری")
        sb.append(tx(180f, 224f, names[kind] ?: "گیت منطقی", ST, "middle", bold = true, size = 14))
        sb.append(tx(180f, 250f, "Y = $out", if (out == 1) "#1b7f4c" else PT, "middle", bold = true, size = 13))
        return sb.toString()
    }

    private fun automaton(spec: FigureSpec): String {
        val states = splitList(spec.xStr("states", "q0,q1,q2")).take(4).ifEmpty { listOf("q0", "q1", "q2") }
        val start = spec.xStr("start", "q0")
        val accept = splitList(spec.xStr("accept", "q2"))
        val edges = spec.xStr("edges", "q0-q1:a;q1-q2:b;q1-q1:a").split(Regex("[;؛]")).map { it.trim() }.filter { it.isNotBlank() }
        val cy = 132f
        val r = 30f
        val step = min(96f, 280f / states.size)
        val pos = HashMap<String, Pair<Float, Float>>()
        states.forEachIndexed { i, s -> pos[s] = (180f + (i - (states.size - 1) / 2f) * step) to cy }
        val sb = StringBuilder()
        pos[start]?.let { sb.append(arrow(it.first - r - 34f, cy, it.first - r - 4f, cy, ST, 1.6f, 8f)) }
        states.forEach { s ->
            val p = pos[s]!!
            sb.append(cls(p.first, p.second, r, if (accept.contains(s)) "rgba(39,196,168,.18)" else "#ffffff", ST, 2f))
            if (accept.contains(s)) sb.append(cls(p.first, p.second, r - 6f, "none", ST, 1.4f))
            sb.append(tx(p.first, p.second + 5f, s, ST, "middle", bold = true, size = 12))
        }
        edges.forEach { ed ->
            val m = ed.split(Regex("[-:—]")).map { it.trim() }.filter { it.isNotBlank() }
            if (m.size >= 3) {
                val from = m[0]; val to = m[1]; val lbl = m.drop(2).joinToString(":")
                val p1 = pos[from]; val p2 = pos[to]
                if (p1 != null && p2 != null) {
                    if (from == to) {
                        sb.append(path("M ${f(p1.first - 12f)},${f(p1.second - r)} A 18 18 0 1 1 ${f(p1.first + 12f)},${f(p1.second - r)}", "none", ACC, 1.6f))
                        sb.append(tx(p1.first, p1.second - r - 18f, lbl, ACC, "middle", bold = true, size = 10))
                    } else {
                        val dx = p2.first - p1.first; val dy = p2.second - p1.second
                        val dl = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        sb.append(arrow(p1.first + dx / dl * r, p1.second + dy / dl * r, p2.first - dx / dl * r, p2.second - dy / dl * r, ACC, 1.6f, 8f))
                        sb.append(tx((p1.first + p2.first) / 2f, (p1.second + p2.second) / 2f - 8f, lbl, "#0d47a1", "middle", bold = true, size = 10))
                    }
                }
            }
        }
        sb.append(tx(180f, 238f, "ماشین حالت متناهی (DFA)", ST, "middle", bold = true, size = 13))
        sb.append(tx(180f, 260f, "حالت شروع: $start • حالت پایانی: ${accept.joinToString(" , ")}", MU, "middle", size = 11))
        return sb.toString()
    }

    private fun binaryTree(spec: FigureSpec): String {
        val vals = ints(spec, "vals", "50,30,70,20,40,60,80").take(12).ifEmpty { listOf(50, 30, 70, 20, 40, 60, 80) }
        val heap = spec.xStr("mode", "bst").lowercase() == "heap"
        class Node(val v: Int) { var l: Node? = null; var r: Node? = null }
        fun insert(node: Node?, v: Int): Node {
            val n = node ?: return Node(v)
            if (v < n.v) n.l = insert(n.l, v) else n.r = insert(n.r, v)
            return n
        }
        fun build(i: Int): Node? = if (i >= vals.size) null else Node(vals[i]).also { it.l = build(i * 2 + 1); it.r = build(i * 2 + 2) }
        var root: Node? = null
        if (heap) root = build(0) else vals.forEach { root = insert(root, it) }
        var maxD = 0
        fun depth(n: Node?, d: Int) { if (n == null) return; maxD = max(maxD, d); depth(n.l, d + 1); depth(n.r, d + 1) }
        depth(root, 0)
        val rowH = min(46f, 190f / max(1, maxD + 1))
        val sb = StringBuilder()
        fun draw(n: Node?, x: Float, d: Int, span: Float) {
            if (n == null) return
            val y = 40f + d * rowH
            if (n.l != null) { sb.append(ln(x, y, x - span, y + rowH, ST, 1.4f)); draw(n.l, x - span, d + 1, span / 2f) }
            if (n.r != null) { sb.append(ln(x, y, x + span, y + rowH, ST, 1.4f)); draw(n.r, x + span, d + 1, span / 2f) }
            sb.append(cls(x, y, 15f, "rgba(108,99,245,.16)", ACC, 1.8f))
            sb.append(tx(x, y + 5f, n.v.toString(), ST, "middle", bold = true, size = 11))
        }
        draw(root, 180f, 0, min(84f, 210f / 2f.pow(max(0, maxD - 1).toFloat())))
        sb.append(tx(180f, 258f, if (heap) "درخت هرم (آرایه‌ای)" else "درخت دودویی جستجو (BST)", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun bodePlot(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "bode").lowercase()
        val sb = StringBuilder()
        val poles = splitList(spec.xStr("poles", "1,10")).mapNotNull { faEnDigits(it).toFloatOrNull()?.let { v -> max(0.1f, v) } }.take(3)
        if (kind == "nyquist") {
            val pts = StringBuilder()
            for (i in 0..120) {
                val w = 10.0.pow(-2 + 4.0 * i / 120)
                var re = 1.0
                var im = 0.0
                poles.forEach { pl ->
                    val dr = 1.0; val di = w / pl
                    val nr = re * dr + im * di
                    val ni = im * dr - re * di
                    val dd = dr * dr + di * di
                    re = nr / dd; im = ni / dd
                }
                pts.append("${f(180f + re.toFloat() * 110f)},${f(140f - im.toFloat() * 110f)} ")
            }
            sb.append(ln(30f, 140f, 330f, 140f, ST, 1.4f)); sb.append(ln(180f, 20f, 180f, 260f, ST, 1.4f))
            sb.append("<polyline points=\"${pts.toString().trim()}\" fill=\"none\" stroke=\"$PT\" stroke-width=\"2\"/>")
            sb.append(tx(316f, 156f, "Re", MU, "middle", bold = true, size = 11))
            sb.append(tx(196f, 30f, "Im", MU, "middle", bold = true, size = 11))
            sb.append(tx(180f, 272f, "نمودار نایکوئیست", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        val gain = spec.xNum("gain", 20f)
        fun magDb(w: Float): Float {
            var m = gain
            poles.forEach { pl -> m -= (10 * Math.log(1 + (w / pl).pow(2f).toDouble()) / Math.log(10.0)).toFloat() }
            return m
        }
        fun phase(w: Float): Float {
            var p = 0f
            poles.forEach { pl -> p -= (atan2(w / pl, 1f) * 180 / PI).toFloat() }
            return p
        }
        fun plot(y0: Float, h: Float, fn: (Float) -> Float, label: String, col: String): String {
            val out = StringBuilder(rc(40f, y0, 292f, h, "#ffffff", ST, 1.2f, 3f))
            for (i in 0..3) { val yy = y0 + h * i / 3f; out.append(ln(40f, yy, 332f, yy, "#e8eef6", 1f)) }
            val pts = StringBuilder()
            for (i in 0..120) {
                val w = 10.0.pow(-1 + 3.0 * i / 120).toFloat()
                pts.append("${f(40f + 292f * i / 120f)},${f(y0 + h / 2f - fn(w))} ")
            }
            out.append("<polyline points=\"${pts.toString().trim()}\" fill=\"none\" stroke=\"$col\" stroke-width=\"2\"/>")
            out.append(tx(44f, y0 + 13f, label, col, "start", bold = true, size = 10))
            return out.toString()
        }
        sb.append(plot(34f, 92f, { w -> (magDb(w) * 0.9f).coerceIn(-46f, 46f) }, "اندازه (dB)", ACC))
        sb.append(plot(142f, 92f, { w -> (phase(w) * 0.5f).coerceIn(-46f, 46f) }, "فاز (°)", "#00796b"))
        sb.append(tx(180f, 250f, "نمودار بود — بهره ${num(gain)} dB ، قطب‌ها: ${poles.joinToString(" , ") { num(it) }}", MU, "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun beamDiagrams(spec: FigureSpec): String {
        val len = max(2f, spec.xNum("len", 6f))
        val p = spec.xNum("p", 10f)
        val a = spec.xNum("a", 2f).coerceIn(0.2f, len - 0.2f)
        val q = spec.xNum("q", 2f)
        val udl = spec.xStr("load", "point").lowercase() == "udl"
        val x0 = 46f; val x1 = 320f
        fun sx(v: Float) = x0 + (v / len) * (x1 - x0)
        val y = 74f
        val sb = StringBuilder()
        sb.append(ln(x0, y, x1, y, ST, 6f))
        sb.append(poly("${f(x0)},${f(y)} ${f(x0 + 12f)},${f(y + 16f)} ${f(x0 - 12f)},${f(y + 16f)}", "#ffffff", ST, 1.6f))
        sb.append(cls(x1, y + 9f, 6f, "#ffffff", ST, 1.6f))
        sb.append(ln(x1 - 10f, y + 20f, x1 + 10f, y + 20f, ST, 1.6f))
        val r1 = if (udl) q * len / 2f else p * (len - a) / len
        val r2 = if (udl) q * len / 2f else p * a / len
        if (udl) {
            var i = 0
            while (i <= (len * 4).toInt()) { val ux = sx(i / 4f); sb.append(arrow(ux, y - 40f, ux, y - 6f, ACC, 1.2f, 5f)); i++ }
            sb.append(ln(x0, y - 40f, x1, y - 40f, ACC, 1.6f))
            sb.append(tx(180f, y - 48f, "بار گسترده ${num(q)} N/m", ACC, "middle", bold = true, size = 11))
        } else {
            sb.append(arrow(sx(a), y - 46f, sx(a), y - 6f, PT, 2.4f, 9f))
            sb.append(tx(sx(a), y - 54f, "${num(p)} N", PT, "middle", bold = true, size = 11))
            sb.append(tx(sx(a), y + 34f, "${num(a)} m", MU, "middle", size = 10))
        }
        sb.append(arrow(x0, y + 34f, x0, y + 8f, "#00796b", 2f, 7f))
        sb.append(tx(x0 + 2f, y + 46f, "R₁=${num(r1)}", "#00796b", "start", bold = true, size = 10))
        sb.append(arrow(x1, y + 34f, x1, y + 8f, "#00796b", 2f, 7f))
        sb.append(tx(x1 - 2f, y + 46f, "R₂=${num(r2)}", "#00796b", "end", bold = true, size = 10))
        fun sf(v: Float) = if (udl) r1 - q * v else r1 - (if (v >= a) p else 0f)
        val maxS = max(abs(r1), abs(r1 - (if (udl) q * len else p))).coerceAtLeast(1f)
        val sfY = 152f
        sb.append(ln(x0, sfY, x1, sfY, ST, 1.4f)); sb.append(tx(44f, sfY - 6f, "V", MU, "end", bold = true, size = 11))
        val sfPts = StringBuilder()
        for (i in 0..100) { val bv = len * i / 100f; sfPts.append("${f(sx(bv))},${f(sfY - sf(bv) / maxS * 44f)} ") }
        sb.append("<polyline points=\"${sfPts.toString().trim()}\" fill=\"none\" stroke=\"$ACC\" stroke-width=\"2\"/>")
        fun bm(v: Float) = if (udl) r1 * v - q * v * v / 2f else if (v <= a) r1 * v else r2 * (len - v)
        val maxB = max(1f, if (udl) q * len * len / 8f else p * a * (len - a) / len)
        val bmY = 232f
        sb.append(ln(x0, bmY, x1, bmY, ST, 1.4f)); sb.append(tx(44f, bmY - 6f, "M", MU, "end", bold = true, size = 11))
        val bmPts = StringBuilder()
        for (i in 0..100) { val bv = len * i / 100f; bmPts.append("${f(sx(bv))},${f(bmY + bm(bv) / maxB * 34f)} ") }
        sb.append("<polyline points=\"${bmPts.toString().trim()}\" fill=\"none\" stroke=\"$PT\" stroke-width=\"2\"/>")
        sb.append(tx(180f, 274f, "تیر دو سر مفصل — نمودار برش (V) و گشتاور (M)", MU, "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun mohrCircle(spec: FigureSpec): String {
        val sx = spec.xNum("sx", 80f)
        val sy = spec.xNum("sy", 20f)
        val txy = spec.xNum("txy", 30f)
        val c = (sx + sy) / 2f
        val r = sqrt(((sx - sy) / 2f).pow(2f) + txy * txy)
        val mx = max(max(c + r, abs(c - r)), max(abs(txy) * 1.3f, 1f))
        val sc = 108f / mx
        val cx = 180f; val cy = 138f
        fun px(v: Float) = cx + v * sc
        fun py(v: Float) = cy - v * sc
        val sb = StringBuilder()
        sb.append(ln(24f, cy, 336f, cy, ST, 1.4f)); sb.append(arrow(324f, cy, 340f, cy, ST, 1.4f, 7f))
        sb.append(ln(cx, 258f, cx, 20f, ST, 1.4f)); sb.append(arrow(cx, 32f, cx, 16f, ST, 1.4f, 7f))
        sb.append(tx(334f, cy + 16f, "σ", MU, "middle", bold = true, size = 12))
        sb.append(tx(cx + 14f, 26f, "τ", MU, "middle", bold = true, size = 12))
        sb.append(cls(cx, cy, 4f, ST, "none", 0f))
        sb.append(cls(px(c), cy, r * sc, "rgba(108,99,245,.10)", ACC, 2f))
        sb.append(cls(px(sx), py(txy), 5f, PT, "none", 0f))
        sb.append(cls(px(sy), py(-txy), 5f, GRN, "none", 0f))
        sb.append(ln(px(sx), cy, px(sx), py(txy), "#c8d2e0", 1f, "4 3"))
        sb.append(ln(px(sy), cy, px(sy), py(-txy), "#c8d2e0", 1f, "4 3"))
        sb.append(ln(px(c), cy, px(sx), cy, ACC, 1.4f))
        sb.append(tx(px((c + sx) / 2f), cy + 16f, "R", ACC, "middle", bold = true, size = 11))
        fun sg(v: Float): String {
            val rr = Math.round(v * 10f) / 10f
            return (if (rr < 0) "−" else "") + num(abs(rr))
        }
        sb.append(tx(px(c - r) - 3f, cy - 8f, "σ₂ = ${sg(c - r)}", "#00796b", "end", bold = true, size = 10))
        sb.append(tx(px(c + r) + 3f, cy - 8f, "σ₁ = ${sg(c + r)}", "#00796b", "start", bold = true, size = 10))
        sb.append(tx(180f, 272f, "دایرهٔ مور — σx = ${sg(sx)} ، σy = ${sg(sy)} ، τxy = ${sg(txy)}", MU, "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun pvCycle(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "carnot").lowercase()
        val sb = StringBuilder()
        when (kind) {
            "otto", "diesel" -> {
                sb.append(path("M 96 96 L 252 96 L 150 214 L 96 214 Z", "rgba(108,99,245,.10)", ACC, 2.2f))
                sb.append(tx(174f, 84f, "احتراق (حجم ثابت)", PT, "middle", bold = true, size = 10))
                sb.append(tx(150f, 232f, "انبساط", MU, "middle", size = 10))
            }
            "rankine" -> {
                sb.append(path("M 96 96 L 252 96 L 252 216 L 96 216 Z", "rgba(108,99,245,.10)", ACC, 2.2f))
                sb.append(tx(174f, 84f, "گرم شدن", PT, "middle", bold = true, size = 10))
            }
            else -> {
                sb.append(path("M 104 78 C 176 54 250 92 262 150 C 238 206 160 226 104 196 C 66 170 70 110 104 78 Z", "rgba(108,99,245,.10)", ACC, 2.2f))
                sb.append(tx(180f, 52f, "هم‌دما", PT, "middle", bold = true, size = 10))
            }
        }
        sb.append(ln(40f, 226f, 328f, 226f, ST, 1.6f)); sb.append(arrow(316f, 226f, 332f, 226f, ST, 1.6f, 7f))
        sb.append(ln(66f, 226f, 66f, 30f, ST, 1.6f)); sb.append(arrow(66f, 42f, 66f, 24f, ST, 1.6f, 7f))
        sb.append(tx(320f, 242f, "V", MU, "middle", bold = true, size = 12))
        sb.append(tx(54f, 34f, "P", MU, "middle", bold = true, size = 12))
        val name = when (kind) { "otto" -> "چرخهٔ اتو"; "diesel" -> "چرخهٔ دیزل"; "rankine" -> "چرخهٔ رانکین"; else -> "چرخهٔ کارنو" }
        sb.append(tx(180f, 258f, name, ST, "middle", bold = true, size = 13))
        return sb.toString()
    }

    private fun projectNetwork(spec: FigureSpec): String {
        val acts = spec.xStr("acts", "A:1-2:3;B:2-3:2;C:2-4:4;D:3-4:1").split(Regex("[;؛]")).filter { it.isNotBlank() }
        class Act(val name: String, val from: String, val to: String, val dur: Float)
        val list = acts.mapNotNull { ac ->
            val m = ac.split(":").map { it.trim() }
            if (m.size < 3) return@mapNotNull null
            val nd = m[1].split(Regex("[-—]")).map { it.trim() }
            Act(m[0], nd[0], nd.getOrElse(1) { nd[0] }, faEnDigits(m[2]).toFloatOrNull() ?: 0f)
        }.ifEmpty { listOf(Act("A", "1", "2", 3f)) }
        val nodes = LinkedHashSet<String>()
        list.forEach { nodes.add(it.from); nodes.add(it.to) }
        val depth = HashMap<String, Int>()
        nodes.forEach { depth[it] = 0 }
        repeat(6) { list.forEach { a -> depth[a.to] = max(depth[a.to] ?: 0, (depth[a.from] ?: 0) + 1) } }
        val maxD = nodes.maxOf { depth[it] ?: 0 }
        val pos = HashMap<String, Pair<Float, Float>>()
        nodes.forEach { k ->
            val same = nodes.filter { depth[it] == depth[k] }
            val idx = same.indexOf(k)
            pos[k] = (48f + (if (maxD > 0) (depth[k] ?: 0) / maxD.toFloat() else 0f) * 260f) to (54f + idx * min(58f, 160f / max(1, same.size)))
        }
        val sb = StringBuilder()
        list.forEach { a ->
            val p1 = pos[a.from]; val p2 = pos[a.to]
            if (p1 != null && p2 != null) {
                val d = sqrt((p2.first - p1.first).pow(2) + (p2.second - p1.second).pow(2)).coerceAtLeast(1f)
                val sxx = p1.first + (p2.first - p1.first) / d * 15f
                val syy = p1.second + (p2.second - p1.second) / d * 15f
                val exx = p2.first - (p2.first - p1.first) / d * 15f
                val eyy = p2.second - (p2.second - p1.second) / d * 15f
                // V326 — فعالیتی که از روی یک گره می‌پرد (مثل C: ۲→۴) با کمان زیر ردیف کشیده می‌شود
                // تا خط و برچسبش روی گرهٔ میانی نیفتد؛ برچسب بقیهٔ یال‌ها هم با بردار عمود جدا می‌شود.
                val jump = if (abs(eyy - syy) < 6f) nodes.firstOrNull { k ->
                    val q = pos[k]!!
                    abs(q.second - p1.second) <= 12f && q.first > min(p1.first, p2.first) + 14f && q.first < max(p1.first, p2.first) - 14f
                } else null
                if (jump != null) {
                    val jmx = (sxx + exx) / 2f
                    val jmy = max(syy, eyy) + 44f
                    val apy = (syy + 2f * jmy + eyy) / 4f
                    val tdx = exx - jmx; val tdy = eyy - jmy
                    val tl = sqrt(tdx * tdx + tdy * tdy).coerceAtLeast(1f)
                    val tux = tdx / tl; val tuy = tdy / tl
                    val tbx = exx - tux * 8f; val tby = eyy - tuy * 8f
                    sb.append(path("M ${num(sxx)} ${num(syy)} Q ${num(jmx)} ${num(jmy)} ${num(tbx)} ${num(tby)}", "none", ACC, 1.8f))
                    sb.append(poly(
                        "${num(exx)},${num(eyy)} ${num(tbx - tuy * 4f)},${num(tby + tux * 4f)} ${num(tbx + tuy * 4f)},${num(tby - tux * 4f)}",
                        ACC, ACC, 1f
                    ))
                    sb.append(tx(jmx, apy + 14f, "${a.name} (${num(a.dur)})", "#0d47a1", "middle", bold = true, size = 10))
                } else {
                    val wdx = exx - sxx; val wdy = eyy - syy
                    val wl = sqrt(wdx * wdx + wdy * wdy).coerceAtLeast(1f)
                    var wnx = -wdy / wl; var wny = wdx / wl
                    if (wny > 0f) { wnx = -wnx; wny = -wny }
                    sb.append(arrow(sxx, syy, exx, eyy, ACC, 1.8f, 8f))
                    sb.append(tx((sxx + exx) / 2f + wnx * 13f, (syy + eyy) / 2f + wny * 13f + 4f,
                        "${a.name} (${num(a.dur)})", "#0d47a1", "middle", bold = true, size = 10))
                }
            }
        }
        nodes.forEach { k -> val p = pos[k]!!; sb.append(cls(p.first, p.second, 14f, "#ffffff", ST, 2f)); sb.append(tx(p.first, p.second + 4f, k, ST, "middle", bold = true, size = 10)) }
        sb.append(tx(180f, 262f, "شبکهٔ فعالیت‌ها (CPM/PERT) — ${list.size} فعالیت", MU, "middle", bold = true, size = 11))
        return sb.toString()
    }


    /** V327 — عددهای فارسی (۰ تا ۹) برای ابزارهای دبستان. */
    private fun pFa(v: Int): String = v.toString().map { c ->
        when (c) {
            '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
            '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
            else -> c
        }
    }.joinToString("")

    /**
     * V327 — شبکهٔ رنگ‌آمیزی.
     * دانش‌آموز «n خانه» را رنگ می‌کند؛ همان مختصات خانه‌ها در پاسخ لمسیِ اپ (حالت paint) استفاده می‌شود،
     * پس شبکه این‌جا باید دقیقاً با آن هم‌شکل باشد: خانه‌ها از بالا-چپ، شمارهٔ سطر/ستون از ۱.
     */
    private fun paintGrid(spec: FigureSpec): String {
        val rows = spec.xStr("rows", "4").toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val cols = spec.xStr("cols", "4").toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val goal = spec.xStr("goal", "1").toFloatOrNull()?.toInt()?.coerceIn(0, rows * cols) ?: 1
        val cells = spec.xStr("cells", "").split(Regex("[;؛]")).mapNotNull { chunk ->
            val a = chunk.split(",")
            val r = a.getOrNull(0)?.trim()?.toFloatOrNull()?.toInt()
            val c = a.getOrNull(1)?.trim()?.toFloatOrNull()?.toInt()
            if (r != null && c != null && r in 1..rows && c in 1..cols) r to c else null
        }
        val w = min(300f, cols * 42f)
        val h = min(150f, rows * 34f)
        val cw = w / cols
        val ch = h / rows
        val x0 = (360f - w) / 2f
        val y0 = 66f + (150f - h) / 2f
        val sb = StringBuilder()
        sb.append(tx(180f, 40f, "خانه‌ها را رنگ کن", ST, "middle", bold = true, size = 13))
        for (r in 0 until rows) for (c in 0 until cols) {
            val on = cells.any { it.first == r + 1 && it.second == c + 1 }
            sb.append(rc(x0 + c * cw, y0 + r * ch, cw, ch, if (on) "rgba(108,99,245,.55)" else "#ffffff", ST, 1.4f, 4f))
        }
        if (goal > 0) sb.append(tx(180f, y0 + h + 28f, "دقیقاً ${pFa(goal)} خانه را رنگ کن", ACC, "middle", bold = true, size = 12))
        sb.append(tx(180f, 258f, "رنگ‌آمیزی روی شکل — پاسخ با لمس خانه‌ها ثبت می‌شود", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 — نویسه‌های حرف فارسی: تنها، آغازین، میانی، پایانی. */
    private val LETTER_FORMS: Map<String, List<String>> = mapOf(
        "ب" to listOf("ب", "بـ", "ـبـ", "ـب"), "پ" to listOf("پ", "پـ", "ـپـ", "ـپ"),
        "ت" to listOf("ت", "تـ", "ـتـ", "ـت"), "ث" to listOf("ث", "ثـ", "ـثـ", "ـث"),
        "ج" to listOf("ج", "جـ", "ـجـ", "ـج"), "چ" to listOf("چ", "چـ", "ـچـ", "ـچ"),
        "ح" to listOf("ح", "حـ", "ـحـ", "ـح"), "خ" to listOf("خ", "خـ", "ـخـ", "ـخ"),
        "س" to listOf("س", "سـ", "ـسـ", "ـس"), "ش" to listOf("ش", "شـ", "ـشـ", "ـش"),
        "ص" to listOf("ص", "صـ", "ـصـ", "ـص"), "ض" to listOf("ض", "ضـ", "ـضـ", "ـض"),
        "ع" to listOf("ع", "عـ", "ـعـ", "ـع"), "غ" to listOf("غ", "غـ", "ـغـ", "ـغ"),
        "ف" to listOf("ف", "فـ", "ـفـ", "ـف"), "ق" to listOf("ق", "قـ", "ـقـ", "ـق"),
        "ک" to listOf("ک", "کـ", "ـکـ", "ـک"), "گ" to listOf("گ", "گـ", "ـگـ", "ـگ"),
        "ل" to listOf("ل", "لـ", "ـلـ", "ـل"), "م" to listOf("م", "مـ", "ـمـ", "ـم"),
        "ن" to listOf("ن", "نـ", "ـنـ", "ـن"), "ه" to listOf("ه", "هـ", "ـهـ", "ـه"),
        "ی" to listOf("ی", "یـ", "ـیـ", "ـی")
    )

    private fun letterForms(spec: FigureSpec): String {
        val ch = spec.xStr("ch", "ب").trim().firstOrNull()?.toString() ?: "ب"
        val forms = LETTER_FORMS[ch] ?: listOf(ch, "${ch}ـ", "ـ${ch}ـ", "ـ$ch")
        val caps = listOf("تنها", "آغازین", "میانی", "پایانی")
        val sb = StringBuilder()
        sb.append(tx(180f, 34f, "نویسه‌های حرف «$ch»", ST, "middle", bold = true, size = 13))
        for (i in 0 until 4) {
            val x = 306f - i * 72f
            sb.append(rc(x - 30f, 84f, 60f, 60f, "rgba(108,99,245,.10)", ACC, 1.6f, 10f))
            sb.append(tx(x, 124f, forms[i], ST, "middle", bold = true, size = 26))
            sb.append(tx(x, 164f, caps[i], MU, "middle", size = 11))
        }
        sb.append(tx(180f, 206f, "حرف در اول، میان و آخر کلمه شکلش عوض می‌شود", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 — هجاهای کلمه (کلمه با «-» به هجاها شکسته می‌شود). */
    private fun syllableChart(spec: FigureSpec): String {
        val word = spec.xStr("word", "با-با").ifBlank { "با-با" }
        val list = word.split(Regex("[-–—]")).map { it.trim() }.filter { it.isNotEmpty() }.take(6)
            .ifEmpty { listOf("با", "با") }
        val n = list.size
        val w = min(72f, 300f / n)
        val x0 = 180f + (n * (w + 8f)) / 2f
        val sb = StringBuilder()
        sb.append(tx(180f, 40f, "هجاهای کلمه", ST, "middle", bold = true, size = 13))
        list.forEachIndexed { i, syl ->
            val bx = x0 - (i + 1) * (w + 8f)
            sb.append(rc(bx, 74f, w, 52f, "rgba(39,196,168,.16)", "#1f8a78", 1.6f, 10f))
            sb.append(tx(bx + w / 2f, 108f, syl, ST, "middle", bold = true, size = 18))
            sb.append(cls(bx + w / 2f, 146f, 4f, "#1f8a78", "none", 0f))
        }
        sb.append(tx(180f, 174f, "تعداد هجا: ${pFa(n)}", ACC, "middle", bold = true, size = 12))
        sb.append(tx(180f, 202f, "هر هجا را با یک دست‌زدن بشمار", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 — آهنربا: جذب/دفع دو قطب. */
    private fun magnetSpec(spec: FigureSpec): String {
        val repel = spec.xStr("mode", "attract").lowercase() == "repel"
        val sb = StringBuilder()
        sb.append(tx(180f, 36f, if (repel) "دو قطب هم‌نام — دفع" else "دو قطب ناهم‌نام — جذب", ST, "middle", bold = true, size = 13))
        val left = 96f
        val right = 264f
        val y = 150f
        listOf(left to "N", right to "S").forEach { (x, pole) ->
            sb.append(rc(x - 46f, y - 26f, 92f, 52f, "#e8eaf6", ST, 1.8f, 6f))
            val wx = if (pole == "N") x + 18f else x - 46f
            sb.append(rc(wx, y - 26f, 28f, 52f, if (pole == "N") "#e53935" else "#1e88e5", "#ffffff", 1f, 4f))
            sb.append(tx(x + if (pole == "N") 32f else -32f, y + 7f, pole, "#ffffff", "middle", bold = true, size = 16))
        }
        if (!repel) {
            sb.append(arrow(left + 50f, y, left + 78f, y, "#1f8a78", 2f, 8f))
            sb.append(arrow(right - 50f, y, right - 78f, y, "#1f8a78", 2f, 8f))
            sb.append(tx(180f, y - 48f, "جذب", "#1f8a78", "middle", bold = true, size = 12))
        } else {
            sb.append(arrow(left + 78f, y, left + 40f, y, PT, 2f, 8f))
            sb.append(arrow(right - 78f, y, right - 40f, y, PT, 2f, 8f))
            sb.append(tx(180f, y - 48f, "دفع", PT, "middle", bold = true, size = 12))
        }
        sb.append(tx(180f, 232f, "قطب‌های هم‌نام یکدیگر را می‌رانند و ناهم‌نام‌ها جذب می‌شوند", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 — حواس پنج‌گانه: هر حس با اندامش. */
    private fun fiveSenses(spec: FigureSpec): String {
        val items = listOf("بینایی" to "چشم", "شنوایی" to "گوش", "بویایی" to "بینی", "چشایی" to "زبان", "لامسه" to "پوست")
        val pos = listOf(180f to 62f, 92f to 112f, 268f to 112f, 112f to 188f, 248f to 188f)
        val lab = listOf(180f to 26f, 92f to 152f, 268f to 152f, 112f to 224f, 248f to 224f)
        val sb = StringBuilder()
        sb.append(cls(180f, 138f, 34f, "rgba(108,99,245,.12)", ACC, 1.8f))
        sb.append(tx(180f, 144f, "حواس", ST, "middle", bold = true, size = 12))
        items.forEachIndexed { i, (sense, organ) ->
            val (px, py) = pos[i]
            val (lx, ly) = lab[i]
            sb.append(ln(180f, 138f, px, py, "#b9c2d0", 1.3f))
            sb.append(cls(px, py, 22f, "rgba(108,99,245,.16)", ACC, 1.8f))
            sb.append(tx(px, py + 5f, pFa(i + 1), ACC, "middle", bold = true, size = 12))
            sb.append(tx(lx, ly, "$sense — $organ", ST, "middle", bold = true, size = 10))
        }
        sb.append(tx(180f, 246f, "پنج حس بدن و اندام هر حس", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 — مراحل رشد دانه (دانه → جوانه → گیاه کوچک → گیاه گل‌دار). */
    private fun seedGrow(spec: FigureSpec): String {
        val stages = listOf("دانه", "جوانه", "گیاه کوچک", "گیاه گل‌دار")
        val sb = StringBuilder()
        sb.append(tx(180f, 40f, "مراحل رشد دانه", ST, "middle", bold = true, size = 13))
        sb.append(ln(40f, 190f, 320f, 190f, "#8d6e63", 3f))
        stages.forEachIndexed { i, label ->
            val x = 300f - i * 76f
            if (i == 0) {
                sb.append("<ellipse cx=\"${f(x)}\" cy=\"184\" rx=\"9\" ry=\"6\" fill=\"#a1887f\" stroke=\"$ST\" stroke-width=\"1.2\"/>")
            } else {
                val hgt = 14f + i * 26f
                sb.append(ln(x, 190f, x, 190f - hgt, "#2e7d32", 2.6f))
                for (lv in 0 until i) {
                    val ly = 190f - 14f - lv * 20f
                    sb.append("<path d=\"M${f(x)},${f(ly)} q-16,-9 -22,2 q12,7 22,-2\" fill=\"rgba(46,125,50,.35)\" stroke=\"#2e7d32\" stroke-width=\"1.2\"/>")
                    sb.append("<path d=\"M${f(x)},${f(ly)} q16,-9 22,2 q-12,7 -22,-2\" fill=\"rgba(46,125,50,.25)\" stroke=\"#2e7d32\" stroke-width=\"1.2\"/>")
                }
                if (i == 3) sb.append(cls(x, 190f - hgt - 10f, 7f, "#e91e63", "#e91e63", 1.2f))
            }
            sb.append(tx(x, 216f, label, ST, "middle", bold = true, size = 10))
        }
        sb.append(tx(180f, 250f, "از دانه تا گیاه گل‌دار — ترتیب مراحل", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 — اندام‌های بدن (نمای ساده با برچسب). */
    private fun bodyParts(spec: FigureSpec): String {
        val sb = StringBuilder()
        sb.append(tx(180f, 40f, "اندام‌های بدن", ST, "middle", bold = true, size = 13))
        sb.append(cls(180f, 74f, 24f, "#ffe0b2", ST, 1.8f))
        sb.append("<path d=\"M180,98 L180,168\" stroke=\"$ST\" stroke-width=\"16\" stroke-linecap=\"round\"/>")
        sb.append("<path d=\"M146,142 L180,120 L214,142\" fill=\"none\" stroke=\"$ST\" stroke-width=\"8\" stroke-linecap=\"round\"/>")
        sb.append("<path d=\"M180,168 L158,214 M180,168 L202,214\" stroke=\"$ST\" stroke-width=\"9\" stroke-linecap=\"round\"/>")
        sb.append(cls(180f, 70f, 3f, ST, "none", 0f))
        listOf("سر" to (236f to 74f), "دست" to (252f to 146f), "شکم" to (236f to 160f), "پا" to (250f to 214f)).forEach { (label, p) ->
            sb.append(ln(180f, p.second, p.first - 10f, p.second, "#b9c2d0", 1.1f))
            sb.append(cls(p.first, p.second, 3f, ACC, "none", 0f))
            sb.append(tx(p.first + 6f, p.second + 4f, label, ST, "start", bold = true, size = 11))
        }
        sb.append(tx(180f, 252f, "هر اندام را نام‌گذاری کنید", MU, "middle", size = 10))
        return sb.toString()
    }

    private fun organicStructure(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "kekule").lowercase()
        val mode = spec.xStr("mode", "label").lowercase()
        val sb = StringBuilder()
        when (kind) {
            "chair" -> {
                sb.append(path("M 70 118 L 130 94 L 190 118 L 250 94 L 250 138 L 190 162 L 130 138 L 70 162 Z", "none", ST, 2.2f))
                sb.append(ln(70f, 118f, 70f, 162f, ST, 2.2f)); sb.append(ln(130f, 94f, 130f, 138f, ST, 2.2f))
                sb.append(ln(190f, 118f, 190f, 162f, ST, 2.2f)); sb.append(ln(250f, 94f, 250f, 138f, ST, 2.2f))
                sb.append(tx(180f, 214f, "صندلی سیکلوهگزان", MU, "middle", bold = true, size = 12))
                return sb.toString()
            }
            "newman" -> {
                sb.append(cls(180f, 132f, 66f, "none", ST, 2.2f)); sb.append(cls(180f, 132f, 12f, "#ffffff", ST, 1.8f))
                for (i in 0 until 3) {
                    val a = i * 2 * PI / 3
                    sb.append(ln(180f + 12f * cos(a).toFloat(), 132f + 12f * sin(a).toFloat(), 180f + 66f * cos(a).toFloat(), 132f + 66f * sin(a).toFloat(), ST, 2.2f))
                    sb.append(cls(180f + 74f * cos(a).toFloat(), 132f + 74f * sin(a).toFloat(), 7f, "#ffffff", ST, 1.6f))
                    val b = a + PI / 3 + 0.6
                    sb.append(ln(180f + 12f * cos(b).toFloat(), 132f + 12f * sin(b).toFloat(), 180f + 56f * cos(b).toFloat(), 132f + 56f * sin(b).toFloat(), PT, 1.8f, "4 3"))
                }
                sb.append(tx(180f, 244f, "تصویر نیومن — اتان", MU, "middle", bold = true, size = 12))
                return sb.toString()
            }
            "fischer" -> {
                sb.append(ln(120f, 60f, 240f, 60f, ST, 2f)); sb.append(ln(120f, 200f, 240f, 200f, ST, 2f)); sb.append(ln(180f, 60f, 180f, 200f, ST, 2f))
                sb.append(tx(150f, 50f, "CH₃", ACC, "middle", bold = true, size = 12))
                sb.append(tx(210f, 50f, "H", "#00796b", "middle", bold = true, size = 12))
                sb.append(tx(150f, 222f, "H", "#00796b", "middle", bold = true, size = 12))
                sb.append(tx(210f, 222f, "OH", PT, "middle", bold = true, size = 12))
                sb.append(tx(180f, 132f, "C", ST, "middle", bold = true, size = 13))
                sb.append(tx(180f, 268f, "تصویر فیشر", MU, "middle", bold = true, size = 12))
                return sb.toString()
            }
            else -> {
                val cx = 180f; val cy = 130f; val r = 74f
                val pts = (0 until 6).map { i ->
                    val a = -PI / 2 + i * PI / 3
                    (cx + r * cos(a).toFloat()) to (cy + r * sin(a).toFloat())
                }
                sb.append(poly(pts.joinToString(" ") { "${f(it.first)},${f(it.second)}" }, "rgba(108,99,245,.08)", ST, 2.2f))
                if (mode == "circle") sb.append(cls(cx, cy, r * 0.62f, "none", ACC, 2.4f))
                else for (i in 0 until 6 step 2) {
                    val q1 = pts[i]; val q2 = pts[(i + 1) % 6]
                    val mxp = (q1.first + q2.first) / 2f
                    val myp = (q1.second + q2.second) / 2f
                    val inx = cx - mxp; val iny = cy - myp
                    val il = sqrt(inx * inx + iny * iny).coerceAtLeast(1f)
                    val ox = inx / il * 9f; val oy = iny / il * 9f
                    sb.append(ln(mxp - (q2.first - q1.first) / 2f * 0.55f + ox, myp - (q2.second - q1.second) / 2f * 0.55f + oy, mxp + (q2.first - q1.first) / 2f * 0.55f + ox, myp + (q2.second - q1.second) / 2f * 0.55f + oy, ACC, 2.6f))
                }
                // V322 — برچسب اتم‌های کربن (C) که موتور وب می‌کشید در اپ غایب بود.
                if (mode != "circle") pts.forEach { p ->
                    sb.append(tx(p.first + (p.first - cx) * 0.22f, p.second + (p.second - cy) * 0.22f + 4f, "C", MU, "middle", size = 10))
                }
                sb.append(tx(180f, 250f, if (mode == "circle") "بنزن — نمایش حلقهٔ آروماتیک" else "بنزن — ساختار ککوله", ST, "middle", bold = true, size = 13))
            }
        }
        return sb.toString()
    }

    private fun normalCurve(spec: FigureSpec): String {
        val mu = spec.xNum("mu", 0f)
        val sd = max(0.1f, spec.xNum("sd", 1f))
        val lo = spec.xNum("lo", -1f)
        val hi = spec.xNum("hi", 1f)
        val x0 = 34f; val x1 = 326f; val y = 206f; val h = 140f
        val vmin = mu - 3.4f * sd
        val vmax = mu + 3.4f * sd
        // V322 — بازهٔ سایه‌زنی به ناحیهٔ رسم محدود می‌شود (پیش‌تر lo/hi بیرون از بوم خط و نقطه می‌ساخت).
        val loC = lo.coerceIn(vmin, vmax)
        val hiC = hi.coerceIn(vmin, vmax)
        fun px(v: Float) = x0 + (v - vmin) / (vmax - vmin) * (x1 - x0)
        fun pdf(v: Float): Float { val z = (v - mu) / sd; return exp(-(z * z) / 2f) }
        val sb = StringBuilder()
        val shade = StringBuilder()
        for (i in 0..120) {
            val v = vmin + (vmax - vmin) * i / 120f
            if (v >= loC && v <= hiC) shade.append("${f(px(v))},${f(y - pdf(v) * h)} ")
        }
        if (shade.isNotEmpty()) {
            sb.append("<polygon points=\"${f(px(loC))},$y $shade${f(px(hiC))},$y\" fill=\"rgba(39,196,168,.26)\"/>")
        }
        val curve = StringBuilder()
        for (i in 0..140) {
            val v = vmin + (vmax - vmin) * i / 140f
            curve.append("${f(px(v))},${f(y - pdf(v) * h)} ")
        }
        sb.append("<polyline points=\"${curve.toString().trim()}\" fill=\"none\" stroke=\"$ACC\" stroke-width=\"2.4\"/>")
        sb.append(ln(x0, y, x1 + 8f, y, ST, 1.4f))
        for (i in -3..3) {
            val v = mu + i * sd
            sb.append(ln(px(v), y, px(v), y + 6f, ST, 1.2f))
            sb.append(tx(px(v), y + 20f, i.toString(), MU, "middle", size = 10))
        }
        sb.append(tx(px(mu), y + 38f, "μ = ${num(mu)}", ST, "middle", bold = true, size = 11))
        sb.append(ln(px(loC), y, px(loC), y - pdf(loC) * h, PT, 1.4f, "4 3"))
        sb.append(ln(px(hiC), y, px(hiC), y - pdf(hiC) * h, PT, 1.4f, "4 3"))
        sb.append(tx(180f, 262f, "منحنی نرمال — احتمال بین ${num(lo)} و ${num(hi)}", MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun supplyDemand(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "equilibrium").lowercase()
        val ceiling = spec.xNum("ceiling", 0f)
        val x0 = 54f; val x1 = 320f; val y0 = 206f; val y1 = 44f
        val sb = StringBuilder()
        sb.append(ln(x0, y0, x1, y0, ST, 1.6f)); sb.append(arrow(x1 - 12f, y0, x1 + 6f, y0, ST, 1.6f, 7f))
        sb.append(ln(x0, y0, x0, y1 + 18f, ST, 1.6f)); sb.append(arrow(x0, y1 + 30f, x0, y1 + 12f, ST, 1.6f, 7f))
        sb.append(tx(x1 + 2f, y0 + 16f, "Q", MU, "middle", bold = true, size = 12))
        sb.append(tx(x0 - 14f, y1 + 22f, "P", MU, "middle", bold = true, size = 12))
        sb.append(arrow(x0 + 16f, y0 - 30f, x1 - 22f, y0 - 150f, PT, 2.4f, 9f))
        sb.append(arrow(x0 + 16f, y0 - 32f, x1 - 22f, y1 + 40f, "#00796b", 2.4f, 9f))
        sb.append(tx(x0 + 22f, y0 - 34f, "تقاضا", PT, "start", bold = true, size = 11))
        sb.append(tx(x1 - 30f, y1 + 60f, "عرضه", "#00796b", "end", bold = true, size = 11))
        if (kind == "shift") sb.append(arrow(x0 + 60f, y0 - 55f, x1 - 60f, y0 - 162f, "rgba(214,51,108,.45)", 2f, 8f))
        if (ceiling > 0) {
            // V322 — سقف قیمت بزرگ خط را از کادر بیرون می‌برد؛ حالا در ناحیهٔ نمودار مهار می‌شود.
            val cy = (y0 - 40f - ceiling * 12f).coerceIn(y1 + 40f, y0 - 30f)
            sb.append(ln(x0, cy, x1 - 10f, cy, ACC, 1.8f, "6 4"))
            sb.append(tx(x1 - 14f, cy - 6f, "سقف قیمت", ACC, "end", bold = true, size = 10))
        }
        sb.append(cls(x0 + 118f, y0 - 118f, 5f, ST, "none", 0f))
        sb.append(tx(x0 + 130f, y0 - 122f, "تعادل", ST, "start", bold = true, size = 11))
        val name = when (kind) {
            "shift" -> "افزایش تقاضا — قیمت و مقدار تعادلی بالا می‌رود"
            "ceiling" -> "سقف قیمت — کمبود عرضه"
            else -> "تعادل بازار — تلاقی عرضه و تقاضا"
        }
        sb.append(tx(180f, 258f, name, ST, "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun gameMatrix(spec: FigureSpec): String {
        val kind = spec.xStr("kind", "game").lowercase()
        val sb = StringBuilder()
        if (kind == "islm") {
            val x0 = 54f; val x1 = 320f; val y0 = 210f; val y1 = 44f
            sb.append(ln(x0, y0, x1, y0, ST, 1.6f)); sb.append(arrow(x1 - 12f, y0, x1 + 6f, y0, ST, 1.6f, 7f))
            sb.append(ln(x0, y0, x0, y1, ST, 1.6f)); sb.append(arrow(x0, y1 + 14f, x0, y1 - 4f, ST, 1.6f, 7f))
            sb.append(tx(x1 + 2f, y0 + 16f, "Y", MU, "middle", bold = true, size = 12))
            sb.append(tx(x0 - 14f, y1 + 10f, "i", MU, "middle", bold = true, size = 12))
            sb.append(arrow(x0 + 20f, y0 - 24f, x1 - 26f, y1 + 46f, ACC, 2.4f, 9f))
            sb.append(tx(66f, 66f, "IS", ACC, "start", bold = true, size = 12))
            sb.append(arrow(x0 + 20f, y1 + 52f, x1 - 26f, y0 - 30f, "#00796b", 2.4f, 9f))
            sb.append(tx(x1 - 30f, y0 - 34f, "LM", "#00796b", "end", bold = true, size = 12))
            sb.append(cls((x0 + x1) / 2f, (y0 + y1) / 2f + 14f, 5f, ST, "none", 0f))
            sb.append(tx((x0 + x1) / 2f + 10f, (y0 + y1) / 2f + 10f, "E", ST, "start", bold = true, size = 12))
            sb.append(tx(180f, 258f, "مدل IS-LM — تعادل هم‌زمان بازار کالا و پول", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        var m = spec.xStr("mat").ifBlank { spec.xStr("m", "3,0;5,1") }.split(Regex("[;؛]")).map { rw -> rw.split(Regex("[,،]")).map { it.trim() } }.take(2)
        if (m.size < 2) m = listOf(listOf("3", "0"), listOf("5", "1"))
        val cell = min(62f, 240f / 3)
        val x0 = 180f - cell * 1.5f
        val y0 = 56f
        sb.append(tx(x0 + cell * 1.5f, 40f, "بازیکن ستون", MU, "middle", bold = true, size = 11))
        sb.append(tx(x0 - 26f, y0 + cell * 1.5f, "بازیکن", MU, "middle", bold = true, size = 11))
        for (r in 0..1) for (c in 0..1) {
            val v = m.getOrNull(r)?.getOrNull(c) ?: "0"
            sb.append(rc(x0 + c * cell, y0 + r * cell, cell, cell, "#ffffff", ST, 1.6f))
            sb.append(tx(x0 + c * cell + cell / 2f, y0 + r * cell + cell / 2f + 6f, v, ST, "middle", bold = true, size = 14))
        }
        sb.append(rc(x0, y0, cell * 2, cell * 2, "none", ST, 2f))
        sb.append(tx(180f, y0 + cell * 2 + 30f, "ماتریس بازی ۲×۲ — سود بازیکن سطر", MU, "middle", bold = true, size = 11))
        return sb.toString()
    }

    // ============================================================ helpers
    private fun ints(spec: FigureSpec, key: String, def: String): List<Int> =
        spec.xList(key, def).mapNotNull { faEnDigits(it).trim().toIntOrNull() }

    private fun num(v: Float): String {
        val r = Math.round(v * 100f) / 100f
        return if (r == Math.round(r).toFloat()) Math.round(r).toString() else r.toString()
    }

    private fun group(v: Long): String {
        val s = v.toString()
        val sb = StringBuilder()
        s.reversed().forEachIndexed { i, c -> if (i > 0 && i % 3 == 0) sb.append('،'); sb.append(c) }
        return sb.reverse().toString()
    }

    private fun f(v: Float): String {
        val r = Math.round(v * 100f) / 100f
        return if (r == Math.round(r).toFloat()) Math.round(r).toString() else r.toString()
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun ln(x1: Float, y1: Float, x2: Float, y2: Float, color: String, w: Float, dash: String = ""): String =
        "<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"$color\" stroke-width=\"${f(w)}\" stroke-linecap=\"round\"" +
            (if (dash.isNotBlank()) " stroke-dasharray=\"$dash\"" else "") + "/>"

    private fun rc(x: Float, y: Float, w: Float, h: Float, fill: String, stroke: String, sw: Float, rx: Float = 0f): String =
        "<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(w)}\" height=\"${f(h)}\"" +
            (if (rx > 0f) " rx=\"${f(rx)}\"" else "") +
            " fill=\"$fill\"" + (if (stroke.isNotBlank() && stroke != "none") " stroke=\"$stroke\" stroke-width=\"${f(sw)}\"" else "") + "/>"

    private fun cls(cx: Float, cy: Float, r: Float, fill: String, stroke: String, sw: Float): String =
        "<circle cx=\"${f(cx)}\" cy=\"${f(cy)}\" r=\"${f(r)}\" fill=\"$fill\"" +
            (if (stroke.isNotBlank() && stroke != "none") " stroke=\"$stroke\" stroke-width=\"${f(sw)}\"" else "") + "/>"

    private fun poly(pts: String, fill: String, stroke: String, sw: Float): String =
        "<polygon points=\"$pts\" fill=\"$fill\"" +
            (if (stroke.isNotBlank() && stroke != "none") " stroke=\"$stroke\" stroke-width=\"${f(sw)}\" stroke-linejoin=\"round\"" else "") + "/>"

    private fun path(d: String, fill: String, stroke: String, sw: Float, dash: String = ""): String =
        "<path d=\"$d\" fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"${f(sw)}\" stroke-linejoin=\"round\" stroke-linecap=\"round\"" +
            (if (dash.isNotBlank()) " stroke-dasharray=\"$dash\"" else "") + "/>"

    private fun arrow(x1: Float, y1: Float, x2: Float, y2: Float, color: String, w: Float, head: Float): String {
        val an = atan2(y2 - y1, x2 - x1).toFloat()
        val pts = "${f(x2)},${f(y2)} ${f(x2 - head * cos(an - 0.42f))},${f(y2 - head * sin(an - 0.42f))} ${f(x2 - head * cos(an + 0.42f))},${f(y2 - head * sin(an + 0.42f))}"
        return ln(x1, y1, x2, y2, color, w) + poly(pts, color, "none", 0f)
    }

    private fun tx(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean = false, size: Int = 11): String {
        if (s.isBlank()) return ""
        val weight = if (bold) " font-weight=\"700\"" else ""
        // V322 — جاگیری خودکار برچسب: اگر متن با اندازهٔ خواسته‌شده از بوم بیرون بزند، فونت تا ۷ کوچک و در
        // نهایت با «…» کوتاه می‌شود؛ مختصات هم داخل بوم مهار می‌شود تا هیچ برچسبی بریده نشود.
        val fit = fitLabel(x, s, anchor, size)
        return "<text x=\"${f(fit.first)}\" y=\"${f(y)}\" direction=\"ltr\" font-family=\"sans-serif\" font-size=\"${fit.third}\"$weight fill=\"$color\" text-anchor=\"$anchor\">${esc(FigureDigits.apply(fit.second))}</text>"
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
