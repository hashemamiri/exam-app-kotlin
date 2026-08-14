package ir.exam.app.ui.builder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import ir.exam.app.core.math.FormulaTextCodec
import ir.exam.app.data.local.NativeDatabaseProvider
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.data.repository.ExamBuilderDraftStore
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import java.util.UUID
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExamBuilderViewModel(
    context: Context,
    private val initialExamId: String? = null,
    private val initialImport: ExamImportDraft? = null,
    private val repository: SupabaseExamBuilderRepository = SupabaseExamBuilderRepository(context)
) : ViewModel() {
    private val appContext = context.applicationContext
    private val ownerUserId = SupabaseProvider.client.auth.currentUserOrNull()?.id.orEmpty()
    private val draftStore = ExamBuilderDraftStore(
        NativeDatabaseProvider.get(appContext).examBuilderDraftDao()
    )
    private val _state = MutableStateFlow(ExamBuilderState(loading = true, examId = initialExamId))
    val state = _state.asStateFlow()
    /** در تکرار پس از قطع پاسخ شبکه همان شناسه می‌ماند تا سرور دوباره پول کم نکند. */
    private var saveOperationId: String = UUID.randomUUID().toString()
    private var cleanDraftFingerprint: Int? = null

    init {
        load()
        startDraftAutoSave()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.load(if (initialImport == null) initialExamId else null)
            .onSuccess { loaded ->
                _state.value = initialImport?.let { imported ->
                    loaded.copy(
                        loading = false,
                        examId = null,
                        code = null,
                        title = imported.title,
                        subject = imported.subject,
                        durationMinutes = imported.durationMinutes.toString(),
                        opensAtIso = imported.opensAtIso,
                        closesAtIso = imported.closesAtIso,
                        negativeMarking = imported.negativeMarking.toString(),
                        shuffleQuestions = imported.shuffleQuestions,
                        shuffleOptions = imported.shuffleOptions,
                        teacherMessage = imported.teacherMessage,
                        attemptsAllowed = imported.attemptsAllowed,
                        attemptOnTimeout = imported.attemptOnTimeout,
                        gradePolicy = imported.gradePolicy,
                        attemptCooldown = imported.attemptCooldown.toString(),
                        questions = imported.questions,
                        importedBy = imported.exportedBy
                    )
                } ?: loaded.copy(loading = false)
                cleanDraftFingerprint = draftFingerprint(_state.value)
                if (initialImport == null && ownerUserId.isNotBlank()) {
                    val draft = draftStore.load(ownerUserId)
                    if (draft != null && draft.examId == initialExamId &&
                        (draft.title.isNotBlank() || draft.questions.isNotEmpty())) {
                        _state.update { it.copy(recoverableDraft = draft) }
                    }
                }
            }
            .onFailure { error -> _state.update { it.copy(loading = false, error = safeBuilderError(error)) } }
    }

    @OptIn(FlowPreview::class)
    private fun startDraftAutoSave() {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            state.drop(1).debounce(900).collect { current ->
                val fingerprint = draftFingerprint(current)
                if (!current.loading && !current.saving && current.savedCode == null &&
                    current.recoverableDraft == null && fingerprint != cleanDraftFingerprint) {
                    draftStore.save(ownerUserId, current)
                }
            }
        }
    }

    fun restoreDraft() {
        val draft = state.value.recoverableDraft ?: return
        _state.update { current ->
            current.copy(
                examId = draft.examId,
                title = draft.title,
                subject = draft.subject,
                durationMinutes = draft.durationMinutes,
                opensAtIso = draft.opensAtIso,
                closesAtIso = draft.closesAtIso,
                questions = draft.questions,
                shuffleQuestions = draft.shuffleQuestions,
                shuffleOptions = draft.shuffleOptions,
                negativeMarking = draft.negativeMarking,
                teacherMessage = draft.teacherMessage,
                attemptsAllowed = draft.attemptsAllowed,
                attemptOnTimeout = draft.attemptOnTimeout,
                gradePolicy = draft.gradePolicy,
                attemptCooldown = draft.attemptCooldown,
                audienceMode = draft.audienceMode,
                audienceClasses = draft.audienceClasses,
                audienceStudents = draft.audienceStudents,
                recoverableDraft = null,
                error = null
            )
        }
    }

    fun discardDraft() = viewModelScope.launch {
        if (ownerUserId.isNotBlank()) draftStore.clear(ownerUserId)
        _state.update { it.copy(recoverableDraft = null) }
        cleanDraftFingerprint = draftFingerprint(_state.value)
    }

    fun setTitle(value: String) { _state.update { it.copy(title = value, error = null) } }
    fun setSubject(value: String) { _state.update { it.copy(subject = value, error = null) } }
    fun setDuration(value: String) { _state.update { it.copy(durationMinutes = value.filter(Char::isDigit), error = null) } }
    fun setOpensAt(value: String?) { _state.update { it.copy(opensAtIso = value, error = null) } }
    fun setClosesAt(value: String?) { _state.update { it.copy(closesAtIso = value, error = null) } }
    fun setNegativeMarking(value: String) { _state.update { it.copy(negativeMarking = value.filter { c -> c.isDigit() || c == '.' }, error = null) } }
    fun setTeacherMessage(value: String) { _state.update { it.copy(teacherMessage = value.take(1000), error = null) } }
    fun setShuffleQuestions(value: Boolean) { _state.update { it.copy(shuffleQuestions = value) } }
    fun setShuffleOptions(value: Boolean) { _state.update { it.copy(shuffleOptions = value) } }
    fun setAttempts(value: Int) { _state.update { it.copy(attemptsAllowed = value.coerceIn(1, 5)) } }
    fun setAttemptOnTimeout(value: Boolean) { _state.update { it.copy(attemptOnTimeout = value) } }
    fun setGradePolicy(value: String) { if (value in setOf("last", "best", "all")) _state.update { it.copy(gradePolicy = value) } }
    fun setAttemptCooldown(value: String) { _state.update { it.copy(attemptCooldown = value.filter(Char::isDigit).take(4)) } }
    fun reportError(error: Throwable) {
        _state.update { it.copy(error = safeBuilderError(error)) }
    }

    fun setAudienceMode(value: String) {
        if (value in setOf("all", "classes", "students")) _state.update { it.copy(audienceMode = value) }
    }
    fun toggleAudienceClass(id: String) { _state.update { it.copy(audienceClasses = it.audienceClasses.toggle(id)) } }
    fun toggleAudienceStudent(id: String) { _state.update { it.copy(audienceStudents = it.audienceStudents.toggle(id)) } }

    fun addQuestion(type: QuestionType): String {
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
        return question.id
    }

    fun applyImport(imported: ExamImportDraft) {
        _state.update { current ->
            current.copy(
                examId = null,
                code = null,
                title = imported.title,
                subject = imported.subject,
                durationMinutes = imported.durationMinutes.toString(),
                opensAtIso = imported.opensAtIso,
                closesAtIso = imported.closesAtIso,
                negativeMarking = imported.negativeMarking.toString(),
                shuffleQuestions = imported.shuffleQuestions,
                shuffleOptions = imported.shuffleOptions,
                teacherMessage = imported.teacherMessage,
                attemptsAllowed = imported.attemptsAllowed,
                attemptOnTimeout = imported.attemptOnTimeout,
                gradePolicy = imported.gradePolicy,
                attemptCooldown = imported.attemptCooldown.toString(),
                questions = imported.questions.map { it.copy(id = UUID.randomUUID().toString()) },
                importedBy = imported.exportedBy,
                savedCode = null,
                error = null
            )
        }
    }

    fun moveQuestion(id: String, delta: Int) {
        _state.update { state ->
            val from = state.questions.indexOfFirst { it.id == id }
            val to = (from + delta).coerceIn(0, state.questions.lastIndex)
            if (from < 0 || from == to) state else {
                val list = state.questions.toMutableList()
                val item = list.removeAt(from)
                list.add(to, item)
                state.copy(questions = list)
            }
        }
    }

    fun updateText(id: String, text: String) { update(id) { it.copy(text = text) } }
    fun insertFormula(
        id: String,
        target: String,
        index: Int?,
        tex: String,
        occurrenceIndex: Int? = null
    ) {
        update(id) { question ->
            when (target) {
                "question" -> question.copy(text = FormulaTextCodec.upsert(question.text, occurrenceIndex, tex))
                "option" -> question.copy(
                    options = question.options.mapIndexed { i, value ->
                        if (i == index) FormulaTextCodec.upsert(value, occurrenceIndex, tex) else value
                    }
                )
                "matching_left" -> question.copy(
                    matchingLeft = question.matchingLeft.mapIndexed { i, value ->
                        if (i == index) FormulaTextCodec.upsert(value, occurrenceIndex, tex) else value
                    }
                )
                "matching_right" -> question.copy(
                    matchingRight = question.matchingRight.mapIndexed { i, value ->
                        if (i == index) FormulaTextCodec.upsert(value, occurrenceIndex, tex) else value
                    }
                )
                else -> question
            }
        }
    }

    fun deleteFormula(id: String, target: String, index: Int?, occurrenceIndex: Int) {
        update(id) { question ->
            when (target) {
                "question" -> question.copy(text = FormulaTextCodec.delete(question.text, occurrenceIndex))
                "option" -> question.copy(options = question.options.mapIndexed { i, value ->
                    if (i == index) FormulaTextCodec.delete(value, occurrenceIndex) else value
                })
                "matching_left" -> question.copy(matchingLeft = question.matchingLeft.mapIndexed { i, value ->
                    if (i == index) FormulaTextCodec.delete(value, occurrenceIndex) else value
                })
                "matching_right" -> question.copy(matchingRight = question.matchingRight.mapIndexed { i, value ->
                    if (i == index) FormulaTextCodec.delete(value, occurrenceIndex) else value
                })
                else -> question
            }
        }
    }
    fun updateScore(id: String, score: String) { update(id) { it.copy(score = score.toDoubleOrNull() ?: 0.0) } }
    fun updateOption(id: String, index: Int, text: String) { update(id) { question ->
        question.copy(options = question.options.mapIndexed { i, old -> if (i == index) text else old })
    } }
    fun setOptionImage(id: String, index: Int, uri: String?) { update(id) { question ->
        val images = question.options.indices.map { i -> if (i == index) uri else question.optionImages.getOrNull(i) }
        question.copy(optionImages = images)
    } }
    fun setOptionCount(id: String, count: Int) { update(id) { question ->
        val size = count.coerceIn(2, 10)
        val options = question.options.take(size) + List((size - question.options.size).coerceAtLeast(0)) { "" }
        val images = question.optionImages.take(size) + List((size - question.optionImages.size).coerceAtLeast(0)) { null }
        question.copy(options = options, optionImages = images, correctIndex = question.correctIndex?.coerceAtMost(size - 1))
    } }
    fun moveOption(id: String, index: Int, delta: Int) { update(id) { question ->
        val to = (index + delta).coerceIn(0, question.options.lastIndex)
        if (index !in question.options.indices || index == to) question else {
            val options = question.options.toMutableList().apply { add(to, removeAt(index)) }
            val images = question.optionImages.pad(question.options.size).toMutableList().apply { add(to, removeAt(index)) }
            val current = question.correctIndex
            val correct = when {
                current == index -> to
                current != null && current in minOf(index, to)..maxOf(index, to) -> if (index < to) current - 1 else current + 1
                else -> current
            }
            question.copy(options = options, optionImages = images, correctIndex = correct)
        }
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
    fun addMatchingSide(id: String, side: String) { update(id) { q ->
        if (side == "left" && q.matchingLeft.size < 30) q.copy(
            matchingLeft = q.matchingLeft + "",
            matchingLeftImages = q.matchingLeftImages.pad(q.matchingLeft.size) + null
        ) else if (side == "right" && q.matchingRight.size < 30) q.copy(
            matchingRight = q.matchingRight + "",
            matchingRightImages = q.matchingRightImages.pad(q.matchingRight.size) + null
        ) else q
    } }
    fun removeMatchingSide(id: String, side: String, index: Int) { update(id) { q ->
        if (side == "left" && q.matchingLeft.size > 2 && index in q.matchingLeft.indices) {
            val pairs = q.matchingPairs.mapNotNull { (left, right) ->
                when { left == index -> null; left > index -> (left - 1) to right; else -> left to right }
            }.toMap()
            q.copy(
                matchingLeft = q.matchingLeft.filterIndexed { i, _ -> i != index },
                matchingLeftImages = q.matchingLeftImages.pad(q.matchingLeft.size).filterIndexed { i, _ -> i != index },
                matchingPairs = pairs
            )
        } else if (side == "right" && q.matchingRight.size > 2 && index in q.matchingRight.indices) {
            val pairs = q.matchingPairs.mapNotNull { (left, right) ->
                when { right == index -> null; right > index -> left to (right - 1); else -> left to right }
            }.toMap()
            q.copy(
                matchingRight = q.matchingRight.filterIndexed { i, _ -> i != index },
                matchingRightImages = q.matchingRightImages.pad(q.matchingRight.size).filterIndexed { i, _ -> i != index },
                matchingPairs = pairs
            )
        } else q
    } }
    fun moveMatchingItem(id: String, side: String, index: Int, delta: Int) { update(id) { q ->
        val size = if (side == "left") q.matchingLeft.size else q.matchingRight.size
        val to = (index + delta).coerceIn(0, size - 1)
        if (index !in 0 until size || index == to) q else if (side == "left") {
            val values = q.matchingLeft.toMutableList().apply { add(to, removeAt(index)) }
            val images = q.matchingLeftImages.pad(size).toMutableList().apply { add(to, removeAt(index)) }
            val pairs = q.matchingPairs.mapKeys { (left, _) -> remapMovedIndex(left, index, to) }
            q.copy(matchingLeft = values, matchingLeftImages = images, matchingPairs = pairs)
        } else {
            val values = q.matchingRight.toMutableList().apply { add(to, removeAt(index)) }
            val images = q.matchingRightImages.pad(size).toMutableList().apply { add(to, removeAt(index)) }
            val pairs = q.matchingPairs.mapValues { (_, right) -> remapMovedIndex(right, index, to) }
            q.copy(matchingRight = values, matchingRightImages = images, matchingPairs = pairs)
        }
    } }
    fun setCaseSensitive(id: String, value: Boolean) { update(id) { it.copy(caseSensitive = value) } }
    fun setQuestionAlign(id: String, value: String) { if (value in setOf("right","center","left","justify")) update(id) { it.copy(textAlign=value) } }
    fun setImagePosition(id: String, value: String) { if (value in setOf("above","below","right","left","free")) update(id) { it.copy(imagePosition=value) } }
    fun setQuestionFont(id: String, value: String) { update(id) { it.copy(fontFamily=value.take(30)) } }
    fun setQuestionFontSize(id: String, value: Float) { update(id) { it.copy(fontSizeSp=value.coerceIn(8f,40f)) } }
    fun setQuestionBold(id: String, value: Boolean) { update(id) { it.copy(bold=value) } }
    fun setQuestionItalic(id: String, value: Boolean) { update(id) { it.copy(italic=value) } }
    fun setAnswerLines(id: String, value: Int) { update(id) { it.copy(answerLines=value.coerceIn(0,12)) } }
    fun setAnswerLineStyle(id: String, value: String) { if (value in setOf("lined","blank")) update(id) { it.copy(answerLineStyle=value) } }
    fun setTrueFalse(id: String, value: Boolean) { update(id) { it.copy(expectedText = value.toString()) } }
    fun updateExpectedText(id: String, value: String) { update(id) { it.copy(expectedText = value) } }
    fun updateExpectedNumber(id: String, value: String) { update(id) { it.copy(expectedNumber = value.filter { c -> c.isDigit() || c == '.' || c == '-' }) } }
    fun updateTolerance(id: String, value: String) { update(id) { it.copy(tolerance = value.filter { c -> c.isDigit() || c == '.' }) } }
    fun remove(id: String) { _state.update { it.copy(questions = it.questions.filterNot { q -> q.id == id }) } }

    fun addImages(questionId: String, uris: List<String>) { update(questionId) { question ->
        question.copy(images = question.images + uris.take(10 - question.images.size).mapIndexed { index, uri ->
            val slot = question.images.size + index
            MediaDraft(
                uri = uri,
                xMm = 8f + (slot % 2) * 85f,
                yMm = 10f + (slot / 2) * 68f
            )
        })
    } }
    fun moveImage(questionId: String, imageId: String, xMm: Float, yMm: Float) { update(questionId) { question ->
        question.copy(images = question.images.map { image ->
            if (image.id == imageId) image.copy(xMm = xMm.coerceIn(0f, 190f), yMm = yMm.coerceIn(0f, 270f)) else image
        })
    } }
    fun resizeImage(questionId: String, imageId: String, widthMm: Float) { update(questionId) { question ->
        question.copy(images = question.images.map { image ->
            if (image.id == imageId) image.copy(widthMm = widthMm.coerceIn(20f, 190f)) else image
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

    fun setBankQuery(value: String) { _state.update { it.copy(bankQuery=value.take(100)) } }
    fun selectBankCategory(id: Long?) { _state.update { it.copy(selectedBankCategory=id) } }

    fun saveToBank(questionId: String, categoryIds: Set<Long> = emptySet()) {
        val question = state.value.questions.firstOrNull { it.id == questionId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(bankLoading = true, error = null) }
            repository.saveToBank(question, state.value.subject, categoryIds)
                .onSuccess { refreshBankNow() }
                .onFailure { error -> _state.update { it.copy(bankLoading = false, error = safeBuilderError(error)) } }
        }
    }

    fun addBankCategory(name: String) = bankAction { repository.addBankCategory(name).getOrThrow() }
    fun setBankCategories(questionId: Long, categories: Set<Long>) = bankAction {
        repository.setBankCategories(questionId,categories).getOrThrow()
    }
    fun deleteBankCategory(id: Long, deleteQuestions: Boolean) = bankAction {
        repository.deleteBankCategory(id,deleteQuestions).getOrThrow()
        _state.update { it.copy(selectedBankCategory=null) }
    }

    fun deleteFromBank(id: Long) = viewModelScope.launch {
        _state.update { it.copy(bankLoading = true, error = null) }
        repository.deleteFromBank(id)
            .onSuccess { refreshBankNow() }
            .onFailure { error -> _state.update { it.copy(bankLoading = false, error = safeBuilderError(error)) } }
    }

    private fun bankAction(block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(bankLoading=true,error=null) }
        runCatching { block(); refreshBankNow() }
            .onFailure { error -> _state.update { it.copy(bankLoading=false,error=safeBuilderError(error)) } }
    }

    private suspend fun refreshBankNow() {
        val bank = repository.refreshBank().getOrThrow()
        _state.update { it.copy(bankQuestions = bank.questions, bankCategories=bank.categories, bankLoading = false) }
    }

    fun save() = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, savedCode = null, uploadProgress = null) }
        repository.save(state.value, saveOperationId) { done, total ->
            _state.update { it.copy(uploadProgress = "آپلود تصویر $done از $total") }
        }.onSuccess { result ->
            if (ownerUserId.isNotBlank()) draftStore.clear(ownerUserId)
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
            cleanDraftFingerprint = draftFingerprint(_state.value)
        }.onFailure { error ->
            _state.update { it.copy(saving = false, uploadProgress = null, error = safeBuilderError(error)) }
        }
    }
}

private fun draftFingerprint(state: ExamBuilderState): Int = listOf(
    state.examId,
    state.title,
    state.subject,
    state.durationMinutes,
    state.opensAtIso,
    state.closesAtIso,
    state.questions,
    state.shuffleQuestions,
    state.shuffleOptions,
    state.negativeMarking,
    state.teacherMessage,
    state.attemptsAllowed,
    state.attemptOnTimeout,
    state.gradePolicy,
    state.attemptCooldown,
    state.audienceMode,
    state.audienceClasses,
    state.audienceStudents
).hashCode()

private fun remapMovedIndex(value: Int, from: Int, to: Int): Int = when {
    value == from -> to
    from < to && value in (from + 1)..to -> value - 1
    from > to && value in to until from -> value + 1
    else -> value
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
