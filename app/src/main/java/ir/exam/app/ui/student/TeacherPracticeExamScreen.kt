package ir.exam.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.core.network.UserFacingError
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.data.repository.StudentExamPayloadCodec
import ir.exam.app.data.repository.SupabaseGradingRepository
import ir.exam.app.data.repository.SupabaseStudentExamRepository
import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.Question
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.TextAnswer
import ir.exam.app.ui.app.NeumorphicPanel
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import ir.exam.app.ui.math.NativeMathText
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private data class TeacherPracticeBundle(
    val exam: Exam,
    val keyedQuestions: Map<Int, QuestionDraft>
)

private suspend fun loadTeacherPracticeById(userId: String, examId: String): Result<TeacherPracticeBundle> = runCatching {
    val detail = SupabaseProvider.client.from("exams").select {
        filter {
            eq("id", examId)
            eq("teacher_id", userId)
        }
    }.decodeList<ExamDetailDto>().firstOrNull() ?: error("آزمون پیدا نشد یا متعلق به این حساب نیست.")
    val key = SupabaseProvider.client.from("exam_keys").select {
        filter { eq("exam_id", examId) }
    }.decodeList<ExamKeyDto>().firstOrNull()?.answers ?: JsonArray(emptyList())
    val decodedDrafts = withContext(Dispatchers.Default) {
        ExamQuestionCodec.decode(detail.questions, key)
    }
    if (decodedDrafts.isEmpty()) error("این آزمون هنوز سؤالی ندارد.")
    val raw = buildJsonObject {
        put("id", detail.id)
        put("title", detail.title)
        put("code", detail.code.orEmpty())
        put("subject", detail.subject.orEmpty())
        put("duration", detail.duration ?: 0)
        put("shuffle_q", detail.shuffleQuestions)
        put("shuffle_opt", detail.shuffleOptions)
        put("attempts_allowed", detail.attemptsAllowed)
        detail.teacherMessage?.let { put("teacher_message", it) }
        detail.options?.let { put("options", it) }
        put("questions", detail.questions)
    }
    val exam = StudentExamPayloadCodec.decodeFresh(raw, userId, System.currentTimeMillis())
    TeacherPracticeBundle(
        exam = exam,
        keyedQuestions = decodedDrafts.mapIndexed { index, q -> index to q }.toMap()
    )
}

/**
 * V313.0 — صفحهٔ «شرکت در آزمون» ویژهٔ معلم (کارت هشتم از «کارت‌ها»):
 * معلم می‌تواند با وارد کردن کد آزمون یا انتخاب مستقیم یکی از آزمون‌های خود،
 * دقیقاً در محیط پنل دانش‌آموز به سؤال‌ها پاسخ دهد و در پایان، نمره و پاسخ‌برگ
 * آزمایشی را مشاهده کند (بدون ثبت در جدول پاسخ‌ها و آمار واقعی دانش‌آموزان).
 */
