package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.config.jpa.MoneyConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "products")
@Comment("상품")
class Product(
    brandId: Long,
    name: String,
    description: String? = null,
    price: Money,
    stockQuantity: Int,
    displayYn: Boolean = true,
    imageUrl: String? = null,
) : BaseEntity() {

    @Comment("소속 브랜드")
    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Comment("상품명")
    @Column(nullable = false)
    var name: String = name
        protected set

    @Comment("상품 설명")
    @Column
    var description: String? = description
        protected set

    @Comment("판매가")
    @Convert(converter = MoneyConverter::class)
    @Column(nullable = false)
    var price: Money = price
        protected set

    @Comment("재고 수량")
    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = stockQuantity
        protected set

    @Comment("좋아요 수")
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    @Comment("판매 상태 (ACTIVE/INACTIVE/DELETED)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ProductStatus = ProductStatus.ACTIVE
        protected set

    @Comment("전시 여부")
    @Column(name = "display_yn", nullable = false)
    var displayYn: Boolean = displayYn
        protected set

    @Comment("상품 이미지")
    @Column(name = "image_url")
    var imageUrl: String? = imageUrl
        protected set

    init {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.")
        }
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
        }
    }

    fun update(
        name: String,
        description: String?,
        price: Money,
        stockQuantity: Int,
        status: ProductStatus,
        displayYn: Boolean,
        imageUrl: String?,
    ) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.")
        }
        if (stockQuantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
        }
        this.name = name
        this.description = description
        this.price = price
        this.stockQuantity = stockQuantity
        this.status = status
        this.displayYn = displayYn
        this.imageUrl = imageUrl
    }

    fun decreaseStock(quantity: Int) {
        if (this.stockQuantity < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품의 재고가 부족합니다.")
        }
        this.stockQuantity -= quantity
    }

    fun increaseStock(quantity: Int) {
        this.stockQuantity += quantity
    }

    fun increaseLikeCount() {
        this.likeCount++
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }

    fun isOrderable(): Boolean {
        return status == ProductStatus.ACTIVE && displayYn && stockQuantity > 0
    }

    fun softDelete() {
        this.status = ProductStatus.DELETED
        delete()
    }
}
