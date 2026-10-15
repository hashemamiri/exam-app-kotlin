package ir.exam.app.core.update

/**
 * V208 — تجزیهٔ «لیست تغییرات» به بلوک‌های نسخه.
 *
 * قالب text/CHANGELOG_FA.txt از V208: هر نسخه یک سطر عنوان «V208 — خلاصه» و زیر آن چند سطرِ تیتروار که با «- » شروع می‌شوند.
 * از V284 عنوان‌ها یک پرانتز دامنه هم دارند: «V298 (اپ 1.01.98 + سایت) — خلاصه»، «V299 (فقط سایت) — …»، «V291.1 (فقط مستندات)».
 * نسخه‌های قدیمی‌تر فقط یک پاراگراف دارند؛ برای آن‌ها خلاصه با «؛» به چند تیتر کوتاه شکسته می‌شود.
 *
 * V300 — کاربر: «فقط تغییرات مربوط به اپ، تیتروار، بدون نوشتن V299»:
 * - بلوک‌هایی که دامنه‌شان اپ را شامل نمی‌شود (فقط سایت/مستندات/SQL/ابزار) در پنجرهٔ بروزرسانی و اعلان نادیده گرفته می‌شوند.
 * - شمارهٔ داخلی نسخه (Vxxx) هرگز به کاربر نشان داده نمی‌شود؛ به‌جای آن شمارهٔ اپ (مثل 1.01.99) از پرانتز خوانده می‌شود.
 * - دنباله‌های «اپ و سایت.» و «(اپ و سایت)» از انتهای تیترها حذف می‌شوند.
 *
 * - پنجرهٔ بروزرسانی و اعلان «برنامه»: تیترهای نسخه‌(های) جدیدِ مربوط به اپ.
 * - صفحهٔ «درباره»: سه نسخهٔ آخرِ مربوط به اپ، هر کدام با تیترهایش.
 */
data class ReleaseNoteBlock(
    val version: String,
    val summary: String,
    val bullets: List<String>,
    /** متن داخل پرانتز عنوان («اپ 1.01.98 + سایت»، «فقط سایت»)؛ خالی = قالب قدیمی. */
    val scope: String = "",
) {
    /** آیا این نسخه اپ را تغییر داده است؟ (بدون پرانتز = قالب قدیمی = اپ) */
    val appAffecting: Boolean get() = scope.isBlank() || ReleaseNotes.scopeIncludesApp(scope)

    /** شمارهٔ اپ از پرانتز («اپ 1.01.98 + سایت» → «1.01.98»)؛ خالی اگر نبود. */
    val appVersion: String get() = ReleaseNotes.appVersionOf(scope)
}

object ReleaseNotes {
    private val header = Regex("""^(V\d+(?:\.\d+)*)\s*(?:\(([^)]*)\))?\s*(?:[—\-–:]\s*(.*))?$""")
    private val appVersion = Regex("""\d+\.\d+(?:\.\d+)*""")
    private val internalVersion = Regex("""\bV\d+(?:\.\d+)*\b""")
    private val trailingScope = Regex("""[\s،;؛.]*(?:\(?\s*(?:اپ و سایت|سایت و اپ|اپ \+ سایت|فقط اپ|فقط در اپ)\s*\)?)[\s.]*$""")
    const val MAX_BULLETS = 6

    fun scopeIncludesApp(scope: String): Boolean {
        val s = scope.trim()
        if (s.isBlank()) return true
        if (s.startsWith("فقط") && "اپ" !in s) return false
        return "اپ" in s
    }

    fun appVersionOf(scope: String): String = appVersion.find(scope)?.value.orEmpty()

    /** پاک‌سازی تیتر برای نمایش: بدون Vxxx و بدون دنبالهٔ «اپ و سایت». */
    fun cleanBullet(raw: String): String {
        var t = raw.replace("`", "").trim()
        t = internalVersion.replace(t, "").replace(Regex("\\(\\s*\\)"), "").replace(Regex("\\s{2,}"), " ").trim()
        t = trailingScope.replace(t, "").trim()
        return t.trimEnd('،', ';', '؛').trim()
    }

    fun parse(lines: List<String>): List<ReleaseNoteBlock> {
        val blocks = mutableListOf<ReleaseNoteBlock>()
        var version: String? = null
        var scope = ""
        var summary = ""
        var bullets = mutableListOf<String>()
        fun flush() {
            val v = version ?: return
            val list = (if (bullets.isNotEmpty()) bullets.toList() else splitSummary(summary)).map(::cleanBullet).filter { it.isNotEmpty() }
            blocks += ReleaseNoteBlock(v, summary, list.take(MAX_BULLETS), scope)
        }
        for (raw in lines) {
            val line = raw.trim().replace("`", "")
            if (line.isEmpty()) continue
            val m = header.find(line)
            if (m != null) {
                flush()
                version = m.groupValues[1]
                scope = m.groupValues[2].trim()
                summary = m.groupValues[3].trim()
                bullets = mutableListOf()
            } else if (line.startsWith("-") || line.startsWith("•")) {
                bullets += line.removePrefix("-").removePrefix("•").trim()
            } else if (version != null) {
                // ادامهٔ پاراگراف خلاصه
                summary = (summary + " " + line).trim()
            } else {
                // بدون عنوان نسخه (قالب قدیمی یا یادداشت عمومی): هر سطر یک مورد
                blocks += ReleaseNoteBlock("", line, listOf(cleanBullet(line)))
            }
        }
        flush()
        return blocks
    }

    /** خلاصهٔ یک‌پاراگرافی → تیترهای کوتاه (شکستن روی «؛»)؛ هر تیتر حداکثر ۹۰ نویسه. */
    fun splitSummary(summary: String): List<String> =
        summary.split("؛").map { it.trim().trimEnd('.', '،') }.filter { it.isNotEmpty() }
            .map { if (it.length > 90) it.take(88).trimEnd() + "…" else it }

    /** فقط بلوک‌هایی که اپ را تغییر داده‌اند (بلوک‌های بدون عنوان هم می‌مانند). */
    fun appBlocks(lines: List<String>): List<ReleaseNoteBlock> = parse(lines).filter { it.appAffecting }

    /** تیترهای نسخهٔ جدیدِ مربوط به اپ برای پنجرهٔ بروزرسانی (بدون بلوک‌های «فقط سایت/مستندات»). */
    fun latestBullets(lines: List<String>): List<String> = appBlocks(lines).firstOrNull()?.bullets.orEmpty()

    /**
     * V300 — تیترهای اعلان «نسخهٔ جدید برنامه»: چند نسخهٔ اخیرِ مربوط به اپ پشت‌سرهم (جدیدترین اول)،
     * بدون تکرار و حداکثر [max] مورد.
     */
    fun noticeBullets(lines: List<String>, max: Int = 8): List<String> =
        appBlocks(lines).flatMap { it.bullets }.distinct().take(max)

    /** سه نسخهٔ آخرِ مربوط به اپ برای صفحهٔ «درباره». */
    fun lastVersions(lines: List<String>, count: Int = 3): List<ReleaseNoteBlock> = appBlocks(lines).take(count)
}