@Composable
fun TeacherPracticeExamScreen(
    userId: String,
    onExamActiveChanged: (Boolean) -> Unit = {},
    onBackToCards: () -> Unit = {}
) {
    val appContext = LocalContext.current.applicationContext
    val gradingRepo = remember { SupabaseGradingRepository() }
    val studentRepo = remember(appContext) { SupabaseStudentExamRepository(appContext) }
    val scope = rememberCoroutineScope()

    var myExams by remember { mutableStateOf<List<ExamDashboardDto>>(emptyList()) }
    var loadingExams by remember { mutableStateOf(true) }
    var keyedByIndex by remember { mutableStateOf<Map<Int, QuestionDraft>>(emptyMap()) }
    var state by remember { mutableStateOf(StudentExamUiState(restoringExam = false)) }

    LaunchedEffect(userId) {
        loadingExams = true
        myExams = gradingRepo.getExams().getOrDefault(emptyList())
        loadingExams = false
    }

    val examActive = state.exam != null && !state.showPreview && !state.finished
    LaunchedEffect(examActive) { onExamActiveChanged(examActive) }

    LaunchedEffect(state.exam?.id, state.started, state.finished, state.remainingSeconds) {
        if (state.exam != null && state.started && !state.finished && state.remainingSeconds > 0L) {
            delay(1000L)
            val next = (state.remainingSeconds - 1L).coerceAtLeast(0L)
            if (next == 0L) {
                state = state.copy(remainingSeconds = 0L, showSubmitReview = false, finished = true)
            } else {
                state = state.copy(remainingSeconds = next)
            }
        }
    }

    fun openBundle(bundle: TeacherPracticeBundle) {
        val ex = bundle.exam
        val totalSec = if (ex.durationMinutes > 0) ex.durationMinutes * 60L else UNLIMITED_TIME
        keyedByIndex = bundle.keyedQuestions
        state = StudentExamUiState(
            code = ex.code,
            exam = ex,
            answers = emptyMap(),
            responseImages = emptyMap(),
            questionIndex = 0,
            flaggedQuestionIds = emptySet(),
            showSubmitReview = false,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            restoringExam = false,
            showPreview = true,
            started = false,
            finished = false,
            error = null
        )
    }

    fun startByExamId(examId: String) {
        if (state.loading) return
        scope.launch {
            state = state.copy(loading = true, error = null)
            loadTeacherPracticeById(userId, examId)
                .onSuccess { openBundle(it) }
                .onFailure { err ->
                    state = state.copy(
                        loading = false,
                        error = UserFacingError.of(err, "بارگذاری آزمون ناموفق بود.")
                    )
                }
        }
    }

    fun startByCode(rawCode: String) {
        val clean = rawCode.trim().uppercase()
        if (clean.length < 4 || state.loading) return
        scope.launch {
            state = state.copy(loading = true, error = null)
            val own = myExams.firstOrNull { it.code?.trim()?.uppercase() == clean }
            if (own != null) {
                loadTeacherPracticeById(userId, own.id)
                    .onSuccess { openBundle(it) }
                    .onFailure { err ->
                        state = state.copy(
                            loading = false,
                            error = UserFacingError.of(err, "بارگذاری آزمون ناموفق بود.")
                        )
                    }
                return@launch
            }
            studentRepo.joinByCode(clean)
                .onSuccess { ex ->
                    openBundle(TeacherPracticeBundle(exam = ex, keyedQuestions = emptyMap()))
                }
                .onFailure { err ->
                    state = state.copy(
                        loading = false,
                        error = UserFacingError.of(err, "ورود به آزمون ممکن نیست.")
                    )
                }
        }
    }

    val currentExam = state.exam
    if (currentExam != null) {
        if (state.showPreview && !state.finished) {
            StudentExamPreview(
                state = state,
                onStart = { state = state.copy(showPreview = false, started = true, error = null) }
            )
            return
        }
        if (state.finished) {
            TeacherPracticeResultContent(
                exam = currentExam,
                answers = state.answers,
                keyedByIndex = keyedByIndex,
                onRetrySame = {
                    openBundle(TeacherPracticeBundle(currentExam, keyedByIndex))
                },
                onAnotherExam = {
                    state = StudentExamUiState(restoringExam = false)
                },
                onBackToCards = {
                    state = StudentExamUiState(restoringExam = false)
                    onBackToCards()
                }
            )
            return
        }
        StudentExamContent(
            state = state,
            onAnswer = { ans ->
                val q = currentExam.questions.getOrNull(state.questionIndex) ?: return@StudentExamContent
                state = state.copy(answers = state.answers + (q.id to ans))
            },
            onPrevious = {
                state = state.copy(
                    questionIndex = (state.questionIndex - 1).coerceAtLeast(0)
                )
            },
            onNext = {
                state = state.copy(
                    questionIndex = (state.questionIndex + 1).coerceAtMost(currentExam.questions.lastIndex.coerceAtLeast(0))
                )
            },
            onGoTo = { idx ->
                state = state.copy(
                    questionIndex = idx.coerceIn(0, currentExam.questions.lastIndex.coerceAtLeast(0))
                )
            },
            onToggleFlag = { qId ->
                val nextFlags = if (qId in state.flaggedQuestionIds) state.flaggedQuestionIds - qId else state.flaggedQuestionIds + qId
                state = state.copy(flaggedQuestionIds = nextFlags)
            },
            onAddImages = { qId, uris ->
                val cur = state.responseImages[qId].orEmpty()
                state = state.copy(responseImages = state.responseImages + (qId to (cur + uris).distinct()))
            },
            onRemoveImage = { qId, uri ->
                val cur = state.responseImages[qId].orEmpty() - uri
                state = state.copy(responseImages = state.responseImages + (qId to cur))
            },
            onSubmit = { state = state.copy(showSubmitReview = true) },
            onConfirmSubmit = { state = state.copy(showSubmitReview = false, finished = true) },
            onDismissSubmit = { state = state.copy(showSubmitReview = false) },
            onDone = { state = StudentExamUiState(restoringExam = false) },
            onExitExam = { state = StudentExamUiState(restoringExam = false) }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeumorphicPanel(
            modifier = Modifier.fillMaxWidth(),
            radius = 24.dp,
            depth = 12.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("شرکت در آزمون با کد", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.code,
                    onValueChange = { v ->
                        state = state.copy(
                            code = v.uppercase().filter { c -> c in 'A'..'Z' || c.isDigit() }.take(12),
                            error = null
                        )
                    },
                    label = { Text("کد ۶ حرفی آزمون") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { startByCode(state.code) },
                    enabled = !state.loading && state.code.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.loading) "در حال دریافت آزمون..." else "ورود به آزمون")
                }
                Text(
                    "در این بخش می‌توانید آزمون‌های خود را دقیقاً در محیط دانش‌آموز پاسخ دهید و نمره را ببینید (بدون ثبت در آمار کلاس).",
                    style = MaterialTheme.typography.bodySmall
                )
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        Text("آزمون‌های من (شروع سریع آزمایشی)", style = MaterialTheme.typography.titleMedium)
        if (loadingExams) {
            CircularProgressIndicator()
        } else if (myExams.isEmpty()) {
            Text("هنوز آزمونی نساخته‌اید.", style = MaterialTheme.typography.bodyMedium)
        } else {
            myExams.forEach { item ->
                NeumorphicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 22.dp,
                    depth = 10.dp,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(item.title.ifBlank { "بدون عنوان" }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    item.subject?.takeIf { it.isNotBlank() } ?: "بدون درس",
                                    item.code?.takeIf { it.isNotBlank() }?.let { "کد $it" },
                                    "${PersianDigits.convert((item.duration ?: 0).toString())} دقیقه",
                                    if (item.isOpen) "باز" else "بسته"
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { startByExamId(item.id) },
                            enabled = !state.loading
                        ) {
                            Text("شروع آزمون")
                        }
                    }
                }
            }
        }
    }
}

