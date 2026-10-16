package ir.exam.app.ui.figure

import androidx.compose.foundation.gestures.detectDragGestures
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
    if (spec.type == "axclock") {
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
        val nx = if (mode == "hour") {
            x + ("h" to JsonPrimitive(Math.round(deg / 30.0).toInt() % 12))
        } else {
            x + ("m" to JsonPrimitive(Math.round(deg / 6.0).toInt() % 60))
        }
        emit(FigureSpec(JsonObject(latest.raw + ("X" to JsonObject(nx)))))
    }

    Box(
        modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p ->
                        val q = toSvg(p)
                        if (q != null) {
                            grab = if (hypot(q.x - 180f, q.y - 150f) <= 62f) "hour" else "minute"
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
    ) {
        InlineFigureView(spec = latest, modifier = Modifier.fillMaxSize(), contentDescription = contentDescription)
    }
}
