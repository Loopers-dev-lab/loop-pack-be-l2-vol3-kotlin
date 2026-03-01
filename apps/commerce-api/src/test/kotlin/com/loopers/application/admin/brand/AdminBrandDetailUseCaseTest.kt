package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock

@DisplayName("AdminBrandDetailUseCase")
class AdminBrandDetailUseCaseTest {
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminBrandDetailUseCase(brandRepository)

    private val existingBrand = Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE)

    @Nested
    @DisplayName("브랜드 상세 조회 시")
    inner class WhenGetDetail {
        @Test
        @DisplayName("AdminBrandResult.Detail을 반환한다")
        fun getDetail_success() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(existingBrand)

            // act
            val result = useCase.getDetail(1L)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(1L) },
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.status).isEqualTo("ACTIVE") },
            )
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 실패한다")
    inner class WhenNotFound {
        @Test
        @DisplayName("CoreException(BRAND_NOT_FOUND)을 던진다")
        fun getDetail_notFound() {
            // arrange
            given(brandRepository.findById(999L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> {
                useCase.getDetail(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }
}
