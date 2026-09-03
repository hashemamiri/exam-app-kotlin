package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V75.8 — خصوصی‌سازی باکت تصاویر (بند ۲.۱ گزارش امنیتی):
 * باکت exam-images دیگر عمومی نیست و خواندنِ هر تصویر طبق نقش و مالکیت انجام
 * می‌شود؛ برنامه برای نمایش تصویر، توکنِ نشست را همراه درخواست می‌فرستد.
 */
class V75_8PrivateStorageImagesTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(relative: String): String = File(root(), relative).readText()

    private val sql by lazy { source("supabase/migrations/20260903_native_storage_private_images_v75_8.sql") }
    private val interceptor by lazy { source("app/src/main/java/ir/exam/app/ui/image/SupabaseAuthImageInterceptor.kt") }
    private val loader by lazy { source("app/src/main/java/ir/exam/app/ui/image/PrivateImageLoader.kt") }
    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }

    @Test
    fun `bucket becomes private and the public read policy is dropped`() {
        assertTrue("update storage.buckets" in sql)
        assertTrue("set public = false" in sql)
        assertTrue("drop policy if exists v11_public_read_exam_images on storage.objects" in sql)
    }

    @Test
    fun `reading is limited by role and ownership`() {
        assertTrue("v75_8_read_question_images" in sql)
        assertTrue("v75_8_student_read_own_answers" in sql)
        assertTrue("v75_8_teacher_read_exam_answers" in sql)
        assertTrue("v75_8_read_avatars" in sql)
        assertTrue("to authenticated" in sql)
    }

    @Test
    fun `image requests carry the session token`() {
        assertTrue("class SupabaseAuthImageInterceptor : Interceptor" in interceptor)
        // V75.8.4: بدون هیچ $ - فقط هدرها
        assertTrue("Authorization" in interceptor)
        assertTrue("Bearer" in interceptor)
        assertTrue("addHeader(\"apikey\", BuildConfig.SUPABASE_ANON_KEY)" in interceptor)
    }

    @Test
    fun `the auth extension is imported explicitly`() {
        assertTrue("import io.github.jan.supabase.auth.auth" in interceptor)
        assertTrue("currentSessionOrNull()?.accessToken" in interceptor)
        assertTrue("runCatching" not in interceptor.substringAfter("class SupabaseAuthImageInterceptor"))
    }

    @Test
    fun `token is never sent to other hosts`() {
        assertTrue("/storage/v1/object/" in interceptor)
        assertTrue("isOwnStorageUrl" in interceptor)
    }

    @Test
    fun `the custom loader is installed at the app root`() {
        assertTrue("ImageLoader.Builder(context)" in loader)
        assertTrue("add(SupabaseAuthImageInterceptor())" in loader)
        assertTrue("LocalImageLoader provides imageLoader" in app)
    }
}
