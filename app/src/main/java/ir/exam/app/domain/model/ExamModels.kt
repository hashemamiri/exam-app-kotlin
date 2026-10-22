package ir.exam.app.domain.model

data class Exam(
    val id: String,
    val title: String,
    val code: String,
    val durationMinutes: Int,
    val questions: List<Question>,
    val subject: String = "",
    val teacherMessage: String? = null,
    /** زمان محلی متناظر با expires_at سرور؛ null یعنی بدون محدودیت. */
    val deadlineEpochMs: Long? = null,
    val shuffleQuestions: Boolean = false,
    val shuffleOptions: Boolean = false,
    val attemptsAllowed: Int = 1,
    val attemptNumber: Int? = null,
    val attemptsRemaining: Int? = null,
    val questionPresentation: Map<String, QuestionPresentation> = emptyMap(),
    /** V301 — «حالت ابتدایی»: نمای درشت و ساده برای پایهٔ اول تا ششم (exams.options.primary). */
    val primaryMode: Boolean = false,
    /** مهلت مستقل هر بخش، برحسب دقیقه؛ کلید همان sectionTitle است. */
    val sectionDurationsMinutes: Map<String, Int> = emptyMap(),
    /** مهلت سروری بخش‌ها (epoch ms محلی هم‌تراز با server_now) برای جلوگیری از دستکاری ساعت یا پاک‌کردن پیش‌نویس. */
    val serverSectionDeadlinesEpochMs: Map<String, Long> = emptyMap(),
    /** بخش‌هایی که در سرور مهلتشان به پایان رسیده است. */
    val serverExpiredSections: Set<String> = emptySet()
)

data class ServerSectionSync(
    val sectionDeadlinesEpochMs: Map<String, Long> = emptyMap(),
    val expiredSections: Set<String> = emptySet()
)

data class QuestionPresentation(
    val textAlign: String = "right",
    val imagePosition: String = "below",
    val fontFamily: String = "default",
    val fontSizeSp: Float = 16f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val answerLines: Int = 2,
    val answerLineStyle: String = "lined",
    val media: List<QuestionMediaPresentation> = emptyList(),
    /** V58.0 — اجازهٔ معلم: دانش‌آموز بتواند نمودار پاسخ رسم/ویرایش کند. */
    val allowAnswerGraph: Boolean = false,
    /** V135 — فایل صوتی سؤال (URL) و مدت آن؛ دانش‌آموز با پخش‌کننده می‌شنود. */
    val audioUrl: String? = null,
    val audioMs: Long = 0L
)

data class QuestionMediaPresentation(
    val xMm: Float = 20f,
    val yMm: Float = 30f,
    val widthMm: Float = 55f
)

/** تعریف امن زیربخش سؤال؛ rubric فقط در کلید پاسخ/مدل تصحیح تکمیل می‌شود. */
data class AssessmentPart(
    val id: String,
    val label: String,
    val prompt: String,
    val score: Double,
    val carriesForward: Boolean = false,
    val reasonPrompt: String = "",
    val requiresReason: Boolean = false,
    val rubric: String = ""
)

sealed interface Question {
    val id: String
    val text: String
    val score: Double
    val images: List<String>
    val maxAnswerImages: Int
    val answerImagesRequired: Boolean
    /** اندیس اصلی سرور؛ پاسخ‌های آرایه‌ای همیشه با این ترتیب ارسال می‌شوند. */
    val originalIndex: Int
    val passage: String
    val sectionTitle: String
    val topic: String
    val rubric: String get() = ""
    val assessmentParts: List<AssessmentPart> get() = emptyList()
}

data class EssayQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    val answerMode: String = "text",
    val voiceMaxSeconds: Int = 0,
    override val passage: String = "",
    override val sectionTitle: String = "",
    override val topic: String = "",
    val solutionSteps: List<String> = emptyList(),
    override val rubric: String = "",
    override val assessmentParts: List<AssessmentPart> = emptyList(),
    val proofAssumptions: String = "",
    val proofGoal: String = "",
    val constructionPrompt: String = "",
    val constructionLabels: List<String> = emptyList(),
    val constructionAngleLabels: List<String> = emptyList()
) : Question

