package ir.exam.app.core.update

/**
 * V208 — تجزیهٔ «لیست تغییرات» به بلوک‌های نسخه.
 *
 * قالب text/CHANGELOG_FA.txt از V208: هر نسخه یک سطر عنوان «V208 — خلاصه» و زیر آن چند سطرِ تیتروار که با «- » شروع می‌شوند.
 * نسخه‌های قدیمی‌تر فقط یک پاراگراف دارند؛ برای آن‌ها خلاصه با «؛» به چند تیتر کوتاه شکسته می‌شود.
 *
 * - پنجرهٔ بروزرسانی: فقط تیترهای نسخهٔ جدید (بلوک اول).
 * - صفحهٔ «درباره»: سه نسخهٔ آخر، هر کدام با تیترهایش.
 */
data class ReleaseNoteBlock(val version: String, val summary: String, val bullets: List<String>)

object ReleaseNotes {
    private val header = Regex("""^(V\d+(?:\.\d+)*)\s*[—\-–:]\s*(.*)$""")
    const val MAX_BULLETS = 6

    fun parse(lines: List<String>): List<ReleaseNoteBlock> {
        val blocks = mutableListOf<ReleaseNoteBlock>()
        var version: String? = null
        var summary = ""
        var bullets = mutableListOf<String>()
        fun flush() {
            val v = version ?: return
            val list = if (bullets.isNotEmpty()) bullets.toList() else splitSummary(summary)
            blocks += ReleaseNoteBlock(v, summary, list.take(MAX_BULLETS))
        }
        for (raw in lines) {
            val line = raw.trim().replace("`", "")
            if (line.isEmpty()) continue
            val m = header.find(line)
            if (m != null) {
                flush()
                version = m.groupValues[1]
                summary = m.groupValues[2].trim()
                bullets = mutableListOf()
            } else if (line.startsWith("-") || line.startsWith("•")) {
                bullets += line.removePrefix("-").removePrefix("•").trim()
            } else if (version != null) {
                // ادامهٔ پاراگراف خلاصه
                summary = (summary + " " + line).trim()
            } else {
                // بدون عنوان نسخه (قالب قدیمی یا یادداشت عمومی): هر سطر یک مورد
                blocks += ReleaseNoteBlock("", line, listOf(line))
            }
        }
        flush()
        return blocks
    }

    /** خلاصهٔ یک‌پاراگرافی → تیترهای کوتاه (شکستن روی «؛»)؛ هر تیتر حداکثر ۹۰ نویسه. */
    fun splitSummary(summary: String): List<String> =
        summary.split("؛").map { it.trim().trimEnd('.', '،') }.filter { it.isNotEmpty() }
            .map { if (it.length > 90) it.take(88).trimEnd() + "…" else it }

    /** تیترهای نسخهٔ جدید برای پنجرهٔ بروزرسانی. */
    fun latestBullets(lines: List<String>): List<String> = parse(lines).firstOrNull()?.bullets.orEmpty()

    /** سه نسخهٔ آخر برای صفحهٔ «درباره». */
    fun lastVersions(lines: List<String>, count: Int = 3): List<ReleaseNoteBlock> = parse(lines).take(count)
}
