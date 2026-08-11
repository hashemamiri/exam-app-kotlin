package ir.exam.app.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * URIهای Photo Picker را پیش از صف‌شدن داخل فضای خصوصی برنامه کپی می‌کند؛
 * در نتیجه reboot، بسته‌شدن process یا انقضای مجوز موقت باعث گم‌شدن تصویر نمی‌شود.
 */
class PendingMediaStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "pending_submission_media")

    suspend fun materialize(payload: PendingSubmissionPayload): PendingSubmissionPayload = withContext(Dispatchers.IO) {
        val operationDir = File(root, payload.operationId).apply { mkdirs() }
        try {
            val copied = payload.responseImages.mapValues { (questionId, uris) ->
                uris.mapIndexed { index, raw ->
                    if (raw.isRemoteUrl() || raw.startsWith("file://")) raw
                    else copyOne(Uri.parse(raw), operationDir, questionId, index)
                }
            }
            payload.copy(responseImages = copied)
        } catch (error: Throwable) {
            operationDir.deleteRecursively()
            throw error
        }
    }

    fun clear(operationId: String) {
        File(root, operationId).deleteRecursively()
    }

    private fun copyOne(uri: Uri, directory: File, questionId: String, index: Int): String {
        val safeQuestion = questionId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60)
        val target = File(directory, "$safeQuestion-$index-${UUID.randomUUID()}.bin")
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        check(total <= MAX_SOURCE_BYTES) { "حجم تصویر بیش از ۳۰ مگابایت است." }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: error("تصویر پاسخ قابل خواندن نیست.")
            check(target.length() in 1..MAX_SOURCE_BYTES) { "حجم تصویر صف‌شده نامعتبر است." }
            return target.toURI().toString()
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    private fun String.isRemoteUrl(): Boolean = startsWith("https://", true) || startsWith("http://", true)

    private companion object {
        const val MAX_SOURCE_BYTES = 30L * 1024L * 1024L
    }
}
