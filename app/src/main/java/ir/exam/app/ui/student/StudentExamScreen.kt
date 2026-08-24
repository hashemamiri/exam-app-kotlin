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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    onDone: () -> Unit
) {
    val exam = state.exam ?: return
    val activity = LocalContext.current.findActivity()
    var showExit by remember { mutableStateOf(false) }
    BackHandler(enabled=!state.finished){showExit=true}
    DisposableEffect(activity, state.finished) {
        if (!state.finished) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
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

    Scaffold(
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (state.remainingSeconds == UNLIMITED_TIME) "زمان: بدون محدودیت"
                        else "زمان: ${formatRemaining(state.remainingSeconds)}"
                    )
                    Button(onClick = onSubmit, enabled = !state.submitting) {
                        Text(if (state.submitting) "در حال ارسال..." else "ارسال نهایی")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onPrevious, enabled = state.questionIndex > 0) { Text("قبلی") }
                    Button(onClick = onNext, enabled = state.questionIndex < exam.questions.lastIndex) { Text("بعدی") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(exam.title, style = MaterialTheme.typography.titleLarge) }
            item {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    exam.questions.indices.chunked(8).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        row.forEach { i ->
                            val q=exam.questions[i];val answered=state.answers.containsKey(q.id)
                            FilterChip(selected=i==state.questionIndex,onClick={onGoTo(i)},label={Text("${i+1}${if(q.id in state.flaggedQuestionIds)"★" else if(answered)"✓" else ""}")})
                        }
                    } }
                }
            }
            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("سؤال ${state.questionIndex + 1} از ${exam.questions.size} · بارم ${question.score}")
                OutlinedButton(onClick={onToggleFlag(question.id)}){Text(if(question.id in state.flaggedQuestionIds)"برداشتن علامت" else "علامت برای مرور")}
            } }
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
    if(showExit)AlertDialog(onDismissRequest={showExit=false},title={Text("خروج از آزمون")},text={Text("پاسخ‌ها ذخیره شده‌اند و زمان سرور ادامه دارد. برنامه بسته شود؟")},confirmButton={Button(onClick={showExit=false;activity?.finish()}){Text("بستن برنامه")}},dismissButton={TextButton(onClick={showExit=false}){Text("ادامه آزمون")}})
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
