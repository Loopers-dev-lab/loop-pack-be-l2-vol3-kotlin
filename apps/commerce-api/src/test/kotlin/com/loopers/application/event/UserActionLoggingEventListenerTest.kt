package com.loopers.application.event

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UserActionLoggingEventListener")
class UserActionLoggingEventListenerTest {
    private val userActionLogWriter: UserActionLogWriter = mockk()
    private val listener = UserActionLoggingEventListener(userActionLogWriter)

    @DisplayName("주문 완료 이벤트를 받으면 주문 액션 로그를 기록한다")
    @Test
    fun writesOrderActionLog_whenOrderCompletedEventReceived() {
        every { userActionLogWriter.write(any()) } just runs

        listener.handleOrderCompleted(OrderCompletedEvent(orderId = 11L, userId = 1L, totalAmount = 50_000L))

        verify(exactly = 1) {
            userActionLogWriter.write(
                match {
                    it.userId == 1L &&
                        it.targetId == 11L &&
                        it.actionType == UserActionType.ORDER_CREATED &&
                        it.metadata["totalAmount"] == 50_000L
                },
            )
        }
    }

    @DisplayName("좋아요 이벤트를 받으면 좋아요 액션 로그를 기록한다")
    @Test
    fun writesLikeActionLog_whenLikeChangedEventReceived() {
        every { userActionLogWriter.write(any()) } just runs

        listener.handleLikeChanged(LikeChangedEvent(userId = 1L, productId = 10L, actionType = LikeActionType.LIKE))

        verify(exactly = 1) {
            userActionLogWriter.write(
                match {
                    it.userId == 1L &&
                        it.targetId == 10L &&
                        it.actionType == UserActionType.PRODUCT_LIKED
                },
            )
        }
    }

    @DisplayName("상품 조회 이벤트를 받으면 조회 액션 로그를 기록한다")
    @Test
    fun writesProductViewActionLog_whenProductViewedEventReceived() {
        every { userActionLogWriter.write(any()) } just runs

        listener.handleProductViewed(
            ProductViewedEvent(
                productId = 10L,
                actionType = ProductViewActionType.PRODUCT_DETAIL_VIEWED,
            ),
        )

        verify(exactly = 1) {
            userActionLogWriter.write(
                match {
                    it.userId == null &&
                        it.targetId == 10L &&
                        it.actionType == UserActionType.PRODUCT_DETAIL_VIEWED
                },
            )
        }
    }

    @DisplayName("로깅 중 예외가 발생해도 예외를 전파하지 않는다")
    @Test
    fun doesNotThrow_whenWriterFails() {
        every { userActionLogWriter.write(any()) } throws IllegalStateException("log write failure")

        assertThatCode {
            listener.handleOrderCompleted(OrderCompletedEvent(orderId = 11L, userId = 1L, totalAmount = 50_000L))
        }.doesNotThrowAnyException()
    }
}
