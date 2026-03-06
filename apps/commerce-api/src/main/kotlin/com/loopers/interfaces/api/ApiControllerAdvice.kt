package com.loopers.interfaces.api

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.loopers.application.error.ApplicationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.servlet.resource.NoResourceFoundException
import kotlin.collections.joinToString
import kotlin.jvm.java
import kotlin.text.isNotEmpty
import kotlin.text.toRegex

@RestControllerAdvice
class ApiControllerAdvice {
    private val log = LoggerFactory.getLogger(ApiControllerAdvice::class.java)

    @ExceptionHandler
    fun handle(e: ApplicationException): ResponseEntity<ApiResponse<*>> {
        log.warn("ApplicationException : {}", e.message, e)
        return failureResponse(
            httpStatus = HttpStatus.valueOf(e.httpStatus),
            errorCode = e.code,
            errorMessage = e.message,
        )
    }

    @ExceptionHandler
    fun handleBadRequest(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<*>> {
        val name = e.name
        val type = e.requiredType?.simpleName ?: "unknown"
        val value = e.value ?: "null"
        val message = "요청 파라미터 '$name' (타입: $type)의 값 '$value'이(가) 잘못되었습니다."
        return failureResponse(httpStatus = HttpStatus.BAD_REQUEST, errorCode = "Bad Request", errorMessage = message)
    }

    @ExceptionHandler
    fun handleBadRequest(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<*>> {
        val name = e.parameterName
        val type = e.parameterType
        val message = "필수 요청 파라미터 '$name' (타입: $type)가 누락되었습니다."
        return failureResponse(httpStatus = HttpStatus.BAD_REQUEST, errorCode = "Bad Request", errorMessage = message)
    }

    @ExceptionHandler
    fun handleBadRequest(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<*>> {
        val errorMessage = e.bindingResult.fieldErrors
            .joinToString(", ") { "'${it.field}': ${it.defaultMessage}" }
        return failureResponse(httpStatus = HttpStatus.BAD_REQUEST, errorCode = "Bad Request", errorMessage = errorMessage)
    }

    @ExceptionHandler
    fun handleBadRequest(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<*>> {
        val errorMessage = when (val rootCause = e.rootCause) {
            is InvalidFormatException -> {
                val fieldName = rootCause.path.joinToString(".") { it.fieldName ?: "?" }

                val valueIndicationMessage = when {
                    rootCause.targetType.isEnum -> {
                        val enumClass = rootCause.targetType
                        val enumValues = enumClass.enumConstants.joinToString(", ") { it.toString() }
                        "사용 가능한 값 : [$enumValues]"
                    }

                    else -> ""
                }

                val expectedType = rootCause.targetType.simpleName
                val value = rootCause.value

                "필드 '$fieldName'의 값 '$value'이(가) 예상 타입($expectedType)과 일치하지 않습니다. $valueIndicationMessage"
            }

            is MismatchedInputException -> {
                val fieldPath = rootCause.path.joinToString(".") { it.fieldName ?: "?" }
                "필수 필드 '$fieldPath'이(가) 누락되었습니다."
            }

            is JsonMappingException -> {
                val fieldPath = rootCause.path.joinToString(".") { it.fieldName ?: "?" }
                "필드 '$fieldPath'에서 JSON 매핑 오류가 발생했습니다: ${rootCause.originalMessage}"
            }

            else -> "요청 본문을 처리하는 중 오류가 발생했습니다. JSON 메세지 규격을 확인해주세요."
        }

        return failureResponse(httpStatus = HttpStatus.BAD_REQUEST, errorCode = "Bad Request", errorMessage = errorMessage)
    }

    @ExceptionHandler
    fun handleBadRequest(e: ServerWebInputException): ResponseEntity<ApiResponse<*>> {
        fun extractMissingParameter(message: String): String {
            val regex = "'(.+?)'".toRegex()
            return regex.find(message)?.groupValues?.get(1) ?: ""
        }

        val missingParams = extractMissingParameter(e.reason ?: "")
        return if (missingParams.isNotEmpty()) {
            failureResponse(
                httpStatus = HttpStatus.BAD_REQUEST,
                errorCode = "Bad Request",
                errorMessage = "필수 요청 값 \'$missingParams\'가 누락되었습니다.",
            )
        } else {
            failureResponse(httpStatus = HttpStatus.BAD_REQUEST, errorCode = "Bad Request")
        }
    }

    @ExceptionHandler
    fun handleNotFound(e: NoResourceFoundException): ResponseEntity<ApiResponse<*>> {
        return failureResponse(httpStatus = HttpStatus.NOT_FOUND, errorCode = "Not Found", errorMessage = "존재하지 않는 요청입니다.")
    }

    @ExceptionHandler
    fun handleConflict(e: DataIntegrityViolationException): ResponseEntity<ApiResponse<*>> {
        log.warn("Data integrity violation: {}", e.message)
        return failureResponse(HttpStatus.CONFLICT, "Conflict", "이미 존재하는 리소스입니다.")
    }

    @ExceptionHandler
    fun handleConflict(e: ObjectOptimisticLockingFailureException): ResponseEntity<ApiResponse<*>> {
        log.warn("Optimistic lock conflict: {}", e.message)
        return failureResponse(HttpStatus.CONFLICT, "Conflict", "요청이 충돌했습니다. 다시 시도해 주세요.")
    }

    @ExceptionHandler
    fun handle(e: Throwable): ResponseEntity<ApiResponse<*>> {
        log.error("Exception : {}", e.message, e)
        return failureResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = "Internal Server Error",
            errorMessage = "일시적인 오류가 발생했습니다.",
        )
    }

    private fun failureResponse(
        httpStatus: HttpStatus,
        errorCode: String,
        errorMessage: String? = null,
    ): ResponseEntity<ApiResponse<*>> =
        ResponseEntity(
            ApiResponse.fail(errorCode = errorCode, errorMessage = errorMessage ?: ""),
            httpStatus,
        )
}
