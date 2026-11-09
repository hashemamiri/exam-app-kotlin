package ir.exam.app.ui.bank

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.ui.PullRefreshGate
import ir.exam.app.core.network.UserFacingError
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import ir.exam.app.domain.bank.BankPicker
import ir.exam.app.ui.builder.BankCategoryOption
import ir.exam.app.ui.builder.BankQuestionOption
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionBankUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false, // V227 — فقط برای نشانگر «کشیدن برای بازخوانی»
    val actionLoading: Boolean = false,
    val questions: List<BankQuestionOption> = emptyList(),
    val categories: List<BankCategoryOption> = emptyList(),
    val query: String = "",
    val categoryId: Long? = null,
    // V358 — پالایش دشواری؛ null یعنی همهٔ سطح‌ها، صفر یعنی تعیین‌نشده‌ها
    val difficulty: Int? = null,
    val message: String? = null,
    val error: String? = null,
    // V263 — بانک مدرسه
    val schoolOpen: Boolean = false,
    val schoolLoading: Boolean = false,
    val schoolItems: List<ir.exam.app.ui.builder.SchoolBankItem> = emptyList()
) {
    /** V358 — همان منطق سایت؛ انتخاب یک فصل، زیرمجموعه‌هایش را هم می‌گیرد. */
    val visibleQuestions: List<BankQuestionOption>
        get() {
            val allowed = BankPicker.filterItems(
                questions.map { it.toPickerItem() },
                categories.map { BankPicker.Category(it.id.toString(), it.name, it.parentId?.toString()) },
                BankPicker.FilterOpts(
                    text = query,
                    categoryId = categoryId?.toString(),
                    difficulty = difficulty
                )
            ).map { it.id }.toSet()
            return questions.filter { it.id.toString() in allowed }
        }

    /** مسیر کامل دسته برای نمایش در پالایشگر. */
    fun categoryPath(id: Long): String =
        BankPicker.categoryPath(categories.map { BankPicker.Category(it.id.toString(), it.name, it.parentId?.toString()) }, id.toString())

    fun categoryDepth(id: Long): Int =
        BankPicker.depthOf(categories.map { BankPicker.Category(it.id.toString(), it.name, it.parentId?.toString()) }, id.toString())
}

/** V358 — پل میان مدل بانک و پیمانهٔ مشترک انتخاب. */
internal fun BankQuestionOption.toPickerItem(): BankPicker.Item = BankPicker.Item(
    id = id.toString(),
    subject = subject.orEmpty(),
    text = question.text,
    topic = question.topic,
    difficulty = question.difficulty,
    grade = question.grade,
    catIds = categoryIds.map { it.toString() }
)

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
    // V229 — منطق مشترک در PullRefreshGate (تست واحد V229_PullRefreshGateTest)
    private val refreshGate = PullRefreshGate(viewModelScope)
    fun refresh() {
        refreshGate.run({ r -> _state.update { it.copy(refreshing = r) } }) { load().join() }
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

    /** V358 — null یعنی همهٔ سطح‌ها. */
    fun selectDifficulty(level: Int?) {
        _state.update { it.copy(difficulty = level?.takeIf { d -> d in 0..3 }, error = null) }
    }

    fun addCategory(name: String, parentId: Long? = null) = action("دسته ساخته شد.") {
        repository.addBankCategory(name, parentId).getOrThrow()
    }

    /** V358 — جابه‌جایی دسته؛ سرور حلقه و سرریز ژرفا را رد می‌کند. */
    fun moveCategory(id: Long, parentId: Long?) = action("دسته جابه‌جا شد.") {
        repository.moveBankCategory(id, parentId).getOrThrow()
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
        repository.updateBankQuestion(item.id, question, subject, categoryIds, examTitle = item.examTitle).getOrThrow()
    }

    fun deleteQuestion(id: Long) = action("سؤال از بانک حذف شد.") {
        repository.deleteFromBank(id).getOrThrow()
    }

    /** V263 — اشتراک سؤال با همکاران هم‌مدرسه */
    fun setShared(id: Long, shared: Boolean) = action(if (shared) "سؤال با همکاران مدرسه به اشتراک گذاشته شد." else "اشتراک سؤال برداشته شد.") {
        repository.setBankShared(id, shared).getOrThrow()
    }

    /** V263 — باز/بسته کردن بانک مدرسه و بارگذاری سؤال‌های اشتراکی همکاران */
    fun openSchoolBank(open: Boolean) {
        _state.update { it.copy(schoolOpen = open, error = null) }
        if (open) loadSchoolBank()
    }

    fun loadSchoolBank() = viewModelScope.launch {
        _state.update { it.copy(schoolLoading = true) }
        repository.schoolBank()
            .onSuccess { items -> _state.update { it.copy(schoolLoading = false, schoolItems = items) } }
            .onFailure { error -> _state.update { it.copy(schoolLoading = false, error = safeBankError(error)) } }
    }

    fun copyFromSchool(id: Long) = action("سؤال به بانک شما افزوده شد.") {
        repository.copyFromSchoolBank(id).getOrThrow()
        repository.schoolBank().onSuccess { items -> _state.update { it.copy(schoolItems = items) } }
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
