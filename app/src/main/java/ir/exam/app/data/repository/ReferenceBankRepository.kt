package ir.exam.app.data.repository

import ir.exam.app.core.network.RelayHttp
import ir.exam.app.domain.bank.ReferenceBankContract
import ir.exam.app.domain.bank.ReferenceBankQuestion
import ir.exam.app.domain.bank.ReferenceBankRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request

/**
 * V360 — خواندن فقطخواندنی بانک مرجع از همان ذخیرهٔ شیء.
 *
 * هیچ کلید سرویس، توکن یا رمزی در این کلاس نیست؛ فایلها عمومیاند و
 * از نشانی BASE خوانده میشوند. شکست شبکه به لایهٔ رابط برگردانده میشود
 * تا پیام فارسی نشان دهد، نه خطای خام.
 */
class ReferenceBankRepository(
    private val base: String = ReferenceBankContract.BASE
) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetch(path: String): String = withContext(Dispatchers.IO) {
        // V360.9 — کلید نسخه، تا پاسخ کششدۀ لبه/مرورگر برنگردد.
        val url = base.trimEnd('/') + "/" + ReferenceBankContract.shardUrl(path)
        val request = Request.Builder().url(url).get().build()
        RelayHttp.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("دریافت فهرست بانک ناموفق بود (HTTP ${response.code})")
            response.body?.string().orEmpty()
        }
    }

    suspend fun catalog(grade: Int): List<ReferenceBankRow> = withContext(Dispatchers.IO) {
        val raw = fetch(ReferenceBankContract.catalogPath(grade))
        json.decodeFromString<List<ReferenceBankRow>>(raw)
    }

    suspend fun questions(grade: Int): Map<String, ReferenceBankQuestion> = withContext(Dispatchers.IO) {
        val raw = fetch(ReferenceBankContract.questionsPath(grade))
        json.decodeFromString<Map<String, ReferenceBankQuestion>>(raw)
    }

    fun imageUrl(file: String): String = ReferenceBankContract.imageUrl(base, file)
}
