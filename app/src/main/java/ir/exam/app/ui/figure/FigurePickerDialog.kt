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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import kotlinx.serialization.json.JsonPrimitive

enum class FigureKind { GEOMETRY, GRAPH }

/**
 * انتخاب‌گر شکل و نمودار. تب «شکل هندسی» شبکه‌ای از شکل‌ها و تب «نمودار»
 * انواع نمودار با پارامترهای عددی را نشان می‌دهد.
 */
@Composable
fun FigurePickerDialog(
    initialSpec: FigureSpec? = null,
    initialKind: FigureKind = FigureKind.GEOMETRY,
    onDismiss: () -> Unit,
    onInsert: (FigureSpec) -> Unit
) {
    val initialType = initialSpec?.type ?: ""
    val initialIsGraph = initialType in GRAPH_FIGURES.map { it.id }
    var kind by remember {
        mutableStateOf(
            when {
                initialKind == FigureKind.GRAPH || initialIsGraph -> FigureKind.GRAPH
                else -> FigureKind.GEOMETRY
            }
        )
    }
    var graphType by remember { mutableStateOf(initialType.takeIf { initialIsGraph } ?: "line") }
    var title by remember { mutableStateOf(initialSpec?.xStr("title") ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("📐 درج شکل و نمودار", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = kind == FigureKind.GEOMETRY,
                        onClick = { kind = FigureKind.GEOMETRY },
                        label = { Text("شکل هندسی") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = kind == FigureKind.GRAPH,
                        onClick = { kind = FigureKind.GRAPH },
                        label = { Text("نمودار") },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
                Box(Modifier.weight(1f)) {
                    if (kind == FigureKind.GEOMETRY) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(104.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(GEOMETRY_FIGURES, key = { it.id }) { template ->
                                GeometryCell(template) { onInsert(template.toSpec()) }
                            }
                        }
                    } else {
                        GraphPane(
                            graphType = graphType,
                            onGraphType = { graphType = it },
                            title = title,
                            onTitle = { title = it },
                            initialSpec = initialSpec,
                            onInsert = onInsert
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeometryCell(template: FigureTemplate, onClick: () -> Unit) {
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
private fun GraphPane(
    graphType: String,
    onGraphType: (String) -> Unit,
    title: String,
    onTitle: (String) -> Unit,
    initialSpec: FigureSpec?,
    onInsert: (FigureSpec) -> Unit
) {
    val template = GRAPH_FIGURES.firstOrNull { it.id == graphType } ?: GRAPH_FIGURES.first()
    var params by remember(graphType) {
        mutableStateOf(initialParams(template, initialSpec))
    }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GRAPH_FIGURES.forEach { tpl ->
                    FilterChip(
                        selected = graphType == tpl.id,
                        onClick = { onGraphType(tpl.id) },
                        label = { Text(tpl.label) }
                    )
                }
            }
        }
        item { OutlinedTextField(title, onTitle, label = { Text("عنوان نمودار (اختیاری)") }, modifier = Modifier.fillMaxWidth()) }
        items(paramKeys(graphType)) { key ->
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
                        buildGraphSpec(graphType, params, title),
                        Modifier.fillMaxWidth().height(180.dp),
                        contentDescription = "پیش‌نمایش نمودار"
                    )
                }
            }
        }
        item {
            Button(
                onClick = { onInsert(buildGraphSpec(graphType, params, title)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("✅ درج نمودار") }
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

private fun initialParams(template: FigureTemplate, initialSpec: FigureSpec?): Map<String, String> {
    val spec = initialSpec ?: template.toSpec()
    return paramKeys(template.id).associateWith { key ->
        when (key) {
            "labs" -> spec.xStr("labs", "A,B,C,D")
            "vals" -> spec.xStr("vals", "4,7,3,6")
            else -> {
                val n = spec.xNum(key, Float.NaN)
                if (n.isNaN()) "" else trimNum(n)
            }
        }
    }
}

private fun buildGraphSpec(type: String, params: Map<String, String>, title: String): FigureSpec {
    val extra = mutableMapOf<String, JsonPrimitive>()
    paramKeys(type).forEach { key ->
        val value = params[key] ?: return@forEach
        if (key == "labs" || key == "vals") {
            extra[key] = JsonPrimitive(value)
        } else {
            value.toFloatOrNull()?.let { extra[key] = JsonPrimitive(it) }
        }
    }
    if (title.isNotBlank()) extra["title"] = JsonPrimitive(title)
    return FigureSpec.build(type, extra = extra)
}

private fun trimNum(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
