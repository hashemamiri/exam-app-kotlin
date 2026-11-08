package ir.exam.app.domain.grading

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * V355 — تشخیص شباهت پاسخ‌ها و دستیار کلیدواژه‌ای تصحیح تشریحی.
 *
 * این فایل آینهٔ دقیق site/src/similarity.js است. هر دو با بردارهای آزمون
 * مشترک به هم بسته شده‌اند؛ اگر یکی عوض شود آزمون می‌شکند. منطق خالص است:
 * نه شبکه، نه اندروید، و مهم‌تر از همه **کد دانش‌آموز هرگز اجرا نمی‌شود**.
 *
 * هشدار: شباهت فقط پرچم هشدار برای معلم است. هیچ نمره‌ای خودکار کم نمی‌شود.
 */
object Similarity {
    const val SHINGLE_K = 5
    const val MIN_TOKENS = 8
    const val THRESHOLD = 0.75
    const val STEP = 0.25

    private val KEYWORDS: Set<String> = (
        "abstract,and,as,assert,async,await,begin,bool,boolean,break,by,case,catch,char," +
        "class,const,continue,def,defer,default,delete,do,double,elif,else,end,enum,except,export," +
        "extends,extern,false,final,finally,float,fn,for,foreach,friend,from,func,function,global," +
        "go,goto,if,impl,import,in,inline,int,interface,is,lambda,let,long,loop,match,mod,module," +
        "mut,namespace,new,nil,none,not,null,nullptr,object,or,override,package,pass,private," +
        "protected,public,raise,range,record,ref,return,select,self,short,signed,sizeof,static," +
        "struct,super,switch,template,then,this,throw,throws,trait,true,try,type,typedef,typename," +
        "union,unless,until,use,using,val,var,virtual,void,volatile,when,where,while,with,yield"
        ).split(',').toSet()

    private val FENCE = Regex("```[A-Za-z0-9_+#.\\-]*[ \\t]*\\r?\\n([\\s\\S]*?)```")
    private val FENCE_HEAD = Regex("```[A-Za-z0-9_+#.\\-]*[ \\t]*\\r?\\n")

    /** فضای خالی دقیقاً مانند \s جاوااسکریپت (در JVM معنای \s فرق دارد). */
    private fun isWs(c: Char): Boolean {
        val x = c.code
        return (x in 0x09..0x0D) || x == 0x20 || x == 0x85 || x == 0xA0 ||
            x == 0x1680 || (x in 0x2000..0x200A) || x == 0x2028 || x == 0x2029 ||
            x == 0x202F || x == 0x205F || x == 0x3000
    }

