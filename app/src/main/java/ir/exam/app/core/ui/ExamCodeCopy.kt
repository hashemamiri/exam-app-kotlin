package ir.exam.app.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/** V234 — کپی کد آزمون در حافظهٔ موقت با یک ضربه (آینهٔ سایت: کلیک روی کد = کپی). */
fun copyExamCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard == null || code.isBlank()) {
        Toast.makeText(context, "کپی کد ممکن نشد.", Toast.LENGTH_SHORT).show(); return
    }
    clipboard.setPrimaryClip(ClipData.newPlainText("کد آزمون", code))
    Toast.makeText(context, "کد آزمون $code کپی شد.", Toast.LENGTH_SHORT).show()
}

/** متن کد آزمون که با ضربه کپی می‌شود؛ `prefix`/`suffix` بدون رفتار کپی نمایش داده می‌شوند. */
@Composable
fun CopyableExamCode(code: String?, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall, prefix: String = "", suffix: String = "") {
    val context = LocalContext.current
    val value = code?.takeIf { it.isNotBlank() }
    if (value == null) {
        Text(prefix + "—" + suffix, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
        return
    }
    Text(
        prefix + value + suffix,
        style = style.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.clickable(onClickLabel = "کپی کد آزمون") { copyExamCode(context, value) }
    )
}
