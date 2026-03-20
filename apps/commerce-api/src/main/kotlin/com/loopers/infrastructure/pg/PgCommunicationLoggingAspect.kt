package com.loopers.infrastructure.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.pg.PgCommunicationLog
import com.loopers.domain.pg.PgCommunicationLogRepository
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Aspect
@Component
class PgCommunicationLoggingAspect(
    private val pgCommunicationLogRepository: PgCommunicationLogRepository,
    private val transactionManager: PlatformTransactionManager,
    private val objectMapper: ObjectMapper,
    @Value("\${pg.base-url}") private val pgBaseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val requiresNewTxTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    @Around(
        "execution(* com.loopers.application.payment.PgPaymentClient.requestPayment(..))" +
            " || execution(* com.loopers.application.payment.PgPaymentClient.getPaymentStatus(..))",
    )
    fun logPgCommunication(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = joinPoint.signature.name
        val args = joinPoint.args
        val startTime = System.currentTimeMillis()

        val (method, orderId, requestBody) = extractRequestInfo(methodName, args)

        return try {
            val result = joinPoint.proceed()
            val elapsed = System.currentTimeMillis() - startTime

            val responseBody = toJson(result)
            val transactionKey = extractTransactionKey(responseBody)
            val success = (result as? PgApiResponse<*>)?.isSuccess() ?: true

            saveLog(method, orderId, transactionKey, requestBody, responseBody, success, null, elapsed)
            result
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime

            saveLog(method, orderId, null, requestBody, null, false, e.message, elapsed)
            throw e
        }
    }

    private fun extractRequestInfo(methodName: String, args: Array<Any>): Triple<String, String?, String?> {
        return when (methodName) {
            "requestPayment" -> {
                val request = args.getOrNull(1)
                val orderId = if (request is PgPaymentRequest) request.orderId else null
                Triple("POST", orderId, toJson(request))
            }
            "getPaymentStatus" -> {
                val transactionKey = args.getOrNull(1) as? String
                Triple("GET", null, transactionKey)
            }
            else -> Triple(methodName, null, toJson(args))
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

    private fun toJson(obj: Any?): String? {
        return try {
            obj?.let { objectMapper.writeValueAsString(it) }
        } catch (e: Exception) {
            obj?.toString()
        }
    }

    private fun saveLog(
        method: String,
        orderId: String?,
        transactionKey: String?,
        requestBody: String?,
        responseBody: String?,
        success: Boolean,
        errorMessage: String?,
        elapsed: Long,
    ) {
        try {
            requiresNewTxTemplate.execute {
                pgCommunicationLogRepository.save(
                    PgCommunicationLog(
                        method = method,
                        url = pgBaseUrl,
                        orderId = orderId,
                        transactionKey = transactionKey,
                        requestBody = requestBody,
                        responseBody = responseBody,
                        httpStatus = null,
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
}
