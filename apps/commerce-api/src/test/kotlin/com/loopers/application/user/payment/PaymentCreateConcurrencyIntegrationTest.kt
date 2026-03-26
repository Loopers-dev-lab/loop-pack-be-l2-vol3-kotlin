package com.loopers.application.user.payment

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.check
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("PaymentCreateUseCase 동시성 통합 테스트")
@SpringBootTest
class PaymentCreateConcurrencyIntegrationTest
@Autowired
constructor(
    private val paymentCreateUseCase: PaymentCreateUseCase,
    private val paymentRepository: PaymentRepository,
    private val orderCreateUseCase: OrderCreateUseCase,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockitoBean
    private lateinit var pgPaymentPort: PgPaymentPort

    private var orderId: Long = 0
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        given(pgPaymentPort.isAvailable()).willReturn(true)
        given(pgPaymentPort.requestPayment(check { }))
            .willReturn(PgPaymentResponse.Accepted("txn-concurrent-test"))

        val user = User.register(
            loginId = "concurrentuser",
            rawPassword = "Password1!",
            name = "동시성테스터",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "concurrent@test.com",
            passwordHasher = passwordHasher,
        )
        val savedUser = userRepository.save(user)
        userId = savedUser.id!!

        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), "loopers.admin")
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), "loopers.admin")

        val product = Product.register(
            name = "동시성 테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(8000)),
            brandId = activeBrand.id!!,
        )
        val savedProduct = productRepository.save(product, "loopers.admin")
        val activeProduct = productRepository.save(savedProduct.activate(), "loopers.admin")

        productStockRepository.save(
            ProductStock.create(productId = activeProduct.id!!, initialQuantity = Quantity(100)),
            "loopers.admin",
        )

        val result = orderCreateUseCase.create(
            OrderCreateCommand(
                userId = userId,
                idempotencyKey = UUID.randomUUID().toString(),
                items = listOf(OrderCreateCommand.Item(productId = activeProduct.id!!, quantity = 2)),
            ),
        )
        orderId = result.orderId
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("같은 orderId + 다른 idempotencyKey로 동시 요청 시")
    inner class WhenDifferentIdempotencyKeys {

        @Test
        @DisplayName("PENDING Payment는 최대 1건만 생성된다")
        fun create_concurrent_onlyOnePending() {
            // arrange
            val threadCount = 5
            val executor = Executors.newFixedThreadPool(threadCount)
            val readyLatch = CountDownLatch(threadCount)
            val startLatch = CountDownLatch(1)
            val finishLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val errorTypes = CopyOnWriteArrayList<ErrorType>()

            // act
            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await()
                        paymentCreateUseCase.create(
                            PaymentCreateCommand(
                                userId = userId,
                                orderId = orderId,
                                idempotencyKey = UUID.randomUUID().toString(),
                                cardType = "VISA",
                                cardNo = "4111111111111234",
                            ),
                        )
                        successCount.incrementAndGet()
                    } catch (e: CoreException) {
                        errorTypes.add(e.errorType)
                    } finally {
                        finishLatch.countDown()
                    }
                }
            }

            try {
                assertThat(readyLatch.await(5, TimeUnit.SECONDS))
                    .describedAs("모든 worker가 5초 내에 준비되어야 한다")
                    .isTrue()
                startLatch.countDown()
                assertThat(finishLatch.await(10, TimeUnit.SECONDS))
                    .describedAs("모든 worker가 10초 내에 완료되어야 한다")
                    .isTrue()
            } finally {
                startLatch.countDown()
                executor.shutdownNow()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }

            // assert
            val pendingPayments = paymentRepository.findAllByOrderId(orderId)
                .filter { it.status == Payment.Status.PENDING }

            assertThat(pendingPayments).hasSize(1)
            assertThat(successCount.get()).isEqualTo(1)
            assertThat(errorTypes).hasSize(threadCount - 1)
            assertThat(errorTypes).allMatch { it == ErrorType.PAYMENT_ACTIVE_PENDING_EXISTS }
        }
    }

    @Nested
    @DisplayName("같은 orderId + 같은 idempotencyKey + 같은 요청으로 동시 호출 시")
    inner class WhenSameIdempotencyKeySameRequest {

        @Test
        @DisplayName("1건만 생성되고 나머지는 idempotent replay로 처리된다")
        fun create_concurrent_sameKey_sameRequest_replay() {
            // arrange
            val threadCount = 5
            val executor = Executors.newFixedThreadPool(threadCount)
            val readyLatch = CountDownLatch(threadCount)
            val startLatch = CountDownLatch(1)
            val finishLatch = CountDownLatch(threadCount)
            val newlyCreatedCount = AtomicInteger(0)
            val replayCount = AtomicInteger(0)
            val errorTypes = CopyOnWriteArrayList<ErrorType>()

            val sharedKey = UUID.randomUUID().toString()

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await()
                        val result = paymentCreateUseCase.create(
                            PaymentCreateCommand(
                                userId = userId,
                                orderId = orderId,
                                idempotencyKey = sharedKey,
                                cardType = "VISA",
                                cardNo = "4111111111111234",
                            ),
                        )
                        when (result) {
                            is PaymentCreateResult.NewlyCreated -> newlyCreatedCount.incrementAndGet()
                            is PaymentCreateResult.IdempotentReplay -> replayCount.incrementAndGet()
                        }
                    } catch (e: CoreException) {
                        errorTypes.add(e.errorType)
                    } finally {
                        finishLatch.countDown()
                    }
                }
            }

            try {
                assertThat(readyLatch.await(5, TimeUnit.SECONDS))
                    .describedAs("모든 worker가 5초 내에 준비되어야 한다")
                    .isTrue()
                startLatch.countDown()
                assertThat(finishLatch.await(10, TimeUnit.SECONDS))
                    .describedAs("모든 worker가 10초 내에 완료되어야 한다")
                    .isTrue()
            } finally {
                startLatch.countDown()
                executor.shutdownNow()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }

            // assert
            val allPayments = paymentRepository.findAllByOrderId(orderId)

            assertThat(allPayments).hasSize(1)
            assertThat(newlyCreatedCount.get()).isEqualTo(1)
            assertThat(replayCount.get()).isEqualTo(threadCount - 1)
            assertThat(errorTypes).isEmpty()
        }
    }

    @Nested
    @DisplayName("같은 orderId + 같은 idempotencyKey + 다른 요청으로 동시 호출 시")
    inner class WhenSameIdempotencyKeyDifferentRequest {

        @Test
        @DisplayName("1건 생성 후 나머지는 PAYMENT_IDEMPOTENCY_CONFLICT로 처리된다")
        fun create_concurrent_sameKey_differentRequest_conflict() {
            // arrange
            val threadCount = 5
            val executor = Executors.newFixedThreadPool(threadCount)
            val readyLatch = CountDownLatch(threadCount)
            val startLatch = CountDownLatch(1)
            val finishLatch = CountDownLatch(threadCount)
            val newlyCreatedCount = AtomicInteger(0)
            val errorTypes = CopyOnWriteArrayList<ErrorType>()

            val sharedKey = UUID.randomUUID().toString()

            // act
            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await()
                        paymentCreateUseCase.create(
                            PaymentCreateCommand(
                                userId = userId,
                                orderId = orderId,
                                idempotencyKey = sharedKey,
                                cardType = "VISA",
                                cardNo = "card-variant-$i",
                            ),
                        )
                        newlyCreatedCount.incrementAndGet()
                    } catch (e: CoreException) {
                        errorTypes.add(e.errorType)
                    } finally {
                        finishLatch.countDown()
                    }
                }
            }

            try {
                assertThat(readyLatch.await(5, TimeUnit.SECONDS))
                    .describedAs("모든 worker가 5초 내에 준비되어야 한다")
                    .isTrue()
                startLatch.countDown()
                assertThat(finishLatch.await(10, TimeUnit.SECONDS))
                    .describedAs("모든 worker가 10초 내에 완료되어야 한다")
                    .isTrue()
            } finally {
                startLatch.countDown()
                executor.shutdownNow()
                executor.awaitTermination(5, TimeUnit.SECONDS)
            }

            // assert
            val allPayments = paymentRepository.findAllByOrderId(orderId)

            assertThat(allPayments).hasSize(1)
            assertThat(newlyCreatedCount.get()).isEqualTo(1)
            assertThat(errorTypes).hasSize(threadCount - 1)
            assertThat(errorTypes).allMatch { it == ErrorType.PAYMENT_IDEMPOTENCY_CONFLICT }
        }
    }
}
