package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V28:
 * ۱) جابه‌جایی گزینه/جورکردنی مثل کارت سؤال
 * ۲) نبستن برنامه پس از انتخاب تصویر
 * ۳) پنجره گروهی هم‌اندازه پنجره تکی + اسکرول خودکار روی +
 * ۴) رشته تحصیلی واقعی در همه مسیرها
 */
class V28ReorderImageBulkFieldTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String): String = File(root(), path).readText()

    private val optionUi by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/QuestionOptionMedia.kt")
    }
    private val builder by lazy {
        source("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
    }
    private val school by lazy {
        source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt")
    }
    private val imageRepository by lazy {
        source("app/src/main/java/ir/exam/app/data/repository/LocalImageRepository.kt")
    }

    // ============================================================
    // ۱) جابه‌جایی گزینه‌ها
    // ============================================================

    @Test
    fun `option reorder uses the same long-press contract as question cards`() {
        // همان gesture کارت سؤال: لمس طولانی، آستانه تجمعی، مصرف رویداد.
        assertTrue("detectDragGesturesAfterLongPress" in optionUi)
        assertTrue("while (abs(accumulated) >= stepPx)" in optionUi)
        assertTrue("onMove(dragIndex, delta)" in optionUi)
        assertTrue("dragIndex = target" in optionUi)
        // index جاری باید دنبال شود تا بعد از recomposition مبدأ درست بماند.
        assertTrue("rememberUpdatedState(currentIndex)" in optionUi)
        assertTrue("rememberUpdatedState(itemCount)" in optionUi)
    }

    @Test
    fun `option drag gives haptics colour and list scrolling like question drag`() {
        assertTrue("LocalHapticFeedback" in optionUi)
        assertTrue("HapticFeedbackType.LongPress" in optionUi)
        assertTrue("HapticFeedbackType.TextHandleMove" in optionUi)
        assertTrue("animateColorAsState" in optionUi)
        assertTrue("onDragScroll(amount.y)" in optionUi)
        // کارت سؤال هم دقیقاً همین سه رفتار را دارد.
        assertTrue("onDragScroll(amount.y)" in builder)
    }

    @Test
    fun `builder locks list scrolling while an inner option drag is active`() {
        assertTrue("userScrollEnabled = !innerReorderActive" in builder)
        assertTrue("onItemDragStarted = { innerReorderActive = true }" in builder)
        assertTrue("onItemDragEnded = { innerReorderActive = false }" in builder)
    }

    @Test
    fun `multiple choice and both matching columns are wired to the shared drag`() {
        assertTrue("onDragStarted = onItemDragStarted" in builder)
        assertTrue("onItemDragScroll = onDragScroll" in builder)
        assertTrue("onDragStarted = onItemDragStarted" in optionUi)
        // شناسه پایدار باید همچنان مبنای جابه‌جایی گروه Compose باشد.
        assertTrue("key(optionId) {" in builder)
        assertTrue("key(itemId) {" in optionUi)
    }

    // ============================================================
    // ۲) تصویر
    // ============================================================

    @Test
    fun `image preparation never throws OutOfMemoryError out of the repository`() {
        // OutOfMemoryError یک Error است و runCatching آن را نمی‌گیرد؛
        // باید صریحاً گرفته و به پیام فارسی تبدیل شود.
        assertTrue("catch (oom: OutOfMemoryError)" in imageRepository)
        assertTrue("is OutOfMemoryError -> IllegalStateException" in imageRepository)
        assertTrue("recoverCatching" in imageRepository)
    }

    @Test
    fun `image decoding retries with a smaller budget instead of dying`() {
        assertTrue("MAX_ATTEMPTS" in imageRepository)
        assertTrue("while (attempt < MAX_ATTEMPTS)" in imageRepository)
        assertTrue("maxPixelsFor(attempt)" in imageRepository)
        assertTrue("maxEdgeFor(attempt)" in imageRepository)
        assertTrue("Bitmap.Config.RGB_565" in imageRepository)
    }

    @Test
    fun `decode budget follows real free memory and intermediates are recycled`() {
        assertTrue("runtime.maxMemory()" in imageRepository)
        assertTrue("runtime.totalMemory() - runtime.freeMemory()" in imageRepository)
        assertTrue("SAFETY_DIVISOR" in imageRepository)
        assertTrue("working?.recycle()" in imageRepository)
        assertTrue("if (cropped !== bitmap) bitmap.recycle()" in imageRepository)
        assertTrue("if (scaled !== bitmap) bitmap.recycle()" in imageRepository)
    }

    @Test
    fun `every picker still preprocesses before showing the editor`() {
        val media = source("app/src/main/java/ir/exam/app/ui/image/QuestionMediaEditor.kt")
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        listOf(media, optionUi, profile).forEach { text ->
            assertTrue("missing safe repository prepare", "prepare(ImageEditRequest(uri))" in text)
        }
        assertFalse("main picker still opens raw editor", "editQueue.firstOrNull" in media)
    }

    // ============================================================
    // ۳) پنجره گروهی
    // ============================================================

    private fun bulkSection(): String =
        school.substringAfter("private fun BulkStudentDialog(").substringBefore("internal fun studentClipboardText")

    @Test
    fun `bulk window matches the single student window geometry`() {
        val bulk = bulkSection()
        // پنجره تکی: عرض حداکثر ۶۲۰dp، padding افقی ۱۴ و عمودی ۱۰، از بالا.
        assertTrue("widthIn(max = 620.dp)" in bulk)
        assertTrue("padding(horizontal = 14.dp, vertical = 10.dp)" in bulk)
        assertTrue("contentAlignment = Alignment.TopCenter" in bulk)
        // دیگر کل ارتفاع را اشغال نمی‌کند.
        assertFalse("bulk window still forces full height", "height(maxHeight)" in bulk)
        assertTrue("heightIn(max = availableHeight)" in bulk)
    }

    @Test
    fun `bulk dialog shows one card and never grows with new rows`() {
        val bulk = bulkSection()
        // یک کارت در هر لحظه؛ ارتفاع با افزودن ردیف تغییر نمی‌کند.
        assertTrue("val row = rows[index]" in bulk)
        assertFalse("bulk window still grows its own list", "LazyColumn(" in bulk)
        assertFalse("list weight kept", "weight(1f, fill = false)" in bulk)
        assertTrue("Modifier.fillMaxWidth().padding(14.dp)" in bulk)
    }

    @Test
    fun `plus button replaces the visible card instead of growing the window`() {
        val bulk = bulkSection()
        assertTrue("activeIndex = rows.lastIndex" in bulk)
        // کارت تازه همان‌جا جایگزین کارت قبلی می‌شود و پنجره بزرگ نمی‌شود.
        assertTrue("LazyRow(" in bulk)
        assertTrue("rememberLazyListState()" in bulk)
        assertTrue("selected = activeIndex == index" in bulk)
        assertTrue("activeIndex = (index - 1).coerceAtLeast(0)" in bulk)
    }

    @Test
    fun `bulk dialog keeps ime resize behaviour`() {
        val bulk = bulkSection()
        assertTrue("SOFT_INPUT_ADJUST_RESIZE" in bulk)
        assertTrue("imePadding()" in bulk)
    }

    // ============================================================
    // ۴) رشته تحصیلی
    // ============================================================

    @Test
    fun `field of study exists in domain dto and repository paths`() {
        val schoolModels = source("app/src/main/java/ir/exam/app/domain/model/SchoolModels.kt")
        val schoolDtos = source("app/src/main/java/ir/exam/app/data/dto/SchoolDtos.kt")
        val schoolRepo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseSchoolRepository.kt")
        // مدل‌ها
        assertTrue(schoolModels.split("fieldOfStudy").size - 1 >= 4)
        // DTO با نام ستون واقعی
        assertTrue("@SerialName(\"field_of_study\")" in schoolDtos)
        // RPCهای جدید
        assertTrue("native_save_student_extra_v28" in schoolRepo)
        assertTrue("native_save_class_v28" in schoolRepo)
        assertTrue("native_my_classes_v28" in schoolRepo)
        assertTrue("put(\"p_field\"" in schoolRepo)
        // RPCهای قدیمی بدون رشته دیگر صدا زده نمی‌شوند
        assertFalse("legacy save_student_extra still used", "\"save_student_extra\"" in schoolRepo)
        assertFalse("legacy create_class still used", "\"create_class\"" in schoolRepo)
        assertFalse("legacy update_class still used", "\"update_class\"" in schoolRepo)
    }

    @Test
    fun `field of study is reachable in every student and class form`() {
        assertTrue("FieldOfStudyPicker" in school)
        // فرم تکی، فرم گروهی، کلاس و فیلتر اعضا
        assertTrue(school.split("FieldOfStudyPicker(").size - 1 >= 4)
        assertTrue("row.copy(field = it.take(100))" in school)
        assertTrue("student.fieldOfStudy" in school)
        assertTrue("item.fieldOfStudy" in school)
    }

    @Test
    fun `official exam header carries the field of study`() {
        val profile = source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt")
        val profileRepo = source("app/src/main/java/ir/exam/app/data/repository/SupabaseProfileRepository.kt")
        val print = source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt")
        val reportHelper = source("app/src/main/java/ir/exam/app/ui/reports/ReportPrintHelper.kt")
        assertTrue("FieldOfStudyPicker" in profile)
        assertTrue("onFieldOfStudy" in profile)
        assertTrue("native_save_profile_v28" in profileRepo)
        assertTrue("put(\"p_hdr_field\"" in profileRepo)
        assertTrue("رشته: " in print)
        assertTrue("fieldOfStudy = profile.header.fieldOfStudy" in reportHelper)
    }

    @Test
    fun `field of study reaches exports and backup version four`() {
        val portability = source("app/src/main/java/ir/exam/app/data/repository/SupabasePortabilityRepository.kt")
        assertTrue("native_export_backup_v3" in portability)
        assertTrue("native_restore_backup_v3" in portability)
        assertTrue("in 1..4" in portability)
        assertTrue("fieldOfStudy = profile.headerField.orEmpty()" in portability)
        // ستون رشته در خروجی اکسل دانش‌آموزان
        assertTrue("\"رشته\"" in school)
    }

    @Test
    fun `field picker reuses the grade wheel including the other option`() {
        val picker = source("app/src/main/java/ir/exam/app/ui/common/FieldOfStudyPicker.kt")
        val grade = source("app/src/main/java/ir/exam/app/ui/common/GradeOdometerPicker.kt")
        assertTrue("StandardFieldsOfStudy" in picker)
        assertTrue("ریاضی فیزیک" in picker)
        assertTrue("علوم تجربی" in picker)
        assertTrue("کاردانش" in picker)
        assertTrue("GradeOdometerPicker(" in picker)
        assertTrue("customLabel = \"سایر رشته\"" in picker)
        // چرخ مشترک باید پارامتری شده باشد ولی رفتار «سایر» پایه حفظ شود.
        assertTrue("standardValues: List<String> = StandardSchoolGrades" in grade)
        assertTrue("customLabel: String = \"سایر پایه\"" in grade)
        assertTrue("OtherGradeValue -> \"سایر\"" in grade)
    }

    // ============================================================
    // امنیت
    // ============================================================

    @Test
    fun `old password stays non recoverable after V28`() {
        val main = File(root(), "app/src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
        assertFalse(Regex("\\b(val|var)\\s+plain_password\\b").containsMatchIn(main))
        assertFalse("password retrieval API returned", "getPassword(" in main)
        assertTrue("رمز فعلی hash شده و قابل نمایش نیست" in main)
        assertTrue("copyOneTimeCredential" in main)
    }

    @Test
    fun `v28 migration is owner scoped and safeupdate compatible`() {
        val migration = source("supabase/migrations/20260814_native_field_of_study_v28.sql")
        assertTrue("field_of_study" in migration)
        assertTrue("hdr_field" in migration)
        // مالکیت
        assertTrue("teacher_id = v_uid" in migration)
        assertTrue("teacher_id = auth.uid()" in migration)
        // grant حداقلی
        assertTrue("from public, anon" in migration)
        assertTrue("to authenticated" in migration)
        // هیچ UPDATE/DELETE بدون WHERE
        val unsafe = Regex("(?is)\\b(update|delete)\\s+(from\\s+)?public\\.[a-z_]+(.*?);")
            .findAll(migration)
            .count { " where " !in it.value.lowercase() }
        assertTrue("unsafe DML found in V28 migration", unsafe == 0)
        assertFalse("migration references plain_password", "plain_password" in migration)
    }
}
