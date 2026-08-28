package ir.exam.app.ui.student

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import ir.exam.app.core.figure.AtlasBlankAnswerCodec
import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.NumericQuestion
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.domain.model.TrueFalseQuestion
import ir.exam.app.core.ui.persianFontFamily
import ir.exam.app.ui.figure.ZoomableFigureDialog
import ir.exam.app.ui.image.InteractiveImageEditorDialog
import ir.exam.app.ui.math.NativeMathText

@Composable
fun StudentExamPreview(state: StudentExamUiState, onStart: () -> Unit) {
    val exam = state.exam ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (state.resumedExam) "ادامه آزمون نیمه‌تمام" else "آماده شروع آزمون",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(exam.title, style = MaterialTheme.typography.titleLarge)
        if (exam.subject.isNotBlank()) Text("درس: ${exam.subject}")
        Text("تعداد سؤال: ${exam.questions.size}")
        Text(
            if (state.remainingSeconds == UNLIMITED_TIME) "زمان: بدون محدودیت"
            else "زمان باقی‌مانده: ${formatRemaining(state.remainingSeconds)}"
        )
        if (exam.attemptsAllowed > 1) {
            val attempt = exam.attemptNumber?.let { " · تلاش فعلی: $it" }.orEmpty()
            val remaining = exam.attemptsRemaining?.let { " · باقی‌مانده: $it" }.orEmpty()
            Text("تلاش مجاز: ${exam.attemptsAllowed}$attempt$remaining")
        }
        exam.teacherMessage?.let { message ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("پیام معلم", style = MaterialTheme.typography.titleMedium)
                    NativeMathText(message)
                }
            }
        }
        if (state.resumedExam) {
            Text("پاسخ‌های ذخیره‌شده و همان مهلت قبلی بازیابی شده‌اند. زمان از ابتدا شروع نشده است.")
        } else if (state.remainingSeconds != UNLIMITED_TIME) {
            Text("مهلت سرور از زمان ورود با کد محاسبه می‌شود؛ پیش از شروع معطل نکنید.")
        }
        Button(onClick = onStart, enabled = !state.submitting, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.resumedExam) "ادامه پاسخ‌گویی" else "شروع پاسخ‌گویی")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentExamContent(
    state: StudentExamUiState,
    onAnswer: (StudentAnswer) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoTo: (Int) -> Unit,
    onToggleFlag: (String) -> Unit,
    onAddImages: (String, List<String>) -> Unit,
    onRemoveImage: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onDismissSubmit: () -> Unit,
    onDone: () -> Unit,
    onDismissExamChanges: () -> Unit = {},
    onSecurityEvent: (String) -> Unit = {},
    onExitExam: () -> Unit = {}
) {
    val exam = state.exam ?: return
    val context = LocalContext.current
    val activity = context.findActivity()
    var showExit by remember { mutableStateOf(false) }
    BackHandler(enabled=!state.finished){showExit=true}
    DisposableEffect(activity, state.finished) {
        if (!state.finished) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    // V58.0 — نظارت آزمون (گزارش فقط برای معلم):
    // ۱) تشخیص تلاش اسکرین‌شات (اندروید ۱۴+ با کال‌بک رسمی؛ خود تصویر با
    //    FLAG_SECURE سیاه است)، ۲) تشخیص ضبط صفحه (اندروید ۱۵+)،
    // ۳) خروج از برنامه/بستن برنامه با lifecycle.
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        DisposableEffect(activity, state.finished) {
            val cb = android.app.Activity.ScreenCaptureCallback {
                onSecurityEvent("screenshot_attempt")
            }
            if (!state.finished && activity != null) {
                runCatching {
                    activity.registerScreenCaptureCallback(context.mainExecutor, cb)
                }
            }
            onDispose { runCatching { activity?.unregisterScreenCaptureCallback(cb) } }
        }
    }
    if (android.os.Build.VERSION.SDK_INT >= 35) {
        DisposableEffect(activity, state.finished) {
            val consumer = java.util.function.Consumer<Int> { recording ->
                if (recording == android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE) {
                    onSecurityEvent("screen_record_attempt")
                }
            }
            if (!state.finished && activity != null) {
                runCatching {
                    activity.windowManager.addScreenRecordingCallback(context.mainExecutor, consumer)
                }
            }
            onDispose { runCatching { activity?.windowManager?.removeScreenRecordingCallback(consumer) } }
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.finished) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (!state.finished) when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> onSecurityEvent("app_leave")
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> onSecurityEvent("app_close")
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // خروج از صفحهٔ آزمون داخل برنامه (وسط آزمون)
            if (!state.finished) onSecurityEvent("exam_screen_leave")
        }
    }
    if (state.finished) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (state.queued) "پاسخ شما در صف امن ارسال است." else "پاسخ شما با موفقیت ثبت شد.",
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.queued) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            Text("کد آزمون: ${exam.code}")
            state.submissionMessage?.let { Text(it) }
            if (state.queued) {
                Text("تا ارسال موفق، پیش‌نویس و تصاویر از دستگاه حذف نمی‌شوند.")
            }
            Button(onClick = onDone) { Text("بازگشت به داشبورد") }
        }
        return
    }
    val question = exam.questions.getOrNull(state.questionIndex) ?: return
    val presentation=exam.questionPresentation[question.id] ?: ir.exam.app.domain.model.QuestionPresentation()
    // V58.0.3 — remember فقط در متن Composable مجاز است؛ داخل بدنهٔ LazyColumn
    // (LazyListScope) خطای کامپایل می‌داد و به اینجا منتقل شد.
    // V58.0.2 — اگر خود سؤال نمودار داشته باشد (توکن k='g') رسم نمودار پاسخ
    // بدون نیاز به چیپ معلم فعال است.
    val questionHasGraph = remember(question.id, question.text) {
        ir.exam.app.core.figure.FigureCodec.occurrences(question.text)
            .any { it.spec.kind == "g" }
    }

    Scaffold(
        bottomBar = {
            // V58.0 — نوار پایین: خروج | زمان‌سنج رنگی وسط | ارسال نهایی.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { showExit = true }) { Text("خروج") }
                ExamCountdownText(
                    remainingSeconds = state.remainingSeconds,
                    totalSeconds = state.totalSeconds
                )
                Button(onClick = onSubmit, enabled = !state.submitting) {
                    Text(if (state.submitting) "در حال ارسال..." else "ارسال نهایی")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // V58.0 — هدر و نام آزمون حذف شد؛ شماره سؤال‌ها در یک سطر اسکرول‌شونده
            // با آیکن قبلی/بعدی در دو سر؛ نگه‌داشتن ۲ ثانیه‌ای = علامت برای مرور.
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // V58.0.2 — آیکن‌های AutoMirrored در RTL برعکس رندر می‌شدند؛
                    // نسخهٔ غیرآینه‌ای: قبلی = فلش رو به راست، بعدی = رو به چپ.
                    IconButton(onClick = onPrevious, enabled = state.questionIndex > 0) {
                        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "سؤال قبلی")
                    }
                    // V59.0 — شمارهٔ سؤال جاری همیشه خودکار به دید اسکرول می‌شود.
                    val stripState = rememberLazyListState()
                    LaunchedEffect(state.questionIndex) {
                        stripState.animateScrollToItem(state.questionIndex.coerceAtLeast(0))
                    }
                    androidx.compose.foundation.lazy.LazyRow(
                        state = stripState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(exam.questions.size) { i ->
                            StripChipCell(
                                index = i,
                                state = state,
                                exam = exam,
                                onGoTo = onGoTo,
                                onToggleFlag = onToggleFlag
                            )
                        }
                    }
                    IconButton(onClick = onNext, enabled = state.questionIndex < exam.questions.lastIndex) {
                        Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "سؤال بعدی")
                    }
                }
            }
            item {
                Text("سؤال ${state.questionIndex + 1} از ${exam.questions.size} · بارم ${question.score}")
            }
            item {
                // V57.0 — تصاویر سؤال با لمس زوم می‌شوند؛ متن سؤال سطر به سطر و
                // شکل‌ها زوم‌پذیر؛ کادرهای نامگذاری اطلس داخل TextAnswer ذخیره می‌شوند.
                var zoomImage by remember(question.id) { mutableStateOf<String?>(null) }
                val answerText = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    if(presentation.imagePosition=="above") question.images.forEach { img ->
                        AsyncImage(img,"تصویر سؤال",Modifier.fillMaxWidth().clickable { zoomImage = img })
                    }
                    NativeMathText(
                        question.text,modifier=Modifier.fillMaxWidth(),fontSize=presentation.fontSizeSp.sp,
                        fontWeight=if(presentation.bold)FontWeight.Bold else FontWeight.Normal,
                        fontStyle=if(presentation.italic)FontStyle.Italic else FontStyle.Normal,
                        fontFamily=persianFontFamily(presentation.fontFamily),
                        textAlign=when(presentation.textAlign){"center"->TextAlign.Center;"left"->TextAlign.Left;"justify"->TextAlign.Justify;else->TextAlign.Right},
                        zoomableFigures = true,
                        atlasBlankAnswers = AtlasBlankAnswerCodec.parse(answerText),
                        onAtlasBlankAnswer = { n, value ->
                            val blanks = AtlasBlankAnswerCodec.parse(answerText).toMutableMap()
                            blanks[n] = value
                            onAnswer(TextAnswer(question.id, AtlasBlankAnswerCodec.merge(blanks, AtlasBlankAnswerCodec.freeText(answerText))))
                        }
                    )
                    if(presentation.imagePosition!="above") question.images.forEach { img ->
                        AsyncImage(img,"تصویر سؤال",Modifier.fillMaxWidth().clickable { zoomImage = img })
                    }
                }
                zoomImage?.let { img ->
                    ZoomableFigureDialog(onDismiss = { zoomImage = null }, title = "تصویر سؤال") {
                        AsyncImage(img, "تصویر سؤال بزرگ", Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                when (question) {
                    is EssayQuestion -> {
                        // V57.0 — بخش آزاد پاسخ جدا از خطوط نامگذاری اطلس نگه‌داری
                        // می‌شود تا تایپ در کادر «پاسخ شما» پاسخ کادرهای شکل را پاک نکند.
                        val whole = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        OutlinedTextField(
                            value = AtlasBlankAnswerCodec.freeText(whole),
                            onValueChange = {
                                onAnswer(TextAnswer(question.id, AtlasBlankAnswerCodec.merge(AtlasBlankAnswerCodec.parse(whole), it)))
                            },
                            label = { Text("پاسخ شما") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is FillBlankQuestion -> OutlinedTextField(
                        value = (state.answers[question.id] as? TextAnswer)?.value.orEmpty(),
                        onValueChange = { onAnswer(TextAnswer(question.id, it)) },
                        label = { Text("پاسخ جای خالی") }
                    )
                    is NumericQuestion -> OutlinedTextField(
                        value = (state.answers[question.id] as? TextAnswer)?.value.orEmpty(),
                        onValueChange = { value -> onAnswer(TextAnswer(question.id, value.filter { it.isDigit() || it in ".-" })) },
                        label = { Text("پاسخ عددی") }
                    )
                    is MultipleChoiceQuestion -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEachIndexed { displayIndex, option ->
                            val originalIndex = question.optionOriginalIndices.getOrElse(displayIndex) { displayIndex }
                            Card(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = (state.answers[question.id] as? ChoiceAnswer)?.selectedIndex == originalIndex,
                                        onClick = { onAnswer(ChoiceAnswer(question.id, originalIndex)) }
                                    )
                                    Column {
                                        // V57.0 — گزینه هم سطر به سطر و شکل‌هایش زوم‌پذیر.
                                        NativeMathText(option, zoomableFigures = true)
                                        question.optionImages.getOrNull(displayIndex)?.let { img ->
                                            var zoomOption by remember(question.id, displayIndex) { mutableStateOf(false) }
                                            AsyncImage(img, "تصویر گزینه", Modifier.size(120.dp).clickable { zoomOption = true })
                                            if (zoomOption) ZoomableFigureDialog(onDismiss = { zoomOption = false }, title = "تصویر گزینه") {
                                                AsyncImage(img, "تصویر گزینه بزرگ", Modifier.fillMaxWidth())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is TrueFalseQuestion -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val current = (state.answers[question.id] as? BooleanAnswer)?.value
                        FilterChip(selected = current == true, onClick = { onAnswer(BooleanAnswer(question.id, true)) }, label = { Text("صحیح") })
                        FilterChip(selected = current == false, onClick = { onAnswer(BooleanAnswer(question.id, false)) }, label = { Text("غلط") })
                    }
                    is MatchingQuestion -> MatchingStudentAnswer(
                        question,
                        state.answers[question.id] as? MatchingAnswer,
                        onAnswer
                    )
                }
            }
            if (question.maxAnswerImages > 0) {
                item {
                    ResponseImages(
                        questionId = question.id,
                        max = question.maxAnswerImages,
                        values = state.responseImages[question.id].orEmpty(),
                        onAdd = onAddImages,
                        onRemove = onRemoveImage
                    )
                }
            }
            if (presentation.allowAnswerGraph || questionHasGraph) {
                item {
                    // V58.0 — معلم اجازه داده: دانش‌آموز نمودار پاسخ رسم/ویرایش کند
                    // (مثلاً سهمی یک تابع). توکن %%FIG:...%% داخل همان TextAnswer
                    // ذخیره می‌شود و معلم در تصحیح همان نمودار را می‌بیند.
                    StudentAnswerGraph(
                        answerText = (state.answers[question.id] as? TextAnswer)?.value.orEmpty(),
                        onAnswerText = { onAnswer(TextAnswer(question.id, it)) }
                    )
                }
            }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
    if(state.showSubmitReview){
        val unanswered=exam.questions.mapIndexedNotNull { i,q ->
            val answer=state.answers[q.id]
            val answered=when(answer){is TextAnswer->answer.value.isNotBlank();is ChoiceAnswer,is BooleanAnswer->true;is MatchingAnswer->answer.pairs.isNotEmpty();else->false}
            if(!answered)i+1 else null
        }
        AlertDialog(onDismissRequest=onDismissSubmit,title={Text("مرور پیش از ارسال")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text("پاسخ‌داده‌شده: ${exam.questions.size-unanswered.size} از ${exam.questions.size}")
            Text(if(unanswered.isEmpty())"همه سؤال‌ها پاسخ دارند." else "بدون پاسخ: ${unanswered.joinToString("، ")}")
            val flags=exam.questions.mapIndexedNotNull{i,q->if(q.id in state.flaggedQuestionIds)i+1 else null}
            if(flags.isNotEmpty())Text("علامت‌گذاری‌شده برای مرور: ${flags.joinToString("، ")}")
            Text("پس از تأیید، پاسخ نهایی قابل ویرایش نیست.")
        }},confirmButton={Button(onClick=onConfirmSubmit){Text("تأیید و ارسال نهایی")}},dismissButton={TextButton(onClick=onDismissSubmit){Text("بازگشت و مرور")}})
    }
    // V58.0.2 — درخواست کاربر: تأیید خروج = خروج از «صفحهٔ آزمون» (نه بستن برنامه).
    if(showExit)AlertDialog(onDismissRequest={showExit=false},title={Text("خروج از آزمون")},text={Text("پاسخ‌ها ذخیره شده‌اند و زمان سرور ادامه دارد. از صفحهٔ آزمون خارج شوید؟")},confirmButton={Button(onClick={showExit=false;onExitExam()}){Text("خروج از آزمون")}},dismissButton={TextButton(onClick={showExit=false}){Text("ادامه آزمون")}})
    // V58.0 — معلم وسط آزمون ویرایش کرد: نمایش موارد؛ تا بستن، تایمر مکث است.
    if (state.examChangeNotes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissExamChanges,
            title = { Text("آزمون توسط معلم ویرایش شد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.examChangeNotes.forEach { note -> Text("• $note") }
                    Text(
                        "زمان‌سنج تا بستن این پنجره متوقف است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = { Button(onClick = onDismissExamChanges) { Text("متوجه شدم") } }
        )
    }
}

@Composable
private fun MatchingStudentAnswer(
    question: MatchingQuestion,
    answer: MatchingAnswer?,
    onAnswer: (StudentAnswer) -> Unit
) {
    val current = answer?.pairs.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.leftItems.forEachIndexed { leftIndex, left ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NativeMathText("${leftIndex + 1}. $left")
                    question.leftImages.getOrNull(leftIndex)?.let { AsyncImage(it, "تصویر جورکردنی", Modifier.size(100.dp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        question.rightItems.indices.forEach { displayIndex ->
                            val originalIndex = question.rightOriginalIndices.getOrElse(displayIndex) { displayIndex }
                            FilterChip(
                                selected = current[leftIndex] == originalIndex,
                                onClick = {
                                    onAnswer(MatchingAnswer(question.id, current + (leftIndex to originalIndex)))
                                },
                                label = { Text((displayIndex + 1).toString()) }
                            )
                        }
                    }
                }
            }
        }
        Text("ستون راست:")
        question.rightItems.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                NativeMathText("${index + 1}. $value")
                question.rightImages.getOrNull(index)?.let { AsyncImage(it, "تصویر ستون راست", Modifier.size(80.dp)) }
            }
        }
    }
}

@Composable
private fun ResponseImages(
    questionId: String,
    max: Int,
    values: List<String>,
    onAdd: (String, List<String>) -> Unit,
    onRemove: (String, String) -> Unit
) {
    val context = LocalContext.current
    var editQueue by remember(questionId){mutableStateOf<List<Uri>>(emptyList())}
    fun persist(uri: Uri) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persist(uri)
            editQueue=listOf(uri)
        }
    }
    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(max.coerceAtLeast(2))
    ) { uris ->
        uris.forEach(::persist)
        editQueue=uris
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تصاویر پاسخ (${values.size} از $max)")
        Button(
            enabled = values.size < max,
            onClick = {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (max - values.size <= 1) singlePicker.launch(request) else multiplePicker.launch(request)
            }
        ) { Text("افزودن تصویر پاسخ") }
        values.forEach { uri ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(uri, "تصویر پاسخ", Modifier.size(100.dp))
                OutlinedButton(onClick = { onRemove(questionId, uri) }) { Text("حذف") }
            }
        }
    }
    editQueue.firstOrNull()?.let { uri -> InteractiveImageEditorDialog(
        source=uri,onDismiss={editQueue=editQueue.drop(1)},
        onDone={edited->onAdd(questionId,listOf(edited.toString()));editQueue=editQueue.drop(1)}
    ) }
}

/**
 * V59.0 — چیپ شمارهٔ سؤال در سطر اسکرول‌شونده (LazyRow با اسکرول خودکار به
 * سؤال جاری). V58.0.2: چیپ دست‌ساز تا نگه‌داشتن (علامت مرور) کار کند.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StripChipCell(
    index: Int,
    state: StudentExamUiState,
    exam: ir.exam.app.domain.model.Exam,
    onGoTo: (Int) -> Unit,
    onToggleFlag: (String) -> Unit
) {
    val q = exam.questions[index]
    val answered = state.answers.containsKey(q.id)
    val flagged = q.id in state.flaggedQuestionIds
    val selectedChip = index == state.questionIndex
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selectedChip) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.combinedClickable(
            onClick = { onGoTo(index) },
            // نگه‌داشتن شمارهٔ سؤال = علامت/برداشتن علامت مرور
            onLongClick = { onToggleFlag(q.id) }
        )
    ) {
        Text(
            "${index + 1}${if (flagged) "★" else if (answered) "✓" else ""}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (flagged) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selectedChip) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * V58.0 — نمودار پاسخ دانش‌آموز: با اجازهٔ معلم، دانش‌آموز از همان کتابخانهٔ
 * ۶۱ نمودار Native یکی را انتخاب و پارامترهایش را ویرایش می‌کند (مثلاً سهمی
 * y=ax²+bx+c). توکن %%FIG:...%% داخل TextAnswer ذخیره می‌شود.
 */
@Composable
private fun StudentAnswerGraph(
    answerText: String,
    onAnswerText: (String) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    var editorSpec by remember { mutableStateOf<ir.exam.app.core.figure.FigureSpec?>(null) }
    val existing = ir.exam.app.core.figure.FigureCodec.occurrences(answerText)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("نمودار پاسخ شما")
        if (existing.isEmpty()) {
            Button(onClick = { pickerOpen = true }) { Text("رسم نمودار پاسخ") }
        } else {
            NativeMathText(answerText, zoomableFigures = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editorSpec = existing.first().spec }) { Text("ویرایش نمودار") }
                OutlinedButton(onClick = {
                    // حذف توکن نمودار از پاسخ؛ متن آزاد دست‌نخورده می‌ماند.
                    val occ = existing.first()
                    onAnswerText(answerText.removeRange(occ.start, occ.endExclusive))
                }) { Text("حذف نمودار") }
            }
        }
    }
    if (pickerOpen) {
        ir.exam.app.ui.figure.FigureTypePickerDialog(
            kind = ir.exam.app.ui.figure.FigureKind.GRAPH,
            onDismiss = { pickerOpen = false },
            onTypeSelected = { spec ->
                pickerOpen = false
                editorSpec = spec
            }
        )
    }
    editorSpec?.let { spec ->
        ir.exam.app.ui.figure.FigurePickerDialog(
            initialSpec = spec,
            initialKind = ir.exam.app.ui.figure.FigureKind.GRAPH,
            onDismiss = { editorSpec = null },
            onInsert = { edited ->
                editorSpec = null
                val token = "%%FIG:" + edited.toJson() + "%%"
                val occ = ir.exam.app.core.figure.FigureCodec.occurrences(answerText).firstOrNull()
                onAnswerText(
                    if (occ != null) answerText.replaceRange(occ.start, occ.endExclusive, token)
                    else if (answerText.isBlank()) token
                    else answerText + "\n" + token
                )
            }
        )
    }
}

