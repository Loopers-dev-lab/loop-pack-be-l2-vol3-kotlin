package com.loopers.domain.product

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import java.time.ZonedDateTime

data class ProductModel(
    val id: Long = 0,
    val brandId: Long,
    val name: String,
    val description: String,
    val price: Long,
    val stockQuantity: Int,
    val likeCount: Int = 0,
    val imageUrl: String,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    init {
        require(price >= 0) { "상품 가격은 0 이상이어야 합니다." }
    }

    fun update(
        name: String,
        description: String,
        price: Long,
        stockQuantity: Int,
        imageUrl: String,
    ): ProductModel = copy(
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        imageUrl = imageUrl,
    )

    fun deductStock(quantity: Int): ProductModel {
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.")
        }
        if (stockQuantity < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        return copy(stockQuantity = stockQuantity - quantity)
    }

    fun delete(): ProductModel =
        copy(status = ProductStatus.DELETED, deletedAt = deletedAt ?: ZonedDateTime.now())

    fun isDeleted(): Boolean = status == ProductStatus.DELETED
}
