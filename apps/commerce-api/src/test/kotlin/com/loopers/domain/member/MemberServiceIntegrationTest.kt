package com.loopers.domain.member

import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

/**
 * MemberService 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Service + Repository 레이어 통합 테스트
 * - 단위 테스트와 달리 Mock 없이 실제 저장/조회 검증
 */
@SpringBootTest
class MemberServiceIntegrationTest @Autowired constructor(
    private val memberService: MemberService,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("회원가입할 때,")
    @Nested
    inner class SignUp {

        @DisplayName("정상적인 정보가 주어지면, 회원이 DB에 저장된다.")
        @Test
        fun savesMemberToDatabase_whenValidInfoProvided() {
            // given
            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // when
            val result = memberService.signUp(command)

            // then
            val savedMember = memberJpaRepository.findByLoginId("testuser1")!!
            assertAll(
                { assertThat(savedMember.id).isEqualTo(result.id) },
                { assertThat(savedMember.loginId).isEqualTo("testuser1") },
                { assertThat(savedMember.name).isEqualTo("홍길동") },
                { assertThat(savedMember.email).isEqualTo("test@example.com") },
            )
        }

        @DisplayName("비밀번호가 암호화되어 저장된다.")
        @Test
        fun savesEncodedPassword_whenSignUp() {
            // given
            val rawPassword = "Password1!"
            val command = SignUpCommand(
                loginId = "testuser1",
                password = rawPassword,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // when
            memberService.signUp(command)

            // then
            val savedMember = memberJpaRepository.findByLoginId("testuser1")!!
            assertAll(
                { assertThat(savedMember.password).isNotEqualTo(rawPassword) },
                { assertThat(passwordEncoder.matches(rawPassword, savedMember.password)).isTrue() },
            )
        }

        @DisplayName("중복된 loginId가 있으면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenDuplicateLoginId() {
            // given
            val existingMember = Member(
                loginId = "testuser1",
                password = "encoded",
                name = "기존회원",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "existing@example.com",
            )
            memberJpaRepository.save(existingMember)

            val command = SignUpCommand(
                loginId = "testuser1",
                password = "Password1!",
                name = "신규회원",
                birthDate = LocalDate.of(1995, 5, 5),
                email = "new@example.com",
            )

            // when & then
            val exception = assertThrows<CoreException> {
                memberService.signUp(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("내 정보 조회할 때,")
    @Nested
    inner class GetMyInfo {

        @DisplayName("존재하는 회원 ID가 주어지면, 회원 정보를 반환한다.")
        @Test
        fun returnsMemberInfo_whenValidMemberId() {
            // given
            val savedMember = memberJpaRepository.save(
                Member(
                    loginId = "testuser1",
                    password = "encoded",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                ),
            )

            // when
            val result = memberService.getMyInfo(savedMember.id)

            // then
            assertAll(
                { assertThat(result.id).isEqualTo(savedMember.id) },
                { assertThat(result.loginId).isEqualTo("testuser1") },
                { assertThat(result.name).isEqualTo("홍길동") },
                { assertThat(result.email).isEqualTo("test@example.com") },
            )
        }

        @DisplayName("존재하지 않는 회원 ID가 주어지면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenMemberNotExists() {
            // given
            val nonExistentId = 9999L

            // when & then
            val exception = assertThrows<CoreException> {
                memberService.getMyInfo(nonExistentId)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
