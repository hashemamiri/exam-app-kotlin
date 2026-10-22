package ir.exam.app

import ir.exam.app.core.geometry.GeometryConstructionEvaluator
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.data.repository.StudentExamPayloadCodec
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.ui.builder.AssessmentPartDraft
import ir.exam.app.ui.builder.ConstructionConstraintDraft
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V332.0 — تست‌های واحد و هم‌ترازی کامل ارزیابی متوسطه (اپ نیتیو، وب و مهاجرت SQL):
 *  ۱) اعتبارسنجی نقاط و شرط‌های هندسی (distance, equal_length, midpoint, angle, collinear, perpendicular, parallel) + برچسب زاویه‌ها.
 *  ۲) کدگذاری/رمزگشایی ExamQuestionCodec بدون نشت هیچ کلید محرمانه در rawPublic / سؤال عمومی.
 *  ۳) پاک‌سازی StudentExamPayloadCodec + محاسبهٔ مهلت بخش‌های سرور با جبران اختلاف ساعت دستگاه و سرور.
 *  ۴) هم‌ترازی کامل قراردادهای اپ، وب (builder/student/admin) و مهاجرت SQL سرور.
 */
class V332_SecondaryParityAndValidationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun source(path: String): String {
        val candidates = listOf(File(path), File("..", path))
        return candidates.first { it.isFile }.readText()
    }

    @Test
    fun geometryEvaluator_validatesAllConstraintsAndAngleLabels() {
        val targets = listOf(
            GeometryConstructionEvaluator.TargetSpec(label = "B", x = 0.0, y = 0.0, tolerance = 0.3, score = 1.0)
        )
        val constraints = listOf(
            GeometryConstructionEvaluator.ConstraintSpec(id = "c1", type = "distance", points = listOf("A", "B"), value = 3.0, tolerance = 0.25, score = 1.0),
            GeometryConstructionEvaluator.ConstraintSpec(id = "c2", type = "angle", points = listOf("A", "B", "C"), value = 90.0, tolerance = 3.0, score = 2.0),
            GeometryConstructionEvaluator.ConstraintSpec(id = "c3", type = "midpoint", points = listOf("M", "A", "C"), tolerance = 0.2, score = 1.0),
            GeometryConstructionEvaluator.ConstraintSpec(id = "c4", type = "collinear", points = listOf("A", "M", "C"), tolerance = 0.2, score = 1.0),
            GeometryConstructionEvaluator.ConstraintSpec(id = "c5", type = "equal_length", points = listOf("A", "M", "M", "C"), tolerance = 0.2, score = 1.0),
            GeometryConstructionEvaluator.ConstraintSpec(id = "c6", type = "perpendicular", points = listOf("B", "A", "B", "C"), tolerance = 2.0, score = 1.0),
            GeometryConstructionEvaluator.ConstraintSpec(id = "c7", type = "parallel", points = listOf("A", "C", "D", "E"), tolerance = 2.0, score = 1.0)
        )

        val validPayload = GeometryConstructionEvaluator.encodeResponse(
            points = mapOf(
                "A" to GeometryConstructionEvaluator.Point2D(0.0, 3.0),
                "B" to GeometryConstructionEvaluator.Point2D(0.0, 0.0),
                "C" to GeometryConstructionEvaluator.Point2D(4.0, 0.0),
                "M" to GeometryConstructionEvaluator.Point2D(2.0, 1.5),
                "D" to GeometryConstructionEvaluator.Point2D(0.0, 1.5),
                "E" to GeometryConstructionEvaluator.Point2D(2.0, 0.0)
            ),
            angleLabels = listOf("∠ABC", "ABC")
        )
        val parsed = GeometryConstructionEvaluator.parseResponse(validPayload)
        assertEquals(listOf("ABC"), parsed.angleLabels)

        val full = GeometryConstructionEvaluator.evaluate(
            questionScore = 5.0,
            targets = targets,
            constraints = constraints,
            rawResponse = validPayload
        )
        assertEquals(5.0, full.score, 0.001)
        assertEquals(true, full.checks.all { it.passed })

        val formatted = GeometryConstructionEvaluator.formatResponseSummary(
            raw = validPayload,
            expectedLabels = listOf("A", "B", "C")
        )
        assertTrue(formatted.contains("∠ABC: 90.0°"))

        // اگر C را روی (3, 3) بگذاریم زاویهٔ ABC برابر ۴۵ درجه می‌شود و شرط قائمه رد می‌شود.
        val wrongPayload = GeometryConstructionEvaluator.encodeResponse(
            points = mapOf(
                "A" to GeometryConstructionEvaluator.Point2D(0.0, 3.0),
                "B" to GeometryConstructionEvaluator.Point2D(0.0, 0.0),
                "C" to GeometryConstructionEvaluator.Point2D(3.0, 3.0),
                "M" to GeometryConstructionEvaluator.Point2D(1.5, 3.0),
                "D" to GeometryConstructionEvaluator.Point2D(0.0, 1.5),
                "E" to GeometryConstructionEvaluator.Point2D(2.0, 0.0)
            ),
            angleLabels = listOf("ABC")
        )
        val partial = GeometryConstructionEvaluator.evaluate(
            questionScore = 5.0,
            targets = targets,
            constraints = constraints,
            rawResponse = wrongPayload
        )
        assertEquals(true, partial.score < 3.0)
    }

    @Test
    fun examQuestionCodec_stripsSecretsEvenFromPollutedRawPublicAndDerivesLabels() {
        val pollutedRawPublic = JsonObject(
            mapOf(
                "rubric" to JsonPrimitive("SECRET_RUBRIC"),
                "partRubrics" to JsonArray(listOf(JsonPrimitive("SECRET_PART"))),
                "constructionTargets" to JsonArray(listOf(JsonObject(mapOf("label" to JsonPrimitive("A"), "x" to JsonPrimitive(3))))),
                "constructionConstraints" to JsonArray(listOf(JsonObject(mapOf("type" to JsonPrimitive("angle"), "value" to JsonPrimitive(90)))))
            )
        )
        val draft = QuestionDraft(
            id = "q-geom-1",
            type = QuestionType.ESSAY,
            text = "مثلث قائم‌الزاویهٔ ABC بسازید.",
            score = 4.0,
            mode = "construct",
            rubric = "معیار محرمانه",
            assessmentParts = listOf(AssessmentPartDraft(id = "p1", label = "الف", prompt = "توجیه", score = 2.0, rubric = "کلید گام")),
            constructionPrompt = "زاویهٔ B قائمه باشد و طول AB برابر ۳ واحد باشد.",
            constructionConstraints = listOf(
                ConstructionConstraintDraft(id = "c1", type = "angle", points = listOf("A", "B", "C"), value = 90.0, tolerance = 4.0, score = 2.0),
                ConstructionConstraintDraft(id = "c2", type = "distance", points = listOf("A", "B"), value = 3.0, tolerance = 0.4, score = 2.0)
            ),
            rawPublic = pollutedRawPublic
        )

        val encoded = ExamQuestionCodec.encode(listOf(draft))
        val pubObj = encoded.publicQuestions.single().jsonObject
        val keyObj = encoded.answerKey.single().jsonObject

        assertFalse("rubric" in pubObj)
        assertFalse("partRubrics" in pubObj)
        assertFalse("constructionTargets" in pubObj)
        assertFalse("constructionConstraints" in pubObj)
        assertEquals("""["A","B","C"]""", pubObj["constructionLabels"].toString())
        assertEquals("""["ABC"]""", pubObj["constructionAngleLabels"].toString())
        assertTrue("constructionConstraints" in keyObj)

        val roundTrip = ExamQuestionCodec.decode(encoded.publicQuestions, encoded.answerKey).single()
        assertEquals(2, roundTrip.constructionConstraints.size)
        assertEquals("angle", roundTrip.constructionConstraints[0].type)
        assertEquals(listOf("ABC"), roundTrip.constructionAngleLabels)
    }

    @Test
    fun studentPayloadCodec_stripsAllSecretsAndComputesClockSafeSectionDeadlines() {
        val raw = json.parseToJsonElement(
            """
            {
              "id": "exam-332",
              "title": "آزمون هندسه دهم",
              "code": "GEO10",
              "server_now": "2026-10-22T08:00:00Z",
              "section_deadlines": {
                "بخش اول": "2026-10-22T08:12:00Z",
                "بخش منقضی": "2026-10-22T07:55:00Z"
              },
              "expired_sections": ["بخش منقضی"],
              "options": { "sectionDurations": { "بخش اول": 15, "بخش منقضی": 10 } },
              "questions": [
                {
                  "id": "q1",
                  "type": "essay",
                  "mode": "construct",
                  "text": "زاویهٔ ABC را قائمه بسازید.",
                  "score": 4,
                  "sectionTitle": "بخش اول",
                  "constructionPrompt": "رأس B در مبدأ",
                  "constructionLabels": ["A", "B", "C"],
                  "constructionAngleLabels": ["ABC"],
                  "rubric": "LEAK_1",
                  "partRubrics": ["LEAK_2"],
                  "constructionTargets": [{"label": "B", "x": 0, "y": 0}],
                  "constructionConstraints": [{"type": "angle", "points": ["A","B","C"], "value": 90}]
                }
              ]
            }
            """.trimIndent()
        ).jsonObject

        val sanitized = StudentExamPayloadCodec.sanitize(raw)
        val q0 = sanitized["questions"]!!.jsonArray[0].jsonObject
        assertFalse("rubric" in q0)
        assertFalse("partRubrics" in q0)
        assertFalse("constructionTargets" in q0)
        assertFalse("constructionConstraints" in q0)

        val localNow = 1_900_000_000_000L
        val deadlines = StudentExamPayloadCodec.serverSectionDeadlines(raw, localNow)
        assertEquals(localNow + 12 * 60_000L, deadlines["بخش اول"])
        assertEquals(localNow - 5 * 60_000L, deadlines["بخش منقضی"])
        assertEquals(setOf("بخش منقضی"), StudentExamPayloadCodec.serverExpiredSections(raw))

        val decoded = StudentExamPayloadCodec.decodeFresh(raw, studentId = "st-1", receivedAtEpochMs = localNow)
        val essay = decoded.questions.single() as EssayQuestion
        assertEquals(listOf("A", "B", "C"), essay.constructionLabels)
        assertEquals(listOf("ABC"), essay.constructionAngleLabels)
        assertEquals(mapOf("بخش اول" to 15, "بخش منقضی" to 10), decoded.sectionDurationsMinutes)
    }

    @Test
    fun appWebAndSqlContractsStayFullyAligned() {
        val builderJs = source("site/src/builder.js")
        val studentJs = source("site/src/student.js")
        val adminJs = source("site/src/admin.js")
        val sql = source("supabase/migrations/20261022_secondary_geometry_and_section_timing_v332.sql")
        val repoKt = source("app/src/main/java/ir/exam/app/data/repository/SupabaseStudentExamRepository.kt")
        val vmKt = source("app/src/main/java/ir/exam/app/ui/student/StudentExamViewModel.kt")
        val screenKt = source("app/src/main/java/ir/exam/app/ui/student/StudentExamScreen.kt")
        val resultsKt = source("app/src/main/java/ir/exam/app/ui/reports/StudentResultsScreen.kt")

        assertTrue(sql.contains("native_sync_exam_section_v1"))
        assertTrue(sql.contains("section_deadlines"))
        assertTrue(sql.contains("section_locked_responses"))
        assertTrue(repoKt.contains("native_sync_exam_section_v1"))
        assertTrue(vmKt.contains("syncSectionToServer"))
        assertTrue(studentJs.contains("native_sync_exam_section_v1"))
        assertTrue(screenKt.contains("زمان این بخش تمام شد؛ پاسخ‌های ثبت‌شده حفظ شده‌اند و امکان ویرایش یا بازگشت نیست."))

        assertTrue(builderJs.contains("constructionConstraints"))
        assertTrue(builderJs.contains("constructionAngleLabels"))
        assertTrue(builderJs.contains("sectionDurationsBox"))
        assertTrue(studentJs.contains("_angles"))
        assertTrue(adminJs.contains("evaluateConstructionResponse"))
        assertTrue(adminJs.contains("part_grades"))
        assertTrue(resultsKt.contains("earnedScore"))
        assertTrue(sql.contains("'part_grades', v_part_grades"))
    }
}
