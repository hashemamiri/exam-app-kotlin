package ir.exam.app.ui.builder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExamBuilderViewModel(
    context: Context,
    private val initialExamId: String? = null,
    private val repository: SupabaseExamBuilderRepository = SupabaseExamBuilderRepository(context)
) : ViewModel() {
    private val _state = MutableStateFlow(ExamBuilderState(loading = true, examId = initialExamId))
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.load(initialExamId)
            .onSuccess { loaded -> _state.value = loaded.copy(loading = false) }
            .onFailure { error -> _state.update { it.copy(loading = false, error = safeBuilderError(error)) } }
    }

    fun setTitle(value: String) { _state.update { it.copy(title = value, error = null) } }
    fun setSubject(value: String) { _state.update { it.copy(subject = value, error = null) } }
    fun setDuration(value: String) { _state.update { it.copy(durationMinutes = value.filter(Char::isDigit), error = null) } }
    fun setNegativeMarking(value: String) { _state.update { it.copy(negativeMarking = value.filter { c -> c.isDigit() || c == '.' }, error = null) } }
    fun setTeacherMessage(value: String) { _state.update { it.copy(teacherMessage = value.take(1000), error = null) } }
    fun setShuffleQuestions(value: Boolean) { _state.update { it.copy(shuffleQuestions = value) } }
    fun setShuffleOptions(value: Boolean) { _state.update { it.copy(shuffleOptions = value) } }
    fun setAttempts(value: Int) { _state.update { it.copy(attemptsAllowed = value.coerceIn(1, 5)) } }
    fun setAttemptOnTimeout(value: Boolean) { _state.update { it.copy(attemptOnTimeout = value) } }
    fun setGradePolicy(value: String) { if (value in setOf("last", "best", "all")) _state.update { it.copy(gradePolicy = value) } }
    fun setAttemptCooldown(value: String) { _state.update { it.copy(attemptCooldown = value.filter(Char::isDigit).take(4)) } }

    fun setAudienceMode(value: String) {
        if (value in setOf("all", "classes", "students")) _state.update { it.copy(audienceMode = value) }
    }
    fun toggleAudienceClass(id: String) { _state.update { it.copy(audienceClasses = it.audienceClasses.toggle(id)) } }
    fun toggleAudienceStudent(id: String) { _state.update { it.copy(audienceStudents = it.audienceStudents.toggle(id)) } }

    fun addQuestion(type: QuestionType) {
        val question = if (type == QuestionType.MULTIPLE_CHOICE) {
            QuestionDraft(type = type, options = List(4) { "" }, optionImages = List(4) { null })
        } else {
            QuestionDraft(type = type)
        }
        _state.update { it.copy(questions = it.questions + question) }
    }

    fun updateText(id: String, text: String) { update(id) { it.copy(text = text) } }
    fun updateScore(id: String, score: String) { update(id) { it.copy(score = score.toDoubleOrNull() ?: 0.0) } }
    fun updateOption(id: String, index: Int, text: String) { update(id) { question ->
        question.copy(options = question.options.mapIndexed { i, old -> if (i == index) text else old })
    } }
    fun setCorrect(id: String, index: Int) { update(id) { it.copy(correctIndex = index) } }
    fun setTrueFalse(id: String, value: Boolean) { update(id) { it.copy(expectedText = value.toString()) } }
    fun updateExpectedText(id: String, value: String) { update(id) { it.copy(expectedText = value) } }
    fun updateExpectedNumber(id: String, value: String) { update(id) { it.copy(expectedNumber = value.filter { c -> c.isDigit() || c == '.' || c == '-' }) } }
    fun updateTolerance(id: String, value: String) { update(id) { it.copy(tolerance = value.filter { c -> c.isDigit() || c == '.' }) } }
    fun remove(id: String) { _state.update { it.copy(questions = it.questions.filterNot { q -> q.id == id }) } }

    fun addImages(questionId: String, uris: List<String>) { update(questionId) { question ->
        question.copy(images = question.images + uris.take(10 - question.images.size).mapIndexed { index, uri ->
            MediaDraft(uri = uri, xMm = 20f + index * 12f, yMm = 30f + index * 12f)
        })
    } }
    fun moveImage(questionId: String, imageId: String, xMm: Float, yMm: Float) { update(questionId) { question ->
        question.copy(images = question.images.map { image ->
            if (image.id == imageId) image.copy(xMm = xMm.coerceIn(0f, 190f), yMm = yMm.coerceIn(0f, 270f)) else image
        })
    } }
    fun removeImage(questionId: String, imageId: String) { update(questionId) { question ->
        question.copy(images = question.images.filterNot { it.id == imageId })
    } }

    private fun update(id: String, change: (QuestionDraft) -> QuestionDraft) {
        _state.update { state -> state.copy(questions = state.questions.map { if (it.id == id) change(it) else it }) }
    }

    fun save() = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, savedCode = null, uploadProgress = null) }
        repository.save(state.value) { done, total ->
            _state.update { it.copy(uploadProgress = "آپلود تصویر $done از $total") }
        }.onSuccess { code ->
            _state.update { it.copy(saving = false, savedCode = code, uploadProgress = null) }
        }.onFailure { error ->
            _state.update { it.copy(saving = false, uploadProgress = null, error = safeBuilderError(error)) }
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

private fun safeBuilderError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(260)
    .ifBlank { "ذخیره آزمون ناموفق بود." }
