package com.loopers.domain.brand

import com.loopers.infrastructure.brand.BrandJpaRepository
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
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val brandJpaRepository: BrandJpaRepository,
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

    private fun createRegisterCommand(
        name: String = DEFAULT_NAME,
        description: String? = DEFAULT_DESCRIPTION,
        logoUrl: String? = DEFAULT_LOGO_URL,
    ): RegisterCommand = RegisterCommand(
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
            val command = createRegisterCommand()

            // act
            val result = brandService.register(command)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(result.description).isEqualTo(DEFAULT_DESCRIPTION) },
                { assertThat(result.logoUrl).isEqualTo(DEFAULT_LOGO_URL) },
            )
        }

        @DisplayName("중복된 이름이 주어지면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenDuplicateNameIsProvided() {
            // arrange
            brandService.register(createRegisterCommand())
            val duplicateCommand = createRegisterCommand()

            // act
            val result = assertThrows<CoreException> {
                brandService.register(duplicateCommand)
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
            val brand = brandService.register(createRegisterCommand())
            val expectedName = "나이키 우먼"
            val expectedDescription = "여성용 나이키 스포츠 브랜드"
            val expectedLogoUrl = "https://example.com/nike-logo-women.png"
            val command = UpdateCommand(brand.id, expectedName, expectedDescription, expectedLogoUrl)

            // act
            brandService.update(command)

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
            val command = UpdateCommand(999, newName, newDescription, newLogoUrl)

            // act
            val result = assertThrows<CoreException> {
                brandService.update(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("같은 이름으로 수정하면, 정상적으로 수정된다.")
        @Test
        fun updatesBrandWhenSameNameIsProvided() {
            // arrange
            val brand = brandService.register(createRegisterCommand())
            val expectedDescription = "변경된 설명"
            val command = UpdateCommand(brand.id, DEFAULT_NAME, expectedDescription, null)

            // act
            brandService.update(command)

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
            brandService.register(createRegisterCommand())
            val anotherBrand = brandService.register(createRegisterCommand(name = "아디다스"))
            val command = UpdateCommand(anotherBrand.id, DEFAULT_NAME, null, null)

            // act
            val result = assertThrows<CoreException> {
                brandService.update(command)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }
}
