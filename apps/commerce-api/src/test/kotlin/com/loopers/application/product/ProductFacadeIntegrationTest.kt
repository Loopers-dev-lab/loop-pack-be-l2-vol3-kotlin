package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.CreateProductCommand
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

/**
 * ProductFacade 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Facade → Service → Repository 레이어 통합 테스트
 * - Facade는 2+ 서비스 조합 시에만 사용 (createProduct: 브랜드 검증 + 상품 생성)
 */
@SpringBootTest
class ProductFacadeIntegrationTest @Autowired constructor(
    private val productFacade: ProductFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandJpaRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("정상적인 정보가 주어지면, 상품이 DB에 저장된다.")
        @Test
        fun savesProductToDatabase_whenValidInfoProvided() {
            // arrange
            val brand = createBrand()
            val command = CreateProductCommand(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = "나이키 에어맥스 90",
                imageUrl = "https://example.com/airmax90.jpg",
            )

            // act
            val result = productFacade.createProduct(command)

            // assert
            val saved = productJpaRepository.findByIdAndDeletedAtIsNull(result.id)!!
            assertAll(
                { assertThat(saved.name).isEqualTo("에어맥스 90") },
                { assertThat(saved.brandId).isEqualTo(brand.id) },
                { assertThat(saved.price).isEqualByComparingTo(BigDecimal("129000")) },
                { assertThat(saved.stock).isEqualTo(100) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 등록하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // arrange
            val command = CreateProductCommand(
                brandId = 999L,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.createProduct(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
