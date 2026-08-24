package ir.exam.app.ui.builder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.math.QuestionToolIcons

/**
 * V55.16 — درخواست کاربر: به‌جای آیکن فرمول روی کارت گزینه‌های چندگزینه‌ای و
 * جورکردنی، یک دکمهٔ «+» که پنجرهٔ ۸ ابزار درج (همان ۸ ابزار کادر متن سؤال:
 * فرمول، شکل، نمودار، جدول، آناتومی، تناوبی، فیزیک، شیمی) را باز می‌کند و با
 * انتخاب هر ابزار، پنجرهٔ درج همان ابزار برای «همان فیلد» باز می‌شود.
 */
enum class OptionInsertTool(val label: String) {
    FORMULA("فرمول"),
    FIGURE("شکل"),
    GRAPH("نمودار"),
    TABLE("جدول"),
    ANATOMY("آناتومی بدن"),
    PERIODIC("جدول تناوبی"),
    PHYSICS("فیزیک"),
    CHEMISTRY("شیمی")
}

private fun toolIcon(tool: OptionInsertTool): ImageVector = when (tool) {
    OptionInsertTool.FORMULA -> QuestionToolIcons.Formula
    OptionInsertTool.FIGURE -> QuestionToolIcons.Figure
    OptionInsertTool.GRAPH -> QuestionToolIcons.Graph
    OptionInsertTool.TABLE -> QuestionToolIcons.Table
    OptionInsertTool.ANATOMY -> QuestionToolIcons.Anatomy
    OptionInsertTool.PERIODIC -> QuestionToolIcons.Periodic
    OptionInsertTool.PHYSICS -> QuestionToolIcons.Physics
    OptionInsertTool.CHEMISTRY -> QuestionToolIcons.Chemistry
}

/** دکمهٔ + روی کارت گزینه/جورکردنی؛ جایگزین آیکن فرمول قبلی. */
@Composable
fun OptionInsertButton(label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = "درج در $label",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/** پنجرهٔ ۸ ابزار درج؛ با انتخاب هر ابزار بسته می‌شود و ابزار انتخابی را برمی‌گرداند. */
@Composable
fun OptionInsertToolsDialog(
    fieldLabel: String,
    onDismiss: () -> Unit,
    onToolSelected: (OptionInsertTool) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("درج در $fieldLabel") },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(OptionInsertTool.entries.toList(), key = { it.name }) { tool ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clickable { onToolSelected(tool) }
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                toolIcon(tool),
                                contentDescription = tool.label,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(tool.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    )
}
