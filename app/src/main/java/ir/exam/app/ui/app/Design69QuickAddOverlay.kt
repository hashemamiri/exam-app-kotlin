package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object Design69QuickAddContract {
    const val TRAVEL_DURATION_MS = 620
    const val OPEN_ROTATION_DEGREES = 135
    // V61.5 — عمل چهارم «مدرسه جدید» (مدیر: ساخت مدرسه؛ معلم: عضویت با کد دعوت).
    const val ACTION_COUNT = 4
}

/**
 * همان دکمه + نوار پایین را به مرکز می‌آورد و سه عملیات واقعی سامانه را مثلثی باز می‌کند.
 */
@Composable
fun Design69QuickAddOverlay(
    onDismiss: () -> Unit,
    onCreateStudent: () -> Unit,
    onCreateExam: () -> Unit,
    onCreateClass: () -> Unit,
    // V61.5 — «مدرسه جدید»: مدیر مدرسه می‌سازد؛ معلم با کد دعوت عضو می‌شود.
    onCreateSchool: () -> Unit = {},
    primaryTitle: String = "آزمون جدید",
    primaryIcon: ImageVector = Design69Icons.ExamAdd
) {
    val colors = neumorphic69Colors
    val travel = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun close(after: (() -> Unit)? = null) {
        if (closing) return
        closing = true
        scope.launch {
            travel.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            onDismiss()
            after?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        travel.animateTo(
            1f,
            tween(Design69QuickAddContract.TRAVEL_DURATION_MS, easing = FastOutSlowInEasing)
        )
    }

    BackHandler { close() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = (maxWidth * .29f).coerceIn(94.dp, 124.dp)
        val startY = (maxHeight / 2f - 49.dp).coerceAtLeast(220.dp)

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 74.dp, bottom = 102.dp)
                .background(colors.background)
        )

        NeumorphicPanel(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(466.dp)
                .graphicsLayer { alpha = .35f + .65f * travel.value },
            radius = 48.dp,
            depth = 14.dp,
            pressed = true
        ) {}

        // V61.7 — چیدمان ضربدری: چهار کارت در چهار گوشهٔ مربع فرضی و هر چهار
        // خط‌چین از مرکز (دکمهٔ ضربدر) به گوشه‌ها.
        val cornerY = 108.dp
        Canvas(
            Modifier
                .align(Alignment.Center)
                .size(width = 318.dp, height = 330.dp)
        ) {
            val alpha = .45f * ((travel.value - .88f) / .12f).coerceIn(0f, 1f)
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 8.dp.toPx()))
            listOf(
                androidx.compose.ui.geometry.Offset(-horizontal.toPx(), -cornerY.toPx()),
                androidx.compose.ui.geometry.Offset(horizontal.toPx(), -cornerY.toPx()),
                androidx.compose.ui.geometry.Offset(-horizontal.toPx(), cornerY.toPx()),
                androidx.compose.ui.geometry.Offset(horizontal.toPx(), cornerY.toPx())
            ).forEach { corner ->
                drawLine(
                    colors.accent.copy(alpha = alpha),
                    center,
                    center + corner,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dash
                )
            }
        }

        QuickAddAction(
            progress = travel.value,
            title = primaryTitle,
            icon = primaryIcon,
            // targetX = 0.dp حذف شد: گوشهٔ بالا-راست مربع (RTL: راست = -x نیست؛ offset مطلق است).
            targetX = horizontal,
            targetY = -cornerY,
            modifier = Modifier.align(Alignment.Center)
        ) { close(onCreateExam) }

        QuickAddAction(
            progress = travel.value,
            title = "دانش‌آموز جدید",
            icon = Design69Icons.PersonAdd,
            targetX = -horizontal,
            targetY = -cornerY,
            modifier = Modifier.align(Alignment.Center)
        ) { close(onCreateStudent) }

        QuickAddAction(
            progress = travel.value,
            title = "کلاس جدید",
            icon = Design69Icons.ClassAdd,
            targetX = horizontal,
            targetY = cornerY,
            modifier = Modifier.align(Alignment.Center)
        ) { close(onCreateClass) }

        // V61.5 — عمل چهارم: مدرسه جدید (گوشهٔ چهارم ضربدر).
        QuickAddAction(
            progress = travel.value,
            title = "مدرسه جدید",
            icon = Design69Icons.Data,
            targetX = -horizontal,
            targetY = cornerY,
            modifier = Modifier.align(Alignment.Center)
        ) { close(onCreateSchool) }

        Box(
            Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = 0,
                        y = (startY.roundToPx() * (1f - travel.value)).roundToInt()
                    )
                }
                .size(58.dp)
                .neumorphic69(colors, 29.dp, 12.dp)
                .graphicsLayer {
                    rotationZ = Design69QuickAddContract.OPEN_ROTATION_DEGREES * travel.value
                    scaleX = 1f + .20f * travel.value
                    scaleY = 1f + .20f * travel.value
                    shape = CircleShape
                    clip = true
                }
                .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = { close() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Design69Icons.Add,
                contentDescription = "بستن افزودن سریع",
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }

    }
}

@Composable
private fun QuickAddAction(
    progress: Float,
    title: String,
    icon: ImageVector,
    targetX: Dp,
    targetY: Dp,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = neumorphic69Colors
    NeumorphicPressable(
        onClick = { if (progress > .96f) onClick() },
        modifier = modifier
            .offset {
                IntOffset(
                    (targetX.roundToPx() * progress).roundToInt(),
                    (targetY.roundToPx() * progress).roundToInt()
                )
            }
            .size(88.dp)
            .graphicsLayer {
                alpha = progress
                scaleX = .25f + .75f * progress
                scaleY = .25f + .75f * progress
            },
        radius = 29.dp,
        depth = 14.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = colors.accent, modifier = Modifier.size(26.dp))
            Text(
                title,
                color = colors.muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
}
