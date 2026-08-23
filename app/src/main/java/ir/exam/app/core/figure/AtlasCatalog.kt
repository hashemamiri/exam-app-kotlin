package ir.exam.app.core.figure

/**
 * V53.3 — کاتالوگ کامل انواع آناتومی (k='a') و فیزیک/شیمی (k='s')؛
 * استخراج برنامه‌ای از ماژول‌های anatomy-fig-js و science-fig-js مرجع.
 * تصاویر اطلس در assets/figure_atlas/{anatomy|science} قرار دارند.
 */
object AtlasCatalog {

    data class AtlasType(val id: String, val cat: String, val name: String, val caption: String = "")
    data class AtlasCategory(val id: String, val name: String)

    /** دسته‌های آناتومی — همان CATS مرجع. */
    val ANATOMY_CATS: List<AtlasCategory> = listOf(
        AtlasCategory("all", "همه"),
        AtlasCategory("body", "بدن"),
        AtlasCategory("bone", "استخوان"),
        AtlasCategory("mus", "ماهیچه"),
        AtlasCategory("circ", "رگ و خون"),
        AtlasCategory("resp", "تنفس"),
        AtlasCategory("dig", "گوارش"),
        AtlasCategory("nerv", "عصب"),
        AtlasCategory("urin", "ادراری"),
        AtlasCategory("endo", "غدد"),
        AtlasCategory("lymph", "لنفاوی"),
        AtlasCategory("sense", "حواس"),
        AtlasCategory("cell", "سلول"),
        AtlasCategory("repro", "تولیدمثل"),
        AtlasCategory("d3", "سه‌بعدی"),
    )

