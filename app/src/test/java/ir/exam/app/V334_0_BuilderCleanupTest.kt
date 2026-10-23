package ir.exam.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V334.0 .. V334.3 (1.02.83) — اطمینان از:
 * ۱) حذف ۱۰۰٪ بخش‌های شلوغ از سازندهٔ آزمون، پنجرهٔ بانک سؤال و تنظیمات آزمون در هر دو پلتفرم،
 * ۲) قرارگیری بلافاصلهٔ بخش اختصاصی هر نوع سؤال (گزینه‌ها، ستون جورکردنی، درست/نادرست، پاسخ کوتاه/عددی و ...)
 *    زیر نوار ابزارهای سؤال و بالاتر از تنظیمات تکمیلی،
 * ۳) تفکیک کامل اجزای «آزمون آنلاین» و «آزمون چاپی» در وب‌سایت هم‌تراز با اپ اندروید:
 *    - در آزمون آنلاین: بدون «قالب متن سؤال» و بدون «فضای پاسخ در برگهٔ چاپی»؛ بارم فشرده در سربرگ کارت سؤال.
 *    - در آزمون چاپی: بدون «سؤال تشریحی به‌صورت دستی تصحیح می‌شود.»، بدون «صوت سؤال» و بدون «پاسخ تصویری دانش‌آموز».
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
    fun androidExamBuilderScreen_hasZeroRemovedControlsAndTypeSectionRightUnderTools() {
        val builderKt = src("app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt")
        forbiddenBuilderStrings.forEach { forbidden ->
            assertFalse("رشتهٔ حذف‌شده نباید در ExamBuilderScreen.kt باقی مانده باشد: $forbidden", forbidden in builderKt)
        }
        assertFalse("SecondaryQuestionControls" in builderKt)
        assertFalse("AssessmentPartsEditor" in builderKt)
        assertTrue("حالت ابتدایی باید دست‌نخورده باقی بماند", "PrimaryModeChooser" in builderKt)

        val toolsIdx = builderKt.indexOf("QuestionMediaEditor(")
        val typeIdx = builderKt.indexOf("when (question.type) {")
        val printSpaceIdx = builderKt.indexOf("PrintAnswerSpaceControls(question = question")
        val imgAnswerIdx = builderKt.indexOf("Text(\"تصویر پاسخ دانش‌آموز\")")
        assertTrue("نوار ابزار و بخش نوع سؤال باید یافت شوند", toolsIdx > 0 && typeIdx > 0)
        assertTrue("بخش نوع سؤال باید زیر نوار ابزارها باشد", toolsIdx < typeIdx)
        assertTrue("فضای پاسخ چاپی باید پایین‌تر از بخش اختصاصی نوع سؤال باشد", typeIdx < printSpaceIdx)
        assertTrue("تصویر پاسخ دانش‌آموز باید پایین‌تر از بخش اختصاصی نوع سؤال باشد", typeIdx < imgAnswerIdx)
    }

    @Test
    fun webExamBuilderAndCompiledBundle_separateOnlineAndPrintCleanly() {
        val builderJs = src("site/src/builder.js")
        forbiddenBuilderStrings.forEach { forbidden ->
            assertFalse("رشتهٔ حذف‌شده نباید در site/src/builder.js باقی مانده باشد: $forbidden", forbidden in builderJs)
        }
        assertTrue("بارم فشرده در سربرگ کارت سؤال باید موجود باشد", "class: 'field b-qscore'" in builderJs)
        assertTrue("صوت سؤال فقط در آزمون آنلاین نمایش داده شود", "state.mode !== 'print' && window.SiteExtras" in builderJs)
        assertTrue("توضیح تصحیح دستی تشریحی فقط در حالت آنلاین باشد", "} else if (state.mode !== 'print') {" in builderJs)

        val toolsIdx = builderJs.indexOf("class: 'b-tools'")
        val typeIdx = builderJs.indexOf("editor.appendChild(typeSection(q));")
        val printGuardIdx = builderJs.indexOf("if (state.mode === 'print') {", typeIdx)
        val fmtIdx = builderJs.indexOf("'b-sec-fmt'", printGuardIdx)
        val printSpaceIdx = builderJs.indexOf("sec('فضای پاسخ در برگهٔ چاپی'", fmtIdx)
        val onlineImgIdx = builderJs.indexOf("if (state.mode === 'online' &&", printSpaceIdx)
        assertTrue("بخش نوع سؤال باید زیر نوار ابزارهای وب باشد", toolsIdx in 1 until typeIdx)
        assertTrue("بخش‌های چاپی باید داخل شرط state.mode === 'print' و زیر بخش نوع سؤال باشند",
            typeIdx < printGuardIdx && printGuardIdx < fmtIdx && fmtIdx < printSpaceIdx && printSpaceIdx < onlineImgIdx)

        val indexHtml = src("site/index.html")
        val match = Regex("""window\.__PANELS_URL\s*=\s*"/([^"]+)"""").find(indexHtml)
        assertNotNull("نشانی باندل فعال window.__PANELS_URL باید در site/index.html موجود باشد", match)
        val activePanelsFile = File(root(), "site/${match!!.groupValues[1]}")
        assertTrue("فایل باندل فعال ${activePanelsFile.name} باید موجود باشد", activePanelsFile.isFile)
        val bundleText = activePanelsFile.readText()
        forbiddenBuilderStrings.forEach { forbidden ->
            assertFalse("رشتهٔ حذف‌شده نباید در باندل فعال ${activePanelsFile.name} باقی مانده باشد: $forbidden", forbidden in bundleText)
        }
        assertTrue("b-qscore" in bundleText)
        val bundleTypeIdx = bundleText.indexOf("editor.appendChild(typeSection(q))")
        val bundleFmtIdx = bundleText.indexOf("'b-sec-fmt'")
        assertTrue("در باندل کامپایل‌شده نیز typeSection باید قبل از b-sec-fmt باشد", bundleTypeIdx in 1 until bundleFmtIdx)
    }
}
