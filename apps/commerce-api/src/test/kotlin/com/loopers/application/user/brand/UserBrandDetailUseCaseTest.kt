package com.loopers.application.user.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@DisplayName("UserBrandDetailUseCase")
class UserBrandDetailUseCaseTest {
    private val brandRepository: BrandRepository = mock()
    private val useCase = UserBrandDetailUseCase(brandRepository)

    @Nested
    @DisplayName("활성 브랜드 조회 시")
    inner class WhenActiveBrand {
        @Test
        @DisplayName("상세 정보를 반환한다")
        fun getDetail_success() {
            given(brandRepository.findById(eq(1L)))
                .willReturn(Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE))

            val result = useCase.getDetail(1L)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.name).isEqualTo("나이키")
        }
    }

    @Nested
    @DisplayName("존재하지 않거나 비활성 브랜드 조회 시")
    inner class WhenNotFoundOrInactive {
        @Test
        @DisplayName("존재하지 않으면 BRAND_NOT_FOUND 예외")
        fun getDetail_notFound() {
            given(brandRepository.findById(eq(999L))).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.getDetail(999L) }

            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }

        @Test
        @DisplayName("비활성이면 BRAND_NOT_FOUND 예외")
        fun getDetail_inactive() {
            given(brandRepository.findById(eq(1L)))
                .willReturn(Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.INACTIVE))

            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }

            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }
}
