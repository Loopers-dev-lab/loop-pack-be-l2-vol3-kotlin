package com.loopers.application.order

import com.loopers.domain.order.ImageUrl
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.order.Price
import com.loopers.domain.order.ProductName
import com.loopers.domain.order.Quantity
import com.loopers.domain.order.TotalAmount
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class OrderInfoTest {

    private fun setField(obj: Any, fieldName: String, value: Any) {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(obj, value)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
    }

    @Test
    fun `OrderModel과 OrderItemModel 목록에서 OrderInfo로 변환 시 모든 필드가 올바르게 매핑된다`() {
        // given
        val now = ZonedDateTime.now()
        val orderModel = OrderModel(
            userId = 1L,
            totalAmount = TotalAmount(30_000L),
            status = OrderStatus.PENDING_PAYMENT,
        )
        setField(orderModel, "createdAt", now)
        setField(orderModel, "updatedAt", now)

        val item1 = OrderItemModel(
            orderId = orderModel.id,
            brandId = 1L,
            productId = 1L,
            quantity = Quantity(2L),
            unitPrice = Price(10_000L),
            productName = ProductName("뉴발란스 991"),
            imageUrl = ImageUrl("test.png"),
        )
        val item2 = OrderItemModel(
            orderId = orderModel.id,
            brandId = 1L,
            productId = 2L,
            quantity = Quantity(1L),
            unitPrice = Price(10_000L),
            productName = ProductName("Air Max"),
            imageUrl = ImageUrl("air.png"),
        )

        // when
        val orderInfo = OrderInfo.from(orderModel, listOf(item1, item2))

        // then
        assertThat(orderInfo.id).isEqualTo(orderModel.id)
        assertThat(orderInfo.userId).isEqualTo(1L)
        assertThat(orderInfo.totalAmount).isEqualTo(30_000L)
        assertThat(orderInfo.status).isEqualTo("PENDING_PAYMENT")
        assertThat(orderInfo.createdAt).isEqualTo(now)
        assertThat(orderInfo.updatedAt).isEqualTo(now)
        assertThat(orderInfo.items).hasSize(2)
        assertThat(orderInfo.items[0].subTotal).isEqualTo(20_000L)
        assertThat(orderInfo.items[1].subTotal).isEqualTo(10_000L)
    }

    @Test
    fun `OrderItemInfo의 subTotal은 unitPrice x quantity로 계산된다`() {
        // given
        val now = ZonedDateTime.now()
        val orderModel = OrderModel(
            userId = 1L,
            totalAmount = TotalAmount(75_000L),
            status = OrderStatus.PENDING_PAYMENT,
        )
        setField(orderModel, "createdAt", now)
        setField(orderModel, "updatedAt", now)

        val item = OrderItemModel(
            orderId = orderModel.id,
            brandId = 1L,
            productId = 1L,
            quantity = Quantity(3L),
            unitPrice = Price(25_000L),
            productName = ProductName("뉴발란스 991"),
            imageUrl = ImageUrl("test.png"),
        )

        // when
        val orderInfo = OrderInfo.from(orderModel, listOf(item))

        // then
        assertThat(orderInfo.items[0].unitPrice).isEqualTo(25_000L)
        assertThat(orderInfo.items[0].quantity).isEqualTo(3L)
        assertThat(orderInfo.items[0].subTotal).isEqualTo(75_000L)
    }

    @Test
    fun `주문 항목이 없는 경우 items는 빈 리스트이다`() {
        // given
        val now = ZonedDateTime.now()
        val orderModel = OrderModel(
            userId = 1L,
            totalAmount = TotalAmount(1L),
            status = OrderStatus.PENDING_PAYMENT,
        )
        setField(orderModel, "createdAt", now)
        setField(orderModel, "updatedAt", now)

        // when
        val orderInfo = OrderInfo.from(orderModel, emptyList())

        // then
        assertThat(orderInfo.items).isEmpty()
    }
}
