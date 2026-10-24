package ir.exam.app.core.figure

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * V335.8 — منطق مشترک تعاملی شکل‌های دبستان (تشخیص خانهٔ کلیک‌شده روی بوم SVG، پنجره‌های انتخاب مقدار،
 * خانه‌های خالی بدون علامت سؤال برای معلم، و ورود پاسخ روی شکل برای دانش‌آموز در جلسهٔ آزمون).
 */
object PrimaryInteractiveFigureHelper {

    data class SawChoice(val code: String, val label: String, val badge: String)

    data class PatternPreset(val seq: String, val label: String)

    data class CellHit(
        val key: String,
        val title: String,
        val defaultVal: String,
        val currentVal: String,
        val isEmpty: Boolean,
        val studentVal: String = ""
    )

    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    fun toFa(s: String): String = buildString(s.length) {
        for (ch in s) {
            if (ch in '0'..'9') append(FA_DIGITS[ch - '0']) else append(ch)
        }
    }

    fun hasXKey(spec: FigureSpec, key: String): Boolean =
        (spec.raw["X"] as? JsonObject)?.containsKey(key) == true

    /** به‌روزرسانی مقادیر داخل `X` در `FigureSpec` (مقدار `null` کلید را حذف می‌کند). */
    fun withX(spec: FigureSpec, vararg updates: Pair<String, String?>): FigureSpec {
        val curX = (spec.raw["X"] as? JsonObject)?.toMutableMap() ?: LinkedHashMap<String, JsonElement>()
        for ((k, v) in updates) {
            if (v == null) curX.remove(k) else curX[k] = JsonPrimitive(v)
        }
        val nextRaw = spec.raw.toMutableMap()
        nextRaw["X"] = JsonObject(curX)
        return FigureSpec(JsonObject(nextRaw))
    }

    val PATTERN_PRESETS: List<PatternPreset> = listOf(
        PatternPreset("●,▲,●,▲,●,?", "تکراری دوتایی شکلی (● ، ▲ ، ● ، ▲ ، ؟)"),
        PatternPreset("●,●,▲,●,●,?", "تکراری سه‌تایی (● ، ● ، ▲ ، ● ، ● ، ؟)"),
        PatternPreset("●,▲,■,●,▲,?", "تکراری سه‌شکلی (● ، ▲ ، ■ ، ● ، ▲ ، ؟)"),
        PatternPreset("★,★,●,▲,★,★,●,?", "تکراری چهارتایی (★ ، ★ ، ● ، ▲ ، ... ، ؟)"),
        PatternPreset("1,2,1,2,1,?", "رنگی یکی‌درمیان (قرمز، آبی، قرمز، آبی، ؟)"),
        PatternPreset("1,2,3,1,2,?", "رنگی سه‌تایی (قرمز، آبی، سبز، ... ، ؟)"),
        PatternPreset("1,1,3,4,1,1,3,?", "رنگی چهارتایی (قرمز، قرمز، سبز، زرد، ... ، ؟)"),
        PatternPreset("2,4,6,8,10,?", "عددی زوج (+۲): ۲ ، ۴ ، ۶ ، ۸ ، ۱۰ ، ؟"),
        PatternPreset("1,3,5,7,9,?", "عددی فرد (+۲): ۱ ، ۳ ، ۵ ، ۷ ، ۹ ، ؟"),
        PatternPreset("5,10,15,20,25,?", "عددی مضرب ۵ (+۵): ۵ ، ۱۰ ، ۱۵ ، ۲۰ ، ۲۵ ، ؟"),
        PatternPreset("3,6,9,12,15,?", "عددی مضرب ۳ (+۳): ۳ ، ۶ ، ۹ ، ۱۲ ، ۱۵ ، ؟"),
        PatternPreset("20,17,14,11,8,?", "عددی نزولی (−۳): ۲۰ ، ۱۷ ، ۱۴ ، ۱۱ ، ۸ ، ؟"),
        PatternPreset("2,4,8,16,32,?", "عددی ضربی (×۲): ۲ ، ۴ ، ۸ ، ۱۶ ، ۳۲ ، ؟"),
        PatternPreset("1,2,4,7,11,?", "عددی افزایشی متغیر (+۱،+۲،+۳،+۴): ۱ ، ۲ ، ۴ ، ۷ ، ۱۱ ، ؟"),
        PatternPreset("1,3,6,10,15,?", "رشدشوندهٔ مثلثی: ۱ ، ۳ ، ۶ ، ۱۰ ، ۱۵ ، ؟"),
        PatternPreset("1,4,9,16,?", "رشدشوندهٔ مربعی: ۱ ، ۴ ، ۹ ، ۱۶ ، ؟"),
        PatternPreset("↑,→,↓,←,↑,?", "جهتی چرخشی: ↑ ، → ، ↓ ، ← ، ↑ ، ؟"),
        PatternPreset("۱●,۲▲,۳■,۴●,?", "ترکیبی عدد و شکل: ۱● ، ۲▲ ، ۳■ ، ۴● ، ؟")
    )

    val PATTERN_TOKENS: List<Pair<String, String>> = listOf(
        "?" to "؟ (جای سؤال)",
        "_" to "⬜ خانهٔ خالی",
        "●" to "● دایره (قرمز)",
        "▲" to "▲ مثلث (آبی)",
        "■" to "■ مربع (سبز)",
        "★" to "★ ستاره (زرد)",
        "◆" to "◆ لوزی (بنفش)",
        "♥" to "♥ قلب (نارنجی)",
        "⬢" to "⬢ شش‌ضلعی",
        "✿" to "✿ گل",
        "↑" to "↑ بالا",
        "→" to "→ راست",
        "↓" to "↓ پایین",
        "←" to "← چپ"
    )

