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
    inner class Execute {

        @Test
        @DisplayName("활성 브랜드를 조회하면 반환된다")
        fun execute_activeBrand_returns() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))

            // act
            val result = useCase.execute(brand.id.value)

            // assert
            assertThat(result.name).isEqualTo("나이키")
        }

        @Test
        @DisplayName("삭제된 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_deletedBrand_throwsNotFound() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(brand.id.value)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        fun execute_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
