package com.loopers.application.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductService
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

@SpringBootTest
class LikeFacadeIntegrationTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
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

        @DisplayName("좋아요하면, 상품의 좋아요 수가 1 증가한다.")
        @Test
        fun incrementsLikeCount() {
            // arrange
            val product = createProduct(likes = LikeCount.of(5))

            // act
            likeFacade.like(userId = 1L, productId = product.id)

            // assert
            val updated = productService.getProduct(product.id)
            assertThat(updated.likes).isEqualTo(LikeCount.of(6))
        }

        @DisplayName("서로 다른 사용자 10명이 동시에 좋아요하면, 좋아요 수가 정확히 10 증가한다.")
        @Test
        fun handlesAllConcurrentLikes() {
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
            val updated = productService.getProduct(product.id)
            assertThat(updated.likes).isEqualTo(LikeCount.of(threadCount))
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class Unlike {

        @DisplayName("좋아요 취소하면, 상품의 좋아요 수가 1 감소한다.")
        @Test
        fun decrementsLikeCount() {
            // arrange
            val product = createProduct(likes = LikeCount.of(5))
            likeFacade.like(userId = 1L, productId = product.id)

            // act
            likeFacade.unlike(userId = 1L, productId = product.id)

            // assert
            val updated = productService.getProduct(product.id)
            assertThat(updated.likes).isEqualTo(LikeCount.of(5))
        }
    }
}
