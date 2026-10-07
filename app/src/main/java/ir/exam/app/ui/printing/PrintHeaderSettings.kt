package ir.exam.app.ui.printing

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * مدل و پنجرهٔ بومی تنظیمات سربرگ چاپ.
 *
 * شِمای مشترک هفت قالب در `assets/print/header_settings_schema.json` نگه‌داری
 * می‌شود و مقدارها در `PrintHeaderStore` ذخیره می‌شوند.
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
    val options: List<HeaderFieldOption> = emptyList(),
    /** V204 — نمایش شرطی: فقط وقتی مقدار فیلد `field` یکی از `in` باشد (مثل لوگوی دلخواه سربرگ ۸). */
    val showIf: HeaderShowIf? = null,
    /** V204.4 — فیلدهای هم‌گروهِ پشت‌سرهم کنار هم (یک ردیف) نمایش داده می‌شوند؛ مثل ردیف‌های سربرگ ۸. */
    val group: String? = null
)

/** V204.4 — فیلدهای پشت‌سرهم با `group` یکسان را در یک گروه می‌گذارد (فیلد بدون group = گروه تک‌عضوی). */
fun groupHeaderFields(fields: List<HeaderField>): List<List<HeaderField>> {
    val out = ArrayList<MutableList<HeaderField>>()
    fields.forEach { f ->
        val last = out.lastOrNull()
        if (f.group != null && last != null && last.first().group == f.group) last.add(f) else out.add(mutableListOf(f))
    }
    return out
}

@Serializable
data class HeaderShowIf(val field: String, val `in`: List<String> = emptyList())

/** V204 — فیلد با توجه به مقدار فعلی بقیهٔ فیلدها دیده شود؟ (مشترک اپ و سایت: همان قاعدهٔ headerSettingsForm) */
fun HeaderField.isVisible(values: Map<String, String>): Boolean {
    val c = showIf ?: return true
    return c.`in`.contains(values[c.field].orEmpty())
}

/** V204 — لوگوی دلخواه سربرگ ۸: کوچک‌سازی تا ۳۲۰ پیکسل و PNG (شفافیت حفظ می‌شود) به‌صورت data-URL محلی؛ هرگز آپلود نمی‌شود. */
fun encodeHeaderLogoDataUrl(bytes: ByteArray, maxEdge: Int = 320): String? {
    if (bytes.isEmpty()) return null
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge * 2) sample *= 2
    val raw = android.graphics.BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    ) ?: return null
    val scale = minOf(1f, maxEdge.toFloat() / maxOf(raw.width, raw.height).coerceAtLeast(1))
    val bmp = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), true) else raw
    val bos = java.io.ByteArrayOutputStream()
    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos)
    return "data:image/png;base64," + android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
}

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

/** پنجرهٔ بومی «تنظیمات سربرگ» بر پایهٔ شِمای مشترک چاپ. */
@Composable
fun HeaderSettingsDialog(
    schema: HeaderSchema,
    currentValues: Map<String, String>,
    onApply: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    // V105 — قالبِ ذخیره‌شدهٔ قبلی پیش‌انتخاب می‌شود تا با هر بار بازکردن به «classic» برنگردد.
    var templateId by remember(currentValues) {
        mutableStateOf(
            currentValues["f_headerTemplate"]?.takeIf { saved -> schema.templates.any { it.id == saved } }
                ?: schema.templates.firstOrNull()?.id ?: "classic"
        )
    }
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
                    // V204 — فیلدهای شرطی (showIf) با تغییر مقدار فیلد کنترل‌کننده پنهان/آشکار می‌شوند
                    // V204.4 — فیلدهای هم‌گروه (ردیف‌های سربرگ ۸) کنار هم؛ جای فیلد پنهان خالی می‌ماند تا ستون‌ها تراز بمانند
                    val groups = groupHeaderFields(template.fields).filter { g -> g.any { it.isVisible(values) } }
                    items(groups, key = { it.first().id }) { g ->
                        if (g.size == 1) {
                            val f = g.first()
                            HeaderFieldEditor(f, values, Modifier.fillMaxWidth())
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                g.forEach { f ->
                                    if (f.isVisible(values)) HeaderFieldEditor(f, values, Modifier.weight(1f))
                                    else Spacer(Modifier.weight(1f))
                                }
                            }
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
private fun HeaderFieldEditor(f: HeaderField, values: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>, modifier: Modifier) {
    Box(modifier) {
        when (f.kind) {
            "select" -> FieldSelect(f, values) { values[f.id] = it }
            "image" -> FieldImage(f, values) { values[f.id] = it }
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

/** V204 — انتخاب تصویر لوگو از گالری (محلی؛ data-URL داخل همان نقشهٔ مقادیر سربرگ). */
@Composable
private fun FieldImage(f: HeaderField, values: Map<String, String>, onChange: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val current = values[f.id].orEmpty()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val data = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { encodeHeaderLogoDataUrl(it) }
            }.getOrNull()
            if (data != null) onChange(data)
        }
    }
    val bitmap = remember(current) {
        if (current.startsWith("data:image/")) runCatching {
            val bytes = android.util.Base64.decode(current.substringAfter("base64,"), android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull() else null
    }
    Column(Modifier.fillMaxWidth()) {
        Text(f.label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(bitmap, contentDescription = "لوگو", modifier = Modifier.height(56.dp))
            } else {
                Text("تصویری انتخاب نشده", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            OutlinedButton(onClick = { picker.launch("image/*") }) { Text(if (bitmap != null) "تغییر" else "انتخاب تصویر") }
            if (bitmap != null) TextButton(onClick = { onChange("") }) { Text("حذف") }
        }
    }
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
            trailingIcon = { TextButton(onClick = { open = true }) { Text("تغییر") } },
            modifier = Modifier.fillMaxWidth()
        )
        // V204.1 — OutlinedTextField خودش لمس را می‌بلعد و clickable روی آن اجرا نمی‌شد (انتخاب لوگو ممکن نبود)؛ لایهٔ شفاف روی فیلد
        Box(
            Modifier
                .matchParentSize()
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
