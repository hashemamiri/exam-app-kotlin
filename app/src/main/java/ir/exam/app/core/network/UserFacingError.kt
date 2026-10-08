package ir.exam.app.core.network

/**
 * V209 — پیام خطای قابل‌فهم برای کاربر.
 *
 * قاعده: هیچ متن فنی (پیام Ktor/OkHttp/Supabase، نام کلاس جاوا، آدرس سرور، «HTTP request to … failed»)
 * نباید به کاربر نشان داده شود. اگر خطا از نوع شبکه باشد پیام اینترنت، اگر متنِ خطا فارسی نباشد یا
 * نشانهٔ فنی داشته باشد پیام عمومی سرور، و فقط پیام‌های فارسیِ معنادار (مثلاً «موجودی کافی نیست»)
 * عیناً نمایش داده می‌شوند. همهٔ safe*Error های برنامه از اینجا عبور می‌کنند.
 */
object UserFacingError {
    const val NETWORK = "خطا در ارتباط با سرور؛ از اتصال به اینترنت مطمئن شوید و دوباره تلاش کنید."
    const val SERVER = "خطا در سرور؛ از اتصال به اینترنت مطمئن شوید و دوباره تلاش کنید."

    private val technicalMarkers = listOf(
        "http request", "failed with message", "unable to resolve host", "no address associated",
        "exception", "supabase.co", "supabase.in", "ktor", "okhttp", "java.", "kotlin.", "io.github",
        "timeout", "timed out", "connection reset", "connection refused", "ssl", "handshake", "socket",
        "pgrst", "jwt", "postgrest", "rest api", "status code", "internal server error", "bad gateway",
        "service unavailable", "gateway timeout", "cloudflare", "econn", "enotfound", "unknownhost", "errno"
    )

    private val persian = Regex("[\\u0600-\\u06FF]")

    /** حذف بخش‌های حساس/فنیِ دنباله‌دار (URL، هدرها، توکن‌ها). */
    fun sanitize(raw: String?): String = raw.orEmpty()
        .substringBefore("URL:")
        .substringBefore("Headers:")
        .replace(Regex("(?i)authorization[^,\\n]*"), "")
        .replace(Regex("(?i)apikey[^,\\n]*"), "")
        .replace(Regex("(?i)bearer\\s+[A-Za-z0-9._-]+"), "")
        .replace(Regex("https?://\\S+"), "")
        .trim()

    fun hasPersian(text: String): Boolean = persian.containsMatchIn(text)

    fun isTechnical(text: String): Boolean {
        if (text.isBlank()) return false
        if (!hasPersian(text)) return true
        val lower = text.lowercase()
        if (technicalMarkers.any(lower::contains)) return true
        // متن فارسی با دنبالهٔ بلند لاتین (پیام خام سرور/کتابخانه) هم فنی حساب می‌شود
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val letters = text.count { it.isLetter() }
        return letters > 0 && latin * 100 / letters >= 40
    }

    /** پیام نهایی از روی متن (وقتی Throwable در دسترس نیست). */
    fun fromText(raw: String?, fallback: String, maxLength: Int = 260): String {
        val clean = sanitize(raw)
        if (clean.isBlank()) return fallback
        val lower = clean.lowercase()
        val networkLike = listOf(
            "unable to resolve host", "no address associated", "failed to connect", "timeout", "timed out",
            "connection reset", "connection refused", "network is unreachable", "unknownhost", "software caused connection abort"
        ).any(lower::contains)
        if (networkLike) return NETWORK
        if (!isTechnical(clean)) return clean.take(maxLength)
        // متن فارسی با دنبالهٔ فنی («آپلود ناموفق بود: HTTP request …») → فقط بخش فارسیِ قبل از «:»
        val head = clean.substringBefore(":").trim()
        if (head.isNotBlank() && hasPersian(head) && !isTechnical(head)) return head.take(maxLength)
        return SERVER
    }

    /** پیام نهایی از روی خطا. */
    fun of(error: Throwable?, fallback: String, maxLength: Int = 260): String {
        if (error == null) return fallback
        if (NetworkFailureClassifier.isNetworkFailure(error)) {
            // اگر خودِ برنامه پیام فارسی شبکه ساخته، همان را نگه می‌داریم
            val clean = sanitize(error.message)
            if (clean.isNotBlank() && hasPersian(clean) && !isTechnical(clean)) return clean.take(maxLength)
            return NETWORK
        }
        return fromText(error.message, fallback, maxLength)
    }
}
