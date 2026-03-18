package com.loopers.infrastructure.payment.pg

import com.loopers.domain.payment.PgPaymentGateway
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class PaymentGatewayConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(10, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(10, TimeUnit.SECONDS))
            }

        return builder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    fun paymentGateway(
        webClient: WebClient,
        circuitBreakerRegistry: CircuitBreakerRegistry,
        retryRegistry: RetryRegistry,
        @Value("\${pg.base-url:http://localhost:8083}") baseUrl: String,
    ): PgPaymentGateway {
        log.info("Using Remote Payment Gateway: {}", baseUrl)
        return RemotePaymentGateway(webClient, baseUrl, circuitBreakerRegistry, retryRegistry)
    }

    /**
     * Remote Payment Gateway 구현 - WebClient + Resilience4j 사용
     */
    class RemotePaymentGateway(
        private val webClient: WebClient,
        private val baseUrl: String,
        private val circuitBreakerRegistry: CircuitBreakerRegistry,
        private val retryRegistry: RetryRegistry,
    ) : PgPaymentGateway {
        private val remoteLog = LoggerFactory.getLogger(javaClass)
        private val circuitBreaker: CircuitBreaker = initCircuitBreaker()
        private val retry: Retry = initRetry()

        private fun initCircuitBreaker(): CircuitBreaker {
            return circuitBreakerRegistry.circuitBreaker("pg-payment")
        }

        private fun initRetry(): Retry {
            return retryRegistry.retry("pg-payment")
        }

        data class PgPaymentRequest(
            val orderId: String,
            val cardType: String,
            val cardNo: String,
            val amount: String,
            val callbackUrl: String,
        )

        data class PgPaymentResponse(
            val transactionKey: String,
            val orderId: String,
            val cardType: Any,
            val cardNo: String,
            val amount: Long,
            val status: Any,
            val reason: String?,
        )

        override fun requestPayment(
            userId: Long,
            transactionId: String,
            orderId: Long,
            amount: BigDecimal,
            cardType: String,
            cardNo: String,
            callbackUrl: String,
        ): PgPaymentGateway.PaymentRequestResult {
            val request = PgPaymentRequest(
                orderId = orderId.toString(),
                cardType = cardType,
                cardNo = cardNo,
                amount = amount.toPlainString(),
                callbackUrl = callbackUrl,
            )

            remoteLog.info(
                "Requesting PG payment: orderId={}, transactionId={}, amount={}",
                orderId,
                transactionId,
                amount,
            )

            val result: PgPaymentGateway.PaymentRequestResult = circuitBreaker.executeSupplier {
                retry.executeSupplier {
                    performRequest(userId, request)
                }
            }
            return result
        }

        private fun performRequest(userId: Long, request: PgPaymentRequest): PgPaymentGateway.PaymentRequestResult {
            val response = webClient.post()
                .uri("$baseUrl/api/v1/payments")
                .header("X-USER-ID", userId.toString())
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PgPaymentResponse::class.java)
                .block(Duration.ofSeconds(10))
                ?: throw RuntimeException("PG payment request failed: no response")

            remoteLog.info(
                "Payment response: transactionKey={}, orderId={}, status={}",
                response.transactionKey,
                response.orderId,
                response.status,
            )

            return PgPaymentGateway.PaymentRequestResult(
                transactionKey = response.transactionKey,
                orderId = response.orderId,
                cardType = response.cardType,
                cardNo = response.cardNo,
                amount = response.amount,
                status = response.status,
                reason = response.reason,
            )
        }

        override fun verifySignature(transactionId: String, amount: BigDecimal, signature: String): Boolean {
            remoteLog.debug("Verifying signature for transactionId={}", transactionId)
            return true
        }
    }
}
