package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItemEntity(
    @Column(name = "ref_order_id", nullable = false)
    var refOrderId: Long,
    @Column(name = "ref_product_id", nullable = false)
    var refProductId: Long,
    @Column(name = "product_name", nullable = false)
    var productName: String,
    @Column(name = "product_price", nullable = false, precision = 10, scale = 2)
    var productPrice: BigDecimal,
    @Column(name = "quantity", nullable = false)
    var quantity: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderItem.ItemStatus,
) : BaseEntity() {

    companion object {
        fun fromDomain(orderItem: OrderItem): OrderItemEntity {
            return OrderItemEntity(
                refOrderId = orderItem.refOrderId.value,
                refProductId = orderItem.refProductId.value,
                productName = orderItem.productName,
                productPrice = orderItem.productPrice.value,
                quantity = orderItem.quantity.value,
                status = orderItem.status,
            ).withBaseFields(
                id = orderItem.id,
            )
        }
    }

    fun toDomain(): OrderItem = OrderItem.fromPersistence(
        id = id,
        refOrderId = OrderId(refOrderId),
        refProductId = ProductId(refProductId),
        productName = productName,
        productPrice = Money(productPrice),
        quantity = Quantity(quantity),
        status = status,
    )
}
