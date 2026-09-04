package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * V78.1 — نوارِ بومیِ مدیریت سؤال برای آزمون‌سازِ چاپ.
 *
 * هیچ‌کدام از این کارها منطقِ تازه‌ای در کاتلین ندارند؛ همگی همان توابعِ
 * موجودِ صفحه را از طریق `window.__qmfQuestionAction` صدا می‌زنند تا رفتار و
 * خروجیِ رندر مو‌به‌مو یکسان بماند. کادرِ متنِ سؤال عمداً اینجا نیست — طبق
 * تصمیم صریح کاربر ویرایشِ متن در همان صفحه انجام می‌شود.
 */
internal data class QuestionRow(
    val id: String,
    val index: Int,
    val type: String,
    val score: String,
    val preview: String
) {
    val typeLabel: String
        get() = when (type) {
            "multiple" -> "🔘 چندگزینه‌ای"
            "truefalse" -> "✓ صحیح/غلط"
            "fill" -> "___ جای‌خالی"
            "numeric" -> "🔢 عددی"
            "matching" -> "↔ جورکردنی"
            else -> "📝 تشریحی"
        }
}

/** خواندنِ فهرستِ سؤال‌ها از خروجیِ `__qmfQuestionList`. */
internal fun parseQuestionRows(raw: String?): List<QuestionRow> {
    val text = unwrapJsString(raw)
    if (text.isBlank()) return emptyList()
    return runCatching {
        Json.parseToJsonElement(text).jsonArray.map { element ->
            val o = element.jsonObject
            QuestionRow(
                id = o["id"]?.jsonPrimitive?.content.orEmpty(),
                index = o["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                type = o["type"]?.jsonPrimitive?.content.orEmpty(),
                score = o["score"]?.jsonPrimitive?.content.orEmpty(),
                preview = o["preview"]?.jsonPrimitive?.content.orEmpty()
            )
        }
    }.getOrDefault(emptyList())
}

/**
 * @param onAction (questionId, action, arg) — action یکی از
 *   remove | up | down | duplicate | score | layout
 */
@Composable
internal fun ExamQuestionManagerSheet(
    rows: List<QuestionRow>,
    totalScore: String,
    onAction: (String, String, String?) -> Unit,
    onJumpTo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf<QuestionRow?>(null) }
    var editScore by remember { mutableStateOf<QuestionRow?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "مدیریت سؤال‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("جمع بارم: $totalScore", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "بستن") }
                }

                if (rows.isEmpty()) {
                    Text("این آزمون هنوز سؤالی ندارد.", Modifier.padding(vertical = 24.dp))
                } else {
                    // نوارِ شمارهٔ سؤال — جایگزینِ بومیِ questionNumberStrip
                    LazyRow(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(rows, key = { it.id }) { row ->
                            AssistChip(
                                onClick = { onJumpTo(row.id) },
                                label = { Text(toPersianDigits(row.index.toString())) }
                            )
                        }
                    }

                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(rows, key = { it.id }) { row ->
                            QuestionCard(
                                row = row,
                                isFirst = row.index == 1,
                                isLast = row.index == rows.size,
                                onAction = onAction,
                                onDelete = { confirmDelete = row },
                                onEditScore = { editScore = row }
                            )
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("حذف سؤال") },
            text = { Text("سؤال ${toPersianDigits(row.index.toString())} حذف شود؟ این کار برگشت‌پذیر نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    onAction(row.id, "remove", null)
                    confirmDelete = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("انصراف") } }
        )
    }

    editScore?.let { row ->
        var value by remember(row.id) { mutableStateOf(row.score) }
        AlertDialog(
            onDismissRequest = { editScore = null },
            title = { Text("بارم سؤال ${toPersianDigits(row.index.toString())}") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("بارم") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAction(row.id, "score", value.trim())
                    editScore = null
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { editScore = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun QuestionCard(
    row: QuestionRow,
    isFirst: Boolean,
    isLast: Boolean,
    onAction: (String, String, String?) -> Unit,
    onDelete: () -> Unit,
    onEditScore: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    toPersianDigits(row.index.toString()) + ". " + row.typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEditScore) {
                    Text(if (row.score.isBlank()) "بارم —" else "بارم " + toPersianDigits(row.score))
                }
            }
            if (row.preview.isNotBlank()) {
                Text(row.preview, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!isFirst) TextButton(onClick = { onAction(row.id, "up", null) }) { Text("▲ بالاتر") }
                if (!isLast) TextButton(onClick = { onAction(row.id, "down", null) }) { Text("▼ پایین‌تر") }
                TextButton(onClick = { onAction(row.id, "duplicate", null) }) { Text("⧉ کپی") }
                TextButton(onClick = onDelete) { Text("🗑 حذف") }
            }
            // چیدمان گزینه‌ها فقط برای سؤال چندگزینه‌ای معنا دارد
            if (row.type == "multiple") {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        "1row" to "یک سطر",
                        "2rows" to "دو سطر",
                        "4rows" to "چهار سطر"
                    ).forEach { (value, label) ->
                        TextButton(onClick = { onAction(row.id, "layout", value) }) { Text(label) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onAction(row.id, "addOption", null) }) { Text("+ گزینه") }
                }
            }
            if (row.type == "matching") {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onAction(row.id, "addPair", null) }) { Text("+ جفت") }
                }
            }
        }
    }
}

/** ارقام فارسی — همان قراردادِ نمایشیِ خودِ آزمون‌ساز. */
internal fun toPersianDigits(value: String): String {
    val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return buildString {
        value.forEach { ch -> append(if (ch in '0'..'9') fa[ch - '0'] else ch) }
    }
}
