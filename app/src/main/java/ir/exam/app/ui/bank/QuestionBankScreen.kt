package ir.exam.app.ui.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.ui.app.NeumorphicPanel
import ir.exam.app.ui.builder.BankCategoryOption
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.math.NativeMathText

@Composable
fun QuestionBankScreen(
    onUseInExam: (BankQuestionOption) -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember(context.applicationContext) {
        QuestionBankViewModel(context.applicationContext)
    }
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<BankQuestionOption?>(null) }
    var deleting by remember { mutableStateOf<BankQuestionOption?>(null) }
    var newCategory by remember { mutableStateOf(false) }
    var deleteCategory by remember { mutableStateOf<BankCategoryOption?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("جست‌وجوی متن یا درس") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = state.categoryId == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("همه (${state.questions.size})") }
                )
                OutlinedButton(onClick = { newCategory = true }) { Text("دسته جدید") }
                OutlinedButton(onClick = viewModel::load, enabled = !state.loading) { Text("تازه‌سازی") }
            }
        }
        if (state.categories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    state.categories.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEach { category ->
                                FilterChip(
                                    selected = state.categoryId == category.id,
                                    onClick = { viewModel.selectCategory(category.id) },
                                    label = { Text("${category.name} (${category.count})") }
                                )
                            }
                        }
                    }
                    state.categoryId?.let { id ->
                        state.categories.firstOrNull { it.id == id }?.let { category ->
                            TextButton(onClick = { deleteCategory = category }) {
                                Text("حذف دسته «${category.name}»")
                            }
                        }
                    }
                }
            }
        }
        if (state.loading || state.actionLoading) item { CircularProgressIndicator() }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        when {
            !state.loading && state.visibleQuestions.isEmpty() -> item {
                Text("سؤالی با این فیلتر یافت نشد.")
            }
            else -> items(state.visibleQuestions, key = { it.id }) { item ->
                NeumorphicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 22.dp,
                    depth = 9.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        NativeMathText(item.question.text.ifBlank { "بدون متن" })
                        Text(
                            "${item.subject.orEmpty().ifBlank { "بدون درس" }} · ${item.question.type.faLabel()} · بارم ${item.question.score}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            if (item.categoryNames.isEmpty()) "بدون دسته"
                            else item.categoryNames.joinToString("، "),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { onUseInExam(item) }) { Text("افزودن به آزمون") }
                            OutlinedButton(onClick = { editing = item }) { Text("ویرایش") }
                            TextButton(onClick = { deleting = item }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }

    if (newCategory) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newCategory = false },
            title = { Text("دسته جدید") },
            text = { OutlinedTextField(name, { name = it.take(100) }, label = { Text("نام دسته") }) },
            confirmButton = {
                Button(
                    onClick = { viewModel.addCategory(name); newCategory = false },
                    enabled = name.isNotBlank()
                ) { Text("ساخت") }
            },
            dismissButton = { TextButton(onClick = { newCategory = false }) { Text("انصراف") } }
        )
    }

    deleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteCategory = null },
            title = { Text("حذف دسته") },
            text = { Text("دسته «${category.name}» چگونه حذف شود؟") },
            confirmButton = {
                Column {
                    Button(onClick = {
                        viewModel.deleteCategory(category.id, false)
                        deleteCategory = null
                    }) { Text("دسته حذف شود؛ سؤال‌ها بمانند") }
                    TextButton(onClick = {
                        viewModel.deleteCategory(category.id, true)
                        deleteCategory = null
                    }) { Text("سؤال‌های بدون دسته دیگر هم حذف شوند") }
                }
            },
            dismissButton = { TextButton(onClick = { deleteCategory = null }) { Text("انصراف") } }
        )
    }

    editing?.let { item ->
        BankQuestionEditorDialog(
            item = item,
            categories = state.categories,
            onDismiss = { editing = null },
            onSave = { question, subject, categoryIds ->
                viewModel.updateQuestion(item, question, subject, categoryIds)
                editing = null
            }
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف سؤال") },
            text = { Text("این سؤال از بانک حذف شود؟ این کار به آزمون‌های قبلی آسیبی نمی‌زند.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteQuestion(item.id); deleting = null }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun BankQuestionEditorDialog(
    item: BankQuestionOption,
    categories: List<BankCategoryOption>,
    onDismiss: () -> Unit,
    onSave: (QuestionDraft, String, Set<Long>) -> Unit
) {
    var question by remember(item.id) { mutableStateOf(item.question) }
    var subject by remember(item.id) { mutableStateOf(item.subject.orEmpty()) }
    var selectedCategories by remember(item.id) { mutableStateOf(item.categoryIds) }
    var score by remember(item.id) { mutableStateOf(item.question.score.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش سؤال بانک") },
        text = {
            LazyColumn(
                Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("نوع سؤال: ${question.type.faLabel()}")
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it.take(250) },
                        label = { Text("درس") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = question.text,
                        onValueChange = { question = question.copy(text = it.take(10_000)) },
                        label = { Text("متن سؤال") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                        label = { Text("بارم") }
                    )
                }
                item { TypeSpecificBankFields(question) { question = it } }
                if (categories.isNotEmpty()) {
                    item {
                        Text("دسته‌ها")
                        categories.forEach { category ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = category.id in selectedCategories,
                                    onCheckedChange = { checked ->
                                        selectedCategories = if (checked) {
                                            selectedCategories + category.id
                                        } else {
                                            selectedCategories - category.id
                                        }
                                    }
                                )
                                Text(category.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        question.copy(score = score.toDoubleOrNull()?.coerceAtLeast(0.0) ?: question.score),
                        subject,
                        selectedCategories
                    )
                },
                enabled = question.text.isNotBlank()
            ) { Text("ذخیره تغییرات") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun TypeSpecificBankFields(
    question: QuestionDraft,
    onChange: (QuestionDraft) -> Unit
) {
    when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            question.options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = question.correctIndex == index,
                        onClick = { onChange(question.copy(correctIndex = index)) }
                    )
                    OutlinedTextField(
                        value = option,
                        onValueChange = { value ->
                            onChange(question.copy(options = question.options.toMutableList().also { it[index] = value }))
                        },
                        label = { Text("گزینه ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        QuestionType.TRUE_FALSE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = question.expectedText == "true",
                onClick = { onChange(question.copy(expectedText = "true")) },
                label = { Text("صحیح") }
            )
            FilterChip(
                selected = question.expectedText == "false",
                onClick = { onChange(question.copy(expectedText = "false")) },
                label = { Text("غلط") }
            )
        }
        QuestionType.FILL_BLANK -> Column {
            OutlinedTextField(
                value = question.expectedText,
                onValueChange = { onChange(question.copy(expectedText = it)) },
                label = { Text("پاسخ‌های قابل قبول با |") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("حساس به حروف", Modifier.weight(1f))
                Switch(
                    checked = question.caseSensitive,
                    onCheckedChange = { onChange(question.copy(caseSensitive = it)) }
                )
            }
        }
        QuestionType.NUMERIC -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = question.expectedNumber,
                onValueChange = { onChange(question.copy(expectedNumber = it)) },
                label = { Text("پاسخ عددی") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = question.tolerance,
                onValueChange = { onChange(question.copy(tolerance = it)) },
                label = { Text("تلورانس") },
                modifier = Modifier.weight(1f)
            )
        }
        QuestionType.MATCHING -> {
            Text("متن و جفت‌های matching حفظ می‌شوند؛ ویرایش ساختاری کامل پس از افزودن به آزمون انجام می‌شود.")
        }
        QuestionType.ESSAY -> Text("سؤال تشریحی پاسخ کلیدی اجباری ندارد.")
    }
}

private fun QuestionType.faLabel(): String = when (this) {
    QuestionType.ESSAY -> "تشریحی"
    QuestionType.MULTIPLE_CHOICE -> "چندگزینه‌ای"
    QuestionType.TRUE_FALSE -> "صحیح/غلط"
    QuestionType.FILL_BLANK -> "جای خالی"
    QuestionType.NUMERIC -> "عددی"
    QuestionType.MATCHING -> "جورکردنی"
}
