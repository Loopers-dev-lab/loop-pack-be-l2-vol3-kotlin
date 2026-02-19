package com.loopers.domain.catalog.product

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
class Product(
    refBrandId: Long,
    name: String,
    price: BigDecimal,
    stock: Int,
) : BaseEntity() {

    @Column(name = "ref_brand_id", nullable = false)
    var refBrandId: Long = refBrandId
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    var price: BigDecimal = price
        protected set

    @Column(name = "stock", nullable = false)
    var stock: Int = stock
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ProductStatus = if (stock > 0) ProductStatus.ON_SALE else ProductStatus.SOLD_OUT
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    enum class ProductStatus {
        ON_SALE,
        SOLD_OUT,
        HIDDEN,
    }

    init {
        guard()
    }

    override fun guard() {
        Price(price)
        Stock(stock)
    }

    fun update(name: String?, price: BigDecimal?, stock: Int?, status: ProductStatus?) {
        name?.let { this.name = it }
        price?.let { this.price = it }
        stock?.let { this.stock = it }
        guard()

        val newStatus = when {
            status == ProductStatus.HIDDEN -> ProductStatus.HIDDEN
            this.status == ProductStatus.HIDDEN && status == null -> ProductStatus.HIDDEN
            else -> if (this.stock > 0) ProductStatus.ON_SALE else ProductStatus.SOLD_OUT
        }

        this.status = newStatus
    }

    fun decreaseStock(quantity: Int) {
        val decreased = Stock(stock).decrease(quantity)
        this.stock = decreased.value
        adjustStatusByStock()
    }

    fun increaseStock(quantity: Int) {
        val increased = Stock(stock).increase(quantity)
        this.stock = increased.value
        adjustStatusByStock()
    }

    fun increaseLikeCount() {
        this.likeCount++
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }

    fun isDeleted(): Boolean = deletedAt != null

    private fun adjustStatusByStock() {
        if (status == ProductStatus.HIDDEN) return
        status = if (stock == 0) ProductStatus.SOLD_OUT else ProductStatus.ON_SALE
    }
}
