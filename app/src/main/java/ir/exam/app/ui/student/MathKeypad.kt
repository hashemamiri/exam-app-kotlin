package ir.exam.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * V264 — صفحه‌کلید ریاضی پاسخ دانش‌آموز (آیتم ۱۲). عددی: فقط رقم/ممیز/منفی (تصحیح خودکار عددی دست‌نخورده می‌ماند)؛
 * جای خالی: رقم + نمادهای ریاضی. کلیدها به انتهای متن افزوده می‌شوند؛ «⌫» آخرین نویسه را پاک می‌کند.
 */
object MathKeys {
    val NUMERIC: List<String> = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0", "-")
    val SYMBOLS: List<String> = listOf(
        "+", "−", "×", "÷", "=", "≠", "<", ">", "≤", "≥", "(", ")", "√", "π", "²", "³", "^", "/", "%", "°", "∞", "θ", "α", "β", "x", "y"
    )
    const val BACKSPACE = "⌫"

    /** اعمال کلید روی متن فعلی (خالص؛ تست واحد). */
    fun apply(current: String, key: String, numeric: Boolean): String = when {
        key == BACKSPACE -> current.dropLast(1)
        numeric -> {
            val next = current + key
            // فقط یک ممیز و منفی فقط در ابتدا
            if (key == "-" && current.isNotEmpty()) current
            else if (key == "." && current.contains('.')) current
            else next.filter { it.isDigit() || it in ".-" }
        }
        else -> current + key
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathKeypad(numeric: Boolean, onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val keys = (if (numeric) MathKeys.NUMERIC else MathKeys.NUMERIC + MathKeys.SYMBOLS) + MathKeys.BACKSPACE
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.forEach { k ->
                    OutlinedButton(
                        onClick = { onKey(k) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp).semantics { contentDescription = if (k == MathKeys.BACKSPACE) "پاک کردن" else "کلید $k" }
                    ) { Text(k, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}
