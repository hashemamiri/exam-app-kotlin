package ir.exam.app.ui.math

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ir.exam.app.core.math.MathNode
import ir.exam.app.core.math.MathSourceRange
import ir.exam.app.core.math.NativeMathCanvasRenderer
import ir.exam.app.core.math.NativeMathParser

/**
 * ویرایشگر ساختاری کاملاً نیتیو: رسم مستقیم Android Canvas، بدون HTML، JavaScript یا WebView.
 *
 * رندر و اندازه‌گیری از همان AST نیتیو استفاده می‌کند. برای لمس، خانه‌های برگ AST از روی
 * مختصات Canvas ساخته می‌شوند و محدودهٔ متن اصلی را برمی‌گردانند تا keypad و کتابخانه
 * همچنان همان قرارداد FormulaBoxEditor را مصرف کنند.
 */
@Composable
fun NativeMathCanvasEditorView(
    tex: String,
    selectionStart: Int,
    selectionEnd: Int,
    zoom: Float,
    onBoxTap: (MathSourceRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sizePx = with(density) { (22f * zoom).dp.toPx() }
    val viewportHeightPx = with(density) { 180.dp.toPx() }
    val parsed = remember(tex) { runCatching { NativeMathParser.parse(tex) }.getOrElse { MathNode.Sequence(emptyList()) } }
    val renderer = remember { NativeMathCanvasRenderer() }
    val measured = remember(parsed, sizePx) { renderer.measure(parsed, sizePx) }
    val leaves = remember(parsed, sizePx) { editableLeaves(parsed, renderer, sizePx) }
    val scroll = rememberScrollState()
    val canvasWidth = with(density) { measured.width.toDp() }.coerceAtLeast(180.dp)
    Box(modifier.horizontalScroll(scroll)) {
        Canvas(
            Modifier.width(canvasWidth).height(180.dp).pointerInput(tex, selectionStart, selectionEnd) {
                detectTapGestures { point ->
                    val top = ((viewportHeightPx - measured.height) / 2f).coerceAtLeast(8f)
                    leaves.firstOrNull { it.contains(point.x, point.y - top) }?.let { onBoxTap(it.range) }
                }
            }
        ) {
            drawIntoCanvas { nativeCanvas ->
                val top = ((size.height - measured.height) / 2f).coerceAtLeast(8f)
                renderer.draw(nativeCanvas.nativeCanvas, parsed, 8f, top, sizePx, android.graphics.Color.BLACK)
                leaves.filter { it.range.start == minOf(selectionStart, selectionEnd) && it.range.endExclusive == maxOf(selectionStart, selectionEnd) }
                    .forEach { box ->
                        drawRoundRect(
                            color = Color(0xFF10B981),
                            topLeft = Offset(box.left, top + box.top),
                            size = androidx.compose.ui.geometry.Size(box.width, box.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
            }
        }
    }
}

private data class CanvasLeafBox(
    val range: MathSourceRange,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun contains(x: Float, y: Float) = x in left..(left + width) && y in top..(top + height)
}

private fun editableLeaves(node: MathNode, renderer: NativeMathCanvasRenderer, size: Float): List<CanvasLeafBox> {
    val symbols = buildList { collectSymbols(node, this) }
    if (symbols.isEmpty()) return emptyList()
    val total = renderer.measure(node, size).width.coerceAtLeast(1f)
    var x = 8f
    return symbols.map { symbol ->
        val width = (total / symbols.size).coerceAtLeast(size * .7f)
        CanvasLeafBox(MathSourceRange(symbol.sourceStart, symbol.sourceEnd), x, 0f, width, size * 1.6f).also { x += width }
    }
}

private fun collectSymbols(node: MathNode, out: MutableList<MathNode.Symbol>) {
    when (node) {
        is MathNode.Symbol -> if (node.editable && node.sourceStart >= 0 && node.sourceEnd >= node.sourceStart) out += node
        is MathNode.Sequence -> node.children.forEach { collectSymbols(it, out) }
        is MathNode.Fraction -> { collectSymbols(node.top, out); collectSymbols(node.bottom, out) }
        is MathNode.Radical -> { node.index?.let { collectSymbols(it, out) }; collectSymbols(node.body, out) }
        is MathNode.Script -> { collectSymbols(node.base, out); node.upper?.let { collectSymbols(it, out) }; node.lower?.let { collectSymbols(it, out) } }
        is MathNode.Matrix -> node.rows.flatten().forEach { collectSymbols(it, out) }
        is MathNode.Accent -> collectSymbols(node.body, out)
        is MathNode.Delimited -> collectSymbols(node.body, out)
        MathNode.LineBreak -> Unit
    }
}
