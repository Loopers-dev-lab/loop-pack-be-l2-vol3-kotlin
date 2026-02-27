package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class OrderRepositoryImplTest {

    private val orderJpaRepository: OrderJpaRepository = mockk()
    private val orderRepositoryImpl = OrderRepositoryImpl(orderJpaRepository)

    @DisplayName("주문을 저장할 때,")
    @Nested
    inner class Save {
        @DisplayName("JpaRepository에 위임하여 저장하고 결과를 반환한다.")
        @Test
        fun delegatesToJpaRepository() {
            // arrange
            val order = OrderModel(userId = 1L)
            every { orderJpaRepository.save(order) } returns order

            // act
            val result = orderRepositoryImpl.save(order)

            // assert
            assertThat(result).isEqualTo(order)
            assertThat(result.orderStatus).isEqualTo(OrderStatus.ORDERED)
            verify(exactly = 1) { orderJpaRepository.save(order) }
        }
    }

    @DisplayName("주문을 ID로 조회할 때,")
    @Nested
    inner class FindById {
        @DisplayName("삭제되지 않은 주문이 존재하면 반환한다.")
        @Test
        fun returnsOrder_whenExists() {
            // arrange
            val order = OrderModel(userId = 1L)
            every { orderJpaRepository.findByIdAndDeletedAtIsNull(1L) } returns order

            // act
            val result = orderRepositoryImpl.findByIdAndDeletedAtIsNull(1L)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.userId).isEqualTo(1L)
            verify(exactly = 1) { orderJpaRepository.findByIdAndDeletedAtIsNull(1L) }
        }

        @DisplayName("존재하지 않으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // arrange
            every { orderJpaRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act
            val result = orderRepositoryImpl.findByIdAndDeletedAtIsNull(999L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("유저의 주문 목록을 조회할 때,")
    @Nested
    inner class FindAllByUserId {
        @DisplayName("삭제되지 않은 주문만 페이징하여 반환한다.")
        @Test
        fun returnsActiveOrdersForUser() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val orders = listOf(
                OrderModel(userId = 1L),
                OrderModel(userId = 1L),
            )
            val page = PageImpl(orders, pageable, 2)
            every { orderJpaRepository.findAllByUserIdAndDeletedAtIsNull(1L, pageable) } returns page

            // act
            val result = orderRepositoryImpl.findAllByUserIdAndDeletedAtIsNull(1L, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content.all { it.userId == 1L }).isTrue()
            verify(exactly = 1) { orderJpaRepository.findAllByUserIdAndDeletedAtIsNull(1L, pageable) }
        }
    }
}
