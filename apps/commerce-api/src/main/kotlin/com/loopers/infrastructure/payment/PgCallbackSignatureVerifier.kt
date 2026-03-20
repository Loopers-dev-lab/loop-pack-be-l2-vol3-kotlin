package com.loopers.infrastructure.payment

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.HexFormat

class PgCallbackSignatureVerifier(
    secret: String,
) {
    private val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA_256)

    fun sign(payload: String): String {
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(secretKeySpec)
        return HexFormat.of().formatHex(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    fun verify(payload: String, signature: String): Boolean {
        return sign(payload) == signature.lowercase()
    }

    companion object {
        private const val HMAC_SHA_256 = "HmacSHA256"
    }
}
