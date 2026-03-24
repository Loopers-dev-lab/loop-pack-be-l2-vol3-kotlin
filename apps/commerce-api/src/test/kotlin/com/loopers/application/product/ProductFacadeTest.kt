package com.loopers.application.product

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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductFacade")
class ProductFacadeTest {

    @Mock
    private lateinit var productCacheManager: ProductCacheManager

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var productFacade: ProductFacade

    @BeforeEach
    fun setUp() {
        productFacade = ProductFacade(productCacheManager, eventPublisher)
    }

    @DisplayName("상품 상세 조회 시,")
    @Nested
    inner class GetProduct {

        @DisplayName("PRODUCT_VIEWED 이벤트를 발행한다.")
        @Test
        fun publishesProductViewedEvent() {
            // arrange
            val productId = 100L
            val detailInfo = ProductDetailInfo(
                id = productId,
                name = "에어맥스",
                description = "러닝화",
                price = 159000L,
                likeCount = 10,
                brandId = 1L,
                brandName = "나이키",
            )
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
    }
}
