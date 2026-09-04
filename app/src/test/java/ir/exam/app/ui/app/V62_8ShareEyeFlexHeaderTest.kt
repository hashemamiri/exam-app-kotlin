package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V62.8 — هفت درخواست کاربر:
 * ۱) اشتراک با مدیر با «آیکن چشم» روی کارت کلاس و کارت دانش‌آموز + پیام.
 * ۲) + کلاس معلم در پنل مدیر: «افزودن جدید» همان فرم پنل معلم؛ ساخته‌شده به
 *    همان کلاس و لیست دانش‌آموزان اضافه شود و roster کلاس نمایش بماند.
 * ۳) چیپ‌های نام مدرسه در کد دعوت منعطف و وسط‌چین (FlowRow).
 * ۴) + داک مدیر: انتخاب معلم/کلاس «اختیاری»؛ بدون انتخاب هم ساخت مجاز؛
 *    در پایان لیست اعضای همان کلاس نمایش داده شود.
 * ۵) + کنار جستجوی مدیر: مستقیم فرم ایجاد؛ بدون کادر و پس‌زمینه.
 * ۶) فونت سربرگ چاپ B Nazanin (با جایگزین امن)؛ تاریخ بدون ساعت؛
 *    «مدت آزمون: N دقیقه».
 * ۷) پنجره‌های بلند (سربرگ و مشابه) با کیبورد بالا کشیده و اسکرول شوند.
 */
class V62_8ShareEyeFlexHeaderTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val classesVm by lazy { source("app/src/main/java/ir/exam/app/ui/classes/ClassesViewModel.kt") }
    private val teacherClass by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerTeacherClassScreen.kt") }
    private val foundation by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val printCenter by lazy { source("app/src/main/java/ir/exam/app/ui/printing/ExamPrintCenterScreen.kt") }
    private val pdfAdapter by lazy { source("app/src/main/java/ir/exam/app/core/printing/OfficialPdfPrintAdapter.kt") }
    private val appShell by lazy { source("app/src/main/java/ir/exam/app/ui/app/ExamApp.kt") }

    @Test
    fun `sharing uses eye icons with on-screen messages`() {
        // چشم کلاس و دانش‌آموز + پیام‌های ViewModel
        assertTrue("نمایش کلاس به مدیر" in school)
        assertTrue("نمایش دانش‌آموز به مدیر" in school)
        assertTrue("fun setStudentShared(id: String, shared: Boolean)" in classesVm)
        assertTrue("دانش‌آموز برای مدیر قابل مشاهده شد." in classesVm)
        assertTrue("کلاس برای مدیر قابل مشاهده شد." in classesVm)
        // مدل/DTO ستون اشتراک دانش‌آموز را می‌خوانند
        assertTrue("sharedWithManager" in source("app/src/main/java/ir/exam/app/domain/model/SchoolModels.kt"))
        assertTrue("shared_with_manager" in source("app/src/main/java/ir/exam/app/data/dto/SchoolDtos.kt"))
        // سوییچ قدیمی حذف شد
        assertFalse("Switch(" in school)
    }

    @Test
    fun `manager class plus opens the teacher form and keeps the roster visible`() {
        // افزودن جدید داخل کلاس: همان فرم پنل معلم (پوستهٔ عمومی)
        assertTrue("fun ManagerStudentCreateDialog(" in school)
        assertTrue("ManagerStudentCreateDialog(" in teacherClass)
        // پس از ساخت: عضویت در همین کلاس + roster تازه؛ لیست دانش‌آموزان باز نمی‌شود
        assertTrue("onCreateStudents(requests){created->" in teacherClass)
        assertTrue("repo.setClassStudent(selected!!.id,studentId,true)" in teacherClass)
        assertTrue("loadRoster(selected!!)" in teacherClass)
        assertTrue("onCreateStudents = { requests, onCreated ->" in appShell)
    }

    @Test
    fun `invite school chips are flexible and centered`() {
        val dialog = foundation.substringAfter("معلم به کدام مدرسه بپیوندد؟")
            .substringBefore("confirmButton")
        assertTrue("FlowRow(" in dialog)
        assertTrue("Alignment.CenterHorizontally" in dialog)
        // چیپ تمام‌عرض قدیمی حذف شد (چیپ داخل FlowRow بدون modifier است)
        val chips = dialog.substringAfter("FlowRow(")
        assertFalse("label = { Text(school.name.ifBlank { \"مدرسه\" }) },\n                                modifier = Modifier.fillMaxWidth()" in chips)
    }

    @Test
    fun `manager dock plus makes teacher and class optional and shows the roster`() {
        // ساخت بدون انتخاب کلاس مجاز است
        assertTrue("Text(\n                        if (managerCreateClassId != null) \"ادامه و ساخت دانش‌آموز\"\n                        else \"ساخت بدون کلاس\"\n                    )" in school)
        // پس از ساخت با کلاس، لیست اعضای همان کلاس باز می‌شود
        assertTrue("managerCreatedRoster=viewModel.managerClassRoster(target)" in school)
        // V62.8.1 — پنجرهٔ لیست اعضا اسکرول دارد و importهای آن حاضرند (رفع خطای CI).
        assertTrue("import androidx.compose.foundation.verticalScroll" in school)
        assertTrue("import androidx.compose.foundation.rememberScrollState" in school)
        assertTrue("suspend fun managerClassRoster(classId: String)" in classesVm)
        assertTrue("دانش‌آموزان کلاس \${className.ifBlank { \"\" }}" in school)
        // + کنار جستجو: مستقیم فرم و بدون کادر (IconButton به‌جای Button)
        assertTrue("onBulk = { managerCreateClassId = null; showBulk = true }" in school)
        val toolbar = school.substringAfter("OutlinedButton(onClick = onExport) { Text(\"Excel\") }")
            .substringBefore("if (!searchOpen)")
        // + حالا IconButton بدون کادر است (نه Button با پس‌زمینه)
        assertTrue("IconButton(\n                onClick = onBulk" in toolbar)
        assertFalse("            Button(\n                onClick = onBulk" in toolbar)
    }

    @Test
    fun `print header uses nazanin date-only and minute suffix`() {
        // فونت B Nazanin با جایگزین امن وزیرمتن
        assertTrue("fonts/bnazanin.ttf" in pdfAdapter)
        assertTrue("R.font.vazirmatn_regular" in pdfAdapter)
        // مدت با پسوند «دقیقه» در خود PDF
        assertTrue("\$it دقیقه" in pdfAdapter)
        // V76.0 — پنجرهٔ سربرگ بومی از صفحهٔ چاپ حذف شد؛ تاریخ/مدت داخل نسخهٔ 30 و PDF است
        assertFalse("jalaliDisplay(it).substringBefore(\" \")" in printCenter)
    }
}
