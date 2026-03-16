package com.loopers.infrastructure.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PgPaymentStatus
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
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
) {
    private val restTemplate: RestTemplate = restTemplateBuilder
        .rootUri(properties.baseUrl)
        .connectTimeout(properties.connectTimeout)
        .readTimeout(properties.readTimeout)
        .build()

    @CircuitBreaker(name = "pgSimulator", fallbackMethod = "requestPaymentFallback")
    fun requestPayment(memberId: Long, request: Request): RequestResult {
        val headers = headers(memberId)
        val responseType = object : ParameterizedTypeReference<PgApiResponse<PgTransactionResponse>>() {}

        return try {
            val response = restTemplate.exchange(
                "/api/v1/payments",
                HttpMethod.POST,
                HttpEntity(request.toBody(), headers),
                responseType,
            )
            val data = response.body?.data ?: return RequestResult.RequestFailed("PG 응답 본문이 비어 있습니다.")
            RequestResult.Accepted(
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: ResourceAccessException) {
            RequestResult.Unknown("PG 요청 타임아웃 또는 네트워크 오류가 발생했습니다.")
        } catch (e: HttpStatusCodeException) {
            RequestResult.RequestFailed(extractMessage(e.responseBodyAsString) ?: "PG 요청에 실패했습니다.")
        }
    }

    @Suppress("unused")
    fun requestPaymentFallback(memberId: Long, request: Request, throwable: Throwable): RequestResult =
        RequestResult.RequestFailed("PG 요청이 차단되었습니다. 잠시 후 다시 시도해주세요.")

    @CircuitBreaker(name = "pgSimulator", fallbackMethod = "lookupFallback")
    fun getTransaction(memberId: Long, transactionKey: String): LookupResult {
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
            val data = response.body?.data ?: return LookupResult.NotFound
            LookupResult.Found(
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: ResourceAccessException) {
            LookupResult.Unavailable("PG 상태 조회 중 타임아웃이 발생했습니다.")
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode.value() == 404) LookupResult.NotFound
            else LookupResult.Unavailable(extractMessage(e.responseBodyAsString) ?: "PG 상태 조회에 실패했습니다.")
        }
    }

    @Suppress("unused")
    fun lookupFallback(memberId: Long, transactionKey: String, throwable: Throwable): LookupResult =
        LookupResult.Unavailable("PG 상태 조회가 차단되었습니다. 잠시 후 다시 시도해주세요.")

    @CircuitBreaker(name = "pgSimulator", fallbackMethod = "lookupByOrderFallback")
    fun findLatestTransactionByOrderId(memberId: Long, orderId: String): LookupResult {
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
            val latest = response.body?.data?.transactions?.lastOrNull() ?: return LookupResult.NotFound
            LookupResult.Found(
                transactionKey = latest.transactionKey,
                status = latest.status,
                reason = latest.reason,
            )
        } catch (e: ResourceAccessException) {
            LookupResult.Unavailable("PG 주문 기준 조회 중 타임아웃이 발생했습니다.")
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode.value() == 404) LookupResult.NotFound
            else LookupResult.Unavailable(extractMessage(e.responseBodyAsString) ?: "PG 주문 기준 조회에 실패했습니다.")
        }
    }

    @Suppress("unused")
    fun lookupByOrderFallback(memberId: Long, orderId: String, throwable: Throwable): LookupResult =
        LookupResult.Unavailable("PG 주문 기준 조회가 차단되었습니다. 잠시 후 다시 시도해주세요.")

    private fun headers(memberId: Long) = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("X-USER-ID", memberId.toString())
    }

    private fun extractMessage(responseBody: String?): String? =
        responseBody
            ?.substringAfter("\"message\":\"", "")
            ?.substringBefore("\"")
            ?.takeIf { it.isNotBlank() }

    data class Request(
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

    sealed interface RequestResult {
        data class Accepted(
            val transactionKey: String,
            val status: PgPaymentStatus,
            val reason: String?,
        ) : RequestResult

        data class RequestFailed(val reason: String) : RequestResult

        data class Unknown(val reason: String) : RequestResult
    }

    sealed interface LookupResult {
        data class Found(
            val transactionKey: String,
            val status: PgPaymentStatus,
            val reason: String?,
        ) : LookupResult

        data object NotFound : LookupResult

        data class Unavailable(val reason: String) : LookupResult
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
