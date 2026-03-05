package com.loopers.interfaces.api

import com.fasterxml.jackson.databind.JsonMappingException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.converter.HttpMessageNotReadableException

@DisplayName("ApiControllerAdvice 단위 테스트")
class ApiControllerAdviceTest {
    private val sut = ApiControllerAdvice()

    @Nested
    @DisplayName("JsonMappingException 처리 시 내부 메시지를 노출하지 않는다")
    inner class WhenJsonMappingException {
        @Test
        @DisplayName("originalMessage가 응답에 포함되지 않는다")
        fun handleBadRequest_jsonMappingException_hidesOriginalMessage() {
            // arrange
            val jsonMappingException = JsonMappingException(null as com.fasterxml.jackson.core.JsonParser?, "Internal detail message")
            jsonMappingException.prependPath(Any(), "fieldName")
            val exception = HttpMessageNotReadableException(
                "Could not read JSON",
                jsonMappingException,
                null,
            )

            // act
            val response = sut.handleBadRequest(exception)

            // assert
            val body = response.body as ApiResponse<*>
            assertThat(body.meta.message).doesNotContain("Internal detail message")
            assertThat(body.meta.message).isEqualTo("입력값이 올바르지 않습니다.")
            assertThat(body.meta.errors).containsKey("fieldName")
            assertThat(body.meta.errors?.get("fieldName")).contains("JSON 매핑 오류가 발생했습니다.")
        }
    }
}