    /** ۶۷ نوع آناتومی مرجع با کپشن آموزشی. */
    val ANATOMY_TYPES: List<AtlasType> = listOf(
        AtlasType("cell3", "cell", "سلول", "۳بعدی · ساختار یک سلول انسانی — واحد سازندهٔ بدن (هسته، میتوکندری، ...)"),
        AtlasType("dna", "cell", "دی‌ان‌ای", "۳بعدی · مارپیچ دوگانهٔ DNA — نقشهٔ ساختمان بدن"),
        AtlasType("bodyF", "body", "بدن روبه‌رو", "۲بعدی · نمای روبه‌رو بدن — آناتومی سطحی"),
        AtlasType("bodyB", "body", "بدن پشت", "۲بعدی · نمای پشت — کتف، ستون فقرات و پشت پاها"),
        AtlasType("bodyS", "body", "بدن نیمرخ", "۲بعدی · نیمرخ — قوس طبیعی ستون فقرات"),
        AtlasType("head", "body", "سر و گردن", "۲بعدی · سر و گردن — مغز، حفره‌ها، حلق و حنجره"),
        AtlasType("torso", "bone", "اسکلت", "۲بعدی · رسم آموزشی اسکلت — نمای روبه‌رو"),
        AtlasType("body3", "d3", "اسکلت سه‌بعدی", "۳بعدی · رندر پزشکی اسکلت"),
        AtlasType("skull", "bone", "جمجمه", "۲بعدی · جمجمه — کاسهٔ سر، کاسهٔ چشم و فک"),
        AtlasType("spine", "bone", "ستون فقرات", "۲بعدی · ستون فقرات — مهره‌ها، دیسک‌ها، خاجی و دنباله"),
        AtlasType("joint", "bone", "مفصل زانو", "۲بعدی · مفصل زانو — غضروف، منیسک، رباط و تاندون"),
        AtlasType("handB", "bone", "استخوان دست", "۲بعدی · ۲۷ استخوان مچ و دست — کارپ‌ها، کف و بندانگشتی"),
        AtlasType("ribs", "bone", "قفسه سینه", "۲بعدی · قفسه سینه — ۱۲ جفت دنده + جناغ"),
        AtlasType("pelvis", "bone", "لگن", "۲بعدی · لگن — سرین، نشیمن، پوپیس + خاجی"),
        AtlasType("legB", "bone", "استخوان پا", "۲بعدی · استخوان پا — ران، کشکک، درشت‌نی، نازک‌نی"),
        AtlasType("footB", "bone", "کف پا", "۲بعدی · کف پا — ۲۶ استخوان تارسال، کف و انگشتان"),
        AtlasType("skel", "mus", "ماهیچه‌ها", "۲بعدی · رسم آموزشی ماهیچه‌ها — نمای روبه‌رو"),
        AtlasType("musB", "mus", "ماهیچه پشت ۳د", "۳بعدی · رندر ماهیچه — نمای پشت"),
        AtlasType("mus3", "d3", "ماهیچه روبه‌رو ۳د", "۳بعدی · رندر عضلات — نمای روبه‌رو"),
        AtlasType("sarc", "mus", "تار عضلانی", "ریزمقیاس · تارهای عضلانی و واحد انقباض (سارکومر)"),
        AtlasType("biceps", "mus", "دوسر بازو", "۲بعدی · دوسر بازو — خم‌کنندهٔ آرنج"),
        AtlasType("quad", "mus", "چهارسر ران", "۲بعدی · چهارسر ران — بازکنندهٔ زانو"),
        AtlasType("abs", "mus", "ماهیچه شکم", "۲بعدی · عضلات شکم — مستقیم (شش‌تکه) و مورب‌ها"),
        AtlasType("heartM", "mus", "بافت عضله قلب", "بافت عضلهٔ قلب: سلول‌های شاخه‌دار و مخطط — انقباض خودکار و بی‌وقفه"),
        AtlasType("circ", "circ", "شبکه رگ‌ها", "۲بعدی · کل شبکه سرخرگ‌ها (قرمز) و سیاهرگ‌ها (آبی)"),
        AtlasType("heart", "circ", "مقطع قلب", "۲بعدی · مقطع قلب — چهار حفره و دریچه‌ها"),
        AtlasType("heart3", "d3", "قلب سه‌بعدی", "۳بعدی · رندر قلب با سرخرگ‌های اصلی"),
        AtlasType("cap", "circ", "مویرگ", "۳بعدی · ریزمقیاس: گلبول‌های قرمز در حال عبور از مویرگ"),
        AtlasType("blood", "cell", "سلول‌های خون", "ریزمقیاس · گلبول قرمز، گلبول سفید و پلاکت در پلاسما"),
        AtlasType("artery", "circ", "سرخرگ و سیاهرگ", "سرخرگ: دیوارهٔ ضخیم و کشسان • سیاهرگ: دیوارهٔ نازک با دریچه • مویرگ: دیوارهٔ یک‌سلولی"),
        AtlasType("flow", "circ", "مدار گردش خون", "مدار کوچک (ریوی) و مدار بزرگ (سیستمیک) — خون کم‌اکسیژن و پراکسیژن"),
        AtlasType("resp", "resp", "دستگاه تنفس", "۲بعدی · رسم آموزشی — نای، برونش‌ها، ریه‌ها و دیافراگم"),
        AtlasType("lungs", "d3", "ریه سه‌بعدی", "۳بعدی · رندر ریه‌ها و درخت برونشی"),
        AtlasType("alve", "resp", "حبابچه", "۳بعدی · ریزمقیاس: کیسه‌های هوایی و مویرگ‌های اطراف"),
        AtlasType("trach", "resp", "نای و نایژه", "نای حدود ۱۲ سانتی‌متر با ۱۶ تا ۲۰ حلقهٔ غضروفی — سپس نایژه‌ها و کیسه‌های هوایی"),
        AtlasType("dig", "dig", "دستگاه گوارش", "۲بعدی · رسم آموزشی — مسیر کامل گوارش و اندام‌های کمکی"),
        AtlasType("org3", "d3", "گوارش سه‌بعدی", "۳بعدی · رندر معده، جگر و روده‌ها"),
        AtlasType("liver", "dig", "کبد", "۲بعدی · کبد — بزرگ‌ترین اندام داخلی، حدود ۵۰۰ وظیفه"),
        AtlasType("stomach", "dig", "معده", "۲بعدی · معده — ته‌هسته، بدنه، پیلور و چین‌های مخاط"),
        AtlasType("tooth", "dig", "دندان", "کودکان ۲۰ دندان شیری، بزرگسالان ۳۲ دندان دائمی — مینا سخت‌ترین مادهٔ بدن"),
        AtlasType("panc", "dig", "لوزالمعده", "۲بعدی · لوزالمعده (پانکراس) — آنزیم گوارشی + کارخانهٔ انسولین"),
        AtlasType("brain", "nerv", "مغز", "۲بعدی · مغز از نمای کنار — مخ، مخچه، ساقه مغز"),
        AtlasType("nerv", "nerv", "دستگاه عصبی", "۲بعدی · مغز، نخاع و شبکهٔ اعصاب کل بدن"),
        AtlasType("nerv3", "d3", "عصب سه‌بعدی", "۳بعدی · رندر مغز، نخاع و شبکهٔ اعصاب در بدن شفاف"),
        AtlasType("brain3", "d3", "مغز سه‌بعدی", "۳بعدی · رندر مغز — مخ، مخچه و ساقهٔ مغز"),
        AtlasType("neuron", "nerv", "نورون", "۲بعدی · ریزمقیاس: ساختار نورون و سیناپس"),
        AtlasType("cord", "nerv", "نخاع", "نخاع حدود ۴۵ سانتی‌متر درون کانال مهره‌ها — مادهٔ خاکستری در مرکز"),
        AtlasType("urin", "urin", "دستگاه ادراری", "۲بعدی · کلیه‌ها، غدد فوق کلیوی، حالب‌ها و مثانه"),
        AtlasType("kidney", "d3", "کلیه سه‌بعدی", "۳بعدی · رندر کلیه‌ها، حالب‌ها و مثانه"),
        AtlasType("neph", "urin", "نفرون", "هر کلیه حدود یک میلیون نفرون — روزانه ۱۸۰ لیتر خون فیلتر می‌شود"),
        AtlasType("endo", "endo", "غدد درون‌ریز", "۲بعدی · غدد درون‌ریز: هیپوفیز، تیروئید، آدرنال، پانکراس"),
        AtlasType("thyr", "endo", "تیروئید", "تیروئید با T3 و T4 سوخت‌وساز را تنظیم می‌کند • پاراتیروئید کلسیم خون را کنترل می‌کند"),
        AtlasType("adr", "endo", "فوق کلیه", "غدهٔ فوق کلیه — کورتکس کورتیزول و مدولا آدرنالین ترشح می‌کند"),
        AtlasType("lymph", "lymph", "لنفاوی", "۲بعدی · شبکهٔ عروق و گره‌های لنفاوی، طحال و تیموس"),
        AtlasType("wbc", "cell", "گلبول سفید", "ریزمقیاس · گلبول سفید در حال شکار باکتری"),
        AtlasType("spleen", "lymph", "طحال", "۲بعدی · طحال — فیلتر خون، انبار گلبول و بافت لنفاوی"),
        AtlasType("skin", "body", "پوست", "۲بعدی · مقطع لایه‌های پوست: فولیکول مو، غدد و گیرنده‌ها"),
        AtlasType("embryo", "repro", "رشد جنین", "توسعهٔ جنین — از سلول تخم تا نوزاد در رحم"),
        AtlasType("uterus", "repro", "رحم و تخمدان", "۲بعدی · رحم، لوله‌های رحم و تخمدان‌ها — خانهٔ جنین"),
        AtlasType("testis", "repro", "بیضه", "۲بعدی · بیضه، اپیدیدیم، واز و پروستات"),
        AtlasType("senses", "sense", "حواس پنج‌گانه", "۲بعدی · اندام‌های حسی: چشم، گوش، بینی و زبان"),
        AtlasType("eye", "sense", "چشم", "۲بعدی · چشم — قرنیه، عدسی، شبکیه و عصب بینایی"),
        AtlasType("ear", "sense", "گوش", "۲بعدی · گوش — پردهٔ صماخ، استخوانچه‌ها، حلزون و تعادل"),
        AtlasType("tongue", "sense", "زبان", "۲بعدی · زبان — حوزه‌های چشایی و غدد بزاقی"),
        AtlasType("nose", "sense", "بینی", "۲بعدی · بینی — حفره، پیچک‌ها، عصب بویایی و سینوس‌ها"),
        AtlasType("rbc", "cell", "گلبول قرمز", "ریزمقیاس · گلبول‌های قرمز (اریتروسیت) — دیسک مقعر دوطرفه"),
        AtlasType("plate", "cell", "پلاکت", "ریزمقیاس · پلاکت‌ها (ترومبوسیت) — فعال‌شده با زائده و رشته‌های لخته"),
    )

