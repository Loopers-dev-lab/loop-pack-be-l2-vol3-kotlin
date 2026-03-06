package com.loopers.domain.user

import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

class UserTest {

    @DisplayName("회원을 생성한다")
    @Test
    fun createUser() {
        // given + when
        val user = User.create(
            loginId = LoginId.of("test123"),
            password = Password.ofEncrypted("encryptedPassword"),
            name = Name.of("테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("test@test.com"),
        )

        // then
        assertNotNull(user)
        assertEquals("test123", user.loginId.value)
        assertEquals("encryptedPassword", user.password.value)
        assertEquals("테스트", user.name.value)
        assertEquals("20260101", user.birthDate.value)
        assertEquals("test@test.com", user.email.value)
    }
}
