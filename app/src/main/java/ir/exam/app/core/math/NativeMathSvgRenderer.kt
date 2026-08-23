package ir.exam.app.core.math

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/** محدودهٔ دقیق یک خانهٔ قابل لمس در دستگاه مختصات SVG. */
data class MathSvgEditBox(
    val sourceStart: Int,
    val sourceEnd: Int,
    val xPx: Float,
    val yPx: Float,
    val widthPx: Float,
    val heightPx: Float
) {
    fun contains(x: Float, y: Float): Boolean =
        x >= xPx && x <= xPx + widthPx && y >= yPx && y <= yPx + heightPx
}

/** خط افقی رادیکال برای تست قطعی کش‌آمدن همراه محتوای داخل ریشه. */
data class MathSvgRadicalBar(
    val startXPx: Float,
    val endXPx: Float,
    val yPx: Float
)

/** یک سند SVG مستقل، بدون URL خارجی، script، style یا foreignObject. */
data class MathSvgDocument(
    val xml: String,
    val widthPx: Float,
    val heightPx: Float,
    val cacheKey: String,
    val editBoxes: List<MathSvgEditBox> = emptyList(),
    val radicalBars: List<MathSvgRadicalBar> = emptyList()
)

/**
 * AST بومی فرمول را به SVG امن، مستقل و در حالت ویرایش جعبه‌ای تبدیل می‌کند.
 *
 * تنها elementهای تولیدشده `svg`، `g`، `text`، `rect`، `path`، `line` و `circle`
 * هستند. ورودی کاربر همیشه XML-escape می‌شود. مختصات خانه‌ها همراه SVG برگردانده
 * می‌شوند تا لمس هر خانه، selection واقعی همان بخش را فعال کند.
 */
object NativeMathSvgRenderer {
    private const val MIN_FONT = 8f
    private const val MAX_FONT = 160f
    private const val MAX_DIMENSION = 16_384f

    fun render(
        tex: String,
        fontSizePx: Float = 32f,
        color: String = "#111111",
        opacity: Float = 1f,
        showEditBoxes: Boolean = false,
        activeStart: Int = -1,
        activeEnd: Int = activeStart,
        boxColor: String = "#78909C",
        activeBoxColor: String = "#FF8F00",
        showOnlyActiveBox: Boolean = false
    ): MathSvgDocument {
        val safeFont = fontSizePx.coerceIn(MIN_FONT, MAX_FONT)
        val safeColor = sanitizeColor(color)
        val safeBoxColor = sanitizeColor(boxColor)
        val safeActiveColor = sanitizeColor(activeBoxColor)
        val safeOpacity = opacity.coerceIn(0f, 1f)
        val node = runCatching { NativeMathParser.parse(tex) }.getOrElse {
            MathNode.Symbol("□", sourceStart = 0, sourceEnd = tex.length)
        }
        val layout = layout(node, safeFont)
        val paddingX = safeFont * .12f
        val paddingY = safeFont * .10f
        val width = (layout.width + paddingX * 2).coerceIn(1f, MAX_DIMENSION)
        val height = (layout.height + paddingY * 2).coerceIn(1f, MAX_DIMENSION)
        val body = translate(layout.body, paddingX, paddingY)
        val layoutBoxes = mergeBoxes(layout.boxes.map { it.moved(paddingX, paddingY) }, safeFont)
        val editBoxes = layoutBoxes.map {
            MathSvgEditBox(it.sourceStart, it.sourceEnd, it.x, it.y, it.width, it.height)
        }
        val radicalBars = layout.radicalBars.map {
            MathSvgRadicalBar(it.startX + paddingX, it.endX + paddingX, it.y + paddingY)
        }
        val activeIndex = activeBoxIndex(editBoxes, activeStart, activeEnd)
        val boxLayer = if (showEditBoxes) {
            renderBoxLayer(
                boxes = editBoxes,
                activeIndex = activeIndex,
                boxColor = safeBoxColor,
                activeColor = safeActiveColor,
                size = safeFont,
                onlyActive = showOnlyActiveBox
            )
        } else {
            ""
        }
        val xml = buildString(body.length + boxLayer.length + 360) {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
            append(number(width)).append(' ').append(number(height))
            append("\" width=\"").append(number(width)).append("\" height=\"")
            append(number(height)).append("\" preserveAspectRatio=\"xMinYMid meet\">")
            append("<title>فرمول ریاضی</title>")
            append(boxLayer)
            append("<g fill=\"").append(safeColor).append("\" stroke=\"").append(safeColor)
            append("\" fill-opacity=\"").append(number(safeOpacity)).append("\" stroke-opacity=\"")
            append(number(safeOpacity))
            append("\" stroke-linecap=\"round\" stroke-linejoin=\"round\">")
            append(body)
            append("</g></svg>")
        }
        val digest = sha256(
            "$safeFont|$safeColor|$safeOpacity|$showEditBoxes|$activeStart|$activeEnd|" +
                "$safeBoxColor|$safeActiveColor|$showOnlyActiveBox|$tex"
        )
        return MathSvgDocument(
            xml = xml,
            widthPx = width,
            heightPx = height,
            cacheKey = "native-math-svg-$digest",
            editBoxes = editBoxes,
            radicalBars = radicalBars
        )
    }

