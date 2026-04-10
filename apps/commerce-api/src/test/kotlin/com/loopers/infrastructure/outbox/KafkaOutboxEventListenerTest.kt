package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import com.loopers.support.event.user.CouponIssueRequestedEvent
import com.loopers.support.event.user.ProductDetailViewedEvent
import com.loopers.support.event.user.ProductLikeCanceledEvent
import com.loopers.support.event.user.ProductLikeRegisteredEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("KafkaOutboxEventListener")
class KafkaOutboxEventListenerTest {
    private val objectMapper = jacksonObjectMapper()
    private val kafkaOutboxJpaRepository: KafkaOutboxJpaRepository = mock()
    private val orderRepository: OrderRepository = mock()
    private val listener = KafkaOutboxEventListener(kafkaOutboxJpaRepository, orderRepository, objectMapper)

    private val now = ZonedDateTime.of(2026, 3, 20, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun order(): Order = Order.retrieve(
        id = ORDER_ID,
        userId = USER_ID,
        idempotencyKey = IdempotencyKey("order-key-001"),
        status = Order.Status.CREATED,
        items = listOf(
            OrderItem.retrieve(
                id = 1L,
                snapshot = OrderSnapshot(
                    productId = PRODUCT_ID,
                    productName = "테스트 상품",
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    regularPrice = Money(BigDecimal("10000")),
                    sellingPrice = Money(BigDecimal("8000")),
                    thumbnailUrl = null,
                ),
                quantity = Quantity(2),
            ),
        ),
        createdAt = now,
    )

    @Nested
    @DisplayName("catalog 이벤트를 outbox로 저장한다")
    inner class WhenCatalogEvents {
        @Test
        @DisplayName("조회 이벤트를 catalog-events로 저장한다")
        fun handle_productDetailViewed() {
            listener.handle(ProductDetailViewedEvent(productId = PRODUCT_ID))

            verify(kafkaOutboxJpaRepository).saveAndFlush(
                check { entity ->
                    assertThat(entity.topic).isEqualTo("catalog-events")
                    assertThat(entity.eventKey).isEqualTo(PRODUCT_ID.toString())
                    assertThat(entity.eventType).isEqualTo(KafkaEventType.PRODUCT_DETAIL_VIEWED)
                    assertThat(entity.aggregateId).isEqualTo(PRODUCT_ID)
                    assertThat(objectMapper.readTree(entity.payload).get("productId").asLong()).isEqualTo(PRODUCT_ID)
                },
            )
        }

        @Test
        @DisplayName("좋아요 등록 이벤트를 catalog-events로 저장한다")
        fun handle_productLikeRegistered() {
            listener.handle(ProductLikeRegisteredEvent(userId = USER_ID, productId = PRODUCT_ID))

            verify(kafkaOutboxJpaRepository).saveAndFlush(
                check { entity ->
                    assertThat(entity.eventType).isEqualTo(KafkaEventType.PRODUCT_LIKE_REGISTERED)
                    assertThat(objectMapper.readTree(entity.payload).get("userId").asLong()).isEqualTo(USER_ID)
                },
            )
        }

        @Test
        @DisplayName("좋아요 취소 이벤트를 catalog-events로 저장한다")
        fun handle_productLikeCanceled() {
            listener.handle(ProductLikeCanceledEvent(userId = USER_ID, productId = PRODUCT_ID))

            verify(kafkaOutboxJpaRepository).saveAndFlush(
                check { entity ->
                    assertThat(entity.eventType).isEqualTo(KafkaEventType.PRODUCT_LIKE_CANCELED)
                },
            )
        }
    }

    @Nested
    @DisplayName("order 이벤트를 outbox로 저장한다")
    inner class WhenOrderEvents {
        @Test
        @DisplayName("payment succeeded 이벤트에 order item quantity를 포함한다")
        fun handle_paymentSucceeded() {
            val savedOrder = order()
            org.mockito.BDDMockito.given(orderRepository.findById(ORDER_ID)).willReturn(savedOrder)

            listener.handle(
                PaymentSucceededEvent(
                    paymentId = PAYMENT_ID,
                    orderId = ORDER_ID,
                    userId = USER_ID,
                ),
            )

            verify(kafkaOutboxJpaRepository).saveAndFlush(
                check { entity ->
                    assertThat(entity.topic).isEqualTo("order-events")
                    assertThat(entity.eventType).isEqualTo(KafkaEventType.PAYMENT_SUCCEEDED)
                    val payload = objectMapper.readTree(entity.payload)
                    assertThat(payload.get("paymentId").asLong()).isEqualTo(PAYMENT_ID)
                    assertThat(payload.get("items")).hasSize(1)
                    assertThat(payload.get("items")[0].get("productId").asLong()).isEqualTo(PRODUCT_ID)
                    assertThat(payload.get("items")[0].get("quantity").asInt()).isEqualTo(2)
                    assertThat(payload.get("items")[0].get("sellingPrice").asLong()).isEqualTo(8000L)
                },
            )
        }

        @Test
        @DisplayName("internal-only 이벤트는 outbox에 저장하지 않는다")
        fun skip_internalOnlyEvents() {
            listener.handle(
                OrderCreatedEvent(
                    orderId = ORDER_ID,
                    userId = USER_ID,
                    productIds = listOf(PRODUCT_ID),
                ),
            )
            listener.handle(
                PaymentFailedEvent(
                    paymentId = PAYMENT_ID,
                    orderId = ORDER_ID,
                    userId = USER_ID,
                    reasonCode = com.loopers.domain.payment.PaymentReasonCode.TIMEOUT_UNCERTAIN.name,
                ),
            )

            verify(kafkaOutboxJpaRepository, never()).saveAndFlush(check<KafkaOutboxEntity> { })
        }
    }

    @Nested
    @DisplayName("coupon issue 요청 이벤트를 outbox로 저장한다")
    inner class WhenCouponIssueEvents {
        @Test
        @DisplayName("coupon-issue-requests topic에 requestId와 couponId를 저장한다")
        fun handle_couponIssueRequested() {
            listener.handle(
                CouponIssueRequestedEvent(
                    requestId = REQUEST_ID,
                    couponId = PRODUCT_ID,
                    userId = USER_ID,
                ),
            )

            verify(kafkaOutboxJpaRepository).saveAndFlush(
                check { entity ->
                    assertThat(entity.topic).isEqualTo("coupon-issue-requests")
                    assertThat(entity.eventKey).isEqualTo(PRODUCT_ID.toString())
                    assertThat(entity.eventType).isEqualTo(KafkaEventType.COUPON_ISSUE_REQUESTED)
                    val payload = objectMapper.readTree(entity.payload)
                    assertThat(payload.get("requestId").asLong()).isEqualTo(REQUEST_ID)
                    assertThat(payload.get("couponId").asLong()).isEqualTo(PRODUCT_ID)
                    assertThat(payload.get("userId").asLong()).isEqualTo(USER_ID)
                },
            )
        }
    }

    companion object {
        private const val PRODUCT_ID = 100L
        private const val ORDER_ID = 200L
        private const val PAYMENT_ID = 300L
        private const val REQUEST_ID = 400L
        private const val USER_ID = 10L
    }
}
