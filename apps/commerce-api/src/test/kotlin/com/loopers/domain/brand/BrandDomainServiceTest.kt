package com.loopers.domain.brand

import com.loopers.domain.product.Product
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.math.BigDecimal

class BrandDomainServiceTest {

    private val brandDomainService = BrandDomainService()

    @DisplayName("브랜드와 상품을 함께 삭제할 때,")
    @Nested
    inner class DeleteBrandWithProducts {

        @DisplayName("브랜드와 해당 상품이 존재하면, 브랜드와 상품 모두 soft delete 된다.")
        @Test
        fun deletesBrandAndProducts_whenBrandExists() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val product1 = Product(brandId = 1L, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100, description = null, imageUrl = null)
            val product2 = Product(brandId = 1L, name = "에어포스 1", price = BigDecimal("139000"), stock = 50, description = null, imageUrl = null)

            // act
            brandDomainService.deleteBrand(brand, listOf(product1, product2))

            // assert
            assertAll(
                { assertThat(brand.isDeleted()).isTrue() },
                { assertThat(product1.isDeleted()).isTrue() },
                { assertThat(product2.isDeleted()).isTrue() },
            )
        }

        @DisplayName("브랜드에 상품이 없으면, 브랜드만 soft delete 된다.")
        @Test
        fun deletesBrandOnly_whenNoProductsExist() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            // act
            brandDomainService.deleteBrand(brand, emptyList())

            // assert
            assertThat(brand.isDeleted()).isTrue()
        }
    }
}
