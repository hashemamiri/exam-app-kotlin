package ir.exam.app.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.domain.model.AppUser
import ir.exam.app.domain.model.UserRole
import ir.exam.app.ui.profile.ProfileAvatar

data class Design69MenuCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val selected: Boolean = false,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

object Design69MenuContract {
    const val COLUMNS = 2
    const val PROFILE_HEIGHT_DP = 148
    const val CARD_HEIGHT_DP = 116
    const val TEACHER_CARD_COUNT = 8
    const val STUDENT_CARD_COUNT = 6

    fun isCompleteGrid(count: Int): Boolean = count > 0 && count % COLUMNS == 0
}

/**
 * صفحه کامل منو بدون انیمیشن‌های تو‌در‌تو و stagger طولانی. گذار سریع فقط در پوسته
 * انجام می‌شود تا لمس همبرگر همیشه فوری و پایدار پاسخ دهد.
 */
@Composable
fun Design69MainMenuScreen(
    user: AppUser,
    cards: List<Design69MenuCard>,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    require(Design69MenuContract.isCompleteGrid(cards.size))
    require(
        cards.size == if (user.role == UserRole.TEACHER) {
            Design69MenuContract.TEACHER_CARD_COUNT
        } else {
            Design69MenuContract.STUDENT_CARD_COUNT
        }
    )
    val colors = neumorphic69Colors

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
            NeumorphicPressable(
                onClick = onProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Design69MenuContract.PROFILE_HEIGHT_DP.dp),
                radius = 29.dp,
                depth = LocalNeumorphic69Depth.current + 2.dp,
                contentPadding = PaddingValues(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileAvatar(user.avatarUrl, user.name.ifBlank { "کاربر" }, 76)
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (user.role == UserRole.TEACHER) "پروفایل معلم" else "پروفایل دانش‌آموز",
                            color = colors.accent,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            user.name.ifBlank { "حساب کاربری من" },
                            color = colors.ink,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            if (user.role == UserRole.TEACHER) {
                                user.email?.takeIf(String::isNotBlank) ?: "حساب معلم"
                            } else {
                                "حساب دانش‌آموز"
                            },
                            color = colors.muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Icon(
                        Design69Icons.ChevronLeft,
                        contentDescription = "بازکردن پروفایل",
                        tint = colors.accent.copy(alpha = .68f),
                        modifier = Modifier.size(29.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            cards.chunked(Design69MenuContract.COLUMNS).forEachIndexed { rowIndex, rowCards ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    rowCards.forEach { card ->
                        NeumorphicMenuTile(
                            title = card.title,
                            subtitle = card.subtitle,
                            icon = card.icon,
                            selected = card.selected,
                            danger = card.danger,
                            onClick = card.onClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(Design69MenuContract.CARD_HEIGHT_DP.dp)
                        )
                    }
                }
                if (rowIndex != cards.lastIndex / Design69MenuContract.COLUMNS) {
                    Spacer(Modifier.height(15.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
