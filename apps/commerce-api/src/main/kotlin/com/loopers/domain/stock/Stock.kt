package com.loopers.domain.stock

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "stocks")
class Stock private constructor(
    productId: Long,
    quantity: Int,
) : BaseEntity() {

    @Column(nullable = false, unique = true)
    var productId: Long = productId
        protected set

    @Column(nullable = false)
    var quantity: Int = quantity
        protected set

    @Version
    var version: Long = 0
        protected set

    fun minusStock(qty: Int) {
        if (qty <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "감소 수량은 0보다 커야 합니다")
        }
        if (quantity < qty) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족 합니다")
        }
        this.quantity -= qty
    }

    fun plusStock(qty: Int) {
        if (qty < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "증가 수량은 0 이상이어야 합니다")
        }
        this.quantity += qty
    }

    override fun guard() {
        if (productId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 ID는 0보다 커야 합니다")
        }
        if (quantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다")
        }
    }

    companion object {
        fun create(
            productId: Long,
            quantity: Int,
        ): Stock {
            return Stock(
                productId = productId,
                quantity = quantity,
            ).apply { guard() }
        }
    }
}
