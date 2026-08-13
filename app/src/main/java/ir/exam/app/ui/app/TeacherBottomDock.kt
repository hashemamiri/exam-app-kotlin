package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class TeacherDockSection { MENU, WALLET, CREATE, EXAMS, CARDS, NONE }

enum class TeacherDockAction { MENU, WALLET, CREATE, EXAMS, CARDS }
enum class TeacherQuickCreateAction { STUDENT, EXAM, CLASS }
enum class TeacherManagementAction { STATS, GRADING, PENDING }

/** قرارداد قابل تست طرح ۶۹؛ ترتیب فیزیکی در RTL از راست به چپ است. */
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

/** نوار ثابت پنج‌دکمه‌ای معلم با ظاهر نئومورفیک، FAB کمانی و مسیرهای واقعی V15. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherBottomDock(
    active: TeacherDockSection,
    onMenu: () -> Unit,
    onWallet: () -> Unit,
    onCreateStudent: () -> Unit,
    onCreateExam: () -> Unit,
    onCreateClass: () -> Unit,
    onExams: () -> Unit,
    onStats: () -> Unit,
    onGrading: () -> Unit,
    onPending: () -> Unit
) {
    var createExpanded by rememberSaveable { mutableStateOf(false) }
    var cardsOpen by rememberSaveable { mutableStateOf(false) }
    val colors = neumorphic69Colors
    val height by animateDpAsState(
        targetValue = if (createExpanded) 232.dp else 102.dp,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "dock-height"
    )
    val rotation by animateFloatAsState(
        targetValue = if (createExpanded) 45f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "plus-rotation"
    )

    BackHandler(enabled = createExpanded) { createExpanded = false }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(height)
        ) {
            AnimatedVisibility(
                visible = createExpanded,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(220)) + scaleIn(tween(360), initialScale = .64f),
                exit = fadeOut(tween(150)) + scaleOut(tween(220), targetScale = .70f)
            ) {
                Box(Modifier.fillMaxWidth().height(220.dp)) {
                    ArcAction(
                        label = "دانش‌آموز جدید",
                        icon = Icons.Outlined.PersonAdd,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(x = (-108).dp, y = (-84).dp)
                    ) {
                        createExpanded = false
                        onCreateStudent()
                    }
                    ArcAction(
                        label = "آزمون جدید",
                        icon = Icons.Outlined.PostAdd,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-140).dp)
                    ) {
                        createExpanded = false
                        onCreateExam()
                    }
                    ArcAction(
                        label = "کلاس جدید",
                        icon = Icons.Outlined.GroupAdd,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(x = 108.dp, y = (-84).dp)
                    ) {
                        createExpanded = false
                        onCreateClass()
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                NeumorphicPanel(
                    modifier = Modifier.fillMaxWidth().height(82.dp),
                    radius = 28.dp,
                    depth = LocalNeumorphic69Depth.current + 2.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(82.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DockItem("منو", Icons.Outlined.Menu, active == TeacherDockSection.MENU, Modifier.weight(1f)) {
                            createExpanded = false
                            onMenu()
                        }
                        DockItem("کیف پول", Icons.Outlined.AccountBalanceWallet, active == TeacherDockSection.WALLET, Modifier.weight(1f)) {
                            createExpanded = false
                            onWallet()
                        }
                        CenterAction(
                            label = "افزودن",
                            expanded = createExpanded,
                            active = active == TeacherDockSection.CREATE,
                            rotation = rotation,
                            modifier = Modifier.weight(1f)
                        ) {
                            createExpanded = !createExpanded
                        }
                        DockItem("آزمون‌ها", Icons.AutoMirrored.Outlined.Assignment, active == TeacherDockSection.EXAMS, Modifier.weight(1f)) {
                            createExpanded = false
                            onExams()
                        }
                        DockItem("کارت‌ها", Icons.Outlined.Dashboard, active == TeacherDockSection.CARDS, Modifier.weight(1f)) {
                            createExpanded = false
                            cardsOpen = true
                        }
                    }
                }
            }
        }

        if (cardsOpen) {
            ModalBottomSheet(
                onDismissRequest = { cardsOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = colors.background,
                contentColor = colors.ink,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                tonalElevation = 0.dp
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("کارت‌های مدیریتی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.ink)
                    Text("یکی از مسیرهای واقعی مدیریت را انتخاب کنید.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    ManagementCard("آمار و گزارش‌ها", "نمودارها، میانگین، تحلیل سؤال و خروجی", Icons.Outlined.BarChart) {
                        cardsOpen = false
                        onStats()
                    }
                    ManagementCard("تصحیح", "پاسخ‌ها، حضور، بازخورد و تأیید نمره", Icons.AutoMirrored.Outlined.FactCheck) {
                        cardsOpen = false
                        onGrading()
                    }
                    ManagementCard("مانده", "پاسخ‌های در انتظار تصحیح و پیگیری", Icons.Outlined.PendingActions) {
                        cardsOpen = false
                        onPending()
                    }
                    Box(Modifier.height(22.dp))
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = neumorphic69Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (selected || pressed) 1.12f else 1f,
        tween(180),
        label = "dock-$label-scale"
    )
    Box(
        modifier
            .height(64.dp)
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .then(
                if (selected || pressed) Modifier.neumorphic69(colors, 18.dp, 8.dp, pressed = true)
                else Modifier
            )
            .clip(RoundedCornerShape(18.dp))
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) colors.accent else colors.muted,
            modifier = Modifier.size(26.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .width(19.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(colors.accent, colors.accent2)))
            )
        }
    }
}

@Composable
private fun CenterAction(
    label: String,
    expanded: Boolean,
    active: Boolean,
    rotation: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = neumorphic69Colors
    Box(modifier.height(82.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .offset(y = (-16).dp)
                .size(66.dp)
                .neumorphic69(colors, 33.dp, if (expanded) 18.dp else 12.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
                .semantics { selected = expanded || active }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = if (expanded) "بستن منوی $label" else "بازکردن منوی $label",
                tint = Color.White,
                modifier = Modifier.size(31.dp)
            )
        }
    }
}

@Composable
private fun ArcAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = neumorphic69Colors
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(56.dp)
                .neumorphic69(colors, 28.dp, 13.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        NeumorphicPanel(
            modifier = Modifier.width(104.dp).height(28.dp),
            radius = 12.dp,
            depth = 7.dp,
            pressed = true,
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = neumorphic69Colors
    NeumorphicPressable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(82.dp),
        radius = 22.dp,
        depth = 11.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .neumorphic69(colors, 16.dp, 8.dp, pressed = true),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.ink)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
