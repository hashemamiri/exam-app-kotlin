package ir.exam.app.ui.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

object Design69ManagementCardsContract {
    const val CARD_COUNT = 6
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
    onQuestionBank: () -> Unit,
    onGrading: () -> Unit,
    onPending: () -> Unit,
    onAnswers: () -> Unit,
    onRequests: () -> Unit
) {
    val neo = neumorphic69Colors
    val cards = remember(
        onStats,
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
                "نمودارها، میانگین‌ها، تحلیل سؤال و خروجی‌های آزمون را مدیریت می‌کند.",
                Design69Icons.Reports,
                listOf(neo.accent, neo.accent2),
                onStats
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

    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    var settling by remember { mutableStateOf(false) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { Design69ManagementCardsContract.DRAG_THRESHOLD_DP.dp.toPx() }
    val exitHorizontal = with(density) { 520.dp.toPx() }

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
                coroutineScope {
                    launch { dragX.animateTo(targetX, tween(280)) }
                    launch { dragY.animateTo(targetY, tween(280)) }
                }
                // V55.18 — گزارش دستگاه: «به راست نرم نیست». در کشیدن به راست
                // (direction=-1) کارت فعالِ جدید = کارت قبلی پشته است و چون
                // translation فقط روی کارت فعال اعمال می‌شود، snap فوری صفر
                // باعث «پرش» ورود آن می‌شد. اکنون کارت جدید از همان سمت خروج
                // (targetX) وارد و نرم به مرکز می‌آید؛ کشیدن به چپ مثل قبل.
                if (direction == -1) {
                    dragX.snapTo(targetX)
                    dragY.snapTo(targetY)
                    activeIndex = (activeIndex + direction + cards.size) % cards.size
                    coroutineScope {
                        launch { dragX.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
                        launch { dragY.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
                    }
                } else {
                    activeIndex = (activeIndex + direction + cards.size) % cards.size
                    dragX.snapTo(0f)
                    dragY.snapTo(0f)
                }
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

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
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
                if (relative <= 2) {
                    val active = relative == 0
                    val data = cards[index]
                    val stackTop by animateDpAsState(
                        (30 + relative * 30).dp,
                        tween(650, easing = FastOutSlowInEasing),
                        label = "management-card-top-$index"
                    )
                    val stackScale by animateFloatAsState(
                        1f - relative * .075f,
                        tween(650, easing = FastOutSlowInEasing),
                        label = "management-card-scale-$index"
                    )
                    val stackAlpha by animateFloatAsState(
                        1f - relative * .25f,
                        tween(500),
                        label = "management-card-alpha-$index"
                    )
                    val stackRotation by animateFloatAsState(
                        if (relative == 0) 0f else if (relative == 1) 5f else -6f,
                        tween(650, easing = FastOutSlowInEasing),
                        label = "management-card-rotation-$index"
                    )
                    Box(
                        Modifier
                            .padding(top = stackTop)
                            .fillMaxWidth(.90f)
                            .height(190.dp)
                            .zIndex(3f - relative)
                            .graphicsLayer {
                                scaleX = stackScale
                                scaleY = stackScale
                                alpha = stackAlpha
                                translationX = if (active) dragX.value else 0f
                                translationY = if (active) dragY.value else 0f
                                rotationZ = if (active) {
                                    stackRotation + dragX.value / 42f + dragY.value / 75f
                                } else stackRotation
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
            depth = 9.dp,
            pressed = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    cards[activeIndex].title,
                    color = neo.ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    cards[activeIndex].subtitle,
                    color = neo.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
