package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetBrandUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var useCase: GetBrandUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        useCase = GetBrandUseCase(brandRepository)
    }

    @Nested
    @DisplayName("활성 브랜드 조회 시")
    inner class ExecuteActive {

        @Test
        @DisplayName("활성 브랜드를 조회하면 반환된다")
        fun executeActive_activeBrand_returns() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))

            // act
            val result = useCase.executeActive(brand.id)

            // assert
            assertThat(result.name).isEqualTo("나이키")
        }

        @Test
        @DisplayName("삭제된 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun executeActive_deletedBrand_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val exception = assertThrows<CoreException> {
                useCase.executeActive(brand.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun executeActive_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.executeActive(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("어드민 브랜드 조회 시")
    inner class ExecuteAdmin {

        @Test
        @DisplayName("삭제 포함하여 브랜드를 조회한다")
        fun executeAdmin_includesDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val result = useCase.executeAdmin(brand.id)

            // assert
            assertThat(result.name).isEqualTo("나이키")
            assertThat(result.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun executeAdmin_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.executeAdmin(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
