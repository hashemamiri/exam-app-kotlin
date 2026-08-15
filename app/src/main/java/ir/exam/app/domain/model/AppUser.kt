package ir.exam.app.domain.model

enum class UserRole { TEACHER, STUDENT, MANAGER }

data class AppUser(
    val id: String,
    val name: String,
    val email: String?,
    val role: UserRole,
    val avatarUrl: String? = null,
    val username: String? = null,
    /** حساب ایمیلی تأیید شده ولی ثبت‌نام staff هنوز کامل نشده است. */
    val requiresTeacherSetup: Boolean = false,
    /** نقش انتخاب‌شده پیش از OTP برای ادامهٔ صحیح setup پس از بازشدن دوباره. */
    val pendingRegistrationRole: UserRole? = null
)
