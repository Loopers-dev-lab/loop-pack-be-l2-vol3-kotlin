package com.loopers.support.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CoreExceptionTest {
    @DisplayName("ErrorCode 기반의 예외 생성 시, 별도의 메시지가 주어지지 않으면 ErrorCode의 메시지를 사용한다.")
    @Test
    fun messageShouldBeErrorCodeMessage_whenCustomMessageIsNull() {
        // arrange
        val errorCodes = CommonErrorCode.entries

        // act
        errorCodes.forEach { errorCode ->
            val exception = CoreException(errorCode)

            // assert
            assertThat(exception.message).isEqualTo(errorCode.message)
        }
    }

    @DisplayName("ErrorCode 기반의 예외 생성 시, 별도의 메시지가 주어지면 해당 메시지를 사용한다.")
    @Test
    fun messageShouldBeCustomMessage_whenCustomMessageIsNotNull() {
        // arrange
        val customMessage = "custom message"

        // act
        val exception = CoreException(CommonErrorCode.INTERNAL_SERVER_ERROR, customMessage)

        // assert
        assertThat(exception.message).isEqualTo(customMessage)
    }
}
