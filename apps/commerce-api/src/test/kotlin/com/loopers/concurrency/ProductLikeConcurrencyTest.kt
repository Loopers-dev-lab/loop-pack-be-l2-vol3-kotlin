package com.loopers.concurrency

import com.loopers.application.user.like.UserProductLikeCommand
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
    @DisplayName("같은 사용자가 동시에 10번 좋아요 → 1건만 등록")
    fun register_concurrent_sameUser_onlyOne() {
        val user = User.register(
            loginId = "liketest1",
            rawPassword = "Password1!",
            name = "테스트",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "like1@test.com",
            passwordHasher = passwordHasher,
        )
        val userId = userRepository.save(user).id!!

        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        repeat(threadCount) {
            executor.submit {
                try {
                    registerUseCase.register(
                        UserProductLikeCommand.Register(
                            userId = userId,
                            productId = productId,
                        ),
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

        val count = productLikeRepository.countByProductId(productId)
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("다른 사용자 10명이 동시에 좋아요 → 10건 모두 등록")
    fun register_concurrent_differentUsers_allSucceed() {
        val userIds = (1..10).map { idx ->
            val user = User.register(
                loginId = "liketest$idx",
                rawPassword = "Password1!",
                name = "테스트유저",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "like$idx@test.com",
                passwordHasher = passwordHasher,
            )
            userRepository.save(user).id!!
        }

        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val successCount = AtomicInteger(0)

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
                } catch (_: Exception) {
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val count = productLikeRepository.countByProductId(productId)
        assertThat(successCount.get()).isEqualTo(10)
        assertThat(count).isEqualTo(10)
    }
}
