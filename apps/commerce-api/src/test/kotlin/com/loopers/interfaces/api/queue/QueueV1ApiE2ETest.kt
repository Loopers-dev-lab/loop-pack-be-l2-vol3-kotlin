package com.loopers.interfaces.api.queue

import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.scheduler.QueueScheduler
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import com.loopers.interfaces.api.ApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
class QueueV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val queueRepository: QueueRepository,
) {
    @MockBean
    private lateinit var queueScheduler: QueueScheduler

    companion object {
        private const val BASE_URL = "/api/v1/queues"
        private const val QUEUE_NAME = "test-queue"
        private const val DEFAULT_PASSWORD = "test"
    }

    @BeforeEach
    fun setUp() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createUserAndGetHeaders(loginId: String = "admin"): HttpHeaders {
        // 먼저 기존 사용자 확인
        val loginIdVo = LoginId.of(loginId)
        val existingUser = userJpaRepository.findByLoginId(loginIdVo)

        // 없을 때만 생성
        if (existingUser == null) {
            try {
                val user = User.create(
                    loginId = loginIdVo,
                    password = Password.ofEncrypted(passwordEncoder.encode(DEFAULT_PASSWORD)),
                    name = Name.of("테스트사용자"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("$loginId@test.com"),
                )
                userJpaRepository.save(user)
            } catch (e: Exception) {
                // 중복 키 예외는 무시 (다른 스레드가 생성한 경우)
                if (e !is org.springframework.dao.DataIntegrityViolationException) {
                    throw e
                }
            }
        }

        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", DEFAULT_PASSWORD)
        }
    }

    @DisplayName("POST /api/v1/queues/{queueName}/enter")
    @Nested
    inner class EnterTest {

        @Test
        fun `정상 진입시 201 응답과 순번을 반환한다`() {
            // arrange
            val url = "$BASE_URL/$QUEUE_NAME/enter"
            val headers = createUserAndGetHeaders()
            val requestEntity = HttpEntity<Any>(Unit, headers)
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}

            // act
            val response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body?.data).isNotNull()
            assertThat(response.body?.data?.queueName).isEqualTo(QUEUE_NAME)
            assertThat(response.body?.data?.position).isEqualTo(1L)
            assertThat(response.body?.data?.estimatedWaitSeconds).isEqualTo(0L)
        }

        @Test
        fun `중복 진입시 기존 항목을 제거하고 맨 뒤로 이동한다`() {
            // arrange
            val url = "$BASE_URL/$QUEUE_NAME/enter"
            val headers = createUserAndGetHeaders()
            val requestEntity = HttpEntity<Any>(Unit, headers)
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}

            // 첫 번째 진입 (position: 1)
            val firstResponse = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType)
            val firstPosition = firstResponse.body?.data?.position ?: 0L
            assertThat(firstPosition).isEqualTo(1L)

            // 다른 사용자 3명 추가 진입
            repeat(3) { i ->
                val otherHeaders = createUserAndGetHeaders(loginId = "user${i + 1}")
                val otherRequest = HttpEntity<Any>(Unit, otherHeaders)
                testRestTemplate.exchange(url, HttpMethod.POST, otherRequest, responseType)
            }

            // act: 두 번째 진입 시도 (기존 항목 제거 후 맨 뒤로)
            val secondResponse = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType)

            // assert: 201 성공, 그리고 맨 뒤 (4번째)
            assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(secondResponse.body?.data?.position).isEqualTo(4L)
        }

        @Test
        fun `인증 헤더 없이 요청시 401 응답을 반환한다`() {
            // arrange
            val url = "$BASE_URL/$QUEUE_NAME/enter"
            val requestEntity = HttpEntity<Any>(Unit)
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}

            // act
            val response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `여러 사용자가 순서대로 진입할 수 있다`() {
            // arrange
            val url = "$BASE_URL/$QUEUE_NAME/enter"
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}

            // act & assert
            for (i in 1..5) {
                val headers = createUserAndGetHeaders(loginId = "user$i")
                val requestEntity = HttpEntity<Any>(Unit, headers)
                val response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType)

                assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
                assertThat(response.body?.data?.position).isEqualTo(i.toLong())
            }
        }
    }

    @DisplayName("GET /api/v1/queues/{queueName}/position")
    @Nested
    inner class GetPositionTest {

        @Test
        fun `대기열에 있는 사용자의 순번을 반환한다`() {
            // arrange: 먼저 진입
            val enterUrl = "$BASE_URL/$QUEUE_NAME/enter"
            val enterHeaders = createUserAndGetHeaders()
            val enterRequestEntity = HttpEntity<Any>(Unit, enterHeaders)
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            testRestTemplate.exchange(enterUrl, HttpMethod.POST, enterRequestEntity, responseType)

            // act
            val positionUrl = "$BASE_URL/$QUEUE_NAME/position"
            val positionHeaders = createUserAndGetHeaders()
            val positionRequestEntity = HttpEntity<Any>(Unit, positionHeaders)
            val positionResponseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}

            val response = testRestTemplate.exchange(positionUrl, HttpMethod.GET, positionRequestEntity, positionResponseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.queueName).isEqualTo(QUEUE_NAME)
            assertThat(response.body?.data?.position).isEqualTo(1L)
        }

        @Test
        fun `대기열에 없는 사용자가 조회시 404 응답을 반환한다`() {
            // arrange
            val positionUrl = "$BASE_URL/$QUEUE_NAME/position"
            val positionHeaders = createUserAndGetHeaders()
            val positionRequestEntity = HttpEntity<Any>(Unit, positionHeaders)
            val positionResponseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}

            // act
            val response = testRestTemplate.exchange(positionUrl, HttpMethod.GET, positionRequestEntity, positionResponseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `인증 헤더 없이 요청시 401 응답을 반환한다`() {
            // arrange
            val positionUrl = "$BASE_URL/$QUEUE_NAME/position"
            val positionRequestEntity = HttpEntity<Any>(Unit)
            val positionResponseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}

            // act
            val response = testRestTemplate.exchange(positionUrl, HttpMethod.GET, positionRequestEntity, positionResponseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `토큰이 발급된 사용자가 순번 조회시 position 0과 token이 포함된 응답을 반환한다`() {
            // arrange: 대기열 진입
            val headers = createUserAndGetHeaders()
            val enterUrl = "$BASE_URL/$QUEUE_NAME/enter"
            testRestTemplate.exchange(enterUrl, HttpMethod.POST, HttpEntity<Any>(Unit, headers), object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {})

            // 대기열에서 제거하고 토큰 발급 (스케줄러 역할)
            val user = userJpaRepository.findByLoginId(LoginId.of("admin"))!!
            queueRepository.popMin(QUEUE_NAME, 1)
            val expectedToken = "test-entry-token"
            queueRepository.issueToken(QUEUE_NAME, user.id, expectedToken, 600L)

            // act
            val positionUrl = "$BASE_URL/$QUEUE_NAME/position"
            val positionResponseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {}
            val response = testRestTemplate.exchange(positionUrl, HttpMethod.GET, HttpEntity<Any>(Unit, headers), positionResponseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.position).isEqualTo(0L)
            assertThat(response.body?.data?.token).isEqualTo(expectedToken)
        }
    }

    @DisplayName("GET /api/v1/queues/{queueName}/status")
    @Nested
    inner class GetStatusTest {

        @Test
        fun `대기열 상태를 반환한다`() {
            // arrange: 5명 진입
            val enterUrl = "$BASE_URL/$QUEUE_NAME/enter"
            val responseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            repeat(5) { i ->
                val headers = createUserAndGetHeaders(loginId = "user$i")
                val requestEntity = HttpEntity<Any>(Unit, headers)
                testRestTemplate.exchange(enterUrl, HttpMethod.POST, requestEntity, responseType)
            }

            // act
            val statusUrl = "$BASE_URL/$QUEUE_NAME/status"
            val statusResponseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.StatusResponse>>() {}

            val response = testRestTemplate.exchange(statusUrl, HttpMethod.GET, HttpEntity<Any>(Unit), statusResponseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.queueName).isEqualTo(QUEUE_NAME)
            assertThat(response.body?.data?.totalWaiting).isEqualTo(5L)
            assertThat(response.body?.data?.throughputPerSecond).isEqualTo(175L)
        }

        @Test
        fun `인증 없이 상태 조회가 가능하다`() {
            // arrange
            val statusUrl = "$BASE_URL/$QUEUE_NAME/status"
            val statusResponseType = object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.StatusResponse>>() {}

            // act
            val response = testRestTemplate.exchange(statusUrl, HttpMethod.GET, HttpEntity<Any>(Unit), statusResponseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}
