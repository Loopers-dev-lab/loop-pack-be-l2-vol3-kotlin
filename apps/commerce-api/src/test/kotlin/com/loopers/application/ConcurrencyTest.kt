package com.loopers.application

import com.loopers.application.like.LikeFacade
import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemRequest
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.product.ProductModel
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponIssueJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@DisplayName("동시성 테스트")
class ConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val likeFacade: LikeFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrandAndProduct(stockQuantity: Int = 10, price: Long = 10000L): ProductModel {
        val brand = brandJpaRepository.save(BrandModel(name = "테스트브랜드"))
        return productJpaRepository.save(
            ProductModel(
                name = "테스트상품",
                price = price,
                brandId = brand.id,
                stockQuantity = stockQuantity,
            ),
        )
    }

    @DisplayName("좋아요 동시성")
    @Nested
    inner class LikeConcurrency {
        @DisplayName("10명이 동시에 좋아요를 누르면 likesCount가 정확히 10이 된다")
        @Test
        fun likesCountIsAccurate_whenConcurrentLikes() {
            // arrange
            val product = createBrandAndProduct()
            val threadCount = 10
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)

            // act
            repeat(threadCount) { index ->
                executor.submit {
                    try {
                        likeFacade.likeProduct(userId = index.toLong() + 1, productId = product.id)
                        successCount.incrementAndGet()
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(successCount.get()).isEqualTo(10)
            assertThat(updatedProduct.likesCount).isEqualTo(10L)
        }
    }

    @DisplayName("재고 동시성")
    @Nested
    inner class StockConcurrency {
        @DisplayName("재고 10개인 상품에 15명이 동시 주문하면 10명만 성공하고 재고는 0이 된다")
        @Test
        fun stockIsAccurate_whenConcurrentOrders() {
            // arrange
            val product = createBrandAndProduct(stockQuantity = 10)
            val threadCount = 15
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            repeat(threadCount) { index ->
                executor.submit {
                    try {
                        orderFacade.createOrder(
                            userId = index.toLong() + 1,
                            items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
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
            executor.shutdown()

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(successCount.get()).isEqualTo(10)
            assertThat(failCount.get()).isEqualTo(5)
            assertThat(updatedProduct.stockQuantity).isEqualTo(0)
        }
    }

    @DisplayName("쿠폰 동시성")
    @Nested
    inner class CouponConcurrency {
        @DisplayName("발급 쿠폰 1장을 여러 요청이 동시에 사용하면 1건만 성공한다")
        @Test
        fun onlyOneOrderSucceeds_whenConcurrentCouponUsage() {
            // arrange
            val product = createBrandAndProduct(stockQuantity = 100, price = 50000L)
            val coupon = couponJpaRepository.save(
                CouponModel(
                    name = "테스트쿠폰",
                    type = CouponType.FIXED,
                    value = 3000L,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            val userId = 1L
            val couponIssue = couponIssueJpaRepository.save(
                CouponIssueModel(couponId = coupon.id, userId = userId),
            )

            val threadCount = 5
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        orderFacade.createOrder(
                            userId = userId,
                            items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                            couponIssueId = couponIssue.id,
                        )
                        successCount.incrementAndGet()
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertThat(successCount.get()).isEqualTo(1)
            val updatedCouponIssue = couponIssueJpaRepository.findById(couponIssue.id).get()
            assertThat(updatedCouponIssue.status).isEqualTo(CouponIssueStatus.USED)
        }
    }

    @DisplayName("트랜잭션 롤백")
    @Nested
    inner class TransactionRollback {
        @DisplayName("사용 불가 쿠폰으로 주문하면 재고가 원복된다")
        @Test
        fun stockIsRestored_whenCouponIsNotUsable() {
            // arrange
            val product = createBrandAndProduct(stockQuantity = 10)
            val coupon = couponJpaRepository.save(
                CouponModel(
                    name = "사용된쿠폰",
                    type = CouponType.FIXED,
                    value = 1000L,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            val userId = 1L
            val couponIssue = couponIssueJpaRepository.save(
                CouponIssueModel(
                    couponId = coupon.id,
                    userId = userId,
                    status = CouponIssueStatus.USED,
                ),
            )

            // act
            try {
                orderFacade.createOrder(
                    userId = userId,
                    items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
                    couponIssueId = couponIssue.id,
                )
            } catch (_: Exception) {
            }

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(updatedProduct.stockQuantity).isEqualTo(10)
        }
    }
}