private data class QuestionPracticeEval(
    val earned: Double?,
    val maxScore: Double,
    val userAnswerText: String,
    val correctAnswerText: String
)

private val PERSIAN_CHOICE_LETTERS = listOf("الف", "ب", "ج", "د", "ه", "و", "ز", "ح", "ط", "ی")

private fun evaluatePracticeQuestion(
    q: Question,
    draft: QuestionDraft?,
    ans: StudentAnswer?
): QuestionPracticeEval {
    val maxScore = q.score
    if (draft == null) {
        return QuestionPracticeEval(
            earned = null,
            maxScore = maxScore,
            userAnswerText = ans?.toString() ?: "بدون پاسخ",
            correctAnswerText = "—"
        )
    }
    return when (draft.type) {
        QuestionType.MULTIPLE_CHOICE -> {
            val ci = draft.correctIndex
            val correctStr = ci?.let { idx ->
                val opt = draft.options.getOrNull(idx).orEmpty()
                "${PERSIAN_CHOICE_LETTERS.getOrElse(idx) { (idx + 1).toString() }}) $opt"
            } ?: "—"
            val chosen = (ans as? ChoiceAnswer)?.selectedIndex
            val userStr = chosen?.let { idx ->
                val opt = draft.options.getOrNull(idx).orEmpty()
                "${PERSIAN_CHOICE_LETTERS.getOrElse(idx) { (idx + 1).toString() }}) $opt"
            } ?: "بدون پاسخ"
            val earned = if (ci != null) (if (chosen == ci) maxScore else 0.0) else null
            QuestionPracticeEval(earned, maxScore, userStr, correctStr)
        }
        QuestionType.TRUE_FALSE -> {
            val correctBool = draft.expectedText.equals("true", ignoreCase = true)
            val correctStr = if (correctBool) "صحیح" else "غلط"
            val userBool = (ans as? BooleanAnswer)?.value
            val userStr = userBool?.let { if (it) "صحیح" else "غلط" } ?: "بدون پاسخ"
            val earned = if (userBool != null && userBool == correctBool) maxScore else 0.0
            QuestionPracticeEval(earned, maxScore, userStr, correctStr)
        }
        QuestionType.FILL_BLANK -> {
            val accepted = draft.expectedText.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            val correctStr = accepted.joinToString(" یا ").ifBlank { "—" }
            val rawText = (ans as? TextAnswer)?.value?.trim().orEmpty()
            val userStr = rawText.ifBlank { "بدون پاسخ" }
            val earned = if (accepted.isEmpty()) null else if (rawText.isEmpty()) 0.0 else {
                val match = accepted.any { acc ->
                    if (draft.caseSensitive) acc == rawText else acc.equals(rawText, ignoreCase = true)
                }
                if (match) maxScore else 0.0
            }
            QuestionPracticeEval(earned, maxScore, userStr, correctStr)
        }
        QuestionType.NUMERIC -> {
            val expected = draft.expectedNumber.toDoubleOrNull()
            val tol = draft.tolerance.toDoubleOrNull() ?: 0.0
            val correctStr = if (draft.expectedNumber.isBlank()) "—"
            else if (tol > 0.0) "${draft.expectedNumber} ± ${draft.tolerance}" else draft.expectedNumber
            val rawText = (ans as? TextAnswer)?.value?.trim().orEmpty()
            val userStr = rawText.ifBlank { "بدون پاسخ" }
            val userVal = PersianDigits.latin(rawText).toDoubleOrNull()
            val earned = if (expected == null) null else if (userVal != null && abs(userVal - expected) <= tol) maxScore else 0.0
            QuestionPracticeEval(earned, maxScore, userStr, correctStr)
        }
        QuestionType.MATCHING -> {
            val pairs = draft.matchingPairs
            val correctStr = pairs.entries.sortedBy { it.key }.joinToString("، ") { (l, r) ->
                "${l + 1} ← ${PERSIAN_CHOICE_LETTERS.getOrElse(r) { (r + 1).toString() }}"
            }.ifBlank { "—" }
            val userPairs = (ans as? MatchingAnswer)?.pairs.orEmpty()
            val userStr = if (userPairs.isEmpty()) "بدون پاسخ" else {
                userPairs.entries.sortedBy { it.key }.joinToString("، ") { (l, r) ->
                    "${l + 1} ← ${PERSIAN_CHOICE_LETTERS.getOrElse(r) { (r + 1).toString() }}"
                }
            }
            val earned = if (pairs.isEmpty()) null else {
                val ok = pairs.count { (l, r) -> userPairs[l] == r }
                ((maxScore * ok / pairs.size.toDouble()) * 100.0).roundToInt() / 100.0
            }
            QuestionPracticeEval(earned, maxScore, userStr, correctStr)
        }
        QuestionType.ESSAY -> {
            val rawText = (ans as? TextAnswer)?.value?.trim().orEmpty()
            QuestionPracticeEval(
                earned = null,
                maxScore = maxScore,
                userAnswerText = rawText.ifBlank { "بدون پاسخ متنی" },
                correctAnswerText = "تصحیح تشریحی توسط معلم"
            )
        }
    }
}

