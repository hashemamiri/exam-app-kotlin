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
    if (spec.type in setOf("axclock", "axfrac", "axabacus", "axthermo", "axtenframe", "axspin", "axbeaker")) {
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
            val ratio = ((206f - q.y) / (206f - 34f)).coerceIn(0f, 1f)
            val v = Math.round(mn + ratio * (mx - mn))
            x + ("val" to JsonPrimitive(v))
        } else if (latest.type == "axbeaker") {
            val mx = kotlin.math.max(10f, latest.xNum("max", 500f))
            val ratio = ((224f - q.y) / (224f - 38f)).coerceIn(0f, 1f)
            val step = if (mx >= 100f) 10 else 1
            val v = (Math.round((ratio * mx) / step) * step).coerceIn(0, mx.toInt())
            x + ("val" to JsonPrimitive(v))
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
            "axthermo", "axbeaker" -> {
                grab = "thermo"
                moveTo(p)
                grab = null
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
                if (latest.type in setOf("axfrac", "axabacus", "axtenframe", "axspin")) {
                    detectTapGestures { p -> tapAt(p) }
                } else {
                    detectDragGestures(
                        onDragStart = { p ->
                            val q = toSvg(p)
                            if (q != null) {
                                grab = if (latest.type == "axthermo" || latest.type == "axbeaker") "thermo" else if (hypot(q.x - 180f, q.y - 150f) <= 62f) "hour" else "minute"
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
