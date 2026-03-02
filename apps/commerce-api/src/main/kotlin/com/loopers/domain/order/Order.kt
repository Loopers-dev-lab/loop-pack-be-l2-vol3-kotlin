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

    @Comment("총 결제금액")
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

    fun calculateTotalAmount() {
        this.totalAmount = orderItems.fold(Money.ZERO) { acc, item -> acc + item.itemTotalPrice() }
    }

    fun generateOrderNumber() {
        val datePart = LocalDate.now(ZoneId.of("Asia/Seoul")).let { date ->
            String.format("%02d%02d%02d", date.year % 100, date.monthValue, date.dayOfMonth)
        }
        val idPart = String.format("%08d", this.id)
        this.orderNumber = "$datePart$idPart"
    }
}
