package com.loopers.interfaces.api

import com.loopers.application.queue.QueueFacade
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.queue.OrderQueueService
import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueStreamE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val orderQueueService: OrderQueueService,
    private val queueFacade: QueueFacade,
) {

    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    companion object {
        private const val QUEUE_ENTER_ENDPOINT = "/api/v1/queue/enter"
        private const val QUEUE_STREAM_ENDPOINT = "/api/v1/queue/stream"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private val RESPONSE_TYPE = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun signUp(loginId: String, password: String) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = "테스트유저",
            email = "$loginId@test.com",
            birthday = LocalDate.of(1990, 1, 1),
        )
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            RESPONSE_TYPE,
        )
    }

    private fun authHeaders(loginId: String, password: String): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    private fun connectSse(
        loginId: String,
        password: String,
        receivedEvents: MutableList<String>,
        eventReceived: CountDownLatch,
        targetEvent: String,
    ): Thread {
        val thread = Thread {
            try {
                val url = URI("http://localhost:$port$QUEUE_STREAM_ENDPOINT").toURL()
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("X-Loopers-LoginId", loginId)
                conn.setRequestProperty("X-Loopers-LoginPw", password)
                conn.setRequestProperty("Accept", "text/event-stream")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { receivedEvents.add(it) }
                        if (line?.startsWith("event:$targetEvent") == true) {
                            eventReceived.countDown()
                        }
                    }
                }
            } catch (_: Exception) {
                // timeout 또는 연결 종료
            }
        }
        thread.isDaemon = true
        thread.start()
        return thread
    }

    @Nested
    @DisplayName("SSE 스트림 E2E")
    inner class SseStreamE2E {

        @Test
        @DisplayName("SSE 연결 후 스케줄러 admit → position 이벤트를 수신한다")
        fun receivesPositionEvent_afterAdmission() {
            // arrange - 2명 회원가입 + 대기열 진입
            signUp("user1", "Pass1234!@")
            signUp("user2", "Pass1234!@")

            testRestTemplate.exchange(
                QUEUE_ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders("user1", "Pass1234!@")),
                RESPONSE_TYPE,
            )
            testRestTemplate.exchange(
                QUEUE_ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders("user2", "Pass1234!@")),
                RESPONSE_TYPE,
            )

            // user2 SSE 연결
            val receivedEvents = CopyOnWriteArrayList<String>()
            val eventReceived = CountDownLatch(1)
            connectSse("user2", "Pass1234!@", receivedEvents, eventReceived, "position")

            Thread.sleep(1000)

            // act - user1만 입장 허용 → user2에게 position 이벤트 전송
            val admittedUserIds = orderQueueService.admitUsers(1)
            queueFacade.broadcastPositions(admittedUserIds)

            // assert
            val received = eventReceived.await(3, TimeUnit.SECONDS)
            assertThat(received)
                .describedAs("SSE position 이벤트를 수신해야 합니다. 수신된 이벤트: $receivedEvents")
                .isTrue()
        }

        @Test
        @DisplayName("토큰 발급 시 admitted 이벤트를 수신하고 SSE 연결이 종료된다")
        fun receivesAdmittedEvent_andConnectionCloses_whenTokenIssued() {
            // arrange
            signUp("user1", "Pass1234!@")

            testRestTemplate.exchange(
                QUEUE_ENTER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders("user1", "Pass1234!@")),
                RESPONSE_TYPE,
            )

            // user1 SSE 연결
            val receivedEvents = CopyOnWriteArrayList<String>()
            val admittedReceived = CountDownLatch(1)
            val sseThread = connectSse("user1", "Pass1234!@", receivedEvents, admittedReceived, "admitted")

            Thread.sleep(1000)

            // act - user1 입장 허용 → admitted 이벤트 전송 + SseEmitter 완료
            val admittedUserIds = orderQueueService.admitUsers(1)
            queueFacade.broadcastPositions(admittedUserIds)

            // assert - admitted 이벤트 수신
            val received = admittedReceived.await(3, TimeUnit.SECONDS)
            assertThat(received)
                .describedAs("SSE admitted 이벤트를 수신해야 합니다. 수신된 이벤트: $receivedEvents")
                .isTrue()

            // assert - SseEmitter 완료로 연결 종료 (스레드 종료 확인)
            sseThread.join(3000)
            assertThat(sseThread.isAlive).isFalse()
        }

        @Test
        @DisplayName("대기열 미진입 유저 SSE 연결 시 404 에러를 반환한다")
        fun returnsNotFound_whenUserNotInQueue() {
            // arrange - 회원가입만, 대기열 미진입
            signUp("user1", "Pass1234!@")

            // act - SSE 연결 시도
            val response = testRestTemplate.exchange(
                QUEUE_STREAM_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders("user1", "Pass1234!@")),
                RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
