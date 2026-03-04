package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.ProductService
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

@SpringBootTest
class AdminBrandFacadeIntegrationTest @Autowired constructor(
    private val adminBrandFacade: AdminBrandFacade,
    private val brandRepository: BrandRepository,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 상세 조회할 때,")
    @Nested
    inner class GetBrand {

        @DisplayName("DB에 저장된 브랜드를 조회하면, BrandInfo를 반환한다.")
        @Test
        fun returnsBrandInfo_whenBrandExistsInDb() {
            // arrange
            val saved = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val result = adminBrandFacade.getBrand(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
                { assertThat(result.createdAt).isNotNull() },
                { assertThat(result.updatedAt).isNotNull() },
            )
        }

        @DisplayName("존재하지 않는 brandId로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                adminBrandFacade.getBrand(9999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드 삭제할 때,")
    @Nested
    inner class DeleteBrand {

        @DisplayName("DB에 저장된 브랜드를 삭제하면, soft delete 되어 조회할 수 없다.")
        @Test
        fun softDeletesBrand_whenBrandExistsInDb() {
            // arrange
            val saved = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            adminBrandFacade.deleteBrand(saved.id)

            // assert
            val exception = assertThrows<CoreException> {
                adminBrandFacade.getBrand(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("브랜드 삭제 시 소속 상품도 연쇄 soft delete 된다.")
        @Test
        fun cascadeDeletesProducts_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productService.createProduct(
                name = "에어맥스",
                description = "운동화",
                price = Money.of(100000L),
                stockQuantity = StockQuantity.of(10),
                brandId = brand.id,
            )

            // act
            adminBrandFacade.deleteBrand(brand.id)

            // assert
            val exception = assertThrows<CoreException> {
                productService.getProduct(product.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 brandId로 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                adminBrandFacade.deleteBrand(9999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
