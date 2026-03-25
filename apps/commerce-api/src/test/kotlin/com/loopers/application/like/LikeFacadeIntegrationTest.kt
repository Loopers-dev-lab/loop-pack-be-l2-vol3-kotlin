package com.loopers.application.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.outbox.OutboxEventRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class LikeFacadeIntegrationTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createProduct(likes: LikeCount = LikeCount.of(0)): Product {
        val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        return productRepository.save(
            Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = likes, stockQuantity = StockQuantity.of(100), brandId = brand.id),
        )
    }

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class Like {

        @DisplayName("좋아요하면, Like가 저장되고 LIKED Outbox 이벤트가 생성된다.")
        @Test
        fun createsLikeAndOutboxEvent() {
            // arrange
            val product = createProduct()

            // act
            likeFacade.like(userId = 1L, productId = product.id)

            // assert
            assertThat(likeRepository.existsByUserIdAndProductId(1L, product.id)).isTrue()
            val outboxEvents = outboxEventRepository.findByPublishedAtIsNull()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].eventType).isEqualTo("LIKED")
        }

        @DisplayName("같은 사용자가 동시에 좋아요하면, 예외 없이 Outbox 이벤트가 1건만 생성된다.")
        @Test
        fun handlesRaceCondition_whenSameUserConcurrentlyLikes() {
            // arrange
            val product = createProduct()
            val threadCount = 10
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val exceptionCount = AtomicInteger(0)

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        likeFacade.like(userId = 1L, productId = product.id)
                    } catch (_: Exception) {
                        exceptionCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertThat(exceptionCount.get()).isZero()
            val outboxEvents = outboxEventRepository.findByPublishedAtIsNull()
                .filter { it.eventType == "LIKED" }
            assertThat(outboxEvents).hasSize(1)
        }

        @DisplayName("서로 다른 사용자 10명이 동시에 좋아요하면, Outbox 이벤트가 10건 생성된다.")
        @Test
        fun createsOutboxEventsForAllConcurrentLikes() {
            // arrange
            val product = createProduct()
            val threadCount = 10
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)

            // act
            repeat(threadCount) { index ->
                executor.submit {
                    try {
                        likeFacade.like(userId = index.toLong() + 1, productId = product.id)
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val outboxEvents = outboxEventRepository.findByPublishedAtIsNull()
                .filter { it.eventType == "LIKED" }
            assertThat(outboxEvents).hasSize(threadCount)
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class Unlike {

        @DisplayName("좋아요 취소하면, Like가 삭제되고 UNLIKED Outbox 이벤트가 생성된다.")
        @Test
        fun deletesLikeAndCreatesOutboxEvent() {
            // arrange
            val product = createProduct()
            likeFacade.like(userId = 1L, productId = product.id)

            // act
            likeFacade.unlike(userId = 1L, productId = product.id)

            // assert
            assertThat(likeRepository.existsByUserIdAndProductId(1L, product.id)).isFalse()
            val outboxEvents = outboxEventRepository.findByPublishedAtIsNull()
                .filter { it.eventType == "UNLIKED" }
            assertThat(outboxEvents).hasSize(1)
        }
    }
}
