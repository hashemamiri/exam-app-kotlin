package ir.exam.app.ui.math

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.exam.app.core.math.NativeMathFormatter

/** Smart Hub مرجع با تمام ده بخش، این بار دارای trigger واقعی و قابل استفاده. */
@Composable
fun FormulaSmartHubDialog(
    library: FormulaReferenceData,
    currentTex: String,
    favorites: List<FormulaReferenceEntry>,
    recentFormulas: List<String>,
    lastFormula: String,
    onDismiss: () -> Unit,
    onInsertAtActive: (FormulaReferenceEntry) -> Unit,
    onReplaceFormula: (String) -> Unit,
    onOpenEntries: (String, List<FormulaReferenceEntry>) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onBackspace: () -> Unit,
    onNewLine: () -> Unit
) {
    var lessonKey by remember { mutableStateOf("math") }
    var rawPreview by remember { mutableStateOf(false) }
    var quickInput by remember { mutableStateOf("") }
    val lesson = FormulaSmartReference.lesson(lessonKey)
    val shownFavorites = favorites.ifEmpty { FormulaSmartReference.defaultFavorites }

    fun closeAndReplace(tex: String) {
        if (tex.isNotBlank()) onReplaceFormula(tex)
        onDismiss()
    }

    fun closeAndInsert(entry: FormulaReferenceEntry) {
        onInsertAtActive(entry)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("✨ مرکز هوشمند فرمول", style = MaterialTheme.typography.titleLarge)
                            Text("همهٔ قابلیت‌های پنهان مرجع در یک صفحه", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = onDismiss) { Text("✕ بستن") }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            OutlinedButton(onClick = { if (lastFormula.isNotBlank()) closeAndReplace(lastFormula) }) {
                                Text(if (lastFormula.isBlank()) "📋 فرمول آخر (خالی)" else "📋 فرمول آخر")
                            }
                        }
                        item { FilterChip(selected = !rawPreview, onClick = { rawPreview = false }, label = { Text("نمایش نهایی") }) }
                        item { FilterChip(selected = rawPreview, onClick = { rawPreview = true }, label = { Text("متن خام") }) }
                        item {
                            OutlinedButton(onClick = {
                                onOpenEntries(
                                    "کتابخانهٔ ${lesson.label}",
                                    FormulaSmartReference.entriesForCategories(library, lesson.categoryIds)
                                )
                                onDismiss()
                            }) { Text("📚 کتابخانهٔ این درس") }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth().height(120.dp)) {
                        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                            if (currentTex.isBlank()) Text("هنوز فرمولی ساخته نشده است")
                            else if (rawPreview) Text(currentTex, style = MaterialTheme.typography.bodySmall)
                            else NativeFormulaView(currentTex, Modifier.fillMaxWidth(), 21.sp, contentDescription = "پیش‌نمایش هوشمند")
                        }
                    }
                }
                item { SmartTitle("⭐ فرمول‌های پرکاربرد من") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(shownFavorites, key = { it.label + "¦" + it.tex }) { entry ->
                            OutlinedButton(onClick = { closeAndInsert(entry) }) { Text(entry.label) }
                        }
                    }
                }
                item { SmartTitle("📘 کتابخانهٔ درس‌به‌درس") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FormulaSmartReference.lessons, key = FormulaSmartLesson::key) { item ->
                            FilterChip(
                                selected = lessonKey == item.key,
                                onClick = { lessonKey = item.key },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
                item { SmartTitle("🧩 قالب‌های آماده با جای خالی") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(lesson.templates, key = { it.label + it.tex }) { template ->
                            Card {
                                TextButton(onClick = {
                                    if (template.insertAtActiveBox) closeAndInsert(FormulaReferenceEntry(template.label, template.tex))
                                    else closeAndReplace(template.tex)
                                }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(template.label)
                                        NativeFormulaIcon(template.tex, Modifier.width(130.dp).height(36.dp), 18.sp, contentDescription = template.label)
                                    }
                                }
                            }
                        }
                    }
                }
                item { SmartTitle("⌨️ تبدیل تایپ ساده و شیمی به فرمول") }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            quickInput,
                            { quickInput = it.take(2_000) },
                            label = { Text("x^2 + a/b یا H2SO4 یا Fe3+ یا ⇌") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(onClick = {
                            val tex = NativeMathFormatter.smartQuickToTex(quickInput)
                            if (tex.isNotBlank()) closeAndReplace(tex)
                        }) { Text("تبدیل") }
                    }
                }
                item { SmartTitle("🕘 اخیراً استفاده‌شده") }
                if (recentFormulas.isEmpty()) item { Text("هنوز فرمولی ثبت نشده است.") }
                items(recentFormulas.take(8), key = { it }) { tex ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { closeAndReplace(tex) }, modifier = Modifier.weight(1f)) {
                                NativeFormulaIcon(tex, Modifier.fillMaxWidth().height(42.dp), 18.sp, contentDescription = "فرمول اخیر")
                            }
                            TextButton(onClick = { onDeleteRecent(tex) }) { Text("حذف") }
                        }
                    }
                }
                item { SmartTitle("🎒 بسته‌های آمادهٔ امتحانی") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FormulaSmartReference.packs, key = FormulaSmartPack::label) { pack ->
                            OutlinedButton(onClick = {
                                onOpenEntries(pack.label, FormulaSmartReference.entriesForPack(library, pack))
                                onDismiss()
                            }) { Text(pack.label) }
                        }
                    }
                }
                item { SmartTitle("() [] {} | | انتخاب سریع پرانتزها") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FormulaSmartReference.delimiters, key = FormulaDelimiterPreset::label) { delimiter ->
                            OutlinedButton(onClick = {
                                closeAndInsert(
                                    FormulaReferenceEntry(
                                        delimiter.label,
                                        delimiterTex(delimiter, "x")
                                    )
                                )
                            }) { Text(delimiter.label) }
                        }
                    }
                }
                item { SmartTitle("📱 کلیدهای درشت مخصوص موبایل") }
                item {
                    FormulaSmartReference.bigKeyLabels.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { label ->
                                Button(
                                    onClick = {
                                        when (label) {
                                            "کسر" -> closeAndInsert(FormulaReferenceEntry(label, "\\frac{a}{b}"))
                                            "توان" -> closeAndInsert(FormulaReferenceEntry(label, "x^{n}"))
                                            "رادیکال" -> closeAndInsert(FormulaReferenceEntry(label, "\\sqrt{x}"))
                                            "( )" -> closeAndInsert(FormulaReferenceEntry(label, "\\left(x\\right)"))
                                            "sin" -> closeAndInsert(FormulaReferenceEntry(label, "\\sin"))
                                            "⌫" -> { onBackspace(); onDismiss() }
                                            "↵" -> { onNewLine(); onDismiss() }
                                            "آخرین" -> if (lastFormula.isNotBlank()) closeAndReplace(lastFormula)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text(label) }
                            }
                        }
                    }
                }
                item { HorizontalDivider() }
                item { Text("Smart Hub در مرجع بدون دکمه بود؛ این نسخه آن را به قابلیت واقعی Native تبدیل کرده است.", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun SmartTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

fun delimiterTex(preset: FormulaDelimiterPreset, body: String): String = when (preset.kind) {
    "floor" -> "\\lfloor $body \\rfloor"
    "ceil" -> "\\lceil $body \\rceil"
    else -> {
        val open = if (preset.open == "{") "\\{" else preset.open
        val close = if (preset.close == "}") "\\}" else preset.close
        "\\left$open$body\\right$close"
    }
}
