package ir.exam.app.ui.figure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureTemplate
import ir.exam.app.core.figure.faEnDigits
import ir.exam.app.core.ui.LocalTabletLayout
import ir.exam.app.core.figure.GEOMETRY_FIGURES
import ir.exam.app.core.figure.GRAPH_FIGURES
import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.PRIMARY_MATH_FIRST_ID
import ir.exam.app.core.figure.MATH_CATEGORIES
import ir.exam.app.core.figure.MathCategory
import ir.exam.app.core.figure.mathTemplatesOf
import ir.exam.app.ui.figure.FigureKind.GEOMETRY
import ir.exam.app.ui.figure.FigureKind.GRAPH
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class FigureKind { GEOMETRY, GRAPH, AXIS }

/**
 * مرحلهٔ اول درج: فقط نوع‌های متعلق به همان آیکن را نشان می‌دهد.
 * آیکن «درج شکل» هرگز فهرست نمودار را باز نمی‌کند و برعکس.
 */
@Composable
fun FigureTypePickerDialog(
    kind: FigureKind,
    onDismiss: () -> Unit,
    onTypeSelected: (FigureSpec) -> Unit,
    // V310 — «ابزار ریاضی ابتدایی» آیکن جدا دارد: فقط همان بخش از فهرست محورها نشان داده می‌شود.
    mathOnly: Boolean = false
) {
    val geometry = kind == GEOMETRY
    val axis = kind == FigureKind.AXIS
    val math = axis && mathOnly
    // V320 — پنجرهٔ پیشین «🧮 ابزار ریاضی ابتدایی» اکنون دو مرحله‌ای است: اول دسته، بعد ابزار.
    var mathCategory by remember { mutableStateOf<MathCategory?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { // V247 — زیر نوار وضعیت نرود
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (geometry) "📐 درج شکل"
                        else if (math && mathCategory == null) "🧮 ابزار ریاضی — انتخاب دسته"
                        else if (math) "🧮 " + mathCategory!!.title
                        else if (axis) "📏 درج محور" else "📈 درج نمودار",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Text(
                    if (geometry) "ابتدا نوع شکل هندسی را انتخاب کنید."
                    else if (math && mathCategory == null) "دستهٔ آموزشی را انتخاب کنید؛ سپس ابزارهای همان دسته باز می‌شود."
                    else if (math) (mathCategory!!.hint + " — " + mathCategory!!.count + " ابزار")
                    else if (axis) "نوع محور را انتخاب کنید."
                    else "ابتدا نوع نمودار را انتخاب کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                // V56.2 — تبلت: ستون‌های بیشتر در پنجره‌های انتخاب.
                val tabletPicker = LocalTabletLayout.current
                if (geometry) {
                    LazyVerticalGrid(
                        columns = if (tabletPicker) GridCells.Adaptive(140.dp) else GridCells.Adaptive(104.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(GEOMETRY_FIGURES, key = { it.id }) { template ->
                            GeometryTypeCell(template) { onTypeSelected(template.toSpec()) }
                        }
                    }
                } else if (math) {
                    // V320 — مرحلهٔ اول: چهار دستهٔ آموزشی؛ مرحلهٔ دوم: ابزارهای همان دسته.
                    val current = mathCategory
                    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (current != null) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { mathCategory = null }) { Text("↩ دسته‌بندی‌ها") }
                                Text(
                                    "  ${current.emoji} ${current.title} — ${current.count} ابزار",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        LazyVerticalGrid(
                            columns = if (tabletPicker) GridCells.Fixed(3) else GridCells.Fixed(2),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (current == null) {
                                items(MATH_CATEGORIES, key = { it.id }) { category ->
                                    MathCategoryCell(category, tabletPicker) { mathCategory = category }
                                }
                            } else {
                                items(mathTemplatesOf(current), key = { it.id }) { template ->
                                    GraphTypeCell(template) { onTypeSelected(template.toSpec()) }
                                }
                            }
                        }
                    }
                } else {
                    // V55.12 — درخواست کاربر: نمودارها در پنجرهٔ انتخاب «هر سطر ۲ تا»؛
                    // V56.2 — در تبلت ۳ تا.
                    LazyVerticalGrid(
                        columns = if (tabletPicker) GridCells.Fixed(3) else GridCells.Fixed(2),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // V136 — محورها همان سلول نمودار را دارند.
                        // V304 — ابزار ریاضی ابتدایی در انتهای فهرست AXIS_FIGURES است.
                        // V310 — آیکن جدا: پنجرهٔ «محور» فقط محورهای پایه را نشان می‌دهد و
                        //        پنجرهٔ «ابزار ریاضی» (mathOnly) فقط بخش ابتدایی را.
                        val all = if (axis) AXIS_FIGURES else GRAPH_FIGURES
                        val split = if (axis) AXIS_FIGURES.indexOfFirst { it.id == PRIMARY_MATH_FIRST_ID } else -1
                        val shown = when {
                            split <= 0 -> all
                            math -> all.subList(split, all.size)
                            else -> all.subList(0, split)
                        }
                        items(shown, key = { it.id }) { template ->
                            GraphTypeCell(template) { onTypeSelected(template.toSpec()) }
                        }
                    }
                }
            }
        }
    }
}

