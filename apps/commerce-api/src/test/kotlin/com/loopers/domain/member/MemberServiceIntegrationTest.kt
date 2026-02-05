package com.loopers.domain.member

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
class MemberServiceIntegrationTest @Autowired constructor(
    private val memberService: MemberService,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Register {
        @Test
        fun `신규_회원을_등록할_수_있다`() {
            // arrange
            val loginId = LoginId("newuser123")
            val birthDate = LocalDate.of(1990, 1, 15)
            val password = Password.of("Password1!", birthDate)
            val name = Name("홍길동")
            val email = Email("test@example.com")

            // act
            val member = memberService.register(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = BirthDate(birthDate),
                email = email,
            )

            // assert
            assertAll(
                { assertThat(member.id).isGreaterThan(0) },
                { assertThat(member.loginId).isEqualTo(loginId) },
                { assertThat(member.name).isEqualTo(name) },
            )
        }

        @Test
        fun `이미_존재하는_로그인ID면_예외가_발생한다`() {
            // arrange
            val loginId = LoginId("existinguser")
            val birthDate = LocalDate.of(1990, 1, 15)
            val existingMember = createAndSaveMember(loginId = "existinguser")

            // act
            val result = assertThrows<CoreException> {
                memberService.register(
                    loginId = loginId,
                    password = Password.of("Password1!", birthDate),
                    name = Name("새회원"),
                    birthDate = BirthDate(birthDate),
                    email = Email("new@example.com"),
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.DUPLICATE_LOGIN_ID)
        }
    }

    @Nested
    inner class FindMember {
        @Test
        fun `로그인ID로_회원을_조회할_수_있다`() {
            // arrange
            val savedMember = createAndSaveMember(loginId = "testuser123")

            // act
            val member = memberService.getMemberByLoginId(LoginId("testuser123"))

            // assert
            assertThat(member.id).isEqualTo(savedMember.id)
        }

        @Test
        fun `존재하지_않는_회원이면_예외가_발생한다`() {
            // arrange
            val nonExistingLoginId = LoginId("nonexisting")

            // act
            val result = assertThrows<CoreException> {
                memberService.getMemberByLoginId(nonExistingLoginId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.MEMBER_NOT_FOUND)
        }
    }

    @Nested
    inner class Authenticate {
        @Test
        fun `로그인ID와_비밀번호가_일치하면_회원을_반환한다`() {
            // arrange
            val rawPassword = "Password1!"
            val savedMember = createAndSaveMember(loginId = "authuser", rawPassword = rawPassword)

            // act
            val member = memberService.authenticate(
                loginId = LoginId("authuser"),
                rawPassword = rawPassword,
            )

            // assert
            assertThat(member.id).isEqualTo(savedMember.id)
        }

        @Test
        fun `비밀번호가_일치하지_않으면_예외가_발생한다`() {
            // arrange
            createAndSaveMember(loginId = "authuser2", rawPassword = "Password1!")

            // act
            val result = assertThrows<CoreException> {
                memberService.authenticate(
                    loginId = LoginId("authuser2"),
                    rawPassword = "WrongPassword1!",
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.AUTHENTICATION_FAILED)
        }
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `비밀번호를_변경할_수_있다`() {
            // arrange
            val rawPassword = "Password1!"
            val newPassword = "NewPassword1!"
            val savedMember = createAndSaveMember(loginId = "pwchangeuser", rawPassword = rawPassword)

            // act
            memberService.changePassword(
                loginId = LoginId("pwchangeuser"),
                currentRawPassword = rawPassword,
                newRawPassword = newPassword,
            )

            // assert
            val updatedMember = memberJpaRepository.findByLoginId(LoginId("pwchangeuser"))
            assertThat(updatedMember?.password?.matches(newPassword)).isTrue()
        }
    }

    private fun createAndSaveMember(
        loginId: String = "testuser123",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 15),
        email: String = "test@example.com",
    ): MemberModel {
        return memberJpaRepository.save(
            MemberModel(
                loginId = LoginId(loginId),
                password = Password.of(rawPassword, birthDate),
                name = Name(name),
                birthDate = BirthDate(birthDate),
                email = Email(email),
            ),
        )
    }
}
