package ir.exam.app.ui.math

data class FormulaSmartTemplate(
    val label: String,
    val preview: String,
    val tex: String,
    val insertAtActiveBox: Boolean = false
)
data class FormulaSmartLesson(
    val key: String,
    val label: String,
    val categoryIds: List<String>,
    val templates: List<FormulaSmartTemplate>
)
data class FormulaSmartPack(
    val label: String,
    val categoryIds: List<String> = emptyList(),
    val items: List<FormulaReferenceEntry> = emptyList()
)
data class FormulaDelimiterPreset(val label: String, val open: String, val close: String, val kind: String = "normal")

/** همهٔ داده‌های Smart Hub مرجع؛ در Native کاملاً قابل دسترسی است، نه کد پنهان. */
object FormulaSmartReference {
    val defaultFavorites = listOf(
        FormulaReferenceEntry("کسر", "\\frac{a}{b}"),
        FormulaReferenceEntry("توان", "x^{n}"),
        FormulaReferenceEntry("رادیکال", "\\sqrt{x}"),
        FormulaReferenceEntry("حد", "\\lim_{x \\to a}"),
        FormulaReferenceEntry("انتگرال", "\\int_{a}^{b}"),
        FormulaReferenceEntry("sin", "\\sin")
    )

    val lessons = listOf(
        FormulaSmartLesson(
            "math", "ریاضی",
            listOf("common", "basic", "calc", "all-basic", "all-algebra", "cur-num", "cur-power", "cur-algebra", "cur-equations", "cur-functions", "cur-sequence", "cur-limit", "cur-derivative", "cur-integral", "matrix", "v34-sets-num", "v34-interval", "v34-seq-extra", "v34-trig-id", "v34-trig-laws", "v34-trig-eq", "v34-power-laws", "v34-identities", "v34-equations", "v34-ineq", "v34-functions", "v34-fn-special", "v34-explog", "v34-limit", "v34-deriv", "v34-integ", "v34-count", "v34-prob", "v34-numberth", "v34-matrix-extra", "v34-math10", "v34-math11e", "v34-math12e", "v34-hesaban1", "v34-hesaban2", "v34-discrete", "v34-human"),
            listOf(
                FormulaSmartTemplate("کسر", "a⁄b", "\\frac{a}{b}", true),
                FormulaSmartTemplate("توان", "x²", "x^{n}", true),
                FormulaSmartTemplate("رادیکال", "√x", "\\sqrt{x}", true),
                FormulaSmartTemplate("معادله", "ax²+bx+c=0", "a x^2 + b x + c = 0"),
                FormulaSmartTemplate("حد", "lim x→a", "\\lim_{x \\to a}", true),
                FormulaSmartTemplate("انتگرال", "∫", "\\int_{a}^{b}", true),
                FormulaSmartTemplate("دلتا", "Δ=b²-4ac", "\\Delta = b^2 - 4 a c"),
                FormulaSmartTemplate("ریشه‌ها", "x=(-b±√Δ)/2a", "x = \\frac{-b \\pm \\sqrt{\\Delta}}{2 a}")
            )
        ),
        FormulaSmartLesson(
            "geometry", "هندسه",
            listOf("all-geometry", "cur-geometry", "cur-solid", "cur-analytic", "cur-coordinate", "cur-vector", "cur-analytic-full", "v34-geo-base", "v34-thales", "v34-circle", "v34-transform", "v34-analytic", "v34-conic", "v34-solid", "v34-geo1", "v34-geo2", "v34-geo3"),
            listOf(
                FormulaSmartTemplate("فیثاغورس", "a²+b²=c²", "a^2 + b^2 = c^2"),
                FormulaSmartTemplate("دایره", "x²+y²=r²", "x^2 + y^2 = r^2"),
                FormulaSmartTemplate("مساحت مثلث", "S=bh⁄2", "S = \\frac{b h}{2}"),
                FormulaSmartTemplate("تالس", "AD/AB=AE/AC", "\\frac{A D}{A B} = \\frac{A E}{A C}"),
                FormulaSmartTemplate("زاویه", "∠ABC", "\\angle A B C"),
                FormulaSmartTemplate("قدر مطلق", "|x|", "\\left| x \\right|"),
                FormulaSmartTemplate("جزء صحیح", "⌊x⌋", "\\lfloor x \\rfloor")
            )
        ),
        FormulaSmartLesson(
            "stats", "آمار",
            listOf("cur-prob", "cur-stats", "cur-statplus", "v34-count", "v34-prob", "v34-stats", "v34-stats11"),
            listOf(
                FormulaSmartTemplate("میانگین", "x̄", "\\overline{x}"),
                FormulaSmartTemplate("احتمال", "P(A)", "P(A)"),
                FormulaSmartTemplate("واریانس", "σ²", "\\sigma^2"),
                FormulaSmartTemplate("انحراف معیار", "σ", "\\sigma"),
                FormulaSmartTemplate("درصد", "a/b×100", "\\frac{a}{b} \\times 100"),
                FormulaSmartTemplate("تناسب", "a⁄b=c⁄d", "\\frac{a}{b} = \\frac{c}{d}"),
                FormulaSmartTemplate("ترکیب", "C(n,r)", "\\binom{n}{r}", true)
            )
        ),
        FormulaSmartLesson(
            "physics", "فیزیک",
            listOf("cur-phys-mech", "cur-phys-energy", "cur-phys-elec", "cur-phys-wave", "cur-konkur10-phys", "cur-konkur11-phys", "cur-konkur12-phys", "cur-phys-oscillation", "cur-phys-optics", "cur-phys-waveoptics", "cur-phys-atomic", "v34-phys-measure", "v34-phys-matter", "v34-phys-thermo", "v34-phys-kine", "v34-phys-dyn", "v34-phys-ac", "v34-phys-lens", "v34-phys-doppler", "v34-phys-atomic"),
            listOf(
                FormulaSmartTemplate("سرعت", "v=Δx⁄Δt", "v = \\frac{\\Delta x}{\\Delta t}"),
                FormulaSmartTemplate("نیوتن دوم", "F=ma", "F = m a"),
                FormulaSmartTemplate("کار", "W=Fd cosθ", "W = F d \\cos\\theta"),
                FormulaSmartTemplate("توان", "P=W/t", "P = \\frac{W}{t}"),
                FormulaSmartTemplate("موج", "v=fλ", "v = f \\lambda"),
                FormulaSmartTemplate("گاز کامل", "PV=nRT", "P V = n R T"),
                FormulaSmartTemplate("مستقل از زمان", "v²-v₀²=2aΔx", "v^2 - v_0^2 = 2 a \\Delta x")
            )
        ),
        FormulaSmartLesson(
            "chemistry", "شیمی",
            listOf("cur-chem-basic", "cur-chem-gas", "cur-chem-acid", "cur-chem-thermo", "cur-chem-organic", "cur-periodic", "cur-konkur10-chem", "cur-konkur11-chem", "cur-konkur12-chem", "cur-physchem", "v34-chem-react", "v34-chem10x", "v34-chem11x", "v34-chem12x", "v34-bio"),
            listOf(
                FormulaSmartTemplate("آب", "H₂O", "H_{2}O"),
                FormulaSmartTemplate("CO₂", "CO₂", "CO_{2}"),
                FormulaSmartTemplate("H₂SO₄", "H₂SO₄", "H_{2}SO_{4}"),
                FormulaSmartTemplate("NaOH", "NaOH", "NaOH"),
                FormulaSmartTemplate("تعادل", "⇌", "\\rightleftharpoons"),
                FormulaSmartTemplate("pH", "pH=-log[H⁺]", "pH = -\\log[H^+]"),
                FormulaSmartTemplate("مول", "n=m/M", "n = \\frac{m}{M}")
            )
        ),
        FormulaSmartLesson(
            "school", "کتب درسی",
            listOf("v34-math10", "v34-math11e", "v34-math12e", "v34-hesaban1", "v34-hesaban2", "v34-geo1", "v34-geo2", "v34-geo3", "v34-discrete", "v34-stats11", "v34-human", "v34-chem10x", "v34-chem11x", "v34-chem12x", "v34-bio", "v34-uni"),
            listOf(
                FormulaSmartTemplate("دنباله حسابی", "a_n=a₁+(n-1)d", "a_n = a_1 + (n - 1) d"),
                FormulaSmartTemplate("دنباله هندسی", "a_n=a₁rⁿ⁻¹", "a_n = a_1 r^{n-1}"),
                FormulaSmartTemplate("اتحاد مربع", "(a+b)²=a²+2ab+b²", "(a+b)^2 = a^2 + 2 a b + b^2"),
                FormulaSmartTemplate("مشتق زنجیره‌ای", "(f(g(x)))'=f'(g(x))g'(x)", "(f(g(x)))' = f'(g(x)) g'(x)"),
                FormulaSmartTemplate("قانون هس", "ΔH=ΣΔH_i", "\\Delta H = \\sum \\Delta H_i"),
                FormulaSmartTemplate("هاردی–واینبرگ", "p²+2pq+q²=1", "p^2 + 2 p q + q^2 = 1")
            )
        )
    )

