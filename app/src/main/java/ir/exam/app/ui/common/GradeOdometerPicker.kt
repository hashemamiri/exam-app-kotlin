package ir.exam.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

val StandardSchoolGrades: List<String> = listOf(
    "پیش‌دبستانی",
    "اول",
    "دوم",
    "سوم",
    "چهارم",
    "پنجم",
    "ششم",
    "هفتم",
    "هشتم",
    "نهم",
    "دهم",
    "یازدهم",
    "دوازدهم"
)

fun gradeOdometerValues(
    current: String,
    availableGrades: List<String> = emptyList(),
    includeStandardGrades: Boolean = true
): List<String> = buildList {
    add("")
    val cleanCurrent = current.trim()
    val cleanAvailable = availableGrades.map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .sortedWith(compareBy<String>({ schoolGradeRank(it) }, { it }))
    val source = buildList {
        if (includeStandardGrades) addAll(StandardSchoolGrades)
        addAll(cleanAvailable)
        if (cleanCurrent.isNotEmpty()) add(cleanCurrent)
    }
    source.distinct().forEach(::add)
}

private const val OtherGradeValue = "__other_grade__"

private fun schoolGradeRank(value: String): Int {
    val normalized = value.trim().removePrefix("پایه").trim()
    return StandardSchoolGrades.indexOf(normalized).takeIf { it >= 0 } ?: Int.MAX_VALUE
}

/**
 * انتخاب پایه با ظاهر جمع‌وجور. لمس کنترل، چرخ انتخاب عمودی و Snapدار را باز می‌کند؛
 * بنابراین در فرم‌ها و ردیف فیلتر فضای ثابت و تمیز باقی می‌ماند.
 */
@Composable
fun GradeOdometerPicker(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    availableGrades: List<String> = emptyList(),
    includeStandardGrades: Boolean = true,
    emptyLabel: String = "بدون پایه",
    label: String = "پایه"
) {
    val values = remember(value, availableGrades, includeStandardGrades) {
        gradeOdometerValues(value, availableGrades, includeStandardGrades)
    }
    val wheelValues = remember(values) { values + OtherGradeValue }
    var open by remember { mutableStateOf(false) }
    var customMode by remember { mutableStateOf(false) }
    val shown = value.trim().ifBlank { emptyLabel }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .semantics {
                    contentDescription = "$label: $shown؛ لمس برای بازکردن انتخاب‌گر"
                    stateDescription = shown
                }
                .clickable { open = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .58f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        shown,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Outlined.UnfoldMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (customMode) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("سایر پایه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (open) {
        GradeWheelDialog(
            values = wheelValues,
            initialValue = if (customMode) OtherGradeValue else value.trim(),
            emptyLabel = emptyLabel,
            label = label,
            onDismiss = { open = false },
            onConfirm = { selected ->
                if (selected == OtherGradeValue) {
                    customMode = true
                } else {
                    customMode = false
                    onValueChange(selected)
                }
                open = false
            }
        )
    }
}

private val WheelItemHeight = 46.dp
private val WheelHeight = 230.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GradeWheelDialog(
    values: List<String>,
    initialValue: String,
    emptyLabel: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialIndex = values.indexOf(initialValue).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    var pending by remember(values, initialValue) { mutableStateOf(values[initialIndex]) }
    val centeredIndex by remember(listState, values) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2 - center)
            }?.index?.coerceIn(values.indices) ?: initialIndex
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { scrolling -> !scrolling }
            .collect { pending = values.getOrElse(centeredIndex) { pending } }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("انتخاب $label", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("برای تغییر، فهرست را به بالا یا پایین بکشید.", style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier.fillMaxWidth().height(WheelHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(WheelItemHeight),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .48f))
                    ) {}
                    LazyColumn(
                        state = listState,
                        flingBehavior = flingBehavior,
                        contentPadding = PaddingValues(vertical = (WheelHeight - WheelItemHeight) / 2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(values, key = { index, item -> "$index-$item" }) { index, item ->
                            val centered = index == centeredIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(WheelItemHeight)
                                    .clickable {
                                        pending = item
                                        scope.launch { listState.animateScrollToItem(index) }
                                    }
                                    .alpha(if (centered) 1f else .38f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (item) {
                                        OtherGradeValue -> "سایر"
                                        "" -> emptyLabel
                                        else -> item
                                    },
                                    textAlign = TextAlign.Center,
                                    style = if (centered) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                                    color = if (centered) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFD63B49)) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "انصراف", tint = Color.White)
                        }
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF19945B)) {
                        IconButton(onClick = { onConfirm(pending) }) {
                            Icon(Icons.Outlined.Check, contentDescription = "تأیید پایه", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
