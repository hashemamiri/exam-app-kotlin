package ir.exam.app.core.speech

/**
 * V108 — تبدیل هوشمند گفتار (فارسی/انگلیسی) به متن سؤال:
 * - عددهای گفتاری («بیست و پنج»، «twenty five»، «سه ممیز چهارده») → رقم؛
 * - نمادها («درصد»، «درجه»، «مثبت»، «برابر»، «بزرگ‌تر از» …) → علامت؛
 * - عبارت‌های ریاضی («ایکس به توان دو»، «رادیکال سه»، «کسر یک روی دو»،
 *   «انتگرال»، «سینوس ایکس»، «x squared», «square root of two» …) →
 *   فرمول LaTeX داخل `$...$` (همان قالبی که ویرایشگر فرمول می‌شناسد).
 * خروجی می‌تواند یک‌راست در متن درج شود یا پیش از درج در ویرایشگر فرمول
 * بازبینی شود (FormulaHostDialog). بدون وابستگی اندروید؛ تست‌پذیر با JUnit.
 */
object SpeechMathConverter {

    data class Result(val text: String, val containsFormula: Boolean)

    // ------------------------------------------------------------ اعداد
    private val faUnits = mapOf(
        "صفر" to 0, "یک" to 1, "یه" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5, "شش" to 6, "شیش" to 6,
        "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10, "یازده" to 11, "دوازده" to 12, "سیزده" to 13,
        "چهارده" to 14, "پانزده" to 15, "پونزده" to 15, "شانزده" to 16, "شونزده" to 16, "هفده" to 17,
        "هیفده" to 17, "هجده" to 18, "هیجده" to 18, "نوزده" to 19, "بیست" to 20, "سی" to 30, "چهل" to 40,
        "پنجاه" to 50, "شصت" to 60, "هفتاد" to 70, "هشتاد" to 80, "نود" to 90, "صد" to 100, "یکصد" to 100,
        "دویست" to 200, "سیصد" to 300, "چهارصد" to 400, "پانصد" to 500, "پونصد" to 500, "ششصد" to 600,
        "شیشصد" to 600, "هفتصد" to 700, "هشتصد" to 800, "نهصد" to 900
    )
    private val faScales = mapOf("هزار" to 1_000L, "میلیون" to 1_000_000L, "میلیارد" to 1_000_000_000L)
    private val enUnits = mapOf(
        "zero" to 0, "oh" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
        "eighty" to 80, "ninety" to 90
    )
    private val enScales = mapOf("hundred" to 100L, "thousand" to 1_000L, "million" to 1_000_000L, "billion" to 1_000_000_000L)

    // ------------------------------------------------------------ نمادهای متنی (بدون فرمول)
    private val plainSymbols: List<Pair<Regex, String>> = listOf(
        Regex("\\bدرصد\\b") to "٪",
        Regex("\\bpercent\\b", RegexOption.IGNORE_CASE) to "%",
        Regex("\\bدرجه\\b") to "°",
        Regex("\\bdegrees?\\b", RegexOption.IGNORE_CASE) to "°",
        Regex("\\bعلامت سوال\\b|\\bعلامت سؤال\\b") to "؟",
        Regex("\\bquestion mark\\b", RegexOption.IGNORE_CASE) to "?",
        Regex("\\bنقطه سر خط\\b|\\bسر خط\\b") to "\n",
        Regex("\\bnew line\\b", RegexOption.IGNORE_CASE) to "\n",
        Regex("\\bویرگول\\b") to "،",
        Regex("\\bcomma\\b", RegexOption.IGNORE_CASE) to ",",
        Regex("\\bدو نقطه\\b") to ":",
        Regex("\\bcolon\\b", RegexOption.IGNORE_CASE) to ":",
        Regex("\\bپرانتز باز\\b") to "(",
        Regex("\\bپرانتز بسته\\b") to ")",
        Regex("\\bopen paren(?:thesis)?\\b", RegexOption.IGNORE_CASE) to "(",
        Regex("\\bclose paren(?:thesis)?\\b", RegexOption.IGNORE_CASE) to ")",
        Regex("\\bجای خالی\\b") to "……",
        Regex("\\bblank\\b", RegexOption.IGNORE_CASE) to "……"
    )

