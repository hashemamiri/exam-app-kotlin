package ir.exam.app.ui.printing

import android.content.Context
import java.io.File

/**
 * V78.2 — آینهٔ پشتیبانِ پیش‌نویسِ آزمون.
 *
 * ذخیرهٔ خودکارِ صفحه در `localStorage` انجام می‌شود و **با پاک‌کردنِ دادهٔ
 * WebView یا کش برنامه از بین می‌رود**. اینجا همان JSON در فضای خصوصیِ برنامه
 * هم نگه داشته می‌شود تا اگر localStorage خالی بود، کارِ کاربر برنگردد.
 *
 * این یک «آینه» است نه منبعِ حقیقت: صفحه همچنان مثل قبل کار می‌کند و اگر
 * localStorage سالم باشد اصلاً سراغِ آینه نمی‌رویم. پس رفتار موجود تغییری
 * نمی‌کند و فقط یک تورِ ایمنی اضافه می‌شود.
 */
internal object ExamDraftMirror {

    private const val FILE_NAME = "exam_print_draft.json"

    /** حداکثر حجمِ پذیرفته‌شده — جلوی پرشدنِ حافظه با دادهٔ خراب را می‌گیرد. */
    private const val MAX_BYTES = 8 * 1024 * 1024

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    /** ذخیرهٔ آینه. خطا عمداً بی‌صداست تا هرگز جریانِ کاربر را قطع نکند. */
    fun save(context: Context, json: String) {
        runCatching {
            if (json.isBlank() || json.length > MAX_BYTES) return
            if (!looksLikeDraft(json)) return
            file(context).writeText(json, Charsets.UTF_8)
        }
    }

    /** خواندنِ آینه؛ اگر نبود یا خراب بود، null. */
    fun load(context: Context): String? = runCatching {
        val f = file(context)
        if (!f.isFile || f.length() == 0L || f.length() > MAX_BYTES) return null
        f.readText(Charsets.UTF_8).takeIf { looksLikeDraft(it) }
    }.getOrNull()

    /** پس از ذخیرهٔ قطعی یا شروعِ آزمون تازه، آینه پاک می‌شود. */
    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    fun exists(context: Context): Boolean = runCatching {
        file(context).let { it.isFile && it.length() > 0L }
    }.getOrDefault(false)

    /**
     * بررسیِ حداقلیِ ساختار — نه اعتبارسنجیِ کامل JSON، فقط جلوگیری از
     * نوشتن/خواندنِ چیزی که آشکارا پیش‌نویسِ آزمون نیست.
     */
    internal fun looksLikeDraft(json: String): Boolean {
        val t = json.trim()
        return t.startsWith("{") && t.endsWith("}") && "\"questions\"" in t
    }
}
