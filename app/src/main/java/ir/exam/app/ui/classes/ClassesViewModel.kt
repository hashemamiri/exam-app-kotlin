package ir.exam.app.ui.classes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.local.NativeDatabaseProvider
import ir.exam.app.data.local.StudentNoteEntity
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.data.repository.SupabaseSchoolRepository
import ir.exam.app.domain.model.BulkStudentCreateResult
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest
import ir.exam.app.domain.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** V61.0 — مدرسهٔ عضو معلم برای نمای «مدارس» بخش کلاس‌ها. */
data class TeacherSchoolItem(
    val id: String,
    val name: String,
    val province: String = "",
    val city: String = "",
    val classCount: Int = 0
)

data class ClassesState(
    val loading: Boolean = true,
    val actionLoading: Boolean = false,
    val classes: List<SchoolClass> = emptyList(),
    val students: List<StudentProfile> = emptyList(),
    val selectedClass: SchoolClass? = null,
    val roster: List<StudentProfile> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val message: String? = null,
    val lastCredential: StudentCredential? = null,
    val bulkResult: BulkStudentCreateResult? = null,
    val studentNotes: Map<String,String> = emptyMap(),
    // V61.0 — نمای مدارس: لیست مدرسه‌ها، مدرسهٔ باز و کلاس‌های معلم در آن.
    val schoolsOpen: Boolean = false,
    val schools: List<TeacherSchoolItem> = emptyList(),
    val selectedSchool: TeacherSchoolItem? = null,
    val schoolClasses: List<SchoolClass> = emptyList(),
    // V61.0 — فقط برای مدیر: معلم‌های مدرسه برای انتخاب معلمِ کلاس جدید.
    val schoolTeachers: List<SchoolTeacherPick> = emptyList(),
    // V61.5 — فیلتر لیست دانش‌آموزان + متادادهٔ فیلتر (معلم/عضویت مدرسه).
    val studentFilter: StudentListFilter = StudentListFilter(),
    val filterMeta: Map<String, StudentFilterMeta> = emptyMap()
)

/** V61.0 — معلم قابل انتخاب هنگام ساخت کلاس توسط مدیر. */
data class SchoolTeacherPick(val id: String, val name: String)

/** V61.5 — متادادهٔ فیلتر هر دانش‌آموز: معلم مالک و عضویت مدرسه.
 *  V61.8 — schoolIds: مدرسه‌های دانش‌آموز برای فیلتر مدرسهٔ خاص. */
data class StudentFilterMeta(
    val teacherId: String = "",
    val teacherName: String = "",
    val inSchool: Boolean = false,
    val schoolIds: Set<String> = emptySet()
)

/** V61.5 — فیلترهای فعال لیست دانش‌آموزان؛ همه با هم قابل ترکیب‌اند.
 *  V61.8 — schoolId: مدرسهٔ خاص انتخابی از لیست مدارس. */
data class StudentListFilter(
    val grade: String? = null,
    val classId: String? = null,
    val gender: String? = null,
    val unassigned: Boolean = false,
    val inSchool: Boolean = false,
    val schoolId: String? = null,
    val teacherId: String? = null
) {
    val isActive: Boolean
        get() = grade != null || classId != null || gender != null ||
            unassigned || inSchool || schoolId != null || teacherId != null
}