    // ------------------------------------------------------------ واژگان ریاضی → LaTeX
    private val greek = mapOf(
        "آلفا" to "\\alpha", "بتا" to "\\beta", "گاما" to "\\gamma", "دلتا" to "\\delta", "تتا" to "\\theta",
        "لاندا" to "\\lambda", "لامبدا" to "\\lambda", "میو" to "\\mu", "پی" to "\\pi", "سیگما" to "\\sigma",
        "امگا" to "\\omega", "فی" to "\\phi", "رو" to "\\rho", "اپسیلون" to "\\epsilon",
        "alpha" to "\\alpha", "beta" to "\\beta", "gamma" to "\\gamma", "delta" to "\\delta", "theta" to "\\theta",
        "lambda" to "\\lambda", "mu" to "\\mu", "pi" to "\\pi", "sigma" to "\\sigma", "omega" to "\\omega",
        "phi" to "\\phi", "rho" to "\\rho", "epsilon" to "\\epsilon"
    )
    private val variables = mapOf(
        "ایکس" to "x", "ایگرگ" to "y", "وای" to "y", "زد" to "z", "زت" to "z", "ان" to "n", "اِن" to "n",
        "ام" to "m", "کا" to "k", "آر" to "r", "ار" to "r", "آ" to "a", "بی" to "b", "سی" to "c", "تی" to "t", "اف" to "f",
        "x" to "x", "y" to "y", "z" to "z", "n" to "n", "m" to "m", "k" to "k", "a" to "a", "b" to "b",
        "c" to "c", "t" to "t", "f" to "f", "ex" to "x", "why" to "y", "zed" to "z", "zee" to "z"
    )
    private val functions = mapOf(
        "سینوس" to "\\sin", "کسینوس" to "\\cos", "تانژانت" to "\\tan", "کتانژانت" to "\\cot", "لگاریتم" to "\\log",
        "لگاریتم طبیعی" to "\\ln", "sine" to "\\sin", "sin" to "\\sin", "cosine" to "\\cos", "cos" to "\\cos",
        "tangent" to "\\tan", "tan" to "\\tan", "cotangent" to "\\cot", "log" to "\\log", "logarithm" to "\\log",
        "natural log" to "\\ln", "ln" to "\\ln"
    )
    private val operators: List<Pair<Regex, String>> = listOf(
        Regex("\\b(?:به علاوه|بعلاوه|به‌علاوه|مثبت|جمع)\\b") to " + ",
        Regex("\\b(?:منهای|منفی|تفریق)\\b") to " - ",
        Regex("\\b(?:ضرب در|ضربدر|ضرب)\\b") to " \\times ",
        Regex("\\b(?:تقسیم بر|تقسیم)\\b") to " \\div ",
        Regex("\\b(?:مساوی است با|مساوی|برابر است با|برابر با|برابر|میشود|می‌شود)\\b") to " = ",
        Regex("\\b(?:نامساوی|مخالف)\\b") to " \\neq ",
        Regex("\\b(?:بزرگتر مساوی|بزرگ‌تر مساوی|بزرگتر یا مساوی|بزرگ‌تر یا مساوی)\\b") to " \\geq ",
        Regex("\\b(?:کوچکتر مساوی|کوچک‌تر مساوی|کوچکتر یا مساوی|کوچک‌تر یا مساوی)\\b") to " \\leq ",
        Regex("\\b(?:بزرگتر از|بزرگ‌تر از|بزرگتر|بزرگ‌تر)\\b") to " > ",
        Regex("\\b(?:کوچکتر از|کوچک‌تر از|کوچکتر|کوچک‌تر)\\b") to " < ",
        Regex("\\b(?:تقریبا برابر|تقریباً برابر|تقریبا|تقریباً)\\b") to " \\approx ",
        Regex("\\bبی نهایت\\b|\\bبینهایت\\b|\\bبی‌نهایت\\b") to " \\infty ",
        Regex("\\bplus\\b", RegexOption.IGNORE_CASE) to " + ",
        Regex("\\bminus\\b", RegexOption.IGNORE_CASE) to " - ",
        Regex("(?<!\\\\)\\btimes\\b|\\bmultiplied by\\b", RegexOption.IGNORE_CASE) to " \\times ",
        Regex("\\bdivided by\\b", RegexOption.IGNORE_CASE) to " \\div ",
        Regex("\\bis equal to\\b|\\bequals\\b|\\bequal to\\b|\\bequal\\b", RegexOption.IGNORE_CASE) to " = ",
        Regex("\\bnot equal to\\b|\\bnot equal\\b", RegexOption.IGNORE_CASE) to " \\neq ",
        Regex("\\bgreater than or equal to\\b", RegexOption.IGNORE_CASE) to " \\geq ",
        Regex("\\bless than or equal to\\b", RegexOption.IGNORE_CASE) to " \\leq ",
        Regex("\\bgreater than\\b", RegexOption.IGNORE_CASE) to " > ",
        Regex("\\bless than\\b", RegexOption.IGNORE_CASE) to " < ",
        Regex("\\bapproximately\\b", RegexOption.IGNORE_CASE) to " \\approx ",
        Regex("(?<!\\\\)\\binfinity\\b", RegexOption.IGNORE_CASE) to " \\infty "
    )

