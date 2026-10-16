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
    /** V310.9 — دروس هر پایهٔ ابتدایی (فهرست رسمی). */
    val GRADE_SUBJECTS: Map<Int, List<String>> = run {
        val p1 = listOf("آموزش قرآن", "فارسی", "نگارش فارسی", "ریاضی", "علوم تجربی")
        val p2 = listOf("آموزش قرآن", "هدیه‌های آسمان", "فارسی", "نگارش فارسی", "ریاضی", "علوم تجربی")
        val p3 = p2 + "مطالعات اجتماعی"
        mapOf(1 to p1, 2 to p2, 3 to p3, 4 to p3, 5 to p3, 6 to p3 + listOf("تفکر و پژوهش", "کار و فناوری"))
    }

    /** همهٔ دروس (برای «همه پایه‌ها» / «بدون پایه»)؛ بدون تکرار. */
    val SUBJECTS: List<String> = GRADE_SUBJECTS.values.flatten().distinct()

    /** دروس مجاز برای یک پایه؛ پایهٔ نامعتبر (۰) → همهٔ دروس. */
    fun subjectsFor(grade: Int): List<String> = if (grade in 1..6) GRADE_SUBJECTS[grade].orEmpty() else SUBJECTS

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
