package ir.exam.app.ui.student

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity as StudentLocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import ir.exam.app.core.calendar.PersianDigits
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Calculate
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
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream
import ir.exam.app.core.figure.AtlasBlankAnswerCodec
import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.HotspotZone
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
    // V137 — رفع «گزارش می‌گوید دانش‌آموز از برنامه خارج شد ولی نشده بود»:
    // ۱) DisposableEffect با کلید state.finished پس از ارسال، با مقدار کهنهٔ finished=false
    //    دوباره ساخته می‌شد و onDispose «exam_screen_leave» را بعد از ثبت پاسخ می‌نوشت →
    //    حالا وضعیت زنده (rememberUpdatedState) خوانده می‌شود.
    // ۲) ON_PAUSE برای انتخاب‌گر عکس/گالری، پنجرهٔ مجوز و خاموش‌شدن صفحه هم رخ می‌دهد؛
    //    فقط ON_STOP (رفتن واقعی به پس‌زمینه) ثبت می‌شود و اگر خود برنامه انتخاب‌گر را باز
    //    کرده باشد (ExamLeaveGuard) شمرده نمی‌شود.
    val finishedNow by androidx.compose.runtime.rememberUpdatedState(state.finished)
    val securityEventNow by androidx.compose.runtime.rememberUpdatedState(onSecurityEvent)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (!finishedNow) when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP ->
                    if (!ExamLeaveGuard.consumeSuppressed()) securityEventNow("app_leave")
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> ExamLeaveGuard.clear()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // خروج از صفحهٔ آزمون داخل برنامه (وسط آزمون)
            if (!finishedNow) securityEventNow("exam_screen_leave")
        }
    }
    val primary = exam.primaryMode // V301 — حالت ابتدایی
    if (state.finished) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (primary) Text("🌟", style = MaterialTheme.typography.displayMedium) // V301 — پایان دوستانه
            Text(
                if (primary) (if (state.queued) "آفرین! پاسخ‌هایت ذخیره شد و به‌زودی فرستاده می‌شود." else "آفرین! پاسخ‌هایت ثبت شد.")
                else if (state.queued) "پاسخ شما در صف امن ارسال است." else "پاسخ شما با موفقیت ثبت شد.",
                style = MaterialTheme.typography.headlineSmall,
                color = if (state.queued) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
            if (!primary) Text("کد آزمون: ${exam.code}")
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
    // V58.0.2 — اگر خود سؤال نمودار داشته باشد (توکن k='g') تختهٔ وایت‌برد (V137)
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
                if (state.activeSectionTitle.isNotBlank() && state.sectionRemainingSeconds != UNLIMITED_TIME) {
                    Text("${state.activeSectionTitle}: ${formatRemaining(state.sectionRemainingSeconds)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                // V301 — ابتدایی: زمان‌سنج کم‌رنگ و کوچک (بدون استرس)
                if (primary) Box(Modifier.alpha(0.55f)) {
                    ExamCountdownText(initialRemainingSeconds = state.remainingSeconds, totalSeconds = state.totalSeconds, paused = state.timerPaused)
                } else ExamCountdownText(
                    initialRemainingSeconds = state.remainingSeconds,
                    totalSeconds = state.totalSeconds,
                    // V121 — وقتی معلم وسط آزمون ویرایش می‌کند، تایمر باید واقعاً
                    // متوقف بماند؛ قبلاً این نمایش مستقل از state.timerPaused
                    // همچنان می‌شمرد و با پیام «زمان‌سنج متوقف است» ناسازگار بود.
                    paused = state.timerPaused
                )
                Button(onClick = onSubmit, enabled = !state.submitting) {
                    Text(if (state.submitting) "در حال ارسال..." else if (primary) "تمام شد" else "ارسال نهایی")
                }
            }
        }
    ) { padding ->
        // V301 — ابتدایی: همهٔ متن‌ها ۳۰٪ درشت‌تر (fontScale) و فاصلهٔ بیشتر
        val baseDensity = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides if (primary) Density(baseDensity.density, baseDensity.fontScale * 1.3f) else baseDensity) {
        LazyColumn(
            modifier = Modifier.padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(if (primary) 18.dp else 12.dp)
        ) {
            // V281 — نوار اطمینان: قطع اینترنت ⇒ هشدار کهربایی با توضیح ساده؛ وصل ⇒ «ذخیره روی دستگاه» با ساعت آخرین ذخیره
            item { ConnectionBanner(online = state.online, savedAtMs = state.draftSavedAtMs) }
            // V58.0 — هدر و نام آزمون حذف شد؛ شماره سؤال‌ها در یک سطر اسکرول‌شونده
            // با آیکن قبلی/بعدی در دو سر؛ نگه‌داشتن ۲ ثانیه‌ای = علامت برای مرور.
            if (primary) item {
                // V301 — ابتدایی: به‌جای نوار شماره‌ها، دو دکمهٔ بزرگ و «سؤال ۳ از ۱۰» در وسط
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPrevious, enabled = state.questionIndex > 0, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null); Text("قبلی")
                    }
                    Text(
                        "سؤال ${PersianDigits.convert(state.questionIndex + 1)} از ${PersianDigits.convert(exam.questions.size)}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1.2f)
                    )
                    Button(onClick = onNext, enabled = state.questionIndex < exam.questions.lastIndex, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text("بعدی"); Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                    }
                }
            } else item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // V58.0.2 / V335.2 — در RTL آیکن‌های AutoMirrored آینه‌ای می‌شوند:
                    // KeyboardArrowLeft -> فلش رو به راست (قبلی) و KeyboardArrowRight -> فلش رو به چپ (بعدی).
                    IconButton(onClick = onPrevious, enabled = state.questionIndex > 0) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "سؤال قبلی")
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
                        // V214 — کلید پایدار + فقط سه مقدار ساده به هر تراشه: با هر تایپ پاسخ، تراشه‌های نوار
                        // دیگر بازترکیب نمی‌شوند (قبلاً کل state به هر تراشه می‌رفت).
                        items(exam.questions.size, key = { i -> exam.questions[i].id }) { i ->
                            val qid = exam.questions[i].id
                            StripChipCell(
                                index = i,
                                questionId = qid,
                                answered = state.answers.containsKey(qid),
                                flagged = qid in state.flaggedQuestionIds,
                                selectedChip = i == state.questionIndex,
                                onGoTo = onGoTo,
                                onToggleFlag = onToggleFlag
                            )
                        }
                    }
                    IconButton(onClick = onNext, enabled = state.questionIndex < exam.questions.lastIndex) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "سؤال بعدی")
                    }
                }
            }
            if (!primary) item {
                Text("سؤال ${state.questionIndex + 1} از ${exam.questions.size} · بارم ${question.score}")
            }
            item {
                // V57.0 — تصاویر سؤال با لمس زوم می‌شوند؛ متن سؤال سطر به سطر و
                // شکل‌ها زوم‌پذیر؛ کادرهای نامگذاری اطلس داخل TextAnswer ذخیره می‌شوند.
                var zoomImage by remember(question.id) { mutableStateOf<String?>(null) }
                val answerText = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    if (question.sectionTitle.isNotBlank()) {
                        val sectionQuestions = exam.questions.filter { it.sectionTitle.trim() == question.sectionTitle.trim() }
                        val sectionScore = sectionQuestions.sumOf { it.score }
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(question.sectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val sectionMinutes = exam.sectionDurationsMinutes[question.sectionTitle.trim()]
                                Text("${sectionQuestions.size} سؤال · جمع بارم ${sectionScore}" + (sectionMinutes?.let { " · زمان مجاز ${it} دقیقه" } ?: ""), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    // V327 — متن مشترک درک مطلب فقط یک بار بالای اولین سؤالِ وابسته نشان داده می‌شود.
                    if (question.passage.isNotBlank()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (primary) "متن روخوانی" else "متن / صورت مشترک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                NativeMathText(question.passage, fontSize = if (primary) 22.sp else 18.sp)
                                if (primary) ir.exam.app.ui.audio.ReadAloudButton(text = question.passage, modifier = Modifier.fillMaxWidth(), key = "passage-${question.id}")
                            }
                        }
                    }
                    // V135 — فایل صوتی سؤال: پلی/پاز + نوار جلو/عقب
                    presentation.audioUrl?.let { audio ->
                        ir.exam.app.ui.audio.QuestionAudioPlayer(url = audio, durationMs = presentation.audioMs, modifier = Modifier.fillMaxWidth())
                    }
                    // V302 — ابتدایی: اگر معلم صدا ضبط نکرده، دکمهٔ بزرگ «بخوان» با TTS دستگاه
                    if ((primary || (question as? FillBlankQuestion)?.dictationText != null) && presentation.audioUrl == null) {
                        ir.exam.app.ui.audio.ReadAloudButton(
                            // V306/V327 — املا: واژه‌ها جدا، تکرار و سکوت مطابق تنظیم معلم.
                            text = remember(question.id) { (question as? FillBlankQuestion)?.dictationText ?: ir.exam.app.core.speech.ReadAloudText.forQuestion(question) },
                            modifier = Modifier.fillMaxWidth(),
                            key = question.id,
                            dictation = (question as? FillBlankQuestion)?.dictationText != null,
                            gapSeconds = (question as? FillBlankQuestion)?.dictationGap ?: 6,
                            repeats = (question as? FillBlankQuestion)?.dictationRepeats ?: 2
                        )
                    }
                    if(presentation.imagePosition=="above") question.images.forEach { img ->
                        StudentCachedImage(img, "تصویر سؤال", Modifier.fillMaxWidth().clickable { zoomImage = img })
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
                        StudentCachedImage(img, "تصویر سؤال", Modifier.fillMaxWidth().clickable { zoomImage = img })
                    }
                }
                zoomImage?.let { img ->
                    ZoomableFigureDialog(onDismiss = { zoomImage = null }, title = "تصویر سؤال") {
                        StudentCachedImage(img, "تصویر سؤال بزرگ", Modifier.fillMaxWidth())
                    }
                }
            }
            val currentSectionExpired = question.sectionTitle.trim().isNotBlank() && question.sectionTitle.trim() in state.expiredSections
            item {
                if (currentSectionExpired) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            "زمان این بخش تمام شد؛ پاسخ‌های ثبت‌شده حفظ شده‌اند و امکان ویرایش یا بازگشت نیست.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else when (question) {
                    is EssayQuestion -> if (question.answerMode == "construct" && question.constructionLabels.isNotEmpty()) {
                        StudentConstructionAnswer(question, (state.answers[question.id] as? TextAnswer)?.value.orEmpty()) {
                            onAnswer(TextAnswer(question.id, it))
                        }
                    } else if (question.assessmentParts.isNotEmpty() || question.answerMode == "steps" || question.answerMode == "proof") {
                        StepwiseEssayAnswer(question, (state.answers[question.id] as? TextAnswer)?.value.orEmpty()) {
                            onAnswer(TextAnswer(question.id, it))
                        }
                    } else if (question.answerMode == "voice") {
                        // V309 — پاسخ صوتی: ضبط با سقف مدت تعیین‌شدهٔ معلم؛ فایل مثل نقاشی ذخیره می‌شود
                        StudentVoiceAnswerEntry(
                            questionId = question.id,
                            current = state.responseImages[question.id].orEmpty(),
                            maxSeconds = question.voiceMaxSeconds.takeIf { it > 0 } ?: 60,
                            onAdd = onAddImages,
                            onRemove = onRemoveImage
                        )
                    } else if (question.answerMode == "draw" || question.answerMode == "construct") {
                        // V306 — ابتدایی: پاسخ با تخته؛ کادر متنی نشان داده نمی‌شود
                        Text("پاسخ را روی تخته بکشید و ذخیره کنید؛ با دکمهٔ بزرگ زیر باز کنید.", style = MaterialTheme.typography.bodyLarge)
                        StudentWhiteboardEntry(
                            questionId = question.id,
                            current = if (question.maxAnswerImages > 0) emptyList() else state.responseImages[question.id].orEmpty(),
                            onAdd = onAddImages,
                            onRemove = onRemoveImage,
                            questionNumber = state.questionIndex + 1,
                            remainingSeconds = state.remainingSeconds,
                            questionImages = question.images,
                            big = true
                        )
                    } else {
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
                    // V264 — صفحه‌کلید ریاضی (آیتم ۱۲) کنار کادر پاسخ جای خالی/عددی؛ با دکمهٔ ماشین‌حساب باز/بسته می‌شود
                    is FillBlankQuestion -> if (primary && question.paintRows > 0 && question.paintCols > 0) {
                        val saved = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        PrimaryPaintAnswer(question, saved) { onAnswer(TextAnswer(question.id, it)) }
                    } else if (primary && question.orderItems.isNotEmpty()) {
                        // V305 — ابتدایی: مرتب‌سازی با کشیدن؛ پاسخ = ترتیب فعلی موارد با «,»
                        val saved = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        val order = orderFromAnswer(saved, question.orderItems)
                        PrimaryOrderAnswer(order, onChange = { onAnswer(TextAnswer(question.id, it.joinToString(","))) })
                    } else if (primary && question.buckets.isNotEmpty() && question.bucketItems.isNotEmpty()) {
                        val saved = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        PrimaryBucketAnswer(
                            buckets = question.buckets,
                            items = question.bucketItems,
                            saved = saved,
                            onChange = { onAnswer(TextAnswer(question.id, it)) }
                        )
                    } else if (primary && question.syllableCount >= 2) {
                        val saved = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        PrimarySyllableAnswer(
                            questionId = question.id,
                            count = question.syllableCount,
                            saved = saved,
                            onChange = { onAnswer(TextAnswer(question.id, it)) }
                        )
                    } else {
                        val current = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        var keypad by remember(question.id) { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = current,
                                onValueChange = { onAnswer(TextAnswer(question.id, it)) },
                                label = { Text("پاسخ جای خالی") },
                                trailingIcon = {
                                    IconButton(onClick = { keypad = !keypad }) {
                                        Icon(Icons.Outlined.Calculate, contentDescription = "صفحه‌کلید ریاضی", tint = if (keypad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (keypad) MathKeypad(numeric = false, onKey = { k -> onAnswer(TextAnswer(question.id, MathKeys.apply(current, k, numeric = false))) })
                        }
                    }
                    is NumericQuestion -> if (primary) {
                        // V305 — ابتدایی: کادر پاسخ + پد عددی بزرگ ۰–۹ (لمسی)
                        val current = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = current,
                                onValueChange = { value -> onAnswer(TextAnswer(question.id, value.filter { it.isDigit() || it in ".-" })) },
                                label = { Text("پاسخ عددی") },
                                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth()
                            )
                            BigNumberPad(onKey = { k -> onAnswer(TextAnswer(question.id, MathKeys.apply(current, k, numeric = true))) })
                        }
                    } else {
                        val current = (state.answers[question.id] as? TextAnswer)?.value.orEmpty()
                        var keypad by remember(question.id) { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = current,
                                onValueChange = { value -> onAnswer(TextAnswer(question.id, value.filter { it.isDigit() || it in ".-" })) },
                                label = { Text("پاسخ عددی") },
                                trailingIcon = {
                                    IconButton(onClick = { keypad = !keypad }) {
                                        Icon(Icons.Outlined.Calculate, contentDescription = "صفحه‌کلید عددی", tint = if (keypad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (keypad) MathKeypad(numeric = true, onKey = { k -> onAnswer(TextAnswer(question.id, MathKeys.apply(current, k, numeric = true))) })
                        }
                    }
                    // V303 — ابتدایی: وقتی همهٔ گزینه‌ها تصویر (یا متن خیلی کوتاه/ایموجی) هستند → شبکهٔ ۲×۲ کارت‌های تصویری بزرگ
                    is MultipleChoiceQuestion -> if (primary && question.hotspots.isNotEmpty() && question.images.isNotEmpty()) {
                        // V305 — ابتدایی: انتخاب روی تصویر؛ روی ناحیهٔ درست بزنید
                        PrimaryHotspotAnswer(
                            question = question,
                            selectedOriginal = (state.answers[question.id] as? ChoiceAnswer)?.selectedIndex,
                            onPick = { onAnswer(ChoiceAnswer(question.id, it)) }
                        )
                    } else if (primary && isPictureOptionSet(question)) {
                        val labels = listOf("الف", "ب", "ج", "د", "هـ", "و")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            question.options.indices.chunked(2).forEach { rowIdx ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    rowIdx.forEach { displayIndex ->
                                        val originalIndex = question.optionOriginalIndices.getOrElse(displayIndex) { displayIndex }
                                        val chosen = (state.answers[question.id] as? ChoiceAnswer)?.selectedIndex == originalIndex
                                        PrimaryPictureOptionCard(
                                            label = labels.getOrElse(displayIndex) { (displayIndex + 1).toString() },
                                            text = question.options[displayIndex],
                                            image = question.optionImages.getOrNull(displayIndex),
                                            selected = chosen,
                                            onClick = { onAnswer(ChoiceAnswer(question.id, originalIndex)) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowIdx.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else Column(verticalArrangement = Arrangement.spacedBy(if (primary) 12.dp else 8.dp)) {
                        question.options.forEachIndexed { displayIndex, option ->
                            val originalIndex = question.optionOriginalIndices.getOrElse(displayIndex) { displayIndex }
                            val chosen = (state.answers[question.id] as? ChoiceAnswer)?.selectedIndex == originalIndex
                            if (primary) {
                                // V301 — ابتدایی: کارت بزرگ لمسی با نشان حرفی؛ انتخاب‌شده = حاشیهٔ رنگی و تیک
                                PrimaryOptionCard(
                                    label = listOf("الف", "ب", "ج", "د", "هـ", "و").getOrElse(displayIndex) { (displayIndex + 1).toString() },
                                    selected = chosen,
                                    onClick = { onAnswer(ChoiceAnswer(question.id, originalIndex)) }
                                ) {
                                    NativeMathText(option, zoomableFigures = true, fontSize = 18.sp)
                                    question.optionImages.getOrNull(displayIndex)?.let { img ->
                                        StudentCachedImage(img, "تصویر گزینه", Modifier.fillMaxWidth().heightIn(max = 220.dp))
                                    }
                                }
                                return@forEachIndexed
                            }
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onAnswer(ChoiceAnswer(question.id, originalIndex)) }
                            ) {
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
                                            StudentCachedImage(img, "تصویر گزینه", Modifier.size(120.dp).clickable { zoomOption = true })
                                            if (zoomOption) ZoomableFigureDialog(onDismiss = { zoomOption = false }, title = "تصویر گزینه") {
                                                StudentCachedImage(img, "تصویر گزینه بزرگ", Modifier.fillMaxWidth())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is TrueFalseQuestion -> if (primary) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // V301 — ابتدایی: دو کارت بزرگ «درست ✅» / «نادرست ❌»
                        val current = (state.answers[question.id] as? BooleanAnswer)?.value
                        PrimaryOptionCard(label = "✅", selected = current == true, onClick = { onAnswer(BooleanAnswer(question.id, true)) }, modifier = Modifier.weight(1f)) { Text("درست", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        PrimaryOptionCard(label = "❌", selected = current == false, onClick = { onAnswer(BooleanAnswer(question.id, false)) }, modifier = Modifier.weight(1f)) { Text("نادرست", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            if (!currentSectionExpired && question.maxAnswerImages > 0) {
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
            // V136 — «تخته وایت‌برد» (جایگزین نمودار پاسخ V58): با اجازهٔ معلم، دانش‌آموز
            // رسم آزاد می‌کند و نتیجه به‌عنوان تصویر پاسخ ثبت می‌شود.
            // V137 — «نمودار پاسخ دانش‌آموز» به‌کلی حذف شد؛ سؤالی که خودش نمودار دارد
            // (questionHasGraph) هم فقط تختهٔ وایت‌برد می‌گیرد.
            val drawAnswer = (question as? EssayQuestion)?.answerMode in setOf("draw", "construct", "voice")
            if (!drawAnswer) if (presentation.allowAnswerGraph || questionHasGraph) {
                if (!currentSectionExpired) item {
                    StudentWhiteboardEntry(
                        questionId = question.id,
                        current = if (question.maxAnswerImages > 0) emptyList() else state.responseImages[question.id].orEmpty(),
                        onAdd = onAddImages,
                        onRemove = onRemoveImage,
                        questionNumber = state.questionIndex + 1,
                        remainingSeconds = state.remainingSeconds,
                        questionImages = question.images
                    )
                }
            }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
        }
    }
    if(state.showSubmitReview){
        val unanswered=exam.questions.mapIndexedNotNull { i,q ->
            val answer=state.answers[q.id]
            val answered=when(answer){is TextAnswer->answer.value.isNotBlank();is ChoiceAnswer,is BooleanAnswer->true;is MatchingAnswer->answer.pairs.isNotEmpty();else->false}
            if(!answered && state.responseImages[q.id].orEmpty().isEmpty())i+1 else null
        }
        AlertDialog(onDismissRequest=onDismissSubmit,title={Text(if (primary) "تمام شد؟" else "مرور پیش از ارسال")},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(if (primary) "به ${PersianDigits.convert(exam.questions.size-unanswered.size)} سؤال از ${PersianDigits.convert(exam.questions.size)} سؤال جواب دادی." else "پاسخ‌داده‌شده: ${exam.questions.size-unanswered.size} از ${exam.questions.size}")
            Text(if(unanswered.isEmpty())"همه سؤال‌ها پاسخ دارند." else "بدون پاسخ: ${unanswered.joinToString("، ")}")
            val flags=exam.questions.mapIndexedNotNull{i,q->if(q.id in state.flaggedQuestionIds)i+1 else null}
            if(flags.isNotEmpty())Text("علامت‌گذاری‌شده برای مرور: ${flags.joinToString("، ")}")
            Text("پس از تأیید، پاسخ نهایی قابل ویرایش نیست.")
        }},confirmButton={Button(onClick=onConfirmSubmit){Text("تأیید و ارسال نهایی")}},dismissButton={TextButton(onClick=onDismissSubmit){Text("بازگشت و مرور")}})
    }
    // V58.0.2 — درخواست کاربر: تأیید خروج = خروج از «صفحهٔ آزمون» (نه بستن برنامه).
    if(showExit)AlertDialog(onDismissRequest={showExit=false},title={Text("خروج از آزمون")},text={Text(if (state.online) "پاسخ‌ها ذخیره شده‌اند و زمان سرور ادامه دارد. از صفحهٔ آزمون خارج شوید؟" else "اینترنت قطع است ولی پاسخ‌ها روی دستگاه ذخیره شده‌اند و زمان آزمون ادامه دارد. با وصل شدن اینترنت می‌توانید با همان کد ادامه دهید. خارج می‌شوید؟")},confirmButton={Button(onClick={showExit=false;onExitExam()}){Text("خروج از آزمون")}},dismissButton={TextButton(onClick={showExit=false}){Text("ادامه آزمون")}})
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

/** V303 — آیا همهٔ گزینه‌ها «تصویری»اند؟ (هر گزینه تصویر دارد، یا متنش بدون فرمول و حداکثر ۴ نویسه مثل ایموجی/عدد/حرف است) */
private fun isPictureOptionSet(q: MultipleChoiceQuestion): Boolean {
    if (q.options.size < 2 || q.options.size > 6) return false
    return q.options.indices.all { i ->
        val img = q.optionImages.getOrNull(i)
        val t = q.options[i].trim()
        !img.isNullOrBlank() || (t.isNotEmpty() && !t.contains('$') && !t.contains("%%") && t.codePointCount(0, t.length) <= 4)
    }
}

/** V303 — کارت تصویری بزرگ (شبکهٔ ۲×۲): تصویر مربعی، نشان حرفی گوشهٔ بالا، متن کوتاه زیر تصویر درشت. */
@Composable
private fun PrimaryPictureOptionCard(
    label: String,
    text: String,
    image: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) scheme.primaryContainer else scheme.surface),
        border = androidx.compose.foundation.BorderStroke(if (selected) 4.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(if (selected) scheme.primary else scheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(if (selected) "✓" else label, color = if (selected) scheme.onPrimary else scheme.onSecondaryContainer, fontWeight = FontWeight.Bold) }
            }
            if (!image.isNullOrBlank()) {
                StudentCachedImage(image, "تصویر گزینه", Modifier.fillMaxWidth().aspectRatio(1f))
                if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Text(text, fontSize = 56.sp, textAlign = TextAlign.Center, lineHeight = 64.sp)
                }
            }
        }
    }
}

/** V301 — کارت گزینهٔ بزرگ حالت ابتدایی: کل کارت لمسی، نشان حرفی/نماد در راست، حاشیهٔ رنگی و پس‌زمینهٔ روشن وقتی انتخاب شده. */
@Composable
private fun PrimaryOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) scheme.primaryContainer else scheme.surface),
        border = androidx.compose.foundation.BorderStroke(if (selected) 3.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(if (selected) scheme.primary else scheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (selected && label.length <= 2 && label.all { it.isLetter() }) "✓" else label, color = if (selected) scheme.onPrimary else scheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
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
                    question.leftImages.getOrNull(leftIndex)?.let { StudentCachedImage(it, "تصویر جورکردنی", Modifier.size(100.dp)) }
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
                question.rightImages.getOrNull(index)?.let { StudentCachedImage(it, "تصویر ستون راست", Modifier.size(80.dp)) }
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
    // V327 — عکس مستقیم با دوربین، سپس همان ویرایشگر تصویر پاسخ.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) runCatching {
            val file = File(context.cacheDir, "answer_${questionId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out) }
            editQueue = listOf(Uri.fromFile(file))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تصاویر پاسخ (${values.size} از $max)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = values.size < max,
            onClick = {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                // V137 — بازشدن گالری خروج از برنامه نیست.
                ExamLeaveGuard.suppressNext()
                if (max - values.size <= 1) singlePicker.launch(request) else multiplePicker.launch(request)
            }
        ) { Text("انتخاب از گالری") }
        Button(enabled = values.size < max, onClick = {
            ExamLeaveGuard.suppressNext()
            camera.launch(null)
        }) { Text("عکس بگیر") }
        }
        values.forEach { uri ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudentCachedImage(uri, "تصویر پاسخ", Modifier.size(100.dp))
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
    questionId: String,
    answered: Boolean,
    flagged: Boolean,
    selectedChip: Boolean,
    onGoTo: (Int) -> Unit,
    onToggleFlag: (String) -> Unit
) {
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selectedChip) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.combinedClickable(
            onClick = { onGoTo(index) },
            // نگه‌داشتن شمارهٔ سؤال = علامت/برداشتن علامت مرور
            onLongClick = { onToggleFlag(questionId) }
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

/** V136 — دکمهٔ بازکردن تخته وایت‌برد؛ خروجی به تصاویر پاسخ اضافه می‌شود. */
@Composable
private fun StudentWhiteboardEntry(
    questionId: String,
    current: List<String>,
    onAdd: (String, List<String>) -> Unit,
    onRemove: (String, String) -> Unit,
    questionNumber: Int = 0,
    remainingSeconds: Long = UNLIMITED_TIME,
    questionImages: List<String> = emptyList(),
    big: Boolean = false
) {
    var open by remember(questionId) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("تخته وایت‌برد")
        // V137.1 — پیش‌نویس تخته ذخیره می‌شود؛ بازکردن دوباره همان محتوا را برای ویرایش می‌آورد.
        // V306 — حالت نقاشی ابتدایی: دکمهٔ تمام‌عرض و بلند
        Button(onClick = { open = true }, modifier = if (big) Modifier.fillMaxWidth().heightIn(min = 68.dp) else Modifier) {
            Text(if (current.isEmpty()) "بازکردن تخته و نوشتن پاسخ" else "ویرایش تخته", fontSize = if (big) 20.sp else 14.sp)
        }
        current.forEachIndexed { index, uri ->
            Column {
                if (current.size > 1) Text("صفحهٔ ${index + 1}", style = MaterialTheme.typography.labelMedium)
                coil.compose.AsyncImage(model = uri, contentDescription = "پاسخ تخته", modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onRemove(questionId, uri) }) { Text("حذف این تصویر تخته") }
            }
        }
    }
    if (open) {
        StudentWhiteboardDialog(
            questionId = questionId,
            onDismiss = { open = false },
            onDone = { uris -> onAdd(questionId, uris); open = false },
            questionNumber = questionNumber,
            remainingSeconds = remainingSeconds,
            questionImages = questionImages
        )
    }
}

/** تصویر شبکه‌ای با ImageRequest پایدار و بدون crossfade در بازترکیب‌های صفحهٔ آزمون. */
@Composable
private fun StudentCachedImage(uri: String, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .memoryCacheKey("student-exam:$uri")
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = description,
        modifier = modifier
    )
}

/**
 * V58.0 — زمان‌سنج رنگی: از سبز شروع می‌شود، با گذشت زمان به نارنجی و در
 * دقایق پایانی (کمتر از ۱۵٪ مهلت یا ۵ دقیقه) قرمز می‌شود.
 */
@Composable
fun ExamCountdownText(initialRemainingSeconds: Long, totalSeconds: Long, paused: Boolean = false) {
    var remainingSeconds by remember(initialRemainingSeconds) {
        mutableLongStateOf(initialRemainingSeconds)
    }
    // V121 — قبلاً این شمارش محلی مستقل از state.timerPaused بود؛ در نتیجه در
    // بازهٔ نمایش دیالوگ «آزمون توسط معلم ویرایش شد» (که می‌گوید زمان‌سنج
    // متوقف است) همچنان ثانیه‌شمار به کار خود ادامه می‌داد و در پایان با
    // مقدار واقعی remainingSeconds ناگهان هم‌گام می‌شد. اکنون تا وقتی paused
    // است، حلقهٔ شمارش متوقف می‌ماند و دوباره از همان لحظه ادامه می‌یابد.
    LaunchedEffect(initialRemainingSeconds, paused) {
        if (initialRemainingSeconds != UNLIMITED_TIME && !paused) {
            while (remainingSeconds > 0L) {
                delay(1_000L)
                remainingSeconds--
            }
        }
    }
    if (remainingSeconds == UNLIMITED_TIME) {
        Text("بدون محدودیت", fontWeight = FontWeight.Bold)
        return
    }
    val fraction = if (totalSeconds > 0L) remainingSeconds.toFloat() / totalSeconds else 1f
    val nearEnd = remainingSeconds <= 300L || fraction <= .15f
    val color = when {
        nearEnd -> Color(0xFFD32F2F)
        fraction <= .5f -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
    Text(
        formatRemaining(remainingSeconds),
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

/**
 * V137 — نگهبان «خروج از برنامه»: وقتی خود برنامه یک فعالیت بیرونی (گالری/انتخاب‌گر) را
 * باز می‌کند، ON_STOP بعدی نباید «خارج شدن از برنامه» شمرده شود.
 */
internal object ExamLeaveGuard {
    @Volatile private var suppressed = false
    fun suppressNext() { suppressed = true }
    fun consumeSuppressed(): Boolean { val s = suppressed; suppressed = false; return s }
    fun clear() { suppressed = false }
}


/** V281 — نوار وضعیت اتصال وسط آزمون؛ بدون واژهٔ فنی. */
@Composable
private fun ConnectionBanner(online: Boolean, savedAtMs: Long) {
    if (!online) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.medium,
            color = androidx.compose.ui.graphics.Color(0xFFFFF4D6),
            contentColor = androidx.compose.ui.graphics.Color(0xFF7A4B00),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("اتصال اینترنت قطع است", style = MaterialTheme.typography.titleSmall)
                Text("نگران نباشید: هر پاسخی که می‌نویسید روی همین دستگاه ذخیره می‌شود و زمان آزمون ادامه دارد. به‌محض وصل شدن، همه‌چیز خودکار ارسال می‌شود.", style = MaterialTheme.typography.bodySmall)
            }
        }
    } else if (savedAtMs > 0L) {
        val time = remember(savedAtMs) { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(savedAtMs)) }
        Text(
            "✓ پاسخ‌ها روی دستگاه ذخیره شد (" + ir.exam.app.core.calendar.PersianDigits.convert(time) + ")",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


/* ============================================================
 * V305 — ابزارهای ابتدایی: مرتب‌سازی با کشیدن، انتخاب روی تصویر، پد عددی بزرگ
 * ============================================================ */

/** V305 — حرف گزینه (الف، ب، …) بدون وابستگی به بستهٔ builder. */
internal fun primaryLetter(index: Int): String = listOf("الف", "ب", "ج", "د", "ه", "و", "ز", "ح").getOrElse(index) { PersianDigits.convert(index + 1) }

/** ترتیب نمایش از پاسخ ذخیره‌شده؛ اگر پاسخ معتبر نبود (موارد جور نیست) = ترتیب اولیهٔ آزمون. */
internal fun orderFromAnswer(saved: String, items: List<String>): List<String> {
    if (saved.isBlank()) return items
    val parts = saved.split(",").map { it.trim() }
    return if (parts.size == items.size && parts.sorted() == items.sorted()) parts else items
}

@Composable
private fun PrimaryOrderAnswer(order: List<String>, onChange: (List<String>) -> Unit) {
    val rowPx = with(StudentLocalDensity.current) { 68.dp.toPx() }
    var dragIndex by remember(order.size) { mutableIntStateOf(-1) }
    var dragOffset by remember(order.size) { mutableFloatStateOf(0f) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("موارد را با گرفتن دستگیرهٔ ☰ به ترتیب درست بکشید.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        order.forEachIndexed { index, item ->
            val dragging = dragIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                colors = CardDefaults.cardColors(containerColor = if (dragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("☰", fontSize = 26.sp, modifier = Modifier
                        .size(48.dp)
                        .pointerInput(order) {
                            detectDragGestures(
                                onDragStart = { dragIndex = index; dragOffset = 0f },
                                onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                                onDragEnd = {
                                    val target = (index + Math.round(dragOffset / rowPx)).coerceIn(0, order.lastIndex)
                                    if (target != index) onChange(order.toMutableList().also { it.add(target, it.removeAt(index)) })
                                    dragIndex = -1; dragOffset = 0f
                                },
                                onDragCancel = { dragIndex = -1; dragOffset = 0f }
                            )
                        }
                        .padding(4.dp))
                    Text("${PersianDigits.convert(index + 1)}.", fontWeight = FontWeight.Bold)
                    Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    // دکمه‌های ▲▼ برای کسانی که کشیدن برایشان سخت است
                    IconButton(enabled = index > 0, onClick = { onChange(order.toMutableList().also { it.add(index - 1, it.removeAt(index)) }) }) { Text("▲", fontSize = 18.sp) }
                    IconButton(enabled = index < order.lastIndex, onClick = { onChange(order.toMutableList().also { it.add(index + 1, it.removeAt(index)) }) }) { Text("▼", fontSize = 18.sp) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** V314.0 — بخش‌کردن و صداکشی: کادرهای رنگی مجزا + صفحه‌کلید نشانه‌ها و مصوت‌های فارسی. */
@Composable
private fun PrimarySyllableAnswer(
    questionId: String,
    count: Int,
    saved: String,
    onChange: (String) -> Unit
) {
    val parts = remember(questionId, saved, count) {
        val sp = if (saved.isBlank()) emptyList() else saved.split('-')
        List(count) { i -> sp.getOrNull(i).orEmpty() }
    }
    var activeBox by remember(questionId) { mutableIntStateOf(0) }
    val signs = listOf("ـَ", "ـِ", "ـُ", "ـّ", "ا", "آ", "ای", "او", "ی", "و")
    fun updateAt(idx: Int, v: String) {
        val next = parts.toMutableList().also { it[idx] = v.replace("-", "") }
        onChange(next.joinToString("-"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("هر بخش یا صدا را در کادر خودش بنویس:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            parts.forEachIndexed { idx, txt ->
                if (idx > 0) Text(" - ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                OutlinedTextField(
                    value = txt,
                    onValueChange = { activeBox = idx; updateAt(idx, it) },
                    label = { Text("بخش ${PersianDigits.convert(idx + 1)}") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            signs.forEach { sign ->
                val ch = sign.removePrefix("ـ")
                OutlinedButton(onClick = {
                    val idx = activeBox.coerceIn(0, parts.lastIndex)
                    updateAt(idx, parts[idx] + ch)
                }) { Text(sign, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** V314.0 — دسته‌بندی در سبد: قرار دادن هر مورد در سبد مناسب با لمس. */
@Composable
private fun PrimaryBucketAnswer(
    buckets: List<String>,
    items: List<String>,
    saved: String,
    onChange: (String) -> Unit
) {
    val assigned = remember(saved, buckets, items) {
        val map = mutableMapOf<String, String>()
        saved.split('|').forEach { seg ->
            val idx = seg.indexOf(':')
            if (idx > 0) {
                val b = seg.substring(0, idx).trim()
                seg.substring(idx + 1).split('+').map { it.trim() }.filter { it.isNotBlank() }.forEach { itItem ->
                    map[itItem] = b
                }
            }
        }
        map
    }
    fun assignItem(item: String, bucket: String?) {
        val next = assigned.toMutableMap()
        if (bucket == null) next.remove(item) else next[item] = bucket
        val out = buckets.joinToString("|") { b ->
            val list = items.filter { next[it] == b }.map { it.trim() }.sorted()
            "$b:${list.joinToString("+")}"
        }
        onChange(out)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("برای هر مورد، سبد درست را انتخاب کن:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        items.forEach { item ->
            val cur = assigned[item]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        buckets.forEach { b ->
                            val on = cur == b
                            if (on) {
                                Button(onClick = { assignItem(item, null) }) { Text("🧺 $b ✓") }
                            } else {
                                OutlinedButton(onClick = { assignItem(item, b) }) { Text("🧺 $b") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** V328 — یک پاسخ مستقل برای هر گام؛ متن پاسخ به‌صورت خط‌به‌خط ذخیره می‌شود. */
@Composable
private fun StepwiseEssayAnswer(question: EssayQuestion, saved: String, onChange: (String) -> Unit) {
    val parts = question.assessmentParts
    if (question.answerMode == "proof" && parts.isNotEmpty()) {
        val proofAnswers = remember(question.id, saved) {
            runCatching {
                val root = Json.parseToJsonElement(saved).jsonObject
                val data = root["parts"]?.jsonObject ?: JsonObject(emptyMap())
                parts.associate { part ->
                    val value = data[part.id]?.jsonObject
                    part.id to ((value?.get("answer") as? JsonPrimitive)?.contentOrNull.orEmpty() to (value?.get("reason") as? JsonPrimitive)?.contentOrNull.orEmpty())
                }
            }.getOrElse {
                val oldLines = saved.split("\n")
                parts.mapIndexed { index, part -> part.id to (oldLines.getOrElse(index) { "" } to "") }.toMap()
            }
        }
        fun emit(updated: Map<String, Pair<String, String>>) {
            onChange(buildJsonObject {
                put("parts", buildJsonObject {
                    parts.forEach { part ->
                        val pair = updated[part.id] ?: ("" to "")
                        put(part.id, buildJsonObject { put("answer", JsonPrimitive(pair.first)); put("reason", JsonPrimitive(pair.second)) })
                    }
                })
            }.toString())
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (question.proofAssumptions.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("فرض‌ها / داده‌ها", fontWeight = FontWeight.Bold); NativeMathText(question.proofAssumptions) }
            }
            if (question.proofGoal.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("خواسته", fontWeight = FontWeight.Bold); NativeMathText(question.proofGoal) }
            }
            parts.forEachIndexed { index, part ->
                val values = proofAnswers[part.id] ?: ("" to "")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("گام ${part.label.ifBlank { (index + 1).toString() }}: ${part.prompt}", fontWeight = FontWeight.Bold)
                        OutlinedTextField(values.first, { next -> emit(proofAnswers + (part.id to (next to values.second))) }, label = { Text("نتیجه / گزارهٔ این گام") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        if (part.requiresReason) OutlinedTextField(values.second, { next -> emit(proofAnswers + (part.id to (values.first to next))) }, label = { Text(part.reasonPrompt.ifBlank { "دلیل این گام" }) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        if (part.carriesForward && index > 0) {
                            val prior = parts.getOrNull(index - 1)?.let { proofAnswers[it.id]?.first }.orEmpty()
                            Text("ارجاع به نتیجهٔ گام قبل: ${prior.ifBlank { "پاسخ گام قبل هنوز ثبت نشده است." }}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        return
    }
    val prompts = parts.map { part -> listOf(part.label, part.prompt).filter(String::isNotBlank).joinToString(" · ").ifBlank { "زیرسؤال" } }
        .ifEmpty { question.solutionSteps.ifEmpty { listOf("گام اول", "گام دوم", "پاسخ نهایی") } }
    val values = remember(question.id, saved, prompts.size) {
        val parts = saved.split("\n")
        List(prompts.size) { parts.getOrElse(it) { "" } }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        prompts.forEachIndexed { index, prompt ->
            val carriesForward = parts.getOrNull(index)?.carriesForward == true && index > 0
            val previousResult = values.getOrNull(index - 1).orEmpty()
            OutlinedTextField(
                value = values[index],
                onValueChange = { next ->
                    val updated = values.toMutableList()
                    updated[index] = next.replace("\n", " ")
                    onChange(updated.joinToString("\n"))
                },
                label = { Text(prompt.ifBlank { "گام ${index + 1}" }) },
                supportingText = if (carriesForward) ({ Text("ارجاع به نتیجهٔ گام قبل: ${previousResult.ifBlank { "پاسخ گام قبل هنوز ثبت نشده است." }}") }) else null,
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** نمودار مختصاتی تعاملی با برچسب‌گذاری نقاط و زاویه‌ها؛ مختصات و شرط‌های هدف هرگز به دانش‌آموز ارسال نمی‌شوند. */
@Composable
private fun StudentConstructionAnswer(question: EssayQuestion, saved: String, onChange: (String) -> Unit) {
    val labels = question.constructionLabels.take(12)
    var selectedLabel by remember(question.id) { mutableStateOf<String?>(null) }
    var snapHalf by remember(question.id) { mutableStateOf(true) }
    var customAngleInput by remember(question.id) { mutableStateOf("") }
    val parsed = remember(question.id, saved) {
        ir.exam.app.core.geometry.GeometryConstructionEvaluator.parseResponse(saved)
    }
    val points = remember(parsed, labels) {
        labels.mapNotNull { label ->
            val p = parsed.points[label] ?: return@mapNotNull null
            label to Offset(p.x.toFloat().coerceIn(-5f, 5f), p.y.toFloat().coerceIn(-5f, 5f))
        }.toMap()
    }
    val activeAngles = remember(parsed, question.constructionAngleLabels) {
        (question.constructionAngleLabels + parsed.angleLabels)
            .let { ir.exam.app.core.geometry.GeometryConstructionEvaluator.normalizeAngleLabels(it) }
    }
    fun emitUpdated(nextPoints: Map<String, Offset>, nextAngles: List<String> = activeAngles) {
        val domainPts = nextPoints.mapValues { (_, v) ->
            ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(v.x.toDouble(), v.y.toDouble())
        }
        onChange(ir.exam.app.core.geometry.GeometryConstructionEvaluator.encodeResponse(domainPts, nextAngles))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(question.constructionPrompt.ifBlank { "نقاط خواسته‌شده را روی صفحهٔ مختصات مشخص کنید." }, style = MaterialTheme.typography.bodyLarge)
        Text("برای ثبت نقطه روی نمودار بزنید؛ برای جابه‌جایی، برچسب نقطه را انتخاب و محل تازه را لمس کنید. مختصات از −۵ تا ۵ است.", style = MaterialTheme.typography.bodySmall)
        Canvas(
            Modifier.fillMaxWidth().height(290.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .pointerInput(question.id, labels, points, selectedLabel, snapHalf, activeAngles) {
                    detectTapGestures { tap ->
                        val margin = 28f
                        val width = (size.width - 2 * margin).coerceAtLeast(1f)
                        val height = (size.height - 2 * margin).coerceAtLeast(1f)
                        val label = selectedLabel ?: labels.firstOrNull { it !in points } ?: labels.lastOrNull() ?: return@detectTapGestures
                        val rawX = (((tap.x - margin) / width) * 10f - 5f).coerceIn(-5f, 5f)
                        val rawY = (5f - ((tap.y - margin) / height) * 10f).coerceIn(-5f, 5f)
                        val x = if (snapHalf) kotlin.math.round(rawX * 2f) / 2f else rawX
                        val y = if (snapHalf) kotlin.math.round(rawY * 2f) / 2f else rawY
                        emitUpdated(points + (label to Offset(x, y)))
                    }
                }
        ) {
            val margin = 28f
            val w = (size.width - 2 * margin).coerceAtLeast(1f)
            val h = (size.height - 2 * margin).coerceAtLeast(1f)
            fun px(x: Float) = margin + (x + 5f) / 10f * w
            fun py(y: Float) = margin + (5f - y) / 10f * h
            val grid = Color.Gray.copy(alpha = 0.25f)
            val tickPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(110, 120, 132)
                textSize = 10.dp.toPx()
            }
            for (tick in -5..5) {
                val x = px(tick.toFloat()); val y = py(tick.toFloat())
                drawLine(grid, Offset(x, margin), Offset(x, margin + h), 1.dp.toPx())
                drawLine(grid, Offset(margin, y), Offset(margin + w, y), 1.dp.toPx())
                if (tick != 0) {
                    drawContext.canvas.nativeCanvas.drawText(tick.toString(), x - 4.dp.toPx(), py(0f) + 12.dp.toPx(), tickPaint)
                    drawContext.canvas.nativeCanvas.drawText(tick.toString(), px(0f) + 4.dp.toPx(), y + 4.dp.toPx(), tickPaint)
                }
            }
            drawLine(Color.DarkGray, Offset(px(0f), margin), Offset(px(0f), margin + h), 2.dp.toPx())
            drawLine(Color.DarkGray, Offset(margin, py(0f)), Offset(margin + w, py(0f)), 2.dp.toPx())
            val anglePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(176, 88, 12)
                textSize = 12.dp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            activeAngles.forEach { token ->
                val verts = ir.exam.app.core.geometry.GeometryConstructionEvaluator.splitAngleVertices(token) ?: return@forEach
                val p0 = points[verts[0]]; val pv = points[verts[1]]; val p2 = points[verts[2]]
                if (p0 != null && pv != null && p2 != null) {
                    val vScreen = Offset(px(pv.x), py(pv.y))
                    val aScreen = Offset(px(p0.x), py(p0.y))
                    val cScreen = Offset(px(p2.x), py(p2.y))
                    drawLine(Color(0xFFD97706), vScreen, aScreen, 2.dp.toPx())
                    drawLine(Color(0xFFD97706), vScreen, cScreen, 2.dp.toPx())
                    val deg = ir.exam.app.core.geometry.GeometryConstructionEvaluator.angleDegrees(
                        ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(p0.x.toDouble(), p0.y.toDouble()),
                        ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(pv.x.toDouble(), pv.y.toDouble()),
                        ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(p2.x.toDouble(), p2.y.toDouble())
                    )
                    if (deg != null) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "∠$token=${"%.1f".format(java.util.Locale.US, deg)}°",
                            vScreen.x + 8.dp.toPx(),
                            vScreen.y + 16.dp.toPx(),
                            anglePaint
                        )
                    }
                }
            }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30, 83, 170); textSize = 15.dp.toPx(); typeface = android.graphics.Typeface.DEFAULT_BOLD }
            points.forEach { (label, point) ->
                val cx = px(point.x); val cy = py(point.y)
                drawCircle(Color(0xFF1E53AA), radius = 7.dp.toPx(), center = Offset(cx, cy))
                drawContext.canvas.nativeCanvas.drawText(label, cx + 8.dp.toPx(), cy - 8.dp.toPx(), paint)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ثبت‌شده: ${points.size} از ${labels.size}", style = MaterialTheme.typography.labelMedium)
            FilterChip(selected = snapHalf, onClick = { snapHalf = !snapHalf }, label = { Text("چسبیدن به شبکه ۰٫۵") })
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            labels.forEach { label -> FilterChip(selected = selectedLabel == label, onClick = { selectedLabel = if (selectedLabel == label) null else label }, label = { Text("$label${points[label]?.let { " (${"%.1f".format(it.x)}, ${"%.1f".format(it.y)})" } ?: " —"}") }) }
            TextButton(onClick = { selectedLabel = null; onChange("{}") }, enabled = points.isNotEmpty() || activeAngles.isNotEmpty()) { Text("پاک‌کردن") }
        }
        if (activeAngles.isNotEmpty() || labels.size >= 3) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                activeAngles.forEach { token ->
                    val verts = ir.exam.app.core.geometry.GeometryConstructionEvaluator.splitAngleVertices(token)
                    val deg = verts?.let { v ->
                        val p0 = points[v[0]]; val p1 = points[v[1]]; val p2 = points[v[2]]
                        if (p0 != null && p1 != null && p2 != null) ir.exam.app.core.geometry.GeometryConstructionEvaluator.angleDegrees(
                            ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(p0.x.toDouble(), p0.y.toDouble()),
                            ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(p1.x.toDouble(), p1.y.toDouble()),
                            ir.exam.app.core.geometry.GeometryConstructionEvaluator.Point2D(p2.x.toDouble(), p2.y.toDouble())
                        ) else null
                    }
                    FilterChip(
                        selected = deg != null,
                        onClick = {
                            if (token !in question.constructionAngleLabels) {
                                emitUpdated(points, activeAngles.filterNot { it == token })
                            }
                        },
                        label = { Text("∠$token" + (deg?.let { " = ${"%.1f".format(java.util.Locale.US, it)}°" } ?: "")) }
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customAngleInput,
                    onValueChange = { customAngleInput = it.take(12) },
                    label = { Text("برچسب زاویه (مثل ABC)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        val canonical = ir.exam.app.core.geometry.GeometryConstructionEvaluator.splitAngleVertices(customAngleInput)
                            ?.let(ir.exam.app.core.geometry.GeometryConstructionEvaluator::canonicalAngleToken)
                        if (canonical != null) {
                            customAngleInput = ""
                            emitUpdated(points, activeAngles + canonical)
                        }
                    }
                ) { Text("ثبت زاویه") }
            }
        }
    }
}

/** V327 — دانش‌آموز خانه‌ها را لمس می‌کند؛ پاسخ، مختصات مرتب‌شده است. */
@Composable
private fun PrimaryPaintAnswer(question: FillBlankQuestion, saved: String, onChange: (String) -> Unit) {
    val selected = remember(question.id, saved) {
        saved.split(';').mapNotNull { part ->
            val xy = part.split(',')
            val r = xy.getOrNull(0)?.toIntOrNull()
            val c = xy.getOrNull(1)?.toIntOrNull()
            if (r != null && c != null) r to c else null
        }.toSet()
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("خانه‌ها را رنگ کن · ${selected.size} از ${question.paintGoal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        repeat(question.paintRows) { ri ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(question.paintCols) { ci ->
                    val cell = (ri + 1) to (ci + 1)
                    val active = cell in selected
                    Card(
                        modifier = Modifier.size(48.dp).clickable {
                            val next = selected.toMutableSet()
                            if (active) next.remove(cell) else if (next.size < question.paintGoal) next.add(cell)
                            onChange(next.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second }).joinToString(";") { "${it.first},${it.second}" })
                        },
                        colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (active) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp) } }
                }
            }
        }
        Text("برای پاک‌کردن، دوباره روی همان خانه بزن.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PrimaryHotspotAnswer(question: MultipleChoiceQuestion, selectedOriginal: Int?, onPick: (Int) -> Unit) {
    val img = question.images.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("روی ناحیهٔ درست تصویر بزنید:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth()) {
            if (img != null) StudentCachedImage(img, "تصویر سؤال", Modifier.fillMaxWidth())
            BoxWithConstraints(Modifier.matchParentSize()) {
                question.options.indices.forEach { displayIndex ->
                    val original = question.optionOriginalIndices.getOrElse(displayIndex) { displayIndex }
                    val zone: HotspotZone = question.hotspots.getOrNull(original) ?: return@forEach
                    if (zone.w <= 0f || zone.h <= 0f) return@forEach
                    val on = selectedOriginal == original
                    Box(
                        Modifier
                            .offset(maxWidth * (zone.x / 100f), maxHeight * (zone.y / 100f))
                            .size(maxWidth * (zone.w / 100f), maxHeight * (zone.h / 100f))
                            .border(if (on) 4.dp else 2.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
                            .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onPick(original) },
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            if (on) "✓ " + primaryLetter(displayIndex) else primaryLetter(displayIndex),
                            modifier = Modifier.padding(4.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        // گزینه‌ها به‌صورت متن هم فهرست می‌شوند تا معنی هر ناحیه روشن باشد
        question.options.forEachIndexed { displayIndex, option ->
            val original = question.optionOriginalIndices.getOrElse(displayIndex) { displayIndex }
            val on = selectedOriginal == original
            Text(
                "${primaryLetter(displayIndex)}) $option",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** V305 — پد عددی بزرگ ۰–۹ برای ابتدایی؛ منطق کلیدها همان MathKeys (تست‌شده) است. */
@Composable
private fun BigNumberPad(onKey: (String) -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", MathKeys.BACKSPACE))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f).heightIn(min = 68.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (key == MathKeys.BACKSPACE) "⌫ پاک" else PersianDigits.convert(key), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
