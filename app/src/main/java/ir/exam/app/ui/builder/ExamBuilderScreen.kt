package ir.exam.app.ui.builder

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.data.repository.ExamPackageCodec
import ir.exam.app.domain.model.WalletRules
import ir.exam.app.ui.image.QuestionMediaEditor
import ir.exam.app.ui.math.ExistingFormulaEditor
import ir.exam.app.ui.math.FormulaEditorDialog
import ir.exam.app.ui.math.NativeMathText
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamBuilderScreen(
    viewModel: ExamBuilderViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var radialMenuOpen by rememberSaveable { mutableStateOf(false) }
    var bankDialogOpen by rememberSaveable { mutableStateOf(false) }
    var settingsExpanded by rememberSaveable { mutableStateOf(true) }
    var expandedQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSave by remember { mutableStateOf(false) }
    var previewQuestion by remember { mutableStateOf<QuestionDraft?>(null) }
    var previewAll by remember { mutableStateOf(false) }
    val questionPrefaceCount = 2 + if (state.importedBy != null) 1 else 0

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use(::readBuilderImportLimited)
                    ?: error("فایل آزمون خوانده نشد.")
            }.mapCatching(ExamPackageCodec::decode)
                .onSuccess { imported ->
                    viewModel.applyImport(imported)
                    expandedQuestionId = imported.questions.firstOrNull()?.id
                }
                .onFailure(viewModel::reportError)
        }
    }

    fun revealQuestion(type: QuestionType) {
        val id = viewModel.addQuestion(type)
        expandedQuestionId = id
        val questionIndex = state.questions.size
        scope.launch {
            withFrameNanos { }
            listState.animateScrollToItem(questionPrefaceCount + questionIndex, 0)
        }
    }

    LaunchedEffect(state.questions.map { it.id }) {
        if (expandedQuestionId != null && state.questions.none { it.id == expandedQuestionId }) {
            expandedQuestionId = state.questions.lastOrNull()?.id
        }
    }

    BackHandler(enabled = radialMenuOpen) { radialMenuOpen = false }
    BackHandler(enabled = !radialMenuOpen, onBack = onBack)

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(if (state.examId == null) "ساخت آزمون" else "ویرایش آزمون") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.loading) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                    FloatingActionButton(
                        onClick = { confirmSave = true },
                        modifier = Modifier.align(Alignment.CenterStart),
                        containerColor = Color(0xFF27A86B),
                        contentColor = Color.White
                    ) { Text("✓", style = MaterialTheme.typography.headlineSmall) }
                    if (!radialMenuOpen) {
                        FloatingActionButton(
                            onClick = { radialMenuOpen = true },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                    }
                }
            }
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 10.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = { settingsExpanded = !settingsExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (settingsExpanded) "بستن مشخصات آزمون" else "مشخصات آزمون")
                }
                AnimatedVisibility(
                    visible = settingsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ExamSettingsCard(state, viewModel)
                }
            }
            state.importedBy?.let { by ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "فایل آزمون وارد شد${by.takeIf(String::isNotBlank)?.let { " · سازنده: $it" } ?: ""}. پیش از ذخیره همه سؤال‌ها را بررسی کنید.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            item { AudienceCard(state, viewModel) }
            itemsIndexed(state.questions, key = { _, item -> item.id }) { index, question ->
                QuestionEditor(
                    question = question,
                    index = index,
                    total = state.questions.size,
                    expanded = expandedQuestionId == question.id,
                    onToggle = {
                        if (expandedQuestionId == question.id) {
                            expandedQuestionId = null
                        } else {
                            expandedQuestionId = question.id
                            scope.launch {
                                listState.animateScrollToItem(questionPrefaceCount + index, 0)
                            }
                        }
                    },
                    viewModel = viewModel,
                    onPreview = { previewQuestion = question }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { previewAll = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("پیش‌نمایش کامل A4")
                    }
                    Text(
                        "هزینه هر سؤال مشمول: ${PersianDigits.convert("%,d".format(java.util.Locale.US, WalletRules.QUESTION_COST_TOMAN))} تومان؛ محاسبه نهایی و کسر به‌صورت اتمیک در سرور انجام می‌شود.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.saving) CircularProgressIndicator()
                    state.uploadProgress?.let { Text(it) }
                    state.savedCode?.let { code ->
                        Text("ذخیره شد. کد آزمون: $code", color = MaterialTheme.colorScheme.primary)
                        Text(
                            "مبلغ کسرشده: ${state.chargedToman.asToman()} تومان" +
                                (state.walletBalanceToman?.let { " · مانده: ${it.asToman()} تومان" } ?: ""),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text("بازگشت به آزمون‌ها")
                        }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (radialMenuOpen) {
        BuilderRadialMenuOverlay(
            onDismiss = { radialMenuOpen = false },
            onQuestionType = { type ->
                radialMenuOpen = false
                revealQuestion(type)
            },
            onImport = {
                radialMenuOpen = false
                importLauncher.launch(arrayOf("application/octet-stream", "application/json", "text/plain"))
            },
            onBank = {
                radialMenuOpen = false
                bankDialogOpen = true
            }
        )
    }

    if (bankDialogOpen) {
        BuilderQuestionBankDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { bankDialogOpen = false },
            onAdd = { id ->
                viewModel.addFromBank(id)
                val newId = viewModel.state.value.questions.lastOrNull()?.id
                expandedQuestionId = newId
                bankDialogOpen = false
                scope.launch {
                    withFrameNanos { }
                    listState.animateScrollToItem(
                        questionPrefaceCount + state.questions.size,
                        0
                    )
                }
            }
        )
    }

    state.recoverableDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("پیش‌نویس بازیابی‌نشده") },
            text = {
                Text(
                    "یک پیش‌نویس محلی با عنوان «${draft.title.ifBlank { "بدون عنوان" }}» و " +
                        "${draft.questions.size} سؤال پیدا شد. بازیابی شود؟"
                )
            },
            confirmButton = { Button(onClick = viewModel::restoreDraft) { Text("بازیابی") } },
            dismissButton = { TextButton(onClick = viewModel::discardDraft) { Text("حذف پیش‌نویس") } }
        )
    }

    previewQuestion?.let { question ->
        QuestionPrintPreviewDialog(question = question, onDismiss = { previewQuestion = null })
    }
    if (previewAll) {
        ExamPrintPreviewDialog(state = state, onDismiss = { previewAll = false })
    }

    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text(if (state.examId == null) "تأیید ساخت آزمون" else "تأیید ذخیره تغییرات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.examId == null) {
                        Text("این آزمون ${PersianDigits.convert(state.questions.size)} سؤال دارد و حداکثر ${state.maximumChargeToman.asToman()} تومان کسر می‌شود.")
                    } else {
                        Text("سرور فقط سؤال‌های مشمول تغییر را محاسبه می‌کند. سقف این ذخیره ${state.maximumChargeToman.asToman()} تومان است.")
                    }
                    Text("ذخیره و کسر موجودی در یک تراکنش انجام می‌شود؛ اگر ذخیره شکست بخورد، مبلغی کم نخواهد شد.")
                }
            },
            confirmButton = {
                Button(onClick = { confirmSave = false; viewModel.save() }) { Text("تأیید و ذخیره") }
            },
            dismissButton = { TextButton(onClick = { confirmSave = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun ExamSettingsCard(state: ExamBuilderState, viewModel: ExamBuilderViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("مشخصات آزمون", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(state.title, viewModel::setTitle, label = { Text("عنوان آزمون") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.subject, viewModel::setSubject, label = { Text("درس") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.durationMinutes, viewModel::setDuration, label = { Text("مدت (دقیقه)") }, modifier = Modifier.fillMaxWidth())
            JalaliDateTimeField("زمان بازشدن", state.opensAtIso, viewModel::setOpensAt)
            JalaliDateTimeField("مهلت پایان", state.closesAtIso, viewModel::setClosesAt)
            OutlinedTextField(state.negativeMarking, viewModel::setNegativeMarking, label = { Text("نمره منفی") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.teacherMessage, viewModel::setTeacherMessage, label = { Text("پیام معلم") }, modifier = Modifier.fillMaxWidth())
            ToggleRow("تصادفی‌سازی سؤال‌ها", state.shuffleQuestions, viewModel::setShuffleQuestions)
            ToggleRow("تصادفی‌سازی گزینه‌ها", state.shuffleOptions, viewModel::setShuffleOptions)
            ToggleRow("اتمام تلاش در پایان زمان", state.attemptOnTimeout, viewModel::setAttemptOnTimeout)
            Text("تعداد تلاش مجاز")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { count ->
                    FilterChip(selected = state.attemptsAllowed == count, onClick = { viewModel.setAttempts(count) }, label = { Text(count.toString()) })
                }
            }
            Text("سیاست نمره")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("last" to "آخرین", "best" to "بهترین", "all" to "همه").forEach { (value, label) ->
                    FilterChip(selected = state.gradePolicy == value, onClick = { viewModel.setGradePolicy(value) }, label = { Text(label) })
                }
            }
            OutlinedTextField(state.attemptCooldown, viewModel::setAttemptCooldown, label = { Text("فاصله تلاش‌ها (دقیقه)") })
        }
    }
}

@Composable
private fun AudienceCard(state: ExamBuilderState, viewModel: ExamBuilderViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("مخاطبان آزمون", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "همه", "classes" to "کلاس‌ها", "students" to "دانش‌آموزان").forEach { (value, label) ->
                    FilterChip(selected = state.audienceMode == value, onClick = { viewModel.setAudienceMode(value) }, label = { Text(label) })
                }
            }
            if (state.audienceMode == "classes") {
                state.availableClasses.forEach { item ->
                    SelectionRow(item.name, item.id in state.audienceClasses) { viewModel.toggleAudienceClass(item.id) }
                }
            }
            if (state.audienceMode == "students") {
                state.availableStudents.forEach { item ->
                    SelectionRow(
                        label = item.name + (item.classNames?.takeIf(String::isNotBlank)?.let { " · $it" } ?: ""),
                        selected = item.id in state.audienceStudents
                    ) { viewModel.toggleAudienceStudent(item.id) }
                }
            }
        }
    }
}

