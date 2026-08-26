package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V61.7 — شش اصلاح درخواستی:
 * ۱) کارت داده‌های مدیر: پشتیبان‌گیری واقعی (native_manager_export_backup_v61).
 * ۲) پنجرهٔ +: چهار کارت ضربدری در چهار گوشهٔ مربع فرضی، هر چهار خط‌چین به مرکز.
 * ۳) کد دعوت استفاده‌شده: زمان‌سنج «منجمد» در لحظهٔ استفاده نمایش داده می‌شود.
 * ۴) حذف کارت دعوت: دیالوگ تأیید قبل از حذف.
 * ۵) فیلتر دانش‌آموزان: هر بخش یک کارت بازشونده، آیکن قرمز بخش فعال، دکمه‌ها
 *    یک سطر وسط‌چین بالا، «حذف فیلترها» بدون بستن پنجره.
 * ۶) دکمه‌های منوی + سازنده: مربع با گوشه‌های گرد (نه دایره).
 */
class V61_7BackupCrossFilterCardsTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private fun source(path: String) = File(root(), path).readText()

    private val profile by lazy { source("app/src/main/java/ir/exam/app/ui/profile/ProfileSettingsScreen.kt") }
    private val add by lazy { source("app/src/main/java/ir/exam/app/ui/app/Design69QuickAddOverlay.kt") }
    private val manager by lazy { source("app/src/main/java/ir/exam/app/ui/manager/ManagerFoundationScreens.kt") }
    private val school by lazy { source("app/src/main/java/ir/exam/app/ui/classes/SchoolManagementScreen.kt") }
    private val radial by lazy { source("app/src/main/java/ir/exam/app/ui/builder/BuilderRadialMenuOverlay.kt") }
    private val migration by lazy { source("supabase/migrations/20260826_native_manager_backup_invite_freeze_v61_7.sql") }

    @Test
    fun `manager data card has a real backup flow`() {
        assertTrue("private fun ManagerBackupSection()" in profile)
        assertTrue("native_manager_export_backup_v61" in profile)
        assertTrue("school-backup.json" in profile)
        // سرور: بدون رمز/توکن؛ فقط نقش مدیر
        assertTrue("native_manager_export_backup_v61" in migration)
        assertTrue("فقط مدیر/معاون دسترسی دارد" in migration)
    }

    @Test
    fun `quick add is a cross with four dashed corner lines`() {
        // چهار خط‌چین از مرکز به گوشه‌ها (حلقهٔ forEach روی چهار Offset)
        val canvas = add.substringAfter("// V61.7 — چیدمان ضربدری")
            .substringBefore("QuickAddAction(")
        assertTrue(canvas.split("androidx.compose.ui.geometry.Offset(").size - 1 >= 5)
        assertTrue(".forEach { corner ->" in canvas)
        // چهار کارت در چهار گوشه: -corner/-corner و +corner/+corner
        assertTrue("targetY = -cornerY" in add && "targetY = cornerY" in add)
        assertTrue("targetX = horizontal" in add && "targetX = -horizontal" in add)
    }

    @Test
    fun `used invite shows the frozen timer and deletion asks first`() {
        assertTrue("زمان‌سنج متوقف شد: %02d:%02d:%02d" in manager)
        assertTrue("usedAt" in manager)
        assertTrue("'used_at',coalesce(i.used_at::text,'')" in migration)
        // تأیید قبل از حذف
        assertTrue("deleteInviteTarget = invite" in manager)
        assertTrue("حذف کارت کد دعوت" in manager)
        assertTrue("بله، حذف شود" in manager)
        assertTrue("این کد استفاده نشده و با حذف، بلافاصله منقضی می‌شود." in manager)
    }

    @Test
    fun `filter dialog has section cards top buttons and sticky clear`() {
        assertTrue("fun FilterSectionCard(" in school)
        // آیکن قرمز بخش فعال
        assertTrue("tint = if (active) Color(0xFFD32F2F) else LocalContentColor.current" in school)
        // دکمه‌ها یک سطر وسط‌چین در title (بدون هدر متنی)
        val dialog = school.substringAfter("private fun StudentFilterDialog(")
            .substringBefore("private fun StudentCard(")
        assertFalse("Text(\"فیلتر دانش‌آموزان\")" in dialog)
        val header = dialog.substringAfter("title = {").substringBefore("text = {")
        assertTrue("حذف فیلترها" in header && "اعمال فیلتر" in header && "انصراف" in header)
        assertTrue("Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)" in header)
        // حذف فیلترها فقط draft را پاک می‌کند (بدون onApply/onDismiss)
        assertTrue("TextButton(onClick = { draft = StudentListFilter() }) { Text(\"حذف فیلترها\") }" in dialog)
        // شش بخش کارت
        for (key in listOf("\"grade\"", "\"class\"", "\"gender\"", "\"unassigned\"", "\"school\"", "\"teacher\"")) {
            assertTrue(key, "key = $key" in dialog)
        }
    }

    @Test
    fun `builder radial buttons are rounded squares`() {
        // V61.8 — کلیپ داخل graphicsLayer تا کل انیمیشن مربع گوشه‌گرد بماند.
        val action = radial.substringAfter("actions.forEachIndexed").substringBefore("val startX")
        assertTrue("shape = RoundedCornerShape(22.dp)" in action)
        assertTrue("clip = true" in action)
        assertFalse(".clip(CircleShape)" in action)
    }
}
