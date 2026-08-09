package ir.exam.app.core.update
/** مقایسهٔ نسخه قابل تست؛ دریافت و نصب در Android installer جدا انجام می‌شود. */
class UpdateUseCase(private val repo:AppUpdateRepository){suspend fun check(installedCode:Int):Result<RemoteVersion?>=repo.latest().map{if(it.code>installedCode)it else null}}