/**
 * V58.0 — زمان‌سنج رنگی: از سبز شروع می‌شود، با گذشت زمان به نارنجی و در
 * دقایق پایانی (کمتر از ۱۵٪ مهلت یا ۵ دقیقه) قرمز می‌شود.
 */
@Composable
fun ExamCountdownText(remainingSeconds: Long, totalSeconds: Long) {
    var shownSeconds by remember(remainingSeconds) { mutableLongStateOf(remainingSeconds) }
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds != UNLIMITED_TIME) {
            while (shownSeconds > 0L) {
                delay(1_000L)
                shownSeconds--
            }
        }
    }
    if (shownSeconds == UNLIMITED_TIME) {
        Text("بدون محدودیت", fontWeight = FontWeight.Bold)
        return
    }
    val fraction = if (totalSeconds > 0L) shownSeconds.toFloat() / totalSeconds else 1f
    val nearEnd = shownSeconds <= 300L || fraction <= .15f
    val color = when {
        nearEnd -> Color(0xFFD32F2F)
        fraction <= .5f -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
    Text(
        formatRemaining(shownSeconds),
        color = color,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium
    )
}

private fun formatRemaining(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3_600L
    val minutes = (safe % 3_600L) / 60L
    val rest = safe % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, rest)
    else "%d:%02d".format(minutes, rest)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
