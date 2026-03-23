package com.loopers.support.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType

fun HttpServletResponse.writeFilterErrorResponse(
    objectMapper: ObjectMapper,
    errorType: ErrorType,
    message: String,
) {
    status = errorType.status.value()
    contentType = MediaType.APPLICATION_JSON_VALUE
    characterEncoding = "UTF-8"

    val errorResponse = ApiResponse.fail(errorType.code, message)
    writer.write(objectMapper.writeValueAsString(errorResponse))
}
