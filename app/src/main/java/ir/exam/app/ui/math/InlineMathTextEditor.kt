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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.math.FormulaTextCodec

/**
 * ویرایشگر متن سؤال با فرمول‌های درون‌متنی (شبیه وب‌اپ).
 *
 * هر `$...$` داخل متن به‌صورت نماد رندر می‌شود — نه کد — و با لمس همان نماد
 * ویرایشگر فرمول باز می‌شود؛ بنابراین برای نمایش فرمول نیازی به کادر یا
 * فهرست جداگانه نیست. متن عادی نیز به‌صورت درون‌متنی قابل تایپ است.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineMathTextEditor(
    source: String,
    onSourceChange: (String) -> Unit,
    onEditFormula: (occurrenceIndex: Int, tex: String) -> Unit,
    onInsertFormula: () -> Unit,
    onDeleteFormula: (occurrenceIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "متن سؤال",
    placeholder: String = "متن سؤال را بنویسید…"
) {
    val parts = splitParts(source)
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
                        is Part.Plain -> {
                            BasicTextField(
                                value = part.text,
                                onValueChange = { newText ->
                                    onSourceChange(rebuild(parts, index, newText))
                                },
                                modifier = Modifier
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
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
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
                        is Part.Formula -> FormulaChip(
                            tex = part.tex,
                            onEdit = { onEditFormula(part.index, part.tex) },
                            onDelete = { onDeleteFormula(part.index) }
                        )
                    }
                }
                IconButton(onClick = onInsertFormula, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.Functions,
                        contentDescription = "درج فرمول در متن سؤال",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private sealed interface Part {
    data class Plain(val text: String) : Part
    data class Formula(val index: Int, val tex: String) : Part
}

/**
 * متن را به بخش‌های متناوب متن/فرمول می‌شکند؛ همیشه با Plain شروع و پایان
 * می‌یابد تا بتوان قبل و بعد از هر فرمول هم تایپ کرد.
 */
private fun splitParts(source: String): List<Part> {
    val occurrences = FormulaTextCodec.occurrences(source)
    val result = mutableListOf<Part>()
    var cursor = 0
    for (occurrence in occurrences) {
        result += Part.Plain(source.substring(cursor, occurrence.start))
        result += Part.Formula(occurrence.index, occurrence.tex)
        cursor = occurrence.endExclusive
    }
    result += Part.Plain(source.substring(cursor))
    return result
}

/** بازسازی متن کامل با جایگزینی متن بخش ویرایش‌شده و حفظ فرمول‌ها. */
private fun rebuild(parts: List<Part>, editedIndex: Int, newText: String): String {
    // حذف `$` از تایپ آزاد تا ساختار `$...$` فقط از مسیر ویرایشگر فرمول ساخته شود.
    val clean = newText.replace("$", "")
    val builder = StringBuilder()
    parts.forEachIndexed { index, part ->
        when (part) {
            is Part.Plain -> builder.append(if (index == editedIndex) clean else part.text)
            is Part.Formula -> builder.append('$').append(part.tex).append('$')
        }
    }
    return builder.toString()
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
