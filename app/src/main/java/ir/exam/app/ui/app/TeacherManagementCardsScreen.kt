package ir.exam.app.ui.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ir.exam.app.core.ui.LocalTabletLayout
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

object Design69ManagementCardsContract {
    // V131 — کارت «کارنامه» (کارنامه و لیست نمرات) از «آمار» جدا شد.
    const val CARD_COUNT = 7
    const val DRAG_THRESHOLD_DP = 52
}

private data class ManagementCardSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val action: () -> Unit
)

/** سه کارت واقعی آمار/تصحیح/مانده با drag چهارجهته و کلیدهای جهت. */
@Composable
fun TeacherManagementCardsScreen(
    cycleKey: Int,
    onStats: () -> Unit,
    onGradeList: () -> Unit,
    onQuestionBank: () -> Unit,
    onGrading: () -> Unit,
    onPending: () -> Unit,
    onAnswers: () -> Unit,
    onRequests: () -> Unit
) {
    val neo = neumorphic69Colors
    val cards = remember(
        onStats,
        onGradeList,
        onQuestionBank,
        onGrading,
        onPending,
        onAnswers,
        onRequests,
        neo.accent,
        neo.accent2
    ) {
        listOf(
            ManagementCardSpec(
                "آمار",
                "نمودارها، میانگین‌ها و تحلیل کیفیت سؤال‌های آزمون را نشان می‌دهد.",
                Design69Icons.Reports,
                listOf(neo.accent, neo.accent2),
                onStats
            ),
            ManagementCardSpec(
                "کارنامه",
                "کارنامه و لیست نمرات کلاس؛ انتخاب آزمون‌ها و خروجی Excel یا PDF.",
                Design69Icons.Reports,
                listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
                onGradeList
            ),
            ManagementCardSpec(
                "بانک سؤال",
                "جست‌وجو، دسته‌بندی، مشاهده، ویرایش، حذف و افزودن سؤال به آزمون.",
                Design69Icons.Exams,
                listOf(Color(0xFF2878DB), Color(0xFF24B8C8)),
                onQuestionBank
            ),
            ManagementCardSpec(
                "تصحیح",
                "همه پاسخ‌ها، حضور، بازخورد و ثبت یا اصلاح نمره را باز می‌کند.",
                Design69Icons.Grading,
                listOf(Color(0xFF25BFA4), Color(0xFF45D7BD)),
                onGrading
            ),
            ManagementCardSpec(
                "مانده",
                "فقط پاسخ‌های در انتظار تصحیح و پیگیری را نمایش می‌دهد.",
                Design69Icons.Cards,
                listOf(Color(0xFFE0587F), Color(0xFF7D6CF4)),
                onPending
            ),
            ManagementCardSpec(
                "پاسخ",
                "فقط پاسخ‌های تصحیح‌شده دارای نمره و بازخورد نهایی را نمایش می‌دهد.",
                Design69Icons.Grading,
                listOf(Color(0xFF4D5B74), Color(0xFF273247)),
                onAnswers
            ),
            ManagementCardSpec(
                "درخواست‌ها",
                "درخواست‌های ویرایش یا حذف مدیر را مشاهده، تأیید یا رد کنید.",
                Design69Icons.Cards,
                listOf(Color(0xFF7D6CF4), Color(0xFFE0587F)),
                onRequests
            )
        )
    }
    require(cards.size == Design69ManagementCardsContract.CARD_COUNT)
    ManagementCardsStack(cycleKey = cycleKey, cards = cards)
}

/**
 * V61.9 — کارت‌های مدیر با همان پشتهٔ کارتی معلم؛ سه کارت:
 * «مدارس»، «کارنامه» و «وضعیت» (داشبورد).
 */
