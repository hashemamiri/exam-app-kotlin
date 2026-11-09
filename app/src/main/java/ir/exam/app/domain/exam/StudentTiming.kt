package ir.exam.app.domain.exam

/**
 * V359 — وضعیت سمت دانش‌آموز برای زمان هر سؤال و حالت آفلاین.
 *
 * چرا این فایل جداست؟ منطق V357 پیش‌تر در [ExamTiming] بود و سمت معلم
 * از آن استفاده می‌کرد، ولی هیچ چیز آن را به صفحهٔ آزمون دانش‌آموز وصل
 * نمی‌کرد. این مدل، «چه چیزی باید نشان داده شود» را از کامپوز و شبکه
 * جدا می‌کند تا بدون اندروید آزمون شود.
 *
 * قاعدهٔ اعتماد: مهلت را **سرور** تعیین می‌کند. این مدل فقط مهلت سرور
 * را نگه می‌دارد و وضعیت را حساب می‌کند؛ هیچ‌گاه مهلت نمی‌سازد و قفل
 * را دور نمی‌زند. اگر سرور در دسترس نباشد، محدودیت اعمال نمی‌شود
 * (شکست باز) تا دانش‌آموز بی‌جهت از پاسخ‌گویی محروم نشود؛ ولی قفلِ
 * یک‌بار ثبت‌شده هرگز باز نمی‌شود.
 */
object StudentTiming {

    /** سقف رویدادهای آفلاین، همان قرارداد سرور. */
    const val MAX_EVENTS = ExamTiming.MAX_EVENTS

    /**
     * مهلت تراز‌شدهٔ یک سؤال. [deadlineMs] صفر یعنی «محدود نیست».
     * [locked] یک‌طرفه است: وقتی true شد، دیگر false نمی‌شود.
     */
    data class QuestionClock(
        val deadlineMs: Long = 0L,
        val locked: Boolean = false,
        val seconds: Int = 0
    ) {
        val limited: Boolean get() = deadlineMs > 0L
    }

    /**
     * پاسخ سرور را به ساعت سؤال تبدیل می‌کند.
     * اگر سرور بگوید محدود نیست، ساعت صفر می‌ماند؛ ولی قفلِ پیشین حفظ
     * می‌شود چون باز کردن قفل مجاز نیست.
     */
    fun applySync(
        previous: QuestionClock?,
        limited: Boolean,
        locked: Boolean,
        expiresAtMs: Long,
        serverNowMs: Long,
        seconds: Int,
        localNowMs: Long
    ): QuestionClock {
        val wasLocked = previous?.locked == true
        if (!limited) return QuestionClock(0L, wasLocked, 0)
        val aligned = ExamTiming.alignDeadline(expiresAtMs, serverNowMs, localNowMs)
        return QuestionClock(aligned, wasLocked || locked, seconds)
    }

    /** آیا این سؤال باید قفل نشان داده شود؟ */
    fun isLocked(clock: QuestionClock?, nowMs: Long): Boolean {
        val c = clock ?: return false
        if (c.locked) return true
        if (!c.limited) return false
        return ExamTiming.remainingMs(c.deadlineMs, nowMs) <= 0L
    }

    /** متن شمارش معکوس، یا رشتهٔ خالی اگر محدود نیست. */
    fun clockText(clock: QuestionClock?, nowMs: Long): String {
        val c = clock ?: return ""
        if (!c.limited || c.locked) return ""
        return ExamTiming.formatClock(ExamTiming.remainingMs(c.deadlineMs, nowMs))
    }

    /** هشدار رنگ: کمتر از یک‌چهارم زمان مانده یا کمتر از ۳۰ ثانیه. */
    fun isWarning(clock: QuestionClock?, nowMs: Long): Boolean {
        val c = clock ?: return false
        if (!c.limited || c.locked) return false
        val left = ExamTiming.remainingMs(c.deadlineMs, nowMs)
        val total = c.seconds.toLong() * 1000L
        val quarter = if (total > 0L) total / 4L else 30_000L
        return left <= (if (quarter > 30_000L) quarter else 30_000L)
    }

