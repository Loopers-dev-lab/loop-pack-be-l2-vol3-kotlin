package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginIdTest {
    @DisplayName("로그인 아이디를 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("유효한 로그인 아이디로 생성하면 LoginId 객체를 반환한다")
        @Test
        fun createsLoginId_whenValidLoginIdIsProvided() {
            // arrange
            val validLoginId = "test123"

            // act
            val loginId = LoginId.of(validLoginId)

            // assert
            assertThat(loginId.value).isEqualTo(validLoginId)
        }

        @DisplayName("로그인 아이디가 비어있으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdIsBlank() {
            // arrange
            val blankLoginId = ""

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(blankLoginId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디가 4자 미만이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdIsShorterThanFourCharacters() {
            // arrange
            val shortLoginId = "abc"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(shortLoginId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디가 20자 초과이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdIsLongerThanTwentyCharacters() {
            // arrange
            val longLoginId = "abcdefghijklmnopqrstu"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(longLoginId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디에 특수문자가 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsSpecialCharacter() {
            // arrange
            val loginIdWithSpecial = "test@123"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(loginIdWithSpecial)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디에 공백이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsSpace() {
            // arrange
            val loginIdWithSpace = "test 123"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(loginIdWithSpace)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디에 한글이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsKorean() {
            // arrange
            val loginIdWithKorean = "테스트123"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(loginIdWithKorean)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디에 영문 대소문자, 숫자만 포함되면 성공한다")
        @Test
        fun createsLoginId_whenOnlyContainsValidCharacters() {
            // arrange
            val validLoginId = "AbcDefgh1234"

            // act
            val loginId = LoginId.of(validLoginId)

            // assert
            assertThat(loginId.value).isEqualTo(validLoginId)
        }

        @DisplayName("로그인 아이디가 4자이면 성공한다")
        @Test
        fun createsLoginId_whenLoginIdIsFourCharacters() {
            // arrange
            val validLoginId = "abcd"

            // act
            val loginId = LoginId.of(validLoginId)

            // assert
            assertThat(loginId.value).isEqualTo(validLoginId)
        }

        @DisplayName("로그인 아이디가 20자이면 성공한다")
        @Test
        fun createsLoginId_whenLoginIdIsTwentyCharacters() {
            // arrange
            val validLoginId = "abcdefghij0123456789"

            // act
            val loginId = LoginId.of(validLoginId)

            // assert
            assertThat(loginId.value).isEqualTo(validLoginId)
        }

        @DisplayName("로그인 아이디에 언더스코어가 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsUnderscore() {
            // arrange
            val loginIdWithUnderscore = "test_123"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(loginIdWithUnderscore)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 아이디에 하이픈이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsHyphen() {
            // arrange
            val loginIdWithHyphen = "test-123"

            // act
            val result = assertThrows<CoreException> {
                LoginId.of(loginIdWithHyphen)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
