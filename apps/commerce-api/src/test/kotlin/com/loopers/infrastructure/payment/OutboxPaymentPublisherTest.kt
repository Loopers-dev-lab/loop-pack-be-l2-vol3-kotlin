package com.loopers.infrastructure.payment

import com.loopers.application.outbox.FakeOutboxEventRepository
import com.loopers.infrastructure.outbox.OutboxEventPublisherImpl
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Outbox 기반 Payment Publisher 테스트")
class OutboxPaymentPublisherTest {

    private lateinit var outboxRepository: FakeOutboxEventRepository
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        outboxRepository = FakeOutboxEventRepository()
        objectMapper = ObjectMapper()
    }

    @Nested
    @DisplayName("OutboxCompleteOrderCommandPublisher")
    inner class CompleteOrderPublisherTest {
        private lateinit var publisher: OutboxCompleteOrderCommandPublisher

        @BeforeEach
        fun setUp() {
            publisher = OutboxCompleteOrderCommandPublisher(
                OutboxEventPublisherImpl(outboxRepository, objectMapper),
            )
        }

        @Test
        @DisplayName("publish 호출 시 payment.succeeded 토픽으로 outbox 이벤트가 저장된다")
        fun `publish 시 outbox에 저장된다`() {
            // act
            publisher.publish(orderId = 1L)

            // assert
            val events = outboxRepository.findAll()
            assertThat(events).hasSize(1)
            assertThat(events[0].topic).isEqualTo("payment.succeeded")
            assertThat(events[0].aggregateType).isEqualTo("ORDER")
            assertThat(events[0].aggregateId).isEqualTo("1")
            assertThat(events[0].partitionKey).isEqualTo("1")
            assertThat(events[0].eventType).isEqualTo("PaymentSucceeded")

            val payload = objectMapper.readValue(events[0].payload, Map::class.java)
            assertThat(payload["orderId"]).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("OutboxPaymentCompensationPublisher")
    inner class CompensationPublisherTest {
        private lateinit var publisher: OutboxPaymentCompensationPublisher

        @BeforeEach
        fun setUp() {
            publisher = OutboxPaymentCompensationPublisher(
                OutboxEventPublisherImpl(outboxRepository, objectMapper),
            )
        }

        @Test
        @DisplayName("publish 호출 시 payment.failed 토픽으로 outbox 이벤트가 저장된다")
        fun `publish 시 outbox에 저장된다`() {
            // act
            publisher.publish(orderId = 10L, paymentId = 99L)

            // assert
            val events = outboxRepository.findAll()
            assertThat(events).hasSize(1)
            assertThat(events[0].topic).isEqualTo("payment.failed")
            assertThat(events[0].aggregateType).isEqualTo("ORDER")
            assertThat(events[0].aggregateId).isEqualTo("10")
            assertThat(events[0].eventType).isEqualTo("PaymentFailed")

            val payload = objectMapper.readValue(events[0].payload, Map::class.java)
            assertThat(payload["orderId"]).isEqualTo(10)
            assertThat(payload["paymentId"]).isEqualTo(99)
        }

        @Test
        @DisplayName("partitionKey는 orderId로 설정된다")
        fun `partitionKey는 orderId이다`() {
            // act
            publisher.publish(orderId = 42L, paymentId = 7L)

            // assert
            assertThat(outboxRepository.findAll()[0].partitionKey).isEqualTo("42")
        }
    }
}
