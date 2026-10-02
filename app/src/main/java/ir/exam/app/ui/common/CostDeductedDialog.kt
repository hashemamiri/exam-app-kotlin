package ir.exam.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * V202 — پیام یکسانِ «کسر هزینه» برای همهٔ مسیرهای کسر از کیف پول
 * (ذخیرهٔ آزمون، تکثیر، چاپ V132، پرداخت چاپ V199، بازیابی پشتیبان، انتقال اعتبار مدیر).
 * همیشه وسط صفحه است، با لمس بیرون/بازگشت بسته نمی‌شود و فقط با دکمهٔ «تأیید» محو می‌شود.
 */
@Composable
fun CostDeductedDialog(
    message: String,
    title: String = "کسر هزینه انجام شد",
    details: List<String> = emptyList(),
    confirmLabel: String = "تأیید",
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(message)
                details.forEach { Text(it) }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } }
    )
}

/** متن استاندارد: «کسر ۱٬۰۰۰ تومان از کیف پول با موفقیت انجام شد (موجودی: … تومان)» */
fun costDeductedMessage(costToman: Long, balanceToman: Long?): String =
    "کسر " + "%,d".format(java.util.Locale.US, costToman) + " تومان از کیف پول با موفقیت انجام شد" +
        (balanceToman?.let { " (موجودی: " + "%,d".format(java.util.Locale.US, it) + " تومان)" } ?: "")
