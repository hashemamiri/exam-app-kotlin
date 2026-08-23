package ir.exam.app.ui.math

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** مرحلهٔ ۲: آداپتور تک‌مقداری؛ هر بار فقط target فعال Builder را ویرایش می‌کند. */
@Composable
fun QuestionEditorWebViewDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    var ready by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = ready, onClick = { onApply(value) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        text = {
            QuestionEditorWebViewPoc(
                modifier = Modifier.fillMaxWidth().height(520.dp),
                initialValue = initialValue,
                onValueChanged = { value = it },
                onReady = { ready = true }
            )
        }
    )
}
