package com.loopers.interfaces.support.scheduler

import com.loopers.application.event.FakeOutboxEventPublisher
import com.loopers.application.event.RelayOutboxUseCase
import com.loopers.domain.outbox.FakeCatalogOutboxRepository
import com.loopers.domain.outbox.FakeCouponOutboxRepository
import com.loopers.domain.outbox.FakeOrderOutboxRepository
import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.model.CouponOutbox
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import com.loopers.domain.outbox.repository.CouponOutboxRepository
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate

class OutboxRelaySchedulerTest {

    private lateinit var catalogOutboxRepository: CatalogOutboxRepository
    private lateinit var orderOutboxRepository: OrderOutboxRepository
    private lateinit var couponOutboxRepository: CouponOutboxRepository
    private lateinit var eventPublisher: FakeOutboxEventPublisher
    private lateinit var scheduler: OutboxRelayScheduler

    @BeforeEach
    fun setUp() {
        catalogOutboxRepository = FakeCatalogOutboxRepository()
        orderOutboxRepository = FakeOrderOutboxRepository()
        couponOutboxRepository = FakeCouponOutboxRepository()
        eventPublisher = FakeOutboxEventPublisher()
        val noOpTransactionManager = object : PlatformTransactionManager {
            override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
            override fun commit(status: TransactionStatus) {}
            override fun rollback(status: TransactionStatus) {}
        }
        val useCase = RelayOutboxUseCase(
            catalogOutboxRepository = catalogOutboxRepository,
            orderOutboxRepository = orderOutboxRepository,
            couponOutboxRepository = couponOutboxRepository,
            outboxEventPublisher = eventPublisher,
            transactionTemplate = TransactionTemplate(noOpTransactionManager),
        )
        scheduler = OutboxRelayScheduler(useCase)
    }

    @Nested
    @DisplayName("relay 실행 시")
    inner class Relay {

        @Test
        @DisplayName("미발행 CatalogOutbox 메시지가 Kafka로 발행되고 published로 마킹된다")
        fun relayCatalogEvents() {
            catalogOutboxRepository.save(
                CatalogOutbox(eventType = "LIKE_ADDED", productId = 1L, userId = 2L),
            )

            scheduler.relay()

            assertThat(catalogOutboxRepository.findAllUnpublished()).isEmpty()
            assertThat(eventPublisher.published).hasSize(1)
            assertThat(eventPublisher.published[0].first).isEqualTo("catalog-events")
            assertThat(eventPublisher.published[0].second).isEqualTo("1")
        }

        @Test
        @DisplayName("미발행 OrderOutbox 메시지가 Kafka로 발행되고 published로 마킹된다")
        fun relayOrderEvents() {
            orderOutboxRepository.save(
                OrderOutbox(
                    eventType = "PAYMENT_COMPLETED",
                    orderId = 10L,
                    userId = 100L,
                    totalAmount = 50000L,
                ),
            )

            scheduler.relay()

            assertThat(orderOutboxRepository.findAllUnpublished()).isEmpty()
            assertThat(eventPublisher.published).hasSize(1)
            assertThat(eventPublisher.published[0].first).isEqualTo("order-events")
            assertThat(eventPublisher.published[0].second).isEqualTo("10")
        }

        @Test
        @DisplayName("미발행 CouponOutbox 메시지가 Kafka로 발행되고 published로 마킹된다")
        fun relayCouponEvents() {
            couponOutboxRepository.save(
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 5L, userId = 100L),
            )

            scheduler.relay()

            assertThat(couponOutboxRepository.findAllUnpublished()).isEmpty()
            assertThat(eventPublisher.published).hasSize(1)
            assertThat(eventPublisher.published[0].first).isEqualTo("coupon-issue-requests")
            assertThat(eventPublisher.published[0].second).isEqualTo("5")
        }

        @Test
        @DisplayName("이미 발행된 메시지는 다시 발행하지 않는다")
        fun skipAlreadyPublished() {
            val outbox = catalogOutboxRepository.save(
                CatalogOutbox(eventType = "LIKE_ADDED", productId = 1L, userId = 2L),
            )
            outbox.markPublished()
            catalogOutboxRepository.save(outbox)

            scheduler.relay()

            assertThat(eventPublisher.published).isEmpty()
        }

        @Test
        @DisplayName("여러 타입의 미발행 메시지가 모두 처리된다")
        fun relayAllTypes() {
            catalogOutboxRepository.save(
                CatalogOutbox(eventType = "PRODUCT_VIEWED", productId = 1L, userId = null),
            )
            orderOutboxRepository.save(
                OrderOutbox(eventType = "PAYMENT_COMPLETED", orderId = 2L, userId = 100L, totalAmount = 30000L),
            )
            couponOutboxRepository.save(
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 3L, userId = 100L),
            )

            scheduler.relay()

            assertThat(eventPublisher.published).hasSize(3)
            assertThat(catalogOutboxRepository.findAllUnpublished()).isEmpty()
            assertThat(orderOutboxRepository.findAllUnpublished()).isEmpty()
            assertThat(couponOutboxRepository.findAllUnpublished()).isEmpty()
        }
    }
}
