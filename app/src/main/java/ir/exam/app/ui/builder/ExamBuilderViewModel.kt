package ir.exam.app.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExamBuilderViewModel(
    private val repository: SupabaseExamBuilderRepository = SupabaseExamBuilderRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(ExamBuilderState())
    val state = _state.asStateFlow()

    fun setTitle(value: String) { _state.update { it.copy(title = value, error = null) } }
    fun setSubject(value: String) { _state.update { it.copy(subject = value, error = null) } }
    fun setDuration(value: String) { _state.update { it.copy(durationMinutes = value.filter(Char::isDigit), error = null) } }
    fun addQuestion(type: QuestionType) {
        val question = if (type == QuestionType.MULTIPLE_CHOICE) QuestionDraft(type = type, options = List(4) { "" }) else QuestionDraft(type = type)
        _state.update { it.copy(questions = it.questions + question) }
    }
    fun updateText(id: String, text: String) { update(id) { it.copy(text = text) } }
    fun updateOption(id: String, index: Int, text: String) { update(id) { q -> q.copy(options = q.options.mapIndexed { i, old -> if (i == index) text else old }) } }
    fun setCorrect(id: String, index: Int) { update(id) { it.copy(correctIndex = index) } }
    fun updateExpectedText(id: String, value: String) { update(id) { it.copy(expectedText = value) } }
    fun updateExpectedNumber(id: String, value: String) { update(id) { it.copy(expectedNumber = value.filter { c -> c.isDigit() || c == '.' || c == '-' }) } }
    fun updateTolerance(id: String, value: String) { update(id) { it.copy(tolerance = value.filter { c -> c.isDigit() || c == '.' }) } }
    fun remove(id: String) { _state.update { it.copy(questions = it.questions.filterNot { q -> q.id == id }) } }
    private fun update(id: String, change: (QuestionDraft) -> QuestionDraft) { _state.update { s -> s.copy(questions = s.questions.map { q -> if (q.id == id) change(q) else q }) } }

    fun save() = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, savedCode = null) }
        repository.create(state.value)
            .onSuccess { code -> _state.update { it.copy(saving = false, savedCode = code) } }
            .onFailure { error -> _state.update { it.copy(saving = false, error = error.message ?: "ذخیره آزمون ناموفق بود.") } }
    }
}
