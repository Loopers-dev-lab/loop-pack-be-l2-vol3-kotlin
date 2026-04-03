package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import java.util.UUID
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class StockConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val stockReservationRepository: StockReservationRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @BeforeEach
    fun setUpPaymentGateway() {
        whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
            PaymentGatewayResponse(transactionKey = "txn-test", status = "PENDING", reason = null),
        )
        whenever(paymentGateway.getTransactionsByOrderId(any(), any())).thenReturn(emptyList())
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createBrand(): Brand {
        return brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
    }

    private fun createProduct(stockQuantity: StockQuantity = StockQuantity.of(100)): Product {
        val brand = createBrand()
        val product = productRepository.save(
            Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(10000L),
                likes = LikeCount.of(0),
                stockQuantity = stockQuantity,
                brandId = brand.id,
            ),
        )
        stockReservationRepository.setStock(product.id, stockQuantity.value)
        return product
    }

    private fun issueEntryToken(userId: Long): String {
        val token = UUID.randomUUID().toString()
        entryTokenRepository.issue(userId, token, 300L)
        return token
    }

    @DisplayName("DB 비관적 락으로 상품을 조회할 때,")
    @Nested
    inner class FindAllByIdsWithLock {

        @DisplayName("ID 목록으로 조회하면, 해당 상품들을 반환한다.")
        @Transactional
        @Test
        fun returnsProducts_whenValidIdsProvided() {
            // arrange
            val product1 = createProduct(stockQuantity = StockQuantity.of(10))
            val product2 = createProduct(stockQuantity = StockQuantity.of(20))

            // act
            val result = productRepository.findAllByIdsWithLock(listOf(product1.id, product2.id))

            // assert
            assertAll(
                { assertThat(result).hasSize(2) },
                { assertThat(result.map { it.id }).containsExactlyInAnyOrder(product1.id, product2.id) },
            )
        }

        @DisplayName("삭제된 상품은 조회되지 않는다.")
        @Transactional
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val product = createProduct(stockQuantity = StockQuantity.of(10))
            val deletedProduct = createProduct(stockQuantity = StockQuantity.of(20))
            deletedProduct.delete()
            productRepository.save(deletedProduct)

            // act
            val result = productRepository.findAllByIdsWithLock(listOf(product.id, deletedProduct.id))

            // assert
            assertAll(
                { assertThat(result).hasSize(1) },
                { assertThat(result[0].id).isEqualTo(product.id) },
            )
        }

        @DisplayName("ID 오름차순으로 정렬하여 반환한다. (데드락 방지)")
        @Transactional
        @Test
        fun returnsProductsOrderedById() {
            // arrange
            val product1 = createProduct(stockQuantity = StockQuantity.of(10))
            val product2 = createProduct(stockQuantity = StockQuantity.of(20))
            val product3 = createProduct(stockQuantity = StockQuantity.of(30))

            // act — 역순으로 요청해도 ID 오름차순으로 반환
            val result = productRepository.findAllByIdsWithLock(listOf(product3.id, product1.id, product2.id))

            // assert
            assertThat(result.map { it.id }).containsExactly(product1.id, product2.id, product3.id)
        }
    }

    @DisplayName("Redis 재고 선점 동시성 제어")
    @Nested
    inner class StockDeductionConcurrency {

        @DisplayName("동시에 여러 주문이 같은 상품에 들어와도 Redis 재고가 정확히 차감된다.")
        @Test
        fun deductsStockCorrectly_whenConcurrentOrders() {
            // arrange
            val product = createProduct(stockQuantity = StockQuantity.of(100))
            val threadCount = 10
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // 각 userId별로 토큰을 미리 발급
            val tokens = (1..threadCount).associate { i ->
                val userId = i.toLong()
                userId to issueEntryToken(userId)
            }

            // act
            repeat(threadCount) { i ->
                val userId = i.toLong() + 1
                val token = tokens[userId]!!
                executorService.submit {
                    try {
                        orderFacade.placeOrder(
                            userId = userId,
                            items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1))),
                            entryToken = token,
                            cardType = CardType.SAMSUNG,
                            cardNo = "1234-5678-9012-3456",
                        )
                        successCount.incrementAndGet()
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert: Redis 재고 확인 (DB 재고는 consumer가 비동기 차감)
            val redisStock = redisTemplate.opsForValue().get("stock:${product.id}")
            assertAll(
                { assertThat(successCount.get()).isEqualTo(10) },
                { assertThat(redisStock).isEqualTo("90") },
            )
        }

        @DisplayName("재고보다 많은 동시 주문이 들어오면, Redis 재고가 음수가 되지 않는다.")
        @Test
        fun doesNotGoNegative_whenConcurrentOrdersExceedStock() {
            // arrange
            val product = createProduct(stockQuantity = StockQuantity.of(5))
            val threadCount = 10
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // 각 userId별로 토큰을 미리 발급
            val tokens = (1..threadCount).associate { i ->
                val userId = i.toLong()
                userId to issueEntryToken(userId)
            }

            // act
            repeat(threadCount) { i ->
                val userId = i.toLong() + 1
                val token = tokens[userId]!!
                executorService.submit {
                    try {
                        orderFacade.placeOrder(
                            userId = userId,
                            items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1))),
                            entryToken = token,
                            cardType = CardType.SAMSUNG,
                            cardNo = "1234-5678-9012-3456",
                        )
                        successCount.incrementAndGet()
                    } catch (_: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert: Redis 재고 확인
            val redisStock = redisTemplate.opsForValue().get("stock:${product.id}")
            assertAll(
                { assertThat(successCount.get()).isEqualTo(5) },
                { assertThat(failCount.get()).isEqualTo(5) },
                { assertThat(redisStock).isEqualTo("0") },
            )
        }
    }
}
