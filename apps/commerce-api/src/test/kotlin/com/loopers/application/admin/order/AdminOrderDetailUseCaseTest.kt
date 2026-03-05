package com.loopers.application.admin.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.AdminOrderRepository
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderSnapshot
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("AdminOrderDetailUseCase")
class AdminOrderDetailUseCaseTest {

    private val adminOrderRepository: AdminOrderRepository = mock()
    private val useCase = AdminOrderDetailUseCase(adminOrderRepository)

    private val now = ZonedDateTime.of(2026, 3, 5, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun order(
        id: Long = 100L,
        userId: Long = 1L,
    ): Order = Order.retrieve(
        id = id,
        userId = userId,
        idempotencyKey = IdempotencyKey("key-001"),
        status = Order.Status.CREATED,
        items = listOf(
            OrderItem.retrieve(
                id = 1L,
                snapshot = OrderSnapshot(
                    productId = 1L,
                    productName = "테스트 상품",
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    regularPrice = Money(BigDecimal("10000")),
                    sellingPrice = Money(BigDecimal("8000")),
                    thumbnailUrl = null,
                ),
                quantity = Quantity(2),
            ),
        ),
        createdAt = now,
    )

    @Nested
    @DisplayName("주문 상세 조회 시 AdminOrderResult.Detail을 반환한다")
    inner class WhenGetDetail {

        @Test
        @DisplayName("정상 조회 → Detail(orderId, userId, status, items, totalAmount, createdAt)")
        fun getDetail_success() {
            // arrange
            given(adminOrderRepository.findById(100L)).willReturn(order())

            // act
            val result = useCase.getDetail(orderId = 100L)

            // assert
            assertAll(
                { assertThat(result.orderId).isEqualTo(100L) },
                { assertThat(result.userId).isEqualTo(1L) },
                { assertThat(result.status).isEqualTo("CREATED") },
                { assertThat(result.items).hasSize(1) },
                { assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("16000")) },
                { assertThat(result.createdAt).isEqualTo(now) },
            )
        }

        @Test
        @DisplayName("주문 항목 상세 정보가 정확하다")
        fun getDetail_itemDetail() {
            // arrange
            given(adminOrderRepository.findById(100L)).willReturn(order())

            // act
            val result = useCase.getDetail(orderId = 100L)

            // assert
            val item = result.items[0]
            assertAll(
                { assertThat(item.productId).isEqualTo(1L) },
                { assertThat(item.productName).isEqualTo("테스트 상품") },
                { assertThat(item.brandName).isEqualTo("테스트 브랜드") },
                { assertThat(item.regularPrice).isEqualByComparingTo(BigDecimal("10000")) },
                { assertThat(item.sellingPrice).isEqualByComparingTo(BigDecimal("8000")) },
                { assertThat(item.quantity).isEqualTo(2) },
            )
        }
    }

    @Nested
    @DisplayName("존재하지 않는 주문이면 ORDER_NOT_FOUND 예외를 던진다")
    inner class WhenOrderNotFound {

        @Test
        @DisplayName("주문 없음 → 예외")
        fun getDetail_notFound() {
            // arrange
            given(adminOrderRepository.findById(999L)).willReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                useCase.getDetail(orderId = 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.ORDER_NOT_FOUND)
        }
    }
}
