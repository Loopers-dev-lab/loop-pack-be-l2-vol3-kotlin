package com.loopers.application.user.product

import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.product.ProductQueryResult
import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.support.event.user.ProductDetailViewedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.check
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal

@DisplayName("UserProductDetailUseCase")
class UserProductDetailUseCaseTest {
    private val productQueryRepository: ProductQueryRepository = mock()
    private val eventPublisher: ApplicationEventPublisher = mock()
    private val productRankingQueryRepository: ProductRankingQueryRepository = mock()
    private val useCase = UserProductDetailUseCase(productQueryRepository, eventPublisher, productRankingQueryRepository)

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

    @Nested
    @DisplayName("상품 상세 조회 시 랭킹 순위가 반환된다")
    inner class WhenGetDetailWithRank {
        @Test
        @DisplayName("랭킹에 존재하는 상품 → rank 값 반환 (1-based)")
        fun getDetail_withRank() {
            givenProductDetail(1L)
            whenever(productRankingQueryRepository.getRank(any(), eq(1L))).thenReturn(3L)

            val result = useCase.getDetail(1L)

            assertThat(result.rank).isEqualTo(3)
        }

        @Test
        @DisplayName("랭킹에 없는 상품 → rank=null")
        fun getDetail_withoutRank() {
            givenProductDetail(1L)
            whenever(productRankingQueryRepository.getRank(any(), eq(1L))).thenReturn(null)

            val result = useCase.getDetail(1L)

            assertThat(result.rank).isNull()
        }

        @Test
        @DisplayName("Redis 장애 시 rank=null로 반환하고 API는 정상 동작")
        fun getDetail_redisFailure_rankNull() {
            givenProductDetail(1L)
            whenever(productRankingQueryRepository.getRank(any(), any()))
                .thenThrow(RuntimeException("Redis connection refused"))

            val result = useCase.getDetail(1L)

            assertThat(result.rank).isNull()
            assertThat(result.name).isEqualTo("테스트 상품")
        }
    }

    private fun givenProductDetail(productId: Long) {
        given(productQueryRepository.getDetail(eq(productId))).willReturn(
            ProductQueryResult.Detail(
                id = productId,
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
    }
}
