package com.loopers.infrastructure.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.pg.PgCommunicationLog
import com.loopers.domain.pg.PgCommunicationLogRepository
import feign.Client
import feign.Request
import feign.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import java.net.SocketTimeoutException

class PgLoggingClientTest {

    private val delegate: Client = mock()
    private val logRepository: PgCommunicationLogRepository = mock()
    private val transactionManager: PlatformTransactionManager = mock()
    private val objectMapper = ObjectMapper()

    private lateinit var loggingClient: PgLoggingClient

    @BeforeEach
    fun setUp() {
        whenever(transactionManager.getTransaction(any())).thenReturn(SimpleTransactionStatus())
        whenever(logRepository.save(any())).thenAnswer { it.arguments[0] }

        loggingClient = PgLoggingClient(
            delegate = delegate,
            pgCommunicationLogRepository = logRepository,
            transactionManager = transactionManager,
            objectMapper = objectMapper,
        )
    }

    private fun createRequest(
        url: String = "http://localhost:8082/api/v1/payments",
        body: String? = """{"orderId":"20260318000001","cardType":"SAMSUNG","cardNo":"1234-5678-9814-1451","amount":50000}""",
    ): Request {
        return Request.create(
            Request.HttpMethod.POST,
            url,
            mapOf("Content-Type" to listOf("application/json")),
            body?.toByteArray(Charsets.UTF_8),
            Charsets.UTF_8,
            null,
        )
    }

    private fun createResponse(status: Int = 200, body: String? = null): Response {
        return Response.builder()
            .status(status)
            .reason("OK")
            .request(createRequest())
            .headers(emptyMap())
            .body(body?.toByteArray(Charsets.UTF_8) ?: ByteArray(0))
            .build()
    }

    @DisplayName("PG 호출 성공 시,")
    @Nested
    inner class Success {

        @DisplayName("요청/응답 정보가 로그에 저장된다.")
        @Test
        fun savesLog_whenCallSucceeds() {
            // arrange
            val request = createRequest()
            val responseBody =
                """{"meta":{"result":"SUCCESS"},"data":{"transactionKey":"20260318:TR:abc123","status":"PENDING"}}"""
            val response = createResponse(200, responseBody)
            whenever(delegate.execute(any(), any())).thenReturn(response)

            // act
            loggingClient.execute(request, Request.Options())

            // assert
            val captor = argumentCaptor<PgCommunicationLog>()
            verify(logRepository).save(captor.capture())
            val log = captor.firstValue

            assertThat(log.method).isEqualTo("POST")
            assertThat(log.success).isTrue()
            assertThat(log.httpStatus).isEqualTo(200)
            assertThat(log.orderId).isEqualTo("20260318000001")
            assertThat(log.transactionKey).isEqualTo("20260318:TR:abc123")
            assertThat(log.requestBody).contains("20260318000001")
            assertThat(log.responseBody).contains("transactionKey")
            assertThat(log.elapsed).isGreaterThanOrEqualTo(0)
        }
    }

    @DisplayName("PG 호출 실패 시,")
    @Nested
    inner class Failure {

        @DisplayName("예외 정보가 로그에 저장되고, 예외가 그대로 throw 된다.")
        @Test
        fun savesLogAndRethrows_whenCallFails() {
            // arrange
            val request = createRequest()
            whenever(delegate.execute(any(), any())).thenThrow(SocketTimeoutException("Read timed out"))

            // act & assert
            assertThrows<SocketTimeoutException> {
                loggingClient.execute(request, Request.Options())
            }

            val captor = argumentCaptor<PgCommunicationLog>()
            verify(logRepository).save(captor.capture())
            val log = captor.firstValue

            assertThat(log.success).isFalse()
            assertThat(log.httpStatus).isNull()
            assertThat(log.errorMessage).isEqualTo("Read timed out")
            assertThat(log.orderId).isEqualTo("20260318000001")
        }
    }

    @DisplayName("로그 저장 실패 시,")
    @Nested
    inner class LogSaveFailure {

        @DisplayName("원본 응답이 정상적으로 반환된다.")
        @Test
        fun returnsResponse_whenLogSaveFails() {
            // arrange
            val request = createRequest()
            val response = createResponse(200, """{"meta":{"result":"SUCCESS"},"data":null}""")
            whenever(delegate.execute(any(), any())).thenReturn(response)
            whenever(logRepository.save(any())).thenThrow(RuntimeException("DB 연결 실패"))

            // act
            val result = loggingClient.execute(request, Request.Options())

            // assert
            assertThat(result.status()).isEqualTo(200)
        }

        @DisplayName("원본 예외가 그대로 throw 된다.")
        @Test
        fun rethrowsOriginalException_whenLogSaveFails() {
            // arrange
            val request = createRequest()
            whenever(delegate.execute(any(), any())).thenThrow(SocketTimeoutException("Read timed out"))
            whenever(logRepository.save(any())).thenThrow(RuntimeException("DB 연결 실패"))

            // act & assert
            val exception = assertThrows<SocketTimeoutException> {
                loggingClient.execute(request, Request.Options())
            }
            assertThat(exception.message).isEqualTo("Read timed out")
        }
    }
}
