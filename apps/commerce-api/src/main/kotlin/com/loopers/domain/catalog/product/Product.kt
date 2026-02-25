package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

/**
 * 상품 도메인 모델 (JPA 비의존)
 *
 * @property id 식별자 (영속화 전에는 0L)
 * @property brandId 브랜드 ID (참조 키)
 * @property name 상품명
 * @property description 상품 설명
 * @property price 가격 (0 이상)
 * @property stock 재고 (0 이상)
 * @property likeCount 좋아요 수 (비정규화, 0 이상)
 */
class Product(
    brandId: Long,
    name: String,
    description: String,
    price: Int,
    stock: Int,
    likeCount: Int = 0,
    val id: Long = 0L,
) {
    var brandId: Long = brandId
        private set

    var name: String = name
        private set

    var description: String = description
        private set

    var price: Int = price
        private set

    var stock: Int = stock
        private set

    var likeCount: Int = likeCount
        private set

    init {
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.")
        if (price < 0) throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.")
        if (stock < 0) throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.")
    }

    fun update(name: String, description: String, price: Int, stock: Int) {
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.")
        if (price < 0) throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.")
        if (stock < 0) throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.")
        this.name = name
        this.description = description
        this.price = price
        this.stock = stock
    }

    fun validateStock(quantity: Int) {
        if (quantity <= 0) throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        if (stock < quantity) throw CoreException(ErrorType.BAD_REQUEST, "[$name] 재고가 부족합니다. 현재 재고: $stock, 요청 수량: $quantity")
    }

    fun decrementStock(quantity: Int) {
        if (quantity <= 0) throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        if (stock - quantity < 0) throw CoreException(ErrorType.BAD_REQUEST, "[$name] 재고가 부족합니다. 현재 재고: $stock, 요청 수량: $quantity")
        this.stock -= quantity
    }

    fun incrementLike() {
        this.likeCount++
    }

    fun decrementLike() {
        if (likeCount <= 0) throw CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 미만이 될 수 없습니다.")
        this.likeCount--
    }
}
