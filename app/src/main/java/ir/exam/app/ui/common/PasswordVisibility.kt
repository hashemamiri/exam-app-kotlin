package ir.exam.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

fun passwordTransformation(visible: Boolean): VisualTransformation =
    if (visible) VisualTransformation.None else PasswordVisualTransformation()

@Composable
fun PasswordVisibilityButton(
    visible: Boolean,
    onToggle: () -> Unit
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (visible) "پنهان‌کردن رمز" else "نمایش رمز"
        )
    }
}