    private val mathTriggers = listOf(
        "به توان", "توان", "رادیکال", "جذر", "کسر", "روی", "تقسیم بر", "ضرب در", "ضربدر", "به علاوه", "منهای",
        "مساوی", "برابر", "انتگرال", "سیگما", "مجموع", "حد", "مشتق", "بزرگتر", "بزرگ‌تر", "کوچکتر", "کوچک‌تر",
        "سینوس", "کسینوس", "تانژانت", "لگاریتم", "فاکتوریل", "بی نهایت", "بی‌نهایت", "پی", "آلفا", "بتا", "تتا",
        "power", "squared", "cubed", "square root", "root", "fraction", "over", "divided by", "times", "plus", "minus",
        "equals", "equal", "integral", "sum", "limit", "derivative", "greater than", "less than", "sine", "cosine",
        "tangent", "log", "factorial", "infinity", "pi", "alpha", "beta", "theta", "delta"
    )

    // ------------------------------------------------------------ V109: اصلاح خطاهای رایج موتور گفتار فارسی
    private val corrections: List<Pair<Regex, String>> = listOf(
        Regex("\\bبتوان\\b") to "به توان", Regex("\\bبه توانه\\b") to "به توان",
        Regex("\\bبعلاوه\\b|\\bبه علاوه ی\\b|\\bبعلاوه ی\\b") to "به علاوه",
        Regex("\\bاکس\\b|\\bایکسه\\b|\\bایکز\\b|\\bاکسی\\b") to "ایکس",
        Regex("\\bایگرک\\b|\\bایگرگه\\b|\\bای گرگ\\b|\\bوایه\\b") to "ایگرگ",
        Regex("\\bزده\\b|\\bزته\\b") to "زد",
        Regex("\\bرادیکاله\\b|\\bرادی کال\\b") to "رادیکال",
        Regex("\\bجزر\\b") to "جذر",
        Regex("\\bمساویه\\b|\\bمساوی ه\\b|\\bمساوی با\\b") to "مساوی",
        Regex("\\bبرابره\\b") to "برابر",
        Regex("\\bمنهایه\\b|\\bمنحای\\b") to "منهای",
        Regex("\\bضربدره\\b|\\bزربدر\\b|\\bضرب دره\\b") to "ضربدر",
        Regex("\\bتقسیم بره\\b|\\bتقسیمه\\b") to "تقسیم بر",
        Regex("\\bکسره\\b") to "کسر",
        Regex("\\bانتگراله\\b|\\bانتگرا\\b|\\bاینتگرال\\b") to "انتگرال",
        Regex("\\bسینوسه\\b|\\bسینوز\\b") to "سینوس",
        Regex("\\bکسینوسه\\b|\\bکوسینوس\\b|\\bکسینوز\\b") to "کسینوس",
        Regex("\\bتانژانته\\b|\\bتانجانت\\b") to "تانژانت",
        Regex("\\bلگاریتمه\\b|\\bلگاریتیم\\b") to "لگاریتم",
        Regex("\\bبینهایته\\b|\\bبی نهایته\\b") to "بی نهایت",
        Regex("\\bدر صد\\b") to "درصد",
        Regex("\\bعلامت سئوال\\b|\\bعلامت سوآل\\b") to "علامت سوال",
        Regex("\\bالفا\\b") to "آلفا", Regex("\\bتیتا\\b|\\bتتاه\\b") to "تتا", Regex("\\bلامدا\\b") to "لاندا",
        Regex("\\bپای\\b(?=\\s+(?:ضرب|به|منهای|تقسیم|مساوی|برابر|روی))") to "پی",
        Regex("\\bصفر\\b\\s+\\bتا\\b") to "صفر تا",
        // اعداد که موتور با «ی» می‌چسباند: «دوی»، «سه‌ی»
        Regex("\\bدوی\\b") to "دو", Regex("\\bسه ی\\b") to "سه",
        // انگلیسی
        Regex("\\bsquare route\\b|\\bsquare root\\s+of\\s+of\\b", RegexOption.IGNORE_CASE) to "square root",
        Regex("\\bx squared\\b|\\bex squared\\b|\\becks squared\\b", RegexOption.IGNORE_CASE) to "x squared",
        Regex("\\bto the power\\s+of\\b", RegexOption.IGNORE_CASE) to "to the power of",
        Regex("\\bwhy\\b(?=\\s+(?:squared|cubed|equals|plus|minus|over|to the power))", RegexOption.IGNORE_CASE) to "y"
    )

