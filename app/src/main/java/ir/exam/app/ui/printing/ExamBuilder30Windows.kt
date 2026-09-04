package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * V76.4 — پنجره‌های بومی آزمون‌ساز (نسخهٔ ۳۰):
 *
 * ۱) HeaderSettingsDialog — تنظیمات سربرگ: شِمای فرم از خودِ فایلِ ۳۰ استخراج و در
 *    assets/print/header_settings_schema.json ذخیره شده (۷ قالب، همهٔ فیلدها با
 *    شناسه/برچسب/نوع واقعی)؛ مقدارها از صفحه خوانده و با __qmfSetFields اعمال می‌شوند.
 * ۲) SaveExamDialog — ذخیره: «این جلسه» (نگه‌داشتن در برنامه) یا «فایل JSON در دستگاه».
 * ۳) OpenExamSummaryDialog — پیش‌نمایش فایل باز‌شده قبل از اعمال.
 * ۴) NewQuestionTypeDialog — انتخاب نوع سؤال با همان ۶ گزینهٔ خود فایل.
 */

@Serializable
data class HeaderFieldOption(val v: String, val t: String)

@Serializable
data class HeaderField(
    val id: String,
    val label: String,
    val kind: String = "input",
    val full: Boolean = false,
    val type: String = "text",
    val placeholder: String? = null,
    val rows: Int = 2,
    val options: List<HeaderFieldOption> = emptyList()
)

@Serializable
data class HeaderTemplate(val id: String, val label: String, val fields: List<HeaderField> = emptyList())

@Serializable
data class HeaderSchema(val templates: List<HeaderTemplate>)

private val headerSchemaJson = Json { ignoreUnknownKeys = true }

fun loadHeaderSchema(context: android.content.Context): HeaderSchema? = runCatching {
    headerSchemaJson.decodeFromString(
        HeaderSchema.serializer(),
        context.assets.open("print/header_settings_schema.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    )
}.getOrNull()

/** پنجرهٔ بومی «تنظیمات سربرگ» — معادلِ کاملِ پنل HTML فایل. */
@Composable
fun HeaderSettingsDialog(
    schema: HeaderSchema,
    currentValues: Map<String, String>,
    onApply: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var templateId by remember { mutableStateOf(schema.templates.firstOrNull()?.id ?: "classic") }
    // نقشهٔ observable — وگرنه تایپ در فیلدها بازسازی نمی‌شود
    val values = remember(currentValues) { mutableStateMapOf<String, String>().apply { putAll(currentValues) } }
    var templateMenu by remember { mutableStateOf(false) }
    val template = schema.templates.firstOrNull { it.id == templateId } ?: schema.templates.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "اطلاعات سربرگ آزمون",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }
                // انتخاب نوع سربرگ — همان ۷ قالبِ خود فایل
                Box {
                    OutlinedTextField(
                        value = template.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("انتخاب نوع سربرگ") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { templateMenu = true }) { Text("تغییر") }
                        }
                    )
                    DropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                        schema.templates.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = {
                                    templateId = t.id
                                    templateMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(template.fields, key = { it.id }) { f ->
                        when (f.kind) {
                            "select" -> FieldSelect(f, values) { values[f.id] = it }
                            "textarea" -> OutlinedTextField(
                                value = values[f.id].orEmpty(),
                                onValueChange = { values[f.id] = it },
                                label = { Text(f.label) },
                                minLines = f.rows,
                                modifier = Modifier.fillMaxWidth()
                            )
                            else -> FieldInput(f, values) { values[f.id] = it }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("انصراف") }
                    TextButton(
                        onClick = {
                            val payload = LinkedHashMap<String, String>()
                            payload["f_headerTemplate"] = templateId
                            template.fields.forEach { payload[it.id] = values[it.id].orEmpty() }
                            onApply(payload)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("اعمال") }
                }
            }
        }
    }
}

@Composable
private fun FieldInput(f: HeaderField, values: Map<String, String>, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = values[f.id].orEmpty(),
        onValueChange = onChange,
        label = { Text(f.label) },
        placeholder = f.placeholder?.let { p -> { Text(p, maxLines = 1) } },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FieldSelect(f: HeaderField, values: Map<String, String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = values[f.id].orEmpty().ifEmpty { f.options.firstOrNull()?.v.orEmpty() }
    val currentLabel = f.options.firstOrNull { it.v == current }?.t ?: current
    Box {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(f.label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            f.options.forEach { o ->
                DropdownMenuItem(text = { Text(o.t) }, onClick = {
                    onChange(o.v)
                    open = false
                })
            }
        }
    }
}

/** پنجرهٔ بومی «ذخیره آزمون». */
@Composable
fun SaveExamDialog(
    onSaveSession: () -> Unit,
    onSaveFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ذخیره آزمون") },
        text = { Text("آزمون کجا ذخیره شود؟ «این جلسه» همان بازشدن نگه می‌دارد و «فایل JSON» نسخهٔ پشتیبان قابل‌ویرایش در دستگاه می‌سازد.") },
        confirmButton = {
            TextButton(onClick = onSaveFile) { Text("فایل JSON در دستگاه") }
        },
        dismissButton = {
            TextButton(onClick = onSaveSession) { Text("ذخیره در همین جلسه") }
        }
    )
}

/** پنجرهٔ بومی تأیید «بازکردن آزمون» — خلاصهٔ فایل قبل از اعمال. */
@Composable
fun OpenExamSummaryDialog(
    course: String?,
    school: String?,
    questionCount: Int,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بازکردن آزمون") },
        text = {
            Column {
                Text("فایل آزمون خوانده شد:")
                Spacer(Modifier.height(6.dp))
                Text("درس: " + (course?.takeIf { it.isNotBlank() } ?: "—"))
                Text("آزمون‌دهنده/مدرسه: " + (school?.takeIf { it.isNotBlank() } ?: "—"))
                Text("تعداد سؤال: $questionCount")
            }
        },
        confirmButton = { TextButton(onClick = onApply) { Text("اعمال") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/** پنجرهٔ بومی «سوال جدید» — همان ۶ نوعِ خود فایل (ترتیب/نمادها عیناً). */
@Composable
fun NewQuestionTypeDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val types = listOf(
        "multiple" to "🔘 چندگزینه‌ای",
        "truefalse" to "✓ صحیح/غلط",
        "long" to "📝 تشریحی",
        "fill" to "___ جای‌خالی",
        "numeric" to "🔢 عددی",
        "matching" to "↔ جورکردنی"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب نوع سوال") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { (id, label) ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .clickable { onPick(id) }
                            .padding(vertical = 14.dp, horizontal = 12.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

/** پاک‌سازی نام فایل برای ذخیرهٔ JSON. */
internal fun safeExamFileName(course: String?): String {
    val base = (course.orEmpty()).ifBlank { "exam" }
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .take(40)
    return "exam-$base-${System.currentTimeMillis()}.json"
}
