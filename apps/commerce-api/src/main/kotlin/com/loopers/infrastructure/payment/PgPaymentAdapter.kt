package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.domain.payment.PgPaymentStatusResponse
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class PgPaymentAdapter(
    private val pgRestClient: RestClient,
    private val pgOutboundExecutor: ExecutorService,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    @Value("\${pg.timeout.overall-ms:600}") private val overallTimeoutMs: Long,
) : PgPaymentPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker: CircuitBreaker by lazy {
        circuitBreakerRegistry.circuitBreaker("pgPayment")
    }

    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        val pgRequest = PgApiRequest.Payment(
            orderId = request.orderId.toString(),
            cardType = request.cardType,
            cardNo = request.cardNo,
            amount = request.amount.toLong(),
            callbackUrl = request.callbackUrl,
        )

        return try {
            circuitBreaker.executeCallable {
                val future = CompletableFuture.supplyAsync(
                    {
                        pgRestClient.post()
                            .uri("/api/v1/payments")
                            .header("X-USER-ID", request.userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(pgRequest)
                            .retrieve()
                            .body(PgApiResponse.Payment::class.java)!!
                    },
                    pgOutboundExecutor,
                ).orTimeout(overallTimeoutMs, TimeUnit.MILLISECONDS)

                val response = future.join()
                PgPaymentResponse.Accepted(response.data!!.transactionKey)
            }
        } catch (e: CallNotPermittedException) {
            log.warn("PG circuit breaker OPEN: {}", e.message)
            PgPaymentResponse.CircuitOpen
        } catch (e: CompletionException) {
            when (e.cause) {
                is TimeoutException -> {
                    log.warn("PG 결제 요청 timeout ({}ms)", overallTimeoutMs)
                    PgPaymentResponse.Timeout
                }
                else -> {
                    log.warn("PG 결제 요청 실패: {}", e.cause?.message)
                    PgPaymentResponse.ImmediateFailure(PaymentReasonCode.PG_INTERNAL_ERROR)
                }
            }
        } catch (e: Exception) {
            log.warn("PG 결제 요청 실패: {}", e.message)
            PgPaymentResponse.ImmediateFailure(PaymentReasonCode.PG_INTERNAL_ERROR)
        }
    }

    override fun queryPaymentStatus(transactionKey: String, userId: Long): PgPaymentStatusResponse {
        val response = pgRestClient.get()
            .uri("/api/v1/payments/{transactionKey}", transactionKey)
            .header("X-USER-ID", userId.toString())
            .retrieve()
            .body(PgApiResponse.PaymentDetail::class.java)!!

        val data = response.data!!
        return PgPaymentStatusResponse(
            transactionKey = data.transactionKey,
            status = data.status,
            reason = data.reason,
        )
    }

    override fun isAvailable(): Boolean {
        return circuitBreaker.state != CircuitBreaker.State.OPEN
    }

    fun mapReasonCode(reason: String?): PaymentReasonCode {
        return when {
            reason == null -> PaymentReasonCode.PG_INTERNAL_ERROR
            reason.contains("한도초과") || reason.contains("한도") -> PaymentReasonCode.LIMIT_EXCEEDED
            reason.contains("잘못된 카드") || reason.contains("카드") -> PaymentReasonCode.INVALID_CARD
            else -> PaymentReasonCode.PG_INTERNAL_ERROR
        }
    }
}

object PgApiRequest {
    data class Payment(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val callbackUrl: String,
    )
}

object PgApiResponse {
    data class Payment(
        val meta: Meta,
        val data: PaymentData?,
    )

    data class PaymentDetail(
        val meta: Meta,
        val data: PaymentDetailData?,
    )

    data class Meta(
        val result: String,
        val errorCode: String?,
        val message: String?,
    )

    data class PaymentData(
        val transactionKey: String,
        val status: String,
        val reason: String?,
    )

    data class PaymentDetailData(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    )
}
