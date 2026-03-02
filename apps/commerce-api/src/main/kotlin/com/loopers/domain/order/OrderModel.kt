package com.loopers.domain.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "tb_order")
class OrderModel(
    userId: Long,
    totalAmount: TotalAmount,
    status: OrderStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(nullable = false)
    var userId: Long = userId
        protected set

    @Column(nullable = false)
    var totalAmount: TotalAmount = totalAmount
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = status
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {

        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {

        val now = ZonedDateTime.now()
        updatedAt = now
    }

    // 결제 완료 처리
    fun pay() {
        verifyStatus(OrderStatus.PENDING_PAYMENT)
        this.status = OrderStatus.PAID
    }

    // 상품 준비 시작
    fun prepare() {
        verifyStatus(OrderStatus.PAID)
        this.status = OrderStatus.PREPARING
    }

    // 배송 시작
    fun ship() {
        verifyStatus(OrderStatus.PREPARING)
        this.status = OrderStatus.SHIPPED
    }

    // 배송 완료
    fun delivered() {
        verifyStatus(OrderStatus.SHIPPED)
        this.status = OrderStatus.DELIVERED
    }

    // 구매 확정
    fun completed() {
        verifyStatus(OrderStatus.DELIVERED)
        this.status = OrderStatus.COMPLETED
    }

    // 취소
    fun cancel() {
        check(
            status == OrderStatus.PAID ||
            status == OrderStatus.PREPARING ||
            status == OrderStatus.DELIVERED
        ) {
            throw IllegalStateException("${this.status} 상태에서는 취소 할 수 없습니다. 반품을 이용해주세요.")
        }
        this.status = OrderStatus.CANCELLED
    }

    // 교환 신청
    fun requestExchange() {
        check(status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw IllegalStateException("${this.status} 상태에서는 교환 신청 할 수 없습니다.")
        }
        this.status = OrderStatus.EXCHANGE_REQUESTED
    }

    // 교환 중
    fun exchange() {
        verifyStatus(OrderStatus.EXCHANGE_REQUESTED)
        this.status = OrderStatus.EXCHANGING
    }

    // 교환 완료
    fun completeExchange() {
        verifyStatus(OrderStatus.EXCHANGING)
        this.status = OrderStatus.EXCHANGED
    }

    // 환불 신청
    fun requestReturn() {
        check(status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw IllegalStateException("${this.status} 상태에서는 환불 신청 할 수 없습니다.")
        }
    }

    // 환불 중
    fun returns() {
        verifyStatus(OrderStatus.RETURN_REQUESTED)
        this.status = OrderStatus.RETURNING
    }

    // 환불 완료
    fun completeReturns() {
        verifyStatus(OrderStatus.RETURNING)
        this.status = OrderStatus.RETURNED
    }

    private fun verifyStatus(targetStatus: OrderStatus) {
        if (this.status != targetStatus) {
            throw IllegalStateException("${this.status} 상태에서는 해당 작업을 수행할 수 없습니다.")
        }
    }
}
