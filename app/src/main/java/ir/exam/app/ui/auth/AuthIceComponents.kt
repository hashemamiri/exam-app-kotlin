package ir.exam.app.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

/**
 * V62.0 — کامپوننت‌های بصری «یخی قطبی» صفحهٔ ورود (برگرفته از ماژول پیشنهادی
 * کاربر azmoon-auth-compose): فقط پوستهٔ UI؛ تمام منطق احراز هویت همان
 * AuthViewModel/SupabaseAuthRepository تست‌شدهٔ فعلی می‌ماند.
 */
internal val IceInk = Color(0xFF0F2E4C)
internal val IceAccent = Color(0xFF2E9BD6)
internal val IceStroke = Color(0xFFBBD9EE)

/** پس‌زمینهٔ یخی: گرادیان آسمان + هالهٔ دایره + دو موج متحرک پایین. */
@Composable
internal fun IceBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ice-waves")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "ice-wave-phase"
    )
    Canvas(modifier) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFFEAF6FF), Color(0xFFD9ECFC), Color(0xFFC9E4F9))
            )
        )
        // هالهٔ دایره پشت کارت
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = .75f), Color.White.copy(alpha = 0f)),
                center = Offset(size.width / 2f, size.height * .30f),
                radius = size.width * .58f
            ),
            radius = size.width * .58f,
            center = Offset(size.width / 2f, size.height * .30f)
        )
        // دو موج پایین
        fun wave(amplitude: Float, baseY: Float, shift: Float, color: Color) {
            val path = Path()
            path.moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width) {
                val y = baseY + amplitude * sin((x / size.width) * 4f * Math.PI.toFloat() + phase + shift)
                path.lineTo(x, y)
                x += 24f
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, color)
        }
        wave(14f, size.height * .90f, 0f, Color(0xFFB7DCF6).copy(alpha = .55f))
        wave(18f, size.height * .94f, 1.6f, Color(0xFF9FCFF2).copy(alpha = .45f))
    }
}

/** بارش برف سبک (فقط جریان بازیابی رمز، مطابق طراحی یخی). */
@Composable
internal fun Snowfall(modifier: Modifier = Modifier, flakeCount: Int = 34) {
    val flakes = remember {
        List(flakeCount) {
            Triple(Random.nextFloat(), Random.nextFloat(), .35f + Random.nextFloat() * .65f)
        }
    }
    val transition = rememberInfiniteTransition(label = "snow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "snow-progress"
    )
    Canvas(modifier.alpha(.8f)) {
        flakes.forEach { (seedX, seedY, speed) ->
            val y = ((seedY + t * speed) % 1f) * size.height
            val x = (seedX + .04f * sin(y / 90f + seedX * 8f)) * size.width
            drawCircle(Color.White.copy(alpha = .38f + .4f * speed), radius = 2.2f + 2.6f * speed, center = Offset(x, y))
        }
    }
}

/**
 * ورود پلکانی محتوای هر پنجره: با هر تغییر صفحه، فرم با محو/بالاآمدن کوتاه
 * وارد می‌شود (نسخهٔ سبک stagger طراحی یخی).
 */
@Composable
internal fun StaggeredEntrance(key: Any?, content: @Composable () -> Unit) {
    val progress = remember(key) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(360))
    }
    Box(
        Modifier
            .alpha(.35f + .65f * progress.value)
            .padding(top = ((1f - progress.value) * 10).dp)
    ) { content() }
}

/** کارت شیشه‌ای گرد میزبان فرم‌ها. */
@Composable
internal fun IceAuthCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = .88f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Box(Modifier.padding(18.dp)) { content() }
    }
}

/**
 * باکس‌های کد یک‌بارمصرف: ورودی از یک فیلد مخفی گرفته می‌شود تا Paste و
 * Backspace طبیعی کار کنند. کد سوپابیس ما ۶ تا ۸ رقمی است؛ تعداد باکس‌ها
 * با طول کد (حداکثر ۸) تطبیق می‌یابد.
 */
@Composable
internal fun OtpBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 8
) {
    val focusRequester = remember { FocusRequester() }
    val boxCount = maxOf(6, value.length.coerceAtMost(maxLength))
    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                onValueChange(raw.filter(Char::isDigit).take(maxLength))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            cursorBrush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusRequester.requestFocus() },
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)
        ) {
            repeat(boxCount) { index ->
                val char = value.getOrNull(index)?.toString().orEmpty()
                val activeBox = index == value.length.coerceAtMost(boxCount - 1) && value.length < maxLength
                Box(
                    Modifier
                        .width(42.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White)
                        .border(
                            width = if (activeBox || char.isNotEmpty()) 2.dp else 1.dp,
                            color = if (activeBox) IceAccent else if (char.isNotEmpty()) IceAccent.copy(alpha = .55f) else IceStroke,
                            shape = RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        char,
                        color = IceInk,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * نوار مراحل سه‌گانهٔ بازیابی رمز؛ مرحلهٔ کامل‌شده تیک انیمیشنی (Canvas)
 * می‌گیرد — همان AnimatedCheck طراحی یخی، ادغام‌شده در نشانگر مراحل.
 */
@Composable
internal fun StepIndicator(
    steps: List<String>,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        steps.forEachIndexed { index, label ->
            val done = index < current
            val active = index == current
            val checkProgress by animateFloatAsState(
                targetValue = if (done) 1f else 0f,
                animationSpec = tween(420),
                label = "step-check-$index"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (done || active) IceAccent else Color.White)
                        .border(2.dp, if (done || active) IceAccent else IceStroke, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Canvas(Modifier.size(15.dp)) {
                            val p = checkProgress
                            val path = Path()
                            path.moveTo(size.width * .08f, size.height * .55f)
                            path.lineTo(size.width * .38f, size.height * .82f)
                            path.lineTo(size.width * .92f, size.height * .18f)
                            drawPath(
                                path,
                                Color.White,
                                alpha = p,
                                style = Stroke(width = 3.4f * p + .1f, cap = StrokeCap.Round)
                            )
                        }
                    } else {
                        Text(
                            (index + 1).toString(),
                            color = if (active) Color.White else IceInk.copy(alpha = .55f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done || active) IceInk else IceInk.copy(alpha = .5f)
                )
            }
            if (index != steps.lastIndex) {
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .width(26.dp)
                        .height(2.dp)
                        .background(if (index < current) IceAccent else IceStroke)
                )
            }
        }
    }
}

