package ir.exam.app.core.security

/**
 * V335.80 — پرچم‌های «نگه‌داری نشست».
 *
 * قاعدهٔ محصول: نشست فقط با **درخواست کاربر** (دکمهٔ «خروج» یا حذف حساب) پاک می‌شود.
 * پلاگین Supabase Auth هر زمان تازه‌سازی ناموفق باشد `deleteSession()` را صدا می‌زند —
 * این پرچم‌ها اجازه می‌دهند «حذف به‌خواست کاربر» از «حذف خودکار» جدا شود.
 */
object SessionKeeper {
    /** فقط در جریان خروج کاربر (یا حذف حساب) true می‌شود. */
    @Volatile
    var userInitiatedLogout: Boolean = false

    /** آیا روی این دستگاه نشستی وجود دارد؟ (برای اینکه تعویض مسیر شبکه آن را نابود نکند) */
    @Volatile
    var sessionPresent: Boolean = false

    /** آخرین بار کتابخانه نشست را «باطل» دید؛ فقط برای گزارش، مانع حذف نمی‌شود. */
    @Volatile
    var lastAutoInvalidation: Long = 0L
}
