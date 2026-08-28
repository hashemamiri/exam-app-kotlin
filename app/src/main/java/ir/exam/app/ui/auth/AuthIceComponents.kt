package ir.exam.app.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * V62.0/V62.1 — کامپوننت‌های بصری «یخی قطبی» صفحهٔ ورود، مطابق ماژول پیشنهادی
 * کاربر (azmoon-auth-compose): فقط پوستهٔ UI؛ تمام منطق احراز هویت همان
 * AuthViewModel/SupabaseAuthRepository تست‌شدهٔ فعلی می‌ماند.
 * V62.1 — پالت و اجزای ماژول عیناً وارد شد: RoleTabs سگمنتی لغزان، Brand،
 * ScreenHeader، فیلد/دکمه‌های یخی، موج سه‌لایه، برف هاله‌دار و StaggeredItem.
 */
internal val IceInk = Color(0xFF0C3D5C)
internal val IceTextSecondary = Color(0xFF5F8AA8)
internal val IceHint = Color(0xFF8FB4CC)
internal val IceAccent = Color(0xFF0284C7)
internal val IceAccentLight = Color(0xFF38BDF8)
internal val IceDisc = Color(0xFF7DD3FC)
internal val IceStroke = Color(0x220284C7)
internal val IceFieldBg = Color(0xC0FFFFFF)
internal val IceBgTop = Color(0xFFE8F6FB)
internal val IceBgMid = Color(0xFFD0EBF7)
internal val IceBgBottom = Color(0xFFBFE3F5)
internal val IceDisabledBg = Color(0xFFDCE9F2)
internal val IceDisabledText = Color(0xFF9AB4C6)

/** تبدیل عدد به ارقام فارسی (مثل ماژول). */
internal fun faNum(n: Int): String = n.toString().map { "۰۱۲۳۴۵۶۷۸۹"[it - '0'] }.joinToString("")

/**
 * V62.2/V62.4 — اسپینر صفحهٔ «در حال بازیابی نشست ورود»: دو کمان چرخان
 * ناهم‌جهت با گرادیان sweep آبی یخی. V62.4 به درخواست کاربر از حالت نئونی
 * خارج شد (هاله‌ها و هستهٔ نبض‌دار حذف) و بزرگ‌تر شد (۷۲ → ۹۶dp).
 */
