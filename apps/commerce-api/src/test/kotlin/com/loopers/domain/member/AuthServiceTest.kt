package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var authService: AuthService

    @DisplayName("인증할 때,")
    @Nested
    inner class Authenticate {

        @DisplayName("로그인 ID와 비밀번호가 일치하면, 회원 정보가 반환된다.")
        @Test
        fun returnsMember_whenCredentialsMatch() {
            // arrange
            val loginId = "testuser1"
            val password = "Password1!"
            val member = Member(
                loginId = loginId,
                password = "encodedPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(memberRepository.findByLoginId(loginId)).thenReturn(member)
            whenever(passwordEncoder.matches(password, member.password)).thenReturn(true)

            // act
            val result = authService.authenticate(loginId, password)

            // assert
            assertThat(result.loginId).isEqualTo(loginId)
        }

        @DisplayName("로그인 ID가 존재하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsException_whenLoginIdNotFound() {
            // arrange
            val loginId = "nonexistent"
            val password = "Password1!"

            whenever(memberRepository.findByLoginId(loginId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                authService.authenticate(loginId, password)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsException_whenPasswordNotMatch() {
            // arrange
            val loginId = "testuser1"
            val password = "WrongPassword!"
            val member = Member(
                loginId = loginId,
                password = "encodedPassword",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            whenever(memberRepository.findByLoginId(loginId)).thenReturn(member)
            whenever(passwordEncoder.matches(password, member.password)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                authService.authenticate(loginId, password)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
