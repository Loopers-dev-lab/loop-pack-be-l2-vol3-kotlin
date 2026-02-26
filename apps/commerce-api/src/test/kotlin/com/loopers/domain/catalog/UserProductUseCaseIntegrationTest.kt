package com.loopers.domain.catalog

import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterBrandResult
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.application.catalog.UserGetProductUseCase
import com.loopers.application.catalog.UserGetProductsUseCase
import com.loopers.application.catalog.UserListProductsCriteria
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
class UserProductUseCaseIntegrationTest @Autowired constructor(
    private val userGetProductUseCase: UserGetProductUseCase,
    private val userGetProductsUseCase: UserGetProductsUseCase,
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
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

    @DisplayName("사용자 상품 상세 조회")
    @Nested
    inner class GetProduct {
        @DisplayName("존재하는 상품을 조회하면, quantity 없이 상품 정보를 반환한다.")
        @Test
        fun returnsProductWithoutQuantityWhenProductExists() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)

            // act
            val result = userGetProductUseCase.execute(product.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(product.id) },
                { assertThat(result.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(result.price).isEqualByComparingTo(DEFAULT_PRICE) },
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
                userGetProductUseCase.execute(nonExistentId)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("사용자 상품 목록 조회")
    @Nested
    inner class ListProducts {
        @DisplayName("상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyListWhenNoProductsExist() {
            // arrange
            val criteria = UserListProductsCriteria(page = 0, size = 10)

            // act
            val result = userGetProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
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
            val criteria = UserListProductsCriteria(page = 0, size = 2)

            // act
            val result = userGetProductsUseCase.execute(criteria)

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
            val criteria = UserListProductsCriteria(page = 0, size = 10, brandId = brand1.id)

            // act
            val result = userGetProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("PRICE_ASC 정렬 시, 가격 오름차순으로 반환한다.")
        @Test
        fun returnsSortedByPriceAscWhenPriceAscSortIsProvided() {
            // arrange
            val brand = registerBrand()
            registerProduct(brandId = brand.id, name = "비싼 상품", price = BigDecimal("200000"))
            registerProduct(brandId = brand.id, name = "싼 상품", price = BigDecimal("50000"))
            registerProduct(brandId = brand.id, name = "중간 상품", price = BigDecimal("100000"))
            val criteria = UserListProductsCriteria(
                page = 0,
                size = 10,
                sort = ProductSortType.PRICE_ASC,
            )

            // act
            val result = userGetProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(3) },
                { assertThat(result.content[0].name).isEqualTo("싼 상품") },
                { assertThat(result.content[1].name).isEqualTo("중간 상품") },
                { assertThat(result.content[2].name).isEqualTo("비싼 상품") },
            )
        }
    }
}
