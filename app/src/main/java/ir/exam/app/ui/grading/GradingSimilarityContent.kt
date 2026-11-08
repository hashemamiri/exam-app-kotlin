package ir.exam.app.ui.grading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.domain.model.GradingSubmission
import ir.exam.app.domain.grading.Similarity
import ir.exam.app.domain.grading.SimilarityReviewRow
import ir.exam.app.domain.grading.SimilarityReviewStatus
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * V355 — تب «شباهت پاسخ‌ها» در صفحهٔ تصحیح.
 *
 * شباهت فقط پرچم هشدار است؛ هیچ نمره‌ای خودکار کم نمی‌شود. محاسبه روی
 * دستگاه معلم انجام می‌شود و کد دانش‌آموز هرگز اجرا نمی‌شود.
 */
@Composable
internal fun SimilarityContent(state: GradingUiState, viewModel: GradingViewModel) {
    var confirming by remember { mutableStateOf<Similarity.Hit?>(null) }
    var comparing by remember { mutableStateOf<Similarity.Hit?>(null) }
    var keywordsOpen by remember { mutableStateOf(false) }
    val exam = state.selectedExam ?: return
    val nameOf = state.submissions.associate { it.id to it.studentName.ifBlank { "دانش‌آموز" } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "شباهت فقط هشدار است و نمره‌ای را کم نمی‌کند. پاسخ کوتاه‌تر از " +
                "${PersianDigits.convert(Similarity.MIN_TOKENS)} واژه مقایسه نمی‌شود. " +
                "کد با نام متغیر و توضیح متفاوت هم شناسایی می‌شود؛ کد اجرا نمی‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0.6 to "حساس", 0.75 to "متعادل", 0.9 to "سخت‌گیر").forEach { (v, label) ->
                FilterChip(
                    selected = state.similarityThreshold == v,
                    onClick = { viewModel.setSimilarityThreshold(v) },
                    label = { Text(label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = viewModel::loadSimilarity, enabled = !state.actionLoading) { Text("تازه‌سازی") }
            OutlinedButton(onClick = { keywordsOpen = true }) { Text("کلیدواژه‌ها") }
        }
        Text(
            "${PersianDigits.convert(state.similarityHits.size)} جفت مشکوک در " +
                "${PersianDigits.convert(state.similarityUnits)} واحد مقایسه",
            style = MaterialTheme.typography.labelLarge
        )
        if (state.similarityHits.isEmpty()) {
            Text("با این آستانه، پاسخ شبیهی پیدا نشد.")
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.similarityHits.take(200), key = { it.pairKey + "#" + it.questionIndex + "#" + it.partKey }) { hit ->
                val rev = state.similarityReviews[SimilarityReviewRow.lookupKey(hit.pairKey, hit.questionIndex, hit.partKey)]
                HitCard(
                    hit = hit,
                    aName = nameOf[hit.aId] ?: "دانش‌آموز",
                    bName = nameOf[hit.bId] ?: "دانش‌آموز",
                    review = rev,
                    enabled = !state.actionLoading,
                    onCompare = { comparing = hit },
                    onChecked = { viewModel.saveSimilarityReview(hit, "checked", "") },
                    onConfirm = { confirming = hit }
                )
            }
        }
    }

    confirming?.let { hit ->
        ConfirmCheatingDialog(
            onDismiss = { confirming = null },
            onConfirm = { note ->
                viewModel.saveSimilarityReview(hit, "confirmed", note)
                confirming = null
            }
        )
    }

    comparing?.let { hit ->
        val a = state.submissions.firstOrNull { it.id == hit.aId }
        val b = state.submissions.firstOrNull { it.id == hit.bId }
        AlertDialog(
            onDismissRequest = { comparing = null },
            title = { Text("مقایسه — شباهت ${PersianDigits.convert((hit.score * 100).toInt())}٪") },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(a, b).forEach { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(s?.studentName ?: "دانش‌آموز", style = MaterialTheme.typography.titleSmall)
                            Text(
                                responseTextOf(s, hit),
                                style = MaterialTheme.typography.bodySmall
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { comparing = null }) { Text("بستن") } }
        )
    }

    if (keywordsOpen) {
        KeywordEditorDialog(
            state = state,
            questionCount = exam.questions.size,
            onDismiss = { keywordsOpen = false },
            onSave = { qi, items ->
                viewModel.saveEssayKeywords(qi, "", items)
                keywordsOpen = false
            }
        )
    }
}

