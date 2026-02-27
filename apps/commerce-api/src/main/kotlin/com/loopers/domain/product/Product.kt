package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(
    name = "products",
    indexes = [Index(name = "idx_products_brand_id", columnList = "brand_id")],
)
class Product(
    brandId: Long,
    name: String,
    price: BigDecimal,
    stock: Int,
    description: String?,
    imageUrl: String?,
) : BaseEntity() {

    @Column(name = "brand_id", nullable = false)
    val brandId: Long = brandId

    @Column(name = "name", nullable = false, length = 200)
    var name: String = name
        private set

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    var price: BigDecimal = price
        private set

    @Column(name = "stock", nullable = false)
    var stock: Int = stock
        private set

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = description
        private set

    @Column(name = "image_url", length = 500)
    var imageUrl: String? = imageUrl
        private set

    init {
        validateName(name)
        validatePrice(price)
        validateStock(stock)
    }

    fun update(
        name: String,
        price: BigDecimal,
        stock: Int,
        description: String?,
        imageUrl: String?,
    ) {
        validateName(name)
        validatePrice(price)
        validateStock(stock)
        this.name = name
        this.price = price
        this.stock = stock
        this.description = description
        this.imageUrl = imageUrl
    }

    fun hasEnoughStock(quantity: Int): Boolean = stock >= quantity

    fun decreaseStock(quantity: Int) {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.")
        }
        if (!hasEnoughStock(quantity)) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다. 현재 재고: $stock, 요청 수량: $quantity")
        }
        this.stock -= quantity
    }

    fun reserve(quantity: Int): Boolean {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "예약 수량은 1 이상이어야 합니다.")
        }
        if (!hasEnoughStock(quantity)) return false
        this.stock -= quantity
        return true
    }

    fun isDeleted(): Boolean = deletedAt != null

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 빈 값일 수 없습니다.")
        }
        if (name.length > MAX_NAME_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 ${MAX_NAME_LENGTH}자 이하여야 합니다.")
        }
    }

    private fun validatePrice(price: BigDecimal) {
        if (price < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.")
        }
    }

    private fun validateStock(stock: Int) {
        if (stock < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.")
        }
    }

    companion object {
        private const val MAX_NAME_LENGTH = 200
    }
}
