package com.loopers.concurrency

import com.loopers.application.user.like.UserProductLikeCommand
import com.loopers.application.user.like.UserProductLikeCancelUseCase
import com.loopers.application.user.like.UserProductLikeRegisterUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("ProductLike 동시성 테스트")
@SpringBootTest
class ProductLikeConcurrencyTest
@Autowired
constructor(
    private val registerUseCase: UserProductLikeRegisterUseCase,
    private val cancelUseCase: UserProductLikeCancelUseCase,
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
    }

    private var productId: Long = 0

    private fun createUser(loginId: String, email: String): Long {
        val user = User.register(
            loginId = loginId,
            rawPassword = "Password1!",
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 1),
            email = email,
            passwordHasher = passwordHasher,
        )
        return userRepository.save(user).id!!
    }

    private fun assertLikeCountSynced(expectedCount: Int) {
        val likeCount = productLikeRepository.countByProductId(productId)
        val product = productRepository.findById(productId)!!
        assertThat(likeCount).isEqualTo(expectedCount)
        assertThat(product.likeCount).isEqualTo(expectedCount)
    }

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "좋아요 테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("같은 사용자가 동시에 10번 좋아요 → 1건만 등록, unexpected exception=0")
    fun register_concurrent_sameUser_onlyOne() {
        val userId = createUser(loginId = "liketest1", email = "like1@test.com")

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val unexpectedExceptions = AtomicInteger(0)

        repeat(threadCount) {
            executor.submit {
                try {
                    registerUseCase.register(
                        UserProductLikeCommand.Register(
                            userId = userId,
                            productId = productId,
                        ),
                    )
                } catch (e: Exception) {
                    unexpectedExceptions.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertThat(unexpectedExceptions.get()).isEqualTo(0)
        assertLikeCountSynced(expectedCount = 1)
    }

    @Test
    @DisplayName("같은 사용자가 동시에 10번 좋아요 해제 → row=0, likeCount=0, unexpected exception=0")
    fun cancel_concurrent_sameUser_onlyOne() {
        val userId = createUser(loginId = "liketestcancel", email = "likecancel@test.com")
        registerUseCase.register(UserProductLikeCommand.Register(userId = userId, productId = productId))
        assertLikeCountSynced(expectedCount = 1)

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val unexpectedExceptions = AtomicInteger(0)

        repeat(threadCount) {
            executor.submit {
                try {
                    cancelUseCase.cancel(
                        UserProductLikeCommand.Cancel(
                            userId = userId,
                            productId = productId,
                        ),
                    )
                } catch (e: Exception) {
                    unexpectedExceptions.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertThat(unexpectedExceptions.get()).isEqualTo(0)
        assertLikeCountSynced(expectedCount = 0)
    }

    @Test
    @DisplayName("다른 사용자 10명이 동시에 좋아요 → 10건 모두 등록, unexpected exception=0")
    fun register_concurrent_differentUsers_allSucceed() {
        val userIds = (1..10).map { idx ->
            createUser(loginId = "liketest$idx", email = "like$idx@test.com")
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val successCount = AtomicInteger(0)
        val unexpectedExceptions = AtomicInteger(0)

        userIds.forEach { userId ->
            executor.submit {
                try {
                    registerUseCase.register(
                        UserProductLikeCommand.Register(
                            userId = userId,
                            productId = productId,
                        ),
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    unexpectedExceptions.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertThat(unexpectedExceptions.get()).isEqualTo(0)
        assertThat(successCount.get()).isEqualTo(10)
        assertLikeCountSynced(expectedCount = 10)
    }

    @Test
    @DisplayName("좋아요/취소가 동시에 섞여도 최종 좋아요 수가 정합하게 반영된다")
    fun registerAndCancel_concurrent_mixedOperations_consistentCount() {
        val cancelUserIds = (1..50).map { idx ->
            createUser(loginId = "likecancel$idx", email = "likecancel$idx@test.com")
        }
        val registerUserIds = (51..100).map { idx ->
            createUser(loginId = "likecancel$idx", email = "likecancel$idx@test.com")
        }

        cancelUserIds.forEach { userId ->
            registerUseCase.register(
                UserProductLikeCommand.Register(
                    userId = userId,
                    productId = productId,
                ),
            )
        }
        assertLikeCountSynced(expectedCount = 50)

        val threadCount = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val unexpectedExceptions = AtomicInteger(0)

        cancelUserIds.forEach { userId ->
            executor.submit {
                try {
                    readyLatch.countDown()
                    startLatch.await()
                    cancelUseCase.cancel(
                        UserProductLikeCommand.Cancel(
                            userId = userId,
                            productId = productId,
                        ),
                    )
                } catch (e: Exception) {
                    unexpectedExceptions.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        registerUserIds.forEach { userId ->
            executor.submit {
                try {
                    readyLatch.countDown()
                    startLatch.await()
                    registerUseCase.register(
                        UserProductLikeCommand.Register(
                            userId = userId,
                            productId = productId,
                        ),
                    )
                } catch (e: Exception) {
                    unexpectedExceptions.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        readyLatch.await()
        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        assertThat(unexpectedExceptions.get()).isEqualTo(0)
        assertLikeCountSynced(expectedCount = 50)
    }
}
