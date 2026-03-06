package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@DisplayName("ProductFacade")
class ProductFacadeTest {

    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val productFacade = ProductFacade(productService, brandService)

    companion object {
        private const val PRODUCT_ID = 1L
        private const val BRAND_ID = 10L
        private const val BRAND_NAME = "루프팩"
        private const val PRODUCT_NAME = "감성 티셔츠"
        private const val PRODUCT_PRICE = 25000L
    }

    private fun createProduct(
        id: Long = PRODUCT_ID,
        name: String = PRODUCT_NAME,
        price: Long = PRODUCT_PRICE,
        brandId: Long = BRAND_ID,
        likesCount: Long = 5L,
    ): ProductModel {
        val product = ProductModel(
            name = name,
            price = price,
            brandId = brandId,
            likesCount = likesCount,
        )
        return spyk(product) {
            every { this@spyk.id } returns id
            every { createdAt } returns ZonedDateTime.now()
        }
    }

    private fun createBrand(
        id: Long = BRAND_ID,
        name: String = BRAND_NAME,
    ): BrandModel {
        val brand = BrandModel(name = name)
        return spyk(brand) {
            every { this@spyk.id } returns id
        }
    }

    @DisplayName("getProductDetail")
    @Nested
    inner class GetProductDetail {
        @DisplayName("상품과 브랜드 정보를 조합하여 ProductInfo를 반환한다")
        @Test
        fun returnsProductInfoWithBrandName_whenProductExists() {
            // arrange
            val product = createProduct()
            val brand = createBrand()
            every { productService.findById(PRODUCT_ID) } returns product
            every { brandService.findById(BRAND_ID) } returns brand

            // act
            val result = productFacade.getProductDetail(PRODUCT_ID)

            // assert
            assertThat(result.name).isEqualTo(PRODUCT_NAME)
            assertThat(result.price).isEqualTo(PRODUCT_PRICE)
            assertThat(result.brandName).isEqualTo(BRAND_NAME)
            assertThat(result.likesCount).isEqualTo(5L)
            verify(exactly = 1) { productService.findById(PRODUCT_ID) }
            verify(exactly = 1) { brandService.findById(BRAND_ID) }
        }
    }

    @DisplayName("getProductList")
    @Nested
    inner class GetProductList {
        @DisplayName("상품 목록 조회 시 브랜드를 일괄 조회하여 N+1을 방지한다")
        @Test
        fun returnProductInfoList_withBatchedBrandLookup() {
            // arrange
            val product1 = createProduct(id = 1L, name = "상품A", brandId = 1L)
            val product2 = createProduct(id = 2L, name = "상품B", brandId = 2L)
            val brand1 = createBrand(id = 1L, name = "브랜드A")
            val brand2 = createBrand(id = 2L, name = "브랜드B")
            val pageable = PageRequest.of(0, 10)
            val productPage = PageImpl(listOf(product1, product2), pageable, 2)

            every { productService.findAll(null, pageable) } returns productPage
            every { brandService.findAllByIds(listOf(1L, 2L)) } returns listOf(brand1, brand2)

            // act
            val result = productFacade.getProductList(null, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("상품A")
            assertThat(result.content[0].brandName).isEqualTo("브랜드A")
            assertThat(result.content[1].name).isEqualTo("상품B")
            assertThat(result.content[1].brandName).isEqualTo("브랜드B")
            verify(exactly = 1) { productService.findAll(null, pageable) }
            verify(exactly = 1) { brandService.findAllByIds(any()) }
        }

        @DisplayName("브랜드별 필터링 조회 시에도 브랜드를 일괄 조회한다")
        @Test
        fun returnsFilteredProductInfoList_whenBrandIdProvided() {
            // arrange
            val product1 = createProduct(id = 1L, name = "브랜드1 상품A", brandId = BRAND_ID)
            val product2 = createProduct(id = 2L, name = "브랜드1 상품B", brandId = BRAND_ID)
            val brand = createBrand()
            val pageable = PageRequest.of(0, 10)
            val productPage = PageImpl(listOf(product1, product2), pageable, 2)

            every { productService.findAll(BRAND_ID, pageable) } returns productPage
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)

            // act
            val result = productFacade.getProductList(BRAND_ID, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content).allSatisfy { assertThat(it.brandName).isEqualTo(BRAND_NAME) }
            verify(exactly = 1) { productService.findAll(BRAND_ID, pageable) }
            verify(exactly = 1) { brandService.findAllByIds(listOf(BRAND_ID)) }
        }
    }
}