    private data class LayoutBox(
        val sourceStart: Int,
        val sourceEnd: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val mergeable: Boolean
    ) {
        fun moved(dx: Float, dy: Float) = copy(x = x + dx, y = y + dy)
    }

    private data class RadicalLine(val startX: Float, val endX: Float, val y: Float) {
        fun moved(dx: Float, dy: Float) = copy(startX = startX + dx, endX = endX + dx, y = y + dy)
    }

    private data class Layout(
        val width: Float,
        val height: Float,
        val baseline: Float,
        val body: String,
        val boxes: List<LayoutBox> = emptyList(),
        val radicalBars: List<RadicalLine> = emptyList()
    )

    private fun layout(node: MathNode, size: Float): Layout = when (node) {
        is MathNode.Symbol -> symbol(node, size)
        is MathNode.Sequence -> sequence(node, size)
        is MathNode.Fraction -> fraction(node, size)
        is MathNode.Radical -> radical(node, size)
        is MathNode.Script -> script(node, size)
        is MathNode.Matrix -> matrix(node, size)
        is MathNode.Accent -> accent(node, size)
        is MathNode.Delimited -> delimited(node, size)
        MathNode.LineBreak -> Layout(0f, size * 1.32f, size, "")
    }

    private fun symbol(node: MathNode.Symbol, size: Float): Layout {
        val naturalWidth = estimateTextWidth(node.value, size)
        val editable = node.editable && node.sourceStart >= 0 && node.sourceEnd >= node.sourceStart
        val width = if (editable) max(naturalWidth, size * .72f) else naturalWidth.coerceAtLeast(size * .08f)
        val baseline = size * .94f
        val height = size * 1.24f
        if (node.value.isEmpty()) {
            val box = if (editable) {
                listOf(
                    LayoutBox(
                        node.sourceStart,
                        node.sourceEnd,
                        size * .04f,
                        size * .02f,
                        (width - size * .08f).coerceAtLeast(size * .62f),
                        height - size * .04f,
                        mergeable = false
                    )
                )
            } else emptyList()
            return Layout(width, height, baseline, "", box)
        }

        val italic = node.value.codePointCount(0, node.value.length) == 1 &&
            node.value.firstOrNull()?.isLetter() == true && node.value.all { it.code < 128 }
        val rtl = node.value.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }
        val textX = (width - naturalWidth) / 2f + if (rtl) naturalWidth else 0f
        val body = buildString {
            append("<text x=\"").append(number(textX)).append("\" y=\"")
            append(number(baseline))
            append("\" font-family=\"serif\" font-size=\"").append(number(size)).append('"')
            if (node.bold) append(" font-weight=\"700\"")
            if (italic) append(" font-style=\"italic\"")
            if (rtl) append(" text-anchor=\"end\" direction=\"rtl\"")
            else append(" direction=\"ltr\"")
            append(" unicode-bidi=\"bidi-override\">")
            append(escapeXml(node.value)).append("</text>")
        }
        val plainValue = node.value.codePoints().allMatch { Character.isLetterOrDigit(it) }
        val sourceLength = node.sourceEnd - node.sourceStart
        val mergeable = editable && plainValue && sourceLength == node.value.length
        val boxes = if (editable) {
            listOf(
                LayoutBox(
                    node.sourceStart,
                    node.sourceEnd,
                    size * .025f,
                    size * .015f,
                    (width - size * .05f).coerceAtLeast(size * .60f),
                    height - size * .03f,
                    mergeable
                )
            )
        } else emptyList()
        return Layout(width, height, baseline, body, boxes)
    }

    private fun sequence(node: MathNode.Sequence, size: Float): Layout {
        val lines = mutableListOf<MutableList<MathNode>>(mutableListOf())
        node.children.forEach { child ->
            if (child == MathNode.LineBreak) lines.add(mutableListOf()) else lines.last().add(child)
        }
        val layouts = lines.map { line -> horizontal(line.map { layout(it, size) }, size) }
        if (layouts.size == 1) return layouts.first()
        val gap = size * .28f
        val width = layouts.maxOfOrNull { it.width } ?: size * .2f
        var y = 0f
        val boxes = mutableListOf<LayoutBox>()
        val bars = mutableListOf<RadicalLine>()
        val body = buildString {
            layouts.forEach { line ->
                append(translate(line.body, 0f, y))
                boxes += line.boxes.map { it.moved(0f, y) }
                bars += line.radicalBars.map { it.moved(0f, y) }
                y += line.height + gap
            }
        }
        return Layout(
            width,
            (y - gap).coerceAtLeast(size),
            layouts.first().baseline,
            body,
            boxes,
            bars
        )
    }

    private fun horizontal(items: List<Layout>, size: Float): Layout {
        if (items.isEmpty()) return Layout(size * .2f, size * 1.24f, size * .94f, "")
        val baseline = items.maxOf { it.baseline }
        val descent = items.maxOf { it.height - it.baseline }
        var x = 0f
        val boxes = mutableListOf<LayoutBox>()
        val bars = mutableListOf<RadicalLine>()
        val body = buildString {
            items.forEach { item ->
                val y = baseline - item.baseline
                append(translate(item.body, x, y))
                boxes += item.boxes.map { it.moved(x, y) }
                bars += item.radicalBars.map { it.moved(x, y) }
                x += item.width
            }
        }
        return Layout(x, baseline + descent, baseline, body, boxes, bars)
    }

    private fun fraction(node: MathNode.Fraction, size: Float): Layout {
        val numerator = layout(node.top, size * .76f)
        val denominator = layout(node.bottom, size * .76f)
        val padding = size * .18f
        val gap = size * .10f
        val width = max(numerator.width, denominator.width) + padding * 2
        val topX = (width - numerator.width) / 2f
        val lineY = numerator.height + gap
        val bottomY = lineY + gap
        val bottomX = (width - denominator.width) / 2f
        val height = bottomY + denominator.height
        val body = buildString {
            append(translate(numerator.body, topX, 0f))
            append("<line x1=\"").append(number(padding * .25f)).append("\" y1=\"")
            append(number(lineY)).append("\" x2=\"").append(number(width - padding * .25f))
            append("\" y2=\"").append(number(lineY)).append("\" stroke-width=\"")
            append(number(max(1.2f, size * .055f))).append("\"/>")
            append(translate(denominator.body, bottomX, bottomY))
        }
        return Layout(
            width,
            height,
            lineY + size * .35f,
            body,
            numerator.boxes.map { it.moved(topX, 0f) } + denominator.boxes.map { it.moved(bottomX, bottomY) },
            numerator.radicalBars.map { it.moved(topX, 0f) } +
                denominator.radicalBars.map { it.moved(bottomX, bottomY) }
        )
    }

    private fun radical(node: MathNode.Radical, size: Float): Layout {
        val radicand = layout(node.body, size)
        val rootIndex = node.index?.let { layout(it, size * .46f) }
        val indexWidth = rootIndex?.width ?: 0f
        val hookWidth = size * .72f
        val topPadding = size * .15f
        val bodyX = indexWidth + hookWidth
        val bodyY = topPadding + size * .08f
        val width = bodyX + radicand.width + size * .10f
        val height = max(radicand.height + topPadding, size * 1.30f)
        val stroke = max(1.3f, size * .065f)
        val hookStartX = indexWidth + size * .04f
        val middleY = topPadding + radicand.height * .58f
        val lowY = topPadding + radicand.height * .84f
        val topY = topPadding
        val indexY = topPadding + size * .22f
        val body = buildString {
            rootIndex?.let { append(translate(it.body, 0f, indexY)) }
            append("<path d=\"M ").append(number(hookStartX)).append(' ').append(number(middleY))
            append(" L ").append(number(hookStartX + size * .18f)).append(' ').append(number(middleY))
            append(" L ").append(number(hookStartX + size * .28f)).append(' ').append(number(lowY))
            append(" L ").append(number(bodyX - size * .08f)).append(' ').append(number(topY))
            append(" L ").append(number(width)).append(' ').append(number(topY))
            append("\" fill=\"none\" stroke-width=\"").append(number(stroke)).append("\"/>")
            append(translate(radicand.body, bodyX, bodyY))
        }
        val ownBar = RadicalLine(bodyX - size * .08f, width, topY)
        return Layout(
            width,
            height + size * .08f,
            bodyY + radicand.baseline,
            body,
            (rootIndex?.boxes.orEmpty().map { it.moved(0f, indexY) }) +
                radicand.boxes.map { it.moved(bodyX, bodyY) },
            (rootIndex?.radicalBars.orEmpty().map { it.moved(0f, indexY) }) +
                radicand.radicalBars.map { it.moved(bodyX, bodyY) } + ownBar
        )
    }

    private fun isDisplayOperator(node: MathNode): Boolean {
        val value = (node as? MathNode.Symbol)?.value ?: return false
        return value in setOf("lim", "max", "min", "∑", "∏", "∐", "∫", "∬", "∭", "∮", "⋃", "⋂")
    }

    private fun script(node: MathNode.Script, size: Float): Layout {
        val operator = isDisplayOperator(node.base)
        val base = layout(node.base, if (operator) size * 1.12f else size)
        val scriptScale = if (operator) .52f else .58f
        val upper = node.upper?.let { layout(it, size * scriptScale) }
        val lower = node.lower?.let { layout(it, size * scriptScale) }
        if (operator) {
            val gap = size * .06f
            val width = max(base.width, max(upper?.width ?: 0f, lower?.width ?: 0f)) + size * .08f
            val upperY = 0f
            val baseY = (upper?.height ?: 0f) + if (upper != null) gap else 0f
            val lowerY = baseY + base.height + if (lower != null) gap else 0f
            val height = lowerY + (lower?.height ?: 0f)
            val baseX = (width - base.width) / 2f
            val upperX = (width - (upper?.width ?: 0f)) / 2f
            val lowerX = (width - (lower?.width ?: 0f)) / 2f
            val body = buildString {
                upper?.let { append(translate(it.body, upperX, upperY)) }
                append(translate(base.body, baseX, baseY))
                lower?.let { append(translate(it.body, lowerX, lowerY)) }
            }
            return Layout(
                width,
                height,
                baseY + base.baseline,
                body,
                base.boxes.map { it.moved(baseX, baseY) } +
                    upper?.boxes.orEmpty().map { it.moved(upperX, upperY) } +
                    lower?.boxes.orEmpty().map { it.moved(lowerX, lowerY) },
                base.radicalBars.map { it.moved(baseX, baseY) } +
                    upper?.radicalBars.orEmpty().map { it.moved(upperX, upperY) } +
                    lower?.radicalBars.orEmpty().map { it.moved(lowerX, lowerY) }
            )
        }
        val upperLift = if (upper != null) max(size * .30f, upper.height * .68f) else 0f
        val baseY = upperLift
        val scriptX = base.width + size * .035f
        val lowerY = baseY + base.baseline + size * .12f
        val width = base.width + max(upper?.width ?: 0f, lower?.width ?: 0f) + size * .04f
        val height = max(baseY + base.height, lowerY + (lower?.height ?: 0f))
        val body = buildString {
            append(translate(base.body, 0f, baseY))
            upper?.let { append(translate(it.body, scriptX, 0f)) }
            lower?.let { append(translate(it.body, scriptX, lowerY)) }
        }
        return Layout(
            width,
            height,
            baseY + base.baseline,
            body,
            base.boxes.map { it.moved(0f, baseY) } +
                upper?.boxes.orEmpty().map { it.moved(scriptX, 0f) } +
                lower?.boxes.orEmpty().map { it.moved(scriptX, lowerY) },
            base.radicalBars.map { it.moved(0f, baseY) } +
                upper?.radicalBars.orEmpty().map { it.moved(scriptX, 0f) } +
                lower?.radicalBars.orEmpty().map { it.moved(scriptX, lowerY) }
        )
    }

    private fun matrix(node: MathNode.Matrix, size: Float): Layout {
        val cellSize = size * .72f
        val rows = node.rows.map { row -> row.map { layout(it, cellSize) } }
        val columnCount = rows.maxOfOrNull { it.size } ?: 0
        val columnGap = size * .38f
        val rowGap = size * .24f
        val columnWidths = (0 until columnCount).map { column ->
            rows.maxOfOrNull { row -> row.getOrNull(column)?.width ?: 0f } ?: 0f
        }
        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: cellSize }
        val sideWidth = if (node.delimiter == ' ') size * .10f else size * .42f
        val insideWidth = columnWidths.sum() + columnGap * (columnCount - 1).coerceAtLeast(0)
        val insideHeight = rowHeights.sum() + rowGap * (rows.size - 1).coerceAtLeast(0)
        val width = insideWidth + sideWidth * 2
        val height = insideHeight + size * .20f
        val boxes = mutableListOf<LayoutBox>()
        val bars = mutableListOf<RadicalLine>()
        val body = buildString {
            var y = size * .10f
            rows.forEachIndexed { rowIndex, row ->
                var x = sideWidth
                row.forEachIndexed { columnIndex, cell ->
                    val cellX = x + (columnWidths[columnIndex] - cell.width) / 2f
                    val cellY = y + (rowHeights[rowIndex] - cell.height) / 2f
                    append(translate(cell.body, cellX, cellY))
                    boxes += cell.boxes.map { it.moved(cellX, cellY) }
                    bars += cell.radicalBars.map { it.moved(cellX, cellY) }
                    x += columnWidths[columnIndex] + columnGap
                }
                y += rowHeights[rowIndex] + rowGap
            }
            if (node.delimiter != ' ') {
                append(delimiterPath(node.delimiter.toString(), sideWidth, height, true, size))
                val close = when (node.delimiter) {
                    '(' -> ")"
                    '{' -> ""
                    '|' -> "|"
                    else -> "]"
                }
                if (close.isNotEmpty()) {
                    append(translate(delimiterPath(close, sideWidth, height, false, size), width - sideWidth, 0f))
                }
            }
        }
        return Layout(width, height, height / 2f + size * .35f, body, boxes, bars)
    }

    private fun accent(node: MathNode.Accent, size: Float): Layout {
        val base = layout(node.body, size)
        if (node.mark == "underline") {
            val gap = size * .08f
            val lineY = base.height + gap
            val stroke = max(1.15f, size * .05f)
            val line = "<line x1=\"0\" y1=\"${number(lineY)}\" x2=\"${number(base.width)}\" y2=\"${number(lineY)}\" stroke-width=\"${number(stroke)}\"/>"
            return Layout(base.width, lineY + stroke, base.baseline, base.body + line, base.boxes, base.radicalBars)
        }
        val accentHeight = size * .28f
        val y = accentHeight + size * .08f
        val width = max(base.width, size * .38f)
        val center = width / 2f
        val baseX = (width - base.width) / 2f
        val stroke = max(1.15f, size * .055f)
        val accent = when (node.mark) {
            "hat" -> "<path d=\"M ${number(center - size * .22f)} ${number(accentHeight)} L ${number(center)} ${number(size * .04f)} L ${number(center + size * .22f)} ${number(accentHeight)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            "bar" -> "<line x1=\"${number(baseX)}\" y1=\"${number(accentHeight * .55f)}\" x2=\"${number(baseX + base.width)}\" y2=\"${number(accentHeight * .55f)}\" stroke-width=\"${number(stroke)}\"/>"
            "vec" -> "<path d=\"M ${number(center - size * .28f)} ${number(accentHeight * .62f)} L ${number(center + size * .28f)} ${number(accentHeight * .62f)} M ${number(center + size * .28f)} ${number(accentHeight * .62f)} L ${number(center + size * .13f)} ${number(accentHeight * .18f)} M ${number(center + size * .28f)} ${number(accentHeight * .62f)} L ${number(center + size * .13f)} ${number(accentHeight)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            "dot" -> "<circle cx=\"${number(center)}\" cy=\"${number(accentHeight * .55f)}\" r=\"${number(max(1.2f, size * .065f))}\" stroke=\"none\"/>"
            "ddot" -> "<circle cx=\"${number(center - size * .14f)}\" cy=\"${number(accentHeight * .55f)}\" r=\"${number(max(1.1f, size * .055f))}\" stroke=\"none\"/><circle cx=\"${number(center + size * .14f)}\" cy=\"${number(accentHeight * .55f)}\" r=\"${number(max(1.1f, size * .055f))}\" stroke=\"none\"/>"
            "tilde" -> "<path d=\"M ${number(center - size * .22f)} ${number(accentHeight * .6f)} Q ${number(center - size * .11f)} ${number(accentHeight * .2f)} ${number(center)} ${number(accentHeight * .6f)} T ${number(center + size * .22f)} ${number(accentHeight * .6f)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            "overbrace" -> "<path d=\"M ${number(baseX)} ${number(accentHeight)} Q ${number(baseX)} ${number(size * .08f)} ${number(center - size * .1f)} ${number(size * .08f)} L ${number(center)} 0 L ${number(center + size * .1f)} ${number(size * .08f)} Q ${number(baseX + base.width)} ${number(size * .08f)} ${number(baseX + base.width)} ${number(accentHeight)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            "underbrace" -> "<path d=\"M ${number(baseX)} 0 Q ${number(baseX)} ${number(accentHeight * .92f)} ${number(center - size * .1f)} ${number(accentHeight * .92f)} L ${number(center)} ${number(accentHeight)} L ${number(center + size * .1f)} ${number(accentHeight * .92f)} Q ${number(baseX + base.width)} ${number(accentHeight * .92f)} ${number(baseX + base.width)} 0\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            else -> ""
        }
        return Layout(
            width,
            y + base.height,
            y + base.baseline,
            accent + translate(base.body, baseX, y),
            base.boxes.map { it.moved(baseX, y) },
            base.radicalBars.map { it.moved(baseX, y) }
        )
    }

    private fun delimited(node: MathNode.Delimited, size: Float): Layout {
        val bodyLayout = layout(node.body, size)
        val sideWidth = max(size * .34f, bodyLayout.height * .20f)
        val padding = size * .08f
        val height = max(bodyLayout.height, size * 1.24f)
        val bodyY = (height - bodyLayout.height) / 2f
        val bodyX = sideWidth + padding
        val width = bodyLayout.width + sideWidth * 2 + padding * 2
        val body = buildString {
            if (node.open.isNotEmpty()) append(delimiterPath(node.open, sideWidth, height, true, size))
            append(translate(bodyLayout.body, bodyX, bodyY))
            if (node.close.isNotEmpty()) {
                append(translate(delimiterPath(node.close, sideWidth, height, false, size), width - sideWidth, 0f))
            }
        }
        return Layout(
            width,
            height,
            bodyY + bodyLayout.baseline,
            body,
            bodyLayout.boxes.map { it.moved(bodyX, bodyY) },
            bodyLayout.radicalBars.map { it.moved(bodyX, bodyY) }
        )
    }

    private fun renderCaret(
        boxes: List<MathSvgEditBox>,
        cursor: Int,
        color: String,
        size: Float,
        canvasWidth: Float,
        @Suppress("UNUSED_PARAMETER") canvasHeight: Float,
        paddingX: Float,
        paddingY: Float
    ): String {
        val host = boxes.firstOrNull { cursor >= it.sourceStart && cursor <= it.sourceEnd }
            ?: boxes.lastOrNull { it.sourceEnd <= cursor }
        val x = when {
            host == null -> paddingX
            cursor <= host.sourceStart -> host.xPx
            cursor >= host.sourceEnd -> host.xPx + host.widthPx
            host.sourceEnd == host.sourceStart -> host.xPx + host.widthPx / 2f
            else -> {
                val span = (host.sourceEnd - host.sourceStart).coerceAtLeast(1)
                host.xPx + host.widthPx * ((cursor - host.sourceStart).toFloat() / span)
            }
        }
        val y = host?.yPx ?: paddingY
        val h = host?.heightPx ?: (size * 1.05f)
        return "<rect x=\"${number(x)}\" y=\"${number(y)}\" width=\"${number(max(1.6f, size * .07f))}\" height=\"${number(h)}\" fill=\"$color\" fill-opacity=\".92\"/>"
    }

    private fun renderBoxLayer(
        boxes: List<MathSvgEditBox>,
        activeIndex: Int,
        boxColor: String,
        activeColor: String,
        size: Float,
        onlyActive: Boolean
    ): String = buildString(boxes.size * 150) {
        append("<g>")
        boxes.forEachIndexed { index, box ->
            if (onlyActive && index != activeIndex) return@forEachIndexed
            val active = index == activeIndex
            val color = if (active) activeColor else boxColor
            append("<rect x=\"").append(number(box.xPx)).append("\" y=\"")
            append(number(box.yPx)).append("\" width=\"").append(number(box.widthPx))
            append("\" height=\"").append(number(box.heightPx)).append("\" rx=\"")
            append(number(size * .11f)).append("\" fill=\"").append(color)
            append("\" fill-opacity=\"").append(if (active) ".28" else ".09")
            append("\" stroke=\"").append(color).append("\" stroke-opacity=\"")
            append(if (active) ".95" else ".62").append("\" stroke-width=\"")
            append(number(if (active) max(2f, size * .075f) else max(1f, size * .04f)))
            append("\"/>")
        }
        append("</g>")
    }

    private fun activeBoxIndex(boxes: List<MathSvgEditBox>, start: Int, end: Int): Int {
        if (boxes.isEmpty() || start < 0) return -1
        val selectionStart = minOf(start, end)
        val selectionEnd = maxOf(start, end)
        if (selectionStart != selectionEnd) {
            val exact = boxes.indexOfFirst {
                it.sourceStart == selectionStart && it.sourceEnd == selectionEnd
            }
            if (exact >= 0) return exact
            return boxes.indexOfFirst {
                it.sourceStart < selectionEnd && it.sourceEnd > selectionStart
            }
        }
        boxes.indexOfFirst {
            it.sourceStart == selectionStart && it.sourceEnd == selectionStart
        }.takeIf { it >= 0 }?.let { return it }
        boxes.indexOfFirst {
            selectionStart >= it.sourceStart && selectionStart < it.sourceEnd
        }.takeIf { it >= 0 }?.let { return it }
        boxes.indexOfLast { it.sourceEnd == selectionStart }.takeIf { it >= 0 }?.let { return it }
        return boxes.indexOfFirst { it.sourceStart > selectionStart }
    }

    private fun mergeBoxes(input: List<LayoutBox>, size: Float): List<LayoutBox> {
        if (input.size < 2) return input
        val sorted = input.sortedWith(compareBy<LayoutBox> { it.sourceStart }.thenBy { it.sourceEnd })
        val output = mutableListOf<LayoutBox>()
        sorted.forEach { box ->
            val previous = output.lastOrNull()
            val sameLine = previous != null &&
                abs(previous.y - box.y) <= size * .10f &&
                abs(previous.height - box.height) <= size * .16f
            val close = previous != null && box.x - (previous.x + previous.width) <= size * .18f
            if (
                previous != null && previous.mergeable && box.mergeable && sameLine && close &&
                previous.sourceEnd == box.sourceStart
            ) {
                val right = max(previous.x + previous.width, box.x + box.width)
                val bottom = max(previous.y + previous.height, box.y + box.height)
                output[output.lastIndex] = previous.copy(
                    sourceEnd = box.sourceEnd,
                    x = minOf(previous.x, box.x),
                    y = minOf(previous.y, box.y),
                    width = right - minOf(previous.x, box.x),
                    height = bottom - minOf(previous.y, box.y)
                )
            } else {
                output += box
            }
        }
        return output
    }

    private fun delimiterPath(
        delimiter: String,
        width: Float,
        height: Float,
        left: Boolean,
        size: Float
    ): String {
        val stroke = max(1.15f, size * .052f)
        val inset = width * .18f
        val outside = if (left) width - inset else inset
        val inside = if (left) inset else width - inset
        return when (delimiter) {
            "(", ")" -> "<path d=\"M ${number(outside)} ${number(inset)} Q ${number(inside)} ${number(height / 2f)} ${number(outside)} ${number(height - inset)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            "[", "]", "⌊", "⌋", "⌈", "⌉" -> {
                val top = delimiter !in setOf("⌊", "⌋")
                val bottom = delimiter !in setOf("⌈", "⌉")
                buildString {
                    append("<path d=\"")
                    if (top) append("M ${number(outside)} ${number(inset)} L ${number(inside)} ${number(inset)} ")
                    append("M ${number(inside)} ${number(inset)} L ${number(inside)} ${number(height - inset)} ")
                    if (bottom) append("M ${number(inside)} ${number(height - inset)} L ${number(outside)} ${number(height - inset)}")
                    append("\" fill=\"none\" stroke-width=\"").append(number(stroke)).append("\"/>")
                }
            }
            "{", "}" -> {
                val mid = height / 2f
                "<path d=\"M ${number(outside)} ${number(inset)} Q ${number(inside)} ${number(height * .22f)} ${number(outside)} ${number(mid - inset)} Q ${number(inside)} ${number(mid)} ${number(outside)} ${number(mid + inset)} Q ${number(inside)} ${number(height * .78f)} ${number(outside)} ${number(height - inset)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            }
            "|" -> "<line x1=\"${number(width / 2f)}\" y1=\"${number(inset)}\" x2=\"${number(width / 2f)}\" y2=\"${number(height - inset)}\" stroke-width=\"${number(stroke)}\"/>"
            "⟨", "⟩", "<", ">" -> "<path d=\"M ${number(outside)} ${number(inset)} L ${number(inside)} ${number(height / 2f)} L ${number(outside)} ${number(height - inset)}\" fill=\"none\" stroke-width=\"${number(stroke)}\"/>"
            else -> {
                val glyph = escapeXml(delimiter)
                "<text x=\"0\" y=\"${number(height * .78f)}\" font-family=\"serif\" font-size=\"${number(minOf(height * .82f, size * 1.7f))}\">$glyph</text>"
            }
        }
    }

    private fun estimateTextWidth(value: String, size: Float): Float {
        var width = 0f
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            width += when {
                Character.getType(codePoint) == Character.NON_SPACING_MARK.toInt() -> 0f
                Character.isWhitespace(codePoint) -> size * .34f
                codePoint in '0'.code..'9'.code -> size * .58f
                codePoint in 'A'.code..'Z'.code -> size * .66f
                codePoint in 'a'.code..'z'.code -> size * .56f
                codePoint in 0x0600..0x06FF -> size * .67f
                codePoint in 0x2200..0x22FF || codePoint in 0x2190..0x21FF -> size * .75f
                codePoint > 0xFFFF -> size * .90f
                else -> size * .62f
            }
            index += Character.charCount(codePoint)
        }
        return width
    }

    private fun translate(body: String, x: Float, y: Float): String {
        if (body.isEmpty()) return ""
        if (x == 0f && y == 0f) return body
        return "<g transform=\"translate(${number(x)} ${number(y)})\">$body</g>"
    }

    private fun sanitizeColor(value: String): String =
        if (Regex("^#[0-9A-Fa-f]{6}$").matches(value)) value.uppercase(Locale.US) else "#111111"

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }

    private fun number(value: Float): String {
        val rounded = round(value * 100f) / 100f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString()
        else String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