class ClassesViewModel(
    private val repository: SchoolRepository = SupabaseSchoolRepository(),
    context:Context?=null
) : ViewModel() {
    private val ownerId=if(context!=null)runCatching{SupabaseProvider.client.auth.currentUserOrNull()?.id.orEmpty()}.getOrDefault("")else ""
    private val noteDao=context?.applicationContext?.let{NativeDatabaseProvider.get(it).studentNoteDao()}
    private val _state = MutableStateFlow(ClassesState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val classes = repository.getClasses().getOrElse { return@launch failLoad(it) }
        val students = repository.getStudents().getOrElse { return@launch failLoad(it) }
        val notes=if(noteDao!=null&&ownerId.isNotBlank())noteDao.list(ownerId).associate{it.studentId to it.note}else emptyMap()
        _state.update { old ->
            val selected = old.selectedClass?.id?.let { id -> classes.firstOrNull { it.id == id } }
            old.copy(loading = false, classes = classes, students = students, selectedClass = selected,studentNotes=notes)
        }
        state.value.selectedClass?.let { loadRoster(it.id) }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null, lastCredential = null,bulkResult=null) }
    }

    fun selectClass(item: SchoolClass) {
        _state.update { it.copy(selectedClass = item, roster = emptyList(), error = null) }
        loadRoster(item.id)
    }

    fun closeClass() {
        _state.update { it.copy(selectedClass = null, roster = emptyList()) }
    }

    fun saveClass(
        id: String?,
        name: String,
        grade: String,
        fieldOfStudy: String = "",
        teacherId: String? = null
    ) = action(
        successMessage = if (id == null) "کلاس ساخته شد." else "کلاس ویرایش شد."
    ) {
        when {
            // V61.0 — مدیر: ساخت کلاس برای معلم انتخابی از لیست معلم‌های مدرسه.
            id == null && !teacherId.isNullOrBlank() -> {
                val raw = SupabaseProvider.client.postgrest.rpc(
                    "native_manager_save_teacher_class_v40c",
                    kotlinx.serialization.json.buildJsonObject {
                        put("p_teacher", kotlinx.serialization.json.JsonPrimitive(teacherId))
                        put("p_name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                        put("p_grade", kotlinx.serialization.json.JsonPrimitive(grade.trim()))
                        put("p_field", kotlinx.serialization.json.JsonPrimitive(fieldOfStudy.trim()))
                    }
                ).decodeAs<kotlinx.serialization.json.JsonObject>()
                (raw["error"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    ?.takeIf(String::isNotBlank)?.let(::error)
            }
            id == null -> repository.createClass(name, grade, fieldOfStudy).getOrThrow()
            else -> repository.updateClass(id, name, grade, fieldOfStudy).getOrThrow()
        }
        reloadData()
    }

    /** V61.5 — تنظیم فیلترهای لیست دانش‌آموزان. */
    fun setStudentFilter(filter: StudentListFilter) {
        _state.update { it.copy(studentFilter = filter) }
    }

    /** V61.5 — متادادهٔ فیلتر (معلم مالک/عضویت مدرسه)؛ نبودن SQL بی‌صدا خالی می‌ماند. */
    fun loadFilterMeta() = viewModelScope.launch {
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_student_filter_meta_v61")
                .decodeAs<kotlinx.serialization.json.JsonObject>()
            (raw["error"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?.takeIf(String::isNotBlank)?.let(::error)
            ((raw["items"] as? kotlinx.serialization.json.JsonArray) ?: kotlinx.serialization.json.JsonArray(emptyList())).mapNotNull { element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                id to StudentFilterMeta(
                    teacherId = (obj["teacher_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                    teacherName = (obj["teacher_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                    inSchool = (obj["in_school"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true",
                    schoolIds = ((obj["schools"] as? kotlinx.serialization.json.JsonArray)
                        ?: kotlinx.serialization.json.JsonArray(emptyList()))
                        .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        .toSet()
                )
            }.toMap()
        }.onSuccess { meta -> _state.update { it.copy(filterMeta = meta) } }
            .onFailure { }
    }

    /** V61.1 — مدیر مدرسهٔ جدید می‌سازد؛ لیست مدارس تازه می‌شود. */
    fun createSchool(name: String, province: String, city: String) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null) }
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc(
                "native_manager_create_school_v61",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                    put("p_province", kotlinx.serialization.json.JsonPrimitive(province.trim()))
                    put("p_city", kotlinx.serialization.json.JsonPrimitive(city.trim()))
                }
            ).decodeAs<kotlinx.serialization.json.JsonObject>()
            (raw["error"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?.takeIf(String::isNotBlank)?.let(::error)
        }.onSuccess {
            _state.update { it.copy(actionLoading = false, message = "مدرسه ساخته شد.") }
            loadSchoolsNow()
        }.onFailure { error ->
            _state.update { it.copy(actionLoading = false, error = safeSchoolError(error)) }
        }
    }

    /** V61.0 — معلم‌های مدرسه برای انتخاب در ساخت کلاس؛ غیرمدیر بی‌صدا خالی می‌ماند. */
    fun loadSchoolTeachers() = viewModelScope.launch {
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_manager_teachers_v37")
                .decodeAs<kotlinx.serialization.json.JsonObject>()
            (raw["error"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?.takeIf(String::isNotBlank)?.let(::error)
            ((raw["items"] as? kotlinx.serialization.json.JsonArray) ?: kotlinx.serialization.json.JsonArray(emptyList())).mapNotNull { element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                SchoolTeacherPick(id, (obj["full_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty())
            }
        }.onSuccess { list -> _state.update { it.copy(schoolTeachers = list) } }
            .onFailure { _state.update { it.copy(schoolTeachers = emptyList()) } }
    }

    fun deleteClass(id: String) = action("کلاس حذف شد؛ حساب دانش‌آموزان حفظ شد.") {
        repository.deleteClass(id).getOrThrow()
        _state.update { it.copy(selectedClass = null, roster = emptyList()) }
        reloadData()
    }

    fun addStudents(studentIds: List<String>) {
        val classId = state.value.selectedClass?.id ?: return
        action("دانش‌آموزان به کلاس اضافه شدند.") {
            repository.addStudentsToClass(classId, studentIds).getOrThrow()
            reloadData()
            loadRosterNow(classId)
        }
    }

    fun addStudentToClasses(studentId: String, classIds: Set<String>) = action(
        "دانش‌آموز به کلاس‌های انتخاب‌شده اضافه شد."
    ) {
        repository.addStudentToClasses(studentId, classIds).getOrThrow()
        reloadData()
        state.value.selectedClass?.id?.let { loadRosterNow(it) }
    }

    fun addClassStudentToMyList(studentId: String) {
        val classId = state.value.selectedClass?.id ?: return
        action("دانش‌آموز به لیست شما اضافه شد.") {
            repository.addClassStudentToMyList(classId, studentId).getOrThrow()
            reloadData(); loadRosterNow(classId)
        }
    }

    fun removeStudent(studentId: String) {
        val classId = state.value.selectedClass?.id ?: return
        action("دانش‌آموز از کلاس خارج شد؛ حساب او حفظ شد.") {
            repository.removeStudentFromClass(classId, studentId).getOrThrow()
            reloadData()
            loadRosterNow(classId)
        }
    }

    fun setStudentActive(studentId: String, active: Boolean) = action(
        if (active) "دانش‌آموز فعال شد." else "دانش‌آموز غیرفعال شد."
    ) {
        repository.setStudentActive(studentId, active).getOrThrow()
        reloadData()
        state.value.selectedClass?.id?.let { loadRosterNow(it) }
    }

    fun createStudent(request: NewStudentRequest) = action("حساب دانش‌آموز ساخته شد.") {
        val credential = repository.createStudent(request).getOrThrow()
        reloadData()
        request.classId?.let { loadRosterNow(it) }
        _state.update { it.copy(lastCredential = credential) }
    }

    fun createStudentsBulk(classId:String?,requests:List<NewStudentRequest>) = action("ساخت گروهی انجام شد.") {
        val result=repository.createStudentsBulk(classId,requests).getOrThrow();reloadData();classId?.takeIf(String::isNotBlank)?.let{loadRosterNow(it)};_state.update{it.copy(bulkResult=result)}
    }

    fun saveStudentNote(studentId:String,note:String)=viewModelScope.launch{
        if(noteDao==null||ownerId.isBlank())return@launch
        val clean=note.take(4000)
        if(clean.isBlank())noteDao.delete(ownerId,studentId)else noteDao.upsert(StudentNoteEntity(ownerId,studentId,clean,System.currentTimeMillis()))
        _state.update{it.copy(studentNotes=if(clean.isBlank())it.studentNotes-studentId else it.studentNotes+(studentId to clean),message="یادداشت خصوصی ذخیره شد.")}
    }

    fun updateStudent(request: UpdateStudentRequest) = action("مشخصات دانش‌آموز ویرایش شد.") {
        repository.updateStudent(request).getOrThrow()
        reloadData()
        state.value.selectedClass?.id?.let { loadRosterNow(it) }
        request.newPassword?.takeIf(String::isNotBlank)?.let { password ->
            // فقط پس از موفقیت سرور و فقط در حافظه تا بسته‌شدن پنجره نمایش داده می‌شود.
            _state.update {
                it.copy(lastCredential = StudentCredential(request.id, request.username, password))
            }
        }
    }

    fun resetPassword(studentId: String, password: String) = action("رمز دانش‌آموز تغییر کرد؛ فقط همین بار نمایش داده می‌شود.") {
        val credential = repository.resetStudentPassword(studentId, password).getOrThrow()
        _state.update { it.copy(lastCredential = credential) }
    }

    fun deleteStudent(studentId: String) = action("حساب دانش‌آموز و عضویت‌های وابسته حذف شد.") {
        repository.deleteStudent(studentId).getOrThrow()
        reloadData()
        state.value.selectedClass?.id?.let { loadRosterNow(it) }
    }

    private fun loadRoster(classId: String) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null) }
        repository.getClassRoster(classId)
            .onSuccess { roster -> _state.update { it.copy(actionLoading = false, roster = roster) } }
            .onFailure { error -> _state.update { it.copy(actionLoading = false, error = safeSchoolError(error)) } }
    }

    private suspend fun loadRosterNow(classId: String) {
        val roster = repository.getClassRoster(classId).getOrThrow()
        _state.update { it.copy(roster = roster) }
    }

    // V61.0 — نمای «مدارس»: لیست مدرسه‌های عضو، کلاس‌های معلم در مدرسهٔ انتخابی.
    fun openSchools() = viewModelScope.launch {
        _state.update { it.copy(schoolsOpen = true, selectedSchool = null, schoolClasses = emptyList(), error = null) }
        loadSchoolsNow()
    }

    fun closeSchools() {
        _state.update { it.copy(schoolsOpen = false, selectedSchool = null, schoolClasses = emptyList()) }
    }

    /** V61.8 — لیست مدارس بدون بازکردن نما (برای بخش «مدرسه» فیلتر). */
    fun refreshSchoolList() = viewModelScope.launch { loadSchoolsNow() }

    fun selectSchool(item: TeacherSchoolItem) = viewModelScope.launch {
        _state.update { it.copy(selectedSchool = item, schoolClasses = emptyList(), error = null) }
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "native_teacher_school_classes_v61",
                kotlinx.serialization.json.buildJsonObject {
                    put("p_school", kotlinx.serialization.json.JsonPrimitive(item.id))
                }
            ).decodeList<ir.exam.app.data.dto.SchoolClassDto>().map(ir.exam.app.data.dto.SchoolClassDto::toDomain)
        }.onSuccess { list -> _state.update { it.copy(schoolClasses = list) } }
            .onFailure { error -> _state.update { it.copy(error = safeSchoolError(error)) } }
    }

    fun closeSchool() {
        _state.update { it.copy(selectedSchool = null, schoolClasses = emptyList()) }
    }

    private suspend fun loadSchoolsNow() {
        runCatching {
            val raw = SupabaseProvider.client.postgrest.rpc("native_teacher_schools_v61")
                .decodeAs<kotlinx.serialization.json.JsonObject>()
            (raw["error"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?.takeIf(String::isNotBlank)?.let(::error)
            ((raw["items"] as? kotlinx.serialization.json.JsonArray) ?: kotlinx.serialization.json.JsonArray(emptyList())).mapNotNull { element ->
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                TeacherSchoolItem(
                    id = id,
                    name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                    province = (obj["province"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                    city = (obj["city"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                    classCount = (obj["classes"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0
                )
            }
        }.onSuccess { list -> _state.update { it.copy(schools = list) } }
            .onFailure { error -> _state.update { it.copy(error = safeSchoolError(error)) } }
    }

    private suspend fun reloadData() {
        val classes = repository.getClasses().getOrThrow()
        val students = repository.getStudents().getOrThrow()
        _state.update { old ->
            old.copy(
                classes = classes,
                students = students,
                selectedClass = old.selectedClass?.id?.let { id -> classes.firstOrNull { it.id == id } }
            )
        }
    }

    private fun action(successMessage: String, block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(actionLoading = true, error = null, message = null, lastCredential = null) }
        runCatching { block() }
            .onSuccess { _state.update { it.copy(actionLoading = false, message = successMessage) } }
            .onFailure { error -> _state.update { it.copy(actionLoading = false, error = safeSchoolError(error)) } }
    }

    private fun failLoad(error: Throwable) {
        _state.update { it.copy(loading = false, error = safeSchoolError(error)) }
    }
}

private fun safeSchoolError(error: Throwable): String {
    val raw = error.message.orEmpty()
        .substringBefore("URL:")
        .substringBefore("Headers:")
        .replace(Regex("(?i)authorization[^,\n]*"), "")
        .replace(Regex("(?i)apikey[^,\n]*"), "")
        .take(240)
    return raw.ifBlank { "عملیات کلاس و دانش‌آموز ناموفق بود." }
}