data class MultipleChoiceQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    val options: List<String>,
    val optionImages: List<String?> = emptyList(),
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    /** هر گزینه نمایشی به کدام اندیس اصلی سرور اشاره می‌کند. */
    val optionOriginalIndices: List<Int> = options.indices.toList(),
    /** V305 — انتخاب روی تصویر: ناحیهٔ هر گزینه به «اندیس اصلی» (درصد x,y,w,h)؛ خالی = حالت عادی. */
    val hotspots: List<HotspotZone> = emptyList(),
    override val passage: String = "",
    override val sectionTitle: String = "",
    override val topic: String = ""
) : Question

/** V305 — ناحیهٔ Hotspot (درصد نسبت به تصویر). */
data class HotspotZone(val x: Float, val y: Float, val w: Float, val h: Float)

data class TrueFalseQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    override val passage: String = "",
    override val sectionTitle: String = "",
    override val topic: String = ""
) : Question

data class FillBlankQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    /** V305 — مرتب‌سازی با کشیدن: موارد (به ترتیب نمایش بُرخورده)؛ خالی = جای خالی عادی. پاسخ = ترتیب با «,». */
    val orderItems: List<String> = emptyList(),
    /** V306 — املا: متنی که «بخوان» می‌خواند (فقط وقتی صدای معلم نیست؛ در پاسخ عمومی همین است). */
    val dictationText: String? = null,
    /** V314.0 — بخش‌کردن و صداکشی: تعداد بخش‌ها/صداها (≥ ۲). */
    val syllableCount: Int = 0,
    /** V314.0 — دسته‌بندی در سبد: نام سبدها و فهرست موارد بُرخورده. */
    val buckets: List<String> = emptyList(),
    val bucketItems: List<String> = emptyList(),
    /** V327 — املا: فاصلهٔ سکوت بین کلمه‌ها (ثانیه) و تعداد تکرار هر کلمه. */
    val dictationGap: Int = 6,
    val dictationRepeats: Int = 2,
    /** V327 — رنگ‌آمیزی روی شکل: ابعاد شبکه و چند خانه باید رنگ شود (۰ = غیرفعال). */
    val paintRows: Int = 0,
    val paintCols: Int = 0,
    val paintGoal: Int = 0,
    override val passage: String = "",
    override val sectionTitle: String = "",
    override val topic: String = ""
) : Question

data class NumericQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    override val passage: String = "",
    override val sectionTitle: String = "",
    override val topic: String = ""
) : Question

data class MatchingQuestion(
    override val id: String,
    override val text: String,
    override val score: Double,
    val leftItems: List<String>,
    val rightItems: List<String>,
    val leftImages: List<String?> = emptyList(),
    val rightImages: List<String?> = emptyList(),
    override val images: List<String> = emptyList(),
    override val maxAnswerImages: Int = 0,
    override val answerImagesRequired: Boolean = false,
    override val originalIndex: Int = 0,
    /** هر عضو نمایشی ستون راست به کدام اندیس اصلی کلید پاسخ اشاره می‌کند. */
    val rightOriginalIndices: List<Int> = rightItems.indices.toList(),
    override val passage: String = "",
    override val sectionTitle: String = "",
    override val topic: String = ""
) : Question

sealed interface StudentAnswer { val questionId: String }
data class TextAnswer(override val questionId: String, val value: String) : StudentAnswer
data class ChoiceAnswer(override val questionId: String, val selectedIndex: Int) : StudentAnswer
data class BooleanAnswer(override val questionId: String, val value: Boolean) : StudentAnswer
data class MatchingAnswer(override val questionId: String, val pairs: Map<Int, Int>) : StudentAnswer

data class StudentDraft(
    val answers: Map<String, StudentAnswer> = emptyMap(),
    val responseImages: Map<String, List<String>> = emptyMap(),
    val flaggedQuestionIds: Set<String> = emptySet(),
    val lastQuestionIndex: Int = 0,
    val sectionStartedAtEpochMs: Map<String, Long> = emptyMap(),
    val expiredSections: Set<String> = emptySet()
)

data class SubmittedExam(
    val examId: String,
    val answers: Map<String, StudentAnswer>,
    val responseImages: Map<String, List<String>>,
    val submittedAtEpochMs: Long,
    /** V58.0 — گزارش نظارتی JSON (رویدادهای امنیتی/زمان سؤال‌ها) فقط برای معلم. */
    val monitorReportJson: String? = null
)

sealed interface SubmissionOutcome {
    data class Sent(val receipt: String? = null) : SubmissionOutcome
    data class Queued(val actionId: String) : SubmissionOutcome
}

data class PendingSubmissionStatus(
    val id: String,
    val examId: String,
    val state: String,
    val attempts: Int,
    val createdAt: Long,
    val lastError: String?
)
