package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductStatus
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProductMapperTest {

    @Test
    fun `ProductEntity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = ProductEntity(
            id = null,
            brandId = 1L,
            name = "테스트상품",
            description = "설명",
            price = 10000L,
            stock = 100,
            thumbnailUrl = null,
            status = ProductStatus.ACTIVE,
            likeCount = 0,
            deletedAt = null,
        )

        assertThatThrownBy { ProductMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ProductEntity.id가 null입니다")
    }

    @Test
    fun `ProductImageEntity id가 null이면 IllegalArgumentException이 발생한다`() {
        val productEntity = ProductEntity(
            id = 1L,
            brandId = 1L,
            name = "테스트상품",
            description = "설명",
            price = 10000L,
            stock = 100,
            thumbnailUrl = null,
            status = ProductStatus.ACTIVE,
            likeCount = 0,
            deletedAt = null,
        )
        val imageEntity = ProductImageEntity(
            id = null,
            product = productEntity,
            imageUrl = "https://example.com/image.png",
            displayOrder = 1,
        )
        productEntity.images.add(imageEntity)

        assertThatThrownBy { ProductMapper.toDomain(productEntity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ProductImageEntity.id가 null입니다")
    }
}
