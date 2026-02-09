package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

/**
 * Member 엔티티 단위 테스트
 * - 엔티티 생성 시 기본 검증 로직 테스트
 * - 비밀번호는 이미 암호화된 상태로 전달받는다고 가정
 */
class MemberTest {

    @DisplayName("회원을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("정상적인 정보가 주어지면, 회원이 생성된다.")
        @Test
        fun createsMember_whenValidInfoProvided() {
            // arrange
            val loginId = "testuser1"
            val encodedPassword = "encodedPassword123"
            val name = "홍길동"
            val birthDate = LocalDate.of(1990, 1, 15)
            val email = "test@example.com"

            // act
            val member = Member(
                loginId = loginId,
                password = encodedPassword,
                name = name,
                birthDate = birthDate,
                email = email,
            )

            // assert
            assertAll(
                { assertThat(member.loginId).isEqualTo(loginId) },
                { assertThat(member.password).isEqualTo(encodedPassword) },
                { assertThat(member.name).isEqualTo(name) },
                { assertThat(member.birthDate).isEqualTo(birthDate) },
                { assertThat(member.email).isEqualTo(email) },
            )
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenLoginIdContainsSpecialCharacter() {
            // arrange
            val invalidLoginId = "test@user"

            // act
            val exception = assertThrows<CoreException> {
                Member(
                    loginId = invalidLoginId,
                    password = "encodedPassword123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 ID가 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenLoginIdIsBlank() {
            // arrange
            val blankLoginId = "  "

            // act
            val exception = assertThrows<CoreException> {
                Member(
                    loginId = blankLoginId,
                    password = "encodedPassword123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 ID가 10자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenLoginIdExceeds10Characters() {
            // arrange
            val longLoginId = "abcdefghijk" // 11자

            // act
            val exception = assertThrows<CoreException> {
                Member(
                    loginId = longLoginId,
                    password = "encodedPassword123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenEmailFormatInvalid() {
            // arrange
            val invalidEmail = "invalid-email"

            // act
            val exception = assertThrows<CoreException> {
                Member(
                    loginId = "testuser1",
                    password = "encodedPassword123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = invalidEmail,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameIsBlank() {
            // arrange
            val blankName = "  "

            // act
            val exception = assertThrows<CoreException> {
                Member(
                    loginId = "testuser1",
                    password = "encodedPassword123",
                    name = blankName,
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("이름을 마스킹할 때,")
    @Nested
    inner class MaskName {

        @DisplayName("이름의 마지막 글자가 *로 마스킹된다.")
        @Test
        fun masksLastCharacter() {
            // arrange
            val member = Member(
                loginId = "testuser1",
                password = "encodedPassword123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val maskedName = member.getMaskedName()

            // assert
            assertThat(maskedName).isEqualTo("홍길*")
        }

        @DisplayName("이름이 한 글자면, 전체가 *로 마스킹된다.")
        @Test
        fun masksSingleCharacterName() {
            // arrange
            val member = Member(
                loginId = "testuser1",
                password = "encodedPassword123",
                name = "홍",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val maskedName = member.getMaskedName()

            // assert
            assertThat(maskedName).isEqualTo("*")
        }
    }
}
