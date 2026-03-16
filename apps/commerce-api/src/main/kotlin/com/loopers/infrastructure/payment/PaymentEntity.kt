package com.loopers.infrastructure.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payments_order_id", columnList = "order_id"),
        Index(name = "idx_payments_member_order", columnList = "member_id, order_id"),
        Index(name = "idx_payments_pg_transaction_key", columnList = "pg_transaction_key", unique = true),
    ],
)
class PaymentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "card_type", nullable = false)
    val cardType: String,

    @Column(name = "card_no", nullable = false)
    val cardNo: String,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "pg_transaction_key")
    var pgTransactionKey: String? = null,

    @Column(name = "reason")
    var reason: String? = null,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: ZonedDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
