package ir.exam.app.data.local

import android.content.Context
import ir.exam.app.ui.builder.QuestionDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * V86.8 — آزمون‌های چاپی که روی دستگاه ذخیره می‌شوند.
 *
 * آزمونِ چاپی مخاطب و زمان‌بندی ندارد و نباید به جدولِ آزمونِ سرور برود
 * (قانونِ پابرجا: «هیچ ذخیره‌ای به آزمون سرور برنگردد»). پس اینجا کنارِ
 * آزمون‌های سرور، اما جدا از آن‌ها نگهداری می‌شود.
 */
@Serializable
data class PrintExamRecord(
    val id: String,
    val title: String,
    val subject: String = "",
    val questions: List<QuestionDraft> = emptyList(),
    val savedAt: Long = 0L
)

@Serializable
private data class PrintExamBox(val items: List<PrintExamRecord> = emptyList())

class PrintExamStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "print_local_exams",
        Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** تازه‌ترین ذخیره اول. */
    fun list(): List<PrintExamRecord> {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(PrintExamBox.serializer(), raw).items
        }.getOrDefault(emptyList()).sortedByDescending { it.savedAt }
    }

    fun get(id: String): PrintExamRecord? = list().firstOrNull { it.id == id }

    /** ذخیره یا به‌روزرسانی بر پایهٔ شناسه. */
    fun save(record: PrintExamRecord) {
        val next = list().filterNot { it.id == record.id } + record
        write(next)
    }

    fun delete(id: String) = write(list().filterNot { it.id == id })

    private fun write(items: List<PrintExamRecord>) {
        preferences.edit()
            .putString(KEY, json.encodeToString(PrintExamBox.serializer(), PrintExamBox(items)))
            .apply()
    }

    private companion object {
        const val KEY = "items"
    }
}
