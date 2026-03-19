package com.loopers.infrastructure.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.pg.PgCommunicationLog
import com.loopers.domain.pg.PgCommunicationLogRepository
import feign.Client
import feign.Request
import feign.Response
import org.slf4j.LoggerFactory
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

class PgLoggingClient(
    private val delegate: Client,
    private val pgCommunicationLogRepository: PgCommunicationLogRepository,
    private val transactionManager: PlatformTransactionManager,
    private val objectMapper: ObjectMapper,
) : Client {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(request: Request, options: Request.Options): Response {
        val startTime = System.currentTimeMillis()
        val requestUrl = request.url()
        val httpMethod = request.httpMethod().name
        val requestBody = request.body()?.let { String(it, Charsets.UTF_8) }
        val orderId = extractOrderId(requestUrl, requestBody)

        return try {
            val response = delegate.execute(request, options)
            val elapsed = System.currentTimeMillis() - startTime

            val responseBytes = response.body()?.asInputStream()?.readAllBytes()
            val responseBody = responseBytes?.let { String(it, Charsets.UTF_8) }
            val bufferedResponse = response.toBuilder()
                .body(responseBytes ?: ByteArray(0))
                .build()

            val transactionKey = extractTransactionKey(responseBody)

            val isSuccess = bufferedResponse.status() in 200..299

            saveLog(
                httpMethod, requestUrl, orderId, transactionKey,
                requestBody, responseBody, bufferedResponse.status(),
                isSuccess, null, elapsed,
            )

            bufferedResponse
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime

            saveLog(
                httpMethod, requestUrl, orderId, null,
                requestBody, null, null,
                false, e.message, elapsed,
            )

            throw e
        }
    }

    private fun saveLog(
        method: String,
        url: String,
        orderId: String?,
        transactionKey: String?,
        requestBody: String?,
        responseBody: String?,
        httpStatus: Int?,
        success: Boolean,
        errorMessage: String?,
        elapsed: Long,
    ) {
        try {
            val txTemplate = TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }
            txTemplate.execute {
                pgCommunicationLogRepository.save(
                    PgCommunicationLog(
                        method = method,
                        url = url,
                        orderId = orderId,
                        transactionKey = transactionKey,
                        requestBody = requestBody,
                        responseBody = responseBody,
                        httpStatus = httpStatus,
                        success = success,
                        errorMessage = errorMessage?.take(500),
                        elapsed = elapsed,
                    ),
                )
            }
        } catch (e: Exception) {
            log.warn("PG 통신 로그 저장 실패: ${e.message}", e)
        }
    }

    private fun extractOrderId(url: String, requestBody: String?): String? {
        if (url.contains("orderId=")) {
            return url.substringAfter("orderId=").substringBefore("&")
        }
        return requestBody?.let {
            try {
                val tree = objectMapper.readTree(it)
                tree.get("orderId")?.asText()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun extractTransactionKey(responseBody: String?): String? {
        return responseBody?.let {
            try {
                val tree = objectMapper.readTree(it)
                tree.path("data").path("transactionKey").asText(null)
            } catch (e: Exception) {
                null
            }
        }
    }
}
