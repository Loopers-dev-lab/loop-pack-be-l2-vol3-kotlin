package com.loopers.infrastructure.activity

import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderSnapshot
import com.loopers.support.event.user.OrderCreatedEvent
import com.loopers.support.event.user.PaymentFailedEvent
import com.loopers.support.event.user.PaymentSucceededEvent
import com.loopers.support.event.user.ProductDetailViewedEvent
import com.loopers.support.event.user.ProductLikeCanceledEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@DisplayName("User activity event logger integration")
@ExtendWith(OutputCaptureExtension::class)
@SpringBootTest
class UserActivityEventLoggerIntegrationTest
@Autowired
constructor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val orderRepository: OrderRepository,
    platformTransactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    @Test
    @DisplayName("product detail viewed event is logged immediately")
    fun log_productDetailViewed_immediately(output: CapturedOutput) {
        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(ProductDetailViewedEvent(productId = PRODUCT_ID))

            assertThat(output.out)
                .contains("action=product_detail_viewed")
                .contains("productId=$PRODUCT_ID")
        }
    }

    @Test
    @DisplayName("mutating user activity events are logged after commit")
    fun log_mutatingEvents_afterCommit(output: CapturedOutput) {
        val seededOrderId = seedOrder("activity-test-order-after-commit")

        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(ProductLikeRegisteredEvent(userId = USER_ID, productId = PRODUCT_ID))
            applicationEventPublisher.publishEvent(ProductLikeCanceledEvent(userId = USER_ID, productId = PRODUCT_ID))
            applicationEventPublisher.publishEvent(
                OrderCreatedEvent(
                    orderId = seededOrderId,
                    userId = USER_ID,
                    productIds = listOf(PRODUCT_ID, SECOND_PRODUCT_ID),
                ),
            )
            applicationEventPublisher.publishEvent(
                PaymentSucceededEvent(
                    paymentId = PAYMENT_SUCCESS_ID,
                    orderId = seededOrderId,
                    userId = USER_ID,
                ),
            )
            applicationEventPublisher.publishEvent(
                PaymentFailedEvent(
                    paymentId = PAYMENT_FAILURE_ID,
                    orderId = FAILED_ORDER_ID,
                    userId = USER_ID,
                    reasonCode = PaymentReasonCode.TIMEOUT_UNCERTAIN.name,
                ),
            )

            assertThat(output.out)
                .doesNotContain("action=product_like_registered")
                .doesNotContain("action=product_like_canceled")
                .doesNotContain("action=order_created")
                .doesNotContain("action=payment_succeeded")
                .doesNotContain("action=payment_failed")
        }

        assertThat(output.out)
            .contains("action=product_like_registered")
            .contains("userId=$USER_ID")
            .contains("productId=$PRODUCT_ID")
            .contains("action=product_like_canceled")
            .contains("action=order_created")
            .contains("orderId=$seededOrderId")
            .contains("productIds=[$PRODUCT_ID, $SECOND_PRODUCT_ID]")
            .contains("action=payment_succeeded")
            .contains("paymentId=$PAYMENT_SUCCESS_ID")
            .contains("action=payment_failed")
            .contains("paymentId=$PAYMENT_FAILURE_ID")
            .contains("reasonCode=TIMEOUT_UNCERTAIN")
    }

    @Test
    @DisplayName("mutating user activity events are not logged when transaction rolls back")
    fun log_mutatingEvents_notLogged_onRollback(output: CapturedOutput) {
        val seededOrderId = seedOrder("activity-test-order-on-rollback")

        assertThrows<RuntimeException> {
            transactionTemplate.executeWithoutResult {
                applicationEventPublisher.publishEvent(ProductLikeRegisteredEvent(userId = USER_ID, productId = PRODUCT_ID))
                applicationEventPublisher.publishEvent(ProductLikeCanceledEvent(userId = USER_ID, productId = PRODUCT_ID))
                applicationEventPublisher.publishEvent(
                    OrderCreatedEvent(
                        orderId = seededOrderId,
                        userId = USER_ID,
                        productIds = listOf(PRODUCT_ID, SECOND_PRODUCT_ID),
                    ),
                )
                applicationEventPublisher.publishEvent(
                    PaymentSucceededEvent(
                        paymentId = PAYMENT_SUCCESS_ID,
                        orderId = seededOrderId,
                        userId = USER_ID,
                    ),
                )
                applicationEventPublisher.publishEvent(
                PaymentFailedEvent(
                    paymentId = PAYMENT_FAILURE_ID,
                    orderId = FAILED_ORDER_ID,
                    userId = USER_ID,
                    reasonCode = PaymentReasonCode.TIMEOUT_UNCERTAIN.name,
                ),
                )
                throw RuntimeException("rollback")
            }
        }

        assertThat(output.out)
            .doesNotContain("action=product_like_registered")
            .doesNotContain("action=product_like_canceled")
            .doesNotContain("action=order_created")
            .doesNotContain("action=payment_succeeded")
            .doesNotContain("action=payment_failed")
    }

    private fun seedOrder(idempotencyKey: String): Long {
        val order = Order.create(
            userId = USER_ID,
            idempotencyKey = IdempotencyKey(idempotencyKey),
            items = listOf(
                OrderItem.create(
                    snapshot = OrderSnapshot(
                        productId = PRODUCT_ID,
                        productName = "activity-test-product",
                        brandId = 1L,
                        brandName = "activity-test-brand",
                        regularPrice = Money(BigDecimal("10000")),
                        sellingPrice = Money(BigDecimal("8000")),
                        thumbnailUrl = null,
                    ),
                    quantity = Quantity(1),
                ),
            ),
        )

        return orderRepository.save(order).id!!
    }

    companion object {
        private const val USER_ID = 1L
        private const val PRODUCT_ID = 100L
        private const val SECOND_PRODUCT_ID = 101L
        private const val FAILED_ORDER_ID = 201L
        private const val PAYMENT_SUCCESS_ID = 300L
        private const val PAYMENT_FAILURE_ID = 301L
    }
}
