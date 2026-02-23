package com.loopers.domain.catalog.product.entity

import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.Money
import java.time.ZonedDateTime

class Product(
    val id: Long = 0,
    val refBrandId: Long,
    name: String,
    price: Money,
    stock: Int,
    status: ProductStatus? = null,
    likeCount: Int = 0,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val updatedAt: ZonedDateTime = ZonedDateTime.now(),
    deletedAt: ZonedDateTime? = null,
) {

    var name: String = name
        private set

    var price: Money = price
        private set

    var stock: Int = stock
        private set

    var status: ProductStatus = status ?: if (stock > 0) ProductStatus.ON_SALE else ProductStatus.SOLD_OUT
        private set

    var likeCount: Int = likeCount
        private set

    var deletedAt: ZonedDateTime? = deletedAt
        private set

    enum class ProductStatus {
        ON_SALE,
        SOLD_OUT,
        HIDDEN,
    }

    init {
        guard()
    }

    private fun guard() {
        Stock(stock)
    }

    fun update(name: String?, price: Money?, stock: Int?, status: ProductStatus?) {
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

    fun isActive(): Boolean = !isDeleted() && status != ProductStatus.HIDDEN

    fun isAvailableForOrder(): Boolean = !isDeleted() && status == ProductStatus.ON_SALE

    fun delete() {
        deletedAt ?: run { deletedAt = ZonedDateTime.now() }
    }

    fun restore() {
        deletedAt?.let { deletedAt = null }
    }

    private fun adjustStatusByStock() {
        if (status == ProductStatus.HIDDEN) return
        status = if (stock == 0) ProductStatus.SOLD_OUT else ProductStatus.ON_SALE
    }
}
