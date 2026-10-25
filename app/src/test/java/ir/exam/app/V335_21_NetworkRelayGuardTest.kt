package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V335.21 — نگهبان هم‌زیستی «مسیر جایگزین Supabase» با نگهبان‌های قدیمی‌تر:
 * ۱) کارت «تست اتصال به سرور» نباید از API منسوخ کلیپ‌بورد در Compose استفاده کند
 *    (وگرنه ممیزی صفر-هشدار V335.2/V335.3 دوباره CI را قرمز می‌کند).
 * ۲) خواندن رسانهٔ راه‌دور باید همچنان پیشوند Storage همین پروژه را بشناسد (پین V160).
 * ۳) میان‌گیر تصویر باید پیش از فرستادن توکن، مسیر Storage را بررسی کند (پین V75.8).
 */
class V335_21_NetworkRelayGuardTest {

    private fun root(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return dir ?: error("ریشهٔ مخزن پیدا نشد")
    }

    private fun source(relative: String): String = File(root(), relative).readText(Charsets.UTF_8)

    private val diagnostics by lazy { source("app/src/main/java/ir/exam/app/ui/profile/NetworkDiagnostics.kt") }
    private val media by lazy { source("app/src/main/java/ir/exam/app/core/media/RemoteMediaBytes.kt") }
    private val interceptor by lazy { source("app/src/main/java/ir/exam/app/ui/image/SupabaseAuthImageInterceptor.kt") }

    @Test
    fun `diagnostics card copies report with the platform clipboard api`() {
        assertFalse(
            "کارت تست اتصال نباید از API منسوخ کلیپ‌بورد در Compose استفاده کند",
            diagnostics.contains("LocalClipboardManager")
        )
        assertFalse(
            "استفاده از AnnotatedString برای کلیپ‌بورد لازم نیست",
            diagnostics.contains("AnnotatedString")
        )
        assertTrue(
            "کپی گزارش باید با ClipboardManager اندروید انجام شود",
            "Context.CLIPBOARD_SERVICE" in diagnostics
        )
        assertTrue(
            "متن گزارش اتصال باید در کلیپ‌بورد گذاشته شود",
            "setPrimaryClip(ClipData.newPlainText(" in diagnostics && "SupabaseRouter.report()" in diagnostics
        )
    }

    @Test
    fun `remote media still recognizes the project storage prefix`() {
        assertTrue(
            "بررسی پیشوند Storage همین پروژه باید باقی بماند",
            "url.startsWith(\"\$base/storage/v1/object/\")" in media
        )
        assertTrue(
            "مسیر جایگزین هم باید Storage همین پروژه شمرده شود",
            "SupabaseRouter.isOwnStorageUrl(url)" in media
        )
        assertTrue(
            "هدر نشست فقط برای Storage خودمان ساخته شود",
            "\"Authorization\" to \"Bearer \$token\", \"apikey\" to BuildConfig.SUPABASE_ANON_KEY" in media
        )
    }

    @Test
    fun `image interceptor keeps the storage guard before sending the token`() {
        // بررسی مسیر Storage باید در خودِ تابع intercept و پیش از خواندن نشست انجام شود.
        val callIndex = interceptor.indexOf("!isOwnStorageUrl(target)) return chain.proceed(request)")
        val tokenIndex = interceptor.indexOf("currentSessionOrNull()?.accessToken")
        assertTrue("فراخوانی بررسی مسیر Storage باید در میان‌گیر باشد", callIndex >= 0)
        assertTrue("توکن باید از نشست خوانده شود", tokenIndex >= 0)
        assertTrue("بررسی مسیر Storage باید پیش از خواندن توکن باشد", callIndex < tokenIndex)
        assertTrue(
            "تابع بررسی مسیر باید مسیر Storage را بشناسد",
            "value.contains(\"/storage/v1/object/\")" in interceptor
        )
        assertTrue(
            "مسیر جایگزین هم باید Storage همین پروژه شمرده شود",
            "SupabaseRouter.isOwnStorageUrl(value)" in interceptor
        )
    }

    @Test
    fun `relay router constants are still wired`() {
        val router = source("app/src/main/java/ir/exam/app/core/network/SupabaseRouter.kt")
        assertTrue("https://sb.onlineexam.ir" in router)
        assertTrue("/auth/v1/health" in router)
        assertTrue("return cached ?: directBase" in router)
        val app = source("app/src/main/java/ir/exam/app/ExamApplication.kt")
        assertTrue("SupabaseRouter.bootstrap(this)" in app)
    }
}
