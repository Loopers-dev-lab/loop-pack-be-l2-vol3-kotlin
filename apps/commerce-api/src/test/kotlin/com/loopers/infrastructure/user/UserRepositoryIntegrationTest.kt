package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
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

@DisplayName("UserRepository 통합 테스트")
@SpringBootTest
class UserRepositoryIntegrationTest
@Autowired
constructor(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(
        loginId: String = "testuser1",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): User = User.register(
        loginId = loginId,
        rawPassword = rawPassword,
        name = name,
        birthDate = birthDate,
        email = email,
        passwordHasher = passwordHasher,
    )

    @Nested
    @DisplayName("회원 저장 및 조회")
    inner class SaveAndFind {
        @Test
        @DisplayName("회원 저장 및 조회 성공")
        fun save_andFindById_success() {
            // arrange
            val user = createUser()

            // act
            val savedUser = userRepository.save(user)

            // assert
            assertAll(
                { assertThat(savedUser.loginId).isEqualTo("testuser1") },
                { assertThat(savedUser.name).isEqualTo("홍길동") },
                { assertThat(savedUser.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(savedUser.email).isEqualTo("test@example.com") },
                { assertThat(savedUser.password).isNotEqualTo("Password1!") },
            )
        }

        @Test
        @DisplayName("existsByLoginId - 존재하는 loginId - true 반환")
        fun existsByLoginId_existing_returnsTrue() {
            // arrange
            val user = createUser()
            userRepository.save(user)

            // act
            val exists = userRepository.existsByLoginId("testuser1")

            // assert
            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("existsByLoginId - 존재하지 않는 loginId - false 반환")
        fun existsByLoginId_notExisting_returnsFalse() {
            // act
            val exists = userRepository.existsByLoginId("nonexistent")

            // assert
            assertThat(exists).isFalse()
        }
    }

    @Nested
    @DisplayName("중복 loginId 저장 시 예외 변환")
    inner class SaveDuplicateLoginId {
        @Test
        @DisplayName("동일 loginId로 저장하면 CoreException(USER_DUPLICATE_LOGIN_ID)을 던진다")
        fun save_duplicateLoginId_throwsCoreException() {
            // arrange
            val user = createUser()
            userRepository.save(user)

            val duplicateUser = createUser(email = "other@example.com")

            // act & assert
            val exception = assertThrows<CoreException> {
                userRepository.save(duplicateUser)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }
    }

    @Nested
    @DisplayName("loginId로 회원 조회")
    inner class FindByLoginId {
        @Test
        @DisplayName("존재하는 loginId로 조회 시 User를 반환한다")
        fun findByLoginId_existing_returnsUser() {
            // arrange
            val user = createUser()
            userRepository.save(user)

            // act
            val found = userRepository.findByLoginId("testuser1")

            // assert
            assertAll(
                { assertThat(found).isNotNull },
                { assertThat(found!!.loginId).isEqualTo("testuser1") },
                { assertThat(found!!.name).isEqualTo("홍길동") },
                { assertThat(found!!.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(found!!.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        @DisplayName("존재하지 않는 loginId로 조회 시 null을 반환한다")
        fun findByLoginId_notExisting_returnsNull() {
            // act
            val found = userRepository.findByLoginId("nonexistent")

            // assert
            assertThat(found).isNull()
        }
    }
}
