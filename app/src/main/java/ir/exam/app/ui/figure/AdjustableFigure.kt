package ir.exam.app.ui.figure

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import ir.exam.app.core.figure.FigureSpec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** V318 — چرخهٔ پارامتر با هر لمس برای ۴۰ ابزار جامع (هم‌تراز با CYCLES در graph_fig.js). */
private val ADV_CYCLE_PARAMS: Map<String, Pair<String, List<String>>> = mapOf(
    "axrod" to ("blank" to listOf("0", "1")),
    "axtower" to ("op" to listOf("+", "-")),
    "axmulwheel" to ("c" to listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")),
    "axmultgrid" to ("blank" to listOf("0", "1", "2")),
    "axnet" to ("shape" to listOf("cube", "cuboid", "pyramid", "prism")),
    "axcubes" to ("view" to listOf("iso", "top", "front", "side")),
    "axcalendar" to ("start" to listOf("0", "1", "2", "3", "4", "5", "6")),
    "axlever" to ("kind" to listOf("lever1", "lever2", "lever3", "pulley", "gears")),
    "axcycle" to ("kind" to listOf("water", "plant", "frog", "food")),
    "axwordtrain" to ("mode" to listOf("join", "blank")),
    // V327
    "axmagnet" to ("mode" to listOf("attract", "repel")),
    "axpaintgrid" to ("goal" to listOf("1", "2", "3", "4", "5", "6", "8", "10", "12")),
    "axtiles" to ("x" to listOf("0", "1", "2", "3", "4", "5", "6", "7", "8")),
    "axkenken" to ("size" to listOf("3", "4", "5", "6")),
    "axdicefold" to ("top" to listOf("1", "2", "3", "4", "5", "6")),
    "axtransform" to ("op" to listOf("translate", "rotate", "reflect")),
    "axsigntable" to ("lead" to listOf("1", "-1")),
    "axprobtree" to ("mode" to listOf("prob", "count")),
    "axconic" to ("kind" to listOf("ellipse", "hyperbola", "parabola")),
    "axpunnett" to ("mode" to listOf("genotype", "ratio")),
    "axecg" to ("kind" to listOf("ecg", "shm")),
    "axprosody" to ("mode" to listOf("arkan", "taqti")),
    "axtruthtable" to ("kind" to listOf("truth", "square")),
    "axstaff" to ("kind" to listOf("staff", "color")),
    "axkmap" to ("vars" to listOf("2", "3", "4")),
    "axbst" to ("mode" to listOf("bst", "heap")),
    "axbode" to ("kind" to listOf("bode", "nyquist")),
    "axbeam" to ("load" to listOf("point", "udl")),
    "axpv" to ("kind" to listOf("carnot", "otto", "diesel", "rankine")),
    "axbenzene" to ("kind" to listOf("kekule", "chair", "newman", "fischer")),
    "axsupply" to ("kind" to listOf("equilibrium", "shift", "ceiling")),
    "axgame" to ("kind" to listOf("game", "islm")),
    "axunitconv" to ("kind" to listOf("len", "mass", "area")),
    "axperimeter" to ("shape" to listOf("rect", "tri", "circle", "poly")),
    "axpictograph" to ("mode" to listOf("picto", "bar")),
    "axrounding" to ("mode" to listOf("ten", "hundred")),
    "axdivision" to ("b" to listOf("2", "3", "4", "5", "6", "7", "8", "9", "10")),
    "axshapeid" to ("kind" to listOf("tri", "square", "rect", "circle", "poly")),
    "axcircleparts" to ("show" to listOf("all", "radius", "diameter", "chord", "tangent", "arc")),
    "axcylinder" to ("kind" to listOf("cylinder", "cone")),
    "axproportion" to ("a" to listOf("1", "2", "3", "4", "5", "6", "8", "10")),
    "axbarchart" to ("show" to listOf("mean", "median", "none")),
    "axinthales" to ("ang" to listOf("30", "45", "60", "90", "120", "150")),
    "axpendulum" to ("amp" to listOf("10", "20", "30", "40")),
    "axincline" to ("ang" to listOf("15", "30", "45", "60")),
    "axcircuit" to ("mode" to listOf("series", "parallel")),
    "axoptics" to ("kind" to listOf("convex", "concave")),
    "axdna" to ("mode" to listOf("helix", "flat")),
    "axcell" to ("kind" to listOf("animal", "plant")),
    "axmapgeo" to ("kind" to listOf("compass", "map")),
    "axsentence" to ("mode" to listOf("verbal", "nominal")),
    "axvectors" to ("mode" to listOf("sum", "diff")),
    "axintegral" to ("f" to listOf("quad", "sin", "line")),
    "axstack" to ("mode" to listOf("stack", "queue")),
    "axsort" to ("step" to listOf("0", "1", "2", "3"))

)

/** V318 — پارامتر عددی با گام مشخص (کلیک = یک گام؛ پس از بیشینه به کمینه برمی‌گردد). */
private val ADV_NUM_PARAMS: Map<String, Triple<String, Float, ClosedFloatingPointRange<Float>>> = mapOf(
    "axcaliper" to Triple("val", 1f, 0f..50f),
    "axorbital" to Triple("z", 1f, 1f..36f),
    "axmohr" to Triple("txy", 5f, 0f..120f),
    "axunitconv" to Triple("val", 1f, 1f..20f),
    "axperimeter" to Triple("w", 1f, 1f..12f),
    "axrounding" to Triple("n", 10f, 10f..990f),
    "axdivision" to Triple("a", 1f, 0f..60f),
    "axshapeid" to Triple("n", 1f, 3f..8f),
    "axcylinder" to Triple("r", 1f, 1f..8f),
    "axproportion" to Triple("c", 1f, 1f..20f),
    "axlinear" to Triple("b1", 1f, -5f..5f),
    "axparabola" to Triple("c", 1f, -6f..6f),
    "axinthales" to Triple("ang", 10f, 20f..160f),
    "axpendulum" to Triple("len", 1f, 1f..3f),
    "axincline" to Triple("ang", 5f, 10f..60f),
    "axcircuit" to Triple("r1", 1f, 1f..20f),
    "axoptics" to Triple("d", 5f, 10f..60f),
    "axsort" to Triple("step", 1f, 0f..4f)

)


/**
 * V310.7 — شکل‌ها: ساعت عقربه‌ای با دست قابل جابه‌جایی است (معلم و دانش‌آموز).
 * نزدیک مرکز = عقربهٔ ساعت، بیرون = عقربهٔ دقیقه. بقیهٔ شکل‌ها همان InlineFigureView هستند.
 * [onChanged] وقتی ساعت جابه‌جا شود (مقادیر X.h و X.m تازه) صدا زده می‌شود.
 */
@Composable
fun AdjustableFigure(
    spec: FigureSpec,
    modifier: Modifier = Modifier,
    contentDescription: String = "شکل",
    onChanged: (FigureSpec) -> Unit = {}
) {
    var current by remember(spec) { mutableStateOf(spec) }
    val interactiveTypes = setOf(
        "axclock", "axfrac", "axabacus", "axthermo", "axtenframe", "axspin", "axbeaker",
        "axtally", "axbase10", "axplace", "axruler", "axmoney", "axjump", "axbalance",
        "axsaw", "axpattern", "axsym", "axgeoboard", "axdice", "axmularea", "axvenn",
        "axnumgrid", "axfacttree", "axfnmachine", "axfracwall", "axtangram", "axangle",
        // V319
        "axunitconv", "axperimeter", "axpictograph", "axrounding", "axdivision", "axshapeid", "axcircleparts", "axcylinder", "axproportion", "axlinear", "axbarchart", "axparabola", "axinthales", "axpendulum", "axincline", "axcircuit", "axoptics", "axheatcurve", "axdna", "axcell", "axtimeline", "axmapgeo", "axsentence", "axvectors", "axmatrix", "axintegral", "axflowchart", "axstack", "axsort", "axregression",
        "axrod", "axtower", "axmulwheel", "axmultgrid", "axnet", "axcubes", "axcalendar", "axlever", "axcycle", "axwordtrain",
        "axpaintgrid", "axletterforms", "axsyllablechart", "axmagnet", "axsenses", "axseedgrow", "axbodyparts",
        "axtiles", "axkenken", "axdicefold", "axtransform", "axcaliper", "axsigntable", "axtrigcircle", "axmapping", "axprobtree", "axconic", "axorbital", "axpunnett", "axecg", "axprosody", "axtruthtable", "axledger", "axstaff", "axkmap", "axgate", "axdfa", "axbst", "axbode", "axbeam", "axmohr", "axpv", "axpert", "axbenzene", "axnormal", "axsupply", "axgame",
        "axnumpts", "axnumint", "axnumineq", "axxypts", "axxyline", "axxyvec", "axxycirc", "axpolpts",
        "gauge", "ang", "sec", "arc", "segm", "parll", "ngon"
    )
    if (spec.type in interactiveTypes) {
        InteractiveClock(current, modifier, contentDescription) { next ->
            current = next
            onChanged(next)
        }
    } else {
        InlineFigureView(spec = spec, modifier = modifier, contentDescription = contentDescription)
    }
}

@Composable
private fun InteractiveClock(
    spec: FigureSpec,
    modifier: Modifier,
    contentDescription: String,
    onNext: (FigureSpec) -> Unit
) {
    val latest by rememberUpdatedState(spec)
    val emit by rememberUpdatedState(onNext)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var grab by remember { mutableStateOf<String?>(null) }

    // مختصات صفحه → مختصات SVG (بوم 360×280 یا 360×370 با ContentScale.Fit وسط‌چین)
    fun toSvg(p: Offset): Offset? {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w <= 0f || h <= 0f) return null
        val vh = if (latest.type == "axbase10") 370f else 280f
        val sc = min(w / 360f, h / vh)
        return Offset((p.x - (w - 360f * sc) / 2f) / sc, (p.y - (h - vh * sc) / 2f) / sc)
    }

    fun moveTo(p: Offset) {
        val mode = grab ?: return
        val q = toSvg(p) ?: return
        val deg = (atan2((q.x - 180f).toDouble(), -(q.y - 150f).toDouble()) * 180.0 / PI + 360.0) % 360.0
        val x = (latest.raw["X"] as? JsonObject) ?: JsonObject(emptyMap())
        val nx = if (latest.type == "axthermo") {
            val mn = latest.xNum("min", -10f)
            val mx = kotlin.math.max(mn + 5f, latest.xNum("max", 50f))
            val ratio = ((204f - q.y) / (204f - 34f)).coerceIn(0f, 1f)
            val v = Math.round(mn + ratio * (mx - mn))
            x + ("val" to JsonPrimitive(v))
        } else if (latest.type == "axbeaker") {
            val mx = kotlin.math.max(10f, latest.xNum("max", 500f))
            val ratio = ((216f - q.y) / (216f - 36f)).coerceIn(0f, 1f)
            val step = if (mx >= 100f) 10 else 1
            val v = (Math.round((ratio * mx) / step) * step).coerceIn(0, mx.toInt())
            x + ("val" to JsonPrimitive(v))
        } else if (latest.type == "axruler") {
            val len = latest.xNum("len", 10f).toInt().coerceIn(2, 50)
            val ratio = ((q.x - 24f) / (336f - 24f)).coerceIn(0f, 1f)
            val bVal = Math.round(ratio * len).coerceIn(1, len)
            x + ("a" to JsonPrimitive(0)) + ("b" to JsonPrimitive(bVal))
        } else if (latest.type == "axnumint") {
            val xmin = latest.xNum("xmin", -5f)
            val xmax = kotlin.math.max(xmin + 2f, latest.xNum("xmax", 5f))
            val v = Math.round((xmin + ((q.x - 40f) / (336f - 40f)).coerceIn(0f, 1f) * (xmax - xmin)) * 2f) / 2f
            if (q.x < 188f) x + ("lo" to JsonPrimitive(v)) else x + ("hi" to JsonPrimitive(v))
        } else if (latest.type == "axnumineq") {
            val xmin = latest.xNum("xmin", -5f)
            val xmax = kotlin.math.max(xmin + 2f, latest.xNum("xmax", 5f))
            val v = Math.round((xmin + ((q.x - 40f) / (336f - 40f)).coerceIn(0f, 1f) * (xmax - xmin)) * 2f) / 2f
            x + ("x0" to JsonPrimitive(v))
        } else if (latest.type in setOf("axxyline", "axxyvec", "axxycirc")) {
            val xmin = latest.xNum("xmin", -5f)
            val xmax = kotlin.math.max(xmin + 2f, latest.xNum("xmax", 5f))
            val ymin = latest.xNum("ymin", -4f)
            val ymax = kotlin.math.max(ymin + 2f, latest.xNum("ymax", 4f))
            val xv = Math.round((xmin + ((q.x - 40f) / (336f - 40f)).coerceIn(0f, 1f) * (xmax - xmin)) * 2f) / 2f
            val yv = Math.round((ymin + ((246f - q.y) / (246f - 26f)).coerceIn(0f, 1f) * (ymax - ymin)) * 2f) / 2f
            when (latest.type) {
                "axxyline" -> x + ("b" to JsonPrimitive(yv))
                "axxyvec" -> x + ("x2" to JsonPrimitive(xv)) + ("y2" to JsonPrimitive(yv))
                else -> x + ("cx" to JsonPrimitive(xv)) + ("cy" to JsonPrimitive(yv))
            }
        } else if (latest.type == "gauge") {
            val vmin = latest.xNum("vmin", 0f)
            val vmax = kotlin.math.max(vmin + 1f, latest.xNum("vmax", 100f))
            val ratio = ((q.x - 60f) / 240f).coerceIn(0f, 1f)
            x + ("val" to JsonPrimitive(Math.round(vmin + ratio * (vmax - vmin))))
        } else if (latest.type == "axtrigcircle") {
            val angDeg = (atan2((140f - q.y).toDouble(), (q.x - 176f).toDouble()) * 180.0 / PI + 360.0) % 360.0
            x + ("deg" to JsonPrimitive(Math.round(angDeg).toInt()))
        } else if (latest.type == "axangle") {
            val angDeg = (atan2((212f - q.y).toDouble(), (q.x - 180f).toDouble()) * 180.0 / PI).toInt().coerceIn(5, 175)
            x + ("deg" to JsonPrimitive(angDeg))
        } else if (latest.type in setOf("ang", "sec", "arc", "segm", "parll")) {
            val angDeg = ((atan2((160f - q.y).toDouble(), (q.x - 180f).toDouble()) * 180.0 / PI + 360.0) % 360.0).toInt().coerceIn(10, 350)
            if (latest.type == "parll") x + ("tilt" to JsonPrimitive(angDeg.coerceIn(15, 165).toString()))
            else x + ("m" to JsonPrimitive(angDeg.toString()))
        } else if (mode == "hour") {
            x + ("h" to JsonPrimitive(Math.round(deg / 30.0).toInt() % 12))
        } else {
            x + ("m" to JsonPrimitive(Math.round(deg / 6.0).toInt() % 60))
        }
        emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(nx)))))
    }

    fun tapAt(p: Offset) {
        val q = toSvg(p) ?: return
        val x = (latest.raw["X"] as? JsonObject) ?: JsonObject(emptyMap())
        when (latest.type) {
            "axfrac" -> {
                val den = latest.xNum("den", 4f).toInt().coerceIn(1, 24)
                val num = latest.xNum("num", 1f).toInt().coerceIn(0, den)
                val nextNum = (num + 1) % (den + 1)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("num" to JsonPrimitive(nextNum)))))))
            }
            "axabacus" -> {
                val cols = latest.xNum("cols", 4f).toInt().coerceIn(2, 6)
                val step = 288f / cols
                val colIdx = ((q.x - 36f) / step).toInt().coerceIn(0, cols - 1)
                val place = cols - 1 - colIdx
                var pow = 1L
                repeat(place) { pow *= 10L }
                val curN = latest.xNum("n", 2435f).toLong().coerceIn(0L, 999999L)
                val curDigit = (curN / pow) % 10L
                val nextDigit = (curDigit + 1L) % 10L
                val nextN = curN + (nextDigit - curDigit) * pow
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("n" to JsonPrimitive(nextN)))))))
            }
            "axthermo", "axbeaker", "axruler" -> {
                grab = "thermo"
                moveTo(p)
                grab = null
            }
            "axtally" -> {
                val cur = latest.xNum("n", 7f).toInt().coerceIn(0, 75)
                val nextN = if (q.x >= 180f) (cur + 1).coerceAtMost(75) else (cur - 1).coerceAtLeast(1)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("n" to JsonPrimitive(nextN)))))))
            }
            "axbase10" -> {
                val cur = latest.xNum("n", 134f).toLong().coerceIn(0L, 1_000_000L)
                if (q.y >= 340f) {
                    val lb = latest.xStr("label", "1")
                    val nextLb = if (lb == "1") "2" else if (lb == "2") "0" else "1"
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("label" to JsonPrimitive(nextLb)))))))
                } else {
                    val rowIdx = ((q.y - 26f) / 45f).toInt().coerceIn(0, 6)
                    val pows = longArrayOf(1_000_000L, 100_000L, 10_000L, 1_000L, 100L, 10L, 1L)
                    val pow = pows[rowIdx]
                    val nextN = if (pow == 1_000_000L) {
                        if (cur >= 1_000_000L) 0L else 1_000_000L
                    } else {
                        val base = cur % 1_000_000L
                        val d = (base / pow) % 10L
                        base + (((d + 1L) % 10L) - d) * pow
                    }
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("n" to JsonPrimitive(nextN)))))))
                }
            }
            "axplace" -> {
                val cur = latest.xStr("n", "2457").filter { it.isDigit() }.ifEmpty { "0" }.toLongOrNull() ?: 0L
                val col = ((q.x - 30f) / (326f / 12f)).toInt().coerceIn(0, 11)
                val place = 11 - col
                var pow = 1L
                repeat(place) { pow *= 10L }
                val d = (cur / pow) % 10L
                val nextN = cur + (((d + 1L) % 10L) - d) * pow
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("n" to JsonPrimitive(nextN.toString())))))))
            }
            "axmoney" -> {
                if (q.y < 140f) {
                    val curNotes = latest.xStr("notes", "1000,500").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    if (curNotes.size >= 5) curNotes.removeAt(curNotes.lastIndex) else curNotes.add("1000")
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("notes" to JsonPrimitive(curNotes.joinToString(","))))))))
                } else if (q.y < 230f) {
                    val curCoins = latest.xStr("coins", "200,100,100").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    if (curCoins.size >= 6) curCoins.removeAt(curCoins.lastIndex) else curCoins.add("100")
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("coins" to JsonPrimitive(curCoins.joinToString(","))))))))
                } else {
                    val cur = latest.xStr("sum", "1")
                    val nextSum = if (cur == "1") "2" else if (cur == "2") "0" else "1"
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("sum" to JsonPrimitive(nextSum)))))))
                }
            }
            "axjump" -> {
                if (q.x >= 180f) {
                    val j = (latest.xNum("jumps", 4f).toInt() % 8) + 1
                    val st = latest.xNum("step", 3f).toInt().coerceAtLeast(1)
                    val s0 = latest.xNum("start", 0f).toInt().coerceAtLeast(0)
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("jumps" to JsonPrimitive(j)) + ("max" to JsonPrimitive(s0 + j * st)))))))
                } else {
                    val st = (latest.xNum("step", 3f).toInt() % 6) + 1
                    val j = latest.xNum("jumps", 4f).toInt().coerceAtLeast(1)
                    val s0 = latest.xNum("start", 0f).toInt().coerceAtLeast(0)
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("step" to JsonPrimitive(st)) + ("max" to JsonPrimitive(s0 + j * st)))))))
                }
            }
            "axbalance" -> {
                if (q.x < 180f) {
                    val l = (latest.xNum("left", 5f).toInt() % 10) + 1
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("left" to JsonPrimitive(l)))))))
                } else {
                    val r = (latest.xNum("right", 3f).toInt() % 10) + 1
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("right" to JsonPrimitive(r)))))))
                }
            }
            "axsaw" -> {
                val raw = latest.xStr("vals", "1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1")
                val parsed = raw.split(';', '؛').map { it.split(',', '،').map { c -> c.trim() } }.filter { it.isNotEmpty() }
                val explicitSize = latest.xNum("size", 0f).toInt()
                val n = (if (explicitSize in 3..9) explicitSize else parsed.size.coerceIn(3, 9)).coerceIn(3, 9)
                val mode = latest.xStr("mode", "shape").lowercase()
                val showLegend = mode == "color" || mode == "color_shape"
                val maxGridPx = if (showLegend) 196f else 216f
                val cs = kotlin.math.min(52f, maxGridPx / n)
                val gw = cs * n
                val x0 = (360f - gw) / 2f
                val y0 = (if (showLegend) 24f else 28f) + (maxGridPx - gw) / 2f
                val c = ((q.x - x0) / cs).toInt().coerceIn(0, n - 1)
                val r = ((q.y - y0) / cs).toInt().coerceIn(0, n - 1)
                val baseRows = if (parsed.size == n && parsed.all { it.size >= n }) parsed else ir.exam.app.core.figure.PrimaryMathSvgRenderer.defaultSudokuRows(n)
                val grid = MutableList(n) { ri ->
                    MutableList(n) { ci -> baseRows.getOrNull(ri)?.getOrNull(ci)?.ifBlank { "?" } ?: "?" }
                }
                val cycle = (1..n).map { it.toString() } + "?"
                val cur = grid[r][c]
                grid[r][c] = cycle[(cycle.indexOf(cur) + 1) % cycle.size]
                val nextVals = grid.joinToString(";") { it.joinToString(",") }
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("size" to JsonPrimitive(n)) + ("vals" to JsonPrimitive(nextVals)))))))
            }
            "axpattern" -> {
                val items = latest.xStr("seq", "●,▲,●,▲,●,?").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (items.isNotEmpty()) {
                    val n = items.size
                    val cw = kotlin.math.min(42f, 300f / n)
                    val gap = if (n <= 8) 6f else 4f
                    val tw = n * cw + (n - 1) * gap
                    val x0 = (360f - tw) / 2f
                    val idx = ((q.x - x0) / (cw + gap)).toInt().coerceIn(0, n - 1)
                    val mode = latest.xStr("mode", "shape").lowercase()
                    val cycle = if (mode == "color" || mode == "bead") listOf("قرمز", "آبی", "سبز", "زرد", "بنفش", "?") else listOf("●", "▲", "■", "★", "?")
                    items[idx] = cycle[(cycle.indexOf(items[idx]) + 1) % cycle.size]
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("seq" to JsonPrimitive(items.joinToString(","))))))))
                }
            }
            "axnumgrid" -> {
                val start = latest.xNum("start", 1f).toInt().coerceIn(0, 900)
                val cols = latest.xStr("cols", "10").trim().toIntOrNull()?.coerceIn(4, 10) ?: 10
                val rows = latest.xStr("rows", "10").trim().toIntOrNull()?.coerceIn(3, 10) ?: 10
                val cs = kotlin.math.min(280f / cols, 218f / rows)
                val gw = cs * cols
                val gh = cs * rows
                val x0 = (360f - gw) / 2f
                val y0 = 26f + (222f - gh) / 2f
                val c = ((q.x - x0) / cs).toInt().coerceIn(0, cols - 1)
                val r = ((q.y - y0) / cs).toInt().coerceIn(0, rows - 1)
                val targetNum = (start + r * cols + c).toString()
                val cur = latest.xStr("blanks", "15,35,60,85").split(',', '،', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (targetNum in cur) cur.remove(targetNum) else cur.add(targetNum)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("blanks" to JsonPrimitive(cur.joinToString(","))))))))
            }
            "axfacttree", "axfnmachine" -> {
                val cur = latest.xNum("blank", 1f).toInt()
                val nextB = (cur + 1) % 3
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("blank" to JsonPrimitive(nextB)))))))
            }
            "axfracwall" -> {
                val dens = latest.xStr("dens", "1,2,3,4,6,8").split(',', '،', ';').mapNotNull { it.trim().toIntOrNull()?.coerceIn(1, 12) }.ifEmpty { listOf(1, 2, 3, 4, 6, 8) }.take(7)
                val fills = latest.xStr("fills", "1,1,2,2,3,4").split(',', '،', ';').mapNotNull { it.trim().toIntOrNull() }.toMutableList()
                while (fills.size < dens.size) fills.add(1)
                val rh = kotlin.math.min(34f, 210f / dens.size)
                val y0 = 30f + (214f - rh * dens.size) / 2f
                val rIdx = ((q.y - y0) / rh).toInt().coerceIn(0, dens.lastIndex)
                val den = dens[rIdx]
                fills[rIdx] = (fills[rIdx] + 1) % (den + 1)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("fills" to JsonPrimitive(fills.take(dens.size).joinToString(","))))))))
            }
            "axtangram" -> {
                val shapes = listOf("square", "house", "boat", "cat")
                val cur = latest.xStr("shape", "square").lowercase()
                val nextS = shapes[(shapes.indexOf(cur) + 1) % shapes.size]
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("shape" to JsonPrimitive(nextS)))))))
            }
            "axsym" -> {
                val c = ((q.x - 76f) / 26f).toInt().coerceIn(0, 7)
                val r = ((q.y - 32f) / 26f).toInt().coerceIn(0, 7)
                val axis = latest.xStr("axis", "v").lowercase()
                val pair = if (axis == "h") {
                    val d1 = if (r < 4) 4 - r else r - 3
                    "${c + 1},$d1"
                } else {
                    val d1 = if (c < 4) 4 - c else c - 3
                    "${r + 1},$d1"
                }
                val cur = latest.xStr("cells", "1,1;2,1;2,2;3,2").split(';', '؛').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (pair in cur && cur.size > 1) cur.remove(pair) else if (pair !in cur) cur.add(pair)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("cells" to JsonPrimitive(cur.joinToString(";"))))))))
            }
            "axgeoboard" -> {
                val g = latest.xNum("grid", 5f).toInt().coerceIn(3, 7)
                val step = 200f / (g - 1).coerceAtLeast(1)
                val px = Math.round((q.x - 80f) / step).coerceIn(0, g - 1) + 1
                val py = g - Math.round((q.y - 36f) / step).coerceIn(0, g - 1)
                val key = "$px,$py"
                val cur = latest.xStr("pts", "1,1;4,1;4,3;1,3").split(';', '؛').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (key in cur && cur.size > 3) cur.remove(key) else if (key !in cur) cur.add(key)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("pts" to JsonPrimitive(cur.joinToString(";"))))))))
            }
            "axdice" -> {
                val list = latest.xStr("dice", "4,6").split(',', '،').mapNotNull { it.trim().toIntOrNull()?.coerceIn(1, 6) }.ifEmpty { listOf(4, 6) }.toMutableList()
                val totalW = list.size * 66f + (list.size - 1) * 22f
                val x0 = (360f - totalW) / 2f
                val idx = ((q.x - x0) / 88f).toInt().coerceIn(0, list.lastIndex)
                list[idx] = (list[idx] % 6) + 1
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("dice" to JsonPrimitive(list.joinToString(","))))))))
            }
            "axmularea" -> {
                val cur = latest.xNum("blank", 0f).toInt()
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("blank" to JsonPrimitive(if (cur == 0) 1 else 0)))))))
            }
            "axtenframe" -> {
                val frames = latest.xNum("frames", 2f).toInt().coerceIn(1, 2)
                val cap = frames * 10
                val a = latest.xNum("a", 7f).toInt().coerceIn(0, cap)
                val b = latest.xNum("b", 5f).toInt().coerceIn(0, cap - a)
                val next = if (q.x <= 180f) {
                    val na = (a + 1) % (cap - b + 1)
                    x + ("a" to JsonPrimitive(na))
                } else {
                    val nb = (b + 1) % (cap - a + 1)
                    x + ("b" to JsonPrimitive(nb))
                }
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(next)))))
            }
            "axspin" -> {
                val parts = latest.xStr("parts", "قرمز,آبی,سبز,زرد,قرمز,آبی").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
                val n = parts.size.coerceAtLeast(2)
                var deg = atan2((q.y - 142f).toDouble(), (q.x - 180f).toDouble()) * 180.0 / PI + 90.0
                if (deg < 0) deg += 360.0
                val nextArrow = ((deg % 360.0) / (360.0 / n)).toInt().coerceIn(0, n - 1) + 1
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("arrow" to JsonPrimitive(nextArrow)))))))
            }
            "axvenn" -> {
                val key = if (q.x < 135f) "left" else if (q.x > 225f) "right" else "mid"
                val fb = if (key == "left") "پرنده: کبوتر، گنجشک" else if (key == "right") "آبزی: ماهی، میگو" else "اردک"
                val cur = latest.xStr(key, fb)
                val nextVal = if (cur.trim() == "؟" || cur.endsWith(": ؟")) fb else if (':' in cur) cur.substringBefore(':') + ": ؟" else "؟"
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + (key to JsonPrimitive(nextVal)))))))
            }
            "axnumpts" -> {
                val xmin = latest.xNum("xmin", -5f)
                val xmax = kotlin.math.max(xmin + 2f, latest.xNum("xmax", 5f))
                val v = Math.round((xmin + ((q.x - 40f) / (336f - 40f)).coerceIn(0f, 1f) * (xmax - xmin)) * 2f) / 2f
                val vs = if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
                val cur = latest.xList("pts", "-2,1.5,4").toMutableList()
                if (vs in cur && cur.size > 1) cur.remove(vs) else if (vs !in cur) cur.add(vs)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("pts" to JsonPrimitive(cur.joinToString(","))))))))
            }
            "axxypts" -> {
                val xmin = latest.xNum("xmin", -5f)
                val xmax = kotlin.math.max(xmin + 2f, latest.xNum("xmax", 5f))
                val ymin = latest.xNum("ymin", -4f)
                val ymax = kotlin.math.max(ymin + 2f, latest.xNum("ymax", 4f))
                val xv = Math.round(xmin + ((q.x - 40f) / (336f - 40f)).coerceIn(0f, 1f) * (xmax - xmin))
                val yv = Math.round(ymin + ((246f - q.y) / (246f - 26f)).coerceIn(0f, 1f) * (ymax - ymin))
                val key = "$xv,$yv"
                val cur = latest.xStr("pts", "1,2;-3,1;2,-2").split(';', '؛').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (key in cur && cur.size > 1) cur.remove(key) else if (key !in cur) cur.add(key)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("pts" to JsonPrimitive(cur.joinToString(";"))))))))
            }
            "axpolpts" -> {
                val rings = latest.xNum("xmax", 4f).toInt().coerceIn(1, 12)
                val rMax = 104f
                val dx = q.x - 180f
                val dy = 136f - q.y
                val rr = (hypot(dx, dy) / rMax * rings).coerceIn(1f, rings.toFloat())
                val deg = (((atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI + 360.0) % 360.0) / 30.0).let { Math.round(it) * 30 } % 360
                val key = "${Math.round(rr)},$deg"
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("pts" to JsonPrimitive(key)))))))
            }
            "ngon" -> {
                val n = ((latest.xNum("n", 6f).toInt() - 2) % 10) + 3
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("n" to JsonPrimitive(n.toString())))))))
            }
            "axgate" -> {
                val key = if (q.y > 132f) "b" else "a"
                val cur = latest.xStr(key, if (key == "a") "1" else "0")
                val next = if (cur == "1") "0" else "1"
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + (key to JsonPrimitive(next)))))))
            }
            else -> {
                val cyc = ADV_CYCLE_PARAMS[latest.type]
                val nmp = ADV_NUM_PARAMS[latest.type]
                if (cyc != null) {
                    val (key, vals) = cyc
                    val cur = latest.xStr(key, vals.first())
                    val idx = vals.indexOf(cur)
                    val next = vals[(idx + 1) % vals.size]
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + (key to JsonPrimitive(next)))))))
                } else if (nmp != null) {
                    val (key, step, range) = nmp
                    val cur = latest.xNum(key, range.start)
                    var next = Math.round((cur + step) * 100f) / 100f
                    if (next > range.endInclusive) next = range.start
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + (key to JsonPrimitive(next)))))))
                }
            }
        }
    }

    val dragTypes = setOf(
        "axclock", "axthermo", "axbeaker", "axruler", "axangle",
        "axtrigcircle",
        "axnumint", "axnumineq", "axxyline", "axxyvec", "axxycirc",
        "gauge", "ang", "sec", "arc", "segm", "parll"
    )
    Box(
        modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(latest.type) {
                if (latest.type !in dragTypes) {
                    detectTapGestures { p -> tapAt(p) }
                } else {
                    detectDragGestures(
                        onDragStart = { p ->
                            val q = toSvg(p)
                            if (q != null) {
                                grab = if (latest.type != "axclock") "thermo" else if (hypot(q.x - 180f, q.y - 150f) <= 62f) "hour" else "minute"
                                moveTo(p)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            moveTo(change.position)
                        },
                        onDragEnd = { grab = null },
                        onDragCancel = { grab = null }
                    )
                }
            }
    ) {
        InlineFigureView(spec = latest, modifier = Modifier.fillMaxSize(), contentDescription = contentDescription)
    }
}
