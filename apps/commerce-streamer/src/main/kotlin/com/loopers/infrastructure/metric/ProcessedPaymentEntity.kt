package com.loopers.infrastructure.metric

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Table(
    name = "processed_payments",
    indexes = [
        Index(name = "idx_processed_payments_payment_id", columnList = "payment_id", unique = true),
    ],
)
@Entity
class ProcessedPaymentEntity(
    id: Long? = null,
    @Column(name = "payment_id", nullable = false, unique = true)
    val paymentId: Long,
) : BaseEntity() {
    init {
        this.id = id
    }
}
