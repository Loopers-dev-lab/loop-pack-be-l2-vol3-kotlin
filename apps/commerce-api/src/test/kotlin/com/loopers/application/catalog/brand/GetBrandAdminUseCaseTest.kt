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

class GetBrandAdminUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var useCase: GetBrandAdminUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        useCase = GetBrandAdminUseCase(brandRepository)
    }

    @Nested
    @DisplayName("어드민 브랜드 조회 시")
    inner class Execute {

        @Test
        @DisplayName("삭제 포함하여 브랜드를 조회한다")
        fun execute_includesDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = BrandName("나이키")))
            brand.delete()
            brandRepository.save(brand)

            // act
            val result = useCase.execute(brand.id)

            // assert
            assertThat(result.name).isEqualTo("나이키")
            assertThat(result.deletedAt).isNotNull()
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
