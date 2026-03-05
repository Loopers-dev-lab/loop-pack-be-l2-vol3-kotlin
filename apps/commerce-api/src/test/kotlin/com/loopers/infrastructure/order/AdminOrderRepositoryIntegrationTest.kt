package com.loopers.infrastructure.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.AdminOrderRepository
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderSnapshot
import com.loopers.support.page.PageRequest
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@DisplayName("AdminOrderRepository 통합 테스트")
@SpringBootTest
class AdminOrderRepositoryIntegrationTest
@Autowired
constructor(
    private val adminOrderRepository: AdminOrderRepository,
    private val orderRepository: OrderRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createOrder(
        userId: Long = 1L,
        idempotencyKey: String = "key-001",
    ): Order {
        val order = Order.create(
            userId = userId,
            idempotencyKey = IdempotencyKey(idempotencyKey),
            items = listOf(
                OrderItem.create(
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
        )
        return orderRepository.save(order)
    }

    @Nested
    @DisplayName("findById 시")
    inner class WhenFindById {

        @Test
        @Transactional
        @DisplayName("주문을 상세 조회할 수 있다")
        fun findById_success() {
            // arrange
            val saved = createOrder()

            // act
            val found = adminOrderRepository.findById(saved.id!!)

            // assert
            assertAll(
                { assertThat(found).isNotNull },
                { assertThat(found!!.id).isEqualTo(saved.id) },
                { assertThat(found!!.userId).isEqualTo(1L) },
                { assertThat(found!!.items).hasSize(1) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 주문은 null을 반환한다")
        fun findById_notFound() {
            // act
            val found = adminOrderRepository.findById(999L)

            // assert
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAll 시")
    inner class WhenFindAll {

        @Test
        @Transactional
        @DisplayName("모든 사용자의 주문을 조회할 수 있다")
        fun findAll_allUsers() {
            // arrange
            val order1 = createOrder(userId = 1L, idempotencyKey = "key-001")
            createOrder(userId = 2L, idempotencyKey = "key-002")

            // act
            val result = adminOrderRepository.findAll(
                from = order1.createdAt!!.minusDays(1),
                to = order1.createdAt!!.plusDays(1),
                pageRequest = PageRequest(),
            )

            // assert
            assertThat(result.content).hasSize(2)
        }

        @Test
        @DisplayName("기간 외 주문은 조회되지 않는다")
        fun findAll_periodFilter() {
            // arrange
            val saved = createOrder()

            // act
            val result = adminOrderRepository.findAll(
                from = saved.createdAt!!.plusDays(1),
                to = saved.createdAt!!.plusDays(2),
                pageRequest = PageRequest(),
            )

            // assert
            assertThat(result.content).isEmpty()
        }

        @Test
        @Transactional
        @DisplayName("페이징이 정상 동작한다")
        fun findAll_paging() {
            // arrange
            val order1 = createOrder(userId = 1L, idempotencyKey = "key-001")
            createOrder(userId = 2L, idempotencyKey = "key-002")
            createOrder(userId = 3L, idempotencyKey = "key-003")

            val pageRequest = PageRequest().apply { size = 10 }

            // act
            val result = adminOrderRepository.findAll(
                from = order1.createdAt!!.minusDays(1),
                to = order1.createdAt!!.plusDays(1),
                pageRequest = pageRequest,
            )

            // assert
            assertAll(
                { assertThat(result.content).hasSize(3) },
                { assertThat(result.totalElements).isEqualTo(3L) },
                { assertThat(result.page).isEqualTo(0) },
            )
        }
    }
}
