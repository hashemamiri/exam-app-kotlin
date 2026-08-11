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
    val remainingSeconds: Long = 0,
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val finished: Boolean = false,
    val queued: Boolean = false,
    val submissionMessage: String? = null,
    val pendingSubmissions: List<PendingSubmissionStatus> = emptyList(),
    val error: String? = null
)

class StudentExamViewModel(
    private val exams: ExamRepository,
    private val drafts: AnswerDraftRepository,
    private val pending: PendingActionRepository? = null,
    private val ownerUserId: String = ""
) : ViewModel() {
    private val _state = MutableStateFlow(StudentExamUiState())
    val state = _state.asStateFlow()
    private var timer: Job? = null

    init {
        if (pending != null && ownerUserId.isNotBlank()) {
            pending.schedule()
            viewModelScope.launch {
                pending.observeSubmissions(ownerUserId).collect { rows ->
                    _state.update { it.copy(pendingSubmissions = rows) }
                }
            }
        }
    }

    fun setCode(value: String) { _state.update { it.copy(code = value.trim(), error = null) } }

    fun join() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        exams.joinByCode(state.value.code)
            .onSuccess { exam ->
                _state.update { it.copy(exam = exam, remainingSeconds = exam.durationMinutes * 60L) }
                startTimer()
                observeDrafts(exam.id)
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "ورود به آزمون ممکن نیست") } }
        _state.update { it.copy(loading = false) }
    }

    private fun observeDrafts(examId: String) = viewModelScope.launch {
        drafts.observe(examId).collect { draft ->
            _state.update { it.copy(answers = draft.answers, responseImages = draft.responseImages) }
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (state.value.remainingSeconds > 0 && !state.value.finished) {
                delay(1000)
                _state.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)) }
            }
            if (state.value.remainingSeconds == 0L) submit()
        }
    }

    fun answer(answer: StudentAnswer) {
        val exam = state.value.exam ?: return
        val answers = state.value.answers + (answer.questionId to answer)
        _state.update { it.copy(answers = answers) }
        saveDraft(exam.id, answers, state.value.responseImages)
    }

    fun addResponseImages(questionId: String, uris: List<String>) {
        val exam = state.value.exam ?: return
        val max = exam.questions.firstOrNull { it.id == questionId }?.maxAnswerImages ?: 0
        if (max <= 0) return
        val current = state.value.responseImages[questionId].orEmpty()
        val next = current + uris.take((max - current.size).coerceAtLeast(0))
        val images = state.value.responseImages + (questionId to next)
        _state.update { it.copy(responseImages = images) }
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
        viewModelScope.launch { drafts.save(examId, StudentDraft(answers, images)) }
    }

    fun goTo(index: Int) {
        val max = state.value.exam?.questions?.lastIndex ?: 0
        _state.update { it.copy(questionIndex = index.coerceIn(0, max)) }
    }

    fun submit() {
        val exam = state.value.exam ?: return
        if (state.value.submitting || state.value.finished) return
        val missingImageIndex = exam.questions.indexOfFirst { question ->
            question.answerImagesRequired && state.value.responseImages[question.id].isNullOrEmpty()
        }
        if (missingImageIndex >= 0) {
            _state.update { it.copy(questionIndex = missingImageIndex, error = "ارسال تصویر پاسخ برای این سؤال اجباری است.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val attempt = SubmittedExam(
                examId = exam.id,
                answers = state.value.answers,
                responseImages = state.value.responseImages,
                submittedAtEpochMs = System.currentTimeMillis()
            )
            exams.submitAttempt(attempt)
                .onSuccess { outcome ->
                    timer?.cancel()
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
                    _state.update { it.copy(submitting = false, error = error.message ?: "ارسال پاسخ ناموفق بود") }
                }
        }
    }

    fun leaveFinishedExam() {
        if (!state.value.finished) return
        _state.update {
            StudentExamUiState(
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
    }
}
