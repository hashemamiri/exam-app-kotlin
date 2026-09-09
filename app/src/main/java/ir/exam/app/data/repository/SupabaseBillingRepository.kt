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
import ir.exam.app.domain.repository.PrintChargeResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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

    // V132 — هزینهٔ چاپ: rpc native_charge_print_v1 (همان الگوی native_duplicate_exam_v2).
    override suspend fun chargePrint(examId: String, operationId: String, questionCount: Int, mode: String): Result<PrintChargeResult> = runCatching {
        val raw: JsonObject = SupabaseProvider.client.postgrest.rpc("native_charge_print_v1", buildJsonObject {
            put("p_exam", examId)
            put("p_operation", operationId)
            put("p_questions", questionCount)
            put("p_mode", mode)
        }).decodeAs()
        raw["error"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)?.let { message ->
            val required = raw["required"]?.jsonPrimitive?.longOrNull
            val balance = raw["balance"]?.jsonPrimitive?.longOrNull
            error(if (required != null && balance != null) "$message (لازم: $required تومان، موجودی: $balance تومان)" else message)
        }
        PrintChargeResult(
            costToman = raw["cost"]?.jsonPrimitive?.longOrNull ?: 0,
            balanceToman = raw["balance"]?.jsonPrimitive?.longOrNull
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