@Composable
fun ManagerManagementCardsScreen(
    cycleKey: Int,
    onSchools: () -> Unit,
    onReport: () -> Unit,
    onStatus: () -> Unit
) {
    val neo = neumorphic69Colors
    val cards = remember(onSchools, onReport, onStatus, neo.accent, neo.accent2) {
        listOf(
            ManagementCardSpec(
                "مدارس",
                "لیست مدرسه‌ها، ساخت مدرسه جدید و کلاس‌های هر مدرسه را باز می‌کند.",
                Design69Icons.SchoolAdd,
                listOf(neo.accent, neo.accent2),
                onSchools
            ),
            ManagementCardSpec(
                "کارنامه",
                "آمار پاسخ‌ها، میانگین نمره و فعالیت معلم‌های مدرسه.",
                Design69Icons.Reports,
                listOf(Color(0xFF2878DB), Color(0xFF24B8C8)),
                onReport
            ),
            ManagementCardSpec(
                "وضعیت",
                "داشبورد مدرسه با اطلاعات، آمار کلی و پنل سریع بخش‌ها.",
                Design69Icons.Dashboard,
                listOf(Color(0xFF25BFA4), Color(0xFF45D7BD)),
                onStatus
            )
        )
    }
    ManagementCardsStack(cycleKey = cycleKey, cards = cards)
}