    /** V109 — اصلاح خطاهای رایج تشخیص گفتار؛ پیش از هر تبدیل دیگری اعمال می‌شود. */
    fun correct(spoken: String): String = applyAll(normalize(spoken), corrections).replace(Regex("\\s+"), " ").trim()

    /**
     * V109 — انتخاب بهترین گزینه از میان چند حدس موتور (n-best): امتیاز به
     * واژگان ریاضی/عددی/نمادی شناخته‌شده و طول معقول؛ گزینهٔ اول امتیاز پایه دارد.
     */
    fun pickBest(candidates: List<String>): String {
        if (candidates.isEmpty()) return ""
        val vocab = mathTriggers + faUnits.keys + enUnits.keys + faScales.keys + enScales.keys +
            variables.keys + greek.keys + functions.keys + listOf("درصد", "درجه", "علامت سوال", "percent", "degrees", "ممیز", "point")
        var bestScore = Int.MIN_VALUE
        var best = candidates.first()
        candidates.forEachIndexed { index, raw ->
            val c = " " + correct(raw).lowercase() + " "
            var score = if (index == 0) 1 else 0
            vocab.forEach { word -> if (c.contains(" $word ")) score += if (word.length > 3) 3 else 2 }
            score -= c.count { it == '?' }
            if (score > bestScore) { bestScore = score; best = raw }
        }
        return best
    }

    // ============================================================ API
    /** تبدیل کامل گفتار: اعداد و نمادها همیشه؛ عبارت ریاضی فقط اگر نشانهٔ ریاضی داشته باشد. */
    fun convert(spoken: String): Result {
        val normalized = correct(spoken)
        if (normalized.isBlank()) return Result("", false)
        val withNumbers = convertNumbers(normalized)
        val withSymbols = applyAll(withNumbers, plainSymbols)
        return if (looksMathematical(normalized)) {
            val tex = toLatex(withNumbers)
            Result(wrapFormula(withSymbols, tex), tex.isNotBlank())
        } else {
            Result(cleanup(withSymbols), false)
        }
    }

    /** فقط بخش ریاضی: LaTeX خام (برای بازکردن در ویرایشگر فرمول). */
    fun toLatexOnly(spoken: String): String = toLatex(convertNumbers(correct(spoken)))

    fun looksMathematical(spoken: String): Boolean {
        val s = " " + correct(spoken).lowercase() + " "
        return mathTriggers.any { s.contains(" $it ") }
    }

    // ============================================================ پیاده‌سازی
    private fun normalize(input: String): String = input
        .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
        .replace(Regex("[\\u064B-\\u0652]"), "")
        .replace(Regex("\\s+"), " ").trim()

    private fun applyAll(input: String, rules: List<Pair<Regex, String>>): String =
        rules.fold(input) { acc, (rx, rep) -> rx.replace(acc) { rep } }

