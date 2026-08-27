package ir.exam.app.data.local

import android.content.Context
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * V63.5 — چیدمان چاپی هر آزمون، جدا از خود آزمون (درخواست کاربر):
 * ویرایش‌های «ویرایشگر سند» فقط برای چاپ‌اند، بنابراین به‌جای بازنویسی
 * آزمون در سرور، نسخهٔ ویرایش‌شدهٔ سؤال‌ها محلی و به‌ازای exam_id ذخیره
 * می‌شود و فقط مسیر چاپ آن را می‌خواند. آزمون دانش‌آموز دست‌نخورده می‌ماند.
 */
class PrintLayoutStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "print_layout_overrides",
        Context.MODE_PRIVATE
    )

    fun write(examId: String, questions: List<QuestionDraft>) {
        val encoded = ExamQuestionCodec.encode(questions)
        val payload = JsonObject(
            mapOf("q" to encoded.publicQuestions, "k" to encoded.answerKey)
        )
        preferences.edit().putString(KEY_PREFIX + examId, payload.toString()).apply()
    }

    fun read(examId: String): List<QuestionDraft>? {
        val raw = preferences.getString(KEY_PREFIX + examId, null) ?: return null
        return runCatching {
            val payload = Json.parseToJsonElement(raw).jsonObject
            ExamQuestionCodec.decode(payload.getValue("q"), payload["k"])
        }.getOrNull()
    }

    fun clear(examId: String) {
        preferences.edit().remove(KEY_PREFIX + examId).apply()
    }

    private companion object {
        const val KEY_PREFIX = "layout_"
    }
}
