package com.loopers.support.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.support.error.ErrorType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

@Component
@Order(0)
class PgCallbackSignatureFilter(
    @Value("\${pg.callback-secret-key}") private val secretKey: String,
    private val objectMapper: ObjectMapper,
) : Filter {

    companion object {
        private const val CALLBACK_PATH = "/api/v1/payments/callback"
        const val SIGNATURE_HEADER = "X-PG-Signature"
        const val TIMESTAMP_HEADER = "X-PG-Timestamp"
        private const val MAX_TIMESTAMP_DRIFT_SECONDS = 300L
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (httpRequest.requestURI != CALLBACK_PATH) {
            chain.doFilter(request, response)
            return
        }

        val bodyBytes = httpRequest.inputStream.readAllBytes()

        val timestamp = httpRequest.getHeader(TIMESTAMP_HEADER)
        if (timestamp == null) {
            httpResponse.writeFilterErrorResponse(objectMapper, ErrorType.UNAUTHORIZED, "콜백 타임스탬프가 필요합니다.")
            return
        }

        val timestampEpoch = try {
            timestamp.toLong()
        } catch (e: NumberFormatException) {
            httpResponse.writeFilterErrorResponse(objectMapper, ErrorType.UNAUTHORIZED, "유효하지 않은 타임스탬프입니다.")
            return
        }

        val drift = abs(Instant.now().epochSecond - timestampEpoch)
        if (drift > MAX_TIMESTAMP_DRIFT_SECONDS) {
            httpResponse.writeFilterErrorResponse(objectMapper, ErrorType.UNAUTHORIZED, "만료된 콜백 요청입니다.")
            return
        }

        val signature = httpRequest.getHeader(SIGNATURE_HEADER)
        if (signature == null) {
            httpResponse.writeFilterErrorResponse(objectMapper, ErrorType.UNAUTHORIZED, "콜백 서명이 필요합니다.")
            return
        }

        val expectedSignature = computeHmacHex(bodyBytes, timestamp)
        if (!MessageDigest.isEqual(expectedSignature.toByteArray(), signature.toByteArray())) {
            httpResponse.writeFilterErrorResponse(objectMapper, ErrorType.UNAUTHORIZED, "유효하지 않은 콜백 서명입니다.")
            return
        }

        chain.doFilter(CachedBodyRequestWrapper(httpRequest, bodyBytes), response)
    }

    private fun computeHmacHex(body: ByteArray, timestamp: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secretKey.toByteArray(), HMAC_ALGORITHM))
        val payload = "$timestamp.".toByteArray() + body
        return mac.doFinal(payload).joinToString("") { "%02x".format(it) }
    }

    private class CachedBodyRequestWrapper(
        request: HttpServletRequest,
        private val cachedBody: ByteArray,
    ) : HttpServletRequestWrapper(request) {

        override fun getInputStream(): ServletInputStream {
            val byteArrayInputStream = ByteArrayInputStream(cachedBody)
            return object : ServletInputStream() {
                override fun read(): Int = byteArrayInputStream.read()
                override fun isFinished(): Boolean = byteArrayInputStream.available() == 0
                override fun isReady(): Boolean = true
                override fun setReadListener(listener: ReadListener?) {}
            }
        }

        override fun getReader(): BufferedReader =
            BufferedReader(InputStreamReader(getInputStream()))
    }
}