    private val SAW_META = listOf(
        Triple("1", "●", "قرمز"),
        Triple("2", "▲", "آبی"),
        Triple("3", "■", "سبز"),
        Triple("4", "★", "زرد"),
        Triple("5", "◆", "بنفش"),
        Triple("6", "♥", "نارنجی"),
        Triple("7", "⬢", "فیروزه‌ای"),
        Triple("8", "✿", "صورتی"),
        Triple("9", "▼", "قهوه‌ای")
    )

    // =========================================================================
    // ۵) جدول شگفت‌انگیز و سودوکو (axsaw)
    // =========================================================================

    fun parseSawGrid(spec: FigureSpec): Pair<Int, MutableList<MutableList<String>>> {
        val rawVals = spec.xStr("vals", "1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1")
        val parsed = rawVals.split(';', '؛')
            .map { row -> row.split(',', '،').map { normSawToken(it) }.toMutableList() }
            .filter { it.isNotEmpty() }
        val explicitSize = spec.xNum("size", 0f).toInt()
        val n = (if (explicitSize in 3..9) explicitSize else parsed.size.coerceIn(3, 9)).coerceIn(3, 9)
        val rows = if (parsed.size == n && parsed.all { it.size >= n }) {
            MutableList(n) { r -> MutableList(n) { c -> parsed[r][c] } }
        } else {
            PrimaryMathSvgRenderer.defaultSudokuRows(n).map { it.toMutableList() }.toMutableList()
        }
        return n to rows
    }

    private fun normSawToken(raw: String): String {
        val c = faEnDigits(raw.trim())
        if (c == "_" || c == "-" || c == "empty") return "_"
        if (c.isEmpty() || c == "?" || c == "؟") return "?"
        return when {
            c in "1".."9" && c.length == 1 -> c
            "قرمز" in c || c == "●" -> "1"
            "آبی" in c || c == "▲" -> "2"
            "سبز" in c || c == "■" -> "3"
            "زرد" in c || c == "★" -> "4"
            "بنفش" in c || c == "◆" -> "5"
            "نارنجی" in c || c == "♥" -> "6"
            "فیروزه" in c || c == "⬢" -> "7"
            "صورتی" in c || c == "✿" -> "8"
            "قهوه" in c || c == "▼" -> "9"
            else -> c
        }
    }

    fun hitSawCell(spec: FigureSpec, sx: Float, sy: Float): Pair<Int, Int>? {
        val (n, _) = parseSawGrid(spec)
        val mode = spec.xStr("mode", "shape").lowercase()
        val showLegend = mode == "color" || mode == "color_shape"
        val maxGridPx = if (showLegend) 196f else 216f
        val cs = min(52f, maxGridPx / n)
        val gw = cs * n
        val x0 = (360f - gw) / 2f
        val y0 = (if (showLegend) 24f else 28f) + (maxGridPx - gw) / 2f
        if (sx < x0 || sy < y0) return null
        val c = ((sx - x0) / cs).toInt()
        val r = ((sy - y0) / cs).toInt()
        return if (r in 0 until n && c in 0 until n) (r to c) else null
    }

    fun sawChoices(spec: FigureSpec): List<SawChoice> {
        val (n, _) = parseSawGrid(spec)
        val mode = spec.xStr("mode", "shape").lowercase()
        val items = (0 until n).map { idx ->
            val (code, sym, colorFa) = SAW_META[idx]
            val faNum = toFa(code)
            when (mode) {
                "num" -> SawChoice(code, "عدد $faNum", faNum)
                "color" -> SawChoice(code, colorFa, "■ $colorFa")
                "color_shape" -> SawChoice(code, "$sym ($colorFa)", sym)
                else -> SawChoice(code, "شکل $sym", sym)
            }
        }
        return items + listOf(
            SawChoice("?", "علامت سؤال (؟)", "؟"),
            SawChoice("_", "خانهٔ خالی (بدون ؟)", "⬜")
        )
    }

    fun setSawCell(spec: FigureSpec, row: Int, col: Int, newCode: String): FigureSpec {
        val (n, rows) = parseSawGrid(spec)
        if (row !in 0 until n || col !in 0 until n) return spec
        rows[row][col] = newCode.ifBlank { "?" }
        val encoded = rows.joinToString(";") { it.joinToString(",") }
        return withX(spec, "size" to n.toString(), "vals" to encoded)
    }

    // =========================================================================
    // ۶) الگویابی رنگی/شکلی/عددی (axpattern)
    // =========================================================================

    fun parsePatternItems(spec: FigureSpec): MutableList<String> =
        spec.xList("seq", "●,▲,●,▲,●,?").map { it.trim() }.filter { it.isNotEmpty() }.take(12)
            .ifEmpty { listOf("●", "▲", "●", "▲", "?") }.toMutableList()

