package ir.exam.app.core.printing

/**
 * V69.0 — کش LRU کمینه، بدون وابستگی اندروید (JVM-تست‌پذیر).
 *
 * موتور واحد سند پیش از این بیت‌مپ شکل‌ها را در یک HashMap بدون سقف نگه می‌داشت
 * (نشت حافظه در آزمون‌های بزرگ). این کلاس همان رفتار کش را با سقف بایتی و
 * حذف قدیمی‌ترین ورودی‌ها (LRU) فراهم می‌کند تا هم رندر تکراری انجام نشود و هم
 * حافظه محدود بماند.
 *
 * @param maxBytes سقف بودجهٔ کش به بایت.
 * @param sizeOf اندازهٔ تقریبی هر مقدار به بایت.
 */
class LruCacheK<V>(
    private val maxBytes: Long,
    private val sizeOf: (V) -> Long
) {
    private val map = LinkedHashMap<String, V>(64, 0.75f, true)
    private var bytes = 0L

    @Synchronized
    operator fun get(key: String): V? = map[key]

    @Synchronized
    fun put(key: String, value: V) {
        map.remove(key)?.let { bytes -= sizeOf(it) }
        map[key] = value
        bytes += sizeOf(value)
        trim()
    }

    @Synchronized
    fun clear() {
        map.clear()
        bytes = 0
    }

    val size: Int
        @Synchronized get() = map.size

    private fun trim() {
        val iterator = map.entries.iterator()
        while (bytes > maxBytes && iterator.hasNext()) {
            val entry = iterator.next()
            bytes -= sizeOf(entry.value)
            iterator.remove()
        }
    }
}
