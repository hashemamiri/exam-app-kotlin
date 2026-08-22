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
import ir.exam.app.core.figure.FigureSpec
import ir.exam.app.core.figure.FigureTemplate
import ir.exam.app.core.figure.GEOMETRY_FIGURES
import ir.exam.app.core.figure.GRAPH_FIGURES
import ir.exam.app.ui.figure.FigureKind.GEOMETRY
import ir.exam.app.ui.figure.FigureKind.GRAPH
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class FigureKind { GEOMETRY, GRAPH }

/**
 * مرحلهٔ اول درج: فقط نوع‌های متعلق به همان آیکن را نشان می‌دهد.
 * آیکن «درج شکل» هرگز فهرست نمودار را باز نمی‌کند و برعکس.
 */
@Composable
fun FigureTypePickerDialog(
    kind: FigureKind,
    onDismiss: () -> Unit,
    onTypeSelected: (FigureSpec) -> Unit
) {
    val geometry = kind == GEOMETRY
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (geometry) "📐 درج شکل" else "📈 درج نمودار",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Text(
                    if (geometry) "ابتدا نوع شکل هندسی را انتخاب کنید."
                    else "ابتدا نوع نمودار را انتخاب کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                if (geometry) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(104.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(GEOMETRY_FIGURES, key = { it.id }) { template ->
                            GeometryTypeCell(template) { onTypeSelected(template.toSpec()) }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(GRAPH_FIGURES, key = { it.id }) { template ->
                            GraphTypeCell(template) { onTypeSelected(template.toSpec()) }
                        }
                    }
                }
            }
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
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InlineFigureView(
                template.toSpec(),
                Modifier.width(150.dp).height(112.dp),
                contentDescription = template.label
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(template.label, style = MaterialTheme.typography.titleMedium)
                Text("برای ویرایش انتخاب کنید", style = MaterialTheme.typography.bodySmall)
            }
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
    val resolvedKind = if (initialSpec?.type?.let { type -> GRAPH_FIGURES.any { it.id == type } } == true) {
        GRAPH
    } else {
        initialKind
    }
    val baseSpec = initialSpec ?: if (resolvedKind == GEOMETRY) {
        GEOMETRY_FIGURES.first().toSpec()
    } else {
        GRAPH_FIGURES.first().toSpec()
    }
    val typeLabel = templateLabel(resolvedKind, baseSpec.type)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (resolvedKind == GEOMETRY) "✏️ ویرایش شکل" else "✏️ ویرایش نمودار",
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
    "circ", "semi" -> listOf(v("O"), x("r", "شعاع r"))
    "ring" -> listOf(v("O"), x("R", "شعاع بیرونی R"), x("r", "شعاع داخلی r"))
    "ell" -> listOf(v("O"), x("a", "نیم‌محور a"))
    "ang" -> listOf(v("O"), v("A"), v("B"), x("m", "اندازه زاویه"))
    "parll" -> listOf(v("d1"), v("d2"), x("n", "تعداد خطوط"), x("tilt", "زاویه خطوط"))
    "pseg", "ray", "ln" -> listOf(v("A"), v("B"))
    "cube" -> listOf(s("s"))
    "box" -> listOf(s("a"), s("b"), s("h"))
    "cyl", "cone" -> listOf(x("h", "ارتفاع"), x("r", "شعاع"))
    "sph" -> listOf(v("O"), x("r", "شعاع"))
    "pyr" -> listOf(v("S"), v("A"), v("B"), s("a"), x("h", "ارتفاع"))
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
            OutlinedTextField(
                value = values[field.id].orEmpty(),
                onValueChange = { values = values + (field.id to it) },
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("پیش‌نمایش", style = MaterialTheme.typography.labelLarge)
                    InlineFigureView(
                        draft,
                        Modifier.fillMaxWidth().height(220.dp),
                        contentDescription = "پیش‌نمایش شکل"
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
    val graphType = initialSpec.type.takeIf { type -> GRAPH_FIGURES.any { it.id == type } } ?: GRAPH_FIGURES.first().id
    val template = GRAPH_FIGURES.firstOrNull { it.id == graphType } ?: GRAPH_FIGURES.first()
    var title by remember(initialSpec.toJson()) { mutableStateOf(initialSpec.xStr("title")) }
    var params by remember(initialSpec.toJson()) {
        mutableStateOf(initialParams(template, initialSpec))
    }
    val draft = buildGraphSpec(initialSpec, graphType, params, title)

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("نوع نمودار: ${template.label}", style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedTextField(
                title,
                { title = it },
                label = { Text("عنوان نمودار (اختیاری)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(paramKeys(graphType), key = { it }) { key ->
            OutlinedTextField(
                value = params[key] ?: "",
                onValueChange = { value -> params = params + (key to value) },
                label = { Text(key) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("پیش‌نمایش", style = MaterialTheme.typography.labelLarge)
                    InlineFigureView(
                        draft,
                        Modifier.fillMaxWidth().height(180.dp),
                        contentDescription = "پیش‌نمایش نمودار"
                    )
                }
            }
        }
        item {
            Button(onClick = { onInsert(draft) }, modifier = Modifier.fillMaxWidth()) {
                Text("✅ درج نمودار")
            }
        }
    }
}

private fun paramKeys(type: String): List<String> = when (type) {
    "line" -> listOf("m", "b")
    "quad" -> listOf("a", "b", "c")
    "sine" -> listOf("A", "w", "ph")
    "exp" -> listOf("a", "b")
    else -> listOf("labs", "vals")
}

private fun initialParams(template: FigureTemplate, initialSpec: FigureSpec): Map<String, String> {
    return paramKeys(template.id).associateWith { key ->
        when (key) {
            "labs" -> initialSpec.xStr("labs", "A,B,C,D")
            "vals" -> initialSpec.xStr("vals", "4,7,3,6")
            else -> {
                val n = initialSpec.xNum(key, Float.NaN)
                if (n.isNaN()) "" else trimNum(n)
            }
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
    val extra = (root["X"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
    paramKeys(type).forEach { key ->
        val value = params[key].orEmpty().trim()
        if (value.isBlank()) {
            extra.remove(key)
        } else if (key == "labs" || key == "vals") {
            extra[key] = JsonPrimitive(value)
        } else {
            value.toFloatOrNull()?.let { extra[key] = JsonPrimitive(it) }
                ?: extra.remove(key)
        }
    }
    if (title.isBlank()) extra.remove("title") else extra["title"] = JsonPrimitive(title.trim())
    if (extra.isEmpty()) root.remove("X") else root["X"] = JsonObject(extra)
    return FigureSpec(JsonObject(root))
}

private fun templateLabel(kind: FigureKind, type: String): String =
    (if (kind == GEOMETRY) GEOMETRY_FIGURES else GRAPH_FIGURES)
        .firstOrNull { it.id == type }
        ?.label
        ?: type

private fun trimNum(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
