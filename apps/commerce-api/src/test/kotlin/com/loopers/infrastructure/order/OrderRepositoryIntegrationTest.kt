package com.loopers.infrastructure.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
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

@DisplayName("OrderRepository 통합 테스트")
@SpringBootTest
class OrderRepositoryIntegrationTest
@Autowired
constructor(
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
        items: List<OrderItem> = listOf(
            OrderItem.create(
                snapshot = OrderSnapshot(
                    productId = 1L,
                    productName = "테스트 상품",
                    brandId = 1L,
                    brandName = "테스트 브랜드",
                    regularPrice = Money(BigDecimal("10000")),
                    sellingPrice = Money(BigDecimal("8000")),
                    thumbnailUrl = "https://img.test/thumb.jpg",
                ),
                quantity = Quantity(2),
            ),
        ),
    ): Order {
        val order = Order.create(
            userId = userId,
            idempotencyKey = IdempotencyKey(idempotencyKey),
            items = items,
        )
        return orderRepository.save(order)
    }

    @Nested
    @DisplayName("save 시")
    inner class WhenSave {

        @Test
        @DisplayName("Order와 OrderItem이 함께 저장된다")
        fun save_withItems() {
            // act
            val saved = createOrder()

            // assert
            assertAll(
                { assertThat(saved.id).isNotNull() },
                { assertThat(saved.userId).isEqualTo(1L) },
                { assertThat(saved.idempotencyKey.value).isEqualTo("key-001") },
                { assertThat(saved.status).isEqualTo(Order.Status.CREATED) },
                { assertThat(saved.items).hasSize(1) },
                { assertThat(saved.items[0].id).isNotNull() },
                { assertThat(saved.createdAt).isNotNull() },
            )
        }

        @Test
        @DisplayName("OrderItem 스냅샷 필드가 정확히 저장된다")
        fun save_snapshotFields() {
            // act
            val saved = createOrder()

            // assert
            val item = saved.items[0]
            assertAll(
                { assertThat(item.snapshot.productId).isEqualTo(1L) },
                { assertThat(item.snapshot.productName).isEqualTo("테스트 상품") },
                { assertThat(item.snapshot.brandId).isEqualTo(1L) },
                { assertThat(item.snapshot.brandName).isEqualTo("테스트 브랜드") },
                { assertThat(item.snapshot.regularPrice).isEqualTo(Money(BigDecimal("10000"))) },
                { assertThat(item.snapshot.sellingPrice).isEqualTo(Money(BigDecimal("8000"))) },
                { assertThat(item.snapshot.thumbnailUrl).isEqualTo("https://img.test/thumb.jpg") },
                { assertThat(item.quantity).isEqualTo(Quantity(2)) },
            )
        }

        @Test
        @DisplayName("여러 OrderItem이 함께 저장된다")
        fun save_multipleItems() {
            // act
            val saved = createOrder(
                items = listOf(
                    OrderItem.create(
                        snapshot = OrderSnapshot(
                            productId = 1L,
                            productName = "상품A",
                            brandId = 1L,
                            brandName = "브랜드A",
                            regularPrice = Money(BigDecimal("10000")),
                            sellingPrice = Money(BigDecimal("8000")),
                            thumbnailUrl = null,
                        ),
                        quantity = Quantity(1),
                    ),
                    OrderItem.create(
                        snapshot = OrderSnapshot(
                            productId = 2L,
                            productName = "상품B",
                            brandId = 2L,
                            brandName = "브랜드B",
                            regularPrice = Money(BigDecimal("5000")),
                            sellingPrice = Money(BigDecimal("3000")),
                            thumbnailUrl = null,
                        ),
                        quantity = Quantity(3),
                    ),
                ),
            )

            // assert
            assertThat(saved.items).hasSize(2)
        }
    }

    @Nested
    @DisplayName("findById 시")
    inner class WhenFindById {

        @Test
        @Transactional
        @DisplayName("저장된 주문을 조회할 수 있다")
        fun findById_success() {
            // arrange
            val saved = createOrder()

            // act
            val found = orderRepository.findById(saved.id!!)

            // assert
            assertAll(
                { assertThat(found).isNotNull },
                { assertThat(found!!.id).isEqualTo(saved.id) },
                { assertThat(found!!.items).hasSize(1) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 주문은 null을 반환한다")
        fun findById_notFound() {
            // act
            val found = orderRepository.findById(999L)

            // assert
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findByIdAndUserId 시")
    inner class WhenFindByIdAndUserId {

        @Test
        @Transactional
        @DisplayName("userId가 일치하면 조회할 수 있다")
        fun findByIdAndUserId_success() {
            // arrange
            val saved = createOrder(userId = 1L)

            // act
            val found = orderRepository.findByIdAndUserId(saved.id!!, 1L)

            // assert
            assertThat(found).isNotNull
            assertThat(found!!.userId).isEqualTo(1L)
        }

        @Test
        @DisplayName("userId가 일치하지 않으면 null을 반환한다")
        fun findByIdAndUserId_wrongUser() {
            // arrange
            val saved = createOrder(userId = 1L)

            // act
            val found = orderRepository.findByIdAndUserId(saved.id!!, 999L)

            // assert
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAllByUserId 시")
    inner class WhenFindAllByUserId {

        @Test
        @Transactional
        @DisplayName("해당 사용자의 주문만 반환한다")
        fun findAllByUserId_filtered() {
            // arrange
            val order1 = createOrder(userId = 1L, idempotencyKey = "key-001")
            createOrder(userId = 2L, idempotencyKey = "key-002")

            // act
            val result = orderRepository.findAllByUserId(
                userId = 1L,
                from = order1.createdAt!!.minusDays(1),
                to = order1.createdAt!!.plusDays(1),
                pageRequest = PageRequest(),
            )

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].userId).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("existsByIdempotencyKey 시")
    inner class WhenExistsByIdempotencyKey {

        @Test
        @DisplayName("존재하는 멱등성 키는 true를 반환한다")
        fun existsByIdempotencyKey_exists() {
            // arrange
            createOrder(idempotencyKey = "key-001")

            // act & assert
            assertThat(orderRepository.existsByIdempotencyKey(IdempotencyKey("key-001"))).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 멱등성 키는 false를 반환한다")
        fun existsByIdempotencyKey_notExists() {
            // act & assert
            assertThat(orderRepository.existsByIdempotencyKey(IdempotencyKey("key-not-exist"))).isFalse()
        }
    }
}
