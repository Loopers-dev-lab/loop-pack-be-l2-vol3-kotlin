package com.loopers.domain.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OrderItemModelTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            assertDoesNotThrow {
                OrderItemModel(
                    orderId = 1L,
                    brandId = 1L,
                    productId = 1L,
                    quantity = Quantity(2),
                    unitPrice = Price(10000),
                    productName = ProductName("뉴발란스 991"),
                    imageUrl = ImageUrl("/tmp/test.png"),
                )
            }
        }
    }

    @Nested
    inner class SubTotal {

        @Test
        fun `수량과 단가를 곱한 소계를 반환한다`() {
            val orderItem = OrderItemModel(
                orderId = 1L,
                brandId = 1L,
                productId = 1L,
                quantity = Quantity(3),
                unitPrice = Price(10000),
                productName = ProductName("뉴발란스 991"),
                imageUrl = ImageUrl("/tmp/test.png"),
            )

            assertThat(orderItem.subTotal).isEqualTo(30000L)
        }
    }
}
