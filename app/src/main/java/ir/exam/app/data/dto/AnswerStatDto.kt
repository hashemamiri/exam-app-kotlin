package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** V212 — نمای سبکِ یک پاسخ فقط برای شمارش (کارت‌های صفحهٔ تصحیح). */
@Serializable
data class AnswerStatDto(
    val id: String,
    @SerialName("student_id") val studentId: String? = null,
    val graded: Boolean = false
)
