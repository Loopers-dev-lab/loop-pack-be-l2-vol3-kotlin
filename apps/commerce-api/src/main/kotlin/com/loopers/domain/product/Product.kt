package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.Brand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
class Product private constructor(
    brand: Brand,
    name: String,
    price: BigDecimal,
    stock: Int,
    status: ProductStatus,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    var brand: Brand = brand
        protected set

    @Column(nullable = false, length = 200)
    var name: String = name
        protected set

    @Column(nullable = false, precision = 19, scale = 2)
    var price: BigDecimal = price
        protected set

    @Column(nullable = false)
    var stock: Int = stock
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ProductStatus = status
        protected set

    @Column(nullable = false)
    var likeCount: Int = 0
        protected set

    fun isDeleted(): Boolean = deletedAt != null

    fun isAvailable(): Boolean = !isDeleted() && status != ProductStatus.INACTIVE

    fun updateStock(newStock: Int) {
        if (newStock < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다")
        }
        this.stock = newStock
    }

    override fun guard() {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다")
        }
        if (name.length > 200) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 200자 이하여야 합니다")
        }
        if (price < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다")
        }
        if (stock < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다")
        }
    }

    fun updateInfo(newName: String, newPrice: BigDecimal) {
        this.name = newName
        this.price = newPrice
        guard()
    }

    fun changeStatus(newStatus: ProductStatus) {
        this.status = newStatus
        guard()
    }

    fun incrementLikeCount() {
        this.likeCount++
    }

    fun decrementLikeCount() {
        if (likeCount <= 0) {
            this.likeCount = 0
            return
        }
        this.likeCount--
    }

    companion object {
        fun create(
            brand: Brand,
            name: String,
            price: BigDecimal,
            stock: Int,
            status: ProductStatus = ProductStatus.ACTIVE,
        ): Product {
            return Product(
                brand = brand,
                name = name,
                price = price,
                stock = stock,
                status = status,
            ).apply { guard() }
        }
    }
}
