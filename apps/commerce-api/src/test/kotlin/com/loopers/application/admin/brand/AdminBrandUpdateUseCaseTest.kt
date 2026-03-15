package com.loopers.application.admin.brand

import com.loopers.application.event.product.ProductQueryChangedEventPublisher
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock

@DisplayName("AdminBrandUpdateUseCase")
class AdminBrandUpdateUseCaseTest {
    private val productQueryChangedEventPublisher: ProductQueryChangedEventPublisher = mock()
    private val brandRepository: BrandRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val useCase = AdminBrandUpdateUseCase(brandRepository, productRepository, productQueryChangedEventPublisher)

    private val existingBrand = Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.INACTIVE)

    @Nested
    @DisplayName("브랜드 수정 시")
    inner class WhenUpdate {
        @Test
        @DisplayName("이름과 상태를 변경하고 AdminBrandResult.Update를 반환한다")
        fun update_success() {
            given(brandRepository.findById(1L)).willReturn(existingBrand)
            given(brandRepository.save(any(), any())).willAnswer { it.arguments[0] as Brand }
            given(productRepository.findAllByBrandId(1L)).willReturn(emptyList())

            val result = useCase.update(
                AdminBrandCommand.Update(brandId = 1L, name = "아디다스", status = "ACTIVE", admin = "loopers.admin"),
            )

            assertAll(
                { assertThat(result.name).isEqualTo("아디다스") },
                { assertThat(result.status).isEqualTo("ACTIVE") },
            )
            then(productQueryChangedEventPublisher).should().publish(
                check {
                    assertThat(it.productIds).isEmpty()
                    assertThat(it.brandIds).containsExactly(1L)
                },
            )
        }

        @Test
        @DisplayName("INACTIVE 상태를 ACTIVE로 변경한다")
        fun update_statusChange() {
            given(brandRepository.findById(1L)).willReturn(existingBrand)
            given(brandRepository.save(any(), any())).willAnswer { it.arguments[0] as Brand }
            given(productRepository.findAllByBrandId(1L)).willReturn(emptyList())

            val result = useCase.update(
                AdminBrandCommand.Update(brandId = 1L, name = "나이키", status = "ACTIVE", admin = "loopers.admin"),
            )

            assertThat(result.status).isEqualTo("ACTIVE")
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 실패한다")
    inner class WhenNotFound {
        @Test
        @DisplayName("CoreException(BRAND_NOT_FOUND)을 던진다")
        fun update_notFound() {
            given(brandRepository.findById(999L)).willReturn(null)

            val exception = assertThrows<CoreException> {
                useCase.update(
                    AdminBrandCommand.Update(brandId = 999L, name = "아디다스", status = "ACTIVE", admin = "loopers.admin"),
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
            org.mockito.Mockito.verifyNoInteractions(productRepository, productQueryChangedEventPublisher)
        }
    }
}