    /** نگاشت id آناتومی به فایل اطلس — همان FILE مرجع (aliasها حفظ شدند). */
    private val ANATOMY_FILES: Map<String, String> = mapOf(
        "cell3" to "atlas-01.jpg",
        "dna" to "atlas-02.jpg",
        "bodyF" to "atlas-03.jpg",
        "bodyB" to "atlas-04.jpg",
        "bodyS" to "atlas-05.jpg",
        "head" to "atlas-06.jpg",
        "torso" to "atlas-07.jpg",
        "body3" to "atlas-08.jpg",
        "skull" to "atlas-09.jpg",
        "spine" to "atlas-10.jpg",
        "joint" to "atlas-11.jpg",
        "handB" to "atlas-12.jpg",
        "ribs" to "atlas-13.jpg",
        "pelvis" to "atlas-14.jpg",
        "legB" to "atlas-15.jpg",
        "footB" to "atlas-16.jpg",
        "skel" to "atlas-17.jpg",
        "musB" to "atlas-18.jpg",
        "mus3" to "atlas-19.jpg",
        "sarc" to "atlas-20.jpg",
        "biceps" to "atlas-21.jpg",
        "quad" to "atlas-22.jpg",
        "abs" to "atlas-23.jpg",
        "heartM" to "atlas-24.jpg",
        "circ" to "atlas-25.jpg",
        "heart" to "atlas-26.jpg",
        "heart3" to "atlas-27.jpg",
        "cap" to "atlas-28.jpg",
        "blood" to "atlas-29.jpg",
        "artery" to "atlas-30.jpg",
        "flow" to "atlas-31.jpg",
        "resp" to "atlas-32.jpg",
        "lungs" to "atlas-33.jpg",
        "alve" to "atlas-34.jpg",
        "trach" to "atlas-35.jpg",
        "dig" to "atlas-36.jpg",
        "org3" to "atlas-37.jpg",
        "liver" to "atlas-38.jpg",
        "stomach" to "atlas-39.jpg",
        "tooth" to "atlas-40.jpg",
        "panc" to "atlas-41.jpg",
        "brain" to "atlas-42.jpg",
        "nerv" to "atlas-43.jpg",
        "nerv3" to "atlas-44.jpg",
        "brain3" to "atlas-45.jpg",
        "neuron" to "atlas-46.jpg",
        "cord" to "atlas-47.jpg",
        "urin" to "atlas-48.jpg",
        "kidney" to "atlas-49.jpg",
        "neph" to "atlas-50.jpg",
        "endo" to "atlas-51.jpg",
        "thyr" to "atlas-52.jpg",
        "adr" to "atlas-53.jpg",
        "lymph" to "atlas-54.jpg",
        "wbc" to "atlas-55.jpg",
        "spleen" to "atlas-56.jpg",
        "skin" to "atlas-57.jpg",
        "embryo" to "atlas-58.jpg",
        "uterus" to "atlas-59.jpg",
        "testis" to "atlas-60.jpg",
        "senses" to "atlas-61.jpg",
        "eye" to "atlas-62.jpg",
        "ear" to "atlas-63.jpg",
        "tongue" to "atlas-64.jpg",
        "nose" to "atlas-65.jpg",
        "organs" to "atlas-36.jpg",
        "musF" to "atlas-19.jpg",
        "skull3" to "atlas-09.jpg",
        "eye3" to "atlas-62.jpg",
        "rbc" to "rbc.jpg",
        "plate" to "plate.jpg",
        "vein" to "atlas-30.jpg",
        "vessel" to "atlas-25.jpg",
        "blad" to "atlas-48.jpg",
        "gall" to "atlas-38.jpg",
        "intest" to "atlas-36.jpg",
        "armB" to "atlas-12.jpg",
    )

