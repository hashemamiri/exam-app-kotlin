package ir.exam.app.domain.exam

/**
 * V357 — منطق زمان‌بندی آزمون و حالت آفلاین.
 *
 * آینهٔ دقیق site/src/examtiming.js؛ بردارهای مشترک دو پیاده‌سازی را
 * به هم می‌بندند.
 *
 * قاعدهٔ اعتماد: مهلت را **سرور** تعیین می‌کند. این فایل فقط مهلت
 * سرور را با ساعت محلی تراز می‌کند؛ هیچ‌گاه مهلت نمی‌سازد و قفل را
 * دور نمی‌زند.
 */
object ExamTiming {
    const val MAX_EXTRA_MINUTES = 240
    const val MIN_QUESTION_SECONDS = 10
    const val MAX_QUESTION_SECONDS = 7200
    const val MIN_REASON = 5
    const val MAX_REASON = 500
    const val OFFLINE_GRACE_MS = 120_000L
    const val MAX_EVENTS = 500

    private val LINE_SPLIT = Regex("\r\n|\r|\n")
    private val DIGITS = Regex("^[0-9]+$")

    /** رقم فارسی/عربی به لاتین — معلم فارسی‌زبان طبعاً رقم فارسی می‌نویسد. */
    fun latinDigits(s: String?): String {
        val src = s.orEmpty()
        val out = StringBuilder(src.length)
        for (ch in src) {
            when (ch.code) {
                in 0x06F0..0x06F9 -> out.append((ch.code - 0x06F0).toString())
                in 0x0660..0x0669 -> out.append((ch.code - 0x0660).toString())
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    /** تراز کردن مهلت سرور با ساعت محلی؛ فاصلهٔ سرور حفظ می‌شود. */
    fun alignDeadline(expiresAtMs: Long, serverNowMs: Long, localNowMs: Long): Long {
        if (expiresAtMs <= 0L) return 0L
        if (serverNowMs <= 0L) return expiresAtMs
        return localNowMs + (expiresAtMs - serverNowMs)
    }

    /** میلی‌ثانیهٔ باقی‌مانده؛ -1 یعنی بی‌نهایت. */
    fun remainingMs(alignedDeadlineMs: Long, localNowMs: Long): Long {
        if (alignedDeadlineMs <= 0L) return -1L
        return maxOf(0L, alignedDeadlineMs - localNowMs)
    }

    fun formatClock(ms: Long): String {
        if (ms < 0) return "∞"
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        fun pad(n: Long) = if (n < 10) "0$n" else n.toString()
        return if (h > 0) "$h:${pad(m)}:${pad(s)}" else "$m:${pad(s)}"
    }

    data class QuestionState(
        val limited: Boolean,
        val locked: Boolean,
        val remainingMs: Long,
        val clock: String,
        val warn: Boolean
    )

    /**
     * وضعیت یک سؤال. قفل نهایی از سرور می‌آید؛ اگر مهلت گذشته باشد هم
     * قفل حساب می‌شود تا رابط بی‌درنگ واکنش دهد.
     */
    fun questionState(
        limited: Boolean,
        alignedDeadlineMs: Long,
        localNowMs: Long,
        serverLocked: Boolean
    ): QuestionState {
        val left = if (limited) remainingMs(alignedDeadlineMs, localNowMs) else -1L
        val expired = limited && left == 0L
        return QuestionState(
            limited = limited,
            locked = serverLocked || expired,
            remainingMs = left,
            clock = if (limited) formatClock(left) else "",
            warn = limited && left > 0 && left <= 30_000L
        )
    }

    /** اعتبارسنجی مجوز زمان اضافه — پیش از فرستادن به سرور. */
    fun validateGrant(minutes: Int, reason: String?): String {
        val r = reason.orEmpty().trim()
        if (minutes < 0 || minutes > MAX_EXTRA_MINUTES) {
            return "زمان اضافه باید عدد صحیح بین ۰ تا ۲۴۰ دقیقه باشد."
        }
        if (r.length < MIN_REASON || r.length > MAX_REASON) {
            return "دلیل زمان اضافه باید بین ۵ تا ۵۰۰ نویسه باشد."
        }
        return ""
    }

    data class LimitsParseError(val line: Int, val message: String)
    data class LimitsParseResult(
        val limits: Map<Int, Int>,
        val errors: List<LimitsParseError>,
        val count: Int
    )

    /**
     * قالب محدودیت سؤال‌ها — هر خط: «شمارهٔ سؤال = ثانیه»
     * «همه = ۳۰۰» پیش‌فرض همهٔ سؤال‌ها است (کلید -1).
     * شمارهٔ سؤال از ۱ شمرده می‌شود و به شاخص صفرم تبدیل می‌گردد.
     */
    fun parseLimits(text: String?): LimitsParseResult {
        val limits = linkedMapOf<Int, Int>()
        val errors = mutableListOf<LimitsParseError>()
        var count = 0
        val lines = LINE_SPLIT.split(latinDigits(text))
        for (idx in lines.indices) {
            val raw = lines[idx].trim()
            if (raw.isEmpty() || raw[0] == '#') continue
            val cut = raw.indexOf('=')
            if (cut < 0) {
                errors.add(LimitsParseError(idx + 1, "شمارهٔ سؤال و ثانیه را با «=» جدا کنید.")); continue
            }
            val key = raw.substring(0, cut).trim()
            val value = raw.substring(cut + 1).trim()
            val isAll = key == "همه" || key.lowercase() == "all" || key == "*"
            val qi: Int
            if (isAll) {
                qi = -1
            } else {
                if (!DIGITS.matches(key)) {
                    errors.add(LimitsParseError(idx + 1, "شمارهٔ سؤال باید عدد باشد یا «همه».")); continue
                }
                qi = key.toInt() - 1
                if (qi < 0) {
                    errors.add(LimitsParseError(idx + 1, "شمارهٔ سؤال از ۱ شروع می‌شود.")); continue
                }
            }
            if (!DIGITS.matches(value)) {
                errors.add(LimitsParseError(idx + 1, "ثانیه باید عدد صحیح باشد.")); continue
            }
            val sec = value.toInt()
            if (sec < MIN_QUESTION_SECONDS || sec > MAX_QUESTION_SECONDS) {
                errors.add(LimitsParseError(idx + 1, "زمان هر سؤال باید بین ۱۰ تا ۷۲۰۰ ثانیه باشد.")); continue
            }
            limits[qi] = sec
            count++
        }
        return LimitsParseResult(limits, errors, count)
    }

    /** برگرداندن محدودیت‌ها به همان قالب (رفت‌وبرگشت پایدار). */
    fun formatLimits(limits: Map<Int, Int>): String =
        limits.keys.sorted().joinToString("\n") { k ->
            (if (k == -1) "همه" else (k + 1).toString()) + " = " + limits[k]
        }

    data class OfflinePack(val token: String, val used: Boolean)
    data class OfflineVerdict(val ok: Boolean, val reason: String)

    /** آیا برگهٔ آفلاین هنوز قابل ارسال است؟ همان قاعدهٔ سرور. */
    fun offlineSubmittable(pack: OfflinePack?, localNowMs: Long, alignedDeadlineMs: Long): OfflineVerdict {
        if (pack == null || pack.token.isEmpty()) return OfflineVerdict(false, "بستهٔ آفلاین گرفته نشده است.")
        if (pack.used) return OfflineVerdict(false, "این بستهٔ آفلاین پیش‌تر مصرف شده است.")
        if (alignedDeadlineMs <= 0L) return OfflineVerdict(false, "مهلت آزمون معلوم نیست.")
        if (localNowMs > alignedDeadlineMs + OFFLINE_GRACE_MS) {
            return OfflineVerdict(false, "مهلت آزمون گذشته است؛ برگهٔ آفلاین پذیرفته نمی‌شود.")
        }
        return OfflineVerdict(true, "")
    }

    data class OfflineEvent(val t: String, val at: Long, val d: String)

    /**
     * دفتر رویداد آفلاین. چون نظارت زنده ممکن نیست، رویدادها نگه داشته
     * می‌شوند تا معلم پس از ارسال ببیند.
     */
    fun pushEvent(events: List<OfflineEvent>, kind: String?, atMs: Long, detail: String?): List<OfflineEvent> {
        val k = kind.orEmpty().trim().take(40)
        if (k.isEmpty()) return events
        val list = events.toMutableList()
        list.add(OfflineEvent(k, maxOf(0L, atMs), detail.orEmpty().take(200)))
        return if (list.size > MAX_EVENTS) list.subList(list.size - MAX_EVENTS, list.size).toList() else list
    }
}
