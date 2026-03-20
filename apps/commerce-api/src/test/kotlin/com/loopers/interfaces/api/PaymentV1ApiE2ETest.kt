package com.loopers.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.catalog.BrandModel
import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentResponse
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
import com.loopers.interfaces.api.payment.PaymentV1Dto
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
class PaymentV1ApiE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderItemJpaRepository: OrderItemJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockkBean
    private lateinit var pgClient: PgClient

    companion object {
        private const val PAYMENT_ENDPOINT = "/api/v1/payments"
        private const val CALLBACK_ENDPOINT = "/internal/v1/payments/callback"
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
        private val DEFAULT_PRODUCT_PRICE = BigDecimal("129000")
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_PRODUCT_QUANTITY = 10
        private const val DEFAULT_CARD_TYPE = "VISA"
        private const val DEFAULT_CARD_NO = "4111111111111111"
        private const val DEFAULT_TRANSACTION_KEY = "txn-e2e-12345"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        val user = UserModel(
            username = Username.of(username),
            password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
            name = DEFAULT_NAME,
            email = Email.of(DEFAULT_EMAIL),
            birthDate = DEFAULT_BIRTH_DATE,
        )
        user.applyEncodedPassword(passwordEncoder.encode(DEFAULT_PASSWORD))
        return userJpaRepository.save(user)
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
            OrderModel(userId = userId, originalPrice = totalPrice, totalPrice = totalPrice),
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

    private fun createPayment(orderId: Long, userId: Long, transactionKey: String? = null): PaymentModel {
        val payment = PaymentModel(
            orderId = orderId,
            userId = userId,
            amount = DEFAULT_PRODUCT_PRICE * BigDecimal(2),
            cardType = DEFAULT_CARD_TYPE,
            cardNo = DEFAULT_CARD_NO,
        )
        transactionKey?.let { payment.updateTransactionKey(it) }
        return paymentJpaRepository.save(payment)
    }

    @DisplayName("POST /api/v1/payments")
    @Nested
    inner class RequestPayment {
        @DisplayName("유효한 결제 요청이면, 201 CREATED와 결제 정보를 반환한다.")
        @Test
        fun returnsCreatedWithPaymentInfoWhenValidRequest() {
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

            val request = PaymentV1Dto.RequestPaymentRequest(
                orderId = order.id,
                cardType = DEFAULT_CARD_TYPE,
                cardNo = DEFAULT_CARD_NO,
            )

            // act & assert
            mockMvc.perform(
                post(PAYMENT_ENDPOINT)
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentId").isNumber)
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionKey").value(DEFAULT_TRANSACTION_KEY))
        }

        @DisplayName("PG 서버 장애 시에도, 201 CREATED와 PENDING 결제를 반환한다.")
        @Test
        fun returnsCreatedWithPendingPaymentWhenPgFails() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)

            every { pgClient.requestPayment(any()) } throws
                CoreException(ErrorType.INTERNAL_ERROR, "PG 서버 불안정")

            val request = PaymentV1Dto.RequestPaymentRequest(
                orderId = order.id,
                cardType = DEFAULT_CARD_TYPE,
                cardNo = DEFAULT_CARD_NO,
            )

            // act & assert
            mockMvc.perform(
                post(PAYMENT_ENDPOINT)
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionKey").doesNotExist())
        }
    }

    @DisplayName("GET /api/v1/payments/{paymentId}")
    @Nested
    inner class GetPayment {
        @DisplayName("본인의 결제를 조회하면, 200 OK와 결제 상세 정보를 반환한다.")
        @Test
        fun returnsOkWithPaymentDetailWhenOwnPayment() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)
            val payment = createPayment(orderId = order.id, userId = user.id, transactionKey = DEFAULT_TRANSACTION_KEY)

            // act & assert
            mockMvc.perform(
                get("$PAYMENT_ENDPOINT/${payment.id}")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(payment.id))
                .andExpect(jsonPath("$.data.orderId").value(order.id))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.cardType").value(DEFAULT_CARD_TYPE))
        }

        @DisplayName("다른 사용자의 결제를 조회하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorizedWhenOtherUsersPayment() {
            // arrange
            val owner = createUser("owner")
            createUser("other")
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = owner.id)
            createOrderItem(orderId = order.id, productId = product.id)
            val payment = createPayment(orderId = order.id, userId = owner.id)

            // act & assert
            mockMvc.perform(
                get("$PAYMENT_ENDPOINT/${payment.id}")
                    .header("X-Loopers-LoginId", "other")
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isUnauthorized)
        }
    }

    @DisplayName("POST /internal/v1/payments/callback")
    @Nested
    inner class PaymentCallback {
        @DisplayName("SUCCESS 콜백이면, 200 OK를 반환하고 결제가 SUCCESS로 전이된다.")
        @Test
        fun returnsOkAndTransitionsToSuccessWhenSuccessCallback() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)
            val payment = createPayment(orderId = order.id, userId = user.id, transactionKey = DEFAULT_TRANSACTION_KEY)

            val request = PaymentV1Dto.CallbackRequest(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "SUCCESS",
            )

            // act
            mockMvc.perform(
                post(CALLBACK_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("FAILED 콜백이면, 200 OK를 반환하고 결제가 FAILED로 전이된다.")
        @Test
        fun returnsOkAndTransitionsToFailedWhenFailedCallback() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val stockAfterOrder = DEFAULT_PRODUCT_QUANTITY - 2
            val product = createProduct(brandId = brand.id, quantity = stockAfterOrder)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)
            val payment = createPayment(orderId = order.id, userId = user.id, transactionKey = DEFAULT_TRANSACTION_KEY)

            val expectedReason = "잔액 부족"
            val request = PaymentV1Dto.CallbackRequest(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "FAILED",
                reason = expectedReason,
            )

            // act
            mockMvc.perform(
                post(CALLBACK_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            assertAll(
                { assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(updatedPayment.failReason).isEqualTo(expectedReason) },
            )
        }

        @DisplayName("콜백 엔드포인트는 인증 없이 접근 가능하다.")
        @Test
        fun callbackEndpointIsAccessibleWithoutAuth() {
            // arrange
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brandId = brand.id)
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = product.id)
            createPayment(orderId = order.id, userId = user.id, transactionKey = DEFAULT_TRANSACTION_KEY)

            val request = PaymentV1Dto.CallbackRequest(
                transactionKey = DEFAULT_TRANSACTION_KEY,
                status = "SUCCESS",
            )

            // act & assert — 인증 헤더 없이 요청
            mockMvc.perform(
                post(CALLBACK_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
                .andExpect(status().isOk)
        }
    }
}
