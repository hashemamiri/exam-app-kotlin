package ir.exam.app.ui.app

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** رنگ‌های تطبیقی پوستهٔ نئومورفیک؛ هیچ داده یا state نمایشی در این لایه نگهداری نمی‌شود. */
@Immutable
data class Neumorphic69Colors(
    val background: Color = Color(0xFFE9EEF5),
    val surface: Color = Color(0xFFE9EEF5),
    val ink: Color = Color(0xFF263142),
    val muted: Color = Color(0xFF7B8798),
    val accent: Color = Color(0xFF6C63F5),
    val accent2: Color = Color(0xFF27C4A8),
    val lightShadow: Color = Color.White,
    val darkShadow: Color = Color(0xFFABB7C7),
    val danger: Color = Color(0xFFC85B6B)
)

val LocalNeumorphic69Colors = staticCompositionLocalOf { Neumorphic69Colors() }
val LocalNeumorphic69Depth = staticCompositionLocalOf { 14.dp }

val neumorphic69Colors: Neumorphic69Colors
    @Composable
    @ReadOnlyComposable
    get() = LocalNeumorphic69Colors.current

/**
 * پالت را از MaterialTheme واقعی می‌گیرد؛ بنابراین light/dark، dynamic color و انتخاب کاربر حفظ می‌شود.
 */
@Composable
fun Neumorphic69Provider(
    depth: Float,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < .42f
    val colors = remember(
        scheme.background,
        scheme.surface,
        scheme.onSurface,
        scheme.onSurfaceVariant,
        scheme.primary,
        scheme.secondary,
        scheme.error,
        isDark
    ) {
        Neumorphic69Colors(
            background = scheme.background,
            surface = scheme.surface,
            ink = scheme.onSurface,
            muted = scheme.onSurfaceVariant,
            accent = scheme.primary,
            accent2 = scheme.secondary,
            lightShadow = if (isDark) lerp(scheme.surface, Color.White, .13f) else lerp(scheme.surface, Color.White, .84f),
            darkShadow = if (isDark) lerp(scheme.surface, Color.Black, .55f) else lerp(scheme.surface, Color.Black, .25f),
            danger = scheme.error
        )
    }
    CompositionLocalProvider(
        LocalNeumorphic69Colors provides colors,
        LocalNeumorphic69Depth provides depth.coerceIn(8f, 22f).dp,
        content = content
    )
}

/** سایهٔ دوطرفه و حالت فرورفتهٔ Native، اقتباس‌شده از مرجع طرح ۶۹. */
fun Modifier.neumorphic69(
    colors: Neumorphic69Colors,
    radius: Dp = 24.dp,
    depth: Dp = 14.dp,
    pressed: Boolean = false
): Modifier = drawBehind {
    val corner = radius.toPx()
    val shadow = depth.toPx().coerceAtLeast(1f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colors.surface.toArgb()
    }
    val canvas = drawContext.canvas.nativeCanvas

    if (!pressed) {
        paint.setShadowLayer(
            shadow * .72f,
            shadow * .34f,
            shadow * .38f,
            colors.darkShadow.copy(alpha = .72f).toArgb()
        )
        canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)
        paint.setShadowLayer(
            shadow * .66f,
            -shadow * .30f,
            -shadow * .32f,
            colors.lightShadow.copy(alpha = .92f).toArgb()
        )
        canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)
        paint.clearShadowLayer()
        paint.color = colors.surface.toArgb()
        canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(
                    colors.lightShadow.copy(alpha = .55f),
                    Color.Transparent,
                    colors.darkShadow.copy(alpha = .16f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    } else {
        drawRoundRect(colors.surface, cornerRadius = CornerRadius(corner, corner))
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(
                    colors.darkShadow.copy(alpha = .48f),
                    Color.Transparent,
                    colors.lightShadow.copy(alpha = .72f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (shadow * .30f).coerceAtLeast(3f))
        )
    }
}

@Composable
fun NeumorphicPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    depth: Dp = LocalNeumorphic69Depth.current,
    pressed: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = neumorphic69Colors
    Box(
        modifier
            .neumorphic69(colors, radius, depth, pressed)
            .clip(RoundedCornerShape(radius))
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun NeumorphicPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    depth: Dp = LocalNeumorphic69Depth.current,
    role: Role = Role.Button,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = neumorphic69Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .975f else 1f,
        animationSpec = tween(130),
        label = "neumorphic-press"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neumorphic69(colors, radius, depth, pressed)
            .clip(RoundedCornerShape(radius))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = role,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun NeumorphicIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val colors = neumorphic69Colors
    NeumorphicPressable(
        onClick = onClick,
        modifier = modifier.size(46.dp).semantics { this.selected = selected },
        radius = 16.dp,
        depth = 9.dp
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (selected) colors.accent else colors.muted,
            modifier = Modifier.size(23.dp)
        )
    }
}

/** سربرگ واقعی پوسته؛ همبرگر فقط در نقشی نشان داده می‌شود که دکمهٔ پایین ندارد. */
@Composable
fun NeumorphicTopBar(
    title: String,
    subtitle: String,
    navigationIcon: ImageVector? = null,
    navigationDescription: String? = null,
    navigationIconContent: (@Composable (Color, Modifier) -> Unit)? = null,
    onNavigation: () -> Unit
) {
    val colors = neumorphic69Colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp)
                    .height(74.dp)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationDescription != null && (navigationIcon != null || navigationIconContent != null)) {
                    if (navigationIconContent != null) {
                        NeumorphicPressable(
                            onClick = onNavigation,
                            modifier = Modifier
                                .size(46.dp)
                                .semantics { contentDescription = navigationDescription },
                            radius = 16.dp,
                            depth = 9.dp
                        ) {
                            navigationIconContent(colors.muted, Modifier.size(23.dp))
                        }
                    } else {
                        NeumorphicIconButton(
                            icon = requireNotNull(navigationIcon),
                            description = navigationDescription,
                            onClick = onNavigation
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(colors.accent, colors.accent2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Design69Icons.Exams, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        title,
                        color = colors.ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier
                        .width(34.dp)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(colors.accent, colors.accent2)))
                )
            }
        }
    }
}


/** نوار فشرده و بدون عنوان برای نقش دانش‌آموز؛ فقط کنترل منو را نگه می‌دارد. */
@Composable
fun NeumorphicCompactMenuBar(
    menuOpen: Boolean,
    onToggleMenu: () -> Unit
) {
    val colors = neumorphic69Colors
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .height(54.dp)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        NeumorphicPressable(
            onClick = onToggleMenu,
            modifier = Modifier
                .size(42.dp)
                .semantics {
                    contentDescription = if (menuOpen) "بستن منو" else "بازکردن منو"
                },
            radius = 14.dp,
            depth = 8.dp
        ) {
            Design69MorphingMenuIcon(
                open = menuOpen,
                tint = colors.muted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun NeumorphicMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    val colors = neumorphic69Colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) .97f else 1f,
        animationSpec = tween(130),
        label = "drawer-card-$title"
    )
    val tint = when {
        danger -> colors.danger
        selected -> colors.accent
        else -> colors.muted
    }
    Box(
        modifier
            .height(Design69MenuContract.CARD_HEIGHT_DP.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neumorphic69(colors, 22.dp, 10.dp, pressed = selected || pressed)
            .clip(RoundedCornerShape(22.dp))
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(13.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .neumorphic69(colors, 13.dp, 7.dp, pressed = true),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
                }
                if (selected) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(colors.accent, colors.accent2)))
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                color = if (danger) colors.danger else if (selected) colors.accent else colors.ink,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = colors.muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
