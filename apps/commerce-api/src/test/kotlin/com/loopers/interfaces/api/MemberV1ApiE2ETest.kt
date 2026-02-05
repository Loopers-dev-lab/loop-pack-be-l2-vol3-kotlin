package com.loopers.interfaces.api

import com.loopers.domain.member.MemberService
import com.loopers.domain.member.RegisterCommand
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val memberService: MemberService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/members"
        private const val ENDPOINT_ME = "/api/v1/members/me"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/members/me/password"

        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("POST /api/v1/members - 회원가입")
    inner class RegisterMemberTest {

        @Test
        @DisplayName("유효한 정보로 회원가입 성공")
        fun `should register member successfully with valid data`() {
            // Arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser123",
                password = "Valid@Pass123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )

            val savedMember = memberJpaRepository.findByLoginId("testuser123")
            assertThat(savedMember).isNotNull
        }

        @Test
        @DisplayName("이미 존재하는 loginId로 가입 시 409 Conflict")
        fun `should return 409 when loginId already exists`() {
            // Arrange
            memberService.register(
                RegisterCommand(
                    loginId = "testuser123",
                    password = "Valid@Pass123",
                    name = "기존유저",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "existing@example.com",
                ),
            )

            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser123",
                password = "NewValid@Pass123",
                name = "홍길동",
                birthDate = LocalDate.of(1995, 5, 5),
                email = "test@example.com",
            )

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        @DisplayName("잘못된 이메일 형식으로 가입 시 400 Bad Request")
        fun `should return 400 when email format is invalid`() {
            // Arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser123",
                password = "Valid@Pass123",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "invalid-email-format",
            )

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/members/me - 내 정보 조회")
    inner class GetMyInfoTest {

        @Test
        @DisplayName("유효한 헤더로 조회 성공, 이름 마지막 글자 마스킹 확인")
        fun `should return my info with name masked successfully`() {
            // Arrange
            memberService.register(
                RegisterCommand(
                    loginId = "testuser123",
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser123")
                set(HEADER_LOGIN_PW, "Valid@Pass123")
            }

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {},
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser123") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },
                { assertThat(response.body?.data?.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        @DisplayName("존재하지 않는 loginId 헤더로 404 Not Found")
        fun `should return 404 when loginId does not exist`() {
            // Arrange
            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "nonexistent")
                set(HEADER_LOGIN_PW, "SomePass@123")
            }

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        @DisplayName("잘못된 비밀번호 헤더로 400 Bad Request")
        fun `should return 400 when password is incorrect`() {
            // Arrange
            memberService.register(
                RegisterCommand(
                    loginId = "testuser123",
                    password = "Valid@Pass123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser123")
                set(HEADER_LOGIN_PW, "WrongPassword@123")
            }

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("헤더 누락 시 400 Bad Request")
        fun `should return 400 when required headers are missing`() {
            // Arrange
            val headers = HttpHeaders()

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/members/me/password - 비밀번호 수정")
    inner class ChangePasswordTest {

        @Test
        @DisplayName("유효한 정보로 비밀번호 변경 성공")
        fun `should change password successfully with valid data`() {
            // Arrange
            memberService.register(
                RegisterCommand(
                    loginId = "testuser123",
                    password = "OldPass@123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser123")
                set(HEADER_LOGIN_PW, "OldPass@123")
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                newPassword = "NewPass@456",
            )

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @Test
        @DisplayName("잘못된 현재 비밀번호로 400 Bad Request")
        fun `should return 400 when current password is incorrect`() {
            // Arrange
            memberService.register(
                RegisterCommand(
                    loginId = "testuser123",
                    password = "OldPass@123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser123")
                set(HEADER_LOGIN_PW, "WrongPass@123")
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                newPassword = "NewPass@456",
            )

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호가 현재와 동일할 때 400 Bad Request")
        fun `should return 400 when new password is same as current`() {
            // Arrange
            memberService.register(
                RegisterCommand(
                    loginId = "testuser123",
                    password = "SamePass@123",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser123")
                set(HEADER_LOGIN_PW, "SamePass@123")
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                newPassword = "SamePass@123",
            )

            // Act
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