    private fun cleanup(text: String): String = text
        .replace(Regex(" +([،,:؟?%٪°)])"), "$1")
        .replace(Regex("\\( +"), "(")
        .replace(Regex("[ ]{2,}"), " ")
        .replace(Regex(" *\n *"), "\n")
        .trim()

    private fun wrapFormula(plain: String, tex: String): String {
        if (tex.isBlank()) return cleanup(plain)
        // اگر کل جمله ریاضی است، تنها فرمول درج می‌شود؛ وگرنه متنِ عادیِ اطراف حفظ می‌شود.
        return "\$$tex\$"
    }

    // ------------------------------------------------------------ اعداد گفتاری → رقم
    private fun convertNumbers(text: String): String {
        val tokens = text.split(' ').filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val parsed = parseNumberAt(tokens, i)
            if (parsed != null) {
                out += parsed.first
                i = parsed.second
            } else {
                out += tokens[i]
                i++
            }
        }
        return out.joinToString(" ")
    }

    /** از موقعیت i یک عدد گفتاری (با «و»/«and»، ممیز/point) می‌خواند؛ (متن عدد، اندیس بعدی). */
    private fun parseNumberAt(tokens: List<String>, start: Int): Pair<String, Int>? {
        var i = start
        var total = 0L
        var current = 0L
        var consumedAny = false
        var lastWasConnector = false
        while (i < tokens.size) {
            val w = tokens[i].lowercase()
            val unit = faUnits[w] ?: enUnits[w]
            val scale = faScales[w] ?: enScales[w]
            when {
                unit != null -> {
                    if (unit >= 100) current = if (current == 0L) unit.toLong() else current + unit
                    else current += unit
                    consumedAny = true; lastWasConnector = false; i++
                }
                scale != null && consumedAny -> {
                    if (scale == 100L) current = (if (current == 0L) 1 else current) * 100
                    else { total += (if (current == 0L) 1 else current) * scale; current = 0 }
                    lastWasConnector = false; i++
                }
                scale != null && !consumedAny && (w == "هزار" || w == "hundred" || w == "thousand") -> {
                    total += scale; consumedAny = true; lastWasConnector = false; i++
                }
                (w == "و" || w == "and") && consumedAny && i + 1 < tokens.size &&
                    (faUnits.containsKey(tokens[i + 1].lowercase()) || enUnits.containsKey(tokens[i + 1].lowercase()) ||
                        faScales.containsKey(tokens[i + 1].lowercase()) || enScales.containsKey(tokens[i + 1].lowercase())) -> {
                    lastWasConnector = true; i++
                }
                else -> break
            }
        }
        if (!consumedAny) return null
        if (lastWasConnector) i--
        var value = (total + current).toString()
        // ممیز / point
        if (i + 1 < tokens.size && (tokens[i].lowercase() in setOf("ممیز", "اعشار", "point", "dot"))) {
            // «سه ممیز چهارده» → 3.14 (کسر اعشاری چندرقمی)
            val fracParsed = parseNumberAt(tokens, i + 1)
            if (fracParsed != null && !fracParsed.first.contains('.')) { value = "$value.${fracParsed.first}"; return value to fracParsed.second }
            val frac = StringBuilder()
            var j = i + 1
            while (j < tokens.size) {
                val d = faUnits[tokens[j].lowercase()] ?: enUnits[tokens[j].lowercase()]
                if (d != null && d in 0..9) { frac.append(d); j++ } else break
            }
            if (frac.isNotEmpty()) { value = "$value.$frac"; i = j }
        }
        return value to i
    }

    // ------------------------------------------------------------ متن ریاضی → LaTeX
    private fun toLatex(text: String): String {
        var s = " " + text.lowercase() + " "
        // ۱) واژه‌های چندکلمه‌ای که باید یک‌تکه شوند
        s = s.replace(Regex("\\bبی[ ]?نهایت\\b"), " INFTY ").replace(Regex("\\binfinity\\b"), " INFTY ")
        s = s.replace(Regex("\\bلگاریتم طبیعی\\b|\\bnatural log\\b"), " LNFN ")
        // ۲) توابع و حروف یونانی و متغیرها → نشانه‌های تک‌واژه (بدون فاصله داخلی)
        functions.entries.sortedByDescending { it.key.length }.forEach { (k, v) ->
            s = s.replace(Regex("(?<![\\p{L}\\\\])${Regex.escape(k)}(?!\\p{L})")) { " $v " }
        }
        greek.forEach { (k, v) -> s = s.replace(Regex("(?<![\\p{L}\\\\])${Regex.escape(k)}(?!\\p{L})")) { " $v " } }
        s = s.split(' ').joinToString(" ") { w -> variables[w] ?: w }
        s = s.replace(" INFTY ", " \\infty ").replace(" LNFN ", " \\ln ")
        // ۳) عملگرها (پیش از ساختارها تا «به علاوه» به‌عنوان آرگومان گرفته نشود)
        s = applyAll(s, operators)
        s = s.replace(Regex("\\s+"), " ")
        // ۴) ساختارها
        val atom = "([\\\\\\p{L}\\p{N}.]+|\\([^()]*\\))"
        s = s.replace(Regex("(?:رادیکال|جذر|square root of|square root|root of|root)\\s+$atom")) { "\\sqrt{${it.groupValues[1]}}" }
        s = s.replace(Regex("(?:کسر|fraction)\\s+$atom\\s+(?:روی|بر|over)\\s+$atom")) { "\\frac{${it.groupValues[1]}}{${it.groupValues[2]}}" }
        s = s.replace(Regex("$atom\\s+(?:روی|over)\\s+$atom")) { "\\frac{${it.groupValues[1]}}{${it.groupValues[2]}}" }
        s = s.replace(Regex("$atom\\s+(?:به توان دو|squared|مربع)(?!\\p{L})")) { "${it.groupValues[1]}^{2}" }
        s = s.replace(Regex("$atom\\s+(?:به توان سه|cubed|مکعب)(?!\\p{L})")) { "${it.groupValues[1]}^{3}" }
        s = s.replace(Regex("$atom\\s+(?:به توان|توان|to the power of|to the power|power of|power)\\s+$atom")) { "${it.groupValues[1]}^{${it.groupValues[2]}}" }
        s = s.replace(Regex("$atom\\s+(?:اندیس|زیروند|subscript|sub)\\s+$atom")) { "${it.groupValues[1]}_{${it.groupValues[2]}}" }
        s = s.replace(Regex("$atom\\s+(?:فاکتوریل|factorial)(?!\\p{L})")) { "${it.groupValues[1]}!" }
        s = s.replace(Regex("(?:انتگرال|integral)(?:\\s+(?:از|from)\\s+$atom\\s+(?:تا|to)\\s+$atom)?")) {
            if (it.groupValues[1].isNotEmpty()) "\\int_{${it.groupValues[1]}}^{${it.groupValues[2]}}" else "\\int"
        }
        s = s.replace(Regex("(?:مجموع|sum)(?:\\s+(?:از|from)\\s+$atom\\s+(?:تا|to)\\s+$atom)?")) {
            if (it.groupValues[1].isNotEmpty()) "\\sum_{${it.groupValues[1]}}^{${it.groupValues[2]}}" else "\\sum"
        }
        s = s.replace(Regex("(?:حد|limit)(?:\\s+(?:وقتی|when|as)?\\s*$atom\\s+(?:به سمت|میل می کند به|approaches|to)\\s+$atom)?")) {
            if (it.groupValues[1].isNotEmpty()) "\\lim_{${it.groupValues[1]} \\to ${it.groupValues[2]}}" else "\\lim"
        }
        s = s.replace(Regex("(?:مشتق|derivative of|derivative)\\s+$atom")) { "\\frac{d}{dx}${it.groupValues[1]}" }
        // ۵) پاک‌سازی
        s = s.replace(Regex("(?<!\\p{L})(?:از|of|the)(?!\\p{L})"), " ")
        s = s.replace(Regex("\\s+"), " ").trim()
        s = s.replace(Regex("\\{\\s+"), "{").replace(Regex("\\s+}"), "}")
        s = s.replace(Regex("\\s*([=+<>])\\s*"), " $1 ").replace(Regex("\\s*-\\s*"), " - ").trim()
        return s
    }
}
