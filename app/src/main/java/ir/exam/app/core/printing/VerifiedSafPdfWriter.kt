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
 * روش اصلی از ContentResolver.openOutputStream استفاده می‌کند؛ این مسیر برای
 * DocumentProviderهای محلی و ابری سازگارتر است و پس از close کپی کامل را به
 * provider تحویل می‌دهد. descriptor با حالت rwt فقط fallback دستگاهی است، چون
 * بعضی providerهای ابری برای آن pipe غیرقابل seek برمی‌گردانند. پس از بسته‌شدن
 * مقصد، URI چند بار دوباره خوانده می‌شود و اندازه + SHA-256 باید دقیقاً با PDF
 * سالم مرحله‌ای برابر باشد؛ در غیر این صورت هرگز موفقیت گزارش نمی‌شود.
 */
internal class VerifiedSafPdfWriter(
    private val contentResolver: ContentResolver
) {
    suspend fun commit(source: File, target: Uri, expected: PdfArtifact) {
        // برای Google Drive/OneDrive و سایر providerهای ابری، openOutputStream
        // مسیر رسمی و قابل‌اعتماد است. بعضی providerها برای ParcelFileDescriptor
        // یک pipe برمی‌گردانند و استفاده از channel.position/truncate روی آن
        // فایل صفر یا ناقص ایجاد می‌کند؛ descriptor فقط fallback دستگاهی است.
        val streamAttempt = attempt(target, expected) {
            writeWithCompatibleStream(source, target, expected.byteCount)
        }
        if (streamAttempt.verified) return

        val descriptorAttempt = attempt(target, expected) {
            writeWithDurableDescriptor(source, target, expected.byteCount)
        }
        if (descriptorAttempt.verified) return

        val latest = descriptorAttempt.observed ?: streamAttempt.observed
        val observedText = latest?.byteCount?.let { "$it بایت" } ?: "نامشخص"
        val cause = descriptorAttempt.error ?: streamAttempt.error
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
        // بدون ساختن FileOutputStream روی descriptor بنویسیم؛ این مسیر برای
        // local و cloud DocumentsProviderها سازگارتر است و close آن commit را
        // به provider اعلام می‌کند. حالت w برای ACTION_CREATE_DOCUMENT مقصد
        // را از ابتدا برای نوشتن/جایگزینی باز می‌کند.
        val stream = contentResolver.openOutputStream(target)
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
        // بعضی providerهای ابری بعد از close چند لحظه برای read-back زمان می‌خواهند.
        val READ_BACK_DELAYS_MS = longArrayOf(0L, 150L, 400L, 900L, 1_800L, 3_000L)
    }
}