    private fun isDigit(c: Char) = c in '0'..'9'
    private fun isIdentStart(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_' || c == '$'
    private fun isIdentPart(c: Char) = isIdentStart(c) || isDigit(c)

    /** حرف یا شماره، هم‌ارز \p{L}\p{N} جاوااسکریپت (شامل No و Nl). */
    private fun isLetterOrNumber(c: Char): Boolean = when (Character.getType(c)) {
        Character.UPPERCASE_LETTER.toInt(), Character.LOWERCASE_LETTER.toInt(),
        Character.TITLECASE_LETTER.toInt(), Character.MODIFIER_LETTER.toInt(),
        Character.OTHER_LETTER.toInt(), Character.DECIMAL_DIGIT_NUMBER.toInt(),
        Character.LETTER_NUMBER.toInt(), Character.OTHER_NUMBER.toInt() -> true
        else -> false
    }

    private fun foldChar(ch: Char): String = when (ch.code) {
        0x064A -> "\u06CC" // ي → ی
        0x0643 -> "\u06A9" // ك → ک
        0x200C -> " "      // نیم‌فاصله → فاصله
        in 0x0660..0x0669 -> (48 + ch.code - 0x0660).toChar().toString()
        in 0x06F0..0x06F9 -> (48 + ch.code - 0x06F0).toChar().toString()
        else -> ch.toString()
    }

    fun normalizeText(s: String?): String {
        val src = s.orEmpty()
        val sb = StringBuilder()
        for (ch in src) {
            // مانند جاوااسکریپت، کوچک‌سازی روی رشته انجام می‌شود (ممکن است بسط یابد)
            for (low in foldChar(ch).lowercase()) {
                sb.append(if (isLetterOrNumber(low)) low else ' ')
            }
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun essayTokens(s: String?): List<String> {
        val n = normalizeText(s)
        return if (n.isEmpty()) emptyList() else n.split(' ')
    }

    /** برداشتن کد از قالب سه‌بک‌تیک؛ اگر نبود، همان متن. */
    fun stripFence(text: String?): String {
        val src = text.orEmpty()
        val parts = FENCE.findAll(src).map { it.groupValues[1] }.toList()
        return if (parts.isNotEmpty()) parts.joinToString("\n") else src
    }

    /** حذف توضیحات کد با پویش دستی، دقیقاً مانند سایت. */
    fun stripComments(text: String?): String {
        val s = text.orEmpty()
        val out = StringBuilder()
        var i = 0
        val n = s.length
        while (i < n) {
            val c = s[i]
            if (c == '/' && i + 1 < n && s[i + 1] == '*') {
                val close = s.indexOf("*/", i + 2)
                i = if (close < 0) n else close + 2
                out.append(' ')
                continue
            }
            if ((c == '/' && i + 1 < n && s[i + 1] == '/') || c == '#') {
                while (i < n && s[i] != '\n' && s[i] != '\r') i++
                out.append(' ')
                continue
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    /** توکن‌سازی کد: رشته→STR، عدد→NUM، شناسهٔ غیرکلیدواژه→ID. */
    fun codeTokens(s: String?): List<String> {
        val src = stripComments(stripFence(s))
        val out = mutableListOf<String>()
        var i = 0
        val n = src.length
        fun at(k: Int): Char = if (k in 0 until n) src[k] else '\u0000'
        while (i < n) {
            val c = src[i]
            if (isWs(c)) { i++; continue }
            if (c == '"' || c == '\'') {
                val q = c
                i++
                while (i < n) {
                    if (src[i] == '\\') { i += 2; continue }
                    if (src[i] == q) { i++; break }
                    i++
                }
                out.add("STR")
                continue
            }
            if (isDigit(c)) {
                while (i < n && isDigit(src[i])) i++
                if (at(i) == '.' && isDigit(at(i + 1))) {
                    i++
                    while (i < n && isDigit(src[i])) i++
                }
                out.add("NUM")
                continue
            }
            if (isIdentStart(c)) {
                val j = i
                while (i < n && isIdentPart(src[i])) i++
                val low = src.substring(j, i).lowercase()
                out.add(if (low in KEYWORDS) low else "ID")
                continue
            }
            out.add(c.toString())
            i++
        }
        return out
    }

    /** همان هش ۳۲ بیتی: x = x*31 + کد نویسه، با سرریز علامت‌دار. */
    fun hash32(s: String?): Int {
        var x = 0
        for (ch in s.orEmpty()) x = x * 31 + ch.code
        return x
    }

    fun shingles(tokens: List<String>, k: Int = SHINGLE_K): List<Int> {
        if (tokens.isEmpty()) return emptyList()
        if (tokens.size < k) return listOf(hash32(tokens.joinToString("\u0001")))
        val seen = HashSet<Int>()
        val out = mutableListOf<Int>()
        var i = 0
        while (i + k <= tokens.size) {
            val h = hash32(tokens.subList(i, i + k).joinToString("\u0001"))
            if (seen.add(h)) out.add(h)
            i++
        }
        return out
    }

    data class Comparison(
        val jaccard: Double,
        val containment: Double,
        val score: Double,
        val shared: Int,
        val sizeA: Int,
        val sizeB: Int
    )

    fun compare(a: List<String>?, b: List<String>?, minTokens: Int = MIN_TOKENS): Comparison? {
        if (a == null || b == null) return null
        if (a.size < minTokens || b.size < minTokens) return null
        val A = shingles(a)
        val B = shingles(b)
        if (A.isEmpty() || B.isEmpty()) return null
        val setA = A.toHashSet()
        val shared = B.count { it in setA }
        val union = A.size + B.size - shared
        val jac = if (union > 0) shared.toDouble() / union else 0.0
        val small = minOf(A.size, B.size)
        val con = if (small > 0) shared.toDouble() / small else 0.0
        return Comparison(jac, con, maxOf(jac, con), shared, A.size, B.size)
    }

    /** کلید پایدار جفت پاسخ؛ ترتیب شناسه‌ها مهم نیست. */
    fun pairKey(a: String, b: String): String = if (a <= b) "$a|$b" else "$b|$a"

    fun isCodeAnswer(questionMode: String?, raw: String?): Boolean {
        if (questionMode == "code") return true
        return FENCE_HEAD.containsMatchIn(raw.orEmpty())
    }

    /** پاسخ یک زیربخش از رشتهٔ JSON سؤال چندبخشی. */
    fun partText(raw: String?, partId: String, withReason: Boolean): String {
        val text = raw?.trim().orEmpty()
        if (!text.startsWith("{")) return ""
        val obj = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return ""
        val parts = obj["parts"] as? JsonObject ?: return ""
        val entry = parts[partId] as? JsonObject ?: return ""
        val answer = entry["answer"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val reason = entry["reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return if (withReason && reason.isNotEmpty()) "$answer $reason" else answer
    }

    data class EssayKeyword(val keyword: String, val weight: Double = 1.0)

    data class AssistResult(
        val matched: List<String>,
        val missing: List<String>,
        val ratio: Double,
        val suggested: Double,
        val weightTotal: Double,
        val weightGot: Double
    )

    private fun containsSeq(hay: List<String>, needle: List<String>): Boolean {
        if (needle.isEmpty() || needle.size > hay.size) return false
        var i = 0
        while (i + needle.size <= hay.size) {
            var all = true
            for (j in needle.indices) {
                if (hay[i + j] != needle[j]) { all = false; break }
            }
            if (all) return true
            i++
        }
        return false
    }

    /** کلیدواژه می‌تواند چند بدل داشته باشد: «وتر|ضلع بزرگ». */
    fun keywordMatch(keyword: String?, answerTokens: List<String>): Boolean =
        keyword.orEmpty().split('|').any { alt ->
            val tk = essayTokens(alt)
            tk.isNotEmpty() && containsSeq(answerTokens, tk)
        }

    private fun roundStep(v: Double) = Math.round(v / STEP) * STEP

    /**
     * پیشنهاد نمره بر پایهٔ کلیدواژه‌های وزن‌دار. فقط پیشنهاد است؛
     * ذخیره نمی‌شود و معلم آن را می‌پذیرد یا ویرایش می‌کند.
     */
    fun essayAssist(keywords: List<EssayKeyword>, answerText: String?, maxScore: Double): AssistResult {
        val tk = essayTokens(answerText)
        var total = 0.0
        var got = 0.0
        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()
        keywords.forEach { k ->
            val w = if (k.weight > 0) k.weight else 1.0
            total += w
            if (keywordMatch(k.keyword, tk)) { got += w; matched.add(k.keyword) } else missing.add(k.keyword)
        }
        val ratio = if (total > 0) got / total else 0.0
        val suggested = maxOf(0.0, minOf(maxScore, roundStep(maxScore * ratio)))
        return AssistResult(matched, missing, ratio, suggested, total, got)
    }

    // ------------------------------------------------------------------
    // واحدهای مقایسه و یافتن جفت‌های مشکوک
    // ------------------------------------------------------------------

    data class Unit(
        val questionIndex: Int,
        val partKey: String,
        val kind: String,
        val byAnswer: Map<String, List<String>>
    )

    data class Hit(
        val pairKey: String,
        val aId: String,
        val bId: String,
        val questionIndex: Int,
        val partKey: String,
        val kind: String,
        val jaccard: Double,
        val containment: Double,
        val score: Double
    )

    /** یک پاسخ خام برای مقایسه. */
    data class RawAnswer(val id: String, val responses: List<String?>)

    /** توصیف سؤال برای استخراج واحدها. */
    data class QuestionShape(val mode: String?, val parts: List<PartShape>)
    data class PartShape(val id: String, val requiresReason: Boolean)

    fun extractUnits(
        questions: List<QuestionShape>,
        answers: List<RawAnswer>,
        minTokens: Int = MIN_TOKENS
    ): List<Unit> {
        val units = mutableListOf<Unit>()
        questions.forEachIndexed { qi, q ->
            if (q.parts.isNotEmpty()) {
                q.parts.forEach { p ->
                    val by = LinkedHashMap<String, List<String>>()
                    answers.forEach { a ->
                        val text = partText(a.responses.getOrNull(qi), p.id, p.requiresReason)
                        val tk = essayTokens(text)
                        if (tk.size >= minTokens) by[a.id] = tk
                    }
                    if (by.size >= 2) units.add(Unit(qi, p.id, "essay", by))
                }
                return@forEachIndexed
            }
            val by = LinkedHashMap<String, List<String>>()
            val kinds = HashMap<String, String>()
            answers.forEach { a ->
                val raw = a.responses.getOrNull(qi).orEmpty()
                val code = isCodeAnswer(q.mode, raw)
                val tk = if (code) codeTokens(raw) else essayTokens(raw)
                if (tk.size >= minTokens) { by[a.id] = tk; kinds[a.id] = if (code) "code" else "essay" }
            }
            if (by.size >= 2) {
                val allCode = by.keys.all { kinds[it] == "code" }
                units.add(Unit(qi, "", if (allCode) "code" else "essay", by))
            }
        }
        return units
    }

    fun findSimilar(units: List<Unit>, threshold: Double = THRESHOLD): List<Hit> {
        val out = mutableListOf<Hit>()
        units.forEach { u ->
            val ids = u.byAnswer.keys.toList()
            for (i in ids.indices) {
                for (j in i + 1 until ids.size) {
                    val r = compare(u.byAnswer[ids[i]], u.byAnswer[ids[j]], 0) ?: continue
                    if (r.score < threshold) continue
                    val a = if (ids[i] <= ids[j]) ids[i] else ids[j]
                    val b = if (ids[i] <= ids[j]) ids[j] else ids[i]
                    out.add(Hit(pairKey(ids[i], ids[j]), a, b, u.questionIndex, u.partKey, u.kind,
                        r.jaccard, r.containment, r.score))
                }
            }
        }
        return out.sortedWith(
            compareByDescending<Hit> { it.score }
                .thenBy { it.questionIndex }
                .thenBy { it.pairKey }
        )
    }
}

/** وضعیت بررسی یک هشدار شباهت — تصمیم معلم، نه نمره. */
enum class SimilarityReviewStatus(val wire: String, val label: String) {
    OPEN("open", "بررسی‌نشده"),
    CHECKED("checked", "بررسی شد — مشکلی نبود"),
    CONFIRMED("confirmed", "تخلف تأیید شد");

    companion object {
        fun fromWire(value: String?): SimilarityReviewStatus =
            entries.firstOrNull { it.wire == value } ?: OPEN
    }
}

/** یک ردیف کلیدواژه از سرور. */
data class EssayKeywordRow(
    val questionIndex: Int,
    val partKey: String,
    val keyword: String,
    val weight: Double
) {
    companion object {
        fun listFrom(array: kotlinx.serialization.json.JsonArray): List<EssayKeywordRow> =
            array.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val kw = o["keyword"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                EssayKeywordRow(
                    questionIndex = o["question_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                    partKey = o["part_key"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    keyword = kw,
                    weight = o["weight"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 1.0
                )
            }
    }
}

/** تصمیم ثبت‌شدهٔ معلم درباره یک جفت پاسخ. */
data class SimilarityReviewRow(
    val pairKey: String,
    val questionIndex: Int,
    val partKey: String,
    val status: SimilarityReviewStatus,
    val note: String?
) {
    /** کلید ترکیبی برای یافتن سریع تصمیم یک هشدار. */
    val lookupKey: String get() = "$pairKey#$questionIndex#$partKey"

    companion object {
        fun lookupKey(pairKey: String, questionIndex: Int, partKey: String) = "$pairKey#$questionIndex#$partKey"

        fun listFrom(array: kotlinx.serialization.json.JsonArray): List<SimilarityReviewRow> =
            array.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val pk = o["pair_key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                SimilarityReviewRow(
                    pairKey = pk,
                    questionIndex = o["question_index"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: -1,
                    partKey = o["part_key"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    status = SimilarityReviewStatus.fromWire(o["status"]?.jsonPrimitive?.contentOrNull),
                    note = o["note"]?.jsonPrimitive?.contentOrNull
                )
            }
    }
}