private fun responseTextOf(submission: GradingSubmission?, hit: Similarity.Hit): String {
    val el = submission?.responses?.getOrNull(hit.questionIndex) ?: return "—"
    val raw = when (el) {
        is JsonPrimitive -> el.contentOrNull.orEmpty()
        is JsonObject -> (el["text"] as? JsonPrimitive)?.contentOrNull
            ?: (el["answer"] as? JsonPrimitive)?.contentOrNull
            ?: el.toString()
        else -> el.toString()
    }
    return if (hit.partKey.isNotEmpty()) {
        Similarity.partText(raw, hit.partKey, true).ifBlank { "—" }
    } else raw.ifBlank { "—" }
}

@Composable
private fun HitCard(
    hit: Similarity.Hit,
    aName: String,
    bName: String,
    review: SimilarityReviewRow?,
    enabled: Boolean,
    onCompare: () -> Unit,
    onChecked: () -> Unit,
    onConfirm: () -> Unit
) {
    val status = review?.status ?: SimilarityReviewStatus.OPEN
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$aName ↔ $bName", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (status) {
                        SimilarityReviewStatus.CONFIRMED -> MaterialTheme.colorScheme.error
                        SimilarityReviewStatus.CHECKED -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            val where = buildString {
                append("سؤال ${PersianDigits.convert(hit.questionIndex + 1)}")
                if (hit.partKey.isNotEmpty()) append(" · زیربخش")
                if (hit.kind == "code") append(" · کد")
            }
            Text(
                "$where — شباهت ${PersianDigits.convert((hit.score * 100).toInt())}٪ " +
                    "(اشتراک ${PersianDigits.convert((hit.jaccard * 100).toInt())}٪، " +
                    "پوشش ${PersianDigits.convert((hit.containment * 100).toInt())}٪)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            review?.note?.let { Text("یادداشت: $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = false, enabled = enabled, onClick = onCompare, label = { Text("مقایسه") })
                FilterChip(selected = false, enabled = enabled, onClick = onChecked, label = { Text("مشکلی نبود") })
                FilterChip(selected = false, enabled = enabled, onClick = onConfirm, label = { Text("تأیید تخلف") })
            }
        }
    }
}

@Composable
private fun ConfirmCheatingDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var note by remember { mutableStateOf("") }
    val ok = note.trim().length >= 3
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأیید تخلف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "این کار نمره را تغییر نمی‌دهد؛ فقط تصمیم شما ثبت می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیح (دست‌کم ۳ نویسه)") },
                    isError = note.isNotBlank() && !ok,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onConfirm(note) }) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/**
 * ویرایشگر کلیدواژه. قالب هر خط: «کلیدواژه» یا «کلیدواژه = وزن».
 * برای چند بدل «|» بگذارید. خروجی فقط پیشنهاد نمره می‌سازد.
 */
@Composable
private fun KeywordEditorDialog(
    state: GradingUiState,
    questionCount: Int,
    onDismiss: () -> Unit,
    onSave: (Int, List<Pair<String, Double>>) -> Unit
) {
    var question by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun fill(qi: Int) {
        text = state.essayKeywords.filter { it.questionIndex == qi && it.partKey.isEmpty() }
            .joinToString("\n") { if (it.weight == 1.0) it.keyword else "${it.keyword} = ${PersianDigits.latin(it.weight.toString())}" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کلیدواژه‌های تصحیح تشریحی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(enabled = question > 0, onClick = {
                        question--; fill(question); error = null
                    }) { Text("قبلی") }
                    Text(
                        "سؤال ${PersianDigits.convert(question + 1)} از ${PersianDigits.convert(questionCount)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(enabled = question < questionCount - 1, onClick = {
                        question++; fill(question); error = null
                    }) { Text("بعدی") }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("هر خط یک کلیدواژه") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
                Text(
                    "وزن پیش‌فرض ۱ است. برای چند بدل «|» بگذارید. حداکثر ۶۰ کلیدواژه.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val items = mutableListOf<Pair<String, Double>>()
                text.lines().forEachIndexed { idx, raw ->
                    val line = raw.trim()
                    if (line.isEmpty()) return@forEachIndexed
                    var kw = line
                    var w = 1.0
                    val eq = line.lastIndexOf('=')
                    if (eq > 0) {
                        val maybe = PersianDigits.latin(line.substring(eq + 1).trim()).toDoubleOrNull()
                        if (maybe != null && maybe > 0) { w = maybe; kw = line.substring(0, eq).trim() }
                    }
                    if (kw.isEmpty() || kw.length > 120) {
                        error = "کلیدواژهٔ خط ${PersianDigits.convert(idx + 1)} معتبر نیست."
                        return@TextButton
                    }
                    items.add(kw to w)
                }
                if (items.size > 60) { error = "بیش از ۶۰ کلیدواژه مجاز نیست."; return@TextButton }
                error = null
                onSave(question, items)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
