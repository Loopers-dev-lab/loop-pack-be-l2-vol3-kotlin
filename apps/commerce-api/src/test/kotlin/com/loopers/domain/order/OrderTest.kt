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
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("Order 도메인")
class OrderTest {

    private fun createSnapshot(
        productId: Long = 1L,
        productName: String = "테스트 상품",
        brandId: Long = 1L,
        brandName: String = "테스트 브랜드",
        regularPrice: Money = Money(BigDecimal("10000")),
        sellingPrice: Money = Money(BigDecimal("8000")),
    ): OrderSnapshot = OrderSnapshot(
        productId = productId,
        productName = productName,
        brandId = brandId,
        brandName = brandName,
        regularPrice = regularPrice,
        sellingPrice = sellingPrice,
        thumbnailUrl = null,
    )

    private fun createOrderItem(
        snapshot: OrderSnapshot = createSnapshot(),
        quantity: Int = 2,
    ): OrderItem = OrderItem.create(snapshot = snapshot, quantity = Quantity(quantity))

    @Nested
    @DisplayName("생성")
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 생성 성공 — status는 CREATED")
        fun create_success() {
            // act
            val order = Order.create(
                userId = 1L,
                idempotencyKey = IdempotencyKey("order-key-123"),
                items = listOf(createOrderItem()),
            )

            // assert
            assertAll(
                { assertThat(order.id).isNull() },
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.idempotencyKey.value).isEqualTo("order-key-123") },
                { assertThat(order.status).isEqualTo(Order.Status.CREATED) },
                { assertThat(order.items).hasSize(1) },
            )
        }

        @Test
        @DisplayName("여러 상품으로 주문 생성 성공")
        fun create_multipleItems() {
            // arrange
            val items = listOf(
                createOrderItem(snapshot = createSnapshot(productId = 1L)),
                createOrderItem(snapshot = createSnapshot(productId = 2L)),
            )

            // act
            val order = Order.create(
                userId = 1L,
                idempotencyKey = IdempotencyKey("order-key-456"),
                items = items,
            )

            // assert
            assertThat(order.items).hasSize(2)
        }
    }

    @Nested
    @DisplayName("주문 항목이 비어있으면 생성 실패한다")
    inner class WhenItemsEmpty {

        @Test
        @DisplayName("빈 항목 리스트 → 실패")
        fun create_emptyItems() {
            val exception = assertThrows<CoreException> {
                Order.create(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("order-key-789"),
                    items = emptyList(),
                )
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_INVALID_ITEMS)
        }
    }

    @Nested
    @DisplayName("Snapshot 값 보존")
    inner class SnapshotPreservation {

        @Test
        @DisplayName("주문 생성 시 스냅샷 값이 그대로 보존된다")
        fun create_snapshotPreserved() {
            // arrange
            val snapshot = createSnapshot(
                productId = 42L,
                productName = "특별 상품",
                brandId = 7L,
                brandName = "프리미엄 브랜드",
                regularPrice = Money(BigDecimal("50000")),
                sellingPrice = Money(BigDecimal("35000")),
            )

            // act
            val order = Order.create(
                userId = 1L,
                idempotencyKey = IdempotencyKey("snapshot-test"),
                items = listOf(createOrderItem(snapshot = snapshot, quantity = 3)),
            )

            // assert
            val item = order.items[0]
            assertAll(
                { assertThat(item.snapshot.productId).isEqualTo(42L) },
                { assertThat(item.snapshot.productName).isEqualTo("특별 상품") },
                { assertThat(item.snapshot.brandId).isEqualTo(7L) },
                { assertThat(item.snapshot.brandName).isEqualTo("프리미엄 브랜드") },
                { assertThat(item.snapshot.regularPrice).isEqualTo(Money(BigDecimal("50000"))) },
                { assertThat(item.snapshot.sellingPrice).isEqualTo(Money(BigDecimal("35000"))) },
                { assertThat(item.quantity.value).isEqualTo(3) },
            )
        }
    }

    @Nested
    @DisplayName("totalAmount — 주문 항목의 (판매가 × 수량) 합산")
    inner class TotalAmount {

        @Test
        @DisplayName("단일 상품: 판매가 8000 × 수량 3 = 24000")
        fun totalAmount_singleItem() {
            val order = Order.create(
                userId = 1L,
                idempotencyKey = IdempotencyKey("total-1"),
                items = listOf(
                    createOrderItem(
                        snapshot = createSnapshot(sellingPrice = Money(BigDecimal("8000"))),
                        quantity = 3,
                    ),
                ),
            )

            assertThat(order.totalAmount()).isEqualTo(Money(BigDecimal("24000")))
        }

        @Test
        @DisplayName("여러 상품: (8000×2) + (5000×3) = 31000")
        fun totalAmount_multipleItems() {
            val order = Order.create(
                userId = 1L,
                idempotencyKey = IdempotencyKey("total-2"),
                items = listOf(
                    createOrderItem(
                        snapshot = createSnapshot(
                            productId = 1L,
                            sellingPrice = Money(BigDecimal("8000")),
                        ),
                        quantity = 2,
                    ),
                    createOrderItem(
                        snapshot = createSnapshot(
                            productId = 2L,
                            regularPrice = Money(BigDecimal("7000")),
                            sellingPrice = Money(BigDecimal("5000")),
                        ),
                        quantity = 3,
                    ),
                ),
            )

            assertThat(order.totalAmount()).isEqualTo(Money(BigDecimal("31000")))
        }
    }

    @Nested
    @DisplayName("영속성 복원")
    inner class Retrieve {

        @Test
        @DisplayName("retrieve로 기존 데이터 복원 성공 — createdAt 포함")
        fun retrieve_success() {
            // arrange
            val now = ZonedDateTime.of(2026, 3, 5, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            // act
            val order = Order.retrieve(
                id = 100L,
                userId = 1L,
                idempotencyKey = IdempotencyKey("existing-key"),
                status = Order.Status.CREATED,
                items = listOf(createOrderItem()),
                createdAt = now,
            )

            // assert
            assertAll(
                { assertThat(order.id).isEqualTo(100L) },
                { assertThat(order.status).isEqualTo(Order.Status.CREATED) },
                { assertThat(order.createdAt).isEqualTo(now) },
            )
        }
    }
}