@Composable
private fun BuilderQuestionBankDialog(
    state: ExamBuilderState,
    viewModel: ExamBuilderViewModel,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit
) {
    val query = state.bankQuery.trim().lowercase()
    val visible = state.bankQuestions.filter { item ->
        (state.selectedBankCategory == null || state.selectedBankCategory in item.categoryIds) &&
            (query.isBlank() || item.question.text.lowercase().contains(query) ||
                item.subject.orEmpty().lowercase().contains(query))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بانک سؤال") },
        text = {
            LazyColumn(
                Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                item {
                    OutlinedTextField(
                        state.bankQuery,
                        viewModel::setBankQuery,
                        label = { Text("جست‌وجوی سؤال یا درس") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FilterChip(
                            selected = state.selectedBankCategory == null,
                            onClick = { viewModel.selectBankCategory(null) },
                            label = { Text("همه") }
                        )
                        state.bankCategories.take(4).forEach { category ->
                            FilterChip(
                                selected = state.selectedBankCategory == category.id,
                                onClick = { viewModel.selectBankCategory(category.id) },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
                if (visible.isEmpty()) item { Text("سؤالی یافت نشد.") }
                items(visible, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            NativeMathText(item.question.text.ifBlank { "بدون متن" })
                            Text(
                                "${item.subject.orEmpty().ifBlank { "بدون درس" }} · ${item.question.type.faLabel()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = { onAdd(item.id) }) { Text("افزودن به آزمون") }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

private data class FormulaTarget(
    val field: String,
    val index: Int? = null,
    val occurrenceIndex: Int? = null,
    val initialTex: String = ""
)

@Composable
private fun QuestionEditor(
    question: QuestionDraft,
    index: Int,
    total: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    viewModel: ExamBuilderViewModel,
    onPreview: () -> Unit
) {
    var formulaTarget by remember(question.id) { mutableStateOf<FormulaTarget?>(null) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("سؤال ${index + 1} · ${question.type.faLabel()}")
                Row {
                    TextButton(onClick={viewModel.moveQuestion(question.id,-1)},enabled=index>0){Text("↑")}
                    TextButton(onClick={viewModel.moveQuestion(question.id,1)},enabled=index<total-1){Text("↓")}
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(question.text, { viewModel.updateText(question.id, it) }, label = { Text("متن سؤال") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { formulaTarget = FormulaTarget("question") }) { Text("درج فرمول") }
                if ('$' in question.text) Text("پیش‌نمایش Native:")
            }
            if ('$' in question.text) NativeMathText(question.text, modifier = Modifier.fillMaxWidth())
            ExistingFormulaEditor(
                source = question.text,
                onEdit = { occurrence, tex -> formulaTarget = FormulaTarget("question", occurrenceIndex = occurrence, initialTex = tex) },
                onDelete = { occurrence -> viewModel.deleteFormula(question.id, "question", null, occurrence) }
            )
            OutlinedTextField(question.score.toString(), { viewModel.updateScore(question.id, it) }, label = { Text("بارم") })
            QuestionMediaEditor(
                images = question.images,
                onAdd = { uris -> viewModel.addImages(question.id, uris) },
                onMove = { imageId, x, y -> viewModel.moveImage(question.id, imageId, x, y) },
                onResize = { imageId, width -> viewModel.resizeImage(question.id, imageId, width) },
                onRemove = { imageId -> viewModel.removeImage(question.id, imageId) }
            )
            Text("تصویر پاسخ دانش‌آموز")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("no" to "غیرفعال", "optional" to "اختیاری", "required" to "اجباری").forEach { (mode, label) ->
                    FilterChip(
                        selected = question.answerImageMode == mode,
                        onClick = { viewModel.setAnswerImageMode(question.id, mode) },
                        label = { Text(label) }
                    )
                }
            }
            if (question.answerImageMode != "no") {
                (1..10).chunked(5).forEach { counts ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        counts.forEach { count ->
                            FilterChip(
                                selected = question.maxAnswerImages == count,
                                onClick = { viewModel.setMaxAnswerImages(question.id, count) },
                                label = { Text(count.toString()) }
                            )
                        }
                    }
                }
            }
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تعداد گزینه: ${question.options.size}")
                        OutlinedButton(onClick={viewModel.setOptionCount(question.id,question.options.size-1)},enabled=question.options.size>2){Text("−")}
                        OutlinedButton(onClick={viewModel.setOptionCount(question.id,question.options.size+1)},enabled=question.options.size<10){Text("+")}
                    }
                    question.options.forEachIndexed { index, option ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(question.correctIndex == index, onClick = { viewModel.setCorrect(question.id, index) })
                            OutlinedTextField(
                                option,
                                { viewModel.updateOption(question.id, index, it) },
                                label = { Text("گزینه ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick={viewModel.moveOption(question.id,index,-1)},enabled=index>0){Text("↑ گزینه")}
                            TextButton(onClick={viewModel.moveOption(question.id,index,1)},enabled=index<question.options.lastIndex){Text("↓ گزینه")}
                            OutlinedButton(onClick = { formulaTarget = FormulaTarget("option", index) }) {
                                Text("فرمول گزینه ${index + 1}")
                            }
                            if ('$' in option) NativeMathText(option)
                        }
                        ExistingFormulaEditor(
                            source = option,
                            onEdit = { occurrence, tex ->
                                formulaTarget = FormulaTarget("option", index, occurrence, tex)
                            },
                            onDelete = { occurrence ->
                                viewModel.deleteFormula(question.id, "option", index, occurrence)
                            }
                        )
                        SingleImagePicker(
                            value = question.optionImages.getOrNull(index),
                            label = "تصویر گزینه ${index + 1}"
                        ) { uri -> viewModel.setOptionImage(question.id, index, uri) }
                    }
                }
                }
                QuestionType.TRUE_FALSE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = question.expectedText == "true", onClick = { viewModel.setTrueFalse(question.id, true) }, label = { Text("صحیح") })
                    FilterChip(selected = question.expectedText == "false", onClick = { viewModel.setTrueFalse(question.id, false) }, label = { Text("غلط") })
                }
                QuestionType.FILL_BLANK -> Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(question.expectedText, { viewModel.updateExpectedText(question.id, it) }, label = { Text("پاسخ‌های قابل قبول با |") })
                    ToggleRow("حساس به حروف بزرگ و کوچک",question.caseSensitive){viewModel.setCaseSensitive(question.id,it)}
                }
                QuestionType.NUMERIC -> {
                    OutlinedTextField(question.expectedNumber, { viewModel.updateExpectedNumber(question.id, it) }, label = { Text("پاسخ عددی") })
                    OutlinedTextField(question.tolerance, { viewModel.updateTolerance(question.id, it) }, label = { Text("تلورانس") })
                }
                QuestionType.MATCHING -> MatchingQuestionEditor(
                    question,
                    viewModel,
                    onFormulaEdit = { side, itemIndex, occurrence, tex ->
                        formulaTarget = FormulaTarget("matching_$side", itemIndex, occurrence, tex)
                    },
                    onFormulaDelete = { side, itemIndex, occurrence ->
                        viewModel.deleteFormula(question.id, "matching_$side", itemIndex, occurrence)
                    }
                )
                QuestionType.ESSAY -> Unit
            }
            QuestionStyleControls(question,viewModel,onPreview)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.saveToBank(question.id) }) { Text("ذخیره در بانک") }
                TextButton(onClick = { viewModel.remove(question.id) }) { Text("حذف سؤال") }
            }
                }
            }
        }
    }
    formulaTarget?.let { target ->
        FormulaEditorDialog(
            initialTex = target.initialTex,
            onDismiss = { formulaTarget = null },
            onInsert = { tex ->
                viewModel.insertFormula(
                    question.id,
                    target.field,
                    target.index,
                    tex,
                    target.occurrenceIndex
                )
                formulaTarget = null
            }
        )
    }
}

