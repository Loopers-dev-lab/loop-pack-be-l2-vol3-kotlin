package com.loopers.domain.member

import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Password
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
class MemberReaderIntegrationTest @Autowired constructor(
    private val memberReader: MemberReader,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class GetByLoginId {
        @Test
        fun `로그인ID로_회원을_조회할_수_있다`() {
            // arrange
            val savedEntity = createAndSaveMemberEntity(loginId = "testuser123")

            // act
            val member = memberReader.getByLoginId(LoginId("testuser123"))

            // assert
            assertThat(member.id).isEqualTo(savedEntity.id)
        }

        @Test
        fun `존재하지_않는_회원이면_예외가_발생한다`() {
            // arrange
            val nonExistingLoginId = LoginId("nonexisting")

            // act
            val result = assertThrows<CoreException> {
                memberReader.getByLoginId(nonExistingLoginId)
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
                password = Password.of(rawPassword, birthDate).value,
                name = name,
                birthDate = birthDate,
                email = email,
            ),
        )
    }
}
