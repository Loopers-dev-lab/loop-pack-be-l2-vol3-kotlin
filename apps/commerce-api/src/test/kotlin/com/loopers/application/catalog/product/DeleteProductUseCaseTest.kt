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

class DeleteProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: DeleteProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = DeleteProductUseCase(productRepository)
    }

    @Nested
    @DisplayName("상품 삭제 시")
    inner class Execute {

        @Test
        @DisplayName("상품을 삭제하면 soft delete된다")
        fun deleteProduct_softDeletes() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = BrandId(1), name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )

            // act
            useCase.execute(product.id.value)

            // assert
            val deleted = productRepository.findById(product.id)
            assertThat(deleted?.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("이미 삭제된 상품을 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteProduct_alreadyDeleted_isIdempotent() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = BrandId(1), name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )
            product.delete()
            productRepository.save(product)

            // act & assert — 예외 없이 정상 반환
            useCase.execute(product.id.value)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteProduct_nonExistent_isIdempotent() {
            // act & assert — 예외 없이 정상 반환
            useCase.execute(999L)
        }
    }
}
