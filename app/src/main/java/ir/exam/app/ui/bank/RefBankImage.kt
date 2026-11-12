package ir.exam.app.ui.bank

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import ir.exam.app.domain.bank.ReferenceBankContract

/**
 * V372 — تصویر بانک مرجع با گارد «تکه‌ای».
 *
 * در استخراج از PDF بعضی علامت‌های ریز (نمونه: «؟» ۱۶×۳۲) به‌صورت تصویر جدا
 * بریده شده‌اند و در ده‌ها سؤال تکرار می‌شوند. آن‌ها نه به متن چیزی می‌افزایند
 * و نه خوانا هستند؛ پس پنهان می‌شوند.
 *
 * قاعده عیناً همان است که ابزار داده و سایت به کار می‌برند
 * ([ReferenceBankContract.isTinyImage]) تا اگر دوباره چنین تکه‌ای وارد شد
 * یا نسخهٔ کهنهٔ کش‌شده به دست کاربر رسید، دیده نشود.
 *
 * تنها اندازه پس از بار شدنِ خود تصویر سنجیده می‌شود؛ هیچ درخواست شبکهٔ
 * اضافه‌ای در کار نیست.
 */
@Composable
fun RefBankImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var visible by remember(url) { mutableStateOf(true) }
    if (!visible) return
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onSuccess = { state ->
            val drawable = state.result.drawable
            if (ReferenceBankContract.isTinyImage(drawable.intrinsicWidth, drawable.intrinsicHeight)) {
                visible = false
            }
        }
    )
}
