package ir.exam.app.ui.figure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.TableSvgRenderer

/**
 * V53.1 — ویرایشگر کاملاً Native «درج جدول» با همان قرارداد مرجع:
 * `{k:'t', t:سبک, X:{title}, C:[[...]]}`. ۱۸ سبک، ۱..۱۵ سطر و ۱..۱۰ ستون.
 */
@Composable
fun TableEditorDialog(
    initialSpec: FigureSpec? = null,
    onDismiss: () -> Unit,
    onInsert: (FigureSpec) -> Unit
) {
    var style by remember {
        mutableStateOf(initialSpec?.type?.takeIf { s -> TableSvgRenderer.STYLES.any { it.first == s } } ?: "header")
    }
    var title by remember { mutableStateOf(initialSpec?.xStr("title").orEmpty()) }
    var cells by remember {
        mutableStateOf(
            initialSpec?.tableCells()?.takeIf { it.isNotEmpty() }
                ?.let { TableSvgRenderer.resize(it, it.size, it.maxOf { row -> row.size }) }
                ?: TableSvgRenderer.defaultSize("header").let { (r, c) -> TableSvgRenderer.sampleCells("header", r, c) }
        )
    }
    val rows = cells.size
    val cols = cells.firstOrNull()?.size ?: 1
    val isEdit = initialSpec != null

    fun hasContent(): Boolean = cells.any { row -> row.any { it.isNotBlank() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "ویرایش جدول" else "درج جدول") },
        confirmButton = {
            TextButton(onClick = { onInsert(FigureSpec.buildTable(style, title.trim(), cells)) }) {
                Text(if (isEdit) "اعمال تغییرات" else "درج در سؤال")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("سبک جدول", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TableSvgRenderer.STYLES.forEach { (id, name) ->
                        val selected = style == id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.clickable {
                                style = id
                                if (!hasContent()) {
                                    val (r, c) = TableSvgRenderer.defaultSize(id)
                                    cells = TableSvgRenderer.sampleCells(id, r, c)
                                }
                            }
                        ) {
                            Text(
                                name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان جدول (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SizeStepper(
                        label = "سطر",
                        value = rows,
                        min = TableSvgRenderer.MIN_ROWS,
                        max = TableSvgRenderer.MAX_ROWS,
                        onChange = { cells = TableSvgRenderer.resize(cells, it, cols) },
                        modifier = Modifier.weight(1f)
                    )
                    SizeStepper(
                        label = "ستون",
                        value = cols,
                        min = TableSvgRenderer.MIN_COLS,
                        max = TableSvgRenderer.MAX_COLS,
                        onChange = { cells = TableSvgRenderer.resize(cells, rows, it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("محتوای خانه‌ها", style = MaterialTheme.typography.labelMedium)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    cells.forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEachIndexed { c, value ->
                                val head = TableSvgRenderer.isHead(style, r, c)
                                BasicTextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        cells = cells.mapIndexed { ri, oldRow ->
                                            if (ri != r) oldRow
                                            else oldRow.mapIndexed { ci, old -> if (ci == c) newValue else old }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(86.dp)
                                        .height(38.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        textDirection = TextDirection.Content,
                                        color = if (head) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { inner ->
                                        Surface(
                                            shape = RoundedCornerShape(7.dp),
                                            color = if (head) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                        ) {
                                            Box(
                                                Modifier.padding(horizontal = 6.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) { inner() }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Text("پیش‌نمایش", style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    InlineFigureView(
                        spec = FigureSpec.buildTable(style, title.trim(), cells),
                        modifier = Modifier.fillMaxWidth(),
                        contentDescription = "پیش‌نمایش جدول"
                    )
                }
                OutlinedButton(
                    onClick = {
                        val (r, c) = TableSvgRenderer.defaultSize(style)
                        cells = TableSvgRenderer.sampleCells(style, r, c)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("پرکردن با نمونهٔ این سبک") }
            }
        }
    )
}

@Composable
private fun SizeStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("$label:", style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = { if (value > min) onChange(value - 1) }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Outlined.Remove, contentDescription = "کاهش $label", modifier = Modifier.size(18.dp))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { if (value < max) onChange(value + 1) }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = "افزایش $label", modifier = Modifier.size(18.dp))
        }
    }
}
