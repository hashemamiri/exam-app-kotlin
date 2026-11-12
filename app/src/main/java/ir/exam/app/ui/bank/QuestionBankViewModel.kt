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
import ir.exam.app.domain.bank.ReferenceBankMedia
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
    // V360 — بانک مرجع، فقط‌خواندنی از روی فضای ابری
    val referenceOpen: Boolean = false,
    /** V360.5 — صفر یعنی «همهٔ پایه ها»؛ پیش‌فرض همهٔ کشوها روی «همه» است. */
    val referenceGrade: Int = 0,
    val referenceTrack: String = "",
    val referenceSubject: String = "",
    val referenceChapter: String = "",
    val referenceType: String = "",
    val referenceDifficulty: Int? = null,
    /** V369 — «همه» (خالی)، «1» = با تصویر، «0» = بدون تصویر. */
    val referenceImageFilter: String = "",
    val referenceQuery: String = "",
    val referencePage: Int = 0,
    val referencePageSize: Int = ReferenceBankContract.PAGE_SIZE,
    val referenceRows: List<ReferenceBankRow> = emptyList(),
    val referenceLoading: Boolean = false,
    val referenceError: String? = null,
    val referencePreview: ReferenceBankQuestion? = null,
    val referenceDetails: Map<Long, ReferenceBankQuestion> = emptyMap(),
    val referenceDetailsLoading: Boolean = false,
    val referenceDetailsError: String? = null,
    val referenceAnswerOverrides: Map<Long, Boolean> = emptyMap(),
    // V360.5 — پنجرهٔ کشویی و افزودن گروهی
    val referenceResultsOpen: Boolean = false,
    val referenceIncludeAnswer: Boolean = true,
    val referencePicked: Set<Long> = emptySet(),
    val referenceCatalogMissing: Boolean = false,
    val referenceLoadedNote: String? = null
) {

    /** پالایش محلی؛ پس از تأیید فیلتر، محتوای شارد پایه خودکار بارگیری می‌شود. */
    val referenceFiltered: List<ReferenceBankRow>
        get() = referenceRows.filter { r ->
            (referenceGrade == 0 || r.grade == referenceGrade) &&
                (referenceTrack.isBlank() || r.track == referenceTrack) &&
                (referenceSubject.isBlank() || r.subject == referenceSubject) &&
                (referenceChapter.isBlank() || r.chapter == referenceChapter) &&
                (referenceType.isBlank() || r.typeFa == referenceType) &&
                (referenceDifficulty == null || r.difficulty == referenceDifficulty) &&
                // V369 — «با تصویر / بدون تصویر»
                (referenceImageFilter.isBlank() ||
                    if (referenceImageFilter == "1") (r.hasImage || r.imageCount > 0)
                    else !(r.hasImage || r.imageCount > 0)) &&
                (referenceQuery.isBlank() || r.text.contains(referenceQuery, ignoreCase = true) ||
                    referenceDetails[r.id]?.let { q ->
                        (q.displayText + " " + q.options.joinToString(" ")).contains(referenceQuery, ignoreCase = true)
                    } == true)
        }

    val referencePages: Int
        get() = ((referenceFiltered.size + referencePageSize - 1) / referencePageSize).coerceAtLeast(1)

    val referencePageRows: List<ReferenceBankRow>
        get() = referenceFiltered.drop(referencePage * referencePageSize).take(referencePageSize)

    /** V360.5 — رشته فقط از پایهٔ ۱۰ به بعد دیده می شود. */
    val referenceTracksVisible: Boolean
        get() = referenceGrade >= ReferenceBankContract.TRACK_MIN_GRADE

    val referenceTrackOptions: List<String>
        get() = ReferenceBankContract.tracksFor(referenceGrade)

    /** تا وقتی پایهای انتخاب نشده، فقط گزینهٔ «همه» برای درس هست. */
    val referenceSubjectOptions: List<String>
        get() {
            if (referenceGrade == 0) return emptyList()
            val fromRows = referenceRows
                .filter { referenceTrack.isBlank() || it.track == referenceTrack }
                .map { it.subject }.filter { it.isNotBlank() }.distinct().sorted()
            return fromRows.ifEmpty { ReferenceBankContract.subjectsFor(referenceGrade, referenceTrack) }
        }

    /** فصلها فقط پس از انتخاب درس پر میشوند. */
    val referenceChapterOptions: List<String>
        get() {
            if (referenceSubject.isBlank()) return emptyList()
            return referenceRows
                .filter { referenceTrack.isBlank() || it.track == referenceTrack }
                .filter { it.subject == referenceSubject }
                .map { it.chapter }.filter { it.isNotBlank() }.distinct().sorted()
        }

    /** نوع سؤال و دشواری به پایه وابسته نیستند؛ همیشه همهٔ گزینه ها را دارند. */
    val referenceTypeOptions: List<String>
        get() {
            val base = listOf("تستی", "تشریحی", "جایخالی", "صحیح/غلط", "دستهبندی", "مرتبسازی", "کوتاهپاسخ")
            val fromRows = referenceRows.map { it.typeFa }.filter { it.isNotBlank() }
            return (base + fromRows).distinct().sorted()
        }

    val referencePickedCount: Int
        get() = referencePicked.size

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
    // V370 — کش دیسکی شاردها در cacheDir؛ بدون آن هر باز کردن بانک همه‌چیز را از نو می‌گرفت.
    private val referenceRepository: ReferenceBankRepository =
        ReferenceBankRepository(cacheDir = context.applicationContext.cacheDir)
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionBankUiState())
    private val referenceQuestionCache = mutableMapOf<Int, Map<String, ReferenceBankQuestion>>()
    private val referenceQuestionLoading = mutableSetOf<Int>()
    /** V370 — پایه‌هایی که پیش‌خوانی‌شان در جریان است، تا دوباره شروع نشود. */
    private val prefetchingGrades = mutableSetOf<Int>()
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
                referenceResultsOpen = if (open) false else it.referenceResultsOpen
            )
        }
        if (open && _state.value.referenceRows.isEmpty()) loadReferenceCatalog()
    }

    fun setReferenceGrade(grade: Int) {
        val g = grade.coerceIn(0, ReferenceBankContract.MAX_GRADE)
        _state.update {
            it.copy(
                referenceGrade = g,
                referenceTrack = "",
                referenceSubject = "",
                referenceChapter = "",
                referencePage = 0,
                referenceRows = emptyList(),
                referenceDetails = emptyMap(),
                referenceDetailsError = null,
                referenceAnswerOverrides = emptyMap(),
                referencePicked = emptySet(),
                referenceCatalogMissing = false,
                referenceLoadedNote = null,
                referenceError = null
            )
        }
        loadReferenceCatalog()
    }

    fun setReferenceTrack(value: String) =
        _state.update { it.copy(referenceTrack = value, referenceSubject = "", referenceChapter = "", referencePage = 0) }

    fun setReferenceSubject(value: String) =
        _state.update { it.copy(referenceSubject = value, referenceChapter = "", referencePage = 0) }

    fun setReferenceChapter(value: String) = _state.update { it.copy(referenceChapter = value, referencePage = 0) }
    fun setReferenceType(value: String) = _state.update { it.copy(referenceType = value, referencePage = 0) }
    fun setReferenceDifficulty(value: Int?) = _state.update { it.copy(referenceDifficulty = value, referencePage = 0) }
    fun setReferenceImageFilter(value: String) = _state.update { it.copy(referenceImageFilter = value, referencePage = 0) }
    fun setReferenceQuery(value: String) = _state.update { it.copy(referenceQuery = value.take(200), referencePage = 0) }
    fun setReferencePageSize(value: Int) = _state.update { it.copy(referencePageSize = value.coerceIn(10, 100), referencePage = 0) }
    fun nextReferencePage() {
        _state.update { it.copy(referencePage = (it.referencePage + 1).coerceAtMost(it.referencePages - 1)) }
        loadReferenceQuestionDetails()
    }
    fun prevReferencePage() {
        _state.update { it.copy(referencePage = (it.referencePage - 1).coerceAtLeast(0)) }
        loadReferenceQuestionDetails()
    }

    fun setReferenceIncludeAnswer(value: Boolean) = _state.update { it.copy(referenceIncludeAnswer = value) }

    fun toggleReferenceAnswer(id: Long) = _state.update { st ->
        val included = st.referenceAnswerOverrides[id] ?: st.referenceIncludeAnswer
        st.copy(referenceAnswerOverrides = st.referenceAnswerOverrides + (id to !included))
    }

    fun referenceAnswerIncluded(id: Long): Boolean =
        _state.value.referenceAnswerOverrides[id] ?: _state.value.referenceIncludeAnswer

    /** V360.5 — آیکن فیلتر، همان پنجرهٔ کشویی را برمیگرداند. */
    fun openReferenceFilter() = _state.update { it.copy(referenceResultsOpen = false) }

    /** بستن فیلتر؛ محتوای شاردهای پایه‌های فیلترشده خودکار خوانده می‌شود. */
    fun applyReferenceFilter() {
        _state.update { it.copy(referenceResultsOpen = true, referencePage = 0, referenceError = null) }
        loadReferenceQuestionDetails()
    }

    fun loadReferenceQuestionDetails() = viewModelScope.launch {
        val initial = _state.value
        // V369 — فقط پایه‌های همین صفحه؛ قبلاً با «همهٔ پایه‌ها» هر ۱۲ شارد خوانده می‌شد.
        val pageRows = initial.referencePageRows
        val rowsByGrade = pageRows.groupBy { it.grade }
        val grades = if (initial.referenceGrade == 0) rowsByGrade.keys.sorted()
            else listOf(initial.referenceGrade)
        // V370 — شارد صفحه‌ای: «کمبود» یعنی هنوز همهٔ شناسه‌های این صفحه در حافظه نیستند.
        val missing = grades.filter { grade ->
            val have = referenceQuestionCache[grade]
            val need = if (initial.referenceGrade == 0) rowsByGrade[grade].orEmpty() else pageRows
            val absent = need.any { have?.containsKey(it.id.toString()) != true }
            absent && referenceQuestionLoading.add(grade)
        }
        if (missing.isNotEmpty()) _state.update { it.copy(referenceDetailsLoading = true, referenceDetailsError = null) }
        val failures = mutableListOf<String>()
        for (grade in missing) {
            val needIds = (if (initial.referenceGrade == 0) rowsByGrade[grade].orEmpty() else pageRows)
                .map { it.id }
            runCatching { referenceRepository.questions(grade, needIds) }
                .onSuccess { fetched ->
                    referenceQuestionCache[grade] = referenceQuestionCache[grade].orEmpty() + fetched
                }
                .onFailure { failures += safeBankError(it) }
            referenceQuestionLoading.remove(grade)
        }
        val now = _state.value
        val details = mutableMapOf<Long, ReferenceBankQuestion>()
        grades.forEach { grade ->
            val shard = referenceQuestionCache[grade].orEmpty()
            pageRows.asSequence().filter { it.grade == grade }.forEach { row ->
                shard[row.id.toString()]?.let { details[row.id] = it }
            }
        }
        _state.update {
            it.copy(
                referenceDetails = it.referenceDetails + details,
                referenceDetailsLoading = referenceQuestionLoading.isNotEmpty(),
                referenceDetailsError = failures.firstOrNull()
            )
        }
        prefetchNextReferencePage()
    }

    /**
     * V370 — صفحهٔ بعد در پس‌زمینه خوانده می‌شود تا دکمهٔ «بعدی» فوری جواب دهد.
     * فقط حافظهٔ نشست پر می‌شود؛ هیچ حالتی از رابط تغییر نمی‌کند.
     */
    private fun prefetchNextReferencePage() {
        val snapshot = _state.value
        if (snapshot.referenceGrade != 0) return
        val rows = snapshot.referenceFiltered
            .drop((snapshot.referencePage + 1) * snapshot.referencePageSize)
            .take(snapshot.referencePageSize)
        if (rows.isEmpty()) return
        rows.groupBy { it.grade }.forEach { (grade, group) ->
            val have = referenceQuestionCache[grade].orEmpty()
            val need = group.map { it.id }
            if (need.all { have.containsKey(it.toString()) }) return@forEach
            if (!prefetchingGrades.add(grade)) return@forEach
            viewModelScope.launch {
                runCatching { referenceRepository.questions(grade, need) }
                    .onSuccess { fetched ->
                        referenceQuestionCache[grade] = referenceQuestionCache[grade].orEmpty() + fetched
                    }
                prefetchingGrades.remove(grade)
            }
        }
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

    /** V360.5 — پایهٔ «همه» یعنی فقط شاردهایی که واقعاً بارگذاری شدهاند. */
    fun loadReferenceCatalog() = viewModelScope.launch {
        _state.update { it.copy(referenceLoading = true, referenceError = null) }
        val grade = _state.value.referenceGrade
        if (grade == 0) {
            val merged = mutableListOf<ReferenceBankRow>()
            val loaded = mutableListOf<Int>()
            for (g in ReferenceBankContract.MIN_GRADE..ReferenceBankContract.MAX_GRADE) {
                runCatching { referenceRepository.catalog(g) }.onSuccess { rows ->
                    if (rows.isNotEmpty()) { merged.addAll(rows); loaded.add(g) }
                }
            }
            _state.update {
                it.copy(
                    referenceLoading = false,
                    referenceRows = merged,
                    referencePage = 0,
                    referenceCatalogMissing = merged.isEmpty(),
                    referenceLoadedNote = if (loaded.isEmpty()) "هیچ پایهای بارگذاری نشده است."
                        else null
                )
            }
        } else {
            runCatching { referenceRepository.catalog(grade) }
                .onSuccess { rows ->
                    _state.update {
                        it.copy(referenceLoading = false, referenceRows = rows, referencePage = 0,
                            referenceCatalogMissing = rows.isEmpty(),
                            referenceLoadedNote = if (rows.isEmpty()) "فهرست این پایه هنوز بارگذاری نشده است." else null)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(referenceLoading = false, referenceRows = emptyList(),
                            referenceCatalogMissing = true,
                            referenceLoadedNote = "فهرست این پایه هنوز بارگذاری نشده است.",
                            referenceError = safeBankError(e))
                    }
                }
        }
    }

    private fun rowGrade(row: ReferenceBankRow): Int =
        row.grade.coerceIn(ReferenceBankContract.MIN_GRADE, ReferenceBankContract.MAX_GRADE)

    private suspend fun referenceQuestion(grade: Int, id: Long): ReferenceBankQuestion? {
        // V370 — اگر شارد صفحه‌ای باشد، فقط تکهٔ همین سؤال خوانده می‌شود، نه کل پایه.
        referenceQuestionCache[grade]?.get(id.toString())?.let { return it }
        val fetched = referenceRepository.questions(grade, listOf(id))
        referenceQuestionCache[grade] = referenceQuestionCache[grade].orEmpty() + fetched
        return fetched[id.toString()]
    }

    fun previewReference(row: ReferenceBankRow) = viewModelScope.launch {
        runCatching { referenceQuestion(rowGrade(row), row.id) }
            .onSuccess { q -> _state.update { it.copy(referencePreview = q, referenceError = if (q == null) "محتوای این سؤال یافت نشد." else null) } }
            .onFailure { e -> _state.update { it.copy(referenceError = safeBankError(e)) } }
    }

    fun dismissReferencePreview() = _state.update { it.copy(referencePreview = null) }

    /** افزودن یک سؤال؛ پاسخ فقط با انتخاب کاربر و فقط در بخش پاسخ. */
    fun addReference(row: ReferenceBankRow, onAdd: (List<BankQuestionOption>) -> Unit, onDone: () -> Unit = {}) = viewModelScope.launch {
        runCatching { referenceQuestion(rowGrade(row), row.id) }
            .onSuccess { q ->
                if (q == null) _state.update { it.copy(referenceError = "محتوای این سؤال یافت نشد.") }
                else { onAdd(listOf(q.toBankQuestionOption(referenceAnswerIncluded(row.id)))); onDone() }
            }
            .onFailure { e -> _state.update { it.copy(referenceError = safeBankError(e)) } }
    }

    /** V360.5 — افزودن گروهی؛ اگر پایه «همه» باشد، شارد هر پایه جداگانه خوانده می شود. */
    fun addReferenceMany(onAdd: (List<BankQuestionOption>) -> Unit, onDone: () -> Unit = {}) = viewModelScope.launch {
        val picked = _state.value.referenceFiltered.filter { it.id in _state.value.referencePicked }
        if (picked.isEmpty()) {
            _state.update { it.copy(referenceError = "نخست سؤال ها را انتخاب کنید.") }
            return@launch
        }
        _state.update { it.copy(referenceLoading = true, referenceError = null) }
        val items = mutableListOf<BankQuestionOption>()
        picked.groupBy { rowGrade(it) }.forEach { (grade, rows) ->
            // V370 — برای «افزودن گروهی» هم فقط تکه‌های همان شناسه‌های انتخاب‌شده می‌آید.
            val cached = referenceQuestionCache[grade]
            val need = rows.map { it.id }.filter { cached?.containsKey(it.toString()) != true }
            runCatching {
                if (need.isEmpty()) emptyMap() else referenceRepository.questions(grade, need)
            }.onSuccess { fetched ->
                val all = cached.orEmpty() + fetched
                referenceQuestionCache[grade] = all
                rows.forEach { row -> all[row.id.toString()]?.let { items += it.toBankQuestionOption(referenceAnswerIncluded(row.id)) } }
            }
        }
        _state.update {
            it.copy(referenceLoading = false,
                referenceError = if (items.isEmpty()) "محتوای سؤال ها در دسترس نیست." else null)
        }
        if (items.isNotEmpty()) { onAdd(items); onDone() }
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
