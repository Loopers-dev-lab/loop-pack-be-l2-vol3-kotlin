package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("OrderItem Entity")
class OrderItemTest {

    private fun createSnapshot(
        productId: Long = 1L,
        productName: String = "테스트 상품",
        brandId: Long = 1L,
        brandName: String = "테스트 브랜드",
        regularPrice: Money = Money(BigDecimal("10000")),
        sellingPrice: Money = Money(BigDecimal("8000")),
        thumbnailUrl: String? = null,
    ): OrderSnapshot = OrderSnapshot(
        productId = productId,
        productName = productName,
        brandId = brandId,
        brandName = brandName,
        regularPrice = regularPrice,
        sellingPrice = sellingPrice,
        thumbnailUrl = thumbnailUrl,
    )

    @Nested
    @DisplayName("생성")
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 생성 성공")
        fun create_success() {
            // act
            val item = OrderItem.create(
                snapshot = createSnapshot(),
                quantity = Quantity(3),
            )

            // assert
            assertAll(
                { assertThat(item.id).isNull() },
                { assertThat(item.snapshot.productId).isEqualTo(1L) },
                { assertThat(item.quantity.value).isEqualTo(3) },
            )
        }

        @Test
        @DisplayName("수량 1로 생성 성공 (경계값: 최소)")
        fun create_minimumQuantity() {
            // act
            val item = OrderItem.create(
                snapshot = createSnapshot(),
                quantity = Quantity(1),
            )

            // assert
            assertThat(item.quantity.value).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("주문 수량이 0이면 생성 실패한다")
    inner class WhenQuantityZero {

        @Test
        @DisplayName("수량 0 → 실패")
        fun create_zeroQuantity() {
            val exception = assertThrows<CoreException> {
                OrderItem.create(
                    snapshot = createSnapshot(),
                    quantity = Quantity(0),
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_QUANTITY)
        }
    }

    @Nested
    @DisplayName("영속성 복원")
    inner class Retrieve {

        @Test
        @DisplayName("retrieve로 기존 데이터 복원 성공")
        fun retrieve_success() {
            // act
            val item = OrderItem.retrieve(
                id = 10L,
                snapshot = createSnapshot(),
                quantity = Quantity(5),
            )

            // assert
            assertAll(
                { assertThat(item.id).isEqualTo(10L) },
                { assertThat(item.quantity.value).isEqualTo(5) },
            )
        }
    }
}
