package ir.exam.app.ui.code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * V349 — نمایش پاسخ دانش‌آموز در تصحیح: اگر بلوک کد دارد، رنگی نشان داده می‌شود؛
 * بخش متنی (در حالت «هر دو») مثل قبل متن ساده می‌ماند.
 * پاسخ‌های بدون کد هیچ تغییری نمی‌کنند.
 */
@Composable
fun CodeAwareAnswer(text: String, label: String = "پاسخ: ") {
    if (!CodeText.hasFence(text)) {
        Text(label + text, style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (part in CodeText.split(text)) {
            if (part.isCode) {
                CodeBlockView(part.text, part.lang)
            } else if (part.text.isNotBlank()) {
                Text(label + part.text.trim(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