    /** دسته‌های فیزیک — همان PHYS_CATS مرجع. */
    val PHYS_CATS: List<AtlasCategory> = listOf(
        AtlasCategory("all", "همه"),
        AtlasCategory("circ", "مدار"),
        AtlasCategory("force", "نیرو"),
        AtlasCategory("optic", "نور"),
        AtlasCategory("wave", "موج"),
        AtlasCategory("mag", "مغناطیس"),
        AtlasCategory("heat", "گرما"),
        AtlasCategory("nuc", "هسته"),
    )

    /** دسته‌های شیمی — همان CHEM_CATS مرجع. */
    val CHEM_CATS: List<AtlasCategory> = listOf(
        AtlasCategory("all", "همه"),
        AtlasCategory("lab", "آزمایشگاه"),
        AtlasCategory("mol", "مولکول"),
        AtlasCategory("atom", "اتم"),
        AtlasCategory("nrg", "انرژی"),
        AtlasCategory("org", "آلی"),
    )

    /** ۷۰ نوع فیزیک/شیمی مرجع. */
    val SCIENCE_TYPES: List<AtlasType> = listOf(
        AtlasType("cSer", "circ", "مدار سری"),
        AtlasType("cPar", "circ", "مدار موازی"),
        AtlasType("cSim", "circ", "مدار ساده"),
        AtlasType("cSym", "circ", "نمادهای مدار"),
        AtlasType("fbd", "force", "جسم آزاد"),
        AtlasType("inc", "force", "سطح شیب‌دار"),
        AtlasType("pul", "force", "قرقره"),
        AtlasType("lenC", "optic", "عدسی همگرا"),
        AtlasType("lenD", "optic", "عدسی واگرا"),
        AtlasType("mirP", "optic", "آینه تخت"),
        AtlasType("refr", "optic", "شکست نور"),
        AtlasType("wavT", "wave", "موج عرضی"),
        AtlasType("wavL", "wave", "موج طولی"),
        AtlasType("magB", "mag", "آهنربا"),
        AtlasType("coil", "mag", "سیم‌لوله"),
        AtlasType("beak", "lab", "بشر"),
        AtlasType("erl", "lab", "ارلن"),
        AtlasType("rbf", "lab", "بالن"),
        AtlasType("ttub", "lab", "لوله آزمایش"),
        AtlasType("buns", "lab", "چراغ بونزن"),
        AtlasType("bur", "lab", "بورت"),
        AtlasType("h2o", "mol", "آب"),
        AtlasType("co2", "mol", "دی‌اکسید کربن"),
        AtlasType("ch4", "mol", "متان"),
        AtlasType("nh3", "mol", "آمونیاک"),
        AtlasType("o2", "mol", "اکسیژن"),
        AtlasType("bohr", "atom", "مدل بور"),
        AtlasType("shell", "atom", "لایه‌های الکترونی"),
        AtlasType("cMix", "circ", "مدار ترکیبی"),
        AtlasType("lev", "force", "اهرم"),
        AtlasType("vec", "force", "برآیند بردار"),
        AtlasType("mirC", "optic", "آینه کاو"),
        AtlasType("mirV", "optic", "آینه کوژ"),
        AtlasType("wavS", "wave", "موج ایستاده"),
        AtlasType("comp", "mag", "قطب‌نما"),
        AtlasType("therm", "heat", "دماسنج"),
        AtlasType("expan", "heat", "انبساط"),
        AtlasType("pip", "lab", "پیپت"),
        AtlasType("dist", "lab", "تقطیر"),
        AtlasType("elec", "lab", "الکترولیز"),
        AtlasType("nacl", "mol", "کلرید سدیم"),
        AtlasType("ion", "atom", "یون"),
        AtlasType("ph", "atom", "مقیاس pH"),
        AtlasType("decay", "nuc", "واپاشی هسته‌ای"),
        AtlasType("safe", "lab", "علائم ایمنی"),
        AtlasType("spr", "force", "فنر"),
        AtlasType("pend", "force", "آونگ"),
        AtlasType("hydr", "force", "پرس هیدرولیک"),
        AtlasType("float", "force", "شناوری"),
        AtlasType("tir", "optic", "فیبر نوری"),
        AtlasType("prism", "optic", "منشور"),
        AtlasType("wireB", "mag", "میدان سیم راست"),
        AtlasType("trans", "circ", "ترانسفورماتور"),
        AtlasType("mot", "circ", "موتور ساده"),
        AtlasType("titr", "lab", "تیتراسیون"),
        AtlasType("filt", "lab", "صاف کردن"),
        AtlasType("sep", "lab", "قیف جداکننده"),
        AtlasType("volt", "lab", "پیل گالوانی"),
        AtlasType("exo", "nrg", "واکنش گرمازا"),
        AtlasType("endo", "nrg", "واکنش گرماگیر"),
        AtlasType("benz", "org", "بنزن"),
        AtlasType("alk", "org", "آلکان"),
        AtlasType("func", "org", "گروه عاملی"),
        AtlasType("echo", "wave", "پژواک"),
        AtlasType("calor", "heat", "کالریمتر"),
        AtlasType("led", "circ", "دیود نورانی"),
        AtlasType("dyno", "force", "نیوتن‌سنج"),
        AtlasType("baro", "heat", "فشارسنج"),
        AtlasType("litm", "lab", "تورنسل"),
        AtlasType("hcl", "mol", "هیدروکلریک"),
    )

