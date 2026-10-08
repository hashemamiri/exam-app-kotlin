package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamDashboardDto(
    val id: String,
    val title: String = "",
    val subject: String? = null,
    val duration: Int? = null,
    val code: String? = null,
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("total_score") val totalScore: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null
) {
    companion object {
        /**
         * V212 — فهرست آزمون‌ها فقط همین ستون‌ها را از سرور می‌خواند؛ قبلاً `select()` بدون ستون
         * برای هر آزمون ستون سنگین `questions` (کل سؤال‌ها) را هم دانلود می‌کرد.
         */
        const val COLUMNS = "id,title,subject,duration,code,is_open,total_score,created_at"
    }
}
