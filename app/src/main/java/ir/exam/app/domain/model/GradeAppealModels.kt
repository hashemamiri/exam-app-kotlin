package ir.exam.app.domain.model

import ir.exam.app.core.calendar.PersianDigits
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * V354 — اعتراض به نمره، مصحح دوم، و تاریخچهٔ تغییر نمره.
 *
 * این فایل تنها داده و منطق خالص دارد (بدون اندروید و بدون شبکه) تا
 * بتوان آن را با آزمون واحد JVM سنجید. قرارداد آن با تابع‌های
 * native_*_v354 در پایگاه‌داده یکسان است.
 */

/** وضعیت اعتراض؛ مقدار روی سیم همان رشتهٔ انگلیسی پایگاه‌داده است. */
enum class AppealStatus(val wire: String, val label: String) {
    OPEN("open", "در انتظار بررسی"),
    SECOND("second", "نزد مصحح دوم"),
    ACCEPTED("accepted", "پذیرفته شد"),
    REJECTED("rejected", "رد شد");

    val isOpen: Boolean get() = this == OPEN || this == SECOND

    companion object {
        fun fromWire(value: String?): AppealStatus =
            entries.firstOrNull { it.wire == value } ?: OPEN
    }
}

/** شاخص «کل برگه»؛ برای اعتراض به یک سؤال، شاخص همان سؤال است. */
const val APPEAL_WHOLE_PAPER: Int = -1

data class GradeAppeal(
    val id: String,
    val answerId: String,
    val examId: String,
    val examTitle: String? = null,
    val studentId: String? = null,
    val studentName: String? = null,
    val questionIndex: Int = APPEAL_WHOLE_PAPER,
    val reason: String,
    val status: AppealStatus = AppealStatus.OPEN,
    val secondScore: Double? = null,
    val secondNote: String? = null,
    val resolutionNote: String? = null,
    val createdAt: String? = null,
    val resolvedAt: String? = null
) {
    val isWholePaper: Boolean get() = questionIndex == APPEAL_WHOLE_PAPER

    /** عنوان کوتاه برای فهرست معلم. */
    val targetLabel: String
        get() = if (isWholePaper) "کل برگه" else "سؤال ${PersianDigits.convert(questionIndex + 1)}"

    companion object {
        /** کمینه و بیشینهٔ طول دلیل اعتراض — همان محدودیت پایگاه‌داده. */
        const val REASON_MIN = 5
        const val REASON_MAX = 2000

        /**
         * پیش از فرستادن به سرور، همان قید پایگاه‌داده را این‌جا می‌سنجیم
         * تا کاربر پیام روشن بگیرد. null یعنی معتبر است.
         */
        fun reasonError(reason: String?): String? {
            val text = reason?.trim().orEmpty()
            if (text.length < REASON_MIN) return "دلیل اعتراض را کامل‌تر بنویسید (دست‌کم ${PersianDigits.convert(REASON_MIN)} نویسه)."
            if (text.length > REASON_MAX) return "متن اعتراض بیش از حد بلند است."
            return null
        }

        fun fromJson(row: JsonObject): GradeAppeal = GradeAppeal(
            id = row.str("id").orEmpty(),
            answerId = row.str("answer_id").orEmpty(),
            examId = row.str("exam_id").orEmpty(),
            examTitle = row.str("exam_title"),
            studentId = row.str("student_id"),
            studentName = row.str("student_name"),
            questionIndex = row["question_index"]?.jsonPrimitive?.intOrNull ?: APPEAL_WHOLE_PAPER,
            reason = row.str("reason").orEmpty(),
            status = AppealStatus.fromWire(row.str("status")),
            secondScore = row["second_score"]?.jsonPrimitive?.doubleOrNull,
            secondNote = row.str("second_note"),
            resolutionNote = row.str("resolution_note"),
            createdAt = row.str("created_at"),
            resolvedAt = row.str("resolved_at")
        )

        fun listFrom(array: JsonArray): List<GradeAppeal> =
            array.mapNotNull { (it as? JsonObject)?.let(::fromJson) }
    }
}

/** یک ردیف از تاریخچهٔ تغییر نمره — فقط خواندنی؛ کاربر نمی‌تواند بنویسد. */
data class GradeHistoryRow(
    val id: Long,
    val answerId: String,
    val studentName: String,
    val changedByName: String,
    val beforeScore: Double?,
    val afterScore: Double?,
    val beforeGraded: Boolean?,
    val afterGraded: Boolean?,
    val createdAt: String?
) {
    /** آیا نمره واقعاً جابه‌جا شده است (نه فقط وضعیت تصحیح). */
    val scoreChanged: Boolean get() = beforeScore != afterScore

    companion object {
        fun fromJson(row: JsonObject): GradeHistoryRow = GradeHistoryRow(
            id = row["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            answerId = row.str("answer_id").orEmpty(),
            studentName = row.str("student_name") ?: "دانش‌آموز",
            changedByName = row.str("changed_by_name") ?: "سامانه",
            beforeScore = row["before_score"]?.jsonPrimitive?.doubleOrNull,
            afterScore = row["after_score"]?.jsonPrimitive?.doubleOrNull,
            beforeGraded = row.str("before_graded")?.toBooleanStrictOrNull(),
            afterGraded = row.str("after_graded")?.toBooleanStrictOrNull(),
            createdAt = row.str("created_at")
        )

        fun listFrom(array: JsonArray): List<GradeHistoryRow> =
            array.mapNotNull { (it as? JsonObject)?.let(::fromJson) }
    }
}

/**
 * تصحیح ناشناس — نام دانش‌آموز پنهان و ترتیب برگه‌ها درهم می‌شود.
 *
 * الگوریتم درهم‌سازی دقیقاً آینهٔ سایت است (admin.js، تابع blindOrder):
 * هش ۳۲ بیتی با ضریب ۳۱ روی واحدهای کد UTF-16، سپس صعودی.
 * چون هش پایدار است، با هر بار رندر ترتیب جابه‌جا نمی‌شود.
 */
object BlindMarking {
    /** همان هش سایت: x = (x * 31 + code) با سرریز ۳۲ بیتی علامت‌دار. */
    fun hash(id: String?): Int {
        var x = 0
        val text = id.orEmpty()
        for (ch in text) x = x * 31 + ch.code
        return x
    }

    fun label(index: Int): String = "برگهٔ ${PersianDigits.convert(index + 1)}"

    /** ترتیب درهم بر پایهٔ شناسه؛ لیست اصلی دست‌نخورده می‌ماند. */
    fun <T> order(list: List<T>, idOf: (T) -> String): List<T> =
        list.sortedBy { hash(idOf(it)) }

    /**
     * اعمال تصحیح ناشناس روی برگه‌ها. اگر خاموش باشد، همان لیست ورودی
     * بی‌هیچ تغییری برمی‌گردد (نه کپی، نه مرتب‌سازی).
     */
    fun apply(list: List<GradingSubmission>, on: Boolean): List<GradingSubmission> {
        if (!on) return list
        return order(list) { it.id }.mapIndexed { i, item -> item.copy(studentName = label(i)) }
    }

    /** نام نمایشی یک برگه در حالت ناشناس؛ در حالت عادی نام واقعی. */
    fun displayName(realName: String, index: Int, on: Boolean): String =
        if (on) label(index) else realName
}

private fun JsonObject.str(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
