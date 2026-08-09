package ir.exam.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** پوستهٔ مرکزی؛ تنظیم قلم و حالت تیره در فاز تنظیمات به این نقطه متصل می‌شود. */
@Composable fun ExamAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), content = content)
}
