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
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class MemberRegisterIntegrationTest @Autowired constructor(
    private val memberRegister: MemberRegister,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordPolicy: PasswordPolicy,
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
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val member = memberRegister.register(
                loginId = "newuser123",
                rawPassword = "Password1!",
                name = "홍길동",
                birthDate = birthDate,
                email = "test@example.com",
            )

            // assert
            assertAll(
                { assertThat(member.id).isNotNull() },
                { assertThat(member.id).isGreaterThan(0) },
                { assertThat(member.loginId.value).isEqualTo("newuser123") },
                { assertThat(member.name.value).isEqualTo("홍길동") },
            )
        }

        @Test
        fun `이미_존재하는_로그인ID면_예외가_발생한다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            createAndSaveMemberEntity(loginId = "existinguser")

            // act
            val result = assertThrows<CoreException> {
                memberRegister.register(
                    loginId = "existinguser",
                    rawPassword = "Password1!",
                    name = "새회원",
                    birthDate = birthDate,
                    email = "new@example.com",
                )
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.DUPLICATE_LOGIN_ID)
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