@Composable
internal fun IceSpinner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ice-spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "ice-spinner-angle"
    )
    // چرخش حلقهٔ سفید باید در پایان هر دور به همان زاویهٔ شروع برسد؛
    // ضریب ۱٫۴ قبلی در ۳۶۰ درجه به ۵۰۴ درجه می‌رسید و هنگام restart
    // حلقه را ناگهان ۱۴۴ درجه می‌پراند.
    val innerAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "ice-spinner-inner-angle"
    )
    Canvas(modifier.size(96.dp)) {
        val stroke = 9.dp.toPx()
        val inset = stroke
        val arcSize = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset)
        val topLeft = Offset(inset, inset)
        val sweepBrush = Brush.sweepGradient(
            listOf(
                Color.Transparent,
                IceDisc.copy(alpha = .45f),
                IceAccentLight,
                IceAccent,
                Color.Transparent
            )
        )
        // کمان بیرونی با گرادیان چرخشی
        rotate(angle) {
            drawArc(
                brush = sweepBrush,
                startAngle = 20f, sweepAngle = 280f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        // کمان داخلی ناهم‌جهت
        val innerInset = inset + 16.dp.toPx()
        val innerSize = androidx.compose.ui.geometry.Size(size.width - 2 * innerInset, size.height - 2 * innerInset)
        // offset ثابت، بدون ضریب غیرصحیح: seam حلقهٔ سفید بدون پرش است.
        rotate(-innerAngle + 160f) {
            drawArc(
                color = Color.White,
                startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(innerInset, innerInset), size = innerSize,
                style = Stroke(width = stroke * .6f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * V62.2 — صفحهٔ انتظار بازیابی نشست با همان پس‌زمینهٔ یخی صفحهٔ ورود
 * (گرادیان + هاله + موج سه‌لایه) و اسپینر یخی؛ متن با رنگ IceInk.
 */
@Composable
fun IceSessionLoading(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IceBackdrop(Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            IceSpinner()
            Text(message, color = IceInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * V62.4 — پس‌زمینهٔ یخی سراسری برنامه: صفحات ورود/بازیابی نشست/قفل برنامه
 * با موج و بقیهٔ برنامه بدون موج. در تم تیره گرادیان روشن یخی معنا ندارد و
 * همان پس‌زمینهٔ تم کشیده می‌شود تا حالت تاریک کاربر خراب نشود.
 */
@Composable
fun IceAppBackdrop(modifier: Modifier = Modifier, waves: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    if (scheme.background.luminance() < .42f) {
        Box(modifier.background(scheme.background))
    } else {
        IceBackdrop(modifier, waves = waves)
    }
}

/**
 * پس‌زمینهٔ یخی ماژول: گرادیان آسمان + هالهٔ دایرهٔ بزرگ + موج سه‌لایهٔ متحرک پایین.
 * V62.4 — پارامتر waves: صفحات ورود/بازیابی نشست/قفل برنامه با موج؛ بقیهٔ
 * برنامه (پشت Scaffold) بدون موج تا زیر داک و منوها شلوغ نشود.
 */
@Composable
internal fun IceBackdrop(modifier: Modifier = Modifier, waves: Boolean = true) {
    // بدون موج، انیمیشن بی‌نهایت هم ساخته نمی‌شود تا پس‌زمینهٔ سراسری برنامه
    // هر فریم دوباره کشیده نشود (waves در طول عمر هر نمونه ثابت است).
    val phase: Float
    if (waves) {
        val transition = rememberInfiniteTransition(label = "ice-waves")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
            label = "ice-wave-phase"
        )
        phase = animated
    } else {
        phase = 0f
    }
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(IceBgTop, IceBgMid, IceBgBottom)))
        // هالهٔ دایرهٔ پشت کارت
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IceDisc.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(size.width / 2f, size.height * 0.60f),
                radius = size.width * 0.95f
            ),
            radius = size.width * 0.95f,
            center = Offset(size.width / 2f, size.height * 0.60f)
        )
        // موج‌های سه‌لایهٔ پایین (ارتفاع ~۱۵۰dp مطابق ماژول)
        if (!waves) return@Canvas
        val h = 150.dp.toPx().coerceAtMost(size.height)
        val top = size.height - h
        val w = size.width
        val lambda = w / 2.5f
        val colors = listOf(
            IceAccent.copy(alpha = 0.28f),
            IceAccentLight.copy(alpha = 0.20f),
            IceDisc.copy(alpha = 0.30f)
        )
        colors.forEachIndexed { i, color ->
            val shift = ((phase + i * 0.33f) % 1f) * lambda
            val path = Path()
            var x = -lambda * 2f + shift
            path.moveTo(x, top + h * 0.62f)
            var up = true
            while (x < w + lambda * 2f) {
                path.quadraticBezierTo(
                    x + lambda / 2f,
                    top + h * (if (up) 0.18f else 1.05f),
                    x + lambda,
                    top + h * 0.62f
                )
                x += lambda
                up = !up
            }
            path.lineTo(x, size.height + 10f)
            path.lineTo(-lambda * 2f + shift, size.height + 10f)
            path.close()
            drawPath(path, color)
        }
    }
}

/** یک دانه برف با پارامترهای تصادفی (مثل ماژول). */
private data class SnowFlake(
    val x: Float,
    val radius: Float,
    val speed: Float,
    val offset: Float,
    val phase: Float,
    val sway: Float
)

/** بارش برف یخی هاله‌دار — فقط در جریان بازیابی رمز نمایش داده می‌شود. */
@Composable
internal fun Snowfall(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "snow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "snow-progress"
    )
    val flakes = remember {
        List(16) {
            SnowFlake(
                x = Random.nextFloat(),
                radius = 0.004f + Random.nextFloat() * 0.004f,
                speed = 0.5f + Random.nextFloat() * 0.8f,
                offset = Random.nextFloat(),
                phase = Random.nextFloat() * 6.28f,
                sway = 0.01f + Random.nextFloat() * 0.02f
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        flakes.forEach { f ->
            val progress = (t * f.speed + f.offset) % 1f
            val y = progress * size.height * 1.1f
            val x = (f.x + sin((t * 4f + f.phase).toDouble()).toFloat() * f.sway) * size.width
            val r = f.radius * size.width
            drawCircle(Color.White.copy(alpha = 0.35f), radius = r * 2.2f, center = Offset(x, y))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = r, center = Offset(x, y))
        }
    }
}

/** انیمیشن ورود پلکانی هر آیتم (stagger) — معادل نسخهٔ ماژول. */
@Composable
internal fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val offsetAnim = remember { androidx.compose.animation.core.Animatable(48f) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        launch { alphaAnim.animateTo(1f, tween(480, easing = FastOutSlowInEasing)) }
        launch { offsetAnim.animateTo(0f, tween(480, easing = FastOutSlowInEasing)) }
    }
    Box(
        Modifier.graphicsLayer {
            this.alpha = alphaAnim.value
            this.translationY = offsetAnim.value
        }
    ) { content() }
}

/**
 * کارت مرکزی همهٔ صفحه‌ها (گوشهٔ ۲۴ ماژول).
 * V62.1.3 — گزارش دستگاه: «یک کادر سفید در پس‌زمینهٔ کادر اصلی». ریشه با
 * تحلیل پیکسلی اسکرین‌شات: هالهٔ سایهٔ الویشن دور و زیر لبهٔ کارت از پشت
 * سطح نیمه‌شفاف ۹۲٪ مثل کارت دومی دیده می‌شد. سایه حذف و سطح کاملاً مات
 * شد؛ خط دور ظریف آبی یخی مرز کارت را روی پس‌زمینه نگه می‌دارد.
 */
@Composable
internal fun IceAuthCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, IceStroke, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 24.dp),
        content = content
    )
}

