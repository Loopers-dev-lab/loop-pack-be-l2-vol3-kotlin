package com.loopers.infrastructure.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PgPaymentStatus
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate

@Component
class PgSimulatorClient(
    restTemplateBuilder: RestTemplateBuilder,
    private val properties: PgSimulatorProperties,
) : PaymentGateway {
    private val restTemplate: RestTemplate = restTemplateBuilder
        .rootUri(properties.baseUrl)
        .connectTimeout(properties.connectTimeout)
        .readTimeout(properties.readTimeout)
        .build()

    @CircuitBreaker(name = "pgSimulator", fallbackMethod = "requestPaymentFallback")
    override fun requestPayment(memberId: Long, request: PaymentGateway.Request): PaymentGateway.RequestResult {
        val headers = headers(memberId)
        val responseType = object : ParameterizedTypeReference<PgApiResponse<PgTransactionResponse>>() {}

        return try {
            val response = restTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(
                    RequestBody(
                        orderId = request.orderId,
                        cardType = request.cardType,
                        cardNo = request.cardNo,
                        amount = request.amount,
                        callbackUrl = properties.callbackUrl,
                    ).toBody(),
                    headers,
                ),
                responseType,
            )
            val data = response.body?.data ?: return PaymentGateway.RequestResult.RequestFailed("PG 응답 본문이 비어 있습니다.")
            PaymentGateway.RequestResult.Accepted(
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: ResourceAccessException) {
            PaymentGateway.RequestResult.Unknown("PG 요청 타임아웃 또는 네트워크 오류가 발생했습니다.")
        } catch (e: HttpStatusCodeException) {
            PaymentGateway.RequestResult.RequestFailed(extractMessage(e.responseBodyAsString) ?: "PG 요청에 실패했습니다.")
        }
    }

    @Suppress("unused")
    fun requestPaymentFallback(
        memberId: Long,
        request: PaymentGateway.Request,
        throwable: Throwable,
    ): PaymentGateway.RequestResult = PaymentGateway.RequestResult.RequestFailed("PG 요청이 차단되었습니다. 잠시 후 다시 시도해주세요.")

    @Retry(name = "pgSimulatorLookup", fallbackMethod = "lookupFallback")
    @CircuitBreaker(name = "pgSimulator", fallbackMethod = "lookupFallback")
    override fun getTransaction(memberId: Long, transactionKey: String): PaymentGateway.LookupResult {
        val headers = headers(memberId)
        val responseType = object : ParameterizedTypeReference<PgApiResponse<PgTransactionDetailResponse>>() {}

        return try {
            val response = restTemplate.exchange(
                "/api/v1/payments/{transactionKey}",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                responseType,
                transactionKey,
            )
            val data = response.body?.data ?: return PaymentGateway.LookupResult.NotFound
            PaymentGateway.LookupResult.Found(
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: ResourceAccessException) {
            PaymentGateway.LookupResult.Unavailable("PG 상태 조회 중 타임아웃이 발생했습니다.")
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode.value() == 404) {
                PaymentGateway.LookupResult.NotFound
            } else {
                PaymentGateway.LookupResult.Unavailable(extractMessage(e.responseBodyAsString) ?: "PG 상태 조회에 실패했습니다.")
            }
        }
    }

    @Suppress("unused")
    fun lookupFallback(
        memberId: Long,
        transactionKey: String,
        throwable: Throwable,
    ): PaymentGateway.LookupResult = PaymentGateway.LookupResult.Unavailable("PG 상태 조회가 차단되었습니다. 잠시 후 다시 시도해주세요.")

    @Retry(name = "pgSimulatorLookup", fallbackMethod = "lookupByOrderFallback")
    @CircuitBreaker(name = "pgSimulator", fallbackMethod = "lookupByOrderFallback")
    override fun findLatestTransactionByOrderId(memberId: Long, orderId: String): PaymentGateway.LookupResult {
        val headers = headers(memberId)
        val responseType = object : ParameterizedTypeReference<PgApiResponse<PgOrderResponse>>() {}

        return try {
            val response = restTemplate.exchange(
                "/api/v1/payments?orderId={orderId}",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                responseType,
                orderId,
            )
            val latest = response.body?.data?.transactions?.lastOrNull() ?: return PaymentGateway.LookupResult.NotFound
            PaymentGateway.LookupResult.Found(
                transactionKey = latest.transactionKey,
                status = latest.status,
                reason = latest.reason,
            )
        } catch (e: ResourceAccessException) {
            PaymentGateway.LookupResult.Unavailable("PG 주문 기준 조회 중 타임아웃이 발생했습니다.")
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode.value() == 404) {
                PaymentGateway.LookupResult.NotFound
            } else {
                PaymentGateway.LookupResult.Unavailable(extractMessage(e.responseBodyAsString) ?: "PG 주문 기준 조회에 실패했습니다.")
            }
        }
    }

    @Suppress("unused")
    fun lookupByOrderFallback(
        memberId: Long,
        orderId: String,
        throwable: Throwable,
    ): PaymentGateway.LookupResult = PaymentGateway.LookupResult.Unavailable("PG 주문 기준 조회가 차단되었습니다. 잠시 후 다시 시도해주세요.")

    private fun headers(memberId: Long) = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("X-USER-ID", memberId.toString())
    }

    private fun extractMessage(responseBody: String?): String? =
        responseBody
            ?.substringAfter("\"message\":\"", "")
            ?.substringBefore("\"")
            ?.takeIf { it.isNotBlank() }

    private data class RequestBody(
        val orderId: String,
        val cardType: CardType,
        val cardNo: String,
        val amount: Long,
        val callbackUrl: String,
    ) {
        fun toBody() = mapOf(
            "orderId" to orderId,
            "cardType" to cardType.name,
            "cardNo" to cardNo,
            "amount" to amount,
            "callbackUrl" to callbackUrl,
        )
    }

    data class PgApiResponse<T>(
        val meta: Meta,
        val data: T?,
    ) {
        data class Meta(
            val result: String,
            val errorCode: String?,
            val message: String?,
        )
    }

    data class PgTransactionResponse(
        val transactionKey: String,
        val status: PgPaymentStatus,
        val reason: String?,
    )

    data class PgTransactionDetailResponse(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: PgPaymentStatus,
        val reason: String?,
    )

    data class PgOrderResponse(
        val orderId: String,
        val transactions: List<PgTransactionResponse>,
    )
}
