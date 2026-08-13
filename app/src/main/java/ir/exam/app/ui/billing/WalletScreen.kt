package ir.exam.app.ui.billing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.exam.app.core.calendar.JalaliCalendar
import ir.exam.app.core.calendar.PersianDigits
import ir.exam.app.domain.model.WalletRules
import ir.exam.app.domain.model.WalletTransaction
import ir.exam.app.ui.app.Design69Icons
import ir.exam.app.ui.app.LocalNeumorphic69Depth
import ir.exam.app.ui.app.NeumorphicPanel
import ir.exam.app.ui.app.neumorphic69Colors
import java.time.Instant
import java.time.ZoneId

@Composable
fun WalletScreen(refreshKey: Int = 0) {
    val context = LocalContext.current
    val viewModel = remember { BillingViewModel() }
    val state by viewModel.state.collectAsState()
    val neo = neumorphic69Colors
    val balanceTilt = remember { Animatable(0f) }
    var balanceVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            viewModel.load()
            balanceTilt.snapTo(0f)
            balanceTilt.animateTo(10f, tween(250))
            balanceTilt.animateTo(0f, tween(270))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(174.dp)
                    .graphicsLayer {
                        rotationY = balanceTilt.value
                        cameraDistance = 12f * density
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(neo.accent, neo.accent2)))
                    .padding(20.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Design69Icons.Wallet, contentDescription = null, tint = Color.White)
                        Text(
                            "موجودی کیف پول",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp).weight(1f)
                        )
                        IconButton(onClick = { balanceVisible = !balanceVisible }) {
                            Icon(
                                if (balanceVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (balanceVisible) "مخفی‌کردن موجودی" else "نمایش موجودی",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = viewModel::load, enabled = !state.loading) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "تازه‌سازی موجودی", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (!balanceVisible) "••••••••" else state.wallet?.balanceToman?.let {
                            "${formatToman(it)} تومان"
                        } ?: "—",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "هزینه هر سؤال: ${formatToman(WalletRules.QUESTION_COST_TOMAN)} تومان",
                        color = Color.White.copy(alpha = .80f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).align(Alignment.End),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
        item {
            NeumorphicPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = 26.dp,
                depth = LocalNeumorphic69Depth.current,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth(),
        radius = 20.dp,
        depth = 9.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