/** نام اپ در سربرگ کارت‌ها؛ V62.1.2: آیکن کنار عنوان به درخواست کاربر حذف شد. */
@Composable
internal fun Brand() {
    Text(
        "آزمون آنلاین",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = IceInk,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** لوگوی بزرگ صفحهٔ خوش‌آمد (۸۴dp با گرادیان، مثل WelcomeScreen ماژول). */
@Composable
internal fun BrandHero() {
    Box(
        Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(IceAccent, IceAccentLight))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.School,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(38.dp)
        )
    }
}

/** سربرگ صفحه: آیکون دایره‌ای + عنوان + توضیح (مثل ماژول). */
@Composable
internal fun ScreenHeader(icon: ImageVector, title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(IceAccent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = IceAccent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = IceInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = IceTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 22.sp
            )
        }
    }
}

/** رنگ‌های فیلد یخی (Outlined + پس‌زمینهٔ نیم‌شفاف). */
@Composable
internal fun iceFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IceAccent,
    unfocusedBorderColor = IceStroke,
    disabledBorderColor = IceStroke,
    focusedContainerColor = IceFieldBg,
    unfocusedContainerColor = IceFieldBg,
    disabledContainerColor = IceFieldBg.copy(alpha = 0.5f),
    cursorColor = IceAccent
)

/** فیلد ورودی استاندارد ماژول (گوشهٔ ۱۴ + hint داخل فیلد). */
@Composable
internal fun IceField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = { Text(hint, color = IceHint, fontSize = 14.sp) },
        supportingText = supporting?.let { { Text(it, color = IceTextSecondary) } },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        colors = iceFieldColors(),
        textStyle = LocalTextStyle.current.copy(color = IceInk, fontSize = 14.sp)
    )
}

/** دکمهٔ اصلی ماژول (پر از رنگ اکسنت، ۵۲dp) با حالت Loading. */
@Composable
internal fun IceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IceAccent,
            contentColor = Color.White,
            disabledContainerColor = IceDisabledBg,
            disabledContentColor = IceDisabledText
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** دکمهٔ ثانویهٔ ماژول (سفید خط‌دار) با جای آیکون. */
@Composable
internal fun IceOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = IceInk,
            disabledContainerColor = Color.White,
            disabledContentColor = IceDisabledText
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, IceStroke)
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** لینک متنی ماژول. */
@Composable
internal fun LinkTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    gray: Boolean = false
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            color = if (gray) IceTextSecondary else IceAccent,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * تب‌های سگمنتی نقش‌ها با نشانگر لغزان (از ماژول؛ سازگار با RTL —
 * نشانگر از سمت راست شروع می‌شود).
 */
