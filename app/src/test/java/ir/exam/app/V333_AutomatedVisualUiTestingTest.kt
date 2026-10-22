package ir.exam.app

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.okhttp.OkHttp
import ir.exam.app.data.dto.AnswerDto
import ir.exam.app.data.dto.NativeProfileDto
import ir.exam.app.data.dto.WalletSnapshotDto
import ir.exam.app.data.repository.StudentExamPayloadCodec
import ir.exam.app.domain.model.EssayQuestion
import ir.exam.app.domain.model.MultipleChoiceQuestion
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.nio.file.Files
import java.util.Base64
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
 * V333.0 — تست خودکار تصویری و رابط کاربری (مرورگر واقعی + شبیه‌ساز اندروید در GitHub Actions).
 *
 * این مجموعه هم ۴ باگ واقعی کشف‌شده توسط ربات UI وب را در برابر بازگشت قفل می‌کند،
 * هم ایزوله‌سازی امنیتی debug/release اندروید را بررسی می‌کند، و هم یک جریان کامل
 * واقعی روی شبکهٔ TCP با کلاینت واقعی `supabase-kt` + `OkHttp` و کدک‌های واقعی اندروید
 * علیه سرور `scripts/mock_supabase_server.py` اجرا می‌کند.
 */
class V333_AutomatedVisualUiTestingTest {

