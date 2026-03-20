package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.payment.PaymentJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.payment.PaymentV1Dto
import com.loopers.utils.DatabaseCleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockkBean
    private lateinit var pgClient: PgClient

    private lateinit var userHeaders: HttpHeaders
    private lateinit var user: User
    private lateinit var order: Order

    companion object {
        private const val PASSWORD = "abcd1234"
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
        order = orderJpaRepository.save(
            Order(
                userId = user.id,
                items = listOf(OrderItem(productId = product.id, productName = "에어맥스", productPrice = 139000, quantity = 2)),
            ),
        )
        userHeaders = HttpHeaders()
        userHeaders.set("X-Loopers-LoginId", "testuser1")
        userHeaders.set("X-Loopers-LoginPw", PASSWORD)
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/payments")
    @Nested
    inner class RequestPayment {
        @DisplayName("유효한 결제 요청이 주어지면, 결제를 생성하고 201 응답을 반환한다.")
        @Test
        fun createsPayment_whenValidRequest() {
            // arrange
            every { pgClient.requestPayment(any<PgPaymentRequest>()) } returns PgPaymentResponse(
                transactionKey = "20250816:TR:abc123",
                status = "PENDING",
                reason = null,
            )
            val req = PaymentV1Dto.PaymentRequest(orderId = order.id, cardType = CardType.SAMSUNG, cardNo = "1234-5678-9012-3456")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/payments", HttpMethod.POST, HttpEntity(req, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.orderId).isEqualTo(order.id) },
                { assertThat(response.body?.data?.amount).isEqualTo(139000 * 2L) },
                { assertThat(response.body?.data?.status).isEqualTo("PENDING") },
                { assertThat(response.body?.data?.transactionKey).isEqualTo("20250816:TR:abc123") },
                { assertThat(response.body?.data?.cardNo).isEqualTo("xxxx-xxxx-xxxx-3456") },
            )
        }

        @DisplayName("이미 결제된 주문에 결제 요청을 하면, 409 CONFLICT 응답을 반환한다.")
        @Test
        fun returnsConflict_whenAlreadyPaid() {
            // arrange
            val payment = paymentJpaRepository.save(
                Payment(orderId = order.id, userId = user.id, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-3456", amount = 278000L),
            )
            payment.complete(PaymentStatus.SUCCESS, "정상 승인")
            paymentJpaRepository.save(payment)

            val req = PaymentV1Dto.PaymentRequest(orderId = order.id, cardType = CardType.KB, cardNo = "1234-5678-9012-7890")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/payments", HttpMethod.POST, HttpEntity(req, userHeaders), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @DisplayName("POST /api/v1/payments/callback")
    @Nested
    inner class HandleCallback {
        @DisplayName("SUCCESS 콜백이 수신되면, 결제 완료 및 주문 상태가 PAID로 변경된다.")
        @Test
        fun completesPaymentAndUpdatesOrder_whenSuccessCallback() {
            // arrange
            val payment = paymentJpaRepository.save(
                Payment(orderId = order.id, userId = user.id, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-3456", amount = 278000L),
            )
            payment.assignTransactionKey("20250816:TR:abc123")
            paymentJpaRepository.save(payment)

            val callbackReq = PaymentV1Dto.CallbackRequest(
                transactionKey = "20250816:TR:abc123",
                orderId = order.id.toString().padStart(6, '0'),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 278000L,
                status = "SUCCESS",
                reason = "정상 승인되었습니다.",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api/v1/payments/callback", HttpMethod.POST, HttpEntity(callbackReq), responseType)

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            val updatedOrder = orderJpaRepository.findById(order.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(updatedPayment.reason).isEqualTo("정상 승인되었습니다.") },
                { assertThat(updatedOrder.status.name).isEqualTo("PAID") },
            )
        }

        @DisplayName("FAILED 콜백이 수신되면, 결제 실패되고 주문 상태는 PENDING을 유지한다.")
        @Test
        fun failsPaymentAndKeepsOrderPending_whenFailedCallback() {
            // arrange
            val payment = paymentJpaRepository.save(
                Payment(orderId = order.id, userId = user.id, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-3456", amount = 278000L),
            )
            payment.assignTransactionKey("20250816:TR:def456")
            paymentJpaRepository.save(payment)

            val callbackReq = PaymentV1Dto.CallbackRequest(
                transactionKey = "20250816:TR:def456",
                orderId = order.id.toString().padStart(6, '0'),
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 278000L,
                status = "FAILED",
                reason = "한도초과입니다.",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api/v1/payments/callback", HttpMethod.POST, HttpEntity(callbackReq), responseType)

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            val updatedOrder = orderJpaRepository.findById(order.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(updatedPayment.reason).isEqualTo("한도초과입니다.") },
                { assertThat(updatedOrder.status.name).isEqualTo("PENDING") },
            )
        }
    }

    @DisplayName("GET /api/v1/payments/{paymentId}")
    @Nested
    inner class GetPayment {
        @DisplayName("존재하는 결제 ID를 주면, 결제 상세 정보를 반환한다.")
        @Test
        fun returnsPaymentInfo_whenPaymentExists() {
            // arrange
            val payment = paymentJpaRepository.save(
                Payment(orderId = order.id, userId = user.id, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-3456", amount = 278000L),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/payments/${payment.id}", HttpMethod.GET, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(payment.id) },
                { assertThat(response.body?.data?.orderId).isEqualTo(order.id) },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}/payments")
    @Nested
    inner class GetPaymentsByOrderId {
        @DisplayName("주문에 결제 내역이 있으면, 목록을 반환한다.")
        @Test
        fun returnsPaymentList_whenPaymentsExist() {
            // arrange
            paymentJpaRepository.save(
                Payment(orderId = order.id, userId = user.id, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-3456", amount = 278000L),
            )
            paymentJpaRepository.save(
                Payment(orderId = order.id, userId = user.id, cardType = CardType.KB, cardNo = "xxxx-xxxx-xxxx-7890", amount = 278000L),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<PaymentV1Dto.PaymentResponse>>>() {}
            val response = testRestTemplate.exchange("/api/v1/orders/${order.id}/payments", HttpMethod.GET, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }
    }
}
