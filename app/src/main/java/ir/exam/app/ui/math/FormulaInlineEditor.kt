package ir.exam.app.ui.math

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.math.FormulaTextCodec

/** فهرست فرمول‌های موجود در یک متن برای ویرایش/حذف مستقیم، بدون انتخاب دستی `$...$`. */
@Composable
fun ExistingFormulaEditor(
    source: String,
    modifier: Modifier = Modifier,
    onEdit: (occurrenceIndex: Int, tex: String) -> Unit,
    onDelete: (occurrenceIndex: Int) -> Unit
) {
    val formulas = FormulaTextCodec.occurrences(source)
    if (formulas.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("فرمول‌های قابل ویرایش")
        formulas.forEach { occurrence ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    NativeFormulaIcon(
                        occurrence.tex,
                        Modifier.width(130.dp).height(42.dp),
                        18.sp,
                        contentDescription = "فرمول ${occurrence.index + 1}"
                    )
                    TextButton(onClick = { onEdit(occurrence.index, occurrence.tex) }) { Text("ویرایش") }
                    TextButton(onClick = { onDelete(occurrence.index) }) { Text("حذف") }
                }
            }
        }
    }
}
