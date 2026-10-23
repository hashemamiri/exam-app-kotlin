package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V334.0 (1.02.80) — اطمینان از حذف ۱۰۰٪ بخش‌های شلوغ از سازندهٔ آزمون، پنجرهٔ بانک سؤال و تنظیمات آزمون
 * در هر دو پلتفرم (اپلیکیشن بومی اندروید + وب‌سایت و باندل فعال تولیدی سایت) همراه با حفظ سازگاری عقب‌رو.
 */
class V334_0_BuilderCleanupTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private fun src(path: String) = File(root(), path).readText()

    private val forbiddenBuilderStrings = listOf(
        "بخش آزمون / صورت مشترک",
        "فصل یا مبحث",
        "همهٔ فصل‌ها/مباحث",
        "سطح دشواری",
        "سطح تعیین‌نشده",
        "همهٔ سطوح",
        "هدف یادگیری",
        "متن/مسئلهٔ مشترک",
        "متن مشترک / صورت چندبخشی",
        "زیرسؤال‌های مستقل",
        "زیرسؤال‌ها (صورت مشترک",
        "افزودن زیربخش",
        "افزودن گام اثبات",
        "حالت پاسخ",
        "حل مرحله‌ای",
        "اثبات با گام‌ها",
        "رسم/ساخت روی تخته",
        "Rubric دبیر",
        "sectionDurationsBox",
        "زمان مجاز هر بخش",
        "انتخاب تصادفی بر اساس مبحث/بخش"
    )

    @Test
    fun androidExamBuilderScreen_hasZeroRemovedControlsAnywhere() {
        val builderKt = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        forbiddenBuilderStrings.forEach { forbidden ->
            assertFalse("رشتهٔ حذف‌شده نباید در ExamBuilderScreen.kt باقی مانده باشد: $forbidden", forbidden in builderKt)
        }
        assertFalse("SecondaryQuestionControls" in builderKt)
        assertFalse("AssessmentPartsEditor" in builderKt)
        assertTrue("حالت ابتدایی باید دست‌نخورده باقی بماند", "PrimaryModeChooser" in builderKt)
    }

    @Test
    fun webExamBuilderAndCompiledBundle_haveZeroRemovedControlsAnywhere() {
        val builderJs = src("site/src/builder.js")
        forbiddenBuilderStrings.forEach { forbidden ->
            assertFalse("رشتهٔ حذف‌شده نباید در site/src/builder.js باقی مانده باشد: $forbidden", forbidden in builderJs)
        }
        val indexHtml = src("site/index.html")
        val match = Regex("""window\.__PANELS_URL\s*=\s*"/([^"]+)"""").find(indexHtml)
        assertNotNull("نشانی باندل فعال window.__PANELS_URL باید در site/index.html موجود باشد", match)
        val activePanelsFile = File(root(), "site/${match!!.groupValues[1]}")
        assertTrue("فایل باندل فعال ${activePanelsFile.name} باید موجود باشد", activePanelsFile.isFile)
        val bundleText = activePanelsFile.readText()
        forbiddenBuilderStrings.forEach { forbidden ->
            assertFalse("رشتهٔ حذف‌شده نباید در باندل فعال ${activePanelsFile.name} باقی مانده باشد: $forbidden", forbidden in bundleText)
        }
    }
}
