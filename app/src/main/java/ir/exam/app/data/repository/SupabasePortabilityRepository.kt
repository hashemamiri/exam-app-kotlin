package ir.exam.app.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.dto.NativeProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.BackupPreview
import ir.exam.app.domain.model.OfficialExamPrintable
import ir.exam.app.domain.model.OfficialPrintHeader
import ir.exam.app.domain.model.OfficialPrintQuestion
import ir.exam.app.domain.model.PortableFile
import ir.exam.app.domain.model.RestoreOptions
import ir.exam.app.domain.model.RestoreSummary
import ir.exam.app.ui.builder.QuestionType
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class SupabasePortabilityRepository {
    private val prettyJson = Json { prettyPrint = true; explicitNulls = false }

    suspend fun exportExam(examId: String): Result<PortableFile> = runCatching {
        val uid = currentUserId()
        val exam = SupabaseProvider.client.from("exams").select {
            filter { eq("id", examId); eq("teacher_id", uid) }
        }.decodeList<ExamDetailDto>().firstOrNull() ?: error("آزمون یافت نشد یا متعلق به این حساب نیست.")
        val key = SupabaseProvider.client.from("exam_keys").select {
            filter { eq("exam_id", examId) }
        }.decodeList<ExamKeyDto>().firstOrNull()?.answers ?: JsonArray(emptyList())
        val profile = SupabaseProvider.client.postgrest.rpc("native_my_profile").decodeSingle<NativeProfileDto>()
        val questions = ExamQuestionCodec.decode(exam.questions, key)
        val content = ExamPackageCodec.encode(
            ExamPackageCodec.ExportedExam(
                title = exam.title,
                subject = exam.subject.orEmpty(),
                duration = exam.duration ?: 0,
                negativeMarking = exam.negativeMarking,
                shuffleQuestions = exam.shuffleQuestions,
                shuffleOptions = exam.shuffleOptions,
                teacherMessage = exam.teacherMessage.orEmpty(),
                attemptsAllowed = exam.attemptsAllowed,
                attemptOnTimeout = exam.attemptOnTimeout,
                gradePolicy = exam.gradePolicy,
                attemptCooldown = exam.attemptCooldown,
                questions = questions,
                by = profile.displayName?.takeIf(String::isNotBlank) ?: profile.fullName.orEmpty()
            )
        )
        PortableFile(
            fileName = ExamPackageCodec.safeFileName(exam.title),
            mimeType = "application/octet-stream",
            content = content
        )
    }

    suspend fun printableExam(examId: String, includeAnswerKey: Boolean): Result<OfficialExamPrintable> = runCatching {
        val uid = currentUserId()
        val exam = SupabaseProvider.client.from("exams").select {
            filter { eq("id", examId); eq("teacher_id", uid) }
        }.decodeList<ExamDetailDto>().firstOrNull() ?: error("آزمون برای چاپ یافت نشد.")
        val key = SupabaseProvider.client.from("exam_keys").select {
            filter { eq("exam_id", examId) }
        }.decodeList<ExamKeyDto>().firstOrNull()?.answers ?: JsonArray(emptyList())
        val profile = SupabaseProvider.client.postgrest.rpc("native_my_profile").decodeSingle<NativeProfileDto>()
        val questions = ExamQuestionCodec.decode(exam.questions, key)
        OfficialExamPrintable(
            documentTitle = exam.title,
            header = OfficialPrintHeader(
                province = profile.headerProvince.orEmpty(),
                city = profile.headerCity.orEmpty(),
                district = profile.headerDistrict.orEmpty(),
                school = profile.headerSchool.orEmpty()
            ),
            subject = exam.subject.orEmpty(),
            durationMinutes = exam.duration ?: 0,
            totalScore = exam.totalScore,
            includeAnswerKey = includeAnswerKey,
            questions = questions.mapIndexed { index, question ->
                val answer = when (question.type) {
                    QuestionType.MULTIPLE_CHOICE -> question.correctIndex?.let { question.options.getOrNull(it) }
                    QuestionType.TRUE_FALSE -> if (question.expectedText == "true") "صحیح" else "غلط"
                    QuestionType.FILL_BLANK -> question.expectedText.replace('|', '،')
                    QuestionType.NUMERIC -> question.expectedNumber + " ± " + question.tolerance
                    QuestionType.MATCHING -> question.matchingPairs.entries.sortedBy { it.key }
                        .joinToString("، ") { (left, right) -> "${left + 1}←${right + 1}" }
                    QuestionType.ESSAY -> null
                }
                OfficialPrintQuestion(
                    number = index + 1,
                    text = question.text,
                    score = question.score,
                    options = question.options,
                    answerText = answer,
                    answerLines = if (question.type == QuestionType.ESSAY) 5 else 2,
                    imageUrls = question.images.map { it.uri } + question.optionImages.filterNotNull()
                )
            }
        )
    }

    fun parseExam(raw: String) = ExamPackageCodec.decode(raw)

    suspend fun exportBackup(): Result<PortableFile> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_export_backup_v1")
            .decodeSingle<JsonObject>()
        raw.throwPortabilityError()
        val content = prettyJson.encodeToString(JsonObject.serializer(), raw)
        check(content.toByteArray(StandardCharsets.UTF_8).size <= MAX_BACKUP_BYTES) {
            "حجم پشتیبان از سقف ۲۰ مگابایت بیشتر است."
        }
        val date = ir.exam.app.core.calendar.JalaliCalendar.fromGregorian(LocalDate.now()).display().replace('/', '-')
        PortableFile("پشتیبان-سامانه-آزمون-$date.json", "application/json", content)
    }

    fun parseBackup(raw: String): BackupPreview {
        require(raw.toByteArray(StandardCharsets.UTF_8).size <= MAX_BACKUP_BYTES) { "حجم پشتیبان بیش از ۲۰ مگابایت است." }
        val root = prettyJson.parseToJsonElement(raw).jsonObject
        require(root["_app"]?.jsonPrimitive?.contentOrNull == "exam-native") { "فایل پشتیبان متعلق به نسخه Native نیست." }
        require(root["_kind"]?.jsonPrimitive?.contentOrNull == "backup") { "نوع فایل، پشتیبان کامل نیست." }
        require((root["_version"]?.jsonPrimitive?.intOrNull ?: 0) in 1..2) { "نسخه فایل پشتیبان پشتیبانی نمی‌شود." }
        val exams = root["exams"]?.jsonArray ?: JsonArray(emptyList())
        val classes = root["classes"]?.jsonArray ?: JsonArray(emptyList())
        require(exams.size <= 200) { "تعداد آزمون‌های پشتیبان بیش از حد مجاز است." }
        require(classes.size <= 500) { "تعداد کلاس‌های پشتیبان بیش از حد مجاز است." }
        val memberships = classes.sumOf { item -> item.jsonObject["members"]?.jsonArray?.size ?: 0 }
        val totalQuestions = exams.sumOf { item -> item.jsonObject["questions"]?.jsonArray?.size ?: 0 }
        require(totalQuestions <= 10_000) { "مجموع سؤال‌های پشتیبان بیش از حد مجاز است." }
        return BackupPreview(
            createdAt = root["created_at"]?.jsonPrimitive?.contentOrNull,
            teacherName = root["profile"]?.jsonObject?.get("display_name")?.jsonPrimitive?.contentOrNull
                ?: root["profile"]?.jsonObject?.get("full_name")?.jsonPrimitive?.contentOrNull,
            examCount = exams.size,
            totalQuestionCount = totalQuestions,
            classCount = classes.size,
            membershipCount = memberships,
            hasHeader = root["profile"]?.jsonObject?.get("header") is JsonObject,
            bundle = root
        )
    }

    suspend fun restoreBackup(
        preview: BackupPreview,
        options: RestoreOptions,
        operationId: String = UUID.randomUUID().toString()
    ): Result<RestoreSummary> = runCatching {
        require(options.exams || options.classes || options.header) { "حداقل یک بخش برای بازیابی انتخاب کنید." }
        val raw = SupabaseProvider.client.postgrest.rpc(
            "native_restore_backup_v1",
            buildJsonObject {
                put("p_operation", operationId)
                put("p_bundle", preview.bundle)
                put("p_options", buildJsonObject {
                    put("exams", options.exams)
                    put("classes", options.classes)
                    put("memberships", options.memberships)
                    put("header", options.header)
                })
            }
        ).decodeSingle<JsonObject>()
        raw.throwPortabilityError()
        RestoreSummary(
            examsCreated = raw["exams_created"]?.jsonPrimitive?.intOrNull ?: 0,
            classesCreated = raw["classes_created"]?.jsonPrimitive?.intOrNull ?: 0,
            membershipsRestored = raw["memberships_restored"]?.jsonPrimitive?.intOrNull ?: 0,
            membershipsMissing = raw["memberships_missing"]?.jsonPrimitive?.intOrNull ?: 0,
            chargedToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0,
            balanceToman = raw["balance"]?.jsonPrimitive?.longOrNull ?: 0
        )
    }

    private fun currentUserId(): String = SupabaseProvider.client.auth.currentUserOrNull()?.id
        ?: error("نشست معلم پیدا نشد.")

    private companion object {
        const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
    }
}

private fun JsonObject.throwPortabilityError(): JsonObject {
    this["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
    return this
}
