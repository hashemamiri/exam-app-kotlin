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
)
