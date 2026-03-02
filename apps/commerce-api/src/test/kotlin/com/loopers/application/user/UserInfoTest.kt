package com.loopers.application.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.UserModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserInfoTest {
    @Test
    fun `UserModel에서 UserInfo로 변환 시 이름이 마스킹된다`() {
        // given
        val userModel = UserModel(
            loginId = LoginId("testuser"),
            encryptedPassword = "hashedPassword123",
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("test@example.com"),
        )

        // when
        val userInfo = UserInfo.from(userModel)

        // then
        assertThat(userInfo.name).isEqualTo("홍길*")
        assertThat(userInfo.loginId).isEqualTo("testuser")
        assertThat(userInfo.birthDate).isEqualTo("1990-01-01")
        assertThat(userInfo.email).isEqualTo("test@example.com")
    }

    @Test
    fun `영어 이름도 마지막 글자가 마스킹된다`() {
        val userModel = UserModel(
            loginId = LoginId("johnsmith"),
            encryptedPassword = "hashedPassword123",
            name = Name("John"),
            birthDate = BirthDate("1985-05-15"),
            email = Email("john@example.com"),
        )

        val userInfo = UserInfo.from(userModel)

        assertThat(userInfo.name).isEqualTo("Joh*")
    }

    @Test
    fun `한 글자 이름도 마스킹된다`() {
        // given
        val userModel = UserModel(
            loginId = LoginId("user123"),
            encryptedPassword = "hashedPassword123",
            name = Name("홍"),
            birthDate = BirthDate("1995-12-25"),
            email = Email("kim@example.com"),
        )

        // when
        val userInfo = UserInfo.from(userModel)

        // then
        assertThat(userInfo.name).isEqualTo("*")
    }
}
