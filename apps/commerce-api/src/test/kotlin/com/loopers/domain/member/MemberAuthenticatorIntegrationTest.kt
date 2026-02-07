package com.loopers.domain.member

import com.loopers.infrastructure.member.MemberEntity
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class MemberAuthenticatorIntegrationTest @Autowired constructor(
    private val memberAuthenticator: MemberAuthenticator,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordPolicy: PasswordPolicy,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Authenticate {
        @Test
        fun `로그인ID와_비밀번호가_일치하면_회원을_반환한다`() {
            // arrange
            val rawPassword = "Password1!"
            val savedEntity = createAndSaveMemberEntity(loginId = "authuser", rawPassword = rawPassword)

            // act
            val member = memberAuthenticator.authenticate(
                loginId = "authuser",
                rawPassword = rawPassword,
            )

            // assert
            assertThat(member.id).isEqualTo(savedEntity.id)
        }

        @Test
        fun `비밀번호가_일치하지_않으면_예외가_발생한다`() {
            // arrange
            createAndSaveMemberEntity(loginId = "authuser2", rawPassword = "Password1!")

            // act
            val result = assertThrows<CoreException> {
                memberAuthenticator.authenticate(
                    loginId = "authuser2",
                    rawPassword = "WrongPassword1!",
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.AUTHENTICATION_FAILED)
        }

        @Test
        fun `존재하지_않는_회원이면_예외가_발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                memberAuthenticator.authenticate(
                    loginId = "nonexisting",
                    rawPassword = "Password1!",
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.MEMBER_NOT_FOUND)
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