    // ------------------------------------------------------------------
    // حالت آفلاین
    // ------------------------------------------------------------------

    /**
     * بستهٔ آفلاین. [used] یک‌طرفه است. [alignedMs] مهلت تراز‌شده با
     * ساعت محلی است؛ مقایسه با مهلت خام سرور غلط می‌بود.
     */
    data class OfflinePack(
        val token: String = "",
        val alignedMs: Long = 0L,
        val used: Boolean = false,
        val takenAtMs: Long = 0L
    ) {
        val taken: Boolean get() = token.isNotBlank()
    }

    enum class OfflineStatus { NONE, READY, USED, EXPIRED }

    fun offlineStatus(pack: OfflinePack?, nowMs: Long): OfflineStatus {
        if (pack == null || !pack.taken) return OfflineStatus.NONE
        if (pack.used) return OfflineStatus.USED
        if (pack.alignedMs > 0L && ExamTiming.remainingMs(pack.alignedMs, nowMs) <= 0L) {
            return OfflineStatus.EXPIRED
        }
        return OfflineStatus.READY
    }

    /** برچسب فارسی وضعیت، همان واژه‌های سایت. */
    fun offlineLabel(status: OfflineStatus): String = when (status) {
        OfflineStatus.NONE -> "گرفته نشده"
        OfflineStatus.READY -> "آماده"
        OfflineStatus.USED -> "مصرف‌شده"
        OfflineStatus.EXPIRED -> "مهلت گذشته"
    }

    /** آیا دکمهٔ «گرفتن بسته» باید نشان داده شود؟ */
    fun canTakePack(pack: OfflinePack?, practice: Boolean): Boolean {
        if (practice) return false
        return pack == null || !pack.taken || pack.used
    }

    /**
     * آیا ارسال آفلاین مجاز است؟ از همان سنجهٔ مشترک استفاده می‌کند
     * تا اپ و سایت و سرور یک حرف بزنند.
     */
    fun canSubmitOffline(pack: OfflinePack?, nowMs: Long): Boolean {
        val p = pack ?: return false
        if (!p.taken || p.used) return false
        return ExamTiming.offlineSubmittable(
            ExamTiming.OfflinePack(p.token, p.used), nowMs, p.alignedMs
        ).ok
    }

    /** پیام راهنمای وضعیت آفلاین برای دانش‌آموز. */
    fun offlineHint(status: OfflineStatus): String = when (status) {
        OfflineStatus.NONE ->
            "برای مناطق کم‌پوشش: پیش از قطع اینترنت بسته را بگیرید. مهلت همان مهلت سرور است و برگه با نشان «آفلاین» به دست معلم می‌رسد، چون نظارت زنده ممکن نیست."
        OfflineStatus.READY ->
            "بسته آماده است. اگر اینترنت قطع شد، پاسخ‌ها روی همین دستگاه می‌مانند؛ پس از وصل شدن «ارسال آفلاین» را بزنید."
        OfflineStatus.USED ->
            "برگهٔ آفلاین ارسال شد و برای بررسی معلم علامت خورد."
        OfflineStatus.EXPIRED ->
            "مهلت بستهٔ آفلاین گذشته است. اگر اینترنت دارید، ارسال معمولی را انجام دهید."
    }

    /**
     * پاسخ‌ها را به آرایهٔ رشته‌ای ترتیب‌سؤال تبدیل می‌کند؛ همان قالبی
     * که سایت می‌فرستد. سؤال بی‌پاسخ رشتهٔ خالی می‌شود، نه حذف.
     */
    fun responsesInOrder(questionIds: List<String>, answers: Map<String, String>): List<String> =
        questionIds.map { answers[it].orEmpty() }
}
