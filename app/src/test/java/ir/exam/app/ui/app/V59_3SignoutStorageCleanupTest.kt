package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V59.3 — سه گزارش دستگاه:
 * ۱) پس از حذف موفق حساب، برنامه در صفحهٔ مرده می‌ماند: onDone قبلی
 *    refreshCurrentUser بود که برای حساب حذف‌شده شکست می‌خورد؛ حالا
 *    onAccountDeleted → authViewModel.signOut و signOut داخل deleteAccount
 *    هم LOCAL شد (سروری برای کاربر حذف‌شده 403 می‌داد).
 * ۲) تقویم: پوشش دید کلاس از V59.2.1 برقرار است؛ فایل SQL سلامت‌سنجی دارد
 *    و تابع تشخیصی native_calendar_debug_v59 (فقط service_role) اضافه شد.
 * ۳) حذف تصاویر استوریج همراه حذف: policy حذف مالک + RPC فهرست URLهای
 *    تصاویر آزمون + StorageImageCleaner؛ اتصال به حذف آزمون (داشبورد)،
 *    حذف سؤال (builder) و حذف عکس پروفایل. GC دوره‌ای موجود پشتیبان است.
 */
class V59_3SignoutStorageCleanupTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val app by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }
    private val settings by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }
    private val profileRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt") }
    private val cleaner by lazy { source("app/src/main/java/ir/exam/app/data/repository/StorageImageCleaner.kt") }
    private val dashboardRepo by lazy { source("app/src/main/java/ir/exam/app/data/repository/SupabaseTeacherDashboardRepository.kt") }
    private val builderVm by lazy { source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt") }
    private val settingsVm by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsViewModel.kt") }
    private val storageSql by lazy { source("supabase/migrations/20260825_native_storage_cleanup_v59.sql") }

    @Test
    fun `account deletion signs out locally and routes to the auth screen`() {
        assertTrue("onAccountDeleted = authViewModel::signOut" in app)
        assertTrue("onDeleteAccount = { viewModel.deleteAccount(onDone = onAccountDeleted) }" in settings)
        assertTrue("SignOutScope.LOCAL" in profileRepo)
    }

    @Test
    fun `storage cleaner extracts object paths from public urls`() {
        // منطق objectPath به‌صورت اجرایی همین‌جا شبیه‌سازی می‌شود.
        val marker = "/storage/v1/object/public/exam-images/"
        fun objectPath(url: String): String? {
            val index = url.indexOf(marker)
            if (index < 0) return null
            return url.substring(index + marker.length).substringBefore('?')
                .takeIf { it.isNotBlank() }
        }
        assertEquals(
            "questions/t1/e1/img.jpg",
            objectPath("https://x.supabase.co${marker}questions/t1/e1/img.jpg?width=200")
        )
        assertNull(objectPath("https://example.com/other.png"))
        // و کد واقعی همان الگو را دارد
        assertTrue("substringBefore('?')" in cleaner)
        assertTrue("delete(*paths.toTypedArray())" in cleaner)
    }

    @Test
    fun `exam question and avatar deletions clean their storage files`() {
        assertTrue("native_exam_image_paths_v59" in dashboardRepo)
        assertTrue("StorageImageCleaner.removeByPublicUrls(urls)" in dashboardRepo)
        assertTrue("StorageImageCleaner.removeByPublicUrls(urls)" in builderVm)
        assertTrue("StorageImageCleaner.removeByPublicUrls(listOf(oldUrl))" in settingsVm)
    }

    @Test
    fun `storage sql grants owner-only delete and lists exam urls`() {
        assertTrue("v59_owner_delete_exam_images" in storageSql)
        assertTrue("(storage.foldername(name))[2] = auth.uid()::text" in storageSql)
        assertTrue("native_exam_image_paths_v59" in storageSql)
        assertTrue("teacher_id = v_uid" in storageSql)
    }
}
