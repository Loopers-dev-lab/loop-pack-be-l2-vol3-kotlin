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

class RestoreBrandUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var useCase: RestoreBrandUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        useCase = RestoreBrandUseCase(brandRepository)
    }

    @Nested
    @DisplayName("브랜드 복구 시")
    inner class Execute {

        @Test
        @DisplayName("삭제된 브랜드를 복구하면 deletedAt이 null이 된다")
        fun restoreBrand_deletedBrand_restoresSuccessfully() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val result = useCase.execute(brand.id.value)

            // assert
            assertThat(result.deletedAt).isNull()
            assertThat(result.name).isEqualTo("나이키")
        }

        @Test
        @DisplayName("삭제되지 않은 브랜드를 복구해도 정상 동작한다 (멱등)")
        fun restoreBrand_activeBrand_isIdempotent() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))

            // act
            val result = useCase.execute(brand.id.value)

            // assert
            assertThat(result.deletedAt).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 복구하면 NOT_FOUND 예외가 발생한다")
        fun restoreBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
