package ir.exam.app.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.PendingActionRepository
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.PendingSubmissionStatus
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.model.SubmittedExam
import ir.exam.app.domain.repository.AnswerDraftRepository
import ir.exam.app.domain.repository.ExamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentExamUiState(
    val code: String = "",
    val exam: Exam? = null,
    val answers: Map<String, StudentAnswer> = emptyMap(),
    val responseImages: Map<String, List<String>> = emptyMap(),
    val questionIndex: Int = 0,
    val flaggedQuestionIds: Set<String> = emptySet(),
    val showSubmitReview: Boolean = false,
    /** مقدار -1 یعنی آزمون بدون محدودیت زمانی است. */
    val remainingSeconds: Long = UNLIMITED_TIME,
    val restoringExam: Boolean = true,
    val showPreview: Boolean = false,
    val resumedExam: Boolean = false,
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val finished: Boolean = false,
    val queued: Boolean = false,
    val submissionMessage: String? = null,
    val pendingSubmissions: List<PendingSubmissionStatus> = emptyList(),
    val error: String? = null
)

const val UNLIMITED_TIME = -1L

class StudentExamViewModel(
    private val exams: ExamRepository,
    private val drafts: AnswerDraftRepository,
    private val pending: PendingActionRepository? = null,
    private val ownerUserId: String = ""
) : ViewModel() {
    private val _state = MutableStateFlow(StudentExamUiState())
    val state = _state.asStateFlow()
    private var timer: Job? = null
    private var draftObserver: Job? = null

    init {
        if (pending != null && ownerUserId.isNotBlank()) {
            pending.schedule()
            viewModelScope.launch {
                pending.observeSubmissions(ownerUserId).collect { rows ->
                    _state.update { it.copy(pendingSubmissions = rows) }
                }
            }
        }
        restoreActiveExam()
    }

    private fun restoreActiveExam() = viewModelScope.launch {
        _state.update { it.copy(restoringExam = true, error = null) }
        exams.restoreActiveExam()
            .onSuccess { exam ->
                if (exam == null) _state.update { it.copy(restoringExam = false) }
                else openExam(exam, resumed = true)
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        restoringExam = false,
                        error = safeStudentError(error, "بازیابی آزمون فعال ناموفق بود.")
                    )
                }
            }
    }

    fun setCode(value: String) {
        _state.update { it.copy(code = value.trim().uppercase(), error = null) }
    }

    fun join() = viewModelScope.launch {
        if (state.value.loading) return@launch
        _state.update { it.copy(loading = true, error = null, submissionMessage = null) }
        exams.joinByCode(state.value.code)
            .onSuccess { exam -> openExam(exam, resumed = false) }
            .onFailure { error ->
                _state.update { it.copy(error = safeStudentError(error, "ورود به آزمون ممکن نیست")) }
            }
        _state.update { it.copy(loading = false, restoringExam = false) }
    }

    fun startExam() {
        if (state.value.exam == null || state.value.finished) return
        _state.update { it.copy(showPreview = false, error = null) }
    }

    private suspend fun openExam(exam: Exam, resumed: Boolean) {
        timer?.cancel()
        draftObserver?.cancel()
        val draft = runCatching { drafts.load(exam.id) }.getOrElse { StudentDraft() }
        _state.update {
            it.copy(
                code = exam.code,
                exam = exam,
                answers = draft.answers,
                responseImages = draft.responseImages,
                questionIndex = draft.lastQuestionIndex.coerceIn(0,exam.questions.lastIndex.coerceAtLeast(0)),
                flaggedQuestionIds = draft.flaggedQuestionIds,
                showSubmitReview = false,
                remainingSeconds = remainingFor(exam),
                restoringExam = false,
                showPreview = true,
                resumedExam = resumed,
                loading = false,
                submitting = false,
                finished = false,
                queued = false,
                submissionMessage = null,
                error = null
            )
        }
        observeDrafts(exam.id)
        startTimer()
    }

    private fun observeDrafts(examId: String) {
        draftObserver?.cancel()
        draftObserver = viewModelScope.launch {
            drafts.observe(examId).collect { draft ->
                if (state.value.exam?.id == examId && !state.value.finished) {
                    _state.update { it.copy(
                        answers = draft.answers,
                        responseImages = draft.responseImages,
                        flaggedQuestionIds = draft.flaggedQuestionIds
                    ) }
                }
            }
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (!state.value.finished) {
                val exam = state.value.exam ?: return@launch
                val remaining = remainingFor(exam)
                _state.update { it.copy(remainingSeconds = remaining) }
                if (remaining == UNLIMITED_TIME) return@launch
                if (remaining <= 0L) {
                    val refreshed = exams.refreshActiveExam().getOrNull()
                    if (refreshed != null && remainingFor(refreshed) > 0L) {
                        _state.update { it.copy(exam = refreshed, remainingSeconds = remainingFor(refreshed)) }
                        continue
                    }
                    submit()
                    return@launch
                }
                delay(1_000L.coerceAtMost(remaining * 1_000L))
            }
        }
    }

    private fun remainingFor(exam: Exam): Long {
        val deadline = exam.deadlineEpochMs ?: return UNLIMITED_TIME
        val millis = deadline - System.currentTimeMillis()
        return if (millis <= 0L) 0L else (millis + 999L) / 1_000L
    }

    fun answer(answer: StudentAnswer) {
        val exam = state.value.exam ?: return
        val answers = state.value.answers + (answer.questionId to answer)
        _state.update { it.copy(answers = answers, error = null) }
        saveDraft(exam.id, answers, state.value.responseImages)
    }

    fun addResponseImages(questionId: String, uris: List<String>) {
        val exam = state.value.exam ?: return
        val max = exam.questions.firstOrNull { it.id == questionId }?.maxAnswerImages ?: 0
        if (max <= 0) return
        val current = state.value.responseImages[questionId].orEmpty()
        val next = (current + uris.take((max - current.size).coerceAtLeast(0))).distinct()
        val images = state.value.responseImages + (questionId to next)
        _state.update { it.copy(responseImages = images, error = null) }
        saveDraft(exam.id, state.value.answers, images)
    }

    fun removeResponseImage(questionId: String, uri: String) {
        val exam = state.value.exam ?: return
        val images = state.value.responseImages + (
            questionId to state.value.responseImages[questionId].orEmpty().filterNot { it == uri }
        )
        _state.update { it.copy(responseImages = images) }
        saveDraft(exam.id, state.value.answers, images)
    }

    private fun saveDraft(examId: String, answers: Map<String, StudentAnswer>, images: Map<String, List<String>>) {
        val current=state.value
        viewModelScope.launch { drafts.save(examId, StudentDraft(answers,images,current.flaggedQuestionIds,current.questionIndex)) }
    }

    fun goTo(index: Int) {
        val exam=state.value.exam ?: return
        val next=index.coerceIn(0,exam.questions.lastIndex)
        _state.update { it.copy(questionIndex = next, error = null) }
        saveDraft(exam.id,state.value.answers,state.value.responseImages)
    }

    fun toggleFlag(questionId:String){
        val exam=state.value.exam?:return
        _state.update{it.copy(flaggedQuestionIds=if(questionId in it.flaggedQuestionIds)it.flaggedQuestionIds-questionId else it.flaggedQuestionIds+questionId)}
        saveDraft(exam.id,state.value.answers,state.value.responseImages)
    }

    fun requestSubmitReview(){if(state.value.exam!=null&&!state.value.submitting)_state.update{it.copy(showSubmitReview=true,error=null)}}
    fun dismissSubmitReview(){_state.update{it.copy(showSubmitReview=false)}}
    fun confirmSubmit(){_state.update{it.copy(showSubmitReview=false)};submit()}

    fun submit() {
        val exam = state.value.exam ?: return
        if (state.value.submitting || state.value.finished) return
        val missingImageIndex = exam.questions.indexOfFirst { question ->
            question.answerImagesRequired && state.value.responseImages[question.id].isNullOrEmpty()
        }
        if (missingImageIndex >= 0) {
            _state.update {
                it.copy(
                    questionIndex = missingImageIndex,
                    showPreview = false,
                    error = "ارسال تصویر پاسخ برای این سؤال اجباری است."
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null, showPreview = false) }
            val attempt = SubmittedExam(
                examId = exam.id,
                answers = state.value.answers,
                responseImages = state.value.responseImages,
                submittedAtEpochMs = System.currentTimeMillis()
            )
            exams.submitAttempt(attempt)
                .onSuccess { outcome ->
                    timer?.cancel()
                    runCatching { exams.clearActiveExam(exam.id).getOrThrow() }
                    when (outcome) {
                        is SubmissionOutcome.Sent -> {
                            drafts.clear(exam.id)
                            _state.update {
                                it.copy(
                                    submitting = false,
                                    finished = true,
                                    queued = false,
                                    submissionMessage = outcome.receipt?.let { receipt -> "کد رهگیری: $receipt" }
                                )
                            }
                        }
                        is SubmissionOutcome.Queued -> _state.update {
                            it.copy(
                                submitting = false,
                                finished = true,
                                queued = true,
                                submissionMessage = "پاسخ در حافظه امن دستگاه صف شد و با اتصال اینترنت خودکار ارسال می‌شود."
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = safeStudentError(error, "ارسال پاسخ ناموفق بود")
                        )
                    }
                }
        }
    }

    fun leaveFinishedExam() {
        if (!state.value.finished) return
        draftObserver?.cancel()
        _state.update {
            StudentExamUiState(
                restoringExam = false,
                pendingSubmissions = it.pendingSubmissions,
                submissionMessage = it.submissionMessage
            )
        }
    }

    fun retryPending() {
        val queue = pending ?: return
        if (ownerUserId.isBlank()) return
        viewModelScope.launch { queue.retryAll(ownerUserId) }
    }

    fun clearFailedPending() {
        val queue = pending ?: return
        if (ownerUserId.isBlank()) return
        viewModelScope.launch { queue.deleteFailed(ownerUserId) }
    }

    override fun onCleared() {
        timer?.cancel()
        draftObserver?.cancel()
    }
}

private fun safeStudentError(error: Throwable, fallback: String): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .replace(Regex("https?://\\S+"), "")
    .take(260)
    .ifBlank { fallback }
