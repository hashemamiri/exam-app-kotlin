package ir.exam.app.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class TeacherDockSection { MENU, WALLET, CREATE, EXAMS, CARDS, NONE }

/** نوار ثابت پنج‌دکمه‌ای معلم با FAB کمانی و Sheet کارت‌های مدیریتی. */
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
    var createExpanded by remember { mutableStateOf(false) }
    var cardsOpen by remember { mutableStateOf(false) }
    val height by animateDpAsState(if (createExpanded) 226.dp else 96.dp, label = "dock-height")
    val accent = Color(0xFFFF6F91)
    val rotation by animateFloatAsState(if (createExpanded) 45f else 0f, label = "plus-rotation")

    BackHandler(enabled = createExpanded) { createExpanded = false }

    Box(Modifier.fillMaxWidth().height(height)) {
        AnimatedVisibility(
            visible = createExpanded,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + scaleIn(initialScale = .72f),
            exit = fadeOut() + scaleOut(targetScale = .72f)
        ) {
            Box(Modifier.fillMaxWidth().height(210.dp)) {
                ArcAction(
                    label = "دانش‌آموز جدید",
                    icon = Icons.Outlined.PersonAdd,
                    accent = accent,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-112).dp, y = (-82).dp)
                ) { createExpanded = false; onCreateStudent() }
                ArcAction(
                    label = "آزمون جدید",
                    icon = Icons.Outlined.PostAdd,
                    accent = accent,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-132).dp)
                ) { createExpanded = false; onCreateExam() }
                ArcAction(
                    label = "کلاس جدید",
                    icon = Icons.Outlined.GroupAdd,
                    accent = accent,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(x = 112.dp, y = (-82).dp)
                ) { createExpanded = false; onCreateClass() }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(82.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .shadow(14.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                )
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier.fillMaxWidth().height(68.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DockItem("منو", Icons.Outlined.Menu, active == TeacherDockSection.MENU, accent, Modifier.weight(1f)) {
                        createExpanded = false; onMenu()
                    }
                    DockItem("کیف پول", Icons.Outlined.AccountBalanceWallet, active == TeacherDockSection.WALLET, accent, Modifier.weight(1f)) {
                        createExpanded = false; onWallet()
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier
                                .offset(y = (-22).dp)
                                .size(66.dp)
                                .shadow(if (createExpanded) 18.dp else 10.dp, CircleShape)
                                .graphicsLayer { rotationZ = rotation }
                                .clickable(role = Role.Button) { createExpanded = !createExpanded },
                            shape = CircleShape,
                            color = accent,
                            contentColor = Color.White
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Add, contentDescription = if (createExpanded) "بستن منوی افزودن" else "بازکردن منوی افزودن", modifier = Modifier.size(32.dp))
                            }
                        }
                        Text(
                            "افزودن",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (createExpanded || active == TeacherDockSection.CREATE) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DockItem("آزمون‌ها", Icons.AutoMirrored.Outlined.Assignment, active == TeacherDockSection.EXAMS, accent, Modifier.weight(1f)) {
                        createExpanded = false; onExams()
                    }
                    DockItem("کارت‌ها", Icons.Outlined.Dashboard, active == TeacherDockSection.CARDS, accent, Modifier.weight(1f)) {
                        createExpanded = false; cardsOpen = true
                    }
                }
            }
        }
    }

    if (cardsOpen) {
        ModalBottomSheet(
            onDismissRequest = { cardsOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("کارت‌های مدیریتی", style = MaterialTheme.typography.titleLarge)
                Text("یکی از کارت‌ها را برای مشاهده و مدیریت انتخاب کنید.", style = MaterialTheme.typography.bodySmall)
                ManagementCard("آمار و گزارش‌ها", "نمودارها، میانگین، تحلیل سؤال و خروجی", Icons.Outlined.BarChart, accent) {
                    cardsOpen = false; onStats()
                }
                ManagementCard("تصحیح", "پاسخ‌ها، حضور، بازخورد و تأیید نمره", Icons.AutoMirrored.Outlined.FactCheck, accent) {
                    cardsOpen = false; onGrading()
                }
                ManagementCard("مانده", "پاسخ‌های در انتظار تصحیح و پیگیری", Icons.Outlined.PendingActions, accent) {
                    cardsOpen = false; onPending()
                }
                Box(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.13f else 1f, label = "dock-$label-scale")
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) accent.copy(alpha = .19f) else Color.Transparent,
            contentColor = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Box(Modifier.size(36.dp).graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(23.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ArcAction(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier.size(54.dp).shadow(12.dp, CircleShape).clickable(role = Role.Button, onClick = onClick),
            shape = CircleShape,
            color = accent,
            contentColor = Color.White
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(27.dp)) }
        }
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .96f)) {
            Text(label, Modifier.width(104.dp).padding(horizontal = 5.dp, vertical = 3.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ManagementCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .16f), contentColor = accent) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