@Composable
internal fun RoleTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(IceStroke.copy(alpha = 0.5f))
    ) {
        val itemWidth = maxWidth / labels.size
        // V62.1.1 — رفع خطای کامپایل CI: «Int * Dp» در کاتلین تعریف نشده؛
        // ترتیب ضرب برعکس شد تا از عملگر عضو Dp.times(Int) استفاده شود.
        // V62.1.2 — رفع باگ دستگاه: Modifier.offset(x) خودش RTL-آگاه است
        // (مثبت = به سمت انتهای چیدمان) و مبدأ فرزند Box هم topStart است؛
        // آینه‌سازی دستی ماژول جبران دوباره می‌شد و نشانگر سفید دقیقاً روی
        // تب قرینه می‌نشست (عکس کاربر: عنوان «ورود مدیر/معاون» ولی نشانگر
        // روی «دانش‌آموز»). offset منطقی مستقیم استفاده می‌شود.
        val animated by animateDpAsState(
            targetValue = itemWidth * selected,
            animationSpec = tween(280),
            label = "tabOffset"
        )
        Box(
            Modifier
                .offset(x = animated)
                .width(itemWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // V62.1.4 — گزارش دستگاه: قبل از رسیدن نشانگر سفید،
                        // کادر خاکستری روی تب مقصد ظاهر می‌شد. آن کادر ریپل
                        // پیش‌فرض متریالِ clickable است؛ چون نشانگر لغزان خودش
                        // بازخورد انتخاب است، ریپل حذف شد (مثل OtpBoxes).
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) IceAccent else IceTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * باکس‌های کد یک‌بارمصرف: ورودی از یک فیلد مخفی گرفته می‌شود تا Paste و
 * Backspace طبیعی کار کنند. کد سوپابیس ما ۶ تا ۸ رقمی است؛ تعداد باکس‌ها
 * با طول کد (حداکثر ۸) تطبیق می‌یابد. V62.1: ابعاد/فوکوس خودکار مثل ماژول.
 */
@Composable
internal fun OtpBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 8
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    val boxCount = maxOf(6, value.length.coerceAtMost(maxLength))
    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                onValueChange(raw.filter(Char::isDigit).take(maxLength))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            cursorBrush = SolidColor(Color.Transparent),
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
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally)
        ) {
            repeat(boxCount) { index ->
                val char = value.getOrNull(index)?.toString().orEmpty()
                val activeBox = index == value.length.coerceAtMost(boxCount - 1) && value.length < maxLength
                Box(
                    Modifier
                        .width(44.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(
                            width = if (activeBox || char.isNotEmpty()) 2.dp else 1.dp,
                            color = if (activeBox) IceAccent else if (char.isNotEmpty()) IceAccent.copy(alpha = .55f) else IceStroke,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        char,
                        color = IceInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * نوار مراحل سه‌گانهٔ بازیابی رمز به سبک ماژول: دایره با گرادیان برای مرحلهٔ
 * فعال (با انیمیشن مقیاس)، تیک انیمیشنی Canvas برای مرحلهٔ کامل‌شده و
 * ارقام فارسی برای مراحل بعدی.
 */
@Composable
internal fun StepIndicator(
    steps: List<String>,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
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
            val scaleAnim by animateFloatAsState(
                targetValue = if (active) 1.08f else 1f,
                animationSpec = tween(300),
                label = "step-scale-$index"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                Box(
                    Modifier
                        .size(30.dp)
                        .scale(scaleAnim)
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> Brush.linearGradient(listOf(IceAccent, IceAccentLight))
                                done -> SolidColor(IceAccent)
                                else -> SolidColor(IceFieldBg)
                            }
                        )
                        .then(
                            if (!done && !active) Modifier.border(1.5.dp, IceStroke, CircleShape)
                            else Modifier
                        ),
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
                            faNum(index + 1),
                            color = if (active) Color.White else IceTextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    label,
                    fontSize = 10.5.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        active -> IceInk
                        done -> IceAccent
                        else -> IceTextSecondary
                    }
                )
            }
            if (index != steps.lastIndex) {
                Box(
                    Modifier
                        .padding(top = 14.dp)
                        .width(20.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < current) IceAccent else IceStroke)
                )
            }
        }
    }
}
