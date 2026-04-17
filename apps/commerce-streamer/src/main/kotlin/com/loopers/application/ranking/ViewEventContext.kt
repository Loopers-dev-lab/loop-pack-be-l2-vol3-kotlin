package com.loopers.application.ranking

data class ViewEventContext(
    val loginId: String?,
    val clientIp: String?,
    val userAgent: String?,
    val referer: String?,
) {
    companion object {
        fun from(payload: Map<String, Any?>): ViewEventContext {
            return ViewEventContext(
                loginId = payload["loginId"] as? String,
                clientIp = payload["clientIp"] as? String,
                userAgent = payload["userAgent"] as? String,
                referer = payload["referer"] as? String,
            )
        }
    }
}
