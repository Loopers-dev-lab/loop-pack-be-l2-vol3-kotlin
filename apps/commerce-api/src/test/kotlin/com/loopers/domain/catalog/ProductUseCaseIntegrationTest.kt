package com.loopers.domain.catalog

import com.loopers.application.catalog.AdminDeleteProductUseCase
import com.loopers.application.catalog.AdminGetProductUseCase
import com.loopers.application.catalog.AdminListProductsUseCase
import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.AdminUpdateProductUseCase
import com.loopers.application.catalog.ListProductsCriteria
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterBrandResult
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.application.catalog.UpdateProductCriteria
import com.loopers.infrastructure.catalog.ProductJpaRepository
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

@SpringBootTest
class ProductUseCaseIntegrationTest @Autowired constructor(
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val adminGetProductUseCase: AdminGetProductUseCase,
    private val adminListProductsUseCase: AdminListProductsUseCase,
    private val adminUpdateProductUseCase: AdminUpdateProductUseCase,
    private val adminDeleteProductUseCase: AdminDeleteProductUseCase,
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")
        private const val DEFAULT_BRAND_NAME = "나이키"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerBrand(name: String = DEFAULT_BRAND_NAME): RegisterBrandResult {
        return adminRegisterBrandUseCase.execute(RegisterBrandCriteria(name = name))
    }

    private fun registerProduct(
        brandId: Long,
        name: String = DEFAULT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_PRICE,
    ): RegisterProductResult {
        return adminRegisterProductUseCase.execute(
            RegisterProductCriteria(
                brandId = brandId,
                name = name,
                quantity = quantity,
                price = price,
            ),
        )
    }

    @DisplayName("상품 등록")
    @Nested
    inner class Register {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 등록된다.")
        @Test
        fun registersProductWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()

            // act
            val result = registerProduct(brandId = brand.id)

            // assert
            assertThat(result.id).isNotNull()
        }

        @DisplayName("존재하지 않는 브랜드이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenBrandDoesNotExist() {
            // arrange
            val nonExistentBrandId = 999L

            // act & assert
            val result = assertThrows<CoreException> {
                registerProduct(brandId = nonExistentBrandId)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 상세 조회")
    @Nested
    inner class GetProduct {
        @DisplayName("존재하는 상품을 조회하면, 상품 정보와 브랜드명을 반환한다.")
        @Test
        fun returnsProductWithBrandNameWhenProductExists() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)

            // act
            val result = adminGetProductUseCase.execute(product.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(product.id) },
                { assertThat(result.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(result.price).isEqualByComparingTo(DEFAULT_PRICE) },
                { assertThat(result.quantity).isEqualTo(DEFAULT_QUANTITY) },
                { assertThat(result.brandId).isEqualTo(brand.id) },
                { assertThat(result.brandName).isEqualTo(DEFAULT_BRAND_NAME) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenProductDoesNotExist() {
            // arrange
            val nonExistentId = 999L

            // act & assert
            val result = assertThrows<CoreException> {
                adminGetProductUseCase.execute(nonExistentId)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 목록 조회")
    @Nested
    inner class ListProducts {
        @DisplayName("상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyListWhenNoProductsExist() {
            // arrange
            val criteria = ListProductsCriteria(page = 0, size = 10)

            // act
            val result = adminListProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("상품이 존재하면, 목록을 반환한다.")
        @Test
        fun returnsProductsWhenProductsExist() {
            // arrange
            val brand = registerBrand()
            registerProduct(brandId = brand.id, name = "상품1")
            registerProduct(brandId = brand.id, name = "상품2")
            val criteria = ListProductsCriteria(page = 0, size = 10)

            // act
            val result = adminListProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("페이지 크기보다 상품이 많으면, hasNext가 true이다.")
        @Test
        fun returnsHasNextTrueWhenMoreProductsExist() {
            // arrange
            val brand = registerBrand()
            registerProduct(brandId = brand.id, name = "상품1")
            registerProduct(brandId = brand.id, name = "상품2")
            registerProduct(brandId = brand.id, name = "상품3")
            val criteria = ListProductsCriteria(page = 0, size = 2)

            // act
            val result = adminListProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isTrue() },
            )
        }

        @DisplayName("brandId로 필터링하면, 해당 브랜드 상품만 반환한다.")
        @Test
        fun returnsFilteredProductsWhenBrandIdIsProvided() {
            // arrange
            val brand1 = registerBrand(name = "나이키")
            val brand2 = registerBrand(name = "아디다스")
            registerProduct(brandId = brand1.id, name = "나이키 상품")
            registerProduct(brandId = brand2.id, name = "아디다스 상품")
            val criteria = ListProductsCriteria(page = 0, size = 10, brandId = brand1.id)

            // act
            val result = adminListProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("삭제된 상품은 목록에 포함되지 않는다.")
        @Test
        fun excludesDeletedProductsFromList() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id, name = "상품1")
            registerProduct(brandId = brand.id, name = "상품2")
            adminDeleteProductUseCase.execute(product.id)
            val criteria = ListProductsCriteria(page = 0, size = 10)

            // act
            val result = adminListProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo("상품2") },
            )
        }
    }

    @DisplayName("상품 수정")
    @Nested
    inner class Update {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesProductWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val expectedName = "에어포스 1"
            val expectedQuantity = 50
            val expectedPrice = BigDecimal("99000")
            val criteria = UpdateProductCriteria(
                productId = product.id,
                newName = expectedName,
                newQuantity = expectedQuantity,
                newPrice = expectedPrice,
            )

            // act
            adminUpdateProductUseCase.execute(criteria)

            // assert
            val updated = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(updated.name).isEqualTo(expectedName) },
                { assertThat(updated.quantity).isEqualTo(expectedQuantity) },
                { assertThat(updated.price).isEqualByComparingTo(expectedPrice) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenProductDoesNotExist() {
            // arrange
            val criteria = UpdateProductCriteria(
                productId = 999L,
                newName = "변경된 상품",
                newQuantity = 50,
                newPrice = BigDecimal("99000"),
            )

            // act & assert
            val result = assertThrows<CoreException> {
                adminUpdateProductUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 삭제")
    @Nested
    inner class Delete {
        @DisplayName("존재하는 상품을 삭제하면, deletedAt이 설정된다.")
        @Test
        fun setsDeletedAtWhenProductIsDeleted() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)

            // act
            adminDeleteProductUseCase.execute(product.id)

            // assert
            val deleted = productJpaRepository.findById(product.id).get()
            assertThat(deleted.deletedAt).isNotNull()
        }

        @DisplayName("존재하지 않는 상품을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenProductDoesNotExist() {
            // arrange
            val nonExistentId = 999L

            // act & assert
            val result = assertThrows<CoreException> {
                adminDeleteProductUseCase.execute(nonExistentId)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 삭제된 상품을 다시 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenProductIsAlreadyDeleted() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            adminDeleteProductUseCase.execute(product.id)

            // act & assert
            val result = assertThrows<CoreException> {
                adminDeleteProductUseCase.execute(product.id)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
