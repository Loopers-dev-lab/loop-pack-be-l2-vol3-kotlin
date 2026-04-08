package com.loopers.application.product

import com.loopers.application.event.DirectEventPublisher
import com.loopers.domain.ranking.RankingService
import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductFacade")
class ProductFacadeTest {

    @Mock
    private lateinit var productCacheManager: ProductCacheManager

    @Mock
    private lateinit var directEventPublisher: DirectEventPublisher

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var rankingService: RankingService

    private lateinit var productFacade: ProductFacade

    @BeforeEach
    fun setUp() {
        productFacade = ProductFacade(productCacheManager, directEventPublisher, eventPublisher, rankingService)
    }

    @DisplayName("상품 상세 조회 시,")
    @Nested
    inner class GetProduct {

        private val productId = 100L
        private val detailInfo = ProductDetailInfo(
            id = productId,
            name = "에어맥스",
            description = "러닝화",
            price = 159000L,
            likeCount = 10,
            brandId = 1L,
            brandName = "나이키",
        )

        @DisplayName("VIEWED 이벤트를 Kafka에 직접 발행한다.")
        @Test
        fun publishesViewedEventToKafka() {
            // arrange
            whenever(productCacheManager.getProduct(productId)).thenReturn(detailInfo)

            // act
            productFacade.getProduct(productId)

            // assert
            verify(directEventPublisher).publish(
                eq("catalog-events"),
                eq(productId.toString()),
                eq("VIEWED"),
                any(),
            )
        }

        @DisplayName("PRODUCT_VIEWED 유저 행동 이벤트를 발행한다.")
        @Test
        fun publishesProductViewedEvent() {
            // arrange
            whenever(productCacheManager.getProduct(productId)).thenReturn(detailInfo)

            // act
            productFacade.getProduct(productId)

            // assert
            val captor = argumentCaptor<UserActionEvent>()
            verify(eventPublisher).publishEvent(captor.capture())
            val event = captor.firstValue
            assertThat(event.actionType).isEqualTo(ActionType.PRODUCT_VIEWED)
            assertThat(event.targetId).isEqualTo(productId)
        }

        @DisplayName("랭킹 진입 상품이면, 1-based 순위를 포함하여 반환한다.")
        @Test
        fun returnsRankForRankedProduct() {
            // arrange
            whenever(productCacheManager.getProduct(productId)).thenReturn(detailInfo)
            whenever(rankingService.getRank(any(), eq(productId))).thenReturn(0L)

            // act
            val result = productFacade.getProduct(productId)

            // assert
            assertThat(result.rank).isEqualTo(1L)
        }

        @DisplayName("랭킹 미진입 상품이면, rank는 null이다.")
        @Test
        fun returnsNullRankForUnrankedProduct() {
            // arrange
            whenever(productCacheManager.getProduct(productId)).thenReturn(detailInfo)
            whenever(rankingService.getRank(any(), eq(productId))).thenReturn(null)

            // act
            val result = productFacade.getProduct(productId)

            // assert
            assertThat(result.rank).isNull()
        }
    }
}
