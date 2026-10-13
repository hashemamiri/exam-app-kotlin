package ir.exam.app.core.io

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * V263 — ورود دانش‌آموز از اکسل/CSV در اپ (آینهٔ `readSpreadsheet`/`rowsToStudents` سایت V230).
 * بدون کتابخانهٔ خارجی: xlsx = zip (xl/sharedStrings.xml + xl/worksheets/sheet1.xml) با تجزیهٔ regex؛ CSV/TSV با BOM.
 * ستون‌ها از سرستون فارسی/انگلیسی نگاشت می‌شوند؛ بدون سرستون = ترتیب ثابت
 * (نام، نام خانوادگی، نام پدر، پایه، رشته، جنسیت، نام کاربری، رمز).
 */
object SpreadsheetImport {
    data class Row(
        val first: String, val last: String, val father: String, val grade: String, val field: String,
        val gender: String, val username: String, val password: String
    )

    fun readRows(bytes: ByteArray, fileName: String): List<List<String>> {
        val isZip = bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        return if (isZip || fileName.lowercase().endsWith(".xlsx")) readXlsx(ByteArrayInputStream(bytes)) else readDelimited(String(bytes, Charsets.UTF_8))
    }

    fun readDelimited(text: String): List<List<String>> {
        val body = text.removePrefix("\uFEFF")
        val lines = body.split("\r\n", "\n", "\r").filter { it.isNotBlank() }
        val delim = if (lines.firstOrNull().orEmpty().count { it == '\t' } > 0) '\t' else if (lines.firstOrNull().orEmpty().count { it == ';' } > lines.firstOrNull().orEmpty().count { it == ',' }) ';' else ','
        return lines.map { line -> splitCsvLine(line, delim) }
    }

    private fun splitCsvLine(line: String, delim: Char): List<String> {
        val out = mutableListOf<String>(); val sb = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == delim && !quoted -> { out += sb.toString().trim(); sb.setLength(0) }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString().trim()
        return out
    }

    fun readXlsx(input: InputStream): List<List<String>> {
        var shared: List<String> = emptyList(); var sheet: String? = null
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                val name = e.name
                if (name == "xl/sharedStrings.xml") shared = parseSharedStrings(zip.readBytes().toString(Charsets.UTF_8))
                else if (sheet == null && Regex("xl/worksheets/sheet1\\.xml").matches(name)) sheet = zip.readBytes().toString(Charsets.UTF_8)
                else if (sheet == null && name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) sheet = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry(); e = zip.nextEntry
            }
        }
        val xml = sheet ?: return emptyList()
        val rows = mutableListOf<List<String>>()
        val rowRe = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRe = Regex("<c([^>]*?)(?:/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL)
        val refRe = Regex("r=\"([A-Z]+)\\d+\""); val typeRe = Regex("t=\"(\\w+)\"")
        val vRe = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL); val tRe = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
        for (rm in rowRe.findAll(xml)) {
            val cells = mutableMapOf<Int, String>(); var auto = 0
            for (cm in cellRe.findAll(rm.groupValues[1])) {
                val attrs = cm.groupValues[1]; val inner = cm.groupValues[2]
                val col = refRe.find(attrs)?.groupValues?.get(1)?.let(::colIndex) ?: auto
                auto = col + 1
                val type = typeRe.find(attrs)?.groupValues?.get(1)
                val value = when (type) {
                    "s" -> vRe.find(inner)?.groupValues?.get(1)?.trim()?.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
                    "inlineStr" -> tRe.findAll(inner).joinToString("") { unescape(it.groupValues[1]) }
                    else -> unescape(vRe.find(inner)?.groupValues?.get(1).orEmpty())
                }
                cells[col] = value
            }
            if (cells.isEmpty()) continue
            val width = (cells.keys.maxOrNull() ?: -1) + 1
            rows += List(width) { cells[it].orEmpty().trim() }
        }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val siRe = Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL); val tRe = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
        return siRe.findAll(xml).map { si -> tRe.findAll(si.groupValues[1]).joinToString("") { unescape(it.groupValues[1]) } }.toList()
    }

    private fun colIndex(letters: String): Int { var n = 0; for (ch in letters) n = n * 26 + (ch - 'A' + 1); return n - 1 }
    private fun unescape(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")

    private val HEADERS = linkedMapOf(
        "first" to Regex("^(نام|first)"), "last" to Regex("خانوادگی|last"), "father" to Regex("پدر|father"), "grade" to Regex("پایه|grade"),
        "field" to Regex("رشته|field"), "gender" to Regex("جنسیت|gender"), "username" to Regex("کاربری|user"), "password" to Regex("رمز|pass")
    )

    /** نگاشت ردیف‌ها به دانش‌آموز؛ ردیف‌های بدون نام حذف می‌شوند. رمز کوتاه‌تر از ۸ خالی برمی‌گردد تا فراخوان رمز بسازد. */
    fun toStudents(rows: List<List<String>>): List<Row> {
        if (rows.isEmpty()) return emptyList()
        val head = rows.first().map { it.lowercase().trim() }
        val map = HashMap<String, Int>()
        var data = rows
        if (head.any { Regex("خانوادگی|نام|جنسیت|پایه").containsMatchIn(it) }) {
            head.forEachIndexed { i, h -> HEADERS.forEach { (k, re) -> if (!map.containsKey(k) && re.containsMatchIn(h)) map[k] = i } }
            if (map["first"] != null && map["first"] == map["last"]) map.remove("first")
            if (map["first"] == null) head.indexOfFirst { it.trim() == "نام" }.takeIf { it >= 0 }?.let { map["first"] = it }
            data = rows.drop(1)
        } else listOf("first", "last", "father", "grade", "field", "gender", "username", "password").forEachIndexed { i, k -> map[k] = i }
        fun g(r: List<String>, k: String) = map[k]?.let { r.getOrNull(it) }.orEmpty().trim()
        return data.map { r ->
            val gRaw = g(r, "gender").lowercase()
            val gender = when {
                Regex("female|دختر|زن|^f$|^2$").containsMatchIn(gRaw) -> "female"
                Regex("male|پسر|مرد|^m$|^1$").containsMatchIn(gRaw) -> "male"
                else -> ""
            }
            val u = toEnglishDigits(g(r, "username")).lowercase().replace(Regex("[^a-z0-9_]"), "")
            val pw = toEnglishDigits(g(r, "password"))
            Row(g(r, "first"), g(r, "last"), g(r, "father"), g(r, "grade"), g(r, "field"), gender, u, if (pw.length >= 8) pw else "")
        }.filter { it.first.isNotBlank() || it.last.isNotBlank() }
    }

    private fun toEnglishDigits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) sb.append(when (ch) { in '۰'..'۹' -> ('0' + (ch - '۰')); in '٠'..'٩' -> ('0' + (ch - '٠')); else -> ch })
        return sb.toString()
    }
}
