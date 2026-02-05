package com.loopers.application.user

import com.loopers.domain.user.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserInfoTest {
    @Test
    @DisplayName("UserModel에서 UserInfo로 변환 시 이름이 마스킹된다")
    fun from_masksName() {
        // given
        val userModel = UserModel(
            loginId = LoginId("testuser"),
            password = "hashedPassword123",
            name = Name("김철수"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("test@example.com"),
        )

        // when
        val userInfo = UserInfo.from(userModel)

        // then
        assertThat(userInfo.name).isEqualTo("김철*")
        assertThat(userInfo.loginId).isEqualTo("testuser")
        assertThat(userInfo.birthDate).isEqualTo("1990-01-01")
        assertThat(userInfo.email).isEqualTo("test@example.com")
    }

    @Test
    @DisplayName("영어 이름도 마지막 글자가 마스킹된다")
    fun from_masksEnglishName() {
        // given
        val userModel = UserModel(
            loginId = LoginId("johnsmith"),
            password = "hashedPassword123",
            name = Name("John"),
            birthDate = BirthDate("1985-05-15"),
            email = Email("john@example.com"),
        )

        // when
        val userInfo = UserInfo.from(userModel)

        // then
        assertThat(userInfo.name).isEqualTo("Joh*")
    }

    @Test
    @DisplayName("한 글자 이름도 마스킹된다")
    fun from_masksSingleCharacterName() {
        // given
        val userModel = UserModel(
            loginId = LoginId("user123"),
            password = "hashedPassword123",
            name = Name("김"),
            birthDate = BirthDate("1995-12-25"),
            email = Email("kim@example.com"),
        )

        // when
        val userInfo = UserInfo.from(userModel)

        // then
        assertThat(userInfo.name).isEqualTo("*")
    }
}
