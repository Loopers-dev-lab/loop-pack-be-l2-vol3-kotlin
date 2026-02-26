package com.loopers.domain.brand

import com.loopers.application.catalog.AdminDeleteBrandUseCase
import com.loopers.application.catalog.AdminGetBrandsUseCase
import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.AdminUpdateBrandUseCase
import com.loopers.application.catalog.ListBrandsCriteria
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.UpdateBrandCriteria
import com.loopers.infrastructure.catalog.BrandJpaRepository
import com.loopers.infrastructure.catalog.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import java.math.BigDecimal
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
class BrandServiceIntegrationTest @Autowired constructor(
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val adminUpdateBrandUseCase: AdminUpdateBrandUseCase,
    private val adminGetBrandsUseCase: AdminGetBrandsUseCase,
    private val adminDeleteBrandUseCase: AdminDeleteBrandUseCase,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_NAME = "나이키"
        private const val DEFAULT_DESCRIPTION = "스포츠 브랜드"
        private const val DEFAULT_LOGO_URL = "https://example.com/nike-logo.png"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createRegisterCriteria(
        name: String = DEFAULT_NAME,
        description: String? = DEFAULT_DESCRIPTION,
        logoUrl: String? = DEFAULT_LOGO_URL,
    ): RegisterBrandCriteria = RegisterBrandCriteria(
        name = name,
        description = description,
        logoUrl = logoUrl,
    )

    @DisplayName("브랜드 등록")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 등록된다.")
        @Test
        fun registersBrandWhenValidInfoIsProvided() {
            // arrange
            val command = createRegisterCriteria()

            // act
            val result = adminRegisterBrandUseCase.execute(command)

            // assert
            assertThat(result.id).isNotNull()
        }

        @DisplayName("중복된 이름이 주어지면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenDuplicateNameIsProvided() {
            // arrange
            adminRegisterBrandUseCase.execute(createRegisterCriteria())
            val duplicateCommand = createRegisterCriteria()

            // act
            val result = assertThrows<CoreException> {
                adminRegisterBrandUseCase.execute(duplicateCommand)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("브랜드 수정")
    @Nested
    inner class Update {
        @DisplayName("유효한 정보가 주어지면,정상적으로 수정된다.")
        @Test
        fun updatesBrandWhenValidInfoIsProvided() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria())
            val expectedName = "나이키 우먼"
            val expectedDescription = "여성용 나이키 스포츠 브랜드"
            val expectedLogoUrl = "https://example.com/nike-logo-women.png"
            val command = UpdateBrandCriteria(brand.id, expectedName, expectedDescription, expectedLogoUrl)

            // act
            adminUpdateBrandUseCase.execute(command)

            // assert
            val updatedBrand = brandJpaRepository.findById(brand.id).get()
            assertAll(
                { assertThat(updatedBrand.name).isEqualTo(expectedName) },
                { assertThat(updatedBrand.description).isEqualTo(expectedDescription) },
                { assertThat(updatedBrand.logoUrl).isEqualTo(expectedLogoUrl) },
            )
        }

        @DisplayName("브랜드가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenBrandDoesNotExist() {
            // arrange
            val newName = "나이키 우먼"
            val newDescription = "여성용 나이키 스포츠 브랜드"
            val newLogoUrl = "https://example.com/nike-logo-women.png"
            val command = UpdateBrandCriteria(999, newName, newDescription, newLogoUrl)

            // act
            val result = assertThrows<CoreException> {
                adminUpdateBrandUseCase.execute(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("같은 이름으로 수정하면, 정상적으로 수정된다.")
        @Test
        fun updatesBrandWhenSameNameIsProvided() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria())
            val expectedDescription = "변경된 설명"
            val command = UpdateBrandCriteria(brand.id, DEFAULT_NAME, expectedDescription, null)

            // act
            adminUpdateBrandUseCase.execute(command)

            // assert
            val updatedBrand = brandJpaRepository.findById(brand.id).get()
            assertAll(
                { assertThat(updatedBrand.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(updatedBrand.description).isEqualTo(expectedDescription) },
            )
        }

        @DisplayName("이미 존재하는 이름으로 수정하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenDuplicateNameIsProvided() {
            // arrange
            adminRegisterBrandUseCase.execute(createRegisterCriteria())
            val anotherBrand = adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "아디다스"))
            val command = UpdateBrandCriteria(anotherBrand.id, DEFAULT_NAME, null, null)

            // act
            val result = assertThrows<CoreException> {
                adminUpdateBrandUseCase.execute(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("브랜드 목록 조회")
    @Nested
    inner class ListBrands {

        @DisplayName("브랜드가 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyListWhenNoBrandsExist() {
            // arrange
            val criteria = ListBrandsCriteria(page = 0, size = 10)

            // act
            val result = adminGetBrandsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("브랜드가 존재하면, 목록을 반환한다.")
        @Test
        fun returnsBrandsWhenBrandsExist() {
            // arrange
            adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "나이키"))
            adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "아디다스"))
            val criteria = ListBrandsCriteria(page = 0, size = 10)

            // act
            val result = adminGetBrandsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("페이지 크기보다 브랜드가 많으면, hasNext가 true이다.")
        @Test
        fun returnsHasNextTrueWhenMoreBrandsExist() {
            // arrange
            adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "나이키"))
            adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "아디다스"))
            adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "퓨마"))
            val criteria = ListBrandsCriteria(page = 0, size = 2)

            // act
            val result = adminGetBrandsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isTrue() },
            )
        }

        @DisplayName("삭제된 브랜드는 목록에 포함되지 않는다.")
        @Test
        fun excludesDeletedBrandsFromList() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "나이키"))
            adminRegisterBrandUseCase.execute(createRegisterCriteria(name = "아디다스"))
            adminDeleteBrandUseCase.execute(brand.id)
            val criteria = ListBrandsCriteria(page = 0, size = 10)

            // act
            val result = adminGetBrandsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo("아디다스") },
            )
        }
    }

    @DisplayName("브랜드 삭제")
    @Nested
    inner class Delete {

        @DisplayName("존재하는 브랜드를 삭제하면, deletedAt이 설정된다.")
        @Test
        fun setsDeletedAtWhenBrandIsDeleted() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria())

            // act
            adminDeleteBrandUseCase.execute(brand.id)

            // assert
            val deletedBrand = brandJpaRepository.findById(brand.id).get()
            assertThat(deletedBrand.deletedAt).isNotNull()
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenBrandDoesNotExist() {
            // arrange
            val nonExistentId = 999L

            // act
            val result = assertThrows<CoreException> {
                adminDeleteBrandUseCase.execute(nonExistentId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenBrandIsAlreadyDeleted() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria())
            adminDeleteBrandUseCase.execute(brand.id)

            // act
            val result = assertThrows<CoreException> {
                adminDeleteBrandUseCase.execute(brand.id)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 브랜드의 이름으로 새 브랜드를 등록할 수 있다.")
        @Test
        fun allowsRegisteringBrandWithDeletedBrandName() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria())
            adminDeleteBrandUseCase.execute(brand.id)

            // act
            val newBrand = adminRegisterBrandUseCase.execute(createRegisterCriteria())

            // assert
            assertThat(newBrand.id).isNotEqualTo(brand.id)
        }

        @DisplayName("브랜드를 삭제하면, 해당 브랜드의 상품도 함께 삭제된다.")
        @Test
        fun cascadeDeletesProductsWhenBrandIsDeleted() {
            // arrange
            val brand = adminRegisterBrandUseCase.execute(createRegisterCriteria())
            val product = adminRegisterProductUseCase.execute(
                RegisterProductCriteria(
                    brandId = brand.id,
                    name = "에어맥스 90",
                    quantity = 100,
                    price = BigDecimal("129000"),
                ),
            )

            // act
            adminDeleteBrandUseCase.execute(brand.id)

            // assert
            val deletedProduct = productJpaRepository.findById(product.id).get()
            assertThat(deletedProduct.deletedAt).isNotNull()
        }
    }
}
