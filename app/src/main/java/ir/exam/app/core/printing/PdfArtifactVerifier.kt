package ir.exam.app.core.printing

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.security.MessageDigest

/** مشخصات یک PDF نهایی که با iText 7 خوانده شده و اثرانگشت دارد. */
internal data class PdfArtifact(
    val byteCount: Long,
    val pageCount: Int,
    val sha256: String
) {
    fun hasSameBytes(other: PdfFingerprint?): Boolean =
        other != null && byteCount == other.byteCount && sha256 == other.sha256
}

/** اثرانگشت جریانی؛ بدون بارگذاری کل PDF در RAM. */
internal data class PdfFingerprint(
    val byteCount: Long,
    val sha256: String
)

/**
 * V72.0 — اعتبارسنج مستقل فایل PDF با iText 7.
 *
 * موفقیت صرفاً «بدون exception تمام‌شدن write» نیست: فایل مرحله‌ای باید envelope
 * استاندارد PDF داشته باشد، PdfReader/PdfDocument آن را بدون بازسازی بخوانند،
 * حداقل یک صفحه داشته باشد و اثرانگشت SHA-256 آن مشخص باشد. همین اثرانگشت
 * بعداً از URI مقصد دوباره محاسبه می‌شود تا فایل صفر/ناقص هرگز موفقیت گزارش نشود.
 */
internal object PdfArtifactVerifier {
    private val pdfHeader = "%PDF-".toByteArray(Charsets.US_ASCII)
    private const val EOF_MARKER = "%%EOF"
    private const val TAIL_SCAN_BYTES = 1_024
    private const val HASH_BUFFER_BYTES = 64 * 1_024

    fun inspect(file: File): PdfArtifact {
        if (!file.isFile || file.length() < pdfHeader.size + EOF_MARKER.length) {
            throw IOException("فایل PDF مرحله‌ای خالی یا ناقص است.")
        }
        requirePdfEnvelope(file)

        val reader = try {
            PdfReader(file)
        } catch (error: Exception) {
            throw IOException("iText 7 نتوانست فایل ساخته‌شده را باز کند.", error)
        }
        val pdf = try {
            PdfDocument(reader)
        } catch (error: Exception) {
            runCatching { reader.close() }
            throw IOException("iText 7 نتوانست ساختار PDF ساخته‌شده را بخواند.", error)
        }
        val pages = try {
            if (reader.hasRebuiltXref()) {
                throw IOException("ساختار PDF ناقص بود و iText 7 آن را بازسازی کرد.")
            }
            pdf.numberOfPages
        } finally {
            pdf.close()
        }
        if (pages < 1) throw IOException("PDF ساخته‌شده هیچ صفحه‌ای ندارد.")

        val fingerprint = FileInputStream(file).buffered(HASH_BUFFER_BYTES).use(::fingerprint)
        if (fingerprint.byteCount != file.length()) {
            throw IOException("اندازهٔ PDF هنگام اعتبارسنجی تغییر کرد.")
        }
        return PdfArtifact(
            byteCount = fingerprint.byteCount,
            pageCount = pages,
            sha256 = fingerprint.sha256
        )
    }

    fun fingerprint(input: InputStream): PdfFingerprint {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(HASH_BUFFER_BYTES)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            digest.update(buffer, 0, count)
            total += count
        }
        return PdfFingerprint(total, digest.digest().toHex())
    }

    private fun requirePdfEnvelope(file: File) {
        RandomAccessFile(file, "r").use { random ->
            val header = ByteArray(pdfHeader.size)
            random.readFully(header)
            if (!header.contentEquals(pdfHeader)) {
                throw IOException("هدر استاندارد PDF در فایل ساخته‌شده وجود ندارد.")
            }

            val tailSize = minOf(random.length(), TAIL_SCAN_BYTES.toLong()).toInt()
            val tail = ByteArray(tailSize)
            random.seek(random.length() - tailSize)
            random.readFully(tail)
            if (EOF_MARKER !in tail.toString(Charsets.ISO_8859_1)) {
                throw IOException("پایان استاندارد PDF در فایل ساخته‌شده وجود ندارد.")
            }
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }
}
