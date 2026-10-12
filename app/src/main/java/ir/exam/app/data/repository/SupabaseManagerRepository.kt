package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.ManagerWalletRules
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal data class SchoolTeacherItem(
    val id: String,
    val fullName: String,
    val username: String = "",
    val email: String = "",
    val employeeCode: String = "",
    val phone: String = "",
    val active: Boolean = true,
    val walletBalanceToman: Long = 0
)

internal data class TeacherInviteResult(val code: String)
internal data class ManagerInviteItem(
    val id: String, val code: String, val expiresAt: String, val used: Boolean, val revoked: Boolean,
    // V61.7 — لحظهٔ استفاده برای نمایش زمان‌سنج «منجمد» کد استفاده‌شده.
    val usedAt: String = ""
)
internal data class ManagerTeacherClass(
    val id: String, val name: String, val grade: String, val field: String, val total: Int
)
internal data class ManagerStudentItem(val id: String, val fullName: String, val username: String)
// V62.6 — مدرسهٔ قابل انتخاب برای مقصد کد دعوت.
internal data class ManagerSchoolOption(val id: String, val name: String)
internal data class ManagerTeacherClasses(val teacherName: String, val items: List<ManagerTeacherClass>)
internal data class ManagerClassRoster(val className: String, val items: List<ManagerStudentItem>)
// V261 — کارنامهٔ کلاس (native_manager_class_report_v1) و درخواست‌های خود مدیر (native_manager_my_requests_v1)
internal data class ManagerReportExam(val id: String, val title: String, val totalScore: Double)
internal data class ManagerReportGrade(val grade: Double, val max: Double, val graded: Boolean)
internal data class ManagerReportStudent(val id: String, val fullName: String, val username: String, val grades: Map<String, ManagerReportGrade>)
internal data class ManagerClassReport(val className: String, val exams: List<ManagerReportExam>, val students: List<ManagerReportStudent>)
internal data class ManagerMyRequest(val id: String, val targetType: String, val targetName: String, val action: String, val status: String, val teacherName: String, val changeSummary: String, val createdAt: String)
/** V261 — نتیجهٔ تغییر کلاس معلم: اجرا شد یا به‌صورت درخواست برای معلم ثبت شد (خطا نیست). */
internal data class ManagerChangeResult(val requested: Boolean, val message: String)
internal data class WalletTransferResult(
    val amountToman: Long,
    val managerBalanceToman: Long,
    val teacherBalanceToman: Long
)

