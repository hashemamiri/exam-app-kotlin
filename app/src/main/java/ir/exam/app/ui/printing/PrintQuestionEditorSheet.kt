package ir.exam.app.ui.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * V88.1 — ویرایشگرِ بومیِ سؤال برای آزمون‌سازِ چاپی.
 *
 * سؤال‌ها همچنان در `questions` جاوااسکریپتِ صفحه زندگی می‌کنند تا موتورِ
 * چاپ و پیش‌نمایش دست‌نخورده بماند؛ این پنجره از راهِ پل‌های
 * `__qmfQuestionDetail` / `__qmfQuestionEdit` / `__qmfOptionEdit` /
 * `__qmfOptionCount` / `__qmfPairEdit` می‌خواند و می‌نویسد. هر نوشتن از
 * تابعِ خودِ صفحه می‌گذرد، پس `renderAll` مثلِ همیشه اجرا می‌شود.
 */

/** یک گزینهٔ چندگزینه‌ای یا صحیح/غلط. */
data class PrintOptionRow(val text: String, val correct: Boolean)

/** یک جفتِ جورکردنی. */
data class PrintPairRow(val left: String, val right: String)

/** عکسِ فوریِ یک سؤال، همان‌طور که پل می‌دهد. */
data class PrintQuestionDetail(
    val id: String = "",
    val type: String = "long",
    val text: String = "",
    val score: String = "",
    val optionsLayout: String = "2rows",
    val answerLines: Int? = null,
    val answerStyle: String = "lined",
    val answerLineHeightCm: Double? = null,
    val answer: String = "",
    val options: List<PrintOptionRow> = emptyList(),
    val pairs: List<PrintPairRow> = emptyList()
)

private val detailJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** خروجیِ `__qmfQuestionDetail` را می‌خواند. `{}` یعنی سؤال پیدا نشد. */
fun parsePrintQuestionDetail(raw: String?): PrintQuestionDetail? {
    val body = raw?.trim().orEmpty()
    if (body.isEmpty() || body == "{}" || body == "\"{}\"") return null
    return runCatching {
        val o: JsonObject = detailJson.parseToJsonElement(body).jsonObject
        fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull.orEmpty()
        PrintQuestionDetail(
            id = str("id"),
            type = str("type").ifBlank { "long" },
            text = str("text"),
            score = str("score"),
            optionsLayout = str("optionsLayout").ifBlank { "2rows" },
            answerLines = o["answerLines"]?.jsonPrimitive?.intOrNull,
            answerStyle = str("answerStyle").ifBlank { "lined" },
            answerLineHeightCm = o["answerLineHeightCm"]?.jsonPrimitive?.doubleOrNull,
            answer = str("answer"),
            options = o["options"]?.jsonArray?.map { el ->
                val it = el.jsonObject
                PrintOptionRow(
                    text = it["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    correct = it["correct"]?.jsonPrimitive?.booleanOrNull ?: false
                )
            }.orEmpty(),
            pairs = o["pairs"]?.jsonArray?.map { el ->
                val it = el.jsonObject
                PrintPairRow(
                    left = it["left"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    right = it["right"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }.orEmpty()
        )
    }.getOrNull()
}

/** برچسبِ فارسیِ هر نوع سؤال. */
fun printQuestionTypeLabel(type: String): String = when (type) {
    "multiple" -> "چندگزینه‌ای"
    "truefalse" -> "صحیح/غلط"
    "fill" -> "جای خالی"
    "numeric" -> "عددی"
    "matching" -> "جورکردنی"
    else -> "تشریحی"
}

/** آیا این نوع، فضای پاسخ دارد؟ */
fun printTypeHasAnswerSpace(type: String): Boolean = type == "long" || type == "fill"

@Composable
fun PrintQuestionEditorSheet(
    detail: PrintQuestionDetail,
    index: Int,
    onEditField: (field: String, value: String) -> Unit,
    onEditOption: (index: Int, field: String, value: String) -> Unit,
    onOptionCount: (action: String, index: Int) -> Unit,
    onEditPair: (index: Int, side: String, value: String) -> Unit,
    onOpenFormula: () -> Unit,
    onOpenFigureTool: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // مقادیرِ متنی محلی نگه داشته می‌شوند تا تایپ روان بماند؛ هر تغییر
    // بی‌درنگ به صفحه هم می‌رود، مثلِ ویرایشگرِ آنلاین.
    var text by remember(detail.id) { mutableStateOf(detail.text) }
    var score by remember(detail.id) { mutableStateOf(detail.score) }
    var answer by remember(detail.id) { mutableStateOf(detail.answer) }
    var lines by remember(detail.id) { mutableStateOf(detail.answerLines?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$index",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    "  " + printQuestionTypeLabel(detail.type),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- متنِ سؤال (بومی؛ کاربر گفت لازم نیست WebView بماند) ----
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; onEditField("text", it) },
                    label = { Text("متن سؤال") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- ابزارهای درج، همان‌های بومیِ موجود ----
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(onClick = onOpenFormula, label = { Text("فرمول") })
                    AssistChip(onClick = { onOpenFigureTool("figure") }, label = { Text("شکل") })
                    AssistChip(onClick = { onOpenFigureTool("graph") }, label = { Text("نمودار") })
                    AssistChip(onClick = { onOpenFigureTool("table") }, label = { Text("جدول") })
                }

                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it; onEditField("score", it) },
                    label = { Text("بارم") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- گزینه‌ها ----
                if (detail.type == "multiple" || detail.type == "truefalse") {
                    Text("گزینه‌ها", style = MaterialTheme.typography.titleSmall)
                    detail.options.forEachIndexed { i, option ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RadioButton(
                                selected = option.correct,
                                onClick = { onEditOption(i, "correct", "true") }
                            )
                            OutlinedTextField(
                                value = option.text,
                                onValueChange = { onEditOption(i, "text", it) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            // صحیح/غلط دو گزینهٔ ثابت دارد و حذف معنا ندارد
                            if (detail.type == "multiple") {
                                IconButton(onClick = { onOptionCount("remove", i) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "حذف گزینه",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    if (detail.type == "multiple") {
                        TextButton(onClick = { onOptionCount("add", 0) }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("  افزودن گزینه")
                        }
                    }
                }

                // ---- جورکردنی ----
                if (detail.type == "matching") {
                    Text("جفت‌ها", style = MaterialTheme.typography.titleSmall)
                    detail.pairs.forEachIndexed { i, pair ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = pair.left,
                                onValueChange = { onEditPair(i, "left", it) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text("↔")
                            OutlinedTextField(
                                value = pair.right,
                                onValueChange = { onEditPair(i, "right", it) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onOptionCount("removePair", i) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "حذف جفت",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    TextButton(onClick = { onOptionCount("addPair", 0) }) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("  افزودن جفت")
                    }
                }

                // ---- پاسخِ عددی ----
                if (detail.type == "numeric") {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it; onEditField("answer", it) },
                        label = { Text("پاسخ صحیح") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- فضای پاسخ ----
                if (printTypeHasAnswerSpace(detail.type)) {
                    OutlinedTextField(
                        value = lines,
                        onValueChange = { lines = it; onEditField("answerLines", it) },
                        label = { Text("تعداد خطوط پاسخ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("بستن") } }
    )
}
