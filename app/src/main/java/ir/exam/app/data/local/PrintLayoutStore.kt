package ir.exam.app.data.local

import android.content.Context
import ir.exam.app.data.repository.EncodedQuestions
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * V63.5/V64.6 — چیدمان چاپی هر آزمون، جدا از خود آزمون.
 *
 * «base» آخرین نسخهٔ آزمون است که هنگام ذخیرهٔ چیدمان چاپی دیده شده و «print»
 * نسخهٔ مخصوص چاپ است. هنگام ذخیرهٔ بعدی آزمون در بخش آزمون‌ها، فقط تفاوت‌های
 * print نسبت به base دوباره روی نسخهٔ جدید آزمون اعمال می‌شوند؛ بنابراین تغییر
 * معمولی آزمون به چاپ می‌رسد، اما تغییر چاپی وارد آزمون دانش‌آموز نمی‌شود.
 */
class PrintLayoutStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "print_layout_overrides",
        Context.MODE_PRIVATE
    )

    /** سازگاری با callers قدیمی: این نسخه هر دو snapshot را یکسان می‌نویسد. */
    fun write(examId: String, questions: List<QuestionDraft>) {
        write(examId, questions, questions)
    }

    /** ذخیرهٔ snapshot پایه و نسخهٔ مخصوص چاپ، بدون تماس با سرور. */
    fun write(
        examId: String,
        baseQuestions: List<QuestionDraft>,
        printQuestions: List<QuestionDraft>
    ) {
        val payload = JsonObject(
            mapOf(
                "version" to JsonPrimitive(STORAGE_VERSION),
                "base" to encodedPayload(ExamQuestionCodec.encode(baseQuestions)),
                "print" to encodedPayload(ExamQuestionCodec.encode(printQuestions))
            )
        )
        preferences.edit().putString(KEY_PREFIX + examId, payload.toString()).apply()
    }

    /** نسخهٔ مخصوص چاپ را برای مسیر چاپ برمی‌گرداند. */
    fun read(examId: String): List<QuestionDraft>? = readStored(examId)?.print

    /**
     * V64.6 — snapshot چاپ را با آخرین نسخهٔ آزمون همگام می‌کند.
     * این متد برای ورود به ویرایشگر هم استفاده می‌شود تا اگر تغییر سروری جدیدی
     * وجود داشت، پیش از نمایش چاپ روی دادهٔ تازه merge شود.
     */
    fun readForLatest(examId: String, latestQuestions: List<QuestionDraft>): List<QuestionDraft>? {
        if (readStored(examId) == null) return null
        rebase(examId, latestQuestions)
        return read(examId)
    }

    /**
     * پس از ذخیرهٔ موفق بخش «آزمون‌ها»، base را جلو می‌برد و تفاوت‌های چاپی را
     * حفظ می‌کند. در نتیجه تغییرات canonical آزمون در چاپ دیده می‌شوند.
     */
    fun rebase(examId: String, latestQuestions: List<QuestionDraft>) {
        val stored = readStored(examId) ?: return
        val printQuestions = if (stored.base == null) {
            // قالب قدیمی V63.5 snapshot پایه نداشت؛ یک بار به قالب جدید تبدیل
            // می‌شود. از این به بعد تغییرات بعدی آزمون درست merge خواهند شد.
            stored.print
        } else {
            PrintLayoutMerger.merge(stored.base, stored.print, latestQuestions)
        }
        write(examId, latestQuestions, printQuestions)
    }

    fun clear(examId: String) {
        preferences.edit().remove(KEY_PREFIX + examId).apply()
    }

    private fun encodedPayload(encoded: EncodedQuestions): JsonObject = JsonObject(
        mapOf("q" to encoded.publicQuestions, "k" to encoded.answerKey)
    )

    private fun readStored(examId: String): StoredLayout? {
        val raw = preferences.getString(KEY_PREFIX + examId, null) ?: return null
        return runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            val printElement = root["print"]
            if (printElement != null) {
                val print = decodePayload(printElement) ?: return@runCatching null
                StoredLayout(
                    base = decodePayload(root["base"]),
                    print = print
                )
            } else {
                // V63.5 payload قدیمی مستقیماً q/k داشت.
                decodePayload(root)?.let { StoredLayout(base = null, print = it) }
            }
        }.getOrNull()
    }

    private fun decodePayload(element: JsonElement?): List<QuestionDraft>? {
        val obj = element as? JsonObject ?: return null
        val questions = obj["q"] ?: return null
        return ExamQuestionCodec.decode(questions, obj["k"])
    }

    private data class StoredLayout(
        val base: List<QuestionDraft>?,
        val print: List<QuestionDraft>
    )

    private companion object {
        const val KEY_PREFIX = "layout_"
        const val STORAGE_VERSION = 2
    }
}

/**
 * Merge خالص و قابل‌آزمایش snapshotها.
 *
 * به‌جای جایگزینی کل سؤال، هر کلید JSON که در نسخهٔ چاپ نسبت به base تغییر
 * کرده روی آخرین نسخهٔ آزمون اعمال می‌شود. این کار تغییرات جدید آزمون در متن،
 * گزینه، تصویر، استایل، پاسخ و فیلدهای آیندهٔ raw را هم حفظ می‌کند.
 */
