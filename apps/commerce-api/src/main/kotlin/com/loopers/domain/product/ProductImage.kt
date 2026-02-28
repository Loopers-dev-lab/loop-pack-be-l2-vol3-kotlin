package com.loopers.domain.product

class ProductImage private constructor(
    val persistenceId: Long?,
    val imageUrl: String,
    val displayOrder: Int,
) {

    companion object {
        fun create(imageUrl: String, displayOrder: Int): ProductImage {
            require(imageUrl.isNotBlank()) { "이미지 URL은 빈 문자열일 수 없습니다." }
            require(displayOrder >= 0) { "표시 순서는 0 이상이어야 합니다." }
            return ProductImage(
                persistenceId = null,
                imageUrl = imageUrl,
                displayOrder = displayOrder,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            imageUrl: String,
            displayOrder: Int,
        ): ProductImage {
            return ProductImage(
                persistenceId = persistenceId,
                imageUrl = imageUrl,
                displayOrder = displayOrder,
            )
        }
    }
}
