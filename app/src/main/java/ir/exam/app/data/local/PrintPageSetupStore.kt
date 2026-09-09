package ir.exam.app.data.local

import android.content.Context
import android.print.PrintAttributes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * V121 — «تنظیمات صفحه» موتور چاپ (برگرفته از موتور PGS آزمون‌ساز v20):
 * کاغذ، جهت، حاشیه‌ها، کادر، شمارهٔ صفحه، تکرارِ سرستون، فونتِ پایه، فاصلهٔ سؤال‌ها،
 * نمایشِ بارم. مثلِ [PrintHeaderStore] روی دستگاه می‌ماند و برای همهٔ آزمون‌های چاپی
 * به کار می‌رود. موتورِ وب (print/web) همین JSON را با کلیدِ `pageSetup` از `setExamData`
 * می‌گیرد و هر تغییر در پنلِ 📐 خودش را با `ExamPrintBridge.pageSetupChanged(json)` برمی‌گرداند
 * (V126؛ به‌جای localStorage وب)؛ چاپ همان چیدمانِ پیش‌نمایش است.
 */
data class PrintPageSetup(
    val paper: String = "a4",
    val orient: String = "portrait",
    val customW: Int = 210,
    val customH: Int = 297,
    val mT: Int = 5,
    val mB: Int = 5,
    val mR: Int = 5,
    val mL: Int = 5,
    val border: Boolean = true,
    val pageNumbers: Boolean = true,
    val repeatHeader: Boolean = false,
    val font: Int = 10,
    val spacing: String = "normal",
    val showScores: Boolean = true
) {
    /** JSON برای رندرر (همهٔ مقادیر اعتبارسنجی‌شده‌اند؛ رشته‌ها فقط از فهرستِ ثابت). */
    fun toJson(): String {
        val p = if (paper in PAPERS.keys || paper == "custom") paper else "a4"
        val o = if (orient == "landscape") "landscape" else "portrait"
        val sp = if (spacing in SPACINGS) spacing else "normal"
        return "{\"paper\":\"$p\",\"orient\":\"$o\",\"customW\":${customW.coerceIn(60, 600)},\"customH\":${customH.coerceIn(60, 600)}," +
            "\"mT\":${mT.coerceIn(0, 80)},\"mB\":${mB.coerceIn(0, 80)},\"mR\":${mR.coerceIn(0, 80)},\"mL\":${mL.coerceIn(0, 80)}," +
            "\"border\":$border,\"pageNumbers\":$pageNumbers,\"repeatHeader\":$repeatHeader,\"font\":${font.coerceIn(6, 20)}," +
            "\"spacing\":\"$sp\",\"showScores\":$showScores}"
    }

    /** اندازهٔ کاغذ به میلی‌متر با درنظرگرفتنِ جهت. */
    fun pageMm(): Pair<Double, Double> {
        val (w, h) = if (paper == "custom") customW.toDouble() to customH.toDouble() else (PAPERS[paper] ?: PAPERS.getValue("a4"))
        return if (orient == "landscape" && w < h) h to w else w to h
    }

    /**
     * صفاتِ چاپِ اندروید هم‌اندازه با برگه (پیش‌تر همیشه A4 پیش‌فرضِ سیستم بود و
     * برگه‌های A5/نامه در پنلِ چاپ کوچک/بریده می‌شدند).
     */
    fun printAttributes(): PrintAttributes {
        val (w, h) = pageMm()
        val mils = { mm: Double -> (mm / 25.4 * 1000).toInt() }
        val base = when (paper) {
            "a4" -> PrintAttributes.MediaSize.ISO_A4
            "a5" -> PrintAttributes.MediaSize.ISO_A5
            "b5" -> PrintAttributes.MediaSize.ISO_B5
            "letter" -> PrintAttributes.MediaSize.NA_LETTER
            "legal" -> PrintAttributes.MediaSize.NA_LEGAL
            else -> PrintAttributes.MediaSize("exam_custom_${customW}x$customH", "سفارشی", mils(minOf(w, h)), mils(maxOf(w, h)))
        }
        val size = if (orient == "landscape") base.asLandscape() else base.asPortrait()
        return PrintAttributes.Builder()
            .setMediaSize(size)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    }

    companion object {
        val PAPERS: Map<String, Pair<Double, Double>> = linkedMapOf(
            "a4" to (210.0 to 297.0), "a5" to (148.0 to 210.0), "b5" to (176.0 to 250.0),
            "letter" to (215.9 to 279.4), "f4" to (210.0 to 330.0), "legal" to (215.9 to 355.6)
        )
        val PAPER_NAMES: Map<String, String> = linkedMapOf(
            "a4" to "A4", "a5" to "A5", "b5" to "B5", "letter" to "Letter", "f4" to "F4", "legal" to "Legal", "custom" to "سفارشی"
        )
        val SPACINGS = listOf("compact", "normal", "open")

        /**
         * V126 — خواندنِ JSONِ پنلِ «تنظیمات صفحه»ی خودِ موتورِ وب (همان کلیدهای [toJson]).
         * مقادیرِ نامعتبر به پیش‌فرض برمی‌گردند؛ خروجی هرگز null نیست.
         */
        fun fromJson(json: String?): PrintPageSetup {
            val d = PrintPageSetup()
            val o = runCatching { Json.parseToJsonElement(json.orEmpty()).jsonObject }.getOrNull() ?: return d
            fun str(k: String) = o[k]?.jsonPrimitive?.contentOrNull
            fun int(k: String, def: Int) = o[k]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.toInt() ?: def
            fun bool(k: String, def: Boolean) = when (str(k)) { "true" -> true; "false" -> false; else -> def }
            val paper = str("paper")?.takeIf { it in PAPERS.keys || it == "custom" } ?: d.paper
            return PrintPageSetup(
                paper = paper,
                orient = if (str("orient") == "landscape") "landscape" else "portrait",
                customW = int("customW", d.customW).coerceIn(60, 600),
                customH = int("customH", d.customH).coerceIn(60, 600),
                mT = int("mT", d.mT).coerceIn(0, 80), mB = int("mB", d.mB).coerceIn(0, 80),
                mR = int("mR", d.mR).coerceIn(0, 80), mL = int("mL", d.mL).coerceIn(0, 80),
                border = bool("border", d.border),
                pageNumbers = bool("pageNumbers", d.pageNumbers),
                repeatHeader = bool("repeatHeader", d.repeatHeader),
                font = int("font", d.font).coerceIn(6, 20),
                spacing = str("spacing")?.takeIf { it in SPACINGS } ?: d.spacing,
                showScores = bool("showScores", d.showScores)
            )
        }
    }
}

class PrintPageSetupStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences("print_page_setup", Context.MODE_PRIVATE)

    fun read(): PrintPageSetup = with(preferences) {
        val d = PrintPageSetup()
        PrintPageSetup(
            paper = getString("paper", d.paper) ?: d.paper,
            orient = getString("orient", d.orient) ?: d.orient,
            customW = getInt("customW", d.customW),
            customH = getInt("customH", d.customH),
            mT = getInt("mT", d.mT), mB = getInt("mB", d.mB), mR = getInt("mR", d.mR), mL = getInt("mL", d.mL),
            border = getBoolean("border", d.border),
            pageNumbers = getBoolean("pageNumbers", d.pageNumbers),
            repeatHeader = getBoolean("repeatHeader", d.repeatHeader),
            font = getInt("font", d.font),
            spacing = getString("spacing", d.spacing) ?: d.spacing,
            showScores = getBoolean("showScores", d.showScores)
        )
    }

    fun write(s: PrintPageSetup) {
        preferences.edit()
            .putString("paper", s.paper).putString("orient", s.orient)
            .putInt("customW", s.customW).putInt("customH", s.customH)
            .putInt("mT", s.mT).putInt("mB", s.mB).putInt("mR", s.mR).putInt("mL", s.mL)
            .putBoolean("border", s.border).putBoolean("pageNumbers", s.pageNumbers)
            .putBoolean("repeatHeader", s.repeatHeader).putInt("font", s.font)
            .putString("spacing", s.spacing).putBoolean("showScores", s.showScores)
            .apply()
    }

    fun clear() = preferences.edit().clear().apply()
}
