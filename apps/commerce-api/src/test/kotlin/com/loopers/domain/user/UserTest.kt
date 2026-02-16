package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.Test

class UserTest {

    @Test
    fun `회원을 생성한다`() {
        // given + when
        val user = User.create(
            loginId = "test123",
            password = "test1234",
            name = "테스트",
            birthDate = "20260101",
            email = "test@test.com",
        )

        // then
        assertNotNull(user)
        assertEquals("test123", user.loginId)
        assertEquals("test1234", user.password)
        assertEquals("테스트", user.name)
        assertEquals("20260101", user.birthDate)
        assertEquals("test@test.com", user.email)
    }

    @Test
    fun `로그인 아이디는 공백일 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
                )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `로그인 아이디는 최대 20자를 넘을 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "123456789012345678901",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `로그인 아이디는 영문과 숫자의 조합으로만 가능하다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "te@st123!@",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `비밀번호는 공백일 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `비밀번호는 8~16사이여야 한다`() {
        // given + when
        val user = User.create(
            loginId = "test",
            password = "1234567890",
            name = "test",
            birthDate = "20260101",
            email = "test@test.com",
        )

        // then
        assertEquals("1234567890", user.password)
    }

    @Test
    fun `이름은 공백일 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "test1234",
                name = "",
                birthDate = "20260101",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `이름은 1글자 이하일 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "test1234",
                name = "가",
                birthDate = "20260101",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `이름은 2글자 이상이어야 한다`() {
        // given + when
        val user = User.create(
            loginId = "test",
            password = "test1234",
            name = "test",
            birthDate = "20260101",
            email = "test@test.com",
        )

        // then
        assertEquals("test", user.name)
    }

    @Test
    fun `생일은 공백일 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "test1234",
                name = "test",
                birthDate = "",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `생일은 날짜이 아니면 안된다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "test1234",
                name = "test",
                birthDate = "20269901",
                email = "test@test.com",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `생일은 날짜형식 이어야 한다`() {
        // given + when
        val user = User.create(
            loginId = "test",
            password = "test1234",
            name = "test",
            birthDate = "20260101",
            email = "test@test.com",
        )

        // then
        assertEquals("20260101", user.birthDate)
    }

    @Test
    fun `이메일은 공백일 수 없다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `이메일은 이메일 형식 맞지 않으면 안된다`() {
        // given + when + then
        assertThatThrownBy {
            User.create(
                loginId = "test",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "111eeee",
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting { (it as CoreException).errorType }
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `이메일은 이메일 형식에 맞아야 한다`() {
        // given + when
        val user = User.create(
            loginId = "test",
            password = "test1234",
            name = "test",
            birthDate = "20260101",
            email = "111@test.com",
        )

        // then
        assertEquals("111@test.com", user.email)
    }
}
