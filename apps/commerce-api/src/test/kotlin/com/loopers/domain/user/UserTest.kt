package com.loopers.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserTest {

    @Nested
    @DisplayName("User 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 정보로 생성하면 성공한다")
        fun createUser_withValidData_success() {
            // arrange
            val loginId = "testuser1"
            val password = "Password1!"
            val name = "홍길동"
            val birthDate = LocalDate.of(1990, 1, 15)
            val email = "test@example.com"

            // act
            val user = User(loginId, password, name, birthDate, email)

            // assert
            assertThat(user.loginId).isEqualTo(loginId)
            assertThat(user.name).isEqualTo(name)
            assertThat(user.email).isEqualTo(email)
        }

        @Test
        @DisplayName("비밀번호는 암호화되어 저장된다")
        fun createUser_passwordIsEncoded() {
            // arrange & act
            val user = User("testuser1", "Password1!", "홍길동",
                LocalDate.of(1990, 1, 15), "test@example.com")

            // assert
            assertThat(user.password).isNotEqualTo("Password1!")
        }
    }
}
