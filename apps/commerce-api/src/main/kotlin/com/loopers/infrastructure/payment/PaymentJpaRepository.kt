package com.loopers.infrastructure.payment

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<PaymentEntity, Long> {
    fun findTopByOrderIdOrderByIdDesc(orderId: Long): PaymentEntity?

    fun findTopByOrderIdAndMemberIdOrderByIdDesc(orderId: Long, memberId: Long): PaymentEntity?

    fun findByPgTransactionKey(transactionKey: String): PaymentEntity?
}
