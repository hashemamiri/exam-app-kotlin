package ir.exam.app.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.profile.ProfileAvatar

/**
 * V136 — چیدمان «دسکتاپ» برای تبلت (معلم/مدیر).
 *
 * خواستهٔ کاربر: چیدمان گوشی دست نخورد؛ روی تبلت برنامه مثل نسخهٔ دسکتاپ به‌نظر برسد.
 * به‌جای داکِ پایین + منوی همبرگریِ تمام‌صفحه، یک «ریل کناری» ثابت در سمت راست
 * (شروعِ RTL) نشان داده می‌شود که پروفایل، میان‌برهای داک (کیف پول/افزودن/آزمون‌ها/کارت‌ها)
 * و همهٔ کارت‌های منو را به‌صورت ردیف‌های فهرستی دارد؛ محتوا بقیهٔ عرض را می‌گیرد.
 * فقط ترکیب‌بندی عوض شده — همهٔ اکشن‌ها همان callbackهای داک/منو هستند.
 */
object TabletDesktopContract {
    const val RAIL_WIDTH_DP = 292
    /** ریل فقط برای معلم/مدیر روی چیدمان تبلت؛ دانش‌آموز داک ندارد و منوی خودش را نگه می‌دارد. */
    fun usesSideRail(tablet: Boolean, role: UserRole): Boolean = tablet && role != UserRole.STUDENT
}

@Composable
fun TabletSideRail(
    user: AppUser,
    dockActive: TeacherDockSection,
    quickAddOpen: Boolean,
    menuCards: List<Design69MenuCard>,
    featuredCard: Design69MenuCard?,
    primaryLabel: String,
    primaryIcon: ImageVector,
    onProfile: () -> Unit,
    onWallet: () -> Unit,
    onAdd: () -> Unit,
    onExams: () -> Unit,
    onCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = neumorphic69Colors
    NeumorphicPanel(
        modifier = modifier
            .width(TabletDesktopContract.RAIL_WIDTH_DP.dp)
            .fillMaxHeight()
            .padding(start = 12.dp, top = 8.dp, bottom = 12.dp),
        radius = 26.dp,
        depth = LocalNeumorphic69Depth.current + 2.dp,
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // پروفایل
            NeumorphicPressable(
                onClick = onProfile,
                modifier = Modifier.fillMaxWidth().height(84.dp),
                radius = 22.dp,
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAvatar(user.avatarUrl, user.name.ifBlank { "کاربر" }, 52)
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (user.role == UserRole.MANAGER) "پروفایل مدیر/معاون" else "پروفایل معلم",
                            color = colors.accent,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            user.name.ifBlank { "حساب کاربری من" },
                            color = colors.ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            // دکمهٔ افزودن (همان + وسط داک)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onAdd
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Design69Icons.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        if (quickAddOpen) "بستن" else if (user.role == UserRole.MANAGER) "دعوت / افزودن" else "افزودن",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            RailRow(primaryLabel, primaryIcon, selected = dockActive == TeacherDockSection.EXAMS, onClick = onExams)
            RailRow("کارت‌ها", Design69Icons.Cards, selected = dockActive == TeacherDockSection.CARDS, onClick = onCards)
            RailRow("کیف پول", Design69Icons.Wallet, selected = dockActive == TeacherDockSection.WALLET, onClick = onWallet)
            featuredCard?.let { RailRow(it.title, it.icon, selected = it.selected, onClick = it.onClick) }

            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = colors.darkShadow.copy(alpha = .35f))

            menuCards.forEach { card ->
                RailRow(card.title, card.icon, selected = card.selected, danger = card.danger, onClick = card.onClick)
            }
        }
    }
}

@Composable
private fun RailRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val colors = neumorphic69Colors
    val tint = when {
        danger -> colors.danger
        selected -> colors.accent
        else -> colors.muted
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accent.copy(alpha = .14f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            title,
            color = if (danger) colors.danger else if (selected) colors.accent else colors.ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
