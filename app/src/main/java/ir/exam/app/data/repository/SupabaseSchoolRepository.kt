package ir.exam.app.data.repository

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.dto.StudentProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.BulkStudentCreateResult
import ir.exam.app.domain.model.NewStudentRequest
import ir.exam.app.domain.model.SchoolClass
import ir.exam.app.domain.model.StudentCredential
import ir.exam.app.domain.model.StudentProfile
import ir.exam.app.domain.model.UpdateStudentRequest
import ir.exam.app.domain.repository.SchoolRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseSchoolRepository : SchoolRepository {
    override suspend fun getClasses(): Result<List<SchoolClass>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("my_classes")
            .decodeList<SchoolClassDto>()
            .map(SchoolClassDto::toDomain)
    }

    override suspend fun createClass(name: String, grade: String): Result<Unit> = runCatching {
        require(name.trim().isNotEmpty()) { "نام کلاس را وارد کنید." }
        rpcObject("create_class", buildJsonObject {
            put("p_name", name.trim())
            put("p_grade", grade.trim())
        }).throwIfError()
    }

    override suspend fun updateClass(id: String, name: String, grade: String): Result<Unit> = runCatching {
        require(name.trim().isNotEmpty()) { "نام کلاس را وارد کنید." }
        rpcObject("update_class", buildJsonObject {
            put("p_class", id)
            put("p_name", name.trim())
            put("p_grade", grade.trim())
        }).throwIfError()
    }

    override suspend fun deleteClass(id: String): Result<Unit> = runCatching {
        // این RPC فقط کلاس و عضویت‌ها را حذف می‌کند؛ حساب دانش‌آموزان حفظ می‌شود.
        rpcObject("delete_class", buildJsonObject { put("p_class", id) }).throwIfError()
    }

    override suspend fun getStudents(): Result<List<StudentProfile>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("my_students")
            .decodeList<StudentProfileDto>()
            .map(StudentProfileDto::toDomain)
    }

    override suspend fun getClassRoster(classId: String): Result<List<StudentProfile>> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "class_roster",
            buildJsonObject { put("p_class", classId) }
        ).decodeList<StudentProfileDto>().map(StudentProfileDto::toDomain)
    }

    override suspend fun addStudentsToClass(classId: String, studentIds: List<String>): Result<Int> = runCatching {
        require(studentIds.isNotEmpty()) { "حداقل یک دانش‌آموز انتخاب کنید." }
        val raw = rpcObject("add_students_to_class", buildJsonObject {
            put("p_class", classId)
            put("p_students", kotlinx.serialization.json.buildJsonArray {
                studentIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            })
        }).throwIfError()
        raw["added"]?.jsonPrimitive?.intOrNull ?: studentIds.size
    }

    override suspend fun addStudentToClasses(
        studentId: String,
        classIds: Set<String>
    ): Result<Int> = runCatching {
        require(studentId.isNotBlank()) { "دانش‌آموز نامعتبر است." }
        require(classIds.isNotEmpty()) { "حداقل یک کلاس انتخاب کنید." }
        val raw = rpcObject("native_add_student_to_classes_v22", buildJsonObject {
            put("p_student", studentId)
            put("p_classes", buildJsonArray {
                classIds.sorted().forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            })
        }).throwIfError()
        raw["added"]?.jsonPrimitive?.intOrNull ?: 0
    }

    override suspend fun removeStudentFromClass(classId: String, studentId: String): Result<Unit> = runCatching {
        rpcObject("remove_student_from_class", buildJsonObject {
            put("p_class", classId)
            put("p_student", studentId)
        }).throwIfError()
    }

    override suspend fun setStudentActive(studentId: String, active: Boolean): Result<Unit> = runCatching {
        rpcObject("set_student_active", buildJsonObject {
            put("p_student", studentId)
            put("p_active", active)
        }).throwIfError()
    }

    override suspend fun createStudent(request: NewStudentRequest): Result<StudentCredential> = runCatching {
        require(request.firstName.trim().isNotEmpty()) { "نام دانش‌آموز را وارد کنید." }
        require(request.username.matches(Regex("^[a-z0-9_]{4,20}$"))) {
            "نام کاربری باید ۴ تا ۲۰ کاراکتر انگلیسی، عدد یا _ باشد."
        }
        require(request.password.length in 8..72) { "رمز عبور باید بین ۸ تا ۷۲ کاراکتر باشد." }
        require(request.gender == "male" || request.gender == "female") { "جنسیت را انتخاب کنید." }

        val body = buildJsonObject {
            put("action", "create")
            put("first_name", request.firstName.trim())
            put("last_name", request.lastName.trim())
            put("username", request.username.trim().lowercase())
            put("password", request.password)
            put("gender", request.gender)
            put("class_id", request.classId.orEmpty())
        }
        val raw = SupabaseProvider.client.functions
            .invoke("manage-student", body = body)
            .body<JsonObject>()
            .throwIfError()
        val id = raw["id"]?.jsonPrimitive?.contentOrNull
            ?: error("شناسه دانش‌آموز از سرور دریافت نشد.")

        if (request.fatherName.isNotBlank() || request.grade.isNotBlank()) {
            rpcObject("save_student_extra", buildJsonObject {
                put("p_student", id)
                put("p_username", request.username.trim().lowercase())
                put("p_father_name", request.fatherName.trim())
                put("p_grade", request.grade.trim())
            }).throwIfError()
        }
        StudentCredential(id, request.username.trim().lowercase(), request.password)
    }

    override suspend fun createStudentsBulk(classId:String,requests:List<NewStudentRequest>):Result<BulkStudentCreateResult> = runCatching {
        require(classId.isNotBlank()){ "برای ساخت گروهی یک کلاس انتخاب کنید." }
        require(requests.size in 1..100){ "تعداد ردیف‌های گروهی باید بین ۱ و ۱۰۰ باشد." }
        val rows=buildJsonArray { requests.forEach { r ->
            require(r.firstName.isNotBlank()&&r.username.matches(Regex("^[a-z0-9_]{4,20}$"))&&r.password.length in 8..72&&r.gender in setOf("male","female")){"یک ردیف گروهی نامعتبر است."}
            add(buildJsonObject { put("first_name",r.firstName.trim());put("last_name",r.lastName.trim());put("username",r.username.lowercase());put("password",r.password);put("gender",r.gender) })
        } }
        val raw=SupabaseProvider.client.functions.invoke("manage-student",body=buildJsonObject{put("action","bulk");put("class_id",classId);put("rows",rows)}).body<JsonObject>().throwIfError()
        val credentials=mutableListOf<StudentCredential>();val failures=mutableListOf<String>()
        (raw["results"] as? JsonArray).orEmpty().forEach { element ->
            val row=element as? JsonObject ?: return@forEach;val username=row["username"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if(row["ok"]?.jsonPrimitive?.booleanOrNull==true){credentials+=StudentCredential(row["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),username,row["password"]?.jsonPrimitive?.contentOrNull.orEmpty())}
            else failures+="$username: ${row["message"]?.jsonPrimitive?.contentOrNull.orEmpty()}"
        }
        credentials.forEach { credential -> requests.firstOrNull{it.username.equals(credential.username,true)}?.let { r ->
            if(r.fatherName.isNotBlank()||r.grade.isNotBlank())rpcObject("save_student_extra",buildJsonObject{put("p_student",credential.id);put("p_username",credential.username);put("p_father_name",r.fatherName.trim());put("p_grade",r.grade.trim())}).throwIfError()
        } }
        BulkStudentCreateResult(credentials,failures)
    }

    override suspend fun updateStudent(request: UpdateStudentRequest): Result<Unit> = runCatching {
        require(request.id.isNotBlank()) { "شناسه دانش‌آموز نامعتبر است." }
        require(request.firstName.trim().isNotEmpty()) { "نام دانش‌آموز را وارد کنید." }
        require(request.username.matches(Regex("^[a-z0-9_]{4,20}$"))) { "نام کاربری نامعتبر است." }
        require(request.gender == "male" || request.gender == "female") { "جنسیت را انتخاب کنید." }
        require(request.newPassword == null || request.newPassword.length in 8..72) {
            "رمز جدید باید ۸ تا ۷۲ کاراکتر باشد."
        }
        SupabaseProvider.client.functions.invoke(
            "manage-student",
            body = buildJsonObject {
                put("action", "update")
                put("id", request.id)
                put("first_name", request.firstName.trim())
                put("last_name", request.lastName.trim())
                put("username", request.username.trim().lowercase())
                put("gender", request.gender)
                put("password", request.newPassword.orEmpty())
            }
        ).body<JsonObject>().throwIfError()
        rpcObject("save_student_extra", buildJsonObject {
            put("p_student", request.id)
            put("p_username", request.username.trim().lowercase())
            put("p_father_name", request.fatherName.trim())
            put("p_grade", request.grade.trim())
        }).throwIfError()
    }

    override suspend fun resetStudentPassword(studentId: String, newPassword: String): Result<StudentCredential> = runCatching {
        require(newPassword.length in 8..72) { "رمز جدید باید بین ۸ تا ۷۲ کاراکتر باشد." }
        val username = getStudents().getOrThrow().firstOrNull { it.id == studentId }?.username
            ?: error("دانش‌آموز یافت نشد.")
        SupabaseProvider.client.functions.invoke(
            "manage-student",
            body = buildJsonObject {
                put("action", "reset_password")
                put("id", studentId)
                put("password", newPassword)
            }
        ).body<JsonObject>().throwIfError()
        StudentCredential(studentId, username, newPassword)
    }

    override suspend fun deleteStudent(studentId: String): Result<Unit> = runCatching {
        SupabaseProvider.client.functions.invoke(
            "manage-student",
            body = buildJsonObject { put("action", "delete"); put("id", studentId) }
        ).body<JsonObject>().throwIfError()
    }

    private suspend fun rpcObject(name: String, parameters: JsonObject): JsonObject =
        SupabaseProvider.client.postgrest.rpc(name, parameters).decodeAs()
}

private fun JsonObject.throwIfError(): JsonObject {
    this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
    return this
}
