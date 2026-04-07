package com.loopers.application.event.handler

import com.loopers.application.event.LikeToggledEvent
import com.loopers.application.event.OrderCreatedEvent
import com.loopers.application.event.PaymentCompletedEvent
import com.loopers.application.event.PaymentFailedEvent
import com.loopers.application.event.ProductViewedEvent
import com.loopers.domain.activity.ActivityType
import com.loopers.domain.activity.TargetType
import com.loopers.domain.activity.UserActivityLog
import com.loopers.domain.activity.UserActivityLogRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class UserActivityEventHandlerTest {

    @Mock
    private lateinit var userActivityLogRepository: UserActivityLogRepository

    @InjectMocks
    private lateinit var handler: UserActivityEventHandler

    @DisplayName("OrderCreatedEvent를 처리할 때,")
    @Nested
    inner class HandleOrderCreated {

        @DisplayName("ORDER 활동 로그가 저장된다.")
        @Test
        fun savesOrderActivityLog() {
            // arrange
            val event = OrderCreatedEvent(
                orderId = 1L,
                userId = 1L,
                productIds = listOf(1L, 2L),
                items = emptyList(),
                totalAmount = BigDecimal("30000"),
                couponId = null,
            )
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.ORDER
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handleOrderCreated(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                activityType == ActivityType.ORDER &&
                    targetType == TargetType.ORDER &&
                    targetId == 1L &&
                    userId == 1L
            },
            )
        }
    }

    @DisplayName("PaymentCompletedEvent를 처리할 때,")
    @Nested
    inner class HandlePaymentCompleted {

        @DisplayName("PAYMENT 활동 로그가 저장된다.")
        @Test
        fun savesPaymentActivityLog() {
            // arrange
            val event = PaymentCompletedEvent(
                paymentId = 1L,
                orderId = 1L,
                userId = 1L,
                amount = BigDecimal("30000"),
            )
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.PAYMENT
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handlePaymentCompleted(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                activityType == ActivityType.PAYMENT &&
                    targetType == TargetType.ORDER &&
                    targetId == 1L &&
                    userId == 1L
            },
            )
        }
    }

    @DisplayName("PaymentFailedEvent를 처리할 때,")
    @Nested
    inner class HandlePaymentFailed {

        @DisplayName("PAYMENT_FAILED 활동 로그가 저장된다.")
        @Test
        fun savesPaymentFailedActivityLog() {
            // arrange
            val event = PaymentFailedEvent(
                paymentId = 1L,
                orderId = 1L,
                userId = 1L,
                reason = "PG 시스템 장애",
            )
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.PAYMENT_FAILED
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handlePaymentFailed(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                activityType == ActivityType.PAYMENT_FAILED &&
                    targetType == TargetType.ORDER &&
                    targetId == 1L
            },
            )
        }
    }

    @DisplayName("LikeToggledEvent를 처리할 때,")
    @Nested
    inner class HandleLikeToggled {

        @DisplayName("liked=true이면, LIKE 활동 로그가 저장된다.")
        @Test
        fun savesLikeActivityLog_whenLiked() {
            // arrange
            val event = LikeToggledEvent(userId = 1L, productId = 1L, liked = true)
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.LIKE
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handleLikeToggled(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                activityType == ActivityType.LIKE &&
                    targetType == TargetType.PRODUCT &&
                    targetId == 1L
            },
            )
        }

        @DisplayName("liked=false이면, UNLIKE 활동 로그가 저장된다.")
        @Test
        fun savesUnlikeActivityLog_whenUnliked() {
            // arrange
            val event = LikeToggledEvent(userId = 1L, productId = 1L, liked = false)
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.UNLIKE
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handleLikeToggled(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                activityType == ActivityType.UNLIKE &&
                    targetType == TargetType.PRODUCT &&
                    targetId == 1L
            },
            )
        }
    }

    @DisplayName("ProductViewedEvent를 처리할 때,")
    @Nested
    inner class HandleProductViewed {

        @DisplayName("VIEW 활동 로그가 저장된다.")
        @Test
        fun savesViewActivityLog() {
            // arrange
            val event = ProductViewedEvent(userId = 1L, productId = 1L)
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.VIEW
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handleProductViewed(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                activityType == ActivityType.VIEW &&
                    targetType == TargetType.PRODUCT &&
                    targetId == 1L
            },
            )
        }

        @DisplayName("userId가 null이면, userId=0으로 저장된다.")
        @Test
        fun savesWithZeroUserId_whenUserIdIsNull() {
            // arrange
            val event = ProductViewedEvent(userId = null, productId = 1L)
            whenever(
                userActivityLogRepository.save(
                    argThat<UserActivityLog> {
                activityType == ActivityType.VIEW
            },
                ),
            ).thenAnswer { it.arguments[0] }

            // act
            handler.handleProductViewed(event)

            // assert
            verify(userActivityLogRepository).save(
                argThat {
                userId == 0L && activityType == ActivityType.VIEW
            },
            )
        }
    }
}
