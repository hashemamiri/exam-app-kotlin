package ir.exam.app.core.io

import android.content.Context
import android.net.Uri
import android.widget.Toast
import ir.exam.app.core.network.UserFacingError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * V252 — خواندن/نوشتن امنِ فایل‌های انتخاب‌شده با SAF (ذخیرهٔ اکسل/پشتیبان/آزمون و وارد کردن فایل).
 *
 * ریشهٔ دو اشکال قبلی:
 * ۱) `contentResolver.openOutputStream(uri)?.use { write }` بدون try در ۵ صفحه بود؛ اگر فراهم‌کنندهٔ فایل
 *    (مثلاً درایو ابری آفلاین یا حافظهٔ پر) خطا می‌داد، برنامه با FileNotFoundException/IOException بسته می‌شد.
 * ۲) خواندن فایل‌های تا ۸–۲۰ مگابایت و نوشتن پشتیبان روی نخ اصلی انجام می‌شد (یخ‌زدن رابط، حتی ANR وقتی
 *    فایل از یک درایو ابری می‌آید). حالا همه روی Dispatchers.IO اجرا می‌شوند و نتیجه Result است.
 */
object DocumentIo {
    const val WRITE_FAILED = "ذخیرهٔ فایل ناموفق بود؛ محل دیگری را انتخاب کنید یا فضای حافظه را بررسی کنید."
    const val READ_FAILED = "فایل خوانده نشد؛ دوباره آن را انتخاب کنید."

    suspend fun writeBytes(context: Context, uri: Uri, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes); it.flush() }
                ?: error(WRITE_FAILED)
        }
    }

    suspend fun writeText(context: Context, uri: Uri, text: String): Result<Unit> =
        writeBytes(context, uri, text.toByteArray(Charsets.UTF_8))

    /** متن UTF-8 با سقف حجم؛ پیام سقف همان متن فارسی قبلی هر صفحه است. */
    suspend fun readText(context: Context, uri: Uri, maxBytes: Int, tooBigMessage: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { readLimited(it, maxBytes, tooBigMessage) }
                    ?: error(READ_FAILED)
            }
        }

    fun readLimited(input: InputStream, maxBytes: Int, tooBigMessage: String): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= maxBytes) { tooBigMessage }
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    /** پیام کوتاه فارسی برای صفحه‌هایی که ViewModel/وضعیت خطا ندارند. */
    fun toast(context: Context, error: Throwable, fallback: String = WRITE_FAILED) {
        Toast.makeText(context, UserFacingError.of(error, fallback), Toast.LENGTH_LONG).show()
    }
}
