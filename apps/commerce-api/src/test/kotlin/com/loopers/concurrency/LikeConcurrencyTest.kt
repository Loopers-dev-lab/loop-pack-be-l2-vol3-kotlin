package com.loopers.concurrency

import com.loopers.application.like.LikeService
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.like.LikeJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("좋아요 동시성 테스트")
@SpringBootTest
class LikeConcurrencyTest @Autowired constructor(
    private val likeService: LikeService,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val likeJpaRepository: LikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("동일 (userId, productId)에 10개 스레드가 동시에 좋아요하면, 정확히 1개의 레코드만 존재한다.")
    @Test
    fun maintainsUniqueness_whenConcurrentLikes() {
        // arrange
        val threadCount = 10
        val userId = 1L
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            ),
        )

        val latch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(threadCount)

        // act
        repeat(threadCount) {
            executorService.submit {
                try {
                    latch.await()
                    likeService.addLike(userId = userId, productId = product.id)
                } catch (_: Exception) {
                    // 유니크 제약 위반 예외는 무시 — 정합성만 검증
                }
            }
        }

        latch.countDown()
        executorService.shutdown()
        executorService.awaitTermination(10, TimeUnit.SECONDS)

        // assert
        val likes = likeJpaRepository.findAll()
        assertThat(likes).hasSize(1)
    }

    @DisplayName("서로 다른 10명이 동시에 좋아요하면, likeCount가 정확히 10이 된다.")
    @Test
    fun maintainsCorrectLikeCount_whenConcurrentLikesFromDifferentUsers() {
        // arrange
        val threadCount = 10
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            ),
        )

        val latch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(threadCount)

        // act
        repeat(threadCount) { index ->
            val userId = (index + 1).toLong()
            executorService.submit {
                try {
                    latch.await()
                    likeService.addLike(userId = userId, productId = product.id)
                } catch (_: Exception) {
                }
            }
        }

        latch.countDown()
        executorService.shutdown()
        executorService.awaitTermination(10, TimeUnit.SECONDS)

        // assert
        val likes = likeJpaRepository.findAll()
        val updatedProduct = productJpaRepository.findById(product.id).get()
        assertAll(
            { assertThat(likes).hasSize(threadCount) },
            { assertThat(updatedProduct.likeCount).isEqualTo(threadCount) },
        )
    }
}
