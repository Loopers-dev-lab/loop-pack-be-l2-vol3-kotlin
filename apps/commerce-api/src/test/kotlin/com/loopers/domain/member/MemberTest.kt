package com.loopers.domain.member

import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.domain.member.vo.Password
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class MemberTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_정보로_회원을_생성할_수_있다`() {
            // arrange
            val loginId = LoginId("testuser123")
            val password = Password.of("Password1!", LocalDate.of(1990, 1, 15))
            val name = Name("홍길동")
            val birthDate = BirthDate(LocalDate.of(1990, 1, 15))
            val email = Email("test@example.com")

            // act
            val member = Member(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )

            // assert
            assertAll(
                { assertThat(member.loginId).isEqualTo(loginId) },
                { assertThat(member.password).isEqualTo(password) },
                { assertThat(member.name).isEqualTo(name) },
                { assertThat(member.birthDate).isEqualTo(birthDate) },
                { assertThat(member.email).isEqualTo(email) },
            )
        }
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `새_비밀번호로_변경할_수_있다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val member = createMember(birthDate = birthDate)
            val newRawPassword = "NewPassword1!"

            // act
            member.changePassword(
                currentRawPassword = "Password1!",
                newRawPassword = newRawPassword,
            )

            // assert
            assertThat(member.password.matches(newRawPassword)).isTrue()
        }

        @Test
        fun `현재_비밀번호가_일치하지_않으면_예외가_발생한다`() {
            // arrange
            val member = createMember()

            // act
            val result = assertThrows<CoreException> {
                member.changePassword(
                    currentRawPassword = "WrongPassword1!",
                    newRawPassword = "NewPassword1!",
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.AUTHENTICATION_FAILED)
        }

        @Test
        fun `현재_비밀번호와_동일하면_예외가_발생한다`() {
            // arrange
            val member = createMember()
            val samePassword = "Password1!"

            // act
            val result = assertThrows<CoreException> {
                member.changePassword(
                    currentRawPassword = samePassword,
                    newRawPassword = samePassword,
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.SAME_PASSWORD_NOT_ALLOWED)
        }

        @Test
        fun `생년월일이_포함된_비밀번호로_변경할_수_없다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val member = createMember(birthDate = birthDate)
            val newPassword = "Pass19900115!" // 생년월일 포함

            // act
            val result = assertThrows<CoreException> {
                member.changePassword(
                    currentRawPassword = "Password1!",
                    newRawPassword = newPassword,
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.PASSWORD_CONTAINS_BIRTHDATE)
        }
    }

    @Nested
    inner class Authentication {
        @Test
        fun `비밀번호가_일치하면_true를_반환한다`() {
            // arrange
            val member = createMember()

            // act
            val result = member.authenticate("Password1!")

            // assert
            assertThat(result).isTrue()
        }

        @Test
        fun `비밀번호가_일치하지_않으면_false를_반환한다`() {
            // arrange
            val member = createMember()

            // act
            val result = member.authenticate("WrongPassword1!")

            // assert
            assertThat(result).isFalse()
        }
    }

    private fun createMember(
        loginId: String = "testuser123",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 15),
        email: String = "test@example.com",
    ): Member {
        return Member(
            loginId = LoginId(loginId),
            password = Password.of(rawPassword, birthDate),
            name = Name(name),
            birthDate = BirthDate(birthDate),
            email = Email(email),
        )
    }
}
