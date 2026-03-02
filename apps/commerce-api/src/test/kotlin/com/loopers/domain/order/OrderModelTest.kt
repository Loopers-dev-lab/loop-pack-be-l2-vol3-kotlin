package com.loopers.domain.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OrderModelTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            assertDoesNotThrow {
                OrderModel(
                    userId = 1L,
                    totalAmount = TotalAmount(10000),
                    status = OrderStatus.PENDING_PAYMENT,
                )
            }
        }
    }

    @Nested
    inner class Pay {

        @Test
        fun `결제 완료 처리 시 상태가 PAID로 변경된다`() {
            val order = OrderModel(
                userId = 1L,
                totalAmount = TotalAmount(10000),
                status = OrderStatus.PENDING_PAYMENT,
            )

            order.pay()

            assertThat(order.status).isEqualTo(OrderStatus.PAID)
        }
    }
}
