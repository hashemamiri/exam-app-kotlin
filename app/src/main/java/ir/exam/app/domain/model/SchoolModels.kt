package ir.exam.app.domain.model
import java.time.LocalDate
data class SchoolClass(val id:String,val title:String,val teacherId:String,val studentIds:List<String> = emptyList())
data class StudentProfile(val id:String,val fullName:String,val username:String?,val classId:String?,val active:Boolean=true)
data class CalendarEvent(val id:String,val title:String,val date:LocalDate,val message:String?,val createdBy:String)
data class Wallet(val userId:String,val balanceRials:Long,val updatedAt:Long)
data class Subscription(val userId:String,val plan:String,val expiresAt:Long?,val active:Boolean)
