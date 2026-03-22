package com.loopers.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.PgCallbackSignatureFilter
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.time.LocalDate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val objectMapper: ObjectMapper,
    @Value("\${pg.callback-secret-key}") private val callbackSecretKey: String,
) {

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    companion object {
        private const val PAYMENT_ENDPOINT = "/api/v1/payments"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private val PAYMENT_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        private val PAYMENT_LIST_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<List<Any>>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = name,
            email = email,
            birthday = birthday,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
    }

    private fun authHeaders(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(LOGIN_ID_HEADER, loginId)
            set(LOGIN_PW_HEADER, password)
        }
    }

    private data class PaymentRequest(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
    )

    private fun createOrderWithPendingPayment(
        transactionKey: String = "txn-key-12345",
    ): Payment {
        val order = orderRepository.save(Order(userId = 1L))
        val payment = Payment(
            userId = 1L,
            orderId = order.id.toString(),
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 50000L,
        )
        payment.markPending(transactionKey)
        return paymentRepository.save(payment)
    }

    private fun computeSignature(bodyBytes: ByteArray, timestamp: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(callbackSecretKey.toByteArray(), HMAC_ALGORITHM))
        val payload = "$timestamp.".toByteArray() + bodyBytes
        return mac.doFinal(payload).joinToString("") { "%02x".format(it) }
    }

    private fun signedCallbackHeaders(bodyBytes: ByteArray, timestamp: String = Instant.now().epochSecond.toString()): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(PgCallbackSignatureFilter.SIGNATURE_HEADER, computeSignature(bodyBytes, timestamp))
            set(PgCallbackSignatureFilter.TIMESTAMP_HEADER, timestamp)
        }
    }

    private fun sendCallback(body: Any): org.springframework.http.ResponseEntity<ApiResponse<Any>> {
        val bodyBytes = objectMapper.writeValueAsBytes(body)
        return testRestTemplate.exchange(
            "$PAYMENT_ENDPOINT/callback",
            HttpMethod.POST,
            HttpEntity(String(bodyBytes, Charsets.UTF_8), signedCallbackHeaders(bodyBytes)),
            PAYMENT_RESPONSE_TYPE,
        )
    }

    @DisplayName("POST /api/v1/payments")
    @Nested
    inner class RequestPayment {

        @DisplayName("PG 호출 성공 시, 결제가 PENDING 상태로 생성된다.")
        @Test
        fun returnsOkWithPendingStatus_whenPgCallSucceeds() {
            // arrange
            signUp()
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-key-123", status = "PENDING", reason = null),
            )
            val request = PaymentRequest(
                orderId = "ORDER-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // act
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
            val payments = paymentRepository.findByOrderId("ORDER-001")
            assertThat(payments.first().status).isEqualTo(PaymentStatus.PENDING)
        }

        @DisplayName("PG 호출 실패 + PG에도 결제가 없으면, FAILED 상태로 생성된다.")
        @Test
        fun returnsOkWithFailedStatus_whenPgIsUnavailableAndNoTransactionInPg() {
            // arrange
            signUp()
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)
            whenever(paymentGateway.getTransactionsByOrderId(any(), any()))
                .thenReturn(emptyList())
            val request = PaymentRequest(
                orderId = "ORDER-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // act
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
            val payments = paymentRepository.findByOrderId("ORDER-001")
            assertThat(payments.first().status).isEqualTo(PaymentStatus.FAILED)
        }

        @DisplayName("PG 호출 실패해도 PG에 결제가 존재하면, PENDING 상태로 생성된다.")
        @Test
        fun returnsOkWithPendingStatus_whenPgRequestFailedButTransactionExistsInPg() {
            // arrange
            signUp()
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)
            whenever(paymentGateway.getTransactionsByOrderId(any(), eq("ORDER-001")))
                .thenReturn(
                    listOf(
                        PaymentGatewayResponse(
                            transactionKey = "txn-key-recovered",
                            status = "PENDING",
                            reason = null,
                        ),
                    ),
                )
            val request = PaymentRequest(
                orderId = "ORDER-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // act
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
            val payments = paymentRepository.findByOrderId("ORDER-001")
            assertAll(
                { assertThat(payments.first().status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(payments.first().transactionKey).isEqualTo("txn-key-recovered") },
            )
        }

        @DisplayName("인증되지 않은 사용자가 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val request = PaymentRequest(
                orderId = "ORDER-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("금액이 0이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenAmountIsZero() {
            // arrange
            signUp()
            val request = PaymentRequest(
                orderId = "ORDER-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 0L,
            )

            // act
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("지원하지 않는 카드 유형이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCardTypeIsInvalid() {
            // arrange
            signUp()
            val request = PaymentRequest(
                orderId = "ORDER-001",
                cardType = "INVALID_CARD",
                cardNo = "1234-5678-9012-3456",
                amount = 50000L,
            )

            // act
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("POST /api/v1/payments/callback")
    @Nested
    inner class PaymentCallback {

        @DisplayName("SUCCESS 콜백이면, 결제는 SUCCESS, 주문은 CONFIRMED로 변경된다.")
        @Test
        fun updatesPaymentAndOrderStatus_whenSuccessCallback() {
            // arrange
            val payment = createOrderWithPendingPayment()

            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-12345"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-12345",
                    orderId = payment.orderId,
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to payment.orderId,
                "status" to "SUCCESS",
                "reason" to null,
            )

            // act
            val response = sendCallback(callbackRequest)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val savedPayment = paymentRepository.findByTransactionKey("txn-key-12345")
            val savedOrder = orderRepository.findById(payment.orderId.toLong())
            assertAll(
                { assertThat(savedPayment?.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(savedOrder?.status).isEqualTo(OrderStatus.CONFIRMED) },
            )
        }

        @DisplayName("FAILED 콜백이면, 결제는 FAILED, 주문은 CANCELLED로 변경된다.")
        @Test
        fun updatesPaymentAndOrderStatus_whenFailedCallback() {
            // arrange
            val payment = createOrderWithPendingPayment()

            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-12345"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-12345",
                    orderId = payment.orderId,
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )

            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to payment.orderId,
                "status" to "FAILED",
                "reason" to "한도 초과",
            )

            // act
            val response = sendCallback(callbackRequest)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val savedPayment = paymentRepository.findByTransactionKey("txn-key-12345")
            val savedOrder = orderRepository.findById(payment.orderId.toLong())
            assertAll(
                { assertThat(savedPayment?.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedPayment?.failReason).isEqualTo("한도 초과") },
                { assertThat(savedOrder?.status).isEqualTo(OrderStatus.CANCELLED) },
            )
        }

        @DisplayName("콜백 상태와 PG 실제 상태가 다르면, PG 실제 상태를 따른다.")
        @Test
        fun followsPgStatus_whenCallbackAndPgStatusDiffer() {
            // arrange
            createOrderWithPendingPayment()

            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-12345"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-12345",
                    orderId = "ORDER-001",
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )

            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to "ORDER-001",
                "status" to "SUCCESS",
                "reason" to null,
            )

            // act
            val response = sendCallback(callbackRequest)

            // assert — PG 실제 상태(FAILED)를 따름
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val saved = paymentRepository.findByTransactionKey("txn-key-12345")
            assertAll(
                { assertThat(saved?.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(saved?.failReason).isEqualTo("한도 초과") },
            )
        }

        @DisplayName("존재하지 않는 transactionKey이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenTransactionKeyNotExists() {
            // arrange
            val callbackRequest = mapOf(
                "transactionKey" to "non-existent-key",
                "orderId" to "ORDER-001",
                "status" to "SUCCESS",
                "reason" to null,
            )

            // act
            val response = sendCallback(callbackRequest)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("POST /api/v1/payments/callback - 서명 검증")
    @Nested
    inner class CallbackSignatureVerification {

        @DisplayName("서명 헤더가 없으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenSignatureHeaderMissing() {
            // arrange
            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to "ORDER-001",
                "status" to "SUCCESS",
                "reason" to null,
            )
            val bodyBytes = objectMapper.writeValueAsBytes(callbackRequest)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(PgCallbackSignatureFilter.TIMESTAMP_HEADER, Instant.now().epochSecond.toString())
            }

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(String(bodyBytes, Charsets.UTF_8), headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("타임스탬프 헤더가 없으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenTimestampHeaderMissing() {
            // arrange
            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to "ORDER-001",
                "status" to "SUCCESS",
                "reason" to null,
            )
            val bodyBytes = objectMapper.writeValueAsBytes(callbackRequest)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(PgCallbackSignatureFilter.SIGNATURE_HEADER, "fake-signature")
            }

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(String(bodyBytes, Charsets.UTF_8), headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("위조된 서명이면, 401 UNAUTHORIZED를 반환하고 결제 상태는 변경되지 않는다.")
        @Test
        fun returnsUnauthorized_whenSignatureIsForged() {
            // arrange
            val payment = createOrderWithPendingPayment()
            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to payment.orderId,
                "status" to "SUCCESS",
                "reason" to null,
            )
            val bodyBytes = objectMapper.writeValueAsBytes(callbackRequest)
            val timestamp = Instant.now().epochSecond.toString()
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(PgCallbackSignatureFilter.SIGNATURE_HEADER, "a".repeat(64))
                set(PgCallbackSignatureFilter.TIMESTAMP_HEADER, timestamp)
            }

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(String(bodyBytes, Charsets.UTF_8), headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
            val savedPayment = paymentRepository.findByTransactionKey("txn-key-12345")
            assertThat(savedPayment?.status).isEqualTo(PaymentStatus.PENDING)
            verify(paymentGateway, never()).getTransactionStatus(any(), any())
        }

        @DisplayName("만료된 타임스탬프이면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenTimestampExpired() {
            // arrange
            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to "ORDER-001",
                "status" to "SUCCESS",
                "reason" to null,
            )
            val bodyBytes = objectMapper.writeValueAsBytes(callbackRequest)
            val expiredTimestamp = (Instant.now().epochSecond - 600).toString()
            val headers = signedCallbackHeaders(bodyBytes, expiredTimestamp)

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(String(bodyBytes, Charsets.UTF_8), headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("정상 서명이면, 콜백이 처리되어 결제 상태가 변경된다.")
        @Test
        fun processesCallback_whenSignatureIsValid() {
            // arrange
            val payment = createOrderWithPendingPayment()

            whenever(paymentGateway.getTransactionStatus(any(), eq("txn-key-12345"))).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-12345",
                    orderId = payment.orderId,
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to payment.orderId,
                "status" to "SUCCESS",
                "reason" to null,
            )

            // act
            val response = sendCallback(callbackRequest)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val savedPayment = paymentRepository.findByTransactionKey("txn-key-12345")
            assertThat(savedPayment?.status).isEqualTo(PaymentStatus.SUCCESS)
        }
    }

    @DisplayName("GET /api/v1/payments/{orderId}/sync")
    @Nested
    inner class SyncPayment {

        @DisplayName("PG 조회 후 결제 상태가 업데이트된 결과를 반환한다.")
        @Test
        fun returnsUpdatedPayments_afterPgSync() {
            // arrange
            signUp()
            val payment = createOrderWithPendingPayment(transactionKey = "txn-key-123")
            val orderId = payment.orderId

            whenever(paymentGateway.getTransactionStatus(any(), any())).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = orderId,
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/$orderId/sync",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                PAYMENT_LIST_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
                { assertThat(response.body?.data).isNotEmpty() },
            )
            val saved = paymentRepository.findByTransactionKey("txn-key-123")
            assertThat(saved?.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("PG가 응답하지 못하면, 현재 상태를 그대로 반환한다.")
        @Test
        fun returnsCurrentStatus_whenPgIsUnavailable() {
            // arrange
            signUp()
            val payment = createOrderWithPendingPayment(transactionKey = "txn-key-123")
            val orderId = payment.orderId

            whenever(paymentGateway.getTransactionStatus(any(), any())).thenReturn(null)

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/$orderId/sync",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                PAYMENT_LIST_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
                { assertThat(response.body?.data).isNotEmpty() },
            )
            val saved = paymentRepository.findByTransactionKey("txn-key-123")
            assertThat(saved?.status).isEqualTo(PaymentStatus.PENDING)
        }

        @DisplayName("로그인하지 않으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/ORDER-001/sync",
                HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders()),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
