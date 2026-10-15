package ir.exam.app.ui.builder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import java.util.UUID

/** V306 / ۱۲ — یک سؤال از بانک آمادهٔ ابتدایی (فقط خواندنی؛ کنار برنامه در assets). */
data class PrimaryBankItem(
    val id: String,
    val grade: Int,
    val subject: String,
    val type: String,
    val text: String,
    val options: List<String>,
    val correct: Int?,
    val answer: String
)

/** تجزیه و تبدیل بانک ابتدایی به پیش‌نویس سؤال (بدون وابستگی به Android). */
object PrimaryBank {
    const val ASSET = "primary_bank.json"

    /** V308 / ۱۴ — درس‌های ثابت حالت ابتدایی (برچسب سؤال و فیلتر بانک). */
    val SUBJECTS = listOf("ریاضی", "فارسی", "علوم", "هدیه‌های آسمان", "مطالعات اجتماعی")

    fun parse(raw: String): List<PrimaryBankItem> {
        val root = Json.parseToJsonElement(raw).jsonObject
        val list = root["items"] as? JsonArray ?: return emptyList()
        return list.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val id = o.str("id") ?: return@mapNotNull null
            val type = o.str("type") ?: return@mapNotNull null
            val text = o.str("text").orEmpty()
            if (text.isBlank()) return@mapNotNull null
            PrimaryBankItem(
                id = id,
                grade = (o["grade"] as? JsonPrimitive)?.intOrNull ?: 0,
                subject = o.str("subject").orEmpty(),
                type = type,
                text = text,
                options = (o["options"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
                correct = (o["correct"] as? JsonPrimitive)?.intOrNull,
                answer = o.str("answer").orEmpty()
            )
        }
    }

    /** نوع ناشناخته یا گزینه‌ای با پاسخ نامعتبر → null (سؤال افزوده نمی‌شود). */
    /** V308 / ۱۴ — پیش‌نویس با برچسب پایه و درس همان سؤال بانک. */
    fun toDraft(item: PrimaryBankItem): QuestionDraft? = buildDraft(item)?.copy(grade = item.grade, subject = item.subject)

    private fun buildDraft(item: PrimaryBankItem): QuestionDraft? = when (item.type) {
        "multiple" -> {
            val c = item.correct
            if (item.options.size < 2 || c == null || c !in item.options.indices) null
            else QuestionDraft(
                type = QuestionType.MULTIPLE_CHOICE,
                text = item.text,
                options = item.options,
                optionIds = item.options.map { UUID.randomUUID().toString() },
                optionImages = item.options.map { null },
                correctIndex = c
            )
        }
        "fill" -> if (item.answer.isBlank()) null else QuestionDraft(type = QuestionType.FILL_BLANK, text = item.text, expectedText = item.answer)
        "numeric" -> if (item.answer.isBlank()) null else QuestionDraft(type = QuestionType.NUMERIC, text = item.text, expectedNumber = item.answer)
        "truefalse" -> if (item.answer !in setOf("true", "false")) null else QuestionDraft(type = QuestionType.TRUE_FALSE, text = item.text, expectedText = item.answer)
        else -> null
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
