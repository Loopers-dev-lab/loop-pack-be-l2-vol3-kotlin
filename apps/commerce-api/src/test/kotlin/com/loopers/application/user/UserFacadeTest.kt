package com.loopers.application.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.domain.user.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class UserFacadeTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userFacade: UserFacade

    @DisplayName("회원가입")
    @Nested
    inner class Register {

        @DisplayName("회원가입 성공 시, UserInfo를 반환한다.")
        @Test
        fun success() {
            // arrange
            val loginId = "testuser"
            val rawPassword = "Test123!"
            val name = "홍길동"
            val birthDate = LocalDate.of(1990, 1, 1)
            val email = "test@example.com"

            val savedUser = createUser(
                loginId = loginId,
                name = name,
                birthDate = birthDate,
                email = email,
            )
            whenever(userService.register(any(), any(), any(), any(), any())).thenReturn(savedUser)

            // act
            val result = userFacade.register(
                loginId = loginId,
                rawPassword = rawPassword,
                name = name,
                birthDate = birthDate,
                email = email,
            )

            // assert
            assertAll(
                { assertThat(result.loginId).isEqualTo(loginId) },
                { assertThat(result.name).isEqualTo("홍길*") },
                { assertThat(result.birthDate).isEqualTo(birthDate) },
                { assertThat(result.email).isEqualTo(email) },
            )
        }
    }

    private fun createUser(
        loginId: String = "testuser",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): User = User.create(
        loginId = loginId,
        encodedPassword = "encodedPassword",
        name = name,
        birthDate = birthDate,
        email = email,
    )
}