    val packs = listOf(
        FormulaSmartPack("معادله", listOf("cur-equations", "all-algebra")),
        FormulaSmartPack(
            "نسبت و تناسب", items = listOf(
                FormulaReferenceEntry("تناسب", "\\frac{a}{b} = \\frac{c}{d}"),
                FormulaReferenceEntry("درصد", "\\frac{a}{b} \\times 100"),
                FormulaReferenceEntry("نسبت", "a:b")
            )
        ),
        FormulaSmartPack(
            "اتحادها", items = listOf(
                FormulaReferenceEntry("مربع دو جمله‌ای", "(a+b)^2 = a^2 + 2ab + b^2"),
                FormulaReferenceEntry("مربع تفاضل", "(a-b)^2 = a^2 - 2ab + b^2"),
                FormulaReferenceEntry("مزدوج", "a^2-b^2=(a-b)(a+b)")
            )
        ),
        FormulaSmartPack("تابع", listOf("cur-functions")),
        FormulaSmartPack("مشتق", listOf("cur-derivative")),
        FormulaSmartPack("آمار", listOf("cur-stats", "cur-prob")),
        FormulaSmartPack("شیمی", listOf("cur-chem-basic", "cur-chem-acid", "cur-periodic")),
        FormulaSmartPack("فیزیک", listOf("cur-phys-mech", "cur-phys-elec", "cur-phys-wave"))
    )

    val delimiters = listOf(
        FormulaDelimiterPreset("( )", "(", ")"),
        FormulaDelimiterPreset("[ ]", "[", "]"),
        FormulaDelimiterPreset("{ }", "{", "}"),
        FormulaDelimiterPreset("| |", "|", "|"),
        FormulaDelimiterPreset("⌊ ⌋", "⌊", "⌋", "floor"),
        FormulaDelimiterPreset("⌈ ⌉", "⌈", "⌉", "ceil")
    )

    val bigKeyLabels = listOf("کسر", "توان", "رادیکال", "( )", "sin", "⌫", "↵", "آخرین")

    fun lesson(key: String): FormulaSmartLesson = lessons.firstOrNull { it.key == key } ?: lessons.first()

    fun entriesForCategories(data: FormulaReferenceData, ids: List<String>): List<FormulaReferenceEntry> =
        ids.flatMap { data.categoryById[it]?.items.orEmpty() }
            .distinctBy { it.label + "¦" + it.tex }

    fun entriesForPack(data: FormulaReferenceData, pack: FormulaSmartPack): List<FormulaReferenceEntry> =
        (pack.items + entriesForCategories(data, pack.categoryIds))
            .distinctBy { it.label + "¦" + it.tex }
}
