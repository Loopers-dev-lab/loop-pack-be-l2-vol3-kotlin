package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payments_order_id", columnList = "order_id"),
        Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true),
    ],
)
@Entity
class PaymentEntity(
    id: Long? = null,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "idempotency_key", nullable = false, unique = true)
    val idempotencyKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Payment.Status,

    @Column(name = "card_type", nullable = false)
    val cardType: String,

    @Column(name = "masked_card_no", nullable = false)
    val maskedCardNo: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(name = "transaction_key")
    var transactionKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code")
    var reasonCode: PaymentReasonCode? = null,

    @Column(name = "request_fingerprint", nullable = false)
    val requestFingerprint: String,
) : BaseEntity() {

    init {
        this.id = id
    }
}
