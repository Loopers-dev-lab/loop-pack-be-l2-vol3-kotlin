package com.loopers.domain.order

import com.loopers.domain.product.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OrderTest {

    @Test
    fun `create로 생성한 Order의 persistenceId는 null이어야 한다`() {
        val order = createOrder()

        assertThat(order.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 Order의 상태는 PENDING이어야 한다`() {
        val order = createOrder()

        assertThat(order.status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `create시 totalAmount가 자동 계산되어야 한다`() {
        val order = createOrder()

        assertThat(order.totalAmount.amount).isEqualTo(PRICE * QUANTITY)
    }

    @Test
    fun `create시 orderedAt이 설정되어야 한다`() {
        val order = createOrder()

        assertThat(order.orderedAt).isNotNull()
    }

    @Test
    fun `빈 주문 항목의 경우 create가 실패해야 한다`() {
        assertThatThrownBy {
            Order.create(userId = USER_ID, items = emptyList())
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `reconstitute로 생성한 Order는 persistenceId를 가져야 한다`() {
        val order = Order.reconstitute(
            persistenceId = 1L,
            refUserId = USER_ID,
            status = OrderStatus.PENDING,
            totalAmount = Money(PRICE * QUANTITY),
            orderedAt = java.time.ZonedDateTime.now(),
            items = listOf(createOrderItem()),
        )

        assertThat(order.persistenceId).isEqualTo(1L)
    }

    @Test
    fun `PENDING 상태의 경우 cancel이 CANCELLED 상태를 반환해야 한다`() {
        val order = createOrder()

        val cancelled = order.cancel()

        assertThat(cancelled.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `cancel 호출시 새 Order 인스턴스를 반환해야 한다`() {
        val order = createOrder()

        val cancelled = order.cancel()

        assertThat(cancelled).isNotSameAs(order)
    }

    @Test
    fun `COMPLETED 상태의 경우 cancel이 실패해야 한다`() {
        val order = createOrder().complete()

        assertThatThrownBy { order.cancel() }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `CANCELLED 상태의 경우 cancel이 실패해야 한다`() {
        val order = createOrder().cancel()

        assertThatThrownBy { order.cancel() }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `PENDING 상태의 경우 complete가 COMPLETED 상태를 반환해야 한다`() {
        val order = createOrder()

        val completed = order.complete()

        assertThat(completed.status).isEqualTo(OrderStatus.COMPLETED)
    }

    @Test
    fun `complete 호출시 새 Order 인스턴스를 반환해야 한다`() {
        val order = createOrder()

        val completed = order.complete()

        assertThat(completed).isNotSameAs(order)
    }

    @Test
    fun `COMPLETED 상태의 경우 complete가 실패해야 한다`() {
        val order = createOrder().complete()

        assertThatThrownBy { order.complete() }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `CANCELLED 상태의 경우 complete가 실패해야 한다`() {
        val order = createOrder().cancel()

        assertThatThrownBy { order.complete() }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `본인 userId의 경우 isOwnedBy가 true를 반환해야 한다`() {
        val order = createOrder()

        assertThat(order.isOwnedBy(USER_ID)).isTrue()
    }

    @Test
    fun `타인 userId의 경우 isOwnedBy가 false를 반환해야 한다`() {
        val order = createOrder()

        assertThat(order.isOwnedBy(OTHER_USER_ID)).isFalse()
    }

    @Test
    fun `본인 userId의 경우 assertOwnedBy가 성공해야 한다`() {
        val order = createOrder()

        assertDoesNotThrow { order.assertOwnedBy(USER_ID) }
    }

    @Test
    fun `타인 userId의 경우 assertOwnedBy가 OrderException을 던져야 한다`() {
        val order = createOrder()

        assertThatThrownBy { order.assertOwnedBy(OTHER_USER_ID) }
            .isInstanceOf(OrderException::class.java)
    }

    @Test
    fun `PENDING 상태의 경우 canCancel이 true를 반환해야 한다`() {
        val order = createOrder()

        assertThat(order.canCancel()).isTrue()
    }

    @Test
    fun `COMPLETED 상태의 경우 canCancel이 false를 반환해야 한다`() {
        val order = createOrder().complete()

        assertThat(order.canCancel()).isFalse()
    }

    @Test
    fun `CANCELLED 상태의 경우 canCancel이 false를 반환해야 한다`() {
        val order = createOrder().cancel()

        assertThat(order.canCancel()).isFalse()
    }

    @Test
    fun `여러 주문 항목의 경우 totalAmount가 모든 항목의 소계 합이어야 한다`() {
        val item1 = OrderItem.reconstitute(
            persistenceId = 1L,
            refProductId = PRODUCT_ID,
            productName = PRODUCT_NAME,
            brandName = BRAND_NAME,
            price = Money(10000),
            quantity = 2,
        )
        val item2 = OrderItem.reconstitute(
            persistenceId = 2L,
            refProductId = 20L,
            productName = "에어포스 1",
            brandName = BRAND_NAME,
            price = Money(15000),
            quantity = 3,
        )

        val order = Order.create(userId = USER_ID, items = listOf(item1, item2))

        assertThat(order.totalAmount.amount).isEqualTo(10000 * 2 + 15000 * 3)
    }

    private fun createOrderItem(): OrderItem = OrderItem.reconstitute(
        persistenceId = 1L,
        refProductId = PRODUCT_ID,
        productName = PRODUCT_NAME,
        brandName = BRAND_NAME,
        price = Money(PRICE),
        quantity = QUANTITY,
    )

    private fun createOrder(): Order = Order.create(
        userId = USER_ID,
        items = listOf(createOrderItem()),
    )

    companion object {
        private const val USER_ID = 1L
        private const val OTHER_USER_ID = 2L
        private const val PRODUCT_ID = 10L
        private const val PRODUCT_NAME = "에어맥스 90"
        private const val BRAND_NAME = "나이키"
        private const val PRICE = 10000L
        private const val QUANTITY = 2
    }
}
