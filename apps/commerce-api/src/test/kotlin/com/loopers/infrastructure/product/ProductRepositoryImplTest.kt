package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class ProductRepositoryImplTest {

    private val productJpaRepository: ProductJpaRepository = mockk()
    private val productRepositoryImpl = ProductRepositoryImpl(productJpaRepository)

    @DisplayName("상품을 저장할 때,")
    @Nested
    inner class Save {
        @DisplayName("JpaRepository에 위임하여 저장하고 결과를 반환한다.")
        @Test
        fun delegatesToJpaRepository() {
            // arrange
            val product = createProduct(name = "티셔츠", price = 25000, brandId = 1L)
            every { productJpaRepository.save(product) } returns product

            // act
            val result = productRepositoryImpl.save(product)

            // assert
            assertThat(result).isEqualTo(product)
            verify(exactly = 1) { productJpaRepository.save(product) }
        }
    }

    @DisplayName("상품을 ID로 조회할 때,")
    @Nested
    inner class FindById {
        @DisplayName("삭제되지 않은 상품이 존재하면 반환한다.")
        @Test
        fun returnsProduct_whenExists() {
            // arrange
            val product = createProduct(name = "후드", price = 59000, brandId = 1L)
            every { productJpaRepository.findByIdAndDeletedAtIsNull(1L) } returns product

            // act
            val result = productRepositoryImpl.findByIdAndDeletedAtIsNull(1L)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.name).isEqualTo("후드")
            verify(exactly = 1) { productJpaRepository.findByIdAndDeletedAtIsNull(1L) }
        }

        @DisplayName("존재하지 않으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // arrange
            every { productJpaRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act
            val result = productRepositoryImpl.findByIdAndDeletedAtIsNull(999L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("상품 목록을 조회할 때,")
    @Nested
    inner class FindAll {
        @DisplayName("brandId가 null이면 전체 상품을 조회한다.")
        @Test
        fun returnsAllProducts_whenBrandIdIsNull() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val products = listOf(
                createProduct(name = "상품A", price = 10000, brandId = 1L),
                createProduct(name = "상품B", price = 20000, brandId = 2L),
            )
            val page = PageImpl(products, pageable, 2)
            every { productJpaRepository.findAllByDeletedAtIsNull(pageable) } returns page

            // act
            val result = productRepositoryImpl.findAllByDeletedAtIsNull(null, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            verify(exactly = 1) { productJpaRepository.findAllByDeletedAtIsNull(pageable) }
            verify(exactly = 0) { productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(any(), any()) }
        }

        @DisplayName("brandId가 있으면 해당 브랜드의 상품만 조회한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdIsProvided() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val products = listOf(
                createProduct(name = "나이키 상품", price = 30000, brandId = 1L),
            )
            val page = PageImpl(products, pageable, 1)
            every { productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(1L, pageable) } returns page

            // act
            val result = productRepositoryImpl.findAllByDeletedAtIsNull(1L, pageable)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("나이키 상품")
            verify(exactly = 1) { productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(1L, pageable) }
            verify(exactly = 0) { productJpaRepository.findAllByDeletedAtIsNull(any<PageRequest>()) }
        }
    }

    @DisplayName("여러 ID로 상품을 일괄 조회할 때,")
    @Nested
    inner class FindAllByIds {
        @DisplayName("JpaRepository에 ID 목록을 위임하여 결과를 반환한다.")
        @Test
        fun delegatesIdsToJpaRepository() {
            // arrange
            val ids = listOf(1L, 2L, 3L)
            val products = listOf(
                createProduct(name = "상품1", price = 10000, brandId = 1L),
                createProduct(name = "상품2", price = 20000, brandId = 1L),
            )
            every { productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids) } returns products

            // act
            val result = productRepositoryImpl.findAllByIdInAndDeletedAtIsNull(ids)

            // assert
            assertThat(result).hasSize(2)
            verify(exactly = 1) { productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids) }
        }
    }

    private fun createProduct(
        name: String,
        price: Long,
        brandId: Long,
        stockQuantity: Int = 100,
    ): ProductModel = ProductModel(
        name = name,
        price = price,
        brandId = brandId,
        stockQuantity = stockQuantity,
    )
}
