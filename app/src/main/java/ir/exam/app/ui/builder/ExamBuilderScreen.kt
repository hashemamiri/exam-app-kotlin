package ir.exam.app.ui.builder

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.data.repository.ExamPackageCodec
import ir.exam.app.domain.model.WalletRules
import ir.exam.app.ui.image.QuestionMediaEditor
import ir.exam.app.ui.math.ExistingFormulaEditor
import ir.exam.app.ui.math.FormulaEditorDialog
import ir.exam.app.ui.math.NativeMathText
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var settingsExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSave by remember { mutableStateOf(false) }
    // هنگام جابه‌جایی گزینه/جورکردنی، اسکرول لمسی فهرست غیرفعال می‌شود تا فقط
    // همان انگشت کنترل کند و کارت سؤال زیر انگشت نلغزد.
    var innerReorderActive by remember { mutableStateOf(false) }
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

    suspend fun scrollQuestionToHeader(questionIndex: Int) {
        // دو frame: یکی برای recomposition و یکی برای اندازه‌گیری ارتفاع کارت بازشده.
        withFrameNanos { }
        withFrameNanos { }
        listState.animateScrollToItem(questionPrefaceCount + questionIndex, 0)
    }

    fun revealQuestion(type: QuestionType) {
        val id = viewModel.addQuestion(type)
        expandedQuestionId = id
        val questionIndex = state.questions.size
        scope.launch { scrollQuestionToHeader(questionIndex) }
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
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (!state.loading) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    FloatingActionButton(
                        onClick = { confirmSave = true },
                        modifier = Modifier.align(Alignment.CenterStart).size(56.dp),
                        containerColor = Color(0xFF27A86B),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "ذخیره آزمون",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (!radialMenuOpen) {
                        FloatingActionButton(
                            onClick = { radialMenuOpen = true },
                            modifier = Modifier.align(Alignment.CenterEnd).size(56.dp)
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
            userScrollEnabled = !innerReorderActive,
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExamSettingsCard(state, viewModel)
                        // مخاطبان آزمون داخل مشخصات آزمون است و با آن باز/بسته می‌شود.
                        AudienceCard(state, viewModel)
                    }
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
            itemsIndexed(state.questions, key = { _, item -> item.id }) { index, question ->
                QuestionEditor(
                    modifier = Modifier.animateItem(
                        placementSpec = tween(260, easing = FastOutSlowInEasing)
                    ),
                    question = question,
                    index = index,
                    expanded = expandedQuestionId == question.id,
                    onToggle = {
                        if (expandedQuestionId == question.id) {
                            expandedQuestionId = null
                        } else {
                            // بازکردن کارت سؤال، کارت مشخصات آزمون را می‌بندد.
                            settingsExpanded = false
                            expandedQuestionId = question.id
                            scope.launch { scrollQuestionToHeader(index) }
                        }
                    },
                    onExpand = {
                        // بازکردن کارت سؤال، کارت مشخصات آزمون را می‌بندد.
                        settingsExpanded = false
                        expandedQuestionId = question.id
                        scope.launch { scrollQuestionToHeader(index) }
                    },
                    onMove = { delta -> viewModel.moveQuestion(question.id, delta) },
                    onDragStarted = { expandedQuestionId = null },
                    onDragScroll = { delta -> listState.dispatchRawDelta(delta * .35f) },
                    onItemDragStarted = { innerReorderActive = true },
                    onItemDragEnded = { innerReorderActive = false },
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
                val questionIndex = state.questions.size
                viewModel.addFromBank(id)
                expandedQuestionId = viewModel.state.value.questions.lastOrNull()?.id
                bankDialogOpen = false
                scope.launch { scrollQuestionToHeader(questionIndex) }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JalaliDateTimeField(
                    "شروع",
                    state.opensAtIso,
                    viewModel::setOpensAt,
                    modifier = Modifier.weight(1f)
                )
                JalaliDateTimeField(
                    "پایان",
                    state.closesAtIso,
                    viewModel::setClosesAt,
                    modifier = Modifier.weight(1f),
                    minimumIso = state.opensAtIso
                )
            }
            OutlinedTextField(state.negativeMarking, viewModel::setNegativeMarking, label = { Text("نمره منفی") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.teacherMessage, viewModel::setTeacherMessage, label = { Text("پیام معلم") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoldToggleChip(
                    label = "تصادفی‌سازی سؤال‌ها",
                    selected = state.shuffleQuestions,
                    onClick = { viewModel.setShuffleQuestions(!state.shuffleQuestions) },
                    modifier = Modifier.weight(1f)
                )
                BoldToggleChip(
                    label = "تصادفی‌سازی گزینه‌ها",
                    selected = state.shuffleOptions,
                    onClick = { viewModel.setShuffleOptions(!state.shuffleOptions) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                BoldToggleChip(
                    label = "اتمام تلاش در پایان زمان",
                    selected = state.attemptOnTimeout,
                    onClick = { viewModel.setAttemptOnTimeout(!state.attemptOnTimeout) }
                )
            }
            Text("تعداد تلاش مجاز", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                (1..5).forEach { count ->
                    FilterChip(
                        selected = state.attemptsAllowed == count,
                        onClick = { viewModel.setAttempts(count) },
                        label = { Text(count.toString()) }
                    )
                }
            }
            Text("سیاست نمره", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                listOf("last" to "آخرین", "best" to "بهترین", "all" to "همه").forEach { (value, label) ->
                    FilterChip(selected = state.gradePolicy == value, onClick = { viewModel.setGradePolicy(value) }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                state.attemptCooldown,
                viewModel::setAttemptCooldown,
                label = { Text("فاصله تلاش‌ها (دقیقه)") },
                modifier = Modifier.fillMaxWidth()
            )
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
    modifier: Modifier = Modifier,
    question: QuestionDraft,
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onMove: (Int) -> Unit,
    onDragStarted: () -> Unit,
    onDragScroll: (Float) -> Unit,
    onItemDragStarted: () -> Unit,
    onItemDragEnded: () -> Unit,
    viewModel: ExamBuilderViewModel,
    onPreview: () -> Unit
) {
    var formulaTarget by remember(question.id) { mutableStateOf<FormulaTarget?>(null) }
    var styleExpanded by remember(question.id) { mutableStateOf(false) }
    var scoreText by remember(question.id) {
        mutableStateOf(if (question.score == 1.0) "" else compactScore(question.score))
    }
    var dragAccumulator by remember(question.id) { mutableFloatStateOf(0f) }
    var dragActive by remember(question.id) { mutableStateOf(false) }
    // شناسهٔ گزینه‌ای که اکنون در حال درگ است تا کارت همان گزینه رنگی شود.
    var optionDragId by remember(question.id) { mutableStateOf<String?>(null) }
    // همان آستانهٔ مشترک گزینه/جورکردنی تا رفتار جابه‌جایی‌ها یکسان باشد.
    val dragStepPx = with(LocalDensity.current) { ReorderStepDp.dp.toPx() }
    val neonColor = MaterialTheme.colorScheme.primary
    val cardColor by animateColorAsState(
        targetValue = if (dragActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(170),
        label = "question-drag-color"
    )

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(39.dp)
                        .drawBehind {
                            drawCircle(neonColor.copy(alpha = .13f), radius = size.minDimension * .48f)
                            drawCircle(
                                neonColor.copy(alpha = .58f),
                                radius = size.minDimension * .39f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        PersianDigits.convert(index + 1),
                        color = neonColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    question.type.faLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
                MinimalScoreField(
                    value = scoreText,
                    onValueChange = { raw ->
                        scoreText = raw.filter { it.isDigit() || it == '.' }.take(6)
                        viewModel.updateScore(question.id, scoreText)
                    }
                )
                IconButton(
                    onClick = {
                        styleExpanded = !styleExpanded
                        if (styleExpanded && !expanded) onExpand()
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (styleExpanded) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (styleExpanded) "بستن چیدمان چاپ" else "بازکردن چیدمان چاپ"
                    )
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(42.dp)
                        .pointerInput(question.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragAccumulator = 0f
                                    dragActive = true
                                    onDragStarted()
                                },
                                onDragCancel = {
                                    dragAccumulator = 0f
                                    dragActive = false
                                },
                                onDragEnd = {
                                    dragAccumulator = 0f
                                    dragActive = false
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragAccumulator += amount.y
                                    onDragScroll(amount.y)
                                    while (abs(dragAccumulator) >= dragStepPx) {
                                        val delta = if (dragAccumulator > 0f) 1 else -1
                                        onMove(delta)
                                        dragAccumulator -= delta * dragStepPx
                                    }
                                }
                            )
                        }
                ) {
                    Icon(Icons.Outlined.DragIndicator, contentDescription = "نگه‌دارید و سؤال را جابه‌جا کنید")
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(question.text, { viewModel.updateText(question.id, it) }, label = { Text("متن سؤال") }, modifier = Modifier.fillMaxWidth())
            if ('$' in question.text) NativeMathText(question.text, modifier = Modifier.fillMaxWidth())
            ExistingFormulaEditor(
                source = question.text,
                onEdit = { occurrence, tex -> formulaTarget = FormulaTarget("question", occurrenceIndex = occurrence, initialTex = tex) },
                onDelete = { occurrence -> viewModel.deleteFormula(question.id, "question", null, occurrence) }
            )
            QuestionMediaEditor(
                images = question.images,
                freePlacement = question.imagePosition == "free",
                onAdd = { uris -> viewModel.addImages(question.id, uris) },
                onReplace = { imageId, uri -> viewModel.replaceImage(question.id, imageId, uri) },
                onMove = { imageId, x, y -> viewModel.moveImage(question.id, imageId, x, y) },
                onRemove = { imageId -> viewModel.removeImage(question.id, imageId) },
                onFormula = { formulaTarget = FormulaTarget("question") }
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
                        val optionLabel = persianOptionLetter(index)
                        val optionId = question.optionIds.getOrElse(index) { "option-$index" }
                        key(optionId) {
                        val optionCardColor by animateColorAsState(
                            targetValue = if (optionDragId == optionId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            animationSpec = tween(170),
                            label = "option-card-color"
                        )
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = optionCardColor)
                        ) {
                            Column(
                                Modifier.padding(9.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        optionLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    RadioButton(
                                        selected = question.correctIndex == index,
                                        onClick = { viewModel.setCorrect(question.id, index) }
                                    )
                                    IconButton(onClick = {
                                        formulaTarget = FormulaTarget("option", index)
                                    }) {
                                        Icon(
                                            Icons.Outlined.Functions,
                                            contentDescription = "درج فرمول $optionLabel"
                                        )
                                    }
                                    SingleImagePicker(
                                        value = question.optionImages.getOrNull(index),
                                        label = "تصویر $optionLabel"
                                    ) { uri -> viewModel.setOptionImage(question.id, index, uri) }
                                    ReorderDragButton(
                                        description = "نگه‌دارید و $optionLabel را جابه‌جا کنید",
                                        currentIndex = index,
                                        itemCount = question.options.size,
                                        onDragStarted = onItemDragStarted,
                                        onDragEnded = onItemDragEnded,
                                        onDragScroll = onDragScroll,
                                        onActiveChanged = { active ->
                                            optionDragId = if (active) optionId else null
                                        }
                                    ) { from, delta ->
                                        viewModel.moveOption(question.id, from, delta)
                                    }
                                }
                                OutlinedTextField(
                                    value = option,
                                    onValueChange = { viewModel.updateOption(question.id, index, it) },
                                    placeholder = { Text("متن $optionLabel") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if ('$' in option) NativeMathText(option)
                                ExistingFormulaEditor(
                                    source = option,
                                    onEdit = { occurrence, tex ->
                                        formulaTarget = FormulaTarget("option", index, occurrence, tex)
                                    },
                                    onDelete = { occurrence ->
                                        viewModel.deleteFormula(question.id, "option", index, occurrence)
                                    }
                                )
                            }
                        }
                        }
                    }
                }
                QuestionType.TRUE_FALSE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = question.expectedText == "true", onClick = { viewModel.setTrueFalse(question.id, true) }, label = { Text("صحیح") })
                    FilterChip(selected = question.expectedText == "false", onClick = { viewModel.setTrueFalse(question.id, false) }, label = { Text("غلط") })
                }
                QuestionType.FILL_BLANK -> Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(question.expectedText, { viewModel.updateExpectedText(question.id, it) }, label = { Text("پاسخ‌های قابل قبول با |") })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        BoldToggleChip(
                            label = "حساس به حروف بزرگ و کوچک",
                            selected = question.caseSensitive,
                            onClick = {
                                viewModel.setCaseSensitive(question.id, !question.caseSensitive)
                            }
                        )
                    }
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
                    },
                    onItemDragStarted = onItemDragStarted,
                    onItemDragEnded = onItemDragEnded,
                    onItemDragScroll = onDragScroll
                )
                QuestionType.ESSAY -> Unit
            }
            AnimatedVisibility(
                visible = styleExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                QuestionStyleControls(question, viewModel, onPreview)
            }
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
private fun MinimalScoreField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.width(62.dp).height(40.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (value.isBlank()) {
                        Text(
                            "بارم",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun BoldToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
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
private fun compactScore(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun QuestionType.faLabel(): String = when (this) {
    QuestionType.ESSAY -> "تشریحی"
    QuestionType.MULTIPLE_CHOICE -> "چندگزینه‌ای"
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
