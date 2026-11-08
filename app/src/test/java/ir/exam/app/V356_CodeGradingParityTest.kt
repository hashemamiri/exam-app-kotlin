package ir.exam.app

import ir.exam.app.domain.grading.CodeGrading
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V356 — آزمون برابری سایت و اپ برای تصحیح کد (فاز صفر).
 *
 * بردارها با خودِ site/src/codegrade.js تولید شده‌اند (مولد:
 * scripts/gen_codegrade_vectors.mjs). اگر الگوریتم سایت عوض شود و
 * بردارها بازتولید نشوند این آزمون می‌شکند؛ و اگر کاتلین از سایت فاصله
 * بگیرد هم می‌شکند. پس دو پیاده‌سازی نمی‌توانند بی‌خبر واگرا شوند.
 */
class V356_CodeGradingParityTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun kotlinMatchesSiteCodeGrading() {
        val v = Json.parseToJsonElement(
            File(root(), "app/src/test/resources/v356_codegrade_vectors.json").readText()
        ).jsonObject
        var pass = 0
        val problems = mutableListOf<String>()

        fun ok(name: Boolean, detail: String) {
            if (name) pass++ else problems.add(detail)
        }
        fun arr(k: String) = v[k]!!.jsonArray
        fun opts(o: JsonObject) = CodeGrading.Options(
            trimLines = o["trimLines"]!!.jsonPrimitive.boolean,
            collapseBlank = o["collapseBlank"]!!.jsonPrimitive.boolean,
            ignoreCase = o["ignoreCase"]!!.jsonPrimitive.boolean
        )
        fun str(el: kotlinx.serialization.json.JsonElement) = el.jsonPrimitive.content

        arr("normalize").forEach { e ->
            val o = e.jsonObject
            val got = CodeGrading.normalizeOutput(str(o["in"]!!), opts(o["opts"]!!.jsonObject))
            val want = str(o["out"]!!)
            ok(got == want, "normalize «${str(o["in"]!!)}» → «$got» ولی «$want»")
        }

        arr("matches").forEach { e ->
            val o = e.jsonObject
            val got = CodeGrading.outputMatches(str(o["a"]!!), str(o["b"]!!), opts(o["opts"]!!.jsonObject))
            ok(got == o["out"]!!.jsonPrimitive.boolean, "matches «${str(o["a"]!!)}» / «${str(o["b"]!!)}»")
        }

        arr("parse").forEach { e ->
            val o = e.jsonObject
            val got = CodeGrading.parseTests(str(o["in"]!!))
            val want = o["out"]!!.jsonObject
            val wantTests = want["tests"]!!.jsonArray
            ok(got.tests.size == wantTests.size, "parse size ${got.tests.size} vs ${wantTests.size} برای «${str(o["in"]!!).take(40)}»")
            if (got.tests.size == wantTests.size) {
                got.tests.forEachIndexed { i, t ->
                    val w = wantTests[i].jsonObject
                    ok(t.label == str(w["label"]!!), "parse label #$i")
                    ok(t.input == str(w["input"]!!), "parse input #$i «${t.input}» vs «${str(w["input"]!!)}»")
                    ok(t.expected == str(w["expected"]!!), "parse expected #$i")
                    ok(t.weight == w["weight"]!!.jsonPrimitive.double, "parse weight #$i ${t.weight} vs ${w["weight"]}")
                    ok(t.hidden == w["hidden"]!!.jsonPrimitive.boolean, "parse hidden #$i")
                }
            }
            val wantErr = want["errors"]!!.jsonArray
            ok(got.errors.size == wantErr.size, "parse errors ${got.errors.size} vs ${wantErr.size}")
            if (got.errors.size == wantErr.size) {
                got.errors.forEachIndexed { i, err ->
                    val w = wantErr[i].jsonObject
                    ok(err.line == w["line"]!!.jsonPrimitive.int, "parse error line #$i")
                    ok(err.message == str(w["message"]!!), "parse error text #$i «${err.message}» vs «${str(w["message"]!!)}»")
                }
            }
        }

