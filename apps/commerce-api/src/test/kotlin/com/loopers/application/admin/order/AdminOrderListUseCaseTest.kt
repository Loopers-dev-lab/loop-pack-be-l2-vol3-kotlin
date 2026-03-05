package com.loopers.application.admin.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.AdminOrderRepository
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderSnapshot
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("AdminOrderListUseCase")
class AdminOrderListUseCaseTest {

    private val adminOrderRepository: AdminOrderRepository = mock()
    private val useCase = AdminOrderListUseCase(adminOrderRepository)

    private val now = ZonedDateTime.of(2026, 3, 5, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun order(
        id: Long = 100L,
        userId: Long = 1L,
        items: List<OrderItem> = listOf(
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
    ): Order = Order.retrieve(
        id = id,
        userId = userId,
        idempotencyKey = IdempotencyKey("key-$id"),
        status = Order.Status.CREATED,
        items = items,
        createdAt = now,
    )

    private fun pageRequest(): PageRequest = PageRequest()

    @Nested
    @DisplayName("기간을 지정하면 해당 기간의 주문만 반환한다")
    inner class WhenPeriodSpecified {

        @Test
        @DisplayName("from=2026-03-01, to=2026-03-05 → 해당 기간 주문 반환")
        fun getList_withPeriod() {
            // arrange
            val from = LocalDate.of(2026, 3, 1)
            val to = LocalDate.of(2026, 3, 5)
            val fromZdt = from.atStartOfDay(ZoneId.systemDefault())
            val toZdt = to.plusDays(1).atStartOfDay(ZoneId.systemDefault())

            given(
                adminOrderRepository.findAll(
                    eq(fromZdt),
                    eq(toZdt),
                    check { pageReq ->
                        assertThat(pageReq.page).isEqualTo(0)
                        assertThat(pageReq.size).isEqualTo(20)
                    },
                ),
            ).willReturn(
                PageResponse(
                    content = listOf(order()),
                    totalElements = 1L,
                    page = 0,
                    size = 20,
                ),
            )

            // act
            val result = useCase.getList(
                from = from,
                to = to,
                pageRequest = pageRequest(),
            )

            // assert
            assertThat(result.content).hasSize(1)
        }
    }

    @Nested
    @DisplayName("기간을 지정하지 않으면 최근 1개월 주문을 반환한다")
    inner class WhenNoPeriod {

        @Test
        @DisplayName("from=null, to=null → 기본값 적용")
        fun getList_defaultPeriod() {
            // arrange
            val today = LocalDate.now()
            val defaultFrom = today.minusMonths(1).atStartOfDay(ZoneId.systemDefault())
            val defaultTo = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())

            given(
                adminOrderRepository.findAll(
                    eq(defaultFrom),
                    eq(defaultTo),
                    check { pageReq ->
                        assertThat(pageReq.page).isEqualTo(0)
                        assertThat(pageReq.size).isEqualTo(20)
                    },
                ),
            ).willReturn(
                PageResponse(
                    content = listOf(order()),
                    totalElements = 1L,
                    page = 0,
                    size = 20,
                ),
            )

            // act
            val result = useCase.getList(
                from = null,
                to = null,
                pageRequest = pageRequest(),
            )

            // assert
            assertThat(result.content).hasSize(1)
        }
    }

    @Nested
    @DisplayName("빈 결과면 빈 PageResponse를 반환한다")
    inner class WhenEmpty {

        @Test
        @DisplayName("주문 없음 → 빈 content")
        fun getList_empty() {
            // arrange
            val today = LocalDate.now()
            val defaultFrom = today.minusMonths(1).atStartOfDay(ZoneId.systemDefault())
            val defaultTo = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())

            given(
                adminOrderRepository.findAll(
                    eq(defaultFrom),
                    eq(defaultTo),
                    check { pageReq ->
                        assertThat(pageReq.page).isEqualTo(0)
                        assertThat(pageReq.size).isEqualTo(20)
                    },
                ),
            ).willReturn(
                PageResponse(
                    content = emptyList(),
                    totalElements = 0L,
                    page = 0,
                    size = 20,
                ),
            )

            // act
            val result = useCase.getList(
                from = null,
                to = null,
                pageRequest = pageRequest(),
            )

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0L) },
            )
        }
    }

    @Nested
    @DisplayName("ListItem에 userId, orderSummary, totalAmount가 정상 포함된다")
    inner class WhenListItemContent {

        @Test
        @DisplayName("Admin ListItem에 userId가 포함된다")
        fun getList_includesUserId() {
            // arrange
            val today = LocalDate.now()
            val defaultFrom = today.minusMonths(1).atStartOfDay(ZoneId.systemDefault())
            val defaultTo = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())

            given(
                adminOrderRepository.findAll(
                    eq(defaultFrom),
                    eq(defaultTo),
                    check { pageReq ->
                        assertThat(pageReq.page).isEqualTo(0)
                        assertThat(pageReq.size).isEqualTo(20)
                    },
                ),
            ).willReturn(
                PageResponse(
                    content = listOf(order(userId = 42L)),
                    totalElements = 1L,
                    page = 0,
                    size = 20,
                ),
            )

            // act
            val result = useCase.getList(
                from = null,
                to = null,
                pageRequest = pageRequest(),
            )

            // assert
            val item = result.content[0]
            assertAll(
                { assertThat(item.userId).isEqualTo(42L) },
                { assertThat(item.orderSummary).isEqualTo("테스트 상품") },
                { assertThat(item.itemCount).isEqualTo(1) },
                { assertThat(item.totalAmount).isEqualByComparingTo(BigDecimal("16000")) },
            )
        }

        @Test
        @DisplayName("여러 상품 → '첫번째상품명 외 N건' 형식")
        fun getList_multipleItemSummary() {
            // arrange
            val today = LocalDate.now()
            val defaultFrom = today.minusMonths(1).atStartOfDay(ZoneId.systemDefault())
            val defaultTo = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())

            val multiItemOrder = order(
                items = listOf(
                    OrderItem.retrieve(
                        id = 1L,
                        snapshot = OrderSnapshot(
                            productId = 1L,
                            productName = "첫번째 상품",
                            brandId = 1L,
                            brandName = "브랜드A",
                            regularPrice = Money(BigDecimal("10000")),
                            sellingPrice = Money(BigDecimal("8000")),
                            thumbnailUrl = null,
                        ),
                        quantity = Quantity(1),
                    ),
                    OrderItem.retrieve(
                        id = 2L,
                        snapshot = OrderSnapshot(
                            productId = 2L,
                            productName = "두번째 상품",
                            brandId = 2L,
                            brandName = "브랜드B",
                            regularPrice = Money(BigDecimal("5000")),
                            sellingPrice = Money(BigDecimal("3000")),
                            thumbnailUrl = null,
                        ),
                        quantity = Quantity(2),
                    ),
                ),
            )

            given(
                adminOrderRepository.findAll(
                    eq(defaultFrom),
                    eq(defaultTo),
                    check { pageReq ->
                        assertThat(pageReq.page).isEqualTo(0)
                        assertThat(pageReq.size).isEqualTo(20)
                    },
                ),
            ).willReturn(
                PageResponse(
                    content = listOf(multiItemOrder),
                    totalElements = 1L,
                    page = 0,
                    size = 20,
                ),
            )

            // act
            val result = useCase.getList(
                from = null,
                to = null,
                pageRequest = pageRequest(),
            )

            // assert
            val item = result.content[0]
            assertAll(
                { assertThat(item.orderSummary).isEqualTo("첫번째 상품 외 1건") },
                { assertThat(item.itemCount).isEqualTo(2) },
                { assertThat(item.totalAmount).isEqualByComparingTo(BigDecimal("14000")) },
            )
        }
    }
}
