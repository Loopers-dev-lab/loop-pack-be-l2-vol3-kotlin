package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.queue.OrderQueueService
import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
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
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val orderQueueService: OrderQueueService,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val stockReservationRepository: StockReservationRepository,
) {

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @BeforeEach
    fun setUpPaymentGateway() {
        whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
            PaymentGatewayResponse(transactionKey = "txn-test", status = "PENDING", reason = null),
        )
        whenever(paymentGateway.getTransactionsByOrderId(any(), any())).thenReturn(emptyList())
    }

    companion object {
        private const val QUEUE_ENTER_ENDPOINT = "/api/v1/queue/enter"
        private const val QUEUE_POSITION_ENDPOINT = "/api/v1/queue/position"
        private const val ORDER_ENDPOINT = "/api/v1/orders"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private val RESPONSE_TYPE = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun signUp(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = "홍길동",
            email = "test@example.com",
            birthday = LocalDate.of(1990, 1, 15),
        )
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            RESPONSE_TYPE,
        )
    }

    private fun authHeaders(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    private fun createProduct(): Product {
        val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productRepository.save(
            Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                likes = LikeCount.of(0),
                stockQuantity = StockQuantity.of(100),
                brandId = brand.id,
            ),
        )
        stockReservationRepository.setStock(product.id, 100)
        return product
    }

    @DisplayName("POST /api/v1/queue/enter")
    @Nested
    inner class EnterQueueApi {

        @Test
        @DisplayName("로그인한 사용자가 대기열에 진입하면, 200 OK와 순번을 반환한다.")
        fun returnsOk_withPosition() {
            // arrange
            signUp()

            // act
            val response = testRestTemplate.exchange(
                QUEUE_ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                {
                    val data = response.body?.data as Map<*, *>
                    assertThat(data["position"]).isEqualTo(0)
                },
            )
        }
    }

    @DisplayName("GET /api/v1/queue/position")
    @Nested
    inner class GetPositionApi {

        @Test
        @DisplayName("대기열에 진입한 유저가 순번을 조회하면, 200 OK와 순번 정보를 반환한다.")
        fun returnsOk_withPositionInfo() {
            // arrange
            signUp()
            testRestTemplate.exchange(
                QUEUE_ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )

            // act
            val response = testRestTemplate.exchange(
                QUEUE_POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                {
                    val data = response.body?.data as Map<*, *>
                    assertThat(data.keys).contains("position", "estimatedWaitSeconds", "pollingIntervalMs")
                },
            )
        }

        @Test
        @DisplayName("대기열에 진입하지 않은 유저가 순번을 조회하면, 404 NOT_FOUND를 반환한다.")
        fun returnsNotFound_whenNotInQueue() {
            // arrange
            signUp()

            // act
            val response = testRestTemplate.exchange(
                QUEUE_POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("전체 흐름 E2E")
    @Nested
    inner class FullFlowE2E {

        @Test
        @DisplayName("대기열 진입 → 순번 조회 → 스케줄러 토큰 발급 → 순번 조회(토큰) → 토큰으로 주문 성공")
        fun fullFlow_enterQueue_admitByScheduler_placeOrderWithToken() {
            // arrange
            signUp()
            val product = createProduct()

            // 1. 대기열 진입
            val enterResponse = testRestTemplate.exchange(
                QUEUE_ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )
            assertThat(enterResponse.statusCode).isEqualTo(HttpStatus.OK)

            // 2. 순번 조회 — 아직 토큰이 없으므로 position >= 0, token == null
            val positionBeforeAdmit = testRestTemplate.exchange(
                QUEUE_POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )
            assertThat(positionBeforeAdmit.statusCode).isEqualTo(HttpStatus.OK)
            val beforeData = positionBeforeAdmit.body?.data as Map<*, *>
            assertThat(beforeData["token"]).isNull()

            // 3. 스케줄러 역할 — admitUsers 호출로 토큰 발급
            orderQueueService.admitUsers(100)

            // 4. 순번 조회 — 토큰 발급 완료, position=0, token 존재
            val positionAfterAdmit = testRestTemplate.exchange(
                QUEUE_POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
            )
            assertThat(positionAfterAdmit.statusCode).isEqualTo(HttpStatus.OK)
            val afterData = positionAfterAdmit.body?.data as Map<*, *>
            assertAll(
                { assertThat(afterData["position"]).isEqualTo(0) },
                { assertThat(afterData["token"]).isNotNull() },
                { assertThat(afterData["pollingIntervalMs"]).isEqualTo(0) },
            )
            val token = afterData["token"] as String

            // 5. 토큰으로 주문 성공
            val orderHeaders = authHeaders().apply {
                set("Idempotency-Key", UUID.randomUUID().toString())
                set("X-Entry-Token", token)
            }
            val orderRequest = mapOf(
                "items" to listOf(mapOf("productId" to product.id, "quantity" to 1)),
                "cardType" to "SAMSUNG",
                "cardNo" to "1234-5678-9012-3456",
            )
            val orderResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(orderRequest, orderHeaders),
                RESPONSE_TYPE,
            )
            assertAll(
                { assertThat(orderResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(orderResponse.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }
    }
}
