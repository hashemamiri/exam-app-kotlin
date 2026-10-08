package ir.exam.app.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V221 — حذف رسانه از سرور هنگام حذف در اپ/سایت؛ کارت «تماس با ما و نظرات»؛ یکی‌شدن کارت‌های ظاهر نئومورفیک. */
class V221_MediaCleanupContactAppearanceTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }
    private fun src(rel: String): String = File(root(), rel).readText()

    @Test
    fun edgeFunctionHasDeleteAction() {
        val fn = src("supabase/functions/media-upload/index.ts")
        assertTrue("body0.action === 'delete'" in fn)
        assertTrue("async function handleDelete(" in fn)
        assertTrue("const OWNED_FOLDERS = ['questions', 'option_images', 'matching_images', 'audio', 'answers', 'profiles'];" in fn)
        assertTrue(File(root(), "supabase/migrations/20261008_native_exam_media_urls_v221.sql").isFile)
    }

    @Test
    fun appCleanerDeletesOwnedMediaEverywhere() {
        val c = src("app/src/main/java/ir/exam/app/data/repository/StorageImageCleaner.kt")
        assertTrue("fun isOwnedMediaUrl(url: String): Boolean" in c)
        assertTrue("put(\"action\", \"delete\")" in c)
        val dash = src("app/src/main/java/ir/exam/app/data/repository/SupabaseTeacherDashboardRepository.kt")
        assertTrue("native_exam_media_urls_v221" in dash)
        val print = src("app/src/main/java/ir/exam/app/data/repository/SupabasePrintExamRepository.kt")
        assertTrue("StorageImageCleaner.removeByPublicUrls(" in print)
        val vm = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderViewModel.kt")
        assertTrue("val orphans = (persistedStorageUrls - nowUrls).toList()" in vm)
    }

    @Test
    fun siteDeletesMediaOnExamDeleteAndBuilderSave() {
        val app = src("site/src/app.js")
        assertTrue("async function deleteMedia(urls) {" in app)
        assertTrue("native_exam_media_urls_v221" in app)
        val b = src("site/src/builder.js")
        assertTrue("state.persistedMedia = (state.examId || state.printId) && S.mediaUrlsIn" in b)
        assertTrue("if (gone.length && S.deleteMedia) S.deleteMedia(gone);" in b)
    }

    @Test
    fun contactCardInAppAndSite() {
        val about = src("app/src/main/java/ir/exam/app/ui/update/AboutScreen.kt")
        assertTrue("const val SUPPORT_EMAIL = \"info@onlineexam.ir\"" in about)
        assertTrue("Text(\"تماس با ما و نظرات\"" in about)
        val m = src("site/src/mobile.js")
        assertTrue("el('h3', {text: 'تماس با ما و نظرات'})" in m)
        assertTrue("var mail = 'info@onlineexam.ir';" in m)
    }

    @Test
    fun neumorphicAppearanceIsOneCard() {
        val p = src("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        assertTrue("SettingsAccordionCard(title = \"ظاهر نئومورفیک\", expanded = expanded == \"ap7\"" in p)
        assertFalse("ظاهر نئومورفیک — " in p)
        val m = src("site/src/mobile.js")
        assertTrue("card('ظاهر نئومورفیک', [el('div', {class: 'm-pal-preview', text: 'پیش‌نمایش پالت'})" in m)
        assertFalse("ظاهر نئومورفیک — " in m)
    }
}
