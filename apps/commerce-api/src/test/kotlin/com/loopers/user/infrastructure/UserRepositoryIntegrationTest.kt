package com.loopers.user.infrastructure

import com.loopers.user.domain.User
import com.loopers.user.domain.UserRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@DisplayName("UserRepository 통합 테스트")
@SpringBootTest
class UserRepositoryIntegrationTest
@Autowired
constructor(
    private val userRepository: UserRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(
        loginId: String = "testuser1",
        password: String = "encodedPassword",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): User = User.retrieve(loginId, password, name, birthDate, email)

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
                { assertThat(savedUser.password).isEqualTo("encodedPassword") },
                { assertThat(savedUser.name).isEqualTo("홍길동") },
                { assertThat(savedUser.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(savedUser.email).isEqualTo("test@example.com") },
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
}
