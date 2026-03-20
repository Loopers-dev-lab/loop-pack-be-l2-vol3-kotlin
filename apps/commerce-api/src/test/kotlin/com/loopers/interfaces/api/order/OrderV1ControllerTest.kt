package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderInfo
import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.infrastructure.auth.JwtAuthenticationFilter
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

@DisplayName("OrderV1Controller")
class OrderV1ControllerTest {
    private val orderFacade: OrderFacade = mockk()
    private val orderService: OrderService = mockk()
    private val controller = OrderV1Controller(orderFacade, orderService)

    @DisplayName("주문 생성 응답은 명시적 주문 DTO를 반환하고 결제 필드를 포함하지 않는다")
    @Test
    fun returnsCreateResponse_withoutPaymentFields() {
        // arrange
        val request = mockRequest()
        val orderInfo = createOrderInfo(id = 1L)
        val body = OrderV1Dto.CreateRequest(
            items = listOf(OrderV1Dto.OrderItemDto(productId = 10L, quantity = 2)),
            couponIssueId = 20L,
        )
        every { orderFacade.createOrder(1L, any(), 20L) } returns orderInfo

        // act
        val response = controller.createOrder(body, request)

        // assert
        assertThat(response.data?.id).isEqualTo(orderInfo.id)
        assertThat(response.data?.totalAmount).isEqualTo(orderInfo.totalAmount)
        assertThat(response.data?.javaClass?.declaredFields?.map { it.name })
            .doesNotContain("paymentId", "paymentStatus")
        verify(exactly = 1) { orderFacade.createOrder(1L, any(), 20L) }
    }

    @DisplayName("내 주문 목록 응답은 명시적 주문 DTO 페이지를 반환한다")
    @Test
    fun returnsOrderResponsePage_whenFindingMyOrders() {
        // arrange
        val request = mockRequest()
        val pageable = PageRequest.of(0, 20)
        val orderInfo = createOrderInfo(id = 1L)
        every {
            orderService.findByUserIdAndDateRange(
                userId = 1L,
                startAt = java.time.LocalDate.of(2026, 3, 1),
                endAt = java.time.LocalDate.of(2026, 3, 20),
                pageable = pageable,
            )
        } returns PageImpl(listOf(orderInfo.toOrderModel()), pageable, 1)

        // act
        val response = controller.findMyOrders(
            httpRequest = request,
            startAt = java.time.LocalDate.of(2026, 3, 1),
            endAt = java.time.LocalDate.of(2026, 3, 20),
            pageable = pageable,
        )

        // assert
        assertThat(response.data?.content).hasSize(1)
        assertThat(response.data?.content?.first()?.id).isEqualTo(orderInfo.id)
        assertThat(response.data?.content?.first()?.totalAmount).isEqualTo(orderInfo.totalAmount)
        assertThat(response.data?.content?.first()?.javaClass?.declaredFields?.map { it.name })
            .doesNotContain("paymentId", "paymentStatus")
    }

    @DisplayName("주문 상세 응답은 명시적 주문 DTO를 반환한다")
    @Test
    fun returnsOrderResponse_whenFindingById() {
        // arrange
        val request = mockRequest()
        val orderInfo = createOrderInfo(id = 1L)
        every { orderService.findByIdAndUserId(1L, 1L) } returns orderInfo.toOrderModel()

        // act
        val response = controller.findById(1L, request)

        // assert
        assertThat(response.data?.id).isEqualTo(orderInfo.id)
        assertThat(response.data?.totalAmount).isEqualTo(orderInfo.totalAmount)
        assertThat(response.data?.javaClass?.declaredFields?.map { it.name })
            .doesNotContain("paymentId", "paymentStatus")
    }

    private fun mockRequest(): HttpServletRequest {
        val request = mockk<HttpServletRequest>()
        every {
            request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE)
        } returns AuthenticatedMember(memberId = 1L, loginId = "tester")
        return request
    }

    private fun createOrderInfo(id: Long): OrderInfo {
        return OrderInfo(
            id = id,
            userId = 1L,
            orderStatus = OrderStatus.ORDERED,
            couponIssueId = 20L,
            originalTotalAmount = 50_000L,
            discountAmount = 3_000L,
            totalAmount = 47_000L,
            orderItems = listOf(
                com.loopers.application.order.OrderItemInfo(
                    id = 101L,
                    productId = 10L,
                    productName = "감성 티셔츠",
                    brandName = "루프팩",
                    price = 25_000L,
                    quantity = 2,
                    subTotal = 50_000L,
                ),
            ),
            createdAt = ZonedDateTime.parse("2026-03-20T10:15:30+09:00[Asia/Seoul]"),
        )
    }

    private fun OrderInfo.toOrderModel(): com.loopers.domain.order.OrderModel {
        val orderId = id
        val orderCreatedAt = checkNotNull(createdAt)
        val order = spyk(com.loopers.domain.order.OrderModel(userId = userId)) {
            every { this@spyk.id } returns orderId
            every { this@spyk.createdAt } returns orderCreatedAt
        }
        orderItems.forEach { item ->
            order.addItem(
                com.loopers.domain.order.OrderItemModel(
                    order = order,
                    productId = item.productId,
                    productName = item.productName,
                    brandName = item.brandName,
                    price = item.price,
                    quantity = item.quantity,
                ),
            )
        }
        if (couponIssueId != null) {
            order.applyCouponDiscount(couponIssueId, discountAmount)
        }
        return order
    }
}
