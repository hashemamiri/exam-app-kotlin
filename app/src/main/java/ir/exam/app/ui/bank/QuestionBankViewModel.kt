package ir.exam.app.ui.bank

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.network.UserFacingError
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class QuestionBankUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false, // V227 — فقط برای نشانگر «کشیدن برای بازخوانی»
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

/** V214 — بستهٔ حافظهٔ موقت بانک سؤال */
private data class BankCache(val questions: List<BankQuestionOption>, val categories: List<BankCategoryOption>)
private const val CACHE_KEY = "bank.snapshot"

class QuestionBankViewModel(
    context: Context,
    private val repository: SupabaseExamBuilderRepository =
        SupabaseExamBuilderRepository(context.applicationContext)
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionBankUiState())
    val state = _state.asStateFlow()

    init { load() }

    // V227 — بازخوانی با کشیدن صفحه: پرچم جداگانهٔ refreshing (نه loading که با حافظهٔ موقت false می‌ماند و نه actionLoading)؛
    // یک بازخوانی هم‌زمان، حداقل ۵۰۰ms نمایش تا نشانگر Material3 بین حالت‌ها گیر نکند.
    private var refreshJob: Job? = null
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val started = System.currentTimeMillis()
            runCatching { load().join() }
            val rest = 500L - (System.currentTimeMillis() - started)
            if (rest > 0) delay(rest)
            _state.update { it.copy(refreshing = false) }
        }
    }

    fun load() = viewModelScope.launch {
        // V214 — بانک قبلی (حافظهٔ موقت) فوراً نشان داده می‌شود؛ نسخهٔ تازه جایگزین می‌شود.
        val cached = SessionCache.get<BankCache>(CACHE_KEY)
        _state.update { it.copy(loading = cached == null, questions = cached?.questions ?: it.questions, categories = cached?.categories ?: it.categories, error = null) }
        repository.refreshBank()
            .onSuccess { snapshot ->
                SessionCache.put(CACHE_KEY, BankCache(snapshot.questions, snapshot.categories))
                _state.update {
                    it.copy(
                        loading = false,
                        questions = snapshot.questions,
                        categories = snapshot.categories
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = if (cached == null) safeBankError(error) else null) }
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

private fun safeBankError(error: Throwable): String = UserFacingError.of(error, "عملیات بانک سؤال ناموفق بود.") // V209 — بدون متن فنی
