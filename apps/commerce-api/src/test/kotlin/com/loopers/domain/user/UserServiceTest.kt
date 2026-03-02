package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    private val userRepository: UserRepository = mockk()

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }

    private fun createUserModel() = UserModel(
        loginId = LoginId("test1234"),
        encryptedPassword = "encrypted_password",
        name = Name("loopers"),
        birthDate = BirthDate("2000-01-01"),
        email = Email("test@loopers.com"),
    )

    @Nested
    inner class CreateUser {

        @Test
        fun `사용자 생성 성공`() {
            // given
            every { userRepository.existsByLoginId(LoginId("test1234")) } returns false
            every { userRepository.save(any()) } answers { firstArg() }

            // when
            val result = userService.createUser(
                loginId = "test1234",
                encryptedPassword = "encrypted_password",
                name = "loopers",
                birthDate = "2000-01-01",
                email = "test@loopers.com",
            )

            // then
            assertNotNull(result)
            verify(exactly = 1) { userRepository.save(any()) }
        }

        @Test
        fun `이미 존재하는 loginId면 CONFLICT 예외`() {
            // given
            every { userRepository.existsByLoginId(LoginId("test1234")) } returns true

            // when
            val exception = assertThrows<CoreException> {
                userService.createUser(
                    loginId = "test1234",
                    encryptedPassword = "encrypted_password",
                    name = "loopers",
                    birthDate = "2000-01-01",
                    email = "test@loopers.com",
                )
            }

            // then
            assertEquals(ErrorType.CONFLICT, exception.errorType)
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    inner class GetUserByLoginId {

        @Test
        fun `존재하는 loginId로 조회 성공`() {
            // given
            val user = createUserModel()
            every { userRepository.findByLoginId(LoginId("test1234")) } returns user

            // when
            val result = userService.getUserByLoginId("test1234")

            // then
            assertNotNull(result)
            assertEquals("test1234", result.loginId.value)
        }

        @Test
        fun `존재하지 않는 loginId 조회 시 NOT_FOUND 예외`() {
            // given
            every { userRepository.findByLoginId(LoginId("nonexistent")) } returns null

            // when
            val exception = assertThrows<CoreException> {
                userService.getUserByLoginId("nonexistent")
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }
    }

    @Nested
    inner class UpdatePassword {

        @Test
        fun `존재하지 않는 loginId로 수정 시 NOT_FOUND 예외`() {
            // given
            every { userRepository.findByLoginId(LoginId("nonexistent")) } returns null

            // when
            val exception = assertThrows<CoreException> {
                userService.updatePassword("nonexistent", "new_encrypted_password")
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }

        @Test
        fun `유효한 비밀번호로 수정 성공`() {
            // given
            val user = createUserModel()
            every { userRepository.findByLoginId(LoginId("test1234")) } returns user

            // when
            userService.updatePassword("test1234", "new_encrypted_password")

            // then
            assertEquals("new_encrypted_password", user.password)
        }
    }
}
