package ir.exam.app.ui.grading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import ir.exam.app.domain.grading.CodeGrading
import ir.exam.app.domain.model.GradingQuestion

/**
 * V356 — چک‌لیست تصحیح کد، فاز صفر.
 *
 * معلم خودش کد را می‌خواند یا در محیط خودش اجرا می‌کند و نتیجهٔ هر مورد
 * آزمون را علامت می‌زند. **این صفحه هیچ کدی را اجرا نمی‌کند.** نمره فقط
 * پیشنهاد می‌شود و اعمال و ذخیرهٔ آن با معلم است.
 */
@Composable
internal fun CodeChecklistCard(
    question: GradingQuestion,
    questionIndex: Int,
    answerId: String,
    state: GradingUiState,
    viewModel: GradingViewModel,
    onApply: (Double) -> Unit
) {
    val tests = viewModel.codeTestsFor(questionIndex)
    if (tests.isEmpty()) return
    val spec = viewModel.codeSpecFor(questionIndex)
    val checks = viewModel.codeChecksFor(answerId, questionIndex)
    val result = CodeGrading.scoreFromTests(tests.map { it.weighted }, checks, question.score)
    var showReference by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "چک‌لیست کد — ${CodeGrading.summary(tests.map { it.weighted }, checks)}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text("کد اجرا نمی‌شود", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "قبول ${PersianDigits.convert(result.passedCount)} از ${PersianDigits.convert(result.totalCount)}" +
                    " · بررسی‌شده ${PersianDigits.convert(result.checkedCount)}" +
                    " · پیشنهاد: ${PersianDigits.convert(result.suggested)} از ${PersianDigits.convert(question.score)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            tests.forEach { test ->
                CodeTestRowItem(
                    label = test.label.ifBlank { "مورد" },
                    weight = test.weight,
                    hidden = test.hidden,
                    input = test.input,
                    expected = test.expected,
                    passed = checks[test.id]?.passed ?: false,
                    options = spec?.options ?: CodeGrading.Options(),
                    onSave = { passed, actual ->
                        viewModel.saveCodeCheck(answerId, questionIndex, test.id, passed, actual)
                    }
                )
            }
            if (!spec?.reference.isNullOrBlank()) {
                TextButton(onClick = { showReference = !showReference }) {
                    Text(if (showReference) "بستن پاسخ مرجع" else "نمایش پاسخ مرجع (${spec?.language})")
                }
                if (showReference) Text(
                    spec?.reference.orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { onApply(result.suggested) }) { Text("اعمال پیشنهاد") }
            }
        }
    }
}

@Composable
private fun CodeTestRowItem(
    label: String,
    weight: Double,
    hidden: Boolean,
    input: String,
    expected: String,
    passed: Boolean,
    options: CodeGrading.Options,
    onSave: (Boolean, String) -> Unit
) {
    var actual by remember { mutableStateOf("") }
    val verdict = if (actual.isBlank()) "" else
        if (CodeGrading.outputMatches(expected, actual, options)) "✓ مطابق" else "✗ نامطابق"
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = passed, onCheckedChange = { onSave(it, actual) })
            Text(
                "$label (وزن ${PersianDigits.convert(weight)})" + if (hidden) " · پنهان" else "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (verdict.isNotEmpty()) Text(verdict, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            "in: ${input.ifBlank { "—" }}  →  expected: $expected",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = actual,
            onValueChange = { actual = it },
            label = { Text("خروجی واقعی (اختیاری)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { onSave(passed, actual) }) { Text("ثبت خروجی") }
    }
}

/**
 * ویرایشگر موارد آزمون. قالب هر خط:
 *   برچسب | وزن | ورودی => خروجی
 * ذخیره، چک‌لیست ثبت‌شدهٔ همان سؤال را پاک می‌کند؛ به معلم هشدار داده می‌شود.
 */
@Composable
internal fun CodeSpecEditorDialog(
    state: GradingUiState,
    questionCount: Int,
    onDismiss: () -> Unit,
    onSave: (Int, String, String, List<CodeGrading.TestCase>) -> Unit
) {
    var question by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf("python") }
    var reference by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun fill(qi: Int) {
        val spec = state.codeSpecs.firstOrNull { it.questionIndex == qi }
        language = spec?.language ?: "python"
        reference = spec?.reference.orEmpty()
        text = CodeGrading.formatTests(
            state.codeTests.filter { it.questionIndex == qi }
                .map { CodeGrading.TestCase(it.label, it.input, it.expected, it.weight, it.hidden) }
        )
        error = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("موارد آزمون کد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(enabled = question > 0, onClick = { question--; fill(question) }) { Text("قبلی") }
                    Text(
                        "سؤال ${PersianDigits.convert(question + 1)} از ${PersianDigits.convert(questionCount)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(enabled = question < questionCount - 1, onClick = { question++; fill(question) }) { Text("بعدی") }
                }
                OutlinedTextField(
                    value = language, onValueChange = { language = it },
                    label = { Text("زبان") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reference, onValueChange = { reference = it },
                    label = { Text("پاسخ مرجع (فقط شما می‌بینید)") },
                    minLines = 3, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("هر خط: برچسب | وزن | ورودی => خروجی") },
                    minLines = 4, modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "برای مورد پنهان «پنهان» بنویسید. حداکثر ۴۰ مورد. " +
                        "توجه: ذخیره، چک‌لیست ثبت‌شدهٔ همین سؤال را پاک می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = CodeGrading.parseTests(text)
                if (parsed.errors.isNotEmpty()) {
                    val e = parsed.errors.first()
                    error = "خط ${PersianDigits.convert(e.line)}: ${e.message}"
                    return@TextButton
                }
                if (reference.length > CodeGrading.MAX_REFERENCE) {
                    error = "پاسخ مرجع بیش از حد بلند است."
                    return@TextButton
                }
                if (language.isBlank()) { error = "نام زبان را بنویسید."; return@TextButton }
                error = null
                onSave(question, language.trim(), reference, parsed.tests)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