/** V320 — کارت دستهٔ آموزشی در مرحلهٔ اول پنجرهٔ «ابزار ریاضی». */
@Composable
private fun MathCategoryCell(category: MathCategory, wide: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            Modifier.fillMaxWidth().padding(if (wide) 12.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(category.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(category.title, style = MaterialTheme.typography.titleMedium)
            Text("${category.count} ابزار", style = MaterialTheme.typography.labelLarge)
            Text(
                category.hint,
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun GeometryTypeCell(template: FigureTemplate, onClick: () -> Unit) {
    Card(Modifier.clickable(onClick = onClick)) {
        Column(
            Modifier.fillMaxWidth().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InlineFigureView(
                template.toSpec(),
                Modifier.fillMaxWidth().height(74.dp),
                contentDescription = template.label
            )
            Text(template.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GraphTypeCell(template: FigureTemplate, onClick: () -> Unit) {
    // V55.12 — سلول فشردهٔ شبکهٔ ۲ستونه؛ جملهٔ راهنمای اضافی زیر نام حذف شد.
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            Modifier.fillMaxWidth().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            InlineFigureView(
                template.toSpec(),
                Modifier.fillMaxWidth().height(96.dp),
                contentDescription = template.label
            )
            Text(template.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * مرحلهٔ دوم درج/ویرایش. این پنجره فقط ویرایش همان نوع انتخاب‌شده را انجام می‌دهد؛
 * انتخاب نوع قبلاً در FigureTypePickerDialog انجام شده است.
 */
@Composable
fun FigurePickerDialog(
    initialSpec: FigureSpec? = null,
    initialKind: FigureKind = GEOMETRY,
    onDismiss: () -> Unit,
    onInsert: (FigureSpec) -> Unit
) {
    val resolvedKind = when {
        initialSpec?.type?.let { type -> AXIS_FIGURES.any { it.id == type } } == true -> FigureKind.AXIS
        initialSpec?.type?.let { type -> GRAPH_FIGURES.any { it.id == type } } == true -> GRAPH
        else -> initialKind
    }
    val baseSpec = initialSpec ?: when (resolvedKind) {
        GEOMETRY -> GEOMETRY_FIGURES.first().toSpec()
        FigureKind.AXIS -> AXIS_FIGURES.first().toSpec()
        else -> GRAPH_FIGURES.first().toSpec()
    }
    val typeLabel = templateLabel(resolvedKind, baseSpec.type)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { // V247 — زیر نوار وضعیت نرود
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (resolvedKind == GEOMETRY) "✏️ ویرایش شکل" else if (resolvedKind == FigureKind.AXIS) "✏️ ویرایش محور" else "✏️ ویرایش نمودار",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Text("نوع انتخاب‌شده: $typeLabel", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
                Box(Modifier.weight(1f)) {
                    if (resolvedKind == GEOMETRY) {
                        GeometryEditorPane(baseSpec, onInsert)
                    } else {
                        GraphEditorPane(baseSpec, onInsert)
                    }
                }
            }
        }
    }
}

private data class GeometryField(
    val group: String,
    val key: String,
    val label: String
) {
    val id: String get() = "$group:$key"
}

private fun v(key: String): GeometryField = GeometryField("V", key, "رأس $key")
private fun s(key: String): GeometryField = GeometryField("S", key, "ضلع $key")
private fun a(key: String): GeometryField = GeometryField("A", key, "زاویه $key")
private fun x(key: String, label: String): GeometryField = GeometryField("X", key, label)

private fun geometryFields(type: String): List<GeometryField> = when (type) {
    "tri", "iso", "eq", "scal", "acut", "obt" ->
        listOf(v("A"), v("B"), v("C"), s("a"), s("b"), s("c"), a("A"), a("B"), a("C"))
    "rtri" ->
        listOf(v("A"), v("B"), v("C"), s("a"), s("b"), s("c"), a("A"), a("B"))
    "sq" ->
        listOf(v("A"), v("B"), v("C"), v("D"), s("s"))
    "rect" ->
        listOf(v("A"), v("B"), v("C"), v("D"), s("a"), s("b"))
    "para", "rhomb" ->
        listOf(v("A"), v("B"), v("C"), v("D"), s("a"), s("b"), a("A"))
    "trap", "itrap", "rtrap" ->
        listOf(v("A"), v("B"), v("C"), v("D"), s("a"), s("b"), s("c"), s("d"), x("h", "ارتفاع"))
    "kite" ->
        listOf(v("A"), v("B"), v("C"), v("D"))
    "pent" ->
        listOf(v("A"), v("B"), v("C"), v("D"), v("E"), s("a"))
    "hex" ->
        listOf(v("A"), v("B"), v("C"), v("D"), v("E"), v("F"), s("a"))
    "oct" ->
        listOf(v("A"), v("B"), v("C"), v("D"), v("E"), v("F"), v("G"), v("H"), s("a"))
    "circ", "semi", "hemi", "sph" -> listOf(v("O"), x("r", "شعاع r"))
    "sec" -> listOf(v("O"), x("r", "شعاع r"), x("m", "زاویهٔ مرکزی"))
    "arc" -> listOf(v("O"), v("A"), v("B"), x("m", "اندازهٔ کمان"))
    "segm" -> listOf(v("A"), v("B"), x("r", "شعاع"), x("m", "زاویهٔ کمان"))
    "ngon" -> listOf(x("n", "تعداد اضلاع n"), s("a"))
    "hept", "deca" -> listOf(s("a"))
    "ring" -> listOf(v("O"), x("R", "شعاع بیرونی R"), x("r", "شعاع داخلی r"))
    "torus" -> listOf(x("R", "شعاع بزرگ R"), x("r", "شعاع لوله r"))
    "frust" -> listOf(x("R", "شعاع قاعده بزرگ R"), x("r", "شعاع قاعده کوچک r"), x("h", "ارتفاع h"))
    "ell" -> listOf(v("O"), x("a", "نیم‌محور بزرگ a"), x("b", "نیم‌محور کوچک b"))
    "elps" -> listOf(v("O"), x("a", "نیم‌قطر a"), x("b", "نیم‌قطر b"), x("c", "نیم‌قطر c"))
    "ang" -> listOf(v("O"), v("A"), v("B"), x("m", "اندازه زاویه"))
    "parll" -> listOf(v("d1"), v("d2"), x("n", "تعداد خطوط"), x("tilt", "زاویه خطوط"))
    "pseg", "ray", "ln" -> listOf(v("A"), v("B"), s("a"))
    "cube" -> listOf(s("s"))
    "box" -> listOf(s("a"), s("b"), s("h"))
    "cyl", "cone" -> listOf(x("h", "ارتفاع"), x("r", "شعاع"))
    "pyr" -> listOf(v("S"), v("A"), v("B"), s("a"), x("h", "ارتفاع"))
    "tet" -> listOf(v("S"), v("A"), v("B"), v("C"), x("h", "ارتفاع"))
    "hpris" -> listOf(x("h", "ارتفاع h"), s("a"))
    "octa", "dodec", "icosa" -> listOf(v("S"), v("A"), v("B"), s("a"))
    "star" -> listOf(v("A"), v("B"), v("C"), v("D"), v("E"), s("a"))
    "pris" -> listOf(v("A"), v("B"), v("C"), v("A2"), x("h", "ارتفاع"))
    else -> listOf(v("A"), v("B"), v("C"), s("a"))
}

private fun fieldValue(spec: FigureSpec, field: GeometryField): String = when (field.group) {
    "V" -> spec.vertex(field.key)
    "S" -> spec.side(field.key)
    "A" -> spec.angle(field.key)
    else -> spec.xStr(field.key)
}

private fun withGeometryFields(
    base: FigureSpec,
    fields: List<GeometryField>,
    values: Map<String, String>
): FigureSpec {
    val root = base.raw.toMutableMap()
    fields.groupBy(GeometryField::group).forEach { (group, groupFields) ->
        val nested = (root[group] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        groupFields.forEach { field ->
            val value = values[field.id].orEmpty().trim()
            if (value.isBlank()) nested.remove(field.key) else nested[field.key] = JsonPrimitive(value)
        }
        if (nested.isEmpty()) root.remove(group) else root[group] = JsonObject(nested)
    }
    return FigureSpec(JsonObject(root))
}

@Composable
private fun GeometryEditorPane(initialSpec: FigureSpec, onInsert: (FigureSpec) -> Unit) {
    val fields = remember(initialSpec.toJson()) { geometryFields(initialSpec.type) }
    var values by remember(initialSpec.toJson()) {
        mutableStateOf(fields.associate { it.id to fieldValue(initialSpec, it) })
    }
    val draft = withGeometryFields(initialSpec, fields, values)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                "برچسب‌ها و اندازه‌های شکل را ویرایش کنید.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(fields, key = GeometryField::id) { field ->
            val ddOpts = dropdownOptionsFor(initialSpec.type, field.id)
            if (ddOpts != null) {
                FigureShapeDropdown(
                    label = field.label,
                    value = values[field.id].orEmpty().ifBlank { ddOpts.first().first },
                    onSelect = { v -> values = values + (field.id to v) },
                    options = ddOpts
                )
            } else {
                OutlinedTextField(
                    value = values[field.id].orEmpty(),
                    onValueChange = { values = values + (field.id to it) },
                    label = { Text(field.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("پیش‌نمایش", style = MaterialTheme.typography.labelLarge)
                    AdjustableFigure(
                        draft,
                        Modifier.fillMaxWidth().height(220.dp),
                        contentDescription = "پیش‌نمایش شکل",
                        onChanged = { s ->
                            values = values + fields.associate { f -> f.id to fieldValue(s, f) }
                        }
                    )
                }
            }
        }
        item {
            Button(onClick = { onInsert(draft) }, modifier = Modifier.fillMaxWidth()) {
                Text("✅ درج شکل")
            }
        }
    }
}

@Composable
private fun GraphEditorPane(initialSpec: FigureSpec, onInsert: (FigureSpec) -> Unit) {
    // V136 — محورها هم از همین پنل (پارامتر + پیش‌نمایش) ویرایش می‌شوند.
    val allTemplates = GRAPH_FIGURES + AXIS_FIGURES
    val graphType = initialSpec.type.takeIf { type -> allTemplates.any { it.id == type } } ?: GRAPH_FIGURES.first().id
    val template = allTemplates.firstOrNull { it.id == graphType } ?: GRAPH_FIGURES.first()
    val isAxis = AXIS_FIGURES.any { it.id == graphType }
    var title by remember(initialSpec.toJson()) { mutableStateOf(initialSpec.xStr("title")) }
    var params by remember(initialSpec.toJson()) {
        mutableStateOf(initialParams(template, initialSpec))
    }
    val draft = buildGraphSpec(initialSpec, graphType, params, title)

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text((if (isAxis) "نوع محور: " else "نوع نمودار: ") + template.label, style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedTextField(
                title,
                { title = it },
                label = { Text(if (isAxis) "عنوان محور (اختیاری)" else "عنوان نمودار (اختیاری)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(paramKeys(graphType), key = { it }) { key ->
            val ddOpts = dropdownOptionsFor(graphType, key)
            if (ddOpts != null) {
                FigureShapeDropdown(
                    label = paramLabel(graphType, key),
                    value = params[key].orEmpty().ifBlank { ddOpts.first().first },
                    onSelect = { v -> params = params + (key to v) },
                    options = ddOpts
                )
            } else {
                OutlinedTextField(
                    value = params[key] ?: "",
                    onValueChange = { value -> params = params + (key to value) },
                    label = { Text(paramLabel(graphType, key)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("پیش‌نمایش", style = MaterialTheme.typography.labelLarge)
                    // V310.7 / V315.1 — همهٔ ابزارهای ابتدایی با لمس در پیش‌نمایش قابل تنظیم‌اند و مقادیر تازه به فرم برمی‌گردد
                    AdjustableFigure(
                        draft,
                        Modifier.fillMaxWidth().height(180.dp),
                        contentDescription = "پیش‌نمایش نمودار",
                        onChanged = { s ->
                            val xo = s.raw["X"] as? JsonObject
                            if (xo != null) {
                                params = params + paramKeys(graphType).mapNotNull { k ->
                                    (xo[k] as? JsonPrimitive)?.contentOrNull?.let { k to it }
                                }
                            }
                        }
                    )
                }
            }
        }
        item {
            Button(onClick = { onInsert(draft) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isAxis) "✅ درج محور" else "✅ درج نمودار")
            }
        }
    }
}

/**
 * V54.1 — فیلدهای هر نوع نمودار با برچسب فارسی مرجع (`fieldsFor` ماژول
 * graph-fig-js). کلیدهای متنی (labs/vals/xs/...) رشته می‌مانند و بقیه عددی.
 */
private fun paramFields(type: String): List<Pair<String, String>> = when (type) {
    "line" -> listOf("m" to "شیب m", "b" to "عرض از مبدأ b")
    "quad" -> listOf("a" to "a", "b" to "b", "c" to "c")
    "sine" -> listOf("A" to "دامنه A", "w" to "ω", "ph" to "فاز")
    "exp" -> listOf("a" to "ضریب a", "b" to "نرخ b")
    "scat" -> listOf("xs" to "مقدارهای x", "ys" to "مقدارهای y")
    "bub" -> listOf("xs" to "مقدارهای x", "ys" to "مقدارهای y", "zs" to "اندازه حباب‌ها")
    "gauge" -> listOf("val" to "مقدار عقربه", "vmin" to "حداقل", "vmax" to "حداکثر")
    "cmp", "hcmp" -> listOf(
        "labs" to "برچسب‌ها", "vals" to "سری ۱", "vals2" to "سری ۲",
        "s1" to "نام سری ۱", "s2" to "نام سری ۲"
    )
    "stack", "st100", "sarea" -> listOf(
        "labs" to "برچسب‌ها", "vals" to "سری ۱", "vals2" to "سری ۲", "vals3" to "سری ۳",
        "s1" to "نام سری ۱", "s2" to "نام سری ۲", "s3" to "نام سری ۳"
    )
    "combo" -> listOf(
        "labs" to "برچسب‌ها", "vals" to "ستون‌ها", "vals2" to "خط",
        "s1" to "نام ستون", "s2" to "نام خط"
    )
    // V54.2 — فیلدهای فارسی مرجع برای ۱۴ نوع مرحلهٔ دوم.
    "box" -> listOf(
        "labs" to "گروه‌ها", "mins" to "حداقل", "q1s" to "چارک ۱",
        "meds" to "میانه", "q3s" to "چارک ۳", "maxs" to "حداکثر"
    )
    "ohlc" -> listOf(
        "labs" to "دوره‌ها", "opens" to "باز", "highs" to "بیشینه",
        "lows" to "کمینه", "closes" to "بسته"
    )
    "fall" -> listOf("labs" to "برچسب‌ها", "vals" to "مقدارها (منفی مجاز)")
    "ctrl" -> listOf(
        "labs" to "نمونه‌ها", "vals" to "مقدارها",
        "mean" to "میانگین (خالی=خودکار)", "ucl" to "UCL (خالی=خودکار)", "lcl" to "LCL (خالی=خودکار)"
    )
    "venn" -> listOf(
        "n" to "تعداد مجموعه (۲ یا ۳)", "s1" to "مجموعه A", "s2" to "مجموعه B", "s3" to "مجموعه C",
        "ab" to "A∩B", "ac" to "A∩C", "bc" to "B∩C", "abc" to "A∩B∩C"
    )
    "tree" -> listOf("labs" to "بخش‌ها", "vals" to "مقدارها")
    "sun" -> listOf(
        "labs" to "حلقهٔ داخلی", "vals" to "مقدار داخلی",
        "labs2" to "حلقهٔ بیرونی", "vals2" to "مقدار بیرونی"
    )
    "pict" -> listOf("labs" to "برچسب‌ها", "vals" to "مقدارها", "unit" to "هر نماد برابر است با")
    "heat", "hmap" -> listOf("rows" to "ردیف‌ها", "cols" to "ستون‌ها", "vals" to "مقدارها سطری")
    "bull" -> listOf("labs" to "شاخص‌ها", "vals" to "مقدار واقعی", "vals2" to "هدف")
    "pyra" -> listOf(
        "labs" to "گروه‌های سنی", "vals" to "سمت چپ", "vals2" to "سمت راست",
        "s1" to "نام چپ", "s2" to "نام راست"
    )
    "mekko" -> listOf(
        "labs" to "دسته‌ها (پهنای ستون)", "vals" to "سری ۱ + پهنا", "vals2" to "سری ۲", "vals3" to "سری ۳",
        "s1" to "نام سری ۱", "s2" to "نام سری ۲", "s3" to "نام سری ۳"
    )
    // V54.3 — فیلدهای فارسی مرجع برای ۲۲ نوع مرحلهٔ پایانی.
    "plot" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max")
    // V136 — محورها
    "axnum" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام برچسب")
    "axxy" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max", "step" to "گام برچسب")
    "axq1" -> listOf("xmax" to "x max", "ymax" to "y max", "step" to "گام برچسب")
    "axgrid" -> listOf("xmax" to "تعداد ستون", "ymax" to "تعداد سطر")
    "axpol" -> listOf("xmax" to "تعداد حلقه")
    "ax3d" -> listOf("xmax" to "تعداد تیک", "step" to "گام")
    // V137.2 — ۲۰ نوع تازهٔ محور
    "axnumpts" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام", "pts" to "نقطه‌ها (با ویرگول)", "labs" to "نام نقطه‌ها")
    "axnumint" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام", "lo" to "ابتدای بازه", "hi" to "انتهای بازه", "lc" to "ابتدا بسته؟ (۱/۰)", "hc" to "انتها بسته؟ (۱/۰)", "labs" to "برچسب")
    "axnumineq" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام", "x0" to "نقطهٔ مرز", "dir" to "جهت (۱ راست، -۱ چپ)", "cl" to "بسته؟ (۱/۰)", "labs" to "برچسب")
    "axnumfrac" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام", "den" to "مخرج")
    "axnumdec" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام اعشاری")
    "axnumblank" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام")
    "axnumlog" -> listOf("xmax" to "تعداد دهه")
    "axnumtwo" -> listOf("xmin" to "کمینه", "xmax" to "بیشینه", "step" to "گام", "m" to "ضریب", "b" to "عرض از مبدأ", "s1" to "نام محور بالا", "s2" to "نام محور پایین")
    "axxypts" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max", "step" to "گام", "pts" to "نقطه‌ها (x,y;x,y)", "labs" to "نام نقطه‌ها")
    "axxyline" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max", "step" to "گام", "m" to "شیب m", "b" to "عرض از مبدأ b", "labs" to "برچسب")
    "axxyvec" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max", "step" to "گام", "x1" to "x ابتدا", "y1" to "y ابتدا", "x2" to "x انتها", "y2" to "y انتها", "labs" to "برچسب")
    "axxycirc" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max", "step" to "گام", "cx" to "x مرکز", "cy" to "y مرکز", "r" to "شعاع")
    "axxynogrid", "axxyquads", "axxyblank" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y min", "ymax" to "y max", "step" to "گام")
    "axdual" -> listOf("xmin" to "x min", "xmax" to "x max", "ymin" to "y چپ min", "ymax" to "y چپ max", "lo" to "y راست min", "hi" to "y راست max", "s1" to "نام محور چپ", "s2" to "نام محور راست")
    "axlog" -> listOf("xmax" to "x max", "ymax" to "تعداد دههٔ y")
    "axloglog" -> listOf("xmax" to "تعداد دههٔ x", "ymax" to "تعداد دههٔ y")
    "axtime" -> listOf("labs" to "زمان‌ها (با ویرگول)", "s1" to "نام محور")
    // V304 — ابزار ریاضی ابتدایی
    "axtally" -> listOf("n" to "تعداد (۰ تا ۷۵)", "label" to "نمایش تعداد زیر شکل")
    "axbase10" -> listOf("n" to "عدد (۰ تا ۱٬۰۰۰٬۰۰۰)", "label" to "نمایش عدد زیر شکل")
    "axplace" -> listOf("n" to "ردیف ۱ (عدد)", "n2" to "ردیف ۲ (خالی = ندارد)", "op2" to "علامت ردیف ۲ (+ یا −)", "n3" to "ردیف ۳ (خالی = ندارد)", "op3" to "علامت ردیف ۳ (+ یا −)", "rows" to "تعداد ردیف‌ها (۱ تا ۶)", "blank" to "خانه‌ها خالی؟ (۱=بله)")
    "axclock" -> listOf("h" to "ساعت (۰ تا ۱۲)", "m" to "دقیقه (۰ تا ۵۹)", "nums" to "نمایش عددها (۱/۰)", "label" to "نوشتن ساعت زیر شکل (۱/۰)")
    "axruler" -> listOf("len" to "طول خط‌کش (۲ تا ۵۰ سانتی‌متر)", "a" to "ابتدای اندازه‌گیری (اختیاری)", "b" to "انتهای اندازه‌گیری (اختیاری)", "label" to "نمایش طول بازه")
    "axfrac" -> listOf("num" to "صورت", "den" to "مخرج", "shape" to "شکل", "label" to "نوشتن کسر زیر شکل (۱/۰)")
    "axmoney" -> listOf("notes" to "اسکناس‌ها (با ویرگول)", "coins" to "سکه‌ها (با ویرگول)", "unit" to "واحد", "sum" to "نوشتن جمع (۱/۰)")
    // V314.0 — ۶ ابزار جدید ریاضی و علوم ابتدایی
    "axabacus" -> listOf("n" to "عدد (۰ تا ۹۹۹٬۹۹۹)", "cols" to "تعداد ستون‌ها (۲ تا ۶)", "blank" to "مخفی کردن ارقام زیر چرتکه (۱=بله)")
    "axjump" -> listOf("start" to "نقطهٔ شروع", "step" to "طول هر پرش", "jumps" to "تعداد پرش", "max" to "انتهای محور", "label" to "نمایش عبارت جمع/ضرب (۱/۰)")
    "axbalance" -> listOf("left" to "وزنهٔ کفهٔ چپ", "right" to "وزنهٔ کفهٔ راست", "unit" to "واحد جرم", "label" to "نمایش علامت مقایسه")
    "axthermo" -> listOf("val" to "دما (°C)", "min" to "کمینهٔ دما", "max" to "بیشینهٔ دما")
    "axsaw" -> listOf("size" to "اندازهٔ جدول (۳×۳ تا ۹×۹)", "mode" to "نمایش (رنگ / شکل / عدد)", "vals" to "سطرها با ; و خانه‌ها با , (جای خالی = ?)")
    "axpattern" -> listOf("seq" to "دنباله با ویرگول (مثلاً ●,▲,●,▲,?)", "mode" to "نوع نمایش الگو (شکل / رنگ / مهره)")
    // V315.0 — ۸ ابزار جدید ریاضی و علوم ابتدایی
    "axtenframe" -> listOf("a" to "مهره‌های قرمز", "b" to "مهره‌های آبی", "frames" to "تعداد قاب (۱ یا ۲)", "label" to "نمایش عبارت جمع")
    "axsym" -> listOf("cells" to "خانه‌ها (ردیف,ستون با ;)", "axis" to "خط تقارن (v عمودی / h افقی)", "mirror" to "نمایش قرینه (۱=بله، ۰=؟)")
    "axspin" -> listOf("parts" to "بخش‌های چرخنده (با کاما)", "arrow" to "شمارهٔ بخش عقربه")
    "axbeaker" -> listOf("val" to "حجم مایع", "max" to "ظرفیت کل", "unit" to "واحد (mL یا لیتر)")
    "axgeoboard" -> listOf("pts" to "مختصات میخ‌ها (x,y با ;)", "grid" to "اندازهٔ شبکه (۳ تا ۷)")
    "axdice" -> listOf("dice" to "اعداد تاس‌ها (با کاما)", "sum" to "نمایش جمع (۱=جمع، ۲=؟، ۰=خیر)")
    "axmularea" -> listOf("a" to "عدد اول (افقی)", "b" to "عدد دوم (عمودی)", "blank" to "مخفی کردن حاصل‌ها (۰ یا ۱)")
    "axvenn" -> listOf("left" to "دایرهٔ چپ", "right" to "دایرهٔ راست", "mid" to "بخش مشترک (وسط)")
    // V317.0 — ۶ ابزار پیشرفتهٔ جدید ریاضی ابتدایی
    "axnumgrid" -> listOf("start" to "عدد شروع", "cols" to "تعداد ستون", "rows" to "تعداد سطر", "rule" to "رنگ‌آمیزی مضرب/الگو", "blanks" to "خانه‌های جای خالی (با ویرگول)")
    "axfacttree" -> listOf("n" to "عدد مورد تجزیه (۴ تا ۹۹۹)", "blank" to "حالت جای خالی (؟)")
    "axfnmachine" -> listOf("in" to "ورودی‌ها (با ویرگول)", "op1" to "عملگر مرحلهٔ اول", "op2" to "عملگر مرحلهٔ دوم (اختیاری)", "blank" to "حالت جای خالی (؟)")
    "axfracwall" -> listOf("dens" to "مخرج ردیف‌ها (با ویرگول)", "fills" to "تعداد رنگ‌شدهٔ هر ردیف", "mode" to "نحوهٔ نمایش کسرها")
    "axtangram" -> listOf("shape" to "طرح تانگرام (۷ تکه)", "mode" to "حالت نمایش قطعات")
    "axangle" -> listOf("deg" to "زاویه (۰ تا ۱۸۰ درجه)", "pro" to "نمایش نقاله", "label" to "نمایش اندازهٔ زاویه")
    "axrod" -> listOf("nums" to "میله‌ها (۱ تا ۱۰ با ویرگول)", "blank" to "جای خالی (؟)")
    "axtower" -> listOf("base" to "آجرهای ردیف پایین", "op" to "عمل (+ یا −)", "blank" to "جای خالی (؟)")
    "axmulwheel" -> listOf("c" to "عدد مرکز", "n" to "تعداد پره (۳ تا ۱۲)", "op" to "عمل (× یا +)", "hide" to "جای خالی (؟)")
    "axmultgrid" -> listOf("rows" to "عددهای سطر", "cols" to "عددهای ستون", "blank" to "جای خالی (۰ کامل / ۱ خانه‌ها / ۲ سرستون‌ها)")
    "axnet" -> listOf("shape" to "نوع حجم", "mode" to "حالت نمایش")
    "axcubes" -> listOf("map" to "ارتفاع ستون‌ها (سطرها با ;)", "view" to "نما", "hide" to "مخفی کردن تعداد (؟)")
    "axcalendar" -> listOf("month" to "نام ماه", "start" to "روز شروع (۰ = شنبه)", "days" to "تعداد روزها", "mark" to "روزهای نشان‌دار (با ویرگول)", "blank" to "جای خالی (؟)")
    "axlever" -> listOf("kind" to "نوع ماشین", "load" to "نیروی بار (N)", "effort" to "نیروی محرک (N)", "l1" to "بازوی بار", "l2" to "بازوی نیرو")
    "axcycle" -> listOf("kind" to "نوع چرخه")
    "axwordtrain" -> listOf("syll" to "هجاها (با ویرگول)", "word" to "کلمهٔ ساخته‌شده", "mode" to "حالت")
    "axtiles" -> listOf("x2" to "تعداد کاشی x²", "x" to "تعداد کاشی x", "k" to "تعداد کاشی ۱", "neg" to "منفی (۱/۰)")
    "axkenken" -> listOf("size" to "ابعاد جدول (۳ تا ۶)", "cages" to "قفس‌ها (خانه‌ها|هدف|عمل با ;)")
    "axdicefold" -> listOf("mode" to "نمایش وجه‌ها", "top" to "شمارهٔ وجه بالا", "ask" to "پرسش وجه مقابل (۱/۰)")
    "axtransform" -> listOf("shape" to "شکل", "op" to "نوع تبدیل", "dx" to "انتقال افقی", "dy" to "انتقال عمودی", "ang" to "زاویهٔ دوران", "axis" to "محور بازتاب", "grid" to "اندازهٔ شبکه")
    "axcaliper" -> listOf("kind" to "نوع ابزار", "val" to "مقدار اندازه‌گیری (mm)", "max" to "بیشینهٔ مقیاس")
    "axsigntable" -> listOf("roots" to "ریشه‌ها (با ویرگول)", "mult" to "تکرار ریشه‌ها (۱ فرد / ۲ زوج)", "lead" to "ضریب پیشرو (۱ یا -۱)")
    "axtrigcircle" -> listOf("deg" to "زاویه (درجه)", "show" to "نمایش تصویر sin و cos (۱/۰)")
    "axmapping" -> listOf("from" to "اعضای دامنه", "to" to "اعضای برد", "arrows" to "پیکان‌ها (مبدأ-مقصد با ;)")
    "axprobtree" -> listOf("first" to "گزینه‌های مرحلهٔ ۱", "second" to "گزینه‌های مرحلهٔ ۲", "mode" to "نمایش")
    "axconic" -> listOf("kind" to "نوع مقطع", "a" to "نیم‌محور a", "b" to "نیم‌محور b")
    "axorbital" -> listOf("z" to "عدد اتمی", "mode" to "حالت (جعبهٔ اوربیتال / ساختار لوئیس)", "sym" to "نماد اتم (لوئیس)", "val" to "الکترون ظرفیت (لوئیس)")
    "axpunnett" -> listOf("kind" to "نوع", "p1" to "والد ۱", "p2" to "والد ۲", "mode" to "نمایش")
    "axecg" -> listOf("kind" to "نوع نوار", "n" to "تعداد ضربان", "label" to "برچسب موج‌ها (۱/۰)", "A" to "دامنه (نوسان)", "T" to "دوره (نوسان)")
    "axprosody" -> listOf("line" to "مصراع (ارکان با فاصله)", "mode" to "حالت نمایش")
    "axtruthtable" -> listOf("kind" to "نوع", "expr" to "عبارت (عنوان ستون)")
    "axledger" -> listOf("title" to "عنوان حساب", "debit" to "اقلام بدهکار (با ویرگول)", "credit" to "اقلام بستانکار (با ویرگول)")
    "axstaff" -> listOf("kind" to "نوع", "clef" to "کلید", "notes" to "نت‌ها (شمارهٔ MIDI)")
    "axkmap" -> listOf("vars" to "تعداد متغیر", "ones" to "خانه‌های ۱ (شماره)", "groups" to "گروه‌ها (خانه‌ها با ;)")
    "axgate" -> listOf("kind" to "نوع گیت", "a" to "ورودی A (۱/۰)", "b" to "ورودی B (۱/۰)")
    "axdfa" -> listOf("states" to "حالت‌ها", "start" to "حالت شروع", "accept" to "حالت پایانی", "edges" to "یال‌ها (مبدأ-مقصد:برچسب با ;)")
    "axbst" -> listOf("vals" to "مقدارها (با ویرگول)", "mode" to "نوع درخت")
    "axbode" -> listOf("kind" to "نوع نمودار", "gain" to "بهره (dB)", "poles" to "قطب‌ها (با ویرگول)")
    "axbeam" -> listOf("len" to "طول تیر (m)", "load" to "نوع بار", "p" to "بار متمرکز (N)", "a" to "فاصلهٔ بار (m)", "q" to "بار گسترده (N/m)")
    "axmohr" -> listOf("sx" to "تنش σx", "sy" to "تنش σy", "txy" to "تنش برشی τxy")
    "axpv" -> listOf("kind" to "نوع چرخه")
    "axpert" -> listOf("acts" to "فعالیت‌ها (نام:شروع-پایان:مدت با ;)")
    "axbenzene" -> listOf("kind" to "نوع ساختار", "mode" to "نمایش حلقه")
    "axnormal" -> listOf("lo" to "کران پایین", "hi" to "کران بالا", "mu" to "میانگین μ", "sd" to "انحراف معیار σ")
    "axsupply" -> listOf("kind" to "نوع نمودار", "shift" to "مقدار جابه‌جایی", "ceiling" to "سقف قیمت (۰ = ندارد)")
    "axgame" -> listOf("kind" to "نوع", "mat" to "ماتریس سود (سطرها با ;)")
    "axunitconv" -> listOf("kind" to "نوع واحد", "val" to "مقدار")
    "axperimeter" -> listOf("shape" to "شکل", "w" to "عرض / شعاع", "h" to "ارتفاع", "n" to "تعداد ضلع چندضلعی")
    "axpictograph" -> listOf("mode" to "نوع نمودار", "data" to "داده‌ها (با ویرگول)", "labs" to "نام دسته‌ها")
    "axrounding" -> listOf("n" to "عدد", "mode" to "گرد به")
    "axdivision" -> listOf("a" to "عدد کل", "b" to "تعداد هر گروه")
    "axshapeid" -> listOf("kind" to "شکل", "n" to "تعداد ضلع (چندضلعی)")
    "axcircleparts" -> listOf("show" to "بخش نمایش‌داده‌شده")
    "axcylinder" -> listOf("kind" to "نوع جسم", "r" to "شعاع قاعده", "h" to "ارتفاع")
    "axproportion" -> listOf("a" to "مخرج معلوم", "b" to "صورت معلوم", "c" to "مقدار متناظر")
    "axlinear" -> listOf("m1" to "شیب خط اول", "b1" to "عرض از مبدأ اول", "m2" to "شیب خط دوم", "b2" to "عرض از مبدأ دوم")
    "axbarchart" -> listOf("data" to "داده‌ها", "labs" to "برچسب‌ها", "show" to "نمایش شاخص")
    "axparabola" -> listOf("a" to "ضریب a", "b" to "ضریب b", "c" to "جملهٔ ثابت c")
    "axinthales" -> listOf("ang" to "زاویهٔ مرکزی (درجه)")
    "axpendulum" -> listOf("len" to "طول نخ (متر)", "amp" to "دامنه (درجه)")
    "axincline" -> listOf("ang" to "زاویهٔ شیب (درجه)", "mass" to "جرم (kg)")
    "axcircuit" -> listOf("mode" to "نوع مدار", "r1" to "مقاومت ۱ (Ω)", "r2" to "مقاومت ۲ (Ω)", "v" to "ولتاژ باتری")
    "axoptics" -> listOf("kind" to "نوع عدسی", "f" to "فاصلهٔ کانونی (cm)", "d" to "فاصلهٔ جسم (cm)")
    "axheatcurve" -> listOf()
    "axdna" -> listOf("mode" to "نمایش", "seq" to "توالی بازها (A/T/G/C)")
    "axcell" -> listOf("kind" to "نوع سلول")
    "axtimeline" -> listOf("events" to "رویدادها (سال:عنوان)")
    "axmapgeo" -> listOf("kind" to "نوع نمایش")
    "axsentence" -> listOf("subj" to "نهاد", "verb" to "گزاره", "obj" to "مفعول (اختیاری)", "mode" to "نوع جمله")
    "axvectors" -> listOf("ax1" to "مؤلفهٔ x بردار a", "ay1" to "مؤلفهٔ y بردار a", "bx" to "مؤلفهٔ x بردار b", "by" to "مؤلفهٔ y بردار b", "mode" to "عمل")
    "axmatrix" -> listOf("a" to "ماتریس A (سطرها با ;)", "b" to "ماتریس B (سطرها با ;)")
    "axintegral" -> listOf("f" to "تابع", "lo" to "کران پایین a", "hi" to "کران بالا b")
    "axflowchart" -> listOf("steps" to "مراحل (با ویرگول)")
    "axstack" -> listOf("mode" to "ساختار", "items" to "عناصر", "op" to "عملیات")
    "axsort" -> listOf("vals" to "اعداد آرایه", "step" to "شمارهٔ دور")
    "axregression" -> listOf("pts" to "نقطه‌ها (x,y;x,y)")
    "axpolpts" -> listOf("xmax" to "تعداد حلقه", "pts" to "نقطه‌ها (r,θ;r,θ)", "labs" to "نام نقطه‌ها")
    "ax3dpt" -> listOf("xmax" to "تعداد تیک", "step" to "گام", "x1" to "x نقطه", "y1" to "y نقطه", "z1" to "z نقطه", "labs" to "نام نقطه")
    "flow" -> listOf("labs" to "مراحل (با ویرگول)")
    "gantt" -> listOf("labs" to "فعالیت‌ها", "vals" to "شروع", "vals2" to "مدت")
    "time" -> listOf("labs" to "رویدادها", "vals" to "تاریخ / مقدار")
    "dumb", "slope" -> listOf(
        "labs" to "برچسب‌ها", "vals" to "مقدار شروع / قبل", "vals2" to "مقدار پایان / بعد",
        "s1" to "نام سری ۱", "s2" to "نام سری ۲"
    )
    "stream" -> listOf(
        "labs" to "برچسب‌ها", "vals" to "سری ۱", "vals2" to "سری ۲", "vals3" to "سری ۳",
        "s1" to "نام سری ۱", "s2" to "نام سری ۲", "s3" to "نام سری ۳"
    )
    "viol", "strip" -> listOf(
        "labs" to "گروه‌ها", "mins" to "حداقل", "q1s" to "چارک ۱",
        "meds" to "میانه", "q3s" to "چارک ۳", "maxs" to "حداکثر"
    )
    "stem" -> listOf("vals" to "عددها (با ویرگول)")
    "smat" -> listOf(
        "xs" to "متغیر X", "ys" to "متغیر Y", "zs" to "متغیر Z",
        "s1" to "نام X", "s2" to "نام Y", "s3" to "نام Z"
    )
    "dend" -> listOf("labs" to "برگ‌ها / نام‌ها")
    "sank" -> listOf("vals" to "جریان‌ها (مثل A-C:8,B-D:5)")
    "chrd" -> listOf("labs" to "گره‌ها", "vals" to "ماتریس سطری")
    "netw" -> listOf("labs" to "گره‌ها", "vals" to "یال‌ها (مثل A-B,B-C)")
    "map" -> listOf("labs" to "نام مناطق", "vals" to "مقدار هر منطقه")
    "bmap" -> listOf("labs" to "نام مناطق", "vals" to "اندازه حباب")
    "surf" -> listOf("nrows" to "تعداد ردیف", "ncols" to "تعداد ستون", "vals" to "ارتفاع‌ها (سطری)")
    "calh" -> listOf("vals" to "مقدار روزها (از شنبه، سطری)")
    "word" -> listOf("labs" to "واژه‌ها", "vals" to "وزن / فراوانی")
    else -> listOf("labs" to "برچسب‌ها", "vals" to "مقدارها")
}

/** کلیدهایی که مقدار متنی (فهرست با ویرگول یا نام سری) دارند نه عدد. */
private val TEXT_PARAM_KEYS = setOf(
    "shape", "unit", "notes", "coins", "op2", "op3", "n2", "n3", "mode", "seq", // V304 — V310.8/V310.9/V314.0: کلیدهای متنی ابزارهای ابتدایی
    "cells", "axis", "parts", "dice", "left", "right", "mid", // V315.0: کلیدهای متنی ۸ ابزار جدید ابتدایی
    "rule", "blanks", "in", "op1", "dens", "fills", "label", // V317.0: کلیدهای متنی ابزارهای پیشرفتهٔ ابتدایی
    "month", "mark", "syll", "word", "from", "to", "arrows", "first", "second", "sym",
    "p1", "p2", "states", "accept", "edges", "line", "expr", "title", "debit", "credit",
    "ones", "groups", "cages", "nums", "base", "map", "mat", // V318.0: کلیدهای متنی ۴۰ ابزار جامع
    "val", "data", "n", "a", "b", "c", "show", "kind", "mode", "seq", "subj", "verb", "obj",
    "ax1", "ay1", "bx", "by", "f", "lo", "hi", "steps", "items", "op", "pts", "events", "labs", "w", "h",
    "shape", "r", "m1", "b1", "m2", "b2", // V319.1: کلیدهای متنی ۳۰ ابزار جدید
    "labs", "labs2", "vals", "vals2", "vals3", "xs", "ys", "zs", "s1", "s2", "s3",
    // V54.2 — فهرست‌های عددی چندتایی و مقادیر آزاد مرحلهٔ دوم متنی ذخیره می‌شوند
    // (همان قرارداد رشته‌ای مرجع).
    "mins", "q1s", "meds", "q3s", "maxs", "opens", "highs", "lows", "closes",
    "rows", "cols", "mean", "ucl", "lcl", "unit", "n", "ab", "ac", "bc", "abc",
    // V54.3 — کلیدهای متنی مرحلهٔ پایانی.
    "nrows", "ncols",
    // V137.2 — نقطه‌های محور (جفت‌های عددی) متنی ذخیره می‌شوند.
    "pts"
)

private fun paramKeys(type: String): List<String> = paramFields(type).map { it.first }

internal fun paramLabel(type: String, key: String): String =
    paramFields(type).firstOrNull { it.first == key }?.second ?: key

private fun initialParams(template: FigureTemplate, initialSpec: FigureSpec): Map<String, String> {
    val defaults = FigureSpec.parse(template.specJson)
    return paramKeys(template.id).associateWith { key ->
        if (key in TEXT_PARAM_KEYS) {
            initialSpec.xStr(key).ifBlank { defaults?.xStr(key).orEmpty() }
        } else {
            val n = initialSpec.xNum(key, Float.NaN)
            if (!n.isNaN()) trimNum(n)
            else defaults?.xNum(key, Float.NaN)?.takeIf { !it.isNaN() }?.let { trimNum(it) }.orEmpty()
        }
    }
}

private fun buildGraphSpec(
    base: FigureSpec,
    type: String,
    params: Map<String, String>,
    title: String
): FigureSpec {
    val root = base.raw.toMutableMap()
    root["t"] = JsonPrimitive(type)
    // V55.15 — مرجع، توکن بدون k را «هندسه» می‌گیرد؛ نمودار باید k='g' داشته
    // باشد وگرنه box جعبه‌ای در کادر متن به‌شکل مکعب‌مستطیل هندسه رندر می‌شود.
    root["k"] = JsonPrimitive("g")
    val extra = (root["X"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
    paramKeys(type).forEach { key ->
        val value = params[key].orEmpty().trim()
        if (value.isBlank()) {
            extra.remove(key)
        } else if (key in TEXT_PARAM_KEYS) {
            extra[key] = JsonPrimitive(value)
        } else {
            faEnDigits(value).replace('،', '.').toFloatOrNull()?.let { extra[key] = JsonPrimitive(it) }
                ?: extra.remove(key)
        }
    }
    if (title.isBlank()) extra.remove("title") else extra["title"] = JsonPrimitive(title.trim())
    if (extra.isEmpty()) root.remove("X") else root["X"] = JsonObject(extra)
    return FigureSpec(JsonObject(root))
}

private fun templateLabel(kind: FigureKind, type: String): String =
    (if (kind == GEOMETRY) GEOMETRY_FIGURES else GRAPH_FIGURES + AXIS_FIGURES)
        .firstOrNull { it.id == type }
        ?.label
        ?: type

private fun trimNum(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()

/** V315.1 / V315.2 — گزینه‌های منوی کشویی برای پارامترهای چندحالتهٔ ابزارهای ریاضی/علوم ابتدایی، محورها، نمودارها و هندسه. */
private fun dropdownOptionsFor(type: String, key: String): List<Pair<String, String>>? = when (type to key) {
    "axfrac" to "shape" -> listOf("circle" to "دایره‌ای (قطاع)", "ring" to "حلقه‌ای", "bar" to "نواری", "square" to "مربعی (شبکه)")
    "axsaw" to "size" -> listOf(
        "3" to "۳×۳ (پایهٔ اول — ۳ خانه)",
        "4" to "۴×۴ (استاندارد ابتدایی — بلوک ۲×۲)",
        "5" to "۵×۵ (پنج‌تایی)",
        "6" to "۶×۶ (پیشرفته — بلوک ۲×۳)",
        "7" to "۷×۷ (هفت‌تایی)",
        "8" to "۸×۸ (هشت‌تایی)",
        "9" to "۹×۹ (سودوکوی کامل — بلوک ۳×۳)"
    )
    "axsaw" to "mode" -> listOf(
        "color" to "رنگی — ویژهٔ اول ابتدایی (خانه‌های رنگی)",
        "shape" to "شکلی (● ▲ ■ ★ ◆ ♥ ⬢ ✿ ▼)",
        "color_shape" to "رنگ و شکل با هم (خانهٔ رنگی + شکل)",
        "num" to "عددی (۱ تا ۹)"
    )
    "axpattern" to "mode" -> listOf(
        "shape" to "شکلی و عددی (● ▲ ■ ★)",
        "color" to "الگوی رنگ‌آمیزی — ویژهٔ اول ابتدایی",
        "bead" to "نخ و مهره‌های رنگی"
    )
    "axnumgrid" to "cols" -> listOf("10" to "۱۰ ستون (جدول صدتایی)", "5" to "۵ ستون", "6" to "۶ ستون", "8" to "۸ ستون")
    "axnumgrid" to "rows" -> listOf("10" to "۱۰ سطر (جدول صدتایی)", "5" to "۵ سطر (۵۰تایی)", "6" to "۶ سطر", "8" to "۸ سطر")
    "axnumgrid" to "rule" -> listOf(
        "0" to "بدون رنگ‌آمیزی الگو",
        "2" to "مضرب‌های ۲ (شمارش دو تا دو تا)",
        "3" to "مضرب‌های ۳ (شمارش سه تا سه تا)",
        "5" to "مضرب‌های ۵ (شمارش پنج تا پنج تا)",
        "10" to "مضرب‌های ۱۰ (شمارش ده تا ده تا)",
        "prime" to "اعداد اول (غربال اراتستن)",
        "odd" to "اعداد فرد"
    )
    "axfacttree" to "blank" -> listOf(
        "0" to "نمایش کامل همهٔ عوامل",
        "1" to "جای خالی عوامل اول انتهایی (؟)",
        "2" to "جای خالی شاخه‌های میانی (؟)"
    )
    "axfnmachine" to "op1" -> listOf("+ 3" to "+ 3 (جمع با ۳)", "+ 5" to "+ 5 (جمع با ۵)", "- 2" to "- 2 (تفریق ۲)", "× 2" to "× 2 (ضرب در ۲)", "× 3" to "× 3 (ضرب در ۳)", "÷ 2" to "÷ 2 (تقسیم بر ۲)")
    "axfnmachine" to "op2" -> listOf("" to "ندارد (ماشین یک‌مرحله‌ای)", "+ 1" to "+ 1", "- 1" to "- 1", "× 2" to "× 2", "+ 5" to "+ 5")
    "axfnmachine" to "blank" -> listOf("1" to "جای خالی در خروجی‌ها (؟)", "2" to "جای خالی قانون ماشین (؟)", "0" to "نمایش کامل ورودی و خروجی")
    "axfracwall" to "mode" -> listOf("label" to "نمایش کسر و رنگ", "color" to "فقط رنگ‌آمیزی (بدون نوشته)", "blank" to "جای خالی کسرهای مساوی (؟)")
    "axtangram" to "shape" -> listOf("square" to "مربع ۷ تکه (کلاسیک)", "house" to "خانه", "boat" to "قایق", "cat" to "گربه")
    "axtangram" to "mode" -> listOf("color" to "رنگی کامل", "num" to "رنگی با شمارهٔ قطعات (۱ تا ۷)", "outline" to "سفید خط‌چین (برای رنگ‌آمیزی)", "blank" to "جای خالی شمارهٔ قطعات (؟)")
    "axangle" to "pro" -> listOf("1" to "همراه با نقالهٔ مدرج", "0" to "فقط دو ضلع و کمان زاویه")
    "axangle" to "label" -> listOf("1" to "نمایش درجه و نوع زاویه", "2" to "نمایش با جای خالی (؟°)", "0" to "بدون برچسب")
    "axsym" to "axis" -> listOf("v" to "عمودی (چپ و راست)", "h" to "افقی (بالا و پایین)")
    "axsym" to "mirror" -> listOf("1" to "نمایش کامل شکل قرینه", "0" to "جای خالی (؟) برای سؤال")
    "axplace" to "op2", "axplace" to "op3" -> listOf("" to "ندارد", "+" to "جمع (+)", "-" to "تفریق (−)")
    "axplace" to "rows" -> listOf("1" to "۱ ردیف", "2" to "۲ ردیف", "3" to "۳ ردیف", "4" to "۴ ردیف")
    "axplace" to "blank", "axabacus" to "blank", "axmularea" to "blank" -> listOf("0" to "نمایش عددها", "1" to "جای خالی (برای طرح سؤال)")
    "axabacus" to "cols" -> listOf("2" to "۲ ستون (تا دهگان)", "3" to "۳ ستون (تا صدگان)", "4" to "۴ ستون (تا هزار)", "5" to "۵ ستون (تا ده‌هزار)", "6" to "۶ ستون (تا صدهزار)")
    "axclock" to "nums" -> listOf("1" to "نمایش عددهای ۱ تا ۱۲", "0" to "بدون عدد (تمرین ساعت)")
    "axclock" to "label" -> listOf("0" to "ندارد", "1" to "نمایش ساعت زیر شکل", "2" to "نمایش با جای خالی (ساعت = ؟ : ؟)")
    "axfrac" to "label" -> listOf("1" to "نمایش کسر زیر شکل", "2" to "نمایش با جای خالی (؟ / ؟)", "0" to "ندارد")
    "axtally" to "label", "axbase10" to "label" -> listOf("1" to "نمایش عدد زیر شکل", "2" to "نمایش با جای خالی (؟) برای سؤال", "0" to "ندارد")
    "axruler" to "label" -> listOf("1" to "نمایش طول بازه (cm)", "2" to "نمایش با جای خالی (؟ cm)", "0" to "ندارد")
    "axbalance" to "label" -> listOf("1" to "نمایش علامت مقایسه (> / < / =)", "2" to "نمایش با جای خالی (؟) برای سؤال", "0" to "ندارد")
    "axtenframe" to "label", "axjump" to "label" -> listOf("1" to "نمایش عبارت کامل", "2" to "نمایش با جای خالی (= ؟)", "0" to "ندارد")
    "axmoney" to "unit" -> listOf("تومان" to "تومان", "ریال" to "ریال")
    "axmoney" to "sum" -> listOf("1" to "نمایش جمع کل", "2" to "نمایش با جای خالی (جمع: ؟)", "0" to "مخفی (بدون جمع)")
    "axbalance" to "unit" -> listOf("کیلوگرم" to "کیلوگرم", "گرم" to "گرم")
    "axbeaker" to "unit" -> listOf("mL" to "میلی‌لیتر (mL)", "لیتر" to "لیتر", "سی‌سی" to "سی‌سی (cc)")
    "axtenframe" to "frames" -> listOf("2" to "۲ قاب (۲۰تایی)", "1" to "۱ قاب (۱۰تایی)")
    "axgeoboard" to "grid" -> listOf("3" to "۳×۳", "4" to "۴×۴", "5" to "۵×۵ (استاندارد)", "6" to "۶×۶", "7" to "۷×۷")
    "axdice" to "sum" -> listOf("1" to "نمایش جمع کامل (۴ + ۶ = ۱۰)", "2" to "نمایش با جای خالی (۴ + ۶ = ؟)", "0" to "فقط نمایش تاس‌ها")
    "axrod" to "blank" -> listOf("0" to "نمایش طول میله‌ها", "1" to "طول با جای خالی (؟)")
    "axtower" to "op" -> listOf("+" to "جمع (+)", "-" to "تفریق (−)")
    "axtower" to "blank" -> listOf("0" to "کامل", "1" to "با جای خالی (؟)")
    "axmulwheel" to "op" -> listOf("×" to "ضرب (×)", "+" to "جمع (+)")
    "axmulwheel" to "hide" -> listOf("0" to "کامل", "1" to "با جای خالی (؟)")
    "axmultgrid" to "blank" -> listOf("0" to "کامل", "1" to "خانه‌های خالی (؟)", "2" to "سرستون‌های خالی (؟)")
    "axnet" to "shape" -> listOf("cube" to "مکعب (۶ وجه)", "cuboid" to "مکعب‌مستطیل", "pyramid" to "هرم (۵ وجه)", "prism" to "منشور (۵ وجه)")
    "axnet" to "mode" -> listOf("color" to "رنگی کامل", "num" to "شمارهٔ وجه‌ها", "outline" to "سفید برای رنگ‌آمیزی", "blank" to "جای خالی (؟)")
    "axcubes" to "view" -> listOf("iso" to "نمای سه‌بعدی (ایزومتریک)", "top" to "نمای از بالا", "front" to "نمای روبه‌رو", "side" to "نمای از پهلو")
    "axcubes" to "hide" -> listOf("0" to "نمایش تعداد", "1" to "تعداد با جای خالی (؟)")
    "axcalendar" to "start" -> listOf("0" to "شنبه", "1" to "یکشنبه", "2" to "دوشنبه", "3" to "سه‌شنبه", "4" to "چهارشنبه", "5" to "پنجشنبه", "6" to "جمعه")
    "axcalendar" to "blank" -> listOf("0" to "همهٔ روزها", "1" to "چند خانه جای خالی (؟)")
    "axlever" to "kind" -> listOf("lever1" to "اهرم نوع اول", "lever2" to "اهرم نوع دوم", "lever3" to "اهرم نوع سوم", "pulley" to "قرقرهٔ متحرک", "gears" to "چرخ‌دنده")
    "axcycle" to "kind" -> listOf("water" to "چرخهٔ آب", "plant" to "مراحل رشد گیاه", "frog" to "دگردیسی قورباغه", "food" to "زنجیرهٔ غذایی")
    "axwordtrain" to "mode" -> listOf("join" to "نمایش کلمه", "blank" to "کلمه با جای خالی (؟)")
    "axtiles" to "neg" -> listOf("0" to "مثبت (آبی)", "1" to "منفی (قرمز)")
    "axkenken" to "size" -> listOf("3" to "۳×۳", "4" to "۴×۴", "5" to "۵×۵", "6" to "۶×۶")
    "axdicefold" to "mode" -> listOf("dots" to "نقطه‌ها", "num" to "عددها")
    "axdicefold" to "top" -> listOf("1" to "1", "2" to "2", "3" to "3", "4" to "4", "5" to "5", "6" to "6")
    "axdicefold" to "ask" -> listOf("0" to "نمایش همهٔ وجه‌ها", "1" to "چند وجه جای خالی (؟)")
    "axtransform" to "shape" -> listOf("tri" to "مثلث", "sq" to "مربع", "rect" to "مستطیل")
    "axtransform" to "op" -> listOf("translate" to "انتقال (بردار)", "rotate" to "دوران", "reflect" to "بازتاب")
    "axtransform" to "axis" -> listOf("x" to "نسبت به محور x", "y" to "نسبت به محور y")
    "axcaliper" to "kind" -> listOf("vernier" to "کولیس ورنیه (دقت ۰٫۱)", "micrometer" to "ریزسنج (دقت ۰٫۰۱)")
    "axsigntable" to "lead" -> listOf("1" to "مثبت (۱)", "-1" to "منفی (-۱)")
    "axtrigcircle" to "show" -> listOf("1" to "با تصویر sin و cos", "0" to "فقط ضلع زاویه")
    "axprobtree" to "mode" -> listOf("prob" to "نمایش درصد احتمال", "count" to "نمایش نتیجهٔ هر شاخه")
    "axconic" to "kind" -> listOf("ellipse" to "بیضی", "hyperbola" to "هذلولی", "parabola" to "سهمی")
    "axorbital" to "mode" -> listOf("box" to "جعبه‌های اوربیتال", "lewis" to "ساختار لوئیس")
    "axpunnett" to "kind" -> listOf("punnett" to "مربع پانت", "pedigree" to "شجره‌نامه")
    "axpunnett" to "mode" -> listOf("genotype" to "ژنوتیپ", "ratio" to "نسبت فنوتیپی")
    "axecg" to "kind" -> listOf("ecg" to "نوار قلب (ECG)", "shm" to "نوسان هماهنگ ساده")
    "axecg" to "label" -> listOf("1" to "با برچسب P و QRS و T", "0" to "بدون برچسب")
    "axecg" to "n" -> listOf("1" to "۱ ضربان", "2" to "۲ ضربان", "3" to "۳ ضربان")
    "axprosody" to "mode" -> listOf("arkan" to "نام ارکان + هجاها", "taqti" to "فقط تقطیع (∪ و −)")
    "axtruthtable" to "kind" -> listOf("truth" to "جدول ارزش گزاره‌ها", "square" to "مربع تقابل ارسطویی")
    "axstaff" to "kind" -> listOf("staff" to "خطوط حامل", "color" to "چرخهٔ ۱۲ رنگ ایتن")
    "axstaff" to "clef" -> listOf("treble" to "کلید سل", "bass" to "کلید فا")
    "axkmap" to "vars" -> listOf("2" to "۲ متغیره", "3" to "۳ متغیره", "4" to "۴ متغیره")
    "axgate" to "kind" -> listOf("and" to "AND (و)", "or" to "OR (یا)", "not" to "NOT (نقیض)", "nand" to "NAND", "nor" to "NOR", "xor" to "XOR")
    "axgate" to "a" -> listOf("0" to "۰", "1" to "۱")
    "axgate" to "b" -> listOf("0" to "۰", "1" to "۱")
    "axbst" to "mode" -> listOf("bst" to "درخت جستجوی دودویی", "heap" to "درخت هرم (آرایه‌ای)")
    "axbode" to "kind" -> listOf("bode" to "نمودار بود (اندازه و فاز)", "nyquist" to "نمودار نایکوئیست")
    "axbeam" to "load" -> listOf("point" to "بار متمرکز", "udl" to "بار گسترده")
    "axpv" to "kind" -> listOf("carnot" to "چرخهٔ کارنو", "otto" to "چرخهٔ اتو", "diesel" to "چرخهٔ دیزل", "rankine" to "چرخهٔ رانکین")
    "axbenzene" to "kind" -> listOf("kekule" to "بنزن (ککوله)", "chair" to "صندلی سیکلوهگزان", "newman" to "تصویر نیومن", "fischer" to "تصویر فیشر")
    "axbenzene" to "mode" -> listOf("label" to "پیوندهای دوگانه", "circle" to "حلقهٔ آروماتیک")
    "axsupply" to "kind" -> listOf("equilibrium" to "تعادل بازار", "shift" to "افزایش تقاضا", "ceiling" to "سقف قیمت")
    "axgame" to "kind" -> listOf("game" to "ماتریس بازی ۲×۲", "islm" to "مدل IS-LM")
    // محورها و نمودارها
    "axnumint" to "lc" -> listOf("1" to "بسته [ (نقطهٔ توپر)", "0" to "باز ( (نقطهٔ توخالی)")
    "axnumint" to "hc" -> listOf("0" to "باز ) (نقطهٔ توخالی)", "1" to "بسته ] (نقطهٔ توپر)")
    "axnumineq" to "dir" -> listOf("1" to "به سمت راست (x > یا x ≥)", "-1" to "به سمت چپ (x < یا x ≤)")
    "axnumineq" to "cl" -> listOf("0" to "باز (توخالی: > یا <)", "1" to "بسته (توپر: ≥ یا ≤)")
    "axnumfrac" to "den" -> listOf("2" to "۲ (نیم)", "3" to "۳ (ثلث)", "4" to "۴ (ربع)", "5" to "۵ (خمس)", "6" to "۶ (سدس)", "8" to "۸", "10" to "۱۰ (دهم)")
    "venn" to "n" -> listOf("2" to "۲ مجموعه (A و B)", "3" to "۳ مجموعه (A، B و C)")
    // شکل‌های هندسی
    "ngon" to "X:n" -> listOf("3" to "۳ضلعی", "4" to "۴ضلعی", "5" to "۵ضلعی", "6" to "۶ضلعی", "7" to "۷ضلعی", "8" to "۸ضلعی", "9" to "۹ضلعی", "10" to "۱۰ضلعی", "12" to "۱۲ضلعی")
    "parll" to "X:n" -> listOf("1" to "۱ خط مورب", "2" to "۲ خط مورب", "3" to "۳ خط مورب", "4" to "۴ خط مورب")
    "axunitconv" to "kind" -> listOf("len" to "طول: کیلومتر → متر → میلی‌متر", "mass" to "جرم: تن → کیلوگرم → گرم", "area" to "مساحت: m² → dm² → cm²")
    "axperimeter" to "shape" -> listOf("rect" to "مستطیل", "tri" to "مثلث قائم‌الزاویه", "circle" to "دایره", "poly" to "چندضلعی منتظم")
    "axpictograph" to "mode" -> listOf("picto" to "تصویری (شکل‌ها)", "bar" to "میله‌ای")
    "axrounding" to "mode" -> listOf("ten" to "دهگان", "hundred" to "صدگان")
    "axshapeid" to "kind" -> listOf("tri" to "مثلث", "square" to "مربع", "rect" to "مستطیل", "circle" to "دایره", "poly" to "چندضلعی منتظم")
    "axcircleparts" to "show" -> listOf("all" to "همهٔ اجزا", "radius" to "شعاع", "diameter" to "قطر", "chord" to "وتر", "tangent" to "مماس", "arc" to "کمان")
    "axcylinder" to "kind" -> listOf("cylinder" to "استوانه", "cone" to "مخروط")
    "axbarchart" to "show" -> listOf("mean" to "میانگین", "median" to "میانه", "none" to "بدون شاخص")
    "axcircuit" to "mode" -> listOf("series" to "سری", "parallel" to "موازی")
    "axoptics" to "kind" -> listOf("convex" to "محدب (همگرا)", "concave" to "مقعر (واگرا)")
    "axdna" to "mode" -> listOf("helix" to "مارپیچ دوگانه", "flat" to "نردبان صاف")
    "axcell" to "kind" -> listOf("animal" to "سلول جانوری", "plant" to "سلول گیاهی")
    "axmapgeo" to "kind" -> listOf("compass" to "گل‌آویز جهت‌ها", "map" to "نقشه و مقیاس")
    "axsentence" to "mode" -> listOf("verbal" to "جملهٔ فعلیه", "nominal" to "جملهٔ اسمیه")
    "axvectors" to "mode" -> listOf("sum" to "جمع a + b", "diff" to "تفاضل a − b")
    "axintegral" to "f" -> listOf("quad" to "f(x) = ¼x²", "sin" to "f(x) = sin x", "line" to "f(x) = ۰٫۶x + ۰٫۴")
    "axstack" to "mode" -> listOf("stack" to "پشته (LIFO)", "queue" to "صف (FIFO)")
    "axstack" to "op" -> listOf("push" to "افزودن", "pop" to "حذف")
    else -> null
}

/** V310.7 / V310.9 / V315.1 — منوی کشویی گزینه‌های چندحالتهٔ شکل‌های ابتدایی. */
@Composable
private fun FigureShapeDropdown(
    label: String,
    value: String,
    onSelect: (String) -> Unit,
    options: List<Pair<String, String>> = listOf("circle" to "دایره‌ای (قطاع)", "ring" to "حلقه‌ای", "bar" to "نواری", "square" to "مربعی (شبکه)")
) {
    var open by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == value }?.second ?: options.first().second
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $currentLabel", modifier = Modifier.weight(1f))
            Text("▼")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { open = false; onSelect(id) })
            }
        }
    }
}
