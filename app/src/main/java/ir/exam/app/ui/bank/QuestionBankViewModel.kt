package ir.exam.app.ui.bank

import ir.exam.app.core.cache.SessionCache
import ir.exam.app.core.ui.PullRefreshGate
import ir.exam.app.core.network.UserFacingError
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.exam.app.data.repository.SupabaseExamBuilderRepository
import ir.exam.app.data.repository.ReferenceBankRepository
import ir.exam.app.domain.bank.BankPicker
import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.domain.bank.ReferenceBankRow
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
    val schoolItems: List<ir.exam.app.ui.builder.SchoolBankItem> = emptyList(),
    // V360 — بانک مرجع، فقطخواندنی از روی فضای ابری
    val referenceOpen: Boolean = false,
    val referenceGrade: Int = 1,
    val referenceTrack: String = "",
    val referenceSubject: String = "",
    val referenceChapter: String = "",
    val referenceType: String = "",
    val referenceDifficulty: Int? = null,
    val referenceQuery: String = "",
    val referencePage: Int = 0,
    val referencePageSize: Int = ReferenceBankContract.PAGE_SIZE,
    val referenceRows: List<ReferenceBankRow> = emptyList(),
    val referenceLoading: Boolean = false,
    val referenceError: String? = null,
    val referencePreview: ReferenceBankQuestion? = null,
    // V360.3 — پنجرهٔ فیلتر مرحلهای و افزودن گروهی
    val referenceGradeChosen: Boolean = false,
    val referenceTrackChosen: Boolean = false,
    val referenceSubjectChosen: Boolean = false,
    val referenceStage: Int = ReferenceBankContract.STEP_GRADE,
    val referenceResultsOpen: Boolean = false,
    val referenceIncludeAnswer: Boolean = true,
    val referencePicked: Set<Long> = emptySet(),
    val referenceCatalogMissing: Boolean = false
) {
    /** V360 — پالایش محلی روی همان شارد کوچک؛ هیچ سؤال حجیمی یکجا گرفته نمیشود. */
    val referenceFiltered: List<ReferenceBankRow>
        get() = referenceRows.filter { r ->
            (referenceTrack.isBlank() || r.track == referenceTrack) &&
                (referenceSubject.isBlank() || r.subject == referenceSubject) &&
                (referenceChapter.isBlank() || r.chapter == referenceChapter) &&
                (referenceType.isBlank() || r.typeFa == referenceType) &&
                (referenceDifficulty == null || r.difficulty == referenceDifficulty) &&
                (referenceQuery.isBlank() || r.text.contains(referenceQuery, ignoreCase = true))
        }

    val referencePages: Int
        get() = ((referenceFiltered.size + referencePageSize - 1) / referencePageSize).coerceAtLeast(1)

    val referencePageRows: List<ReferenceBankRow>
        get() = referenceFiltered.drop(referencePage * referencePageSize).take(referencePageSize)

    val referenceSubjects: List<String>
        get() = referenceRows.map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()

    val referenceChapters: List<String>
        get() = referenceRows.filter { referenceSubject.isBlank() || it.subject == referenceSubject }
            .map { it.chapter }.filter { it.isNotBlank() }.distinct().sorted()

    val referenceTypes: List<String>
        get() = referenceRows.map { it.typeFa }.filter { it.isNotBlank() }.distinct().sorted()

    /** V360.3 — پایههای ۱ تا ۹ فیلد رشته ندارند. */
    val referenceTracksVisible: Boolean
        get() = referenceGrade >= ReferenceBankContract.TRACK_MIN_GRADE

    val referenceTrackOptions: List<String>
        get() = ReferenceBankContract.tracksFor(referenceGrade)

    /** فهرست درسها از شارد همان پایه/رشته، وگرنه از فهرست کوچک درون برنامه. */
    val referenceSubjectOptions: List<String>
        get() {
            val fromRows = referenceRows
                .filter { referenceTrack.isBlank() || it.track == referenceTrack }
                .map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
            return fromRows.ifEmpty { ReferenceBankContract.subjectsFor(referenceGrade, referenceTrack) }
        }

    /** فصلها فقط پس از انتخاب درس، و فقط همان درس. */
    val referenceChapterOptions: List<String>
        get() = referenceRows
            .filter { referenceTrack.isBlank() || it.track == referenceTrack }
            .filter { referenceSubject.isBlank() || it.subject == referenceSubject }
            .map { it.chapter }.filter { it.isNotBlank() }.distinct().sorted()

    val referenceTypeOptions: List<String>
        get() {
            val fromRows = referenceRows.map { it.typeFa }.filter { it.isNotBlank() }.distinct().sorted()
            return fromRows.ifEmpty { listOf("تستی", "تشریحی", "جایخالی", "صحیح/غلط") }
        }

    val referencePickedCount: Int
        get() = referencePicked.size

    fun referenceStepReady(): Boolean = when (referenceStage) {
        ReferenceBankContract.STEP_GRADE -> referenceGradeChosen
        ReferenceBankContract.STEP_TRACK -> referenceTrackChosen || !referenceTracksVisible
        ReferenceBankContract.STEP_SUBJECT -> referenceSubjectChosen
        else -> true
    }

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
        SupabaseExamBuilderRepository(context.applicationContext),
    private val referenceRepository: ReferenceBankRepository = ReferenceBankRepository()
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
        _state.update { it.copy(difficulty = level?.takeIf { d -> d in 0..4 }, error = null) }
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

    // ------------------------------------------------------------------
    // V360 — بانک مرجع
    // ------------------------------------------------------------------

    fun openReferenceBank(open: Boolean) {
        _state.update {
            it.copy(
                referenceOpen = open,
                referenceError = null,
                referenceStage = if (open) ReferenceBankContract.STEP_GRADE else it.referenceStage,
                referenceResultsOpen = if (open) false else it.referenceResultsOpen,
                referenceGradeChosen = if (open) false else it.referenceGradeChosen,
                referenceTrackChosen = if (open) false else it.referenceTrackChosen,
                referenceSubjectChosen = if (open) false else it.referenceSubjectChosen
            )
        }
        if (open && _state.value.referenceRows.isEmpty()) loadReferenceCatalog()
    }

    fun setReferenceGrade(grade: Int) {
        val g = grade.coerceIn(ReferenceBankContract.MIN_GRADE, ReferenceBankContract.MAX_GRADE)
        _state.update {
            it.copy(
                referenceGrade = g,
                referenceGradeChosen = true,
                referenceTrack = "",
                referenceTrackChosen = false,
                referenceSubject = "",
                referenceSubjectChosen = false,
                referenceChapter = "",
                referenceType = "",
                referenceDifficulty = null,
                referencePage = 0,
                referenceRows = emptyList(),
                referencePicked = emptySet(),
                referenceResultsOpen = false,
                referenceCatalogMissing = false,
                referenceError = null
            )
        }
        loadReferenceCatalog()
    }

    fun setReferenceTrack(value: String) =
        _state.update { it.copy(referenceTrack = value, referenceTrackChosen = true, referenceSubject = "", referenceSubjectChosen = false, referenceChapter = "", referencePage = 0) }

    fun setReferenceSubject(value: String) =
        _state.update { it.copy(referenceSubject = value, referenceSubjectChosen = true, referenceChapter = "", referencePage = 0) }

    fun setReferenceChapter(value: String) = _state.update { it.copy(referenceChapter = value, referencePage = 0) }
    fun setReferenceType(value: String) = _state.update { it.copy(referenceType = value, referencePage = 0) }
    fun setReferenceDifficulty(value: Int?) = _state.update { it.copy(referenceDifficulty = value, referencePage = 0) }
    fun setReferenceQuery(value: String) = _state.update { it.copy(referenceQuery = value.take(200), referencePage = 0) }
    fun setReferencePageSize(value: Int) = _state.update { it.copy(referencePageSize = value.coerceIn(10, 100), referencePage = 0) }
    fun nextReferencePage() = _state.update { it.copy(referencePage = (it.referencePage + 1).coerceAtMost(it.referencePages - 1)) }
    fun prevReferencePage() = _state.update { it.copy(referencePage = (it.referencePage - 1).coerceAtLeast(0)) }

    fun setReferenceIncludeAnswer(value: Boolean) = _state.update { it.copy(referenceIncludeAnswer = value) }

    /** V360.3 — گام بعدی پالایشگر؛ رشته برای پایههای ۱ تا ۹ پریده میشود. */
    fun nextReferenceStage() {
        _state.update { st ->
            val next = when (st.referenceStage) {
                ReferenceBankContract.STEP_GRADE ->
                    if (st.referenceTracksVisible) ReferenceBankContract.STEP_TRACK
                    else ReferenceBankContract.STEP_SUBJECT
                else -> (st.referenceStage + 1).coerceAtMost(ReferenceBankContract.STEP_DIFFICULTY)
            }
            st.copy(referenceStage = next)
        }
    }

    fun prevReferenceStage() {
        _state.update { st ->
            val prev = when (st.referenceStage) {
                ReferenceBankContract.STEP_SUBJECT ->
                    if (st.referenceTracksVisible) ReferenceBankContract.STEP_TRACK
                    else ReferenceBankContract.STEP_GRADE
                else -> (st.referenceStage - 1).coerceAtLeast(ReferenceBankContract.STEP_GRADE)
            }
            st.copy(referenceStage = prev)
        }
    }

    fun openReferenceFilter() = _state.update {
        it.copy(referenceResultsOpen = false, referenceStage = ReferenceBankContract.STEP_GRADE,
            referenceGradeChosen = false, referenceTrackChosen = false, referenceSubjectChosen = false)
    }

    /** تأیید فیلتر → فهرست سؤالها با امکان انتخاب گروهی. */
    fun applyReferenceFilter() = _state.update {
        it.copy(referenceResultsOpen = true, referencePage = 0, referenceError = null)
    }

    fun toggleReferencePick(id: Long) = _state.update { st ->
        val next = if (id in st.referencePicked) st.referencePicked - id else st.referencePicked + id
        st.copy(referencePicked = next)
    }

    fun toggleReferencePagePicks() = _state.update { st ->
        val ids = st.referencePageRows.map { it.id }.toSet()
        val all = ids.all { it in st.referencePicked }
        st.copy(referencePicked = if (all) st.referencePicked - ids else st.referencePicked + ids)
    }

    fun clearReferencePicks() = _state.update { it.copy(referencePicked = emptySet()) }

    fun loadReferenceCatalog() = viewModelScope.launch {
        _state.update { it.copy(referenceLoading = true, referenceError = null) }
        runCatching { referenceRepository.catalog(_state.value.referenceGrade) }
            .onSuccess { rows ->
                _state.update {
                    it.copy(referenceLoading = false, referenceRows = rows, referencePage = 0,
                        referenceCatalogMissing = rows.isEmpty())
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(referenceLoading = false, referenceRows = emptyList(),
                        referenceCatalogMissing = true,
                        referenceError = "فهرست این پایه هنوز بارگذاری نشده است؛ میتوانید فیلتر را ببینید. " + safeBankError(e))
                }
            }
    }

    fun previewReference(row: ReferenceBankRow) = viewModelScope.launch {
        runCatching { referenceRepository.questions(_state.value.referenceGrade)[row.id.toString()] }
            .onSuccess { q -> _state.update { it.copy(referencePreview = q, referenceError = if (q == null) "محتوای این سؤال یافت نشد." else null) } }
            .onFailure { e -> _state.update { it.copy(referenceError = safeBankError(e)) } }
    }

    fun dismissReferencePreview() = _state.update { it.copy(referencePreview = null) }

    /** افزودن یک سؤال؛ پاسخ فقط با انتخاب کاربر و فقط در بخش پاسخ. */
    fun addReference(row: ReferenceBankRow, onAdd: (List<BankQuestionOption>) -> Unit, onDone: () -> Unit = {}) = viewModelScope.launch {
        runCatching { referenceRepository.questions(_state.value.referenceGrade)[row.id.toString()] }
            .onSuccess { q ->
                if (q == null) _state.update { it.copy(referenceError = "محتوای این سؤال یافت نشد.") }
                else { onAdd(listOf(q.toBankQuestionOption(_state.value.referenceIncludeAnswer))); onDone() }
            }
            .onFailure { e -> _state.update { it.copy(referenceError = safeBankError(e)) } }
    }

    /** V360.3 — افزودن گروهی: شارد همان پایه یکبار خوانده و فقط سؤالهای انتخابشده کپی میشوند. */
    fun addReferenceMany(onAdd: (List<BankQuestionOption>) -> Unit, onDone: () -> Unit = {}) = viewModelScope.launch {
        val picked = _state.value.referenceFiltered
            .filter { it.id in _state.value.referencePicked }
        if (picked.isEmpty()) {
            _state.update { it.copy(referenceError = "نخست سؤالها را انتخاب کنید.") }
            return@launch
        }
        _state.update { it.copy(referenceLoading = true, referenceError = null) }
        runCatching { referenceRepository.questions(_state.value.referenceGrade) }
            .onSuccess { all ->
                val include = _state.value.referenceIncludeAnswer
                val items = picked.mapNotNull { row ->
                    all[row.id.toString()]?.toBankQuestionOption(include)
                }
                _state.update { it.copy(referenceLoading = false, referenceError = if (items.isEmpty()) "محتوای سؤالها در دسترس نیست." else null) }
                if (items.isNotEmpty()) { onAdd(items); onDone() }
            }
            .onFailure { e ->
                _state.update { it.copy(referenceLoading = false, referenceError = safeBankError(e)) }
            }
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