internal object PrintLayoutMerger {
    fun merge(
        baseQuestions: List<QuestionDraft>,
        printQuestions: List<QuestionDraft>,
        latestQuestions: List<QuestionDraft>
    ): List<QuestionDraft> {
        if (latestQuestions.isEmpty()) return emptyList()

        val baseEncoded = ExamQuestionCodec.encode(baseQuestions)
        val printEncoded = ExamQuestionCodec.encode(printQuestions)
        val latestEncoded = ExamQuestionCodec.encode(latestQuestions)

        val basePublic = publicRowsById(baseEncoded.publicQuestions)
        val printPublic = publicRowsById(printEncoded.publicQuestions)
        val latestPublic = publicRowsById(latestEncoded.publicQuestions)
        val baseAnswers = answerRowsById(baseEncoded, baseQuestions)
        val printAnswers = answerRowsById(printEncoded, printQuestions)
        val latestAnswers = answerRowsById(latestEncoded, latestQuestions)

        val mergedPublic = latestPublic.toMutableMap()
        latestQuestions.forEach { question ->
            val id = question.id
            val latestRow = latestPublic[id] ?: return@forEach
            val baseRow = basePublic[id]
            val printRow = printPublic[id]
            if (baseRow != null && printRow != null) {
                mergedPublic[id] = overlayChangedFields(
                    base = baseRow,
                    print = printRow,
                    latest = latestRow,
                    ignoredKeys = setOf("id")
                )
            }
        }

        val mergedAnswers = latestAnswers.toMutableMap()
        latestQuestions.forEach { question ->
            val id = question.id
            val latestRow = latestAnswers[id] ?: return@forEach
            val baseRow = baseAnswers[id]
            val printRow = printAnswers[id]
            if (baseRow != null && printRow != null) {
                mergedAnswers[id] = overlayChangedFields(
                    base = baseRow,
                    print = printRow,
                    latest = latestRow,
                    ignoredKeys = setOf("i")
                )
            }
        }

        val latestOrder = latestQuestions.map { it.id }
        val baseOrder = baseQuestions.map { it.id }
        val printOrder = printQuestions.map { it.id }
        val outputOrder = if (printOrder != baseOrder) {
            val retainedPrintOrder = printOrder
                .filter { id -> id in latestOrder }
                .distinct()
            retainedPrintOrder + latestOrder.filter { id -> id !in retainedPrintOrder }
        } else {
            latestOrder
        }

        val publicRows = outputOrder.mapNotNull(mergedPublic::get)
        val answerRows = outputOrder.mapIndexed { index, id ->
            val values = (mergedAnswers[id] ?: JsonObject(emptyMap())).toMutableMap()
            values["i"] = JsonPrimitive(index)
            JsonObject(values)
        }
        return ExamQuestionCodec.decode(JsonArray(publicRows), JsonArray(answerRows))
    }

    private fun publicRowsById(rows: JsonArray): Map<String, JsonObject> = rows.mapNotNull { element ->
        val row = element as? JsonObject ?: return@mapNotNull null
        val id = row["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        id to row
    }.toMap()

    private fun answerRowsById(
        encoded: EncodedQuestions,
        questions: List<QuestionDraft>
    ): Map<String, JsonObject> = questions.mapIndexedNotNull { index, question ->
        (encoded.answerKey.getOrNull(index) as? JsonObject)?.let { question.id to it }
    }.toMap()

    private fun overlayChangedFields(
        base: JsonObject,
        print: JsonObject,
        latest: JsonObject,
        ignoredKeys: Set<String>
    ): JsonObject {
        val values = latest.toMutableMap()
        (base.keys + print.keys + latest.keys).forEach { key ->
            if (key in ignoredKeys) return@forEach
            val merged = mergeJsonValue(base[key], print[key], latest[key])
            if (merged == null) values.remove(key) else values[key] = merged
        }
        return JsonObject(values)
    }

    /**
     * سه‌طرفهٔ بازگشتی: اگر فقط یکی از print/latest نسبت به base تغییر کرده
     * باشد همان تغییر برنده است؛ برای آرایه/آبجکت، اعضای مستقل نیز جدا merge
     * می‌شوند تا مثلاً تغییر گزینهٔ اول در چاپ، تغییر گزینهٔ دوم در آزمون را
     * پنهان نکند. تعارض واقعی به نفع ویرایش صریح چاپ باقی می‌ماند.
     */
    private fun mergeJsonValue(
        base: JsonElement?,
        print: JsonElement?,
        latest: JsonElement?
    ): JsonElement? {
        if (print == base) return latest
        if (latest == base) return print

        if (base is JsonObject && print is JsonObject && latest is JsonObject) {
            val values = latest.toMutableMap()
            (base.keys + print.keys + latest.keys).forEach { key ->
                val merged = mergeJsonValue(base[key], print[key], latest[key])
                if (merged == null) values.remove(key) else values[key] = merged
            }
            return JsonObject(values)
        }
        if (base is JsonArray && print is JsonArray && latest is JsonArray &&
            base.size == print.size && print.size == latest.size
        ) {
            return JsonArray(print.indices.map { index ->
                mergeJsonValue(base[index], print[index], latest[index]) ?: JsonPrimitive("")
            })
        }
        // اگر هر دو طرف همان مقدار را تغییر داده‌اند (یا طول آرایه عوض شده)،
        // تغییر صریح ویرایشگر چاپ حفظ می‌شود.
        return print
    }

}
