package ir.exam.app.core.figure

/**
 * V320 — دسته‌بندی ابزارهای ریاضی/علوم در پنجرهٔ انتخاب.
 *
 * پیش از این، لمس آیکن «ابزار ریاضی» یک فهرست بلند ۹۷تایی را باز می‌کرد. از این نسخه،
 * **اول دسته** انتخاب می‌شود (دبستان / متوسطهٔ اول / متوسطهٔ دوم و کنکور / دانشگاه) و
 * سپس ابزارهای همان دسته نمایش داده می‌شوند. فهرست‌ها باید با `AXIS_FIGURES` هم‌خوان بمانند
 * (تست `V320_MathToolCategoriesTest` این را تضمین می‌کند).
 */
data class MathCategory(
    val id: String,
    val title: String,
    val hint: String,
    val emoji: String,
    val ids: List<String>
) {
    val count: Int get() = ids.size
}

/** ترتیب نمایش دسته‌ها در پنجرهٔ اول. */
val MATH_CATEGORIES: List<MathCategory> = listOf(
    MathCategory(
        id = "elementary",
        title = "دبستان",
        hint = "پایهٔ اول تا ششم — شمارش، جمع و تفریق، ضرب، شکل و اندازه",
        emoji = "🧮",
        ids = listOf(
            "axtally", "axbase10", "axplace", "axclock", "axruler", "axfrac", "axmoney",
            "axabacus", "axjump", "axbalance", "axthermo", "axsaw", "axpattern",
            "axtenframe", "axsym", "axspin", "axbeaker", "axgeoboard", "axdice", "axmularea", "axvenn",
            "axnumgrid", "axfacttree", "axfnmachine", "axfracwall", "axtangram", "axangle",
            "axrod", "axtower", "axmulwheel", "axmultgrid", "axnet", "axcubes", "axcalendar",
            "axlever", "axcycle", "axwordtrain"
        )
    ),
    MathCategory(
        id = "middle",
        title = "متوسطهٔ اول",
        hint = "پایهٔ هفتم تا نهم — جبر، هندسه، اندازه‌گیری و آمار مقدماتی",
        emoji = "📐",
        ids = listOf(
            "axtiles", "axkenken", "axdicefold", "axtransform", "axcaliper",
            "axunitconv", "axperimeter", "axpictograph", "axrounding", "axdivision",
            "axshapeid", "axcircleparts", "axcylinder", "axproportion", "axlinear"
        )
    ),
    MathCategory(
        id = "high",
        title = "متوسطهٔ دوم و کنکور",
        hint = "ریاضی، فیزیک، شیمی و زیست — از تابع و مثلثات تا مدار و سلول",
        emoji = "🧪",
        ids = listOf(
            "axsigntable", "axtrigcircle", "axmapping", "axprobtree", "axconic", "axorbital",
            "axpunnett", "axecg", "axprosody", "axtruthtable", "axledger", "axstaff",
            "axbarchart", "axparabola", "axinthales", "axpendulum", "axincline", "axcircuit",
            "axoptics", "axheatcurve", "axdna", "axcell"
        )
    ),
    MathCategory(
        id = "university",
        title = "انسانی و دانشگاه",
        hint = "انسانی، هنر، فنی و دانشگاه — از عروض و حسابداری تا منطق دیجیتال و الگوریتم",
        emoji = "🎓",
        ids = listOf(
            "axkmap", "axgate", "axdfa", "axbst", "axbode", "axbeam", "axmohr", "axpv",
            "axpert", "axbenzene", "axnormal", "axsupply", "axgame",
            "axtimeline", "axmapgeo", "axsentence", "axvectors", "axmatrix", "axintegral",
            "axflowchart", "axstack", "axsort", "axregression"
        )
    )
)

/** همهٔ شناسه‌های ابزار ریاضی، به ترتیب دسته‌ها. */
val MATH_TOOL_IDS: List<String> = MATH_CATEGORIES.flatMap { it.ids }

/** دستهٔ یک شناسه (یا null اگر شناسه ابزار ریاضی نباشد). */
fun mathCategoryOf(id: String): MathCategory? = MATH_CATEGORIES.firstOrNull { id in it.ids }

/** فهرست قالب‌های همان دسته، با حفظ ترتیب گالری. */
fun mathTemplatesOf(category: MathCategory, all: List<FigureTemplate> = AXIS_FIGURES): List<FigureTemplate> =
    all.filter { it.id in category.ids }
