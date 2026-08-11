package ir.exam.app.ui.math

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.exam.app.core.math.NativeMathFormatter

private data class FormulaTemplate(val label: String, val tex: String)

private val formulaTemplates = listOf(
    FormulaTemplate("کسر", "\\frac{a}{b}"),
    FormulaTemplate("رادیکال", "\\sqrt{x}"),
    FormulaTemplate("توان", "x^{2}"),
    FormulaTemplate("زیرنویس", "a_{1}"),
    FormulaTemplate("انتگرال", "\\int_{a}^{b} f(x) dx"),
    FormulaTemplate("مجموع", "\\sum_{i=1}^{n} x_i"),
    FormulaTemplate("حد", "lim_{x\\rightarrow a} f(x)"),
    FormulaTemplate("ماتریس", "\\begin{bmatrix}a&b\\\\c&d\\end{bmatrix}"),
    FormulaTemplate("دلتا", "\\Delta x"),
    FormulaTemplate("تتا", "\\theta"),
    FormulaTemplate("نامساوی", "a\\leq b"),
    FormulaTemplate("بی‌نهایت", "\\infty")
)

@Composable
fun FormulaEditorDialog(
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit
) {
    var raw by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val tex = runCatching { NativeMathFormatter.quickToTex(raw) }.getOrDefault(raw)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("درج فرمول Native") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("فرمول را به صورت TeX یا تایپ سریع بنویسید. خروجی داخل علامت دلار در متن قرار می‌گیرد.")
                }
                item {
                    OutlinedTextField(
                        value = raw,
                        onValueChange = { raw = it.take(2_000); error = null },
                        label = { Text("مثال: \\frac{1}{2} یا sqrt(x)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("پیش‌نمایش")
                    NativeMathText("${'$'}$tex${'$'}", modifier = Modifier.fillMaxWidth())
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        formulaTemplates.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                row.forEach { item ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { raw = item.tex; error = null },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    }
                }
                error?.let { item { Text(it) } }
            }
        },
        confirmButton = {
            Button(
                enabled = raw.isNotBlank(),
                onClick = {
                    if (!NativeMathFormatter.isBalanced(tex)) {
                        error = "تعداد آکولادهای باز و بسته برابر نیست."
                    } else {
                        onInsert(tex)
                    }
                }
            ) { Text("درج فرمول") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
