package ir.exam.app

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.engine.okhttp.OkHttp
import ir.exam.app.data.dto.AnswerDto
import ir.exam.app.data.dto.AnswerStatDto
import ir.exam.app.data.dto.ExamDashboardDto
import ir.exam.app.data.dto.ExamDetailDto
import ir.exam.app.data.dto.ExamKeyDto
import ir.exam.app.data.dto.NativeProfileDto
import ir.exam.app.data.dto.SchoolClassDto
import ir.exam.app.data.dto.StudentProfileDto
import ir.exam.app.data.repository.ExamQuestionCodec
import ir.exam.app.data.repository.StudentExamPayloadCodec
import ir.exam.app.ui.builder.BuilderPreflight
import ir.exam.app.ui.builder.QuestionDraft
import ir.exam.app.ui.builder.QuestionType
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V333.1 — قفل رگرسیون و تست زندهٔ شبکه برای تمام یافته‌های شبیه‌ساز واقعی GitHub Actions (`0.txt` و `00.txt`):
 * ۱) جلوگیری از نشت متغیرهای قدیمی مخزن (`UI_TEST_TEACHER_USER`) در اجرای خودکار `push` و اجرای قطعی هر دو تست بدون Skip
 * ۲) تطبیق ۱۰۰٪ تمام RPCها و DTOهای Kotlin در مسیر کامل `TeacherExamFlowUiTest` (ذخیرهٔ اول + ویرایش مجدد با حفظ کد ۶حرفی)
 *    و `FullVisualJourneyUiTest` (معلم ← دانش‌آموز ← تصحیح با بازخورد ← کارنامه و جزئیات پاسخ دانش‌آموز)
 * ۳) هم‌خوانی تمام برچسب‌های معنایی Compose (`بازگشت به آزمون‌ها`، `منو`، `بازکردن منو`، `imePadding`) با سورس تولیدی
 */
