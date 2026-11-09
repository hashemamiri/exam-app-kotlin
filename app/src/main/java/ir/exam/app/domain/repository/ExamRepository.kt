package ir.exam.app.domain.repository

import ir.exam.app.domain.model.Exam
import ir.exam.app.domain.model.ServerSectionSync
import ir.exam.app.domain.model.SubmissionOutcome
import ir.exam.app.domain.model.SubmittedExam

interface ExamRepository {
    suspend fun joinByCode(code: String): Result<Exam>
    suspend fun restoreActiveExam(): Result<Exam?> = Result.success(null)
    suspend fun refreshActiveExam(): Result<Exam?> = Result.success(null)
    suspend fun clearActiveExam(examId: String): Result<Unit> = Result.success(Unit)
    suspend fun submitAttempt(attempt: SubmittedExam): Result<SubmissionOutcome>

    /** همگام‌سازی زمان شروع/قفل هر بخش در سرور و ثبت snapshot پاسخ‌های بخش پیش از اتمام مهلت. */
    suspend fun syncSectionTiming(
        examId: String,
        sectionTitle: String,
        sectionSnapshot: Map<Int, String> = emptyMap()
    ): Result<ServerSectionSync?> = Result.success(null)

    /** V58.0 — ثبت گزارش نظارتی آزمون (best-effort؛ فقط معلم می‌خواند). */
    suspend fun reportMonitor(examId: String, reportJson: String): Result<Unit> = Result.success(Unit)

    // ------------------------------------------------------------------
    // V359 — زمان هر سؤال و حالت آفلاین، سمت دانش‌آموز
    // پیاده‌سازی پیش‌فرض «محدود نیست» می‌دهد تا مخزن‌های صف‌دار و
    // ساختگیِ آزمون نشکنند و رفتار پیشین دست‌نخورده بماند.
    // ------------------------------------------------------------------

    /** ساعت سؤال با نخستین بازدید روشن می‌شود؛ بازگشت آن را از نو شروع نمی‌کند. */
    suspend fun syncQuestionTiming(examId: String, questionIndex: Int): Result<StudentQuestionTiming> =
        Result.success(StudentQuestionTiming())

    /** بستهٔ آفلاین: توکن یک‌بارمصرف + مهلت سرور. کلید پاسخ در آن نیست. */
    suspend fun offlinePack(examId: String): Result<StudentOfflinePackPayload> =
        Result.failure(IllegalStateException("حالت آفلاین در این مخزن پشتیبانی نمی‌شود."))

    /** ارسال آفلاین؛ سرور توکن و زمان پایان را می‌سنجد و برگه را آفلاین علامت می‌زند. */
    suspend fun submitOffline(
        examId: String,
        token: String,
        responses: List<String>,
        events: List<String>,
        finishedAtIso: String
    ): Result<Unit> = Result.failure(IllegalStateException("حالت آفلاین در این مخزن پشتیبانی نمی‌شود."))
}

/** پاسخ سرور برای ساعت یک سؤال. [limited]=false یعنی این سؤال محدودیت ندارد. */
data class StudentQuestionTiming(
    val limited: Boolean = false,
    val locked: Boolean = false,
    val expiresAtMs: Long = 0L,
    val serverNowMs: Long = 0L,
    val seconds: Int = 0
)

/** بستهٔ آفلاینی که سرور می‌دهد. */
data class StudentOfflinePackPayload(
    val token: String = "",
    val expiresAtMs: Long = 0L,
    val serverNowMs: Long = 0L,
    val limits: Map<Int, Int> = emptyMap()
)
