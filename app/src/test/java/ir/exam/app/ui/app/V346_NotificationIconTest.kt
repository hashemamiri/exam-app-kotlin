package ir.exam.app.ui.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V346 — آیکون اعلان روی گوشی.
 *
 * اشکال: آیکون کوچک اعلان همان لایهٔ تک‌رنگِ آیکون تطبیقی بود (بوم ۱۰۸) که
 * نقش‌مایه‌اش فقط ~۴۳٪ بوم را پر می‌کرد؛ اندروید کل بوم را در جای ۲۴dp می‌نشاند،
 * پس علامتِ دیده‌شدنی ~۱۰dp می‌شد و کاربر آن را «کوچک» می‌دید.
 * این آزمون هندسهٔ آیکون را واقعاً اندازه می‌گیرد، نه فقط وجود فایل را.
 */
class V346_NotificationIconTest {
    private fun root(): File = listOf(File("."), File("..")).first { File(it, "app/src/main/AndroidManifest.xml").isFile }
    private fun src(p: String) = File(root(), p).readText()

    /** جعبهٔ پیمایش مسیر وکتور؛ نقاط کنترل هم شمرده می‌شوند (محافظه‌کارانه). */
    private fun pathBounds(d: String): DoubleArray {
        val tokens = Regex("([MmLlHhVvCcSsQqTtAaZz])|(-?\\d*\\.?\\d+)").findAll(d)
            .map { it.groupValues[1].ifEmpty { it.groupValues[2] } }.toList()
        var x = 0.0; var y = 0.0; var startX = 0.0; var startY = 0.0
        var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        fun mark(px: Double, py: Double) {
            if (px < minX) minX = px; if (px > maxX) maxX = px
            if (py < minY) minY = py; if (py > maxY) maxY = py
        }
        var cmd = ""
        var i = 0
        fun num(): Double = tokens[i++].toDouble()
        while (i < tokens.size) {
            val t = tokens[i]
            if (t.isNotEmpty() && t[0].isLetter()) { cmd = t; i++; continue }
            when (cmd) {
                "M", "m" -> {
                    val dx = num(); val dy = num()
                    if (cmd == "m") { x += dx; y += dy } else { x = dx; y = dy }
                    startX = x; startY = y; mark(x, y)
                    cmd = if (cmd == "m") "l" else "L"
                }
                "L", "l" -> {
                    val dx = num(); val dy = num()
                    if (cmd == "l") { x += dx; y += dy } else { x = dx; y = dy }
                    mark(x, y)
                }
                "H", "h" -> { val dx = num(); x = if (cmd == "h") x + dx else dx; mark(x, y) }
                "V", "v" -> { val dy = num(); y = if (cmd == "v") y + dy else dy; mark(x, y) }
                "C", "c" -> {
                    val v = DoubleArray(6) { num() }
                    if (cmd == "c") { for (k in 0..5) v[k] += if (k % 2 == 0) x else y }
                    mark(v[0], v[1]); mark(v[2], v[3]); mark(v[4], v[5])
                    x = v[4]; y = v[5]
                }
                "A", "a" -> {
                    val rx = num(); val ry = num(); num(); num(); num()
                    val dx = num(); val dy = num()
                    val nx = if (cmd == "a") x + dx else dx
                    val ny = if (cmd == "a") y + dy else dy
                    mark(minOf(x, nx) - rx, minOf(y, ny) - ry)
                    mark(maxOf(x, nx) + rx, maxOf(y, ny) + ry)
                    x = nx; y = ny
                }
                "Z", "z" -> { x = startX; y = startY }
                else -> i++
            }
        }
        return doubleArrayOf(minX, minY, maxX, maxY)
    }

    private fun attr(xml: String, name: String): String =
        Regex("android:$name=\"([^\"]+)\"").find(xml)!!.groupValues[1]

    /**
     * V346.1 — بلوک را با شمارش آکولاد بیرون می‌کشد.
     * برش در «اولین }» غلط بود: به آکولادِ آبجکت داخلی (`{ priority: 'high' }`)
     * می‌خورد و ادامهٔ بلوک (خط message.data) دیده نمی‌شد.
     */
    private fun blockAfter(source: String, marker: String): String {
        val start = source.indexOf(marker)
        assertTrue("نشانگر در فایل پیدا نشد: $marker", start >= 0)
        val open = source.indexOf('{', start + marker.length)
        assertTrue("آکولاد بازکننده پیدا نشد", open >= 0)
        var depth = 0
        var i = open
        while (i < source.length) {
            val ch = source[i]
            if (ch == '{') depth++
            if (ch == '}') {
                depth--
                if (depth == 0) return source.substring(open + 1, i)
            }
            i++
        }
        return source.substring(open + 1)
    }

