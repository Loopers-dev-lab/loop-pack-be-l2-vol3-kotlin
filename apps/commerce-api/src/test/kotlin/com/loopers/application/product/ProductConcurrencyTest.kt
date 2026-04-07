package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.support.TransactionTemplate

/**
 * 동시성 테스트: Lost Update 문제 검증
 * - 락 없이 동시 재고 차감 시 Lost Update 발생을 증명
 * - 비관적 락(PESSIMISTIC_WRITE)으로 Lost Update 해결을 검증
 */
@SpringBootTest
class ProductConcurrencyTest @Autowired constructor(
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val transactionTemplate: TransactionTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createProduct(stock: Int): Product {
        val brand = brandJpaRepository.save(Brand(name = "테스트 브랜드", description = "동시성 테스트용"))
        return productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "테스트 상품",
                price = BigDecimal("10000"),
                stock = stock,
                description = null,
                imageUrl = null,
            ),
        )
    }

    @DisplayName("동시성 재고 차감 테스트")
    @Nested
    inner class ConcurrentStockDecrease {

        @DisplayName("락 없이 동시에 재고를 차감하면, Lost Update가 발생하여 재고가 0보다 크다.")
        @Test
        fun lostUpdateOccurs_whenNoLock() {
            // arrange
            val product = createProduct(stock = 100)
            val productId = product.id
            val threadCount = 2
            val decreasePerThread = 50
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            repeat(threadCount) {
                executorService.submit {
                    try {
                        repeat(decreasePerThread) {
                            transactionTemplate.execute {
                                val found = productJpaRepository.findByIdAndDeletedAtIsNull(productId)!!
                                found.decreaseStock(1)
                                productJpaRepository.saveAndFlush(found)
                            }
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val result = productJpaRepository.findByIdAndDeletedAtIsNull(productId)!!
            assertThat(result.stock).isGreaterThan(0)
        }

        @DisplayName("비관적 락으로 동시에 재고를 차감하면, Lost Update가 발생하지 않아 재고가 정확히 0이다.")
        @Test
        fun noLostUpdate_whenPessimisticLock() {
            // arrange
            val product = createProduct(stock = 100)
            val productId = product.id
            val threadCount = 2
            val decreasePerThread = 50
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            repeat(threadCount) {
                executorService.submit {
                    try {
                        repeat(decreasePerThread) {
                            transactionTemplate.execute {
                                val found = productJpaRepository.findByIdWithLock(productId)!!
                                found.decreaseStock(1)
                                productJpaRepository.saveAndFlush(found)
                            }
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val result = productJpaRepository.findByIdAndDeletedAtIsNull(productId)!!
            assertThat(result.stock).isEqualTo(0)
        }
    }
}
