package ir.exam.app

import ir.exam.app.core.figure.AXIS_FIGURES
import ir.exam.app.core.figure.PrimaryMathSvgRenderer
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.data.repository.StudentExamPayloadCodec
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.ui.builder.PrimaryBank
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V314.0 / V314.1 — بستهٔ ۵گانهٔ ارتقای دورهٔ ابتدایی در اندروید:
 * ۱) گونه‌های تعاملی جدید: بخش‌کردن و صداکشی (syllable) و دسته‌بندی در سبد (bucket)
 * ۲) ۶ ابزار ریاضی/علوم ابتدایی جدید: چرتکه (axabacus)، محور پرش (axjump)، ترازو (axbalance)، دماسنج (axthermo)، جدول شگفت‌انگیز (axsaw)، الگویابی (axpattern)
 * ۳) بانک آمادهٔ غنی‌شدهٔ ابتدایی (۱۱۴ سؤال در ۹ درس رسمی پایه‌های ۱ تا ۶ + جستجو و افزودن گروهی)
 * ۴) مهرهای تشویقی معلم و کارنامهٔ توصیفی به تفکیک درس
 * ۵) تعامل لمسی مستقیم روی شکل‌های ابتدایی (axfrac، axabacus، axthermo کنار axclock)
 */
class V314_PrimarySuiteTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun category1_syllableAndBucketCodecRoundTrip() {
        val drafts = listOf(
            QuestionDraft(
                id = "q_syl",
                type = QuestionType.FILL_BLANK,
                text = "کلمهٔ «مدرسه» را بخش کن:",
                mode = "syllable",
                expectedText = "مد - ر - سه | مد-ر-سه"
            ),
            QuestionDraft(
                id = "q_bkt",
                type = QuestionType.FILL_BLANK,
                text = "جانوران را در سبد درست قرار بده:",
                mode = "bucket",
                expectedText = "پرنده: کبوتر، گنجشک | پستاندار: شیر، گربه"
            )
        )
        val encoded = ExamQuestionCodec.encode(drafts)
        val decodedDrafts = ExamQuestionCodec.decode(
            encoded.publicQuestions,
            encoded.answerKey
        )
        assertEquals("syllable", decodedDrafts[0].mode)
        assertEquals("bucket", decodedDrafts[1].mode)

        val rawExam = """{"id":"ex1","title":"آزمون ابتدایی","options":{"primary":true},"questions":${encoded.publicQuestions}}"""
        val studentExam = StudentExamPayloadCodec.decodeFresh(StudentExamPayloadCodec.parse(rawExam), studentId = "st1", receivedAtEpochMs = 1000L)
        val qSyl = studentExam.questions[0] as FillBlankQuestion
        val qBkt = studentExam.questions[1] as FillBlankQuestion
        assertEquals(3, qSyl.syllableCount)
        assertEquals(listOf("پرنده", "پستاندار"), qBkt.buckets)
        assertEquals(4, qBkt.bucketItems.size)
        assertEquals(
            "پستاندار:شیر+گربه|پرنده:کبوتر+گنجشک".split("|").sorted(),
            ExamQuestionCodec.bucketCanonicalAnswer(ExamQuestionCodec.parseBucketSpec(drafts[1].expectedText)).split("|").sorted()
        )
    }

    @Test
    fun category2_sixNewPrimaryMathFiguresRenderValidSvg() {
        val ids = AXIS_FIGURES.map { it.id }.toSet()
        val allNew = listOf(
            "axabacus", "axjump", "axbalance", "axthermo", "axsaw", "axpattern",
            "axtenframe", "axsym", "axspin", "axbeaker", "axgeoboard", "axdice", "axmularea", "axvenn"
        )
        for (k in allNew) {
            assertTrue("Missing $k in AXIS_FIGURES", k in ids)
            assertTrue("Missing $k in PrimaryMathSvgRenderer.SUPPORTED", k in PrimaryMathSvgRenderer.SUPPORTED)
            val tpl = AXIS_FIGURES.first { it.id == k }
            val body = PrimaryMathSvgRenderer.body(tpl.toSpec())
            assertTrue("Empty SVG body for $k", body.isNotBlank())
        }
    }

    @Test
    fun category3_expandedPrimaryBankHasAllSubjectsAndGrades() {
        val raw = File(root(), "app/src/main/assets/${PrimaryBank.ASSET}").readText()
        val base = PrimaryBank.parse(raw)
        val all = PrimaryBank.parseAll(raw)
        assertEquals(30, base.size)
        assertTrue("Expected at least 110 items in parseAll, got ${all.size}", all.size >= 110)
        assertEquals((1..6).toSet(), all.map { it.grade }.toSet())
        assertTrue(all.map { it.subject }.toSet().size >= 9)
        val dlg = File(root(), "app/src/main/java/ir/exam/app/ui/builder/PrimaryBankDialog.kt").readText()
        assertTrue(dlg.contains("PrimaryBank.parseAll"))
        assertTrue(dlg.contains("➕ افزودن همه"))
    }

    @Test
    fun category4_and_5_stickersDescriptiveReportAndInteractiveFigures() {
        val grading = File(root(), "app/src/main/java/ir/exam/app/ui/grading/GradingScreen.kt").readText()
        assertTrue(grading.contains("🌟 آفرین قهرمان! خیلی خوب درخشیدی"))
        val results = File(root(), "app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt").readText()
        assertTrue(results.contains("📋 کارنامهٔ توصیفی به تفکیک درس"))
        assertTrue(results.contains("lvl.stars"))
        val adj = File(root(), "app/src/main/java/ir/exam/app/ui/figure/AdjustableFigure.kt").readText()
        for (t in listOf(
            "axclock", "axfrac", "axabacus", "axthermo", "axtenframe", "axspin", "axbeaker",
            "axtally", "axbase10", "axplace", "axruler", "axmoney", "axjump", "axbalance",
            "axsaw", "axpattern", "axsym", "axgeoboard", "axdice", "axmularea", "axvenn",
            "axnumpts", "axnumint", "axnumineq", "axxypts", "axxyline", "axxyvec", "axxycirc", "axpolpts",
            "gauge", "ang", "sec", "arc", "segm", "parll", "ngon"
        )) {
            assertTrue("Missing $t in AdjustableFigure.kt", adj.contains("\"$t\""))
        }
        val picker = File(root(), "app/src/main/java/ir/exam/app/ui/figure/FigurePickerDialog.kt").readText()
        assertTrue(picker.contains("dropdownOptionsFor("))
        assertTrue(picker.contains("\"axsaw\" to \"mode\""))
        assertTrue(picker.contains("\"axbeaker\" to \"unit\""))
        assertTrue(picker.contains("\"axnumint\" to \"lc\""))
        assertTrue(picker.contains("\"axnumineq\" to \"dir\""))
        assertTrue(picker.contains("\"ngon\" to \"X:n\""))
    }
}
