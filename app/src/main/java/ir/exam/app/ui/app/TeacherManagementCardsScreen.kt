package ir.exam.app.ui.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object Design69ManagementCardsContract {
    // V131 — کارت «کارنامه» (کارنامه و لیست نمرات) از «آمار» جدا شد.
    // V313.0/V313.2 — کارت «شرکت در آزمون» + ۴ کارت عملیاتی آزمون (حضور و غیاب، گزارش نظارتی، تحلیل سؤال‌ها، بانک بازخورد).
    const val CARD_COUNT = 12
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
    onRequests: () -> Unit,
    onPracticeExam: () -> Unit = {},
    onAttendance: () -> Unit = onGrading,
    onMonitor: () -> Unit = onGrading,
    onAnalysis: () -> Unit = onStats,
    onFeedbackBank: () -> Unit = onGrading
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
        onPracticeExam,
        onAttendance,
        onMonitor,
        onAnalysis,
        onFeedbackBank,
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
                Design69Icons.ReportCard, // V280 — آیکن متمایز از «آمار»
                listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
                onGradeList
            ),
            ManagementCardSpec(
                "بانک سؤال",
                "جست‌وجو، دسته‌بندی، مشاهده، ویرایش، حذف و افزودن سؤال به آزمون.",
                Design69Icons.QuestionBank,
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
                Design69Icons.Pending,
                listOf(Color(0xFFE0587F), Color(0xFF7D6CF4)),
                onPending
            ),
            ManagementCardSpec(
                "پاسخ",
                "فقط پاسخ‌های تصحیح‌شده دارای نمره و بازخورد نهایی را نمایش می‌دهد.",
                Design69Icons.Answers,
                listOf(Color(0xFF4D5B74), Color(0xFF273247)),
                onAnswers
            ),
            ManagementCardSpec(
                "درخواست‌ها",
                "درخواست‌های ویرایش یا حذف مدیر را مشاهده، تأیید یا رد کنید.",
                Design69Icons.Requests,
                listOf(Color(0xFF7D6CF4), Color(0xFFE0587F)),
                onRequests
            ),
            ManagementCardSpec(
                "شرکت در آزمون",
                "پاسخ‌گویی آزمایشی به آزمون‌های خود دقیقاً در محیط دانش‌آموز و مشاهدهٔ نمره.",
                Design69Icons.Exams,
                listOf(Color(0xFF10B981), Color(0xFF3B82F6)),
                onPracticeExam
            ),
            ManagementCardSpec(
                "حضور و غیاب",
                "حاضرین و غایبین آزمون، وضعیت زنده، تمدید وقت و ریست تلاش دانش‌آموزان.",
                Design69Icons.Students,
                listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
                onAttendance
            ),
            ManagementCardSpec(
                "گزارش نظارتی",
                "بررسی خروج از صفحه، اسکرین‌شات، تلاش برای کپی و زمان صرف‌شده روی هر سؤال.",
                Design69Icons.InfoUpdate,
                listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
                onMonitor
            ),
            ManagementCardSpec(
                "تحلیل سؤال‌ها",
                "ضریب دشواری، درصد پاسخ صحیح و توزیع گزینه‌های انتخاب‌شده در هر سؤال.",
                Design69Icons.Dashboard,
                listOf(Color(0xFF14B8A6), Color(0xFF6366F1)),
                onAnalysis
            ),
            ManagementCardSpec(
                "بانک بازخورد",
                "مدیریت، افزودن، ویرایش و حذف جملات آمادهٔ تشویقی و بازخورد تصحیح.",
                Design69Icons.Header,
                listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)),
                onFeedbackBank
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

private fun wrapFerrisDistance(d: Float, n: Int): Float {
    var w = ((d % n) + n) % n
    if (w > n / 2f) w -= n
    return w
}

