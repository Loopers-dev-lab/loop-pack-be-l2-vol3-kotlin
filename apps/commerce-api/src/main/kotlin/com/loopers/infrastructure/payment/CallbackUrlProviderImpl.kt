package com.loopers.infrastructure.payment

import com.loopers.application.payment.CallbackUrlProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CallbackUrlProviderImpl(
    @Value("\${payment.pg.callback-url}") private val callbackUrl: String,
) : CallbackUrlProvider {

    override fun getCallbackUrl(): String = callbackUrl
}
