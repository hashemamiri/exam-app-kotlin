package ir.exam.app.ui.builder

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.app.neumorphic69
import ir.exam.app.ui.app.neumorphic69Colors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class BuilderRadialAction(
    val title: String,
    val symbol: String,
    // V61.6 — رنگ پاستلی اختصاصی هر نوع سؤال (پس‌زمینهٔ دکمهٔ منوی +).
    val background: Long? = null,
    val onClick: () -> Unit
)

/** + سازنده هم‌زمان با هشت گزینه به مرکز می‌رسد؛ خط‌چین فقط در انتهای حرکت رسم می‌شود. */
@Composable
fun BuilderRadialMenuOverlay(
    onDismiss: () -> Unit,
    onQuestionType: (QuestionType) -> Unit,
    onImport: () -> Unit,
    onBank: () -> Unit
) {
    val colors = neumorphic69Colors
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val actions = remember(onQuestionType, onImport, onBank) {
        listOf(
            // V61.6 — هر نوع سؤال با رنگ پاستلی خودش (هماهنگ با کارت سؤال).
            BuilderRadialAction("تشریحی", "✎", QuestionType.ESSAY.pastelColor()) { onQuestionType(QuestionType.ESSAY) },
            BuilderRadialAction("چندگزینه‌ای", "◉", QuestionType.MULTIPLE_CHOICE.pastelColor()) { onQuestionType(QuestionType.MULTIPLE_CHOICE) },
            BuilderRadialAction("صحیح/غلط", "✓", QuestionType.TRUE_FALSE.pastelColor()) { onQuestionType(QuestionType.TRUE_FALSE) },
            BuilderRadialAction("جای خالی", "＿", QuestionType.FILL_BLANK.pastelColor()) { onQuestionType(QuestionType.FILL_BLANK) },
            BuilderRadialAction("عددی", "۱۲", QuestionType.NUMERIC.pastelColor()) { onQuestionType(QuestionType.NUMERIC) },
            BuilderRadialAction("جورکردنی", "↔", QuestionType.MATCHING.pastelColor()) { onQuestionType(QuestionType.MATCHING) },
            BuilderRadialAction("وارد کردن", "⇩", 0xFF98FF98, onImport),
            BuilderRadialAction("بانک سؤال", "▤", 0xFFE6E6FA, onBank)
        )
    }

    fun close(after: (() -> Unit)? = null) {
        scope.launch {
            progress.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
            onDismiss()
            after?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
    }
    BackHandler { close() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = .16f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { close() }
            )
    ) {
        val radius = (maxWidth * .31f).coerceIn(104.dp, 138.dp)
        val p = progress.value
        val dottedAlpha = ((p - .88f) / .12f).coerceIn(0f, 1f)

        Canvas(Modifier.align(Alignment.Center).size(radius * 2f + 82.dp)) {
            val r = radius.toPx()
            drawCircle(
                color = colors.accent.copy(alpha = .42f * dottedAlpha),
                radius = r,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(7.dp.toPx(), 8.dp.toPx())
                    )
                )
            )
        }

        actions.forEachIndexed { index, action ->
            val angle = (-90f + index * 45f) * PI.toFloat() / 180f
            val targetX = radius * cos(angle)
            val targetY = radius * sin(angle)
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(
                            (targetX.roundToPx() * p).roundToInt(),
                            (targetY.roundToPx() * p).roundToInt()
                        )
                    }
                    .size(66.dp)
                    // V61.8 — شکل داخل خود لایهٔ انیمیشن clip می‌شود تا در کل
                    // مسیر باز شدن «مربع گوشه‌گرد» بماند (قبلاً کلیپ جدا بود و
                    // ابتدای انیمیشن دایره‌ای دیده می‌شد).
                    .graphicsLayer {
                        alpha = p
                        // V61.8 — شروع از .6 (نه .22): دکمهٔ خیلی کوچک با گوشهٔ گرد
                        // «دایره» دیده می‌شد؛ حالا از ابتدا مربع گوشه‌گرد است.
                        scaleX = .6f + .4f * p
                        scaleY = .6f + .4f * p
                        shape = RoundedCornerShape(22.dp)
                        clip = true
                    }
                    .neumorphic69(colors, 22.dp, 10.dp)
                    // V61.6 — پس‌زمینهٔ پاستلی اختصاصی نوع؛ متن تیره برای خوانایی.
                    .background(action.background?.let(::Color) ?: colors.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = p > .96f,
                        role = Role.Button
                    ) { close(action.onClick) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        action.symbol,
                        // V61.6 — روی پس‌زمینهٔ پاستلی متن تیرهٔ ثابت خواناتر است.
                        color = if (action.background != null) Color(0xFF37474F) else colors.accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        action.title,
                        color = if (action.background != null) Color(0xFF37474F) else colors.ink,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        val startX = maxWidth / 2f - 52.dp
        val startY = maxHeight / 2f - 58.dp
        Box(
            Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        (startX.roundToPx() * (1f - p)).roundToInt(),
                        (startY.roundToPx() * (1f - p)).roundToInt()
                    )
                }
                .size(64.dp)
                .graphicsLayer {
                    rotationZ = 135f * p
                    shape = CircleShape
                    clip = true
                }
                .background(
                    Brush.linearGradient(
                        listOf(
                            lerp(colors.accent, Color(0xFFE5484D), p),
                            lerp(colors.accent2, Color(0xFFB91C35), p)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = { close() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
