package com.loopers.concurrency

import com.loopers.domain.Money
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductService
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class ProductConcurrencyTest @Autowired constructor(
    private val productService: ProductService,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    txManager: PlatformTransactionManager,
) {

    private val txTemplate = TransactionTemplate(txManager)

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createProduct(stockQuantity: Int): Product {
        val brand = brandJpaRepository.save(Brand(name = "테스트 브랜드"))
        return productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "테스트 상품",
                price = Money(10000),
                stockQuantity = stockQuantity,
            ),
        )
    }

    @Nested
    @DisplayName("재고 차감 동시성")
    inner class DecreaseStock {

        @Test
        @Timeout(30, unit = TimeUnit.SECONDS)
        @DisplayName("[문제] dirty checking으로 재고를 차감하면, Lost Update가 발생한다")
        fun dirtyCheckingCausesLostUpdate() {
            // arrange
            val initialStock = 100
            val product = createProduct(initialStock)
            val threadCount = 100
            val executorService = Executors.newFixedThreadPool(32)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // act — dirty checking: 각 스레드가 읽은 시점의 재고에서 1을 빼고 덮어쓴다
            // UPDATE SET stock_quantity = (메모리값) WHERE id = ?
            // → 여러 스레드가 같은 값을 읽고 같은 값으로 덮어쓰므로 일부 차감이 유실됨
            repeat(threadCount) {
                executorService.submit {
                    try {
                        startLatch.await()
                        txTemplate.execute {
                            val p = productJpaRepository.findById(product.id).get()
                            p.decreaseStock(1)
                        }
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                // 모든 스레드가 예외 없이 "성공"했지만
                { assertThat(successCount.get()).isEqualTo(threadCount) },
                // 재고가 0이어야 하는데 Lost Update로 인해 0보다 크다
                { assertThat(updatedProduct.stockQuantity).isGreaterThan(0) },
            )
        }

        @Test
        @Timeout(10, unit = TimeUnit.SECONDS)
        @DisplayName("[해결] Atomic Update로 재고를 차감하면, 정확하게 재고만큼만 성공한다")
        fun atomicUpdatePreventsLostUpdate() {
            // arrange
            val initialStock = 10
            val product = createProduct(initialStock)
            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act — Atomic Update: UPDATE SET stock = stock - 1 WHERE stock >= 1
            // → DB 레벨에서 원자적으로 차감하므로 Lost Update 불가
            repeat(threadCount) {
                executorService.submit {
                    try {
                        startLatch.await()
                        productService.decreaseStock(product.id, 1)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(successCount.get()).isEqualTo(initialStock) },
                { assertThat(failCount.get()).isEqualTo(threadCount - initialStock) },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(0) },
            )
        }
    }

    @Nested
    @DisplayName("좋아요 수 동시성")
    inner class IncreaseLikeCount {

        @Test
        @Timeout(30, unit = TimeUnit.SECONDS)
        @DisplayName("[문제] dirty checking으로 좋아요를 증가하면, Lost Update가 발생한다")
        fun dirtyCheckingCausesLostUpdate() {
            // arrange
            val product = createProduct(100)
            val threadCount = 100
            val executorService = Executors.newFixedThreadPool(32)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // act — dirty checking: 각 스레드가 읽은 시점의 likeCount에 1을 더해 덮어쓴다
            // UPDATE SET like_count = (메모리값) WHERE id = ?
            // → 여러 스레드가 likeCount=0을 읽고 1로 덮어씀 → 증가분 유실
            repeat(threadCount) {
                executorService.submit {
                    try {
                        startLatch.await()
                        txTemplate.execute {
                            val p = productJpaRepository.findById(product.id).get()
                            p.increaseLikeCount()
                        }
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                // 모든 스레드가 예외 없이 "성공"했지만
                { assertThat(successCount.get()).isEqualTo(threadCount) },
                // 좋아요 수가 100이어야 하는데 Lost Update로 인해 100보다 적다
                { assertThat(updatedProduct.likeCount).isLessThan(threadCount) },
            )
        }

        @Test
        @Timeout(10, unit = TimeUnit.SECONDS)
        @DisplayName("[해결] Atomic Update로 좋아요를 증가하면, 모두 정확하게 반영된다")
        fun atomicUpdatePreventsLostUpdate() {
            // arrange
            val product = createProduct(100)
            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act — Atomic Update: UPDATE SET like_count = like_count + 1
            // → DB 레벨에서 원자적으로 증가하므로 Lost Update 불가
            repeat(threadCount) {
                executorService.submit {
                    try {
                        startLatch.await()
                        productService.increaseLikeCount(product.id)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(successCount.get()).isEqualTo(threadCount) },
                { assertThat(failCount.get()).isEqualTo(0) },
                { assertThat(updatedProduct.likeCount).isEqualTo(threadCount) },
            )
        }
    }
}
