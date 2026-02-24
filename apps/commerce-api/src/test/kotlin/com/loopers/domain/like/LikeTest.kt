package com.loopers.domain.like

import com.loopers.domain.like.model.Like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LikeTest {

    @Nested
    @DisplayName("Like 생성 시")
    inner class Create {

        @Test
        @DisplayName("정상 입력이면 생성에 성공한다")
        fun create_validInput_success() {
            // act
            val like = Like(refUserId = 1L, refProductId = 1L)

            // assert
            assertThat(like.refUserId).isEqualTo(1L)
            assertThat(like.refProductId).isEqualTo(1L)
        }

        @Test
        @DisplayName("refUserId가 0이면 CoreException이 발생한다")
        fun create_zeroUserId_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Like(refUserId = 0L, refProductId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("refUserId가 음수이면 CoreException이 발생한다")
        fun create_negativeUserId_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Like(refUserId = -1L, refProductId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("refProductId가 0이면 CoreException이 발생한다")
        fun create_zeroProductId_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Like(refUserId = 1L, refProductId = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("refProductId가 음수이면 CoreException이 발생한다")
        fun create_negativeProductId_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Like(refUserId = 1L, refProductId = -1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
