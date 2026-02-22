package com.loopers.domain.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class OrderItemModelTest {
    @DisplayName("주문 아이템 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 값이 주어지면, amount가 자동 계산된다.")
        @Test
        fun calculatesAmount_whenCreated() {
            // act
            val item = OrderItemModel(
                productId = 1L,
                productName = "감성 티셔츠",
                productPrice = 39000,
                brandName = "루퍼스",
                quantity = 3,
            )

            // assert
            assertAll(
                { assertThat(item.productId).isEqualTo(1L) },
                { assertThat(item.productName).isEqualTo("감성 티셔츠") },
                { assertThat(item.productPrice).isEqualTo(39000L) },
                { assertThat(item.brandName).isEqualTo("루퍼스") },
                { assertThat(item.quantity).isEqualTo(3) },
                { assertThat(item.amount).isEqualTo(117000L) },
            )
        }
    }
}
