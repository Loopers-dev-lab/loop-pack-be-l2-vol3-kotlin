package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MemberService")
class MemberServiceTest {

    private val memberRepository: MemberRepository = mockk()
    private val memberService = MemberService(memberRepository)

    companion object {
        private const val VALID_LOGIN_ID = "test_user1"
        private const val VALID_ENCODED_PASSWORD = "\$2a\$10\$encodedPasswordHash"
        private const val VALID_NAME = "홍길동"
        private const val VALID_EMAIL = "test@example.com"
        private val VALID_BIRTH_DATE = LocalDate.of(1990, 5, 15)
    }

    @DisplayName("signUp")
    @Nested
    inner class SignUp {
        @DisplayName("모든 필드가 유효하면 회원이 저장된다")
        @Test
        fun savesMember_whenAllFieldsAreValid() {
            // arrange
            every { memberRepository.existsByLoginId(VALID_LOGIN_ID) } returns false
            every { memberRepository.save(any()) } answers { firstArg() }

            // act
            val result = memberService.signUp(
                loginId = VALID_LOGIN_ID,
                encodedPassword = VALID_ENCODED_PASSWORD,
                name = VALID_NAME,
                birthDate = VALID_BIRTH_DATE,
                email = VALID_EMAIL,
            )

            // assert
            assertThat(result.loginId).isEqualTo(VALID_LOGIN_ID)
            assertThat(result.password).isEqualTo(VALID_ENCODED_PASSWORD)
            assertThat(result.name).isEqualTo(VALID_NAME)
            assertThat(result.email).isEqualTo(VALID_EMAIL)
            assertThat(result.birthDate).isEqualTo(VALID_BIRTH_DATE)

            verify(exactly = 1) { memberRepository.existsByLoginId(VALID_LOGIN_ID) }
            verify(exactly = 1) { memberRepository.save(any()) }
        }

        @DisplayName("이미 존재하는 로그인 ID면 CONFLICT 예외가 발생한다")
        @Test
        fun throwsConflictException_whenLoginIdAlreadyExists() {
            // arrange
            every { memberRepository.existsByLoginId(VALID_LOGIN_ID) } returns true

            // act & assert
            assertThatThrownBy {
                memberService.signUp(
                    loginId = VALID_LOGIN_ID,
                    encodedPassword = VALID_ENCODED_PASSWORD,
                    name = VALID_NAME,
                    birthDate = VALID_BIRTH_DATE,
                    email = VALID_EMAIL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)

            verify(exactly = 1) { memberRepository.existsByLoginId(VALID_LOGIN_ID) }
            verify(exactly = 0) { memberRepository.save(any()) }
        }
    }
}
