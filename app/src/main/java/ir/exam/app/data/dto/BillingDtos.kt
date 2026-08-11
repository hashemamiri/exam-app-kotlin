package ir.exam.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletTransactionDto(
    val id: Long,
    val amount: Long,
    val reason: String? = null,
    @SerialName("balance_after") val balanceAfter: Long? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class WalletSnapshotDto(
    val ok: Boolean = false,
    val balance: Long = 0,
    val currency: String = "toman",
    val transactions: List<WalletTransactionDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class PaymentLaunchDto(
    val ok: Boolean = false,
    val url: String? = null,
    @SerialName("order_id") val orderId: Long? = null,
    val provider: String? = null,
    val sandbox: Boolean = false,
    val error: String? = null,
    val code: String? = null
)
