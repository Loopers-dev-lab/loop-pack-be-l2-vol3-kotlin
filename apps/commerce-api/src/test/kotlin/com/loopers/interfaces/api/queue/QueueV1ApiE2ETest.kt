package com.loopers.interfaces.api.queue

import com.loopers.infrastructure.queue.WaitingQueueRedisRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.order.OrderV1Dto
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.utils.DatabaseCleanUp
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
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    @Qualifier("redisTemplateMaster") private val masterRedisTemplate: RedisTemplate<String, String>,
) {

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
    }

    private var testUserId: Long = 0

    @BeforeEach
    fun setUp() {
        flushQueueRedis()
        createTestUser()
        testUserId = userJpaRepository.findByLoginId(TEST_LOGIN_ID)!!.id
    }

    @AfterEach
    fun tearDown() {
        flushQueueRedis()
        databaseCleanUp.truncateAllTables()
    }

    private fun flushQueueRedis() {
        val keys = masterRedisTemplate.keys("queue:*")
        if (keys.isNotEmpty()) masterRedisTemplate.delete(keys)
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", TEST_LOGIN_ID)
            set("X-Loopers-LoginPw", TEST_PASSWORD)
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

    @DisplayName("POST /api/v1/queue/enter")
    @Nested
    inner class EnterQueue {

        @BeforeEach
        fun cleanQueue() {
            flushQueueRedis()
        }

        @DisplayName("인증된 유저가 대기열에 진입하면, 200 OK와 순번을 반환한다.")
        @Test
        fun returnsOkWithPosition_whenEnterQueue() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get("position")).isNotNull() },
                { assertThat(response.body?.data?.get("totalWaiting")).isNotNull() },
            )
        }

        @DisplayName("인증 헤더가 없으면, 에러를 반환한다.")
        @Test
        fun returnsError_whenNoAuthHeaders() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(HttpHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isNotEqualTo(HttpStatus.OK)
        }

        @DisplayName("같은 유저가 다시 진입하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenEnterAgain() {
            // arrange
            val firstResponse = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // act
            val secondResponse = testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert — 두 번 진입해도 200 OK이고 순번이 악화되지 않음
            val firstPosition = (firstResponse.body?.data?.get("position") as Number).toLong()
            val secondPosition = (secondResponse.body?.data?.get("position") as Number).toLong()
            assertAll(
                { assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(secondPosition).isLessThanOrEqualTo(firstPosition) },
            )
        }
    }

    @DisplayName("GET /api/v1/queue/position")
    @Nested
    inner class GetPosition {

        @BeforeEach
        fun cleanQueue() {
            flushQueueRedis()
        }

        @DisplayName("대기열에 있는 유저가 순번을 조회하면, 200 OK와 순번을 반환한다.")
        @Test
        fun returnsOkWithPosition_whenInQueue() {
            // arrange
            testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get("position")).isNotNull() },
                { assertThat(response.body?.data?.get("estimatedWaitSeconds")).isNotNull() },
            )
        }

        @DisplayName("토큰이 발급된 유저가 조회하면, position=0과 토큰을 반환한다.")
        @Test
        fun returnsTokenWithZeroPosition_whenTokenIssued() {
            // arrange — 대기열 진입 후 Redis에서 직접 토큰 발급
            testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            masterRedisTemplate.opsForValue().set(
                "${WaitingQueueRedisRepository.TOKEN_KEY_PREFIX}$testUserId",
                "test-token",
                WaitingQueueRedisRepository.TOKEN_TTL,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat((response.body?.data?.get("position") as Number).toLong()).isEqualTo(0L) },
                { assertThat(response.body?.data?.get("token")).isNotNull() },
            )
        }
    }

    @DisplayName("토큰 없이 주문 시")
    @Nested
    inner class OrderWithoutToken {

        @DisplayName("토큰 없이 주문하면, 403 FORBIDDEN을 반환한다.")
        @Test
        fun returnsForbidden_whenNoToken() {
            // act
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = 1L, quantity = 1)),
            )
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }
}
