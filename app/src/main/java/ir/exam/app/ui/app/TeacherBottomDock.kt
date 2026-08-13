package ir.exam.app.ui.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class TeacherDockSection { MENU, WALLET, CREATE, EXAMS, CARDS, NONE }
enum class TeacherDockAction { MENU, WALLET, CREATE, EXAMS, CARDS }
enum class TeacherQuickCreateAction { STUDENT, EXAM, CLASS }
enum class TeacherManagementAction { STATS, GRADING, PENDING }

object TeacherDockContract {
    val order = listOf(
        TeacherDockAction.MENU,
        TeacherDockAction.WALLET,
        TeacherDockAction.CREATE,
        TeacherDockAction.EXAMS,
        TeacherDockAction.CARDS
    )
    val quickCreateOrder = listOf(
        TeacherQuickCreateAction.STUDENT,
        TeacherQuickCreateAction.EXAM,
        TeacherQuickCreateAction.CLASS
    )
    val managementOrder = listOf(
        TeacherManagementAction.STATS,
        TeacherManagementAction.GRADING,
        TeacherManagementAction.PENDING
    )
}

private enum class DockMotion { MENU, WALLET, EXAMS, CARDS }

/** نوار پنج‌دکمه‌ای طرح ۶۹ با آیکن‌های خطی، ripple و میکروانیمیشن اختصاصی. */
@Composable
fun TeacherBottomDock(
    active: TeacherDockSection,
    menuOpen: Boolean,
    quickAddOpen: Boolean,
    onMenu: () -> Unit,
    onWallet: () -> Unit,
    onAdd: () -> Unit,
    onExams: () -> Unit,
    onCards: () -> Unit
) {
    val colors = neumorphic69Colors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(102.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            NeumorphicPanel(
                modifier = Modifier.fillMaxWidth().height(82.dp),
                radius = 28.dp,
                depth = LocalNeumorphic69Depth.current + 2.dp,
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockItem(
                        label = "منو",
                        selected = menuOpen || active == TeacherDockSection.MENU,
                        motion = DockMotion.MENU,
                        modifier = Modifier.weight(1f),
                        onClick = onMenu
                    ) { tint, iconModifier ->
                        Design69MorphingMenuIcon(menuOpen, tint, iconModifier)
                    }
                    DockItem(
                        label = "کیف پول",
                        selected = active == TeacherDockSection.WALLET && !menuOpen,
                        motion = DockMotion.WALLET,
                        modifier = Modifier.weight(1f),
                        onClick = onWallet,
                        icon = Design69Icons.Wallet
                    )
                    CenterAddAction(
                        expanded = quickAddOpen,
                        modifier = Modifier.weight(1f),
                        onClick = onAdd
                    )
                    DockItem(
                        label = "آزمون‌ها",
                        selected = active == TeacherDockSection.EXAMS && !menuOpen,
                        motion = DockMotion.EXAMS,
                        modifier = Modifier.weight(1f),
                        onClick = onExams,
                        icon = Design69Icons.Exams
                    )
                    DockItem(
                        label = "کارت‌ها",
                        selected = active == TeacherDockSection.CARDS && !menuOpen,
                        motion = DockMotion.CARDS,
                        modifier = Modifier.weight(1f),
                        onClick = onCards,
                        icon = Design69Icons.Cards
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    selected: Boolean,
    motion: DockMotion,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    customIcon: (@Composable (Color, Modifier) -> Unit)? = null
) {
    val colors = neumorphic69Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(if (selected) (-3).dp else 0.dp, tween(300), label = "dock-lift-$label")
    val scale by animateFloatAsState(if (pressed) .94f else 1f, tween(120), label = "dock-press-$label")
    val motionProgress = remember { Animatable(0f) }
    val rippleProgress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var pulse by remember { mutableIntStateOf(0) }

    LaunchedEffect(pulse) {
        if (pulse > 0) {
            motionProgress.snapTo(0f)
            motionProgress.animateTo(1f, tween(if (motion == DockMotion.WALLET) 650 else 620))
            motionProgress.snapTo(0f)
        }
    }

    val p = motionProgress.value
    val wave = sin(PI.toFloat() * p)
    val iconModifier = Modifier
        .size(24.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            when (motion) {
                DockMotion.MENU -> Unit
                DockMotion.WALLET -> rotationY = 180f * wave
                DockMotion.EXAMS -> translationY =
                    (-4.dp.toPx() * wave) + (2.dp.toPx() * sin(2f * PI.toFloat() * p))
                DockMotion.CARDS -> {
                    translationX = 4.dp.toPx() * sin(2f * PI.toFloat() * p)
                    rotationZ = 7f * sin(2f * PI.toFloat() * p)
                }
            }
        }

    Box(
        modifier
            .height(62.dp)
            .semantics {
                this.selected = selected
                contentDescription = label
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) {
                pulse += 1
                scope.launch {
                    rippleProgress.snapTo(0f)
                    rippleProgress.animateTo(1f, tween(520))
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .offset { IntOffset(0, lift.roundToPx()) }
                .size(44.dp)
                .then(
                    if (selected || pressed) {
                        Modifier.neumorphic69(colors, 14.dp, 6.dp, pressed = true)
                    } else Modifier
                )
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            val ripple = rippleProgress.value
            if (ripple < 1f) {
                Canvas(Modifier.size(44.dp)) {
                    drawCircle(
                        color = colors.accent.copy(alpha = .22f * (1f - ripple)),
                        radius = size.minDimension * (.12f + .62f * ripple),
                        center = center
                    )
                }
            }
            when {
                customIcon != null -> customIcon(
                    if (selected) colors.accent else colors.muted,
                    iconModifier
                )
                icon != null -> Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) colors.accent else colors.muted,
                    modifier = iconModifier
                )
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
        }
    }
}

@Composable
private fun CenterAddAction(
    expanded: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = neumorphic69Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(modifier.height(82.dp), contentAlignment = Alignment.Center) {
        if (!expanded) {
            Box(
                Modifier
                    .size(58.dp)
                    .neumorphic69(colors, 29.dp, if (pressed) 8.dp else 11.dp)
                    .graphicsLayer {
                        shape = CircleShape
                        clip = true
                        scaleX = if (pressed) .94f else 1f
                        scaleY = if (pressed) .94f else 1f
                    }
                    .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Design69Icons.Add,
                    contentDescription = "بازکردن افزودن سریع",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}
