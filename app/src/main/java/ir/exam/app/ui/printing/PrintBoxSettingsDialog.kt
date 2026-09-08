package ir.exam.app.ui.printing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.data.local.PrintBoxStyle

/**
 * V121 — تنظیماتِ کادر/جدول‌بندیِ سراسریِ جدولِ سؤال‌های چاپی: ضخامت/رنگِ
 * خطِ دور، پهنای ستونِ شماره/بارم، فاصلهٔ داخلیِ سلولِ متنِ سؤال.
 *
 * برخلافِ `HeaderSettingsDialog` (فیلدهای متنیِ سربرگ)، این‌ها همه عددی/رنگی
 * هستند؛ اسلایدر برای عدد و یک فیلدِ متنیِ کوتاه برای هگزِ رنگ به کار می‌رود.
 */
@Composable
fun PrintBoxSettingsDialog(
    initial: PrintBoxStyle,
    onApply: (PrintBoxStyle) -> Unit,
    onDismiss: () -> Unit
) {
    var borderWidth by remember { mutableStateOf(initial.borderWidthPx) }
    var borderColor by remember { mutableStateOf(initial.borderColor) }
    var numberColWidth by remember { mutableStateOf(initial.numberColWidthPercent) }
    var scoreColWidth by remember { mutableStateOf(initial.scoreColWidthPercent) }
    var cellPadding by remember { mutableStateOf(initial.cellPaddingPx) }
    val colorValid = borderColor.matches(Regex("#[0-9a-fA-F]{6}"))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "کادر و جدول‌بندی سؤال‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "این تنظیمات روی جدولِ سؤال‌های همهٔ آزمون‌های چاپی این دستگاه اعمال می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))

                LabeledSlider(
                    label = "ضخامت خط دور کادر: ${"%.1f".format(borderWidth)} پیکسل",
                    value = borderWidth,
                    valueRange = 0f..6f,
                    onChange = { borderWidth = it }
                )
                OutlinedTextField(
                    value = borderColor,
                    onValueChange = { borderColor = it },
                    label = { Text("رنگ خط دور کادر (#rrggbb)") },
                    isError = !colorValid,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LabeledSlider(
                    label = "پهنای ستون «ردیف»: ${"%.0f".format(numberColWidth)}٪",
                    value = numberColWidth,
                    valueRange = 3f..20f,
                    onChange = { numberColWidth = it }
                )
                LabeledSlider(
                    label = "پهنای ستون «بارم»: ${"%.0f".format(scoreColWidth)}٪",
                    value = scoreColWidth,
                    valueRange = 3f..20f,
                    onChange = { scoreColWidth = it }
                )
                LabeledSlider(
                    label = "فاصلهٔ داخلی متن سؤال: ${"%.0f".format(cellPadding)} پیکسل",
                    value = cellPadding,
                    valueRange = 0f..30f,
                    onChange = { cellPadding = it }
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            // بازگشت به مقادیرِ پیش‌فرضِ کارخانه.
                            val d = PrintBoxStyle()
                            borderWidth = d.borderWidthPx; borderColor = d.borderColor
                            numberColWidth = d.numberColWidthPercent; scoreColWidth = d.scoreColWidthPercent
                            cellPadding = d.cellPaddingPx
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("پیش‌فرض") }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("انصراف") }
                    TextButton(
                        onClick = {
                            onApply(
                                PrintBoxStyle(
                                    borderWidthPx = borderWidth,
                                    borderColor = if (colorValid) borderColor else initial.borderColor,
                                    numberColWidthPercent = numberColWidth,
                                    scoreColWidthPercent = scoreColWidth,
                                    cellPaddingPx = cellPadding
                                ).sanitized()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("اعمال") }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = valueRange)
    }
}
