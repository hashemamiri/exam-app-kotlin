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
    FigureTemplate("funn", "قیفی", """{"t":"funn","X":{"labs":"بازدید,ثبت‌نام,خرید","vals":"9,5,2"}}"""),
    // V54.2 — ۱۴ نوع مرحلهٔ دوم؛ شناسه‌ها و کلیدهای X دقیقاً مطابق مرجع.
    FigureTemplate("box", "جعبه‌ای", """{"t":"box","X":{"labs":"A,B,C,D","mins":"2,3,1,2","q1s":"3,4,2,3","meds":"4,5,3,4","q3s":"5,6,4,5","maxs":"7,8,5,6"}}"""),
    FigureTemplate("ohlc", "سهام", """{"t":"ohlc","X":{"labs":"فر,ارد,خرد,تیر","opens":"3,5,4,6","highs":"6,7,6,8","lows":"2,4,3,5","closes":"5,4,6,7"}}"""),
    FigureTemplate("fall", "آبشاری", """{"t":"fall","X":{"labs":"شروع,فروش,هزینه,پایان","vals":"4,3,-2,1"}}"""),
    FigureTemplate("ctrl", "کنترلی", """{"t":"ctrl","X":{"labs":"۱,۲,۳,۴,۵,۶","vals":"4,7,3,6,5,4"}}"""),
    FigureTemplate("venn", "ون", """{"t":"venn","X":{"n":"3","s1":"A","s2":"B","s3":"C","ab":"۳","ac":"۲","bc":"۲","abc":"۱"}}"""),
    FigureTemplate("tree", "نقشه درختی", """{"t":"tree","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("sun", "خورشیدی", """{"t":"sun","X":{"labs":"A,B,C","vals":"4,7,3","labs2":"A1,A2,B1,B2,C1","vals2":"2,2,4,3,3"}}"""),
    FigureTemplate("waff", "وافل", """{"t":"waff","X":{"labs":"A,B,C,D","vals":"4,7,3,6"}}"""),
    FigureTemplate("pict", "پیکتوگرام", """{"t":"pict","X":{"labs":"A,B,C,D","vals":"4,7,3,6","unit":"1"}}"""),
    FigureTemplate("heat", "کانتور", """{"t":"heat","X":{"rows":"A,B,C","cols":"۱,۲,۳,۴","vals":"1,2,3,4,5,6,7,8,9,10,11,12"}}"""),
    FigureTemplate("hmap", "حرارتی", """{"t":"hmap","X":{"rows":"A,B,C","cols":"۱,۲,۳,۴","vals":"1,2,3,4,5,6,7,8,9,10,11,12"}}"""),
    FigureTemplate("bull", "گلوله‌ای", """{"t":"bull","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2"}}"""),
    FigureTemplate("pyra", "هرم جمعیت", """{"t":"pyra","X":{"labs":"۰-۱۴,۱۵-۲۹,۳۰-۴۴,۴۵-۵۹,۶۰+","vals":"4,7,6,5,3","vals2":"4,6,7,5,4","s1":"مرد","s2":"زن"}}"""),
    FigureTemplate("mekko", "مکّو", """{"t":"mekko","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2","vals3":"1,2,2,1"}}"""),
    // V54.3 — ۲۲ نوع مرحلهٔ پایانی؛ شناسه‌ها و کلیدهای X دقیقاً مطابق مرجع.
    FigureTemplate("plot", "محور مختصات", """{"t":"plot","X":{"xmin":-5,"xmax":5,"ymin":-4,"ymax":4}}"""),
    FigureTemplate("flow", "فلوچارت", """{"t":"flow","X":{"labs":"شروع,پردازش,تصمیم؟,پایان"}}"""),
    FigureTemplate("gantt", "گانت", """{"t":"gantt","X":{"labs":"طراحی,ساخت,آزمون,تحویل","vals":"0,2,5,7","vals2":"3,4,3,1"}}"""),
    FigureTemplate("time", "تایملاین", """{"t":"time","X":{"labs":"رویداد ۱,رویداد ۲,رویداد ۳,رویداد ۴","vals":"۱۴۰۰,۱۴۰۱,۱۴۰۲,۱۴۰۳"}}"""),
    FigureTemplate("dumb", "دمبل", """{"t":"dumb","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2"}}"""),
    FigureTemplate("slope", "شیب", """{"t":"slope","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2"}}"""),
    FigureTemplate("spark", "اسپارک‌لاین", """{"t":"spark","X":{"labs":"","vals":"4,7,3,6,5,8,4"}}"""),
    FigureTemplate("stream", "جریانی", """{"t":"stream","X":{"labs":"A,B,C,D","vals":"4,7,3,6","vals2":"5,4,6,2","vals3":"1,2,2,1"}}"""),
    FigureTemplate("viol", "ویولن", """{"t":"viol","X":{"labs":"A,B,C,D","mins":"2,3,1,2","q1s":"3,4,2,3","meds":"4,5,3,4","q3s":"5,6,4,5","maxs":"7,8,5,6"}}"""),
    FigureTemplate("strip", "نوار نقطه‌ای", """{"t":"strip","X":{"labs":"A,B,C,D","mins":"2,3,1,2","q1s":"3,4,2,3","meds":"4,5,3,4","q3s":"5,6,4,5","maxs":"7,8,5,6"}}"""),
    FigureTemplate("stem", "ساقه و برگ", """{"t":"stem","X":{"vals":"12,15,21,23,24,31,35,42"}}"""),
    FigureTemplate("smat", "ماتریس پراکندگی", """{"t":"smat","X":{"xs":"1,2,3,4,5","ys":"2,3,1,5,4","zs":"8,14,6,18,10"}}"""),
    FigureTemplate("dend", "دندروگرام", """{"t":"dend","X":{"labs":"A,B,C,D,E"}}"""),
    FigureTemplate("sank", "سنکی", """{"t":"sank","X":{"vals":"A-C:8,B-C:5,B-D:4"}}"""),
    FigureTemplate("chrd", "کورد", """{"t":"chrd","X":{"labs":"A,B,C,D","vals":"0,2,1,1,2,0,2,1,1,2,0,1,1,1,1,0"}}"""),
    FigureTemplate("netw", "شبکه‌ای", """{"t":"netw","X":{"labs":"A,B,C,D,E","vals":"A-B,B-C,C-D,B-E"}}"""),
    FigureTemplate("map", "نقشه‌ای", """{"t":"map","X":{"labs":"شمال,مرکز,جنوب,شرق","vals":"4,7,3,6"}}"""),
    FigureTemplate("bmap", "نقشه حبابی", """{"t":"bmap","X":{"labs":"شمال,مرکز,جنوب,شرق","vals":"4,7,3,6"}}"""),
    FigureTemplate("surf", "سطحی", """{"t":"surf","X":{"nrows":"4","ncols":"4","vals":"1,2,3,2,2,4,5,3,3,5,6,4,2,3,4,3"}}"""),
    FigureTemplate("calh", "تقویم حرارتی", """{"t":"calh","X":{"vals":"1,3,0,2,4,1,0,2,5,3,1,0,4,2,1,3,0,2,1,4,5,2,3,1,0,2,3,4"}}"""),
    FigureTemplate("rose", "گل رز / قطبی", """{"t":"rose","X":{"labs":"شمال,شمال‌شرق,شرق,جنوب‌شرق,جنوب,جنوب‌غرب,غرب,شمال‌غرب","vals":"4,7,3,6,5,2,4,3"}}"""),
    FigureTemplate("word", "ابر واژه", """{"t":"word","X":{"labs":"ریاضی,فیزیک,شیمی,زیست,ادبیات,تاریخ","vals":"8,6,5,4,3,2"}}""")
)
