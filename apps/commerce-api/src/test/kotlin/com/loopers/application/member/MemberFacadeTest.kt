package com.loopers.application.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.domain.member.vo.Password
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class MemberFacadeTest @Autowired constructor(
    private val memberFacade: MemberFacade,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class GetMyProfile {
        @Test
        fun `내_정보를_조회할_수_있다`() {
            // arrange
            createAndSaveMember(loginId = "myuser", name = "홍길동")

            // act
            val result = memberFacade.getMyProfile(loginId = "myuser")

            // assert
            assertAll(
                { assertThat(result.loginId).isEqualTo("myuser") },
                // 마스킹 확인
                { assertThat(result.name).isEqualTo("홍길*") },
                { assertThat(result.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        fun `이름의_마지막_글자가_마스킹되어_반환된다`() {
            // arrange
            createAndSaveMember(loginId = "maskuser", name = "김철수")

            // act
            val result = memberFacade.getMyProfile(loginId = "maskuser")

            // assert
            assertThat(result.name).isEqualTo("김철*")
        }
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `비밀번호를_수정할_수_있다`() {
            // arrange
            createAndSaveMember(loginId = "pwuser", rawPassword = "OldPassword1!")
            val command = MemberFacade.ChangePasswordCommand(
                loginId = "pwuser",
                currentPassword = "OldPassword1!",
                newPassword = "NewPassword1!",
            )

            // act
            memberFacade.changePassword(command)

            // assert
            val updatedMember = memberJpaRepository.findByLoginId(LoginId("pwuser"))
            assertThat(updatedMember?.password?.matches("NewPassword1!")).isTrue()
        }

        @Test
        fun `현재_비밀번호가_틀리면_예외가_발생한다`() {
            // arrange
            createAndSaveMember(loginId = "pwuser2", rawPassword = "OldPassword1!")
            val command = MemberFacade.ChangePasswordCommand(
                loginId = "pwuser2",
                currentPassword = "WrongPassword1!",
                newPassword = "NewPassword1!",
            )

            // act
            val result = assertThrows<CoreException> { memberFacade.changePassword(command) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.AUTHENTICATION_FAILED)
        }

        @Test
        fun `현재_비밀번호와_동일하면_예외가_발생한다`() {
            // arrange
            val samePassword = "SamePassword1!"
            createAndSaveMember(loginId = "pwuser3", rawPassword = samePassword)
            val command = MemberFacade.ChangePasswordCommand(
                loginId = "pwuser3",
                currentPassword = samePassword,
                newPassword = samePassword,
            )

            // act
            val result = assertThrows<CoreException> { memberFacade.changePassword(command) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.SAME_PASSWORD_NOT_ALLOWED)
        }
    }

    private fun createAndSaveMember(
        loginId: String = "testuser123",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 15),
    ): MemberModel {
        return memberJpaRepository.save(
            MemberModel(
                loginId = LoginId(loginId),
                password = Password.of(rawPassword, birthDate),
                name = Name(name),
                birthDate = BirthDate(birthDate),
                email = Email("test@example.com"),
            ),
        )
    }
}