/** V315.4 — چرخ‌وفلک کارت‌ها (Ferris Wheel): چرخش روی مدار بیضوی با حفظ حالت سرپا برای معلم و مدیر. */
@Composable
private fun ManagementCardsStack(cycleKey: Int, cards: List<ManagementCardSpec>) {
    val neo = neumorphic69Colors
    val n = cards.size.coerceAtLeast(1)
    val wheelPos = remember { Animatable(0f) }
    var targetPos by rememberSaveable { mutableFloatStateOf(0f) }
    var dragVel by remember { mutableFloatStateOf(0f) }
    var lastDragMs by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragSpanPx = with(density) { 190.dp.toPx() }
    val orbitRxPx = with(density) { 112.dp.toPx() }
    val orbitRyPx = with(density) { 44.dp.toPx() }
    val activeIndex = ((wheelPos.value.roundToInt() % n) + n) % n

    fun animateWheelTo(nextTarget: Float, initialVelocity: Float = 0f) {
        targetPos = nextTarget
        scope.launch {
            wheelPos.animateTo(
                targetValue = nextTarget,
                animationSpec = spring(dampingRatio = 0.92f, stiffness = 180f),
                initialVelocity = initialVelocity
            )
        }
    }

    fun changeCard(direction: Int) {
        animateWheelTo((targetPos.roundToInt() + direction).toFloat())
    }

    fun goTo(index: Int) {
        val curI = ((targetPos.roundToInt() % n) + n) % n
        var d = ((index - curI) % n + n) % n
        if (d > n / 2f) d -= n
        animateWheelTo((targetPos.roundToInt() + d).toFloat())
    }

    LaunchedEffect(cycleKey) {
        if (cycleKey > 0) changeCard(1)
    }

    // V56.1 — تبلت: چرخ‌وفلک کارت‌ها وسط صفحه با سقف پهنا تا کارت‌ها بیش از حد
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
                .height(316.dp)
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
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragVel = 0f
                            lastDragMs = android.os.SystemClock.uptimeMillis()
                        },
                        onDragEnd = {
                            val projected = (wheelPos.value + dragVel * 0.14f).roundToInt().toFloat()
                            animateWheelTo(projected, dragVel)
                        },
                        onDragCancel = {
                            animateWheelTo(wheelPos.value.roundToInt().toFloat(), 0f)
                        }
                    ) { change, amount ->
                        change.consume()
                        val now = android.os.SystemClock.uptimeMillis()
                        val dtSec = ((now - lastDragMs).coerceAtLeast(1L)) / 1000f
                        lastDragMs = now
                        val dp = -amount.x / dragSpanPx
                        if (dtSec > 0f) {
                            dragVel = 0.7f * dragVel + 0.3f * (dp / dtSec)
                        }
                        val nextP = wheelPos.value + dp
                        targetPos = nextP.roundToInt().toFloat()
                        scope.launch {
                            wheelPos.snapTo(nextP)
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            cards.indices.forEach { index ->
                val data = cards[index]
                val w = wrapFerrisDistance(index - wheelPos.value, n)
                val angle = (w * 2.0 * PI / n).toFloat()
                val cs = cos(angle)
                val sn = sin(angle)
                val active = index == activeIndex
                Box(
                    Modifier
                        .padding(top = 84.dp)
                        .fillMaxWidth(.90f)
                        .height(190.dp)
                        .zIndex(100f + cs * 100f)
                        .graphicsLayer {
                            val sc = 0.78f + 0.22f * cs
                            scaleX = sc
                            scaleY = sc
                            alpha = 0.35f + 0.65f * ((cs + 1f) / 2f)
                            translationX = -sn * orbitRxPx
                            translationY = (cs - 1f) * orbitRyPx
                            rotationZ = 0f
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            cards.indices.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(7.dp)
                        .width(if (index == activeIndex) 23.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (index == activeIndex) neo.accent else neo.darkShadow.copy(alpha = .45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { goTo(index) }
                        )
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
