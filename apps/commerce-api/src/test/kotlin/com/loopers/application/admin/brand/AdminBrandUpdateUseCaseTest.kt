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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@DisplayName("AdminBrandUpdateUseCase")
class AdminBrandUpdateUseCaseTest {
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminBrandUpdateUseCase(brandRepository)

    private val existingBrand = Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.INACTIVE)

    @Nested
    @DisplayName("브랜드 수정 시")
    inner class WhenUpdate {
        @Test
        @DisplayName("이름과 상태를 변경하고 AdminBrandResult.Update를 반환한다")
        fun update_success() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(existingBrand)
            given(brandRepository.save(any(), any())).willAnswer { it.arguments[0] as Brand }

            // act
            val result = useCase.update(
                AdminBrandCommand.Update(brandId = 1L, name = "아디다스", status = "ACTIVE", admin = "loopers.admin"),
            )

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("아디다스") },
                { assertThat(result.status).isEqualTo("ACTIVE") },
            )
        }

        @Test
        @DisplayName("INACTIVE 상태를 ACTIVE로 변경한다")
        fun update_statusChange() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(existingBrand)
            given(brandRepository.save(any(), any())).willAnswer { it.arguments[0] as Brand }

            // act
            val result = useCase.update(
                AdminBrandCommand.Update(brandId = 1L, name = "나이키", status = "ACTIVE", admin = "loopers.admin"),
            )

            // assert
            assertThat(result.status).isEqualTo("ACTIVE")
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 실패한다")
    inner class WhenNotFound {
        @Test
        @DisplayName("CoreException(BRAND_NOT_FOUND)을 던진다")
        fun update_notFound() {
            // arrange
            given(brandRepository.findById(999L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> {
                useCase.update(
                    AdminBrandCommand.Update(brandId = 999L, name = "아디다스", status = "ACTIVE", admin = "loopers.admin"),
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }
}