        arr("format").forEach { e ->
            val o = e.jsonObject
            val tests = o["tests"]!!.jsonArray.map { t ->
                val j = t.jsonObject
                CodeGrading.TestCase(
                    str(j["label"]!!), str(j["input"]!!), str(j["expected"]!!),
                    j["weight"]!!.jsonPrimitive.double, j["hidden"]!!.jsonPrimitive.boolean
                )
            }
            val got = CodeGrading.formatTests(tests)
            ok(got == str(o["out"]!!), "format «$got» vs «${str(o["out"]!!)}»")
        }

        arr("escape").forEach { e ->
            val o = e.jsonObject
            ok(CodeGrading.escapeText(str(o["in"]!!)) == str(o["out"]!!), "escape «${str(o["in"]!!)}»")
        }
        arr("unescape").forEach { e ->
            val o = e.jsonObject
            ok(CodeGrading.unescapeText(str(o["in"]!!)) == str(o["out"]!!), "unescape «${str(o["in"]!!)}»")
        }

        arr("score").forEach { e ->
            val o = e.jsonObject
            val tag = str(o["tag"]!!)
            val tests = o["tests"]!!.jsonArray.map {
                CodeGrading.WeightedTest(it.jsonObject["id"]!!.jsonPrimitive.content.toLong(), it.jsonObject["weight"]!!.jsonPrimitive.double)
            }
            val checks = o["checks"]!!.jsonObject.entries.associate { (k, value) ->
                k.toLong() to CodeGrading.Check(value.jsonObject["passed"]!!.jsonPrimitive.boolean)
            }
            val got = CodeGrading.scoreFromTests(tests, checks, o["max"]!!.jsonPrimitive.double)
            val w = o["out"]!!.jsonObject
            fun d(k: String) = w[k]!!.jsonPrimitive.double
            fun i(k: String) = w[k]!!.jsonPrimitive.int
            ok(Math.abs(got.suggested - d("suggested")) < 1e-9, "score suggested $tag: ${got.suggested} vs ${d("suggested")}")
            ok(Math.abs(got.ratio - d("ratio")) < 1e-9, "score ratio $tag")
            ok(Math.abs(got.passedWeight - d("passedWeight")) < 1e-9, "score passedWeight $tag")
            ok(Math.abs(got.totalWeight - d("totalWeight")) < 1e-9, "score totalWeight $tag")
            ok(got.checkedCount == i("checkedCount"), "score checkedCount $tag")
            ok(got.passedCount == i("passedCount"), "score passedCount $tag")
            ok(got.totalCount == i("totalCount"), "score totalCount $tag")
            ok(got.complete == w["complete"]!!.jsonPrimitive.boolean, "score complete $tag")
            ok(got.hasTests == w["hasTests"]!!.jsonPrimitive.boolean, "score hasTests $tag")
        }

        arr("summary").forEach { e ->
            val o = e.jsonObject
            val tests = o["tests"]!!.jsonArray.map {
                CodeGrading.WeightedTest(it.jsonObject["id"]!!.jsonPrimitive.content.toLong(), it.jsonObject["weight"]!!.jsonPrimitive.double)
            }
            val checks = o["checks"]!!.jsonObject.entries.associate { (k, value) ->
                k.toLong() to CodeGrading.Check(value.jsonObject["passed"]!!.jsonPrimitive.boolean)
            }
            ok(CodeGrading.summary(tests, checks) == str(o["out"]!!), "summary ${str(o["tag"]!!)}")
        }

        problems.take(8).forEach { println("  ✗ $it") }
        assertTrue("کاتلین از سایت فاصله گرفت (${problems.size} خطا از $pass سنجه)", problems.isEmpty())
        assertTrue("بردارها باید پر باشند", pass > 1000)
    }
}
