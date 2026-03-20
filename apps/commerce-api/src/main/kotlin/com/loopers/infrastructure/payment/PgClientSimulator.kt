package com.loopers.infrastructure.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component("delegatePgClient")
class PgClientSimulator(
    scenario: Scenario = Scenario.SUCCESS,
) : PgClient {
    @Volatile
    private var scenario: Scenario = scenario

    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        return when (scenario) {
            Scenario.SUCCESS -> mapSuccess(
                ExternalPgSuccessResponse(
                    result = "APPROVED",
                    transactionId = "pg-${request.orderId}",
                    amount = request.amount,
                ),
                request,
            )
            Scenario.FAILURE -> throw mapFailure(
                ExternalPgFailureResponse(
                    errorCode = "DECLINED",
                    message = "카드 승인에 실패했습니다.",
                ),
            )
            Scenario.TIMEOUT -> throw mapFailure(
                ExternalPgFailureResponse(
                    errorCode = "TIMEOUT",
                    message = "PG 응답 시간이 초과되었습니다.",
                ),
            )
        }
    }

    fun setScenario(scenario: Scenario) {
        this.scenario = scenario
    }

    fun resetScenario() {
        this.scenario = Scenario.SUCCESS
    }

    private fun mapSuccess(
        response: ExternalPgSuccessResponse,
        request: PgPaymentRequest,
    ): PgPaymentResponse {
        return PgPaymentResponse(
            orderId = request.orderId,
            amount = response.amount,
            transactionId = response.transactionId,
            status = when (response.result) {
                "APPROVED" -> PgPaymentStatus.APPROVED
                else -> throw CoreException(ErrorType.INTERNAL_ERROR, "알 수 없는 PG 응답입니다: ${response.result}")
            },
        )
    }

    private fun mapFailure(response: ExternalPgFailureResponse): CoreException {
        return when (response.errorCode) {
            "DECLINED" -> CoreException(ErrorType.CONFLICT, response.message)
            "TIMEOUT" -> CoreException(ErrorType.INTERNAL_ERROR, response.message)
            else -> CoreException(ErrorType.INTERNAL_ERROR, response.message)
        }
    }

    enum class Scenario {
        SUCCESS,
        FAILURE,
        TIMEOUT,
    }

    private data class ExternalPgSuccessResponse(
        val result: String,
        val transactionId: String,
        val amount: Long,
    )

    private data class ExternalPgFailureResponse(
        val errorCode: String,
        val message: String,
    )
}
