package ir.exam.app.ui.builder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import java.util.UUID
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
    /** در تکرار پس از قطع پاسخ شبکه همان شناسه می‌ماند تا سرور دوباره پول کم نکند. */
    private var saveOperationId: String = UUID.randomUUID().toString()

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
        val question = when (type) {
            QuestionType.MULTIPLE_CHOICE -> QuestionDraft(
                type = type,
                options = List(4) { "" },
                optionImages = List(4) { null }
            )
            QuestionType.MATCHING -> QuestionDraft(
                type = type,
                matchingLeft = List(3) { "" },
                matchingRight = List(3) { "" },
                matchingPairs = mapOf(0 to 0, 1 to 1, 2 to 2),
                matchingLeftImages = List(3) { null },
                matchingRightImages = List(3) { null }
            )
            else -> QuestionDraft(type = type)
        }
        _state.update { it.copy(questions = it.questions + question) }
    }

    fun updateText(id: String, text: String) { update(id) { it.copy(text = text) } }
    fun updateScore(id: String, score: String) { update(id) { it.copy(score = score.toDoubleOrNull() ?: 0.0) } }
    fun updateOption(id: String, index: Int, text: String) { update(id) { question ->
        question.copy(options = question.options.mapIndexed { i, old -> if (i == index) text else old })
    } }
    fun setOptionImage(id: String, index: Int, uri: String?) { update(id) { question ->
        val images = question.options.indices.map { i -> if (i == index) uri else question.optionImages.getOrNull(i) }
        question.copy(optionImages = images)
    } }
    fun setCorrect(id: String, index: Int) { update(id) { it.copy(correctIndex = index) } }

    fun updateMatchingText(id: String, side: String, index: Int, text: String) { update(id) { question ->
        if (side == "left") question.copy(matchingLeft = question.matchingLeft.replaceAt(index, text))
        else question.copy(matchingRight = question.matchingRight.replaceAt(index, text))
    } }
    fun setMatchingImage(id: String, side: String, index: Int, uri: String?) { update(id) { question ->
        if (side == "left") question.copy(matchingLeftImages = question.matchingLeftImages.pad(question.matchingLeft.size).replaceAt(index, uri))
        else question.copy(matchingRightImages = question.matchingRightImages.pad(question.matchingRight.size).replaceAt(index, uri))
    } }
    fun setMatchingPair(id: String, leftIndex: Int, rightIndex: Int) { update(id) { question ->
        question.copy(matchingPairs = question.matchingPairs + (leftIndex to rightIndex))
    } }
    fun addMatchingRow(id: String) { update(id) { question ->
        val next = minOf(question.matchingLeft.size, question.matchingRight.size)
        question.copy(
            matchingLeft = question.matchingLeft + "",
            matchingRight = question.matchingRight + "",
            matchingLeftImages = question.matchingLeftImages.pad(question.matchingLeft.size) + null,
            matchingRightImages = question.matchingRightImages.pad(question.matchingRight.size) + null,
            matchingPairs = question.matchingPairs + (next to next)
        )
    } }
    fun removeMatchingRow(id: String) { update(id) { question ->
        if (question.matchingLeft.size <= 2 || question.matchingRight.size <= 2) question else {
            val last = minOf(question.matchingLeft.lastIndex, question.matchingRight.lastIndex)
            question.copy(
                matchingLeft = question.matchingLeft.dropLast(1),
                matchingRight = question.matchingRight.dropLast(1),
                matchingLeftImages = question.matchingLeftImages.dropLast(1),
                matchingRightImages = question.matchingRightImages.dropLast(1),
                matchingPairs = question.matchingPairs.filterKeys { it != last }.mapValues { (_, right) -> right.coerceAtMost(last - 1) }
            )
        }
    } }
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
    fun setAnswerImageMode(questionId: String, mode: String) { update(questionId) { question ->
        if (mode !in setOf("no", "optional", "required")) question
        else question.copy(answerImageMode = mode, maxAnswerImages = if (mode == "no") 0 else question.maxAnswerImages.coerceAtLeast(1))
    } }
    fun setMaxAnswerImages(questionId: String, max: Int) { update(questionId) { question ->
        question.copy(maxAnswerImages = max.coerceIn(1, 10))
    } }

    private fun update(id: String, change: (QuestionDraft) -> QuestionDraft) {
        _state.update { state -> state.copy(questions = state.questions.map { if (it.id == id) change(it) else it }) }
    }

    fun addFromBank(id: Long) {
        val item = state.value.bankQuestions.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(questions = it.questions + item.question.copy(id = UUID.randomUUID().toString())) }
    }

    fun saveToBank(questionId: String) {
        val question = state.value.questions.firstOrNull { it.id == questionId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(bankLoading = true, error = null) }
            repository.saveToBank(question, state.value.subject)
                .onSuccess { refreshBankNow() }
                .onFailure { error -> _state.update { it.copy(bankLoading = false, error = safeBuilderError(error)) } }
        }
    }

    fun deleteFromBank(id: Long) = viewModelScope.launch {
        _state.update { it.copy(bankLoading = true, error = null) }
        repository.deleteFromBank(id)
            .onSuccess { refreshBankNow() }
            .onFailure { error -> _state.update { it.copy(bankLoading = false, error = safeBuilderError(error)) } }
    }

    private suspend fun refreshBankNow() {
        val bank = repository.refreshBank().getOrThrow()
        _state.update { it.copy(bankQuestions = bank, bankLoading = false) }
    }

    fun save() = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, savedCode = null, uploadProgress = null) }
        repository.save(state.value, saveOperationId) { done, total ->
            _state.update { it.copy(uploadProgress = "آپلود تصویر $done از $total") }
        }.onSuccess { result ->
            saveOperationId = UUID.randomUUID().toString()
            _state.update {
                it.copy(
                    saving = false,
                    savedCode = result.code,
                    chargedToman = result.chargedToman,
                    walletBalanceToman = result.walletBalanceToman,
                    uploadProgress = null
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(saving = false, uploadProgress = null, error = safeBuilderError(error)) }
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id
private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> = mapIndexed { i, old -> if (i == index) value else old }
private fun List<String?>.pad(size: Int): List<String?> = if (this.size >= size) this else this + List(size - this.size) { null }

private fun safeBuilderError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(260)
    .ifBlank { "ذخیره آزمون ناموفق بود." }
