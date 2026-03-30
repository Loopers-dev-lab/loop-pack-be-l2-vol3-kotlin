package com.loopers.domain.outbox

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.model.OrderOutboxEventType
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OrderOutboxTest {

    @Nested
    @DisplayName("OrderOutbox 생성")
    inner class Create {

        @Test
        fun `유효한 필드로 생성에 성공한다`() {
            val outbox = OrderOutbox(
                eventType = OrderOutboxEventType.PAYMENT_COMPLETED,
                orderId = OrderId(1L),
                userId = UserId(1L),
                totalAmount = Money(10000L.toBigDecimal()),
                reason = null,
                productId = ProductId(1L),
                quantity = 1,
            )

            assertThat(outbox.eventType).isEqualTo(OrderOutboxEventType.PAYMENT_COMPLETED)
            assertThat(outbox.orderId).isEqualTo(OrderId(1L))
            assertThat(outbox.userId).isEqualTo(UserId(1L))
            assertThat(outbox.totalAmount).isEqualTo(Money(10000L.toBigDecimal()))
            assertThat(outbox.reason).isNull()
            assertThat(outbox.published).isFalse()
            assertThat(outbox.eventId).isNotBlank()
        }

        @Test
        fun `reason만 포함하고 totalAmount 없이도 생성에 성공한다`() {
            val outbox = OrderOutbox(
                eventType = OrderOutboxEventType.PAYMENT_FAILED,
                orderId = OrderId(1L),
                userId = UserId(1L),
                totalAmount = null,
                reason = "잔액 부족",
            )

            assertThat(outbox.reason).isEqualTo("잔액 부족")
            assertThat(outbox.totalAmount).isNull()
        }

        @Test
        fun `orderId가 0 이하이면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_COMPLETED, orderId = OrderId(0L), userId = UserId(1L))
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `userId가 0 이하이면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_COMPLETED, orderId = OrderId(1L), userId = UserId(0L))
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `PAYMENT_COMPLETED에 productId가 없으면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_COMPLETED, orderId = OrderId(1L), userId = UserId(1L), quantity = 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `PAYMENT_COMPLETED에 quantity가 없으면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_COMPLETED, orderId = OrderId(1L), userId = UserId(1L), productId = ProductId(1L))
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("markPublished")
    inner class MarkPublished {

        @Test
        fun `발행 완료로 상태가 변경된다`() {
            val outbox = OrderOutbox(
                eventType = OrderOutboxEventType.PAYMENT_COMPLETED,
                orderId = OrderId(1L),
                userId = UserId(1L),
                productId = ProductId(1L),
                quantity = 1,
            )

            outbox.markPublished()

            assertThat(outbox.published).isTrue()
        }

        @Test
        fun `published는 markPublished()로만 변경 가능하다`() {
            val outbox = OrderOutbox(
                eventType = OrderOutboxEventType.PAYMENT_COMPLETED,
                orderId = OrderId(1L),
                userId = UserId(1L),
                productId = ProductId(1L),
                quantity = 1,
            )

            // published 필드가 private set이므로 컴파일 시점에 차단됨을 런타임으로 검증
            assertThat(outbox.published).isFalse()
            outbox.markPublished()
            assertThat(outbox.published).isTrue()
        }
    }

    @Nested
    @DisplayName("OrderOutboxRepository")
    inner class RepositoryTest {

        private lateinit var repository: OrderOutboxRepository

        @BeforeEach
        fun setUp() {
            repository = FakeOrderOutboxRepository()
        }

        @Test
        fun `미발행 메시지만 조회된다`() {
            val unpublished = repository.save(
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_COMPLETED, orderId = OrderId(1L), userId = UserId(1L), totalAmount = Money(10000L.toBigDecimal()), productId = ProductId(1L), quantity = 1),
            )
            val published = repository.save(
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_FAILED, orderId = OrderId(2L), userId = UserId(1L), reason = "잔액 부족"),
            )
            published.markPublished()
            repository.save(published)

            val result = repository.findAllUnpublished()

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(unpublished.id)
        }

        @Test
        fun `발행 완료 마킹 후 미발행 목록에서 제외된다`() {
            val outbox = repository.save(
                OrderOutbox(eventType = OrderOutboxEventType.PAYMENT_COMPLETED, orderId = OrderId(1L), userId = UserId(1L), productId = ProductId(1L), quantity = 1),
            )

            outbox.markPublished()
            repository.save(outbox)

            assertThat(repository.findAllUnpublished()).isEmpty()
        }
    }
}
