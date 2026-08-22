package ir.exam.app.ui.math

import android.content.Context
import java.nio.charset.StandardCharsets

/**
 * دسترسی به asset کتابخانهٔ V34 (کتب درسی / نماد و تزئین / زیست و دانشگاه).
 *
 * فایل از `assets/formula/install_lib_v34.js` یک‌بار و به‌صورت تنبل خوانده و
 * کش می‌شود. این object عمداً فایل جداگانه است تا ارجاع به
 * MathEditorWebViewDialog در توابع کمکی نباشد؛ K2 در کامپایل برخی ترکیب‌های
 * `clazz.getResourceAsStream` درون همین فایل را به‌اشتباه به‌عنوان
 * فراخوانی @Composable تشخیص می‌داد (به V45.7.1 و V45.7.2 رجوع کنید).
 */
object FormulaV34Library {
    private const val ASSET_PATH = "formula/install_lib_v34.js"

    @Volatile
    private var cached: String? = null

    /** محتوای جاوااسکریپت کتابخانه را برمی‌گرداند؛ در صورت نبود فایل، رشتهٔ خالی. */
    fun load(context: Context): String {
        cached?.let { return it }
        val text = runCatching {
            context.assets.open(ASSET_PATH).use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            }
        }.getOrDefault("")
        cached = text
        return text
    }
}
