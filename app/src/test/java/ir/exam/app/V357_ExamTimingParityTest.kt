package ir.exam.app

import ir.exam.app.domain.exam.ExamTiming
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V357 — برابری سایت و اپ برای زمان‌بندی و حالت آفلاین.
 * بردارها با خودِ site/src/examtiming.js تولید شده‌اند (مولد:
 * scripts/gen_examtiming_vectors.mjs). اگر یکی از دو پیاده‌سازی
 * واگرا شود این آزمون می‌شکند.
 */
class V357_ExamTimingParityTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun kotlinMatchesSiteExamTiming() {
        val v = Json.parseToJsonElement(
            File(root(), "app/src/test/resources/v357_examtiming_vectors.json").readText()
        ).jsonObject
        var pass = 0
        val problems = mutableListOf<String>()
        fun ok(cond: Boolean, detail: String) { if (cond) pass++ else problems.add(detail) }
        fun arr(k: String) = v[k]!!.jsonArray
        fun s(el: kotlinx.serialization.json.JsonElement?) =
            if (el == null || el is kotlinx.serialization.json.JsonNull) null else el.jsonPrimitive.content

        arr("latinDigits").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.latinDigits(s(o["in"]))
            ok(got == s(o["out"]), "latinDigits «${s(o["in"])}» → «$got»")
        }

        arr("align").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.alignDeadline(
                o["expires"]!!.jsonPrimitive.long, o["serverNow"]!!.jsonPrimitive.long, o["localNow"]!!.jsonPrimitive.long)
            ok(got == o["out"]!!.jsonPrimitive.long, "align $got vs ${o["out"]}")
        }

        arr("remaining").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.remainingMs(o["deadline"]!!.jsonPrimitive.long, o["now"]!!.jsonPrimitive.long)
            ok(got == o["out"]!!.jsonPrimitive.long, "remaining $got vs ${o["out"]}")
        }

        arr("clock").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.formatClock(o["in"]!!.jsonPrimitive.long)
            ok(got == s(o["out"]), "clock «$got» vs «${s(o["out"])}»")
        }

        arr("question").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.questionState(
                o["limited"]!!.jsonPrimitive.boolean, o["deadline"]!!.jsonPrimitive.long,
                o["now"]!!.jsonPrimitive.long, o["serverLocked"]!!.jsonPrimitive.boolean)
            val w = o["out"]!!.jsonObject
            ok(got.limited == w["limited"]!!.jsonPrimitive.boolean, "question limited")
            ok(got.locked == w["locked"]!!.jsonPrimitive.boolean, "question locked")
            ok(got.remainingMs == w["remainingMs"]!!.jsonPrimitive.long, "question remaining ${got.remainingMs} vs ${w["remainingMs"]}")
            ok(got.clock == s(w["clock"]), "question clock «${got.clock}» vs «${s(w["clock"])}»")
            ok(got.warn == w["warn"]!!.jsonPrimitive.boolean, "question warn")
        }

        arr("validate").forEach { e ->
            val o = e.jsonObject
            val raw = o["minutes"]!!.jsonPrimitive.content.toDouble()
            // ورودی غیرصحیح در سایت رد می‌شود؛ کاتلین نوع Int دارد و
            // همان پیام باید برگردد، پس کفِ عدد را می‌فرستیم.
            val minutes = if (raw != Math.floor(raw)) -1 else raw.toInt()
            val got = ExamTiming.validateGrant(minutes, s(o["reason"]))
            ok(got == s(o["out"]), "validate $minutes «${s(o["reason"])}» → «$got» ولی «${s(o["out"])}»")
        }

        arr("parse").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.parseLimits(s(o["in"]))
            val w = o["out"]!!.jsonObject
            val wl = w["limits"]!!.jsonObject
            ok(got.limits.size == wl.size, "parse size ${got.limits.size} vs ${wl.size} برای «${s(o["in"])}»")
            wl.forEach { (k, value) ->
                ok(got.limits[k.toInt()] == value.jsonPrimitive.int, "parse limit $k")
            }
            ok(got.count == w["count"]!!.jsonPrimitive.int, "parse count")
            val we = w["errors"]!!.jsonArray
            ok(got.errors.size == we.size, "parse errors ${got.errors.size} vs ${we.size}")
            if (got.errors.size == we.size) {
                got.errors.forEachIndexed { i, err ->
                    val x = we[i].jsonObject
                    ok(err.line == x["line"]!!.jsonPrimitive.int, "parse error line #$i")
                    ok(err.message == s(x["message"]), "parse error text #$i «${err.message}» vs «${s(x["message"])}»")
                }
            }
        }

        arr("format").forEach { e ->
            val o = e.jsonObject
            val limits = o["limits"]!!.jsonObject.entries.associate { (k, value) -> k.toInt() to value.jsonPrimitive.int }
            val got = ExamTiming.formatLimits(limits)
            ok(got == s(o["out"]), "format «$got» vs «${s(o["out"])}»")
        }

        arr("offline").forEach { e ->
            val o = e.jsonObject
            val pj = o["pack"]
            val pack = if (pj == null || pj is kotlinx.serialization.json.JsonNull) null else {
                val po = pj.jsonObject
                ExamTiming.OfflinePack(
                    token = s(po["token"]).orEmpty(),
                    used = po["used"]?.jsonPrimitive?.boolean ?: false)
            }
            val got = ExamTiming.offlineSubmittable(pack, o["now"]!!.jsonPrimitive.long, o["deadline"]!!.jsonPrimitive.long)
            val w = o["out"]!!.jsonObject
            ok(got.ok == w["ok"]!!.jsonPrimitive.boolean, "offline ok")
            ok(got.reason == s(w["reason"]), "offline reason «${got.reason}» vs «${s(w["reason"])}»")
        }

        arr("events").forEach { e ->
            val o = e.jsonObject
            val got = ExamTiming.pushEvent(emptyList(), s(o["kind"]), o["at"]!!.jsonPrimitive.long, s(o["detail"]))
            val w = o["out"]!!.jsonArray
            ok(got.size == w.size, "events size")
            if (got.size == w.size) {
                got.forEachIndexed { i, ev ->
                    val x = w[i].jsonObject
                    ok(ev.t == s(x["t"]), "event kind #$i")
                    ok(ev.at == x["at"]!!.jsonPrimitive.long, "event at #$i")
                    ok(ev.d == s(x["d"]), "event detail #$i")
                }
            }
        }

        problems.take(8).forEach { println("  ✗ $it") }
        assertTrue("کاتلین از سایت فاصله گرفت (${problems.size} خطا از $pass سنجه)", problems.isEmpty())
        assertTrue("بردارها باید پر باشند", pass > 500)
    }
}
