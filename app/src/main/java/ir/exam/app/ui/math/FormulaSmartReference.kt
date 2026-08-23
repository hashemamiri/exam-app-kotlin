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
            listOf("common", "basic", "calc", "all-basic", "all-algebra", "cur-num", "cur-power", "cur-algebra", "cur-equations", "cur-functions", "cur-sequence", "cur-limit", "cur-derivative", "cur-integral", "matrix"),
            listOf(
                FormulaSmartTemplate("کسر", "a⁄b", "\\frac{a}{b}", true),
                FormulaSmartTemplate("توان", "x²", "x^{n}", true),
                FormulaSmartTemplate("رادیکال", "√x", "\\sqrt{x}", true),
                FormulaSmartTemplate("معادله", "ax²+bx+c=0", "a x^2 + b x + c = 0"),
                FormulaSmartTemplate("حد", "lim x→a", "\\lim_{x \\to a}", true),
                FormulaSmartTemplate("انتگرال", "∫", "\\int_{a}^{b}", true)
            )
        ),
        FormulaSmartLesson(
            "geometry", "هندسه",
            listOf("all-geometry", "cur-geometry", "cur-solid", "cur-analytic", "cur-coordinate", "cur-vector", "cur-analytic-full"),
            listOf(
                FormulaSmartTemplate("فیثاغورس", "a²+b²=c²", "a^2 + b^2 = c^2"),
                FormulaSmartTemplate("دایره", "x²+y²=r²", "x^2 + y^2 = r^2"),
                FormulaSmartTemplate("مساحت مثلث", "S=bh⁄2", "S = \\frac{b h}{2}"),
                FormulaSmartTemplate("زاویه", "∠ABC", "\\angle A B C"),
                FormulaSmartTemplate("قدر مطلق", "|x|", "\\left| x \\right|"),
                FormulaSmartTemplate("جزء صحیح", "⌊x⌋", "\\lfloor x \\rfloor")
            )
        ),
        FormulaSmartLesson(
            "stats", "آمار",
            listOf("cur-prob", "cur-stats", "cur-statplus"),
            listOf(
                FormulaSmartTemplate("میانگین", "x̄", "\\overline{x}"),
                FormulaSmartTemplate("احتمال", "P(A)", "P(A)"),
                FormulaSmartTemplate("واریانس", "σ²", "\\sigma^2"),
                FormulaSmartTemplate("انحراف معیار", "σ", "\\sigma"),
                FormulaSmartTemplate("درصد", "a/b×100", "\\frac{a}{b} \\times 100"),
                FormulaSmartTemplate("تناسب", "a⁄b=c⁄d", "\\frac{a}{b} = \\frac{c}{d}")
            )
        ),
        FormulaSmartLesson(
            "physics", "فیزیک",
            listOf("cur-phys-mech", "cur-phys-energy", "cur-phys-elec", "cur-phys-wave", "cur-konkur10-phys", "cur-konkur11-phys", "cur-konkur12-phys", "cur-phys-oscillation", "cur-phys-optics", "cur-phys-waveoptics", "cur-phys-atomic"),
            listOf(
                FormulaSmartTemplate("سرعت", "v=Δx⁄Δt", "v = \\frac{\\Delta x}{\\Delta t}"),
                FormulaSmartTemplate("نیوتن دوم", "F=ma", "F = m a"),
                FormulaSmartTemplate("کار", "W=Fd cosθ", "W = F d \\cos\\theta"),
                FormulaSmartTemplate("توان", "P=W/t", "P = \\frac{W}{t}"),
                FormulaSmartTemplate("موج", "v=fλ", "v = f \\lambda"),
                FormulaSmartTemplate("گاز کامل", "PV=nRT", "P V = n R T")
            )
        ),
        FormulaSmartLesson(
            "chemistry", "شیمی",
            listOf("cur-chem-basic", "cur-chem-gas", "cur-chem-acid", "cur-chem-thermo", "cur-chem-organic", "cur-periodic", "cur-konkur10-chem", "cur-konkur11-chem", "cur-konkur12-chem", "cur-physchem"),
            listOf(
                FormulaSmartTemplate("آب", "H₂O", "H_{2}O"),
                FormulaSmartTemplate("CO₂", "CO₂", "CO_{2}"),
                FormulaSmartTemplate("H₂SO₄", "H₂SO₄", "H_{2}SO_{4}"),
                FormulaSmartTemplate("NaOH", "NaOH", "NaOH"),
                FormulaSmartTemplate("تعادل", "⇌", "\\rightleftharpoons"),
                FormulaSmartTemplate("pH", "pH=-log[H⁺]", "pH = -\\log[H^+]")
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
