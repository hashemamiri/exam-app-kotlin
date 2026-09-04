package ir.exam.app.ui.printing

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * V76.9 — OCRِ بومیِ فارسی (جایگزینِ OCR استودیوی HTML).
 *
 * موتور: Tesseract 4 (tesseract4android) با دادهٔ زبانِ فارسیِ `fas.traineddata`
 * که داخل assets اپ است (۴۲۱KB، نسخهٔ fast). کاملاً **آفلاین** — برخلاف
 * استودیوی HTML که به آینه‌های آنلاین تکیه می‌کرد.
 *
 * Tesseract نمی‌تواند مستقیم از assets بخواند و به یک مسیرِ واقعیِ دیسک نیاز
 * دارد که زیرپوشه‌ای به نام `tessdata` داشته باشد؛ پس یک‌بار در `filesDir/ocr`
 * کپی می‌شود (`ensureTessData`) و دفعات بعد از همان استفاده می‌شود.
 */
object ExamImageOcr {

    /** نام زبان همان نامِ فایل بدون پسوند است. */
    const val LANG = "fas"

    /** حداکثر بُعدِ تصویرِ ورودیِ OCR — بزرگ‌تر از این فقط کُند می‌کند. */
    const val MAX_OCR_EDGE = 2200

    /** خروجیِ یک اجرای OCR. */
    data class OcrResult(val text: String, val confidence: Int)

    /**
     * دادهٔ زبان را از assets به `filesDir/ocr/tessdata/` کپی می‌کند (فقط بار اول)
     * و مسیرِ **والدِ** پوشهٔ tessdata را برمی‌گرداند — چیزی که `TessBaseAPI.init`
     * انتظار دارد.
     */
    fun ensureTessData(context: Context): String {
        val base = File(context.filesDir, "ocr")
        val dir = File(base, "tessdata")
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, "$LANG.traineddata")
        val assetName = "tessdata/$LANG.traineddata"
        // اگر فایل نیست یا ناقص کپی شده (قطعِ نصب/کم‌بودن فضا) دوباره کپی شود.
        // اندازهٔ مرجع از خودِ asset خوانده می‌شود (بدون AssetFileDescriptor تا
        // به رفتار فشرده‌سازی وابسته نباشیم).
        val expected = runCatching {
            var total = 0L
            context.assets.open(assetName).use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    total += n
                }
            }
            total
        }.getOrDefault(-1L)
        if (!target.isFile || target.length() <= 0L || (expected > 0L && target.length() != expected)) {
            context.assets.open(assetName).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return base.absolutePath
    }

    /**
     * تصویر را برای OCR کوچک می‌کند (اگر لازم باشد). تصویرهای خیلی بزرگِ دوربین
     * بدون این کار چند برابر طول می‌کشند بدون آنکه دقت بهتر شود.
     */
    internal fun boundForOcr(src: Bitmap, maxEdge: Int = MAX_OCR_EDGE): Bitmap {
        val largest = maxOf(src.width, src.height)
        if (largest <= maxEdge) return src
        val scale = maxEdge.toFloat() / largest
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    /**
     * متنِ فارسیِ تصویر را استخراج می‌کند. روی Dispatchers.Default اجرا می‌شود
     * چون کارِ سنگینِ CPU است. در هر خطا `null` برمی‌گردد (بدون کرش).
     */
    suspend fun recognize(context: Context, bitmap: Bitmap): OcrResult? =
        withContext(Dispatchers.Default) {
            var api: TessBaseAPI? = null
            runCatching {
                val dataPath = ensureTessData(context)
                val engine = TessBaseAPI()
                api = engine
                if (!engine.init(dataPath, LANG)) return@runCatching null
                // بلوکِ یکپارچهٔ متن — مناسبِ عکسِ یک سؤال از کتاب/جزوه.
                engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                engine.setImage(boundForOcr(bitmap))
                val raw = engine.getUTF8Text()
                val conf = engine.meanConfidence()
                val text = normalizePersian(raw ?: "")
                if (text.isBlank()) null else OcrResult(text, conf)
            }.getOrNull().also {
                runCatching { api?.recycle() }
            }
        }

    /**
     * پاک‌سازیِ خروجیِ خام: حروفِ عربیِ ی/ک به فارسی، ارقامِ عربی/فارسی به لاتین
     * (چون فیلدهای نمره/شماره در آزمون‌ساز عددی‌اند)، حذفِ فاصله‌های اضافه و
     * خط‌های خالیِ پشت‌سرهم. عمداً محافظه‌کار است تا متنِ کاربر دستکاری نشود.
     */
    internal fun normalizePersian(raw: String): String {
        var s = raw
            .replace('\u064A', '\u06CC') // ي عربی → ی فارسی
            .replace('\u0649', '\u06CC') // ى → ی
            .replace('\u0643', '\u06A9') // ك عربی → ک فارسی
            .replace('\u200F', ' ')
            .replace('\u00A0', ' ')
        val digits = mapOf(
            '\u06F0' to '0', '\u06F1' to '1', '\u06F2' to '2', '\u06F3' to '3', '\u06F4' to '4',
            '\u06F5' to '5', '\u06F6' to '6', '\u06F7' to '7', '\u06F8' to '8', '\u06F9' to '9',
            '\u0660' to '0', '\u0661' to '1', '\u0662' to '2', '\u0663' to '3', '\u0664' to '4',
            '\u0665' to '5', '\u0666' to '6', '\u0667' to '7', '\u0668' to '8', '\u0669' to '9'
        )
        s = s.map { digits[it] ?: it }.joinToString("")
        val lines = s.split("\n").map { line ->
            line.trim().replace(Regex("[ \\t]{2,}"), " ")
        }
        val cleaned = StringBuilder()
        var blank = 0
        for (line in lines) {
            if (line.isEmpty()) {
                blank++
                if (blank > 1) continue
            } else {
                blank = 0
            }
            cleaned.append(line).append('\n')
        }
        return cleaned.toString().trim()
    }
}
