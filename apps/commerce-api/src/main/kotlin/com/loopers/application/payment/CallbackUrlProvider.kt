package com.loopers.application.payment

interface CallbackUrlProvider {
    fun getCallbackUrl(): String
}
