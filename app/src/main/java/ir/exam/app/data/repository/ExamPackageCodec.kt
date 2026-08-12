package ir.exam.app.data.repository

import ir.exam.app.ui.builder.ExamImportDraft
import ir.exam.app.ui.builder.QuestionDraft
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ExamPackageCodec {
    const val TAG = "EXAMPKG1"
    const val EXTENSION = ".azmoon"
    private const val MAX_PACKAGE_BYTES = 8 * 1024 * 1024
    private const val MAX_QUESTIONS = 500
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    data class ExportedExam(
        val title: String,
        val subject: String,
        val duration: Int,
        val negativeMarking: Double,
        val shuffleQuestions: Boolean,
        val shuffleOptions: Boolean,
        val teacherMessage: String,
        val attemptsAllowed: Int,
        val attemptOnTimeout: Boolean,
        val gradePolicy: String,
        val attemptCooldown: Int,
        val questions: List<QuestionDraft>,
        val by: String,
        val opensAtIso: String? = null,
        val closesAtIso: String? = null
    )

    fun encode(source: ExportedExam): String {
        validateQuestions(source.questions)
        val encoded = ExamQuestionCodec.encode(source.questions)
        val combined = JsonArray(encoded.publicQuestions.mapIndexed { index, item ->
            val public = item.jsonObject
            val answer = encoded.answerKey.getOrNull(index)?.jsonObject ?: JsonObject(emptyMap())
            JsonObject(public + (answer - "i"))
        })
        val root = buildJsonObject {
            put("_app", "exam-system")
            put("_kind", "exam")
            put("_v", 2)
            put("exported_at", java.time.Instant.now().toString())
            put("by", source.by.take(120))
            put("exam", buildJsonObject {
                put("title", source.title.take(250))
                put("subject", source.subject.take(250))
                put("duration", source.duration.coerceIn(0, 1440))
                put("opens_at", source.opensAtIso)
                put("closes_at", source.closesAtIso)
                put("neg_marking", source.negativeMarking.coerceAtLeast(0.0))
                put("shuffle_q", source.shuffleQuestions)
                put("shuffle_opt", source.shuffleOptions)
                put("teacher_message", source.teacherMessage.take(1000))
                put("attempts_allowed", source.attemptsAllowed.coerceIn(1, 5))
                put("attempt_on_timeout", source.attemptOnTimeout)
                put("grade_policy", source.gradePolicy)
                put("attempt_cooldown", source.attemptCooldown.coerceIn(0, 1440))
                put("questions", combined)
            })
        }
        val raw = json.encodeToString(JsonObject.serializer(), root)
        val body = Base64.getEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
        return "$TAG\n$body"
    }

    fun decode(raw: String): ExamImportDraft {
        require(raw.toByteArray(StandardCharsets.UTF_8).size <= MAX_PACKAGE_BYTES) { "حجم فایل آزمون بیش از ۸ مگابایت است." }
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "فایل آزمون خالی است." }
        val jsonText = when {
            trimmed.startsWith(TAG) -> decodeBase64(trimmed.removePrefix(TAG).trim())
            trimmed.startsWith("{") -> trimmed
            else -> decodeBase64(trimmed)
        }
        val root = json.parseToJsonElement(jsonText).jsonObject
        require(root["_app"]?.jsonPrimitive?.contentOrNull == "exam-system") { "این فایل متعلق به سامانه آزمون نیست." }
        require(root["_kind"]?.jsonPrimitive?.contentOrNull == "exam") { "نوع فایل، بسته آزمون نیست." }
        val exam = root["exam"]?.jsonObject ?: error("بدنه آزمون در فایل وجود ندارد.")
        val combined = exam["questions"]?.jsonArray ?: JsonArray(emptyList())
        require(combined.size in 1..MAX_QUESTIONS) { "تعداد سؤال‌های فایل باید بین ۱ و ۵۰۰ باشد." }
        val questions = ExamQuestionCodec.decode(combined, combined).map(::sanitizeImportedQuestion)
        validateQuestions(questions)
        return ExamImportDraft(
            title = exam.text("title", 250).ifBlank { "آزمون واردشده" },
            subject = exam.text("subject", 250),
            durationMinutes = exam["duration"]?.jsonPrimitive?.intOrNull?.coerceIn(0, 1440) ?: 0,
            negativeMarking = exam["neg_marking"]?.jsonPrimitive?.doubleOrNull?.coerceAtLeast(0.0) ?: 0.0,
            shuffleQuestions = exam["shuffle_q"]?.jsonPrimitive?.booleanOrNull ?: false,
            shuffleOptions = exam["shuffle_opt"]?.jsonPrimitive?.booleanOrNull ?: false,
            teacherMessage = exam.text("teacher_message", 1000),
            attemptsAllowed = exam["attempts_allowed"]?.jsonPrimitive?.intOrNull?.coerceIn(1, 5) ?: 1,
            attemptOnTimeout = exam["attempt_on_timeout"]?.jsonPrimitive?.booleanOrNull ?: false,
            gradePolicy = exam["grade_policy"]?.jsonPrimitive?.contentOrNull?.takeIf { it in setOf("last", "best", "all") } ?: "last",
            attemptCooldown = exam["attempt_cooldown"]?.jsonPrimitive?.intOrNull?.coerceIn(0, 1440) ?: 0,
            questions = questions,
            opensAtIso = exam["opens_at"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank),
            closesAtIso = exam["closes_at"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank),
            exportedBy = root["by"]?.jsonPrimitive?.contentOrNull?.take(120)
        )
    }

    fun safeFileName(title: String): String {
        val safe = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(48).ifBlank { "exam" }
        return "آزمون-$safe$EXTENSION"
    }

    private fun decodeBase64(value: String): String = try {
        String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        error("رمزگذاری فایل آزمون معتبر نیست.")
    }

    private fun JsonObject.text(key: String, max: Int): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty().take(max)

    private fun sanitizeImportedQuestion(question: QuestionDraft): QuestionDraft {
        fun safeUrl(value: String?): String? = value?.takeIf { it.startsWith("https://", true) }?.take(2048)
        return question.copy(
            text = question.text.take(10_000),
            options = question.options.take(20).map { it.take(2_000) },
            optionImages = question.optionImages.take(20).map(::safeUrl),
            matchingLeft = question.matchingLeft.take(30).map { it.take(2_000) },
            matchingRight = question.matchingRight.take(30).map { it.take(2_000) },
            matchingLeftImages = question.matchingLeftImages.take(30).map(::safeUrl),
            matchingRightImages = question.matchingRightImages.take(30).map(::safeUrl),
            images = question.images.take(10).mapNotNull { image -> safeUrl(image.uri)?.let { image.copy(uri = it) } },
            score = question.score.coerceIn(0.0, 10_000.0),
            maxAnswerImages = question.maxAnswerImages.coerceIn(0, 10)
        )
    }

    private fun validateQuestions(questions: List<QuestionDraft>) {
        require(questions.size in 1..MAX_QUESTIONS) { "تعداد سؤال‌ها نامعتبر است." }
        require(questions.all { it.text.isNotBlank() && it.text.length <= 10_000 }) { "متن یک سؤال خالی یا بیش از حد بلند است." }
        require(questions.all { it.score in 0.0..10_000.0 }) { "بارم یک سؤال نامعتبر است." }
    }
}
