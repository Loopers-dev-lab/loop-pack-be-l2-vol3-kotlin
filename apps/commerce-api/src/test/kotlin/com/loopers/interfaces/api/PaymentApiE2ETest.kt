package com.loopers.interfaces.api

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus

import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val paymentRepository: PaymentRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    companion object {
        private const val PAYMENT_ENDPOINT = "/api/v1/payments"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
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

    private fun createPendingPayment(
        orderId: String = "ORDER-001",
        transactionKey: String = "txn-key-12345",
    ): Payment {
        val payment = Payment(
            userId = 1L,
            orderId = orderId,
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 50000L,
        )
        payment.markPending(transactionKey)
        return paymentRepository.save(payment)
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

        @DisplayName("PG가 응답하지 못하면, 결제가 REQUESTED 상태로 생성된다 (Fallback).")
        @Test
        fun returnsOkWithRequestedStatus_whenPgIsUnavailable() {
            // arrange
            signUp()
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(null)
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
            assertThat(payments.first().status).isEqualTo(PaymentStatus.REQUESTED)
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

        @DisplayName("SUCCESS 콜백이면, 결제 상태가 SUCCESS로 변경된다.")
        @Test
        fun updatesStatusToSuccess_whenSuccessCallback() {
            // arrange
            createPendingPayment()

            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to "ORDER-001",
                "status" to "SUCCESS",
                "reason" to null,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(callbackRequest, headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val saved = paymentRepository.findByTransactionKey("txn-key-12345")
            assertThat(saved?.status).isEqualTo(PaymentStatus.SUCCESS)
        }

        @DisplayName("FAILED 콜백이면, 결제 상태가 FAILED로 변경되고 사유가 저장된다.")
        @Test
        fun updatesStatusToFailed_whenFailedCallback() {
            // arrange
            createPendingPayment()

            val callbackRequest = mapOf(
                "transactionKey" to "txn-key-12345",
                "orderId" to "ORDER-001",
                "status" to "FAILED",
                "reason" to "한도 초과",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(callbackRequest, headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
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
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(callbackRequest, headers),
                PAYMENT_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
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
            createPendingPayment(transactionKey = "txn-key-123")

            whenever(paymentGateway.getTransactionStatus(any(), any())).thenReturn(
                PaymentGatewayTransactionDetail(
                    transactionKey = "txn-key-123",
                    orderId = "ORDER-001",
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/ORDER-001/sync",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                PAYMENT_LIST_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
            )
        }

        @DisplayName("PG가 응답하지 못하면, 현재 상태를 그대로 반환한다.")
        @Test
        fun returnsCurrentStatus_whenPgIsUnavailable() {
            // arrange
            signUp()
            createPendingPayment(transactionKey = "txn-key-123")

            whenever(paymentGateway.getTransactionStatus(any(), any())).thenReturn(null)

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/ORDER-001/sync",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                PAYMENT_LIST_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
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
