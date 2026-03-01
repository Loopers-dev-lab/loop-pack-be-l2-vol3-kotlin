package com.loopers.application.admin.brand

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
import org.mockito.BDDMockito.then
import org.mockito.kotlin.mock

@DisplayName("AdminBrandDeleteUseCase")
class AdminBrandDeleteUseCaseTest {
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminBrandDeleteUseCase(brandRepository)

    @Nested
    @DisplayName("브랜드 삭제 시")
    inner class WhenDelete {
        @Test
        @DisplayName("존재하는 브랜드면 repository.delete()를 호출한다")
        fun delete_success() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(
                Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE),
            )

            // act
            useCase.delete(1L, "loopers.admin")

            // assert
            then(brandRepository).should().delete(1L, "loopers.admin")
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 실패한다")
    inner class WhenNotFound {
        @Test
        @DisplayName("CoreException(BRAND_NOT_FOUND)을 던진다")
        fun delete_notFound() {
            // arrange
            given(brandRepository.findById(999L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> {
                useCase.delete(999L, "loopers.admin")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }
}
