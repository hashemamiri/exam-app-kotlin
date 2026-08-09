package ir.exam.app.ui.calendar
import androidx.lifecycle.ViewModel
import ir.exam.app.domain.model.CalendarEvent
import kotlinx.coroutines.flow.*
data class CalendarState(val month:String="",val events:List<CalendarEvent> = emptyList(),val selectedDate:String?=null)
/** جریان تقویم آموزشی، رویداد و پیام معلم. */
class CalendarViewModel:ViewModel(){private val _state=MutableStateFlow(CalendarState());val state=_state.asStateFlow();fun showMonth(month:String,events:List<CalendarEvent>){_state.value=CalendarState(month,events)};fun selectDate(date:String){_state.update{it.copy(selectedDate=date)}}}
