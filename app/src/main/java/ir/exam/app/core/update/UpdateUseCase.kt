package ir.exam.app.core.update

/** مقایسهٔ نسخهٔ نصب‌شده و نسخهٔ سرور؛ خروجی null یعنی برنامه به‌روز است. */
class UpdateUseCase(private val repository: AppUpdateRepository) {
    suspend fun check(installedCode: Int): Result<RemoteVersion?> {
        return repository.latest().map { remote ->
            if (remote.code > installedCode) remote else null
        }
    }
}