    private val physCatIds = setOf("circ", "force", "optic", "wave", "mag", "heat", "nuc")
    private val chemCatIds = setOf("lab", "mol", "atom", "nrg", "org")

    fun anatomyType(id: String): AtlasType? = ANATOMY_TYPES.firstOrNull { it.id == id }
    fun scienceType(id: String): AtlasType? = SCIENCE_TYPES.firstOrNull { it.id == id }

    /** همان inferDomain مرجع: دستهٔ شیمی → chem، دستهٔ فیزیک → phys. */
    fun scienceDomain(typeId: String): String {
        val cat = scienceType(typeId)?.cat ?: return "phys"
        return if (cat in chemCatIds && cat !in physCatIds) "chem" else "phys"
    }

    /** مسیر asset تصویر اطلس برای spec آناتومی (k='a') یا علوم (k='s'). */
    fun assetPath(spec: FigureSpec): String? = when (spec.kind) {
        "a" -> ANATOMY_FILES[spec.type]?.let { "figure_atlas/anatomy/$it" }
            ?: "figure_atlas/anatomy/atlas-03.jpg"
        "s" -> "figure_atlas/science/${spec.type}.jpg"
        else -> null
    }

    fun displayName(spec: FigureSpec): String = when (spec.kind) {
        "a" -> anatomyType(spec.type)?.name ?: spec.type
        "s" -> scienceType(spec.type)?.name ?: spec.type
        else -> spec.type
    }
}