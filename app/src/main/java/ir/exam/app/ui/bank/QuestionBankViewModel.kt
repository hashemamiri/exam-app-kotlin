package ir.exam.app.ui.bank

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import ir.exam.app.ui.builder.BankCategoryOption
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionBankUiState(
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val questions: List<BankQuestionOption> = emptyList(),
    val categories: List<BankCategoryOption> = emptyList(),
    val query: String = "",
    val categoryId: Long? = null,
    val message: String? = null,
    val error: String? = null
) {
    val visibleQuestions: List<BankQuestionOption>
        get() {
            val needle = query.trim().lowercase()
            return questions.filter { item ->
                (categoryId == null || categoryId in item.categoryIds) &&
                    (needle.isBlank() || item.question.text.lowercase().contains(needle) ||
                        item.subject.orEmpty().lowercase().contains(needle))
            }
        }
}

class QuestionBankViewModel(
    context: Context,
    private val repository: SupabaseExamBuilderRepository =
        SupabaseExamBuilderRepository(context.applicationContext)
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionBankUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.refreshBank()
            .onSuccess { snapshot ->
                _state.update {
                    it.copy(
                        loading = false,
                        questions = snapshot.questions,
                        categories = snapshot.categories
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = safeBankError(error)) }
            }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value.take(200), error = null) }
    }

    fun selectCategory(id: Long?) {
        _state.update { it.copy(categoryId = id, error = null) }
    }

    fun addCategory(name: String) = action("دسته ساخته شد.") {
        repository.addBankCategory(name).getOrThrow()
    }

    fun deleteCategory(id: Long, deleteQuestions: Boolean) = action("دسته حذف شد.") {
        repository.deleteBankCategory(id, deleteQuestions).getOrThrow()
    }

    fun updateQuestion(
        item: BankQuestionOption,
        question: QuestionDraft,
        subject: String,
        categoryIds: Set<Long>
    ) = action("سؤال بانک بروزرسانی شد.") {
        repository.updateBankQuestion(item.id, question, subject, categoryIds).getOrThrow()
    }

    fun deleteQuestion(id: Long) = action("سؤال از بانک حذف شد.") {
        repository.deleteFromBank(id).getOrThrow()
    }

    private fun action(success: String, block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        runCatching { block() }
            .onSuccess {
                repository.refreshBank()
                    .onSuccess { snapshot ->
                        _state.update {
                            it.copy(
                                actionLoading = false,
                                questions = snapshot.questions,
                                categories = snapshot.categories,
                                message = success
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(actionLoading = false, error = safeBankError(error)) }
                    }
            }
            .onFailure { error ->
                _state.update { it.copy(actionLoading = false, error = safeBankError(error)) }
            }
    }
}

private fun safeBankError(error: Throwable): String = error.message.orEmpty()
    .substringBefore("URL:")
    .substringBefore("Headers:")
    .replace(Regex("(?i)authorization[^,\n]*"), "")
    .replace(Regex("(?i)apikey[^,\n]*"), "")
    .take(260)
    .ifBlank { "عملیات بانک سؤال ناموفق بود." }