internal class SupabaseManagerRepository {
    suspend fun teachers(): Result<List<SchoolTeacherItem>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_manager_teachers_v37").decodeAs<JsonObject>().checked()
        (raw["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            SchoolTeacherItem(
                id = item.text("id"),
                fullName = item.text("full_name"),
                username = item.text("username"),
                email = item.text("email"),
                employeeCode = item.text("employee_code"),
                phone = item.text("phone"),
                active = item.text("status") == "active",
                walletBalanceToman = item["wallet_balance"]?.jsonPrimitive?.longOrNull ?: 0
            )
        }
    }

    // V62.6 — کد دعوت با مدرسهٔ مقصد انتخابی؛ بدون مدرسه = مسیر قدیمی V40B.
    suspend fun createInvites(count: Int, schoolId: String? = null): Result<List<ManagerInviteItem>> = runCatching {
        require(count in 1..5) { "تعداد کد باید بین ۱ تا ۵ باشد." }
        val raw = if (schoolId != null) {
            SupabaseProvider.client.postgrest.rpc(
                "native_manager_create_teacher_invites_v62",
                buildJsonObject { put("p_count", count); put("p_school", schoolId) }
            ).decodeAs<JsonObject>().checked()
        } else {
            SupabaseProvider.client.postgrest.rpc(
                "native_manager_create_teacher_invites_v40b",
                buildJsonObject { put("p_count", count) }
            ).decodeAs<JsonObject>().checked()
        }
        raw.inviteItems()
    }

    // V62.6 — مدرسه‌های مدیر برای انتخاب مقصد کد دعوت (همان RPC مدارس V61).
    suspend fun managerSchools(): Result<List<ManagerSchoolOption>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_teacher_schools_v61")
            .decodeAs<JsonObject>().checked()
        (raw["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            ManagerSchoolOption(item.text("id"), item.text("name"))
        }
    }

    suspend fun invites(): Result<List<ManagerInviteItem>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("native_manager_invites_v40b")
            .decodeAs<JsonObject>().checked().inviteItems()
    }

    suspend fun revokeInvite(id: String): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_revoke_invite_v40b", buildJsonObject { put("p_invite", id) }
        ).decodeAs<JsonObject>().checked(); Unit
    }

    /**
     * V61.8 — حذف پایدار کارت کد دعوت: revoke قدیمی فقط سطر را باطل می‌کرد و
     * لیست سرور همچنان کارت را برمی‌گرداند (کارت پس از refresh «برمی‌گشت»).
     * حذف واقعی سطر = ابطال فوری کد استفاده‌نشده. اگر تابع v61 هنوز deploy
     * نشده بود، به revoke قدیمی برمی‌گردیم تا کد لااقل باطل شود.
     */
    suspend fun deleteInvite(id: String): Result<Unit> = runCatching {
        val result = runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "native_manager_delete_invite_v61", buildJsonObject { put("p_invite", id) }
            ).decodeAs<JsonObject>().checked()
        }
        if (result.isFailure) {
            SupabaseProvider.client.postgrest.rpc(
                "native_manager_revoke_invite_v40b", buildJsonObject { put("p_invite", id) }
            ).decodeAs<JsonObject>().checked()
        }
        Unit
    }

    suspend fun transferWallet(teacherId: String, amountToman: Long): Result<WalletTransferResult> = runCatching {
        ManagerWalletRules.validateTransfer(amountToman)
        val operationId = java.util.UUID.randomUUID().toString()
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_manager_transfer_wallet_v38",
            buildJsonObject {
                put("p_teacher", teacherId)
                put("p_amount_toman", amountToman)
                put("p_operation", operationId)
            }
        ).decodeAs<JsonObject>().checked()
        WalletTransferResult(
            amountToman = raw["amount"]?.jsonPrimitive?.longOrNull ?: amountToman,
            managerBalanceToman = raw["manager_balance"]?.jsonPrimitive?.longOrNull ?: 0,
            teacherBalanceToman = raw["teacher_balance"]?.jsonPrimitive?.longOrNull ?: 0
        )
    }

    suspend fun teacherClasses(teacherId: String): Result<ManagerTeacherClasses> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_manager_teacher_classes_v40c", buildJsonObject { put("p_teacher", teacherId) }).decodeAs<JsonObject>().checked()
        ManagerTeacherClasses(raw.text("teacher_name"), (raw["items"] as? JsonArray).orEmpty().mapNotNull { e ->
            val i=e as? JsonObject ?: return@mapNotNull null
            ManagerTeacherClass(i.text("id"),i.text("name"),i.text("grade"),i.text("field_of_study"),i["total"]?.jsonPrimitive?.intOrNull?:0)
        })
    }
    suspend fun createTeacherClass(teacherId:String,name:String,grade:String,field:String):Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc("native_manager_save_teacher_class_v40c",buildJsonObject{put("p_teacher",teacherId);put("p_name",name);put("p_grade",grade);put("p_field",field)}).decodeAs<JsonObject>().checked();Unit
    }
    suspend fun editTeacherClass(classId:String,name:String,grade:String,field:String):Result<ManagerChangeResult> = runCatching {
        val raw=SupabaseProvider.client.postgrest.rpc("native_manager_change_teacher_class_v41",buildJsonObject{put("p_class",classId);put("p_action","edit");put("p_payload",buildJsonObject{put("name",name);put("grade",grade);put("field",field)})}).decodeAs<JsonObject>().checked()
        raw.changeResult()
    }
    suspend fun deleteTeacherClass(classId:String):Result<ManagerChangeResult> = runCatching {
        val raw=SupabaseProvider.client.postgrest.rpc("native_manager_change_teacher_class_v41",buildJsonObject{put("p_class",classId);put("p_action","delete")}).decodeAs<JsonObject>().checked()
        raw.changeResult()
    }
    // V261 — approval_required خطا نیست؛ پیام اطلاع‌رسانی (قبلاً به‌صورت خطا نشان داده می‌شد)
    private fun JsonObject.changeResult(): ManagerChangeResult =
        if (this["approval_required"]?.jsonPrimitive?.booleanOrNull == true) ManagerChangeResult(true, "درخواست برای تأیید معلم ارسال شد؛ نتیجه در «درخواست‌های من» دیده می‌شود.")
        else ManagerChangeResult(false, "انجام شد.")

    /** V261 — کارنامهٔ کلاس: نمرهٔ هر دانش‌آموز در هر آزمونِ کلاس (فقط کلاس اشتراکی/ساختهٔ مدیر). */
    suspend fun classReport(classId: String): Result<ManagerClassReport> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_manager_class_report_v1", buildJsonObject { put("p_class", classId) }).decodeAs<JsonObject>().checked()
        val exams = (raw["exams"] as? JsonArray).orEmpty().mapNotNull { e ->
            val i = e as? JsonObject ?: return@mapNotNull null
            ManagerReportExam(i.text("id"), i.text("title"), i["total_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
        }
        val students = (raw["students"] as? JsonArray).orEmpty().mapNotNull { e ->
            val i = e as? JsonObject ?: return@mapNotNull null
            val grades = (i["grades"] as? JsonObject).orEmpty().mapNotNull { (examId, g) ->
                val go = g as? JsonObject ?: return@mapNotNull null
                examId to ManagerReportGrade(go["grade"]?.jsonPrimitive?.doubleOrNull ?: 0.0, go["max"]?.jsonPrimitive?.doubleOrNull ?: 0.0, go["graded"]?.jsonPrimitive?.booleanOrNull ?: false)
            }.toMap()
            ManagerReportStudent(i.text("id"), i.text("full_name"), i.text("username"), grades)
        }
        ManagerClassReport(raw.text("class_name"), exams, students)
    }

    /** V261 — درخواست‌های خود مدیر (در انتظار / انجام شد / رد شد). */
    suspend fun myRequests(): Result<List<ManagerMyRequest>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_manager_my_requests_v1").decodeAs<JsonObject>().checked()
        (raw["items"] as? JsonArray).orEmpty().mapNotNull { e ->
            val i = e as? JsonObject ?: return@mapNotNull null
            val pl = i["payload"] as? JsonObject
            fun p(k: String) = pl?.get(k)?.jsonPrimitive?.contentOrNull.orEmpty()
            val summary = when {
                i.text("action") == "delete" -> "حذف"
                p("kind") == "reset_password" -> "رمز جدید"
                p("name").isNotBlank() -> "ویرایش → «${p("name")}»"
                else -> "ویرایش"
            }
            ManagerMyRequest(i.text("id"), i.text("target_type"), i.text("target_name"), i.text("action"), i.text("status"), i.text("teacher_name"), summary, i.text("decided_at").ifBlank { i.text("created_at") })
        }
    }
    suspend fun classRoster(classId:String):Result<ManagerClassRoster> = runCatching {
        val raw=SupabaseProvider.client.postgrest.rpc("native_manager_class_roster_v40c",buildJsonObject{put("p_class",classId)}).decodeAs<JsonObject>().checked()
        ManagerClassRoster(raw.text("class_name"),raw.studentItems())
    }
    suspend fun schoolStudents():Result<List<ManagerStudentItem>> = runCatching {
        SupabaseProvider.client.postgrest.rpc("native_manager_school_students_v40c").decodeAs<JsonObject>().checked().studentItems()
    }
    suspend fun setClassStudent(classId:String,studentId:String,add:Boolean):Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc("native_manager_set_class_student_v40c",buildJsonObject{put("p_class",classId);put("p_student",studentId);put("p_add",add)}).decodeAs<JsonObject>().checked();Unit
    }

    suspend fun setTeacherActive(id: String, active: Boolean): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_set_teacher_active_v40b",
            buildJsonObject { put("p_teacher", id); put("p_active", active) }
        ).decodeAs<JsonObject>().checked(); Unit
    }

    suspend fun removeTeacher(id: String): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "native_manager_remove_teacher_v40b", buildJsonObject { put("p_teacher", id) }
        ).decodeAs<JsonObject>().checked(); Unit
    }

    private fun JsonObject.studentItems():List<ManagerStudentItem> =
        (this["items"] as? JsonArray).orEmpty().mapNotNull { e -> val i=e as? JsonObject?:return@mapNotNull null; ManagerStudentItem(i.text("id"),i.text("full_name"),i.text("username")) }

    private fun JsonObject.inviteItems(): List<ManagerInviteItem> =
        (this["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            ManagerInviteItem(
                id = item.text("id"), code = item.text("code"), expiresAt = item.text("expires_at"),
                used = item["used"]?.jsonPrimitive?.booleanOrNull ?: false,
                revoked = item["revoked"]?.jsonPrimitive?.booleanOrNull ?: false,
                usedAt = item.text("used_at")
            )
        }

    private fun JsonObject.checked(): JsonObject {
        this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        return this
    }
    private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
}
