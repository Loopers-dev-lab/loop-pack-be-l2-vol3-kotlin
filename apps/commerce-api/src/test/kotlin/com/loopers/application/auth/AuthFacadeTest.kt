package com.loopers.application.auth

import com.loopers.domain.member.PasswordPolicy
import com.loopers.infrastructure.member.MemberEntity
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
class AuthFacadeTest @Autowired constructor(
    private val authFacade: AuthFacade,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordPolicy: PasswordPolicy,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Signup {
        @Test
        fun `회원가입을_처리할_수_있다`() {
            // arrange
            val command = AuthFacade.SignupCommand(
                loginId = "newuser123",
                rawPassword = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val result = authFacade.signup(command)

            // assert
            assertAll(
                { assertThat(result.id).isGreaterThan(0) },
                { assertThat(result.loginId).isEqualTo("newuser123") },
                { assertThat(result.name).isEqualTo("홍길동") },
                { assertThat(result.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        fun `중복된_로그인ID로_가입하면_예외가_발생한다`() {
            // arrange
            createAndSaveMemberEntity(loginId = "existinguser")
            val command = AuthFacade.SignupCommand(
                loginId = "existinguser",
                rawPassword = "Password1!",
                name = "새회원",
                birthDate = LocalDate.of(1995, 5, 20),
                email = "new@example.com",
            )

            // act
            val result = assertThrows<CoreException> { authFacade.signup(command) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.DUPLICATE_LOGIN_ID)
        }
    }

    @Nested
    inner class Authenticate {
        @Test
        fun `인증_정보를_검증할_수_있다`() {
            // arrange
            createAndSaveMemberEntity(loginId = "authuser", rawPassword = "Password1!")

            // act
            val member = authFacade.authenticate(
                loginId = "authuser",
                rawPassword = "Password1!",
            )

            // assert
            assertThat(member.loginId.value).isEqualTo("authuser")
        }

        @Test
        fun `잘못된_비밀번호면_예외가_발생한다`() {
            // arrange
            createAndSaveMemberEntity(loginId = "authuser2", rawPassword = "Password1!")

            // act
            val result = assertThrows<CoreException> {
                authFacade.authenticate(
                    loginId = "authuser2",
                    rawPassword = "WrongPassword1!",
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.AUTHENTICATION_FAILED)
        }
    }

    private fun createAndSaveMemberEntity(
        loginId: String = "testuser123",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 15),
        email: String = "test@example.com",
    ): MemberEntity {
        return memberJpaRepository.save(
            MemberEntity(
                loginId = loginId,
                password = passwordPolicy.createPassword(rawPassword, birthDate).value,
                name = name,
                birthDate = birthDate,
                email = email,
            ),
        )
    }
}
