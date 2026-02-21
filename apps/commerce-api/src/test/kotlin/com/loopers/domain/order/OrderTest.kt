package com.loopers.domain.order

import com.loopers.domain.order.entity.Order
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderTest {

    @Nested
    @DisplayName("Order.create 시")
    inner class Create {

        @Test
        @DisplayName("정상적인 주문이 생성된다")
        fun create_validOrder_success() {
            // act
            val order = Order.create(1L, BigDecimal("258000"))

            // assert
            assertThat(order.refUserId).isEqualTo(1L)
            assertThat(order.status).isEqualTo(Order.OrderStatus.CREATED)
            assertThat(order.totalPrice).isEqualByComparingTo(BigDecimal("258000"))
        }
    }
}
