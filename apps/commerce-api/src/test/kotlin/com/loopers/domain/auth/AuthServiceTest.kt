package com.loopers.domain.auth

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AuthService")
class AuthServiceTest {

    private val memberRepository: MemberRepository = mockk()
    private val passwordEncoder: BCryptPasswordEncoder = mockk()
    private val authService = AuthService(memberRepository, passwordEncoder)

    companion object {
        private const val LOGIN_ID = "test_user1"
        private const val RAW_PASSWORD = "Password1!"
        private const val ENCODED_PASSWORD = "\$2a\$10\$encodedPasswordHash"
    }

    @DisplayName("authenticate")
    @Nested
    inner class Authenticate {
        @DisplayName("유효한 자격 증명이면 회원 모델을 반환한다")
        @Test
        fun returnsMember_whenCredentialsAreValid() {
            // arrange
            val member = MemberModel(
                loginId = LOGIN_ID,
                password = ENCODED_PASSWORD,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 5, 15),
                email = "test@example.com",
            )
            every { memberRepository.findByLoginId(LOGIN_ID) } returns member
            every { passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD) } returns true

            // act
            val result = authService.authenticate(LOGIN_ID, RAW_PASSWORD)

            // assert
            assertThat(result.loginId).isEqualTo(LOGIN_ID)
        }

        @DisplayName("존재하지 않는 로그인 ID면 UNAUTHORIZED 예외가 발생한다")
        @Test
        fun throwsUnauthorized_whenLoginIdNotFound() {
            // arrange
            every { memberRepository.findByLoginId(LOGIN_ID) } returns null

            // act & assert
            assertThatThrownBy { authService.authenticate(LOGIN_ID, RAW_PASSWORD) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
        }

        @DisplayName("비밀번호가 일치하지 않으면 UNAUTHORIZED 예외가 발생한다")
        @Test
        fun throwsUnauthorized_whenPasswordDoesNotMatch() {
            // arrange
            val member = MemberModel(
                loginId = LOGIN_ID,
                password = ENCODED_PASSWORD,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 5, 15),
                email = "test@example.com",
            )
            every { memberRepository.findByLoginId(LOGIN_ID) } returns member
            every { passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD) } returns false

            // act & assert
            assertThatThrownBy { authService.authenticate(LOGIN_ID, RAW_PASSWORD) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
        }
    }
}
