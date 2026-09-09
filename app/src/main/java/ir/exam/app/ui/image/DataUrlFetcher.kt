package ir.exam.app.ui.image

import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

/**
 * V118 — Coil 2.x هیچ Fetcher داخلی برای `data:image/...;base64,...` ندارد (در
 * Coil 3.1 اضافه شد). استودیوی تصویر خروجی‌اش را به همین شکل ذخیره می‌کند؛ پس
 * AsyncImage در آزمون‌ساز خطا می‌داد و کاشیِ سیاه نشان می‌داد و ExamHtmlImageInliner
 * هم تصویر را برای چاپ نمی‌توانست بخواند. این Fetcher رشتهٔ data-URL را دیکد و
 * به‌عنوان منبعِ بایت به رمزگشاهای عادی Coil می‌دهد.
 */
class DataUrlFetcher(private val data: String, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val payload = decodeBytes(data) ?: return null
        val mime = data.substringAfter("data:", "").substringBefore(';').substringBefore(',').ifBlank { null }
        return SourceResult(
            source = ImageSource(Buffer().write(payload), options.context),
            mimeType = mime,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (isDataUrl(data)) DataUrlFetcher(data, options) else null
    }

    /**
     * V135.4 — Coil 2 پیش از انتخاب Fetcher، هر String را با StringMapper به Uri تبدیل
     * می‌کند؛ بنابراین Factory<String> برای AsyncImage(model = "data:image/...") هرگز صدا
     * زده نمی‌شد و تصویرِ کاربر (گالری شکل‌ها، t='photo') در ویرایشگر و کادر متن خالی
     * می‌ماند. این Factory همان data-URL را در قالب Uri (scheme = data) می‌گیرد.
     */
    class UriFactory : Fetcher.Factory<android.net.Uri> {
        override fun create(data: android.net.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.scheme.equals("data", ignoreCase = true)) return null
            val raw = data.toString()
            return if (isDataUrl(raw)) DataUrlFetcher(raw, options) else null
        }
    }

    companion object {
        fun isDataUrl(value: String): Boolean = value.startsWith("data:image/", ignoreCase = true)

        fun decodeBytes(value: String): ByteArray? {
            if (!isDataUrl(value)) return null
            val comma = value.indexOf(',')
            if (comma < 0) return null
            val header = value.substring(0, comma)
            val body = value.substring(comma + 1)
            return runCatching {
                if (header.contains(";base64", ignoreCase = true)) {
                    Base64.decode(body, Base64.DEFAULT)
                } else {
                    java.net.URLDecoder.decode(body, "UTF-8").toByteArray(Charsets.ISO_8859_1)
                }
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        }
    }
}
