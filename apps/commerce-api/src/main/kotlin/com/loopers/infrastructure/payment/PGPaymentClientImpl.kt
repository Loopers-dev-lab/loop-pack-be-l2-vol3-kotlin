package com.loopers.infrastructure.payment

import com.loopers.config.pg.PGProperties
import com.loopers.domain.payment.PGOrderPayments
import com.loopers.domain.payment.PGPaymentClient
import com.loopers.domain.payment.PGPaymentRequest
import com.loopers.domain.payment.PGPaymentResponse
import com.loopers.domain.payment.PGTransactionDetail
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * PG 결제 클라이언트 구현체.
 *
 * Resilience4j 적용 구조:
 * - CircuitBreaker(Outer) → Retry(Inner) 순서로 동작
 * - 같은 클래스 내부 호출은 AOP 프록시를 타지 않으므로, 실제 HTTP 호출은 [PGPaymentApiCaller]로 분리
 * - 결제 요청(pg-request)과 조회(pg-query) 서킷브레이커 인스턴스 분리
 */
@Component
class PGPaymentClientImpl(
    private val pgPaymentApiCaller: PGPaymentApiCaller,
) : PGPaymentClient {

    private val log = LoggerFactory.getLogger(javaClass)

    @RateLimiter(name = "pg-request", fallbackMethod = "requestPaymentFallback")
    @CircuitBreaker(name = "pg-request", fallbackMethod = "requestPaymentFallback")
    override fun requestPayment(request: PGPaymentRequest): PGPaymentResponse {
        return pgPaymentApiCaller.requestPayment(request)
    }

    @CircuitBreaker(name = "pg-query", fallbackMethod = "getPaymentByTransactionKeyFallback")
    override fun getPaymentByTransactionKey(userId: Long, transactionKey: String): PGTransactionDetail {
        return pgPaymentApiCaller.getPaymentByTransactionKey(userId, transactionKey)
    }

    @CircuitBreaker(name = "pg-query", fallbackMethod = "getPaymentsByOrderIdFallback")
    override fun getPaymentsByOrderId(userId: Long, orderId: String): PGOrderPayments {
        return pgPaymentApiCaller.getPaymentsByOrderId(userId, orderId)
    }

    @Suppress("unused")
    private fun requestPaymentFallback(request: PGPaymentRequest, ex: Throwable): PGPaymentResponse {
        log.error("[PG Fallback] 결제 요청 실패 - orderId: {}, error: {}", request.orderId, ex.message)
        throw classifyException(ex)
    }

    @Suppress("unused")
    private fun getPaymentByTransactionKeyFallback(userId: Long, transactionKey: String, ex: Throwable): PGTransactionDetail {
        log.error("[PG Fallback] 결제 조회 실패 - transactionKey: {}, error: {}", transactionKey, ex.message)
        throw classifyException(ex)
    }

    @Suppress("unused")
    private fun getPaymentsByOrderIdFallback(userId: Long, orderId: String, ex: Throwable): PGOrderPayments {
        log.error("[PG Fallback] 주문별 결제 조회 실패 - orderId: {}, error: {}", orderId, ex.message)
        throw classifyException(ex)
    }

    /**
     * 예외 타입별 세분화된 에러 분류.
     * - 서킷 OPEN: PG 시스템 중단 → 503
     * - 타임아웃: 응답 지연 → 504 (실제 처리됐을 수 있음)
     * - 4xx (HttpClientErrorException): 사용자 입력 오류 → 400
     * - 5xx / 기타: PG 서버 오류 → 502
     */
    private fun classifyException(ex: Throwable): CoreException {
        return when (ex) {
            is io.github.resilience4j.ratelimiter.RequestNotPermitted ->
                CoreException(ErrorType.PG_RATE_LIMITED, "결제 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")

            is io.github.resilience4j.circuitbreaker.CallNotPermittedException ->
                CoreException(ErrorType.PG_CIRCUIT_OPEN)

            is java.net.SocketTimeoutException,
            is java.util.concurrent.TimeoutException,
            ->
                CoreException(ErrorType.PG_TIMEOUT)

            is org.springframework.web.client.HttpClientErrorException ->
                CoreException(ErrorType.BAD_REQUEST, "PG 요청이 거부되었습니다: ${ex.message}")

            is org.springframework.web.client.HttpServerErrorException ->
                CoreException(ErrorType.PG_SERVER_ERROR)

            else ->
                CoreException(ErrorType.PG_SERVER_ERROR, "PG 시스템 연동 중 오류가 발생했습니다.")
        }
    }
}

/**
 * 실제 PG HTTP 호출을 담당하는 빈.
 * [PGPaymentClientImpl]과 분리하여 AOP 프록시가 @Retry를 정상적으로 적용할 수 있게 한다.
 */
@Component
class PGPaymentApiCaller(
    private val pgRestClient: RestClient,
    private val pgProperties: PGProperties,
) {

    fun requestPayment(request: PGPaymentRequest): PGPaymentResponse {
        val body = SimulatorPaymentDto.PaymentRequestBody(
            orderId = request.orderId,
            cardType = request.cardType,
            cardNo = request.cardNo,
            amount = request.amount,
            callbackUrl = pgProperties.callbackUrl,
        )

        val response = pgRestClient.post()
            .uri("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-USER-ID", request.userId.toString())
            .body(body)
            .retrieve()
            .body<SimulatorPaymentDto.ApiResponse<SimulatorPaymentDto.TransactionResponseBody>>()
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청 응답이 비어있습니다.")

        val responseBody = response.data

        return PGPaymentResponse(
            transactionKey = responseBody.transactionKey,
            status = responseBody.status,
            reason = responseBody.reason,
        )
    }

    @Retry(name = "pg-query")
    fun getPaymentByTransactionKey(userId: Long, transactionKey: String): PGTransactionDetail {
        val response = pgRestClient.get()
            .uri("/api/v1/payments/{transactionKey}", transactionKey)
            .header("X-USER-ID", userId.toString())
            .retrieve()
            .body<SimulatorPaymentDto.ApiResponse<SimulatorPaymentDto.TransactionDetailResponseBody>>()
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 조회 응답이 비어있습니다.")

        val responseBody = response.data

        return PGTransactionDetail(
            transactionKey = responseBody.transactionKey,
            orderId = responseBody.orderId,
            cardType = responseBody.cardType,
            cardNo = responseBody.cardNo,
            amount = responseBody.amount,
            status = responseBody.status,
            reason = responseBody.reason,
        )
    }

    @Retry(name = "pg-query")
    fun getPaymentsByOrderId(userId: Long, orderId: String): PGOrderPayments {
        val response = pgRestClient.get()
            .uri("/api/v1/payments?orderId={orderId}", orderId)
            .header("X-USER-ID", userId.toString())
            .retrieve()
            .body<SimulatorPaymentDto.ApiResponse<SimulatorPaymentDto.OrderResponseBody>>()
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 주문별 결제 조회 응답이 비어있습니다.")

        val responseBody = response.data

        return PGOrderPayments(
            orderId = responseBody.orderId,
            transactions = responseBody.transactions.map {
                PGPaymentResponse(
                    transactionKey = it.transactionKey,
                    status = it.status,
                    reason = it.reason,
                )
            },
        )
    }
}
