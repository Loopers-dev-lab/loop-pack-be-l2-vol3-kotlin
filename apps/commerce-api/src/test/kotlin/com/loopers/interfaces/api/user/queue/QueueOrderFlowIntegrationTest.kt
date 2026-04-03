package com.loopers.interfaces.api.user.queue

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.infrastructure.queue.QueueScheduler
import com.loopers.interfaces.api.ApiResponse
import com.loopers.config.redis.RedisConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@DisplayName("대기열 → 토큰 발급 → 주문 전체 흐름")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueOrderFlowIntegrationTest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val queueScheduler: QueueScheduler,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val QUEUE_ENTER = "/api/v1/queue/enter"
        private const val QUEUE_POSITION = "/api/v1/queue/position"
        private const val ORDER_ENDPOINT = "/api/v1/orders"
    }

    private var productId: Long = 0
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = LOGIN_ID,
            rawPassword = PASSWORD,
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
            passwordHasher = passwordHasher,
        )
        userId = userRepository.save(user).id!!

        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("나이키", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(8000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        val activeProduct = productRepository.save(saved.activate(), ADMIN)
        productId = activeProduct.id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(10)),
            ADMIN,
        )
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders =
        HttpHeaders().apply {
            set("X-Loopers-LoginId", LOGIN_ID)
            set("X-Loopers-LoginPw", PASSWORD)
        }

    @Nested
    @DisplayName("대기열 → 토큰 발급 → 주문 전체 흐름")
    inner class FullFlow {
        @Test
        @DisplayName("대기열 진입 → 스케줄러 실행 → 토큰으로 주문 → 동일 토큰 재사용 거부")
        fun fullFlow_enterAndOrder() {
            // 1. 대기열 진입
            val enterResponse = testRestTemplate.exchange(
                QUEUE_ENTER,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Enter>>() {},
            )
            assertAll(
                { assertThat(enterResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(enterResponse.body?.data?.status).isEqualTo("WAITING") },
                { assertThat(enterResponse.body?.data?.position).isEqualTo(0) },
            )

            // 2. 순번 조회 (WAITING)
            val positionResponse = testRestTemplate.exchange(
                QUEUE_POSITION,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Position>>() {},
            )
            assertThat(positionResponse.body?.data?.status).isEqualTo("WAITING")

            // 3. 스케줄러 직접 호출 → 토큰 발급
            queueScheduler.processQueue()

            // 4. 순번 조회 (READY) — token은 position 응답에 포함되지 않음
            val readyResponse = testRestTemplate.exchange(
                QUEUE_POSITION,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Position>>() {},
            )
            assertAll(
                { assertThat(readyResponse.body?.data?.status).isEqualTo("READY") },
                { assertThat(readyResponse.body?.data?.tokenExpiresInSeconds).isNotNull() },
            )

            // 4-1. POST /enter 재호출로 토큰 획득 (인증 필수 API)
            val enterReadyResponse = testRestTemplate.exchange(
                QUEUE_ENTER,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Enter>>() {},
            )
            assertAll(
                { assertThat(enterReadyResponse.body?.data?.status).isEqualTo("READY") },
                { assertThat(enterReadyResponse.body?.data?.token).isNotNull() },
            )
            val token = enterReadyResponse.body!!.data!!.token!!

            // 5. 토큰으로 주문
            val orderHeaders = authHeaders().apply {
                set("X-Entry-Token", token)
                set("X-Idempotency-Key", UUID.randomUUID().toString())
                contentType = MediaType.APPLICATION_JSON
            }
            val orderBody = """{"items":[{"productId":$productId,"quantity":1}]}"""
            val orderResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(orderBody, orderHeaders),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )
            assertThat(orderResponse.statusCode).isEqualTo(HttpStatus.CREATED)

            // 6. 동일 토큰 재사용 → 거부
            val replayHeaders = authHeaders().apply {
                set("X-Entry-Token", token)
                set("X-Idempotency-Key", UUID.randomUUID().toString())
                contentType = MediaType.APPLICATION_JSON
            }
            val replayResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(orderBody, replayHeaders),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )
            assertThat(replayResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @Nested
    @DisplayName("동시 진입 시 순서가 보장된다")
    inner class ConcurrentEnter {
        @Test
        @DisplayName("20명 동시 대기열 진입 → 각 position 유일")
        fun concurrentEnter_orderPreserved() {
            val users = (2..21).map { i ->
                val user = User.register(
                    loginId = "testuser$i",
                    rawPassword = PASSWORD,
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "user$i@example.com",
                    passwordHasher = passwordHasher,
                )
                userRepository.save(user)
            }

            val positions = java.util.concurrent.ConcurrentHashMap<Long, Long>()
            val latch = CountDownLatch(users.size)
            val executor = Executors.newFixedThreadPool(10)

            for (user in users) {
                executor.submit {
                    try {
                        val headers = HttpHeaders().apply {
                            set("X-Loopers-LoginId", user.loginId.value)
                            set("X-Loopers-LoginPw", PASSWORD)
                        }
                        val response = testRestTemplate.exchange(
                            QUEUE_ENTER,
                            HttpMethod.POST,
                            HttpEntity<Void>(headers),
                            object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Enter>>() {},
                        )
                        val position = response.body?.data?.position
                        if (position != null) {
                            positions[user.id!!] = position
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            assertAll(
                { assertThat(positions).hasSize(users.size) },
                { assertThat(positions.values.toSet()).hasSize(users.size) },
                { assertThat(positions.values.min()).isEqualTo(0) },
                { assertThat(positions.values.max()).isEqualTo(users.size.toLong() - 1) },
            )
        }
    }

    @Nested
    @DisplayName("토큰 없이 주문하면 거부된다")
    inner class NoToken {
        @Test
        @DisplayName("X-Entry-Token 없이 POST /api/v1/orders → 400")
        fun noToken_orderRejected() {
            val headers = authHeaders().apply {
                set("X-Idempotency-Key", UUID.randomUUID().toString())
                contentType = MediaType.APPLICATION_JSON
            }
            val body = """{"items":[{"productId":$productId,"quantity":1}]}"""

            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(body, headers),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo("ENTRY_TOKEN_REQUIRED") },
            )
        }
    }

    @Nested
    @DisplayName("만료된 토큰으로 주문하면 거부된다")
    inner class TokenExpired {
        @Test
        @DisplayName("토큰 발급 후 Redis에서 삭제(만료 시뮬레이션) → POST /api/v1/orders → 실패")
        fun tokenExpired_orderRejected() {
            // 1. 대기열 진입
            val enterResponse = testRestTemplate.exchange(
                QUEUE_ENTER,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Enter>>() {},
            )
            assertThat(enterResponse.body?.data?.status).isEqualTo("WAITING")

            // 2. 스케줄러 실행 → 토큰 발급
            queueScheduler.processQueue()

            // 3. POST /enter 재호출로 토큰 획득
            val enterReadyResponse = testRestTemplate.exchange(
                QUEUE_ENTER,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserQueueV1Response.Enter>>() {},
            )
            val token = enterReadyResponse.body!!.data!!.token!!

            // 4. Redis에서 토큰 직접 삭제 (만료 시뮬레이션)
            redisTemplate.delete("queue:entry-token:$userId")

            // 5. 만료된 토큰으로 주문 시도
            val orderHeaders = authHeaders().apply {
                set("X-Entry-Token", token)
                set("X-Idempotency-Key", UUID.randomUUID().toString())
                contentType = MediaType.APPLICATION_JSON
            }
            val orderBody = """{"items":[{"productId":$productId,"quantity":1}]}"""
            val orderResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(orderBody, orderHeaders),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // 6. 만료된 토큰 → validate 실패 → ENTRY_TOKEN_INVALID (403)
            assertAll(
                { assertThat(orderResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN) },
                { assertThat(orderResponse.body?.meta?.errorCode).isEqualTo("ENTRY_TOKEN_INVALID") },
            )
        }
    }
}
