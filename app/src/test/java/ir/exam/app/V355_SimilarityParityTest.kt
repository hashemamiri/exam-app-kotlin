package ir.exam.app

import ir.exam.app.domain.grading.Similarity
import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V355 — آزمون برابری سایت و اپ.
 *
 * بردارها با خودِ site/src/similarity.js تولید شده‌اند (مولد:
 * scripts/gen_similarity_vectors.py). اگر الگوریتم سایت عوض شود و
 * بردارها بازتولید نشوند، این آزمون می‌شکند؛ و اگر کاتلین از سایت
 * فاصله بگیرد هم می‌شکند. پس دو پیاده‌سازی نمی‌توانند بی‌خبر واگرا شوند.
 */
class V355_SimilarityParityTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    @Test
    fun kotlinMatchesSiteAlgorithm() {
    val v = Json.parseToJsonElement(File(root(), "app/src/test/resources/v355_similarity_vectors.json").readText()).jsonObject
    var pass = 0; var fail = 0
        fun ok(name: String, cond: Boolean, extra: String = "") {
        if (cond) pass++ else { fail++; println("  ✗ $name $extra") }
    }
        fun arr(k: String) = v[k]!!.jsonArray
    for (e in arr("normalize")) {
        val i = e.jsonObject["in"]!!.jsonPrimitive.content; val o = e.jsonObject["out"]!!.jsonPrimitive.content
        ok("normalize", Similarity.normalizeText(i) == o, "«$i» → «${Similarity.normalizeText(i)}» ولی «$o»")
    }
    for (e in arr("essayTokens")) {
        val i = e.jsonObject["in"]!!.jsonPrimitive.content
        val o = e.jsonObject["out"]!!.jsonArray.map { it.jsonPrimitive.content }
        ok("essayTokens", Similarity.essayTokens(i) == o, "«$i»")
    }
    for (e in arr("codeTokens")) {
        val i = e.jsonObject["in"]!!.jsonPrimitive.content
        val o = e.jsonObject["out"]!!.jsonArray.map { it.jsonPrimitive.content }
        ok("codeTokens", Similarity.codeTokens(i) == o, "«${i.take(30)}» → ${Similarity.codeTokens(i)} ولی $o")
    }
    for (e in arr("stripFence")) {
        val i = e.jsonObject["in"]!!.jsonPrimitive.content; val o = e.jsonObject["out"]!!.jsonPrimitive.content
        ok("stripFence", Similarity.stripFence(i) == o)
    }
    for (e in arr("stripComments")) {
        val i = e.jsonObject["in"]!!.jsonPrimitive.content; val o = e.jsonObject["out"]!!.jsonPrimitive.content
        ok("stripComments", Similarity.stripComments(i) == o, "«${i.take(30)}»")
    }
    for (e in arr("hash")) {
        val i = e.jsonObject["in"]!!.jsonPrimitive.content; val o = e.jsonObject["out"]!!.jsonPrimitive.int
        ok("hash32", Similarity.hash32(i) == o, "«$i»")
    }
    for (e in arr("shingles")) {
        val t = e.jsonObject["tokens"]!!.jsonArray.map { it.jsonPrimitive.content }
        val k = e.jsonObject["k"]!!.jsonPrimitive.int
        val o = e.jsonObject["out"]!!.jsonArray.map { it.jsonPrimitive.int }
        ok("shingles", Similarity.shingles(t, k) == o)
    }
    for (e in arr("pairKey")) {
        val a = e.jsonObject["a"]!!.jsonPrimitive.content; val b = e.jsonObject["b"]!!.jsonPrimitive.content
        ok("pairKey", Similarity.pairKey(a, b) == e.jsonObject["out"]!!.jsonPrimitive.content)
    }
    for (e in arr("pairs")) {
        val a = e.jsonObject["a"]!!.jsonPrimitive.content; val b = e.jsonObject["b"]!!.jsonPrimitive.content
        val kind = e.jsonObject["kind"]!!.jsonPrimitive.content
        val ta = if (kind == "code") Similarity.codeTokens(a) else Similarity.essayTokens(a)
        val tb = if (kind == "code") Similarity.codeTokens(b) else Similarity.essayTokens(b)
        val r = Similarity.compare(ta, tb, 0)
        val out = e.jsonObject["out"]
        if (out is JsonNull) { ok("compare-null", r == null); continue }
        val o = out!!.jsonObject
        if (r == null) { ok("compare", false, "تهی برگشت"); continue }
        fun d(k: String) = o[k]!!.jsonPrimitive.double
        ok("compare.score", Math.abs(r.score - d("score")) < 1e-9, "${r.score} vs ${d("score")}")
        ok("compare.jaccard", Math.abs(r.jaccard - d("jaccard")) < 1e-9)
        ok("compare.containment", Math.abs(r.containment - d("containment")) < 1e-9)
        ok("compare.shared", r.shared == o["shared"]!!.jsonPrimitive.int)
    }
    for (e in arr("assist")) {
        val ans = e.jsonObject["answer"]!!.jsonPrimitive.content
        val max = e.jsonObject["max"]!!.jsonPrimitive.double
        val kws = e.jsonObject["keywords"]!!.jsonArray.map {
            Similarity.EssayKeyword(it.jsonObject["keyword"]!!.jsonPrimitive.content, it.jsonObject["weight"]!!.jsonPrimitive.double)
        }
        val r = Similarity.essayAssist(kws, ans, max)
        val o = e.jsonObject["out"]!!.jsonObject
        ok("assist.suggested", Math.abs(r.suggested - o["suggested"]!!.jsonPrimitive.double) < 1e-9, "${r.suggested} vs ${o["suggested"]}")
        ok("assist.ratio", Math.abs(r.ratio - o["ratio"]!!.jsonPrimitive.double) < 1e-9)
        ok("assist.matched", r.matched == o["matched"]!!.jsonArray.map { it.jsonPrimitive.content })
        ok("assist.missing", r.missing == o["missing"]!!.jsonArray.map { it.jsonPrimitive.content })
    }
        assertTrue("کاتلین از سایت فاصله گرفت ($fail خطا از $pass سنجه)", fail == 0)
        assertTrue("بردارها باید پر باشند", pass > 300)
    }
}
