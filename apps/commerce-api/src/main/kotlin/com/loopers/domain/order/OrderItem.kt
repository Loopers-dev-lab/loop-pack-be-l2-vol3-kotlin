package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    order: Order,
    productId: Long,
    productName: String,
    brandName: String,
    quantity: Int,
    unitPrice: BigDecimal,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order = order

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String = productName

    @Column(name = "brand_name", nullable = false, length = 100)
    val brandName: String = brandName

    @Column(name = "quantity", nullable = false)
    val quantity: Int = quantity

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    val unitPrice: BigDecimal = unitPrice

    init {
        require(quantity > 0) { throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.") }
        require(unitPrice >= BigDecimal.ZERO) { throw CoreException(ErrorType.BAD_REQUEST, "단가는 0 이상이어야 합니다.") }
    }

    fun getSubtotal(): BigDecimal = unitPrice.multiply(BigDecimal(quantity))
}
