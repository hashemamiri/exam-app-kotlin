package ir.exam.app.core.cache

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File

/** یک آزمون در عکس فوریِ کارنامه (فقط عنوان/درس؛ بدون سؤال و پاسخ). */
data class SnapshotExam(val id: String, val title: String, val subject: String = "")

/** یک کلاس در عکس فوریِ کارنامه. */
data class SnapshotClass(val id: String, val name: String)

/** یک سطر کارنامه: نام دانش‌آموز + نمرهٔ خام و سقف نمرهٔ هر آزمون. */
data class SnapshotRow(
    val studentId: String,
    val name: String,
    val scores: Map<String, Double?>,
    val maxScores: Map<String, Double> = emptyMap()
)

/** محتوای عکس فوریِ صفحهٔ کارنامه/آمار. */
data class ReportsSnapshotPayload(
    val savedAtMillis: Long,
    val exams: List<SnapshotExam>,
    val classes: List<SnapshotClass>,
    val rows: List<SnapshotRow>
) {
    val isEmpty: Boolean get() = exams.isEmpty() && rows.isEmpty()
}

/**
 * V321 — «آفلاین»: صفحهٔ کارنامه/آمار تا پیش از این فقط در حافظهٔ رم نگه داشته می‌شد و با
 * بستن برنامه یا بی‌اینترنتی، خالی باز می‌شد. حالا آخرین وضعیت (آزمون‌ها، کلاس‌ها و سطرهای
 * نمره) روی دیسکِ خصوصی برنامه ذخیره می‌شود و در نبود شبکه همان نشان داده می‌شود.
 *
 * فقط عنوان/درس/نام/درصد ذخیره می‌شود؛ هیچ سؤال، پاسخ یا توکنی روی دیسک نمی‌رود و با خروج
 * از حساب (کلید کاربر عوض شود) فایل قدیمی خوانده نمی‌شود.
 *
 * این فایل بدون وابستگی اندروید نوشته شده تا با تست واحد بررسی شود.
 */
object ReportsSnapshot {
    private const val FILE_PREFIX = "reports-"
    private const val VERSION = 1
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    @Volatile private var dir: File? = null

    /** مسیر پوشهٔ ذخیره (مثلاً `filesDir/list_cache`)؛ از `ExamApplication` صدا زده می‌شود. */
    fun attachDir(directory: File?) {
        dir = directory
        runCatching { dir?.mkdirs() }
    }

    internal fun fileFor(userKey: String): File? {
        val base = dir ?: return null
        val safe = userKey.filter { it.isLetterOrDigit() || it == '-' }.ifBlank { "guest" }
        return File(base, FILE_PREFIX + safe + ".json")
    }

    fun read(userKey: String): ReportsSnapshotPayload? {
        val file = fileFor(userKey) ?: return null
        if (!file.exists()) return null
        return runCatching { decode(file.readText(Charsets.UTF_8)) }.getOrNull()?.takeIf { !it.isEmpty }
    }

    fun write(userKey: String, payload: ReportsSnapshotPayload) {
        val file = fileFor(userKey) ?: return
        runCatching { file.writeText(encode(payload), Charsets.UTF_8) }
    }

    fun clear(userKey: String) {
        runCatching { fileFor(userKey)?.delete() }
    }

    fun encode(payload: ReportsSnapshotPayload): String = buildJsonObject {
        put("v", VERSION)
        put("savedAt", payload.savedAtMillis)
        put("exams", buildJsonArray {
            payload.exams.forEach { exam ->
                add(buildJsonObject {
                    put("id", exam.id)
                    put("title", exam.title)
                    put("subject", exam.subject)
                })
            }
        })
        put("classes", buildJsonArray {
            payload.classes.forEach { item ->
                add(buildJsonObject { put("id", item.id); put("name", item.name) })
            }
        })
        put("rows", buildJsonArray {
            payload.rows.forEach { row ->
                add(buildJsonObject {
                    put("id", row.studentId)
                    put("name", row.name)
                    put("scores", buildJsonObject {
                        row.scores.forEach { (examId, value) ->
                            if (value == null) put(examId, JsonPrimitive(null as String?))
                            else put(examId, JsonPrimitive(value))
                        }
                    })
                    put("max", buildJsonObject {
                        row.maxScores.forEach { (examId, value) -> put(examId, JsonPrimitive(value)) }
                    })
                })
            }
        })
    }.toString()

    fun decode(text: String): ReportsSnapshotPayload? = runCatching {
        val root = json.parseToJsonElement(text).jsonObject
        ReportsSnapshotPayload(
            savedAtMillis = root["savedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            exams = array(root["exams"]).mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SnapshotExam(
                    id = id,
                    title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    subject = obj["subject"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            },
            classes = array(root["classes"]).mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SnapshotClass(id, obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty())
            },
            rows = array(root["rows"]).mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val scores = (obj["scores"] as? JsonObject).orEmpty().mapValues { (_, value) ->
                    value.jsonPrimitive.doubleOrNull
                }
                val maxScores = (obj["max"] as? JsonObject).orEmpty().mapNotNull { (key, value) ->
                    value.jsonPrimitive.doubleOrNull?.let { key to it }
                }.toMap()
                SnapshotRow(id, obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty(), scores, maxScores)
            }
        )
    }.getOrNull()

    private fun array(element: kotlinx.serialization.json.JsonElement?): JsonArray =
        (element as? JsonArray) ?: JsonArray(emptyList())
}