    fun hitPatternSlot(spec: FigureSpec, sx: Float, sy: Float): Int? {
        val items = parsePatternItems(spec)
        val n = items.size
        if (n == 0) return null
        val mode = spec.xStr("mode", "shape").lowercase()
        val gap = when {
            n <= 6 -> 8f
            n <= 8 -> 6f
            else -> 4f
        }
        val cw = min(if (mode == "growing") 54f else 44f, (324f - (n - 1) * gap) / n)
        val totalW = n * cw + (n - 1) * gap
        val x0 = (360f - totalW) / 2f
        val yTop = if (mode == "growing") 58f else 96f
        val yBot = if (mode == "growing") 206f else 186f
        if (sy !in yTop..yBot) return null
        for (i in 0 until n) {
            val slotX = x0 + i * (cw + gap)
            if (sx in (slotX - 2f)..(slotX + cw + 2f)) return i
        }
        return null
    }

    fun setPatternSlot(spec: FigureSpec, index: Int, token: String): FigureSpec {
        val items = parsePatternItems(spec)
        if (index !in items.indices) return spec
        items[index] = token.trim().ifEmpty { "?" }
        return withX(spec, "seq" to items.joinToString(","))
    }

    fun appendPatternSlot(spec: FigureSpec, token: String = "?"): FigureSpec {
        val items = parsePatternItems(spec)
        if (items.size >= 12) return spec
        items.add(token)
        return withX(spec, "seq" to items.joinToString(","))
    }

    fun removeLastPatternSlot(spec: FigureSpec): FigureSpec {
        val items = parsePatternItems(spec)
        if (items.size <= 2) return spec
        items.removeAt(items.lastIndex)
        return withX(spec, "seq" to items.joinToString(","))
    }

    // =========================================================================
    // ۷) تقارن شطرنجی (axsym) — کلیک روی خانه‌ها توسط معلم و دانش‌آموز
    // =========================================================================

    fun hitSymCell(sx: Float, sy: Float): Pair<Int, Int>? {
        val cell = 26f
        val n = 8
        val gw = cell * n
        val x0 = (360f - gw) / 2f
        val y0 = 32f
        if (sx < x0 || sy < y0 || sx >= x0 + gw || sy >= y0 + gw) return null
        val c = ((sx - x0) / cell).toInt().coerceIn(0, 7)
        val r = ((sy - y0) / cell).toInt().coerceIn(0, 7)
        return r to c
    }

    private fun parseSymPairs(spec: FigureSpec): MutableSet<Pair<Int, Int>> {
        val raw = spec.xStr("cells", "")
        if (raw.isBlank() && !hasXKey(spec, "cells")) {
            return linkedSetOf(1 to 1, 2 to 1, 2 to 2, 3 to 2)
        }
        val out = linkedSetOf<Pair<Int, Int>>()
        raw.split(';', '؛').forEach { tok ->
            val p = tok.split(',', '،').mapNotNull { faEnDigits(it.trim()).toIntOrNull() }
            if (p.size >= 2) out += (p[0].coerceIn(1, 8) to p[1].coerceIn(1, 8))
        }
        return out
    }

    /** تغییر وضعیت خانه در حالت طراح سؤال (معلم): هر کدام از ۶۴ خانه با کلیک رنگ یا پاک می‌شود. */
    fun toggleTeacherSymCell(spec: FigureSpec, r0: Int, c0: Int): FigureSpec {
        val axis = spec.xStr("axis", "v").lowercase()
        val mirror = spec.xNum("mirror", 1f).toInt() != 0
        val pairs = parseSymPairs(spec)
        val pairDirect = if (axis == "h") {
            val d1 = if (r0 <= 3) 4 - r0 else r0 + 1
            (c0 + 1) to d1
        } else {
            val d1 = if (c0 <= 3) 4 - c0 else c0 + 1
            (r0 + 1) to d1
        }
        val pairMirror = if (axis == "h") {
            val rRef = 7 - r0
            val dRef = if (rRef <= 3) 4 - rRef else rRef + 1
            (c0 + 1) to dRef
        } else {
            val cRef = 7 - c0
            val dRef = if (cRef <= 3) 4 - cRef else cRef + 1
            (r0 + 1) to dRef
        }
        when {
            pairDirect in pairs -> pairs.remove(pairDirect)
            mirror && pairMirror in pairs -> pairs.remove(pairMirror)
            else -> pairs.add(pairDirect)
        }
        val encoded = pairs.joinToString(";") { "${it.first},${it.second}" }
        return withX(spec, "cells" to encoded)
    }

    /** مجموعه‌ای از مختصات ۰..۷ خانه‌های اصلی شکل که معلم داده است (تا دانش‌آموز آن‌ها را پاک نکند). */
    fun givenSymCoords(spec: FigureSpec): Set<Pair<Int, Int>> {
        val axis = spec.xStr("axis", "v").lowercase()
        return parseSymPairs(spec).map { (r1, d1) ->
            if (axis == "h") {
                val rMain = if (d1 <= 4) (4 - d1).coerceIn(0, 3) else (d1 - 1).coerceIn(4, 7)
                rMain to (r1 - 1).coerceIn(0, 7)
            } else {
                val cMain = if (d1 <= 4) (4 - d1).coerceIn(0, 3) else (d1 - 1).coerceIn(4, 7)
                (r1 - 1).coerceIn(0, 7) to cMain
            }
        }.toSet()
    }

    fun toggleStudentSymCell(spec: FigureSpec, currentAnswerRaw: String, r0: Int, c0: Int): String {
        val given = givenSymCoords(spec)
        if ((r0 to c0) in given) return currentAnswerRaw
        val cur = parseCoordSet1Based(currentAnswerRaw, 8, 8).toMutableSet()
        val key = (r0 + 1) to (c0 + 1)
        if (!cur.add(key)) cur.remove(key)
        return cur.sortedWith(compareBy({ it.first }, { it.second })).joinToString(";") { "${it.first},${it.second}" }
    }

