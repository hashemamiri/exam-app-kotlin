package ir.exam.app.ui.billing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.domain.model.WalletRules
import ir.exam.app.domain.model.WalletTransaction
import java.time.Instant
import java.time.ZoneId

@Composable
fun WalletScreen() {
    val context = LocalContext.current
    val viewModel = remember { BillingViewModel() }
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("موجودی کیف پول", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                        IconButton(onClick = viewModel::load, enabled = !state.loading) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "تازه‌سازی موجودی")
                        }
                    }
                    Text(
                        text = state.wallet?.balanceToman?.let { "${formatToman(it)} تومان" } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("هزینه هر سؤال: ${formatToman(WalletRules.QUESTION_COST_TOMAN)} تومان")
                    if (state.loading) CircularProgressIndicator()
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("شارژ امن کیف پول", style = MaterialTheme.typography.titleMedium)
                    Text("پرداخت فقط در Edge Function تأیید می‌شود؛ برنامه اجازه شارژ مستقیم موجودی را ندارد.")
                    OutlinedTextField(
                        value = PersianDigits.convert(state.topUpAmount),
                        onValueChange = { viewModel.setTopUpAmount(PersianDigits.latin(it)) },
                        label = { Text("مبلغ به تومان") },
                        supportingText = { Text("حداقل ۱۰۰٬۰۰۰ · مضرب ۱۰٬۰۰۰ · سقف موجودی ۱۰٬۰۰۰٬۰۰۰") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100_000L, 250_000L, 500_000L).forEach { amount ->
                            FilterChip(
                                selected = state.topUpAmount.toLongOrNull() == amount,
                                onClick = { viewModel.selectPreset(amount) },
                                label = { Text(formatToman(amount)) }
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::startPayment,
                        enabled = !state.startingPayment && state.wallet != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.startingPayment) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                        Text(if (state.startingPayment) "در حال ساخت سفارش..." else "ساخت سفارش پرداخت")
                    }
                    state.payment?.let { payment ->
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payment.checkoutUrl)))
                                    viewModel.paymentOpened()
                                } catch (_: ActivityNotFoundException) {
                                    // دکمهٔ تازه‌سازی و پیام عمومی باقی می‌مانند؛ URL حساس در UI چاپ نمی‌شود.
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                            Text("رفتن به درگاه امن")
                        }
                    }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text("پس از بازگشت از درگاه، «تازه‌سازی موجودی» را بزنید.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("گردش‌های اخیر", style = MaterialTheme.typography.titleMedium) }
        val transactions = state.wallet?.transactions.orEmpty()
        if (!state.loading && transactions.isEmpty()) {
            item { Text("هنوز تراکنشی ثبت نشده است.") }
        }
        items(transactions, key = { it.id }) { transaction -> TransactionCard(transaction) }
    }
}

@Composable
private fun TransactionCard(transaction: WalletTransaction) {
    val positive = transaction.amountToman >= 0
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(transaction.reason.faReason(), fontWeight = FontWeight.SemiBold)
                Text(formatTransactionDate(transaction.createdAt), style = MaterialTheme.typography.bodySmall)
                Text("مانده: ${formatToman(transaction.balanceAfterToman)} تومان", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = (if (positive) "+ " else "− ") + formatToman(kotlin.math.abs(transaction.amountToman)),
                color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatToman(value: Long): String = PersianDigits.convert("%,d".format(java.util.Locale.US, value))

private fun formatTransactionDate(value: String): String = runCatching {
    val local = Instant.parse(value).atZone(ZoneId.systemDefault())
    val jalali = JalaliCalendar.fromGregorian(local.toLocalDate())
    "${jalali.display()} — ${PersianDigits.convert(local.toLocalTime().toString().take(5))}"
}.getOrDefault("")

private fun String.faReason(): String = when {
    startsWith("payment:") -> "شارژ از درگاه"
    startsWith("exam:create:") -> "ساخت آزمون"
    startsWith("exam:update:") -> "ویرایش آزمون"
    startsWith("exam:duplicate:") -> "تکثیر آزمون"
    startsWith("refund") -> "بازگشت وجه"
    this == "topup" -> "شارژ"
    else -> ifBlank { "تراکنش کیف پول" }
}
