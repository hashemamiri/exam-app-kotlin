package ir.exam.app.core.figure

/** یک قالب آمادهٔ شکل/نمودار با همان شناسهٔ وب‌اپ. */
data class FigureTemplate(val id: String, val label: String, val specJson: String) {
    fun toSpec(): FigureSpec = FigureSpec.parse(specJson) ?: FigureSpec.build(id)
}

/** شکل‌های هندسی پرکاربرد (شناسه‌ها دقیقاً مطابق وب‌اپ). */
val GEOMETRY_FIGURES: List<FigureTemplate> = listOf(
    FigureTemplate("tri", "مثلث", """{"t":"tri"}"""),
    FigureTemplate("rtri", "قائم‌الزاویه", """{"t":"rtri"}"""),
    FigureTemplate("iso", "متساوی‌الساقین", """{"t":"iso"}"""),
    FigureTemplate("eq", "متساوی‌الاضلاع", """{"t":"eq"}"""),
    FigureTemplate("scal", "مختلف‌الاضلاع", """{"t":"scal"}"""),
    FigureTemplate("acut", "مثلث حاده", """{"t":"acut"}"""),
    FigureTemplate("obt", "مثلث منفرجه", """{"t":"obt"}"""),
    FigureTemplate("sq", "مربع", """{"t":"sq"}"""),
    FigureTemplate("rect", "مستطیل", """{"t":"rect"}"""),
    FigureTemplate("para", "متوازی‌الاضلاع", """{"t":"para"}"""),
    FigureTemplate("rhomb", "لوزی", """{"t":"rhomb"}"""),
    FigureTemplate("trap", "ذوزنقه", """{"t":"trap"}"""),
    FigureTemplate("itrap", "ذوزنقه متساوی‌الساقین", """{"t":"itrap"}"""),
    FigureTemplate("rtrap", "ذوزنقه قائم", """{"t":"rtrap"}"""),
    FigureTemplate("kite", "بادبادک", """{"t":"kite"}"""),
    FigureTemplate("circ", "دایره", """{"t":"circ"}"""),
    FigureTemplate("semi", "نیم‌دایره", """{"t":"semi"}"""),
    FigureTemplate("ring", "حلقه", """{"t":"ring"}"""),
    FigureTemplate("ell", "بیضی", """{"t":"ell"}"""),
    FigureTemplate("ang", "زاویه", """{"t":"ang"}"""),
    FigureTemplate("parll", "خطوط موازی", """{"t":"parll"}"""),
    FigureTemplate("pseg", "پاره‌خط", """{"t":"pseg"}"""),
    FigureTemplate("ray", "نیم‌خط", """{"t":"ray"}"""),
    FigureTemplate("ln", "خط", """{"t":"ln"}"""),
    FigureTemplate("pent", "پنج‌ضلعی", """{"t":"pent"}"""),
    FigureTemplate("hex", "شش‌ضلعی", """{"t":"hex"}"""),
    FigureTemplate("star", "ستاره پنج‌پر", """{"t":"star"}"""),
    FigureTemplate("cube", "مکعب", """{"t":"cube"}"""),
    FigureTemplate("box", "مکعب‌مستطیل", """{"t":"box"}"""),
    FigureTemplate("cyl", "استوانه", """{"t":"cyl"}"""),
    FigureTemplate("cone", "مخروط", """{"t":"cone"}"""),
    FigureTemplate("sph", "کره", """{"t":"sph"}"""),
    FigureTemplate("pyr", "هرم", """{"t":"pyr"}"""),
    FigureTemplate("pris", "منشور", """{"t":"pris"}""")
)

/** نمودارهای پرکاربرد (شناسه‌ها دقیقاً مطابق وب‌اپ). */
val GRAPH_FIGURES: List<FigureTemplate> = listOf(
    FigureTemplate(
        "line", "خط",
        """{"t":"line","X":{"xmin":-5,"xmax":5,"ymin":-4,"ymax":4,"m":1,"b":0}}"""
    ),
    FigureTemplate(
        "quad", "سهمی",
        """{"t":"quad","X":{"xmin":-5,"xmax":5,"ymin":-4,"ymax":4,"a":1,"b":0,"c":0}}"""
    ),
    FigureTemplate(
        "sine", "سینوسی",
        """{"t":"sine","X":{"xmin":-5,"xmax":5,"ymin":-4,"ymax":4,"A":1,"w":1,"ph":0}}"""
    ),
    FigureTemplate(
        "exp", "نمایی",
        """{"t":"exp","X":{"xmin":-5,"xmax":5,"ymin":-4,"ymax":4,"a":1,"b":0.5}}"""
    ),
    FigureTemplate(
        "bar", "نمودار ستونی",
        """{"t":"bar","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""
    ),
    // V54.1 — ۲۰ نوع جدید؛ شناسه‌ها و کلیدهای X دقیقاً مطابق ماژول graph-fig-js مرجع.
    FigureTemplate("pie", "دایره‌ای", """{"t":"pie","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("donut", "دوناتی", """{"t":"donut","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("lchr", "خطی", """{"t":"lchr","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("area", "ناحیه‌ای", """{"t":"area","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("step", "پله‌ای", """{"t":"step","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("sarea", "مساحت انباشته", """{"t":"sarea","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2","vals3":"1,2,2,1"}}"""),
    FigureTemplate("hbar", "میله‌ای", """{"t":"hbar","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("cmp", "ستونی خوشه‌ای", """{"t":"cmp","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2"}}"""),
    FigureTemplate("hcmp", "میله‌ای خوشه‌ای", """{"t":"hcmp","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2"}}"""),
    FigureTemplate("stack", "پشته‌ای", """{"t":"stack","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2","vals3":"1,2,2,1"}}"""),
    FigureTemplate("st100", "انباشته ۱۰۰٪", """{"t":"st100","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2","vals3":"1,2,2,1"}}"""),
    FigureTemplate("scat", "پراکندگی", """{"t":"scat","X":{"xs":"1,2,3,4,5","ys":"2,3,1,5,4"}}"""),
    FigureTemplate("bub", "حبابی", """{"t":"bub","X":{"xs":"1,2,3,4,5","ys":"2,3,1,5,4","zs":"8,14,6,18,10"}}"""),
    FigureTemplate("hist", "هیستوگرام", """{"t":"hist","X":{"labs":"۰-۵,۵-۱۰,۱۰-۱۵,۱۵-۲۰","vals":"4,7,3,6"}}"""),
    FigureTemplate("pareto", "پارتو", """{"t":"pareto","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("gauge", "عقربه‌ای", """{"t":"gauge","X":{"val":"65","vmin":"0","vmax":"100"}}"""),
    FigureTemplate("radar", "راداری", """{"t":"radar","X":{"labs":"A,B,C,D,E","vals":"4,7,3,6,5"}}"""),
    FigureTemplate("combo", "ترکیبی", """{"t":"combo","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2"}}"""),
    FigureTemplate("lolli", "لولی‌پاپ", """{"t":"lolli","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("funn", "قیفی", """{"t":"funn","X":{"labs":"بازدید,ثبت‌نام,خرید","vals":"9,5,2"}}""")
)
