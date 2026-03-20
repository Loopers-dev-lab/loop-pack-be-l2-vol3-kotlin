package com.loopers.infrastructure.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.timelimiter.TimeLimiter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException

class ResilientPgClient(
    private val delegate: PgClient,
    private val properties: PaymentResilienceProperties,
    private val circuitBreaker: CircuitBreaker,
    private val timeLimiter: TimeLimiter,
    private val executor: ExecutorService,
) : PgClient {
    constructor(
        delegate: PgClient,
        properties: PaymentResilienceProperties,
        executor: ExecutorService,
    ) : this(
        delegate = delegate,
        properties = properties,
        circuitBreaker = PaymentResilienceConfig().paymentCircuitBreaker(properties),
        timeLimiter = PaymentResilienceConfig().paymentTimeLimiter(properties),
        executor = executor,
    )

    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        val decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker) {
            timeLimiter.executeFutureSupplier {
                CompletableFuture.supplyAsync(
                    { delegate.requestPayment(request) },
                    executor,
                )
            }
        }

        return try {
            decoratedSupplier.get()
        } catch (e: Exception) {
            handleFailure(request, unwrap(e))
        }
    }

    private fun handleFailure(request: PgPaymentRequest, throwable: Throwable): PgPaymentResponse {
        if (throwable is CallNotPermittedException || throwable is TimeoutException) {
            return deferredResponse(request)
        }

        if (throwable is CoreException && throwable.errorType == ErrorType.INTERNAL_ERROR) {
            return deferredResponse(request)
        }

        return when (throwable) {
            else -> throw throwable
        }
    }

    private fun unwrap(throwable: Throwable): Throwable {
        return when (throwable) {
            is CompletionException -> throwable.cause ?: throwable
            else -> throwable
        }
    }

    private fun deferredResponse(request: PgPaymentRequest): PgPaymentResponse {
        return PgPaymentResponse(
            orderId = request.orderId,
            amount = request.amount,
            transactionId = "deferred",
            status = PgPaymentStatus.DEFERRED,
        )
    }
}
