package ir.exam.app

import ir.exam.app.domain.bank.BankPicker
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V358 — برابری سایت و اپ در بانک سؤال.
 *
 * بردارها از پیمانهٔ جاوااسکریپت تولید شده‌اند
 * (scripts/gen_bankpick_vectors.mjs). اگر یک نویسه اختلاف باشد،
 * معلم در سایت یک گزینش و در اپ گزینش دیگری می‌بیند.
 */
class V358_BankPickerParityTest {

    private fun root(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (!File(d, "settings.gradle.kts").exists() && d.parentFile != null) d = d.parentFile!!
        return d
    }

    private val vectors: JsonObject by lazy {
        val f = File(root(), "app/src/test/resources/v358_bankpick_vectors.json")
        assertTrue("بردارها ساخته نشده‌اند: node scripts/gen_bankpick_vectors.mjs", f.exists())
        Json.parseToJsonElement(f.readText()).jsonObject
    }

    private fun arr(key: String): JsonArray = vectors[key]?.jsonArray ?: JsonArray(emptyList())

    private fun str(o: JsonObject, k: String): String? =
        (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun cats(a: JsonArray?): List<BankPicker.Category> =
        (a ?: JsonArray(emptyList())).map { e ->
            val o = e.jsonObject
            BankPicker.Category(
                id = o["id"].toString().trim('"'),
                name = str(o, "name") ?: "",
                parentId = str(o, "parent_id") ?: o["parent_id"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonPrimitive?.contentOrNull
            )
        }

    private fun items(a: JsonArray?): List<BankPicker.Item> =
        (a ?: JsonArray(emptyList())).map { e ->
            val o = e.jsonObject
            val q = o["question"]?.jsonObject ?: JsonObject(emptyMap())
            BankPicker.Item(
                id = o["id"].toString().trim('"'),
                subject = str(o, "subject") ?: "",
                text = str(q, "text") ?: "",
                topic = str(q, "topic") ?: "",
                difficulty = q["difficulty"]?.jsonPrimitive?.intOrNull ?: 0,
                grade = q["grade"]?.jsonPrimitive?.intOrNull ?: 0,
                catIds = (o["cat_ids"]?.jsonArray ?: JsonArray(emptyList())).map { it.toString().trim('"') }
            )
        }

    private fun rules(a: JsonArray?): List<BankPicker.Rule> =
        (a ?: JsonArray(emptyList())).map { e ->
            val o = e.jsonObject
            BankPicker.Rule(
                category = str(o, "category") ?: "",
                categoryId = o["categoryId"]?.jsonPrimitive?.contentOrNull,
                difficulty = o["difficulty"]!!.jsonPrimitive.int,
                count = o["count"]!!.jsonPrimitive.int
            )
        }

    private fun strList(a: JsonArray?): List<String> =
        (a ?: JsonArray(emptyList())).map { it.toString().trim('"') }

    @Test
    fun allVectorsMatchTheSiteModule() {
        var checked = 0

        arr("latinDigits").forEach { e ->
            val o = e.jsonObject
            assertEquals("latinDigits", str(o, "output"), BankPicker.latinDigits(str(o, "input")))
            checked++
        }

        arr("subtree").forEach { e ->
            val o = e.jsonObject
            assertEquals("subtreeIds", strList(o["ids"]?.jsonArray), BankPicker.subtreeIds(cats(o["categories"]?.jsonArray), str(o, "root")))
            checked++
        }

        arr("path").forEach { e ->
            val o = e.jsonObject
            assertEquals("categoryPath", str(o, "path"), BankPicker.categoryPath(cats(o["categories"]?.jsonArray), str(o, "id")))
            checked++
        }

        arr("depth").forEach { e ->
            val o = e.jsonObject
            assertEquals("depthOf", o["depth"]!!.jsonPrimitive.int, BankPicker.depthOf(cats(o["categories"]?.jsonArray), str(o, "id")))
            checked++
        }

        arr("filter").forEach { e ->
            val o = e.jsonObject
            val op = o["opts"]?.jsonObject ?: JsonObject(emptyMap())
            val opts = BankPicker.FilterOpts(
                text = str(op, "text") ?: "",
                categoryId = str(op, "categoryId"),
                difficulty = op["difficulty"]?.jsonPrimitive?.intOrNull,
                subject = str(op, "subject") ?: "",
                grade = op["grade"]?.jsonPrimitive?.intOrNull
            )
            val got = BankPicker.filterItems(items(o["items"]?.jsonArray), cats(o["categories"]?.jsonArray), opts).map { it.id }
            assertEquals("filterItems", strList(o["ids"]?.jsonArray), got)
            checked++
        }

        arr("diffWord").forEach { e ->
            val o = e.jsonObject
            val want = o["value"]?.jsonPrimitive?.intOrNull
            assertEquals("difficultyFromWord: " + str(o, "word"), want, BankPicker.difficultyFromWord(str(o, "word")))
            checked++
        }

        arr("parse").forEach { e ->
            val o = e.jsonObject
            val got = BankPicker.parseBlueprint(str(o, "text"), cats(o["categories"]?.jsonArray))
            val wantRules = rules(o["rules"]?.jsonArray)
            assertEquals("parse.rules", wantRules, got.rules)
            assertEquals("parse.errors", strList(o["errors"]?.jsonArray), got.errors)
            assertEquals("parse.total", o["total"]!!.jsonPrimitive.int, got.total)
            checked++
        }

        arr("format").forEach { e ->
            val o = e.jsonObject
            assertEquals("formatBlueprint", str(o, "text"), BankPicker.formatBlueprint(rules(o["rules"]?.jsonArray)))
            checked++
        }

        arr("shuffle").forEach { e ->
            val o = e.jsonObject
            val input = (o["arr"]?.jsonArray ?: JsonArray(emptyList())).map { it.jsonPrimitive.int }
            val want = (o["out"]?.jsonArray ?: JsonArray(emptyList())).map { it.jsonPrimitive.int }
            assertEquals("shuffled", want, BankPicker.shuffled(input, o["seed"]!!.jsonPrimitive.int))
            checked++
        }

        arr("pick").forEach { e ->
            val o = e.jsonObject
            val got = BankPicker.pick(
                items(o["items"]?.jsonArray), cats(o["categories"]?.jsonArray),
                rules(o["rules"]?.jsonArray), o["seed"]!!.jsonPrimitive.int
            )
            assertEquals("pick.ids", strList(o["ids"]?.jsonArray), got.picked.map { it.id })
            assertEquals("pick.total", o["total"]!!.jsonPrimitive.int, got.total)
            assertEquals("pick.summary", str(o, "summary"), BankPicker.summary(got))
            val wantPer = (o["perRule"]?.jsonArray ?: JsonArray(emptyList())).map { it.jsonObject }
            assertEquals("pick.perRule.size", wantPer.size, got.perRule.size)
            wantPer.forEachIndexed { i, w ->
                val g = got.perRule[i]
                assertEquals("perRule.category", str(w, "category"), g.category)
                assertEquals("perRule.difficulty", w["difficulty"]!!.jsonPrimitive.int, g.difficulty)
                assertEquals("perRule.wanted", w["wanted"]!!.jsonPrimitive.int, g.wanted)
                assertEquals("perRule.got", w["got"]!!.jsonPrimitive.int, g.got)
                assertEquals("perRule.shortage", w["shortage"]!!.jsonPrimitive.int, g.shortage)
            }
            checked++
        }

        assertTrue("بردارها خیلی کم‌اند: $checked", checked >= 150)
        println("V358 parity: $checked vectors matched")
    }

    @Test
    fun seededPickIsReproducibleAndNeverDuplicates() {
        val cats = listOf(BankPicker.Category("1", "ریاضی", null), BankPicker.Category("2", "فصل ۱", "1"))
        val items = (1..20).map { BankPicker.Item(id = it.toString(), difficulty = (it % 3) + 1, catIds = listOf("2")) }
        val rules = listOf(
            BankPicker.Rule("همه", null, -1, 7),
            BankPicker.Rule("همه", null, -1, 7)
        )
        val a = BankPicker.pick(items, cats, rules, 42)
        val b = BankPicker.pick(items, cats, rules, 42)
        assertEquals("همان بذر باید همان گزینش بدهد", a.picked.map { it.id }, b.picked.map { it.id })
        assertEquals("هیچ سؤالی دو بار برداشته نشود", 14, a.picked.map { it.id }.toSet().size)
        val c = BankPicker.pick(items, cats, rules, 43)
        assertTrue("بذر دیگر باید گزینش دیگر بدهد", a.picked.map { it.id } != c.picked.map { it.id })
    }

    @Test
    fun constantsMatchTheSite() {
        assertEquals(3, BankPicker.MAX_DEPTH)
        assertEquals(40, BankPicker.MAX_RULES)
        assertEquals(500, BankPicker.MAX_PICK)
        assertEquals(200, BankPicker.MAX_COUNT)
        assertEquals(listOf("تعیین‌نشده", "ساده", "متوسط", "دشوار"), BankPicker.DIFFICULTY_LABELS)
    }
}
