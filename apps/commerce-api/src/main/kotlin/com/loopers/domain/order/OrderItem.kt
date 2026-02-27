package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "order_items")
@Comment("주문 상품")
class OrderItem(
    productId: Long,
    quantity: Int,
    productSnapshot: ProductSnapshot,
    priceSnapshot: PriceSnapshot,
) : BaseEntity() {

    @Comment("원본 상품 참조")
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Comment("주문 수량")
    @Column(nullable = false)
    var quantity: Int = quantity
        protected set

    @Comment("품목별 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    var itemStatus: OrderItemStatus = OrderItemStatus.ORDERED
        protected set

    @Embedded
    var productSnapshot: ProductSnapshot = productSnapshot
        protected set

    @Embedded
    var priceSnapshot: PriceSnapshot = priceSnapshot
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: Order? = null
        internal set

    init {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        }
    }

    fun itemTotalPrice(): Money = priceSnapshot.finalPrice * quantity
}