/** V61.9 — پشتهٔ مشترک کارت‌ها (drag/کلید/نقطه‌ها) برای معلم و مدیر. */
@Composable
private fun ManagementCardsStack(cycleKey: Int, cards: List<ManagementCardSpec>) {
    val neo = neumorphic69Colors
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    var settling by remember { mutableStateOf(false) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    // V55.18.1 — کارت در حال «برگشت به پشته» هنگام کشیدن به راست: بعد از تغییر
    // activeIndex کارت قبلی هنوز مرئی است (relative=1) و بدون این state از زیر
    // انگشت به جایگاه پشته تلپورت می‌کرد.
    var returningIndex by remember { mutableIntStateOf(-1) }
    val returnX = remember { Animatable(0f) }
    val returnY = remember { Animatable(0f) }
    // V137.2 — کج‌شدن/مقیاس/محوِ کارتِ در حال پرواز و بازگشت.
    val returnRotation = remember { Animatable(0f) }
    val returnScale = remember { Animatable(1f) }
    val returnAlpha = remember { Animatable(1f) }
    // V137.3 — ریشهٔ «انیمیشن قدیمی دیده می‌شود»: معلم ۱۰ کارت دارد؛ با کشیدن به چپ، کارتِ
    // رفته بلافاصله relative = ۹ می‌شد و چون فقط relative ≤ ۲ رسم می‌شود، همان لحظه ناپدید
    // می‌شد (پروازش هرگز دیده نمی‌شد) و شرط «cards.size ≤ ۳» هم فاز بازگشت را حذف می‌کرد.
    // حالا: کارتِ در حال پرواز (flying) همیشه رسم می‌شود و در جای کارت فعال می‌ماند؛ فاز
    // بازگشت وقتی اجرا می‌شود که کارت پس از تغییر در پشته دیده شود؛ و کارتی که از ته پشته
    // جلو می‌آید (enteringIndex) با محو/مقیاس از پشت وارد می‌شود — هر دو جهت عیناً یکسان.
    var flying by remember { mutableStateOf(false) }
    var enteringIndex by remember { mutableIntStateOf(-1) }
    val enterProgress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { Design69ManagementCardsContract.DRAG_THRESHOLD_DP.dp.toPx() }
    val exitHorizontal = with(density) { 520.dp.toPx() }
    val liftPx = with(density) { 36.dp.toPx() }

    fun changeCard(direction: Int) {
        if (settling) return
        activeIndex = (activeIndex + direction + cards.size) % cards.size
    }

    fun settle(cancel: Boolean) {
        if (settling) return
        settling = true
        scope.launch {
            val x = dragX.value
            val y = dragY.value
            val horizontal = abs(x) >= abs(y)
            val accepted = !cancel && horizontal && abs(x) > threshold
            if (accepted) {
                val direction = if (x < 0f) 1 else -1
                val targetX = (sign(x).takeIf { it != 0f } ?: 1f) * exitHorizontal
                val targetY = y * 1.20f
                // V137.2 — حرکت تازهٔ «پرواز و بازگشت» و کاملاً یکسان برای چپ و راست (خواستهٔ کاربر):
                // ۱) کارت فعلی از نقطهٔ رهاشدن با کمی بلندشدن (scale ۱٫۰۶) و کج‌شدن به سمت حرکت (±۱۴°)
                //    از همان سمت بیرون می‌پرد و در نیمهٔ راه محو می‌شود؛ هم‌زمان activeIndex عوض شده و
                //    کارت بعدی با انیمیشن‌های پشته (فاصله/مقیاس/چرخش) جلو می‌آید.
                // ۲) اگر کارتِ رفته در پشته دیده می‌شود، از همان سمت و از پشتِ پشته با محوِ معکوس و
                //    فنر نرم (spring) به جایگاه انتهای پشته می‌نشیند. هیچ تفاوتی بین دو جهت نیست؛
                //    فقط علامتِ targetX و زاویه عوض می‌شود.
                val leaving = activeIndex
                val arriving = (activeIndex + direction + cards.size) % cards.size
                val arrivingWasVisible = ((arriving - leaving + cards.size) % cards.size) <= 2
                val leavingStaysVisible = ((leaving - arriving + cards.size) % cards.size) <= 2
                returningIndex = leaving
                flying = true
                if (!arrivingWasVisible) {
                    enteringIndex = arriving
                    enterProgress.snapTo(0f)
                }
                returnX.snapTo(x)
                returnY.snapTo(y)
                returnRotation.snapTo(x / 42f + y / 75f)
                returnScale.snapTo(1f)
                returnAlpha.snapTo(1f)
                dragX.snapTo(0f)
                dragY.snapTo(0f)
                activeIndex = arriving
                coroutineScope {
                    if (!arrivingWasVisible) {
                        launch { enterProgress.animateTo(1f, tween(420, delayMillis = 60, easing = FastOutSlowInEasing)) }
                    }
                    launch { returnX.animateTo(targetX, tween(340, easing = FastOutSlowInEasing)) }
                    launch { returnY.animateTo(targetY - liftPx, tween(340, easing = FastOutSlowInEasing)) }
                    launch { returnRotation.animateTo(direction * 14f, tween(340, easing = FastOutSlowInEasing)) }
                    launch {
                        returnScale.animateTo(1.06f, tween(150, easing = FastOutSlowInEasing))
                        returnScale.animateTo(.92f, tween(190, easing = FastOutSlowInEasing))
                    }
                    launch { returnAlpha.animateTo(0f, tween(220, delayMillis = 120)) }
                }
                flying = false
                // فاز بازگشت فقط وقتی کارتِ رفته پس از تغییر در پشته (۳ کارت اول) دیده می‌شود.
                if (leavingStaysVisible) {
                    returnX.snapTo(targetX * .45f)
                    returnY.snapTo(-liftPx * .6f)
                    returnRotation.snapTo(direction * 6f)
                    returnScale.snapTo(.96f)
                    coroutineScope {
                        launch { returnAlpha.animateTo(1f, tween(260)) }
                        launch { returnX.animateTo(0f, spring(dampingRatio = .78f, stiffness = 260f)) }
                        launch { returnY.animateTo(0f, spring(dampingRatio = .78f, stiffness = 260f)) }
                        launch { returnRotation.animateTo(0f, spring(dampingRatio = .78f, stiffness = 260f)) }
                        launch { returnScale.animateTo(1f, spring(dampingRatio = .78f, stiffness = 260f)) }
                    }
                } else {
                    returnX.snapTo(0f)
                    returnY.snapTo(0f)
                    returnRotation.snapTo(0f)
                    returnScale.snapTo(1f)
                    returnAlpha.snapTo(1f)
                }
                returningIndex = -1
                enteringIndex = -1
            } else {
                coroutineScope {
                    launch { dragX.animateTo(0f, tween(280)) }
                    launch { dragY.animateTo(0f, tween(280)) }
                }
            }
            settling = false
        }
    }

    LaunchedEffect(cycleKey) {
        if (cycleKey > 0) changeCard(1)
    }

    // V56.1 — تبلت: پشتهٔ کارت‌ها وسط صفحه با سقف پهنا تا کارت‌ها بیش از حد
    // کشیده و بدقواره نشوند؛ گوشی مثل قبل تمام‌پهنا.
    val tabletCards = LocalTabletLayout.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .widthIn(max = if (tabletCards) 620.dp else Dp.Unspecified)
                .height(300.dp)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            changeCard(1)
                            true
                        }
                        Key.DirectionRight -> {
                            changeCard(-1)
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            cards[activeIndex].action()
                            true
                        }
                        else -> false
                    }
                }
                .pointerInput(activeIndex, settling) {
                    detectDragGestures(
                        onDragEnd = { settle(false) },
                        onDragCancel = { settle(true) }
                    ) { change, amount ->
                        if (settling) return@detectDragGestures
                        change.consume()
                        scope.launch {
                            dragX.snapTo(dragX.value + amount.x)
                            dragY.snapTo(dragY.value + amount.y)
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            cards.indices.reversed().forEach { index ->
                val relative = (index - activeIndex + cards.size) % cards.size
                val isFlying = flying && index == returningIndex
                if (relative <= 2 || isFlying) {
                    val active = relative == 0
                    val data = cards[index]
                    // V137.3 — کارتِ در حال پرواز تا پایان پرواز در جایگاه کارت فعال (relative ۰) می‌ماند.
                    val visualRelative = if (isFlying) 0 else relative
                    val stackTop by animateDpAsState(
                        (30 + visualRelative * 30).dp,
                        tween(650, easing = FastOutSlowInEasing),
                        label = "management-card-top-$index"
                    )
                    val stackScale by animateFloatAsState(
                        1f - visualRelative * .075f,
                        tween(650, easing = FastOutSlowInEasing),
                        label = "management-card-scale-$index"
                    )
                    val stackAlpha by animateFloatAsState(
                        1f - visualRelative * .25f,
                        tween(500),
                        label = "management-card-alpha-$index"
                    )
                    val stackRotation by animateFloatAsState(
                        if (visualRelative == 0) 0f else if (visualRelative == 1) 5f else -6f,
                        tween(650, easing = FastOutSlowInEasing),
                        label = "management-card-rotation-$index"
                    )
                    Box(
                        Modifier
                            .padding(top = stackTop)
                            .fillMaxWidth(.90f)
                            .height(190.dp)
                            .zIndex(if (isFlying) 4f else 3f - relative)
                            .graphicsLayer {
                                // V55.18.1: کارت در حال برگشت به پشته (کشیدن به راست)
                                // از نقطهٔ رهاشدن نرم به جایگاهش می‌رود، نه تلپورت.
                                val returning = index == returningIndex && !active
                                // V137.2 — کارت فعال هنگام کشیدن کمی بلند می‌شود؛ کارتِ در حال پرواز مقیاس/محوِ خودش را دارد.
                                val dragLift = if (active) 1f + (abs(dragX.value) / exitHorizontal).coerceIn(0f, .5f) * .08f else 1f
                                // V137.3 — ورود کارت از ته پشته: از پشت (کمی بالاتر و کوچک‌تر) با محو وارد می‌شود.
                                val enterP = if (index == enteringIndex) enterProgress.value else 1f
                                val enterScale = .86f + .14f * enterP
                                scaleX = stackScale * dragLift * enterScale * (if (returning) returnScale.value else 1f)
                                scaleY = stackScale * dragLift * enterScale * (if (returning) returnScale.value else 1f)
                                alpha = stackAlpha * enterP * (if (returning) returnAlpha.value else 1f)
                                translationX = when {
                                    active -> dragX.value
                                    returning -> returnX.value
                                    else -> 0f
                                }
                                translationY = when {
                                    active -> dragY.value - liftPx * .8f * (1f - enterP)
                                    returning -> returnY.value
                                    else -> 0f
                                }
                                rotationZ = when {
                                    active -> stackRotation + dragX.value / 42f + dragY.value / 75f
                                    returning -> stackRotation + returnRotation.value
                                    else -> stackRotation
                                }
                            }
                            .clip(RoundedCornerShape(29.dp))
                            .background(Brush.linearGradient(data.colors))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = active,
                                role = Role.Button,
                                onClick = data.action
                            )
                            .padding(22.dp)
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = .18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(data.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Text("آزمون آنلاین", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                data.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            cards.indices.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(7.dp)
                        .width(if (index == activeIndex) 23.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (index == activeIndex) neo.accent else neo.darkShadow.copy(alpha = .45f))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        NeumorphicPanel(
            modifier = Modifier.fillMaxWidth(),
            radius = 22.dp,
            depth = neoDepth(9.dp),
            pressed = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            // V137 — عنوان و توضیح کارت وسط‌چین.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    cards[activeIndex].title,
                    color = neo.ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    cards[activeIndex].subtitle,
                    color = neo.muted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
