package com.loopers.interfaces.api

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
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
    fun handle(e: CoreException): ResponseEntity<ApiResponse<*>> {
        log.warn("CoreException : {}", e.customMessage ?: e.message, e)
        return failureResponse(errorType = e.errorType, errorMessage = e.customMessage)
    }

    @ExceptionHandler
    fun handleBadRequest(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<*>> {
        val errors = e.bindingResult.fieldErrors
            .groupBy { it.field }
            .mapValues { (_, fieldErrors) -> fieldErrors.map { it.defaultMessage ?: "유효하지 않은 값입니다." } }
        return ResponseEntity(
            ApiResponse.fail(
                errorCode = ErrorType.BAD_REQUEST.code,
                errorMessage = "입력값이 올바르지 않습니다.",
                errors = errors,
            ),
            ErrorType.BAD_REQUEST.status,
        )
    }

    @ExceptionHandler
    fun handleBadRequest(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<*>> {
        val name = e.name
        val type = e.requiredType?.simpleName ?: "unknown"
        val value = e.value ?: "null"
        val message = "요청 파라미터 '$name' (타입: $type)의 값 '$value'이(가) 잘못되었습니다."
        return failureResponse(errorType = ErrorType.BAD_REQUEST, errorMessage = message)
    }

    @ExceptionHandler
    fun handleBadRequest(e: MissingRequestHeaderException): ResponseEntity<ApiResponse<*>> {
        val name = e.headerName
        val message = "필수 요청 헤더 '$name'가 누락되었습니다."
        return failureResponse(errorType = ErrorType.BAD_REQUEST, errorMessage = message)
    }

    @ExceptionHandler
    fun handleBadRequest(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<*>> {
        val name = e.parameterName
        val type = e.parameterType
        val message = "필수 요청 파라미터 '$name' (타입: $type)가 누락되었습니다."
        return failureResponse(errorType = ErrorType.BAD_REQUEST, errorMessage = message)
    }

    @ExceptionHandler
    fun handleBadRequest(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<*>> {
        return when (val rootCause = e.rootCause) {
            is InvalidFormatException -> {
                val fieldName = rootCause.path.joinToString(".") { it.fieldName ?: "?" }
                val valueIndicationMessage = when {
                    rootCause.targetType.isEnum -> {
                        val enumValues = rootCause.targetType.enumConstants.joinToString(", ") { it.toString() }
                        " 사용 가능한 값 : [$enumValues]"
                    }
                    else -> ""
                }
                val expectedType = rootCause.targetType.simpleName
                val value = rootCause.value
                val message = "예상 타입($expectedType)과 일치하지 않습니다.$valueIndicationMessage"
                val errors = mapOf(fieldName to listOf("값 '$value'이(가) $message"))
                ResponseEntity(
                    ApiResponse.fail("BAD_REQUEST", "입력값이 올바르지 않습니다.", errors),
                    ErrorType.BAD_REQUEST.status,
                )
            }

            is MismatchedInputException -> {
                val fieldPath = rootCause.path.joinToString(".") { it.fieldName ?: "?" }
                val errors = mapOf(fieldPath to listOf("필수 필드가 누락되었습니다."))
                ResponseEntity(
                    ApiResponse.fail("BAD_REQUEST", "입력값이 올바르지 않습니다.", errors),
                    ErrorType.BAD_REQUEST.status,
                )
            }

            is JsonMappingException -> {
                val fieldPath = rootCause.path.joinToString(".") { it.fieldName ?: "?" }
                val errors = mapOf(fieldPath to listOf("JSON 매핑 오류가 발생했습니다."))
                ResponseEntity(
                    ApiResponse.fail("BAD_REQUEST", "입력값이 올바르지 않습니다.", errors),
                    ErrorType.BAD_REQUEST.status,
                )
            }

            else -> failureResponse(
                errorType = ErrorType.BAD_REQUEST,
                errorMessage = "요청 본문을 처리하는 중 오류가 발생했습니다. JSON 메세지 규격을 확인해주세요.",
            )
        }
    }

    @ExceptionHandler
    fun handleBadRequest(e: ServerWebInputException): ResponseEntity<ApiResponse<*>> {
        fun extractMissingParameter(message: String): String {
            val regex = "'(.+?)'".toRegex()
            return regex.find(message)?.groupValues?.get(1) ?: ""
        }

        val missingParams = extractMissingParameter(e.reason ?: "")
        return if (missingParams.isNotEmpty()) {
            failureResponse(errorType = ErrorType.BAD_REQUEST, errorMessage = "필수 요청 값 \'$missingParams\'가 누락되었습니다.")
        } else {
            failureResponse(errorType = ErrorType.BAD_REQUEST)
        }
    }

    @ExceptionHandler
    fun handleNotFound(e: NoResourceFoundException): ResponseEntity<ApiResponse<*>> {
        return failureResponse(errorType = ErrorType.NOT_FOUND)
    }

    @ExceptionHandler
    fun handle(e: Throwable): ResponseEntity<ApiResponse<*>> {
        log.error("Exception : {}", e.message, e)
        val errorType = ErrorType.INTERNAL_ERROR
        return failureResponse(errorType = errorType)
    }

    private fun failureResponse(errorType: ErrorType, errorMessage: String? = null): ResponseEntity<ApiResponse<*>> =
        ResponseEntity(
            ApiResponse.fail(errorCode = errorType.code, errorMessage = errorMessage ?: errorType.message),
            errorType.status,
        )
}
