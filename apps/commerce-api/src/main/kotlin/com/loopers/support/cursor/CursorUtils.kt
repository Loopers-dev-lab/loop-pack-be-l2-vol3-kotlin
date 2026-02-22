package com.loopers.support.cursor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Base64

object CursorUtils {
    private val objectMapper = jacksonObjectMapper()

    fun encode(map: Map<String, Any>): String {
        val json = objectMapper.writeValueAsString(map)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
    }

    fun decode(cursor: String): Map<String, Any> {
        val json = String(Base64.getUrlDecoder().decode(cursor))
        return objectMapper.readValue(json)
    }
}
