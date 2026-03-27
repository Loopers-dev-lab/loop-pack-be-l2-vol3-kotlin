package com.loopers.infrastructure.order

import com.loopers.application.user.order.OrderCreatedEvent
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryResult
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.infrastructure.product.RedisProductQueryCache
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@DisplayName("Order created event listener integration")
@SpringBootTest
class OrderCreatedEventListenerIntegrationTest
@Autowired
constructor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val redisProductQueryCache: RedisProductQueryCache,
    private val databaseCleanUp: DatabaseCleanUp,
    platformTransactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    private lateinit var activeBrand: Brand

    @BeforeEach
    fun setUp() {
        val savedBrand = brandRepository.save(Brand.register(name = "Order Listener Brand"), ADMIN)
        activeBrand = brandRepository.save(savedBrand.update("Order Listener Brand", "ACTIVE"), ADMIN)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("order created event is handled after commit to evict product detail caches")
    fun handle_orderCreated_afterCommit() {
        val firstProductId = createActiveProduct(name = "Order Listener Product 1")
        val secondProductId = createActiveProduct(name = "Order Listener Product 2")
        putDetailCache(firstProductId)
        putDetailCache(secondProductId)

        transactionTemplate.executeWithoutResult {
            applicationEventPublisher.publishEvent(
                OrderCreatedEvent(
                    orderId = ORDER_ID,
                    userId = USER_ID,
                    productIds = listOf(firstProductId, secondProductId),
                ),
            )

            assertThat(redisProductQueryCache.getDetail(firstProductId)).isNotNull()
            assertThat(redisProductQueryCache.getDetail(secondProductId)).isNotNull()
        }

        assertThat(redisProductQueryCache.getDetail(firstProductId)).isNull()
        assertThat(redisProductQueryCache.getDetail(secondProductId)).isNull()
    }

    private fun createActiveProduct(name: String): Long {
        val registered = Product.register(
            name = name,
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("9000")),
            brandId = activeBrand.id!!,
        )
        val activated = productRepository.save(registered, ADMIN).activate()
        val product = productRepository.save(activated, ADMIN)

        productStockRepository.save(
            ProductStock.create(productId = product.id!!, initialQuantity = Quantity(10)),
            ADMIN,
        )

        return product.id!!
    }

    private fun putDetailCache(productId: Long) {
        val product = productRepository.findById(productId)!!
        val stock = productStockRepository.findByProductId(productId)!!

        redisProductQueryCache.putDetail(ProductQueryResult.Detail.from(product, activeBrand, stock))
    }

    companion object {
        private const val ADMIN = "loopers.admin"
        private const val USER_ID = 1L
        private const val ORDER_ID = 100L
    }
}
