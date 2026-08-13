package ir.exam.app.data.repository

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import ir.exam.app.BuildConfig
import ir.exam.app.data.dto.PaymentLaunchDto
import ir.exam.app.data.dto.WalletSnapshotDto
import ir.exam.app.data.remote.SupabaseProvider
import ir.exam.app.domain.model.PaymentLaunch
import ir.exam.app.domain.model.WalletRules
import ir.exam.app.domain.model.WalletSnapshot
import ir.exam.app.domain.model.WalletTransaction
import ir.exam.app.domain.repository.BillingRepository
import java.net.URI
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseBillingRepository : BillingRepository {
    override suspend fun wallet(): Result<WalletSnapshot> = runCatching {
        val dto = SupabaseProvider.client.postgrest.rpc("native_wallet_snapshot")
            .decodeAs<WalletSnapshotDto>()
        dto.error?.takeIf(String::isNotBlank)?.let(::error)
        WalletSnapshot(
            balanceToman = dto.balance.coerceAtLeast(0),
            currency = dto.currency,
            transactions = dto.transactions.map { tx ->
                WalletTransaction(
                    id = tx.id,
                    amountToman = tx.amount,
                    reason = tx.reason.orEmpty(),
                    balanceAfterToman = tx.balanceAfter ?: 0,
                    createdAt = tx.createdAt.orEmpty()
                )
            }
        )
    }

    override suspend fun requestTopUp(amountToman: Long, currentBalanceToman: Long): Result<PaymentLaunch> = runCatching {
        WalletRules.validateTopUp(amountToman, currentBalanceToman)
        val dto = SupabaseProvider.client.functions.invoke(
            "wallet-payment",
            body = buildJsonObject { put("amount_toman", amountToman) }
        ).body<PaymentLaunchDto>()
        dto.error?.takeIf(String::isNotBlank)?.let { message ->
            error(dto.code?.takeIf(String::isNotBlank)?.let { "$it: $message" } ?: message)
        }
        val url = dto.url
        if (!dto.credited) {
            requireSafeCheckoutUrl(url ?: error("نشانی درگاه از سرور دریافت نشد."))
        }
        PaymentLaunch(
            orderId = dto.orderId ?: error("شناسه سفارش از سرور دریافت نشد."),
            checkoutUrl = url,
            provider = dto.provider.orEmpty(),
            sandbox = dto.sandbox,
            credited = dto.credited,
            balanceAfterToman = dto.balance
        )
    }

    private fun requireSafeCheckoutUrl(value: String) {
        val uri = runCatching { URI(value) }.getOrNull() ?: error("نشانی درگاه معتبر نیست.")
        require(uri.scheme.equals("https", true)) { "درگاه پرداخت باید از اتصال امن استفاده کند." }
        val host = uri.host?.lowercase() ?: error("میزبان درگاه معتبر نیست.")
        val supabaseHost = runCatching { URI(BuildConfig.SUPABASE_URL).host?.lowercase() }.getOrNull()
        val allowed = host == "payment.zarinpal.com" || host.endsWith(".idpay.ir") || host == "idpay.ir" || host == supabaseHost
        require(allowed) { "نشانی درگاه در فهرست مجاز نیست." }
    }
}