class V333_1_EmulatorCiFixesAndLiveJourneyTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".").canonicalFile
        return if (File(cwd, "scripts/mock_supabase_server.py").exists()) cwd else cwd.parentFile
    }

    @Test
    fun workflowNeverFallsBackToLegacyRepoSecretsOnPushAndRunsBothPersonasIsolated() {
        val root = repoRoot()
        val yml = File(root, ".github/workflows/app-ui-test.yml").readText()
        assertTrue(
            "app-ui-test.yml must guard UI_TEST_TEACHER_USER / UI_TEST_TEACHER_PASS strictly behind manual workflow_dispatch + use_live_supabase",
            yml.contains("github.event_name == 'workflow_dispatch' && inputs.use_live_supabase && secrets.UI_TEST_TEACHER_USER") &&
                yml.contains("github.event_name == 'workflow_dispatch' && inputs.use_live_supabase && secrets.UI_TEST_TEACHER_PASS")
        )
        assertTrue(
            "app-ui-test.yml must only use live-supabase when workflow_dispatch explicitly sets USE_LIVE=true",
            yml.contains("\"\$EVENT_NAME\" = \"workflow_dispatch\"") &&
                yml.contains("\"\$USE_LIVE\" = \"true\"") &&
                yml.contains("mode=local-mock-supabase") &&
                yml.contains("T_USER=\"uitest_teacher\"") &&
                yml.contains("S_USER=\"uitest_student\"")
        )
        assertTrue(
            "app-ui-test.yml summary parser must parse JUnit XML via ElementTree and report skipped/failures accurately",
            yml.contains("xml.etree.ElementTree")
        )
    }

    @Test
    fun builderScreenLiftsFloatingButtonsAboveSoftKeyboardAndUiTestsMatchProductionSemantics() {
        val root = repoRoot()
        val builderScreen = File(root, "app/src/main/java/ir/exam/app/ui/builder/ExamBuilderScreen.kt").readText()
        assertTrue(
            "ExamBuilderScreen floating save/+ buttons must apply imePadding() so soft keyboard never hides them",
            builderScreen.contains(".fillMaxWidth().imePadding().padding(horizontal = 16.dp)")
        )
        assertTrue(
            "ExamBuilderScreen saved dialog uses «بازگشت به آزمون‌ها» button label",
            builderScreen.contains("\"بازگشت به آزمون‌ها\"")
        )

        val journeyTest = File(root, "app/src/androidTest/java/ir/exam/app/uitest/FullVisualJourneyUiTest.kt").readText()
        val teacherTest = File(root, "app/src/androidTest/java/ir/exam/app/uitest/TeacherExamFlowUiTest.kt").readText()
        val helper = File(root, "app/src/androidTest/java/ir/exam/app/uitest/UiScreenshotHelper.kt").readText()

        assertTrue(journeyTest.contains("\"بازگشت به آزمون‌ها\""))
        assertTrue(journeyTest.contains("clickDialogOrLastText"))
        assertTrue(journeyTest.contains("\"بازکردن منو\"") && journeyTest.contains("\"منو\""))
        assertTrue(journeyTest.contains("scrollUntilTextVisible"))
        assertTrue(journeyTest.contains("SemanticsActions.OnClick"))
        assertTrue(journeyTest.contains("hasSetTextAction()"))
        assertTrue(journeyTest.contains("runWithFailureCapture"))
        assertTrue(!journeyTest.contains("_teacher_") && !teacherTest.contains("_teacher_"))
        assertTrue(teacherTest.contains("clickSaveAndConfirm"))
        assertTrue(teacherTest.contains("runWithFailureCapture"))
        assertTrue(helper.contains("hideKeyboard") && helper.contains("99_failure_"))
        assertTrue(helper.contains("isDialog()") && helper.contains("VerticalScrollAxisRange"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun exactEmulatorUiJourneys_teacherResaveAndTwoPersonaFlowSucceedEndToEndOverHttp() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val root = repoRoot()
        val artifactDir = Files.createTempDirectory("v333-1-emu-journey").toFile()
        val port = ServerSocket(0).use { it.localPort }
        val proc = ProcessBuilder(
            "python3",
            File(root, "scripts/mock_supabase_server.py").absolutePath,
            "--host", "127.0.0.1",
            "--port", port.toString(),
            "--artifact-dir", artifactDir.absolutePath
        ).redirectErrorStream(true).start()

        try {
            val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
            val readyLine = reader.readLine() ?: error("mock_supabase_server.py failed to start")
            val anonKey = readyLine.substringAfter("anon=").trim()

            val client = createSupabaseClient(
                supabaseUrl = "http://127.0.0.1:$port",
                supabaseKey = anonKey
            ) {
                useHTTPS = false
                httpEngine = OkHttp.create()
                install(Auth) {
                    sessionManager = MemorySessionManager()
                    codeVerifierCache = MemoryCodeVerifierCache()
                    enableLifecycleCallbacks = false
                    alwaysAutoRefresh = false
                    autoLoadFromStorage = false
                }
                install(Postgrest)
            }
            ir.exam.app.data.remote.SupabaseProvider.overrideForTesting(client)

            // ۱) ورود معلم با همان مکانیزم دقیق SupabaseAuthRepository (`uitest_teacher`) + اجرای واقعی مخزن‌های تولیدی اندروید
            val gateEmail = client.postgrest.rpc(
                "native_login_gate_v283",
                buildJsonObject { put("p_identifier", "uitest_teacher"); put("p_password", "pw_ephemeral_test") }
            ).decodeAs<JsonObject>()["email"]!!.jsonPrimitive.content
            val staffEmail = client.postgrest.rpc(
                "native_staff_login_email_v1",
                buildJsonObject { put("p_username", "uitest_teacher") }
            ).decodeAs<JsonObject>()["email"]!!.jsonPrimitive.content
            assertEquals(gateEmail, staffEmail)
            client.auth.signInWith(Email) {
                email = staffEmail
                password = "pw_ephemeral_test"
            }
            val teacherProfile = client.postgrest.rpc(
                "native_ensure_profile_v1",
                buildJsonObject { put("p_fallback_name", "معلم") }
            ).decodeAs<NativeProfileDto>()
            assertTrue(teacherProfile.ok)
            assertEquals("teacher", teacherProfile.role)

            // اجرای مستقیم مخزن‌های واقعی اپ اندروید روی HTTP واقعی سرور محلی
            val schoolRepo = ir.exam.app.data.repository.SupabaseSchoolRepository()
            assertTrue(schoolRepo.getClasses().getOrThrow().isNotEmpty())
            assertTrue(schoolRepo.getStudents().getOrThrow().isNotEmpty())
            val billingRepo = ir.exam.app.data.repository.SupabaseBillingRepository()
            assertTrue(billingRepo.wallet().getOrThrow().balanceToman > 0)
            val calendarRepo = ir.exam.app.data.repository.SupabaseCalendarRepository()
            assertEquals(0, calendarRepo.unseenNotes().getOrThrow().size)
            val notifRepo = ir.exam.app.data.repository.SupabaseNotificationRepository()
            assertTrue(notifRepo.feed().isSuccess)

            // بارگذاری موازی سازندهٔ آزمون دقیقاً مطابق SupabaseExamBuilderRepository.load(null)
            val classes = client.postgrest.rpc("my_classes").decodeList<SchoolClassDto>()
            val students = client.postgrest.rpc("my_students_for_pick").decodeList<StudentProfileDto>()
            val bank = client.postgrest.rpc("native_bank_snapshot_v1").decodeAs<JsonObject>()
            assertTrue(classes.isNotEmpty() && students.isNotEmpty() && bank["ok"]?.jsonPrimitive?.booleanOrNull == true)

            // ۲) ساخت سؤال صحیح/غلط دقیقاً مثل TeacherExamFlowUiTest + بررسی نهایی BuilderPreflight
            val tfDraft = QuestionDraft(
                type = QuestionType.TRUE_FALSE,
                text = "در مثلث قائم‌الزاویه، مربع وتر برابر مجموع مربعات دو ضلع قائمه است.",
                expectedText = "true",
                correctIndex = 0,
                score = 1.0
            )
            val preflight = BuilderPreflight.check("آزمون بصری خودکار V333.1", listOf(tfDraft))
            assertTrue("BuilderPreflight must allow saving TRUE_FALSE question: ${preflight.issues}", preflight.canSave)

            val encoded = ExamQuestionCodec.encode(listOf(tfDraft))
            val examId = UUID.randomUUID().toString()
            val payload1 = buildJsonObject {
                put("operation_id", UUID.randomUUID().toString())
                put("id", examId)
                put("title", "آزمون بصری خودکار V333.1")
                put("subject", "ریاضی")
                put("duration", 15)
                put("total_score", 1.0)
                put("questions", encoded.publicQuestions)
                put("answer_key", encoded.answerKey)
                put("audience", "all")
            }
            val save1 = client.postgrest.rpc(
                "native_save_exam_v2",
                buildJsonObject { put("p_payload", payload1) }
            ).decodeAs<JsonObject>()
            val code1 = save1["code"]!!.jsonPrimitive.content
            assertEquals(6, code1.length)

            // ذخیرهٔ دوم همان آزمون با همان کد (دقیقاً سناریوی خط ۱۰۷ TeacherExamFlowUiTest)
            val loadedExam = client.from("exams").select {
                filter { eq("id", examId) }
            }.decodeList<ExamDetailDto>().single()
            val loadedKey = client.from("exam_keys").select {
                filter { eq("exam_id", examId) }
            }.decodeList<ExamKeyDto>().single()
            val reloadedDrafts = ExamQuestionCodec.decode(loadedExam.questions, loadedKey.answers)
            assertTrue(BuilderPreflight.check(loadedExam.title, reloadedDrafts).canSave)

            val payload2 = buildJsonObject {
                put("operation_id", UUID.randomUUID().toString())
                put("id", examId)
                put("code", code1)
                put("title", loadedExam.title)
                put("subject", "ریاضی")
                put("duration", 15)
                put("total_score", 1.0)
                put("questions", encoded.publicQuestions)
                put("answer_key", encoded.answerKey)
                put("audience", "all")
            }
            val save2 = client.postgrest.rpc(
                "native_save_exam_v2",
                buildJsonObject { put("p_payload", payload2) }
            ).decodeAs<JsonObject>()
            val code2 = save2["code"]!!.jsonPrimitive.content
            assertEquals("Re-saving existing exam must preserve exact 6-char exam code", code1, code2)

            // ۳) باز کردن آزمون در داشبورد معلم (`native_set_exam_open_v1`)
            val openResp = client.postgrest.rpc(
                "native_set_exam_open_v1",
                buildJsonObject { put("p_exam", examId); put("p_open", true) }
            ).decodeAs<JsonObject>()
            assertEquals(true, openResp["is_open"]?.jsonPrimitive?.booleanOrNull)
            val dashExams = client.from("exams").select(Columns.raw(ExamDashboardDto.COLUMNS)).decodeList<ExamDashboardDto>()
            assertTrue(dashExams.any { it.id == examId && it.isOpen })

            // خروج امن معلم (`native_push_unregister_v1`) و ورود دانش‌آموز (`uitest_student`)
            client.postgrest.rpc("native_push_unregister_v1", buildJsonObject { })
            client.auth.signInWith(Email) {
                email = "uitest_student@student.exam.local"
                password = "pw_ephemeral_test"
            }
            val unseen = client.postgrest.rpc("cal_unseen_v59").decodeAs<JsonObject>()
            assertEquals(true, unseen["ok"]?.jsonPrimitive?.booleanOrNull)

            // ۴ و ۵) ورود دانش‌آموز به آزمون با کد، تله‌متری نظارت زنده و ارسال نهایی پاسخ «صحیح»
            val rawExam = client.postgrest.rpc(
                "get_exam_for_student",
                buildJsonObject { put("p_code", code1) }
            ).decodeAs<JsonObject>()
            val decodedExam = StudentExamPayloadCodec.decodeFresh(
                StudentExamPayloadCodec.sanitize(rawExam),
                "22222222-2222-4222-8222-222222222222",
                System.currentTimeMillis()
            )
            assertEquals(examId, decodedExam.id)
            assertEquals(1, decodedExam.questions.size)

            val monUpsert = client.postgrest.rpc("native_monitor_upsert_v1", buildJsonObject { put("p_exam", examId) }).decodeAs<JsonObject>()
            assertEquals(true, monUpsert["ok"]?.jsonPrimitive?.booleanOrNull)

            val submitResp = client.postgrest.rpc(
                "native_submit_queued_answer_v1",
                buildJsonObject {
                    put("p_operation", UUID.randomUUID().toString())
                    put("p_exam", examId)
                    put("p_responses", buildJsonArray { add(JsonPrimitive(true)) })
                    put("p_images", buildJsonObject { })
                    put("p_meta", buildJsonObject { put("native", true) })
                }
            ).decodeAs<JsonObject>()
            val answerId = submitResp["answer_id"]!!.jsonPrimitive.content
            assertTrue(answerId.isNotBlank())

            // ۶) ورود مجدد معلم → شمارش کارت تصحیح → ثبت بازخورد و نمره
            client.auth.signInWith(Email) {
                email = staffEmail
                password = "pw_ephemeral_test"
            }
            val stats = client.from("answers").select(Columns.raw("id,student_id,graded")) {
                filter { eq("exam_id", examId) }
            }.decodeList<AnswerStatDto>()
            assertEquals(1, stats.size)

            val answers = client.from("answers").select {
                filter { eq("exam_id", examId) }
            }.decodeList<AnswerDto>()
            assertEquals(1, answers.size)

            val teacherFeedback = "آفرین! تسلط کامل بر قضیهٔ فیثاغورس."
            val saveGrade = client.postgrest.rpc(
                "native_save_grade_parts_v1",
                buildJsonObject {
                    put("p_answer", answerId)
                    put("p_grades", buildJsonArray { add(JsonPrimitive(1.0)) })
                    put("p_feedback", teacherFeedback)
                    put("p_part_grades", buildJsonObject { })
                    put("p_part_feedback", buildJsonObject { })
                }
            ).decodeAs<JsonObject>()
            assertEquals(true, saveGrade["ok"]?.jsonPrimitive?.booleanOrNull)

            // ۷) ورود مجدد دانش‌آموز → «نتایج من» (`my_grades` + `native_my_levels_v1` + `native_my_answers_v1` + `native_my_answer_detail_parts_v1`)
            client.auth.signInWith(Email) {
                email = "uitest_student@student.exam.local"
                password = "pw_ephemeral_test"
            }
            val myGrades = client.postgrest.rpc("my_grades").decodeList<JsonObject>()
            assertEquals(1, myGrades.size)
            assertEquals(teacherFeedback, myGrades.single()["feedback"]?.jsonPrimitive?.contentOrNull)
            assertEquals(1.0, myGrades.single()["total_grade"]?.jsonPrimitive?.doubleOrNull ?: 0.0, 0.001)

            val myLevels = client.postgrest.rpc("native_my_levels_v1").decodeAs<JsonObject>()
            assertEquals(4, myLevels["items"]!!.jsonArray.single().jsonObject["level_rank"]?.jsonPrimitive?.intOrNull)

            val myAnswers = client.postgrest.rpc("native_my_answers_v1").decodeAs<JsonObject>()
            assertEquals(answerId, myAnswers["items"]!!.jsonArray.single().jsonObject["id"]?.jsonPrimitive?.contentOrNull)

            val detail = client.postgrest.rpc(
                "native_my_answer_detail_parts_v1",
                buildJsonObject { put("p_answer", answerId) }
            ).decodeAs<JsonObject>()
            assertEquals(teacherFeedback, detail["feedback"]?.jsonPrimitive?.contentOrNull)
            assertNotNull(detail["questions"]?.jsonArray?.singleOrNull())

            // بررسی مستقیم متدهای واقعی SupabaseGradingRepository برای صفحهٔ «نتایج من»
            val gradingRepo = ir.exam.app.data.repository.SupabaseGradingRepository()
            assertEquals(1, gradingRepo.myGrades().getOrThrow().size)
            assertEquals("★★★★", gradingRepo.myLevels().getOrThrow()[examId]?.second)
            assertEquals(teacherFeedback, gradingRepo.myAnswerSummaries().getOrThrow().single().feedback)
            assertEquals(teacherFeedback, gradingRepo.myAnswerDetail(answerId).getOrThrow().feedback)
        } finally {
            ir.exam.app.data.remote.SupabaseProvider.overrideForTesting(null)
            Dispatchers.resetMain()
            proc.destroyForcibly()
            proc.waitFor(5, TimeUnit.SECONDS)
            artifactDir.deleteRecursively()
        }
    }
}
