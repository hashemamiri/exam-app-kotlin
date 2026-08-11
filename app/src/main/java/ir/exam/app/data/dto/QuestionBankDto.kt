package ir.exam.app.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class QuestionBankDto(
    val id: Long,
    val subject: String? = null,
    val question: JsonObject,
    val created_at: String? = null
)
