package com.loopers.interfaces.api

import com.loopers.domain.queue.QueueStatus
import com.loopers.domain.queue.QueueTokenRepository
import com.loopers.domain.user.User
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.queue.QueueV1Dto
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["queue.enabled=true"])
class QueueV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val queueTokenRepository: QueueTokenRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    private lateinit var user: User

    companion object {
        private const val PASSWORD = "abcd1234"
        private const val ENTER_ENDPOINT = "/api/v1/queue/enter"
        private const val POSITION_ENDPOINT = "/api/v1/queue/position"
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(
            User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"),
        )
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", user.loginId)
            set("X-Loopers-LoginPw", PASSWORD)
        }
    }

    @DisplayName("POST /api/v1/queue/enter")
    @Nested
    inner class Enter {
        @DisplayName("대기열에 진입하면, 순번과 전체 대기 인원을 반환한다.")
        @Test
        fun returnsPositionAndTotalWaiting() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Any>(null, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.position).isEqualTo(1L) },
                { assertThat(response.body?.data?.totalWaiting).isEqualTo(1L) },
            )
        }

        @DisplayName("이미 대기열에 있는 사용자가 재진입하면, 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflict_whenAlreadyEntered() {
            // arrange
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            testRestTemplate.exchange(ENTER_ENDPOINT, HttpMethod.POST, HttpEntity<Any>(null, authHeaders()), responseType)

            // act
            val response = testRestTemplate.exchange(
                ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Any>(null, authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("인증 실패 시 404를 반환한다.")
        @Test
        fun returnsNotFound_whenAuthFailed() {
            // arrange
            val invalidHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", "invalid")
                set("X-Loopers-LoginPw", "invalid")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Any>(null, invalidHeaders),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/queue/position")
    @Nested
    inner class Position {
        @DisplayName("대기열에 있는 사용자는 WAITING 상태와 순번을 반환한다.")
        @Test
        fun returnsWaitingStatus() {
            // arrange
            val enterType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            testRestTemplate.exchange(ENTER_ENDPOINT, HttpMethod.POST, HttpEntity<Any>(null, authHeaders()), enterType)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}
            val response = testRestTemplate.exchange(
                POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(null, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.status).isEqualTo(QueueStatus.WAITING) },
                { assertThat(response.body?.data?.position).isEqualTo(1L) },
                { assertThat(response.body?.data?.totalWaiting).isEqualTo(1L) },
                { assertThat(response.body?.data?.estimatedWaitSeconds).isNotNull() },
            )
        }

        @DisplayName("토큰이 발급된 사용자는 TOKEN_ISSUED 상태를 반환한다.")
        @Test
        fun returnsTokenIssuedStatus() {
            // arrange
            queueTokenRepository.issueToken(user.id, 300)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}
            val response = testRestTemplate.exchange(
                POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(null, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.status).isEqualTo(QueueStatus.TOKEN_ISSUED) },
                { assertThat(response.body?.data?.position).isEqualTo(0L) },
                { assertThat(response.body?.data?.estimatedWaitSeconds).isEqualTo(0L) },
                { assertThat(response.body?.data?.token).isNotBlank() },
            )
        }

        @DisplayName("대기열에 없는 사용자는 NOT_IN_QUEUE 상태를 반환한다.")
        @Test
        fun returnsNotInQueueStatus() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}
            val response = testRestTemplate.exchange(
                POSITION_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(null, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.status).isEqualTo(QueueStatus.NOT_IN_QUEUE) },
                { assertThat(response.body?.data?.position).isEqualTo(0L) },
            )
        }
    }
}
