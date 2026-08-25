package ir.exam.app.data.repository

import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.MatchingQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import ir.exam.app.domain.model.NumericQuestion
import ir.exam.app.domain.model.Question
import ir.exam.app.domain.model.QuestionMediaPresentation
import ir.exam.app.domain.model.QuestionPresentation
import ir.exam.app.domain.model.TrueFalseQuestion
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** تبدیل payload امن get_exam_for_student و ترتیب تصادفی پایدار با نگاشت به اندیس اصلی. */
internal object StudentExamPayloadCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val answerFields = setOf(
        "correctOption", "correctIndex", "correctAnswer", "accept", "answer",
        "tolerance", "matchAnswer", "explanation", "answer_key"
    )

    fun sanitize(raw: JsonObject): JsonObject {
        val values = raw.toMutableMap()
        val questions = (raw["questions"] as? JsonArray).orEmpty().map { element ->
            val question = element as? JsonObject ?: return@map element
            JsonObject(question.filterKeys { it !in answerFields })
        }
        values["questions"] = JsonArray(questions)
        answerFields.forEach(values::remove)
        return JsonObject(values)
    }

    fun parse(raw: String): JsonObject = json.parseToJsonElement(raw).jsonObject

    fun decodeFresh(raw: JsonObject, studentId: String, receivedAtEpochMs: Long): Exam =
        decode(sanitize(raw), studentId, receivedAtEpochMs, overrideDeadline = false, deadlineEpochMs = null)

    fun decodeCached(
        raw: JsonObject,
        studentId: String,
        restoredAtEpochMs: Long,
        deadlineEpochMs: Long?
    ): Exam = decode(sanitize(raw), studentId, restoredAtEpochMs, overrideDeadline = true, deadlineEpochMs)

    private fun decode(
        raw: JsonObject,
        studentId: String,
        localNow: Long,
        overrideDeadline: Boolean,
        deadlineEpochMs: Long?
    ): Exam {
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let(::error)
        val examId = raw.text("id") ?: error("شناسه آزمون در پاسخ سرور موجود نیست.")
        val shuffleQuestions = raw.boolean("shuffle_q")
        val shuffleOptions = raw.boolean("shuffle_opt")
        val rawQuestions = raw["questions"].arrayOrEmpty().mapNotNull { it as? JsonObject }
        val parsedQuestions = rawQuestions.mapIndexed { index, item ->
            parseQuestion(
                fallbackIndex = index,
                obj = item,
                shuffleOptions = shuffleOptions,
                optionSeed = "$studentId:$examId:$index:options"
            )
        }
        val presentations = rawQuestions.mapIndexed { index, item ->
            val id = item.text("id") ?: "q-${item.int("i") ?: index}"
            id to parsePresentation(item)
        }.toMap()
        val orderedQuestions = if (shuffleQuestions) {
            StableExamShuffle.shuffled(parsedQuestions, "$studentId:$examId:questions")
        } else parsedQuestions
        val duration = raw.int("duration")?.coerceIn(0, 1440) ?: 0
        val computedDeadline = if (overrideDeadline) deadlineEpochMs else serverDeadline(raw, localNow, duration)

        return Exam(
            id = examId,
            title = raw.text("title").orEmpty().ifBlank { "آزمون" },
            code = raw.text("code").orEmpty(),
            durationMinutes = duration,
            questions = orderedQuestions,
            subject = raw.text("subject").orEmpty(),
            teacherMessage = raw.text("teacher_message")?.takeIf(String::isNotBlank),
            deadlineEpochMs = computedDeadline,
            shuffleQuestions = shuffleQuestions,
            shuffleOptions = shuffleOptions,
            attemptsAllowed = (raw.int("attempts_allowed") ?: 1).coerceIn(1, 5),
            attemptNumber = raw.int("attempt_no") ?: raw.int("attempt_number"),
            attemptsRemaining = raw.int("attempts_remaining"),
            questionPresentation = presentations
        )
    }

    private fun parseQuestion(
        fallbackIndex: Int,
        obj: JsonObject,
        shuffleOptions: Boolean,
        optionSeed: String
    ): Question {
        val originalIndex = obj.int("i") ?: fallbackIndex
        val id = obj.text("id") ?: "q-$originalIndex"
        val text = obj.text("text").orEmpty()
        val score = obj.double("score") ?: 0.0
        val images = obj["images"].arrayStrings().ifEmpty {
            obj.text("image")?.takeIf(String::isNotBlank)?.let(::listOf).orEmpty()
        }
        val allowImages = obj.text("allowImages") ?: obj.text("allow_images")
        val maxAnswerImages = if (allowImages == null || allowImages == "no") 0
            else (obj.int("maxImages") ?: obj.int("max_images") ?: 1).coerceIn(0, 10)
        val answerImagesRequired = allowImages == "required"

        return when (obj.text("type")?.lowercase()) {
            "multiple", "multiple_choice", "multiplechoice" -> {
                val options = obj["options"].arrayStrings()
                val optionImages = obj["optionImages"].arrayNullableStrings()
                val order = if (shuffleOptions) {
                    StableExamShuffle.shuffled(options.indices.toList(), "$optionSeed:$originalIndex")
                } else options.indices.toList()
                MultipleChoiceQuestion(
                    id = id,
                    text = text,
                    score = score,
                    options = order.map(options::get),
                    optionImages = order.map { optionImages.getOrNull(it) },
                    images = images,
                    maxAnswerImages = maxAnswerImages,
                    answerImagesRequired = answerImagesRequired,
                    originalIndex = originalIndex,
                    optionOriginalIndices = order
                )
            }
            "truefalse", "true_false" -> TrueFalseQuestion(
                id, text, score, images, maxAnswerImages, answerImagesRequired, originalIndex
            )
            "fill", "fill_blank" -> FillBlankQuestion(
                id, text, score, images, maxAnswerImages, answerImagesRequired, originalIndex
            )
            "numeric", "number" -> NumericQuestion(
                id, text, score, images, maxAnswerImages, answerImagesRequired, originalIndex
            )
            "matching", "match" -> {
                val right = obj["rightItems"].arrayStrings()
                val rightImages = obj["rightImages"].arrayNullableStrings()
                val rightOrder = if (shuffleOptions) {
                    StableExamShuffle.shuffled(right.indices.toList(), "$optionSeed:$originalIndex:matching")
                } else right.indices.toList()
                MatchingQuestion(
                    id = id,
                    text = text,
                    score = score,
                    leftItems = obj["leftItems"].arrayStrings(),
                    rightItems = rightOrder.map(right::get),
                    leftImages = obj["leftImages"].arrayNullableStrings(),
                    rightImages = rightOrder.map { rightImages.getOrNull(it) },
                    images = images,
                    maxAnswerImages = maxAnswerImages,
                    answerImagesRequired = answerImagesRequired,
                    originalIndex = originalIndex,
                    rightOriginalIndices = rightOrder
                )
            }
            else -> EssayQuestion(
                id, text, score, images, maxAnswerImages, answerImagesRequired, originalIndex
            )
        }
    }

    private fun parsePresentation(obj: JsonObject): QuestionPresentation {
        val positions = obj["imgFreePositions"].arrayOrEmpty()
        val images = obj["images"].arrayOrEmpty()
        return QuestionPresentation(
            textAlign = obj.text("align")?.takeIf { it in setOf("right", "center", "left", "justify") } ?: "right",
            imagePosition = obj.text("imgPos")?.takeIf { it in setOf("above", "below", "right", "left", "free") } ?: "below",
            fontFamily = obj.text("font").orEmpty().ifBlank { "default" },
            fontSizeSp = (obj.double("fontSize")?.toFloat() ?: 16f).coerceIn(8f, 40f),
            bold = obj.boolean("bold"),
            italic = obj.boolean("italic"),
            answerLines = (obj.int("answerLines") ?: 2).coerceIn(0, 12),
            answerLineStyle = obj.text("answerLineStyle")?.takeIf { it in setOf("lined", "blank") } ?: "lined",
            allowAnswerGraph = obj.boolean("allowAnswerGraph"),
            media = images.indices.map { index ->
                val pos = positions.getOrNull(index) as? JsonObject
                QuestionMediaPresentation(
                    xMm = pos?.double("x")?.toFloat() ?: 20f,
                    yMm = pos?.double("y")?.toFloat() ?: 30f,
                    widthMm = pos?.double("w")?.toFloat() ?: 55f
                )
            }
        )
    }

    private fun serverDeadline(raw: JsonObject, localNow: Long, durationMinutes: Int): Long? {
        val expires = raw.text("expires_at")?.let(::parseInstantMillis)
        if (expires != null) {
            val serverNow = raw.text("server_now")?.let(::parseInstantMillis)
            val remaining = if (serverNow != null) expires - serverNow else expires - localNow
            return localNow + remaining
        }
        return if (durationMinutes > 0) localNow + durationMinutes * 60_000L else null
    }

    private fun parseInstantMillis(value: String): Long? = runCatching {
        Instant.parse(value).toEpochMilli()
    }.recoverCatching {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    }.getOrNull()
}

internal object StableExamShuffle {
    fun <T> shuffled(values: List<T>, seed: String): List<T> {
        if (values.size < 2) return values.toList()
        val result = values.toMutableList()
        var state = 0L
        seed.forEach { char -> state = (state * 31L + char.code.toLong()) and UINT_MASK }
        for (index in result.lastIndex downTo 1) {
            state = (state * 1_664_525L + 1_013_904_223L) and UINT_MASK
            val random = state.toDouble() / UINT_RANGE
            val swap = (random * (index + 1)).toInt().coerceIn(0, index)
            val old = result[index]
            result[index] = result[swap]
            result[swap] = old
        }
        return result
    }

    private const val UINT_MASK = 0xffff_ffffL
    private const val UINT_RANGE = 4_294_967_296.0
}

private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
private fun JsonElement?.arrayOrEmpty(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
private fun JsonElement?.arrayStrings(): List<String> =
    (this as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
private fun JsonElement?.arrayNullableStrings(): List<String?> =
    (this as? JsonArray)?.map { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }.orEmpty()
