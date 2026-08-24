package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * رگرسیون V55.4 — بزرگ‌سازی پنجره‌های کتابخانهٔ ویرایشگر فرمول فقط داخل برنامه
 * (گزارش دستگاه پس از موفقیت V55.3: «پنجره کتابخانه‌ها کوچک است»؛ مثال کاربر:
 * منوی دسته‌های «اعداد و محاسبات»). انتخاب کاربر: بزرگ‌تر ولی نه تمام‌صفحه.
 * مثل nativePaintFix، سبک nativeLibrarySize فقط وقتی window.ExamEditorNative
 * موجود است تزریق می‌شود تا فایل مرجع در مرورگر عادی دست‌نخورده بماند.
 * پوشش: منوی شناور دسته‌ها (mbVar) + پنل مرکزی کتابخانهٔ نمادها + آیتم‌ها.
 * تست اجرایی jsdom: mbGroupLibrary('num') → ۲۲ دسته، انتخاب دسته → کتابخانهٔ
 * ۲۱ آیتمی با پنل، هر دو با سبک تزریقی — PASS.
 */
class V55_4LibrarySizeTest {
    private fun root(): File = listOf(File("."), File("..")).first {
        File(it, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").isFile
    }

    private val asset by lazy {
        File(root(), "app/src/main/assets/formula_editor/formula.html").readText()
    }

    @Test
    fun `library size boost is injected only inside the app`() {
        assertTrue("nativeLibrarySize" in asset)
        // همان الگوی nativePaintFix: بدون پل Native تزریق نمی‌شود.
        val block = asset.substringAfter("nativeLibrarySize()").substringBefore("})();")
        assertTrue("if (!window.ExamEditorNative) return;" in block)
    }

    @Test
    fun `category menu and symbol library get larger touch friendly sizes`() {
        // منوی شناور دسته‌ها (مثال کاربر: «اعداد و محاسبات»)
        assertTrue(".mb-var{min-width:min(340px,88vw) !important" in asset)
        assertTrue("min-height:48px !important" in asset)
        // پنل مرکزی کتابخانه — پهن‌تر و بلندتر ولی نه تمام‌صفحه
        assertTrue("width:min(96vw,720px) !important" in asset)
        assertTrue("max-height:92dvh !important" in asset)
        // آیتم‌های کتابخانه بزرگ‌تر
        assertTrue("min-height:52px !important" in asset)
    }

    @Test
    fun `reference sizes stay untouched for the plain browser`() {
        // قاعده‌های مرجع باید عیناً سر جایشان باشند (تغییر فقط با override تزریقی است).
        assertTrue("min-width: 220px; max-width: min(320px, 92vw); max-height: 62vh;" in asset)
        assertTrue("width: min(94vw, 640px);" in asset)
    }
}
