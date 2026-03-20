package com.loopers.application.product

import com.loopers.domain.product.DisplayStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.SaleStatus
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductCommandFacade")
class ProductCommandFacadeTest {
    private val productService: ProductService = mockk()
    private val productCacheStore: ProductCacheStore = mockk(relaxed = true)
    private val productCommandFacade = ProductCommandFacade(productService, productCacheStore)

    private fun createProduct(productId: Long = 1L): ProductModel {
        val product = mockk<ProductModel>()
        every { product.id } returns productId
        every { product.name } returns "감성 티셔츠"
        every { product.price } returns 25000L
        every { product.brandId } returns 10L
        every { product.stockQuantity } returns 10
        every { product.saleStatus } returns SaleStatus.SELLING
        every { product.displayStatus } returns DisplayStatus.VISIBLE
        return product
    }

    @DisplayName("create")
    @Nested
    inner class Create {
        @DisplayName("상품 생성 후 목록 캐시를 무효화한다")
        @Test
        fun evictsListCacheAfterCreate() {
            // arrange
            val product = createProduct()
            every {
                productService.create(
                    name = "감성 티셔츠",
                    price = 25000L,
                    brandId = 10L,
                    description = "설명",
                    thumbnailImageUrl = "https://example.com/image.png",
                    stockQuantity = 10,
                )
            } returns product

            // act
            val result = productCommandFacade.create(
                name = "감성 티셔츠",
                price = 25000L,
                brandId = 10L,
                description = "설명",
                thumbnailImageUrl = "https://example.com/image.png",
                stockQuantity = 10,
            )

            // assert
            assertThat(result).isEqualTo(product)
            verify(exactly = 1) { productCacheStore.evictProductList() }
        }
    }

    @DisplayName("update")
    @Nested
    inner class Update {
        @DisplayName("상품 수정 후 상세/목록 캐시를 함께 무효화한다")
        @Test
        fun evictsDetailAndListCacheAfterUpdate() {
            // arrange
            val product = createProduct(productId = 3L)
            every {
                productService.update(
                    id = 3L,
                    name = "수정된 티셔츠",
                    price = 30000L,
                    description = "새 설명",
                    thumbnailImageUrl = null,
                    stockQuantity = 30,
                    saleStatus = SaleStatus.SELLING,
                    displayStatus = DisplayStatus.VISIBLE,
                )
            } returns product

            // act
            productCommandFacade.update(
                id = 3L,
                name = "수정된 티셔츠",
                price = 30000L,
                description = "새 설명",
                thumbnailImageUrl = null,
                stockQuantity = 30,
                saleStatus = SaleStatus.SELLING,
                displayStatus = DisplayStatus.VISIBLE,
            )

            // assert
            verify(exactly = 1) { productCacheStore.evictProductDetail(3L) }
            verify(exactly = 1) { productCacheStore.evictProductList() }
        }
    }

    @DisplayName("delete")
    @Nested
    inner class Delete {
        @DisplayName("상품 삭제 후 상세/목록 캐시를 함께 무효화한다")
        @Test
        fun evictsDetailAndListCacheAfterDelete() {
            // arrange
            every { productService.delete(5L) } just runs

            // act
            productCommandFacade.delete(5L)

            // assert
            verify(exactly = 1) { productService.delete(5L) }
            verify(exactly = 1) { productCacheStore.evictProductDetail(5L) }
            verify(exactly = 1) { productCacheStore.evictProductList() }
        }
    }
}
