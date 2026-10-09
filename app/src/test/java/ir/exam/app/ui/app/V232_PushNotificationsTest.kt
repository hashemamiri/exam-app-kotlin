package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V232 — اعلان‌ها: بدون google-services.json اپ باید کامپایل شود و اعلان خاموش بماند؛ توکن فقط با RPC ثبت می‌شود؛
 * متن اعلان از سرور (push_outbox) می‌آید؛ هیچ کلیدی در مخزن نیست.
 */
class V232_PushNotificationsTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun buildIsOptionalWithoutGoogleServices() {
        val gradle = src("app/build.gradle.kts")
        assertTrue(gradle.contains("val hasGoogleServices = file(\"google-services.json\").exists()"))
        assertTrue(gradle.contains("if (hasGoogleServices) apply(plugin = \"com.google.gms.google-services\")"))
        assertTrue(gradle.contains("buildConfigField(\"Boolean\", \"PUSH_ENABLED\", \"\$pushEnabledDebug\")") && gradle.contains("buildConfigField(\"Boolean\", \"PUSH_ENABLED\", \"\$pushEnabledRelease\")"))
        assertTrue(gradle.contains("val pushEnabledDebug = \"ir.exam.app.native\" in googleServicesPackages") && gradle.contains("it.name.endsWith(\"GoogleServices\")"))
        assertTrue(gradle.contains("implementation(\"com.google.firebase:firebase-messaging\")"))
        assertFalse(File(root(), "app/google-services.json").exists())
        assertTrue(src(".gitignore").contains("app/google-services.json"))
        assertTrue(src(".github/workflows/android.yml").contains("GOOGLE_SERVICES_JSON_B64: \${{ secrets.GOOGLE_SERVICES_JSON_B64 }}"))
    }

    @Test
    fun registrarAndServiceContracts() {
        val reg = src("app/src/main/java/ir/exam/app/core/push/PushRegistrar.kt")
        assertTrue(reg.contains("val available: Boolean get() = BuildConfig.PUSH_ENABLED"))
        assertTrue(reg.contains("\"native_push_register_v1\"") && reg.contains("\"native_push_unregister_v1\""))
        assertTrue(reg.contains("put(\"p_platform\", JsonPrimitive(\"android\"))"))
        val svc = src("app/src/main/java/ir/exam/app/core/push/PushMessagingService.kt")
        assertTrue(svc.contains("const val CHANNEL_ID = \"exam_events\"") && svc.contains("if (!PushRegistrar.isEnabled(applicationContext)) return"))
        val manifest = src("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS") && manifest.contains(".core.push.PushMessagingService") && manifest.contains("firebase_analytics_collection_deactivated"))
        assertTrue(src("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").contains("ir.exam.app.core.push.PushRegistrar.sync(pushContext, user.id)"))
        assertTrue(src("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt").contains("Text(\"اعلان‌ها\")"))
    }

    @Test
    fun serverSideContracts() {
        val sql = src("supabase/migrations/20261009_native_push_notifications_v232.sql")
        listOf("create table if not exists public.push_tokens(", "create table if not exists public.push_outbox(", "native_push_register_v1(p_token text, p_platform text)",
            "push_on_answer_graded", "push_on_manager_request", "private.push_enqueue_exam_opens()", "cron.schedule('push_tick_v232'", "x-push-secret").forEach { assertTrue(it, sql.contains(it)) }
        val fn = src("supabase/functions/push-dispatch/index.ts")
        assertTrue(fn.contains("request.headers.get('x-push-secret') !== secret") && fn.contains("https://fcm.googleapis.com/v1/projects/") && fn.contains("channel_id: 'exam_events'"))
    }
}
