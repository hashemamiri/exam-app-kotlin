package ir.exam.app.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.exam.app.domain.model.StudentAnswerReview
import ir.exam.app.domain.model.StudentAnswerReviewQuestion
import ir.exam.app.ui.math.NativeMathText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun StudentResultsScreen(viewModel: StudentResultsViewModel = remember { StudentResultsViewModel() }) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val rows = mutableListOf(listOf("آزمون", "درس", "نمره", "از", "درصد", "بازخورد"))
            state.grades.forEach {
                rows += listOf(
                    it.title,
                    it.subject,
                    it.grade.toString(),
                    it.total.toString(),
                    "%.2f".format(it.percent),
                    it.feedback
                )
            }
            val csv = "\uFEFF" + rows.joinToString("\n") { row ->
                row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
        }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("نتایج و پاسخ‌های من", style = MaterialTheme.typography.headlineSmall) }
        if (state.loading || state.detailLoading) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.grades.isNotEmpty()) {
            item {
                val avg = state.grades.map { it.percent }.average()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("میانگین کل: %.1f%%".format(avg))
                    Button(onClick = { exporter.launch("my-grades.csv") }) { Text("خروجی Excel/CSV") }
                }
            }
            items(state.grades, key = { it.examId + it.submittedAt }) { grade ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(grade.title, style = MaterialTheme.typography.titleMedium)
                        Text("${grade.subject} · ${grade.grade} از ${grade.total} · %.1f%%".format(grade.percent))
                        if (grade.feedback.isNotBlank()) Text("بازخورد: ${grade.feedback}")
                    }
                }
            }
        } else if (!state.loading) item { Text("هنوز نمره‌ای ثبت نشده است.") }

        item { Text("پاسخ‌های ثبت‌شده", style = MaterialTheme.typography.titleMedium) }
        if (!state.loading && state.answers.isEmpty()) item { Text("هنوز پاسخی ثبت نشده است.") }
        items(state.answers, key = { it.id }) { answer ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(answer.title, style = MaterialTheme.typography.titleMedium)
                    if (answer.subject.isNotBlank()) Text(answer.subject)
                    Text(if (answer.graded) "تصحیح‌شده" else "در انتظار تصحیح")
                    if (answer.graded) Text("نمره: ${answer.totalGrade} از ${answer.totalScore}")
                    OutlinedButton(
                        onClick = { viewModel.openAnswer(answer.id) },
                        enabled = !state.detailLoading
                    ) { Text("مشاهده سؤال‌ها و پاسخ‌ها") }
                }
            }
        }
    }

    state.selectedAnswer?.let { detail ->
        AnswerReviewDialog(detail = detail, onDismiss = viewModel::closeAnswer)
    }
}

@Composable
private fun AnswerReviewDialog(detail: StudentAnswerReview, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(detail.title) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    if (detail.graded) Text("نمره: ${detail.totalGrade} از ${detail.totalScore}")
                    else Text("این پاسخ هنوز تصحیح نشده است؛ کلید پاسخ نمایش داده نمی‌شود.")
                    if (detail.feedback.isNotBlank()) Text("بازخورد: ${detail.feedback}")
                }
                items(detail.questions, key = { it.id + it.index }) { question ->
                    AnswerQuestionCard(question, detail.graded)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun AnswerQuestionCard(question: StudentAnswerReviewQuestion, graded: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("سؤال ${question.index + 1}", style = MaterialTheme.typography.titleMedium)
                if (graded && question.earnedScore != null) {
                    Text("${question.earnedScore} از ${question.score}")
                } else Text("بارم ${question.score}")
            }
            NativeMathText(question.text)
            Text("پاسخ شما: ${question.displayResponse()}")
            question.responseImages.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "تصویر پاسخ سؤال ${question.index + 1}",
                    modifier = Modifier.size(180.dp)
                )
            }
            question.correctAnswer?.let { Text("پاسخ صحیح: $it", color = MaterialTheme.colorScheme.primary) }
            question.explanation?.let { NativeMathText("توضیح: $it") }
        }
    }
}

private fun StudentAnswerReviewQuestion.displayResponse(): String {
    val value = response ?: return "بدون پاسخ"
    if (value is JsonNull) return "بدون پاسخ"
    return when {
        type in setOf("multiple", "multiple_choice", "multiplechoice") -> {
            val index = value.jsonPrimitive.intOrNull
            index?.let { options.getOrNull(it) } ?: value.primitiveText()
        }
        type in setOf("truefalse", "true_false") -> value.jsonPrimitive.booleanOrNull?.let {
            if (it) "صحیح" else "غلط"
        } ?: value.primitiveText()
        type in setOf("matching", "match") -> displayMatchingResponse(value)
        else -> when (value) {
            is JsonArray -> value.joinToString("، ") { it.primitiveText() }.ifBlank { "بدون پاسخ" }
            else -> value.primitiveText().ifBlank { "بدون پاسخ" }
        }
    }
}

private fun StudentAnswerReviewQuestion.displayMatchingResponse(value: JsonElement): String {
    val map = when (value) {
        is JsonObject -> value
        is JsonPrimitive -> runCatching {
            Json.parseToJsonElement(value.content).let { it as? JsonObject }
        }.getOrNull()
        else -> null
    } ?: return value.primitiveText().ifBlank { "بدون پاسخ" }
    return map.entries
        .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
        .joinToString("، ") { (left, right) ->
            val leftIndex = left.toIntOrNull() ?: 0
            val rightIndex = right.jsonPrimitive.intOrNull ?: 0
            val leftText = leftItems.getOrNull(leftIndex).orEmpty().ifBlank { (leftIndex + 1).toString() }
            val rightText = rightItems.getOrNull(rightIndex).orEmpty().ifBlank { (rightIndex + 1).toString() }
            "$leftText ← $rightText"
        }.ifBlank { "بدون پاسخ" }
}

private fun JsonElement.primitiveText(): String = when (this) {
    is JsonPrimitive -> contentOrNull.orEmpty()
    else -> toString()
}
