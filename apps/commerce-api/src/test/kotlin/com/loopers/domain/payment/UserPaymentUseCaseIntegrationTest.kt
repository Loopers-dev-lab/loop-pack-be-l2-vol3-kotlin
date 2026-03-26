package com.loopers.domain.payment

import com.loopers.application.payment.PaymentCallbackCriteria
import com.loopers.application.payment.PaymentCallbackUseCase
import com.loopers.application.payment.RequestPaymentCriteria
import com.loopers.application.payment.UserRequestPaymentUseCase
import com.loopers.domain.catalog.BrandModel
import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.user.Email
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.Username
import com.loopers.infrastructure.catalog.BrandJpaRepository
import com.loopers.infrastructure.catalog.ProductJpaRepository
import com.loopers.infrastructure.order.OrderItemJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.payment.PaymentJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest
class UserPaymentUseCaseIntegrationTest @Autowired constructor(
    private val userRequestPaymentUseCase: UserRequestPaymentUseCase,
    private val paymentCallbackUseCase: PaymentCallbackUseCase,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderItemJpaRepository: OrderItemJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockkBean
    private lateinit var pgClient: PgClient

    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_PRODUCT_QUANTITY = 10
        private val DEFAULT_PRODUCT_PRICE = BigDecimal("129000")
        private const val DEFAULT_CARD_TYPE = "VISA"
        private const val DEFAULT_CARD_NO = "4111111111111111"
        private const val DEFAULT_TRANSACTION_KEY = "txn-test-12345"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        return userJpaRepository.save(
            UserModel(
                username = Username.of(username),
                password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
                name = DEFAULT_NAME,
                email = Email.of(DEFAULT_EMAIL),
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun createBrand(): BrandModel {
        return brandJpaRepository.save(BrandModel(name = "나이키"))
    }

    private fun createProduct(brandId: Long, quantity: Int = DEFAULT_PRODUCT_QUANTITY): ProductModel {
        return productJpaRepository.save(
            ProductModel(
                brandId = brandId,
                name = DEFAULT_PRODUCT_NAME,
                quantity = quantity,
                price = DEFAULT_PRODUCT_PRICE,
            ),
        )
    }

    private fun createOrder(userId: Long): OrderModel {
        val totalPrice = DEFAULT_PRODUCT_PRICE * BigDecimal(2)
        return orderJpaRepository.save(
            OrderModel(
                userId = userId,
                originalPrice = totalPrice,
                totalPrice = totalPrice,
            ),
        )
    }

    private fun createOrderItem(orderId: Long, productId: Long): OrderItemModel {
        return orderItemJpaRepository.save(
            OrderItemModel(
                orderId = orderId,
                productId = productId,
                productName = DEFAULT_PRODUCT_NAME,
                quantity = 2,
                price = DEFAULT_PRODUCT_PRICE,
            ),
        )
    }

    @DisplayName("결제 요청")
    @Nested
    inner class RequestPayment {
        @DisplayName("PG 요청이 성공하면, PENDING 상태 결제와 transactionKey가 반환된다.")
        @Test
        fun returnsPaymentWithTransactionKeyWhenPgRequestSucceeds() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            every { pgClient.requestPayment(any()) } returns PgPaymentResponse(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "PENDING",
            )

            val criteria = RequestPaymentCriteria(
                loginId = DEFAULT_USERNAME,
                orderId = order.id,
                cardType = DEFAULT_CARD_TYPE,
                cardNo = DEFAULT_CARD_NO,
            )

            // act
            val result = userRequestPaymentUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.paymentId).isNotNull() },
                { assertThat(result.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(result.transactionKey).isEqualTo(DEFAULT_TRANSACTION_KEY) },
            )
        }

        @DisplayName("PG 요청이 실패하면, PENDING 상태 결제가 transactionKey 없이 반환된다.")
        @Test
        fun returnsPaymentWithoutTransactionKeyWhenPgRequestFails() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            every { pgClient.requestPayment(any()) } throws
                CoreException(ErrorType.INTERNAL_ERROR, "PG 서버 불안정")

            val criteria = RequestPaymentCriteria(
                loginId = DEFAULT_USERNAME,
                orderId = order.id,
                cardType = DEFAULT_CARD_TYPE,
                cardNo = DEFAULT_CARD_NO,
            )

            // act
            val result = userRequestPaymentUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.paymentId).isNotNull() },
                { assertThat(result.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(result.transactionKey).isNull() },
            )
        }

        @DisplayName("이미 결제 완료된 주문이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenOrderAlreadyPaid() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val existingPayment = paymentJpaRepository.save(
                PaymentModel(
                    orderId = order.id,
                    userId = user.id,
                    amount = order.totalPrice,
                    cardType = DEFAULT_CARD_TYPE,
                    cardNo = DEFAULT_CARD_NO,
                ),
            )
            existingPayment.updateTransactionKey("txn-existing")
            existingPayment.markSuccess()
            paymentJpaRepository.save(existingPayment)

            val criteria = RequestPaymentCriteria(
                loginId = DEFAULT_USERNAME,
                orderId = order.id,
                cardType = DEFAULT_CARD_TYPE,
                cardNo = DEFAULT_CARD_NO,
            )

            // act & assert
            val result = assertThrows<CoreException> {
                userRequestPaymentUseCase.execute(criteria)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("결제 콜백")
    @Nested
    inner class PaymentCallback {
        @DisplayName("SUCCESS 콜백이 오면, 결제가 SUCCESS로 전이된다.")
        @Test
        fun transitionsToSuccessWhenSuccessCallbackReceived() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val payment = paymentJpaRepository.save(
                PaymentModel(
                    orderId = order.id,
                    userId = user.id,
                    amount = order.totalPrice,
                    cardType = DEFAULT_CARD_TYPE,
                    cardNo = DEFAULT_CARD_NO,
                ),
            )
            payment.updateTransactionKey(DEFAULT_TRANSACTION_KEY)
            paymentJpaRepository.save(payment)

            val criteria = PaymentCallbackCriteria(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "SUCCESS",
            )

            // act
            paymentCallbackUseCase.execute(criteria)

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("FAILED 콜백이 오면, 결제가 FAILED로 전이되고 재고가 복구된다.")
        @Test
        fun transitionsToFailedAndRestoresStockWhenFailedCallbackReceived() {
            // arrange
            val expectedRestoredQuantity = DEFAULT_PRODUCT_QUANTITY

            val user = createUser()
            val brand = createBrand()
            val stockAfterOrder = DEFAULT_PRODUCT_QUANTITY - 2
            val product = createProduct(brandId = brand.id, quantity = stockAfterOrder)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val payment = paymentJpaRepository.save(
                PaymentModel(
                    orderId = order.id,
                    userId = user.id,
                    amount = order.totalPrice,
                    cardType = DEFAULT_CARD_TYPE,
                    cardNo = DEFAULT_CARD_NO,
                ),
            )
            payment.updateTransactionKey(DEFAULT_TRANSACTION_KEY)
            paymentJpaRepository.save(payment)

            val criteria = PaymentCallbackCriteria(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "FAILED",
                reason = "잔액 부족",
            )

            // act
            paymentCallbackUseCase.execute(criteria)

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            val restoredProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(updatedPayment.failReason).isEqualTo("잔액 부족") },
                { assertThat(restoredProduct.quantity).isEqualTo(expectedRestoredQuantity) },
            )
        }

        @DisplayName("이미 처리된 결제에 콜백이 오면, 무시된다.")
        @Test
        fun ignoresCallbackWhenPaymentAlreadyProcessed() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            val payment = paymentJpaRepository.save(
                PaymentModel(
                    orderId = order.id,
                    userId = user.id,
                    amount = order.totalPrice,
                    cardType = DEFAULT_CARD_TYPE,
                    cardNo = DEFAULT_CARD_NO,
                ),
            )
            payment.updateTransactionKey(DEFAULT_TRANSACTION_KEY)
            payment.markSuccess()
            paymentJpaRepository.save(payment)

            val criteria = PaymentCallbackCriteria(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "FAILED",
                reason = "중복 콜백",
            )

            // act — 예외 없이 정상 종료
            paymentCallbackUseCase.execute(criteria)

            // assert — 상태 변경 없음
            val unchangedPayment = paymentJpaRepository.findById(payment.id).get()
            assertThat(unchangedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
        }
    }
}
