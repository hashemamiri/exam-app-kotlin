package ir.exam.app.data.repository
import ir.exam.app.core.network.NetworkMonitor
import ir.exam.app.domain.model.*
import ir.exam.app.domain.repository.ExamRepository
/** لایهٔ مقاوم به اینترنت: در فاز بعد PendingAction برای ارسال پس‌زمینه افزوده می‌شود. */
class QueuedExamRepository(private val remote:ExamRepository,private val network:NetworkMonitor):ExamRepository{
 override suspend fun joinByCode(code:String)=remote.joinByCode(code)
 override suspend fun submitAttempt(attempt:SubmittedExam):Result<Unit>{
  if(!network.isOnline()) return Result.failure(IllegalStateException("اینترنت قطع است؛ پاسخ در پیش‌نویس محلی باقی می‌ماند."))
  return remote.submitAttempt(attempt)
 }
}
