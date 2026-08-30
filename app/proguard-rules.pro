# V66.13 — Release/R8 runtime entry points.

# Kotlin Serialization: keep generated serializers and serializable model members.
-keep class ir.exam.app.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ir.exam.app.** { *; }
-keepclassmembers class ir.exam.app.** {
    @kotlinx.serialization.SerialName <fields>;
}

# Room entities, DAOs, database and generated implementation classes.
-keep @androidx.room.Database class ir.exam.app.** { *; }
-keep @androidx.room.Entity class ir.exam.app.** { *; }
-keep @androidx.room.Dao interface ir.exam.app.** { *; }
-keep class ir.exam.app.**_Impl { *; }
-keep class ir.exam.app.**_Impl$* { *; }

# Methods called from JavaScript inside local WebViews.
-keepclassmembers class ir.exam.app.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve names used by reflective JSON/model diagnostics.
-keepnames class ir.exam.app.domain.model.**
-keepnames class ir.exam.app.data.dto.**

# Third-party optional platform classes may be absent on some Android devices.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**

# V70.0 — openPDF (فورک iText 5) برای پی دی اف مستقیم: کلاس‌هایش را حفظ کن و از
# وابستگی‌های اختیاری دسکتاپ (AWT و …) که روی اندروید وجود ندارند چشم‌پوشی کن.
-keep class com.lowagie.** { *; }
-dontwarn com.lowagie.**
-dontwarn java.awt.**