@Composable
private fun QuestionStyleControls(question: QuestionDraft, viewModel: ExamBuilderViewModel, onPreview:()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("چیدمان و ظاهر چاپ",style=MaterialTheme.typography.titleSmall)
        Text("تراز متن")
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf("right" to "راست","center" to "وسط","left" to "چپ","justify" to "دوطرفه").forEach { (v,l) ->
                FilterChip(selected=question.textAlign==v,onClick={viewModel.setQuestionAlign(question.id,v)},label={Text(l)})
            }
        }
        Text("جای تصویر")
        listOf(listOf("above" to "بالا","below" to "پایین","right" to "راست"),listOf("left" to "چپ","free" to "آزاد")).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { row.forEach { (v,l) ->
                FilterChip(selected=question.imagePosition==v,onClick={viewModel.setImagePosition(question.id,v)},label={Text(l)})
            } }
        }
        Text("قلم سؤال")
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf("default" to "برنامه","vazirmatn" to "وزیر","shabnam" to "شبنم","sahel" to "ساحل").forEach { (v,l) ->
                FilterChip(selected=question.fontFamily==v,onClick={viewModel.setQuestionFont(question.id,v)},label={Text(l)})
            }
        }
        Text("اندازه قلم: ${question.fontSizeSp.toInt()}")
        Slider(value=question.fontSizeSp,onValueChange={viewModel.setQuestionFontSize(question.id,it)},valueRange=8f..40f,steps=31)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            FilterChip(selected=question.bold,onClick={viewModel.setQuestionBold(question.id,!question.bold)},label={Text("ضخیم")})
            FilterChip(selected=question.italic,onClick={viewModel.setQuestionItalic(question.id,!question.italic)},label={Text("مورب")})
        }
        Text("خط پاسخ: ${question.answerLines}")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick={viewModel.setAnswerLines(question.id,question.answerLines-1)},enabled=question.answerLines>0){Text("−")}
            OutlinedButton(onClick={viewModel.setAnswerLines(question.id,question.answerLines+1)},enabled=question.answerLines<12){Text("+")}
            FilterChip(selected=question.answerLineStyle=="lined",onClick={viewModel.setAnswerLineStyle(question.id,"lined")},label={Text("خط‌دار")})
            FilterChip(selected=question.answerLineStyle=="blank",onClick={viewModel.setAnswerLineStyle(question.id,"blank")},label={Text("خالی")})
        }
        OutlinedButton(onClick=onPreview,modifier=Modifier.fillMaxWidth()){Text("پیش‌نمایش چاپ این سؤال")}
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(label)
    }
}

private fun Long.asToman(): String = PersianDigits.convert("%,d".format(java.util.Locale.US, this))

private fun QuestionType.faLabel(): String = when (this) {
    QuestionType.ESSAY -> "تشریحی"
    QuestionType.MULTIPLE_CHOICE -> "چهارگزینه‌ای"
    QuestionType.TRUE_FALSE -> "صحیح/غلط"
    QuestionType.FILL_BLANK -> "جای خالی"
    QuestionType.NUMERIC -> "عددی"
    QuestionType.MATCHING -> "جورکردنی"
}

private fun readBuilderImportLimited(input: java.io.InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= 8 * 1024 * 1024) { "حجم فایل آزمون بیش از ۸ مگابایت است." }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
