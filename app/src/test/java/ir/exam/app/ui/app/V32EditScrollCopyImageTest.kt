package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V32:
 * ۱) آپلود تصویر: بودجهٔ decode از تلاش اول بیش از حد نیست و bitmapها روی هر
 *    مسیر (به‌ویژه OutOfMemoryError) بازیافت می‌شوند تا برنامه کشته نشود.
 * ۲) شمارهٔ کارت‌های پنجرهٔ گروهی به چپ/راست اسکرول می‌شوند و با اسکرول خودکار
 *    شمارهٔ کارت فعال نمایش داده می‌شود.
 * ۳) پنجرهٔ ویرایش دانش‌آموز دقیقاً مانند پنجرهٔ گروهی است: دکمه‌های انصراف/ذخیره،
 *    فیلدهای پیش‌پر و بدون عنوان «ویرایش دانش‌آموز».
 * ۴) دکمهٔ کپی رمز را از کادر رمز فعلی برمی‌دارد و اخطار
 *    «رمز قبلی در سامانه ذخیره نمی‌شود» نمایش داده نمی‌شود.
 */
class V32EditScrollCopyImageTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val uploader by lazy {
        source("app/src/main/java/ir/exam/app/data/repository/SupabaseQuestionImageUploader.kt")
    }
    private val school by lazy {
        source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
    }

    private fun bulkSection(): String =
        school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")

    private fun editSection(): String =
        school.substringAfter("private fun StudentEditDialog(").substringBefore("private data class BulkStudentDraft")

    // ============================================================
    // ۱) آپلود تصویر
    // ============================================================

    @Test
    fun `upload decode budget never exceeds the target on the first attempt`() {
        assertTrue("maxDimension shr attempt" in uploader)
        assertTrue("MIN_DECODE_EDGE = 640" in uploader)
        assertFalse("maxDimension * 2 shr attempt" in uploader)
    }

    @Test
    fun `upload recycles the bitmap on every path including OutOfMemoryError`() {
        assertTrue("finally {" in uploader)
        assertTrue("bitmap.recycle()" in uploader)
        assertTrue("current.recycle()" in uploader)
        assertTrue("catch (t: Throwable)" in uploader)
        assertTrue("while (attempt < MAX_ATTEMPTS)" in uploader)
    }

    // ============================================================
    // ۲) اسکرول شمارهٔ کارت‌ها در پنجرهٔ گروهی
    // ============================================================

    @Test
    fun `bulk card numbers scroll horizontally and auto-scroll to the active card`() {
        val bulk = bulkSection()
        assertTrue("LazyRow(" in bulk)
        assertTrue("rememberLazyListState()" in bulk)
        assertTrue("animateScrollToItem(activeIndex)" in bulk)
        assertTrue("LaunchedEffect(activeIndex, rows.size)" in bulk)
        assertFalse("rows.indices.chunked(6)" in bulk)
    }

    // ============================================================
    // ۳) پنجرهٔ ویرایش مانند پنجرهٔ گروهی
    // ============================================================

    @Test
    fun `edit dialog mirrors the bulk dialog layout`() {
        val edit = editSection()
        assertTrue("BoxWithConstraints" in edit)
        assertTrue("widthIn(max = 620.dp)" in edit)
        assertTrue("heightIn(max = availableHeight)" in edit)
        assertTrue("SOFT_INPUT_ADJUST_RESIZE" in edit)
        assertTrue("// نام و نام خانوادگی در یک سطر" in edit)
        assertTrue("// نام پدر و نام کاربری در یک سطر" in edit)
        assertTrue("// پایه و رشته در یک سطر" in edit)
        assertTrue("// رمز جدید اختیاری و رمز فعلی همین نشست در یک سطر" in edit)
    }

    @Test
    fun `edit dialog uses cancel and save instead of plus create and cross`() {
        val edit = editSection()
        assertTrue("Text(\"انصراف\")" in edit)
        assertTrue("Text(\"ذخیره\")" in edit)
        assertFalse("Text(\"ایجاد\")" in edit)
        assertFalse("Text(\"+\"" in edit)
        assertFalse("Text(\"×\"" in edit)
    }

    @Test
    fun `edit dialog fields are pre-filled and the title is removed`() {
        val edit = editSection()
        assertTrue("student.firstName.orEmpty()" in edit)
        assertTrue("student.username.orEmpty()" in edit)
        assertTrue("student.fatherName.orEmpty()" in edit)
        assertTrue("student.grade.orEmpty()" in edit)
        assertTrue("student.fieldOfStudy.orEmpty()" in edit)
        assertFalse("Text(\"ویرایش دانش‌آموز\"" in edit)
    }

    // ============================================================
    // ۴) دکمهٔ کپی
    // ============================================================

    @Test
    fun `copy button reads the current password and drops the old password warning`() {
        assertTrue("knownPasswordOf(student.username)" in school)
        assertTrue("knownPasswords[it.username.lowercase()]=it.password" in school)
        assertFalse("رمز قبلی در سامانه ذخیره نمی‌شود" in school)
        assertTrue("اطلاعات دانش‌آموز کپی شد." in school)
    }
}
