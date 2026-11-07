package ir.exam.app.ui.math

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.text.RichSegment
import ir.exam.app.core.text.RichTextSplitter
import ir.exam.app.ui.figure.AtlasFigureView
import ir.exam.app.ui.code.CodeBlockView
import ir.exam.app.ui.figure.InlineFigureView

/**
 * ویرایشگر متن سؤال با فرمول‌ها و شکل‌های درون‌متنی (شبیه وب‌اپ).
 *
 * هر `$...$` به‌صورت نماد و هر `%%FIG:...%%` به‌صورت شکل رندر می‌شود — نه کد —
 * و با لمس همان نماد/شکل ویرایشگر مربوطه باز می‌شود؛ بنابراین برای نمایش
 * فرمول یا شکل نیازی به کادر یا فهرست جداگانه نیست. متن عادی نیز به‌صورت
 * درون‌متنی قابل تایپ است.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineMathTextEditor(
    source: String,
    onSourceChange: (String) -> Unit,
    onEditFormula: (occurrenceIndex: Int, tex: String) -> Unit,
    onInsertFormula: () -> Unit,
    onDeleteFormula: (occurrenceIndex: Int) -> Unit,
    onInsertFigure: () -> Unit = {},
    onInsertGraph: () -> Unit = {},
    onEditFigure: (occurrenceIndex: Int, spec: FigureSpec) -> Unit = { _, _ -> },
    onDeleteFigure: (occurrenceIndex: Int) -> Unit = {},
    // V350 — کد هم مثل شکل/فرمول در همین کادر (سؤال و گزینه‌ها) درج/ویرایش/حذف می‌شود.
    onInsertCode: () -> Unit = {},
    onEditCode: (occurrenceIndex: Int, lang: String, code: String) -> Unit = { _, _, _ -> },
    onDeleteCode: (occurrenceIndex: Int) -> Unit = {},
    modifier: Modifier = Modifier,
    label: String = "متن سؤال",
    placeholder: String = "متن سؤال را بنویسید…",
    // V259 — برای کادر گزینه‌ها: بدون نوار ابزار داخلی (دکمهٔ + گزینه ابزارها را باز می‌کند).
    showToolbar: Boolean = true
) {
    val parts = RichTextSplitter.split(source)
    val showPlaceholder = source.isBlank()
    Column(modifier) {
        if (label.isNotBlank()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                parts.forEachIndexed { index, part ->
                    when (part) {
                        is RichSegment.Text -> {
                            BasicTextField(
                                value = part.text,
                                onValueChange = { newText ->
                                    onSourceChange(RichTextSplitter.reconstruct(parts, index, newText))
                                },
                                modifier = (if (showPlaceholder && part.text.isEmpty()) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier
                                })
                                    .widthIn(min = 48.dp, max = 460.dp)
                                    .heightIn(min = 40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Content,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (part.text.isEmpty() && showPlaceholder) {
                                            Text(
                                                placeholder,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                        is RichSegment.Math -> FormulaChip(
                            tex = part.tex,
                            onEdit = { onEditFormula(part.index, part.tex) },
                            onDelete = { onDeleteFormula(part.index) }
                        )
                        is RichSegment.Figure -> FigureChip(
                            spec = part.spec,
                            onEdit = { onEditFigure(part.index, part.spec) },
                            onDelete = { onDeleteFigure(part.index) }
                        )
                        is RichSegment.Code -> CodeChip(
                            lang = part.lang,
                            code = part.code,
                            onEdit = { onEditCode(part.index, part.lang, part.code) },
                            onDelete = { onDeleteCode(part.index) }
                        )
                    }
                }
            }
        }
        // آیکن‌های درج فقط زیر کادر متن سؤال هستند؛ داخل کادر چیزی نیست.
        if (showToolbar) Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(Icons.Outlined.Functions, "درج فرمول", onInsertFormula)
            ToolbarButton(Icons.Outlined.Category, "درج شکل", onInsertFigure)
            ToolbarButton(Icons.Outlined.Insights, "درج نمودار", onInsertGraph)
            ToolbarCodeButton(onInsertCode)
        }
    }
}

/** V350 — دکمهٔ «</>» برای درج کد؛ همان نماد سایت. */
@Composable
private fun ToolbarCodeButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Text(
            "</>",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FormulaChip(
    tex: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(84.dp)
                    .height(36.dp)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                NativeFormulaIcon(
                    tex,
                    Modifier.fillMaxSize(),
                    18.sp,
                    contentDescription = "فرمول؛ لمس برای ویرایش"
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "حذف فرمول",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FigureChip(
    spec: FigureSpec,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(120.dp)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                // V259 — آناتومی/فیزیک/شیمی/تصویر با نمای اطلس (بدون جای خالی‌های نام‌گذاری؛ معلم است)، بقیه با رندر SVG.
                if (spec.kind in setOf("a", "s")) AtlasFigureView(
                    spec = spec,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentDescription = "شکل؛ لمس برای ویرایش",
                    showBlanks = false,
                    onImageTap = onEdit
                ) else InlineFigureView(
                    spec,
                    Modifier.fillMaxSize().padding(4.dp),
                    contentDescription = "شکل؛ لمس برای ویرایش"
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "حذف شکل",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * V350 — تراشهٔ کد داخل متن سؤال و گزینه‌ها، با همان رفتار بقیهٔ ابزارها:
 * لمس اول = انتخاب و پیدا شدن ضربدر حذف؛ لمس دوم = باز شدن محیط کدنویسی.
 */
@Composable
private fun CodeChip(
    lang: String,
    code: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var selected by remember(code, lang) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .clickable {
                        if (selected) { selected = false; onEdit() } else { selected = true }
                    }
                    .padding(4.dp)
            ) {
                // نوار خودِ CodeBlockView نام زبان و شمار خط را نشان می‌دهد.
                CodeBlockView(
                    code = code,
                    langId = lang,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 96.dp)
                )
                if (selected) {
                    Text(
                        "برای ویرایش دوباره لمس کنید",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "حذف کد",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
