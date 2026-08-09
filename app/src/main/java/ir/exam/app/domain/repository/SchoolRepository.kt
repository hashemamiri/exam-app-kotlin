package ir.exam.app.domain.repository
import ir.exam.app.domain.model.*
import kotlinx.coroutines.flow.Flow
interface SchoolRepository { fun observeClasses():Flow<List<SchoolClass>>; suspend fun saveClass(item:SchoolClass):Result<Unit>; suspend fun deleteClass(id:String):Result<Unit>; fun observeCalendar(month:String):Flow<List<CalendarEvent>>; suspend fun saveEvent(item:CalendarEvent):Result<Unit> }
