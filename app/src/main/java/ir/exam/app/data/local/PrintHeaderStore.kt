package ir.exam.app.data.local

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V86.7 — مقادیرِ «تنظیمات سربرگ» برای مسیرِ چاپ.
 *
 * این میدان‌ها (نامِ استاد، گروهِ آموزشی، واحد، تاریخ و…) مشخصهٔ خودِ آزمون
 * نیستند بلکه مشخصهٔ برگهٔ چاپی‌اند و در جدولِ آزمون جایی ندارند. طبقِ
 * انتخابِ کاربر روی دستگاه می‌مانند تا یک‌بار پر شوند و برای آزمون‌های
 * چاپیِ بعدی هم به کار بیایند — بدونِ هیچ تغییری در دیتابیس.
 *
 * قالبِ سربرگ و آرم اینجا تعیین نمی‌شوند؛ آن‌ها از
 * `assets/print/header_settings_schema.json` می‌آیند و دست‌نخورده‌اند.
 */
class PrintHeaderStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "print_header_fields",
        Context.MODE_PRIVATE
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** آخرین مقادیرِ اعمال‌شده؛ اگر چیزی ذخیره نشده باشد، نقشهٔ خالی. */
    fun read(): Map<String, String> {
        val raw = preferences.getString(KEY, null) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(JsonObject.serializer(), raw)
                .mapNotNull { (k, v) ->
                    val text = (v as? JsonPrimitive)?.contentOrNull ?: v.jsonPrimitive.contentOrNull
                    if (text == null) null else k to text
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    /** مقادیر را جایگزین می‌کند. نقشهٔ خالی یعنی پاک‌کردنِ ذخیره. */
    fun write(values: Map<String, String>) {
        if (values.isEmpty()) {
            preferences.edit().remove(KEY).apply()
            return
        }
        val obj = JsonObject(values.mapValues { JsonPrimitive(it.value) })
        preferences.edit()
            .putString(KEY, json.encodeToString(JsonObject.serializer(), obj))
            .apply()
    }

    fun clear() = preferences.edit().remove(KEY).apply()

    private companion object {
        const val KEY = "fields"
    }
}

/**
 * V86.9 — نگاشتِ میدان‌های فرمِ سربرگ به مدلِ چاپ. یک جا نوشته می‌شود تا
 * مسیرِ پیش‌نمایش و مسیرِ چاپ همان سربرگ را ببینند.
 */
fun printHeaderOf(fields: Map<String, String>): ir.exam.app.domain.model.OfficialPrintHeader =
    ir.exam.app.domain.model.OfficialPrintHeader(
        school = fields["f_branch"].orEmpty(),
        subject = fields["f_course"].orEmpty(),
        examDate = fields["f_examDate"].orEmpty(),
        examDuration = fields["f_duration"].orEmpty()
    )
