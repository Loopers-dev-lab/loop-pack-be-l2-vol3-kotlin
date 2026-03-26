package com.loopers.domain.useractionlog

import com.loopers.CommerceApiApplication
import com.loopers.domain.order.event.OrderCreatedEvent
import com.loopers.domain.payment.event.PaymentCallbackProcessedEvent
import com.loopers.domain.payment.event.PaymentRequestedEvent
import com.loopers.infrastructure.useractionlog.UserActionLogJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest(classes = [CommerceApiApplication::class])
@DisplayName("Order/Payment 사용자 액션 로그 이벤트 리스너")
class OrderPaymentUserActionLogEventListenerTest @Autowired constructor(
    private val eventPublisher: ApplicationEventPublisher,
    transactionManager: PlatformTransactionManager,
    private val userActionLogJpaRepository: UserActionLogJpaRepository,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @AfterEach
    fun tearDown() {
        userActionLogJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("OrderCreatedEvent는 커밋 이후 order.created 로그를 1건 저장한다")
    fun persistsOrderCreatedAfterCommit() {
        val dedupeKey = "order.created:101"

        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(
                OrderCreatedEvent(
                    source = this,
                    orderId = 101L,
                    lineItems = listOf(
                        com.loopers.domain.order.event.OrderLineItem(productId = 1L, quantity = 2),
                    ),
                    dedupeKey = dedupeKey,
                ),
            )
        }

        assertThat(userActionLogJpaRepository.countByActionType("order.created")).isEqualTo(1)
        assertThat(userActionLogJpaRepository.countByDedupeKey(dedupeKey)).isEqualTo(1)
    }

    @Test
    @DisplayName("PaymentRequestedEvent는 커밋 이후 payment.requested 로그를 1건 저장한다")
    fun persistsPaymentRequestedAfterCommit() {
        val dedupeKey = "payment.requested:txn-commit-1"

        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(
                PaymentRequestedEvent(
                    userId = 11L,
                    orderId = 102L,
                    transactionId = "txn-commit-1",
                    amount = 12000L,
                    receiptStatus = "PENDING",
                    dedupeKey = dedupeKey,
                ),
            )
        }

        assertThat(userActionLogJpaRepository.countByActionType("payment.requested")).isEqualTo(1)
        assertThat(userActionLogJpaRepository.countByDedupeKey(dedupeKey)).isEqualTo(1)
    }

    @Test
    @DisplayName("PaymentCallbackProcessedEvent는 커밋 이후 payment.callback.processed 로그를 1건 저장한다")
    fun persistsPaymentCallbackProcessedAfterCommit() {
        val dedupeKey = "payment.callback.processed:txn-callback-1:COMPLETED"

        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(
                PaymentCallbackProcessedEvent(
                    orderId = 103L,
                    status = "COMPLETED",
                    transactionId = "txn-callback-1",
                    amount = 15000L,
                    reason = null,
                    dedupeKey = dedupeKey,
                ),
            )
        }

        assertThat(userActionLogJpaRepository.countByActionType("payment.callback.processed")).isEqualTo(1)
        assertThat(userActionLogJpaRepository.countByDedupeKey(dedupeKey)).isEqualTo(1)
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 user-action-log를 저장하지 않는다")
    fun doesNotPersistWhenTransactionRollsBack() {
        val dedupeKey = "order.created:rollback"

        transactionTemplate.executeWithoutResult { status ->
            eventPublisher.publishEvent(
                OrderCreatedEvent(
                    source = this,
                    orderId = 104L,
                    lineItems = listOf(
                        com.loopers.domain.order.event.OrderLineItem(productId = 1L, quantity = 1),
                    ),
                    dedupeKey = dedupeKey,
                ),
            )
            status.setRollbackOnly()
        }

        assertThat(userActionLogJpaRepository.countByActionType("order.created")).isZero()
        assertThat(userActionLogJpaRepository.countByDedupeKey(dedupeKey)).isZero()
    }

    @Test
    @DisplayName("동일 dedupeKey가 중복 전달되면 append-if-absent로 1건만 저장한다")
    fun persistsOnlyOnceForDuplicateDedupeKey() {
        val dedupeKey = "payment.requested:txn-duplicate-1"
        val event = PaymentRequestedEvent(
            userId = 13L,
            orderId = 105L,
            transactionId = "txn-duplicate-1",
            amount = 17000L,
            receiptStatus = "PENDING",
            dedupeKey = dedupeKey,
        )

        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(event)
        }
        transactionTemplate.executeWithoutResult {
            eventPublisher.publishEvent(event)
        }

        assertThat(userActionLogJpaRepository.countByDedupeKey(dedupeKey)).isEqualTo(1)
        assertThat(userActionLogJpaRepository.countByActionType("payment.requested")).isEqualTo(1)
    }
}
