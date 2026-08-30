package ir.exam.app.core.printing

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * V71.0 — ثبت بادوام و قابل‌اثبات PDF روی URI انتخابی Storage Access Framework.
 *
 * روش اصلی از descriptor با حالت rwt استفاده می‌کند: صفرکردن قطعی، کپی جریانی،
 * flush و fsync. چون رفتار modeها بین DocumentProviderها یکسان نیست، روش سازگار
 * wt نیز fallback است. پس از بسته‌شدن مقصد، URI چند بار دوباره خوانده می‌شود و
 * اندازه + SHA-256 باید دقیقاً با PDF سالم مرحله‌ای برابر باشد؛ در غیر این صورت
 * هرگز موفقیت گزارش نمی‌شود.
 */
internal class VerifiedSafPdfWriter(
    private val contentResolver: ContentResolver
) {
    suspend fun commit(source: File, target: Uri, expected: PdfArtifact) {
        val descriptorAttempt = attempt(target, expected) {
            writeWithDurableDescriptor(source, target, expected.byteCount)
        }
        if (descriptorAttempt.verified) return

        val streamAttempt = attempt(target, expected) {
            writeWithCompatibleStream(source, target, expected.byteCount)
        }
        if (streamAttempt.verified) return

        val latest = streamAttempt.observed ?: descriptorAttempt.observed
        val observedText = latest?.byteCount?.let { "$it بایت" } ?: "نامشخص"
        val cause = streamAttempt.error ?: descriptorAttempt.error
        throw IOException(
            "ذخیرهٔ PDF تأیید نشد؛ اندازهٔ مورد انتظار ${expected.byteCount} بایت " +
                "و اندازهٔ خوانده‌شده از مقصد $observedText بود. دوباره تلاش کنید.",
            cause
        )
    }

    private suspend fun attempt(
        target: Uri,
        expected: PdfArtifact,
        write: () -> Unit
    ): CommitAttempt = try {
        write()
        val readBack = awaitReadBack(target, expected)
        CommitAttempt(
            verified = expected.hasSameBytes(readBack.fingerprint),
            observed = readBack.fingerprint,
            error = readBack.error
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        CommitAttempt(verified = false, observed = null, error = error)
    }

    private fun writeWithDurableDescriptor(source: File, target: Uri, expectedBytes: Long) {
        val descriptor = contentResolver.openFileDescriptor(target, "rwt")
            ?: throw IOException("بازکردن مقصد PDF با حالت نوشتن پایدار ممکن نشد.")
        ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
            output.channel.position(0L)
            output.channel.truncate(0L)
            val copied = FileInputStream(source).buffered(COPY_BUFFER_BYTES).use { input ->
                input.copyTo(output, COPY_BUFFER_BYTES)
            }
            if (copied != expectedBytes) throw IOException("کپی PDF پیش از پایان متوقف شد.")
            output.flush()
            output.channel.force(true)
            output.fd.sync()
            if (output.channel.size() != expectedBytes) {
                throw IOException("اندازهٔ descriptor مقصد پس از fsync صحیح نیست.")
            }
        }
    }

    private fun writeWithCompatibleStream(source: File, target: Uri, expectedBytes: Long) {
        val stream = contentResolver.openOutputStream(target, "wt")
            ?: throw IOException("بازکردن مقصد PDF با حالت سازگار ممکن نشد.")
        stream.use { output ->
            val copied = FileInputStream(source).buffered(COPY_BUFFER_BYTES).use { input ->
                input.copyTo(output, COPY_BUFFER_BYTES)
            }
            if (copied != expectedBytes) throw IOException("کپی سازگار PDF پیش از پایان متوقف شد.")
            output.flush()
            // بعضی providerها FileOutputStream واقعی و بعضی pipe می‌دهند؛ sync در
            // fallback بهترین تلاش است و تطبیق SHA-256 پایین، معیار قطعی موفقیت است.
            if (output is FileOutputStream) runCatching { output.fd.sync() }
        }
    }

    private suspend fun awaitReadBack(target: Uri, expected: PdfArtifact): ReadBack {
        var latest: PdfFingerprint? = null
        var latestError: Exception? = null
        READ_BACK_DELAYS_MS.forEach { waitMs ->
            if (waitMs > 0L) delay(waitMs)
            try {
                val stream = contentResolver.openInputStream(target)
                    ?: throw IOException("خواندن مجدد فایل مقصد ممکن نشد.")
                latest = stream.buffered(COPY_BUFFER_BYTES).use(PdfArtifactVerifier::fingerprint)
                latestError = null
                if (expected.hasSameBytes(latest)) return ReadBack(latest, null)
            } catch (error: Exception) {
                latestError = error
            }
        }
        return ReadBack(latest, latestError)
    }

    private data class CommitAttempt(
        val verified: Boolean,
        val observed: PdfFingerprint?,
        val error: Exception?
    )

    private data class ReadBack(
        val fingerprint: PdfFingerprint?,
        val error: Exception?
    )

    private companion object {
        const val COPY_BUFFER_BYTES = 64 * 1_024
        val READ_BACK_DELAYS_MS = longArrayOf(0L, 80L, 240L, 600L, 1_200L)
    }
}
