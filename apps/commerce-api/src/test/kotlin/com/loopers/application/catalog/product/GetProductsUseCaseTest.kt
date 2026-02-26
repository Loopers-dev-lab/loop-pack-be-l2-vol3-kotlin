package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GetProductsUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: GetProductsUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = GetProductsUseCase(productRepository)
    }

    private fun createProduct(
        brandId: Long = 1L,
        name: String,
        price: BigDecimal,
        stock: Int = 10,
    ): Product {
        return productRepository.save(
            Product(refBrandId = BrandId(brandId), name = name, price = Money(price), stock = Stock(stock)),
        )
    }

    @Nested
    @DisplayName("대고객 상품 목록 조회 시")
    inner class Execute {

        @Test
        @DisplayName("삭제된 상품과 HIDDEN 상품은 제외된다")
        fun getProducts_excludesDeletedAndHidden() {
            // arrange
            createProduct(name = "상품1", price = BigDecimal("10000"))
            val hidden = createProduct(name = "상품2", price = BigDecimal("20000"))
            hidden.update(null, null, null, Product.ProductStatus.HIDDEN)
            productRepository.save(hidden)
            val deleted = createProduct(name = "상품3", price = BigDecimal("30000"))
            deleted.delete()
            productRepository.save(deleted)

            // act
            val result = useCase.execute(null, "LATEST", 0, 10)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("brandId로 필터링한다")
        fun getProducts_filtersByBrandId() {
            // arrange
            createProduct(brandId = 1L, name = "나이키 상품", price = BigDecimal("10000"))
            createProduct(brandId = 2L, name = "아디다스 상품", price = BigDecimal("20000"))

            // act
            val result = useCase.execute(1L, "LATEST", 0, 10)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("나이키 상품")
        }

        @Test
        @DisplayName("가격 오름차순으로 정렬한다")
        fun getProducts_sortsByPriceAsc() {
            // arrange
            createProduct(name = "비싼상품", price = BigDecimal("50000"))
            createProduct(name = "싼상품", price = BigDecimal("10000"))

            // act
            val result = useCase.execute(null, "PRICE_ASC", 0, 10)

            // assert
            assertThat(result.content[0].name).isEqualTo("싼상품")
            assertThat(result.content[1].name).isEqualTo("비싼상품")
        }

        @Test
        @DisplayName("좋아요 내림차순으로 정렬한다")
        fun getProducts_sortsByLikesDesc() {
            // arrange
            val product1 = createProduct(name = "인기없는상품", price = BigDecimal("10000"))
            val product2 = createProduct(name = "인기상품", price = BigDecimal("20000"))
            product2.increaseLikeCount()
            product2.increaseLikeCount()
            productRepository.save(product2)
            product1.increaseLikeCount()
            productRepository.save(product1)

            // act
            val result = useCase.execute(null, "LIKES_DESC", 0, 10)

            // assert
            assertThat(result.content[0].name).isEqualTo("인기상품")
            assertThat(result.content[1].name).isEqualTo("인기없는상품")
        }
    }
}
