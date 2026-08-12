package ir.exam.app.data.repository

/** نگاشت قطعی نام کاربری دانش‌آموز به ایمیل داخلی Auth؛ در UI نمایش داده نمی‌شود. */
internal object AuthIdentifier {
    private val usernamePattern = Regex("^[a-z0-9_]{4,20}$")
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private const val studentDomain = "student.exam.local"

    fun passwordLoginEmail(identifier: String): String {
        val clean = identifier.trim().lowercase()
        if ('@' in clean) return requireEmail(clean)
        require(usernamePattern.matches(clean)) {
            "نام کاربری دانش‌آموز باید ۴ تا ۲۰ حرف انگلیسی، عدد یا زیرخط باشد."
        }
        return "$clean@$studentDomain"
    }

    fun requireEmail(value: String): String {
        val clean = value.trim().lowercase()
        require(emailPattern.matches(clean)) { "ایمیل معتبر وارد کنید." }
        return clean
    }

    fun validUsername(value: String): Boolean = usernamePattern.matches(value.trim().lowercase())
}
