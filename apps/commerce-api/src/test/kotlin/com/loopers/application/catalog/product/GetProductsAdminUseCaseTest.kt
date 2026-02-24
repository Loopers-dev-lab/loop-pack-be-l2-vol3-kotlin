package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GetProductsAdminUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: GetProductsAdminUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = GetProductsAdminUseCase(productRepository)
    }

    @Nested
    @DisplayName("어드민 상품 목록 조회 시")
    inner class Execute {

        @Test
        @DisplayName("삭제된 상품도 포함하여 조회한다")
        fun getAdminProducts_includesDeleted() {
            // arrange
            productRepository.save(
                Product(refBrandId = 1L, name = "활성상품", price = Money(BigDecimal("10000")), stock = 10),
            )
            val deleted = productRepository.save(
                Product(refBrandId = 1L, name = "삭제상품", price = Money(BigDecimal("20000")), stock = 10),
            )
            deleted.delete()
            productRepository.save(deleted)

            // act
            val result = useCase.execute(0, 10)

            // assert
            assertThat(result.content).hasSize(2)
        }
    }
}
