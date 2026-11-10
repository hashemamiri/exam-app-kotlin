package ir.exam.app.domain.bank

/**
 * V358 — بانک سؤال: درختواره، پالایش دشواری، انتخاب خودکار با نقشهٔ آزمون.
 *
 * آینهٔ دقیق site/src/bankpick.js. پیمانه «خالص» است: نه شبکه، نه
 * اندروید، نه ساعت. همان بذر در سایت و اپ همان گزینش را می‌دهد.
 *
 * قاعدهٔ اعتماد: انتخاب تصادفی بازتولیدپذیر است؛ کمبود هر قاعده جدا
 * گزارش می‌شود تا معلم بداند کدام بخش بانک خالی است.
 */
object BankPicker {

    const val MAX_DEPTH = 3
    const val MAX_RULES = 40
    const val MAX_PICK = 500
    const val MAX_COUNT = 200

    /** سطح‌ها: صفر یعنی تعیین‌نشده. چهار سطح، تصمیم V360 کاربر. */
    val DIFFICULTY_LABELS = listOf("تعیین‌نشده", "ساده", "متوسط", "دشوار", "خیلی دشوار")

    data class Category(val id: String, val name: String, val parentId: String? = null)

    data class Item(
        val id: String,
        val subject: String = "",
        val text: String = "",
        val topic: String = "",
        val difficulty: Int = 0,
        val grade: Int = 0,
        val catIds: List<String> = emptyList()
    )

    data class Rule(val category: String, val categoryId: String?, val difficulty: Int, val count: Int)

    data class Parsed(val rules: List<Rule>, val errors: List<String>, val total: Int)

    data class RuleResult(
        val category: String,
        val difficulty: Int,
        val wanted: Int,
        val got: Int,
        val shortage: Int
    )

    data class PickResult(val picked: List<Item>, val perRule: List<RuleResult>, val total: Int)

    data class FilterOpts(
        val text: String = "",
        val categoryId: String? = null,
        val difficulty: Int? = null,
        val subject: String = "",
        val grade: Int? = null
    )

    // ------------------------------------------------------------------
    // یکسان‌سازی رقم و متن
    // ------------------------------------------------------------------

