package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.config.jpa.MoneyConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.ZoneId

@Entity
@Table(name = "orders")
@Comment("주문")
class Order(
    userId: Long,
) : BaseEntity() {

    @Comment("주문자")
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Comment("주문번호 (yyMMdd + id 8자리)")
    @Column(name = "order_number", nullable = false, unique = true)
    var orderNumber: String = ""
        protected set

    @Comment("사용된 발급 쿠폰 ID")
    @Column(name = "coupon_issue_id")
    var couponIssueId: Long? = null
        protected set

    @Comment("쿠폰 적용 전 총 금액")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "original_total_amount", nullable = false)
    var originalTotalAmount: Money = Money.ZERO
        protected set

    @Comment("쿠폰 할인 금액")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "coupon_discount_amount", nullable = false)
    var couponDiscountAmount: Money = Money.ZERO
        protected set

    @Comment("최종 결제 금액")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "total_amount", nullable = false)
    var totalAmount: Money = Money.ZERO
        protected set

    @Comment("주문 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    var orderStatus: OrderStatus = OrderStatus.ORDERED
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderItems: MutableList<OrderItem> = mutableListOf()

    init {
        if (userId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 회원입니다.")
        }
    }

    fun addItem(orderItem: OrderItem) {
        orderItems.add(orderItem)
        orderItem.order = this
    }

    /**
     * 쿠폰 적용 정보를 설정한다.
     * calculateTotalAmount() 호출 전에 실행되어야 한다.
     */
    fun applyCoupon(couponIssueId: Long, discountAmount: Money) {
        this.couponIssueId = couponIssueId
        this.couponDiscountAmount = discountAmount
    }

    /**
     * 주문 총액을 계산한다.
     * - originalTotalAmount: 전체 상품 금액 합계 (쿠폰 적용 전)
     * - totalAmount: 쿠폰 할인 적용 후 최종 결제 금액
     */
    fun calculateTotalAmount() {
        this.originalTotalAmount = orderItems.fold(Money.ZERO) { acc, item -> acc + item.itemTotalPrice() }
        this.totalAmount = this.originalTotalAmount - this.couponDiscountAmount
    }

    fun generateOrderNumber() {
        val datePart = LocalDate.now(ZoneId.of("Asia/Seoul")).let { date ->
            String.format("%02d%02d%02d", date.year % 100, date.monthValue, date.dayOfMonth)
        }
        val idPart = String.format("%08d", this.id)
        this.orderNumber = "$datePart$idPart"
    }
}