@Composable
private fun TeacherPracticeResultContent(
    exam: Exam,
    answers: Map<String, StudentAnswer>,
    keyedByIndex: Map<Int, QuestionDraft>,
    onRetrySame: () -> Unit,
    onAnotherExam: () -> Unit,
    onBackToCards: () -> Unit
) {
    val evaluations = remember(exam, answers, keyedByIndex) {
        exam.questions.map { q ->
            q to evaluatePracticeQuestion(q, keyedByIndex[q.originalIndex], answers[q.id])
        }
    }
    val totalScore = evaluations.sumOf { it.second.maxScore }
    val earnedScore = evaluations.sumOf { it.second.earned ?: 0.0 }
    val correctCount = evaluations.count { (_, ev) -> ev.earned != null && ev.earned >= ev.maxScore && ev.maxScore > 0.0 }
    val wrongCount = evaluations.count { (_, ev) -> ev.earned != null && ev.earned < ev.maxScore }
    val essayCount = evaluations.count { (_, ev) -> ev.earned == null }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeumorphicPanel(
            modifier = Modifier.fillMaxWidth(),
            radius = 24.dp,
            depth = 12.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نتیجهٔ آزمایشی آزمون: ${exam.title}", style = MaterialTheme.typography.titleLarge)
                Text(
                    "نمرهٔ کسب‌شده: $earnedScore از $totalScore",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    listOfNotNull(
                        "صحیح: $correctCount",
                        "نادرست/بی‌پاسخ: $wrongCount",
                        essayCount.takeIf { it > 0 }?.let { "تشریحی: $it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "این آزمون در حالت آزمایشی معلم انجام شد و هیچ نمره یا تلاشی در لیست دانش‌آموزان و آمار کلاس ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRetrySame) { Text("شرکت مجدد") }
                    OutlinedButton(onClick = onAnotherExam) { Text("آزمون دیگر") }
                    OutlinedButton(onClick = onBackToCards) { Text("کارت‌ها") }
                }
            }
        }

        evaluations.forEachIndexed { idx, (q, ev) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (ev.earned == null) "سؤال ${idx + 1} · تشریحی (${ev.maxScore} نمره)"
                        else "سؤال ${idx + 1} · نمره: ${ev.earned} از ${ev.maxScore}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (ev.earned != null && ev.earned >= ev.maxScore && ev.maxScore > 0.0)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    NativeMathText(q.text)
                    Text("پاسخ شما: ${ev.userAnswerText}", style = MaterialTheme.typography.bodySmall)
                    Text("پاسخ صحیح: ${ev.correctAnswerText}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
