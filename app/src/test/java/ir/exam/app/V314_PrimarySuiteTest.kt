package ir.exam.app

import ir.exam.app.core.figure.FigureGallery
import ir.exam.app.core.figure.FigureSpecParser
import ir.exam.app.core.figure.PrimaryMathSvgRenderer
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.data.repository.StudentExamPayloadCodec
import ir.exam.app.domain.model.FillBlankQuestion
import ir.exam.app.domain.model.QuestionDraft
import ir.exam.app.domain.model.QuestionType
import ir.exam.app.ui.builder.PrimaryBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * V314.0 — بستهٔ ۵گانهٔ ارتقای دورهٔ ابتدایی در اندروید:
 * ۱) گونه‌های تعاملی جدید: بخش‌کردن و صداکشی (syllable) و دسته‌بندی در سبد (bucket)
 * ۲) ۶ ابزار ریاضی/علوم ابتدایی جدید: چرتکه (axabacus)، محور پرش (axjump)، ترازو (axbalance)، دماسنج (axthermo)، جدول شگفت‌انگیز (axsaw)، الگویابی (axpattern)
 * ۳) بانک آمادهٔ غنی‌شدهٔ ابتدایی (۱۱۴ سؤال در ۹ درس رسمی پایه‌های ۱ تا ۶ + جستجو و افزودن گروهی)
 * ۴) مهرهای تشویقی معلم و کارنامهٔ توصیفی به تفکیک درس
 * ۵) تعامل لمسی مستقیم روی شکل‌های ابتدایی (axfrac، axabacus، axthermo کنار axclock)
 */
class V314_PrimarySuiteTest {
    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile
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
            encoded.publicQuestions.toString(),
            encoded.answerKey.toString()
        )
        assertEquals("syllable", decodedDrafts[0].mode)
        assertEquals("bucket", decodedDrafts[1].mode)

        val rawExam = """{"id":"ex1","title":"آزمون ابتدایی","options":{"primary":true},"questions":${encoded.publicQuestions}}"""
        val studentExam = StudentExamPayloadCodec.decode(rawExam, studentId = "st1", nowEpochMillis = 1000L)
        val qSyl = studentExam.questions[0] as FillBlankQuestion
        val qBkt = studentExam.questions[1] as FillBlankQuestion
        assertEquals(3, qSyl.syllableCount)
        assertEquals(listOf("پرنده", "پستاندار"), qBkt.buckets)
        assertEquals(4, qBkt.bucketItems.size)
        assertEquals("پستاندار:شیر+گربه|پرنده:کبوتر+گنجشک".split("|").sorted(),
            ExamQuestionCodec.bucketCanonicalAnswer(ExamQuestionCodec.parseBucketSpec(drafts[1].expectedText)).split("|").sorted())
    }

    @Test
    fun category2_sixNewPrimaryMathFiguresRenderValidSvg() {
        val ids = FigureGallery.PRIMARY_MATH_FIGURES.map { it.id }.toSet()
        for (k in listOf("axabacus", "axjump", "axbalance", "axthermo", "axsaw", "axpattern")) {
            assertTrue("Missing $k in PRIMARY_MATH_FIGURES", k in ids)
            val tpl = FigureGallery.PRIMARY_MATH_FIGURES.first { it.id == k }
            val spec = FigureSpecParser.parseFirst(tpl.token)
            assertNotNull("Failed to parse token for $k", spec)
            val svg = PrimaryMathSvgRenderer.render(spec!!)
            assertNotNull("Null SVG for $k", svg)
            assertTrue("Invalid SVG for $k", svg!!.startsWith("<svg") && svg.endsWith("</svg>"))
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
        val adj = File(root(), "app/src/main/java/ir/exam/app/ui/figure/AdjustableFigure.kt").readText()
        for (t in listOf("axclock", "axfrac", "axabacus", "axthermo")) {
            assertTrue("Missing $t in AdjustableFigure.kt", adj.contains("\"$t\""))
        }
    }
}
