package ir.exam.app.data.repository

import ir.exam.app.core.network.RelayHttp
import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.domain.bank.ReferenceBankRow
import ir.exam.app.domain.bank.withFormulaTokens
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * V360 — خواندن فقط‌خواندنی بانک مرجع از همان ذخیرهٔ شیء.
 *
 * هیچ کلید سرویس، توکن یا رمزی در این کلاس نیست؛ فایل‌ها عمومی‌اند و
 * از نشانی BASE خوانده می‌شوند. شکست شبکه به لایهٔ رابط برگردانده می‌شود
 * تا پیام فارسی نشان دهد، نه خطای خام.
 *
 * V370 — سه تغییر کارایی:
 *  • کش دیسکی OkHttp (۵ دقیقه + ETag). نشانی شارد با ?v= نسخه‌دار است، پس
 *    دادهٔ کهنه پایدار نمی‌ماند؛ فقط دانلود تکراری حذف می‌شود.
 *  • شارد صفحه‌ای: با idx فقط تکه‌های همان صفحه خوانده می‌شوند.
 *  • پاییزگاه: هر خطای idx/صفحه ⇒ شارد کامل، مثل پیش.
 */
class ReferenceBankRepository(
    private val base: String = ReferenceBankContract.BASE,
    cacheDir: File? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** V370 — کشِ دیسکی فقط وقتی مسیر داده شد فعال است (در آزمون‌ها null است). */
    private val client: OkHttpClient = if (cacheDir == null) {
        RelayHttp.client
    } else {
        RelayHttp.client.newBuilder()
            .cache(Cache(File(cacheDir, CACHE_DIR).apply { mkdirs() }, CACHE_BYTES))
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=" + CACHE_MAX_AGE_S)
                    .build()
            }
            .build()
    }

    private val idxCache = ConcurrentHashMap<Int, ShardIndex>()
    private val idxTried: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())

    @Serializable
    private data class ShardIndex(val v: String = "", val ids: Map<String, Int> = emptyMap())

    private suspend fun fetch(path: String): String = withContext(Dispatchers.IO) {
        // V360.9 — کلید نسخه، تا پاسخ کش‌شدۀ لبه/مرورگر برنگردد.
        val url = base.trimEnd('/') + "/" + ReferenceBankContract.shardUrl(path)
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("دریافت فهرست بانک ناموفق بود (HTTP ${response.code})")
            response.body?.string().orEmpty()
        }
    }

    /** V370 — نسخهٔ «شاید نباشد»ی خواندن: idx و صفحه‌ها همیشه موجود نیستند. */
    private suspend fun fetchOrNull(path: String): String? = withContext(Dispatchers.IO) {
        val url = base.trimEnd('/') + "/" + ReferenceBankContract.shardUrl(path)
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else null
            }
        }.getOrNull()
    }

    /** پایه‌های ۱ تا ۹ از فهرست کامل و پایه‌های بالاتر از همهٔ تکه‌ها خوانده می‌شوند. */
    suspend fun catalog(grade: Int, track: String = ""): List<ReferenceBankRow> = withContext(Dispatchers.IO) {
        if (grade >= 10) shardedCatalog(grade, track)?.let { return@withContext it }
        val raw = fetch(ReferenceBankContract.catalogPath(grade))
        json.decodeFromString<List<ReferenceBankRow>>(raw).map { it.withFormulaTokens() }
    }

    @Serializable
    private data class CatalogShard(
        val grade: Int = 0, val track: String = "", val subject: String = "",
        val rows: List<ReferenceBankRow> = emptyList()
    )

    private fun decodeShard(raw: String): CatalogShard {
        runCatching { json.decodeFromString<CatalogShard>(raw) }.getOrNull()?.let { return it }
        return CatalogShard(rows = json.decodeFromString<List<ReferenceBankRow>>(raw))
    }

    /** نبود تکه (۴۰۴) طبیعی است؛ خطاهای دیگر نباید به دادهٔ ناقص تبدیل شوند. */
    private suspend fun catalogPartOrNull(path: String): String? = withContext(Dispatchers.IO) {
        val url = base.trimEnd('/') + "/" + ReferenceBankContract.shardUrl(path)
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) error("دریافت فهرست بانک ناموفق بود (HTTP ${response.code})")
            response.body?.string() ?: error("پاسخ فهرست بانک خالی است")
        }
    }

    private suspend fun shardedCatalog(grade: Int, track: String): List<ReferenceBankRow>? = coroutineScope {
        // چهار تکه هم‌زمان، تا ۳۲ دانلود و تجزیهٔ هم‌زمان حافظهٔ گوشی را اشغال نکند.
        val shards = mutableListOf<CatalogShard>()
        for (batch in (1..CATALOG_MAX_PARTS).toList().chunked(4)) {
            shards += batch.map { part -> async {
                catalogPartOrNull(ReferenceBankContract.catalogShardPath(grade, part))?.let { decodeShard(it) }
            } }.awaitAll().filterNotNull()
        }
        if (shards.isEmpty()) return@coroutineScope null
        shards.flatMap { shard -> shard.rows.map { row ->
            val withTrack = if (row.track.isBlank() && shard.track.isNotBlank()) row.copy(track = shard.track) else row
            withTrack.withFormulaTokens()
        } }
    }

    /** V370 — فهرست شناسه → صفحه، یک‌بار در هر نشست؛ نبودنش یعنی شارد صفحه‌ای نیست. */
    private suspend fun questionIndex(grade: Int): ShardIndex? {
        idxCache[grade]?.let { return it }
        if (grade in idxTried) return null
        idxTried.add(grade)
        val raw = fetchOrNull(ReferenceBankContract.questionIndexPath(grade)) ?: return null
        val index = runCatching { json.decodeFromString<ShardIndex>(raw) }.getOrNull() ?: return null
        if (index.v != ReferenceBankContract.PAGED_VERSION || index.ids.isEmpty()) return null
        idxCache[grade] = index
        return index
    }

    /** V370 — فقط تکه‌هایی خوانده می‌شوند که شناسه‌ای از [ids] در آن‌ها هست. */
    private suspend fun questionsFromPages(grade: Int, index: ShardIndex, ids: Collection<Long>): Map<String, ReferenceBankQuestion>? {
        val byPage = mutableMapOf<Int, MutableList<String>>()
        ids.forEach { id ->
            val key = id.toString()
            val page = index.ids[key] ?: return@forEach
            byPage.getOrPut(page) { mutableListOf() }.add(key)
        }
        if (byPage.isEmpty()) return emptyMap()
        val out = mutableMapOf<String, ReferenceBankQuestion>()
        for ((page, wanted) in byPage) {
            val raw = fetchOrNull(ReferenceBankContract.questionPagePath(grade, page)) ?: return null
            val part = runCatching {
                json.decodeFromString<Map<String, ReferenceBankQuestion>>(raw)
            }.getOrNull() ?: return null
            wanted.forEach { id -> part[id]?.let { out[id] = it.withFormulaTokens() } }
        }
        return out
    }

    /**
     * V370 — [ids] تهی/null یعنی شارد کامل. هر شکستی در مسیر صفحه‌ای، به شارد
     * کامل برمی‌گردد تا نبودِ یک تکهٔ بارگذاری‌نشده سؤال‌ها را ناپدید نکند.
     */
    suspend fun questions(grade: Int, ids: Collection<Long>? = null): Map<String, ReferenceBankQuestion> =
        withContext(Dispatchers.IO) {
            if (ids != null) {
                val index = questionIndex(grade)
                if (index != null) {
                    questionsFromPages(grade, index, ids)?.let { return@withContext it }
                }
            }
            val raw = fetch(ReferenceBankContract.questionsPath(grade))
            json.decodeFromString<Map<String, ReferenceBankQuestion>>(raw)
                .mapValues { (_, question) -> question.withFormulaTokens() }
        }

    fun imageUrl(file: String): String = ReferenceBankContract.imageUrl(base, file)

    private companion object {
        /** V370 — کش دیسکی شاردها: ۹۶ مگابایت و پنج دقیقه؛ نسخه‌دار با ?v=. */
        const val CACHE_DIR = "refbank_http"
        const val CACHE_BYTES = 96L * 1024 * 1024
        const val CACHE_MAX_AGE_S = 300

        /** V377 — پایهٔ دهم ۲۲ تکه دارد؛ سقف پیشین (۸) درس‌های آخر را جا می‌گذاشت. */
        const val CATALOG_MAX_PARTS = 32
    }
}