    fun latinDigits(s: String?): String {
        val src = s ?: return ""
        val sb = StringBuilder(src.length)
        for (ch in src) {
            when (ch.code) {
                in 0x6F0..0x6F9 -> sb.append(('0'.code + (ch.code - 0x6F0)).toChar())
                in 0x660..0x669 -> sb.append(('0'.code + (ch.code - 0x660)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun norm(s: String?): String = (s ?: "").trim().lowercase()

    // ------------------------------------------------------------------
    // درخت
    // ------------------------------------------------------------------

    /** همهٔ شناسه‌های زیردرخت، شامل خود گره. حلقه‌ها بی‌خطر شکسته می‌شوند. */
    fun subtreeIds(categories: List<Category>, rootId: String?): List<String> {
        if (rootId.isNullOrEmpty()) return emptyList()
        val kids = HashMap<String, MutableList<String>>()
        categories.forEach { c ->
            kids.getOrPut(c.parentId ?: "") { mutableListOf() }.add(c.id)
        }
        val out = ArrayList<String>()
        val seen = HashSet<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(rootId)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!seen.add(id)) continue
            out.add(id)
            kids[id]?.forEach { k -> if (k !in seen) stack.addLast(k) }
        }
        return out
    }

    fun categoryPath(categories: List<Category>, id: String?): String {
        val m = categories.associateBy { it.id }
        val parts = ArrayList<String>()
        var cur = id ?: ""
        var guard = 0
        while (cur.isNotEmpty() && m.containsKey(cur) && guard++ < MAX_DEPTH + 2) {
            parts.add(0, m[cur]?.name ?: "")
            cur = m[cur]?.parentId ?: ""
        }
        return parts.joinToString(" / ")
    }

    fun depthOf(categories: List<Category>, id: String?): Int {
        val m = categories.associateBy { it.id }
        var d = 0
        var cur = id ?: ""
        var guard = 0
        while (cur.isNotEmpty() && m.containsKey(cur) && guard++ < MAX_DEPTH + 2) {
            d++
            cur = m[cur]?.parentId ?: ""
        }
        return d
    }

    // ------------------------------------------------------------------
    // پالایش
    // ------------------------------------------------------------------

    fun itemDifficulty(it: Item): Int = if (it.difficulty < 0 || it.difficulty > 4) 0 else it.difficulty

    fun filterItems(items: List<Item>, categories: List<Category>, opts: FilterOpts): List<Item> {
        val s = norm(latinDigits(opts.text))
        val allow: Set<String>? = opts.categoryId?.takeIf { it.isNotEmpty() }
            ?.let { subtreeIds(categories, it).toHashSet() }
        return items.filter { it ->
            if (s.isNotEmpty()) {
                val hay = norm(latinDigits(it.text + " " + it.subject + " " + it.topic))
                if (!hay.contains(s)) return@filter false
            }
            if (allow != null && it.catIds.none { c -> c in allow }) return@filter false
            if (opts.difficulty != null && itemDifficulty(it) != opts.difficulty) return@filter false
            if (opts.subject.isNotEmpty() && norm(it.subject) != norm(opts.subject)) return@filter false
            if (opts.grade != null && it.grade != opts.grade) return@filter false
            true
        }
    }

    // ------------------------------------------------------------------
    // نقشهٔ آزمون
    // ------------------------------------------------------------------

    /** ‎-۱ یعنی «هر سطحی»؛ null یعنی واژهٔ نامعتبر. */
    fun difficultyFromWord(w: String?): Int? {
        return when (norm(latinDigits(w))) {
            "", "هر", "همه", "any", "*", "-" -> -1
            "0", "تعیین‌نشده", "نامشخص", "unset" -> 0
            "1", "ساده", "آسان", "easy" -> 1
            "2", "متوسط", "medium" -> 2
            "3", "دشوار", "سخت", "hard" -> 3
            "4", "خیلی دشوار", "بسیار دشوار", "veryhard" -> 4
            else -> null
        }
    }

    /** نتیجهٔ جست‌وجوی دسته: شناسه، یا خطا، یا «همه». */
    data class CatLookup(val id: String?, val error: String?)

    fun findCategory(categories: List<Category>, name: String?): CatLookup {
        val t = norm(latinDigits(name))
        if (t.isEmpty() || t == "همه" || t == "هر" || t == "all" || t == "*") return CatLookup(null, null)
        // نام دسته هم ممکن است رقم فارسی داشته باشد؛ هر دو طرف یکسان می‌شوند.
        val exact = categories.filter { norm(latinDigits(it.name)) == t }
        if (exact.size == 1) return CatLookup(exact[0].id, null)
        if (exact.size > 1) {
            return CatLookup(null, "چند دسته با نام «" + (name ?: "") + "» هست؛ از مسیر کامل استفاده کنید.")
        }
        val byPath = categories.filter { norm(latinDigits(categoryPath(categories, it.id))) == t }
        if (byPath.size == 1) return CatLookup(byPath[0].id, null)
        return CatLookup(null, "دستهٔ «" + (name ?: "") + "» پیدا نشد.")
    }

    /**
     * قالب هر خط:  دسته | دشواری | تعداد
     * نمونه:       فصل ۱ | آسان | ۵
     *              همه | هر | ۱۰
     * خط‌های خالی و # نادیده گرفته می‌شوند.
     */
    fun parseBlueprint(text: String?, categories: List<Category>): Parsed {
        val rules = ArrayList<Rule>()
        val errors = ArrayList<String>()
        val lines = (text ?: "").split("\n")
        for (i in lines.indices) {
            val raw = lines[i].trim()
            if (raw.isEmpty() || raw[0] == '#') continue
            val parts = raw.split("|").map { it.trim() }
            if (parts.size != 3) {
                errors.add("خط " + (i + 1) + ": قالب باید «دسته | دشواری | تعداد» باشد.")
                continue
            }
            val cat = findCategory(categories, parts[0])
            if (cat.error != null) {
                errors.add("خط " + (i + 1) + ": " + cat.error)
                continue
            }
            val d = difficultyFromWord(parts[1])
            if (d == null) {
                errors.add("خط " + (i + 1) + ": دشواری باید ساده، متوسط، دشوار یا «هر» باشد.")
                continue
            }
            val digits = latinDigits(parts[2])
            val n = if (digits.isNotEmpty() && digits.all { it in '0'..'9' }) digits.toIntOrNull() else null
            if (n == null || n < 1 || n > MAX_COUNT) {
                errors.add("خط " + (i + 1) + ": تعداد باید عددی بین ۱ تا " + MAX_COUNT + " باشد.")
                continue
            }
            rules.add(Rule(parts[0], cat.id, d, n))
            if (rules.size > MAX_RULES) {
                errors.add("تعداد قاعده‌ها بیش از " + MAX_RULES + " است.")
                break
            }
        }
        val total = rules.sumOf { it.count }
        if (total > MAX_PICK) errors.add("مجموع درخواست " + total + " سؤال است؛ سقف " + MAX_PICK + " است.")
        return Parsed(rules, errors, total)
    }

    fun formatBlueprint(rules: List<Rule>): String = rules.joinToString("\n") { r ->
        val d = if (r.difficulty < 0) "هر" else DIFFICULTY_LABELS[r.difficulty]
        r.category + " | " + d + " | " + r.count
    }

    // ------------------------------------------------------------------
    // انتخاب
    // ------------------------------------------------------------------

    /** مولد شبه‌تصادفی ۳۲ بیتی — عیناً در جاوااسکریپت تکرار می‌شود. */
    fun rng(seed: Int): () -> Int {
        var s = if (seed == 0) 1 else seed
        return {
            s = (s * 1103515245 + 12345) and 0x7fffffff
            s
        }
    }

    fun <T> shuffled(arr: List<T>, seed: Int): List<T> {
        val a = arr.toMutableList()
        val next = rng(seed)
        for (i in a.size - 1 downTo 1) {
            val j = next() % (i + 1)
            val t = a[i]; a[i] = a[j]; a[j] = t
        }
        return a
    }

    /**
     * قواعد به ترتیب نوشته‌شده اجرا می‌شوند؛ هیچ سؤالی دو بار برداشته
     * نمی‌شود. کمبود هر قاعده جدا گزارش می‌شود.
     */
    fun pick(items: List<Item>, categories: List<Category>, rules: List<Rule>, seed: Int): PickResult {
        val used = HashSet<String>()
        val picked = ArrayList<Item>()
        val perRule = ArrayList<RuleResult>()
        rules.forEachIndexed { idx, r ->
            val pool = filterItems(
                items, categories,
                FilterOpts(categoryId = r.categoryId, difficulty = if (r.difficulty < 0) null else r.difficulty)
            ).filter { it.id !in used }
            val order = shuffled(pool, seed + idx * 7919)
            val take = order.take(minOf(r.count, order.size))
            take.forEach { used.add(it.id); picked.add(it) }
            perRule.add(
                RuleResult(r.category, r.difficulty, r.count, take.size, maxOf(0, r.count - take.size))
            )
        }
        return PickResult(picked, perRule, picked.size)
    }

    fun summary(result: PickResult): String {
        val short = result.perRule.filter { it.shortage > 0 }
        if (short.isEmpty()) return "همهٔ " + result.total + " سؤال انتخاب شد."
        return result.total.toString() + " سؤال انتخاب شد؛ " +
            short.joinToString("، ") { it.category + " (" + it.shortage + " کمبود)" }
    }
}
