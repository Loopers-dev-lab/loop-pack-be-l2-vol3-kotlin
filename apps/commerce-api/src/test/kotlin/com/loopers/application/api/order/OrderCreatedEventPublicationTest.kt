package com.loopers.application.api.order

import com.loopers.CommerceApiApplication
import com.loopers.domain.brand.Brand
import com.loopers.domain.order.event.OrderCreatedEvent
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.stock.Stock
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.infrastructure.useractionlog.UserActionLogJpaRepository
import com.loopers.interfaces.api.order.OrderV1Dto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@SpringBootTest(classes = [CommerceApiApplication::class, OrderCreatedEventPublicationTest.ProbeConfig::class])
@DisplayName("OrderCreatedEvent publication")
class OrderCreatedEventPublicationTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val stockJpaRepository: StockJpaRepository,
    private val userActionLogJpaRepository: UserActionLogJpaRepository,
    private val committedProbe: CommittedOrderCreatedEventProbe,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @AfterEach
    fun tearDown() {
        committedProbe.clear()
    }

    @Test
    @DisplayName("createOrder 성공 시 OrderCreatedEvent가 커밋 후 1회 전달되고 order.created 로그가 저장된다")
    fun publishOneCommittedEventOnSuccess() {
        val product = createProductWithStock(stockQuantity = 10)

        val orderId = transactionTemplate.execute {
            orderFacade.createOrder(
                userId = 1001L,
                orderRequest =
                    OrderV1Dto.OrderRequest(
                        items = listOf(OrderV1Dto.OrderItemRequest(productId = product.id, quantity = 2)),
                        couponId = null,
                    ),
            )
        }!!

        assertThat(committedProbe.events).hasSize(1)
        val publishedEvent = committedProbe.events.single()
        assertThat(publishedEvent.orderId).isEqualTo(orderId)
        assertThat(publishedEvent.userId).isEqualTo(1001L)
        assertThat(publishedEvent.itemCount).isEqualTo(1)
        assertThat(publishedEvent.couponId).isNull()
        assertThat(publishedEvent.dedupeKey).isEqualTo("order.created:$orderId")
        assertThat(userActionLogJpaRepository.countByDedupeKey("order.created:$orderId")).isEqualTo(1)
    }

    @Test
    @DisplayName("createOrder 롤백 경로에서는 committed OrderCreatedEvent와 order.created 로그가 남지 않는다")
    fun noCommittedEventOrAncillaryLogOnRollback() {
        val product1 = createProductWithStock(stockQuantity = 20)
        val product2 = createProductWithStock(stockQuantity = 5)

        val request =
            OrderV1Dto.OrderRequest(
                items =
                    listOf(
                        OrderV1Dto.OrderItemRequest(productId = product1.id, quantity = 10),
                        OrderV1Dto.OrderItemRequest(productId = product2.id, quantity = 10),
                    ),
                couponId = null,
            )
        val persistedLogCountBefore = userActionLogJpaRepository.count()

        runCatching {
            orderFacade.createOrder(userId = 1002L, orderRequest = request)
        }

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
    }

    private fun createProductWithStock(stockQuantity: Int): Product {
        val brand =
            brandJpaRepository.save(
                Brand.create(
                    name = "event-test-brand-$stockQuantity",
                    description = "event-test-brand",
                ),
            )

        val product =
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "event-test-product-$stockQuantity",
                    price = BigDecimal("10000"),
                    status = ProductStatus.ACTIVE,
                ),
            )

        stockJpaRepository.save(
            Stock.create(
                productId = product.id,
                quantity = stockQuantity,
            ),
        )

        return product
    }

    @TestConfiguration
    class ProbeConfig {
        @Bean
        fun committedOrderCreatedEventProbe(): CommittedOrderCreatedEventProbe = CommittedOrderCreatedEventProbe()
    }

    class CommittedOrderCreatedEventProbe {
        val events: MutableList<OrderCreatedEvent> = mutableListOf()

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
        fun onOrderCreated(event: OrderCreatedEvent) {
            events.add(event)
        }

        fun clear() {
            events.clear()
        }
    }
}
