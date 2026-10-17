package ir.exam.app.core.math

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max

data class MathCanvasSize(val width: Float, val height: Float)

/** همان AST فرمول را مستقیماً روی Canvas/PdfDocument به‌صورت دوبعدی و برداری رسم می‌کند. */
class NativeMathCanvasRenderer {
    fun measure(node: MathNode, size: Float): MathCanvasSize = when (node) {
        is MathNode.Symbol -> {
            val paint = paint(size, node.bold)
            MathCanvasSize(paint.measureText(ir.exam.app.core.figure.FigureDigits.apply(node.value)).coerceAtLeast(2f), size * 1.35f) // V287 — اعداد فارسی در فرمول‌ها
        }
        is MathNode.Sequence -> measureSequence(node, size)
        is MathNode.Fraction -> {
            val top = measure(node.top, size * .78f)
            val bottom = measure(node.bottom, size * .78f)
            MathCanvasSize(max(top.width, bottom.width) + 8f, top.height + bottom.height + 5f)
        }
        is MathNode.Radical -> {
            val body = measure(node.body, size)
            val indexWidth = node.index?.let { measure(it, size * .5f).width } ?: 0f
            MathCanvasSize(body.width + size * .8f + indexWidth, body.height + 4f)
        }
        is MathNode.Script -> {
            val base = measure(node.base, size)
            val upper = node.upper?.let { measure(it, size * .58f) }
            val lower = node.lower?.let { measure(it, size * .58f) }
            MathCanvasSize(
                base.width + max(upper?.width ?: 0f, lower?.width ?: 0f),
                max(base.height, (upper?.height ?: 0f) + (lower?.height ?: 0f))
            )
        }
        is MathNode.Matrix -> {
            val columns = node.rows.maxOfOrNull { it.size } ?: 0
            val widths = (0 until columns).map { column ->
                node.rows.maxOfOrNull { row ->
                    row.getOrNull(column)?.let { measure(it, size * .75f).width } ?: 0f
                } ?: 0f
            }
            MathCanvasSize(widths.sum() + columns * 10f + size, node.rows.size * size * 1.25f)
        }
        is MathNode.Accent -> {
            val body = measure(node.body, size)
            MathCanvasSize(body.width, max(body.height, size * 1.6f))
        }
        is MathNode.Delimited -> {
            val body = measure(node.body, size)
            MathCanvasSize(body.width + size, max(body.height, size * 1.5f))
        }
        MathNode.LineBreak -> MathCanvasSize(0f, size * 1.35f)
    }

