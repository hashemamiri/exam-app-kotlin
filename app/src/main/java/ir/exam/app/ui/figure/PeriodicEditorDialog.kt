package ir.exam.app.ui.figure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.PeriodicElements
import ir.exam.app.core.figure.PeriodicSvgRenderer

/**
 * V53.2 — ویرایشگر کاملاً Native «درج جدول تناوبی» با همان رفتار مرجع:
 * چهار حالت (کامل/گروه اصلی/بدون f/بدون عدد اتمی)، دو حالت لمس (حذف عنصر /
 * حذف عدد اتمی)، لمس سرستون گروه و شماره دوره برای حذف ستون/سطر، chipهای
 * بازگردانی تک‌مورد و «بازگردانی همه». خروجی همان `{k:'p',...}` مرجع است.
 */
@Composable
fun PeriodicEditorDialog(
    initialSpec: FigureSpec? = null,
    onDismiss: () -> Unit,
    onInsert: (FigureSpec) -> Unit
) {
    var preset by remember { mutableStateOf(initialSpec?.type ?: "full") }
    var title by remember { mutableStateOf(initialSpec?.xStr("title") ?: "جدول تناوبی") }
    var showZ by remember { mutableStateOf((initialSpec?.xStr("Z", "1") ?: "1") != "0") }
    var hideF by remember { mutableStateOf((initialSpec?.xStr("hideF", "0") ?: "0") == "1") }
    var hiddenElements by remember { mutableStateOf(initialSpec?.xIntList("hid")?.toSet() ?: emptySet()) }
    var hiddenZ by remember { mutableStateOf(initialSpec?.xIntList("hidZ")?.toSet() ?: emptySet()) }
    var hiddenGroups by remember { mutableStateOf(initialSpec?.xIntList("hideCols")?.toSet() ?: emptySet()) }
    var hiddenPeriods by remember { mutableStateOf(initialSpec?.xIntList("hideRows")?.toSet() ?: emptySet()) }
    // حالت لمس: false=حذف عنصر ، true=حذف عدد اتمی — همان دو mode مرجع.
    var zMode by remember { mutableStateOf(false) }
    val isEdit = initialSpec != null

    fun currentSpec(): FigureSpec = FigureSpec.buildPeriodic(
        preset = preset,
        title = title.trim(),
        showZ = showZ,
        hideF = hideF,
        hiddenElements = hiddenElements.toList(),
        hiddenZ = hiddenZ.toList(),
        hiddenGroups = hiddenGroups.toList(),
        hiddenPeriods = hiddenPeriods.toList()
    )

    fun applyPreset(id: String) {
        preset = id
        when (id) {
            "full" -> {
                hiddenGroups = emptySet(); hiddenPeriods = emptySet()
                hiddenElements = emptySet(); hiddenZ = emptySet()
                hideF = false; showZ = true
            }
            "main" -> { hiddenGroups = (3..12).toSet(); hideF = true; hiddenPeriods = emptySet() }
            "noF" -> hideF = true
            "noZ" -> showZ = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "ویرایش جدول تناوبی" else "درج جدول تناوبی") },
        confirmButton = {
            TextButton(onClick = { onInsert(currentSpec()) }) {
                Text(if (isEdit) "اعمال تغییرات" else "درج در سؤال")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 580.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("حالت جدول", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "full" to "کامل",
                        "main" to "گروه اصلی",
                        "noF" to "بدون f",
                        "noZ" to "بدون عدد اتمی"
                    ).forEach { (id, name) ->
                        FilterChip(
                            selected = preset == id,
                            onClick = { applyPreset(id) },
                            label = { Text(name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !zMode, onClick = { zMode = false }, label = { Text("حذف عنصر") })
                    FilterChip(selected = zMode, onClick = { zMode = true }, label = { Text("حذف عدد اتمی") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = showZ, onClick = { showZ = !showZ }, label = { Text("نمایش عدد اتمی") })
                    FilterChip(selected = !hideF, onClick = { hideF = !hideF }, label = { Text("لانتانید و اکتینید") })
                }
                Text(
                    "روی عنصر بزنید تا حذف/بازگردانده شود؛ روی شمارهٔ گروه (۱…۱۸) یا دوره بزنید تا ستون/سطر حذف شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // شبکهٔ لمسی تعاملی
                PeriodicTouchGrid(
                    hiddenElements = hiddenElements,
                    hiddenZ = hiddenZ,
                    hiddenGroups = hiddenGroups,
                    hiddenPeriods = hiddenPeriods,
                    hideF = hideF,
                    showZ = showZ,
                    onElementTap = { z ->
                        if (zMode) hiddenZ = toggle(hiddenZ, z) else hiddenElements = toggle(hiddenElements, z)
                    },
                    onGroupTap = { g -> hiddenGroups = toggle(hiddenGroups, g) },
                    onPeriodTap = { p -> hiddenPeriods = toggle(hiddenPeriods, p) }
                )
                // chipهای بازگردانی — همان ptHidden مرجع.
                val chips = buildList {
                    hiddenGroups.sorted().forEach { add(Triple("گروه ${PeriodicSvgRenderer.faNum(it)} ×", "g", it)) }
                    hiddenPeriods.sorted().forEach { add(Triple("دوره ${PeriodicSvgRenderer.faNum(it)} ×", "p", it)) }
                    hiddenElements.sorted().forEach { z ->
                        add(Triple("${PeriodicElements.byZ[z]?.symbol ?: z} ×", "e", z))
                    }
                    hiddenZ.sorted().forEach { z ->
                        add(Triple("Z ${PeriodicElements.byZ[z]?.symbol ?: z} ×", "z", z))
                    }
                }
                if (chips.isEmpty()) {
                    Text(
                        "چیزی حذف نشده است.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        chips.forEach { (label, kind, value) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                modifier = Modifier.clickable {
                                    when (kind) {
                                        "g" -> hiddenGroups = toggle(hiddenGroups, value)
                                        "p" -> hiddenPeriods = toggle(hiddenPeriods, value)
                                        "e" -> hiddenElements = toggle(hiddenElements, value)
                                        "z" -> hiddenZ = toggle(hiddenZ, value)
                                    }
                                }
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { applyPreset("full") }, modifier = Modifier.fillMaxWidth()) {
                    Text("بازگردانی همه")
                }
            }
        }
    )
}

private fun toggle(set: Set<Int>, value: Int): Set<Int> =
    if (value in set) set - value else set + value

/** شبکهٔ لمسی ۱۸×۷ + بلوک f؛ هر خانه یک Box رنگی با نماد عنصر است. */
@Composable
private fun PeriodicTouchGrid(
    hiddenElements: Set<Int>,
    hiddenZ: Set<Int>,
    hiddenGroups: Set<Int>,
    hiddenPeriods: Set<Int>,
    hideF: Boolean,
    showZ: Boolean,
    onElementTap: (Int) -> Unit,
    onGroupTap: (Int) -> Unit,
    onPeriodTap: (Int) -> Unit
) {
    val groups = (1..18).filter { it !in hiddenGroups }
    val periods = (1..7).filter { it !in hiddenPeriods }
    Column(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // سرستون گروه‌ها
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HeaderCell("")
            groups.forEach { g -> HeaderCell(PeriodicSvgRenderer.faNum(g)) { onGroupTap(g) } }
        }
        periods.forEach { p ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                HeaderCell(PeriodicSvgRenderer.faNum(p)) { onPeriodTap(p) }
                groups.forEach { g ->
                    val fSlot = g == 3 && (p == 6 || p == 7)
                    if (fSlot) {
                        HeaderCell(if (hideF) "" else if (p == 6) "*" else "**")
                    } else {
                        val el = PeriodicElements.at(g, p)
                        if (el == null) HeaderCell("") else ElementCell(el, showZ, el.z in hiddenElements, el.z in hiddenZ) {
                            onElementTap(el.z)
                        }
                    }
                }
            }
        }
        if (!hideF) {
            listOf(8, 9).forEach { p ->
                Row(
                    Modifier.padding(top = if (p == 8) 6.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    HeaderCell(if (p == 8) "*" else "**")
                    (3..17).forEach { g ->
                        val el = PeriodicElements.at(g, p)
                        if (el == null) HeaderCell("") else ElementCell(el, showZ, el.z in hiddenElements, el.z in hiddenZ) {
                            onElementTap(el.z)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(label: String, onTap: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(26.dp)
            .let { if (onTap != null) it.clickable(onClick = onTap) else it },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ElementCell(
    element: PeriodicElements.Element,
    showZ: Boolean,
    hidden: Boolean,
    zHidden: Boolean,
    onTap: () -> Unit
) {
    val categoryColor = PeriodicElements.CATEGORY_COLORS[element.category] ?: "#e2e8f0"
    val fill = if (hidden) Color(0xFFF1F3F7) else Color(android.graphics.Color.parseColor(categoryColor))
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = fill,
        border = BorderStroke(0.5.dp, Color(0xFFB8BFCC)),
        modifier = Modifier.size(26.dp).clickable(onClick = onTap)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!hidden) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showZ && !zHidden) {
                        Text(
                            PeriodicSvgRenderer.faNum(element.z),
                            fontSize = 5.sp,
                            lineHeight = 6.sp,
                            color = Color(0xFF5B6478)
                        )
                    }
                    Text(
                        element.symbol,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        color = Color(0xFF263142)
                    )
                }
            }
        }
    }
}