    @Test
    fun `notification icon is 24dp and its artwork fills the slot`() {
        val icon = src("app/src/main/res/drawable/ic_notification.xml")
        assertEquals("24dp", attr(icon, "width"))
        assertEquals("24dp", attr(icon, "height"))
        val viewport = attr(icon, "viewportWidth").toDouble()
        assertEquals("بوم باید مربع باشد", viewport, attr(icon, "viewportHeight").toDouble(), 0.001)
        assertTrue("بوم باید کوچک باشد نه ۱۰۸ (اندازهٔ آیکون تطبیقی)", viewport < 70.0)

        val tx = attr(icon, "translateX").toDouble()
        val ty = attr(icon, "translateY").toDouble()
        val paths = Regex("android:pathData=\"([^\"]+)\"").findAll(icon).map { it.groupValues[1] }.toList()
        assertTrue("نقش‌مایه باید از آیکون برنامه گرفته شده باشد", paths.size >= 2)

        var minX = Double.MAX_VALUE; var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        for (p in paths) {
            val b = pathBounds(p)
            if (b[0] + tx < minX) minX = b[0] + tx
            if (b[1] + ty < minY) minY = b[1] + ty
            if (b[2] + tx > maxX) maxX = b[2] + tx
            if (b[3] + ty > maxY) maxY = b[3] + ty
        }
        val fill = maxOf(maxX - minX, maxY - minY) / viewport
        assertTrue("نقش‌مایه باید دست‌کم ۷۵٪ جای آیکون را پر کند ولی ${"%.2f".format(fill)} بود", fill >= 0.75)
        assertTrue("نقش‌مایه نباید از بوم بیرون بزند", minX >= -0.5 && minY >= -0.5 && maxX <= viewport + 0.5 && maxY <= viewport + 0.5)
        // سفید و بدون پس‌زمینه: اندروید خودش رنگ می‌زند
        assertTrue("آیکون کوچک باید تک‌رنگ سفید باشد", "#FFFFFF" in icon)
        assertTrue("آیکون کوچک نباید پس‌زمینهٔ رنگی داشته باشد", "<path" in icon && !icon.contains("android:fillColor=\"#6C63F5\""))
    }

    @Test
    fun `the tiny 108-canvas icon is no longer used for notifications`() {
        val service = src("app/src/main/java/ir/exam/app/core/push/PushMessagingService.kt")
        val manifest = src("app/src/main/AndroidManifest.xml")
        assertTrue("سرویس باید آیکون تازه را استفاده کند", ".setSmallIcon(R.drawable.ic_notification)" in service)
        assertTrue("آیکون ۱۰۸ نباید آیکون اعلان باشد", ".setSmallIcon(R.drawable.ic_launcher_monochrome)" !in service)
        assertTrue("مانیفست باید آیکون تازه را اعلام کند",
            "com.google.firebase.messaging.default_notification_icon\" android:resource=\"@drawable/ic_notification\"" in manifest)
        // آیکون تطبیقی سر جایش می‌ماند (برای آیکون تم‌دار لانچر)
        assertTrue("آیکون تطبیقی لانچر دست‌نخورده می‌ماند",
            "@drawable/ic_launcher_monochrome" in src("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"))
    }

    @Test
    fun `notification shows the full colour app icon and the brand colour`() {
        val service = src("app/src/main/java/ir/exam/app/core/push/PushMessagingService.kt")
        assertTrue("آیکون رنگی برنامه کنار اعلان", "setLargeIcon(it)" in service)
        assertTrue("سازندهٔ بیت‌مپ آیکون", "private fun appIconBitmap(context: Context): Bitmap?" in service)
        assertTrue("اندازهٔ استاندارد آیکون بزرگ اعلان", "android.R.dimen.notification_large_icon_width" in service)
        assertTrue("خطای ساخت آیکون نباید اعلان را بیندازد", ".getOrNull()" in service)
        assertTrue("رنگ برند روی آیکون کوچک", ".setColor(ContextCompat.getColor(this, R.color.notification_accent))" in service)
        assertTrue("رنگ برند تعریف شده باشد", "notification_accent" in src("app/src/main/res/values/colors.xml"))
        assertTrue("مانیفست هم رنگ پیش‌فرض داشته باشد",
            "com.google.firebase.messaging.default_notification_color\" android:resource=\"@color/notification_accent\"" in src("app/src/main/AndroidManifest.xml"))
    }

    @Test
    fun `android push stays data-only so the app can build its own notification`() {
        // اگر بلوک notification برگردد، در پس‌زمینه خود FCM اعلان را می‌سازد و
        // آیکون بزرگ و رنگ برندِ این نسخه اعمال نمی‌شود.
        val dispatch = src("supabase/functions/push-dispatch/index.ts")
        val androidBlock = blockAfter(dispatch, "if (platform === 'android')")
        assertTrue("اندروید باید data-only بماند", "delete message.notification" in androidBlock)
        assertTrue("متن باید در data بیاید", "title: row.title" in androidBlock && "body: row.body" in androidBlock)
    }
}
