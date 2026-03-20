package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PgPaymentClient
import com.loopers.infrastructure.pg.PgApiResponse
import com.loopers.infrastructure.pg.PgCallbackRequest
import com.loopers.infrastructure.pg.PgClient
import com.loopers.infrastructure.pg.PgPaymentResponse
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.order.OrderV1Dto
import com.loopers.interfaces.api.product.ProductAdminV1Dto
import com.loopers.interfaces.api.brand.BrandV1Dto
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @MockitoBean
    private lateinit var pgPaymentClient: PgPaymentClient

    @MockitoBean
    private lateinit var pgClient: PgClient

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
        private const val ADMIN_LDAP = "loopers.admin"
    }

    private var testOrderId: Long = 0

    @BeforeEach
    fun setUp() {
        createTestUser()
        val brandId = createTestBrand()!!
        val productId = createTestProduct(brandId)!!
        testOrderId = createTestOrder(productId)!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", TEST_LOGIN_ID)
            set("X-Loopers-LoginPw", TEST_PASSWORD)
            set("Content-Type", "application/json")
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
            set("Content-Type", "application/json")
        }
    }

    private fun createTestUser() {
        val request = UserV1Dto.SignUpRequest(
            loginId = TEST_LOGIN_ID,
            password = TEST_PASSWORD,
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "test@example.com",
        )
        testRestTemplate.exchange(
            "/api/v1/users",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun createTestBrand(): Long? {
        val request = BrandV1Dto.CreateRequest(name = "나이키", description = "스포츠 브랜드")
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestProduct(brandId: Long): Long? {
        val request = ProductAdminV1Dto.CreateRequest(
            brandId = brandId,
            name = "에어맥스 90",
            price = BigDecimal("129000"),
            stock = 100,
            description = "나이키 에어맥스 90",
            imageUrl = null,
        )
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestOrder(productId: Long): Long? {
        val request = OrderV1Dto.CreateRequest(
            items = listOf(OrderV1Dto.OrderItemRequest(productId = productId, quantity = 1)),
        )
        val response = testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
        )

        @Suppress("UNCHECKED_CAST")
        val data = response.body?.data as? Map<String, Any>
        return (data?.get("orderId") as? Number)?.toLong()
    }

    private fun pgSuccessResponse() = PgApiResponse(
        meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
        data = PgPaymentResponse(transactionKey = "20260319:TR:abc123", status = "PENDING", reason = null),
    )

    @DisplayName("POST /api/v1/payments")
    @Nested
    inner class RequestPayment {

        @DisplayName("정상적인 결제 요청이면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            whenever(pgPaymentClient.requestPayment(any(), any())).thenReturn(pgSuccessResponse())

            val request = PaymentV1Dto.CreateRequest(
                orderId = testOrderId,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get("status")).isEqualTo("REQUESTED") },
                { assertThat(response.body?.data?.get("transactionKey")).isEqualTo("20260319:TR:abc123") },
            )
        }

        @DisplayName("인증 헤더가 없으면, 에러를 반환한다.")
        @Test
        fun returnsError_whenNoAuthHeaders() {
            // arrange
            val request = PaymentV1Dto.CreateRequest(
                orderId = testOrderId,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(request, HttpHeaders().apply { set("Content-Type", "application/json") }),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert — 인증 헤더 누락 시 에러 응답
            assertThat(response.statusCode.isError).isTrue()
        }

        @DisplayName("존재하지 않는 주문이면, 404를 반환한다.")
        @Test
        fun returnsNotFound_whenOrderNotExists() {
            // arrange
            val request = PaymentV1Dto.CreateRequest(
                orderId = 999999999L,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("중복 결제 요청이면, 409를 반환한다.")
        @Test
        fun returnsConflict_whenDuplicatePayment() {
            // arrange
            whenever(pgPaymentClient.requestPayment(any(), any())).thenReturn(pgSuccessResponse())

            val request = PaymentV1Dto.CreateRequest(
                orderId = testOrderId,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )

            // 첫 번째 결제 요청
            testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act — 두 번째 결제 요청
            val response = testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @DisplayName("POST /api/v1/payments/callback")
    @Nested
    inner class HandleCallback {

        @DisplayName("SUCCESS 콜백이면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenSuccessCallback() {
            // arrange — 먼저 결제 요청
            whenever(pgPaymentClient.requestPayment(any(), any())).thenReturn(pgSuccessResponse())
            val paymentRequest = PaymentV1Dto.CreateRequest(
                orderId = testOrderId,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )
            testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(paymentRequest, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act — 콜백
            val callbackRequest = PgCallbackRequest(
                transactionKey = "20260319:TR:abc123",
                orderId = testOrderId.toString(),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
                amount = 129000,
                status = "SUCCESS",
                reason = "정상 승인되었습니다.",
            )
            val response = testRestTemplate.exchange(
                "/api/v1/payments/callback",
                HttpMethod.POST,
                HttpEntity(callbackRequest, HttpHeaders().apply { set("Content-Type", "application/json") }),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("존재하지 않는 transactionKey이면, 404를 반환한다.")
        @Test
        fun returnsNotFound_whenUnknownTransactionKey() {
            // arrange
            val callbackRequest = PgCallbackRequest(
                transactionKey = "unknown",
                orderId = "20260319000001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
                amount = 50000,
                status = "SUCCESS",
                reason = null,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/payments/callback",
                HttpMethod.POST,
                HttpEntity(callbackRequest, HttpHeaders().apply { set("Content-Type", "application/json") }),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/payments")
    @Nested
    inner class GetPaymentStatus {

        @DisplayName("결제가 존재하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenPaymentExists() {
            // arrange — 먼저 결제 요청
            whenever(pgPaymentClient.requestPayment(any(), any())).thenReturn(pgSuccessResponse())
            whenever(pgPaymentClient.getPaymentStatus(any(), any())).thenReturn(
                PgApiResponse(
                    meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
                    data = com.loopers.infrastructure.pg.PgTransactionDetailResponse(
                        transactionKey = "20260319:TR:abc123",
                        orderId = testOrderId.toString(),
                        cardType = "SAMSUNG",
                        cardNo = "1234-5678-9814-1451",
                        amount = 129000,
                        status = "PENDING",
                        reason = null,
                    ),
                ),
            )
            val paymentRequest = PaymentV1Dto.CreateRequest(
                orderId = testOrderId,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9814-1451",
            )
            testRestTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(paymentRequest, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/payments?orderId=$testOrderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get("orderId")).isNotNull },
            )
        }

        @DisplayName("결제 정보가 없으면, 404를 반환한다.")
        @Test
        fun returnsNotFound_whenNoPayment() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/payments?orderId=$testOrderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
