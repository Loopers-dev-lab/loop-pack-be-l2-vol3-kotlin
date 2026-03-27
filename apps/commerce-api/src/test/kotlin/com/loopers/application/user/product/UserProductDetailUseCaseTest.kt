package com.loopers.application.user.product

import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.product.ProductQueryResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.check
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal

@DisplayName("UserProductDetailUseCase")
class UserProductDetailUseCaseTest {
    private val productQueryRepository: ProductQueryRepository = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()
    private val useCase = UserProductDetailUseCase(productQueryRepository, eventPublisher)

    @Nested
    @DisplayName("상세 조회 시")
    inner class WhenGetDetail {
        @Test
        @DisplayName("query repository 결과를 응답 DTO로 변환한다")
        fun getDetail_success() {
            given(productQueryRepository.getDetail(eq(1L))).willReturn(
                ProductQueryResult.Detail(
                    id = 1L,
                    name = "테스트 상품",
                    regularPrice = BigDecimal("10000.00"),
                    sellingPrice = BigDecimal("8000.00"),
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    imageUrl = null,
                    thumbnailUrl = null,
                    likeCount = 5,
                    stockQuantity = 10,
                ),
            )

            val result = useCase.getDetail(1L)

            assertThat(result.name).isEqualTo("테스트 상품")
            assertThat(result.brandName).isEqualTo("테스트 브랜드")
            assertThat(result.stockQuantity).isEqualTo(10)
            then(eventPublisher).should().publishEvent(
                check<ProductDetailViewedEvent> { event ->
                    assertThat(event.productId).isEqualTo(1L)
                },
            )
        }
    }
}
