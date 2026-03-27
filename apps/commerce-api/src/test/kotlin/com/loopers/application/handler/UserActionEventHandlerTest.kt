package com.loopers.application.handler

import com.loopers.application.handler.useraction.PublishProductMetricsCommandHandler
import com.loopers.application.handler.useraction.UserActionEventHandler
import com.loopers.application.outbox.FakeOutboxEventRepository
import com.loopers.application.useraction.FakeUserActionLogRepository
import com.loopers.domain.common.event.UserActionEvent
import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType
import com.loopers.infrastructure.outbox.OutboxEventPublisherImpl
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("유저 행동 이벤트 핸들러 테스트")
class UserActionEventHandlerTest {

    private lateinit var repository: FakeUserActionLogRepository
    private lateinit var outboxRepository: FakeOutboxEventRepository
    private lateinit var handler: UserActionEventHandler

    @BeforeEach
    fun setUp() {
        repository = FakeUserActionLogRepository()
        outboxRepository = FakeOutboxEventRepository()
        val outboxPublisher = OutboxEventPublisherImpl(outboxRepository, ObjectMapper())
        handler = UserActionEventHandler(
            repository,
            PublishProductMetricsCommandHandler(outboxPublisher),
        )
    }

    @Nested
    @DisplayName("UserActionEvent 수신")
    inner class OnUserActionEvent {

        @Test
        @DisplayName("VIEW 이벤트 수신 시 user_action_log에 저장되고 outbox에 발행된다")
        fun `VIEW 이벤트가 저장되고 outbox에 발행된다`() {
            // arrange
            val event = UserActionEvent(
                memberId = 1L,
                actionType = UserActionType.VIEW,
                targetType = UserActionTargetType.PRODUCT,
                targetId = 100L,
            )

            // act
            handler.on(event)

            // assert
            val logs = repository.findAll()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].memberId).isEqualTo(1L)
            assertThat(logs[0].actionType).isEqualTo(UserActionType.VIEW)
            assertThat(logs[0].targetId).isEqualTo(100L)

            val outboxEvents = outboxRepository.findAll()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].topic).isEqualTo("product.action")
            assertThat(outboxEvents[0].partitionKey).isEqualTo("100")
            assertThat(outboxEvents[0].eventType).isEqualTo("UserAction.VIEW")
        }

        @Test
        @DisplayName("LIKE 이벤트 수신 시 저장 및 outbox 발행된다")
        fun `LIKE 이벤트가 저장되고 outbox에 발행된다`() {
            // arrange
            val event = UserActionEvent(
                memberId = 2L,
                actionType = UserActionType.LIKE,
                targetType = UserActionTargetType.PRODUCT,
                targetId = 200L,
            )

            // act
            handler.on(event)

            // assert
            assertThat(repository.findAll()).hasSize(1)
            val outboxEvents = outboxRepository.findAll()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].eventType).isEqualTo("UserAction.LIKE")
            assertThat(outboxEvents[0].partitionKey).isEqualTo("200")
        }

        @Test
        @DisplayName("ORDER 이벤트 수신 시 저장 및 outbox 발행된다")
        fun `ORDER 이벤트가 저장되고 outbox에 발행된다`() {
            // arrange
            val event = UserActionEvent(
                memberId = 3L,
                actionType = UserActionType.ORDER,
                targetType = UserActionTargetType.PRODUCT,
                targetId = 300L,
            )

            // act
            handler.on(event)

            // assert
            assertThat(repository.findAll()).hasSize(1)
            val outboxEvents = outboxRepository.findAll()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].eventType).isEqualTo("UserAction.ORDER")
        }

        @Test
        @DisplayName("여러 이벤트 수신 시 각각 독립적으로 저장되고 outbox에 발행된다")
        fun `여러 이벤트가 독립적으로 저장되고 outbox에 발행된다`() {
            // arrange & act
            handler.on(UserActionEvent(1L, UserActionType.VIEW, UserActionTargetType.PRODUCT, 100L))
            handler.on(UserActionEvent(2L, UserActionType.LIKE, UserActionTargetType.PRODUCT, 200L))
            handler.on(UserActionEvent(1L, UserActionType.ORDER, UserActionTargetType.PRODUCT, 300L))

            // assert
            assertThat(repository.findAll()).hasSize(3)
            assertThat(outboxRepository.findAll()).hasSize(3)
        }
    }
}
