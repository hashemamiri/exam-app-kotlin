package ir.exam.app.ui.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ir.exam.app.R

/**
 * V338 — «ما را دنبال کنید» در بخش «درباره».
 *
 * زیر بخش بروزرسانی و بالای «تماس با ما و نظرات» یک دکمه نشان داده می‌شود؛ با لمس آن
 * پنجرهٔ کوچکی باز می‌شود که **فقط آیکن‌های رسمی** کانال‌ها را نشان می‌دهد (بدون نوشته).
 * با لمس هر آیکن، نشانی رسمی همان کانال باز می‌شود: اگر اپ آن سرویس نصب باشد و نشانی
 * را پشتیبانی کند، همان اپ باز می‌شود؛ در غیر این صورت سایت آن در مرورگر باز می‌شود.
 *
 * آیکن‌ها نشان‌های رسمی همان سرویس‌ها هستند و به‌صورت منبع drawable داخل اپ بسته‌بندی
 * شده‌اند (`social_*.png`) تا نمایش آن‌ها به اینترنت یا در دسترس بودن دامنه وابسته نباشد.
 * آینهٔ سایتی همین دکمه در V337.7 ساخته شد (نوار پیوندهای دسکتاپ، منوی دسکتاپ و زیر
 * «محصولات و تعرفه‌ها» در گوشی).
 */
internal data class SocialChannel(val label: String, val url: String, val iconRes: Int)

/** ترتیب نمایش، راست‌به‌چپ: ایتا، بله، روبیکا، سروش / شاد، تلگرام، آپارات. */
internal val SOCIAL_CHANNELS: List<SocialChannel> = listOf(
    SocialChannel("ایتا", "https://eitaa.com/onlineexam_ir", R.drawable.social_eitaa),
    SocialChannel("بله", "https://ble.ir/onlineexam_ir", R.drawable.social_bale),
    SocialChannel("روبیکا", "https://rubika.ir/onlineexam_ir", R.drawable.social_rubika),
    SocialChannel("سروش", "https://splus.ir/onlineexam_ir", R.drawable.social_soroush),
    SocialChannel("شاد", "https://shad.ir/onlineexam_ir", R.drawable.social_shad),
    SocialChannel("تلگرام", "https://t.me/onlineexam_ir", R.drawable.social_telegram),
    SocialChannel("آپارات", "https://www.aparat.com/onlineexam_ir", R.drawable.social_aparat)
)

/** نشانی رسمی کانال را باز می‌کند؛ نبودِ برنامه/مرورگر به‌جای کرش، پیام روشن می‌دهد. */
internal fun openSocialChannel(context: Context, url: String, onError: (Throwable) -> Unit) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        onError(IllegalStateException("برنامه یا مرورگری برای بازکردن این کانال پیدا نشد.", error))
    } catch (error: SecurityException) {
        onError(IllegalStateException("بازکردن این کانال در این دستگاه مجاز نیست.", error))
    }
}

/**
 * دکمهٔ «ما را دنبال کنید» + پنجرهٔ آیکن‌ها.
 *
 * شبکهٔ آیکن‌ها با `Row`های چهارتایی ساخته می‌شود (نه FlowRow آزمایشی) و جای خالیِ
 * ردیف دوم با `Spacer` هم‌وزن پر می‌شود تا اندازهٔ همهٔ آیکن‌ها در هر عرض صفحه یکسان
 * بماند و روی گوشی‌های باریک هم چیزی بیرون نزند.
 */
@Composable
fun FollowUsButton(onError: (Throwable) -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Share, contentDescription = null)
        Text("ما را دنبال کنید")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SOCIAL_CHANNELS.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { channel ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable {
                                            open = false
                                            openSocialChannel(context, channel.url, onError)
                                        }
                                ) {
                                    Image(
                                        painter = painterResource(channel.iconRes),
                                        contentDescription = channel.label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(8.dp)
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("بستن") } }
        )
    }
}
