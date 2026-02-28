package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderStatus
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class OrderMapperTest {

    @Test
    fun `OrderEntity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = OrderEntity(
            id = null,
            userId = 1L,
            status = OrderStatus.COMPLETED,
            totalAmount = 30000L,
            orderedAt = ZonedDateTime.now(),
        )

        assertThatThrownBy { OrderMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("OrderEntity.id가 null입니다")
    }

    @Test
    fun `OrderItemEntity id가 null이면 IllegalArgumentException이 발생한다`() {
        val orderEntity = OrderEntity(
            id = 1L,
            userId = 1L,
            status = OrderStatus.COMPLETED,
            totalAmount = 30000L,
            orderedAt = ZonedDateTime.now(),
        )
        val itemEntity = OrderItemEntity(
            id = null,
            order = orderEntity,
            productId = 1L,
            productName = "테스트상품",
            brandName = "테스트브랜드",
            price = 10000L,
            quantity = 3,
        )
        orderEntity.items.add(itemEntity)

        assertThatThrownBy { OrderMapper.toDomain(orderEntity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("OrderItemEntity.id가 null입니다")
    }
}
