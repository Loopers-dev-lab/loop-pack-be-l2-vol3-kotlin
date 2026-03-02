package com.loopers.domain.catalog.product.model

import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import java.time.ZonedDateTime

class Product(
    val id: ProductId = ProductId(0),
    val refBrandId: BrandId,
    name: String,
    price: Money,
    stock: Stock,
    status: ProductStatus? = null,
    likeCount: Int = 0,
    deletedAt: ZonedDateTime? = null,
) {

    var name: String = name
        private set

    var price: Money = price
        private set

    var stock: Stock = stock
        private set

    var status: ProductStatus = status ?: if (stock.value > 0) ProductStatus.ON_SALE else ProductStatus.SOLD_OUT
        private set

    var likeCount: Int = likeCount
        private set

    var deletedAt: ZonedDateTime? = deletedAt
        private set

    init {
        require(likeCount >= 0) { "likeCount는 0 이상이어야 합니다." }
    }

    enum class ProductStatus {
        ON_SALE,
        SOLD_OUT,
        HIDDEN,
    }

    fun update(name: String?, price: Money?, stock: Int?, status: ProductStatus?) {
        name?.let { this.name = it }
        price?.let { this.price = it }
        stock?.let { this.stock = Stock(it) }

        val newStatus = when {
            status == ProductStatus.HIDDEN -> ProductStatus.HIDDEN
            this.status == ProductStatus.HIDDEN && status == null -> ProductStatus.HIDDEN
            else -> if (this.stock.value > 0) ProductStatus.ON_SALE else ProductStatus.SOLD_OUT
        }

        this.status = newStatus
    }

    fun decreaseStock(quantity: Quantity) {
        this.stock = stock.decrease(quantity)
        adjustStatusByStock()
    }

    fun increaseStock(quantity: Quantity) {
        this.stock = stock.increase(quantity)
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

    fun isActive(): Boolean = status != ProductStatus.HIDDEN

    fun isAvailableForOrder(): Boolean = status == ProductStatus.ON_SALE

    fun delete() {
        deletedAt ?: run { deletedAt = ZonedDateTime.now() }
    }

    fun restore() {
        deletedAt?.let { deletedAt = null }
    }

    private fun adjustStatusByStock() {
        if (status == ProductStatus.HIDDEN) return
        status = if (stock.value == 0) ProductStatus.SOLD_OUT else ProductStatus.ON_SALE
    }
}
