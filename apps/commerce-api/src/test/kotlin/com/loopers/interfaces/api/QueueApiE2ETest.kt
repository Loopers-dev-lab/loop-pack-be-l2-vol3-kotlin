package com.loopers.interfaces.api

import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
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
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    companion object {
        private const val QUEUE_ENTER_ENDPOINT = "/api/v1/queue/enter"
        private const val QUEUE_POSITION_ENDPOINT = "/api/v1/queue/position"
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
}
