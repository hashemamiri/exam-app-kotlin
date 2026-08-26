package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CalendarNoteDto(
    val id: String = "",
    @SerialName("on_date") val onDate: String = "",
    val title: String = "",
    val body: String? = null,
    val audience: String? = null,
    val classes: List<String> = emptyList(),
    val students: List<String> = emptyList(),
    // V61.0 — مدرسه‌های مخاطب پیام.
    val schools: List<String> = emptyList(),
    val error: String? = null
)

@Serializable
data class CalendarMonthResponseDto(
    val ok: Boolean = false,
    val notes: List<CalendarNoteDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class HolidayDto(
    val jy: Int,
    val jm: Int,
    val jd: Int,
    val title: String,
    val holiday: Boolean = true
)

@Serializable
data class HolidayResponseDto(
    val ok: Boolean = false,
    val days: List<HolidayDto> = emptyList(),
    val years: JsonObject = JsonObject(emptyMap()),
    val error: String? = null
)

@Serializable
data class CalendarSaveResponseDto(
    val ok: Boolean = false,
    val id: String? = null,
    val error: String? = null
)
