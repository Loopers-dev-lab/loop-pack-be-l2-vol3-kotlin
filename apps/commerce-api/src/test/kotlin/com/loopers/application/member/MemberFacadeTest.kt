package com.loopers.application.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberService
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("MemberFacade")
class MemberFacadeTest {

    private val memberService: MemberService = mockk()
    private val passwordEncoder: BCryptPasswordEncoder = mockk()
    private val memberFacade = MemberFacade(memberService, passwordEncoder)

    companion object {
        private const val MEMBER_ID = 1L
        private const val CURRENT_PASSWORD = "OldPass1!"
        private const val NEW_PASSWORD = "NewPass1!"
        private const val ENCODED_PASSWORD = "\$2a\$10\$encodedPasswordHash"
        private const val NEW_ENCODED_PASSWORD = "\$2a\$10\$newEncodedPasswordHash"
        private const val VALID_LOGIN_ID = "test_user1"
        private const val VALID_NAME = "홍길동"
        private const val VALID_EMAIL = "test@example.com"
        private val VALID_BIRTH_DATE = LocalDate.of(1990, 5, 15)
    }

    private fun createMember(): MemberModel {
        return MemberModel(
            loginId = VALID_LOGIN_ID,
            password = ENCODED_PASSWORD,
            name = VALID_NAME,
            birthDate = VALID_BIRTH_DATE,
            email = VALID_EMAIL,
        )
    }

    @DisplayName("changePassword")
    @Nested
    inner class ChangePassword {
        @DisplayName("현재 비밀번호가 일치하고 유효한 새 비밀번호면 비밀번호가 변경된다")
        @Test
        fun changesPassword_whenCurrentPasswordMatchesAndNewPasswordIsValid() {
            // arrange
            val member = createMember()
            every { memberService.findById(MEMBER_ID) } returns member
            every { passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD) } returns true
            every { passwordEncoder.encode(NEW_PASSWORD) } returns NEW_ENCODED_PASSWORD
            every { memberService.changePassword(MEMBER_ID, NEW_ENCODED_PASSWORD) } returns member

            // act
            memberFacade.changePassword(MEMBER_ID, CURRENT_PASSWORD, NEW_PASSWORD)

            // assert
            verify(exactly = 1) { memberService.findById(MEMBER_ID) }
            verify(exactly = 1) { passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD) }
            verify(exactly = 1) { passwordEncoder.encode(NEW_PASSWORD) }
            verify(exactly = 1) { memberService.changePassword(MEMBER_ID, NEW_ENCODED_PASSWORD) }
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면 UNAUTHORIZED 예외가 발생한다")
        @Test
        fun throwsUnauthorized_whenCurrentPasswordDoesNotMatch() {
            // arrange
            val member = createMember()
            every { memberService.findById(MEMBER_ID) } returns member
            every { passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD) } returns false

            // act & assert
            assertThatThrownBy {
                memberFacade.changePassword(MEMBER_ID, CURRENT_PASSWORD, NEW_PASSWORD)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.UNAUTHORIZED)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.")

            verify(exactly = 1) { memberService.findById(MEMBER_ID) }
            verify(exactly = 1) { passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD) }
            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { memberService.changePassword(any(), any()) }
        }

        @DisplayName("현재 비밀번호와 새 비밀번호가 동일하면 BAD_REQUEST 예외가 발생한다")
        @Test
        fun throwsBadRequest_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val member = createMember()
            every { memberService.findById(MEMBER_ID) } returns member
            every { passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_PASSWORD) } returns true

            // act & assert
            assertThatThrownBy {
                memberFacade.changePassword(MEMBER_ID, CURRENT_PASSWORD, CURRENT_PASSWORD)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessage("현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")

            verify(exactly = 0) { passwordEncoder.encode(any()) }
            verify(exactly = 0) { memberService.changePassword(any(), any()) }
        }
    }
}
