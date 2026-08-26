package ir.exam.app.data.repository

import io.github.jan.supabase.postgrest.postgrest
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.JalaliDate
import ir.exam.app.data.dto.CalendarMonthResponseDto
import ir.exam.app.data.dto.CalendarNoteDto
import ir.exam.app.data.dto.CalendarSaveResponseDto
import ir.exam.app.data.dto.HolidayResponseDto
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.dto.StudentProfileDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.CalendarAudience
import ir.exam.app.domain.model.CalendarAudienceOption
import ir.exam.app.domain.model.CalendarDay
import ir.exam.app.domain.model.CalendarEditor
import ir.exam.app.domain.model.CalendarMonth
import ir.exam.app.domain.model.CalendarNote
import ir.exam.app.domain.model.OfficialHoliday
import ir.exam.app.domain.model.UserRole
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseCalendarRepository {
    suspend fun loadMonth(year: Int, month: Int): Result<CalendarMonth> = runCatching {
        require(year in JalaliCalendar.MIN_YEAR..JalaliCalendar.MAX_YEAR) { "سال تقویم نامعتبر است." }
        require(month in 1..12) { "ماه تقویم نامعتبر است." }
        val firstJalali = JalaliDate(year, month, 1)
        val lastJalali = JalaliDate(year, month, JalaliCalendar.monthLength(year, month))
        val from = JalaliCalendar.toGregorian(firstJalali)
        val to = JalaliCalendar.toGregorian(lastJalali)

        supervisorScope {
            val notesRequest = async {
                SupabaseProvider.client.postgrest.rpc(
                    "cal_month",
                    buildJsonObject {
                        put("p_from", from.toString())
                        put("p_to", to.toString())
                    }
                ).decodeAs<CalendarMonthResponseDto>()
            }
            val holidaysRequest = async {
                SupabaseProvider.client.postgrest.rpc(
                    "holidays_for",
                    buildJsonObject {
                        put("p_from", from.toString())
                        put("p_to", to.toString())
                    }
                ).decodeAs<HolidayResponseDto>()
            }

            val notesResponse = notesRequest.await().also { it.error?.takeIf(String::isNotBlank)?.let(::error) }
            val holidayResult = runCatching { holidaysRequest.await() }
            val holidaysResponse = holidayResult.getOrNull()
            holidaysResponse?.error?.takeIf(String::isNotBlank)?.let(::error)

            val notesByDate = notesResponse.notes.map(CalendarNoteDto::toDomain).groupBy(CalendarNote::date)
            val holidays = holidaysResponse?.days.orEmpty().mapNotNull { dto ->
                runCatching {
                    val jalali = JalaliDate(dto.jy, dto.jm, dto.jd)
                    OfficialHoliday(
                        date = JalaliCalendar.toGregorian(jalali),
                        jalaliDate = jalali,
                        title = dto.title,
                        isHoliday = dto.holiday
                    )
                }.getOrNull()
            }.groupBy(OfficialHoliday::date)
            val exact = holidaysResponse?.years?.get(year.toString())?.jsonPrimitive?.booleanOrNull == true

            CalendarMonth(
                year = year,
                month = month,
                days = (1..JalaliCalendar.monthLength(year, month)).map { day ->
                    val jalali = JalaliDate(year, month, day)
                    val gregorian = JalaliCalendar.toGregorian(jalali)
                    CalendarDay(
                        jalaliDate = jalali,
                        gregorianDate = gregorian,
                        notes = notesByDate[gregorian].orEmpty(),
                        officialHolidays = holidays[gregorian].orEmpty()
                    )
                },
                officialYearIsExact = exact,
                holidayDataAvailable = holidayResult.isSuccess
            )
        }
    }

    suspend fun loadAudienceOptions(role: UserRole): Result<Pair<List<CalendarAudienceOption>, List<CalendarAudienceOption>>> =
        runCatching {
            if (role != UserRole.TEACHER) return@runCatching emptyList<CalendarAudienceOption>() to emptyList()
            coroutineScope {
                val classes = async {
                    SupabaseProvider.client.postgrest.rpc("my_classes")
                        .decodeList<SchoolClassDto>()
                        .map { CalendarAudienceOption(it.id, it.name, it.grade) }
                }
                val students = async {
                    SupabaseProvider.client.postgrest.rpc("my_students_for_pick")
                        .decodeList<StudentProfileDto>()
                        .map { CalendarAudienceOption(it.id, it.fullName, it.classNames) }
                }
                classes.await() to students.await()
            }
        }

    /** V61.0 — مدرسه‌های عضو معلم برای مخاطب «مدارس» (پیام/آزمون) و دکمهٔ مدارس. */
    suspend fun loadSchoolOptions(): Result<List<CalendarAudienceOption>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("native_teacher_schools_v61")
            .decodeAs<kotlinx.serialization.json.JsonObject>()
        (raw["error"] as? JsonPrimitive)?.content?.takeIf(String::isNotBlank)?.let(::error)
        ((raw["items"] as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { element ->
            val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val name = (obj["name"] as? JsonPrimitive)?.content.orEmpty()
            val city = (obj["city"] as? JsonPrimitive)?.content.orEmpty()
            CalendarAudienceOption(id, name, city.takeIf(String::isNotBlank))
        }
    }

    suspend fun loadNote(noteId: String): Result<CalendarNote> = runCatching {
        val response = SupabaseProvider.client.postgrest.rpc(
            "cal_day",
            buildJsonObject { put("p_id", noteId) }
        ).decodeAs<CalendarNoteDto>()
        response.toDomain()
    }

    suspend fun save(editor: CalendarEditor): Result<String> = runCatching {
        validate(editor)
        val response = SupabaseProvider.client.postgrest.rpc(
            "cal_save_note",
            buildJsonObject {
                put("p_date", editor.date.toIsoDate())
                put("p_title", editor.title.trim())
                put("p_body", editor.body.trim().ifBlank { null })
                put("p_audience", editor.audience.wireValue)
                put("p_classes", JsonArray(editor.classIds.sorted().map(::JsonPrimitive)))
                put("p_students", JsonArray(editor.studentIds.sorted().map(::JsonPrimitive)))
                // V61.0 — مدرسه‌های مخاطب.
                put("p_schools", JsonArray(editor.schoolIds.sorted().map(::JsonPrimitive)))
                put("p_id", editor.id?.let(::JsonPrimitive) ?: JsonNull)
            }
        ).decodeAs<CalendarSaveResponseDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
        response.id ?: editor.id ?: error("شناسه پیام از سرور دریافت نشد.")
    }

    suspend fun delete(noteId: String): Result<Unit> = runCatching {
        val response = SupabaseProvider.client.postgrest.rpc(
            "cal_delete_note",
            buildJsonObject { put("p_id", noteId) }
        ).decodeAs<CalendarSaveResponseDto>()
        response.error?.takeIf(String::isNotBlank)?.let(::error)
    }

    /** V59.2 — پیام‌های دیده‌نشدهٔ دانش‌آموز (۱۴ روز اخیر) برای بنر «پیام جدید دارید». */
    suspend fun unseenNotes(): Result<List<CalendarNote>> = runCatching {
        val raw = SupabaseProvider.client.postgrest.rpc("cal_unseen_v59")
            .decodeAs<kotlinx.serialization.json.JsonObject>()
        (raw["error"] as? JsonPrimitive)?.content
            ?.takeIf(String::isNotBlank)?.let(::error)
        ((raw["notes"] as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { element ->
            val obj = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val date = (obj["on_date"] as? JsonPrimitive)?.content
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
            CalendarNote(
                id = id,
                date = date,
                title = (obj["title"] as? JsonPrimitive)?.content.orEmpty(),
                body = (obj["body"] as? JsonPrimitive)?.content.orEmpty()
            )
        }
    }

    /** V59.2 — علامت‌زدن پیام به عنوان دیده‌شده. */
    suspend fun markSeen(noteId: String): Result<Unit> = runCatching {
        SupabaseProvider.client.postgrest.rpc(
            "cal_mark_seen_v59",
            buildJsonObject { put("p_note", noteId) }
        ).decodeAs<kotlinx.serialization.json.JsonObject>()
        Unit
    }

    private fun validate(editor: CalendarEditor) {
        require(editor.title.trim().isNotEmpty()) { "عنوان پیام را وارد کنید." }
        require(editor.title.trim().length <= 120) { "عنوان پیام حداکثر ۱۲۰ نویسه است." }
        require(editor.body.trim().length <= 2000) { "توضیحات پیام حداکثر ۲۰۰۰ نویسه است." }
        if (editor.audience == CalendarAudience.CLASSES) {
            require(editor.classIds.isNotEmpty()) { "حداقل یک کلاس انتخاب کنید." }
        }
        if (editor.audience == CalendarAudience.STUDENTS) {
            require(editor.studentIds.isNotEmpty()) { "حداقل یک دانش‌آموز انتخاب کنید." }
        }
        if (editor.audience == CalendarAudience.SCHOOLS) {
            require(editor.schoolIds.isNotEmpty()) { "حداقل یک مدرسه انتخاب کنید." }
        }
    }
}

private fun CalendarNoteDto.toDomain(): CalendarNote {
    error?.takeIf(String::isNotBlank)?.let(::error)
    require(id.isNotBlank() && onDate.length >= 10) { "داده پیام تقویم ناقص است." }
    return CalendarNote(
    id = id,
    date = LocalDate.parse(onDate.take(10)),
    title = title,
    body = body.orEmpty(),
    audience = CalendarAudience.fromWire(audience),
    classIds = classes.toSet(),
        studentIds = students.toSet(),
        schoolIds = schools.toSet()
    )
}
