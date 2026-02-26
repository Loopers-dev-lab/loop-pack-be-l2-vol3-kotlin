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

class UpdateBrandUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var useCase: UpdateBrandUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        useCase = UpdateBrandUseCase(brandRepository)
    }

    @Nested
    @DisplayName("브랜드 수정 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 이름으로 수정하면 브랜드명이 변경된다")
        fun updateBrand_withValidName_updatesName() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))

            // act
            val result = useCase.execute(brand.id.value, "아디다스")

            // assert
            assertThat(result.name).isEqualTo("아디다스")
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 수정하면 NOT_FOUND 예외가 발생한다")
        fun updateBrand_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L, "아디다스")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 브랜드를 수정하면 브랜드명이 변경된다")
        fun updateBrand_deletedBrand_updatesName() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val result = useCase.execute(brand.id.value, "아디다스")

            // assert
            assertThat(result.name).isEqualTo("아디다스")
            assertThat(result.deletedAt).isNotNull()
        }
    }
}
