package ir.exam.app.data.repository

import ir.exam.app.domain.model.BooleanAnswer
import ir.exam.app.domain.model.ChoiceAnswer
import ir.exam.app.domain.model.MatchingAnswer
import ir.exam.app.domain.model.StudentAnswer
import ir.exam.app.domain.model.StudentDraft
import ir.exam.app.domain.model.TextAnswer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal object StudentDraftJsonCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(draft: StudentDraft): String {
        val answers = JsonObject(draft.answers.mapValues { (_, value) ->
            when (value) {
                is TextAnswer -> buildJsonObject { put("type", "text"); put("value", value.value) }
                is ChoiceAnswer -> buildJsonObject { put("type", "choice"); put("value", value.selectedIndex) }
                is BooleanAnswer -> buildJsonObject { put("type", "boolean"); put("value", value.value) }
                is MatchingAnswer -> buildJsonObject {
                    put("type", "matching")
                    put("value", JsonObject(value.pairs.mapKeys { it.key.toString() }.mapValues { JsonPrimitive(it.value) }))
                }
            }
        })
        val images = JsonObject(draft.responseImages.mapValues { (_, uris) -> JsonArray(uris.map(::JsonPrimitive)) })
        return JsonObject(mapOf("answers" to answers, "images" to images)).toString()
    }

    fun decode(raw: String): StudentDraft {
        val root = json.parseToJsonElement(raw).jsonObject
        val answersRoot = root["answers"] as? JsonObject ?: root
        val answers = answersRoot.mapNotNull { (id, element) ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val type = item["type"]?.jsonPrimitive?.contentOrNull
            val answer: StudentAnswer = when (type) {
                "text" -> TextAnswer(id, item["value"]?.jsonPrimitive?.contentOrNull.orEmpty())
                "choice" -> ChoiceAnswer(id, item["value"]?.jsonPrimitive?.intOrNull ?: 0)
                "matching" -> {
                    val pairsObject = item["value"] as? JsonObject ?: JsonObject(emptyMap())
                    MatchingAnswer(id, pairsObject.mapNotNull { (left, right) ->
                        val l = left.toIntOrNull()
                        val r = right.jsonPrimitive.intOrNull
                        if (l != null && r != null) l to r else null
                    }.toMap())
                }
                else -> BooleanAnswer(id, item["value"]?.jsonPrimitive?.booleanOrNull ?: false)
            }
            id to answer
        }.toMap()
        val imagesObject = root["images"] as? JsonObject ?: JsonObject(emptyMap())
        val images = imagesObject.mapValues { (_, element) ->
            (element as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }.orEmpty()
        }
        return StudentDraft(answers, images)
    }
}
