package com.loopers.interfaces.api

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("ApiControllerAdvice - ErrorType 매핑 테스트")
class ApiControllerAdviceTest {

    private val advice = ApiControllerAdvice()

    @DisplayName("QUEUE_NOT_FOUND 에러 매핑")
    @Test
    fun `QUEUE_NOT_FOUND는 404 NOT_FOUND로 매핑된다`() {
        // arrange
        val exception = CoreException(ErrorType.QUEUE_NOT_FOUND, "test queue not found")

        // act
        val response = advice.handle(exception)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Queue Not Found")
    }

    @DisplayName("QUEUE_USER_NOT_REGISTERED 에러 매핑")
    @Test
    fun `QUEUE_USER_NOT_REGISTERED는 404 NOT_FOUND로 매핑된다`() {
        // arrange
        val exception = CoreException(ErrorType.QUEUE_USER_NOT_REGISTERED, "user not in queue")

        // act
        val response = advice.handle(exception)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Queue User Not Registered")
    }

    @DisplayName("ENTRY_TOKEN_MISSING 에러 매핑")
    @Test
    fun `ENTRY_TOKEN_MISSING은 403 FORBIDDEN으로 매핑된다`() {
        // arrange
        val exception = CoreException(ErrorType.ENTRY_TOKEN_MISSING, "token missing")

        // act
        val response = advice.handle(exception)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Entry Token Missing")
    }

    @DisplayName("ENTRY_TOKEN_INVALID 에러 매핑")
    @Test
    fun `ENTRY_TOKEN_INVALID는 403 FORBIDDEN으로 매핑된다`() {
        // arrange
        val exception = CoreException(ErrorType.ENTRY_TOKEN_INVALID, "invalid token")

        // act
        val response = advice.handle(exception)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL)
        assertThat(response.body?.meta?.errorCode).isEqualTo("Entry Token Invalid")
    }
}
