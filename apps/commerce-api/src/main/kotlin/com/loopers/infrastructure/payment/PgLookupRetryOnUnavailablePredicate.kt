package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentGateway
import java.util.function.Predicate

class PgLookupRetryOnUnavailablePredicate : Predicate<Any> {

    override fun test(result: Any): Boolean {
        return result is PaymentGateway.LookupResult.Unavailable
    }
}
