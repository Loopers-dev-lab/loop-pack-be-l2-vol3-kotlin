package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "product")
class ProductModel(
    name: String,
    price: Long,
    brandId: Long,
    description: String? = null,
    thumbnailImageUrl: String? = null,
    stockQuantity: Int = 0,
    likesCount: Long = 0,
    saleStatus: SaleStatus = SaleStatus.SELLING,
    displayStatus: DisplayStatus = DisplayStatus.VISIBLE,
) : BaseEntity() {

    @Column(nullable = false, length = 200)
    var name: String = name
        protected set

    @Column(nullable = false)
    var price: Long = price
        protected set

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(length = 1000)
    var description: String? = description
        protected set

    @Column(name = "thumbnail_image_url", length = 500)
    var thumbnailImageUrl: String? = thumbnailImageUrl
        protected set

    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = stockQuantity
        protected set

    @Column(name = "likes_count", nullable = false)
    var likesCount: Long = likesCount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 20)
    var saleStatus: SaleStatus = saleStatus
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", nullable = false, length = 20)
    var displayStatus: DisplayStatus = displayStatus
        protected set

    @Version
    @Column(name = "version")
    var version: Long = 0
        protected set

    init {
        validateName(name)
        validatePrice(price)
        validateStockQuantity(stockQuantity)
        validateLikesCount(likesCount)
    }

    // === 재고 관리 (도메인 규칙) ===

    /** 재고를 차감한다. 재고가 부족하면 도메인 예외를 발생시킨다. */
    fun decreaseStock(quantity: Int) {
        require(quantity > 0) { "차감 수량은 1 이상이어야 합니다." }
        if (this.stockQuantity < quantity) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "상품의 재고가 부족합니다. (상품명: $name, 요청 수량: ${quantity}개, 현재 재고: ${stockQuantity}개)",
            )
        }
        this.stockQuantity -= quantity
    }

    /** 재고를 증가시킨다. */
    fun increaseStock(quantity: Int) {
        require(quantity > 0) { "증가 수량은 1 이상이어야 합니다." }
        this.stockQuantity += quantity
    }

    // === 좋아요 수 관리 ===

    fun incrementLikesCount() {
        this.likesCount++
    }

    fun decrementLikesCount() {
        if (this.likesCount > 0) this.likesCount--
    }

    // === 상품 정보 수정 ===

    fun update(
        name: String,
        price: Long,
        description: String?,
        thumbnailImageUrl: String?,
        stockQuantity: Int,
        saleStatus: SaleStatus,
        displayStatus: DisplayStatus,
    ) {
        validateName(name)
        validatePrice(price)
        validateStockQuantity(stockQuantity)
        this.name = name
        this.price = price
        this.description = description
        this.thumbnailImageUrl = thumbnailImageUrl
        this.stockQuantity = stockQuantity
        this.saleStatus = saleStatus
        this.displayStatus = displayStatus
    }

    // === 검증 로직 ===

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.")
        }
        if (name.length > 200) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 200자 이하여야 합니다.")
        }
    }

    private fun validatePrice(price: Long) {
        if (price < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.")
        }
    }

    private fun validateStockQuantity(stockQuantity: Int) {
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
        }
    }

    private fun validateLikesCount(likesCount: Long) {
        if (likesCount < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.")
        }
    }
}
