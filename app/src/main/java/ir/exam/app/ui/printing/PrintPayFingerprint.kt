package ir.exam.app.ui.printing

/**
 * V199 — اثر انگشت سربرگ برای پرداخت چاپ آزمون چاپی (native_print_quote/pay/pay_status_v199).
 * عیناً همان قاعدهٔ سایت (app.js: headerFingerprint):
 *  • فقط کلیدهای `f_*` (فیلدهای سربرگ موتور چاپ)؛
 *  • `f_course` و `f_duration` حذف می‌شوند (از خودِ آزمون چاپی می‌آیند و سرور درس/مدت را خودش به هش می‌افزاید)؛
 *  • مقدارهای خالی حذف؛ `f_headerTemplate` خالی = «classic»؛
 *  • مرتب بر اساس کلید؛ هر خط «k=v» و خط‌ها با \n به هم می‌چسبند. سرور md5 می‌کند.
 * اگر این متن عوض شود سرور «سربرگ تغییر کرده» می‌بیند و کل هزینه دوباره محاسبه می‌شود.
 */
object PrintPayFingerprint {
    fun headerFingerprint(fields: Map<String, String?>): String {
        val m = LinkedHashMap<String, String>()
        m["f_headerTemplate"] = "classic"
        fields.forEach { (k, v) -> if (k.startsWith("f_")) m[k] = v?.trim().orEmpty() }
        if (m["f_headerTemplate"].isNullOrBlank()) m["f_headerTemplate"] = "classic"
        return m.entries
            .filter { (k, v) -> k != "f_course" && k != "f_duration" && v.isNotBlank() }
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) -> "$k=$v" }
    }

    /** سربرگ فعلی دستگاه (PrintHeaderStore) — همان چیزی که ExamHtmlPrintPayloadBuilder به موتور می‌دهد. */
    fun current(context: android.content.Context): String =
        headerFingerprint(ir.exam.app.data.local.PrintHeaderStore(context.applicationContext).read())
}
