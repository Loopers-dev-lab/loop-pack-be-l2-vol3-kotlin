package com.loopers.infrastructure.pg

import com.loopers.application.payment.PgPaymentClient
import com.loopers.domain.pg.PgCommunicationLog
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * PgCommunicationLoggingAspect 통합 테스트
 * - PgClient(Feign)만 MockBean으로 교체하여 AOP가 실제 PgPaymentClient를 감싸는 구조 검증
 * - PG 통신 성공/실패 시 pg_communication_logs 테이블에 로그가 올바르게 저장되는지 확인
 */
@SpringBootTest
class PgCommunicationLoggingAspectIntegrationTest @Autowired constructor(
    private val pgPaymentClient: PgPaymentClient,
    private val pgCommunicationLogJpaRepository: PgCommunicationLogJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockitoBean
    private lateinit var pgClient: PgClient

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun findAllLogs(): List<PgCommunicationLog> {
        return pgCommunicationLogJpaRepository.findAll()
    }

    @DisplayName("결제 요청 로깅 시,")
    @Nested
    inner class RequestPaymentLogging {

        @DisplayName("PG 결제 요청이 성공하면, 성공 로그가 저장된다.")
        @Test
        fun savesSuccessLog_whenPgRequestSucceeds() {
            // arrange
            val request = PgPaymentRequest(
                orderId = "12345",
                cardType = "VISA",
                cardNo = "1234-5678-9012-3456",
                amount = 129000,
                callbackUrl = "http://localhost:8080/api/v1/payments/callback",
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgApiResponse(
                    meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
                    data = PgPaymentResponse(transactionKey = "txn-log-001", status = "SUCCESS", reason = null),
                ),
            )

            // act
            pgPaymentClient.requestPayment("1", request)

            // assert
            val logs = findAllLogs()
            assertThat(logs).hasSize(1)
            val log = logs[0]
            assertAll(
                { assertThat(log.method).isEqualTo("POST") },
                { assertThat(log.orderId).isEqualTo("12345") },
                { assertThat(log.transactionKey).isEqualTo("txn-log-001") },
                { assertThat(log.success).isTrue() },
                { assertThat(log.errorMessage).isNull() },
                { assertThat(log.requestBody).contains("12345") },
                { assertThat(log.responseBody).contains("txn-log-001") },
                { assertThat(log.elapsed).isGreaterThanOrEqualTo(0) },
            )
        }

        @DisplayName("PG 결제 요청이 실패하면, 재시도 횟수만큼 실패 로그가 저장된다.")
        @Test
        fun savesFailureLog_whenPgRequestFails() {
            // arrange
            val request = PgPaymentRequest(
                orderId = "99999",
                cardType = "MASTER",
                cardNo = "9999-8888-7777-6666",
                amount = 50000,
                callbackUrl = "http://localhost:8080/api/v1/payments/callback",
            )
            whenever(pgClient.requestPayment(any(), any()))
                .thenThrow(RuntimeException("Connection refused"))

            // act
            try {
                pgPaymentClient.requestPayment("1", request)
            } catch (_: Exception) {
                // 예외는 무시 — 로그 저장 여부만 검증
            }

            // assert — Retry(maxAttempts=3)로 재시도마다 AOP 로그가 쌓임
            val logs = findAllLogs()
            assertThat(logs).hasSizeGreaterThanOrEqualTo(1)
            assertAll(
                { assertThat(logs).allSatisfy { log -> assertThat(log.method).isEqualTo("POST") } },
                { assertThat(logs).allSatisfy { log -> assertThat(log.orderId).isEqualTo("99999") } },
                { assertThat(logs).allSatisfy { log -> assertThat(log.success).isFalse() } },
                { assertThat(logs).allSatisfy { log -> assertThat(log.responseBody).isNull() } },
            )
        }
    }

    @DisplayName("상태 조회 로깅 시,")
    @Nested
    inner class GetPaymentStatusLogging {

        @DisplayName("PG 상태 조회가 성공하면, 성공 로그가 저장된다.")
        @Test
        fun savesSuccessLog_whenStatusQuerySucceeds() {
            // arrange
            whenever(pgClient.getPaymentStatus(any(), eq("txn-status-001"))).thenReturn(
                PgApiResponse(
                    meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
                    data = PgTransactionDetailResponse(
                        transactionKey = "txn-status-001",
                        orderId = "12345",
                        cardType = "VISA",
                        cardNo = "1234-5678-9012-3456",
                        amount = 129000,
                        status = "SUCCESS",
                        reason = null,
                    ),
                ),
            )

            // act
            pgPaymentClient.getPaymentStatus("1", "txn-status-001")

            // assert
            val logs = findAllLogs()
            assertThat(logs).hasSize(1)
            val log = logs[0]
            assertAll(
                { assertThat(log.method).isEqualTo("GET") },
                { assertThat(log.success).isTrue() },
                { assertThat(log.requestBody).isEqualTo("txn-status-001") },
                { assertThat(log.responseBody).contains("txn-status-001") },
            )
        }

        @DisplayName("PG 상태 조회가 실패하면, 실패 로그가 저장되고 CircuitBreaker fallback이 응답을 반환한다.")
        @Test
        fun savesFailureLog_whenStatusQueryFails() {
            // arrange
            whenever(pgClient.getPaymentStatus(any(), eq("txn-status-fail")))
                .thenThrow(RuntimeException("Read timed out"))

            // act — AOP가 실패를 기록한 뒤, CircuitBreaker fallback이 PENDING 응답을 반환
            val result = pgPaymentClient.getPaymentStatus("1", "txn-status-fail")

            // assert
            val logs = findAllLogs()
            assertThat(logs).hasSize(1)
            val log = logs[0]
            assertAll(
                { assertThat(log.method).isEqualTo("GET") },
                { assertThat(log.success).isFalse() },
                { assertThat(result.data?.status).isEqualTo("PENDING") },
            )
        }
    }
}
