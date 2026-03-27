package com.loopers.interfaces.api.payment

import com.loopers.domain.auth.JwtTokenProvider
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductModel
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.payment.PendingPaymentExpirationScheduler
import com.loopers.infrastructure.payment.PaymentJpaRepository
import com.loopers.infrastructure.payment.PgCallbackSignatureVerifier
import com.loopers.infrastructure.payment.PgClientSimulator
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.order.OrderV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Requires Docker-backed MySQL testcontainers")
@DisplayName("Payment Flow API")
class PaymentFlowApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: com.loopers.infrastructure.product.ProductJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val pgClientSimulator: PgClientSimulator,
    private val pgCallbackSignatureVerifier: PgCallbackSignatureVerifier,
    private val pendingPaymentExpirationScheduler: PendingPaymentExpirationScheduler,
) {
    companion object {
        private const val ORDER_ENDPOINT = "/api/v1/orders"
        private const val CALLBACK_ENDPOINT = "/api/v1/payments/callbacks/pg"
    }

    @AfterEach
    fun tearDown() {
        pgClientSimulator.resetScenario()
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("주문-결제 흐름")
    @Nested
    inner class OrderPaymentFlow {
        @DisplayName("PG 승인 시 주문과 결제가 함께 성공 상태로 남는다")
        @Test
        fun succeeds_whenPgApproves() {
            // arrange
            pgClientSimulator.setScenario(PgClientSimulator.Scenario.SUCCESS)
            val headers = createAuthHeaders()
            val product = createProduct()
            val request = createOrderRequest(product.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            val orderId = requireNotNull(response.body?.data?.id)
            val payment = requireNotNull(paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId))
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.SUCCEEDED) },
            )
        }

        @DisplayName("PG 승인 실패 시 사용자 응답은 CONFLICT이고 내부 결제는 FAILED로 남는다")
        @Test
        fun fails_whenPgDeclines() {
            // arrange
            pgClientSimulator.setScenario(PgClientSimulator.Scenario.FAILURE)
            val headers = createAuthHeaders()
            val product = createProduct()
            val request = createOrderRequest(product.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            val payment = paymentJpaRepository.findAll().single()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
            )
        }

        @DisplayName("PG 타임아웃이면 fallback으로 주문은 성공하고 결제는 PENDING으로 남는다")
        @Test
        fun keepsPaymentPending_whenPgTimesOut() {
            // arrange
            pgClientSimulator.setScenario(PgClientSimulator.Scenario.TIMEOUT)
            val headers = createAuthHeaders()
            val product = createProduct()
            val request = createOrderRequest(product.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            val orderId = requireNotNull(response.body?.data?.id)
            val payment = requireNotNull(paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId))
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.PENDING) },
            )
        }

        @DisplayName("타임아웃 뒤 유효한 승인 콜백이 오면 결제가 성공 처리된다")
        @Test
        fun appliesApprovedCallback_afterTimeoutFallback() {
            // arrange
            pgClientSimulator.setScenario(PgClientSimulator.Scenario.TIMEOUT)
            val headers = createAuthHeaders()
            val product = createProduct()
            val request = createOrderRequest(product.id)
            val orderResponseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val orderResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                orderResponseType,
            )
            val orderId = requireNotNull(orderResponse.body?.data?.id)
            val callbackRequest = createApprovedCallbackRequest(orderId)

            // act
            val callbackResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val callbackResponse = testRestTemplate.exchange(
                CALLBACK_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(callbackRequest, jsonHeaders()),
                callbackResponseType,
            )

            // assert
            val payment = requireNotNull(paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId))
            assertAll(
                { assertThat(callbackResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.SUCCEEDED) },
            )
        }

        @DisplayName("만료 처리 후 승인 콜백이 와도 EXPIRED 상태는 유지된다")
        @Test
        fun ignoresApprovedCallback_afterExpiration() {
            // arrange
            pgClientSimulator.setScenario(PgClientSimulator.Scenario.TIMEOUT)
            val headers = createAuthHeaders()
            val product = createProduct()
            val request = createOrderRequest(product.id)
            val orderResponseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val orderResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                orderResponseType,
            )
            val orderId = requireNotNull(orderResponse.body?.data?.id)
            val payment = requireNotNull(paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId))
            setExpiresAt(payment, java.time.ZonedDateTime.now().minusMinutes(1))
            paymentJpaRepository.save(payment)
            pendingPaymentExpirationScheduler.expirePendingPayments()
            val callbackRequest = createApprovedCallbackRequest(orderId)

            // act
            val callbackResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val callbackResponse = testRestTemplate.exchange(
                CALLBACK_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(callbackRequest, jsonHeaders()),
                callbackResponseType,
            )

            // assert
            val expiredPayment = requireNotNull(paymentJpaRepository.findByOrderIdAndDeletedAtIsNull(orderId))
            assertAll(
                { assertThat(callbackResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(expiredPayment.status).isEqualTo(PaymentStatus.EXPIRED) },
            )
        }
    }

    private fun createAuthHeaders(): HttpHeaders {
        val member = memberJpaRepository.save(
            MemberModel(
                loginId = "payment-user",
                password = passwordEncoder.encode("Password1!"),
                name = "결제사용자",
                birthDate = java.time.LocalDate.of(1996, 3, 20),
                email = "payment-user@example.com",
            ),
        )
        val token = jwtTokenProvider.generateToken(member.id, member.loginId)
        return HttpHeaders().apply {
            set("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
        }
    }

    private fun createProduct(): ProductModel {
        val brand = brandJpaRepository.save(BrandModel(name = "payment-brand"))
        return productJpaRepository.save(
            ProductModel(
                name = "payment-product",
                price = 50_000L,
                brandId = brand.id,
                stockQuantity = 20,
            ),
        )
    }

    private fun createOrderRequest(productId: Long): OrderV1Dto.CreateRequest {
        return OrderV1Dto.CreateRequest(
            items = listOf(OrderV1Dto.OrderItemDto(productId = productId, quantity = 1)),
        )
    }

    private fun createApprovedCallbackRequest(orderId: Long): PaymentV1Dto.PgCallbackRequest {
        val unsignedRequest = PaymentV1Dto.PgCallbackRequest(
            orderId = orderId,
            transactionId = "pg-$orderId",
            status = com.loopers.application.payment.PaymentCallbackStatus.APPROVED,
            failureReason = null,
            signature = "",
        )
        return unsignedRequest.copy(signature = pgCallbackSignatureVerifier.sign(unsignedRequest.signaturePayload()))
    }

    private fun jsonHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
    }

    private fun setExpiresAt(payment: com.loopers.domain.payment.PaymentModel, expiresAt: java.time.ZonedDateTime) {
        val field = payment.javaClass.getDeclaredField("expiresAt")
        field.isAccessible = true
        field.set(payment, expiresAt)
    }
}