    // =========================================================================
    // ۱۴) شبکهٔ رنگ‌آمیزی (axpaintgrid) — کلیک روی خانه‌ها توسط معلم و دانش‌آموز
    // =========================================================================

    fun hitPaintCell(spec: FigureSpec, sx: Float, sy: Float): Pair<Int, Int>? {
        val rows = faEnDigits(spec.xStr("rows", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val cols = faEnDigits(spec.xStr("cols", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val w = min(300f, cols * 42f)
        val h = min(150f, rows * 34f)
        val cw = w / cols
        val ch = h / rows
        val x0 = (360f - w) / 2f
        val y0 = 62f + (150f - h) / 2f
        if (sx < x0 || sy < y0 || sx >= x0 + w || sy >= y0 + h) return null
        val c1 = ((sx - x0) / cw).toInt().coerceIn(0, cols - 1) + 1
        val r1 = ((sy - y0) / ch).toInt().coerceIn(0, rows - 1) + 1
        return r1 to c1
    }

    fun parseCoordSet1Based(raw: String, maxRows: Int = 8, maxCols: Int = 8): Set<Pair<Int, Int>> =
        raw.split(';', '؛').mapNotNull { chunk ->
            val a = chunk.split(',', '،')
            val r = a.getOrNull(0)?.let { faEnDigits(it.trim()).toIntOrNull() }
            val c = a.getOrNull(1)?.let { faEnDigits(it.trim()).toIntOrNull() }
            if (r != null && c != null && r in 1..maxRows && c in 1..maxCols) (r to c) else null
        }.toSet()

    fun togglePaintCell(spec: FigureSpec, r1: Int, c1: Int): FigureSpec {
        val rows = faEnDigits(spec.xStr("rows", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val cols = faEnDigits(spec.xStr("cols", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val cur = parseCoordSet1Based(spec.xStr("cells", ""), rows, cols).toMutableSet()
        val key = r1.coerceIn(1, rows) to c1.coerceIn(1, cols)
        if (!cur.add(key)) cur.remove(key)
        val encoded = cur.sortedWith(compareBy({ it.first }, { it.second })).joinToString(";") { "${it.first},${it.second}" }
        return withX(spec, "cells" to encoded)
    }

    fun toggleStudentPaintCell(spec: FigureSpec, currentAnswerRaw: String, r1: Int, c1: Int): String {
        val rows = faEnDigits(spec.xStr("rows", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val cols = faEnDigits(spec.xStr("cols", "4")).toFloatOrNull()?.toInt()?.coerceIn(1, 8) ?: 4
        val given = parseCoordSet1Based(spec.xStr("cells", ""), rows, cols)
        val key = r1.coerceIn(1, rows) to c1.coerceIn(1, cols)
        if (key in given) return currentAnswerRaw
        val cur = parseCoordSet1Based(currentAnswerRaw, rows, cols).toMutableSet()
        if (!cur.add(key)) cur.remove(key)
        return cur.sortedWith(compareBy({ it.first }, { it.second })).joinToString(";") { "${it.first},${it.second}" }
    }

    // =========================================================================
    // ۱۱) خانه‌ها و گره‌های تعاملی (دبستان، متوسطه و کنکور)
    // =========================================================================

    val KEYED_CELL_TYPES: Set<String> = setOf(
        "axcalendar", "axmultgrid", "axmulwheel", "axtower",
        "axdicefold", "axunitconv", "axprobtree", "axorbital", "axpunnett", "axkenken"
    )

    /** انتخاب‌های سریع پیشنهادی برای پاپ‌آپ معلم و دانش‌آموز (مثلاً اسپین الکترون یا نماد شجره‌نامه). */
    fun quickChipsForCell(spec: FigureSpec, key: String): List<Pair<String, String>> = when (spec.type) {
        "axorbital" -> {
            if (spec.xStr("mode", "box").lowercase() == "lewis") {
                if (key.startsWith("side_")) listOf("2" to "۲ نقطه (جفت)", "1" to "۱ نقطه (تک)", "0" to "۰ (بدون نقطه)")
                else emptyList()
            } else if (key != "cfg") {
                listOf("↑↓" to "↑↓ (جفت)", "↑" to "↑ (تک بالا)", "↓" to "↓ (تک پایین)")
            } else emptyList()
        }
        "axpunnett" -> {
            if (spec.xStr("kind", "punnett").lowercase() == "pedigree") {
                listOf(
                    "M" to "□ مرد سالم", "M*" to "■ مرد مبتلا", "Mc" to "◧ مرد ناقل",
                    "F" to "○ زن سالم", "F*" to "● زن مبتلا", "Fc" to "◐ زن ناقل"
                )
            } else if (key.startsWith("c_")) {
                listOf("AA" to "AA", "Aa" to "Aa", "aa" to "aa")
            } else emptyList()
        }
        "axdicefold" -> listOf("1" to "۱", "2" to "۲", "3" to "۳", "4" to "۴", "5" to "۵", "6" to "۶")
        "axprobtree" -> {
            if ((key.startsWith("r_") || key.startsWith("e_") || key.startsWith("out_")) && spec.xStr("mode", "prob").lowercase() != "count") {
                listOf("1/2" to "1/2", "1/4" to "1/4", "1/8" to "1/8", "25%" to "25%", "50%" to "50%")
            } else emptyList()
        }
        else -> emptyList()
    }

    fun regionsFor(spec: FigureSpec): List<AdvancedMathSvgRenderer.InteractiveRegion> = when (spec.type) {
        "axunitconv" -> NextMathSvgRenderer.interactiveRegions(spec)
        "axdicefold", "axprobtree", "axorbital", "axpunnett", "axkenken" -> AdvancedMathSvgRenderer.interactiveRegions(spec)
        else -> emptyList()
    }

    private fun hitRegionCell(spec: FigureSpec, sx: Float, sy: Float): CellHit? {
        val regs = regionsFor(spec)
        if (regs.isEmpty()) return null
        val matched = regs.filter { sx in it.x..(it.x + it.w) && sy in it.y..(it.y + it.h) }
            .minByOrNull { it.w * it.h } ?: return null
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov")
        val ansMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ans")
        val isEmpty = matched.key in emptySet || matched.defaultEmpty
        return CellHit(
            key = matched.key,
            title = matched.title,
            defaultVal = matched.defaultVal,
            currentVal = ovMap[matched.key]?.trim() ?: matched.defaultVal,
            isEmpty = isEmpty,
            studentVal = ansMap[matched.key]?.trim().orEmpty()
        )
    }

    fun hitEditableNumberCell(spec: FigureSpec, sx: Float, sy: Float): CellHit? = when (spec.type) {
        "axcalendar" -> hitCalendarCell(spec, sx, sy)
        "axmultgrid" -> hitMultGridCell(spec, sx, sy)
        "axmulwheel" -> hitMulWheelCell(spec, sx, sy)
        "axtower" -> hitTowerCell(spec, sx, sy)
        "axdicefold", "axunitconv", "axprobtree", "axorbital", "axpunnett", "axkenken" -> hitRegionCell(spec, sx, sy)
        else -> null
    }

    /** فهرست مرتب همهٔ خانه‌های خالی شکل (برای ساخت پاسخ یکپارچهٔ دانش‌آموز در تصحیح خودکار). */
    fun listEmptyCellKeys(spec: FigureSpec): List<String> = when (spec.type) {
        "axdicefold", "axunitconv", "axprobtree", "axorbital", "axpunnett", "axkenken" -> {
            val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
            regionsFor(spec).filter { it.key in emptySet || it.defaultEmpty }.map { it.key }
        }
        "axcalendar" -> {
            val days = spec.xNum("days", 30f).toInt().coerceIn(20, 31)
            val blank = spec.xStr("blank", "0") == "1"
            val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
            (1..days).map { it.toString() }.filter { k -> k in emptySet || (blank && k.toInt() % 5 == 0) }
        }
        "axmultgrid" -> {
            val rows = parseIntList(spec.xStr("rows", "2,4,7"), listOf(2, 4, 7)).take(4)
            val cols = parseIntList(spec.xStr("cols", "3,5,8"), listOf(3, 5, 8)).take(4)
            val blank = spec.xNum("blank", 0f).toInt().coerceIn(0, 2)
            val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
            val keys = mutableListOf<String>()
            for (c in cols.indices) {
                val ck = "c$c"
                if (ck in emptySet || blank == 2) keys += ck
            }
            for (r in rows.indices) {
                val rk = "r$r"
                if (rk in emptySet || blank == 2) keys += rk
                for (c in cols.indices) {
                    val pk = "${r}_$c"
                    if (pk in emptySet || (blank == 1 && (r + c) % 2 == 1)) keys += pk
                }
            }
            keys
        }
        "axmulwheel" -> {
            val n = spec.xNum("n", 10f).toInt().coerceIn(3, 12)
            val hide = spec.xStr("hide", "0") == "1"
            val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
            val keys = mutableListOf<String>()
            if ("center" in emptySet) keys += "center"
            for (j in 0 until n) {
                val k = "s_$j"
                if (k in emptySet || "$j" in emptySet || (hide && j % 3 == 2)) keys += k
            }
            keys
        }
        "axtower" -> {
            val base = parseIntList(spec.xStr("base", "1,2,3,4"), listOf(1, 2, 3, 4)).take(6)
            val blank = spec.xStr("blank", "0") == "1"
            val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
            val keys = mutableListOf<String>()
            for (lvIdx in (base.size - 1) downTo 0) {
                val len = base.size - lvIdx
                for (k in 0 until len) {
                    val key = "${lvIdx}_$k"
                    if (key in emptySet || (blank && (lvIdx + k) % 3 == 1)) keys += key
                }
            }
            keys
        }
        else -> emptyList()
    }

    private fun hitCalendarCell(spec: FigureSpec, sx: Float, sy: Float): CellHit? {
        val start = spec.xNum("start", 3f).toInt().coerceIn(0, 6)
        val days = spec.xNum("days", 30f).toInt().coerceIn(20, 31)
        val blank = spec.xStr("blank", "0") == "1"
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov")
        val ansMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ans")
        val cw = 44f; val ch = 30f
        val x0 = 180f - cw * 3.5f
        val y0 = 46f
        for (i in 0 until days) {
            val cell = i + start
            val col = 6 - (cell % 7)
            val row = cell / 7
            val bx = x0 + col * cw
            val by = y0 + ch + row * ch
            if (sx in bx..(bx + cw) && sy in by..(by + ch)) {
                val day = i + 1
                val key = day.toString()
                val isEmpty = key in emptySet || (blank && day % 5 == 0)
                val cur = ovMap[key]?.trim() ?: key
                return CellHit(
                    key = key,
                    title = "روز ${toFa(key)} ماه",
                    defaultVal = key,
                    currentVal = cur,
                    isEmpty = isEmpty,
                    studentVal = ansMap[key]?.trim().orEmpty()
                )
            }
        }
        return null
    }

    private fun hitMultGridCell(spec: FigureSpec, sx: Float, sy: Float): CellHit? {
        val baseRows = parseIntList(spec.xStr("rows", "2,4,7"), listOf(2, 4, 7)).take(4)
        val baseCols = parseIntList(spec.xStr("cols", "3,5,8"), listOf(3, 5, 8)).take(4)
        val blank = spec.xNum("blank", 0f).toInt().coerceIn(0, 2)
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov")
        val ansMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ans")
        val rows = baseRows.mapIndexed { r, v -> ovMap["r$r"]?.let { faEnDigits(it).toIntOrNull() } ?: v }
        val cols = baseCols.mapIndexed { c, v -> ovMap["c$c"]?.let { faEnDigits(it).toIntOrNull() } ?: v }
        val cw = min(56f, 268f / (cols.size + 1))
        val ch = min(42f, 216f / (rows.size + 1))
        val x0 = 180f - cw * (cols.size + 1) / 2f
        val y0 = 26f + (218f - ch * (rows.size + 1)) / 2f
        if (sx < x0 || sy < y0) return null
        val gc = ((sx - x0) / cw).toInt()
        val gr = ((sy - y0) / ch).toInt()
        if (gr !in 0..rows.size || gc !in 0..cols.size) return null
        if (gr == 0 && gc == 0) return null
        if (gr == 0) {
            val c = gc - 1
            val key = "c$c"
            val def = baseCols[c].toString()
            return CellHit(
                key = key,
                title = "سرستون ${toFa((c + 1).toString())}",
                defaultVal = def,
                currentVal = ovMap[key]?.trim() ?: def,
                isEmpty = key in emptySet || blank == 2,
                studentVal = ansMap[key]?.trim().orEmpty()
            )
        }
        if (gc == 0) {
            val r = gr - 1
            val key = "r$r"
            val def = baseRows[r].toString()
            return CellHit(
                key = key,
                title = "سرسطر ${toFa((r + 1).toString())}",
                defaultVal = def,
                currentVal = ovMap[key]?.trim() ?: def,
                isEmpty = key in emptySet || blank == 2,
                studentVal = ansMap[key]?.trim().orEmpty()
            )
        }
        val r = gr - 1
        val c = gc - 1
        val key = "${r}_$c"
        val def = (rows[r] * cols[c]).toString()
        return CellHit(
            key = key,
            title = "خانهٔ (${toFa(rows[r].toString())} × ${toFa(cols[c].toString())})",
            defaultVal = def,
            currentVal = ovMap[key]?.trim() ?: def,
            isEmpty = key in emptySet || (blank == 1 && (r + c) % 2 == 1),
            studentVal = ansMap[key]?.trim().orEmpty()
        )
    }

    private fun hitMulWheelCell(spec: FigureSpec, sx: Float, sy: Float): CellHit? {
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov")
        val ansMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ans")
        val cBase = spec.xNum("c", 3f).toInt().coerceIn(1, 20)
        val c = ovMap["center"]?.let { faEnDigits(it).toIntOrNull() } ?: cBase
        val n = spec.xNum("n", 10f).toInt().coerceIn(3, 12)
        val plus = spec.xStr("op", "×") == "+"
        val hide = spec.xStr("hide", "0") == "1"
        if (hypot((sx - 180f).toDouble(), (sy - 142f).toDouble()) <= 34.0) {
            val def = cBase.toString()
            return CellHit(
                key = "center",
                title = "عدد مرکز چرخ",
                defaultVal = def,
                currentVal = ovMap["center"]?.trim() ?: def,
                isEmpty = "center" in emptySet,
                studentVal = ansMap["center"]?.trim().orEmpty()
            )
        }
        for (j in 0 until n) {
            val a = (-PI / 2 + j * 2 * PI / n)
            val ox = 180f + 96f * cos(a).toFloat()
            val oy = 142f + 96f * sin(a).toFloat()
            if (hypot((sx - ox).toDouble(), (sy - oy).toDouble()) <= 20.0) {
                val key = "s_$j"
                val mFactor = j + 1
                val def = (if (plus) c + mFactor else c * mFactor).toString()
                val opSym = if (plus) "+" else "×"
                return CellHit(
                    key = key,
                    title = "پرهٔ ${toFa(mFactor.toString())} ($c $opSym $mFactor)",
                    defaultVal = def,
                    currentVal = (ovMap[key] ?: ovMap["$j"])?.trim() ?: def,
                    isEmpty = key in emptySet || "$j" in emptySet || (hide && j % 3 == 2),
                    studentVal = (ansMap[key] ?: ansMap["$j"])?.trim().orEmpty()
                )
            }
        }
        return null
    }

    private fun hitTowerCell(spec: FigureSpec, sx: Float, sy: Float): CellHit? {
        val baseRaw = parseIntList(spec.xStr("base", "1,2,3,4"), listOf(1, 2, 3, 4)).take(6)
        val minus = spec.xStr("op", "+") == "-"
        val blank = spec.xStr("blank", "0") == "1"
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec)
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov")
        val ansMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ans")
        val base = baseRaw.mapIndexed { idx, v -> ovMap["0_$idx"]?.let { faEnDigits(it).toIntOrNull() } ?: v }
        val levels = mutableListOf(base)
        var cur = base
        while (cur.size > 1) {
            val lvIdx = levels.size
            val next = (0 until cur.size - 1).map { i ->
                val computed = if (minus) abs(cur[i] - cur[i + 1]) else cur[i] + cur[i + 1]
                ovMap["${lvIdx}_$i"]?.let { faEnDigits(it).toIntOrNull() } ?: computed
            }
            levels.add(next); cur = next
        }
        val bh = min(34f, 216f / levels.size)
        val bw = min(58f, 300f / base.size)
        for (row in levels.indices) {
            val lvIdx = levels.size - 1 - row
            val arr = levels[lvIdx]
            val ly = 24f + row * bh
            for (k in arr.indices) {
                val bx = 180f + (k - (arr.size - 1) / 2f) * bw - bw / 2f
                if (sx in bx..(bx + bw - 3f) && sy in ly..(ly + bh - 3f)) {
                    val key = "${lvIdx}_$k"
                    val def = arr[k].toString()
                    return CellHit(
                        key = key,
                        title = "آجر ردیف ${toFa((lvIdx + 1).toString())}، ستون ${toFa((k + 1).toString())}",
                        defaultVal = def,
                        currentVal = ovMap[key]?.trim() ?: def,
                        isEmpty = key in emptySet || (blank && (lvIdx + k) % 3 == 1),
                        studentVal = ansMap[key]?.trim().orEmpty()
                    )
                }
            }
        }
        return null
    }

    /** اعمال تغییر معلم روی یک خانه: یا مقدار دلخواه، یا خالی‌کردن بدون علامت سؤال، یا بازگشت به مقدار پیش‌فرض. */
    fun applyTeacherCellEdit(spec: FigureSpec, key: String, newValue: String?, makeEmpty: Boolean): FigureSpec {
        val emptySet = AdvancedMathSvgRenderer.parseEmptySet(spec).toMutableSet()
        val ovMap = AdvancedMathSvgRenderer.parseCellMap(spec, "ov").toMutableMap()
        val resetAutoKey = when (spec.type) {
            "axmulwheel" -> "hide"
            "axdicefold" -> "ask"
            else -> "blank"
        }
        val wasAutoBlank = spec.xStr(resetAutoKey, "0") != "0"
        if (wasAutoBlank) {
            listEmptyCellKeys(spec).forEach { emptySet += it }
        }
        var extraCenter: String? = null
        if (makeEmpty) {
            if (spec.type == "axkenken" && emptySet.isEmpty() && key in ovMap) {
                ovMap.remove(key)
            } else {
                emptySet += key
                ovMap.remove(key)
            }
        } else {
            emptySet.remove(key)
            val clean = newValue?.trim()?.replace(';', ',')?.replace('؛', ',').orEmpty()
            if (clean.isEmpty()) {
                ovMap.remove(key)
            } else {
                val ascii = faEnDigits(clean)
                ovMap[key] = ascii
                if (spec.type == "axmulwheel" && key == "center") extraCenter = ascii
            }
        }
        val emptyVal = if (emptySet.isEmpty()) null else emptySet.joinToString(",")
        val ovVal = if (ovMap.isEmpty()) null else ovMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        return if (extraCenter != null) {
            withX(spec, resetAutoKey to "0", "empty" to emptyVal, "ov" to ovVal, "c" to extraCenter)
        } else {
            withX(spec, resetAutoKey to "0", "empty" to emptyVal, "ov" to ovVal)
        }
    }

    fun parseStudentCellMap(spec: FigureSpec, rawAnswer: String): MutableMap<String, String> {
        val trim = rawAnswer.trim()
        if (trim.isEmpty()) return LinkedHashMap()
        if (':' in trim || '=' in trim) {
            val map = LinkedHashMap<String, String>()
            trim.split(';', '؛').forEach { part ->
                val idx = part.indexOf(':').takeIf { it > 0 } ?: part.indexOf('=').takeIf { it > 0 } ?: -1
                if (idx > 0) {
                    val k = faEnDigits(part.substring(0, idx)).trim()
                    val v = part.substring(idx + 1).trim()
                    if (k.isNotEmpty() && v.isNotEmpty()) map[k] = v
                }
            }
            if (map.isNotEmpty()) return map
        }
        val emptyKeys = listEmptyCellKeys(spec)
        if (emptyKeys.size == 1) {
            return linkedMapOf(emptyKeys[0] to trim)
        }
        val parts = trim.split(',', '،', ';', '؛').map { it.trim() }
        val out = LinkedHashMap<String, String>()
        emptyKeys.forEachIndexed { idx, key ->
            val v = parts.getOrNull(idx).orEmpty()
            if (v.isNotEmpty()) out[key] = v
        }
        return out
    }

    fun encodeStudentCellAnswer(spec: FigureSpec, map: Map<String, String>): String {
        if (spec.type == "axkenken" && AdvancedMathSvgRenderer.parseEmptySet(spec).isEmpty()) {
            return map.entries.filter { it.value.isNotBlank() }.joinToString(";") { "${it.key}:${it.value}" }
        }
        val emptyKeys = listEmptyCellKeys(spec)
        if (emptyKeys.size == 1) {
            return map[emptyKeys[0]]?.trim().orEmpty()
        }
        if (emptyKeys.isNotEmpty()) {
            val ordered = emptyKeys.map { map[it]?.trim().orEmpty() }
            return if (ordered.all { it.isEmpty() }) "" else ordered.joinToString("، ")
        }
        return map.entries.filter { it.value.isNotBlank() }.joinToString(";") { "${it.key}:${it.value}" }
    }

    fun injectStudentOverlay(spec: FigureSpec, rawAnswer: String): FigureSpec {
        if (rawAnswer.isBlank()) return spec
        return when (spec.type) {
            "axsym" -> {
                val mirror = spec.xNum("mirror", 1f).toInt() != 0
                if (mirror) spec else withX(spec, "ansCells" to rawAnswer)
            }
            "axpaintgrid" -> withX(spec, "ansCells" to rawAnswer)
            in KEYED_CELL_TYPES -> {
                val map = parseStudentCellMap(spec, rawAnswer)
                if (map.isEmpty()) spec
                else withX(spec, "ans" to map.entries.joinToString(";") { "${it.key}:${it.value}" })
            }
            else -> spec
        }
    }

    // =========================================================================
    // ۱۲) شمارش مکعب‌های روی‌هم‌چیده (axcubes) — ماتریس تعاملی ستون‌ها
    // =========================================================================

    fun parseCubesGrid(spec: FigureSpec): MutableList<MutableList<Int>> {
        val parsed = spec.xStr("map", "2,1,3;1,0,1;2,2,1").split(';', '؛')
            .map { rw -> rw.split(',', '،').map { faEnDigits(it.trim()).toIntOrNull()?.coerceIn(0, 6) ?: 0 } }
            .filter { it.isNotEmpty() }
            .take(5)
        val raw = parsed.ifEmpty { listOf(listOf(2, 1, 3), listOf(1, 0, 1), listOf(2, 2, 1)) }
        val rowsN = raw.size.coerceIn(1, 5)
        val colsN = raw.maxOf { it.size }.coerceIn(1, 5)
        return MutableList(rowsN) { r -> MutableList(colsN) { c -> raw[r].getOrElse(c) { 0 }.coerceIn(0, 6) } }
    }

    fun setCubesCell(spec: FigureSpec, r: Int, c: Int, height: Int): FigureSpec {
        val grid = parseCubesGrid(spec)
        if (r !in grid.indices || c !in grid[r].indices) return spec
        grid[r][c] = height.coerceIn(0, 6)
        val encoded = grid.joinToString(";") { it.joinToString(",") }
        return withX(spec, "map" to encoded)
    }

    fun resizeCubesGrid(spec: FigureSpec, newRows: Int, newCols: Int): FigureSpec {
        val rN = newRows.coerceIn(1, 5)
        val cN = newCols.coerceIn(1, 5)
        val cur = parseCubesGrid(spec)
        val next = List(rN) { r ->
            List(cN) { c -> cur.getOrNull(r)?.getOrNull(c) ?: 1 }
        }
        val encoded = next.joinToString(";") { it.joinToString(",") }
        return withX(spec, "map" to encoded)
    }

    fun hitCubesTopCell(spec: FigureSpec, sx: Float, sy: Float): Pair<Int, Int>? {
        if (spec.xStr("view", "iso").lowercase() != "top") return null
        val grid = parseCubesGrid(spec)
        val rowsN = grid.size
        val colsN = grid[0].size
        val s = min(46f, 186f / max(rowsN, colsN))
        val gw = colsN * s
        val gh = rowsN * s
        val x0 = 180f - gw / 2f
        val y0 = 132f - gh / 2f
        if (sx < x0 || sy < y0 || sx >= x0 + gw || sy >= y0 + gh) return null
        val c = ((sx - x0) / s).toInt().coerceIn(0, colsN - 1)
        val r = ((sy - y0) / s).toInt().coerceIn(0, rowsN - 1)
        return r to c
    }

    // =========================================================================
    // ۹) استوانهٔ مدرج چندگانه (axbeaker) — تشخیص استوانه و تنظیم سطح مایع
    // =========================================================================

    fun adjustBeakerAt(spec: FigureSpec, sx: Float, sy: Float): FigureSpec {
        val mx = max(10f, spec.xNum("max", 500f))
        val count = spec.xNum("count", 1f).toInt().coerceIn(1, 4)
        val top = if (count == 1) 36f else 42f
        val bot = if (count == 1) 216f else 212f
        val step = max(1f, mx / 20f)
        val v = ((((bot - sy) / (bot - top)).coerceIn(0f, 1f) * mx) / step).roundToInt() * step
        val cleanV = v.roundToInt().coerceIn(0, mx.roundToInt()).toString()
        if (count == 1) {
            return withX(spec, "val" to cleanV)
        }
        val slotW = 340f / count
        val idx = ((sx - 10f) / slotW).toInt().coerceIn(0, count - 1)
        val keys = listOf("val", "val2", "val3", "val4")
        return withX(spec, keys[idx] to cleanV)
    }

    private fun parseIntList(raw: String, fallback: List<Int>): List<Int> =
        raw.split(',', '،', ';', '؛')
            .mapNotNull { faEnDigits(it.trim()).toIntOrNull() }
            .ifEmpty { fallback }
}
