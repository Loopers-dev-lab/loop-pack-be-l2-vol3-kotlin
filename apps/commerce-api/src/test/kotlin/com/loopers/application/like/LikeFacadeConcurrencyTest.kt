package com.loopers.application.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.like.ProductLike
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.like.ProductLikeJpaRepository
import com.loopers.domain.product.ProductService
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class LikeFacadeConcurrencyTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productService: ProductService,
    private val productJpaRepository: ProductJpaRepository,
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var user: User
    private lateinit var product: Product

    companion object {
        private const val PASSWORD = "abcd1234"
        private const val THREAD_COUNT = 10
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("좋아요 동시성 - 중복 좋아요 방지")
    @Nested
    inner class ConcurrentLike {
        @DisplayName("동일 사용자가 동일 상품에 동시에 좋아요를 누르면, 좋아요가 1건만 생성된다.")
        @Test
        fun createsOnlyOneLike_whenSameUserLikesConcurrently() {
            // arrange
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val latch = CountDownLatch(THREAD_COUNT)

            // act
            repeat(THREAD_COUNT) {
                executor.submit {
                    try {
                        likeFacade.like(loginId = user.loginId, password = PASSWORD, productId = product.id)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val likes = productLikeJpaRepository.findAllByUserId(user.id)
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(likes).hasSize(1) },
                { assertThat(updatedProduct.likeCount).isEqualTo(1) },
            )
        }

        @DisplayName("서로 다른 10명이 동일 상품에 동시에 좋아요를 누르면, likeCount가 정확히 10 증가한다.")
        @Test
        fun increasesLikeCountExactly_whenDifferentUsersLikeConcurrently() {
            // arrange
            val users = (1..THREAD_COUNT).map { i ->
                userJpaRepository.save(User(loginId = "tester0$i", password = PASSWORD, name = "유저$i", birth = "2000-01-01", email = "user$i@test.com"))
            }
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val latch = CountDownLatch(THREAD_COUNT)

            // act
            users.forEach { u ->
                executor.submit {
                    try {
                        likeFacade.like(loginId = u.loginId, password = PASSWORD, productId = product.id)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val likes = productLikeJpaRepository.findAll()
                .filter { it.productId == product.id }
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(likes).hasSize(THREAD_COUNT) },
                { assertThat(updatedProduct.likeCount).isEqualTo(THREAD_COUNT) },
            )
        }
    }

    @DisplayName("좋아요 취소 동시성 - 중복 취소 방지")
    @Nested
    inner class ConcurrentUnlike {
        @DisplayName("동일 사용자가 동일 상품에 동시에 좋아요 취소를 누르면, likeCount가 1만 감소한다.")
        @Test
        fun decreasesLikeCountOnce_whenSameUserUnlikesConcurrently() {
            // arrange
            productLikeJpaRepository.save(ProductLike(userId = user.id, productId = product.id))
            productService.increaseLikeCount(product.id)

            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val latch = CountDownLatch(THREAD_COUNT)

            // act
            repeat(THREAD_COUNT) {
                executor.submit {
                    try {
                        likeFacade.unlike(loginId = user.loginId, password = PASSWORD, productId = product.id)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val likes = productLikeJpaRepository.findAllByUserId(user.id)
                .filter { it.productId == product.id }
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(likes).isEmpty() },
                { assertThat(updatedProduct.likeCount).isEqualTo(0) },
            )
        }
    }
}
