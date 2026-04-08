package com.loopers.application.queue

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemRequest
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueueService
import com.loopers.domain.queue.QueueTokenRepository
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@TestPropertySource(properties = ["queue.enabled=true"])
class QueueFacadeConcurrencyTest @Autowired constructor(
    private val queueFacade: QueueFacade,
    private val orderFacade: OrderFacade,
    private val queueService: QueueService,
    private val queueRepository: QueueRepository,
    private val queueTokenRepository: QueueTokenRepository,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val PASSWORD = "abcd1234"
    }

    @AfterEach
    fun cleanUp() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("대기열 동시 진입")
    @Nested
    inner class ConcurrentEnter {
        @DisplayName("50명이 동시에 대기열에 진입하면, 정확히 50명이 등록되고 중복이 없다.")
        @Test
        fun exactlyFiftyUsersEntered_whenConcurrentEntry() {
            // arrange
            val threadCount = 50
            val users = (1..threadCount).map { i ->
                userJpaRepository.save(
                    User(loginId = "queueuser$i", password = PASSWORD, name = "유저$i", birth = "2000-01-01", email = "queue$i@test.com"),
                )
            }
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            users.forEach { u ->
                executor.submit {
                    try {
                        queueFacade.enterQueue(u.loginId, PASSWORD)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertAll(
                { assertThat(successCount.get()).isEqualTo(threadCount) },
                { assertThat(failCount.get()).isEqualTo(0) },
                { assertThat(queueRepository.getSize()).isEqualTo(threadCount.toLong()) },
            )
        }

        @DisplayName("동일 사용자가 동시에 2번 진입하면, 1번만 성공한다.")
        @Test
        fun onlyOneSucceeds_whenSameUserEntersTwice() {
            // arrange
            val user = userJpaRepository.save(
                User(loginId = "dupeuser", password = PASSWORD, name = "중복유저", birth = "2000-01-01", email = "dupe@test.com"),
            )
            val executor = Executors.newFixedThreadPool(2)
            val latch = CountDownLatch(2)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            repeat(2) {
                executor.submit {
                    try {
                        queueFacade.enterQueue(user.loginId, PASSWORD)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertAll(
                { assertThat(successCount.get()).isEqualTo(1) },
                { assertThat(failCount.get()).isEqualTo(1) },
                { assertThat(queueRepository.getSize()).isEqualTo(1L) },
            )
        }
    }

    @DisplayName("토큰 만료")
    @Nested
    inner class TokenExpiry {
        @DisplayName("TTL이 만료된 토큰으로는 주문할 수 없다.")
        @Test
        fun cannotOrderWithExpiredToken() {
            // arrange
            val user = userJpaRepository.save(
                User(loginId = "expireuser", password = PASSWORD, name = "만료유저", birth = "2000-01-01", email = "expire@test.com"),
            )
            queueTokenRepository.issueToken(user.id, 1)

            // act
            Thread.sleep(2000)

            // assert
            assertAll(
                { assertThat(queueTokenRepository.hasToken(user.id)).isFalse() },
                { assertThat(queueService.validateToken(user.id, "any-token")).isFalse() },
            )
        }
    }

    @DisplayName("처리량 초과")
    @Nested
    inner class ThroughputOverflow {
        @DisplayName("60명 진입 후 스케줄러 1회 실행(batchSize=30)하면, 30명만 토큰을 받고 30명은 대기한다.")
        @Test
        fun onlyBatchSizeUsersGetTokens() {
            // arrange
            val users = (1L..60L).map { i ->
                userJpaRepository.save(
                    User(loginId = "overflow$i", password = PASSWORD, name = "유저$i", birth = "2000-01-01", email = "overflow$i@test.com"),
                )
            }
            users.forEach { u ->
                queueRepository.addIfAbsent(u.id, System.currentTimeMillis().toDouble() + u.id)
            }

            // act - 스케줄러 1회 실행 (batchSize=30)
            val processed = queueService.popAndIssueTokens(30)

            // assert
            val tokenCount = users.count { queueTokenRepository.hasToken(it.id) }
            val remainingInQueue = queueRepository.getSize()
            assertAll(
                { assertThat(processed).isEqualTo(30) },
                { assertThat(tokenCount).isEqualTo(30) },
                { assertThat(remainingInQueue).isEqualTo(30L) },
            )
        }

        @DisplayName("토큰이 없는 사용자가 주문을 시도하면, QUEUE_TOKEN_REQUIRED 에러가 발생한다.")
        @Test
        fun orderFailsWithoutToken() {
            // arrange
            val user = userJpaRepository.save(
                User(loginId = "notoken", password = PASSWORD, name = "토큰없는유저", birth = "2000-01-01", email = "notoken@test.com"),
            )
            val brand = brandJpaRepository.save(Brand(name = "테스트브랜드", description = "테스트"))
            val product = productJpaRepository.save(
                Product(brandId = brand.id, name = "테스트상품", description = "테스트", price = 10000, stockQuantity = 100),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = user.loginId,
                    password = PASSWORD,
                    itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.QUEUE_TOKEN_REQUIRED)
        }
    }
}
