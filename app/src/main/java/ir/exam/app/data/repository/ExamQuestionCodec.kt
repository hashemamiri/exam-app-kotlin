package ir.exam.app.data.repository

import ir.exam.app.ui.builder.MediaDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class EncodedQuestions(
    val publicQuestions: JsonArray,
    val answerKey: JsonArray
)

internal object ExamQuestionCodec {
    fun decode(publicQuestions: JsonElement, answerKey: JsonElement?): List<QuestionDraft> {
        val keys = answerKey.asArrayOrEmpty().mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            (obj["i"]?.asInt() ?: index) to obj
        }.toMap()

        return publicQuestions.asArrayOrEmpty().mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val key = keys[index] ?: JsonObject(emptyMap())
            val type = parseType(obj["type"]?.asString())
            val imageUrls = obj["images"].asArrayOrEmpty().mapNotNull { it.asString() }.toMutableList()
            obj["image"]?.asString()?.takeIf(String::isNotBlank)?.let { if (it !in imageUrls) imageUrls.add(0, it) }
            val positions = obj["imgFreePositions"].asArrayOrEmpty()

            QuestionDraft(
                id = obj["id"]?.asString() ?: UUID.randomUUID().toString(),
                type = type,
                text = obj["text"]?.asString().orEmpty(),
                score = obj["score"]?.asDouble() ?: 1.0,
                options = obj["options"].asArrayOrEmpty().map { it.asString().orEmpty() },
                optionIds = List(obj["options"].asArrayOrEmpty().size) { UUID.randomUUID().toString() },
                optionImages = obj["optionImages"].asArrayOrEmpty().map { it.asString()?.takeIf(String::isNotBlank) },
                correctIndex = key["correctOption"]?.asInt() ?: obj["correctIndex"]?.asInt(),
                expectedText = when (type) {
                    QuestionType.TRUE_FALSE -> (key["correctAnswer"]?.asBoolean() ?: false).toString()
                    QuestionType.FILL_BLANK -> key["accept"].asArrayOrEmpty().mapNotNull(JsonElement::asString).joinToString("|")
                    else -> key["correctAnswer"]?.asString() ?: obj["expectedText"]?.asString().orEmpty()
                },
                expectedNumber = key["answer"]?.asString() ?: obj["expectedNumber"]?.asString().orEmpty(),
                tolerance = key["tolerance"]?.asString() ?: obj["tolerance"]?.asString() ?: "0",
                caseSensitive = key["caseSensitive"]?.asBoolean() ?: obj["caseSensitive"]?.asBoolean() ?: false,
                matchingLeft = obj["leftItems"].asArrayOrEmpty().map { it.asString().orEmpty() },
                matchingLeftIds = List(obj["leftItems"].asArrayOrEmpty().size) { UUID.randomUUID().toString() },
                matchingRight = obj["rightItems"].asArrayOrEmpty().map { it.asString().orEmpty() },
                matchingRightIds = List(obj["rightItems"].asArrayOrEmpty().size) { UUID.randomUUID().toString() },
                matchingPairs = (key["matchAnswer"] as? JsonObject)?.mapNotNull { (left, right) ->
                    val leftIndex = left.toIntOrNull()
                    val rightIndex = right.asInt()
                    if (leftIndex != null && rightIndex != null) leftIndex to rightIndex else null
                }?.toMap().orEmpty(),
                matchingLeftImages = obj["leftImages"].asArrayOrEmpty().map { it.asString()?.takeIf(String::isNotBlank) },
                matchingRightImages = obj["rightImages"].asArrayOrEmpty().map { it.asString()?.takeIf(String::isNotBlank) },
                answerImageMode = obj["allowImages"]?.asString() ?: "no",
                allowAnswerGraph = obj["allowAnswerGraph"]?.asBoolean() ?: false,
                maxAnswerImages = obj["maxImages"]?.asInt() ?: 0,
                images = imageUrls.mapIndexed { imageIndex, url ->
                    val pos = positions.getOrNull(imageIndex) as? JsonObject
                    MediaDraft(
                        uri = url,
                        xMm = pos?.get("x")?.asFloat() ?: obj["imgX"]?.asFloat() ?: 20f,
                        yMm = pos?.get("y")?.asFloat() ?: obj["imgY"]?.asFloat() ?: 30f,
                        widthMm = pos?.get("w")?.asFloat() ?: obj["imgW"]?.asFloat() ?: 55f
                    )
                },
                textAlign = obj["align"]?.asString()?.takeIf { it in setOf("right", "center", "left", "justify") } ?: "right",
                imagePosition = obj["imgPos"]?.asString()?.takeIf { it in setOf("above", "below", "right", "left", "free") } ?: "below",
                fontFamily = obj["font"]?.asString().orEmpty().ifBlank { "default" },
                fontSizeSp = (obj["fontSize"]?.asFloat() ?: 16f).coerceIn(8f, 40f),
                bold = obj["bold"]?.asBoolean() ?: false,
                italic = obj["italic"]?.asBoolean() ?: false,
                answerLines = (obj["answerLines"]?.asInt() ?: if (type == QuestionType.ESSAY) 5 else 2).coerceIn(0, 12),
                answerLineStyle = obj["answerLineStyle"]?.asString()?.takeIf { it in setOf("lined", "blank") } ?: "lined",
                rawPublic = obj,
                rawAnswer = key
            )
        }
    }

    fun encode(questions: List<QuestionDraft>): EncodedQuestions {
        val publicRows = questions.map { question ->
            val values = question.rawPublic.toMutableMap()
            ANSWER_FIELDS.forEach(values::remove)
            values["id"] = JsonPrimitive(question.id)
            values["type"] = JsonPrimitive(typeValue(question.type))
            values["text"] = JsonPrimitive(question.text.trim())
            values["score"] = JsonPrimitive(question.score)
            values["images"] = JsonArray(question.images.map { JsonPrimitive(it.uri) })
            values["image"] = question.images.firstOrNull()?.let { JsonPrimitive(it.uri) } ?: JsonNull
            values["imgFreePositions"] = JsonArray(question.images.map { image ->
                JsonObject(mapOf(
                    "x" to JsonPrimitive(image.xMm),
                    "y" to JsonPrimitive(image.yMm),
                    "w" to JsonPrimitive(image.widthMm)
                ))
            })
            values["allowImages"] = JsonPrimitive(question.answerImageMode)
            values["allowAnswerGraph"] = JsonPrimitive(question.allowAnswerGraph)
            values["maxImages"] = JsonPrimitive(if (question.answerImageMode == "no") 0 else question.maxAnswerImages.coerceIn(1, 10))
            values["align"] = JsonPrimitive(question.textAlign)
            values["imgPos"] = JsonPrimitive(question.imagePosition)
            values["font"] = JsonPrimitive(question.fontFamily)
            values["fontSize"] = JsonPrimitive(question.fontSizeSp.coerceIn(8f, 40f))
            values["bold"] = JsonPrimitive(question.bold)
            values["italic"] = JsonPrimitive(question.italic)
            values["answerLines"] = JsonPrimitive(question.answerLines.coerceIn(0, 12))
            values["answerLineStyle"] = JsonPrimitive(question.answerLineStyle)
            if (question.type == QuestionType.MULTIPLE_CHOICE) {
                values["options"] = JsonArray(question.options.map(::JsonPrimitive))
                values["optionImages"] = JsonArray(question.options.indices.map { index ->
                    question.optionImages.getOrNull(index)?.let(::JsonPrimitive) ?: JsonPrimitive("")
                })
            }
            if (question.type == QuestionType.MATCHING) {
                values["leftItems"] = JsonArray(question.matchingLeft.map(::JsonPrimitive))
                values["rightItems"] = JsonArray(question.matchingRight.map(::JsonPrimitive))
                values["leftImages"] = JsonArray(question.matchingLeft.indices.map { index ->
                    question.matchingLeftImages.getOrNull(index)?.let(::JsonPrimitive) ?: JsonPrimitive("")
                })
                values["rightImages"] = JsonArray(question.matchingRight.indices.map { index ->
                    question.matchingRightImages.getOrNull(index)?.let(::JsonPrimitive) ?: JsonPrimitive("")
                })
            }
            JsonObject(values)
        }

        val answerRows = questions.mapIndexed { index, question ->
            val values = question.rawAnswer.toMutableMap()
            values["i"] = JsonPrimitive(index)
            when (question.type) {
                QuestionType.MULTIPLE_CHOICE -> values["correctOption"] = JsonPrimitive(question.correctIndex ?: 0)
                QuestionType.TRUE_FALSE -> values["correctAnswer"] = JsonPrimitive(question.expectedText.toBooleanStrictOrNull() ?: false)
                QuestionType.FILL_BLANK -> {
                    values["accept"] = JsonArray(
                        question.expectedText.split('|').map(String::trim).filter(String::isNotBlank).map(::JsonPrimitive)
                    )
                    values["caseSensitive"] = JsonPrimitive(question.caseSensitive)
                }
                QuestionType.NUMERIC -> {
                    values["answer"] = question.expectedNumber.toDoubleOrNull()?.let(::JsonPrimitive) ?: JsonNull
                    values["tolerance"] = JsonPrimitive(question.tolerance.toDoubleOrNull() ?: 0.0)
                }
                QuestionType.MATCHING -> {
                    values["matchAnswer"] = JsonObject(question.matchingPairs.mapKeys { it.key.toString() }
                        .mapValues { JsonPrimitive(it.value) })
                }
                else -> Unit
            }
            JsonObject(values)
        }
        return EncodedQuestions(JsonArray(publicRows), JsonArray(answerRows))
    }

    private fun parseType(value: String?): QuestionType = when (value?.lowercase()) {
        "multiple", "multiple_choice", "multiplechoice" -> QuestionType.MULTIPLE_CHOICE
        "truefalse", "true_false" -> QuestionType.TRUE_FALSE
        "fill", "fill_blank" -> QuestionType.FILL_BLANK
        "numeric", "number" -> QuestionType.NUMERIC
        "matching", "match" -> QuestionType.MATCHING
        else -> QuestionType.ESSAY
    }

    private fun typeValue(type: QuestionType): String = when (type) {
        QuestionType.ESSAY -> "essay"
        QuestionType.MULTIPLE_CHOICE -> "multiple"
        QuestionType.TRUE_FALSE -> "truefalse"
        QuestionType.FILL_BLANK -> "fill"
        QuestionType.NUMERIC -> "numeric"
        QuestionType.MATCHING -> "matching"
    }

    private val ANSWER_FIELDS = setOf("correctOption", "correctAnswer", "accept", "answer", "tolerance", "caseSensitive", "matchAnswer", "pairs")
}

private fun JsonElement?.asArrayOrEmpty(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
private fun JsonElement?.asString(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.asInt(): Int? = (this as? JsonPrimitive)?.intOrNull
private fun JsonElement?.asDouble(): Double? = (this as? JsonPrimitive)?.doubleOrNull
private fun JsonElement?.asFloat(): Float? = (this as? JsonPrimitive)?.floatOrNull
private fun JsonElement?.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
