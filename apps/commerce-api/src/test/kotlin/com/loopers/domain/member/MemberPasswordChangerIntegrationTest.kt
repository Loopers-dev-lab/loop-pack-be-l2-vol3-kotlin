package com.loopers.domain.member

import com.loopers.domain.member.vo.Password
import com.loopers.infrastructure.member.MemberEntity
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class MemberPasswordChangerIntegrationTest @Autowired constructor(
    private val memberPasswordChanger: MemberPasswordChanger,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `비밀번호를_변경할_수_있다`() {
            // arrange
            val rawPassword = "Password1!"
            val newPassword = "NewPassword1!"
            createAndSaveMemberEntity(loginId = "pwchangeuser", rawPassword = rawPassword)

            // act
            memberPasswordChanger.changePassword(
                loginId = "pwchangeuser",
                currentRawPassword = rawPassword,
                newRawPassword = newPassword,
            )

            // assert
            val updatedEntity = memberJpaRepository.findByLoginId("pwchangeuser")
            val updatedPassword = Password.fromEncoded(updatedEntity!!.password)
            assertThat(updatedPassword.matches(newPassword, passwordEncoder)).isTrue()
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
                password = Password.of(rawPassword, birthDate, passwordEncoder).value,
                name = name,
                birthDate = birthDate,
                email = email,
            ),
        )
    }
}