    private fun repoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".").canonicalFile
        return if (File(cwd, "scripts/mock_supabase_server.py").exists()) cwd else cwd.parentFile
    }

    @Test
    fun webUiBugs_elHelperHandlesBooleanPropertiesAndNestedChildArrays() {
        val appJs = File(repoRoot(), "site/src/app.js").readText()
        // Bug #1: el() must set real boolean DOM properties (disabled, checked, readOnly, required, multiple, selected)
        // instead of setAttribute('disabled', 'false') which permanently disables every button in HTML!
        assertTrue(
            "site/src/app.js must set real DOM boolean properties for disabled/checked/readOnly/required/multiple/selected",
            appJs.contains("e[_bp] = !!attrs[k]")
        )
        // Bug #4: el() must recursively flatten nested child arrays (e.g. geometry coordinate select rows).
        assertTrue(
            "site/src/app.js must recursively append nested Array children inside el()",
            appJs.contains("if (Array.isArray(c)) { c.forEach(addChild); return; }")
        )
    }

    @Test
    fun webUiBugs_studentIifeDefinesNumHelperAndAdminPreservesQuestionRubric() {
        val root = repoRoot()
        // Bug #2: every file in site/src/*.js that invokes num(...) must have num defined in scope.
        val studentJs = File(root, "site/src/student.js").readText()
        assertTrue(
            "site/src/student.js must define function num(v) inside its IIFE so sectioned exams never throw ReferenceError",
            studentJs.contains("function num(v)")
        )

        // Bug #3: gradingExam() in site/src/admin.js must restore question-level private rubric from exam_keys.
        val adminJs = File(root, "site/src/admin.js").readText()
        assertTrue(
            "site/src/admin.js gradingExam() must merge private rubric from exam_keys (q.rubric||k.rubric||'')",
            adminJs.replace(" ", "").contains("rubric:q.rubric||k.rubric||''")
        )
    }

    @Test
    fun androidSecurityAndIsolation_cleartextStrictlyLimitedToDebugAndFlagSecureRestored() {
        val root = repoRoot()
        val mainManifest = File(root, "app/src/main/AndroidManifest.xml").readText()
        assertTrue(
            "Production AndroidManifest.xml MUST keep android:usesCleartextTraffic=\"false\"",
            mainManifest.contains("android:usesCleartextTraffic=\"false\"")
        )

        val debugManifest = File(root, "app/src/debug/AndroidManifest.xml").readText()
        assertTrue(
            "Debug AndroidManifest.xml must enable cleartext HTTP only for debug emulator UI tests",
            debugManifest.contains("android:usesCleartextTraffic=\"true\"") &&
                debugManifest.contains("tools:replace=\"android:usesCleartextTraffic\"")
        )

        val supabaseProvider = File(root, "app/src/main/java/ir/exam/app/data/remote/SupabaseProvider.kt").readText()
        val updateProvider = File(root, "app/src/main/java/ir/exam/app/data/remote/PublicUpdateSupabaseProvider.kt").readText()
        assertTrue(
            "SupabaseProvider must set useHTTPS = !BuildConfig.SUPABASE_URL.startsWith(\"http://\", ignoreCase = true)",
            supabaseProvider.contains("useHTTPS = !BuildConfig.SUPABASE_URL.startsWith(\"http://\", ignoreCase = true)")
        )
        assertTrue(
            "PublicUpdateSupabaseProvider must set useHTTPS = !BuildConfig.SUPABASE_URL.startsWith(\"http://\", ignoreCase = true)",
            updateProvider.contains("useHTTPS = !BuildConfig.SUPABASE_URL.startsWith(\"http://\", ignoreCase = true)")
        )

        val screenshotHelper = File(
            root,
            "app/src/androidTest/java/ir/exam/app/uitest/UiScreenshotHelper.kt"
        ).readText()
        assertTrue(
            "UiScreenshotHelper must restore FLAG_SECURE inside a finally block when previously enabled",
            screenshotHelper.contains("finally") && screenshotHelper.contains("WindowManager.LayoutParams.FLAG_SECURE")
        )

        val examApp = File(root, "app/src/main/java/ir/exam/app/ui/app/ExamApp.kt").readText()
        assertTrue(
            "Student TopAppBar navigation button must expose contentDescription = \"منو\" for accessibility and Compose UI tests",
            examApp.contains("contentDescription = \"منو\"")
        )
    }

    @Test
    fun ciWorkflows_siteAndAndroidUiRunAutomaticallyOnMainAndStagingWithVisualArtifacts() {
        val root = repoRoot()
        val siteYml = File(root, ".github/workflows/site.yml").readText()
        assertTrue(siteYml.contains("scripts/web_ui_e2e.py"))
        assertTrue(siteYml.contains("web-ui-visual-report"))
        assertTrue(siteYml.contains("GITHUB_STEP_SUMMARY"))
        assertTrue(siteYml.contains("[main, staging]"))

        val appUiYml = File(root, ".github/workflows/app-ui-test.yml").readText()
        assertTrue(!appUiYml.contains("\n  push:"))
        assertTrue(appUiYml.contains("workflow_dispatch:"))
        assertTrue(appUiYml.contains("scripts/mock_supabase_server.py"))
        assertTrue(appUiYml.contains("http://10.0.2.2:18991"))
        assertTrue(appUiYml.contains("uiPixel6Api34DebugAndroidTest"))
        assertTrue(appUiYml.contains("android-ui-report"))
        assertTrue(appUiYml.contains("GITHUB_STEP_SUMMARY"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun liveKotlinSupabaseWireJourney_teacherToStudentToGradingToReportCardAndScreenshotGallery() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val root = repoRoot()
        val artifactDir = Files.createTempDirectory("v333-wire-gallery").toFile()
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
            val readyLine = reader.readLine() ?: error("mock_supabase_server.py exited before printing READY banner")
            assertTrue("Expected MOCK_SUPABASE_READY banner, got: $readyLine", readyLine.startsWith("MOCK_SUPABASE_READY"))
            val anonKey = readyLine.substringAfter("anon=").trim()
            assertTrue(anonKey.length > 20)

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

            // ۱) ورود معلم از مسیر واقعی Auth + RPC پروفایل و کیف پول
            val gateTeacher = client.postgrest.rpc(
                "native_login_gate_v283",
                buildJsonObject {
                    put("p_identifier", "uitest_teacher")
                    put("p_password", "test-pass-v333")
                }
            ).decodeAs<JsonObject>()
            val teacherEmail = gateTeacher["email"]!!.jsonPrimitive.content
            client.auth.signInWith(Email) {
                email = teacherEmail
                password = "test-pass-v333"
            }
            val teacherProfile = client.postgrest.rpc(
                "native_ensure_profile_v1",
                buildJsonObject { put("p_fallback_name", "معلم") }
            ).decodeAs<NativeProfileDto>()
            assertTrue(teacherProfile.ok)
            assertEquals("teacher", teacherProfile.role)

            val wallet = client.postgrest.rpc("native_wallet_snapshot").decodeAs<WalletSnapshotDto>()
            assertTrue(wallet.ok)
            assertTrue(wallet.balance > 0L)

            // ۲) ذخیرهٔ آزمون متوسطه شامل ترسیم هندسی ۳ نقطه‌ای + اثبات ۲ مرحله‌ای + بخش زمان‌دار + روبریک محرمانه
            val saveResp = client.postgrest.rpc(
                "native_save_exam_v2",
                buildJsonObject {
                    put("p_exam_id", null as String?)
                    put(
                        "p_payload",
                        buildJsonObject {
                            put("title", "آزمون سیم‌کشی V333 — هندسه و اثبات")
                            put("subject", "ریاضی و هندسه")
                            put("duration", 40)
                            put("total_score", 10.0)
                            put(
                                "options",
                                buildJsonObject {
                                    put("primary", false)
                                    put(
                                        "sectionDurations",
                                        buildJsonObject {
                                            put("بخش اول: ترسیم و اثبات", 10)
                                            put("بخش دوم: تکمیلی", 20)
                                        }
                                    )
                                }
                            )
                            put(
                                "questions",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("id", "q-geo")
                                            put("type", "geometry_construction")
                                            put("text", "مثلث قائم‌الزوایه ABC را با رأس قائمه در B رسم کنید.")
                                            put("score", 3.0)
                                            put("sectionTitle", "بخش اول: ترسیم و اثبات")
                                            put("rubric", "روبریک محرمانه ترسیم معلم — نباید افشا شود")
                                            put(
                                                "constructionTargets",
                                                buildJsonArray {
                                                    add(buildJsonObject { put("label", "A"); put("x", 0.0); put("y", 3.0); put("tolerance", 0.25) })
                                                    add(buildJsonObject { put("label", "B"); put("x", 0.0); put("y", 0.0); put("tolerance", 0.25) })
                                                    add(buildJsonObject { put("label", "C"); put("x", 4.0); put("y", 0.0); put("tolerance", 0.25) })
                                                }
                                            )
                                            put(
                                                "constructionConstraints",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("type", "angle")
                                                            put("points", buildJsonArray { add(JsonPrimitive("A")); add(JsonPrimitive("B")); add(JsonPrimitive("C")) })
                                                            put("value", 90.0)
                                                            put("tolerance", 1.5)
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    )
                                    add(
                                        buildJsonObject {
                                            put("id", "q-proof")
                                            put("type", "proof_steps")
                                            put("text", "قضیه فیثاغورس را در دو گام بنویسید.")
                                            put("score", 4.0)
                                            put("sectionTitle", "بخش اول: ترسیم و اثبات")
                                            put("rubric", "روبریک محرمانه اثبات معلم — نباید افشا شود")
                                            put(
                                                "assessmentParts",
                                                buildJsonArray {
                                                    add(buildJsonObject { put("id", "p1"); put("label", "گام الف"); put("prompt", "رابطه توان دوم اضلاع"); put("score", 2.0); put("rubric", "کلید گام الف محرمانه") })
                                                    add(buildJsonObject { put("id", "p2"); put("label", "گام ب"); put("prompt", "نتیجه نهایی وتر"); put("score", 2.0); put("rubric", "کلید گام ب محرمانه") })
                                                }
                                            )
                                        }
                                    )
                                    add(
                                        buildJsonObject {
                                            put("id", "q-mc")
                                            put("type", "multiple_choice")
                                            put("text", "وتر مثلث ۳ و ۴ چند است؟")
                                            put("score", 3.0)
                                            put("sectionTitle", "بخش دوم: تکمیلی")
                                            put("correctOption", 1)
                                            put(
                                                "options",
                                                buildJsonArray {
                                                    add(JsonPrimitive("۴"))
                                                    add(JsonPrimitive("۵"))
                                                    add(JsonPrimitive("۶"))
                                                    add(JsonPrimitive("۷"))
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            ).decodeAs<JsonObject>()
            assertEquals(true, saveResp["ok"]?.jsonPrimitive?.booleanOrNull)
            val examId = saveResp["id"]!!.jsonPrimitive.content
            val examCode = saveResp["code"]!!.jsonPrimitive.content

            val openResp = client.postgrest.rpc(
                "native_set_exam_open_v1",
                buildJsonObject {
                    put("p_exam_id", examId)
                    put("p_open", true)
                }
            ).decodeAs<JsonObject>()
            assertEquals(true, openResp["ok"]?.jsonPrimitive?.booleanOrNull)

            // ۳) ورود دانش‌آموز و دریافت آزمون با کدک واقعی StudentExamPayloadCodec
            client.auth.signOut()
            val gateStudent = client.postgrest.rpc(
                "native_login_gate_v283",
                buildJsonObject {
                    put("p_identifier", "uitest_student")
                    put("p_password", "test-pass-v333")
                }
            ).decodeAs<JsonObject>()
            client.auth.signInWith(Email) {
                email = gateStudent["email"]!!.jsonPrimitive.content
                password = "test-pass-v333"
            }
            val studentProfile = client.postgrest.rpc(
                "native_ensure_profile_v1",
                buildJsonObject { put("p_fallback_name", "دانش‌آموز") }
            ).decodeAs<NativeProfileDto>()
            assertEquals("student", studentProfile.role)

            val rawStudentPayload = client.postgrest.rpc(
                "get_exam_for_student",
                buildJsonObject { put("p_code", examCode) }
            ).decodeAs<JsonObject>()
            val rawWireString = rawStudentPayload.toString()
            assertFalse("Server must NEVER leak private teacher rubric to student wire payload!", rawWireString.contains("محرمانه"))
            assertFalse("Server must NEVER leak constructionTargets to student wire payload!", rawWireString.contains("constructionTargets"))

            val parsedExam = StudentExamPayloadCodec.decodeFresh(
                raw = rawStudentPayload,
                studentId = studentProfile.id ?: "student",
                receivedAtEpochMs = System.currentTimeMillis()
            )
            assertEquals(3, parsedExam.questions.size)
            assertEquals(10, parsedExam.sectionDurationsMinutes["بخش اول: ترسیم و اثبات"])
            assertEquals(20, parsedExam.sectionDurationsMinutes["بخش دوم: تکمیلی"])
            assertTrue(parsedExam.questions[0] is EssayQuestion)
            val proofQ = parsedExam.questions[1] as EssayQuestion
            assertEquals(2, proofQ.assessmentParts.size)
            assertEquals("", proofQ.assessmentParts[0].rubric)
            assertTrue(parsedExam.questions[2] is MultipleChoiceQuestion)

            // ۴) ثبت پاسخ دانش‌آموز (ترسیم هندسی + اثبات ۲ مرحله‌ای + تستی)
            val submitResp = client.postgrest.rpc(
                "native_submit_queued_answer_v1",
                buildJsonObject {
                    put("p_code", examCode)
                    put(
                        "p_responses",
                        buildJsonArray {
                            add(JsonPrimitive("""{"A":{"x":0,"y":3},"B":{"x":0,"y":0},"C":{"x":4,"y":0},"_angles":{"ABC":90}}"""))
                            add(JsonPrimitive("""["AB^2 + BC^2 = 9 + 16 = 25","AC = 5"]"""))
                            add(JsonPrimitive(1))
                        }
                    )
                    put("p_response_images", buildJsonObject {})
                }
            ).decodeAs<JsonObject>()
            assertEquals(true, submitResp["ok"]?.jsonPrimitive?.booleanOrNull)
            val answerId = submitResp["answer_id"]!!.jsonPrimitive.content

            // ۵) بازگشت معلم، بررسی تصحیح خودکار ترسیم هندسی (۳ از ۳) و ثبت نمرهٔ گام‌به‌گام اثبات (۲ + ۲)
            client.auth.signOut()
            client.auth.signInWith(Email) {
                email = teacherEmail
                password = "test-pass-v333"
            }
            val autoInfo = client.postgrest.rpc(
                "exam_autograde_info",
                buildJsonObject { put("p_exam_id", examId) }
            ).decodeAs<JsonObject>()
            assertEquals(true, autoInfo["has_essay"]?.jsonPrimitive?.booleanOrNull)
            val essayIndices = autoInfo["essay_questions"]!!.jsonArray.map { it.jsonPrimitive.intOrNull }
            assertEquals("Geometry must be auto-graded; only proof_steps (1-based index 2) is manual essay", listOf(2), essayIndices)

            val answers = client.postgrest.from("answers").select().decodeList<AnswerDto>()
            assertEquals(1, answers.size)
            assertEquals(answerId, answers[0].id)
            assertEquals(6.0, answers[0].totalGrade, 0.001) // 3.0 geometry + 3.0 MC before manual essay grading!

            val gradeSave = client.postgrest.rpc(
                "native_save_grade_parts_v1",
                buildJsonObject {
                    put("p_answer_id", answerId)
                    put(
                        "p_grades",
                        buildJsonArray {
                            add(JsonPrimitive(3.0))
                            add(JsonPrimitive(4.0))
                            add(JsonPrimitive(3.0))
                        }
                    )
                    put("p_feedback", "آفرین! هم ترسیم هندسی و هم هر دو گام اثبات کامل است.")
                    put(
                        "p_part_grades",
                        buildJsonObject {
                            put("1", buildJsonArray { add(JsonPrimitive(2.0)); add(JsonPrimitive(2.0)) })
                        }
                    )
                    put(
                        "p_part_feedback",
                        buildJsonObject {
                            put("1", buildJsonArray { add(JsonPrimitive("گام الف عالی")); add(JsonPrimitive("گام ب دقیق")) })
                        }
                    )
                }
            ).decodeAs<JsonObject>()
            assertEquals(true, gradeSave["ok"]?.jsonPrimitive?.booleanOrNull)
            assertEquals(10.0, gradeSave["total_grade"]!!.jsonPrimitive.doubleOrNull ?: -1.0, 0.001)

            // ۶) مشاهدهٔ کارنامهٔ نهایی دانش‌آموز همراه با بازخورد گام‌ها و بدون نشت کلید محرمانه
            client.auth.signOut()
            client.auth.signInWith(Email) {
                email = gateStudent["email"]!!.jsonPrimitive.content
                password = "test-pass-v333"
            }
            val detail = client.postgrest.rpc(
                "native_my_answer_detail_parts_v1",
                buildJsonObject { put("p_answer_id", answerId) }
            ).decodeAs<JsonObject>()
            assertEquals(10.0, detail["total_grade"]!!.jsonPrimitive.doubleOrNull ?: -1.0, 0.001)
            assertTrue(detail.toString().contains("گام الف عالی"))
            assertFalse("Report card payload must never leak teacher private rubric", detail.toString().contains("محرمانه"))

            // ۷) ارسال اسکرین‌شات PNG واقعی به POST /__test__/screenshot و بررسی ساخت گالری فارسی
            val tinyPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
            val shotPayload = buildJsonObject {
                put("filename", "01_wire_test.png")
                put("title", "۰۱ — تست سیم‌کشی")
                put("detail", "بررسی تولید گالری تصویری اندروید")
                put("width", 1080)
                put("height", 2400)
                put("png_base64", tinyPngBase64)
            }.toString().toByteArray(Charsets.UTF_8)

            val conn = (URL("http://127.0.0.1:$port/__test__/screenshot").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conn.outputStream.use { it.write(shotPayload) }
            assertEquals(200, conn.responseCode)

            assertTrue(File(artifactDir, "01_wire_test.png").exists())
            assertEquals(Base64.getDecoder().decode(tinyPngBase64).size.toLong(), File(artifactDir, "01_wire_test.png").length())
            assertTrue(File(artifactDir, "summary.md").readText().contains("گزارش تست خودکار بصری"))
            assertTrue(File(artifactDir, "visual-report.html").readText().contains("data:image/png;base64,"))
        } finally {
            proc.destroyForcibly()
            proc.waitFor(5, TimeUnit.SECONDS)
            artifactDir.deleteRecursively()
            Dispatchers.resetMain()
        }
    }
}