    fun draw(
        canvas: Canvas,
        node: MathNode,
        x: Float,
        top: Float,
        size: Float,
        color: Int = android.graphics.Color.BLACK
    ): Float = when (node) {
        is MathNode.Symbol -> {
            val paint = paint(size, node.bold, color)
            canvas.drawText(ir.exam.app.core.figure.FigureDigits.apply(node.value), x, top + size, paint) // V287
            measure(node, size).width
        }
        is MathNode.Sequence -> drawSequence(canvas, node, x, top, size, color)
        is MathNode.Fraction -> {
            val all = measure(node, size)
            val numerator = measure(node.top, size * .78f)
            val denominator = measure(node.bottom, size * .78f)
            draw(canvas, node.top, x + (all.width - numerator.width) / 2, top, size * .78f, color)
            val lineY = top + numerator.height + 1
            canvas.drawLine(x, lineY, x + all.width, lineY, paint(1f, false, color))
            draw(
                canvas,
                node.bottom,
                x + (all.width - denominator.width) / 2,
                lineY + 2,
                size * .78f,
                color
            )
            all.width
        }
        is MathNode.Radical -> {
            val body = measure(node.body, size)
            val radicalPaint = paint(size, false, color)
            val indexWidth = node.index?.let { draw(canvas, it, x, top, size * .5f, color) } ?: 0f
            canvas.drawText("√", x + indexWidth, top + size, radicalPaint)
            val bodyX = x + indexWidth + size * .7f
            canvas.drawLine(bodyX, top + 2, bodyX + body.width, top + 2, radicalPaint)
            draw(canvas, node.body, bodyX, top, size, color)
            body.width + size * .8f + indexWidth
        }
        is MathNode.Script -> {
            val base = measure(node.base, size)
            draw(canvas, node.base, x, top, size, color)
            node.upper?.let { draw(canvas, it, x + base.width, top, size * .58f, color) }
            node.lower?.let { draw(canvas, it, x + base.width, top + size * .72f, size * .58f, color) }
            measure(node, size).width
        }
        is MathNode.Matrix -> {
            val all = measure(node, size)
            val isVop = node.delimiter in charArrayOf('+', '-', '*')
            val isBdiv = node.delimiter == '\\'
            val isLdiv = node.delimiter == '/' || isBdiv
            val hasFrame = node.delimiter != ' ' && !isVop && !isLdiv
            if (hasFrame) {
                canvas.drawText(node.delimiter.toString(), x, top + size, paint(size * 1.4f, false, color))
            }
            var rowY = top
            var col0MaxW = 0f
            val subLineYs = mutableListOf<Float>()
            node.rows.forEachIndexed { rowIdx, row ->
                var cellX = x + (if (hasFrame) size * .65f else size * .15f)
                row.forEachIndexed { colIdx, cell ->
                    val w = draw(canvas, cell, cellX, rowY, size * .75f, color)
                    if (colIdx == 0 && w > col0MaxW) col0MaxW = w
                    cellX += w + 10f
                }
                rowY += size * 1.25f
                val sym = row.firstOrNull() as? MathNode.Symbol
                if (isVop && sym != null && !sym.editable) {
                    canvas.drawLine(x + size * .1f, rowY - size * .2f, x + all.width - size * .1f, rowY - size * .2f, paint(1.2f, false, color))
                } else if (isBdiv && rowIdx % 2 == 1) {
                    subLineYs += rowY - size * .2f
                }
            }
            if (isLdiv) {
                val vX = x + size * .15f + col0MaxW + 5f
                val hY = top + size * 1.05f
                val lp = paint(1.2f, false, color)
                canvas.drawLine(vX, top + size * .1f, vX, rowY - size * .1f, lp)
                canvas.drawLine(vX, hY, x + all.width - size * .1f, hY, lp)
                if (isBdiv) {
                    subLineYs.forEach { sy ->
                        canvas.drawLine(x + size * .1f, sy, vX, sy, lp)
                    }
                }
            } else if (hasFrame) {
                val close = when (node.delimiter) {
                    '(' -> ")"
                    '{', ' ' -> ""
                    '|' -> "|"
                    else -> "]"
                }
                if (close.isNotEmpty()) {
                    canvas.drawText(close, x + all.width - size * .3f, top + size, paint(size * 1.4f, false, color))
                }
            }
            all.width
        }
        is MathNode.Accent -> {
            val body = measure(node.body, size)
            if (node.mark == "underline") {
                draw(canvas, node.body, x, top, size, color)
                canvas.drawLine(x, top + body.height, x + body.width, top + body.height, paint(1.2f, false, color))
            } else {
                val mark = when (node.mark) {
                    "hat" -> "ˆ"
                    "bar" -> "¯"
                    "vec" -> "→"
                    "dot" -> "˙"
                    else -> node.mark
                }
                canvas.drawText(mark, x + body.width / 2 - size * .2f, top + size * .45f, paint(size * .7f, false, color))
                draw(canvas, node.body, x, top + size * .35f, size, color)
            }
            body.width
        }
        is MathNode.Delimited -> {
            val body = measure(node.body, size)
            val delimiterPaint = paint(max(size * 1.15f, body.height * .8f), false, color)
            val sideWidth = size * .45f
            if (node.open.isNotEmpty()) canvas.drawText(node.open, x, top + body.height * .82f, delimiterPaint)
            draw(canvas, node.body, x + sideWidth, top, size, color)
            if (node.close.isNotEmpty()) {
                canvas.drawText(node.close, x + sideWidth + body.width, top + body.height * .82f, delimiterPaint)
            }
            body.width + size
        }
        MathNode.LineBreak -> 0f
    }

    private fun measureSequence(node: MathNode.Sequence, size: Float): MathCanvasSize {
        val lines = mutableListOf<MutableList<MathNode>>(mutableListOf())
        node.children.forEach { child ->
            if (child == MathNode.LineBreak) lines.add(mutableListOf())
            else lines.last().add(child)
        }
        val lineSizes = lines.map { line ->
            val parts = line.map { measure(it, size) }
            MathCanvasSize(
                parts.sumOf { it.width.toDouble() }.toFloat(),
                parts.maxOfOrNull { it.height } ?: size * 1.35f
            )
        }
        return MathCanvasSize(
            lineSizes.maxOfOrNull { it.width } ?: 0f,
            lineSizes.sumOf { it.height.toDouble() }.toFloat()
        )
    }

    private fun drawSequence(
        canvas: Canvas,
        node: MathNode.Sequence,
        x: Float,
        top: Float,
        size: Float,
        color: Int
    ): Float {
        var currentX = x
        var currentTop = top
        var maxWidth = 0f
        var lineHeight = size * 1.35f
        node.children.forEach { child ->
            if (child == MathNode.LineBreak) {
                maxWidth = max(maxWidth, currentX - x)
                currentX = x
                currentTop += lineHeight
                lineHeight = size * 1.35f
            } else {
                lineHeight = max(lineHeight, measure(child, size).height)
                currentX += draw(canvas, child, currentX, currentTop, size, color)
            }
        }
        return max(maxWidth, currentX - x)
    }

    private fun paint(
        size: Float,
        bold: Boolean,
        color: Int = android.graphics.Color.BLACK
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create("serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        strokeWidth = 1f
    }
}
