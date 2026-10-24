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

    data class InteractiveRegion(
        val key: String,
        val title: String,
        val defaultVal: String,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val defaultEmpty: Boolean = false
    )

    fun canvasWidth(spec: FigureSpec): Float = when (spec.type) {
        "axprobtree" -> {
            val st = probTreeStageLists(spec).size
            when {
                st >= 4 -> 520f
                st == 3 -> 440f
                else -> 360f
            }
        }
        else -> 360f
    }

    fun canvasHeight(spec: FigureSpec): Float = when (spec.type) {
        "axprobtree" -> {
            val st = probTreeStageLists(spec).size
            when {
                st >= 4 -> 440f
                st == 3 -> 340f
                else -> 280f
            }
        }
        else -> 280f
    }

    fun interactiveRegions(spec: FigureSpec): List<InteractiveRegion> {
        val out = mutableListOf<InteractiveRegion>()
        when (spec.type) {
            "axkenken" -> kenKenRender(spec, out)
            "axdicefold" -> diceFoldRender(spec, out)
            "axprobtree" -> probabilityTreeRender(spec, out)
            "axorbital" -> orbitalDiagramRender(spec, out)
            "axpunnett" -> punnettSquareRender(spec, out)
        }
        return out
    }

    fun body(spec: FigureSpec): String {
        val cw = canvasWidth(spec)
        val title = spec.xStr("title")
        val head = if (title.isBlank()) "" else tx(cw / 2f, 14f, title, "#1a2433", "middle", bold = true, size = 13, canvasW = cw)
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

    /** V335.8 — استخراج کلید خانه‌های خالی (بدون علامت سؤال) و مقادیر بازنویسی‌شده یا پاسخ دانش‌آموز. */
    internal fun parseEmptySet(spec: FigureSpec): Set<String> =
        spec.xStr("empty", "")
            .split(',', '،', ';', '؛')
            .map { faEnDigits(it).trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    internal fun parseCellMap(spec: FigureSpec, field: String): Map<String, String> {
        val raw = spec.xStr(field, "").trim()
        if (raw.isEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        raw.split(';', '؛').forEach { entry ->
            val idx = entry.indexOf(':').takeIf { it > 0 } ?: entry.indexOf('=').takeIf { it > 0 } ?: -1
            if (idx > 0) {
                val k = faEnDigits(entry.substring(0, idx)).trim()
                val v = entry.substring(idx + 1).trim()
                if (k.isNotEmpty()) out[k] = v
            }
        }
        return out
    }

    private fun numberTower(spec: FigureSpec): String {
        val baseRaw = ints(spec, "base", "1,2,3,4").ifEmpty { listOf(1, 2, 3, 4) }.take(6)
        val minus = spec.xStr("op", "+") == "-"
        val blank = spec.xStr("blank", "0") == "1"
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
        val base = baseRaw.mapIndexed { idx, v ->
            ovMap["0_$idx"]?.let { faEnDigits(it).toIntOrNull() } ?: v
        }
        val levels = mutableListOf(base)
        var cur = base
        while (cur.size > 1) {
            val lvIdx = levels.size
            val next = mutableListOf<Int>()
            for (i in 0 until cur.size - 1) {
                val computed = if (minus) abs(cur[i] - cur[i + 1]) else cur[i] + cur[i + 1]
                val ov = ovMap["${lvIdx}_$i"]?.let { faEnDigits(it).toIntOrNull() }
                next.add(ov ?: computed)
            }
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
                val key = "${lvIdx}_$k"
                val isEmpty = key in emptySet || (blank && (lvIdx + k) % 3 == 1)
                val stuAns = ansMap[key]?.trim().orEmpty()
                val shownVal = ovMap[key]?.trim() ?: arr[k].toString()
                if (isEmpty) {
                    val fill = if (stuAns.isNotEmpty()) "#eef2ff" else "#ffffff"
                    sb.append(rc(bx, ly, bw - 3f, bh - 3f, fill, ACC, 1.8f, 4f))
                    if (stuAns.isNotEmpty()) {
                        sb.append(tx(bx + (bw - 3f) / 2f, ly + bh * 0.66f, stuAns, ACC, "middle", bold = true, size = 13))
                    }
                } else {
                    sb.append(rc(bx, ly, bw - 3f, bh - 3f, if (lvIdx == 0) "rgba(108,99,245,.16)" else "#ffffff", ST, 1.6f, 4f))
                    sb.append(tx(bx + (bw - 3f) / 2f, ly + bh * 0.66f, shownVal, ST, "middle", bold = true, size = 13))
                }
            }
        }
        sb.append(tx(180f, 268f, if (minus) "هر آجر = تفاضل دو آجر زیرین" else "هر آجر = جمع دو آجر زیرین", MU, "middle", size = 11))
        return sb.toString()
    }

    private fun multiplicationWheel(spec: FigureSpec): String {
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
        val cBase = spec.xNum("c", 3f).toInt().coerceIn(1, 20)
        val c = ovMap["center"]?.let { faEnDigits(it).toIntOrNull() } ?: cBase
        val n = spec.xNum("n", 10f).toInt().coerceIn(3, 12)
        val plus = spec.xStr("op", "×") == "+"
        val hide = spec.xStr("hide", "0") == "1"
        val sb = StringBuilder()
        sb.append(cls(180f, 142f, 96f, "none", ACC, 1.6f))
        val cEmpty = "center" in emptySet
        val cAns = ansMap["center"]?.trim().orEmpty()
        sb.append(cls(180f, 142f, 34f, if (cEmpty) "#ffffff" else "rgba(108,99,245,.16)", ACC, 2.2f))
        if (cEmpty) {
            if (cAns.isNotEmpty()) sb.append(tx(180f, 145f, cAns, ACC, "middle", bold = true, size = 18))
            sb.append(tx(180f, 162f, if (plus) "+" else "×", ACC, "middle", bold = true, size = 11))
        } else {
            sb.append(tx(180f, 140f, (ovMap["center"]?.trim() ?: c.toString()), ACC, "middle", bold = true, size = 18))
            sb.append(tx(180f, 158f, if (plus) "+" else "×", ACC, "middle", bold = true, size = 12))
        }
        for (j in 0 until n) {
            val a = (-PI / 2 + j * 2 * PI / n)
            val ox = 180f + 96f * cos(a).toFloat()
            val oy = 142f + 96f * sin(a).toFloat()
            val ix = 180f + 56f * cos(a).toFloat()
            val iy = 142f + 56f * sin(a).toFloat()
            val mFactor = j + 1
            val v = if (plus) c + mFactor else c * mFactor
            val key = "s_$j"
            val isEmpty = key in emptySet || "$j" in emptySet || (hide && j % 3 == 2)
            val stuAns = (ansMap[key] ?: ansMap["$j"])?.trim().orEmpty()
            val shownVal = (ovMap[key] ?: ovMap["$j"])?.trim() ?: v.toString()
            sb.append(ln(180f + 34f * cos(a).toFloat(), 142f + 34f * sin(a).toFloat(), ox, oy, "#d5dce6", 1.6f))
            // نمایش عامل کوچک روی پره تا دانش‌آموز دقیق بداند هر خانهٔ خالی حاصل کدام ضرب/جمع است
            sb.append(cls(ix, iy, 9.5f, "#f8fafc", "#cbd5e1", 1f))
            sb.append(tx(ix, iy + 3.5f, mFactor.toString(), MU, "middle", bold = true, size = 9))
            if (isEmpty) {
                sb.append(cls(ox, oy, 17f, if (stuAns.isNotEmpty()) "#eef2ff" else "#ffffff", ACC, 1.9f))
                if (stuAns.isNotEmpty()) {
                    sb.append(tx(ox, oy + 5f, stuAns, ACC, "middle", bold = true, size = 12))
                }
            } else {
                sb.append(cls(ox, oy, 17f, "#ffffff", ST, 1.8f))
                sb.append(tx(ox, oy + 5f, shownVal, ST, "middle", bold = true, size = 12))
            }
        }
        return sb.toString()
    }

    private fun multiplicationGrid(spec: FigureSpec): String {
        val baseRows = ints(spec, "rows", "2,4,7").ifEmpty { listOf(2, 4, 7) }.take(4)
        val baseCols = ints(spec, "cols", "3,5,8").ifEmpty { listOf(3, 5, 8) }.take(4)
        val blank = spec.xNum("blank", 0f).toInt().coerceIn(0, 2)
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
        val rows = baseRows.mapIndexed { r, v -> ovMap["r$r"]?.let { faEnDigits(it).toIntOrNull() } ?: v }
        val cols = baseCols.mapIndexed { c, v -> ovMap["c$c"]?.let { faEnDigits(it).toIntOrNull() } ?: v }
        val cw = min(56f, 268f / (cols.size + 1))
        val ch = min(42f, 216f / (rows.size + 1))
        val x0 = 180f - cw * (cols.size + 1) / 2f
        val y0 = 26f + (218f - ch * (rows.size + 1)) / 2f
        val sb = StringBuilder()
        for (c in cols.indices) {
            val key = "c$c"
            val isEmpty = key in emptySet || blank == 2
            val stuAns = ansMap[key]?.trim().orEmpty()
            val shown = ovMap[key]?.trim() ?: cols[c].toString()
            sb.append(rc(x0 + (c + 1) * cw, y0, cw, ch, if (isEmpty) "#ffffff" else "rgba(39,196,168,.18)", if (isEmpty) ACC else ST, if (isEmpty) 1.8f else 1.4f))
            if (isEmpty) {
                if (stuAns.isNotEmpty()) sb.append(tx(x0 + (c + 1.5f) * cw, y0 + ch * 0.66f, stuAns, ACC, "middle", bold = true, size = 14))
            } else {
                sb.append(tx(x0 + (c + 1.5f) * cw, y0 + ch * 0.66f, shown, ST, "middle", bold = true, size = 14))
            }
        }
        for (r in rows.indices) {
            val rKey = "r$r"
            val rEmpty = rKey in emptySet || blank == 2
            val rAns = ansMap[rKey]?.trim().orEmpty()
            val rShown = ovMap[rKey]?.trim() ?: rows[r].toString()
            sb.append(rc(x0, y0 + (r + 1) * ch, cw, ch, if (rEmpty) "#ffffff" else "rgba(39,196,168,.18)", if (rEmpty) ACC else ST, if (rEmpty) 1.8f else 1.4f))
            if (rEmpty) {
                if (rAns.isNotEmpty()) sb.append(tx(x0 + cw / 2f, y0 + (r + 1.5f) * ch, rAns, ACC, "middle", bold = true, size = 14))
            } else {
                sb.append(tx(x0 + cw / 2f, y0 + (r + 1.5f) * ch, rShown, ST, "middle", bold = true, size = 14))
            }
            for (c in cols.indices) {
                val key = "${r}_$c"
                val v = rows[r] * cols[c]
                val isEmpty = key in emptySet || (blank == 1 && (r + c) % 2 == 1)
                val stuAns = ansMap[key]?.trim().orEmpty()
                val shown = ovMap[key]?.trim() ?: v.toString()
                val fill = when {
                    isEmpty && stuAns.isNotEmpty() -> "#eef2ff"
                    isEmpty -> "#ffffff"
                    else -> "#ffffff"
                }
                sb.append(rc(x0 + (c + 1) * cw, y0 + (r + 1) * ch, cw, ch, fill, if (isEmpty) ACC else ST, if (isEmpty) 1.8f else 1.4f))
                if (isEmpty) {
                    if (stuAns.isNotEmpty()) sb.append(tx(x0 + (c + 1.5f) * cw, y0 + (r + 1.5f) * ch, stuAns, ACC, "middle", bold = true, size = 14))
                } else {
                    sb.append(tx(x0 + (c + 1.5f) * cw, y0 + (r + 1.5f) * ch, shown, ACC, "middle", bold = true, size = 14))
                }
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
        val parsed = rowsRaw
            .map { rw -> rw.split(Regex("[,،]")).map { max(0, min(6, faEnDigits(it.trim()).toIntOrNull() ?: 0)) } }
            .filter { it.isNotEmpty() }
            .take(5)
        val rawGrid = parsed.ifEmpty { listOf(listOf(2, 1, 3), listOf(1, 0, 1), listOf(2, 2, 1)) }
        val rowsN = rawGrid.size
        val colsN = rawGrid.maxOf { it.size }.coerceIn(1, 5)
        val grid = List(rowsN) { r -> List(colsN) { c -> rawGrid[r].getOrElse(c) { 0 }.coerceIn(0, 6) } }
        val total = grid.sumOf { r -> r.sum() }
        val maxH = grid.maxOf { it.maxOrNull() ?: 0 }.coerceAtLeast(1)
        val hide = spec.xStr("hide", "0") == "1"
        val sb = StringBuilder()
        when (view) {
            "top" -> {
                val s = min(46f, 186f / max(rowsN, colsN))
                val gw = colsN * s
                val gh = rowsN * s
                val x0 = 180f - gw / 2f
                val y0 = 132f - gh / 2f
                sb.append(tx(180f, y0 - 12f, "نمای از بالا (تعداد مکعب در هر ستون)", MU, "middle", bold = true, size = 11))
                for (r in 0 until rowsN) {
                    for (c in 0 until colsN) {
                        val hh = grid[r][c]
                        val x = x0 + c * s
                        val y = y0 + r * s
                        sb.append(rc(x, y, s - 2f, s - 2f, if (hh > 0) "rgba(108,99,245,.20)" else "#f8fafc", ST, 1.5f, 4f))
                        sb.append(tx(x + (s - 2f) / 2f, y + s * 0.62f, hh.toString(), if (hh > 0) ST else "#94a3b8", "middle", bold = true, size = 14))
                    }
                }
            }
            "front" -> {
                // نمای روبه‌رو: بیشترین ارتفاع هر ستون (در راستای دید روبه‌رو) به‌صورت مکعب‌های مربعی روی خط زمین
                val colHeights = (0 until colsN).map { c -> (0 until rowsN).maxOf { r -> grid[r][c] } }
                val s = min(38f, min(220f / colsN, 164f / maxH))
                val gw = colsN * s
                val x0 = 180f - gw / 2f
                val yBase = 216f
                sb.append(tx(180f, 30f, "نمای روبه‌رو", MU, "middle", bold = true, size = 12))
                sb.append(ln(x0 - 16f, yBase, x0 + gw + 16f, yBase, ST, 2.2f))
                for (c in 0 until colsN) {
                    val h = colHeights[c]
                    for (z in 0 until h) {
                        sb.append(rc(x0 + c * s, yBase - (z + 1) * s, s, s, "#c7d2fe", ST, 1.5f, 2f))
                    }
                }
            }
            "side" -> {
                // نمای از پهلو: بیشترین ارتفاع هر سطر به‌صورت مکعب‌های مربعی روی خط زمین
                val rowHeights = (0 until rowsN).map { r -> grid[r].maxOrNull() ?: 0 }
                val s = min(38f, min(220f / rowsN, 164f / maxH))
                val gw = rowsN * s
                val x0 = 180f - gw / 2f
                val yBase = 216f
                sb.append(tx(180f, 30f, "نمای از پهلو", MU, "middle", bold = true, size = 12))
                sb.append(ln(x0 - 16f, yBase, x0 + gw + 16f, yBase, ST, 2.2f))
                for (r in 0 until rowsN) {
                    val h = rowHeights[r]
                    for (z in 0 until h) {
                        sb.append(rc(x0 + r * s, yBase - (z + 1) * s, s, s, "#99f6e4", ST, 1.5f, 2f))
                    }
                }
            }
            else -> {
                // نمای ایزومتریک سه‌بعدی با ترتیب صحیح نقاش (از عقب به جلو و از پایین به بالا) و خطوط تفکیک هر مکعب واحد
                val spanUnits = (rowsN + colsN).coerceAtLeast(3)
                val dx = min(22f, 132f / spanUnits)
                val dy = dx * 0.52f
                val dz = min(20f, 118f / (maxH + spanUnits * 0.35f))
                val ox = 180f - (colsN - rowsN) * dx * 0.5f
                val oy = 172f - (rowsN + colsN) * dy * 0.35f + maxH * dz * 0.25f
                for (r in 0 until rowsN) {
                    for (c in 0 until colsN) {
                        val hh = grid[r][c]
                        for (z in 0 until hh) {
                            val cx = ox + (c - r) * dx
                            val cy = oy + (c + r) * dy - z * dz
                            sb.append(poly("${f(cx)},${f(cy - dz)} ${f(cx + dx)},${f(cy - dy - dz)} ${f(cx)},${f(cy - 2f * dy - dz)} ${f(cx - dx)},${f(cy - dy - dz)}", "#e0e7ff", ST, 1.15f))
                            sb.append(poly("${f(cx - dx)},${f(cy - dy - dz)} ${f(cx)},${f(cy - dz)} ${f(cx)},${f(cy)} ${f(cx - dx)},${f(cy - dy)}", "#a5b4fc", ST, 1.15f))
                            sb.append(poly("${f(cx)},${f(cy - dz)} ${f(cx + dx)},${f(cy - dy - dz)} ${f(cx + dx)},${f(cy - dy)} ${f(cx)},${f(cy)}", "#818cf8", ST, 1.15f))
                        }
                    }
                }
            }
        }
        val cap = when (view) {
            "top" -> if (hide) "تعداد کل مکعب‌ها: ؟" else "تعداد کل مکعب‌ها: $total"
            "front" -> "نمای روبه‌رو" + if (hide) "" else " (کل مکعب‌ها: $total)"
            "side" -> "نمای از پهلو" + if (hide) "" else " (کل مکعب‌ها: $total)"
            else -> "تعداد مکعب‌ها: " + if (hide) "؟" else total.toString()
        }
        sb.append(tx(180f, 258f, cap, if (hide) PT else MU, "middle", bold = true, size = 12))
        return sb.toString()
    }

    private fun calendar(spec: FigureSpec): String {
        val month = spec.xStr("month", "مهر")
        val start = spec.xNum("start", 3f).toInt().coerceIn(0, 6)
        val days = spec.xNum("days", 30f).toInt().coerceIn(20, 31)
        val mark = ints(spec, "mark", "12,23")
        val blank = spec.xStr("blank", "0") == "1"
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
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
            val key = day.toString()
            val bx = x0 + col * cw
            val by = y0 + ch + row * ch
            val isMark = mark.contains(day)
            val isEmpty = key in emptySet || (blank && day % 5 == 0)
            val stuAns = ansMap[key]?.trim().orEmpty()
            val shown = ovMap[key]?.trim() ?: key
            if (isEmpty) {
                sb.append(rc(bx, by, cw, ch, if (stuAns.isNotEmpty()) "#eef2ff" else "#ffffff", ACC, 1.7f))
                if (stuAns.isNotEmpty()) {
                    sb.append(tx(bx + cw / 2f, by + ch * 0.66f, stuAns, ACC, "middle", bold = true, size = 12))
                }
            } else {
                sb.append(rc(bx, by, cw, ch, if (isMark) "rgba(39,196,168,.26)" else "#ffffff", ST, 1.1f))
                sb.append(tx(bx + cw / 2f, by + ch * 0.66f, shown, if (isMark) "#00796b" else ST, "middle", bold = isMark, size = 12))
            }
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
            // سقف و قرقرهٔ ثابت + قرقرهٔ متحرک با طناب کاملاً پیوسته و مماس بر چرخ‌ها
            sb.append(rc(86f, 18f, 188f, 10f, "#cbd5e1", ST, 1.6f, 3f))
            for (hx in 96..264 step 24) {
                sb.append(ln(hx.toFloat(), 18f, (hx + 8).toFloat(), 10f, MU, 1.2f))
            }
            val fx = 156f; val fy = 66f; val fr = 24f
            val mx = 206f; val my = 136f; val mr = 22f
            // اتصال قرقرهٔ ثابت و سر ثابت طناب به سقف
            sb.append(ln(fx, 28f, fx, fy, ST, 3.2f))
            sb.append(ln(mx + mr, 28f, mx + mr, my, "#546e7a", 2.4f))
            // مسیر پیوستهٔ طناب از قرقرهٔ متحرک به قرقرهٔ ثابت و سر آزاد نیروی محرک
            sb.append(ln(mx - mr, my, fx + fr, fy, "#546e7a", 2.4f))
            sb.append(ln(fx - fr, fy, 102f, 162f, "#546e7a", 2.6f))
            sb.append(arrow(112f, 140f, 98f, 170f, ACC, 2.6f, 9f))
            // چرخ‌های قرقره
            sb.append(cls(fx, fy, fr, "#eef2ff", ACC, 2.2f))
            sb.append(cls(fx, fy, 5f, ST, ST, 1f))
            sb.append(cls(mx, my, mr, "rgba(39,196,168,.20)", GRN, 2.2f))
            sb.append(cls(mx, my, 5f, GRN, ST, 1f))
            // وزنهٔ متصل به محور قرقرهٔ متحرک
            sb.append(ln(mx, my + mr, mx, 174f, ST, 2.6f))
            sb.append(rc(mx - 28f, 174f, 56f, 36f, "rgba(214,51,108,.16)", PT, 1.8f, 6f))
            sb.append(tx(mx, 197f, "بار ${num(load)}N", PT, "middle", bold = true, size = 11))
            sb.append(tx(92f, 192f, "نیرو ${num(effort)}N", ACC, "middle", bold = true, size = 11))
            sb.append(tx(180f, 252f, "قرقرهٔ ثابت و متحرک — مزیت مکانیکی ≈ ۲", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }
        if (kind == "gears" || kind == "gear") {
            // چرخ‌دنده‌های واقعاً درگیر متناسب با l1 و l2 همراه با دندانه و جهت چرخش معکوس
            val ratio = (l1 / (l1 + l2)).coerceIn(0.32f, 0.68f)
            val totalR = 88f
            val ra = (totalR * ratio).coerceIn(26f, 58f)
            val rb = (totalR - ra).coerceIn(24f, 58f)
            val toothH = 6.5f
            val dist = ra + rb + toothH * 0.95f
            val cxA = 180f - dist / 2f + (rb - ra) * 0.25f
            val cxB = cxA + dist
            val cy = 126f
            fun drawGear(cx: Float, r: Float, teeth: Int, phase: Float, fill: String, strokeCol: String): String {
                val pts = StringBuilder()
                val step = (2.0 * PI / teeth).toFloat()
                for (i in 0 until teeth) {
                    val a0 = phase + i * step
                    val a1 = a0 + step * 0.22f
                    val a2 = a0 + step * 0.50f
                    val a3 = a0 + step * 0.72f
                    fun addPt(rad: Float, ang: Float) {
                        if (pts.isNotEmpty()) pts.append(' ')
                        pts.append("${f(cx + rad * cos(ang))},${f(cy + rad * sin(ang))}")
                    }
                    addPt(r, a0); addPt(r + toothH, a1); addPt(r + toothH, a2); addPt(r, a3)
                }
                return poly(pts.toString(), fill, strokeCol, 1.8f) +
                    cls(cx, cy, r * 0.66f, "#ffffff", strokeCol, 1.4f) +
                    cls(cx, cy, 7f, strokeCol, ST, 1.2f)
            }
            val tA = max(8, (ra / 4f).toInt())
            val tB = max(6, (rb / 4f).toInt())
            sb.append(drawGear(cxA, ra, tA, 0f, "rgba(108,99,245,.22)", ACC))
            sb.append(drawGear(cxB, rb, tB, (PI / tB).toFloat(), "rgba(240,162,2,.28)", "#d97706"))
            // کمان جهت چرخش معکوس در بالای دو چرخ‌دنده
            sb.append(path("M ${f(cxA - 18f)} ${f(cy - ra - 14f)} Q ${f(cxA)} ${f(cy - ra - 24f)} ${f(cxA + 18f)} ${f(cy - ra - 14f)}", "none", ACC, 2f))
            sb.append(poly("${f(cxA + 20f)},${f(cy - ra - 13f)} ${f(cxA + 12f)},${f(cy - ra - 19f)} ${f(cxA + 13f)},${f(cy - ra - 10f)}", ACC, "none", 0f))
            sb.append(path("M ${f(cxB + 16f)} ${f(cy - rb - 14f)} Q ${f(cxB)} ${f(cy - rb - 24f)} ${f(cxB - 16f)} ${f(cy - rb - 14f)}", "none", "#d97706", 2f))
            sb.append(poly("${f(cxB - 18f)},${f(cy - rb - 13f)} ${f(cxB - 10f)},${f(cy - rb - 19f)} ${f(cxB - 11f)},${f(cy - rb - 10f)}", "#d97706", "none", 0f))
            sb.append(tx(cxA, cy + ra + 24f, "محرک (${tA} دندانه)", ACC, "middle", bold = true, size = 10))
            sb.append(tx(cxB, cy + rb + 24f, "مقاوم (${tB} دندانه)", "#b45309", "middle", bold = true, size = 10))
            sb.append(tx(180f, 250f, "چرخ‌دنده‌های درگیر — نسبت شعاع‌ها = ${num(l1 / l2)}", MU, "middle", bold = true, size = 11))
            return sb.toString()
        }
        if (kind == "inclined") {
            sb.append(ln(30f, 210f, 330f, 210f, ST, 2f))
            sb.append(poly("52,210 306,210 306,86", "rgba(108,99,245,.12)", ST, 2.2f))
            sb.append(rc(290f, 194f, 16f, 16f, "none", MU, 1.2f))
            val slopeDeg = -atan2(124f, 254f) * 180f / PI.toFloat()
            sb.append("<g transform=\"translate(174,150) rotate(${f(slopeDeg)})\">")
            sb.append(rc(-24f, -34f, 48f, 34f, "rgba(214,51,108,.16)", PT, 1.8f, 5f))
            sb.append(tx(0f, -13f, "${num(load)}N", PT, "middle", bold = true, size = 11))
            sb.append(arrow(26f, -17f, 76f, -17f, ACC, 2.6f, 9f))
            sb.append("</g>")
            sb.append(tx(246f, 92f, "نیرو ${num(effort)}N", ACC, "middle", bold = true, size = 11))
            sb.append(tx(180f, 248f, "سطح شیب‌دار — مزیت مکانیکی = ${num(l1 / l2)}", MU, "middle", bold = true, size = 12))
            return sb.toString()
        }

        // اهرم‌های نوع اول، دوم و سوم با جهت فیزیکی صحیح نیروها و بازوهای متناسب با l1 و l2
        val x1 = 48f
        val x2 = 312f
        val y = 148f
        sb.append(ln(x1, y, x2, y, ST, 6f))
        when (kind) {
            "lever2" -> {
                val fulc = x1 + 18f
                val efX = x2 - 22f
                val ratio = (l2 / max(l1, l2 + 0.5f)).coerceIn(0.28f, 0.72f)
                val ldX = fulc + (efX - fulc) * ratio
                sb.append(poly("${f(fulc)},${f(y + 3f)} ${f(fulc - 16f)},${f(y + 36f)} ${f(fulc + 16f)},${f(y + 36f)}", "rgba(108,99,245,.25)", ST, 2f))
                sb.append(tx(fulc, y + 50f, "تکیه‌گاه", ST, "middle", bold = true, size = 10))
                // وزنه روی میله و پیکان وزن (بار) به سمت پایین
                sb.append(rc(ldX - 25f, y - 38f, 50f, 33f, "rgba(214,51,108,.16)", PT, 1.8f, 6f))
                sb.append(tx(ldX, y - 17f, "${num(load)} N", PT, "middle", bold = true, size = 11))
                sb.append(arrow(ldX, y + 5f, ldX, y + 40f, PT, 2.8f, 9f))
                sb.append(tx(ldX, y + 54f, "بار", PT, "middle", bold = true, size = 10))
                // نیروی محرک رو به بالا در سر میله
                sb.append(arrow(efX, y - 4f, efX, y - 54f, ACC, 2.8f, 9f))
                sb.append(rc(efX - 28f, y - 90f, 56f, 30f, "rgba(108,99,245,.14)", ACC, 1.6f, 6f))
                sb.append(tx(efX, y - 70f, "${num(effort)} N", ACC, "middle", bold = true, size = 11))
                sb.append(tx(180f, 248f, "اهرم نوع دوم (بار در وسط) — مزیت مکانیکی = ${num(l1 / l2)}", MU, "middle", bold = true, size = 11))
            }
            "lever3" -> {
                val fulc = x1 + 18f
                val ldX = x2 - 26f
                val ratio = (l1 / max(l2, l1 + 0.5f)).coerceIn(0.28f, 0.72f)
                val efX = fulc + (ldX - fulc) * ratio
                sb.append(poly("${f(fulc)},${f(y + 3f)} ${f(fulc - 16f)},${f(y + 36f)} ${f(fulc + 16f)},${f(y + 36f)}", "rgba(108,99,245,.25)", ST, 2f))
                sb.append(tx(fulc, y + 50f, "تکیه‌گاه", ST, "middle", bold = true, size = 10))
                // نیروی محرک رو به بالا در وسط
                sb.append(arrow(efX, y - 4f, efX, y - 54f, ACC, 2.8f, 9f))
                sb.append(rc(efX - 28f, y - 90f, 56f, 30f, "rgba(108,99,245,.14)", ACC, 1.6f, 6f))
                sb.append(tx(efX, y - 70f, "${num(effort)} N", ACC, "middle", bold = true, size = 11))
                // بار در سر دیگر با پیکان وزن رو به پایین
                sb.append(rc(ldX - 25f, y - 38f, 50f, 33f, "rgba(214,51,108,.16)", PT, 1.8f, 6f))
                sb.append(tx(ldX, y - 17f, "${num(load)} N", PT, "middle", bold = true, size = 11))
                sb.append(arrow(ldX, y + 5f, ldX, y + 40f, PT, 2.8f, 9f))
                sb.append(tx(ldX, y + 54f, "بار", PT, "middle", bold = true, size = 10))
                sb.append(tx(180f, 248f, "اهرم نوع سوم (نیرو در وسط) — مزیت مکانیکی = ${num(l1 / l2)}", MU, "middle", bold = true, size = 11))
            }
            else -> {
                val ldX = x1 + 28f
                val efX = x2 - 28f
                val fulc = (ldX + (efX - ldX) * (l2 / (l1 + l2))).coerceIn(ldX + 46f, efX - 46f)
                sb.append(poly("${f(fulc)},${f(y + 3f)} ${f(fulc - 18f)},${f(y + 38f)} ${f(fulc + 18f)},${f(y + 38f)}", "rgba(108,99,245,.25)", ST, 2f))
                sb.append(tx(fulc, y + 52f, "تکیه‌گاه", ST, "middle", bold = true, size = 10))
                // وزنه (بار) در سمت چپ با پیکان وزن رو به پایین
                sb.append(rc(ldX - 25f, y - 38f, 50f, 33f, "rgba(214,51,108,.16)", PT, 1.8f, 6f))
                sb.append(tx(ldX, y - 17f, "${num(load)} N", PT, "middle", bold = true, size = 11))
                sb.append(arrow(ldX, y + 5f, ldX, y + 40f, PT, 2.8f, 9f))
                sb.append(tx(ldX, y + 54f, "بار", PT, "middle", bold = true, size = 10))
                // نیروی محرک رو به پایین در سمت راست
                sb.append(rc(efX - 28f, y - 92f, 56f, 30f, "rgba(108,99,245,.14)", ACC, 1.6f, 6f))
                sb.append(tx(efX, y - 72f, "${num(effort)} N", ACC, "middle", bold = true, size = 11))
                sb.append(arrow(efX, y - 58f, efX, y - 6f, ACC, 2.8f, 9f))
                sb.append(tx((ldX + fulc) / 2f, y - 8f, "L₂=${num(l2)}", PT, "middle", bold = true, size = 10))
                sb.append(tx((fulc + efX) / 2f, y - 8f, "L₁=${num(l1)}", ACC, "middle", bold = true, size = 10))
                sb.append(tx(180f, 248f, "اهرم نوع اول — مزیت مکانیکی = ${num(l1 / l2)}", MU, "middle", bold = true, size = 12))
            }
        }
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
        val syl = splitList(spec.xStr("syll", "با,ران")).take(6).ifEmpty { listOf("با", "ران") }
        val defaultWord = syl.joinToString("")
        val word = spec.xStr("word", defaultWord).ifBlank { defaultWord }
        val blank = spec.xStr("mode", "join").lowercase() == "blank"
        val n = syl.size
        val locoW = 48f
        val gap = 8f
        val carW = min(56f, (284f - locoW - n * gap) / n)
        val totalW = locoW + n * gap + n * carW
        val startX = 180f - totalW / 2f
        val carY = 82f
        val carH = 48f
        val wheelY = carY + carH + 9f
        val railY = wheelY + 8f
        val sb = StringBuilder()

        // ریل راه‌آهن در پایین چرخ‌ها (بدون عبور از روی متن هجاها!)
        sb.append(ln(18f, railY, 342f, railY, "#64748b", 3f))
        for (txPos in 24..336 step 18) {
            sb.append(ln(txPos.toFloat(), railY, (txPos - 4).toFloat(), railY + 5f, "#94a3b8", 2f))
        }

        // لوکوموتیو در سمت چپ (کشندهٔ قطار در جهت خواندن راست‌به‌چپ فارسی: واگن اول در راست‌ترین جایگاه)
        val lx = startX
        // دودکش و بخار
        sb.append(rc(lx + 8f, carY - 16f, 10f, 16f, "#475569", ST, 1.5f, 2f))
        sb.append(cls(lx + 13f, carY - 24f, 5f, "#e2e8f0", "none", 0f))
        sb.append(cls(lx + 21f, carY - 31f, 6.5f, "#cbd5e1", "none", 0f))
        // بدنه و کابین لوکوموتیو
        sb.append(rc(lx + 22f, carY - 10f, 26f, carH + 10f, ACC, ST, 1.8f, 5f))
        sb.append(rc(lx + 27f, carY - 4f, 16f, 16f, "#e0f2fe", ST, 1.2f, 3f))
        sb.append(rc(lx, carY + 8f, 26f, carH - 8f, "#7c3aed", ST, 1.8f, 5f))
        sb.append(poly("${f(lx)},${f(carY + carH)} ${f(lx - 8f)},${f(carY + carH)} ${f(lx)},${f(carY + carH - 12f)}", "#ef4444", ST, 1.3f))
        sb.append(cls(lx + 12f, wheelY, 7.5f, "#334155", ST, 1.6f))
        sb.append(cls(lx + 36f, wheelY, 8.5f, "#334155", ST, 1.6f))

        val carTints = listOf("#eef2ff", "#ecfdf5", "#fffbeb", "#fdf2f8", "#f0f9ff", "#f5f3ff")
        val carBorders = listOf(ACC, "#10b981", "#f59e0b", PT, "#0284c7", "#8b5cf6")

        // چیدمان راست‌به‌چپ هجاها: هجای اول (i=0) در راست‌ترین واگن و هجای آخر نزدیک لوکوموتیو
        for (i in 0 until n) {
            val posFromLeft = n - 1 - i
            val cx = startX + locoW + gap + posFromLeft * (carW + gap)
            // قلاب اتصال کوتاه فقط در فاصلهٔ خالی بین دو واگن (هرگز از روی متن واگن رد نمی‌شود!)
            val leftGapX = cx - gap
            sb.append(ln(leftGapX, carY + carH * 0.62f, cx, carY + carH * 0.62f, ST, 2.8f))
            val fill = carTints[i % carTints.size]
            val brd = carBorders[i % carBorders.size]
            sb.append(rc(cx, carY, carW, carH, fill, brd, 1.9f, 7f))
            sb.append(tx(cx + carW - 9f, carY + 13f, pFa(i + 1), MU, "middle", bold = true, size = 9))
            val fSize = if (syl[i].length >= 4 || carW < 44f) 13 else 16
            sb.append(tx(cx + carW / 2f, carY + carH * 0.66f, syl[i], ST, "middle", bold = true, size = fSize))
            sb.append(cls(cx + carW * 0.25f, wheelY, 6.5f, "#ffffff", ST, 1.6f))
            sb.append(cls(cx + carW * 0.75f, wheelY, 6.5f, "#ffffff", ST, 1.6f))
        }

        sb.append(rc(68f, 186f, 224f, 44f, if (blank) "#fff8e1" else "rgba(39,196,168,.18)", if (blank) PT else GRN, 2f, 10f))
        sb.append(tx(180f, 214f, if (blank) "کلمهٔ ساخته‌شده: ؟" else "کلمه: $word", if (blank) PT else "#00796b", "middle", bold = true, size = 15))
        return sb.toString()
    }

    // ============================================================ متوسطهٔ اول
    private fun algebraTiles(spec: FigureSpec): String {
        val x2 = spec.xNum("x2", 1f).toInt().coerceIn(0, 9)
        val xcRaw = spec.xNum("x", 2f).toInt().coerceIn(0, 16)
        val kRaw = spec.xNum("k", 3f).toInt().coerceIn(0, 24)
        val neg = spec.xStr("neg", "0") == "1"
        val show = spec.xStr("show", "1")
        val xc = if (neg) -xcRaw else xcRaw
        val k = if (neg) -kRaw else kRaw

        val fillPos = "rgba(108,99,245,.18)"
        val fillXPos = "rgba(39,196,168,.20)"
        val fill1Pos = "rgba(240,162,2,.20)"
        val fillNeg = "rgba(214,51,108,.18)"
        val brdX = if (neg) PT else "#0f766e"
        val brd1 = if (neg) PT else "#b45309"
        val sb = StringBuilder()

        // ۱) نوار «مدل کاشی» در بالای بوم
        sb.append(rc(14f, 6f, 332f, 32f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
        sb.append(rc(22f, 12f, 20f, 20f, fillPos, ACC, 1.4f, 3f))
        sb.append(tx(46f, 26f, "= x²", ACC, "start", bold = true, size = 10))
        sb.append(rc(94f, 16f, 24f, 12f, if (neg) fillNeg else fillXPos, brdX, 1.4f, 2.5f))
        sb.append(tx(122f, 26f, if (neg) "= −x" else "= x", brdX, "start", bold = true, size = 10))
        sb.append(rc(174f, 16f, 12f, 12f, if (neg) fillNeg else fill1Pos, brd1, 1.4f, 2.5f))
        sb.append(tx(190f, 26f, if (neg) "= −1" else "= 1", brd1, "start", bold = true, size = 10))
        sb.append(tx(338f, 26f, "مدل کاشی", "#334155", "end", bold = true, size = 11))

        // ۲) چیدمان کاشی‌ها در میانهٔ بوم (تا ۹ کاشی x²، ۱۶ کاشی x و ۲۴ کاشی ۱)
        val areaTop = 46f
        val areaH = 186f
        val cols2 = when { x2 <= 0 -> 0; x2 <= 2 -> x2; x2 <= 4 -> 2; else -> 3 }
        val rows2 = if (cols2 == 0) 0 else (x2 + cols2 - 1) / cols2
        val s2 = when { x2 <= 2 -> 50f; x2 <= 4 -> 46f; else -> 34f }
        val g2 = 5f
        val w2 = if (x2 > 0) cols2 * s2 + (cols2 - 1) * g2 else 0f
        val h2 = if (x2 > 0) rows2 * s2 + (rows2 - 1) * g2 else 0f

        val colsX = when { xcRaw <= 0 -> 0; xcRaw <= 8 -> xcRaw; else -> 8 }
        val rowsX = if (colsX == 0) 0 else (xcRaw + colsX - 1) / colsX
        val bw = if (xcRaw <= 6) 15f else 12f
        val bh = if (rowsX <= 1) 62f else 50f
        val gx = 3.5f
        val wx = if (xcRaw > 0) colsX * bw + (colsX - 1) * gx else 0f
        val hx = if (xcRaw > 0) rowsX * bh + (rowsX - 1) * 6f else 0f

        val colsK = when { kRaw <= 0 -> 0; kRaw < 4 -> kRaw; else -> 4 }
        val rowsK = if (colsK == 0) 0 else (kRaw + colsK - 1) / colsK
        val uk = if (kRaw <= 12) 18f else 15f
        val gk = 3.5f
        val wk = if (kRaw > 0) colsK * uk + (colsK - 1) * gk else 0f
        val hk = if (kRaw > 0) rowsK * uk + (rowsK - 1) * gk else 0f

        val activeWidths = listOf(w2, wx, wk).filter { it > 0f }
        val zoneGap = if (activeWidths.size >= 3) 12f else 18f
        val totalW = activeWidths.sum() + max(0, activeWidths.size - 1) * zoneGap
        var curX = (360f - totalW) / 2f

        if (x2 > 0) {
            val yStart = areaTop + (areaH - h2) / 2f
            for (i in 0 until x2) {
                val tx0 = curX + (i % cols2) * (s2 + g2)
                val ty0 = yStart + (i / cols2) * (s2 + g2)
                sb.append(rc(tx0, ty0, s2, s2, fillPos, ACC, 1.7f, 4f))
                sb.append(tx(tx0 + s2 / 2f, ty0 + s2 / 2f + 5f, "x²", ACC, "middle", bold = true, size = if (s2 >= 44f) 14 else 11))
            }
            curX += w2
            if (xcRaw > 0 || kRaw > 0) {
                sb.append(ln(curX + zoneGap / 2f, areaTop + 14f, curX + zoneGap / 2f, areaTop + areaH - 14f, "#e2e8f0", 1.2f, "4 3"))
                curX += zoneGap
            }
        }
        if (xcRaw > 0) {
            val yStart = areaTop + (areaH - hx) / 2f
            for (j in 0 until xcRaw) {
                val tx0 = curX + (j % colsX) * (bw + gx)
                val ty0 = yStart + (j / colsX) * (bh + 6f)
                sb.append(rc(tx0, ty0, bw, bh, if (neg) fillNeg else fillXPos, brdX, 1.4f, 3f))
                sb.append(tx(tx0 + bw / 2f, ty0 + bh / 2f + 4f, if (neg) "−x" else "x", brdX, "middle", bold = true, size = if (bw >= 14f) 10 else 8))
            }
            curX += wx
            if (kRaw > 0) {
                sb.append(ln(curX + zoneGap / 2f, areaTop + 14f, curX + zoneGap / 2f, areaTop + areaH - 14f, "#e2e8f0", 1.2f, "4 3"))
                curX += zoneGap
            }
        }
        if (kRaw > 0) {
            val yStart = areaTop + (areaH - hk) / 2f
            for (m in 0 until kRaw) {
                val tx0 = curX + (m % colsK) * (uk + gk)
                val ty0 = yStart + (m / colsK) * (uk + gk)
                sb.append(rc(tx0, ty0, uk, uk, if (neg) fillNeg else fill1Pos, brd1, 1.3f, 2.5f))
                sb.append(tx(tx0 + uk / 2f, ty0 + uk / 2f + 3.5f, if (neg) "−1" else "1", brd1, "middle", bold = true, size = if (uk >= 17f) 9 else 8))
            }
        }
        if (x2 == 0 && xcRaw == 0 && kRaw == 0) {
            sb.append(tx(180f, 142f, "۰ (بدون کاشی)", MU, "middle", bold = true, size = 13))
        }

        val parts = mutableListOf<String>()
        if (x2 > 0) parts.add(if (x2 == 1) "x²" else "${x2}x²")
        if (xc != 0) {
            val ax = abs(xc)
            val term = if (ax == 1) "x" else "${ax}x"
            parts.add(if (xc < 0) (if (parts.isEmpty()) "−$term" else "− $term") else (if (parts.isEmpty()) term else "+ $term"))
        }
        if (k != 0) {
            val ak = abs(k)
            parts.add(if (k < 0) (if (parts.isEmpty()) "−$ak" else "− $ak") else (if (parts.isEmpty()) "$ak" else "+ $ak"))
        }
        val expr = if (parts.isEmpty()) "0" else parts.joinToString(" ")
        if (show != "0") {
            val askExpr = show == "2"
            sb.append(rc(24f, 240f, 312f, 32f, if (askExpr) "#fffbeb" else "#eef2ff", if (askExpr) GLD else ACC, 1.5f, 8f))
            sb.append(tx(180f, 261f, if (askExpr) "عبارت جبری = ؟" else "عبارت جبری:  $expr", if (askExpr) "#92400e" else "#1e1b4b", "middle", bold = true, size = 13))
        }
        return sb.toString()
    }

    private fun defaultKenKenCages(n: Int): String = when (n) {
        3 -> "11,12|5|+;13,23|6|×;21,31|2|−;22,32,33|6|+"
        5 -> "11,12|7|+;13,14,15|12|+;21,31|2|÷;22,23|12|×;24,34|3|−;25,35|8|+;32,33,42|10|+;41,51|4|−;43,44|15|×;45,54,55|9|+;52,53|5|+"
        6 -> "11,12|11|+;13,14|2|÷;15,16,26|14|+;21,31|3|−;22,23|12|×;24,25|7|+;32,33|2|−;34,44|18|×;35,36|9|+;41,42,51|12|+;43,53|6|×;45,46|3|÷;52,61,62|11|+;54,55|9|+;56,65,66|13|+;63,64|2|−"
        else -> "11,12|6|+;13,14|7|+;21,22|3|×;23,24,31,32|10|+;33,34|4|×;41,42,43,44|10|+"
    }

    private fun kenKen(spec: FigureSpec): String = kenKenRender(spec, null)

    private fun kenKenRender(spec: FigureSpec, regions: MutableList<InteractiveRegion>?): String {
        val kind = spec.xStr("kind", "kenken").lowercase()
        val n = spec.xNum("size", 4f).toInt().coerceIn(3, 6)
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
        val cell = min(46f, 204f / n)
        val x0 = 180f - cell * n / 2f
        val y0 = 26f
        val sb = StringBuilder()

        if (kind == "kakuro") {
            val hSums = mapOf((1 to 0) to 7, (2 to 0) to 6, (3 to 0) to 8, (4 to 0) to 9, (5 to 0) to 11)
            val vSums = mapOf((0 to 1) to 6, (0 to 2) to 8, (0 to 3) to 7, (0 to 4) to 9, (0 to 5) to 10)
            for (r in 0 until n) for (c in 0 until n) {
                val cx = x0 + c * cell
                val cy = y0 + r * cell
                val isClue = r == 0 || c == 0 || (n >= 5 && r == 2 && c == 2)
                if (isClue) {
                    sb.append(rc(cx, cy, cell, cell, "#1e293b", "#0f172a", 1.4f))
                    sb.append(ln(cx, cy, cx + cell, cy + cell, "#64748b", 1.1f))
                    val hs = if (r > 0 && c == 0) hSums[r to 0] ?: (r + 5) else if (r == 2 && c == 2) 7 else null
                    val vs = if (r == 0 && c > 0) vSums[0 to c] ?: (c + 5) else if (r == 2 && c == 2) 6 else null
                    if (hs != null) sb.append(tx(cx + cell * 0.74f, cy + cell * 0.36f, hs.toString(), "#f8fafc", "middle", bold = true, size = 9))
                    if (vs != null) sb.append(tx(cx + cell * 0.28f, cy + cell * 0.82f, vs.toString(), "#fbbf24", "middle", bold = true, size = 9))
                } else {
                    val key = "k_${r + 1}_${c + 1}"
                    val isExplicitEmpty = key in emptySet
                    val hasGiven = ovMap[key]?.isNotBlank() == true
                    regions?.add(InteractiveRegion(key, "خانهٔ سطر ${pFa(r + 1)} ستون ${pFa(c + 1)}", "", cx, cy, cell, cell, defaultEmpty = isExplicitEmpty || (emptySet.isEmpty() && !hasGiven)))
                    sb.append(rc(cx, cy, cell, cell, if (isExplicitEmpty) "#eef2ff" else "#ffffff", if (isExplicitEmpty) "#4f46e5" else "#cbd5e1", if (isExplicitEmpty) 1.8f else 1.1f))
                    if (isExplicitEmpty) sb.append(rcDash(cx + 3f, cy + 3f, cell - 6f, cell - 6f, "none", "#4f46e5", 1.3f, 3f, "4 2.5"))
                    val stu = ansMap[key]?.trim().orEmpty()
                    val giv = ovMap[key]?.trim().orEmpty()
                    val disp = when {
                        isExplicitEmpty -> stu
                        giv.isNotEmpty() -> giv
                        else -> stu
                    }
                    if (disp.isNotEmpty()) {
                        val col = if (!isExplicitEmpty && giv.isNotEmpty()) ST else "#1d4ed8"
                        sb.append(tx(cx + cell / 2f, cy + cell * 0.64f, disp, col, "middle", bold = true, size = 14))
                    }
                }
            }
            sb.append(rc(x0, y0, cell * n, cell * n, "none", ST, 2.4f))
            sb.append(tx(180f, y0 + cell * n + 24f, "کاکورو ${n}×${n} — جمع اعداد غیرتکراری ۱ تا ۹ در هر ردیف و ستون", MU, "middle", bold = true, size = 10))
            return sb.toString()
        }

        val default4 = defaultKenKenCages(4)
        val rawCages = spec.xStr("cages", "").trim()
        val cagesStr = if (rawCages.isEmpty() || (n != 4 && rawCages == default4)) defaultKenKenCages(n) else rawCages
        val cages = cagesStr.split(Regex("[;؛]")).filter { it.isNotBlank() }.map { cg ->
            val p = cg.split("|")
            val cells = p[0].split(Regex("[,،]")).mapNotNull { cd ->
                val s = faEnDigits(cd.trim())
                if (s.length < 2) null else {
                    val rr = (s[0] - '1'); val cc = (s[1] - '1')
                    if (rr in 0 until n && cc in 0 until n) rr to cc else null
                }
            }
            Triple(cells, p.getOrElse(1) { "" }, p.getOrElse(2) { "" })
        }.filter { it.first.isNotEmpty() }

        val owner = Array(n) { IntArray(n) { -1 } }
        cages.forEachIndexed { idx, c -> c.first.forEach { (r, col) -> owner[r][col] = idx } }
        val cageTints = listOf("#ffffff", "#f8fafc", "#fdf4ff", "#f0fdf4", "#fffbeb", "#eff6ff")

        for (r in 0 until n) for (c in 0 until n) {
            val cx = x0 + c * cell
            val cy = y0 + r * cell
            val key = "k_${r + 1}_${c + 1}"
            val isExplicitEmpty = key in emptySet
            val giv = ovMap[key]?.trim().orEmpty()
            val stu = ansMap[key]?.trim().orEmpty()
            regions?.add(InteractiveRegion(key, "خانهٔ (${pFa(r + 1)}،${pFa(c + 1)})", "", cx, cy, cell, cell, defaultEmpty = isExplicitEmpty || (emptySet.isEmpty() && giv.isEmpty())))
            val bg = if (isExplicitEmpty) "#eef2ff" else cageTints[(owner[r][c].coerceAtLeast(0)) % cageTints.size]
            sb.append(rc(cx, cy, cell, cell, bg, "#cbd5e1", 0.9f))
            if (isExplicitEmpty) sb.append(rcDash(cx + 3f, cy + 3f, cell - 6f, cell - 6f, "none", "#4f46e5", 1.4f, 3f, "4 2.5"))
            val disp = when {
                isExplicitEmpty -> stu
                giv.isNotEmpty() -> giv
                else -> stu
            }
            if (disp.isNotEmpty()) {
                val col = if (!isExplicitEmpty && giv.isNotEmpty()) ST else "#1d4ed8"
                sb.append(tx(cx + cell / 2f, cy + cell * 0.72f, disp, col, "middle", bold = true, size = 14))
            }
        }
        sb.append(rc(x0, y0, cell * n, cell * n, "none", ST, 2.8f))
        for (r in 0 until n) for (c in 0 until n) {
            if (c < n - 1 && owner[r][c] != owner[r][c + 1]) sb.append(ln(x0 + (c + 1) * cell, y0 + r * cell, x0 + (c + 1) * cell, y0 + (r + 1) * cell, ST, 2.4f))
            if (r < n - 1 && owner[r][c] != owner[r + 1][c]) sb.append(ln(x0 + c * cell, y0 + (r + 1) * cell, x0 + (c + 1) * cell, y0 + (r + 1) * cell, ST, 2.4f))
        }
        cages.forEach { cg ->
            val top = cg.first.sortedWith(compareBy({ it.first }, { it.second })).first()
            sb.append(tx(x0 + top.second * cell + 4f, y0 + top.first * cell + 12f, cg.second + cg.third, ACC, "start", bold = true, size = if (n >= 6) 9 else 10))
        }
        sb.append(tx(180f, y0 + cell * n + 24f, "کن‌کن ${n}×${n} — اعداد ۱ تا $n بدون تکرار در سطر و ستون", MU, "middle", bold = true, size = 10))
        return sb.toString()
    }

    private fun diceFold(spec: FigureSpec): String = diceFoldRender(spec, null)

    private fun diceFoldRender(spec: FigureSpec, regions: MutableList<InteractiveRegion>?): String {
        val mode = spec.xStr("mode", "dots").lowercase()
        val net = spec.xStr("net", "cross").lowercase()
        val top = spec.xNum("top", 1f).toInt().coerceIn(1, 6)
        val ask = spec.xStr("ask", "0") == "1"
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")

        val opp = 7 - top
        val rem = (1..6).filter { it != top && it != opp }
        val defaults = listOf(top, rem[0], rem[1], 7 - rem[0], opp, 7 - rem[1]).map { it.toString() }
        val faceNames = listOf("وجه بالا", "وجه چپ", "وجه روبه‌رو", "وجه راست", "وجه پایین", "وجه پشت")
        val coords = when (net) {
            "t" -> listOf(1 to 0, 0 to 0, 1 to 1, 2 to 0, 1 to 2, 3 to 0)
            "stairs" -> listOf(1 to 0, 0 to 0, 1 to 1, 2 to 1, 2 to 2, 3 to 2)
            "l" -> listOf(0 to 0, 3 to 1, 0 to 1, 1 to 1, 2 to 2, 2 to 1)
            else -> listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1, 1 to 2, 3 to 1)
        }
        val s = 54f
        val x0 = 180f - s * 2f
        val y0 = 32f
        val pips = mapOf(
            1 to listOf(0.5f to 0.5f),
            2 to listOf(0.26f to 0.26f, 0.74f to 0.74f),
            3 to listOf(0.25f to 0.25f, 0.5f to 0.5f, 0.75f to 0.75f),
            4 to listOf(0.26f to 0.26f, 0.74f to 0.26f, 0.26f to 0.74f, 0.74f to 0.74f),
            5 to listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.5f to 0.5f, 0.25f to 0.75f, 0.75f to 0.75f),
            6 to listOf(0.26f to 0.22f, 0.74f to 0.22f, 0.26f to 0.5f, 0.74f to 0.5f, 0.26f to 0.78f, 0.74f to 0.78f)
        )
        val faceTints = listOf("#eef2ff", "#ecfdf5", "#ffffff", "#ecfdf5", "#eef2ff", "#fffbeb")
        val sb = StringBuilder()

        coords.forEachIndexed { idx, (col, row) ->
            val fx = x0 + col * s
            val fy = y0 + row * s
            val key = "f$idx"
            val autoEmpty = ask && (idx == 4 || idx == 5)
            val isEmpty = key in emptySet || autoEmpty
            val defVal = defaults[idx]
            regions?.add(InteractiveRegion(key, faceNames[idx], defVal, fx, fy, s, s, defaultEmpty = autoEmpty))

            val fill = if (isEmpty) "#ffffff" else faceTints[idx]
            sb.append(rc(fx, fy, s, s, fill, ST, 2f, 5f))
            if (isEmpty) {
                sb.append(rcDash(fx + 5f, fy + 5f, s - 10f, s - 10f, "#f8fafc", "#4f46e5", 1.5f, 4f, "4 2.5"))
            }
            val disp = if (isEmpty) ansMap[key]?.trim().orEmpty() else (ovMap[key]?.trim() ?: defVal)
            if (disp.isNotEmpty()) {
                val asPip = faEnDigits(disp).toIntOrNull()
                val drawColor = if (isEmpty) "#1d4ed8" else ST
                if (mode == "dots" && asPip != null && asPip in 1..6) {
                    pips[asPip]?.forEach { (px, py) ->
                        sb.append(cls(fx + px * s, fy + py * s, 4.3f, drawColor, "none", 0f))
                    }
                } else {
                    sb.append(tx(fx + s / 2f, fy + s / 2f + 6f, disp, drawColor, "middle", bold = true, size = 18))
                }
            }
        }
        sb.append(tx(180f, y0 + 3 * s + 28f, "گستردهٔ استاندارد تاس (مجموع هر دو وجه مقابل = ۷)", MU, "middle", bold = true, size = 11))
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
        val showLabel = spec.xStr("label", "1") != "0"
        // V322 — مقدار خوانده‌شده بزرگ‌تر از «حداکثر» عقربه/درجه را بیرون از خط‌کش می‌برد؛ حالا مهار می‌شود.
        val mx = max(10f, spec.xNum("max", 50f)).coerceAtMost(999f)
        val v = max(0f, spec.xNum("val", 23.4f)).coerceAtMost(mx)
        val sb = StringBuilder()

        if (kind == "micrometer") {
            val vMic = v.coerceIn(0f, min(mx, 25f))
            val halfTurns = kotlin.math.floor(vMic * 2.0 + 1e-4).toInt()
            val mainMm = halfTurns * 0.5f
            val thimbleInt = ((vMic - mainMm) * 100f).let { kotlin.math.round(it).toInt().coerceIn(0, 49) }

            sb.append(path("M 42 132 C 42 204, 132 204, 142 132", "none", "#475569", 10f))
            sb.append(rc(36f, 92f, 12f, 44f, "#334155", ST, 1.5f, 2f))
            val objW = (14f + vMic * 2.2f).coerceIn(14f, 68f)
            sb.append(rc(48f, 98f, objW, 32f, "rgba(240,162,2,.28)", "#b45309", 1.6f, 3f))
            sb.append(rc(48f + objW, 96f, (136f - (48f + objW)).coerceAtLeast(8f), 36f, "#cbd5e1", ST, 1.5f))

            val slX = 136f
            val mmPx = 4.2f
            val slW = (vMic * mmPx + 14f).coerceIn(28f, 126f)
            sb.append(rc(slX, 82f, slW, 64f, "#f1f5f9", ST, 1.8f, 3f))
            val datumY = 114f
            sb.append(ln(slX + 4f, datumY, slX + slW, datumY, ST, 1.8f))
            val maxVisibleMm = kotlin.math.floor(vMic.toDouble() + 1e-4).toInt()
            for (m in 0..maxVisibleMm) {
                val txm = slX + 8f + m * mmPx
                if (txm <= slX + slW - 2f) {
                    sb.append(ln(txm, datumY, txm, datumY - (if (m % 5 == 0) 15f else 9f), ST, if (m % 5 == 0) 1.5f else 1.1f))
                    if (m % 5 == 0) sb.append(tx(txm, datumY - 18f, m.toString(), MU, "middle", bold = true, size = 9))
                }
                val hxm = txm + 0.5f * mmPx
                if (m + 0.5f <= vMic + 1e-3f && hxm <= slX + slW - 2f) {
                    sb.append(ln(hxm, datumY, hxm, datumY + 9f, PT, 1.2f))
                }
            }

            val thX = slX + slW
            sb.append(poly("${f(thX)},82 ${f(thX + 18f)},72 ${f(thX + 18f)},156 ${f(thX)},146", "#e2e8f0", ST, 1.8f))
            sb.append(rc(thX + 18f, 72f, 54f, 84f, "rgba(39,196,168,.16)", ST, 1.8f, 4f))
            sb.append(rc(thX + 72f, 88f, 16f, 52f, "#94a3b8", ST, 1.5f, 3f))
            for (d in -4..4) {
                val tickVal = (thimbleInt + d + 50) % 50
                val ty = datumY - d * 7.5f
                val isZeroHit = d == 0
                val tickLen = if (tickVal % 5 == 0) 14f else 8f
                sb.append(ln(thX + 2f, ty, thX + 2f + tickLen, ty, if (isZeroHit) PT else ST, if (isZeroHit) 2.2f else 1.1f))
                if (tickVal % 5 == 0 || isZeroHit) {
                    sb.append(tx(thX + 22f + (if (isZeroHit) 12f else 4f), ty + 3.5f, tickVal.toString(), if (isZeroHit) PT else MU, "start", bold = isZeroHit, size = if (isZeroHit) 10 else 8))
                }
            }
            val readStr = String.format(java.util.Locale.US, "%.2f", mainMm + thimbleInt * 0.01f)
            sb.append(rc(34f, 232f, 292f, 32f, if (showLabel) "#eef2ff" else "#fffbeb", if (showLabel) ACC else GLD, 1.5f, 8f))
            sb.append(tx(180f, 253f, if (showLabel) "ریزسنج (دقت ۰٫۰۱mm):  $readStr mm" else "عدد ریزسنج را بخوانید (دقت ۰٫۰۱ میلی‌متر)", if (showLabel) "#1e1b4b" else "#92400e", "middle", bold = true, size = 12))
            return sb.toString()
        }

        val vRound = kotlin.math.round(v * 10f) / 10f
        val mainInt = kotlin.math.floor(vRound.toDouble() + 1e-4).toInt()
        val tenth = kotlin.math.round((vRound - mainInt) * 10f).toInt().coerceIn(0, 9)

        val xStart = 44f
        val beamW = 286f
        val pxPerMm = beamW / mx
        val jawX = xStart + vRound * pxPerMm

        sb.append(poly("${f(xStart - 14f)},56 ${f(xStart)},56 ${f(xStart)},186 ${f(xStart - 8f)},186 ${f(xStart - 14f)},166", "#cbd5e1", ST, 1.8f))
        if (jawX - xStart > 6f) {
            sb.append(rc(xStart, 146f, jawX - xStart, 28f, "rgba(240,162,2,.26)", "#b45309", 1.5f, 3f))
        }
        sb.append(rc(xStart, 64f, beamW, 38f, "#f8fafc", ST, 1.8f, 3f))

        val tickStep = if (mx > 80f) 2 else 1
        var m = 0
        while (m <= mx.toInt()) {
            val txm = xStart + m * pxPerMm
            val hTick = when { m % 10 == 0 -> 18f; m % 5 == 0 -> 13f; else -> 8f }
            sb.append(ln(txm, 102f, txm, 102f - hTick, ST, if (m % 10 == 0) 1.5f else 1f))
            if (m % 10 == 0) sb.append(tx(txm, 78f, (m / 10).toString(), ST, "middle", bold = true, size = 10))
            m += tickStep
        }
        sb.append(tx(xStart + beamW - 6f, 76f, "cm", MU, "end", bold = true, size = 9))

        val verSpan = max(44f, 9f * pxPerMm)
        val verStep = verSpan / 10f
        val verW = (verSpan + 16f).coerceAtMost(346f - jawX + 20f).coerceAtLeast(52f)
        sb.append(poly("${f(jawX)},102 ${f(jawX + 14f)},102 ${f(jawX + 14f)},186 ${f(jawX + 5f)},186 ${f(jawX)},166", "#bae6fd", ST, 1.8f))
        sb.append(rc(jawX, 102f, verW, 34f, "rgba(39,196,168,.18)", ST, 1.8f, 3f))
        for (j in 0..10) {
            val vx = jawX + j * verStep
            if (vx <= 344f) {
                val hit = j == tenth
                sb.append(ln(vx, 102f, vx, if (hit || j == 0 || j == 10) 118f else 112f, if (hit) PT else ACC, if (hit) 2.2f else 1.2f))
                if (j == 0 || j == 5 || j == 10 || hit) {
                    sb.append(tx(vx, 130f, j.toString(), if (hit) PT else ACC, "middle", bold = hit, size = if (hit) 10 else 8))
                }
            }
        }

        val readText = String.format(java.util.Locale.US, "%.1f", vRound)
        sb.append(rc(28f, 234f, 304f, 32f, if (showLabel) "#eef2ff" else "#fffbeb", if (showLabel) ACC else GLD, 1.5f, 8f))
        sb.append(tx(180f, 255f, if (showLabel) "کولیس ورنیه (دقت ۰٫۱mm):  $readText mm" else "عدد کولیس ورنیه را بخوانید (دقت ۰٫۱ میلی‌متر)", if (showLabel) "#1e1b4b" else "#92400e", "middle", bold = true, size = 12))
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

    private fun exactRadLabel(deg: Float): String {
        val d = ((kotlin.math.round(deg).toInt() % 360) + 360) % 360
        val base = when (d) {
            0 -> "0"
            30 -> "π/6"; 45 -> "π/4"; 60 -> "π/3"; 90 -> "π/2"
            120 -> "2π/3"; 135 -> "3π/4"; 150 -> "5π/6"; 180 -> "π"
            210 -> "7π/6"; 225 -> "5π/4"; 240 -> "4π/3"; 270 -> "3π/2"
            300 -> "5π/3"; 315 -> "7π/4"; 330 -> "11π/6"
            else -> ""
        }
        if (deg < 0f && d != 0 && base.isNotEmpty() && deg >= -360f) {
            val absD = kotlin.math.round(abs(deg)).toInt()
            val negMap = mapOf(30 to "−π/6", 45 to "−π/4", 60 to "−π/3", 90 to "−π/2", 120 to "−2π/3", 135 to "−3π/4", 150 to "−5π/6", 180 to "−π", 270 to "−3π/2", 360 to "−2π")
            return negMap[absD] ?: ""
        }
        if (abs(deg - 360f) < 0.5f) return "2π"
        return base
    }

    private fun trigCircle(spec: FigureSpec): String {
        val deg = spec.xNum("deg", 55f).coerceIn(-720f, 720f)
        val showMode = spec.xStr("show", "1").lowercase()
        val showProj = showMode != "0"
        val showTan = showMode == "tan"
        val cx = 172f
        val cy = 132f
        val r = 84f
        val rad = deg * PI / 180.0
        val px = cx + r * cos(rad).toFloat()
        val py = cy - r * sin(rad).toFloat()
        val sb = StringBuilder()

        sb.append(cls(cx, cy, r, "rgba(227,242,253,.45)", "#1565c0", 1.9f))
        sb.append(ln(cx - r - 24f, cy, cx + r + 28f, cy, ST, 1.5f))
        sb.append(ln(cx, cy - r - 20f, cx, cy + r + 20f, ST, 1.5f))
        sb.append(tx(cx + r + 18f, cy - 8f, "0°", MU, "start", bold = true, size = 9))
        sb.append(tx(cx + 12f, cy - r - 9f, "90°", MU, "start", bold = true, size = 9))
        sb.append(tx(cx - r - 10f, cy - 8f, "180°", MU, "end", bold = true, size = 9))
        sb.append(tx(cx + 12f, cy + r + 15f, "270°", MU, "start", bold = true, size = 9))

        val absDeg = abs(deg)
        val rA = 26f
        if (absDeg >= 359.5f) {
            sb.append(cls(cx, cy, rA, "rgba(245,158,11,.15)", GLD, 2f))
            val rem = deg % 360f
            if (abs(rem) >= 1f) {
                val rA2 = 33f
                val remRad = rem * PI / 180.0
                val ex2 = cx + rA2 * cos(remRad).toFloat()
                val ey2 = cy - rA2 * sin(remRad).toFloat()
                val la2 = if (abs(rem) > 180f) 1 else 0
                val sw2 = if (rem >= 0f) 0 else 1
                sb.append(path("M ${f(cx + rA2)} ${f(cy)} A ${f(rA2)} ${f(rA2)} 0 $la2 $sw2 ${f(ex2)} ${f(ey2)}", "none", "#d97706", 2f))
            }
        } else if (absDeg >= 0.5f) {
            val ex = cx + rA * cos(rad).toFloat()
            val ey = cy - rA * sin(rad).toFloat()
            val largeArc = if (absDeg > 180f) 1 else 0
            val sweep = if (deg >= 0f) 0 else 1
            sb.append(path("M ${f(cx)} ${f(cy)} L ${f(cx + rA)} ${f(cy)} A ${f(rA)} ${f(rA)} 0 $largeArc $sweep ${f(ex)} ${f(ey)} Z", "rgba(245,158,11,.18)", "none", 0f))
            sb.append(path("M ${f(cx + rA)} ${f(cy)} A ${f(rA)} ${f(rA)} 0 $largeArc $sweep ${f(ex)} ${f(ey)}", "none", GLD, 2.2f))
            val dirSign = if (deg >= 0f) 1.0 else -1.0
            val txVec = (-sin(rad) * dirSign).toFloat()
            val tyVec = (-cos(rad) * dirSign).toFloat()
            val ahAngle = atan2(tyVec, txVec)
            val ahPts = "${f(ex)},${f(ey)} ${f(ex - 6.5f * cos(ahAngle - 0.5f))},${f(ey - 6.5f * sin(ahAngle - 0.5f))} ${f(ex - 6.5f * cos(ahAngle + 0.5f))},${f(ey - 6.5f * sin(ahAngle + 0.5f))}"
            sb.append(poly(ahPts, "#d97706", "none", 0f))
        }

        if (showTan && abs(cos(rad)) > 0.12) {
            val tanVal = kotlin.math.tan(rad).toFloat().coerceIn(-1.25f, 1.25f)
            val txLine = cx + r
            val tyHit = cy - r * tanVal
            sb.append(ln(txLine, cy - r - 12f, txLine, cy + r + 12f, "#9333ea", 1.3f, "4 3"))
            sb.append(ln(cx, cy, txLine, tyHit, "#9333ea", 1.5f, "3 3"))
            sb.append(cls(txLine, tyHit, 4f, "#9333ea", "#ffffff", 1.2f))
        }

        if (showProj) {
            sb.append(ln(px, py, px, cy, GRN, 1.7f, "4 3"))
            sb.append(ln(px, py, cx, py, ACC, 1.7f, "4 3"))
            sb.append(ln(cx, cy, px, cy, "#0d9488", 2.8f))
            sb.append(ln(cx, cy, cx, py, ACC, 2.8f))
        }

        sb.append(ln(cx, cy, px, py, PT, 2.5f))
        sb.append(cls(px, py, 5f, PT, "#ffffff", 1.5f))

        val radLbl = exactRadLabel(deg)
        val degPart = if (radLbl.isNotEmpty()) "${num(deg)}° ($radLbl)" else "${num(deg)}°"
        val info = if (showProj) {
            val cStr = String.format(java.util.Locale.US, "%.2f", cos(rad))
            val sStr = String.format(java.util.Locale.US, "%.2f", sin(rad))
            "θ = $degPart  |  cos = $cStr ، sin = $sStr"
        } else {
            "زاویهٔ جهت‌دار:  θ = $degPart"
        }
        sb.append(rc(18f, 246f, 324f, 26f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
        sb.append(tx(180f, 263f, info, "#1e293b", "middle", bold = true, size = 11))
        return sb.toString()
    }

    private fun mappingDiagram(spec: FigureSpec): String {
        val dom = spec.xList("from", "1,2,3").take(6).ifEmpty { listOf("1", "2", "3") }
        val cod = spec.xList("to", "a,b,c").take(6).ifEmpty { listOf("a", "b", "c") }
        val show = spec.xStr("show", "all").lowercase()
        val pairs = spec.xStr("arrows", "1-a;2-a;3-b").split(Regex("[;؛]")).mapNotNull { pr ->
            val p = pr.split(Regex("[-→>:]")).map { it.trim() }
            if (p.size >= 2 && p[0].isNotEmpty() && p[1].isNotEmpty()) p[0] to p[1] else null
        }

        val sb = StringBuilder()
        val boxTop = 34f
        val boxH = 186f
        sb.append(tx(92f, 25f, "مجموعهٔ A (دامنه)", ACC, "middle", bold = true, size = 11))
        sb.append(tx(268f, 25f, "مجموعهٔ B (هم‌دامنه)", "#0f766e", "middle", bold = true, size = 11))
        sb.append(rc(36f, boxTop, 112f, boxH, "rgba(108,99,245,.10)", ACC, 1.8f, 52f))
        sb.append(rc(212f, boxTop, 112f, boxH, "rgba(39,196,168,.10)", GRN, 1.8f, 52f))

        val domStep = (boxH - 28f) / (dom.size + 1)
        val codStep = (boxH - 28f) / (cod.size + 1)
        val domDotX = 122f
        val codDotX = 238f
        val domY = dom.indices.map { boxTop + 14f + (it + 1) * domStep }
        val codY = cod.indices.map { boxTop + 14f + (it + 1) * codStep }

        pairs.forEach { (fromItem, toItem) ->
            val i = dom.indexOf(fromItem)
            val j = cod.indexOf(toItem)
            if (i >= 0 && j >= 0) {
                sb.append(arrow(domDotX + 6f, domY[i], codDotX - 6f, codY[j], PT, 1.9f, 7.5f))
            }
        }
        dom.forEachIndexed { i, d ->
            sb.append(cls(domDotX, domY[i], 4.8f, ACC, "#ffffff", 1.3f))
            sb.append(tx(domDotX - 11f, domY[i] + 4f, d, ST, "end", bold = true, size = 13))
        }
        cod.forEachIndexed { j, c ->
            sb.append(cls(codDotX, codY[j], 4.8f, "#0f766e", "#ffffff", 1.3f))
            sb.append(tx(codDotX + 11f, codY[j] + 4f, c, ST, "start", bold = true, size = 13))
        }

        val validPairs = pairs.filter { it.first in dom && it.second in cod }.distinct()
        val outDeg = dom.associateWith { d -> validPairs.count { it.first == d } }
        val inDeg = cod.associateWith { c -> validPairs.count { it.second == c } }
        val hasMultiOut = outDeg.values.any { it > 1 }
        val hasZeroOut = outDeg.values.any { it == 0 }
        val isFunc = !hasMultiOut && !hasZeroOut
        val statusText = when {
            hasMultiOut -> "رابطه تابع نیست (یک عضو دامنه بیش از یک خروجی دارد)"
            hasZeroOut -> "رابطه تابع نیست (عضو بدون خروجی در دامنه)"
            else -> {
                val inj = inDeg.values.all { it <= 1 }
                val surj = inDeg.values.all { it >= 1 }
                val usedCod = inDeg.filterValues { it > 0 }.keys
                when {
                    inj && surj -> "تابع دوسویی (یک‌به‌یک و پوشا)"
                    inj -> "تابع یک‌به‌یک (غیرپوشا) — برد: {${usedCod.joinToString("، ")}}"
                    surj -> "تابع پوشا (غیر یک‌به‌یک)"
                    usedCod.size == 1 && dom.size > 1 -> "تابع ثابت — برد: {${usedCod.first()}}"
                    else -> "تابع (نه یک‌به‌یک و نه پوشا) — برد: {${usedCod.joinToString("، ")}}"
                }
            }
        }
        if (show != "none") {
            sb.append(rc(16f, 234f, 328f, 32f, if (isFunc) "#ecfdf5" else "#fef2f2", if (isFunc) "#10b981" else PT, 1.5f, 8f))
            sb.append(tx(180f, 255f, statusText, if (isFunc) "#065f46" else "#9f1239", "middle", bold = true, size = 11))
        }
        return sb.toString()
    }

    private fun probTreeStageLists(spec: FigureSpec): List<List<String>> {
        val s1 = spec.xList("first", "شیر,خط").ifEmpty { listOf("شیر", "خط") }
        val s2 = spec.xList("second", "شیر,خط").ifEmpty { listOf("شیر", "خط") }
        val s3 = spec.xList("third", "")
        val s4 = spec.xList("fourth", "")
        val explicit = spec.xNum("stages", 0f).toInt()
        val nStages = when {
            explicit in 1..4 -> explicit
            s4.isNotEmpty() -> 4
            s3.isNotEmpty() -> 3
            else -> 2
        }
        return when (nStages) {
            1 -> listOf(s1.take(4))
            2 -> listOf(s1.take(3), s2.take(3))
            3 -> listOf(s1.take(3), s2.take(2), s3.ifEmpty { listOf("شیر", "خط") }.take(2))
            else -> listOf(s1.take(2), s2.take(2), s3.ifEmpty { listOf("شیر", "خط") }.take(2), s4.ifEmpty { listOf("شیر", "خط") }.take(2))
        }
    }

    private fun probabilityTree(spec: FigureSpec): String = probabilityTreeRender(spec, null)

    private fun probabilityTreeRender(spec: FigureSpec, regions: MutableList<InteractiveRegion>?): String {
        val stages = probTreeStageLists(spec)
        val nSt = stages.size
        val mode = spec.xStr("mode", "prob").lowercase()
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")

        val vw = canvasWidth(spec)
        val vh = canvasHeight(spec)
        val totalLeaves = stages.fold(1) { acc, l -> acc * l.size }

        val yTop = 34f
        val yBot = vh - 16f
        val leafY = FloatArray(totalLeaves) { i -> yTop + (i + 0.5f) * (yBot - yTop) / totalLeaves }

        val stageNodeCounts = IntArray(nSt)
        var prod = 1
        for (s in 0 until nSt) {
            prod *= stages[s].size
            stageNodeCounts[s] = prod
        }

        val nodeY = Array(nSt) { s -> FloatArray(stageNodeCounts[s]) }
        for (i in 0 until totalLeaves) nodeY[nSt - 1][i] = leafY[i]
        for (s in nSt - 2 downTo 0) {
            val childCount = stages[s + 1].size
            for (idx in 0 until stageNodeCounts[s]) {
                var sum = 0f
                for (c in 0 until childCount) sum += nodeY[s + 1][idx * childCount + c]
                nodeY[s][idx] = sum / childCount
            }
        }

        val rootX = 20f
        val rootY = nodeY[0].average().toFloat()
        val outColW = if (mode == "none") 0f else if (nSt >= 3) 68f else 64f
        val usableW = vw - rootX - outColW - 24f
        val colX = FloatArray(nSt) { s -> rootX + (s + 1f) * (usableW / nSt) - (usableW / nSt) * 0.22f }
        val nw = when { nSt >= 4 -> 42f; nSt == 3 -> 48f; else -> 54f }
        val nh = min(22f, (yBot - yTop) / totalLeaves - 2.5f).coerceAtLeast(15f)
        val fontN = when { nh < 17f -> 9; nSt >= 3 -> 10; else -> 11 }

        val sb = StringBuilder()
        for (s in 0 until nSt) {
            sb.append(tx(colX[s], 21f, "مرحلهٔ ${pFa(s + 1)}", MU, "middle", bold = true, size = 10, canvasW = vw))
        }
        if (mode != "none") {
            sb.append(tx(vw - outColW / 2f - 8f, 21f, if (mode == "count") "پیشامد" else "احتمال", MU, "middle", bold = true, size = 10, canvasW = vw))
        }

        sb.append(cls(rootX, rootY, 5.5f, ACC, "#ffffff", 1.5f))

        val stageColors = listOf(ACC, "#0d9488", "#d97706", PT)
        val stageFills = listOf("#eef2ff", "#ecfdf5", "#fffbeb", "#fdf2f8")

        for (s in 0 until nSt) {
            val k = stages[s].size
            for (idx in 0 until stageNodeCounts[s]) {
                val px = if (s == 0) rootX + 5.5f else colX[s - 1] + nw / 2f
                val py = if (s == 0) rootY else nodeY[s - 1][idx / k]
                val cx = colX[s] - nw / 2f
                val cy = nodeY[s][idx]
                sb.append(arrow(px, py, cx - 1.5f, cy, stageColors[s % stageColors.size], 1.6f, 6.5f))
                if (nSt <= 2 && mode == "prob") {
                    val mx = px + (cx - px) * 0.46f
                    val my = py + (cy - py) * 0.46f
                    val eKey = "e_${s + 1}_$idx"
                    val eDef = "1/$k"
                    val eEmpty = eKey in emptySet
                    regions?.add(InteractiveRegion(eKey, "احتمال شاخهٔ مرحلهٔ ${pFa(s + 1)}", eDef, mx - 14f, my - 8f, 28f, 16f))
                    sb.append(rc(mx - 13f, my - 7.5f, 26f, 15f, "#ffffff", if (eEmpty) "#4f46e5" else "#cbd5e1", if (eEmpty) 1.4f else 0.9f, 4f))
                    if (eEmpty) sb.append(rcDash(mx - 12f, my - 6.5f, 24f, 13f, "none", "#4f46e5", 1.2f, 3f, "3 2"))
                    val eDisp = if (eEmpty) ansMap[eKey]?.trim().orEmpty() else (ovMap[eKey]?.trim() ?: eDef)
                    if (eDisp.isNotEmpty()) {
                        sb.append(tx(mx, my + 3.5f, eDisp, if (eEmpty) "#1d4ed8" else MU, "middle", bold = true, size = 8, canvasW = vw))
                    }
                }
            }
        }

        for (s in 0 until nSt) {
            val opts = stages[s]
            val k = opts.size
            for (idx in 0 until stageNodeCounts[s]) {
                val rawOpt = opts[idx % k]
                val cleanOpt = rawOpt.substringBefore(':').trim().ifEmpty { rawOpt }
                val nKey = "n_${s + 1}_$idx"
                val isEmpty = nKey in emptySet
                val bx = colX[s] - nw / 2f
                val by = nodeY[s][idx] - nh / 2f
                regions?.add(InteractiveRegion(nKey, "گره مرحلهٔ ${pFa(s + 1)} شاخهٔ ${pFa(idx + 1)}", cleanOpt, bx, by, nw, nh))
                sb.append(rc(bx, by, nw, nh, if (isEmpty) "#ffffff" else stageFills[s % stageFills.size], if (isEmpty) "#4f46e5" else stageColors[s % stageColors.size], if (isEmpty) 1.6f else 1.4f, 5f))
                if (isEmpty) sb.append(rcDash(bx + 2.5f, by + 2.5f, nw - 5f, nh - 5f, "none", "#4f46e5", 1.3f, 4f, "4 2"))
                val disp = if (isEmpty) ansMap[nKey]?.trim().orEmpty() else (ovMap[nKey]?.trim() ?: cleanOpt)
                if (disp.isNotEmpty()) {
                    sb.append(tx(colX[s], nodeY[s][idx] + fontN * 0.35f, disp, if (isEmpty) "#1d4ed8" else ST, "middle", bold = true, size = fontN, canvasW = vw))
                }
            }
        }

        if (mode != "none") {
            val rx = vw - outColW - 6f
            val rw = outColW - 4f
            for (leaf in 0 until totalLeaves) {
                val rKey = "r_$leaf"
                val isEmpty = rKey in emptySet
                val pathParts = ArrayList<String>(nSt)
                var remIdx = leaf
                for (s in nSt - 1 downTo 0) {
                    val k = stages[s].size
                    val optIdx = remIdx % k
                    remIdx /= k
                    val nKey = "n_${s + 1}_${leaf / stageLeafSpan(stages, s)}"
                    val lbl = ovMap[nKey]?.trim() ?: stages[s][optIdx].substringBefore(':').trim()
                    pathParts.add(0, lbl)
                }
                val pct = kotlin.math.round(1000f / totalLeaves) / 10f
                val pctStr = if (pct == pct.toInt().toFloat()) "${pct.toInt()}%" else "$pct%"
                val rDef = when (mode) {
                    "frac" -> "1/$totalLeaves"
                    "count" -> pathParts.joinToString("-")
                    else -> pctStr
                }
                val ry = leafY[leaf] - nh / 2f
                regions?.add(InteractiveRegion(rKey, "نتیجهٔ برگ ${pFa(leaf + 1)}", rDef, rx, ry, rw, nh))
                sb.append(ln(colX[nSt - 1] + nw / 2f, leafY[leaf], rx - 2f, leafY[leaf], "#cbd5e1", 1.1f, "3 2"))
                sb.append(rc(rx, ry, rw, nh, if (isEmpty) "#ffffff" else "#f8fafc", if (isEmpty) "#4f46e5" else "#94a3b8", if (isEmpty) 1.6f else 1.2f, 5f))
                if (isEmpty) sb.append(rcDash(rx + 2.5f, ry + 2.5f, rw - 5f, nh - 5f, "none", "#4f46e5", 1.3f, 4f, "4 2"))
                val rDisp = if (isEmpty) ansMap[rKey]?.trim().orEmpty() else (ovMap[rKey]?.trim() ?: rDef)
                if (rDisp.isNotEmpty()) {
                    sb.append(tx(rx + rw / 2f, leafY[leaf] + (fontN - 1) * 0.36f, rDisp, if (isEmpty) "#1d4ed8" else "#0f766e", "middle", bold = true, size = max(8, fontN - 1), canvasW = vw))
                }
            }
        }
        return sb.toString()
    }

    private fun stageLeafSpan(stages: List<List<String>>, stageIdx: Int): Int {
        var span = 1
        for (s in stageIdx + 1 until stages.size) span *= stages[s].size
        return span
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

    private val ELEMENTS_54 = listOf(
        "" to "",
        "H" to "هیدروژن", "He" to "هلیم", "Li" to "لیتیم", "Be" to "بریلیم", "B" to "بور",
        "C" to "کربن", "N" to "نیتروژن", "O" to "اکسیژن", "F" to "فلوئور", "Ne" to "نئون",
        "Na" to "سدیم", "Mg" to "منیزیم", "Al" to "آلومینیم", "Si" to "سیلیسیم", "P" to "فسفر",
        "S" to "گوگرد", "Cl" to "کلر", "Ar" to "آرگون", "K" to "پتاسیم", "Ca" to "کلسیم",
        "Sc" to "اسکاندیم", "Ti" to "تیتانیم", "V" to "وانادیم", "Cr" to "کروم", "Mn" to "منگنز",
        "Fe" to "آهن", "Co" to "کبالت", "Ni" to "نیکل", "Cu" to "مس", "Zn" to "روی",
        "Ga" to "گالیم", "Ge" to "ژرمانیم", "As" to "آرسنیک", "Se" to "سلنیم", "Br" to "برم", "Kr" to "کریپتون",
        "Rb" to "روبیدیم", "Sr" to "استرانسیم", "Y" to "ایتریم", "Zr" to "زیرکونیم", "Nb" to "نیوبیم",
        "Mo" to "مولیبدن", "Tc" to "تکنسیم", "Ru" to "روتنیم", "Rh" to "رودیم", "Pd" to "پالادیم",
        "Ag" to "نقره", "Cd" to "کادمیم", "In" to "ایندیم", "Sn" to "قلع", "Sb" to "آنتیموان",
        "Te" to "تلوریم", "I" to "ید", "Xe" to "زنون"
    )

    private fun subshellFill54(z: Int): List<Triple<String, Int, Int>> {
        val order = listOf(
            "1s" to 1, "2s" to 1, "2p" to 3, "3s" to 1, "3p" to 3,
            "4s" to 1, "3d" to 5, "4p" to 3, "5s" to 1, "4d" to 5, "5p" to 3
        )
        var rem = z.coerceIn(1, 54)
        val counts = LinkedHashMap<String, Pair<Int, Int>>()
        for ((name, boxes) in order) {
            if (rem <= 0) break
            val take = min(rem, boxes * 2)
            counts[name] = boxes to take
            rem -= take
        }
        when (z) {
            24 -> { counts["4s"] = 1 to 1; counts["3d"] = 5 to 5 }
            29 -> { counts["4s"] = 1 to 1; counts["3d"] = 5 to 10 }
            42 -> { counts["5s"] = 1 to 1; counts["4d"] = 5 to 5 }
            47 -> { counts["5s"] = 1 to 1; counts["4d"] = 5 to 10 }
        }
        return counts.map { (k, v) -> Triple(k, v.first, v.second) }
    }

    private fun supDigits(n: Int): String {
        val map = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹')
        return n.toString().map { map[it] ?: it }.joinToString("")
    }

    private fun orbitalDiagram(spec: FigureSpec): String = orbitalDiagramRender(spec, null)

    private fun orbitalDiagramRender(spec: FigureSpec, regions: MutableList<InteractiveRegion>?): String {
        val z = spec.xNum("z", 11f).toInt().coerceIn(1, 54)
        val mode = spec.xStr("mode", "box").lowercase()
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
        val (elSym, elFa) = ELEMENTS_54[z]
        val sb = StringBuilder()

        if (mode == "lewis") {
            val sym = spec.xStr("sym", "").ifBlank { elSym }
            val valenceDefault = when {
                z == 2 -> 2
                z <= 2 -> z
                z <= 10 -> z - 2
                z <= 18 -> z - 10
                z in 19..20 -> z - 18
                z in 31..36 -> z - 28
                z in 37..38 -> z - 36
                z in 49..54 -> z - 46
                else -> 2
            }
            val valence = if (spec.xStr("val", "").isNotBlank()) spec.xNum("val", valenceDefault.toFloat()).toInt().coerceIn(1, 8) else valenceDefault
            val cx = 180f; val cy = 128f
            val symEmpty = "sym" in emptySet
            regions?.add(InteractiveRegion("sym", "نماد شیمیایی اتم مرکزی", sym, cx - 28f, cy - 26f, 56f, 52f))
            sb.append(cls(cx, cy, 34f, if (symEmpty) "#eef2ff" else "rgba(108,99,245,.12)", if (symEmpty) "#4f46e5" else ACC, 1.8f))
            if (symEmpty) sb.append(rcDash(cx - 20f, cy - 18f, 40f, 36f, "none", "#4f46e5", 1.3f, 5f, "4 2"))
            val symDisp = if (symEmpty) ansMap["sym"]?.trim().orEmpty() else (ovMap["sym"]?.trim() ?: sym)
            if (symDisp.isNotEmpty()) sb.append(tx(cx, cy + 8f, symDisp, if (symEmpty) "#1d4ed8" else ST, "middle", bold = true, size = 24))

            val spots = listOf(0f to -48f, 48f to 0f, 0f to 48f, -48f to 0f, 14f to -48f, 48f to 14f, -14f to 48f, -48f to -14f)
            val adjPos = listOf(-7f to -48f, 48f to -7f, 7f to 48f, -48f to 7f)
            for (i in 0 until valence) {
                val baseSpot = if (i < 4 && valence > i + 4) adjPos[i] else spots[i]
                val shiftSpot = if (i >= 4) (when (i - 4) { 0 -> 7f to -48f; 1 -> 48f to 7f; 2 -> -7f to 48f; else -> -48f to -7f }) else baseSpot
                sb.append(cls(cx + shiftSpot.first, cy + shiftSpot.second, 5f, PT, "none", 0f))
            }
            val vKey = "val_box"
            val vEmpty = vKey in emptySet
            val vDef = "الکترون ظرفیت: $valence"
            regions?.add(InteractiveRegion(vKey, "تعداد الکترون ظرفیت", valence.toString(), 68f, 224f, 224f, 32f))
            sb.append(rc(68f, 224f, 224f, 32f, if (vEmpty) "#ffffff" else "#f8fafc", if (vEmpty) "#4f46e5" else "#cbd5e1", 1.4f, 7f))
            if (vEmpty) sb.append(rcDash(72f, 228f, 216f, 24f, "none", "#4f46e5", 1.3f, 5f, "4 2"))
            val vDisp = if (vEmpty) ansMap[vKey]?.trim().orEmpty() else (ovMap[vKey]?.trim() ?: vDef)
            if (vDisp.isNotEmpty()) sb.append(tx(180f, 245f, vDisp, if (vEmpty) "#1d4ed8" else MU, "middle", bold = true, size = 12))
            return sb.toString()
        }

        val subs = subshellFill54(z)
        sb.append(tx(180f, 20f, "${z}$elSym ($elFa) — $z الکترون", "#1e293b", "middle", bold = true, size = 12))

        val rowGroups = listOf(
            subs.filter { it.first in setOf("1s", "2s", "2p", "3s", "3p") },
            subs.filter { it.first in setOf("4s", "3d", "4p") },
            subs.filter { it.first in setOf("5s", "4d", "5p") }
        ).filter { it.isNotEmpty() }

        val boxS = if (rowGroups.size >= 3) 21f else 24f
        val subGap = 11f
        val startY = when (rowGroups.size) { 1 -> 82f; 2 -> 56f; else -> 34f }
        val rowStep = when (rowGroups.size) { 1 -> 60f; 2 -> 64f; else -> 52f }

        rowGroups.forEachIndexed { rIdx, grp ->
            val rowW = grp.sumOf { (it.second * boxS).toDouble() }.toFloat() + (grp.size - 1) * subGap
            var curX = (360f - rowW) / 2f
            val by = startY + rIdx * rowStep

            grp.forEach { (subName, nBoxes, elec) ->
                for (j in 0 until nBoxes) {
                    val ox = curX + j * boxS
                    val oKey = "o_${subName}_$j"
                    val hasUp = elec > j
                    val hasDown = elec > nBoxes + j
                    val oDef = when {
                        hasUp && hasDown -> "↑↓"
                        hasUp -> "↑"
                        else -> ""
                    }
                    val isEmpty = oKey in emptySet
                    regions?.add(InteractiveRegion(oKey, "اوربیتال $subName (خانهٔ ${pFa(j + 1)})", oDef.ifEmpty { "0" }, ox, by, boxS, boxS))
                    sb.append(rc(ox, by, boxS, boxS, if (isEmpty) "#eef2ff" else "#ffffff", if (isEmpty) "#4f46e5" else ST, if (isEmpty) 1.7f else 1.4f, 2.5f))
                    if (isEmpty) sb.append(rcDash(ox + 2.5f, by + 2.5f, boxS - 5f, boxS - 5f, "none", "#4f46e5", 1.2f, 2f, "3 2"))

                    val rawDisp = if (isEmpty) ansMap[oKey]?.trim().orEmpty() else (ovMap[oKey]?.trim() ?: oDef)
                    val norm = faEnDigits(rawDisp)
                    val colUp = if (isEmpty) "#1d4ed8" else ACC
                    val colDn = if (isEmpty) "#1d4ed8" else PT
                    when {
                        norm == "↑↓" || norm == "2" || norm == "⇅" -> {
                            sb.append(arrow(ox + boxS * 0.34f, by + boxS - 3.5f, ox + boxS * 0.34f, by + 3.5f, colUp, 1.5f, 4.5f))
                            sb.append(arrow(ox + boxS * 0.68f, by + 3.5f, ox + boxS * 0.68f, by + boxS - 3.5f, colDn, 1.5f, 4.5f))
                        }
                        norm == "↑" || norm == "1" -> {
                            sb.append(arrow(ox + boxS * 0.5f, by + boxS - 3.5f, ox + boxS * 0.5f, by + 3.5f, colUp, 1.6f, 4.8f))
                        }
                        norm == "↓" || norm == "-1" -> {
                            sb.append(arrow(ox + boxS * 0.5f, by + 3.5f, ox + boxS * 0.5f, by + boxS - 3.5f, colDn, 1.6f, 4.8f))
                        }
                        norm.isNotEmpty() && norm != "0" -> {
                            sb.append(tx(ox + boxS / 2f, by + boxS * 0.68f, rawDisp, colUp, "middle", bold = true, size = 10))
                        }
                    }
                }
                val sKey = "s_$subName"
                val sDef = "$subName${supDigits(elec)}"
                val sEmpty = sKey in emptySet
                val grpW = nBoxes * boxS
                regions?.add(InteractiveRegion(sKey, "برچسب زیرلایهٔ $subName", sDef, curX, by + boxS + 1f, grpW, 16f))
                if (sEmpty) {
                    sb.append(rcDash(curX + 1f, by + boxS + 2f, grpW - 2f, 14f, "#ffffff", "#4f46e5", 1.2f, 3f, "3 2"))
                    val sAns = ansMap[sKey]?.trim().orEmpty()
                    if (sAns.isNotEmpty()) sb.append(tx(curX + grpW / 2f, by + boxS + 12f, sAns, "#1d4ed8", "middle", bold = true, size = 9))
                } else {
                    val sTxt = ovMap[sKey]?.trim() ?: sDef
                    sb.append(tx(curX + grpW / 2f, by + boxS + 13f, sTxt, MU, "middle", bold = true, size = 10))
                }
                curX += grpW + subGap
            }
        }

        val fullCfg = subs.joinToString(" ") { "${it.first}${supDigits(it.third)}" }
        val nobleCfg = when {
            z > 36 -> "[Kr] " + subs.filter { it.first in setOf("5s", "4d", "5p") }.joinToString(" ") { "${it.first}${supDigits(it.third)}" }
            z > 18 -> "[Ar] " + subs.filter { it.first in setOf("4s", "3d", "4p") }.joinToString(" ") { "${it.first}${supDigits(it.third)}" }
            else -> fullCfg
        }
        val cfgKey = "cfg"
        val cfgEmpty = cfgKey in emptySet
        regions?.add(InteractiveRegion(cfgKey, "آرایش الکترونی عنصر", nobleCfg, 22f, 232f, 316f, 34f))
        sb.append(rc(22f, 232f, 316f, 34f, if (cfgEmpty) "#ffffff" else "#f8fafc", if (cfgEmpty) "#4f46e5" else "#cbd5e1", 1.4f, 8f))
        if (cfgEmpty) sb.append(rcDash(26f, 236f, 308f, 26f, "none", "#4f46e5", 1.3f, 6f, "4 2.5"))
        val cfgDisp = if (cfgEmpty) ansMap[cfgKey]?.trim().orEmpty() else (ovMap[cfgKey]?.trim() ?: nobleCfg)
        if (cfgDisp.isNotEmpty()) {
            sb.append(tx(180f, 253f, cfgDisp, if (cfgEmpty) "#1d4ed8" else "#1e293b", "middle", bold = true, size = 12))
        }
        return sb.toString()
    }

    private fun combineGametes(g1: String, g2: String): String {
        if (g1.length == 2 && g2.length == 2) {
            val loc1 = if (g1[0].isUpperCase()) "${g1[0]}${g2[0]}" else if (g2[0].isUpperCase()) "${g2[0]}${g1[0]}" else "${g1[0]}${g2[0]}"
            val loc2 = if (g1[1].isUpperCase()) "${g1[1]}${g2[1]}" else if (g2[1].isUpperCase()) "${g2[1]}${g1[1]}" else "${g1[1]}${g2[1]}"
            return loc1 + loc2
        }
        val a = g1.firstOrNull() ?: 'A'
        val b = g2.firstOrNull() ?: 'a'
        return if (a.isUpperCase()) "$a$b" else if (b.isUpperCase()) "$b$a" else "$a$b"
    }

    private fun gametesOf(parent: String): List<String> {
        val p = parent.trim()
        if (p.length >= 4) {
            return listOf("${p[0]}${p[2]}", "${p[0]}${p[3]}", "${p[1]}${p[2]}", "${p[1]}${p[3]}")
        }
        val c0 = p.getOrElse(0) { 'A' }.toString()
        val c1 = p.getOrElse(1) { c0[0] }.toString()
        return listOf(c0, c1)
    }

    private fun punnettSquare(spec: FigureSpec): String = punnettSquareRender(spec, null)

    private fun punnettSquareRender(spec: FigureSpec, regions: MutableList<InteractiveRegion>?): String {
        val kind = spec.xStr("kind", "punnett").lowercase()
        val mode = spec.xStr("mode", "genotype").lowercase()
        val emptySet = parseEmptySet(spec)
        val ovMap = parseCellMap(spec, "ov")
        val ansMap = parseCellMap(spec, "ans")
        val sb = StringBuilder()

        if (kind == "pedigree") {
            val gens = spec.xNum("gens", 3f).toInt().coerceIn(2, 4)
            sb.append(tx(180f, 18f, "شجره‌نامهٔ وراثت (${pFa(gens)} نسل)", "#1e293b", "middle", bold = true, size = 12))

            data class PedNode(val gen: Int, val idx: Int, val x: Float, val y: Float, val defCode: String, val geno: String)
            val yG = when (gens) {
                2 -> floatArrayOf(0f, 76f, 168f)
                3 -> floatArrayOf(0f, 54f, 122f, 190f)
                else -> floatArrayOf(0f, 46f, 98f, 150f, 202f)
            }
            val r = if (gens >= 4) 11.5f else 13.5f

            val nodes = mutableListOf<PedNode>()
            nodes += PedNode(1, 0, 136f, yG[1], "Mc", "Aa")
            nodes += PedNode(1, 1, 224f, yG[1], "Fc", "Aa")
            sb.append(ln(136f + r, yG[1], 224f - r, yG[1], ST, 1.8f))
            val mid12 = 180f
            val sibY2 = (yG[1] + yG[2]) / 2f
            sb.append(ln(mid12, yG[1], mid12, sibY2, ST, 1.7f))

            if (gens == 2) {
                val cXs = listOf(104f, 180f, 256f)
                val cDefs = listOf("M*", "F", "Mc")
                val cGenos = listOf("aa", "AA", "Aa")
                sb.append(ln(cXs.first(), sibY2, cXs.last(), sibY2, ST, 1.7f))
                cXs.forEachIndexed { idx, cx ->
                    sb.append(ln(cx, sibY2, cx, yG[2] - r, ST, 1.7f))
                    nodes += PedNode(2, idx, cx, yG[2], cDefs[idx], cGenos[idx])
                }
            } else {
                val c2Xs = listOf(92f, 164f, 224f)
                sb.append(ln(c2Xs.first(), sibY2, c2Xs.last(), sibY2, ST, 1.7f))
                c2Xs.forEach { cx -> sb.append(ln(cx, sibY2, cx, yG[2] - r, ST, 1.7f)) }
                nodes += PedNode(2, 0, 92f, yG[2], "M*", "aa")
                nodes += PedNode(2, 1, 164f, yG[2], "F", "AA")
                nodes += PedNode(2, 2, 224f, yG[2], "Mc", "Aa")
                nodes += PedNode(2, 3, 288f, yG[2], "Fc", "Aa")
                sb.append(ln(224f + r, yG[2], 288f - r, yG[2], ST, 1.8f))

                val mid23 = 256f
                val sibY3 = (yG[2] + yG[3]) / 2f
                sb.append(ln(mid23, yG[2], mid23, sibY3, ST, 1.7f))
                if (gens == 3) {
                    val c3Xs = listOf(196f, 256f, 316f)
                    sb.append(ln(c3Xs.first(), sibY3, c3Xs.last(), sibY3, ST, 1.7f))
                    c3Xs.forEach { cx -> sb.append(ln(cx, sibY3, cx, yG[3] - r, ST, 1.7f)) }
                    nodes += PedNode(3, 0, 196f, yG[3], "F*", "aa")
                    nodes += PedNode(3, 1, 256f, yG[3], "M", "AA")
                    nodes += PedNode(3, 2, 316f, yG[3], "Fc", "Aa")
                } else {
                    val c3Xs = listOf(208f, 284f)
                    sb.append(ln(c3Xs.first(), sibY3, c3Xs.last(), sibY3, ST, 1.7f))
                    c3Xs.forEach { cx -> sb.append(ln(cx, sibY3, cx, yG[3] - r, ST, 1.7f)) }
                    nodes += PedNode(3, 0, 140f, yG[3], "Mc", "Aa")
                    nodes += PedNode(3, 1, 208f, yG[3], "Fc", "Aa")
                    nodes += PedNode(3, 2, 284f, yG[3], "M*", "aa")
                    sb.append(ln(140f + r, yG[3], 208f - r, yG[3], ST, 1.8f))

                    val mid34 = 174f
                    val sibY4 = (yG[3] + yG[4]) / 2f
                    val c4Xs = listOf(124f, 174f, 224f)
                    sb.append(ln(mid34, yG[3], mid34, sibY4, ST, 1.7f))
                    sb.append(ln(c4Xs.first(), sibY4, c4Xs.last(), sibY4, ST, 1.7f))
                    c4Xs.forEach { cx -> sb.append(ln(cx, sibY4, cx, yG[4] - r, ST, 1.7f)) }
                    nodes += PedNode(4, 0, 124f, yG[4], "F*", "aa")
                    nodes += PedNode(4, 1, 174f, yG[4], "M", "AA")
                    nodes += PedNode(4, 2, 224f, yG[4], "Fc", "Aa")
                }
            }

            val romans = listOf("", "I", "II", "III", "IV")
            for (g in 1..gens) {
                sb.append(tx(28f, yG[g] + 4f, romans[g], ACC, "middle", bold = true, size = 12))
            }

            nodes.forEach { nd ->
                val pKey = "p_${nd.gen}_${nd.idx}"
                val isEmpty = pKey in emptySet
                val hitS = r * 2f + 6f
                regions?.add(InteractiveRegion(pKey, "نسل ${romans[nd.gen]} فرد ${pFa(nd.idx + 1)}", nd.defCode, nd.x - hitS / 2f, nd.y - hitS / 2f, hitS, hitS))
                if (isEmpty) {
                    sb.append(rc(nd.x - r - 2f, nd.y - r - 2f, (r + 2f) * 2f, (r + 2f) * 2f, "#ffffff", "#4f46e5", 1.6f, 4f))
                    sb.append(rcDash(nd.x - r + 1f, nd.y - r + 1f, (r - 1f) * 2f, (r - 1f) * 2f, "none", "#4f46e5", 1.2f, 3f, "3 2"))
                    val stu = ansMap[pKey]?.trim().orEmpty()
                    if (stu.isNotEmpty()) {
                        drawPedigreeSymbol(sb, nd.x, nd.y, r, stu, "#1d4ed8", isStudent = true)
                    }
                } else {
                    val code = ovMap[pKey]?.trim() ?: nd.defCode
                    drawPedigreeSymbol(sb, nd.x, nd.y, r, code, ST, isStudent = false)
                    if (mode == "genotype" && code in setOf("M", "M*", "Mc", "F", "F*", "Fc")) {
                        sb.append(tx(nd.x, nd.y + r + 11f, nd.geno, MU, "middle", bold = true, size = 8))
                    }
                }
            }

            sb.append(rc(14f, 246f, 332f, 26f, "#f8fafc", "#cbd5e1", 1.1f, 6f))
            sb.append(tx(180f, 263f, "□ مرد سالم   ■ مبتلا   ○ زن سالم   ● مبتلا   ◐ ناقل", MU, "middle", bold = true, size = 10))
            return sb.toString()
        }

        val p1 = spec.xStr("p1", "Aa").trim().ifEmpty { "Aa" }
        val p2 = spec.xStr("p2", "Aa").trim().ifEmpty { "Aa" }
        val g1Base = gametesOf(p1)
        val g2Base = gametesOf(p2)
        val k = max(g1Base.size, g2Base.size)
        val g1 = (0 until k).map { i -> ovMap["g1_$i"]?.trim() ?: g1Base.getOrElse(i) { g1Base.last() } }
        val g2 = (0 until k).map { i -> ovMap["g2_$i"]?.trim() ?: g2Base.getOrElse(i) { g2Base.last() } }

        val cell = if (k <= 2) 66f else 40f
        val hdr = if (k <= 2) 32f else 26f
        val x0 = 180f - (cell * k) / 2f + 10f
        val y0 = if (k <= 2) 54f else 42f

        sb.append(tx(180f, 20f, "آمیزش ژنتیکی:  $p1 × $p2", "#1e293b", "middle", bold = true, size = 12))

        for (c in 0 until k) {
            val gk = "g1_$c"
            val gx = x0 + c * cell
            val gy = y0 - hdr
            val gEmpty = gk in emptySet
            regions?.add(InteractiveRegion(gk, "گامت ${pFa(c + 1)} والد اول", g1Base.getOrElse(c) { "" }, gx, gy, cell, hdr))
            sb.append(rc(gx, gy, cell, hdr, if (gEmpty) "#ffffff" else "#eef2ff", if (gEmpty) "#4f46e5" else ACC, 1.4f, 4f))
            if (gEmpty) sb.append(rcDash(gx + 3f, gy + 3f, cell - 6f, hdr - 6f, "none", "#4f46e5", 1.2f, 3f, "3 2"))
            val disp = if (gEmpty) ansMap[gk]?.trim().orEmpty() else g1[c]
            if (disp.isNotEmpty()) sb.append(tx(gx + cell / 2f, gy + hdr * 0.68f, disp, if (gEmpty) "#1d4ed8" else ACC, "middle", bold = true, size = if (k <= 2) 14 else 11))
        }
        for (r in 0 until k) {
            val gk = "g2_$r"
            val gx = x0 - hdr - 4f
            val gy = y0 + r * cell
            val gEmpty = gk in emptySet
            regions?.add(InteractiveRegion(gk, "گامت ${pFa(r + 1)} والد دوم", g2Base.getOrElse(r) { "" }, gx, gy, hdr, cell))
            sb.append(rc(gx, gy, hdr, cell, if (gEmpty) "#ffffff" else "#ecfdf5", if (gEmpty) "#4f46e5" else "#0f766e", 1.4f, 4f))
            if (gEmpty) sb.append(rcDash(gx + 3f, gy + 3f, hdr - 6f, cell - 6f, "none", "#4f46e5", 1.2f, 3f, "3 2"))
            val disp = if (gEmpty) ansMap[gk]?.trim().orEmpty() else g2[r]
            if (disp.isNotEmpty()) sb.append(tx(gx + hdr / 2f, gy + cell * 0.56f, disp, if (gEmpty) "#1d4ed8" else "#0f766e", "middle", bold = true, size = if (k <= 2) 14 else 11))
        }

        var domCount = 0
        val genoCounts = LinkedHashMap<String, Int>()
        for (r in 0 until k) for (c in 0 until k) {
            val ck = "c_${r}_$c"
            val defGeno = combineGametes(g2[r], g1[c])
            val isDom = defGeno.any { it.isUpperCase() }
            if (isDom) domCount++
            genoCounts[defGeno] = (genoCounts[defGeno] ?: 0) + 1

            val cx = x0 + c * cell
            val cy = y0 + r * cell
            val isEmpty = ck in emptySet
            regions?.add(InteractiveRegion(ck, "خانهٔ سطر ${pFa(r + 1)} ستون ${pFa(c + 1)} پانت", defGeno, cx, cy, cell, cell))

            val bg = when {
                isEmpty -> "#ffffff"
                defGeno.all { it.isUpperCase() } -> "rgba(108,99,245,.18)"
                isDom -> "rgba(56,189,248,.16)"
                else -> "rgba(39,196,168,.16)"
            }
            sb.append(rc(cx, cy, cell, cell, bg, if (isEmpty) "#4f46e5" else ST, if (isEmpty) 1.8f else 1.5f))
            if (isEmpty) sb.append(rcDash(cx + 4f, cy + 4f, cell - 8f, cell - 8f, "none", "#4f46e5", 1.3f, 4f, "4 2.5"))
            val disp = if (isEmpty) ansMap[ck]?.trim().orEmpty() else (ovMap[ck]?.trim() ?: defGeno)
            if (disp.isNotEmpty()) {
                sb.append(tx(cx + cell / 2f, cy + cell * 0.58f, disp, if (isEmpty) "#1d4ed8" else ST, "middle", bold = true, size = if (k <= 2) 16 else 11))
            }
        }

        if (mode != "quiz") {
            val total = k * k
            val recCount = total - domCount
            val summary = if (mode == "ratio") {
                "نسبت فنوتیپی:  $domCount غالب  :  $recCount مغلوب"
            } else {
                "ژنوتیپ‌ها:  " + genoCounts.entries.take(4).joinToString(" ، ") { "${it.value}/${total} ${it.key}" }
            }
            val rk = "ratio"
            val rEmpty = rk in emptySet
            val by = y0 + k * cell + 12f
            regions?.add(InteractiveRegion(rk, "نسبت ژنوتیپی/فنوتیپی", summary, 24f, by, 312f, 28f))
            sb.append(rc(24f, by, 312f, 28f, if (rEmpty) "#ffffff" else "#f8fafc", if (rEmpty) "#4f46e5" else "#cbd5e1", 1.3f, 7f))
            if (rEmpty) sb.append(rcDash(28f, by + 3f, 304f, 22f, "none", "#4f46e5", 1.2f, 5f, "4 2"))
            val rDisp = if (rEmpty) ansMap[rk]?.trim().orEmpty() else (ovMap[rk]?.trim() ?: summary)
            if (rDisp.isNotEmpty()) sb.append(tx(180f, by + 18f, rDisp, if (rEmpty) "#1d4ed8" else "#1e293b", "middle", bold = true, size = 11))
        }
        return sb.toString()
    }

    private fun drawPedigreeSymbol(sb: StringBuilder, x: Float, y: Float, r: Float, codeRaw: String, stroke: String, isStudent: Boolean) {
        val code = codeRaw.trim()
        val affectedFill = if (isStudent) "#1d4ed8" else "#334155"
        when (code) {
            "M" -> sb.append(rc(x - r, y - r, r * 2f, r * 2f, "#ffffff", stroke, 2f, 2f))
            "M*" -> sb.append(rc(x - r, y - r, r * 2f, r * 2f, affectedFill, stroke, 2f, 2f))
            "Mc" -> {
                sb.append(rc(x - r, y - r, r * 2f, r * 2f, "#ffffff", stroke, 2f, 2f))
                sb.append(rc(x - r, y - r, r, r * 2f, affectedFill, "none", 0f))
                sb.append(rc(x - r, y - r, r * 2f, r * 2f, "none", stroke, 2f, 2f))
            }
            "F" -> sb.append(cls(x, y, r, "#ffffff", stroke, 2f))
            "F*" -> sb.append(cls(x, y, r, affectedFill, stroke, 2f))
            "Fc" -> {
                sb.append(cls(x, y, r, "#ffffff", stroke, 2f))
                sb.append(path("M ${f(x)} ${f(y - r)} A ${f(r)} ${f(r)} 0 0 0 ${f(x)} ${f(y + r)} Z", affectedFill, "none", 0f))
                sb.append(cls(x, y, r, "none", stroke, 2f))
            }
            else -> {
                sb.append(rc(x - r - 2f, y - r, (r + 2f) * 2f, r * 2f, "#eef2ff", stroke, 1.6f, 4f))
                sb.append(tx(x, y + 4f, code, if (isStudent) "#1d4ed8" else ST, "middle", bold = true, size = 10))
            }
        }
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
    private val ARKAN_PATTERNS = mapOf(
        "فعولن" to "∪−−", "فاعلاتن" to "−∪−−", "مفاعیلن" to "∪−−−", "مستفعلن" to "−−∪−",
        "مفاعلن" to "∪−∪−", "فعلاتن" to "∪∪−−", "مفتعلن" to "−∪∪−", "متفاعلن" to "∪∪−∪−",
        "مفعولن" to "−−−", "فاعلن" to "−∪−", "فاعلات" to "−∪−", "مفاعیل" to "∪−−",
        "مستفعل" to "−−∪", "مفعول" to "−−∪", "فعول" to "∪−", "فعل" to "∪−",
        "فعلن" to "∪∪−", "فع‌لن" to "−−", "فاع" to "−∪", "فع" to "−", "مفاعلاتن" to "∪−−∪−"
    )

    private fun detectProsodyMeter(feet: List<String>): String {
        val joined = feet.joinToString(" ")
        return when {
            joined.startsWith("فعولن فعولن فعولن فعل") -> "بحر متقارب مثمن محذوف (وزن شاهنامه و بوستان)"
            feet.all { it == "فعولن" } -> "بحر متقارب سالم"
            joined.startsWith("فاعلاتن فاعلاتن فاعلاتن فاعلن") || joined.startsWith("فاعلاتن فاعلاتن فاعلن") -> "بحر رمل محذوف (وزن مثنوی معنوی)"
            feet.all { it == "فاعلاتن" } -> "بحر رمل سالم"
            feet.all { it == "مفاعیلن" } -> "بحر هزج سالم"
            joined.startsWith("مفاعیلن مفاعیلن فعولن") -> "بحر هزج مسدس محذوف (وزن خسرو و شیرین)"
            feet.all { it == "مستفعلن" } -> "بحر رجز سالم"
            joined.startsWith("مفاعلن فعلاتن مفاعلن") -> "بحر مجتث مثمن مخبون"
            joined.startsWith("مفعول فاعلات مفاعیل") -> "بحر مضارع اخرب مکفوف"
            joined.startsWith("فعلاتن فاعلاتن") || joined.startsWith("فعلاتن مفاعلن") -> "بحر خفیف"
            joined.startsWith("مفتعلن فاعلات") || joined.startsWith("مفتعلن مفتعلن") -> "بحر سریع / منسرح"
            else -> "وزن عروضی: ${feet.size} رکن"
        }
    }

    private fun prosodyScan(spec: FigureSpec): String {
        val rawLine = spec.xStr("line", "فعولن فعولن فعولن فعل").trim()
        val verse = spec.xStr("verse", "").trim()
        val mode = spec.xStr("mode", "arkan").lowercase()
        val feet = rawLine.split(Regex("\\s+")).filter { it.isNotBlank() }.take(4).ifEmpty { listOf("فعولن", "فعولن", "فعولن", "فعل") }
        val cols = feet.size
        val cw = min(80f, 320f / cols)
        val totalW = cw * cols
        val x0 = 180f - totalW / 2f
        val y0 = if (verse.isNotEmpty()) 58f else 42f
        val sb = StringBuilder()

        if (verse.isNotEmpty()) {
            sb.append(rc(22f, 16f, 316f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 7f))
            sb.append(tx(180f, 36f, verse, "#1e293b", "middle", bold = true, size = 12))
        }

        feet.forEachIndexed { idx, rk ->
            val rtlCol = cols - 1 - idx
            val cx = x0 + rtlCol * cw
            val pat = ARKAN_PATTERNS[rk] ?: rk.replace('U', '∪').replace('u', '∪').replace('-', '−').let {
                if (it.all { ch -> ch == '∪' || ch == '−' }) it else "∪−−"
            }

            val showArkan = mode != "taqti"
            sb.append(rc(cx + 2f, y0, cw - 4f, 36f, if (showArkan) "rgba(108,99,245,.15)" else "#fffbeb", if (showArkan) ACC else GLD, 1.6f, 6f))
            sb.append(tx(cx + cw / 2f, y0 + 23f, if (showArkan) rk else "رکن ${pFa(idx + 1)} ؟", if (showArkan) "#1e1b4b" else "#92400e", "middle", bold = true, size = 12))

            val showTaqti = mode != "blank_taqti"
            sb.append(rc(cx + 2f, y0 + 44f, cw - 4f, 46f, "#ffffff", "#cbd5e1", 1.4f, 6f))
            if (showTaqti) {
                val nSym = pat.length.coerceAtLeast(1)
                val step = (cw - 14f) / nSym
                pat.forEachIndexed { si, ch ->
                    val sx = cx + cw - 7f - (si + 0.5f) * step
                    val sy = y0 + 67f
                    if (ch == '∪') {
                        sb.append(path("M ${f(sx - 5.5f)} ${f(sy - 4f)} Q ${f(sx)} ${f(sy + 7f)} ${f(sx + 5.5f)} ${f(sy - 4f)}", "none", PT, 2.2f))
                    } else {
                        sb.append(ln(sx - 6f, sy, sx + 6f, sy, "#0f766e", 2.6f))
                    }
                }
            } else {
                sb.append(rcDash(cx + 7f, y0 + 50f, cw - 14f, 34f, "#f8fafc", "#6366f1", 1.3f, 4f, "4 2.5"))
            }
            sb.append(tx(cx + cw / 2f, y0 + 106f, "${pFa(pat.length)} هجا", MU, "middle", bold = true, size = 9))
        }

        val meterLabel = if (mode == "arkan") detectProsodyMeter(feet) else "تقطیع هجایی:  ∪ هجای کوتاه   |   − هجای بلند"
        sb.append(rc(20f, 234f, 320f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 8f))
        sb.append(tx(180f, 254f, meterLabel, "#1e293b", "middle", bold = true, size = 11))
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
            val harmony = spec.xStr("harmony", "itten").lowercase()
            val cols = listOf(
                "#facc15", "#fb923c", "#f97316", "#ef4444", "#dc2626", "#be185d",
                "#7e22ce", "#4338ca", "#2563eb", "#0d9488", "#16a34a", "#84cc16"
            )
            val names = listOf("زرد", "ز-ن", "نارنجی", "ق-ن", "قرمز", "ق-ب", "بنفش", "آ-ب", "آبی", "آ-س", "سبز", "ز-س")
            val cx = 180f; val cy = 128f; val r = 84f; val ri = 48f
            for (i in 0 until 12) {
                val a0 = -PI / 2 - PI / 12 + i * PI / 6
                val a1 = a0 + PI / 6
                sb.append(path(
                    "M ${f(cx + ri * cos(a0).toFloat())} ${f(cy + ri * sin(a0).toFloat())} " +
                        "L ${f(cx + r * cos(a0).toFloat())} ${f(cy + r * sin(a0).toFloat())} " +
                        "A ${f(r)} ${f(r)} 0 0 1 ${f(cx + r * cos(a1).toFloat())} ${f(cy + r * sin(a1).toFloat())} " +
                        "L ${f(cx + ri * cos(a1).toFloat())} ${f(cy + ri * sin(a1).toFloat())} " +
                        "A ${f(ri)} ${f(ri)} 0 0 0 ${f(cx + ri * cos(a0).toFloat())} ${f(cy + ri * sin(a0).toFloat())} Z",
                    cols[i], "#ffffff", 1.4f
                ))
                if (harmony != "quiz" && i % 2 == 0) {
                    val am = (a0 + a1) / 2.0
                    val lx = cx + (r + 17f) * cos(am).toFloat()
                    val ly = cy + (r + 14f) * sin(am).toFloat() + 4f
                    sb.append(tx(lx, ly, names[i], ST, "middle", bold = true, size = 9))
                }
            }
            sb.append(cls(cx, cy, ri, "#ffffff", ST, 1.5f))
            fun rimPt(idx: Int, radR: Float): String {
                val a = -PI / 2 + idx * PI / 6
                return "${f(cx + radR * cos(a).toFloat())},${f(cy + radR * sin(a).toFloat())}"
            }
            if (harmony == "comp") {
                sb.append(ln(cx, cy - ri + 4f, cx, cy + ri - 4f, ST, 2f))
                sb.append(ln(cx - (ri - 4f) * 0.866f, cy - (ri - 4f) * 0.5f, cx + (ri - 4f) * 0.866f, cy + (ri - 4f) * 0.5f, "#dc2626", 1.8f, "4 3"))
            } else if (harmony == "analog") {
                val rArc = ri - 6f
                sb.append(path("M ${rimPt(0, rArc).replace(',', ' ')} A ${f(rArc)} ${f(rArc)} 0 0 1 ${rimPt(2, rArc).replace(',', ' ')}", "none", "#ea580c", 2.8f))
            } else if (harmony != "none" && harmony != "warm_cool" && harmony != "warmcool") {
                val rTri = ri - 4f
                sb.append(poly("${rimPt(2, rTri)} ${rimPt(6, rTri)} ${rimPt(10, rTri)}", "rgba(39,196,168,.16)", "#0f766e", 1.4f))
                sb.append(poly("${rimPt(0, rTri)} ${rimPt(4, rTri)} ${rimPt(8, rTri)}", "rgba(250,204,21,.22)", ST, 1.8f))
            }
            val foot = when (harmony) {
                "comp" -> "چرخهٔ رنگ ایتن — رنگ‌های مکمل روبه‌روی هم روی قطر دایره"
                "analog" -> "چرخهٔ رنگ ایتن — رنگ‌های هم‌جوار (کنار هم روی چرخه)"
                "warm_cool", "warmcool" -> "نیمهٔ راست: رنگ‌های گرم (زرد تا قرمز) | نیمهٔ چپ: رنگ‌های سرد"
                "quiz", "none" -> "چرخهٔ ۱۲ رنگ ایتن — جایگاه رنگ‌های اصلی و ثانویه"
                else -> "چرخهٔ ۱۲ رنگ ایتن — مثلث مرکزی: ۳ رنگ اصلی (زرد، قرمز، آبی)"
            }
            sb.append(tx(180f, 258f, foot, MU, "middle", bold = true, size = 10))
            return sb.toString()
        }

        val clef = spec.xStr("clef", "treble").lowercase()
        val timeSig = spec.xStr("time", "4/4")
        val dur = spec.xStr("dur", "quarter").lowercase()
        val showNames = spec.xStr("label", "1") != "0"
        val notes = ints(spec, "notes", "60,62,64,65,67").take(7).ifEmpty { listOf(60, 62, 64, 65, 67) }
            // V322 — شمارهٔ نت خارج از محدودهٔ پنج‌خطی، بیضی را بیرون از بوم می‌برد؛ به [53,74] مهار می‌شود.
            .map { it.coerceIn(53, 74) }

        val topY = 76f
        val lineGap = 14f
        val botY = topY + 4 * lineGap
        for (i in 0..4) sb.append(ln(24f, topY + i * lineGap, 336f, topY + i * lineGap, ST, 1.4f))
        sb.append(ln(24f, topY, 24f, botY, ST, 2.2f))
        sb.append(ln(332f, topY, 332f, botY, ST, 1.2f))
        sb.append(ln(336f, topY, 336f, botY, ST, 2.6f))

        sb.append(tx(42f, topY + 42f, if (clef == "bass") "𝄢" else "𝄞", ACC, "middle", bold = true, size = 34))
        val tsParts = timeSig.split("/")
        if (tsParts.size == 2) {
            sb.append(tx(70f, topY + 23f, tsParts[0], ST, "middle", bold = true, size = 16))
            sb.append(tx(70f, topY + 51f, tsParts[1], ST, "middle", bold = true, size = 16))
        }

        val pcToStep = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)
        val pcSharp = booleanArrayOf(false, true, false, true, false, false, true, false, true, false, true, false)
        val faNoteNames = arrayOf("دو", "ر", "می", "فا", "سل", "لا", "سی")
        val enNoteNames = arrayOf("C", "D", "E", "F", "G", "A", "B")
        val refDiatonic = if (clef == "bass") (3 * 7 + 4) else (4 * 7 + 2)

        val xStart = 104f
        val stepX = (214f / notes.size.coerceAtLeast(1))
        notes.forEachIndexed { i, midi ->
            val oct = midi / 12 - 1
            val pc = midi % 12
            val diatonic = oct * 7 + pcToStep[pc]
            val stepsFromBottomLine = diatonic - refDiatonic
            val ny = botY - stepsFromBottomLine * (lineGap / 2f)
            val nx = xStart + (i + 0.45f) * stepX

            if (stepsFromBottomLine <= -2) {
                var s = -2
                while (s >= stepsFromBottomLine) {
                    val ly = botY - s * (lineGap / 2f)
                    sb.append(ln(nx - 11f, ly, nx + 11f, ly, ST, 1.4f))
                    s -= 2
                }
            } else if (stepsFromBottomLine >= 10) {
                var s = 10
                while (s <= stepsFromBottomLine) {
                    val ly = botY - s * (lineGap / 2f)
                    sb.append(ln(nx - 11f, ly, nx + 11f, ly, ST, 1.4f))
                    s += 2
                }
            }

            val hollow = dur == "whole" || dur == "half"
            sb.append("<ellipse cx=\"${f(nx)}\" cy=\"${f(ny)}\" rx=\"7\" ry=\"5.2\" transform=\"rotate(-18 ${f(nx)} ${f(ny)})\" fill=\"${if (hollow) "#ffffff" else ST}\" stroke=\"$ST\" stroke-width=\"1.8\"/>")
            if (dur != "whole") {
                val stemUp = stepsFromBottomLine < 4
                val sx = if (stemUp) nx + 6.2f else nx - 6.2f
                val sy2 = if (stemUp) ny - 30f else ny + 30f
                sb.append(ln(sx, ny, sx, sy2, ST, 1.7f))
                if (dur == "eighth") {
                    sb.append(path("M ${f(sx)} ${f(sy2)} Q ${f(sx + 10f)} ${f(sy2 + (if (stemUp) 8f else -8f))} ${f(sx + 7f)} ${f(sy2 + (if (stemUp) 18f else -18f))}", "none", ST, 1.8f))
                }
            }
            if (pcSharp[pc]) {
                sb.append(tx(nx - 14f, ny + 4f, "♯", PT, "middle", bold = true, size = 12))
            }
            if (showNames) {
                val stepIdx = pcToStep[pc]
                val sharpSuffix = if (pcSharp[pc]) "♯" else ""
                sb.append(tx(nx, 186f, "${faNoteNames[stepIdx]}$sharpSuffix", ACC, "middle", bold = true, size = 10))
                sb.append(tx(nx, 201f, "${enNoteNames[stepIdx]}$sharpSuffix$oct", MU, "middle", bold = true, size = 9))
            }
        }
        val clefTitle = if (clef == "bass") "کلید فا" else "کلید سل"
        sb.append(rc(28f, 232f, 304f, 30f, "#f8fafc", "#cbd5e1", 1.2f, 8f))
        sb.append(tx(180f, 252f, if (showNames) "پنج‌خط حامل ($clefTitle — میزان $timeSig)" else "پنج‌خط حامل ($clefTitle) — نام نت‌ها را مشخص کنید", "#1e293b", "middle", bold = true, size = 11))
        return sb.toString()
    }

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
        sb.append(tx(px(c - r) - 6f, cy - 10f, "σ₂ = ${sg(c - r)}", "#00796b", "end", bold = true, size = 10))
        sb.append(tx(px(c + r) + 6f, cy - 10f, "σ₁ = ${sg(c + r)}", "#00796b", "start", bold = true, size = 10))
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
        val rows = faEnDigits(spec.xStr("rows", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val cols = faEnDigits(spec.xStr("cols", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val goal = faEnDigits(spec.xStr("goal", "4")).toFloatOrNull()?.toInt()?.coerceIn(0, rows * cols) ?: 0
        fun parseCoords(raw: String): Set<Pair<Int, Int>> = raw.split(Regex("[;؛]")).mapNotNull { chunk ->
            val a = chunk.split(",", "،")
            val r = a.getOrNull(0)?.let { faEnDigits(it.trim()).toFloatOrNull()?.toInt() }
            val c = a.getOrNull(1)?.let { faEnDigits(it.trim()).toFloatOrNull()?.toInt() }
            if (r != null && c != null && r in 1..rows && c in 1..cols) r to c else null
        }.toSet()
        val cells = parseCoords(spec.xStr("cells", ""))
        val ansCells = parseCoords(spec.xStr("ansCells", ""))
        val w = min(300f, cols * 42f)
        val h = min(150f, rows * 34f)
        val cw = w / cols
        val ch = h / rows
        val x0 = (360f - w) / 2f
        val y0 = 62f + (150f - h) / 2f
        val sb = StringBuilder()
        sb.append(tx(180f, 38f, "شبکهٔ رنگ‌آمیزی", ST, "middle", bold = true, size = 13))
        for (r in 0 until rows) for (c in 0 until cols) {
            val pos = (r + 1) to (c + 1)
            val fill = when {
                pos in cells -> "rgba(108,99,245,.58)"
                pos in ansCells -> "rgba(39,196,168,.62)"
                else -> "#ffffff"
            }
            sb.append(rc(x0 + c * cw, y0 + r * ch, cw, ch, fill, ST, 1.4f, 4f))
        }
        if (goal > 0) sb.append(tx(180f, y0 + h + 26f, "هدف رنگ‌آمیزی: ${pFa(goal)} خانه", ACC, "middle", bold = true, size = 12))
        sb.append(tx(180f, 258f, "روی هر خانه کلیک کنید تا رنگ شود یا پاک شود", MU, "middle", size = 10))
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

    /** V327 / V335.8 — هجاهای کلمه: طراح کلمه را می‌نویسد و دانش‌آموز خودش کادرهای بخش را با دکمهٔ «+» اضافه و پر می‌کند. */
    private fun syllableChart(spec: FigureSpec): String {
        val rawWord = spec.xStr("word", "مدرسه").ifBlank { "مدرسه" }
        val mode = spec.xStr("mode", "ask").lowercase()
        val sb = StringBuilder()
        if (mode == "show") {
            val list = rawWord.split(Regex("[-–—،,]")).map { it.trim() }.filter { it.isNotEmpty() }.take(6)
                .ifEmpty { listOf(rawWord) }
            val n = list.size
            val w = min(72f, 300f / n)
            val x0 = 180f + (n * (w + 8f)) / 2f
            sb.append(tx(180f, 38f, "بخش‌های کلمهٔ «${rawWord.replace(Regex("[-–—]"), "")}»", ST, "middle", bold = true, size = 13))
            list.forEachIndexed { i, syl ->
                val bx = x0 - (i + 1) * (w + 8f)
                sb.append(rc(bx, 74f, w, 52f, "rgba(39,196,168,.16)", "#1f8a78", 1.6f, 10f))
                sb.append(tx(bx + w / 2f, 108f, syl, ST, "middle", bold = true, size = 18))
                sb.append(cls(bx + w / 2f, 146f, 4f, "#1f8a78", "none", 0f))
            }
            sb.append(tx(180f, 176f, "تعداد بخش: ${pFa(n)}", ACC, "middle", bold = true, size = 12))
            return sb.toString()
        }

        // حالت پیش‌فرض آزمون (ask): نمایش کلمهٔ یکپارچه بدون لو دادن تعداد بخش‌ها
        val cleanWord = rawWord.replace(Regex("[-–—]"), "").trim().ifBlank { "مدرسه" }
        sb.append(tx(180f, 42f, "کلمهٔ زیر را به بخش‌های (هجاهای) آن تقسیم کن:", ST, "middle", bold = true, size = 13))
        sb.append(rc(66f, 68f, 228f, 76f, "#eef2ff", ACC, 2.2f, 14f))
        val wFont = if (cleanWord.length > 10) 20 else 26
        sb.append(tx(180f, 116f, cleanWord, ST, "middle", bold = true, size = wFont))
        // راهنمای تصویری کادرهای بخش‌بندی با دکمهٔ +
        sb.append(rc(86f, 168f, 68f, 38f, "#ffffff", "#94a3b8", 1.4f, 8f))
        sb.append(tx(120f, 192f, "بخش ...", "#94a3b8", "middle", size = 11))
        sb.append(rc(162f, 168f, 38f, 38f, "rgba(39,196,168,.18)", "#1f8a78", 1.6f, 8f))
        sb.append(tx(181f, 194f, "+", "#1f8a78", "middle", bold = true, size = 20))
        sb.append(tx(248f, 191f, "افزودن کادر بخش", "#1f8a78", "middle", bold = true, size = 11))
        sb.append(tx(180f, 240f, "با دکمهٔ «+» در کادر پاسخ، به تعداد بخش‌ها خانه بساز و هر بخش را بنویس", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 / V335.8 — آهنربا: جذب (قطب‌های ناهم‌نام روبه‌رو) و دفع (قطب‌های هم‌نام روبه‌رو). */
    private fun magnetSpec(spec: FigureSpec): String {
        val mode = spec.xStr("mode", "attract").lowercase()
        val repel = mode == "repel" || mode == "repel_s"
        val repelSouth = mode == "repel_s"
        val sb = StringBuilder()
        sb.append(tx(180f, 36f, if (repel) "دو قطب هم‌نام — دفع" else "دو قطب ناهم‌نام — جذب", ST, "middle", bold = true, size = 13))
        val leftCx = 88f
        val rightCx = 272f
        val y = 136f
        fun drawBarMagnet(cx: Float, leftPole: String, rightPole: String) {
            val wHalf = 46f
            val h = 48f
            val lCol = if (leftPole == "N") "#e53935" else "#1e88e5"
            val rCol = if (rightPole == "N") "#e53935" else "#1e88e5"
            sb.append(rc(cx - wHalf, y - h / 2f, wHalf, h, lCol, ST, 1.6f, 4f))
            sb.append(rc(cx, y - h / 2f, wHalf, h, rCol, ST, 1.6f, 4f))
            sb.append(rc(cx - wHalf, y - h / 2f, wHalf * 2f, h, "none", ST, 1.8f, 5f))
            sb.append(tx(cx - wHalf / 2f, y + 6f, leftPole, "#ffffff", "middle", bold = true, size = 16))
            sb.append(tx(cx + wHalf / 2f, y + 6f, rightPole, "#ffffff", "middle", bold = true, size = 16))
        }
        if (!repel) {
            // جذب: قطب N آهنربای چپ روبه‌روی قطب S آهنربای راست
            drawBarMagnet(leftCx, "S", "N")
            drawBarMagnet(rightCx, "S", "N")
            sb.append(arrow(leftCx + 52f, y, leftCx + 82f, y, "#1f8a78", 2.6f, 9f))
            sb.append(arrow(rightCx - 52f, y, rightCx - 82f, y, "#1f8a78", 2.6f, 9f))
            sb.append(tx(180f, y - 36f, "ربایش (جذب)", "#1f8a78", "middle", bold = true, size = 12))
        } else {
            // دفع: دو قطب هم‌نام روبه‌روی یکدیگر (N روبه‌روی N یا S روبه‌روی S)
            if (repelSouth) {
                drawBarMagnet(leftCx, "N", "S")
                drawBarMagnet(rightCx, "S", "N")
            } else {
                drawBarMagnet(leftCx, "S", "N")
                drawBarMagnet(rightCx, "N", "S")
            }
            sb.append(arrow(leftCx + 80f, y, leftCx + 50f, y, PT, 2.6f, 9f))
            sb.append(arrow(rightCx - 80f, y, rightCx - 50f, y, PT, 2.6f, 9f))
            sb.append(tx(180f, y - 36f, "رانش (دفع)", PT, "middle", bold = true, size = 12))
        }
        sb.append(tx(180f, 236f, "قطب‌های هم‌نام یکدیگر را می‌رانند و قطب‌های ناهم‌نام همدیگر را می‌ربایند", MU, "middle", size = 10))
        return sb.toString()
    }

    /** V327 / V335.8 — حواس پنج‌گانه با آیکن‌های برداری تمیز و بدون تداخل خطوط با نوشته‌ها. */
    private fun fiveSenses(spec: FigureSpec): String {
        val mode = spec.xStr("mode", "full").lowercase()
        data class SenseNode(val sense: String, val organ: String, val cx: Float, val cy: Float, val col: String)
        val nodes = listOf(
            SenseNode("بینایی", "چشم", 180f, 52f, "#2563eb"),
            SenseNode("شنوایی", "گوش", 66f, 108f, "#7c3aed"),
            SenseNode("بویایی", "بینی", 294f, 108f, "#059669"),
            SenseNode("چشایی", "زبان", 92f, 210f, "#e11d48"),
            SenseNode("لامسه", "پوست دست", 268f, 210f, "#d97706")
        )
        val hubX = 180f; val hubY = 136f; val hubR = 30f; val nodeR = 22f
        val sb = StringBuilder()

        // ۱) پرتوهای ارتباطی فقط در فضای بیرون دایره‌ها رسم می‌شوند تا از روی متن یا آیکن رد نشوند
        nodes.forEach { nd ->
            val dx = nd.cx - hubX
            val dy = nd.cy - hubY
            val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            val ux = dx / dist
            val uy = dy / dist
            sb.append(ln(hubX + ux * (hubR + 3f), hubY + uy * (hubR + 3f), nd.cx - ux * (nodeR + 4f), nd.cy - uy * (nodeR + 4f), "#cbd5e1", 1.8f, "4 3"))
        }

        // ۲) دایرهٔ مرکزی توپر سفید با کادر بنفش
        sb.append(cls(hubX, hubY, hubR, "#eef2ff", ACC, 2.2f))
        sb.append(tx(hubX, hubY - 3f, "حواس", ACC, "middle", bold = true, size = 12))
        sb.append(tx(hubX, hubY + 13f, "پنج‌گانه", ST, "middle", bold = true, size = 10))

        // ۳) هر گره با آیکن برداری واضح اندام + پلاک برچسب بدون هیچ تداخلی
        nodes.forEachIndexed { idx, nd ->
            sb.append(rc(nd.cx - 21f, nd.cy - 21f, 42f, 42f, "#ffffff", nd.col, 2f, 14f))
            when (idx) {
                0 -> { // چشم
                    sb.append(path("M ${f(nd.cx - 13f)} ${f(nd.cy)} Q ${f(nd.cx)} ${f(nd.cy - 10f)} ${f(nd.cx + 13f)} ${f(nd.cy)} Q ${f(nd.cx)} ${f(nd.cy + 10f)} ${f(nd.cx - 13f)} ${f(nd.cy)} Z", "#eff6ff", nd.col, 1.5f))
                    sb.append(cls(nd.cx, nd.cy, 4.2f, nd.col, "none", 0f))
                }
                1 -> { // گوش
                    sb.append(path("M ${f(nd.cx - 3f)} ${f(nd.cy - 10f)} C ${f(nd.cx + 9f)} ${f(nd.cy - 12f)} ${f(nd.cx + 10f)} ${f(nd.cy + 4f)} ${f(nd.cx + 2f)} ${f(nd.cy + 9f)} C ${f(nd.cx - 2f)} ${f(nd.cy + 11f)} ${f(nd.cx - 6f)} ${f(nd.cy + 8f)} ${f(nd.cx - 5f)} ${f(nd.cy + 4f)}", "none", nd.col, 2f))
                    sb.append(path("M ${f(nd.cx - 1f)} ${f(nd.cy - 4f)} Q ${f(nd.cx + 4f)} ${f(nd.cy)} ${f(nd.cx)} ${f(nd.cy + 4f)}", "none", nd.col, 1.5f))
                }
                2 -> { // بینی
                    sb.append(path("M ${f(nd.cx - 1f)} ${f(nd.cy - 11f)} L ${f(nd.cx - 7f)} ${f(nd.cy + 5f)} Q ${f(nd.cx)} ${f(nd.cy + 10f)} ${f(nd.cx + 7f)} ${f(nd.cy + 5f)}", "none", nd.col, 2f))
                }
                3 -> { // زبان
                    sb.append(ln(nd.cx - 10f, nd.cy - 4f, nd.cx + 10f, nd.cy - 4f, ST, 1.6f))
                    sb.append(path("M ${f(nd.cx - 7f)} ${f(nd.cy - 4f)} L ${f(nd.cx - 7f)} ${f(nd.cy + 4f)} A 7 7 0 0 0 ${f(nd.cx + 7f)} ${f(nd.cy + 4f)} L ${f(nd.cx + 7f)} ${f(nd.cy - 4f)} Z", "#fecdd3", nd.col, 1.6f))
                    sb.append(ln(nd.cx, nd.cy - 3f, nd.cx, nd.cy + 6f, nd.col, 1.2f))
                }
                else -> { // دست (پوست / لامسه)
                    sb.append(rc(nd.cx - 7f, nd.cy - 2f, 14f, 11f, "#fef3c7", nd.col, 1.5f, 4f))
                    for (fx in -5..5 step 3) {
                        sb.append(ln(nd.cx + fx, nd.cy - 2f, nd.cx + fx, nd.cy - 10f, nd.col, 1.8f))
                    }
                }
            }
            val labelText = when (mode) {
                "blank_sense" -> "؟ — ${nd.organ}"
                "blank_organ" -> "${nd.sense} — ؟"
                else -> "${nd.sense} (${nd.organ})"
            }
            val ly = if (idx == 0) nd.cy - nodeR - 8f else nd.cy + nodeR + 14f
            sb.append(tx(nd.cx, ly, labelText, if ("؟" in labelText) PT else ST, "middle", bold = true, size = 10))
        }
        sb.append(tx(180f, 268f, "حواس پنج‌گانه و اندام‌های حسی بدن", MU, "middle", size = 10))
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

    private fun rcDash(x: Float, y: Float, w: Float, h: Float, fill: String, stroke: String, sw: Float, rx: Float = 0f, dash: String = "4 2"): String =
        "<rect x=\"${f(x)}\" y=\"${f(y)}\" width=\"${f(w)}\" height=\"${f(h)}\"" +
            (if (rx > 0f) " rx=\"${f(rx)}\"" else "") +
            " fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"${f(sw)}\" stroke-dasharray=\"$dash\"/>"

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

    private fun tx(x: Float, y: Float, s: String, color: String, anchor: String, bold: Boolean = false, size: Int = 11, canvasW: Float = 360f): String {
        if (s.isBlank()) return ""
        val weight = if (bold) " font-weight=\"700\"" else ""
        // V322 — جاگیری خودکار برچسب: اگر متن با اندازهٔ خواسته‌شده از بوم بیرون بزند، فونت تا ۷ کوچک و در
        // نهایت با «…» کوتاه می‌شود؛ مختصات هم داخل بوم مهار می‌شود تا هیچ برچسبی بریده نشود.
        val fit = fitLabelOn(x, s, anchor, size, canvasW)
        return "<text x=\"${f(fit.first)}\" y=\"${f(y)}\" direction=\"ltr\" font-family=\"sans-serif\" font-size=\"${fit.third}\"$weight fill=\"$color\" text-anchor=\"$anchor\">${esc(FigureDigits.apply(fit.second))}</text>"
    }

    /** (x نهایی، متن نهایی، اندازهٔ فونت) برای جا شدن برچسب در بوم. */
    private fun fitLabel(x: Float, s: String, anchor: String, size: Int): Triple<Float, String, Int> =
        fitLabelOn(x, s, anchor, size, 360f)

    private fun fitLabelOn(x: Float, s: String, anchor: String, size: Int, canvasW: Float = 360f): Triple<Float, String, Int> {
        val maxW = canvasW - 16f
        val rightLim = canvasW - 3f
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
        val hi = when (anchor) { "middle" -> rightLim - half; "end" -> rightLim; else -> rightLim - est }
        val xx = if (lo <= hi) x.coerceIn(lo, hi) else x.coerceIn(3f, rightLim)
        return Triple(xx, txt, fs)
    }
}
