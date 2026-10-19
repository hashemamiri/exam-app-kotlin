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
        "axsaw", "axpattern", "axsym", "axgeoboard", "axdice", "axmularea"
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

    // مختصات صفحه → مختصات SVG (بوم 360×280 با ContentScale.Fit وسط‌چین)
    fun toSvg(p: Offset): Offset? {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w <= 0f || h <= 0f) return null
        val sc = min(w / 360f, h / 280f)
        return Offset((p.x - (w - 360f * sc) / 2f) / sc, (p.y - (h - 280f * sc) / 2f) / sc)
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
                val cur = latest.xNum("n", 134f).toLong().coerceIn(0L, 999999L)
                val delta = if (q.x < 120f) 100L else if (q.x < 240f) 10L else 1L
                val nextN = (cur + delta).coerceAtMost(999999L)
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("n" to JsonPrimitive(nextN)))))))
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
                val cur = latest.xStr("sum", "1")
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("sum" to JsonPrimitive(if (cur == "0") "1" else "0")))))))
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
                val c = ((q.x - 80f) / 50f).toInt().coerceIn(0, 3)
                val r = ((q.y - 36f) / 50f).toInt().coerceIn(0, 3)
                val raw = latest.xStr("vals", "1,2,3,4;3,4,?,2;2,?,4,3;4,3,2,1")
                val grid = MutableList(4) { ri ->
                    val row = raw.split(';').getOrNull(ri)?.split(',')?.map { it.trim() }.orEmpty()
                    MutableList(4) { ci -> row.getOrNull(ci)?.ifBlank { "?" } ?: "?" }
                }
                val cycle = listOf("1", "2", "3", "4", "?")
                val cur = grid[r][c]
                grid[r][c] = cycle[(cycle.indexOf(cur) + 1) % cycle.size]
                val nextVals = grid.joinToString(";") { it.joinToString(",") }
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("vals" to JsonPrimitive(nextVals)))))))
            }
            "axpattern" -> {
                val items = latest.xStr("seq", "●,▲,●,▲,●,?").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (items.isNotEmpty()) {
                    val n = items.size
                    val cw = kotlin.math.min(42f, 290f / n)
                    val gap = 6f
                    val tw = n * cw + (n - 1) * gap
                    val x0 = (360f - tw) / 2f
                    val idx = ((q.x - x0) / (cw + gap)).toInt().coerceIn(0, n - 1)
                    val cycle = listOf("●", "▲", "■", "★", "?")
                    items[idx] = cycle[(cycle.indexOf(items[idx]) + 1) % cycle.size]
                    emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("seq" to JsonPrimitive(items.joinToString(","))))))))
                }
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
                val parts = latest.xStr("parts", "قرمز,آبی,سبز,زرد").split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }
                val n = parts.size.coerceAtLeast(2)
                val cur = latest.xNum("arrow", 1f).toInt().coerceIn(1, n)
                val nextArrow = (cur % n) + 1
                emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(x + ("arrow" to JsonPrimitive(nextArrow)))))))
            }
        }
    }

    Box(
        modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(latest.type) {
                if (latest.type !in setOf("axclock", "axthermo", "axbeaker", "axruler")) {
                    detectTapGestures { p -> tapAt(p) }
                } else {
                    detectDragGestures(
                        onDragStart = { p ->
                            val q = toSvg(p)
                            if (q != null) {
                                grab = if (latest.type in setOf("axthermo", "axbeaker", "axruler")) "thermo" else if (hypot(q.x - 180f, q.y - 150f) <= 62f) "hour" else "minute"
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
