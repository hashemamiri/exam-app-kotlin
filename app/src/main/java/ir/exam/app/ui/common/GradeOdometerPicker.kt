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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

private fun schoolGradeRank(value: String): Int {
    val normalized = value.trim().removePrefix("پایه").trim()
    return StandardSchoolGrades.indexOf(normalized).takeIf { it >= 0 } ?: Int.MAX_VALUE
}

private const val EmptyGradeValue = ""
private val OdometerHeight = 72.dp
private val OdometerItemHeight = 36.dp

@OptIn(ExperimentalFoundationApi::class)
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
    val targetIndex = values.indexOf(value.trim()).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val latestValue by rememberUpdatedState(value.trim())
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    val centeredIndex by remember(listState, values) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2 - center)
            }?.index?.coerceIn(values.indices) ?: targetIndex
        }
    }

    LaunchedEffect(targetIndex, values) {
        if (!listState.isScrollInProgress && centeredIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { scrolling -> !scrolling }
            .collect {
                val layout = listState.layoutInfo
                val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                val index = layout.visibleItemsInfo.minByOrNull { item ->
                    abs(item.offset + item.size / 2 - center)
                }?.index?.coerceIn(values.indices) ?: return@collect
                val selected = values[index]
                if (selected != latestValue) latestOnValueChange(selected)
            }
    }

    val shownValue = values.getOrNull(centeredIndex).orEmpty()
    val shownLabel = shownValue.ifBlank { emptyLabel }
    Surface(
        modifier = modifier
            .height(OdometerHeight)
            .semantics {
                contentDescription = "$label؛ برای انتخاب به بالا یا پایین پیمایش کنید"
                stateDescription = shownLabel
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f)),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OdometerItemHeight)
                    .padding(horizontal = 3.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .055f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .34f))
            ) {}
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = (OdometerHeight - OdometerItemHeight) / 2),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(values, key = { _, item -> item.ifEmpty { EmptyGradeValue } }) { index, item ->
                    val centered = index == centeredIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(OdometerItemHeight)
                            .clickable {
                                latestOnValueChange(item)
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                            .padding(horizontal = 8.dp)
                            .alpha(if (centered) 1f else .38f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(if (centered) 19.dp else 15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (centered) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = item.ifBlank { emptyLabel },
                                style = if (centered) MaterialTheme.typography.labelLarge
                                else MaterialTheme.typography.labelMedium,
                                fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (centered) {
                            Icon(
                                imageVector = Icons.Outlined.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
